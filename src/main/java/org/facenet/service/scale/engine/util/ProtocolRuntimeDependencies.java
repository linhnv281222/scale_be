package org.facenet.service.scale.engine.util;

import com.fazecast.jSerialComm.SerialPort;
import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSerialComm;
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils;
import org.apache.plc4x.java.api.PlcDriverManager;

import java.util.Locale;
import java.util.Set;

/**
 * Performs synchronous checks for optional protocol adapters before an engine
 * is submitted to its worker thread. This prevents an engine from being logged
 * as started and then immediately dying because a runtime driver is missing.
 */
public final class ProtocolRuntimeDependencies {

    private ProtocolRuntimeDependencies() {
    }

    public static synchronized void prepareModbusRtu() {
        verifyJSerialComm("Modbus RTU");
        try {
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
        } catch (RuntimeException | LinkageError exception) {
            throw unavailable("Modbus RTU", "jlibmodbus jSerialComm adapter", exception);
        }
        if (!(SerialUtils.getSerialPortFactory() instanceof SerialPortFactoryJSerialComm)) {
            throw unavailable("Modbus RTU", "jlibmodbus jSerialComm adapter", null);
        }
    }

    public static void prepareDirectSerial(String protocolName) {
        verifyJSerialComm(protocolName);
    }

    public static void prepareSiemensS7() {
        try {
            Set<String> protocols = PlcDriverManager.getDefault().getProtocolCodes();
            boolean s7Available = protocols.stream()
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch("s7"::equals);
            if (!s7Available) {
                throw unavailable("Siemens S7", "Apache PLC4X S7 driver", null);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (RuntimeException | LinkageError exception) {
            throw unavailable("Siemens S7", "Apache PLC4X S7 driver", exception);
        }
    }

    private static void verifyJSerialComm(String protocolName) {
        try {
            // Enumerating does not open a port, but it verifies that both the Java
            // dependency and the platform-specific native library can be loaded.
            SerialPort.getCommPorts();
        } catch (RuntimeException | LinkageError exception) {
            throw unavailable(protocolName, "jSerialComm runtime", exception);
        }
    }

    private static IllegalStateException unavailable(String protocolName, String dependency, Throwable cause) {
        String message = protocolName + " runtime dependency is unavailable: " + dependency;
        return cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
    }
}
