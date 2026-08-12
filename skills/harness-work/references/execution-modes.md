# Execution Modes

`harness-work` chooses the lightest execution mode that still preserves review
and validation.

> **Java 版本边界**：本文件的完整自动化步骤和 Python 伪代码以 Go 版本
> 为基线。Java 版本由宿主平台执行 worker、worktree、测试和 review；使用
> `harness doctor`、`harness validate all`、`harness sprint-contract` 和
> `harness evidence` 记录结果。下文中的 `HARNESS_PLUGIN_ROOT`、
> `scripts/*.sh`、自动 runner 和 artifact writer 不属于 Java CLI，不要执行。

## Shared Preflight

1. Read `Plans.md` and identify the selected task set.
2. Stop if the task table lacks `Task`, `DoD`, `Depends`, or `Status`.
3. Check whether a project spec SSOT exists when product behavior can drift.
   Prefer existing project-level docs, then `docs/spec/00-project-spec.md`.
4. If the task changes product behavior, API, data model, permissions, billing,
   integrations, or tenant boundaries and no stable spec exists, create or
   update the spec before implementation.
5. Skip spec creation only for mechanical work such as typo, formatting,
   dependency bump, docs-only, or behavior-preserving refactor tasks. Record
   the skip reason in the task context or sprint contract.
6. Resolve helper scripts through `HARNESS_PLUGIN_ROOT`, not the caller
   project's `scripts/` directory.
7. Mark only the selected task as `cc:WIP`.
8. Generate and approve a sprint contract before implementation when the task
   needs reviewable DoD checks.

## Auto Review Integration Function (v2.1.0+)

以下函数用于调用 harness-review 的 --auto 模式：

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
```

## Solo

Use for one task. The parent session implements directly, validates, runs the
review loop, commits unless `--no-commit` is set, and marks `Plans.md`
`cc:完了 [hash]`.

## Parallel

Use for two or three independent tasks, or when `--parallel N` is explicit.
Workers may use isolated worktrees when file ownership can conflict. The Lead
still owns final integration and status updates.

## Codex

Use only when `--codex` is explicit. Delegate implementation to the Codex
companion entrypoint:

```bash
```

Validate the result locally. Codex output is not accepted until the normal
review loop approves it.

## Breezing

Use for four or more tasks, or when `--breezing` is explicit. Lead coordinates
Workers, Advisor, and Reviewer while preserving the implementation/review
boundary.

Codex-native Breezing reads this flow through `spawn_agent`, `send_input`,
`wait_agent`, and `close_agent` rather than Claude Code `Agent` /
`SendMessage` pseudo-code.

## Lane and Stage Contract

Sprint contract generation passes `spec_path`, `lane`, `stage`, and evidence
fields to Worker / Reviewer. See `skills/harness-work/SKILL.md`「Sprint
Contract」for the full field list.

### Stage gate（5 个阶段）

| stage | 目的 |
|-------|------|
| `research` | 现状调查·evidence 收集。未获取数据报告为 `unknown` |
| `plan` | 将 scope / DoD / lane freeze 到 Plans |
| `impl` | TDD Red→Green 实现。`[tdd:required]` 需要 `tdd_red_log` |
| `review` | 将 `review_artifact`（`APPROVE` / `REQUEST_CHANGES`）载入 contract |
| `closeout` | 载入 `pr_closeout`（`base_ref` / `head_ref` / evidence pack） |

### Lane: 轻量化项目 vs 必须保留项目

| lane | 可轻量化项目 | 不可省略项目 |
|------|-------------|-------------|
| `fast` | full review（可仅 major-only 或 advisory）、PR body 详情、release preflight | `spec_path`、unknown data contract（`not_observed != absent`）、focused checks（`runtime_validation` / `checks`）、`tdd_red_log` 或 `skip_tdd_reason`（`[tdd:required]` 时） |
| `gate` | —（无轻量化） | spec alignment、required 时的 TDD、major-only 或 full review、clean 为止的 re-review、`research_evidence` |
| `release` | —（无轻量化） | version/tag/GitHub Release/CI 验证、`pr_closeout` + release preflight、完整 evidence pack |

`[tdd:required]` 任务无论 lane 如何，只要 sprint contract 中没有 `tdd_red_log` 或显式 `skip_tdd_reason` 就不视为完成。

## Solo — Detailed Steps

1. Read `Plans.md`, identify the target task.
   - If `Plans.md` doesn't exist: auto-invoke `harness-plan create --ci` to generate it.
   - If the header lacks DoD / Depends columns: stop and ask for `harness-plan create` regeneration.
   - Extract undocumented requirements from recent conversation into a `cc:TODO` row (action-verb detection: 追加/修正/実装).
2. Task background confirmation (30s): infer the purpose from DoD, infer blast radius via `git grep`/`Glob`. Low confidence → ask one question; high confidence → proceed without delay.
3. **规格正本 preflight**：查找现有的项目规格 SSOT（`docs/spec/00-project-spec.md`、`docs/ARCHITECTURE.md`、`docs/HANDOFF.md`、`docs/oem/PROJECT_COMPASS.md`、`docs/specs/`）。如果任务变更产品行为/API/数据模型/权限/计费/集成/租户边界且没有规格，先创建 `docs/spec/00-project-spec.md`。如果规格过期或与任务矛盾，先更新。仅在 typo/format/dependency-bump/docs-only/behavior-preserving refactor 时跳过，记录跳过原因。将 `spec_path` 或 `spec_skip_reason` 传递到 Worker/Reviewer 上下文。
4. **Active scope + plan-time preapproval read**：preapproval 前，将 `{"phase":"<phase>","task":"<task>"}` 原子写入该任务 worktree 的 `.claude/state/active-task.json`；注册清理，在所有成功、失败或停止路径上删除它。如果存在则读取 `.claude/state/plan-preapprovals.json` 并用 `bash "${HARNESS_PLUGIN_ROOT}/scripts/plan-preapproval.sh" validate .claude/state/plan-preapprovals.json` 验证（写入架构: `templates/schemas/plan-preapproval.v2.json`；v1 仅读取兼容）。只有匹配此任务 `scope.phase`/`scope.task` 且 `decision: approved` 的条目才计为已声明。通过 `bash "${HARNESS_PLUGIN_ROOT}/scripts/plan-preapproval.sh" apply-secret-allow "$PROJECT_ROOT"` 应用已声明的 `secret-read`（在项目配置中写入 `runtimefloor.secretAllow`）。R12 可消费匹配的、未过期的、使用受限制的 v2 `external-send` 批准。它从不压制显式 R12 拒绝或任何 runtime-floor 类别。将已声明的 external-send/destructive 项目作为"plan approved"传递给 worker 简报/sprint-contract。确认仅在 plan 批准时 1 次。工作起因于已声明项目的 `AskUserQuestion` 为零。未声明的项目仍在 runtime floor / ask 停止 — 从不默默地将其加入白名单。
5. Mark the task `cc:WIP`; declare presence with `bin/harness session declare --task <task-id>` (clear with `--clear` on completion).
6. TDD phase (unless `[skip:tdd]` or no test framework): write the failing test first (Red), confirm the failure, log it with `bash "${HARNESS_PLUGIN_ROOT}/scripts/log-tdd-red.sh"` into `.claude/state/tdd-red-log/<task-id>.jsonl` (or attach literal failing output to the `worker-report`'s `self_review` evidence if the script is unavailable). `--tdd-bypass` requires `HARNESS_TDD_BYPASS=1` and `HARNESS_TDD_BYPASS_REASON="<reason>"` recorded in the sprint contract.
7. Generate `sprint-contract.json` with `node "${HARNESS_PLUGIN_ROOT}/scripts/generate-sprint-contract.js" <task-id>`.
8. Enrich with reviewer perspective (`enrich-sprint-contract.sh`) and confirm approval (`ensure-sprint-contract-ready.sh`).
9. Advisor consult (only when needed): a high-risk task (`needs-spike`/`security-sensitive`/`state-migration`) gets one consult before the first attempt; the same failure cause twice in a row triggers a consult before the third attempt; a plateau `PIVOT_REQUIRED` verdict triggers one consult before escalating. The response is `advisor-response.v1`: `PLAN` reshapes the approach, `CORRECTION` is a local fix, `STOP` escalates immediately. The same `trigger_hash` is consulted at most once; max 3 consults per task.
10. Implement via the backend-resolved executor path (Green).
11. `/simplify` for Auto-Refinement (skip with `--no-simplify`).
12. Run the automatic review stage — see [review-loop.md](review-loop.md). When `sprint-contract.json`'s `reviewer_profile` is `runtime`, also run `bash "${HARNESS_PLUGIN_ROOT}/scripts/run-contract-review-checks.sh"`.
13. Normalize the review artifact with `bash "${HARNESS_PLUGIN_ROOT}/scripts/write-review-result.sh"` (pass `--browser-result` for the browser profile; `browser_verdict == PENDING_BROWSER` keeps the static verdict).
14. `git commit` (skip with `--no-commit`).
15. Mark `cc:完了 [hash]`, clear the presence declaration, record the short commit hash.
16. Render the rich completion report — see the `Completion Report Output Contract` in the main `SKILL.md` and [completion-report.md](completion-report.md).
17. On test/CI failure after completion, see [failure-reticketing.md](failure-reticketing.md).

## Breezing — Phase Detail

```
Lead (this agent)
├── Worker (task-worker agent) — implementation
├── Advisor (claude-code-harness:advisor) — guidance
└── Reviewer (code-reviewer agent) — review
```

### Phase A — Pre-delegate

1. Read `Plans.md`, identify target tasks, and resolve execution order from the `Depends` column.
2. For each task, atomically write its phase/task to the task worktree's `.claude/state/active-task.json` before preapproval. Remove it on every task exit path.
3. Read `.claude/state/plan-preapprovals.json` if present; validate v2 or read-compatible v1 with `plan-preapproval.sh validate`.
4. Pass this task's `decision: approved` items to the worker briefing; apply declared `secret-read` via `plan-preapproval.sh apply-secret-allow "$PROJECT_ROOT"`. R12 may consume only a matching v2 `external-send` approval; explicit deny and runtime floor remain unchanged. Do not stop mid-work or ask for declared items; undeclared secret-read/external-send/destructive operations still stop on runtime floor / ask.
5. Score each task's effort tier — see [effort-routing.md](effort-routing.md).
6. Generate and enrich `sprint-contract.json` for each task (same scripts as Solo); stop if `ensure-sprint-contract-ready.sh` reports not-ready.

### Phase B — Delegate (per task, sequential in dependency order)

> API note: the pseudo-code below uses Claude Code syntax. On Codex, read `Agent(...)` as `spawn_agent(...)` and `SendMessage(...)` as `send_input(...)` — see `team-composition.md`'s API mapping table.

```
for task in execution_order:
    contract_path = generate + enrich + ensure-ready sprint-contract for task.number

    Plans.md: task.status = "cc:WIP"
    worker_result = Agent(
        subagent_type="claude-code-harness:worker",
        prompt=briefing_header + "任务: {task.内容}\nDoD: {task.DoD}\ncontract_path: {contract_path}\nmode: breezing",
        isolation="worktree",
        run_in_background=false
    )
    worker_id = worker_result.agentId

    if worker_result.type == "advisor-request.v1":
        advisor_result = Advisor(prompt=worker_result.request_json)
        worker_result = SendMessage(to=worker_id, message="advisor-response.v1: {advisor_result}")

    # self_review gate (Lead checks mechanically before spawning Reviewer)
    self_review_failures = 0
    MAX_SELF_REVIEW_RETRIES = 2  # a 3rd failure escalates
    while True:
        unverified = [r for r in worker_result.self_review if not r.get("verified") or not r.get("evidence")]
        if not unverified:
            break
        self_review_failures += 1
        if self_review_failures > MAX_SELF_REVIEW_RETRIES:
            Plans.md: task.status = "cc:TODO"
            raise EscalationError(f"self_review unresolved after 3 return trips: {[u['rule'] for u in unverified]}")
        SendMessage(to=worker_id, message=f"self_review 中有未确认 rule: {[u['rule'] for u in unverified]}。请用实际命令输出或 literal 测试结果填充 evidence，verified=true 后 amend")
        worker_result = wait_for_response(worker_id)

    # review (使用 harness-review --auto 模式)
    # 职责分离：harness-work 专注于任务实现，harness-review 专注于代码审查
    verdict_result = call_harness_review_auto(
        base_ref=BASE_REF,
        worktree_path=worker_result.worktreePath,
        mode="strict"
    )

    if not verdict_result["success"]:
        # 如果自动审查失败，降级到基础检查
        logger.warning(f"harness-review --auto 失败: {verdict_result.get('error')}")
        verdict = "REQUEST_CHANGES"  # 保守策略
        findings = []
    else:
        verdict = verdict_result["result"]["verdict"]
        findings = verdict_result["result"].get("findings", [])

    # 支持 reviewer_profile 的额外检查
    profile = jq(contract_path, ".review.reviewer_profile")
    if profile == "runtime":
        review_input = run-contract-review-checks.sh output
        # 合并 runtime 检查结果
        runtime_findings = parse_review_check_output(review_input)
        findings.extend(runtime_findings)
        # 重新计算 verdict
        if any(f["severity"] in ["critical", "major"] for f in findings):
            verdict = "REQUEST_CHANGES"
    if profile == "browser":
        browser artifact -> browser-review-runner.sh
        browser_verdict = parse_browser_review_result()
        if browser_verdict in ["REQUEST_CHANGES", "APPROVE"]:
            verdict = browser_verdict  # 覆盖静态判定
        elif browser_verdict == "PENDING_BROWSER":
            verdict = "REQUEST_CHANGES"  # 保守处理

    write-review-result.sh normalizes the artifact

    review_count = 0
    MAX_REVIEWS = read_contract(contract_path, ".review.max_iterations") or 3
    latest_commit = worker_result.commit
    while verdict == "REQUEST_CHANGES" and review_count < MAX_REVIEWS:
        SendMessage(to=worker_id, message="指出的问题: {issues}\n请修正后 amend 提交")
        updated_result = wait_for_response(worker_id)
        latest_commit = updated_result.commit

        # 重新调用 harness-review --auto 进行审查
        verdict_result = call_harness_review_auto(
            base_ref=BASE_REF,
            worktree_path=worker_result.worktreePath,
            mode="strict"
        )

        if verdict_result["success"]:
            verdict = verdict_result["result"]["verdict"]
            findings = verdict_result["result"].get("findings", [])
        else:
            # 如果自动审查失败，保守处理
            logger.warning(f"harness-review --auto 重试失败: {verdict_result.get('error')}")
            verdict = "REQUEST_CHANGES"
            findings = []

        review_count++

    if verdict == "APPROVE":
        # 🔥 端到端检测环节 (Task 12.4 - 自动触发机制集成)
        e2e_result = run_e2e_detection(
            worktree_path=worker_result.worktreePath,
            base_ref=BASE_REF,
            task_context={"task": task, "worker_result": worker_result, "contract_path": contract_path}
        )

        if e2e_result["status"] == "PASS":
            # 检测通过，继续正常流程
            cherry-pick latest_commit onto trunk (如果已是祖先则跳过 — 重入保护)
            remove the worker's worktree; delete the feature branch
            Plans.md: task.status = "cc:完了 [{hash}]"
            bash "${HARNESS_PLUGIN_ROOT}/scripts/auto-checkpoint.sh" "${task.number}" "${HASH}" "${contract_path}" "${REVIEW_RESULT_PATH}" || true  # fail-open

        elif e2e_result["status"] == "FAIL":
            # 🔥 检测失败，直接回到 harness-work 继续修改
            logger.info(f"端到端检测未通过: {e2e_result.get('critical_issues', []).length} 个关键问题")
            logger.info(f"将任务交回 harness-work 继续修改，保留当前工作树状态")

            # 📋 保存端到端检测结果到工作树状态文件
            e2e_detection_state = {
                "status": "FAIL",
                "detection_id": e2e_result.get("detection_id"),
                "critical_issues": e2e_result.get("critical_issues", []),
                "fix_suggestions": e2e_result.get("fix_suggestions", []),
                "timestamp": e2e_result.get("timestamp"),
                "worker_result": {
                    "commit": worker_result.commit,
                    "worktreePath": worker_result.worktreePath,
                    "branch": worker_result.branch
                }
            }

            # 保存状态供后续使用
            state_file = os.path.join(worker_result.worktreePath, ".claude", "state", "e2e-detection", "failure.json")
            os.makedirs(os.path.dirname(state_file), exist_ok=True)
            with open(state_file, 'w') as f:
                json.dump(e2e_detection_state, f, indent=2)

            # 🔥 关键改进：直接回到 harness-work 继续修改
            # 不在这里实现修复循环，而是将控制权交还给 harness-work
            # harness-work 会处理：
            # 1. 告知 Worker 问题
            # 2. Worker 进行修改
            # 3. 重新进入审查+检测流程

            # 设置任务状态为需要修改
            Plans.md: task.status = "cc:WIP [端到端检测失败]"
            Plans.md: task.context = f"端到端检测失败: {len(e2e_result.get('critical_issues', []))}个关键问题需修复"

            # 保留工作树不删除，Worker 可以继续使用
            # 不执行 cherry-pick，不删除分支
            # 下一次 harness-work 执行时，会继续这个任务

            escalate_to_worker(e2e_result, worker_result)

        elif e2e_result["status"] == "SKIPPED":
            # 检测被跳过，继续正常流程（配置禁用或其他原因）
            logger.info(f"端到端检测被跳过: {e2e_result.get('reason', 'Unknown reason')}")
            cherry-pick latest_commit onto trunk (如果已是祖先则跳过 — 重入保护)
            remove the worker's worktree; delete the feature branch
            Plans.md: task.status = "cc:完了 [{hash}]"
            bash "${HARNESS_PLUGIN_ROOT}/scripts/auto-checkpoint.sh" "${task.number}" "${HASH}" "${contract_path}" "${REVIEW_RESULT_PATH}" || true  # fail-open

        else:
            # 检测出错，保守处理
            logger.error(f"端到端检测出错: {e2e_result.get('error', 'Unknown error')}")
            # 根据配置决定是否继续，默认升级到用户
            escalate_e2e_error(e2e_result)
    else:
        escalate to the user

    print("📊 Progress: Task {completed}/{total} 完成 — {task.内容}")
```

### Phase C — Post-delegate

1. Aggregate the commit log for all tasks.
2. Render the rich completion report (Breezing template in [completion-report.md](completion-report.md)).
3. Confirm every task in `Plans.md` reached `cc:完了`.

---

## 端到端检测配置加载 (Task 12.4+)

### 配置文件优先级

端到端检测系统支持多层级配置，优先级从高到低：

1. **harness.toml** - 项目级配置（推荐）
2. **.claude/config/e2e-detection.config.json** - 用户配置
3. **config/e2e-detection.default.config.json** - 默认配置
4. **内置默认值** - 系统内置配置

### 配置加载函数

```python
def load_e2e_config(project_root: str) -> dict:
    """
    加载端到端检测配置

    Args:
        project_root: 项目根目录

    Returns:
        合并后的配置字典
    """
    import toml
    import json
    import os

    # 1. 从 harness.toml 加载（最高优先级）
    config = get_default_e2e_config()

    harness_toml = os.path.join(project_root, "harness.toml")
    if os.path.exists(harness_toml):
        try:
            with open(harness_toml, 'r', encoding='utf-8') as f:
                toml_config = toml.load(f)

            if "e2e_detection" in toml_config:
                # 合并 harness.toml 中的配置
                config = merge_config(config, toml_config["e2e_detection"])
                logger.info("从 harness.toml 加载端到端检测配置")
        except Exception as e:
            logger.warning(f"加载 harness.toml 失败: {e}")

    # 2. 从 JSON 配置加载
    json_config = os.path.join(project_root, ".claude", "config", "e2e-detection.config.json")
    if os.path.exists(json_config):
        try:
            with open(json_config, 'r', encoding='utf-8') as f:
                json_config_data = json.load(f)

            # JSON 配置覆盖 harness.toml
            config = merge_config(config, json_config_data)
            logger.info("从 JSON 配置加载端到端检测配置")
        except Exception as e:
            logger.warning(f"加载 JSON 配置失败: {e}")

    # 3. 应用环境变量覆盖
    config = apply_env_overrides(config)

    return config

def merge_config(base_config: dict, override_config: dict) -> dict:
    """
    深度合并配置

    Args:
        base_config: 基础配置
        override_config: 覆盖配置

    Returns:
        合并后的配置
    """
    import copy

    result = copy.deepcopy(base_config)

    for key, value in override_config.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = merge_config(result[key], value)
        else:
            result[key] = copy.deepcopy(value)

    return result

def get_default_e2e_config() -> dict:
    """
    获取默认端到端检测配置

    Returns:
        默认配置字典
    """
    return {
        "enabled": True,
        "mode": "strict",
        "timeout": 120,
        "retry_on_failure": True,
        "max_retries": 3,

        "auto_fix": {
            "enabled": True,
            "max_iterations": 3,
            "fix_timeout": 60,
            "commit_on_fix": True,
            "types": {
                "dependency_update": True,
                "gitignore_add": True,
                "code_fix": False,
                "test_generation": False
            }
        },

        "test_types": {
            "frontend": {
                "enabled": True,
                "framework": "auto",
                "playwright_config": {
                    "timeout": 30000,
                    "headless": True,
                    "browsers": ["chromium", "firefox", "webkit"],
                    "retries": 1
                }
            },
            "backend": {
                "enabled": True,
                "framework": "auto"
            },
            "integration": {
                "enabled": True,
                "test_scenarios": ["user_login", "data_flow", "error_handling"]
            },
            "performance": {
                "enabled": False,
                "response_time_max": 2000
            },
            "security": {
                "enabled": True,
                "scan_vulnerabilities": True,
                "check_dependencies": True
            }
        },

        "thresholds": {
            "response_time": {
                "warning": 1500,
                "critical": 2000
            },
            "memory_usage": {
                "warning": 0.7,
                "critical": 0.8
            }
        },

        "triggers": {
            "auto_trigger_on_review_pass": True,
            "require_clean_workspace": True,
            "branch_patterns": {
                "include": ["feature/*", "bugfix/*", "hotfix/*"],
                "exclude": ["draft/*", "wip/*"]
            }
        }
    }

def apply_env_overrides(config: dict) -> dict:
    """
    应用环境变量覆盖

    Args:
        config: 当前配置

    Returns:
        应用环境变量后的配置
    """
    import os

    env_mappings = {
        "HARNESS_E2E_ENABLED": ("enabled", bool),
        "HARNESS_E2E_MODE": ("mode", str),
        "HARNESS_E2E_TIMEOUT": ("timeout", int),
        "HARNESS_E2E_MAX_RETRIES": ("max_retries", int),
        "HARNESS_E2E_AUTO_FIX": ("auto_fix.enabled", bool),
        "HARNESS_E2E_FRONTEND": ("test_types.frontend.enabled", bool),
        "HARNESS_E2E_BACKEND": ("test_types.backend.enabled", bool),
        "HARNESS_E2E_INTEGRATION": ("test_types.integration.enabled", bool),
        "HARNESS_E2E_PERFORMANCE": ("test_types.performance.enabled", bool),
        "HARNESS_E2E_SECURITY": ("test_types.security.enabled", bool)
    }

    for env_var, (config_path, value_type) in env_mappings.items():
        env_value = os.environ.get(env_var)
        if env_value is not None:
            # 设置嵌套配置值
            keys = config_path.split('.')
            current = config

            for key in keys[:-1]:
                if key not in current:
                    current[key] = {}
                current = current[key]

            # 类型转换
            try:
                if value_type == bool:
                    current[keys[-1]] = env_value.lower() in ('true', '1', 'yes', 'on')
                else:
                    current[keys[-1]] = value_type(env_value)
            except (ValueError, TypeError):
                logger.warning(f"环境变量 {env_var} 值无效: {env_value}")

    return config
```

---

## 端到端检测集成说明 (Task 12.4+ 优化版本)

### 🎯 优化后的流程架构

```
审查通过 → 端到端检测 → 结果处理
                         ├─ PASS → cherry-pick → 完成
                         ├─ FAIL → 交回 harness-work 继续修改
                         ├─ SKIPPED → 继续（配置禁用）
                         └─ ERROR → 升级到用户
```

### 🔄 harness-work 自动修复循环

当端到端检测失败时，任务会自动回到 harness-work 继续修改：

1. **第一次检测失败** → 交回 harness-work，任务保持 WIP 状态
2. **Worker 修复** → Worker 根据问题报告进行修改
3. **重新审查** → 修改完成后重新进入审查流程
4. **重新检测** → 审查通过后再次端到端检测
5. **循环** → 直到检测通过或达到最大重试次数

### 📋 任务状态管理

端到端检测过程中的任务状态：

- `cc:WIP [端到端检测失败]` - 检测失败，等待修复
- `cc:WIP` - Worker 正在修复问题
- `cc:WIP [重新检测]` - 准备重新端到端检测
- `cc:完了 [hash]` - 检测通过，任务完成

### 🛠️ 自动修复机制

虽然主要修复由 Worker 完成，但系统仍提供自动修复支持：

- **依赖更新** - 自动更新 npm packages
- **敏感文件保护** - 自动添加到 .gitignore
- **配置修复** - 修复简单的配置问题

### 🎨 配置管理优化

#### 使用 harness.toml（推荐）

```toml
# harness.toml
[e2e_detection]
enabled = true
mode = "strict"
timeout = 120

[e2e_detection.test_types.frontend]
enabled = true
framework = "playwright"

[e2e_detection.test_types.frontend.playwright]
timeout = 30000
browsers = ["chromium", "firefox", "webkit"]
```

#### 使用 JSON 配置（兼容）

```json
// .claude/config/e2e-detection.config.json
{
  "enabled": true,
  "mode": "strict",
  "test_types": {
    "frontend": {
      "enabled": true,
      "framework": "playwright"
    }
  }
}
```

#### 使用环境变量（临时）

```bash
export HARNESS_E2E_ENABLED=true
export HARNESS_E2E_MODE=strict
export HARNESS_E2E_FRONTEND=true
```

### 🚀 关键优势

**架构简洁性**：
- ✅ 端到端检测专注于检测，不负责修复
- ✅ harness-work 作为主要协调者处理修复循环
- ✅ Worker 专注代码修改，保持单一职责

**流程清晰性**：
- ✅ 检测失败 → 明确回到 harness-work
- ✅ 修复后重新进入完整流程（审查+检测）
- ✅ 避免复杂的嵌套循环和状态管理

**配置灵活性**：
- ✅ 支持 harness.toml、JSON、环境变量
- ✅ 多层级配置合并
- ✅ 项目级和用户级配置分离

**错误处理**：
- ✅ 明确的失败升级路径
- ✅ 详细的问题报告和修复建议
- ✅ 保留工作树状态供继续使用

---

## 端到端检测集成函数 (Task 12.4 - 优化版本)

以下函数用于端到端检测的自动触发和修复循环：

### `run_e2e_detection()`

```python
def run_e2e_detection(worktree_path: str, base_ref: str, task_context: dict) -> dict:
    """
    运行端到端检测

    Args:
        worktree_path: 工作树路径
        base_ref: 基准 commit SHA 或分支名
        task_context: 任务上下文信息

    Returns:
        包含检测结果的字典，包含字段：
        - status: "PASS" | "FAIL" | "SKIPPED" | "ERROR"
        - detection_id: 检测ID
        - timestamp: 检测时间戳
        - test_results: 各类型测试结果
        - critical_issues: 关键问题列表
        - execution_time: 执行时长（秒）
    """
    import subprocess
    import json
    import os

    e2e_manager_script = os.path.join(
        HARNESS_PLUGIN_ROOT,
        "scripts", "e2e-detection", "e2e-detection-manager.js"
    )

    try:
        result = subprocess.run(
            ["node", e2e_manager_script, worktree_path, base_ref],
            capture_output=True,
            text=True,
            timeout=300  # 5分钟超时
        )

        if result.returncode == 0:
            # 尝试从输出中解析JSON结果
            try:
                detection_result = json.loads(result.stdout)
                return detection_result
            except json.JSONDecodeError:
                # 如果输出不是JSON，从文件中读取
                result_file = os.path.join(worktree_path, ".claude", "state", "e2e-detection", "latest-result.json")
                if os.path.exists(result_file):
                    with open(result_file, 'r') as f:
                        return json.load(f)
                else:
                    return {
                        "status": "ERROR",
                        "error": "无法解析检测结果",
                        "stdout": result.stdout,
                        "stderr": result.stderr
                    }
        else:
            return {
                "status": "ERROR",
                "error": result.stderr or "检测执行失败",
                "returncode": result.returncode
            }

    except subprocess.TimeoutExpired:
        return {
            "status": "ERROR",
            "error": "检测超时"
        }
    except Exception as e:
        return {
            "status": "ERROR",
            "error": str(e)
        }
```

### `run_auto_fix_loop()`

```python
def run_auto_fix_loop(detection_result: dict, worktree_path: str, worker_id: str, max_iterations: int = 3) -> dict:
    """
    运行自动修复循环

    Args:
        detection_result: 端到端检测结果
        worktree_path: 工作树路径
        worker_id: Worker ID（用于重新审查）
        max_iterations: 最大迭代次数

    Returns:
        包含修复结果的字典：
        - success: bool - 是否修复成功
        - iterations: int - 实际迭代次数
        - fixes_applied: int - 应用的修复数量
        - final_result: dict - 最终检测结果
        - escalate_to_user: bool - 是否需要升级到用户
    """
    import subprocess
    import json
    import os

    # 如果检测已通过，无需修复
    if detection_result.get("status") == "PASS":
        return {
            "success": True,
            "iterations": 0,
            "reason": "检测已通过，无需修复"
        }

    # 如果没有关键问题，无需修复
    critical_issues = detection_result.get("critical_issues", [])
    if not critical_issues:
        return {
            "success": True,
            "iterations": 0,
            "reason": "无关键问题需要修复"
        }

    auto_fix_script = os.path.join(
        HARNESS_PLUGIN_ROOT,
        "scripts", "e2e-detection", "auto-fix-controller.js"
    )

    try:
        # 生成临时分析文件
        analysis_result = {
            "overall_status": "FAIL",
            "critical_issues": critical_issues,
            "detection_id": detection_result.get("detection_id"),
            "fix_suggestions": detection_result.get("fix_suggestions", [])
        }

        analysis_file = os.path.join(worktree_path, ".claude", "state", "e2e-detection", "analysis.json")
        with open(analysis_file, 'w') as f:
            json.dump(analysis_result, f)

        # 调用自动修复控制器
        result = subprocess.run(
            ["node", auto_fix_script, analysis_file, worktree_path],
            capture_output=True,
            text=True,
            timeout=180  # 3分钟超时
        )

        if result.returncode == 0:
            try:
                fix_result = json.loads(result.stdout)
                return fix_result
            except json.JSONDecodeError:
                return {
                    "success": False,
                    "error": "无法解析修复结果",
                    "stdout": result.stdout,
                    "stderr": result.stderr
                }
        else:
            return {
                "success": False,
                "error": result.stderr or "修复执行失败"
            }

    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error": "修复超时",
            "escalate_to_user": True
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "escalate_to_user": True
        }
```

### `escalate_e2e_failure()`

```python
def escalate_e2e_failure(e2e_result: dict, fix_result: dict):
    """
    升级端到端检测失败到用户

    Args:
        e2e_result: 端到端检测结果
        fix_result: 自动修复结果
    """
    import subprocess
    import os

    escalation_script = os.path.join(
        HARNESS_PLUGIN_ROOT,
        "scripts", "e2e-detection", "escalate-failure.sh"
    )

    # 如果有升级脚本，调用它
    if os.path.exists(escalation_script):
        subprocess.run(
            [escalation_script, json.dumps(e2e_result), json.dumps(fix_result)],
            capture_output=True
        )

    # 记录到日志
    logger.error(f"端到端检测失败，需要用户介入:")
    logger.error(f"检测状态: {e2e_result.get('status')}")
    logger.error(f"关键问题: {len(e2e_result.get('critical_issues', []))}")

    if fix_result.get("escalate_to_user"):
        logger.error(f"修复失败原因: {fix_result.get('reason', 'Unknown')}")

    # 抛出异常或返回错误，让上层处理
    raise Exception(f"端到端检测失败: {e2e_result.get('status')}, 需要")
```

### `escalate_review_failure()`

```python
def escalate_review_failure(verdict_result: dict):
    """
    升级审查失败到用户

    Args:
        verdict_result: 审查结果
    """
    logger.error(f"代码审查失败，需要用户介入:")
    logger.error(f"审查结果: {verdict_result.get('result', {}).get('verdict')}")

    raise Exception(f"代码审查失败: {verdict_result.get('error', 'Unknown error')}, ")
```

### `escalate_to_worker()`

```python
def escalate_to_worker(e2e_result: dict, worker_result: dict):
    """
    升级端到端检测失败到 Worker 继续修改

    Args:
        e2e_result: 端到端检测结果
        worker_result: Worker 结果
    """
    logger.error(f"🔥 端到端检测失败，任务交回 Worker 继续修改")
    logger.error(f"📋 检测ID: {e2e_result.get('detection_id')}")
    logger.error(f"❌ 关键问题数: {len(e2e_result.get('critical_issues', []))}")

    # 如果有 worker_id，发送消息告知问题
    if worker_id:
        failure_message = f"""
🔥 端到端检测失败，请修复以下问题：

检测ID: {e2e_result.get('detection_id')}
关键问题: {len(e2e_result.get('critical_issues', []))} 个

问题详情:
"""

        for i, issue in enumerate(e2e_result.get('critical_issues', [])[:5], 1):
            failure_message += f"{i}. [{issue.get('test_type', 'unknown')}] {issue.get('description', 'Unknown error')}\n"
            failure_message += f"   文件: {issue.get('file', 'unknown')}\n"
            if issue.get('suggestion'):
                failure_message += f"   建议: {issue.get('suggestion')}\n"
            failure_message += "\n"

        if len(e2e_result.get('critical_issues', [])) > 5:
            failure_message += f"... 还有 {len(e2e_result.get('critical_issues', [])) - 5} 个问题\n"

        failure_message += f"""
请在工作树中修复这些问题后，提交修改：
  git add -A
  git commit -m "fix: 修复端到端检测问题"

修复后任务将重新进入审查+检测流程。
工作树路径: {worker_result.worktreePath}
"""

        SendMessage(to=worker_id, message=failure_message)

    # 保存失败状态到任务上下文
    task_context = {
        "e2e_detection_failure": {
            "detection_id": e2e_result.get("detection_id"),
            "status": "FAILED",
            "critical_issues": e2e_result.get("critical_issues", []),
            "worker_result": {
                "commit": worker_result.get("commit"),
                "worktreePath": worker_result.get("worktreePath"),
                "branch": worker_result.get("branch")
            }
        }
    }

    # 抛出异常，让上层处理
    raise Exception(f"端到端检测失败: {len(e2e_result.get('critical_issues', []))} 个关键问题需要修复")
```

### `escalate_e2e_error()`

```python
def escalate_e2e_error(e2e_result: dict):
    """
    升级端到端检测错误到用户

    Args:
        e2e_result: 端到端检测结果（ERROR状态）
    """
    logger.error(f"端到端检测出错，需要用户介入:")
    logger.error(f"错误信息: {e2e_result.get('error', 'Unknown error')}")

    raise Exception(f"端到端检测出错: {e2e_result.get('error', 'Unknown error')}, 需要用户检查配置和环境")
```

---

## 端到端检测集成说明 (Task 12.4)

### 集成点

端到端检测已集成到 **Breezing 模式的 Phase B - 审查循环** 中，插入点为：

- **位置**: 审查通过后（`verdict == "APPROVE"`），cherry-pick操作前
- **触发**: 自动触发，无需人工干预
- **阻塞**: 是（检测失败会阻塞完成流程）

### 处理流程

1. **PASS**: 直接继续正常流程
2. **FAIL**: 进入自动修复循环
   - 尝试自动修复（最多3次迭代）
   - 修复成功后重新审查
   - 重新审查通过后重新检测
   - 最终失败则升级到用户
3. **SKIPPED**: 跳过检测，继续正常流程
4. **ERROR**: 检测出错，根据配置决定是否继续

### 配置支持

端到端检测的启用/禁用和参数通过配置文件控制：

- **配置文件**: `.claude/config/e2e-detection.config.json`
- **默认配置**: `config/e2e-detection.default.config.json`
- **设置脚本**: `bash scripts/e2e-detection/setup-e2e-detection.sh`

### 重试机制

自动修复循环的参数：

- **最大迭代次数**: 3次（可配置）
- **每次迭代**: 修复 → 重新审查 → 重新检测
- **达到上限**: 升级到用户处理

### 错误处理

所有失败场景都会升级到用户：

- 端到端检测失败
- 自动修复失败
- 重新审查失败
- 检测系统错误

用户可以根据升级报告中的问题描述和修复建议进行手动处理。
