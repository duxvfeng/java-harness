# Quick / Codex Closeout

## 一句话总结

小的变更是固定目标，用实际代码确认 Codex 的建议，clean 就在那里停止。

## target selection decision tree

1. working tree 为 dirty
   - 推荐: 仅未提交变更
   - base: `HEAD`
   - 包含 untracked
2. PR branch / feature branch 有 commits
   - 推荐: `upstream..HEAD` 或 `origin/main..HEAD`
   - working tree 也为 dirty 则用 AskUserQuestion 选择"仅未提交变更 / 全部 / 仅 commit"
3. clean tree 且没有 branch 差分
   - 推荐: 最近 1 commit
   - 必要时最近 5 commits
4. 用户指定 `--base` / `--commit`
   - 优先显式指定

## Advisory rule

Codex 的指出内容是 advisory。
即"参考意见"，不是事实本身。

必须执行以下操作。

- 用实际代码阅读指出的问题
- 用 diff 和测试确认可再现性
- 分为 accepted findings / rejected findings
- 在 rejected 中写入"为何不采用"

## Stop-on-clean

stop-on-clean:
出现 clean result 后，不为外观进行额外的 review。

例:

- Codex review: no major issues
- focused tests: pass
- manual spot check: pass

如果处于这种状态则停止。
额外的重度 review 仅在 release 前、security-sensitive、规格正本变更或用户明确指示时进行。

## Helper contract

`scripts/harness-review-closeout.sh` 是固定 lightweight closeout 执行计划的 helper。

对应的输入：

- `--dry-run`
- `--parallel-tests`
- `--base REF`
- `--commit REF`
- `--uncommitted`
- `--test CMD`
- `--json`

例:

```bash
bash scripts/harness-review-closeout.sh --dry-run --uncommitted
bash scripts/harness-review-closeout.sh --base origin/main --parallel-tests --test "bash tests/test-harness-review-governance.sh"
bash scripts/harness-review-closeout.sh --commit HEAD --json
```

Codex 不可用时:

- fallback 到 full manual pass
- 不将 failure 视为 success
- 在 final report 中保留 `codex_available:false`

## Final report

必須項目:

- review command
- tests
- accepted findings
- rejected findings
- clean result
- fallback reason

JSON 中至少保留以下内容。

```json
{
  "schema_version": "harness-review-closeout.v1",
  "target": "working_tree | branch_range | commit",
  "base_ref": "HEAD",
  "tests": [],
  "accepted_findings": [],
  "rejected_findings": [],
  "clean_result": true,
  "codex_available": true,
  "fallback": ""
}
```
