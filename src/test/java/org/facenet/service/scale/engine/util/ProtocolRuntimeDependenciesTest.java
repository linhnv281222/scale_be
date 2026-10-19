package org.facenet.service.scale.engine.util;

import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSerialComm;
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ProtocolRuntimeDependenciesTest {

    @Test
    void preparesSerialProtocolDependencies() {
        assertDoesNotThrow(ProtocolRuntimeDependencies::prepareModbusRtu);
        assertInstanceOf(SerialPortFactoryJSerialComm.class, SerialUtils.getSerialPortFactory());
        assertDoesNotThrow(() -> ProtocolRuntimeDependencies.prepareDirectSerial("SABUS"));
    }

    @Test
    void discoversSiemensS7Driver() {
        assertDoesNotThrow(ProtocolRuntimeDependencies::prepareSiemensS7);
    }
}
