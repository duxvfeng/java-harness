# Harness Setup Reference: mirrors-agents-localize

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

### mirrors — 公开 skill bundle 同步

在 Windows 的 `core.symlinks=false` 环境下，repository 符号链接会变成普通文件，`harness-*` skill 可能不会出现在命令列表中。公开 bundle 作为实际目录 mirror 同步。

```bash
./scripts/sync-skill-mirrors.sh
./scripts/sync-skill-mirrors.sh --check
```

更新目标:

- `skills/`
- `codex/.codex/skills/`
- `opencode/skills/`

### agents — 代理配置

配置 agents/ 的三代理结构。

```
agents/
├── worker.md      # 实现负责（task-worker + codex-implementer + error-recovery）
└── reviewer.md    # 审查负责（code-reviewer + plan-critic）
```

### localize — 规则本地化

使 `.claude/rules/` 的规则适应当前项目。

```bash
# 确认规则列表
ls .claude/rules/

# 添加项目固有规则
cat >> .claude/rules/project-rules.md << 'EOF'
# Project-Specific Rules
[项目固有规则]
EOF
```

