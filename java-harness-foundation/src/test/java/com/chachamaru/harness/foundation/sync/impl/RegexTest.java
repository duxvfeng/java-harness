package com.chachamaru.harness.foundation.sync.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的正则表达式测试
 */
public class RegexTest {
    public static void main(String[] args) {
        String testLine = "| 8.4.1 | 实现状态持久化引擎 | 支持 JSON/YAML | 8.3.4 | cc:completed ✅ |";

        // 测试不同的模式
        Pattern pattern1 = Pattern.compile(
            "^\\|\\s*(\\d+\\.\\d+\\.\\d+)\\s*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|\\s*([^\\|]*?)\\s*\\|$"
        );

        Pattern pattern2 = Pattern.compile(
            "\\|\\s*(\\d+\\.\\d+\\.\\d+)\\s*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|\\s*([^\\|]*?)\\s*\\|"
        );

        Pattern pattern3 = Pattern.compile(
            "\\|[^\\|]*\\|"
        );

        System.out.println("Testing line: " + testLine);
        System.out.println();

        System.out.println("Pattern 1 (with ^ and $):");
        Matcher matcher1 = pattern1.matcher(testLine);
        System.out.println("Matches: " + matcher1.find());
        if (matcher1.find()) {
            System.out.println("Task ID: " + matcher1.group(1));
            System.out.println("Status: " + matcher1.group(2));
        }

        System.out.println();
        System.out.println("Pattern 2 (without ^ and $):");
        Matcher matcher2 = pattern2.matcher(testLine);
        System.out.println("Matches: " + matcher2.find());
        if (matcher2.find()) {
            System.out.println("Task ID: " + matcher2.group(1));
            System.out.println("Status: " + matcher2.group(2));
        }

        System.out.println();
        System.out.println("Pattern 3 (simple):");
        Matcher matcher3 = pattern3.matcher(testLine);
        int count = 0;
        while (matcher3.find()) {
            count++;
            System.out.println("Match " + count + ": " + matcher3.group());
        }
    }
}
