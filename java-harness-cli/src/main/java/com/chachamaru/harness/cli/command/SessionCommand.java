package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.handler.session.*;
import picocli.CommandLine.Command;

/**
 * Session management hub command.
 *
 * <p>When called without subcommands, displays help information.
 * Supports unified format: /harness-session &lt;subcommand&gt;</p>
 *
 * @author Java Harness Team
 * @since 2026-08-12
 */
@Command(name = "session",
         description = "Session management - shows help when called without arguments",
         subcommands = {
             SaveSessionCommand.class,
             RestoreSessionCommand.class,
             ListSessionsCommand.class,
             ShowSessionCommand.class,
             CleanupSessionsCommand.class
         })
public class SessionCommand implements Runnable {

    @Override
    public void run() {
        // When called without subcommands, display help
        printHelp();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("# Harness Session - 会话管理技能");
        System.out.println();
        System.out.println("💾 save — 保存会话");
        System.out.println("  用途: 保存当前会话状态到本地存储");
        System.out.println("  触发: `/harness-session save` 或 `/harness-session save \"摘要\"`");
        System.out.println("  参数: [summary] [--force]");
        System.out.println();
        System.out.println("📋 restore — 恢复会话");
        System.out.println("  用途: 从保存的会话中恢复工作状态");
        System.out.println("  触发: `/harness-session restore <saveId>`");
        System.out.println("  参数: <saveId> [--full] [--summary-only]");
        System.out.println();
        System.out.println("📑 list — 列出会话");
        System.out.println("  用途: 列出所有保存的会话");
        System.out.println("  触发: `/harness-session list`");
        System.out.println("  参数: [--recent N] [--all]");
        System.out.println();
        System.out.println("📄 show — 查看详情");
        System.out.println("  用途: 显示特定会话的详细信息");
        System.out.println("  触发: `/harness-session show <saveId>`");
        System.out.println("  参数: <saveId>");
        System.out.println();
        System.out.println("🧹 cleanup — 清理会话");
        System.out.println("  用途: 清理旧的会话保存文件");
        System.out.println("  触发: `/harness-session cleanup`");
        System.out.println("  参数: [--older-than HOURS] [--keep N] [--dry-run]");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("示例：");
        System.out.println("• 保存当前会话 → `/harness-session save \"完成Task 11.8\"`");
        System.out.println("• 查看会话列表 → `/harness-session list`");
        System.out.println("• 恢复会话 → `/harness-session restore 20260809-174530-abc123`");
        System.out.println("• 清理旧会话 → `/harness-session cleanup --dry-run`");
        System.out.println();
    }
}
