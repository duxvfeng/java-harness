# Monitor Tool Guide & Session Learning Propagation

## Monitor 工具使用指南 (CC 2.1.98+)

监控长时间执行命令时，不要用 polling（Read 定期读取文件末尾），而是使用 **Monitor 工具**。Monitor 会将后台进程的 stdout 每行作为通知逐次传给 Lead，比 polling 更低延迟且更低 token 消耗地掌握状况。

**适用示例**:
- `go test ./... -v` 执行中的进度监控
- 通过 `gh run watch` 追踪 GitHub Actions 进度
- `npm run build --watch` / `vite build --watch` 的构建错误即时检测
- `docker-compose logs -f` / `kubectl logs -f` 的部署日志追踪

**使用区分的判断基准**:

| 对象 | 使用 Monitor? | 理由 |
|---|---|---|
| Agent (Worker / Reviewer) 的完成监控 | 不需要 | Agent 层自己进行完成通知 |
| 用 `run_in_background: true` 投递的 shell process | 推荐 | 可以逐次通知捡起 stdout 每行 |
| 短时间的一次性命令 (`go test` 执行 1 次) | 不需要 | 通常的 Bash tool 执行就够了 |
| 长时间 tail / watch / stream 系命令 | 推荐 | 比 polling 更高效 |

**Breezing Lead 中的典型模式**:

```
Lead:
  Task(Worker1, ...)           ← 等待 Agent 完成 (不需要 Monitor)
  Task(Worker2, ...)           ← 同上
  Bash(run_in_background, "gh run watch --exit-status")
  Monitor(tailCommand="...")   ← 即时检测 CI 失败 → 向 Worker 发出修正指示
```

这样可以提高 Lead 的「Worker 完成 → CI 失败检测 → 修正指示」反应速度。

## Universal Violations Injection（会话内 Worker 间的学习传播）

将同一 `/breezing` 启动内累积的 Reviewer 的 universal gotchas 自动注入到下一个 Worker 的 briefing 开头。**仅同一会话内有效**（会话结束时废弃，不写入 `session-memory`）。

```python
# Phase A 开始时初始化 Lead 进程的 in-memory 数组
universal_violations = []  # List[str] — 此会话内累积

# Phase B 中 spawn Worker 之前，注入到 briefing 开头:
def build_worker_briefing(task, contract_path):
    header = ""
    if universal_violations:
        header = (
            "🚨 同一会话中已检测出的 universal 违规（禁止再发）:\n"
            + "\n".join(f"- {v}" for v in universal_violations)
            + "\n\n"
        )
    return header + f"任务: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\nmode: breezing"

# Reviewer 返回 review-result.v1 后，Lead 仅抽出 scope="universal" 累积:
for update in reviewer_result.memory_updates:
    # 后方兼容: 字符串视为 task-specific → 忽略
    if isinstance(update, str):
        continue
    if update.get("scope") == "universal":
        universal_violations.append(update["text"])
```

**方针**: 为避免过度设计，不向 `session-memory` 或 `decisions.md` 持久化。仅保持在 Lead 进程的 in-memory 数组，`/breezing` 会话结束时废弃（issue #87 本文的方针）。
