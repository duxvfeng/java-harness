# 长时间 Harness 会话指南

最后更新：2026-08-12

本文说明 Java Harness 长时间执行时的会话、缓存和状态管理边界。

## Java 版本现状

Java Harness 当前没有 Go 版本中的 `scripts/enable-1h-cache.sh`。不要在 Java 版本中执行该脚本，也不要假设存在 `env.local` 自动注入逻辑。

长时间任务应依赖现有的会话状态目录、`Plans.md` 和阶段性验证：

- 开始前检查 `git status --short` 和 `Plans.md`；
- 将任务进度写入项目已有的 `.claude/state/` 会话状态；
- 按阶段执行测试和 review，避免一次运行积累无法定位的变更；
- 发生中断时，先恢复 session state，再从 `Plans.md` 的状态继续；
- 不把 token、credential 或完整敏感 transcript 写入报告。

## 长任务结束标准

长时间运行不改变完成标准。最终仍需核对 `Plans.md`、`git diff`、测试结果和必要的 `harness doctor` / `harness validate` 输出。

## 跨版本提示

Go 版本中的缓存优化、脚本路径和环境变量不能直接视为 Java 版本能力。迁移这类优化前，应先在 Java CLI 中补充实现、测试和配置入口，再更新本文件。
