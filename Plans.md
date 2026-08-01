# Java Harness 实施计划

**Purpose**: 将Java Harness从35-40%功能实现度扩展到与Go项目功能对等（90%+），实现完整的Plan→Work→Review→Release闭环

**目标版本**: v4.1.0-SNAPSHOT
**预计周期**: 14-19周
**架构模式**: 7层功能域驱动设计，9个Maven模块

---

## 阶段 1：基础架构重构（2-3周）

**目标**: 重组模块结构，建立7层架构，迁移现有功能到新模块

### Phase 1.1：创建Maven父项目和多模块结构（2-3天）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1.1 | 创建根POM文件 | 创建pom.xml，定义9个模块依赖管理，验证`mvn help:effective-pom`成功解析 | `mvn help:effective-pom`执行成功，无错误 | - | cc:✅ 74ed47c |
| 1.1.2 | 创建foundation模块 | 创建模块结构、DTO类（HookInput/Output/GuardrailResult）、配置接口 | `cd java-harness-foundation && mvn test`通过，单元测试覆盖率>70% | 1.1.1 | cc:✅ 9cf7f03 |
| 1.1.3 | 创建protocol模块 | 创建HookEventType枚举、HookHandler接口、JacksonHookCodec实现 | `cd java-harness-protocol && mvn test`通过，编解码测试验证 | 1.1.2 | cc:✅ |
| 1.1.4 | 创建security模块 | 创建GuardrailRule接口、GuardrailEngine接口、RuleRegistry实现 | `cd java-harness-security && mvn test`通过，现有Guardrail规则功能无回归 | 1.1.3 | cc:✅ |
| 1.1.5 | 创建其他模块框架 | 创建workflow/collaboration/cli/tools/distribution模块POM | `mvn clean compile`所有9个模块编译成功，依赖关系正确 | 1.1.4 | cc:✅ |

**Phase 1.1 验收标准**:
- [ ] 所有9个Maven模块编译成功
- [ ] 单元测试覆盖率>70%
- [ ] 现有Guardrail规则功能无回归
- [ ] 模块依赖关系正确（单向依赖）
- [ ] 设计文档架构一致性验证通过

---

## 阶段 2：工作流层实现（3-4周）

**目标**: 实现Plans.md解析、任务编排、并行执行和基础状态恢复

### Phase 2.1：Plans.md解析器（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.1.1 | 实现Plans.md数据模型 | 创建Status枚举、Task模型、PlansDocument模型 | `cd java-harness-workflow && mvn test`通过，模型测试验证 | 1.1.5 | cc:✅ c61166a |
| 2.1.2 | 实现RegexPlansParser | 创建PlansParser接口、RegexPlansParser实现，支持表格和标记解析 | `cd java-harness-workflow && mvn test`通过，解析器测试通过，能正确解析Plans.md表格 | 2.1.1 | cc:✅ ab16ad5 |

### Phase 2.2：任务编排和并行执行（1.5周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.2.1 | 实现任务编排器接口 | 创建TaskOrchestrator接口、OrchestrationPlan模型、ExecutionResult模型 | 接口定义完整，模型类编译通过 | 2.1.2 | cc:✅ 0db4851 |
| 2.2.2 | 实现并行执行器 | 创建ParallelExecutor接口、CompletableFutureExecutor实现 | `cd java-harness-workflow && mvn test`通过，并行执行测试通过，并行执行比顺序执行快2倍以上 | 2.2.1 | cc:✅ de58403 |

### Phase 2.3：状态恢复基础（1.5周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.3.1 | 实现状态恢复接口 | 创建StateRecovery接口、RecoveryResult模型、RecoveryStrategy接口 | 接口定义完整，模型类编译通过，包含4阶段恢复方法定义 | 2.2.2 | cc:✅ 93c7a29 |

**Phase 2 验收标准**:
- [x] Plans.md解析器能正确解析表格和标记 ✅ RegexPlansParser支持5/6列表格，解析测试通过
- [x] 任务编排器能创建执行计划 ✅ OrchestrationPlan/ExecutionResult模型完整，接口定义清晰
- [x] 并行执行器能高效执行任务 ✅ CompletableFutureExecutor实现，加速比3.37x (>2x要求)
- [x] 状态恢复接口定义完整 ✅ StateRecovery 4阶段方法，RecoveryStrategy/RecoveryResult模型
- [x] 单元测试覆盖率>75% ✅ 95个测试全部通过，13个源文件全部有测试覆盖
- [x] 性能测试：并行执行比顺序执行快2倍以上 ✅ 加速比3.37x，超过2x要求

---

## 阶段 3：协作层实现（4-5周）

**目标**: 实现技能系统（混合模式）、代理系统（三种代理）和协调机制

### Phase 3.1：技能框架实现（2周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 3.1.1 | 实现技能核心框架 | 创建Skill接口、SkillContext、SkillResult、CoreSkill基类 | `cd java-harness-collaboration && mvn test`通过，技能框架测试验证 | 2.3.1 | cc:✅ 0046d4c |
| 3.1.2 | 实现核心技能 | 实现PlanSkill、WorkSkill、ReviewSkill，创建SkillRegistry | `cd java-harness-collaboration && mvn test`通过，核心技能执行测试通过 | 3.1.1 | cc:✅ aaf46c3 |
| 3.1.3 | 实现Markdown技能加载器 | 实现MarkdownSkillLoader、MarkdownSkill，支持YAML frontmatter解析 | `cd java-harness-collaboration && mvn test`通过，技能加载器测试通过，能正确加载.SKILL.md文件 | 3.1.2 | cc:✅ 079a203 |

### Phase 3.2：代理系统实现（2周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 3.2.1 | 实现代理框架 | 创建Agent接口、AgentContext、AgentResult、AgentRegistry | 接口定义完整，模型类编译通过 | 3.1.3 | cc:✅ fbdeef9 |
| 3.2.2 | 实现三种核心代理 | 实现WorkerAgent、ReviewerAgent、AdvisorAgent，创建AgentCoordinator | `cd java-harness-collaboration && mvn test`通过，代理测试通过，协调机制验证 | 3.2.1 | cc:✅ dada6a7 |

**Phase 3 验收标准**:
- [ ] 技能框架完整实现，支持Java和Markdown技能
- [ ] 核心技能（plan/work/review）执行正确
- [ ] 代理系统完整实现，三种代理都能正常工作
- [ ] 协调机制能正确协调多代理
- [ ] 单元测试覆盖率>75%
- [ ] 集成测试：技能→代理→协调流程完整

---

## 阶段 4：状态恢复完善（2-3周）

**目标**: 实现完整的4阶段恢复机制

### Phase 4.1：4阶段恢复实现

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 4.1.1 | 实现自我修复策略 | 实现SelfHealingStrategy，支持最多3次重试，处理timeout/connection/configuration错误 | `cd java-harness-workflow && mvn test`通过，自我修复测试验证 | 3.2.2 | cc:TODO |
| 4.1.2 | 实现同伴修复策略 | 实现PeerRecoveryStrategy，支持选择替代Worker执行任务 | `cd java-harness-workflow && mvn test`通过，同伴修复测试验证 | 4.1.1 | cc:TODO |
| 4.1.3 | 实现指挥官介入和停止策略 | 实现LeadInterventionStrategy、AbortStrategy、FourPhaseRecovery | `cd java-harness-workflow && mvn test`通过，4阶段恢复测试通过 | 4.1.2 | cc:TODO |

**Phase 4 验收标准**:
- [ ] 4阶段恢复机制完整实现
- [ ] 自我修复能处理常见错误类型
- [ ] 同伴修复能选择替代Worker
- [ ] 指挥官介入能正确escalate
- [ ] 停止策略能正确标记ABORTED状态
- [ ] 状态恢复测试覆盖率>80%

---

## 阶段 5：工具层和优化（2-3周）

**目标**: 实现配置管理工具、验证工具、诊断工具和Native Image优化

### Phase 5.1：配置管理工具（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.1.1 | 实现配置同步工具 | 创建ConfigSyncTool、harness.yaml.example配置模板 | `cd java-harness-tools && mvn test`通过，配置工具测试通过，能正确生成Claude Code配置 | 4.1.3 | cc:TODO |

### Phase 5.2：验证和诊断工具（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.2.1 | 实现验证和诊断工具 | 创建ValidateTool、DoctorTool、HealthCheck接口 | `cd java-harness-tools && mvn test`通过，验证工具能检测配置/技能/代理问题，诊断工具能生成完整健康报告 | 5.1.1 | cc:TODO |

### Phase 5.3：Native Image优化（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.3.1 | 配置GraalVM Native Image | 创建reflect-config.json、resource-config.json、HarnessNativeFeature | `cd java-harness-cli && mvn -Pnative native:compile`编译成功，`./target/harness --version`启动时间<100ms | 5.2.1 | cc:TODO |

**Phase 5 验收标准**:
- [ ] 配置工具能正确生成Claude Code配置
- [ ] 验证工具能检测配置、技能、代理问题
- [ ] 诊断工具能生成完整的健康报告
- [ ] Native Image编译成功且性能达标
- [ ] JAR和Native Image功能一致性验证通过

---

## 阶段 6：集成测试和发布（1-2周）

**目标**: 完整测试、文档完善和发布准备

### Phase 6.1：集成测试完善（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 6.1.1 | 端到端集成测试 | 创建EndToEndWorkflowTest、HookProcessingTest、AgentCoordinationTest | `mvn verify -Pintegration`通过，集成测试覆盖率>85%，Plan→Work→Review完整流程验证 | 5.3.1 | cc:TODO |

### Phase 6.2：文档完善和发布准备（1周）

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 6.2.1 | 创建用户文档 | 创建installation.md、configuration.md、migration.md、更新README.md | 所有文档完整准确，安装指南简单易懂，迁移指南清晰 | 6.1.1 | cc:TODO |
| 6.2.2 | 发布准备 | 更新VERSION文件、创建CHANGELOG.md、运行完整测试套件、创建发布分支 | `mvn verify`所有测试通过，质量检查全部通过，发布分支准备完成 | 6.2.1 | cc:TODO |

**Phase 6 验收标准**:
- [ ] 集成测试覆盖率>85%
- [ ] 所有文档完整准确
- [ ] 发布版本号和CHANGELOG正确
- [ ] 发布分支准备完成
- [ ] 质量检查全部通过

---

## 总体验收标准

### 功能对等性（与Go项目对比）
- [ ] 90%+核心功能对等
- [ ] Plan→Work→Review→Release闭环完整
- [ ] 技能系统功能对等（混合模式）
- [ ] 代理系统功能对等（三种代理）
- [ ] 状态恢复功能对等（4阶段）
- [ ] Hook协议功能对等

### 性能标准
- [ ] Hook处理时间<10ms（95th percentile）
- [ ] Native Image启动时间<100ms
- [ ] Native Image内存占用<50MB
- [ ] JAR模式启动时间<5秒

### 质量标准
- [ ] 单元测试覆盖率>75%
- [ ] 集成测试覆盖率>80%
- [ ] 代码审查通过率>95%
- [ ] 无关键性bug

### 文档标准
- [ ] 用户文档完整准确
- [ ] API文档完整
- [ ] 迁移指南清晰
- [ ] 安装指南简单易懂

### 部署标准
- [ ] JAR和Native Image双模式验证通过
- [ ] 配置模板提供完整
- [ ] 诊断工具功能完整
- [ ] 发布流程验证通过

---

## 成功标志

当所有阶段的验收标准都达成时，Java Harness项目将成功从35-40%功能实现度扩展到与Go项目功能对等（90%+），成为一个完整的、轻量级的、企业级的AI开发工作流工具。

**预期成果**：
- ✅ 功能完整的Java版本Claude Code Harness
- ✅ 清晰的7层架构，支持长期维护
- ✅ 支持双模式部署，满足不同场景需求
- ✅ 为Java开发者提供优秀的AI辅助开发体验

---

**计划版本**: 1.0
**创建日期**: 2026-08-01
**适用项目**: java-harness v4.1.0-SNAPSHOT
**总估算时间**: 14-19周
