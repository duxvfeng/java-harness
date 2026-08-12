# Code Review Flow

## 一句话总结

收集差异，查看实现、规格、Plans、退化、测试，只阻止应该阻止的问题。

## Step 1: collect diff

确认事项:

```bash
git status --short
git diff --stat "${BASE_REF:-HEAD}"
git diff "${BASE_REF:-HEAD}"
git ls-files --others --exclude-standard
```

untracked files 不会出现在 `git diff` 中。
必须包含在 scope 中。

## Step 2: static scans

AI Residuals:

```bash
bash scripts/review-ai-residuals.sh --base "${BASE_REF:-HEAD}"
bash scripts/review-weak-supervision-report.sh
```

候補:

- `mockData`
- `dummy`
- `fake`
- `localhost`
- `TODO`
- `FIXME`
- `it.skip`
- `describe.skip`
- `test.skip`
- `expect(true).toBe(true)`

仅发现候补不视为 major。
在 diff 语境中根据"是否直接导致出库事故或错误配置"来判定 severity。
但判定为 minor 的也不默默认丢弃，而是作为观察记录（参见下文的 Finding coverage）。

## Step 3: eight review lenses

| 视点 | 查看内容 |
|---|---|
| Security | SQL injection, cross-site scripting, secret leak, permission bypass |
| Performance | N+1, needless heavy IO, blocking work |
| Quality | duplicate logic, unclear boundary, fragile parsing |
| Accessibility | labels, focus, contrast, keyboard path |
| AI Residuals | fake success, skipped tests, mock-only implementation |
| Spec Alignment | root `spec.md` product contract 与 sub-spec (`spec_path`) 之间的矛盾 |
| Plans Alignment | `Plans.md` 的 task / DoD / Depends / `[lane:*]` / stage gate 的一致性 |
| Regression Safety | 既存行为·mirror·CLI/skill UX 的退化 |

## TDD compliance

`[tdd:required]` 任务需要确认 `tdd_red_log`、literal failing test output 或显式 `skip_tdd_reason`。
像 docs-only 或 refactor-only 这样 TDD 过度的情况下，记录 `[tdd:skip:<reason>]` 即可。
没有证据不 `APPROVE`。

## Unknown data contract

`not_observed != absent` — 不将未观测数据断定为"不存在""没问题"。
file / API / CI / memory / fixture 看不到时报告为 `unknown` / `not observed`。

## Evidence pack

`APPROVE` 前确认 evidence pack：accepted findings、rejected findings、focused tests、`release-preflight` warnings 的处理方针、residual risk。

## Finding coverage（Opus 4.8）

区分 finding 阶段和 verdict 阶段。

- finding 阶段 **优先网罗**。包括确信度低的指出和 minor，发现的 issue 都要附带 severity 和确信度记录（保留在 `review-result.v1` 的 `observations[]` / `recommendations[]` 中）。
- 只在 verdict 阶段 gate（critical / major 时 `REQUEST_CHANGES`、仅 minor 时 `APPROVE`）。
- "是否直接导致出库事故或错误配置"是 **severity 的判定**，而不是**是否记录的判定**。即使判断为 minor 也不默默认丢弃。

Opus 4.8 忠实地遵守"不要报告 low-severity"，即使调查也会缩小报告范围导致 recall 下降。
缩小 finding 是 verdict 阶段的职责，调查阶段不丢弃 findings。

## Verdict

1. 有 critical / major → `REQUEST_CHANGES`
2. root `spec.md` / `Plans.md` lane-stage / 退化 gate 为 fail → `REQUEST_CHANGES`
3. TDD evidence 缺失、断定 unknown data、evidence pack 为空 → `REQUEST_CHANGES`
4. 需要意思决定 → `decision_needed`
5. 仅有 minor / recommendation → `APPROVE`
6. 证据不足 → `REQUEST_CHANGES` 或 `decision_needed`

## 修正后再审查

`REQUEST_CHANGES` 后必须进行修正后再审查。
连续 2 次遗漏相同 issue 时强制 TeamAgent Debate。

## Quality Quadrants (Q1-Q4)

Every review finding must carry one primary quality quadrant. Add an optional secondary quadrant only when the same finding has a distinct, material effect in another dimension.

| Quadrant | Meaning | Typical evidence |
|---|---|---|
| Q1 技术支撑 | The implementation can technically support the contract. | Code, component, adapter, schema, integration, or build failure |
| Q2 业务验收 | The requested behavior passes explicit acceptance criteria. | Given/When/Then result, acceptance test, workflow output |
| Q3 业务评价 | Real users can use the result effectively and it fits the business context. | User feedback, usability observation, domain fit, operational workflow |
| Q4 技术评价 | Non-functional technical quality is sufficient for operation and change. | Performance, security, reliability, observability, maintainability |

Use the quadrant as a finding label, not as a replacement for severity or confidence:

```text
[Q1][major] Adapter rejects the accepted evidence.v1 field shape.
[Q2][minor] Given a valid plan, When execution starts, Then the handoff is not visible.
[Q3][recommendation] The review summary is difficult for the target operator to act on.
[Q4][major] The feed write path has no bounded handling for malformed records.
```

If a finding spans quadrants, keep one primary label and state the secondary impact explicitly, for example `[Q1][Q4]`. The review verdict still follows the existing severity, evidence, and gate rules.
