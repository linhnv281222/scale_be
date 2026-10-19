package org.facenet.service.scale.engine.util;

import org.facenet.common.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaleConfigSchemaTest {

    @Test
    void normalizesLegacyRegisterAliases() {
        Map<String, Object> normalized = ScaleConfigSchema.normalizeDataField(Map.of(
                "used", true,
                "start_register", "12",
                "register_count", "2"));

        assertEquals(true, normalized.get("is_used"));
        assertEquals(12, normalized.get("start_registers"));
        assertEquals(2, normalized.get("num_registers"));
        assertEquals(3, normalized.get("function_code"));
    }

    @Test
    void rejectsEnabledFieldWithoutRegisterConfiguration() {
        assertThrows(ValidationException.class,
                () -> ScaleConfigSchema.normalizeDataField(Map.of("is_used", true)));
    }

    @Test
    void normalizesRtuConnectionAndAddsOperationalDefaults() {
        Map<String, Object> normalized = ScaleConfigSchema.normalizeConnection("MODBUS_RTU", Map.of(
                "com_port", "COM3",
                "baud_rate", "9600",
                "device_id", 7));

        assertEquals(7, normalized.get("unit_id"));
        assertEquals(3000, normalized.get("response_timeout_ms"));
        assertEquals("NONE", normalized.get("parity"));
    }

    @Test
    void rejectsRtuConnectionWithoutUnitId() {
        assertThrows(ValidationException.class, () ->
                ScaleConfigSchema.normalizeConnection("MODBUS_RTU", Map.of(
                        "com_port", "COM3",
                        "baud_rate", 9600)));
    }

    @Test
    void normalizesSiemensConnectionAndTagAliases() {
        Map<String, Object> connection = ScaleConfigSchema.normalizeConnection("SIEMENS_S7", Map.of(
                "host", "192.168.1.20",
                "remote_rack", "0",
                "remote_slot", "2"));
        Map<String, Object> field = ScaleConfigSchema.normalizeDataField("SIEMENS_S7", Map.of(
                "used", true,
                "address", "%DB1:0:REAL"));

        assertEquals("192.168.1.20", connection.get("ip"));
        assertEquals(0, connection.get("rack"));
        assertEquals(2, connection.get("slot"));
        assertEquals(102, connection.get("port"));
        assertEquals("%DB1:0:REAL", field.get("tag_address"));
    }

    @Test
    void rejectsEnabledSiemensFieldWithoutTagAddress() {
        assertThrows(ValidationException.class,
                () -> ScaleConfigSchema.normalizeDataField("S7", Map.of("is_used", true)));
    }

    @Test
    void rejectsUnsupportedModbusFunctionCode() {
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeDataField(Map.of(
                "is_used", true,
                "start_registers", 0,
                "num_registers", 1,
                "function_code", 6)));
    }

    @Test
    void normalizesSabusConnectionAndCommands() {
        Map<String, Object> connection = ScaleConfigSchema.normalizeConnection("SABUS", Map.of(
                "com_port", "COM4", "baud_rate", "9600", "station_id", 7));
        Map<String, Object> flow = ScaleConfigSchema.normalizeDataField("SABUS", Map.of(
                "used", true, "command", "fl"));

        assertEquals("7", connection.get("device_id"));
        assertEquals(2, connection.get("device_id_width"));
        assertEquals(3000, connection.get("response_timeout_ms"));
        assertEquals("FL", flow.get("command"));
    }

    @Test
    void rejectsInvalidSabusConfiguration() {
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeConnection(
                "SABUS", Map.of("com_port", "COM4", "baud_rate", 9600)));
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeDataField(
                "SABUS", Map.of("is_used", true, "command", "WEIGHT")));
    }

    @Test
    void normalizesGenericSerialConfiguration() {
        Map<String, Object> connection = ScaleConfigSchema.normalizeConnection("SERIAL", Map.of(
                "serial_port", "COM5", "baud_rate", "19200", "parity", "even"));
        Map<String, Object> field = ScaleConfigSchema.normalizeDataField("SERIAL", Map.of(
                "used", true, "request", "W", "regex", "weight=([0-9.]+)"));

        assertEquals("COM5", connection.get("com_port"));
        assertEquals(19200, connection.get("baud_rate"));
        assertEquals("EVEN", connection.get("parity"));
        assertEquals("W", field.get("command"));
        assertEquals("weight=([0-9.]+)", field.get("response_pattern"));
        assertEquals("NUMBER", field.get("value_type"));
    }

    @Test
    void rejectsInvalidGenericSerialConfiguration() {
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeConnection(
                "SERIAL", Map.of("com_port", "COM5", "baud_rate", 0)));
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeDataField(
                "SERIAL", Map.of("is_used", true, "response_pattern", "no-capture-group")));
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeDataField(
                "SERIAL", Map.of("is_used", true, "value_index", -1)));
    }

    @Test
    void normalizesSbusWordAndByteOrder() {
        Map<String, Object> normalized = ScaleConfigSchema.normalizeDataField("SBUS", Map.of(
                "is_used", true,
                "response_type", "float32",
                "byte_order", "CDAB"));

        assertEquals("FLOAT32", normalized.get("response_type"));
        assertEquals("CDAB", normalized.get("byte_order"));
        assertEquals("BIG_ENDIAN", normalized.get("word_order"));
    }

    @Test
    void rejectsInvalidSbusWordOrder() {
        assertThrows(ValidationException.class, () -> ScaleConfigSchema.normalizeDataField(
                "SBUS", Map.of("is_used", true, "response_type", "FLOAT32", "word_order", "REVERSE")));
    }
}
