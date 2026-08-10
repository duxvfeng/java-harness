package com.chachamaru;

public class MemoryCheck {
    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        System.out.println("=====================================");
        System.out.println("JVM Memory Configuration Check");
        System.out.println("=====================================");
        System.out.println("Max Heap Memory (-Xmx):   " + (maxMemory / 1024 / 1024) + " MB");
        System.out.println("Total Heap Memory (-Xms): " + (totalMemory / 1024 / 1024) + " MB");
        System.out.println("Free Heap Memory:         " + (freeMemory / 1024 / 1024) + " MB");
        System.out.println("=====================================");

        // Check if 1GB limit is successfully applied
        if (maxMemory <= 1024 * 1024 * 1024) {
            System.out.println("[OK] Max heap is <= 1GB (limit applied)");
        } else {
            System.out.println("[FAIL] Max heap is " + (maxMemory / 1024 / 1024) + " MB (limit NOT applied)");
        }
    }
}
