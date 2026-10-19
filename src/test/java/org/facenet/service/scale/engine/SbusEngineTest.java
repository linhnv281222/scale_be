package org.facenet.service.scale.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SbusEngineTest {

    @Test
    void decodesThirtyTwoBitValuesWithBothWordOrders() {
        assertEquals("305419896", SbusEngine.decodeResponse(
                bytes(0x12, 0x34, 0x56, 0x78),
                Map.of("response_type", "UINT32")));

        assertEquals("305419896", SbusEngine.decodeResponse(
                bytes(0x56, 0x78, 0x12, 0x34),
                Map.of("response_type", "UINT32", "word_order", "LITTLE_ENDIAN")));
    }

    @Test
    void supportsAbcdBadcCdabAndDcbaLayouts() {
        byte[] expected = bytes(0x12, 0x34, 0x56, 0x78);
        assertArrayEquals(expected, SbusEngine.orderedBytes(expected, Map.of("byte_order", "ABCD")));
        assertArrayEquals(expected, SbusEngine.orderedBytes(
                bytes(0x34, 0x12, 0x78, 0x56), Map.of("byte_order", "BADC")));
        assertArrayEquals(expected, SbusEngine.orderedBytes(
                bytes(0x56, 0x78, 0x12, 0x34), Map.of("byte_order", "CDAB")));
        assertArrayEquals(expected, SbusEngine.orderedBytes(
                bytes(0x78, 0x56, 0x34, 0x12), Map.of("byte_order", "DCBA")));
    }

    @Test
    void decodesWordSwappedFloatFromAsciiAndBinaryFrames() {
        Map<String, Object> config = Map.of(
                "response_type", "FLOAT32", "word_order", "LITTLE_ENDIAN");
        assertEquals("1.0", SbusEngine.parseHexData("00003F80", config));
        assertEquals("1.0", SbusEngine.parseBinaryData(
                bytes(0x02, 0x01, 0x57, 0x00, 0x00, 0x3F, 0x80, 0x00, 0x00), 3, 4, config));
    }

    @Test
    void rejectsIncompleteWords() {
        assertThrows(IllegalArgumentException.class, () -> SbusEngine.orderedBytes(
                bytes(0x01, 0x02, 0x03), Map.of()));
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) result[index] = (byte) values[index];
        return result;
    }
}
