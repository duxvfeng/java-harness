---
name: harness-work
description: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-en: "HAR: Execute Plans.md tasks from single task to full parallel team run. Trigger: implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5. Do NOT load for: planning, review, release, setup."
description-zh: "HAR：负责从单个任务到全并行团队执行的 Plans.md 任务。当用户提到实现、执行、全部完成、breezing、团队执行、并行、composer、作曲器、composer 2.5 时启动。不适用于：计划、审查、发布、设置。"
kind: workflow
purpose: "Execute Plans.md tasks end to end"
trigger: "implement, execute, do everything, breezing, team run, parallel, composer, composer 2.5, composer mode, 作曲器"
shape: workflow
role: executor
pair: harness-review
owner: harness-core
since: "2026-05-05"
allowed-tools: ["Read", "Write", "Edit", "Grep", "Glob", "Bash", "Task", "Monitor"]
argument-hint: "[all] [task-number|range] [--codex] [--parallel N] [--no-commit] [--resume id] [--breezing] [--auto-mode] [--tdd-bypass]"
user-invocable: true
effort: high
---

# Harness Work

Harness 的集成执行技能。
整合以下旧技能:

- `work` — Plans.md 任务的实现（范围自动判断）
- `impl` — 功能实现（基于任务）
- `breezing` — 团队全自动执行
- `parallel-workflows` — 并行工作流优化
- `ci` — CI 失败时的恢复

## Quick Reference

| 用户输入 | 模式 | 动作 |
|------------|--------|------|
| `/harness-work` | **auto** | 按任务数自动判定（参见下文） |
| `/harness-work all` | **auto** | 以自动模式执行所有未完成任务 |
| `/harness-work 3` | solo | 仅立即执行任务3 |
| `/harness-work --parallel 5` | parallel | 5工作器并行执行（强制） |
| `/harness-work --codex` | codex | 向 Codex CLI 委托（仅明确时） |
| `/harness-work --isolate-branch` | **branch-iso** | 在隔离分支执行，测试通过后合并 |
| Cursor host (adapter candidate) | cursor | Task/subagent routing via `.cursor/AGENTS.md`; not auto-selected |
| `/harness-work --breezing` | breezing | 强制团队执行 |
| `/harness-work 3 --plan roadmap` | solo | 从名为 Plans 的 `roadmap` 执行任务3 |

## Execution Mode Auto Selection（无标志时的自动判定）

没有明确的模式标志（`--parallel`, `--breezing`, `--codex`）时，
根据对象任务数自动选择最优模式:

| 对象任务数 | 自动选择模式 | 理由 |
|-------------|---------------|------|
| **1 件** | Solo | 开销最小。直接实现最快 |
| **2-3 件** | Parallel（Task tool） | Worker 分离的效益开始显现的阈值 |
| **4 件以上** | Breezing | Lead 协调 + Worker 并行 + Reviewer 独立的三者分离有效 |

### 规则

1. **明确标志始终覆盖自动模式**（`--parallel N` / `--breezing` / `--codex` 与任务数无关强制执行）
2. **`--codex` 仅在明确时触发**。因存在 Codex CLI 未安装的环境，不自动选择
3. `--codex` 可与其他模式组合: `--codex --breezing` → Codex + Breezing

## Branch Isolation Mode（分支隔离模式）

**Purpose**: 在任务执行前自动创建 git 分支隔离，确保主分支稳定性。测试通过后再合并回主分支。

### 核心流程

```
1. 检查当前分支（是否在主分支）
2. 自动创建 feature 分支（使用 git worktree）
3. 在隔离分支中执行任务实现
4. 运行测试验证
5. 测试通过：合并回主分支，清理 feature 分支
6. 测试失败：保留 feature 分支用于调试
```

### 基础用法

```bash
# 单个任务，自动创建分支并合并
/harness-work 3 --isolate-branch

# 全部任务，每个任务独立分支
/harness-work all --isolate-branch

# Breezing 模式 + 分支隔离
/harness-work --breezing --isolate-branch
```

### 高级用法

```bash
# 执行后保留分支，手动创建 PR
/harness-work 3 --isolate-branch --keep-branch

# 自定义分支名称
/harness-work 5 --isolate-branch --branch-name feature/add-user-auth

# 测试后不自动合并，等待人工审查
/harness-work 3-6 --isolate-branch --no-merge

# 与其他模式组合
/harness-work --parallel 3 --isolate-branch --no-merge
```

### 适用场景

- **团队协作**: 使用 `--keep-branch` 保留分支用于 Code Review
- **CI/CD**: 使用 `--no-merge` 让 CI 控制合并时机
- **调试**: 测试失败时自动保留分支现场
- **安全**: 避免直接在主分支上工作

### 详细文档

分支隔离模式的完整实现细节、错误处理、状态文件格式、配置支持等详见：
**[references/branch-isolation.md](${CLAUDE_SKILL_DIR}/references/branch-isolation.md)**

## Execution Backend Selection（实现后端选择）

后端（哪个运行时**实现**）与执行模式（拓扑: solo / parallel / breezing）正交。

| backend | 实现承担者 | 委托命令 |
|---------|------------|------------|
| `claude`（默认） | Task subagent（`agents/worker.md`） | 用 Agent tool spawn worker |

Codex 调用的治理详情（禁止事项、verdict 映射等）
参见 [references/codex-cli-only.md](${CLAUDE_SKILL_DIR}/references/codex-cli-only.md)。

run 开始时用 resolver 仅解析一次。不得直接读取 `HARNESS_IMPL_BACKEND` env 来决定后端:

```bash
```

precedence（从高到低）: 明确标志（`--backend` / `--cursor` / `--codex`） > env > project file > user file > 默认 `claude`。项目设置覆盖用户范围。

### Backend 默认（`claude` 是意图的默认，警告仅在无效值 fallback 时）

默认 backend 是 `claude`（Native subagent）。resolver 的未设定 fallback 也是 `claude`，正常解析为 `claude` 时不输出警告（2026-07-24 operator 裁定。格式与 `breezing` 的 Narration Rules「Backend 默认和 per-run 的扁平判断」相同，cross-ref: `skills/breezing/SKILL.md`）。

- ⚠️ 警告仅在 resolver 输出 **无效值 fallback** 的 stderr 警告时输出。banner 后立即 1 行，同一 run 内不重复
- Lead 可以根据作业内容、量 per-run 扁平选择 backend。选择时使用对 resolver 的明确 override（`--backend <v>` / `--codex` / `--cursor`）
- `composer` / `作曲器` / `composer 2.5` 等自然语言表现作为 `--cursor` 相同 intent 处理，向 resolver 明示 override 传递 `--backend cursor`（自然语言 backend trigger）

后端是 **role-scoped**: 遵循已解决后端的仅是实现（worker）角色。Reviewer / Advisor 总是固定为 brain（`--host claude`）（不向 primary reviewer routing cursor/codex）。唯一例外是 **fresh-context advisory pre-review**: 不与会话状态共享生成 diff 的 session 的 cursor `review` tier 可以输出 advisory findings，primary verdict（`APPROVE | REQUEST_CHANGES`）仅由 brain 输出。

```bash
```

后端为 `codex` / `cursor` 时，Lead 不会 spawn Worker agent，而是直接调用 companion（无 Worker 介入的拓扑）。跳过 self_review 门控，Lead 的 diff 审查成为唯一的质量门控。在委托前输出 cursor backend banner，在 cherry-pick 前通过两道 contract grep 门控（`test-support-claim-wording.sh` / `check-consistency.sh` / `validate-plugin.sh`）。Mode 1 的 Producer → Sub-Lead → Composer 层级、review→iterate 循环详情参见
[references/backend-selection.md](${CLAUDE_SKILL_DIR}/references/backend-selection.md)。

## 选项

| 选项 | 说明 | 默认值 |
|----------|------|----------|
| `all` | 以全部未完成为对象 | - |
| `N` or `N-M` | 指定任务编号/范围 | - |
| `--parallel N` | 并行 Worker 数量 | auto |
| `--sequential` | 强制串行执行 | - |
| `--codex` | 委托给 Codex CLI 实现（仅在明确指定时，不自动选择） | false |
| `--backend <claude\|codex\|cursor>` | 明确选择后端（仅应用于 worker 角色，优先级最高） | claude |
| `--cursor` | cursor 后端（同 `--codex`，仅在明确指定时。因存在未安装 cursor-agent 的环境，不自动选择） | false |
| `--plan NAME` | 使用 `plans/manifest.json` 中的指定 plan | active/default |
| `--no-commit` | 抑制自动提交 | false |
| `--resume <id\|latest>` | 恢复上一次会话。在长时间中断后推荐与 `/recap` 一起使用 | - |
| `--breezing` | Lead/Worker/Reviewer 团队执行 | false |
| `--no-tdd` | 跳过 TDD 阶段 | false |
| `--tdd-bypass` | 仅在紧急情况下绕过 TDD 强制。将 `HARNESS_TDD_BYPASS_REASON` 或明确理由保留在 audit 中 | false |
| `--no-simplify` | 跳过 Auto-Refinement | false |
| `--auto-mode` | 明确 Harness 侧的 Auto Mode rollout。与在 CC 2.1.111 中已不需要的 `--enable-auto-mode` 不同 | false |
| `--isolate-branch` | 启用分支隔离模式。任务执行前自动创建 feature 分支，测试通过后合并回主分支 | false |
| `--no-merge` | 与 `--isolate-branch` 配合使用，完成测试后保留分支不自动合并 | false |
| `--branch-name <name>` | 自定义分支名称（默认：feature/task-<id>-<timestamp>） | auto |
| `--keep-branch` | 完成后保留 feature 分支（用于人工审查或创建 PR） | false |

## Progressive Disclosure

首先在此正文中确认入口、自动选择、停止条件。详细内容仅在必要时阅读。

| 详细 | 参照 |
|---|---|
| Solo / Breezing 的 1-17 步骤完整版、Phase A/B/C 完整版 | `references/execution-modes.md` |
| Backend role-scoped 限制、非 claude 拓扑、Mode 1 层级、review→iterate | `references/backend-selection.md` |
| **分支隔离模式、git worktree 管理、自动合并流程** | `references/branch-isolation.md` |
| Codex review、Reviewer fallback、verdict mapping、修正循环 | `references/review-loop.md` |
| Sprint Contract 字段一览、PR Closeout | `references/sprint-contract.md` |
| effort tier 的多要素得分详情 | `references/effort-routing.md` |
| Solo / Breezing 完成报告的生成 | `references/completion-report.md` |
| 测试/CI 失败时的重新票决命令 | `references/failure-reticketing.md` |
| 规格权威版本检查的基准 | `docs/plans/spec-ssot.md` |

### 重要停止条件

- `Plans.md` 为旧格式无法读取 DoD / Depends / Status 时停止。
- 规格影响实现判断但找不到 project spec SSOT 时，先创建/更新规格权威版本后再实现。
- sprint-contract 为 required 但未 ready 时不进入实现。
- 残留 critical / major review findings 时不完成。
- 不能以减弱测试、skip 测试、将期待值配合实现放宽的形式解决。
- helper script 从 `${HARNESS_PLUGIN_ROOT}/scripts/` 而非 host project 的 `scripts/` 调用。
- 有多个 Plans.md 时，不在 1 run 中切换 plan。必要时明确 `--plan NAME` 并启动新的 run。

> **Token Optimization (v2.1.69+)**: 在不伴随 git 操作的轻量任务中
> 可以在 plugin settings 中启用 `includeGitInstructions: false` 来减少 prompt token。

> **Prompt Cache (CC 2.1.108+)**: 在较长的实现或频繁使用 `--resume` 的工作中
> 优先使用 `ENABLE_PROMPT_CACHING_1H=1`。

## 范围对话框（无参数时）

```
/harness-work
执行到什么程度？
1) 下一个任务: Plans.md 的下一个未完成任务 → Solo 执行
2) 全部（推荐）: 完成所有剩余任务 → 根据任务数自动选择模式
3) 指定编号: 输入任务编号（例: 3, 5-7）→ 根据件数自动选择模式
```

有参数时立即执行（跳过交互）:
- `/harness-work all` → 全部任务，自动模式选择
- `/harness-work 3-6` → 4 件所以自动选择 Breezing

## Effort 级别控制（Opus 4.8 / v2.1.111+）

effort 是选择模型推理强度的正式旋钮。分为 `low(○)/medium(◐)/high(●)/xhigh` 4 个级别，
可以用 `/effort auto` 重置为默认值。从复杂度得分（文件数·目标目录·关键字·失败历史·明确指定之合计）
决定 tier，不采用向 spawn prompt 注入 free-text marker（旧 `ultrathink`）的方式。

| 得分 | code-risk（包含 core/guardrails/security/architecture/migration） | effort tier |
|--------|-----------------------------------|-------------|
| 0-2 | 不问 | `medium`（Worker frontmatter 默认） |
| ≥ 3 | 无 | `high` |
| ≥ 3 | 有 | `xhigh` |

在 breezing 模式下也应用相同的逻辑（harness-work 统一管理）。得分详情·lever 详情参见
[references/effort-routing.md](${CLAUDE_SKILL_DIR}/references/effort-routing.md)。

## 执行模式详情

### Harness helper script root

Harness 捆绑的 helper script 必须从 plugin bundle root 调用，而不是工作目标项目的 `scripts/`。

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

以后的 `node "${HARNESS_PLUGIN_ROOT}/scripts/..."` / `bash "${HARNESS_PLUGIN_ROOT}/scripts/..."` 以此已解析的 root 为前提。

### Auto Review Integration (v2.1.0+)

从 v2.1.0 开始，harness-work 使用 harness-review 的 --auto 模式进行代码审查，实现职责清晰分离。

**调用函数:**

```python
def call_harness_review_auto(base_ref: str, worktree_path: str, mode: str = "strict") -> dict:
    """
    调用 harness-review --auto 模式进行自动代码审查

    Args:
        base_ref: 基准 commit SHA 或分支名
        worktree_path: 工作树路径
        mode: 审查模式 (strict|lenient)

    Returns:
        包含审查结果的字典
        {
            "success": bool,
            "result": {
                "verdict": "APPROVE|REQUEST_CHANGES",
                "findings": [...],
                "summary": "...",
                "performance": {...}
            },
            "stdout": str,
            "stderr": str
        }
    """
    import subprocess
    import json
    import os

    # 确定脚本路径（项目统一的 scripts/ 目录）
    auto_review_script = os.path.join(
        HARNESS_PLUGIN_ROOT,
        "scripts", "review", "auto-review.sh"
    )

    # 准备输出文件
    output_file = f"/tmp/harness-review-{os.getpid()}.json"

    # 构建命令
    cmd = [
        auto_review_script,
        "--auto",
        "--base-ref", base_ref,
        "--output", output_file,
        "--mode", mode
    ]

    try:
        # 执行审查
        result = subprocess.run(
            cmd,
            cwd=worktree_path,
            timeout=30,
            capture_output=True,
            text=True
        )

        # 读取结果
        if os.path.exists(output_file):
            with open(output_file, 'r') as f:
                review_result = json.load(f)

            # 清理临时文件
            os.remove(output_file)

            return {
                "success": True,
                "result": review_result,
                "stdout": result.stdout,
                "stderr": result.stderr
            }
        else:
            return {
                "success": False,
                "error": "输出文件不存在",
                "stdout": result.stdout,
                "stderr": result.stderr
            }

    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error": "审查超时（超过 30 秒）"
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def lightweight_review_fallback(base_ref: str, worktree_path: str) -> str:
    """
    轻量级审查降级方案
    当 harness-review --auto 不可用时使用

    Args:
        base_ref: 基准 commit SHA
        worktree_path: 工作树路径

    Returns:
        "APPROVE" 或 "REQUEST_CHANGES"
    """
    import subprocess

    try:
        # 基础检查：确保代码可以编译/运行
        result = subprocess.run(
            ["git", "-C", worktree_path, "diff", "--name-only", f"{base_ref}..HEAD"],
            capture_output=True,
            text=True,
            timeout=10
        )

        changed_files = result.stdout.strip().split('\n') if result.stdout.strip() else []

        # 如果有变更文件，进行基本检查
        if changed_files:
            # 检查是否有明显的错误（如编译错误）
            # 这里可以添加更多基础检查
            pass

        # 保守策略：如果有任何变更，要求人工审查
        if changed_files:
            return "REQUEST_CHANGES"
        else:
            return "APPROVE"

    except Exception:
        # 出错时采用保守策略
        return "REQUEST_CHANGES"

class AutoFixReviewLoop:
    """自动修复审查循环 (v2.1.1+)"""

    def __init__(self, max_iterations: int = 3):
        """
        初始化自动修复循环

        Args:
            max_iterations: 最大修复尝试次数
        """
        self.max_iterations = max_iterations
        self.iteration_count = 0

    def auto_fix_issues(self, issues: list, worktree_path: str, worker_context: dict) -> dict:
        """
        自动修复代码问题

        Args:
            issues: 需要修复的问题列表
            worktree_path: 工作树路径
            worker_context: Worker 上下文

        Returns:
            修复结果字典
        """
        import subprocess

        fixed_count = 0
        failed_count = 0

        for issue in issues:
            try:
                fix_command = self.generate_fix_command(issue)
                result = subprocess.run(
                    fix_command,
                    cwd=worktree_path,
                    capture_output=True,
                    text=True,
                    timeout=10
                )

                if result.returncode == 0:
                    print(f"✅ 修复: {issue['file']}:{issue['line']} - {issue['message']}")
                    fixed_count += 1
                else:
                    print(f"⚠️  修复失败: {issue['file']}:{issue['line']}")
                    failed_count += 1

            except Exception as e:
                print(f"❌ 修复异常: {issue['file']}:{issue['line']} - {e}")
                failed_count += 1

        return {
            "success": fixed_count > 0,
            "fixed_count": fixed_count,
            "failed_count": failed_count
        }

    def generate_fix_command(self, issue: dict) -> list:
        """根据问题生成修复命令"""
        file_path = issue["file"]
        line_number = issue["line"]
        rule = issue.get("rule", "")
        suggestion = issue.get("suggestion", "")

        # 根据不同的规则生成不同的修复命令
        if rule == "no-system-out":
            return [
                "sed", "-i",
                f"{line_number}s/System.out.println/LOGGER.info/",
                file_path
            ]
        elif rule == "no-print-statements":
            return [
                "sed", "-i",
                f"{line_number}s/print(/logging.info/",
                file_path
            ]
        else:
            # 通用修复策略
            return [
                "sed", "-i",
                f"{line_number}i// TODO: 需要修复: {issue['message']}",
                file_path
            ]

    def commit_fixes(self, worktree_path: str, commit_message: str) -> dict:
        """提交修复"""
        import subprocess

        try:
            subprocess.run(
                ["git", "add", "-A"],
                cwd=worktree_path,
                check=True,
                capture_output=True
            )

            result = subprocess.run(
                ["git", "diff", "--staged", "--name-only"],
                cwd=worktree_path,
                capture_output=True,
                text=True
            )

            if not result.stdout.strip():
                return {
                    "success": False,
                    "error": "没有需要提交的变更"
                }

            subprocess.run(
                ["git", "commit", "-m", commit_message],
                cwd=worktree_path,
                check=True,
                capture_output=True
            )

            return {
                "success": True,
                "commit_message": commit_message
            }

        except subprocess.CalledProcessError as e:
            return {
                "success": False,
                "error": f"Git 命令执行失败: {e}"
            }
        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }
```

### Backend-resolved executor path (Solo / Parallel / Breezing)

Solo / Parallel / Breezing 从相同的 resolver result 选择实现 executor。
`harness-work 3 --cursor` 或 resolver 输出为 `cursor` 的 run（包含通过 project / user file 的 default），
即使是 1 件任务也不能 fall through 到 local Read/Write/Edit/Bash。

```
resolver_backend_arg = ""
if explicit_backend_value in ["claude", "codex", "cursor"]:
    resolver_backend_arg = "--backend {explicit_backend_value}"
if explicit_flag == "--cursor":
    backend = "cursor"
if explicit_flag == "--codex":
    backend = "codex"

if topology in ["solo", "parallel"] and backend in ["cursor", "codex"]:
    BASE_REF = git("rev-parse", "HEAD")
    WT_ID = "{task.number}-$(date +%Y%m%d-%H%M%S)-$$"
    worktree_path = ".claude/worktrees/{backend}-{WT_ID}"
    worktree_branch = "{backend}-work/{WT_ID}"
    bash("mkdir -p .claude/worktrees && git worktree add -b {worktree_branch} {worktree_path} {BASE_REF}")
    companion_prompt = "{task prompt}\n\nAfter making changes, create exactly one git commit in this worktree before returning."
    if backend == "cursor":
    else:
        companion_state_file = "{worktree_path}/.claude/state/codex-primary-environment.json"
    latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if backend == "cursor" and git("-C", worktree_path, "status", "--porcelain") != "":
        git("-C", worktree_path, "add", "-A")
        git("-C", worktree_path, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: delegated change")
        latest_commit = git("-C", worktree_path, "rev-parse", "HEAD")
    if latest_commit == BASE_REF:
        raise EscalationError("{backend} companion produced no commit")
    worker_result = {type: "companion-result.v1", baseCommit: BASE_REF, commit: latest_commit, worktreePath: worktree_path, branch: worktree_branch, files_changed: git("-C", worktree_path, "diff", "--name-only", "{BASE_REF}..HEAD"), summary: companion_output}
    enter_non_claude_companion_review_loop(worker_result)
else:
    run_native_solo_or_parallel()

def enter_non_claude_companion_review_loop(worker_result):
    # companion-result.v1 has no worker_id and no worker_result.self_review.
    # Do not use the Worker-only SendMessage/self_review loop for cursor/codex.
    latest_commit = worker_result.commit

    # 调用 harness-review --auto 模式进行自动审查
    verdict_result = call_harness_review_auto(
        base_ref=worker_result.baseCommit,
        worktree_path=worker_result.worktreePath,
        mode="strict"
    )

    if not verdict_result["success"]:
        # 如果自动审查失败，降级到基础检查
        logger.warning(f"harness-review --auto 失败，使用 fallback: {verdict_result.get('error')}")
        verdict = lightweight_review_fallback(
            base_ref=worker_result.baseCommit,
            worktree_path=worker_result.worktreePath
        )
        findings = []
    else:
        verdict = verdict_result["result"]["verdict"]
        findings = verdict_result["result"].get("findings", [])

    review_count = 0
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3

    # 集成自动修复循环
    auto_fix_loop = AutoFixReviewLoop(max_iterations=MAX_REVIEWS)

    while review_count < MAX_REVIEWS:
        # 调用自动审查
        verdict_result = call_harness_review_auto(
            base_ref=BASE_REF,
            worktree_path=worker_result.worktreePath,
            mode="strict"
        )

        if not verdict_result["success"]:
            # 如果自动审查失败，降级到基础检查
            logger.warning(f"harness-review --auto 失败，使用 fallback: {verdict_result.get('error')}")
            verdict = lightweight_review_fallback(
                base_ref=BASE_REF,
                worktree_path=worker_result.worktreePath
            )
            # 如果 fallback 也不通过，直接跳过循环
            if verdict == "REQUEST_CHANGES":
                logger.warning("Fallback 审查也不通过，跳过自动修复循环")
                break
        else:
            verdict = verdict_result["result"]["verdict"]
            findings = verdict_result["result"].get("findings", [])

        # 如果审查通过，退出循环
        if verdict == "APPROVE":
            print("✅ 审查通过，继续流程")
            break

        # 审查不通过，执行自动修复循环
        print(f"❌ 审查未通过 (第 {review_count + 1} 次)")

        # 分析问题并自动修复
        critical_major_issues = [
            f for f in findings
            if f.get("severity") in ["critical", "major"]
        ]

        if not critical_major_issues:
            print("⚠️  没有 critical/major 问题，可能是判定问题")
            break

        print(f"🔧 发现 {len(critical_major_issues)} 个需要修复的问题")

        # 调用自动修复
        fix_result = auto_fix_loop.auto_fix_issues(
            critical_major_issues,
            worker_result.worktreePath,
            worker_context
        )

        if not fix_result["success"]:
            print(f"❌ 自动修复失败: {fix_result.get('error')}")
            break

        # 提交修复
        commit_result = auto_fix_loop.commit_fixes(
            worker_result.worktreePath,
            f"fix: 审查问题修复 (第 {review_count + 1} 次尝试)"
        )

        if not commit_result["success"]:
            print(f"❌ 提交修复失败: {commit_result.get('error')}")
            break

        review_count += 1
        print(f"✅ 修复已提交，准备第 {review_count + 1} 次审查...")

    # 检查是否达到最大重试次数
    if review_count >= MAX_REVIEWS and verdict != "APPROVE":
        print(f"⚠️  达到最大重试次数 ({MAX_REVIEWS})，需要人工介入")
        # 可以选择继续或停止
        user_input = input("继续尝试？(y/n): ")
        if user_input.lower() != 'y':
            raise EscalationError("达到最大重试次数，用户选择停止")
        previous_commit = latest_commit
        if backend == "cursor":
        else:
            companion_state_file = "{worker_result.worktreePath}/.claude/state/codex-primary-environment.json"
        latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
        if backend == "cursor" and git("-C", worker_result.worktreePath, "status", "--porcelain") != "":
            git("-C", worker_result.worktreePath, "add", "-A")
            git("-C", worker_result.worktreePath, "-c", "user.name=cursor-composer", "-c", "user.email=cursor-composer@local", "commit", "--no-verify", "-m", "cursor: review fix")
            latest_commit = git("-C", worker_result.worktreePath, "rev-parse", "HEAD")
        if latest_commit == previous_commit:
            raise EscalationError("{backend} companion retry produced no new commit")
        worker_result.commit = latest_commit
        worker_result.summary = companion_output

        # 重新调用 harness-review --auto 进行审查
        verdict_result = call_harness_review_auto(
            base_ref=worker_result.baseCommit,
            worktree_path=worker_result.worktreePath,
            mode="strict"
        )

        if verdict_result["success"]:
            verdict = verdict_result["result"]["verdict"]
            findings = verdict_result["result"].get("findings", [])
        else:
            # 如果自动审查失败，降级处理
            logger.warning(f"harness-review --auto 重试失败，使用 fallback: {verdict_result.get('error')}")
            verdict = "REQUEST_CHANGES"  # 保守策略
            findings = []

        review_count++
    if verdict == "APPROVE":
        git cherry-pick --no-commit {worker_result.baseCommit}..{worker_result.commit}
```

Parallel 对每个 task 应用这个 resolver path。
backend=`cursor` / `codex` 时不使用 native Worker spawn，而是为每个 task 创建 isolated companion worktree，规范化为 `companion-result.v1` 后进入 non-Claude companion 专用的 range review / cherry-pick 循环。

### Solo 模式（1 件时的自动选择）

从 Plans.md 读入到 `cc:完了 [hash]` 的 1-17 步骤完整版参见
[references/execution-modes.md#solo-detailed-steps](${CLAUDE_SKILL_DIR}/references/execution-modes.md)。
要点：在**规格权威版本 preflight** 确认 spec SSOT 的有无并将 `spec_path` 传递给 Worker/Reviewer。应用 plan-time 事前确认，
将工作中因已声明事项引起的 `AskUserQuestion` 降为零。按 TDD Red → sprint-contract → 实现 → 审查循环 → commit → `cc:完了` 的顺序进行。

### Parallel 模式（2-3 件时的自动选择 / `--parallel N` 强制）

用 N 个 Worker 并行执行带 `[P]` 标记的任务。
用 `--parallel N` 明确指定时，无论任务数如何都使用此模式。
向同一文件的写入发生冲突时用 git worktree 分离。
各 task 的实现 executor 遵循 Backend-resolved executor path。
`--parallel N --cursor`、`--backend cursor`、或 resolver 输出为 `cursor` 时，Parallel 也不使用 native Worker spawn，而是使用每个 task 的 Cursor companion worktree。

### Codex 模式（`--codex` 仅在明确指定时）

通过官方插件 `codex-plugin-cc` 的 companion 将任务委托给 Codex CLI。

```bash
# 任务委托（可写入·worktree 分离）
BASE_REF="$(git rev-parse HEAD)"
WT_ID="codex-$(date +%Y%m%d-%H%M%S)-$$"
WORKTREE_PATH=".claude/worktrees/${WT_ID}"
git worktree add -b "codex-work/${WT_ID}" "$WORKTREE_PATH" "$BASE_REF"
HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
  "任务内容。请在此 worktree 中创建 exactly one git commit 后再完成。"

# 通过 stdin（面向较大的 prompt）
CODEX_PROMPT=$(mktemp /tmp/codex-prompt-XXXXXX.md)
# 写出任务内容
cat "$CODEX_PROMPT" | HARNESS_CODEX_PRIMARY_ENV_STATE_FILE="$WORKTREE_PATH/.claude/state/codex-primary-environment.json" \
rm -f "$CODEX_PROMPT"

# Lead review 后批准后取入 range
git -C "$WORKTREE_PATH" diff "$BASE_REF..HEAD"
WORKTREE_HEAD="$(git -C "$WORKTREE_PATH" rev-parse HEAD)"
git cherry-pick --no-commit "$BASE_REF..$WORKTREE_HEAD"
```

companion 通过 App Server Protocol 与 Codex 通信，
提供 Job 管理·thread resume·结构化输出。
验证结果，不满足质量标准时自己修正。

### Cursor 模式（adapter candidate，不自动选择）

Cursor host 中 `.cursor/AGENTS.md` 和 `.cursor-plugin/plugin.json` 是
bootstrap route。Cursor 保持 `candidate` — 禁止 supported claim。

- **Solo / Parallel**: Task tool 或 `.cursor/agents/worker.md` subagent
- **Breezing**: Worker 并行仅限 non-overlapping file groups;
  Reviewer / cherry-pick / Advisor 按 core 规则串行
- **Multitask / background agents**: 仅限 smoke target。不主张 Claude Agent Teams parity

```bash
bash tests/test-cursor-adapter-candidate.sh
```

Explicit Task/subagent `model` 优先于 routed default。

### Breezing 模式（4 件以上时自动选择 / `--breezing` 强制）

通过 Lead / Worker / Advisor / Reviewer 的角色分离进行团队执行。
Codex 假定使用 `spawn_agent`, `wait`, `send_input`, `resume_agent`, `close_agent`
的 native subagent orchestration。
Cursor 向 Task/subagent/background agents mapping，但
review/cherry-pick 的串行责任留在 core 侧（adapter smoke target）。

**权限策略**: 当前 shipped default 为 `bypassPermissions`。`--auto-mode` 是兼容亲会话的 opt-in rollout 标志。
不要在 `permissions.defaultMode` 或 agent frontmatter 的 `permissionMode` 中写入未文档化的 `autoMode` 值。

```
Lead (this agent)
├── Worker (task-worker agent) — 实现担当
├── Advisor (claude-code-harness:advisor) — 方针建议
└── Reviewer (code-reviewer agent) — 审查担当
```

Phase A（准备: 智能分支检测·Plans.md 读入·依赖解决·plan-preapproval 应用·effort 得分·sprint-contract 生成）→
Phase B（各任务: Worker spawn → 必要时 Advisor → self_review 门控 → 审查循环 → APPROVE 时向 trunk cherry-pick）→
Phase C（整合: commit log 集计·丰富完成报告·Plans.md 最终确认）的 3 段构成。
完整版 pseudocode（包含 B-1-B-7 的逐次顺序）参见
[references/execution-modes.md#breezing-phase-detail](${CLAUDE_SKILL_DIR}/references/execution-modes.md)。

### 智能分支隔离检测（Phase A 集成）

**Purpose**: 在任务执行开始前自动检测分支状态并应用适当的隔离策略

**触发时机**: Phase A 准备阶段，在 Plans.md 读入之后，任务执行之前

**检测逻辑**:
1. 自动检测当前分支类型（main/feature/worktree）
2. 根据分支类型和配置文件确定隔离策略（force/ask/skip）
3. 处理用户交互决策
4. 记录决策到状态文件

**策略类型**:
- `force`: 强制隔离（主分支保护）- 自动创建隔离分支，无需用户确认
- `ask`: 可选隔离（功能分支）- 提示用户选择是否隔离
- `skip`: 跳过隔离（已隔离状态）- 当前已在 worktree 中，无需额外隔离

**优先级**:
1. 显式 `--isolate-branch` 标志优先级最高
2. 智能检测次之（根据分支类型和配置）
3. 配置文件可覆盖默认策略

**配置支持**:
```json
// .claude/settings.json
{
  "branchIsolation": {
    "mainBranch": "force",     // 主分支策略：force/ask/skip
    "featureBranch": "ask"     // 功能分支策略：force/ask/skip
  }
}
```

**状态文件**: `.claude/state/branch-isolation-decision.json`
- 记录所有分支隔离决策历史
- 包含时间戳、分支名称、策略类型、用户选择
- 支持审计和调试

**执行脚本**:
```bash
# 智能检测（推荐 - 自动根据分支类型决定）
bash scripts/branch-isolation/handle-isolation.sh --auto

# 强制特定策略
bash scripts/branch-isolation/handle-isolation.sh --strategy force

# 仅检测不执行
bash scripts/branch-isolation/detect-branch.sh --strategy
```

### Active task scope

在各任务的 preapproval preflight 之前，向目标 worktree 的
`.claude/state/active-task.json` 原子性写入 `{"phase":"<phase>","task":"<task>"}`。
Go guardrail 将此文件作为当前 scope 的权威版本读取。
任务结束时，无论成功、失败、停止的哪条路径都删除。环境变量
`HARNESS_ACTIVE_PHASE` / `HARNESS_ACTIVE_TASK` 仅用于
state 文件不存在的 host 的 fallback。

Parallel / Breezing 在每个任务的 worktree 中写入。不在多个任务间共享
同一 worktree 的 `active-task.json`。

### Advisor Protocol（所有模式通用）

Advisor 既不是"实现者"也不是"审查担当"。仅在迷惘时，作为执行者决定下一步的协商对象介入。

1. Worker 不增加 generic subagent，仅在必要时返回 `advisor-request.v1`
2. Lead 仅调用 advisor 一次
3. Advisor 返回 `PLAN` / `CORRECTION` / `STOP` 之一
4. Lead 将其 advice（`advisor-response.v1`）返回给同一 Worker 继续执行
5. Reviewer 只看最后的成果物。不对 advisor 的返答输出 APPROVE / REQUEST_CHANGES

solo 执行时亲会话自身兼任 Lead（自己实现，自己向 advisor 协商，最后交给独立审查）。
协商条件·budget 与 breezing 相同，每个任务的协商次数最多 3 次。`STOP` 在该处停止，上升到用户判断。review artifact 的门控不跳过。

### Sprint Contract 与 PR Closeout

`sprint-contract` 是将"此任务用什么来合格"机械可读化的契约文件（默认: `.claude/state/contracts/<task-id>.sprint-contract.json`，由 `generate-sprint-contract.js` 生成）。
`runtime_validation` 的 LSP/AST 工作流方针:

- If you grep the same symbol twice in the same session, switch to harness_ast_search.
- For a bugfix where homologous implementations appear across multiple modules, run harness_ast_search to find all implementations before editing.
- Only when changed files include .ts or .tsx, the DoD requires zero new harness_lsp_diagnostics errors; if the harness MCP is not connected or the changed file types are not eligible, treat diagnostics as not-configured and non-blocking.

`spec_path` / `lane` / `stage` / `research_evidence` / `tdd_red_log` / `review_artifact` / `pr_closeout` 的字段规格，
以及 review APPROVE 后的 PR title/body 组装（`harness-pr-closeout.sh`，默认 `dry-run`）详情参见
[references/sprint-contract.md](${CLAUDE_SKILL_DIR}/references/sprint-contract.md)。

## CI 失败时的对应

CI 失败时:

1. 确认日志并特定错误
2. 实施修正
3. 同一原因失败 3 次后停止自动修正循环
4. 汇总失败日志·尝试的修正·残留论点并升级

## 失败任务的自动重新票决

任务完成后测试/CI 失败时，自动生成修正任务案，批准后反映到 Plans.md。
触发条件·生成格式·批准命令（`approve fix <task_id>` / `reject fix <task_id>`）详情参见
[references/failure-reticketing.md](${CLAUDE_SKILL_DIR}/references/failure-reticketing.md)。

## 审查循环

实现完成后自动执行的质量验证阶段。**全模式通用**（Solo / Parallel / Breezing）统一应用。
优先级（Codex exec → 内部 Reviewer agent fallback）、APPROVE / REQUEST_CHANGES 判定基准（仅 critical/major 影响 verdict）、
verdict 映射、修正循环（`MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3`）的完整版参见
[references/review-loop.md](${CLAUDE_SKILL_DIR}/references/review-loop.md)。

## 智能模型选择（Smart Model Selection）

**功能**: 根据任务复杂度自动选择最优的 AI 大模型，提高成本效益和性能表现。

### 核心价值

- **成本优化**: 简单任务使用快速/便宜模型，复杂任务使用强大模型
- **性能优化**: 根据任务需求匹配合适的模型能力
- **可靠性**: 完整的降级机制，确保系统总能找到可用模型
- **灵活性**: 配置驱动，支持运行时策略调整

### 工作原理

#### 1. 复杂度评分

基于以下因素计算任务复杂度分数：

| 要素 | 条件 | 分数 |
|------|------|--------|
| 文件数 | 变更对象 4 个文件以上 | +1 |
| 目录 | 包含 core/、guardrails/、security/ | +1 |
| 关键字 | 包含 architecture、security、design、migration | +1 |
| 失败历史 | agent memory 中有同任务的失败记录 | +2 |
| 显式指定 | PM 模板中记载 `effort: high` / `effort: xhigh` | +3（自动采用） |

#### 2. 模型等级映射

| 复杂度分数 | 模型等级 | 主要模型 | 环境变量 |
|------------|----------|---------|---------|
| 0-2 | FAST (低复杂度) | FABLE | `ANTHROPIC_DEFAULT_FABLE_MODEL` |
| 3-4 | BALANCED (中等复杂度) | HAIKU | `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| 5-6 | QUALITY (高复杂度) | SONNET | `ANTHROPIC_DEFAULT_SONNET_MODEL` |
| ≥7 | POWERFUL (超高复杂度) | OPUS | `ANTHROPIC_DEFAULT_OPUS_MODEL` |

#### 3. 降级机制

每个模型等级都有独立的降级链，按顺序尝试直到找到可用模型：

```
1. 主要模型 (如 env:ANTHROPIC_DEFAULT_HAIKU_MODEL)
2. 默认模型 (env:ANTHROPIC_MODEL)
3. 安全模型 (glm-4.7 硬编码兜底)
```

### 配置方式

#### 默认配置（自动启用）

系统会自动加载默认配置，无需手动设置。默认配置包含完整的四个等级配置和合理的降级链。

#### 项目配置（可选）

**.claude/settings.json** (优先级最高):
```json
{
  "modelSelection": {
    "enabled": true,
    "strategy": "effortBased",
    "fallback": {
      "priority": ["tierModel", "defaultModel", "safeModel"],
      "maxAttempts": 3,
      "timeoutMs": 5000,
      "validateApiCall": false
    },
    "tierMapping": {
      "fast": {
        "scoreRange": [0, 2],
        "modelEnv": "ANTHROPIC_DEFAULT_FABLE_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "balanced": {
        "scoreRange": [3, 4],
        "modelEnv": "ANTHROPIC_DEFAULT_HAIKU_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "quality": {
        "scoreRange": [5, 6],
        "modelEnv": "ANTHROPIC_DEFAULT_SONNET_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_SONNET_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      },
      "powerful": {
        "scoreRange": [7, 999],
        "modelEnv": "ANTHROPIC_DEFAULT_OPUS_MODEL",
        "fallbackModels": [
          "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
          "env:ANTHROPIC_MODEL",
          "glm-4.7"
        ]
      }
    }
  }
}
```

**harness.toml** (项目配置):
```toml
[model_selection]
enable_smart_selection = true
strategy = "effort_based"

[model_selection.fallback]
priority = ["tier_model", "default_model", "safe_model"]
max_attempts = 3
timeout_ms = 5000
validate_api_call = false

[model_selection.tiers.fast]
min_score = 0
max_score = 2
model_env = "ANTHROPIC_DEFAULT_FABLE_MODEL"
fallback_models = [
  "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
  "env:ANTHROPIC_MODEL",
  "glm-4.7"
]
```

### 与 Effort Routing 集成

智能模型选择与现有的 Effort Routing 系统无缝集成：

1. **复杂度评分**: 使用现有的多要素评分机制
2. **Effort Tier 决定**: 根据分数和 code-risk 决定 effort tier
3. **模型选择**: 集成 SmartModelSelector 返回选择的模型

**集成示例**:
```java
EffortRouter router = new EffortRouter();
TaskContext context = new TaskContext(5, 2, true, false);
WorkerSpawnConfig config = router.determineWorkerConfig(context);
// config.getEffortTier() 返回 "xhigh"
// config.getSelectedModel() 返回选择的模型
```

### 使用示例

#### 基本使用

```bash
# 自动启用智能模型选择（默认）
/harness-work 3

# 系统会自动：
# 1. 计算任务复杂度分数
# 2. 根据分数选择模型等级
# 3. 执行降级链找到可用模型
# 4. 返回 WorkerSpawnConfig
```

#### 环境变量配置

```bash
# 设置默认模型（可选）
export ANTHROPIC_MODEL="glm-4.7"

# 设置等级特定模型
export ANTHROPIC_DEFAULT_FABLE_MODEL="claude-fable-5-20250514"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="claude-3.5-haiku-20241022"
export ANTHROPIC_DEFAULT_SONNET_MODEL="claude-sonnet-4-20250514"
export ANTHROPIC_DEFAULT_OPUS_MODEL="claude-opus-4-20250514"
```

### 性能目标

- **单次选择时间**: < 100ms（典型任务）
- **并发支持**: 支持 10+ 并发线程
- **内存占用**: < 10MB（配置和缓存）
- **可用性**: 选择成功率 > 98%（完整降级机制）

### 监控和日志

智能模型选择会输出详细日志，便于调试和监控：

- 模型选择日志：记录分数、等级、选择的模型
- 降级链日志：记录降级过程和失败原因
- 性能日志：记录选择耗时和并发统计

### 故障排除

**问题**: 所有模型都不可用
- **解决**: 检查环境变量配置，确保至少有一个兜底模型（glm-4.7）

**问题**: 选择的模型不符合预期
- **解决**: 检查任务复杂度评分，确认选择的等级正确

**问题**: 降级链总是失败
- **解决**: 检查网络连接和模型可用性，调整超时设置

### 相关文档

- **设计文档**: `docs/superpowers/specs/2026-08-10-smart-model-selection-design.md`
- **实施计划**: `docs/superpowers/plans/2026-08-10-smart-model-selection.md`
- **参考文档**: `skills/harness-work/references/effort-routing.md`

## Completion Report Output Contract

<!-- harness-work-completion-output-contract:start -->
Before rendering a Solo, forced single-task Parallel, or Breezing completion
report:

1. Resolve the active locale with the shared `get_harness_locale` function from
   `${HARNESS_PLUGIN_ROOT}/scripts/config-utils.sh`. Pass an explicit session or
   user language as its optional argument; otherwise keep the resolver priority
   of project `i18n.language`, `CLAUDE_CODE_HARNESS_LANG`, then default `en`.
2. Unset, invalid, and resolved `en` render the English template.
3. Only resolved `ja` renders the Japanese template.
4. Japanese input alone does not select the Japanese template.
5. Read `references/completion-report.md` and render exactly one template for
   the selected mode and locale.
6. Keep machine-readable status and review values in English, and never mix
   English and Japanese labels in one report.
<!-- harness-work-completion-output-contract:end -->

## 进度的可视化（面向非工程师）

任务执行中 `harness-progress` 将进度的件数与 drift alert 汇总到一张 HTML。
由 PostToolUse hook 自动再生成，订货人无需记住调用方式就能看到最新的进度板
（`posttool-progress-regen.sh` 最多每 1 分钟再生成 1 次）。

## 相关技能

- `harness-plan` — 计划要执行的任务
- `harness-sync` — 同步实现与 Plans.md
- `harness-review` — 审查实现
- `harness-release` — 版本 bump·发布
- `harness-progress` — 进度板 HTML（面向非工程师，执行中自动再生成）
