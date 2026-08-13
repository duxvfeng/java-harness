# 强制审查集成使用指南

## 概述

强制审查集成确保所有 `harness-work` 执行都强制经过代码审查，实现质量门控。这是方案1的实现：在 `harness-work` 技能内部集成自动审查。

## 核心特性

### ✅ 强制审查
- 所有执行模式（Solo/Parallel/Breezing）完成后必须通过审查
- 所有后端（claude/cursor/codex）使用统一的审查流程
- 审查不通过时阻止任务标记为完成

### 🔄 自动修复
- 智能修复常见代码问题
- 可配置的修复循环（默认最多3次）
- 自动提交修复并重新审查

### 🎯 灵活配置
- 支持环境变量和配置文件
- 紧急情况支持跳过审查
- 可配置的审查严格度

## 快速开始

### 1. 基本使用

```bash
# 在 harness-work 执行完成后，自动调用强制审查
/harness-work 3

# 系统会自动：
# 1. 执行任务实现
# 2. 调用代码审查
# 3. 根据审查结果决定是否完成
```

### 2. 环境变量配置

```bash
# 设置审查模式
export HARNESS_REVIEW_MODE=strict  # strict | lenient

# 设置最大重试次数
export HARNESS_MAX_REVIEW_ITERATIONS=3

# 设置超时时间
export HARNESS_REVIEW_TIMEOUT=30

# 紧急情况跳过审查（不推荐）
export HARNESS_SKIP_REVIEW=true
```

### 3. 配置文件

在项目根目录创建 `harness.toml`：

```toml
[harness.review]
enabled = true
mode = "strict"
max_iterations = 3
auto_fix = true
on_failure = "escalate"
```

## 详细配置

### 审查模式

#### Strict 模式（默认）
```python
# 任何 critical 或 major 问题都会导致 REQUEST_CHANGES
if any(critical_issues) or any(major_issues):
    verdict = "REQUEST_CHANGES"
```

#### Lenient 模式
```python
# 仅 critical 问题导致 REQUEST_CHANGES
if any(critical_issues):
    verdict = "REQUEST_CHANGES"
```

### 审查失败处理策略

| 策略 | 说明 | 推荐度 |
|------|------|--------|
| `escalate` | 升级到用户处理 | ⭐⭐⭐⭐⭐ |
| `continue` | 继续执行（不推荐） | ⭐ |
| `stop` | 停止执行 | ⭐⭐⭐⭐ |

## 使用示例

### 示例 1: Solo 模式

```bash
# 单个任务执行
/harness-work 5

# 执行流程：
# 1. 执行任务 #5
# 2. 自动代码审查
# 3. 审查通过 → 标记完成
#    审查失败 → 进入修复循环
```

### 示例 2: Parallel 模式

```bash
# 并行执行多个任务
/harness-work --parallel 3 2 4 6

# 执行流程：
# 1. 并行执行任务 #2, #4, #6
# 2. 每个任务完成后独立审查
# 3. 所有任务都通过审查后汇总完成
```

### 示例 3: Breezing 模式

```bash
# 团队执行模式
/harness-work --breezing

# 执行流程：
# 1. Lead 协调 Workers 并行执行
# 2. 每个 Worker 完成后立即审查
# 3. 只有审查通过的工作会 cherry-pick 到 trunk
# 4. Reviewer 进行最终审查
```

### 示例 4: Python API

```python
from scripts.review.forced_review_gate import ForcedReviewGate

# 创建审查门控
review_gate = ForcedReviewGate()

# 执行审查
result = review_gate.review(
    base_ref="abc123",
    worktree_path="/path/to/worktree",
    mode="strict"
)

# 检查结果
if review_gate.is_approved(result):
    print("✅ 审查通过")
else:
    print("❌ 审查未通过")
    findings = review_gate.get_findings(result)
    for finding in findings:
        print(f"  {finding['severity']}: {finding['message']}")
```

## 自动修复循环

### 基本概念

当审查发现问题时，系统会尝试自动修复：

```python
from scripts.review.forced_review_gate import AutoFixReviewLoop

def auto_fix_func(findings, worktree_path):
    """自定义修复函数"""
    fixed_count = 0

    for finding in findings:
        # 修复逻辑
        if can_auto_fix(finding):
            apply_fix(finding, worktree_path)
            fixed_count += 1

    return {"success": fixed_count > 0, "fixed_count": fixed_count}

# 创建自动修复循环
auto_fix_loop = AutoFixReviewLoop(review_gate, max_iterations=3)

final_result = auto_fix_loop.fix_and_review(
    base_ref="abc123",
    worktree_path="/path/to/worktree",
    auto_fix_func=auto_fix_func
)
```

### 支持的自动修复类型

| 问题类型 | 修复规则 | 示例 |
|----------|----------|------|
| 调试语句 | `no-system-out` | `System.out.println` → `LOGGER.info` |
| 调试语句 | `no-print-statements` | `print()` → `logging.info()` |
| 命名规范 | `naming-convention` | 自动调整命名 |
| 格式问题 | `code-format` | 自动格式化代码 |

## 紧急情况处理

### 跳过审查（仅紧急情况）

```bash
# 方法 1: 环境变量
export HARNESS_SKIP_REVIEW=true
/harness-work 3

# 方法 2: 配置文件
# harness.toml
[harness.review]
skip_review = true

# 方法 3: Python API
result = review_gate.review(
    base_ref="abc123",
    worktree_path="/path/to/worktree",
    skip_reason="emergency_security_fix"
)
```

### 紧急情况记录

所有跳过审查的操作都会被记录：

```bash
# 查看跳过记录
cat .claude/review-skip-log.txt

# 输出示例：
# 2024-08-13 10:30:00 - SKIP: emergency_security_fix - user:admin - reason:Critical security fix
```

## 监控和调试

### 启用调试日志

```bash
# 启用详细日志
export HARNESS_REVIEW_DEBUG=1

# 查看审查过程
/harness-work 5
```

### 查看审查历史

```bash
# 查看最近的审查记录
cat .claude/review-history.json | jq '.[] | {time, verdict, findings_count}'

# 统计审查通过率
cat .claude/review-history.json | jq '[.[] | .verdict == "APPROVE"] | add / length * 100'
```

### 性能监控

```python
# 获取审查性能数据
result = review_gate.review(...)
performance = result.get('performance', {})

print(f"审查耗时: {performance.get('duration_ms', 0)}ms")
print(f"审查文件: {performance.get('files_reviewed', 0)}个")
```

## 多语言支持

### 自动语言检测

系统会自动检测变更文件的语言并应用相应标准：

| 语言 | 文件扩展名 | 标准 |
|------|------------|------|
| Java | `.java` | Alibaba Java Development Guide |
| Python | `.py`, `.pyi` | PEP 8 |
| Vue | `.vue` | Vue Style Guide |
| Go | `.go` | Effective Go |

### 配置多语言项目

```toml
# harness.toml
[multilang]
enabled = true
default_language = "java"

[multilang.languages]
java = { extensions = [".java"], standard = "alibaba" }
python = { extensions = [".py"], standard = "pep8" }
```

## CI/CD 集成

### GitHub Actions

```yaml
name: Harness Review
on: [push, pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Harness
        run: |
          npm install -g @anthropic-ai/harness-cli

      - name: Run Forced Review
        run: |
          harness-review --auto \
            --base-ref ${{ github.event.before }} \
            --output review-result.json \
            --mode strict

      - name: Check Result
        run: |
          verdict=$(jq -r '.verdict' review-result.json)
          if [ "$verdict" != "APPROVE" ]; then
            echo "❌ 审查未通过"
            exit 1
          fi
```

### GitLab CI

```yaml
harness_review:
  stage: test
  script:
    - harness-review --auto --base-ref $CI_COMMIT_BEFORE --output review.json
    - |
      verdict=$(jq -r '.verdict' review.json)
      if [ "$verdict" != "APPROVE" ]; then
        echo "❌ 审查未通过"
        exit 1
      fi
```

## 故障排除

### 问题 1: 审查超时

```bash
# 症状: 审查执行超过30秒
# 解决: 增加超时时间

export HARNESS_REVIEW_TIMEOUT=60
```

### 问题 2: 语言检测失败

```bash
# 症状: 文件语言未被正确识别
# 解决: 手动指定语言映射

# 在 harness.toml 中配置
[multilang.languages]
custom = { extensions = [".xyz"], standard = "pep8" }
```

### 问题 3: 自动修复失败

```bash
# 症状: 自动修复无法解决问题
# 解决: 检查修复函数实现，或手动修复

# 查看详细错误
export HARNESS_REVIEW_DEBUG=1
```

### 问题 4: 输出文件错误

```bash
# 症状: JSON输出格式错误
# 解决: 验证脚本路径和权限

which harness-review
chmod +x /path/to/forced-review-gate.sh
```

## 最佳实践

### 1. 审查策略
- ✅ 默认使用 `strict` 模式
- ✅ 为不同分支配置不同严格度
- ✅ 主分支强制审查，功能分支可适当放宽

### 2. 自动修复
- ✅ 启用自动修复提高通过率
- ✅ 限制修复次数避免无限循环
- ✅ 记录修复历史便于分析

### 3. 监控
- ✅ 定期查看审查通过率
- ✅ 分析常见问题模式
- ✅ 根据数据调整规则严格度

### 4. 团队协作
- ✅ 在团队中统一审查标准
- ✅ 定期更新审查规则
- ✅ 培训团队成员理解审查结果

## 相关资源

### 文档
- [harness-work 技能文档](../skills/harness-work/SKILL.md)
- [harness-review 技能文档](../skills/harness-review/SKILL.md)
- [强制审查集成设计](../docs/harness-project/superpowers/specs/2026-08-13-forced-review-integration.md)

### 脚本
- [强制审查脚本](../scripts/review/forced-review-gate.sh)
- [Python API](../scripts/review/forced_review_gate.py)
- [集成测试](../tests/integration/test_forced_review_integration.py)

### 配置
- [配置模板](../configs/harness-review-config.toml)
- [环境变量参考](../docs/harness-project/configuration/environment-variables.md)

## 支持

如有问题或建议，请通过以下方式联系：

- GitHub Issues: [项目地址]
- 文档: [在线文档]
- 邮件: [支持邮箱]

---

**版本**: 2.3.0
**更新日期**: 2024-08-13
**维护者**: Harness Team