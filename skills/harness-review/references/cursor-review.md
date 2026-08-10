# Cursor Review (--cursor) — second-opinion only

让 cursor (composer-2.5-fast) 作为 harness-review 的 **second-opinion** 并行的 lean 模式。
`--cursor` 显式时，或 resolver 返回 `cursor` 时（例: `HARNESS_IMPL_BACKEND=cursor` / user-scope default ON）同等触发。

## 不变规则

- **cursor 不升格为 primary reviewer**。Opus reviewer 始终并行，primary verdict 从 Opus 获取。cursor 输出作为 advisory 存储在 `dual_review.cursor_verdict` 中。
- 理由: `harness-work` 的"实现的后端不得审查自己的输出"不变则（避免用 cursor backend 写的代码让 cursor backend 审查的配置）。
- cursor 为 read-only delegate，因此 worktree 隔离 / Lead diff review / cherry-pick / `worker-report.v1` **不需要**。

## 委托前 mandatory banner

启动 cursor delegate 前，必须输出以下 literal 1 行：

```
⚠️ cursor review (read-only): model=composer-2.5-fast / R01-R13 不适用于 cursor-agent 内部 / 输出到 Lead 评定前 untrusted
```

## 委托命令（read-only、无需 workspace）

```bash
HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"
if [ -z "$HARNESS_PLUGIN_ROOT" ] && [ -n "${CLAUDE_SKILL_DIR:-}" ]; then
  probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"
  while [ "$probe" != "/" ] && [ ! -d "$probe/scripts" ]; do
    probe="$(cd "$probe/.." && pwd)"
  done
  [ -d "$probe/scripts" ] && HARNESS_PLUGIN_ROOT="$probe"
fi
```

- 也**不要加** `--workspace`（read mode 时 companion guard 不触发为 optional，不需要）
- 也不要加 `--force` / `--yolo`（Cursor 官方 "Never use"）

review prompt 的构成例：

```
diff 审查（base_ref={BASE_REF}, head=HEAD）：

<git diff 要点 or branch range>

观点:
- 规格逸脱 / 范围外变更
- 现有测试 regression 风险
- secret / 认证信息混入
- 对 protected path（settings*, .eslintrc*, tsconfig*.json）的变更

verdict 返回 APPROVE / REQUEST_CHANGES / NEEDS_INFO 之一。
```

## Trust boundary（read mode 也必须保持）

| 项目 | 内容 | 设置位置 |
|---|---|---|
| Secret 遮断 | `.cursorignore` 中排除 `.env` / `*.pem` / `*.key` / `.ssh` / `.aws` / `.git` | repo root |
| Egress allowlist | `~/.claude/settings.json` 的 `sandbox.network.allowedDomains` 中添加 `*.cursor.sh` | user settings |
| Filesystem allowlist | 同 `sandbox.filesystem.allowWrite` 中添加 `~/.cursor` | user settings |
| permissions.json | `~/.cursor/permissions.json` 的 `terminalAllowlist` / `mcpAllowlist`（best-effort、非 security boundary） | user config |

cursor 官方明言"Allowlists are best-effort convenience. They are not a security guarantee."。这 4 点即使在 **read mode 也必须保持**，但不要过度依赖。实效边界为 Lead 判定。

## Verdict 映射

将 cursor 输出按以下 schema 扩展存储在 `dual_review` 中（参考 `references/dual-review.md`）：

```json
{
  "claude_verdict": "APPROVE | REQUEST_CHANGES | NEEDS_INFO",
  "codex_verdict": "approve | needs-attention | unavailable | timeout",
  "cursor_verdict": "APPROVE | REQUEST_CHANGES | NEEDS_INFO | unavailable | timeout",
  "cursor_divergence_notes": "string?"
}
```

- `cursor_verdict` 为 **optional field**。仅在指定 `--dual` / `--cursor` 时添加
- `cursor_divergence_notes`: Claude/Codex/Cursor 的 verdict 分岐时由 Lead 填入
- 现有 consumer（HTML render / harness-accept 等）作为 optional 处理以不破坏 parser

## Verdict 整合规则

优先采用 primary verdict（Opus reviewer）。cursor / codex 为 **advisory**：

| Opus | Codex | Cursor | 最终 verdict |
|---|---|---|---|
| APPROVE | approve | APPROVE | APPROVE（3 者一致、最高可信） |
| APPROVE | approve | REQUEST_CHANGES | APPROVE + cursor_divergence_notes（Opus 优先、cursor 的指出作为下次 PR 的改善点记录） |
| REQUEST_CHANGES | * | * | REQUEST_CHANGES（Opus 为 REQUEST 时立即 REQUEST） |
| APPROVE | needs-attention | * | 执行 TeamAgent Debate (`--team-debate`) |

## 不可逆保证

对于来自 cursor 的 suggested edit，**在实代码中确认后决定采否**（与 `codex-closeout.md` 的 Advisory rule 相同契约）。即使 cursor 说"应删除此行"，Lead 也要确认 diff 的上下文和影响范围后再判断。不让 cursor 单独触发 commit / push。

## Related

- `.claude/rules/cursor-cli-only.md` — Cursor backend governance + Read mode delegation
- `references/dual-review.md` — dual / triple review 的合格线整合
- `references/governance.md` — review 整体的合格线
- `skills/cursor-ask/SKILL.md` — read-only delegate 的通用版（审查以外的提问・调查）
