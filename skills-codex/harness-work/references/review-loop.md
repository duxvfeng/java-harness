# Codex Review Loop

Codex 审查遵循与 Claude 侧 `harness-work` 相同的 verdict 契约。

## 顺序

1. 在可用时运行 companion 结构化审查。
2. 运行 AI Residuals JSON 扫描:

```bash
bash "${HARNESS_PLUGIN_ROOT}/scripts/review-ai-residuals.sh" --base-ref "${BASE_REF}" --include-untracked
```

3. 仅在 companion 审查不可用时回退到只读审查者代理。

## Verdict 阈值

`critical` 或 `major` 意味着 `REQUEST_CHANGES`。`minor` 和 `recommendation` 不
影响批准。

## Worker 修正

当生成的 Worker 需要变更时，恢复它并使用 `send_input` 仅传递
critical/major 发现。然后再次等待并重新运行审查。
