# 跨平台 Hooks Wrapper 设计文档

**版本**: 1.0.0
**日期**: 2026-08-07
**作者**: dxf
**状态**: 设计阶段

---

## 1. 概述

### 1.1 目标

统一 java-harness 的 hooks 配置，使用 wrapper 脚本实现跨平台自动检测，消除维护多份 `hooks.<platform>.json` 模板文件的开销。

### 1.2 背景

**当前问题**：
- 仓库维护 5 份平台特定的 hooks 模板文件（`hooks.windows.json`、`hooks.linux-amd64.json` 等）
- 用户安装后需要手动复制对应平台的配置到 `hooks/hooks.json`
- hooks.json 中的 `command` 字段硬编码平台路径（`bin/windows/harness.exe`、`bin/linux/linux-amd64/harness`）
- 添加新平台或修改路径需要同步更新 5+ 个文件

**解决方案**：
- 创建 `bin/harness`（Unix）和 `bin/harness.bat`（Windows）wrapper 脚本
- 统一 `hooks/hooks.json` 配置，所有平台使用 `bin/harness`
- wrapper 自动检测 OS/架构，调用正确的 Native Image 或 JAR fallback

### 1.3 优势

| 方面 | 改进 |
|------|------|
| 维护成本 | 从 5 个平台模板减少到 1 个统一配置 |
| 用户体验 | 无需手动复制配置，wrapper 自动检测 |
| 可扩展性 | 添加新平台只需更新 wrapper 逻辑 |
| 可靠性 | JAR fallback 确保 clone 后立即可用 |

---

## 2. 架构设计

### 2.1 文件结构

```
java-harness/
├── bin/
│   ├── harness                    # Unix wrapper（新增）
│   ├── harness.bat                # Windows wrapper（新增）
│   ├── windows/harness.exe        # 保持不变
│   ├── linux/linux-amd64/harness  # 保持不变
│   ├── linux/linux-arm64/harness  # 保持不变
│   ├── macos/macos-amd64/harness  # 保持不变
│   └── macos/macos-arm64/harness  # 保持不变
├── hooks/
│   ├── hooks.json                 # 统一配置（修改）
│   └── [旧模板删除]                # 移到备份目录
├── docs/reference/
│   └── multi-platform-hooks-backup/  # 备份旧配置（新增）
└── java-harness-cli/target/
    └── java-harness-cli-*-shaded.jar  # JAR fallback
```

### 2.2 调用流程

```
Claude Code Hook Event
    ↓
bin/harness (or bin/harness.bat on Windows)
    ↓
OS/Architecture Detection
    ├─ Windows → bin/windows/harness.exe
    ├─ Linux AMD64 → bin/linux/linux-amd64/harness
    ├─ Linux ARM64 → bin/linux/linux-arm64/harness
    ├─ macOS Intel → bin/macos/macos-amd64/harness
    ├─ macOS ARM64 → bin/macos/macos-arm64/harness
    └─ All → JAR fallback (if binary missing)
    ↓
Native Image (or JAR)
    ↓
Hook Subcommand Execution
```

### 2.3 数据流

```
hooks.json: "command": "bin/harness hook pre-tool"
    ↓
bin/harness 解析参数:
    - $0: script path
    - $1..$n: "hook", "pre-tool", [stdin]
    ↓
平台检测:
    - OS=$(uname -s)
    - ARCH=$(uname -m)
    ↓
路径映射:
    - Linux + x86_64 → bin/linux/linux-amd64/harness
    - macOS + arm64 → bin/macos/macos-arm64/harness
    - etc.
    ↓
执行:
    exec $BINARY "$@"  # 替换进程，无额外开销
```

---

## 3. Wrapper 脚本实现

### 3.1 Unix Wrapper (`bin/harness`)

**核心逻辑**：
```bash
#!/bin/sh
set -e

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 平台检测
OS_TYPE="$(uname -s)"
ARCH_TYPE="$(uname -m)"
HARNESS_BIN=""

# 平台匹配
case "$OS_TYPE" in
  Linux*)
    case "$ARCH_TYPE" in
      x86_64|amd64) HARNESS_BIN="$SCRIPT_DIR/linux/linux-amd64/harness" ;;
      aarch64|arm64) HARNESS_BIN="$SCRIPT_DIR/linux/linux-arm64/harness" ;;
    esac
    ;;
  Darwin*)
    case "$ARCH_TYPE" in
      x86_64|amd64) HARNESS_BIN="$SCRIPT_DIR/macos/macos-amd64/harness" ;;
      arm64|aarch64) HARNESS_BIN="$SCRIPT_DIR/macos/macos-arm64/harness" ;;
    esac
    ;;
  MINGW*|MSYS*|CYGWIN*)
    HARNESS_BIN="$SCRIPT_DIR/windows/harness.exe"
    ;;
esac

# 执行 Native Image（如果存在）
if [ -n "$HARNESS_BIN" ] && [ -f "$HARNESS_BIN" ]; then
  if [ -x "$HARNESS_BIN" ] || [ "$OS_TYPE" = "MINGW"* ]; then
    exec "$HARNESS_BIN" "$@"
  fi
fi

# 回退到 JAR
JAR_PATTERN="$PROJECT_ROOT/java-harness-cli/target/java-harness-cli-*-shaded.jar"
JAR_FILE=$(ls $JAR_PATTERN 2>/dev/null | grep -v "original-" | head -1)

if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
  echo "[java-harness] Using JAR fallback (slower performance)" >&2
  exec java -jar "$JAR_FILE" "$@"
fi

# 完全失败
echo "[java-harness] Error: No harness binary or JAR found. Please run: mvn package" >&2
exit 1
```

### 3.2 Windows Wrapper (`bin/harness.bat`)

**核心逻辑**：
```batch
@echo off
setlocal enabledelayedexpansion

REM 获取脚本目录
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%i in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fi"

REM 尝试 Windows Native Image
set "HARNESS_BIN=%SCRIPT_DIR%\windows\harness.exe"
if exist "%HARNESS_BIN%" (
  "%HARNESS_BIN%" %*
  set "EXIT_CODE=!ERRORLEVEL!"
  endlocal
  exit /b !EXIT_CODE!
)

REM 回退到 JAR（排除 original- 文件）
for /f "delims=" %%f in ('dir /b /s "%PROJECT_ROOT%\java-harness-cli\target\java-harness-cli*-shaded.jar" 2^>nul ^| findstr /v "original-"') do (
  set "JAR_FILE=%%f"
  goto :jar_found
)

:jar_found
if defined JAR_FILE (
  echo [java-harness] Using JAR fallback ^(slower performance^) >&2
  java -jar "!JAR_FILE!" %*
  set "EXIT_CODE=!ERRORLEVEL!"
  endlocal
  exit /b !EXIT_CODE!
)

REM 完全失败
echo [java-harness] Error: No harness binary or JAR found. Please run: mvn package >&2
endlocal
exit /b 1
```

### 3.3 关键设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 平台检测 | 方案 B：系统检测 + 精确匹配 | 避免执行不兼容二进制，更可靠 |
| Fallback 机制 | 回退到 JAR | 确保用户 clone 后立即可用 |
| 路径解析 | `$0` + `dirname` | 兼容软链接和相对路径调用 |
| 错误输出 | 全部到 `>&2` | 不干扰 hook 标准输出 |
| 退出码 | 传播原命令退出码 | 让 Claude Code 知道 hook 是否成功 |

---

## 4. 统一的 Hooks 配置

### 4.1 hooks.json 结构

所有 16 个 hook 事件类型统一使用 `"command": "bin/harness"`：

```json
{
  "description": "java-harness: automation hooks - cross-platform wrapper",
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Write|Edit|MultiEdit|Bash|Read",
        "hooks": [
          {
            "type": "command",
            "command": "bin/harness hook pre-tool",
            "timeout": 10
          }
        ]
      }
    ],
    // ... 其他 15 个 hook 事件类型
  }
}
```

### 4.2 关键变更

**变更前**（多平台模板）：
```json
// hooks/hooks.windows.json
"command": "bin/windows/harness.exe hook pre-tool"

// hooks/hooks.linux-amd64.json
"command": "bin/linux/linux-amd64/harness hook pre-tool"
```

**变更后**（统一配置）：
```json
// hooks/hooks.json（唯一配置）
"command": "bin/harness hook pre-tool"
```

### 4.3 平台覆盖

**支持的平台矩阵**：

| OS | 架构 | 二进制路径 | 检测关键字 |
|----|------|-----------|-----------|
| Windows | x86_64 | `bin/windows/harness.exe` | `MINGW*`, `MSYS*`, `CYGWIN*` |
| Linux | x86_64 | `bin/linux/linux-amd64/harness` | `Linux*` + `x86_64` |
| Linux | ARM64 | `bin/linux/linux-arm64/harness` | `Linux*` + `aarch64` |
| macOS | Intel | `bin/macos/macos-amd64/harness` | `Darwin*` + `x86_64` |
| macOS | Apple Silicon | `bin/macos/macos-arm64/harness` | `Darwin*` + `arm64` |
| 全平台 | - | JAR fallback | - |

---

## 5. 错误处理和日志策略

### 5.1 错误处理层级

```
Level 1: Native Binary 不存在
  ├─ 自动尝试下一个优先级（在方案 B 中不存在）
  └─ 跳过，进入 JAR fallback

Level 2: Native Binary 存在但不可执行
  ├─ Unix: [ -x "$HARNESS_BIN" ] 检查失败
  ├─ 输出警告到 stderr
  └─ 尝试 JAR fallback

Level 3: JAR 不存在
  ├─ ls 通配符匹配失败
  ├─ 输出错误: "Please run: mvn package"
  └─ exit 1

Level 4: 执行失败
  ├─ Native Image 或 JAR 返回非零退出码
  └─ 传播退出码到 Claude Code
```

### 5.2 日志输出规范

| 类型 | 格式 | 输出时机 |
|------|------|----------|
| 信息 | `[java-harness] Using JAR fallback (slower performance)` | JAR fallback 触发时 |
| 警告 | `[java-harness] Warning: Unsupported architecture: $ARCH_TYPE` | 平台不受支持时 |
| 错误 | `[java-harness] Error: No harness binary or JAR found...` | 完全失败时 |
| 调试 | `[java-harness] Debug: Detected platform=Linux-ARCH=x86_64` | 仅当 `HARNESS_DEBUG=1` 时 |

**原则**：
- 所有日志输出到 `stderr`（`>&2`），不干扰 hook 标准输出
- 日志格式前缀：`[java-harness]`
- 只在必要时输出（fallback、错误）
- 生产环境无调试日志

### 5.3 退出码

| 场景 | 退出码 | Claude Code 行为 |
|------|--------|-----------------|
| Native Image 执行成功 | 0 | Hook 成功 |
| Native Image 执行失败 | 传播原退出码 | Hook 失败 |
| JAR 执行成功 | 0 | Hook 成功 |
| JAR 执行失败 | 传播原退出码 | Hook 失败 |
| 二进制和 JAR 都不存在 | 1 | Hook 失败 |

---

## 6. 测试策略

### 6.1 单元测试

| 测试项 | 命令 | 预期结果 |
|--------|------|----------|
| Shell 语法 | `sh -n bin/harness` | 无输出（语法正确） |
| Batch 语法 | `bin/harness.bat`（无参数） | 错误提示但无语法错误 |
| 文件权限 | `ls -l bin/harness` | `-rwxr-xr-x`（可执行） |

### 6.2 集成测试

```bash
# Test 1: 版本信息（验证 wrapper 能找到并执行二进制）
bin/harness --version
# 预期: harness 4.1.1

# Test 2: Hook 子命令（验证参数传递）
bin/harness hook pre-tool --help
# 预期: hook 命令的帮助信息

# Test 3: Fallback 机制
mv bin/windows/harness.exe bin/windows/harness.exe.bak
bin/harness --version
# 预期: [java-harness] Using JAR fallback... + harness 4.1.1
mv bin/windows/harness.exe.bak bin/windows/harness.exe

# Test 4: 完全失败
mv bin bin.bak
rm -rf java-harness-cli/target/*-shaded.jar
bin/harness --version
# 预期: [java-harness] Error: No harness binary... + exit 1
mv bin.bak bin
```

### 6.3 跨平台验证

| 平台 | 测试环境 | 验证命令 |
|------|----------|----------|
| Windows | Git Bash / CMD | `bin/harness --version` |
| Windows | PowerShell | `bin\harness.bat --version` |
| Linux AMD64 | x86_64 容器/VM | `bin/harness --version` |
| Linux ARM64 | ARM64 容器/VM | `bin/harness --version` |
| macOS Intel | x86_64 Mac | `bin/harness --version` |
| macOS ARM64 | Apple Silicon | `bin/harness --version` |

### 6.4 真实 Hook 测试

```bash
# 1. 安装到 .claude-plugin/
cp hooks/hooks.json .claude-plugin/hooks.json

# 2. 在 Claude Code 中触发 Write 操作
# 观察是否有 wrapper 输出（stderr）

# 3. 检查 hook 执行日志
# .claude-logs/ 或项目日志中的 hook 记录
```

---

## 7. 迁移步骤

### 7.1 文件操作

```bash
# Step 1: 创建 wrapper 脚本
# bin/harness（已在设计中创建）
# bin/harness.bat（已在设计中创建）

# Step 2: 设置执行权限
chmod +x bin/harness

# Step 3: 更新 hooks/hooks.json 为统一配置
# （修改 command 字段为 bin/harness）

# Step 4: 备份旧的多平台配置
mkdir -p docs/reference/multi-platform-hooks-backup
mv hooks/hooks.linux-amd64.json docs/reference/multi-platform-hooks-backup/
mv hooks/hooks.linux-arm64.json docs/reference/multi-platform-hooks-backup/
mv hooks/hooks.macos-amd64.json docs/reference/multi-platform-hooks-backup/
mv hooks/hooks.macos-arm64.json docs/reference/multi-platform-hooks-backup/
# hooks/hooks.json 保持作为新的统一配置

# Step 5: 更新 PLATFORM_SETUP.md
# 移除"安装后配置"中的多平台复制步骤

# Step 6: 验证
bin/harness --version
```

### 7.2 文档更新

**PLATFORM_SETUP.md** 修改：

**删除**：
```markdown
### 自动安装后配置

### Windows
cp hooks/hooks.windows.json hooks/hooks.json

### Linux (AMD64)
cp hooks/hooks.linux-amd64.json hooks/hooks.json
...
```

**替换为**：
```markdown
### 验证安装

```bash
# Windows
bin\harness.bat --version

# Linux/macOS
bin/harness --version
```

### 自动平台检测

`bin/harness`（Unix）和 `bin/harness.bat`（Windows）会自动检测平台并调用正确的二进制。

**手动测试**：
```bash
# 应该输出: harness 4.1.1
bin/harness --version
```
```

### 7.3 同步到 .claude-plugin/

现有的 HooksSyncer 不需要修改：
- 它仍然读取 `hooks/hooks.json`
- 复制到 `.claude-plugin/hooks.json`
- Claude Code 从 `.claude-plugin/hooks.json` 加载 hooks

### 7.4 Git Commit

```bash
git add bin/harness bin/harness.bat
git add hooks/hooks.json
git add docs/reference/multi-platform-hooks-backup/
git add PLATFORM_SETUP.md
git commit -m "feat(hooks): 统一跨平台 hooks 配置，使用 wrapper 脚本

- 新增 bin/harness 和 bin/harness.bat 作为平台检测 wrapper
- 统一 hooks/hooks.json 配置，所有平台使用 bin/harness
- wrapper 自动检测 OS/架构，调用正确的二进制或 JAR fallback
- 备份旧的多平台配置到 docs/reference/
- 更新 PLATFORM_SETUP.md，移除手动复制步骤"
```

---

## 8. 性能分析

### 8.1 开销分析

| 场景 | Native Image | Wrapper 开销 | 总计 |
|------|-------------|-------------|------|
| Native Image 存在 | ~50-70ms | +5-10ms | ~55-80ms |
| JAR fallback | ~500-1000ms | +5-10ms | ~505-1010ms |

**Wrapper 开销来源**：
- Shell/Batch 启动：~5ms
- 平台检测（`uname`）：~2ms
- 路径解析和 `exec`：~3ms

### 8.2 优化措施

1. **使用 `exec` 替换进程**：避免 Shell 留在内存中
2. **避免不必要的检测**：找到二进制后立即 `exec`，不继续检测
3. **JAR fallback 使用通配符**：一次 `ls` 调用，避免循环

### 8.3 对比原方案

| 方案 | 启动时间 | 维护成本 |
|------|---------|---------|
| 原方案（多模板） | ~50-70ms | 高（5 个文件） |
| 新方案（wrapper） | ~55-80ms | 低（1 个文件） |

**结论**：Wrapper 增加 ~10ms 开销（可接受），大幅降低维护成本。

---

## 9. 风险和缓解措施

### 9.1 已识别风险

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|-------|------|----------|
| Wrapper 脚本 bug 导致错误平台检测 | 中 | 高 | 充分的跨平台测试 |
| Windows Git Bash 环境兼容性 | 中 | 中 | 在 MINGW/MSYS 环境中测试 |
| JAR fallback 性能问题 | 低 | 中 | 文档说明，建议构建 Native Image |
| 用户直接调用 `bin/windows/harness.exe` | 低 | 低 | 保持二进制路径不变，向后兼容 |

### 9.2 回滚计划

如果发现严重问题：
```bash
# 恢复多平台配置
mv docs/reference/multi-platform-hooks-backup/* hooks/
rm bin/harness bin/harness.bat
```

---

## 10. 未来扩展

### 10.1 可能的改进

1. **环境变量覆盖**：允许用户设置 `HARNESS_BIN` 指定自定义路径
2. **版本检测**：wrapper 检测二进制版本，不匹配时输出警告
3. **自动下载**：缺少二进制时自动从 GitHub Releases 下载
4. **性能监控**：记录执行时间，输出性能警告（>500ms）

### 10.2 新平台支持

添加新平台只需更新 wrapper 的 `case` 语句：

```bash
case "$OS_TYPE" in
  FreeBSD*)
    HARNESS_BIN="$SCRIPT_DIR/freebsd/amd64/harness"
    ;;
esac
```

---

## 11. 完成标准

实现完成后，应该满足以下标准：

1. ✅ `bin/harness` 和 `bin/harness.bat` 创建并通过语法检查
2. ✅ `bin/harness --version` 在所有平台输出正确版本号
3. ✅ `hooks/hooks.json` 统一配置，所有 command 使用 `bin/harness`
4. ✅ 旧的多平台模板备份到 `docs/reference/`
5. ✅ `PLATFORM_SETUP.md` 更新，移除手动复制步骤
6. ✅ 跨平台测试通过（Windows/Linux/macOS）
7. ✅ Fallback 机制验证（删除二进制后使用 JAR）
8. ✅ 所有变更 commit 到 git

---

**批准**：[待批准]
**下一步**：调用 writing-plans 技能创建实现计划
