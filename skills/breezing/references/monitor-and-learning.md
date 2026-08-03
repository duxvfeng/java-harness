# Monitor Tool Guide & Session Learning Propagation

## Monitor 工具使用指南 (CC 2.1.98+)

监控长时间运行命令时，不使用轮询（Read 定期读取文件末尾），而是使用 **Monitor 工具**。Monitor 将后台进程的 stdout 每行逐次通知 Lead，因此比轮询更低延迟且更低令牌消耗地掌握状况。

**适用示例**:
- `go test ./... -v` 执行中进度监控
- `gh run watch` 跟踪 GitHub Actions 进度
- `npm run build --watch` / `vite build --watch` 的构建错误即时检测
- `codex-companion.sh status <job-id>` 检测 Codex job 完成
- `docker-compose logs -f` / `kubectl logs -f` 的部署日志跟踪

**使用判断标准**:

| 对象 | 使用 Monitor? | 理由 |
|---|---|---|
| Agent (Worker / Reviewer) 的完成监控 | 不需要 | Agent 层自行完成通知 |
| `run_in_background: true` 投放的 shell process | 推荐 | 可逐次通知拾取 stdout 每行 |
| 短时间单发命令 (`go test` 1 次执行) | 不需要 | 通常 Bash tool 执行足够 |
| 長時間 tail / watch / stream 系コマンド | 推奨 | polling より効率的 |

**Breezing Lead での典型パターン**:

```
Lead:
  Task(Worker1, ...)           ← Agent 完了待ち (Monitor 不要)
  Task(Worker2, ...)           ← 同上
  Bash(run_in_background, "gh run watch --exit-status")
  Monitor(tailCommand="...")   ← CI 失敗を即時検知 → Worker に修正指示
```

これにより Lead が「Worker 完了 → CI 失敗検知 → 修正指示」の反応速度を上げられる。

## Universal Violations Injection（セッション内 Worker 間の学習伝播）

同一 `/breezing` 起動内で蓄積された Reviewer の universal gotchas を次 Worker の briefing 冒頭に自動注入する。**同一セッション内のみ有効**（セッション終了で破棄、`session-memory` には書かない）。

```python
# Phase A 開始時に Lead プロセスの in-memory 配列を初期化
universal_violations = []  # List[str] — このセッション内で蓄積

# Phase B で Worker を spawn する直前、briefing 冒頭に注入:
def build_worker_briefing(task, contract_path):
    header = ""
    if universal_violations:
        header = (
            "🚨 同一セッションで既に検出された universal 違反（再発禁止）:\n"
            + "\n".join(f"- {v}" for v in universal_violations)
            + "\n\n"
        )
    return header + f"タスク: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\nmode: breezing"

# Reviewer が review-result.v1 を返した後、Lead が scope="universal" のみ抽出して累積:
for update in reviewer_result.memory_updates:
    # 後方互換: 文字列は task-specific 扱い → 無視
    if isinstance(update, str):
        continue
    if update.get("scope") == "universal":
        universal_violations.append(update["text"])
```

**方針**: 過剰設計回避のため、`session-memory` や `decisions.md` への永続化は行わない。Lead プロセスの in-memory 配列に保持するだけで、`/breezing` セッション終了時に破棄する（issue #87 本文の方針）。
