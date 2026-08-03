# Harness Setup Reference: init

This file is part of `${CLAUDE_SKILL_DIR}/references/` for `harness-setup`.

## 子命令详细说明

### init — 项目初始化

在新项目中引入 Harness。

**生成的文件**:
```
project/
├── CLAUDE.md            # 项目配置
├── Plans.md             # 任务管理（空模板）
├── .claude/
│   ├── settings.json    # Claude Code 配置
│   └── hooks.json       # hook 配置（Go 二进制文件）
└── hooks/
    ├── pre-tool.sh      # 薄垫片（→ core/src/index.ts）
    └── post-tool.sh     # 薄垫片（→ core/src/index.ts）
```

**流程**:
1. 检测项目类型（Node.js/Python/Go/Rust/其他）
2. 生成最小化的 CLAUDE.md
3. 生成 Plans.md 模板
4. 配置 hooks.json
5. **Go 二进制验证**: 通过 `harness version` 确认二进制文件可用（v4.0 以后不需要 Node.js）
6. **插件文件同步**: 通过 `harness sync` 同步 `.claude-plugin/` 下的文件到最新版本
7. **健康检查**: 通过 `harness doctor` 通过所有检查项，如有问题则提供修复方案

### Go 二进制验证

```bash
# 确认二进制文件存在和运行正常
harness version
# 例: harness v4.0.0 (go1.22.0, darwin/arm64)
```

v4.0 以后，Harness 的核心引擎已迁移到 Go 二进制文件。
不需要 Node.js。二进制文件使用 `bin/harness`（或 PATH 上的 `harness`）。

### 插件文件同步

```bash
# 同步 .claude-plugin/ 下的文件到最新版本
harness sync

# 仅检查同步内容（不更改）
harness sync --dry-run
```

`harness sync` 将 skills/ 的单一真实来源（SSOT）的更改传播到各个镜像（codex/.codex/skills/、opencode/skills/）。
init 后必须执行此命令。

### 健康检查

```bash
# 运行所有检查项
harness doctor
```

`harness doctor` 检查以下内容:

| 检查项 | 内容 |
|------------|------|
| 二进制文件 | `harness version` 是否正常返回 |
| 插件配置 | `.claude-plugin/plugin.json` 格式是否正确 |
| hooks 配置 | hooks 是否存在于正确的路径 |
| 镜像同步 | skills/ 和镜像的内容是否一致 |
| CLAUDE.md | 是否存在必需的章节 |

检测到问题时会提供修复命令。

