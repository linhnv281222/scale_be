package org.facenet.service.scale.engine;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerialEngineTest {

    @Test
    void buildsConfiguredCommand() {
        assertEquals("@W\r\n", SerialEngine.buildCommand(
                Map.of("command_prefix", "@", "command_terminator", "\\r\\n"),
                Map.of("command", "W")));
    }

    @Test
    void parsesNumericRegexAndAppliesTransformation() {
        assertEquals("12.35", SerialEngine.parseResponse(
                "ST,GS,+001234.5 kg\r",
                Map.of("response_pattern", "([+-]?[0-9.]+)\\s*kg", "divisor", 100,
                        "decimal_places", 2)));
    }

    @Test
    void parsesDelimitedAndFixedWidthResponses() {
        assertEquals("42.5", SerialEngine.parseResponse("OK,42.5,kg",
                Map.of("delimiter", ",", "value_index", 1)));
        assertEquals("00123", SerialEngine.parseResponse("WT=00123kg",
                Map.of("start_index", 3, "length", 5, "value_type", "TEXT")));
    }

    @Test
    void readsUntilMultiByteTerminator() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream("12.5 kg\r\nignored"
                .getBytes(StandardCharsets.US_ASCII));
        assertEquals("12.5 kg\r\n", SerialEngine.readFrame(input,
                Map.of("response_terminator", "\\r\\n")));
    }

    @Test
    void rejectsResponsesWithoutExpectedShape() {
        assertThrows(IllegalArgumentException.class, () -> SerialEngine.parseResponse(
                "NO DATA", Map.of("response_pattern", "value=([0-9.]+)")));
        assertThrows(IllegalArgumentException.class, () -> SerialEngine.parseResponse(
                "A,B", Map.of("delimiter", ",", "value_index", 2)));
    }
}
