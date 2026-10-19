package org.facenet.service.scale.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.engine.util.ProtocolRuntimeDependencies;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * S-Bus Engine - Giao thức COMMAND-BASED (không phải register-based như Modbus)
 * 
 * Config example:
 * {
 *   "command": "READ_WEIGHT" hoặc "opcode": "0x57",
 *   "payload": "",
 *   "mode": "ASCII",
 *   "response_type": "FLOAT32",
 *   "response_length": 4
 * }
 */
@Slf4j
public class SbusEngine implements ScaleEngine {
    
    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;
    private volatile boolean running = false;
    private SerialPort serialPort;
    
    private static final byte START_BYTE = 0x02;
    private static final byte END_BYTE = 0x03;
    private static final byte ACK_BYTE = 0x06;
    private static final byte NAK_BYTE = 0x15;
    
    private static final byte CMD_READ_WEIGHT = 0x57;
    private static final byte CMD_ZERO = 0x5A;
    private static final byte CMD_TARE = 0x54;
    
    public SbusEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        ProtocolRuntimeDependencies.prepareDirectSerial("S-Bus");
        this.config = config;
        this.queue = queue;
    }
    
    @Override
    public void run() {
        running = true;
        int pollIntervalMs = getPollIntervalMs();
        log.info("[Engine {}] S-Bus Engine started", config.getScaleId());
        
        try {
            String comPort = getConnParamAsString("com_port");
            Integer baudRate = getConnParamAsInt("baud_rate");
            Integer connectionId = getConnParamAsInt("id"); // Lấy ID từ connection_params
            
            if (comPort == null || baudRate == null) {
                log.error("[Engine {}] Missing COM port or baud rate", config.getScaleId());
                return;
            }
            
            // Log thông tin ID nếu có
            if (connectionId != null) {
                log.info("[Engine {}] Connection ID from connection_params: {}", config.getScaleId(), connectionId);
            }
            
            serialPort = SerialPort.getCommPort(comPort);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);
            
            if (!serialPort.openPort()) {
                log.error("[Engine {}] Failed to open port {}", config.getScaleId(), comPort);
                return;
            }
            
            if (connectionId != null) {
                log.info("[Engine {}] Connected to {} (Connection ID: {})", config.getScaleId(), comPort, connectionId);
            } else {
                log.info("[Engine {}] Connected to {}", config.getScaleId(), comPort);
            }
            
            Integer deviceId = getConnParamAsInt("device_id");
            if (deviceId == null) deviceId = 1;
            
            while (!stopped) {
                try {
                    MeasurementEvent event = MeasurementEvent.builder()
                            .scaleId(config.getScaleId())
                            .lastTime(ZonedDateTime.now())
                            .status("ONLINE")
                            .build();
                    
                    event.setData1(readDataField(deviceId, config.getData1()));
                    event.setData2(readDataField(deviceId, config.getData2()));
                    event.setData3(readDataField(deviceId, config.getData3()));
                    event.setData4(readDataField(deviceId, config.getData4()));
                    event.setData5(readDataField(deviceId, config.getData5()));
                    
                    queue.put(event);
                    log.debug("[Engine {}] Pushed S-Bus data", config.getScaleId());
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[Engine {}] Error: {}", config.getScaleId(), e.getMessage());
                    Thread.sleep(5000);
                }
                
                Thread.sleep(pollIntervalMs);
            }
            
        } catch (Exception e) {
            log.error("[Engine {}] Fatal error: {}", config.getScaleId(), e.getMessage(), e);
        } finally {
            cleanup();
        }
        
        running = false;
        log.info("[Engine {}] S-Bus Engine stopped", config.getScaleId());
    }
    
    private DataField readDataField(int deviceId, Map<String, Object> dataConfig) {
        if (dataConfig == null || !isDataSlotUsed(dataConfig)) return null;
        
        String name = getDataName(dataConfig);
        String value = readByCommand(deviceId, dataConfig);
        
        if (value == null) return null;
        
        return DataField.builder()
                .name(name)
                .value(value)
                .build();
    }
    
    private String readByCommand(int deviceId, Map<String, Object> dataConfig) {
        try {
            String commandName = getDataConfigParam(dataConfig, "command");
            String opcodeHex = getDataConfigParam(dataConfig, "opcode");
            
            byte opcode = CMD_READ_WEIGHT; // default
            if (opcodeHex != null) {
                String cleaned = opcodeHex.replace("0x", "").replace("0X", "").trim();
                opcode = (byte) Integer.parseInt(cleaned, 16);
            } else if (commandName != null) {
                opcode = mapCommandToOpcode(commandName);
            }
            
            String payload = getDataConfigParam(dataConfig, "payload");
            if (payload == null) payload = "";
            
            String mode = getDataConfigParam(dataConfig, "mode");
            if (mode == null) mode = "ASCII";
            
            Integer responseLength = getDataConfigParam(dataConfig, "response_length");
            
            if ("ASCII".equalsIgnoreCase(mode)) {
                return sendCommandASCII(deviceId, opcode, payload, dataConfig);
            } else {
                return sendCommandBinary(deviceId, opcode, payload, dataConfig, responseLength);
            }
            
        } catch (Exception e) {
            log.error("[Engine {}] Error reading command: {}", config.getScaleId(), e.getMessage());
            return null;
        }
    }
    
    private byte mapCommandToOpcode(String commandName) {
        switch (commandName.toUpperCase(Locale.ROOT)) {
            case "READ_WEIGHT":
            case "WEIGHT":
                return CMD_READ_WEIGHT;
            case "ZERO":
                return CMD_ZERO;
            case "TARE":
                return CMD_TARE;
            default:
                return CMD_READ_WEIGHT;
        }
    }
    
    private String sendCommandASCII(int deviceId, byte opcode, String payload, Map<String, Object> dataConfig)
            throws IOException, InterruptedException {
        OutputStream out = serialPort.getOutputStream();
        InputStream in = serialPort.getInputStream();
        
        StringBuilder cmd = new StringBuilder();
        cmd.append((char) START_BYTE);
        cmd.append(String.format("%02X", deviceId));
        cmd.append(String.format("%02X", opcode & 0xFF));
        if (payload != null && !payload.isEmpty()) {
            cmd.append(payload);
        }
        
        byte bcc = 0;
        String dataForBCC = cmd.substring(1);
        for (char c : dataForBCC.toCharArray()) {
            bcc ^= (byte) c;
        }
        cmd.append(String.format("%02X", bcc & 0xFF));
        cmd.append((char) END_BYTE);
        
        byte[] commandBytes = cmd.toString().getBytes(StandardCharsets.US_ASCII);
        out.write(commandBytes);
        out.flush();
        
        Thread.sleep(100);
        
        byte[] buffer = new byte[256];
        int bytesRead = in.read(buffer);
        
        if (bytesRead <= 0) return null;
        
        String response = new String(buffer, 0, bytesRead, StandardCharsets.US_ASCII);
        
        if (response.startsWith(String.valueOf((char) NAK_BYTE))) return null;
        if (response.startsWith(String.valueOf((char) ACK_BYTE))) return "ACK";
        if (!response.startsWith(String.valueOf((char) START_BYTE))) return null;
        
        int dataStart = 5;
        int dataEnd = response.indexOf((char) END_BYTE);
        if (dataEnd > dataStart + 2) {
            String dataHex = response.substring(dataStart, dataEnd - 2);
            return parseHexData(dataHex, dataConfig);
        }
        
        return null;
    }
    
    private String sendCommandBinary(int deviceId, byte opcode, String payload, 
                                      Map<String, Object> dataConfig, Integer responseLength)
            throws IOException, InterruptedException {
        OutputStream out = serialPort.getOutputStream();
        InputStream in = serialPort.getInputStream();
        
        byte[] payloadBytes = payload != null && !payload.isEmpty() ? 
                hexStringToBytes(payload) : new byte[0];
        
        byte[] cmd = new byte[3 + payloadBytes.length + 2];
        cmd[0] = START_BYTE;
        cmd[1] = (byte) (deviceId & 0xFF);
        cmd[2] = opcode;
        System.arraycopy(payloadBytes, 0, cmd, 3, payloadBytes.length);
        
        int crc = calculateCRC16(cmd, 0, 3 + payloadBytes.length);
        cmd[3 + payloadBytes.length] = (byte) (crc & 0xFF);
        cmd[3 + payloadBytes.length + 1] = (byte) ((crc >> 8) & 0xFF);
        
        out.write(cmd);
        out.flush();
        
        Thread.sleep(100);
        
        byte[] buffer = new byte[256];
        int bytesRead = in.read(buffer);
        
        if (bytesRead <= 0) return null;
        if (buffer[0] == NAK_BYTE) return null;
        if (buffer[0] == ACK_BYTE) return "ACK";
        if (buffer[0] != START_BYTE) return null;
        
        int dataLength = bytesRead - 5;
        if (responseLength != null && responseLength > 0) {
            dataLength = Math.min(dataLength, responseLength);
        }
        
        if (dataLength > 0) {
            return parseBinaryData(buffer, 3, dataLength, dataConfig);
        }
        
        return null;
    }
    
    static String parseHexData(String hexData, Map<String, Object> dataConfig) {
        try {
            if (hexData == null || hexData.isEmpty()) return null;
            String responseType = text(dataConfig, "response_type");
            if (responseType == null) return hexData;

            byte[] bytes = hexStringToBytesStatic(hexData);
            return decodeResponse(bytes, dataConfig);
        } catch (Exception e) {
            return hexData;
        }
    }
    
    static String parseBinaryData(byte[] buffer, int offset, int length, Map<String, Object> dataConfig) {
        try {
            if (length == 0) return null;
            Map<String, Object> effectiveConfig = dataConfig;
            if (text(dataConfig, "response_type") == null) {
                effectiveConfig = new java.util.LinkedHashMap<>(dataConfig == null ? Map.of() : dataConfig);
                effectiveConfig.put("response_type", "UINT16");
            }
            return decodeResponse(Arrays.copyOfRange(buffer, offset, offset + length), effectiveConfig);
        } catch (Exception e) {
            return null;
        }
    }

    static String decodeResponse(byte[] response, Map<String, Object> dataConfig) {
        if (response == null || response.length == 0) return null;
        String responseType = text(dataConfig, "response_type");
        if (responseType == null) return bytesToHex(response);

        byte[] bytes = orderedBytes(response, dataConfig);
        String type = responseType.toUpperCase(Locale.ROOT);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        Object decoded = switch (type) {
            case "INT16", "SHORT" -> require(buffer, 2).getShort();
            case "UINT16", "WORD" -> Short.toUnsignedInt(require(buffer, 2).getShort());
            case "INT32", "DINT" -> require(buffer, 4).getInt();
            case "UINT32", "DWORD" -> Integer.toUnsignedLong(require(buffer, 4).getInt());
            case "INT64", "LINT", "LONG" -> require(buffer, 8).getLong();
            case "UINT64", "LWORD" -> new BigInteger(1, Arrays.copyOf(bytes, 8));
            case "FLOAT32", "FLOAT", "REAL" -> require(buffer, 4).getFloat();
            case "FLOAT64", "DOUBLE", "LREAL" -> require(buffer, 8).getDouble();
            default -> throw new IllegalArgumentException("Unsupported S-Bus response_type: " + responseType);
        };
        return String.valueOf(decoded);
    }

    static byte[] orderedBytes(byte[] response, Map<String, Object> dataConfig) {
        if (response.length % 2 != 0) {
            throw new IllegalArgumentException("S-Bus numeric response must contain complete 16-bit words");
        }
        String byteOrder = valueOrDefault(text(dataConfig, "byte_order"), "BIG_ENDIAN").toUpperCase(Locale.ROOT);
        String wordOrder = valueOrDefault(text(dataConfig, "word_order"), "BIG_ENDIAN").toUpperCase(Locale.ROOT);

        if (byteOrder.equals("ABCD") || byteOrder.equals("BIG") || byteOrder.equals("BE")) {
            byteOrder = "BIG_ENDIAN";
        } else if (byteOrder.equals("BADC")) {
            byteOrder = "LITTLE_ENDIAN";
            wordOrder = "BIG_ENDIAN";
        } else if (byteOrder.equals("CDAB") || byteOrder.equals("WORD_SWAP")) {
            byteOrder = "BIG_ENDIAN";
            wordOrder = "LITTLE_ENDIAN";
        } else if (byteOrder.equals("DCBA") || byteOrder.equals("LITTLE") || byteOrder.equals("LE")) {
            byteOrder = "LITTLE_ENDIAN";
            wordOrder = "LITTLE_ENDIAN";
        }

        byte[] ordered = Arrays.copyOf(response, response.length);
        if (isLittle(byteOrder)) {
            for (int index = 0; index < ordered.length; index += 2) {
                byte swap = ordered[index];
                ordered[index] = ordered[index + 1];
                ordered[index + 1] = swap;
            }
        }
        if (isLittle(wordOrder)) {
            for (int left = 0, right = ordered.length - 2; left < right; left += 2, right -= 2) {
                byte high = ordered[left];
                byte low = ordered[left + 1];
                ordered[left] = ordered[right];
                ordered[left + 1] = ordered[right + 1];
                ordered[right] = high;
                ordered[right + 1] = low;
            }
        }
        return ordered;
    }

    private static boolean isLittle(String value) {
        return value.equals("LITTLE_ENDIAN") || value.equals("LITTLE") || value.equals("LE")
                || value.equals("LOW_FIRST") || value.equals("LSW_FIRST");
    }

    private static ByteBuffer require(ByteBuffer buffer, int length) {
        if (buffer.remaining() < length) {
            throw new IllegalArgumentException("S-Bus response type requires " + length
                    + " bytes, got " + buffer.remaining());
        }
        return buffer;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02X", value & 0xFF));
        return result.toString();
    }

    private static byte[] hexStringToBytesStatic(String hexString) {
        String cleaned = hexString.replace(" ", "").replace("0x", "").replace("0X", "");
        if (cleaned.length() % 2 != 0) throw new IllegalArgumentException("Hex response must contain complete bytes");
        byte[] data = new byte[cleaned.length() / 2];
        for (int index = 0; index < cleaned.length(); index += 2) {
            data[index / 2] = (byte) Integer.parseInt(cleaned.substring(index, index + 2), 16);
        }
        return data;
    }

    private static String text(Map<String, Object> values, String key) {
        if (values == null || values.get(key) == null || values.get(key).toString().isBlank()) return null;
        return values.get(key).toString().trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
    
    private byte[] hexStringToBytes(String hexString) {
        String cleaned = hexString.replace(" ", "").replace("0x", "");
        byte[] data = new byte[cleaned.length() / 2];
        for (int i = 0; i < cleaned.length(); i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleaned.charAt(i), 16) << 4)
                    + Character.digit(cleaned.charAt(i + 1), 16));
        }
        return data;
    }
    
    private int calculateCRC16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc = crc >> 1;
                }
            }
        }
        return crc;
    }
    
    private String getDataName(Map<String, Object> dataConfig) {
        if (dataConfig == null) return null;
        Object name = dataConfig.get("name");
        return name != null ? name.toString() : null;
    }
    
    private boolean isDataSlotUsed(Map<String, Object> dataConfig) {
        Object used = dataConfig.get("used");
        if (used == null) used = dataConfig.get("is_used");
        return toBoolean(used);
    }
    
    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        String s = value.toString().trim().toLowerCase(Locale.ROOT);
        return s.equals("true") || s.equals("1") || s.equals("yes");
    }
    
    private int getPollIntervalMs() {
        Integer pollInterval = config.getPollInterval();
        return (pollInterval != null && pollInterval > 0) ? pollInterval : 1000;
    }
    
    private String getConnParamAsString(String key) {
        Object connParams = config.getConnParams();
        if (connParams instanceof Map) {
            Object value = ((Map<?, ?>) connParams).get(key);
            return value != null ? value.toString() : null;
        } else if (connParams instanceof JsonNode) {
            JsonNode node = ((JsonNode) connParams).get(key);
            return node != null ? node.asText() : null;
        }
        return null;
    }
    
    private Integer getConnParamAsInt(String key) {
        Object connParams = config.getConnParams();
        Object value = null;
        if (connParams instanceof Map) {
            value = ((Map<?, ?>) connParams).get(key);
        } else if (connParams instanceof JsonNode) {
            JsonNode node = ((JsonNode) connParams).get(key);
            if (node != null && node.isInt()) return node.asInt();
            if (node != null) value = node.asText();
        }
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getDataConfigParam(Map<String, Object> dataConfig, String key) {
        return (T) dataConfig.get(key);
    }
    
    private void cleanup() {
        try {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
                log.info("[Engine {}] Closed serial port", config.getScaleId());
            }
        } catch (Exception e) {
            log.error("[Engine {}] Error during cleanup: {}", config.getScaleId(), e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        log.info("[Engine {}] Stop requested", config.getScaleId());
        stopped = true;
    }
    
    @Override
    public Long getScaleId() {
        return config.getScaleId();
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
}
