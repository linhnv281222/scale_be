package org.facenet.service.scale.engine;

import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSerialComm;
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.MeasurementEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ModbusRtuEngineTest {

    @Test
    void registersJSerialCommFactory() {
        new ModbusRtuEngine(config(1L, 1, 9600),
                new ArrayBlockingQueue<MeasurementEvent>(10));

        assertInstanceOf(SerialPortFactoryJSerialComm.class,
                SerialUtils.getSerialPortFactory());
    }

    @Test
    void busKeyIgnoresUnitIdButIncludesPhysicalSerialSettings() {
        ScaleConfig unitOne = config(1L, 1, 9600);
        ScaleConfig unitTwo = config(2L, 2, 9600);
        ScaleConfig differentBaud = config(3L, 3, 19200);

        assertEquals(ModbusRtuEngine.busKey(unitOne), ModbusRtuEngine.busKey(unitTwo));
        assertNotEquals(ModbusRtuEngine.busKey(unitOne), ModbusRtuEngine.busKey(differentBaud));

        ModbusRtuEngine engine = new ModbusRtuEngine(
                java.util.List.of(unitOne, unitTwo),
                new ArrayBlockingQueue<MeasurementEvent>(10));
        assertEquals(java.util.Set.of(1L, 2L), engine.getScaleIds());
    }

    private ScaleConfig config(Long scaleId, int unitId, int baudRate) {
        return ScaleConfig.builder()
                .scaleId(scaleId)
                .protocol("MODBUS_RTU")
                .pollInterval(1000)
                .connParams(Map.of(
                        "com_port", "COM3",
                        "baud_rate", baudRate,
                        "data_bits", 8,
                        "stop_bits", 1,
                        "parity", "NONE",
                        "unit_id", unitId))
                .data1(Map.of(
                        "is_used", true,
                        "start_registers", 0,
                        "num_registers", 2))
                .build();
    }
}
