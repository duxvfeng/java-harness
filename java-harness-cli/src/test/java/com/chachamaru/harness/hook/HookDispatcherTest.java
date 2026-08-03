package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookDispatcherTest {
    @Test
    void testHookDispatcherExecutes() {
        HookDispatcher dispatcher = new HookDispatcher();
        assertDoesNotThrow(() -> dispatcher.execute(new String[]{"pre-tool"}));
    }
}
