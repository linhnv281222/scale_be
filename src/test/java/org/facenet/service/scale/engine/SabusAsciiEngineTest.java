package org.facenet.service.scale.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SabusAsciiEngineTest {

    @Test
    void buildsFlowAndTotalCommandsWithConfiguredStationWidth() {
        Map<String, Object> connection = Map.of(
                "device_id", 7,
                "device_id_width", 2,
                "command_terminator", "\\r");

        assertEquals("!07FL\r", SabusAsciiEngine.buildCommand(connection, Map.of("command", "FL")));
        assertEquals("!07NT\r", SabusAsciiEngine.buildCommand(connection, Map.of("command", "NT")));
    }

    @Test
    void parsesCommonSabusResponseShapes() {
        assertEquals("123.45", SabusAsciiEngine.parseResponse(
                "!07FL=123.45 kg/h\r", "07", "FL", Map.of()));
        assertEquals("9876.5", SabusAsciiEngine.parseResponse(
                "!07NT:9876,5 kg\r", "07", "NT", Map.of()));
        assertEquals("42", SabusAsciiEngine.parseResponse(
                "!07 42\r", "07", "FL", Map.of()));
    }

    @Test
    void appliesConfiguredNumericTransformation() {
        assertEquals("12.35", SabusAsciiEngine.parseResponse(
                "FL=1234.5", "01", "FL", Map.of("divisor", 100, "decimal_places", 2)));
    }

    @Test
    void supportsManualResponsePatternAndRejectsDeviceErrors() {
        assertEquals("15.2", SabusAsciiEngine.parseResponse(
                "station=07;value=15.2;ok", "07", "FL",
                Map.of("response_pattern", "value=([+-]?[0-9.]+)")));
        assertThrows(IllegalArgumentException.class, () -> SabusAsciiEngine.parseResponse(
                "!07FL ERROR", "07", "FL", Map.of()));
    }
}
