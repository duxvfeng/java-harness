# Harness Setup Reference: harness-mem

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

### harness-mem — 内存配置

配置统一的 Harness Memory。

```bash
# 创建内存目录
mkdir -p .claude/agent-memory/claude-code-harness-worker
mkdir -p .claude/agent-memory/claude-code-harness-reviewer

# 配置 MEMORY.md 模板
cat > .claude/agent-memory/claude-code-harness-worker/MEMORY.md << 'EOF'
# Worker Agent Memory

## Project Context
[项目概要]

## Patterns
[学习模式]
EOF
```

