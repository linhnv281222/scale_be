package org.facenet.service.scale.engine.util;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Converts Modbus registers according to the byte/word layout in a data-field config. */
public final class ModbusDataConverter {

    private ModbusDataConverter() {
    }

    public static String registersToString(int[] registers, Map<String, Object> config) {
        if (registers == null || registers.length == 0) return null;
        String dataType = text(config, "data_type", "auto").toUpperCase(Locale.ROOT);
        byte[] bytes = orderedBytes(registers, config);
        Object decoded = Set.of("INTEGER_DECIMAL", "INT_DECIMAL", "SPLIT_DECIMAL").contains(dataType)
                ? decodeIntegerDecimal(registers, config) : decode(registers, bytes, dataType);
        return DataValueTransformer.transform(decoded, config, registerVariables(registers));
    }

    /** Backward-compatible entry point used by older callers. */
    public static String registersToString(int[] registers, String dataType) {
        return registersToString(registers, Map.of("data_type", dataType == null ? "auto" : dataType));
    }

    public static String registersToString(int[] registers) {
        return registersToString(registers, Map.of("data_type", "auto"));
    }

    private static Object decode(int[] registers, byte[] bytes, String dataType) {
        return switch (dataType) {
            case "BOOL", "BOOLEAN" -> registers[0] != 0;
            case "INT16", "SHORT" -> require(bytes, 2).getShort();
            case "UINT16", "WORD" -> Short.toUnsignedInt(require(bytes, 2).getShort());
            case "INT", "INTEGER" -> bytes.length >= 4
                    ? require(bytes, 4).getInt() : Short.toUnsignedInt(require(bytes, 2).getShort());
            case "INT32", "DINT" -> require(bytes, 4).getInt();
            case "UINT32", "DWORD" -> Integer.toUnsignedLong(require(bytes, 4).getInt());
            case "INT64", "LINT", "LONG" -> require(bytes, 8).getLong();
            case "UINT64", "LWORD" -> new BigInteger(1, Arrays.copyOf(bytes, 8));
            case "FLOAT", "FLOAT32", "REAL" -> require(bytes, 4).getFloat();
            case "FLOAT64", "DOUBLE", "LREAL" -> require(bytes, 8).getDouble();
            case "STRING", "TEXT", "ASCII" -> decodeString(bytes);
            case "AUTO" -> decodeAuto(registers, bytes);
            default -> throw new IllegalArgumentException("Unsupported Modbus data_type: " + dataType);
        };
    }

    private static Object decodeAuto(int[] registers, byte[] bytes) {
        if (registers.length == 1) return registers[0] & 0xFFFF;
        if (registers.length == 2) return require(bytes, 4).getFloat();
        return Arrays.toString(registers);
    }

    private static BigDecimal decodeIntegerDecimal(int[] registers, Map<String, Object> config) {
        int integerIndex = integer(config, "integer_register_index", 0);
        int decimalIndex = integer(config, "decimal_register_index", 1);
        if (integerIndex < 0 || decimalIndex < 0 || integerIndex >= registers.length || decimalIndex >= registers.length) {
            throw new IllegalArgumentException("INTEGER_DECIMAL register index is outside the returned register block");
        }
        int integerPart = (short) registers[integerIndex];
        int decimalPart = registers[decimalIndex] & 0xFFFF;
        int decimalDigits = integer(config, "decimal_digits", Math.max(1, Integer.toString(decimalPart).length()));
        if (decimalDigits < 1 || decimalDigits > 12) {
            throw new IllegalArgumentException("decimal_digits must be between 1 and 12");
        }
        BigDecimal fraction = BigDecimal.valueOf(decimalPart).movePointLeft(decimalDigits);
        return BigDecimal.valueOf(integerPart).add(integerPart < 0 ? fraction.negate() : fraction);
    }

    private static Map<String, BigDecimal> registerVariables(int[] registers) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (int index = 0; index < registers.length; index++) {
            values.put("R" + index, BigDecimal.valueOf(registers[index] & 0xFFFFL));
        }
        return values;
    }

    private static byte[] orderedBytes(int[] registers, Map<String, Object> config) {
        String byteOrder = text(config, "byte_order", "BIG_ENDIAN").toUpperCase(Locale.ROOT);
        String wordOrder = text(config, "word_order", "BIG_ENDIAN").toUpperCase(Locale.ROOT);

        // Common four-byte aliases: ABCD, BADC, CDAB and DCBA.
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

        int[] words = Arrays.copyOf(registers, registers.length);
        if (isLittle(wordOrder)) {
            for (int left = 0, right = words.length - 1; left < right; left++, right--) {
                int swap = words[left];
                words[left] = words[right];
                words[right] = swap;
            }
        }

        byte[] bytes = new byte[words.length * 2];
        boolean littleBytes = isLittle(byteOrder);
        for (int index = 0; index < words.length; index++) {
            int word = words[index] & 0xFFFF;
            bytes[index * 2] = (byte) (littleBytes ? word : word >>> 8);
            bytes[index * 2 + 1] = (byte) (littleBytes ? word >>> 8 : word);
        }
        return bytes;
    }

    private static boolean isLittle(String value) {
        return value.equals("LITTLE_ENDIAN") || value.equals("LITTLE") || value.equals("LE")
                || value.equals("LOW_FIRST") || value.equals("LSW_FIRST");
    }

    private static ByteBuffer require(byte[] bytes, int length) {
        if (bytes.length < length) {
            throw new IllegalArgumentException("Data type requires " + length + " bytes, got " + bytes.length);
        }
        return ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.BIG_ENDIAN);
    }

    private static String decodeString(byte[] bytes) {
        int length = bytes.length;
        while (length > 0 && bytes[length - 1] == 0) length--;
        return new String(bytes, 0, length, StandardCharsets.UTF_8).trim();
    }

    private static String text(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || config.get(key) == null || config.get(key).toString().isBlank()) return defaultValue;
        return config.get(key).toString().trim();
    }

    private static int integer(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || config.get(key) == null) return defaultValue;
        return Integer.parseInt(config.get(key).toString());
    }

    public static Float registersToFloatBE(int[] registers) {
        if (registers == null || registers.length != 2) return null;
        return (Float) decode(registers, orderedBytes(registers, Map.of()), "FLOAT32");
    }

    public static Float registersToFloatLE(int[] registers) {
        if (registers == null || registers.length != 2) return null;
        return (Float) decode(registers,
                orderedBytes(registers, Map.of("word_order", "LITTLE_ENDIAN")), "FLOAT32");
    }

    public static Integer registerToInt(int register) {
        return register & 0xFFFF;
    }

    public static Integer registersToInt32BE(int[] registers) {
        if (registers == null || registers.length != 2) return null;
        return (Integer) decode(registers, orderedBytes(registers, Map.of()), "INT32");
    }
}
