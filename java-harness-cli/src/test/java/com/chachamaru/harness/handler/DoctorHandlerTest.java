package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DoctorHandlerTest {
    @Test
    void testDoctorHandlerExecutes() {
        DoctorHandler handler = new DoctorHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testDoctorWithMigrationFlag() {
        DoctorHandler handler = new DoctorHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"--migration"}));
    }

    @Test
    void testDoctorWithCustomRoot() {
        DoctorHandler handler = new DoctorHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"/custom/root"}));
    }
}
