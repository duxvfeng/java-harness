package com.chachamaru.harness.handler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EvidenceHandlerTest {
    @Test
    void testEvidenceHandlerExecutes() {
        EvidenceHandler handler = new EvidenceHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{}));
    }

    @Test
    void testEvidenceCollect() {
        EvidenceHandler handler = new EvidenceHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"collect"}));
    }

    @Test
    void testEvidenceReport() {
        EvidenceHandler handler = new EvidenceHandler();
        assertDoesNotThrow(() -> handler.execute(new String[]{"report"}));
    }
}
