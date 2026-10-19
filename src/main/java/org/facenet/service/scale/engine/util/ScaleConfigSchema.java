package org.facenet.service.scale.engine.util;

import org.facenet.common.exception.ValidationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Canonicalizes the JSON configuration accepted by both Modbus engines. */
public final class ScaleConfigSchema {

    private ScaleConfigSchema() {
    }

    public static Map<String, Object> normalizeDataField(Map<String, Object> source) {
        return normalizeDataField("MODBUS_TCP", source);
    }

    public static Map<String, Object> normalizeDataField(String protocol, Map<String, Object> source) {
        if (source == null) {
            return null;
        }

        Map<String, Object> normalized = new LinkedHashMap<>(source);
        copyAlias(normalized, "is_used", "used");
        copyAlias(normalized, "start_registers", "start_register", "register_start");
        copyAlias(normalized, "num_registers", "register_count");
        copyAlias(normalized, "tag_address", "address", "tag", "plc_address");
        copyAlias(normalized, "command", "sabus_command", "request", "request_command");
        copyAlias(normalized, "response_pattern", "regex", "extract_pattern");
        copyAlias(normalized, "value_type", "result_type");

        String normalizedProtocol = normalizeProtocol(protocol);

        if (asBoolean(normalized.get("is_used"))) {
            if (isModbus(normalizedProtocol)) {
                Integer start = asInteger(normalized.get("start_registers"));
                Integer count = asInteger(normalized.get("num_registers"));
                if (start == null || start < 0) {
                    throw new ValidationException("Enabled Modbus data field requires start_registers >= 0");
                }
                if (count == null || count <= 0) {
                    throw new ValidationException("Enabled Modbus data field requires num_registers > 0");
                }
                normalized.put("start_registers", start);
                normalized.put("num_registers", count);
                Integer functionCode = asInteger(normalized.getOrDefault("function_code", 3));
                if (functionCode == null || (functionCode != 3 && functionCode != 4)) {
                    throw new ValidationException("Modbus register field supports function_code 3 or 4");
                }
                normalized.put("function_code", functionCode);
                normalized.putIfAbsent("byte_order", "BIG_ENDIAN");
                normalized.putIfAbsent("word_order", "BIG_ENDIAN");
            } else if (isS7(normalizedProtocol)) {
                String tagAddress = asText(normalized.get("tag_address"));
                if (tagAddress == null) {
                    throw new ValidationException("Enabled Siemens S7 data field requires tag_address");
                }
                normalized.put("tag_address", tagAddress);
            } else if (isSabus(normalizedProtocol)) {
                String command = asText(normalized.get("command"));
                if (command == null || !(command.equalsIgnoreCase("FL") || command.equalsIgnoreCase("NT"))) {
                    throw new ValidationException("Enabled SABUS data field requires command FL or NT");
                }
                normalized.put("command", command.toUpperCase(Locale.ROOT));
            } else if (isSbus(normalizedProtocol)) {
                validateSbusDataField(normalized);
            } else if (isSerial(normalizedProtocol)) {
                validateSerialDataField(normalized);
            }
        }

        return normalized;
    }

    public static Map<String, Object> normalizeConnection(String protocol, Map<String, Object> source) {
        if (source == null) {
            return null;
        }

        Map<String, Object> normalized = new LinkedHashMap<>(source);
        copyAlias(normalized, "unit_id", "device_id", "slave_id", "id");
        String normalizedProtocol = normalizeProtocol(protocol);

        if (normalizedProtocol.equals("MODBUS_TCP") || normalizedProtocol.equals("MODBUSTCP")
                || normalizedProtocol.equals("TCP")) {
            String ip = asText(normalized.get("ip"));
            Integer port = asInteger(normalized.get("port"));
            if (ip == null) {
                throw new ValidationException("Modbus TCP connection requires ip");
            }
            if (port == null || port < 1 || port > 65_535) {
                throw new ValidationException("Modbus TCP connection requires port between 1 and 65535");
            }
            normalized.put("ip", ip);
            normalized.put("port", port);
            normalized.putIfAbsent("response_timeout_ms", 5000);
        }

        if (normalizedProtocol.equals("MODBUS_RTU") || normalizedProtocol.equals("MODBUSRTU")
                || normalizedProtocol.equals("RTU")) {
            String comPort = asText(normalized.get("com_port"));
            Integer baudRate = asInteger(normalized.get("baud_rate"));
            Integer unitId = asInteger(normalized.get("unit_id"));
            if (comPort == null) {
                throw new ValidationException("Modbus RTU connection requires com_port");
            }
            if (baudRate == null || baudRate <= 0) {
                throw new ValidationException("Modbus RTU connection requires baud_rate > 0");
            }
            if (unitId == null || unitId < 1 || unitId > 247) {
                throw new ValidationException("Modbus RTU connection requires unit_id between 1 and 247");
            }
            normalized.put("com_port", comPort);
            normalized.put("baud_rate", baudRate);
            normalized.put("unit_id", unitId);
            normalized.putIfAbsent("data_bits", 8);
            normalized.putIfAbsent("stop_bits", 1);
            normalized.putIfAbsent("parity", "NONE");
            normalized.putIfAbsent("response_timeout_ms", 3000);
        }

        if (isS7(normalizedProtocol)) {
            copyAlias(normalized, "ip", "host", "ip_address");
            copyAlias(normalized, "rack", "remote_rack");
            copyAlias(normalized, "slot", "remote_slot");
            copyAlias(normalized, "controller_type", "plc_type", "cpu_type");
            copyAlias(normalized, "port", "tcp_port");

            String ip = asText(normalized.get("ip"));
            if (ip == null) {
                throw new ValidationException("Siemens S7 connection requires ip");
            }
            Integer rack = asInteger(normalized.getOrDefault("rack", 0));
            Integer slot = asInteger(normalized.getOrDefault("slot", 1));
            Integer port = asInteger(normalized.getOrDefault("port", 102));
            if (port == null || port < 1 || port > 65_535) {
                throw new ValidationException("Siemens S7 port must be between 1 and 65535");
            }
            if (rack == null || rack < 0 || rack > 7) {
                throw new ValidationException("Siemens S7 rack must be between 0 and 7");
            }
            if (slot == null || slot < 0 || slot > 31) {
                throw new ValidationException("Siemens S7 slot must be between 0 and 31");
            }
            normalized.put("ip", ip);
            normalized.put("port", port);
            normalized.put("rack", rack);
            normalized.put("slot", slot);
            normalized.putIfAbsent("connect_timeout_ms", 5000);
            normalized.putIfAbsent("read_timeout_ms", 5000);
        }

        if (isSabus(normalizedProtocol)) {
            copyAlias(normalized, "device_id", "station_id", "unit_id", "id");
            String comPort = asText(normalized.get("com_port"));
            String deviceId = asText(normalized.get("device_id"));
            Integer baudRate = asInteger(normalized.get("baud_rate"));
            if (comPort == null) throw new ValidationException("SABUS connection requires com_port");
            if (baudRate == null || baudRate <= 0) {
                throw new ValidationException("SABUS connection requires baud_rate > 0");
            }
            if (deviceId == null) throw new ValidationException("SABUS connection requires device_id");
            normalized.put("com_port", comPort);
            normalized.put("baud_rate", baudRate);
            normalized.put("device_id", deviceId);
            normalized.putIfAbsent("device_id_width", 2);
            normalized.putIfAbsent("data_bits", 8);
            normalized.putIfAbsent("stop_bits", 1);
            normalized.putIfAbsent("parity", "NONE");
            normalized.putIfAbsent("response_timeout_ms", 3000);
            normalized.putIfAbsent("reconnect_delay_ms", 5000);
            normalized.putIfAbsent("offline_threshold_ms", 60000);
            normalized.putIfAbsent("command_prefix", "!");
            normalized.putIfAbsent("command_terminator", "\\r");
        }

        if (isSerial(normalizedProtocol)) {
            copyAlias(normalized, "com_port", "serial_port", "port_name");
            String comPort = asText(normalized.get("com_port"));
            Integer baudRate = asInteger(normalized.get("baud_rate"));
            Integer dataBits = asInteger(normalized.getOrDefault("data_bits", 8));
            String stopBits = asText(normalized.getOrDefault("stop_bits", 1));
            String parity = asText(normalized.getOrDefault("parity", "NONE"));
            String charset = asText(normalized.getOrDefault("charset", "US-ASCII"));
            Integer maximum = asInteger(normalized.getOrDefault("max_response_bytes", 1024));

            if (comPort == null) throw new ValidationException("Serial connection requires com_port");
            if (baudRate == null || baudRate <= 0) {
                throw new ValidationException("Serial connection requires baud_rate > 0");
            }
            if (dataBits == null || dataBits < 5 || dataBits > 8) {
                throw new ValidationException("Serial data_bits must be between 5 and 8");
            }
            if (stopBits == null || !Set.of("1", "1.0", "1.5", "2", "2.0").contains(stopBits)) {
                throw new ValidationException("Serial stop_bits must be 1, 1.5 or 2");
            }
            if (stopBits.equals("1.0")) stopBits = "1";
            if (stopBits.equals("2.0")) stopBits = "2";
            if (parity == null || !Set.of("NONE", "EVEN", "ODD", "MARK", "SPACE")
                    .contains(parity.toUpperCase(Locale.ROOT))) {
                throw new ValidationException("Serial parity must be NONE, EVEN, ODD, MARK or SPACE");
            }
            try {
                if (charset == null || !Charset.isSupported(charset)) {
                    throw new ValidationException("Serial charset is not supported");
                }
            } catch (IllegalArgumentException exception) {
                throw new ValidationException("Serial charset is not supported");
            }
            if (maximum == null || maximum < 1 || maximum > 1_048_576) {
                throw new ValidationException("Serial max_response_bytes must be between 1 and 1048576");
            }

            normalized.put("com_port", comPort);
            normalized.put("baud_rate", baudRate);
            normalized.put("data_bits", dataBits);
            normalized.put("stop_bits", stopBits);
            normalized.put("parity", parity.toUpperCase(Locale.ROOT));
            normalized.put("charset", charset);
            normalized.put("max_response_bytes", maximum);
            normalized.putIfAbsent("response_timeout_ms", 3000);
            normalized.putIfAbsent("reconnect_delay_ms", 5000);
            normalized.putIfAbsent("offline_threshold_ms", 60000);
            normalized.putIfAbsent("command_prefix", "");
            normalized.putIfAbsent("command_terminator", "\\r");
            normalized.putIfAbsent("response_terminator", "\\r");
            normalized.putIfAbsent("strip_echo", true);
        }

        return normalized;
    }

    private static String normalizeProtocol(String protocol) {
        return protocol == null ? "" : protocol.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isModbus(String protocol) {
        return protocol.equals("MODBUS_TCP") || protocol.equals("MODBUSTCP") || protocol.equals("TCP")
                || protocol.equals("MODBUS_RTU") || protocol.equals("MODBUSRTU") || protocol.equals("RTU");
    }

    private static boolean isS7(String protocol) {
        return protocol.equals("SIEMENS_S7") || protocol.equals("SIEMENS")
                || protocol.equals("S7") || protocol.equals("S7_TCP");
    }

    private static boolean isSabus(String protocol) {
        return protocol.equals("SABUS") || protocol.equals("SABUS_ASCII");
    }

    private static boolean isSbus(String protocol) {
        return protocol.equals("SBUS") || protocol.equals("S-BUS") || protocol.equals("S_BUS")
                || protocol.equals("MODBUS_SBUS") || protocol.equals("MODBUSSBUS");
    }

    private static boolean isSerial(String protocol) {
        return protocol.equals("SERIAL") || protocol.equals("SERIAL_ASCII");
    }

    private static void validateSbusDataField(Map<String, Object> field) {
        String responseType = asText(field.getOrDefault("response_type", "UINT16"));
        Set<String> supportedTypes = Set.of(
                "INT16", "SHORT", "UINT16", "WORD",
                "INT32", "DINT", "UINT32", "DWORD",
                "INT64", "LINT", "LONG", "UINT64", "LWORD",
                "FLOAT32", "FLOAT", "REAL", "FLOAT64", "DOUBLE", "LREAL");
        if (responseType == null || !supportedTypes.contains(responseType.toUpperCase(Locale.ROOT))) {
            throw new ValidationException("Unsupported S-Bus response_type");
        }

        String byteOrder = asText(field.getOrDefault("byte_order", "BIG_ENDIAN"));
        String wordOrder = asText(field.getOrDefault("word_order", "BIG_ENDIAN"));
        Set<String> byteOrders = Set.of("BIG_ENDIAN", "LITTLE_ENDIAN", "BIG", "LITTLE", "BE", "LE",
                "ABCD", "BADC", "CDAB", "DCBA", "WORD_SWAP");
        Set<String> wordOrders = Set.of("BIG_ENDIAN", "LITTLE_ENDIAN", "BIG", "LITTLE", "BE", "LE",
                "HIGH_FIRST", "LOW_FIRST", "MSW_FIRST", "LSW_FIRST");
        if (byteOrder == null || !byteOrders.contains(byteOrder.toUpperCase(Locale.ROOT))) {
            throw new ValidationException("Unsupported S-Bus byte_order");
        }
        if (wordOrder == null || !wordOrders.contains(wordOrder.toUpperCase(Locale.ROOT))) {
            throw new ValidationException("Unsupported S-Bus word_order");
        }
        field.put("response_type", responseType.toUpperCase(Locale.ROOT));
        field.put("byte_order", byteOrder.toUpperCase(Locale.ROOT));
        field.put("word_order", wordOrder.toUpperCase(Locale.ROOT));
    }

    private static void validateSerialDataField(Map<String, Object> field) {
        String pattern = asText(field.get("response_pattern"));
        if (pattern != null) {
            try {
                if (Pattern.compile(pattern).matcher("").groupCount() < 1) {
                    throw new ValidationException("Serial response_pattern requires a capture group");
                }
            } catch (PatternSyntaxException exception) {
                throw new ValidationException("Serial response_pattern is invalid");
            }
        }

        Integer valueIndex = asInteger(field.get("value_index"));
        if (field.get("value_index") != null && (valueIndex == null || valueIndex < 0)) {
            throw new ValidationException("Serial value_index must be >= 0");
        }
        Integer start = asInteger(field.get("start_index"));
        Integer length = asInteger(field.get("length"));
        if (field.get("start_index") != null && (start == null || start < 0)) {
            throw new ValidationException("Serial start_index must be >= 0");
        }
        if (field.get("length") != null && (length == null || length <= 0)) {
            throw new ValidationException("Serial length must be > 0");
        }
        String valueType = asText(field.getOrDefault("value_type", field.getOrDefault("data_type", "NUMBER")));
        if (valueType == null || !Set.of("NUMBER", "DECIMAL", "TEXT", "STRING")
                .contains(valueType.toUpperCase(Locale.ROOT))) {
            throw new ValidationException("Serial value_type must be NUMBER or TEXT");
        }
        field.put("value_type", valueType.toUpperCase(Locale.ROOT));
    }

    private static void copyAlias(Map<String, Object> target, String canonical, String... aliases) {
        if (target.get(canonical) != null) {
            return;
        }
        for (String alias : aliases) {
            if (target.get(alias) != null) {
                target.put(canonical, target.get(alias));
                return;
            }
        }
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && (value.toString().equalsIgnoreCase("true")
                || value.toString().equals("1"));
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asText(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }
}
