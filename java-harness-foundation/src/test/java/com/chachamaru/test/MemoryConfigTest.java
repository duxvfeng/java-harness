package com.chachamaru.test;

/**
 * 验证 JVM 内存配置的简单测试
 * 运行: mvn exec:java -Dexec.mainClass="com.chachamaru.test.MemoryConfigTest" -Dexec.classpathScope=test -pl java-harness-foundation
 */
public class MemoryConfigTest {
    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();

        System.out.println("=== JVM 内存配置验证 ===");
        System.out.println("最大堆内存 (Max Heap): " + (runtime.maxMemory() / 1024 / 1024) + " MB");
        System.out.println("当前堆内存 (Total Heap): " + (runtime.totalMemory() / 1024 / 1024) + " MB");
        System.out.println("空闲堆内存 (Free Heap): " + (runtime.freeMemory() / 1024 / 1024) + " MB");
        System.out.println("已用堆内存 (Used Heap): " +
            ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024) + " MB");

        // 验证 -Xmx1G 是否生效（应该是约 1024 MB）
        long expectedMaxHeap = 1024; // 1GB
        long actualMaxHeap = runtime.maxMemory() / 1024 / 1024;

        if (actualMaxHeap <= expectedMaxHeap) {
            System.out.println("\n✅ 内存限制配置生效！最大堆内存: " + actualMaxHeap + " MB");
        } else {
            System.out.println("\n❌ 内存限制未生效！最大堆内存: " + actualMaxHeap + " MB (预期 <= " + expectedMaxHeap + " MB)");
        }
    }
}
