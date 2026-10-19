package org.facenet.service.scale.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiemensS7EngineTest {

    @Test
    void createsPlc4xConnectionStringFromNormalizedSettings() {
        assertEquals(
                "s7://192.168.1.20:1102?remote-rack=0&remote-slot=2&read-timeout=5000"
                        + "&tcp.default-timeout=5000&controller-type=S7_300",
                SiemensS7Engine.buildConnectionString(Map.of(
                        "ip", "192.168.1.20",
                        "port", 1102,
                        "rack", 0,
                        "slot", 2,
                        "controller_type", "s7-300")));
    }
}
