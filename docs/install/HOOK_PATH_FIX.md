# Hook 路径修复指南

## 问题描述

在 Mac/Linux/Windows 上安装 java-harness 仓库后，Hook 系统报错：

```
⎿  PreToolUse:Read hook error
⎿  Failed with non-blocking status code: /bin/sh: bin/harness: No such file or directory
```

## 根本原因

Hook 系统从项目子目录（如 `java-harness-cli/`、`java-harness-foundation/`）执行命令时，相对路径 `bin/harness` 找不到文件，因为 `bin/` 目录只在项目根目录存在。

## 解决方案

### 自动修复（推荐）

在项目根目录运行安装脚本：

```bash
bash scripts/install-hooks.sh
```

该脚本会：
1. 自动检测项目根目录
2. 将 `hooks/hooks.json` 更新为绝对路径
3. 创建备份文件 `hooks/hooks.json.backup-<timestamp>`
4. 验证 JSON 格式

### 手动修复

如果自动脚本失败，可以手动修复：

1. 找到项目根目录：
```bash
git rev-parse --show-toplevel
# 输出: /path/to/java-harness
```

2. 编辑 `hooks/hooks.json`，将所有 `bin/harness` 替换为：
```json
"command": "bash /path/to/java-harness/.claude-plugin/hook-wrapper.sh hook ..."
```

3. 验证 JSON 格式：
```bash
python -m json.tool hooks/hooks.json
```

## 验证修复

从任何子目录测试 Hook：

```bash
# 从子目录测试
cd java-harness-cli
bash /absolute/path/to/.claude-plugin/hook-wrapper.sh --version
# 应该输出: harness 4.1.1
```

## Mac 特别说明

Mac 用户遇到此问题的原因是：
1. Hook 系统在子目录中执行命令
2. 相对路径 `bin/harness` 在子目录中不存在
3. 需要使用绝对路径或 git 根目录检测

修复后，Hook 系统会：
- 使用 `git rev-parse --show-toplevel` 自动找到项目根目录
- 从任何子目录都能正确调用 `bin/harness`
- 支持跨平台（macOS、Linux、Windows Git Bash）

## 故障排除

### 仍然报错 "No such file or directory"

1. 确认 wrapper 脚本存在：
```bash
ls -la .claude-plugin/hook-wrapper.sh
```

2. 确认脚本可执行：
```bash
chmod +x .claude-plugin/hook-wrapper.sh
```

3. 手动测试脚本：
```bash
bash .claude-plugin/hook-wrapper.sh --version
```

### JSON 格式错误

检查备份文件并恢复：
```bash
# 查看备份
ls hooks/hooks.json.backup-*

# 恢复备份
cp hooks/hooks.json.backup-<timestamp> hooks/hooks.json

# 重新运行安装脚本
bash scripts/install-hooks.sh
```

### 权限错误

确保脚本有执行权限：
```bash
chmod +x .claude-plugin/hook-wrapper.sh
chmod +x scripts/install-hooks.sh
```

## 相关文件

- **Hook Wrapper**: `.claude-plugin/hook-wrapper.sh` - 智能路径解析脚本
- **安装脚本**: `scripts/install-hooks.sh` - 自动配置 hooks.json
- **Hooks 配置**: `hooks/hooks.json` - Hook 系统配置文件
- **备份位置**: `hooks/hooks.json.backup-*` - 自动创建的备份

## 技术细节

### Hook Wrapper 工作原理

```bash
#!/bin/sh
# 找到 Git 仓库根目录
GIT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)

# 使用绝对路径调用 bin/harness
HARNESS_SCRIPT="$GIT_ROOT/bin/harness"
exec "$HARNESS_SCRIPT" "$@"
```

这样无论从哪个子目录调用，都能找到正确的 `bin/harness`。

### Cross-platform 支持

- **macOS**: 使用 `sed -i ''` 进行路径替换
- **Linux**: 使用 `sed -i` 进行路径替换
- **Windows Git Bash**: 兼容 Unix 路径和命令

## 参考文档

- [Phase 9: 跨平台 Hooks Wrapper 统一方案](../../Plans.md#phase-9)
- [设计文档](../superpowers/specs/2026-08-07-cross-platform-hooks-wrapper-design.md)
- [实现计划](../superpowers/plans/2026-08-07-cross-platform-hooks-wrapper.md)
