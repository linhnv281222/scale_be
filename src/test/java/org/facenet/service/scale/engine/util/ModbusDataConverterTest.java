package org.facenet.service.scale.engine.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModbusDataConverterTest {

    @Test
    void decodesFloatWithBigEndianWords() {
        assertEquals("1", ModbusDataConverter.registersToString(
                new int[]{0x3f80, 0x0000}, Map.of("data_type", "float32")));
    }

    @Test
    void decodesFloatWithSwappedWords() {
        assertEquals("1", ModbusDataConverter.registersToString(
                new int[]{0x0000, 0x3f80}, Map.of(
                        "data_type", "float32",
                        "word_order", "little_endian")));
    }

    @Test
    void appliesFormulaAndRounding() {
        assertEquals("12.50", ModbusDataConverter.registersToString(
                new int[]{100}, Map.of(
                        "data_type", "uint16",
                        "formula", "(value / 10) + 2.5",
                        "decimal_places", 2)));
    }

    @Test
    void rejectsUnsupportedFormulaVariables() {
        assertThrows(IllegalArgumentException.class, () ->
                ModbusDataConverter.registersToString(new int[]{1}, Map.of(
                        "data_type", "uint16", "formula", "weight * 2")));
    }

    @Test
    void formulaCanReferenceIndividualRegisters() {
        assertEquals("6553.602", ModbusDataConverter.registersToString(
                new int[]{100, 2}, Map.of(
                        "data_type", "uint32",
                        "formula", "((R0 * 65536) + R1) / 1000")));
    }

    @Test
    void combinesSeparateIntegerAndDecimalRegisters() {
        assertEquals("12.034", ModbusDataConverter.registersToString(
                new int[]{12, 34}, Map.of(
                        "data_type", "integer_decimal",
                        "decimal_digits", 3)));
    }
}
