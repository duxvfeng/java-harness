package com.chachamaru.harness;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    private int exitCode = -1;

    @BeforeEach
    void setUp() {
        // 设置测试用的退出处理器，不真正调用 System.exit
        Main.setExitHandlerForTesting(code -> exitCode = code);
    }

    @AfterEach
    void tearDown() {
        // 恢复默认退出处理器
        Main.setExitHandlerForTesting(code -> System.exit(code));
    }

    @Test
    void testExecuteWithValidCommand() {
        int result = Main.execute(new String[]{"plan"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithVersionFlag() {
        int result = Main.execute(new String[]{"--version"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithShortVersionFlag() {
        int result = Main.execute(new String[]{"-v"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithHelpFlag() {
        int result = Main.execute(new String[]{"help"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithLongHelpFlag() {
        int result = Main.execute(new String[]{"--help"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithShortHelpFlag() {
        int result = Main.execute(new String[]{"-h"});
        assertEquals(0, result);
    }

    @Test
    void testExecuteWithEmptyArgs() {
        int result = Main.execute(new String[]{});
        assertEquals(1, result);
    }

    @Test
    void testExecuteWithUnknownCommand() {
        int result = Main.execute(new String[]{"unknown-command"});
        assertEquals(1, result);
    }

    @Test
    void testMainEntryPointWithVersion() {
        // 验证 main 方法不会抛出异常
        assertDoesNotThrow(() -> Main.main(new String[]{"--version"}));
        assertEquals(0, exitCode);
    }

    @Test
    void testMainEntryPointWithValidCommand() {
        // 验证 main 方法不会抛出异常
        assertDoesNotThrow(() -> Main.main(new String[]{"plan"}));
        assertEquals(0, exitCode);
    }

    @Test
    void testMainEntryPointWithEmptyArgs() {
        // 验证 main 方法不会抛出异常
        assertDoesNotThrow(() -> Main.main(new String[]{}));
        assertEquals(1, exitCode);
    }

    @Test
    void testMainEntryPointWithUnknownCommand() {
        // 验证 main 方法不会抛出异常
        assertDoesNotThrow(() -> Main.main(new String[]{"unknown-command"}));
        assertEquals(1, exitCode);
    }
}
