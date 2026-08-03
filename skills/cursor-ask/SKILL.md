---
name: cursor-ask
description: "Read-only delegate to cursor-agent (Composer) for questions, investigation, design discussion, and adversarial sanity checks. No worktree, no cherry-pick, no Lead diff review — cursor-agent is locked to ask mode and cannot write. Use when user says: ask cursor, cursor sanity check, get a second opinion, adversarial review, design discussion, investigate with cursor, cursor:ask. Do NOT load for: implementation, refactor, file edits, commit/push work, anything requiring write access (use cursor:do or breezing --cursor instead)."
description-en: "Read-only delegate to cursor-agent (Composer) for questions, investigation, design discussion, and adversarial sanity checks. No worktree, no cherry-pick, no Lead diff review — cursor-agent is locked to ask mode and cannot write. Use when user says: ask cursor, cursor sanity check, get a second opinion, adversarial review, design discussion, investigate with cursor, cursor:ask. Do NOT load for: implementation, refactor, file edits, commit/push work, anything requiring write access (use cursor:do or breezing --cursor instead)."
description-zh: "向 cursor-agent (Composer) 的只读委托。用于提问、调查、设计讨论和对抗性检查（sanity check）。无需 worktree、cherry-pick 或 Lead diff review。cursor 锁定为 ask 模式，不可写入。当用户说：问 cursor、cursor 检查、获取第二意见、对抗性审查、设计讨论、用 cursor 调查、cursor:ask 时使用。不适用于：实现、重构、文件编辑、提交/推送工作、需要写入权限的操作（请改用 cursor:do 或 breezing --cursor）。"
allowed-tools: ["Read", "Bash"]
argument-hint: "[question]"
user-invocable: true
---

# cursor:ask — Read-Only Cursor Delegate

向 cursor-agent (Composer) **只读**委托提问・调查・设计咨询・对抗性审查的轻量技能。

`cursor-companion.sh task` 无参数时自动附加 **`--mode ask` (hard read-only stop)**，因此不传递 `--write` 的情况下 cursor 侧 **无法文件写入・命令执行**。从而 worktree 隔离・cherry-pick・Lead diff review 全部不需要。

## Quick Reference

```bash
cursor:ask "这个设计判断，Composer 视点怎么看？"
cursor:ask "读取从 TASK_BASE_REF 的 diff，列举 3 个遗漏"
cursor:ask "harness-mem 的 cross-project N-call，是否有过乐观的前提？"
```

用途:

| 案例 | 例 |
|---|---|
| 提问 | "这个类型错误的根本原因是？" |
| 调查 | "列举 scripts/ 下使用 curl 的全部位置并附理由" |
| 设计咨询 | "这个 abstraction，3 年后能维护吗？" |
| 对抗视点 | "仅列举这个 PR 的最大弱点 1 个" |

## Narration Rules (UX Contract)

敌人是**冗长**而非进度报告。**启动时简洁明了地显示要问什么・如何进行，然后执行**。仅禁止冗长重复・无内容前言。

### 启动时必须输出 (banner + plan、3 行以内)

```
🚀 cursor / composer-2.5-fast / ask
接下来: <提问要点> 投给 composer，结果用 3-5 行摘要
```

banner 1 行 + 计划 1-2 行。1 秒内输出，立即进入 Step 2。

### 可以输出进度报告

- 委托开始的 1 行 (`→ 向 composer 询问中`)
- 判断所需的经纬用 1 行

### 禁止 (= 冗长)

- **同一事实的两次重述**: 不在后段重新说明 cursor-companion 结果
- **无内容前言**: 仅"确认使用方法"的行等 tool call 自明声明
- **3 行以上的经纬回顾**: 必要时压缩为 1 行
- **启动序列中的 ★ Insight 块**: Insight 仅在最终摘要出现一次

违反例 (冗长):
```
× "准备向 cursor 投递提问"→ bash → "投递中"（无内容前言 + 重述）
× "ask 模式是只读所以安全"再说明（已知事实重复）
× ★ Insight ──── 首先确认 cursor 状态: ...
```

正常例 (简洁 + 计划明示):
```
🚀 cursor / composer-2.5-fast / ask
接下来: 向 composer 询问设计弱点，结果用 3-5 行摘要
```

## Execution Flow

### Step 0: 启动时 banner + plan

遵循上述 Narration Rules，输出 banner + 计划 (3 行以内) 后进入 Step 1。

### Step 1: banner 确认

Step 0 已输出 banner + 计划 (3 行以内)，因此这里确认 banner 行已输出。banner 为:

```
🚀 cursor / composer-2.5-fast / ask
```

之后可以委托开始的 1 行状态等显示进度。仅避免冗长重复。

`composer-2.5-fast` 是 `scripts/model-routing.sh --host cursor --role worker --field model` 解析值的代表表记。实际解析的 model 在 cursor-companion 侧日志输出。

### Step 2: helper root 解析 + cursor-companion 直接执行

`$ARGUMENTS` 作为提问文传递。**绝不附加 `--write`**。`scripts/cursor-companion.sh` 用相对路径调用时，在 consumer repo 的 cwd 直下不可见而 exit，因此用与 hooks.json 相同的 `valid_root` 模式解析 `CLAUDE_PLUGIN_ROOT` / `HARNESS_PLUGIN_ROOT` (Issue #193 §2):

```bash
QUESTION="$ARGUMENTS"
if [ -z "$QUESTION" ]; then
  echo "ERROR: question required. Usage: cursor:ask \"<your question>\"" >&2
  exit 1
fi

bash -c '
  set -euo pipefail
  valid_root() {
    [ -n "${1:-}" ] && [ -f "$1/scripts/cursor-companion.sh" ] && { [ -f "$1/.claude-plugin/plugin.json" ] || [ -f "$1/.codex-plugin/plugin.json" ] || [ -f "$1/.cursor-plugin/plugin.json" ]; }
  }
  HARNESS_PLUGIN_ROOT="${HARNESS_PLUGIN_ROOT:-${CLAUDE_PLUGIN_ROOT:-}}"
  ROOT="$HARNESS_PLUGIN_ROOT"
  if ! valid_root "$ROOT"; then
    ROOT=""
    if [ -n "${CLAUDE_SKILL_DIR:-}" ]; then
      probe="$(cd "${CLAUDE_SKILL_DIR}" && pwd)"
      while [ "$probe" != "/" ] && ! valid_root "$probe"; do
        probe="$(cd "$probe/.." && pwd)"
      done
      valid_root "$probe" && ROOT="$probe"
    fi
  fi
  if ! valid_root "$ROOT"; then
    ROOT=""
    for c in "${CLAUDE_PROJECT_DIR:-}" "$PWD" \
             "$HOME/.claude/plugins/marketplaces/claude-code-harness-marketplace" \
             "$HOME/.claude/plugins/cache/claude-code-harness-marketplace/claude-code-harness/"*; do
      if valid_root "$c"; then ROOT="$c"; break; fi
    done
  fi
  if ! valid_root "$ROOT"; then
    echo "ERROR: claude-code-harness plugin root not found (no scripts/cursor-companion.sh)" >&2
    exit 2
  fi
  HARNESS_PLUGIN_ROOT="$ROOT"
  bash "${HARNESS_PLUGIN_ROOT}/scripts/cursor-companion.sh" task "$1"
' _ "$QUESTION"
```

仅此操作 cursor-agent 侧就 locked 到 `--mode ask` (hard read-only stop)。也不附加 `--force` / `--yolo`。

### Step 3: host 用 3-5 行摘要结果

不直接粘贴 cursor 的输出。host (Claude/Codex) 读取后 **用 3-5 行摘要**:

- 结论
- 为何这样认为（cursor 提出的论据核心）
- 注意点 / 需要追加调查的点
- 下一步（如果有）

摘要后，最后用 literal 输出以下一句:

```
↑此结果由 host 摘要。按 Enter 键继续，或用新 prompt 给出其他指示。
```

## Trust Boundary

cursor 是不透明子进程，Harness 的护栏 (R01-R13) 不适用于内部。即使 read-only 委托也需满足以下前提条件。

### 必须前提

| 项目 | 内容 | 设置位置 |
|---|---|---|
| Secret 遮断 | 通过 `.cursorignore` 从读取对象排除 `.env` / `*.pem` / `*.key` / `.ssh` / `.aws` / `.git` | repo root `.cursorignore` |
| Egress allowlist | 在 `~/.claude/settings.json` 的 `sandbox.network.allowedDomains` 添加 `*.cursor.sh` | user settings |
| Filesystem allowlist | 同样在 `sandbox.filesystem.allowWrite` 添加 `~/.cursor` (因为 cursor-agent 进行状态写入) | user settings |
| permissions.json | `~/.cursor/permissions.json` 的 `terminalAllowlist` / `mcpAllowlist` 在 read mode 也有效 (allowlist 是 best-effort，不是 security boundary) | user config |

详情参照 `.claude/rules/cursor-cli-only.md`。

### ask mode 可省略的内容

| 通常 cursor 委托需要 | ask mode 不需要 | 理由 |
|---|---|---|
| 隔离 worktree | 不需要 | cursor 无法写入 |
| Lead diff review | 不需要 | 差分不会产生 |
| cherry-pick | 不需要 | 同上 |
| `worker-report.v1` / self_review 5 件 | 不需要 | 不进行实现 |

### 仍然残留的风险

- **读取泄漏**: 怠于 `.cursorignore` 会导致秘密文件传给 cursor 推理
- **错误信息的轻信**: cursor 输出是 untrusted。Step 3 的摘要中 host 必须保留判断轴
- **allowlist 過信**: Cursor 公式は "Allowlists are best-effort convenience. They are not a security guarantee." と明言。allowlist に依存しない

## Topology

```
Lead (Claude/Codex) ──[cursor-companion.sh task]──> cursor-agent (--mode ask, locked read-only)
       │
       └──[Step 3: 3-5 行要約]──> User
```

Worker 介在なし。Reviewer 介在なし。`worker-report.v1` / `review-result.v1` 契約は発生しない。

## Related Skills / Rules

- `cursor-do` — 書込タスク委譲（worktree + Lead review + cherry-pick の full containment）
- `breezing --cursor` — Reviewer のみ cursor に逃がす lean second-opinion レーン
- `harness-review --cursor` — レビューを cursor (composer-2.5-fast) に second-opinion として依頼
- `.claude/rules/cursor-cli-only.md` — Cursor backend governance (trust boundary, prohibited flags)
