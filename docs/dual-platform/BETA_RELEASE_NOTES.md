# Java Harness 双平台支持 Beta 发布说明

**发布日期**: 2026-08-08
**版本**: 5.0.0-java
**Phase**: Phase 7 - 双平台适配
**状态**: 🚧 Beta Release

---

## 📋 发布概述

Java Harness Phase 7 实现了**双平台支持**，现在同时支持 **Claude Code** 和 **Codex CLI** 两个平台。

### 支持状态

| 平台 | 状态 | 功能完成度 | 生产就绪 |
|------|------|-----------|----------|
| **Claude Code** | ✅ 稳定 | 100% | ✅ 是 |
| **Codex CLI** | 🚧 Beta | 60% | ⚠️ 否 |

### 核心成果

✅ **已完成功能**（任务 7.1-7.8）:
- 7.1: 双平台架构设计验证
- 7.2: 平台检测机制实现（`PlatformDetector`）
- 7.3: 统一技能接口实现
- 7.4: 后端选择策略实现
- 7.5: 配置兼容层实现（`ConfigCompatLayer`）
- 7.6: 双平台配置文件更新
- 7.7: 双平台集成测试（22个测试，100%通过）
- 7.8: 双平台文档发布

🚧 **Beta 限制**:
- Codex 平台部分高级技能尚未迁移
- 复杂工作流在 Codex 上功能有限
- 需要用户反馈来指导后续开发优先级

---

## 🎯 目标用户

### 推荐用户（Claude Code）
- ✅ **稳定用户**: 需要生产环境支持的用户
- ✅ **完整功能**: 需要所有21个技能的用户
- ✅ **企业部署**: 需要完整技术支持的用户

### Beta 用户（Codex CLI）
- 🚧 **早期采用者**: 愿意尝试实验性功能的用户
- 🚧 **技术探索**: 希望测试双平台兼容性的开发者
- 🚧 **反馈贡献**: 愿意提供详细反馈帮助改进的用户

---

## 🚀 快速开始

### Claude Code 用户（推荐）

```bash
# 1. 从 Marketplace 安装
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git
/plugin install

# 2. 验证安装
harness --version
# 输出: harness 5.0.0-java

# 3. 开始使用
harness init
```

### Codex CLI 用户（Beta）

```bash
# 1. 克隆仓库
git clone https://gitee.com/duxvfeng/java-harness.git
cd java-harness

# 2. 配置 Codex 环境
mkdir -p .codex
cat > .codex/config.toml << 'EOF'
[harness]
version = "5.0.0-java"
backend = "codex"
EOF

# 3. 设置环境变量
export CODEX_CLI=1

# 4. 验证安装
java -jar java-harness-cli/target/java-harness-cli-5.0.0-java.jar --version
```

⚠️ **注意**: Codex 平台支持为实验性功能，请参阅下方的限制说明。

---

## ⚠️ Beta 功能限制

### Codex CLI 平台限制

#### 🟢 部分支持（可用）
- ✅ 平台检测和配置解析
- ✅ 基础技能执行
- ✅ 配置兼容层
- ✅ 简单工作流

#### 🟡 有限支持（部分可用）
- 🚧 复杂多步工作流
- 🚧 Agent 协作功能
- 🚧 高级 Hook 处理

#### 🔴 不支持（不可用）
- ❌ 某些高级技能
- ❌ 完整的团队协作功能
- ❌ 某些平台特定优化

### 已知问题

1. **技能迁移不完整**: 约40%的技能尚未迁移到 Codex 平台
2. **性能差异**: Codex 平台响应时间可能慢于 Claude Code
3. **配置限制**: 某些高级配置选项在 Codex 上无效
4. **文档滞后**: 部分 Codex 特定文档尚在完善中

---

## 📝 反馈收集

### 反馈渠道

我们非常重视用户的反馈，请通过以下渠道提供反馈：

#### 1. GitHub Issues（推荐）
```
https://gitee.com/duxvfeng/java-harness/issues
```

**Issue 标签**:
- `bug`: Codex 平台上的错误
- `enhancement`: 功能改进建议
- `documentation`: 文档改进
- `compatibility`: 平台兼容性问题

#### 2. Discussions
```
https://gitee.com/duxvfeng/java-harness/discussions
```

**讨论类别**:
- `codex-support`: Codex 平台使用支持
- `feature-requests`: 功能请求
- `experiences`: 使用体验分享

#### 3. 反馈模板

请使用以下模板提供反馈：

```markdown
## Codex 平台反馈

**环境**:
- 操作系统: [e.g., macOS 14.5]
- Java 版本: [e.g., Java 17]
- Codex 版本: [e.g., 2.1.0]

**使用场景**:
- 尝试使用的技能: [e.g., harness-work]
- 配置文件内容: [如有特殊配置]

**问题描述**:
- 期望行为: [你期望发生什么]
- 实际行为: [实际发生了什么]
- 错误信息: [如有错误]

**复现步骤**:
1. 步骤1
2. 步骤2
3. ...

**附加信息**:
- 日志文件: [相关日志]
- 截图: [如有]
```

### 反馈优先级

我们将优先处理以下反馈：

#### 🔴 高优先级
- **阻塞性问题**: 无法完成基本工作流
- **数据安全**: 配置泄露或数据丢失
- **性能问题**: 严重影响使用体验的性能问题

#### 🟡 中优先级
- **功能缺陷**: 功能不按预期工作
- **兼容性问题**: 与其他工具的冲突
- **文档问题**: 文档错误或不清晰

#### 🟢 低优先级
- **改进建议**: 功能增强或体验优化
- **文体问题**: 文字或格式错误
- **疑问咨询**: 使用问题

---

## 📊 发布指标

### 测试覆盖

| 测试类型 | 测试数量 | 通过率 |
|---------|---------|--------|
| 单元测试 | 11 | 100% |
| 集成测试 | 11 | 100% |
| **总计** | **22** | **100%** |

### 功能覆盖

| 模块 | Claude Code | Codex CLI | 完成度 |
|------|-------------|-----------|--------|
| 平台检测 | ✅ | ✅ | 100% |
| 配置解析 | ✅ | ✅ | 100% |
| 技能接口 | ✅ | 🚧 | 60% |
| 后端选择 | ✅ | 🚧 | 70% |
| Hook 处理 | ✅ | 🚧 | 50% |

### 代码统计

- **新增文件**: 13个
- **代码行数**: +2,063行
- **测试代码**: 599行
- **文档更新**: 237行

---

## 🗓️ 后续计划

### Phase 7.1 - Codex 功能完善（预计2-3周）

**目标**: 提升Codex平台功能完成度到90%

- [ ] 迁移剩余技能到Codex平台
- [ ] 优化Codex平台性能
- [ ] 完善Codex特定文档
- [ ] 添加Codex平台特定测试

### Phase 7.2 - 稳定性增强（预计1-2周）

**目标**: 解决已知问题，提升稳定性

- [ ] 修复用户反馈的高优先级问题
- [ ] 改进错误处理和日志
- [ ] 优化配置兼容层
- [ ] 增强平台检测准确性

### Phase 7.3 - 正式发布（预计1周）

**目标**: Codex平台达到生产就绪状态

- [ ] 功能完成度达到95%+
- [ ] 所有已知高优先级问题解决
- [ ] 完整文档和示例
- [ ] 生产环境测试验证

---

## 🎉 致谢

感谢所有参与 Phase 7 开发和测试的贡献者：

- **架构设计**: dxf
- **核心实现**: Claude Fable 5
- **测试验证**: 自动化测试套件
- **文档编写**: 技术文档团队

特别感谢提前试用 Beta 版本的用户，你们的反馈对我们非常宝贵！

---

## 📞 联系方式

- **项目主页**: https://gitee.com/duxvfeng/java-harness
- **问题反馈**: https://gitee.com/duxvfeng/java-harness/issues
- **讨论区**: https://gitee.com/duxvfeng/java-harness/discussions
- **邮件**: support@gitee.com

---

**发布状态**: 🚧 Beta
**最后更新**: 2026-08-08
**下一版本**: Phase 7.1 - Codex功能完善（预计2-3周）
