# Harness Setup Reference: ci

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

### ci — CI/CD 配置

配置 GitHub Actions 工作流程。

```yaml
# .github/workflows/ci.yml 生成示例
name: CI
on:
  push:
    branches: [main]
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm test
```

