# Java Harness Project Specification

## Product Identity

**Name**: Java Harness  
**Version**: 4.1.1  
**Purpose**: Java implementation of Claude Code Harness v4  
**Reference**: Go implementation at `D:\go-project\claude-code-harness`

## Product Contract

### Core Purpose

Java Harness is the Java-native implementation of Claude Code Harness v4, providing the complete functionality of the Go version with Java ecosystem integration. It serves as the foundation for Java-based AI agent workflows and Claude Code integration.

### Functional Completeness Requirements

The Java implementation MUST achieve functional parity with the Go version across these dimensions:

1. **Hook System** (16 hooks)
   - Pre-tool use guardrails (pre-tool)
   - Post-tool tampering detection (post-tool)
   - Permission management (permission, permission-denied)
   - Session lifecycle hooks (session-start, session-init, session-cleanup, session-monitor, session-summary)
   - CI status integration (ci-status)
   - Subagent tracking (subagent-start, subagent-stop)
   - Other hooks (post-tool-failure, post-compact, notification, ask-user-question-normalize)

2. **CLI Commands** (86 commands)
   - **Hook命令组** (16个子命令): hook pre-tool, post-tool, permission, session-start, etc.
   - **Evidence命令组**: evidence collect
   - **Plans命令组**: plans check-deps
   - **核心工作流命令**: plan, work, review, release, gen
   - **维护命令**: doctor, validate, sync, init
   - **会话管理**: session, session-register, session-unregister
   - **审计命令**: self-audit, retired-alias
   - **监控命令**: night-watch, mirror, plans-watcher
   - **工作树命令**: wt, worktree-create, worktree-remove
   - **CI命令**: ci-check, ci-status
   - **内存命令**: mem, memory-bridge
   - **收件箱命令**: inbox, inbox-check
   - **其他命令**: sprint-contract, status, codex-loop, channels-wake, failure-codifier, impact-score, pre-compact, version, etc.

3. **Workflow Engine**
   - YAML-based workflow definitions
   - Conditional execution
   - Parallel step processing
   - Skill integration
   - State management

4. **Agent Coordination**
   - Multi-agent orchestration (Lead/Worker/Reviewer)
   - Breezing mode
   - Cursor/Codex backend support
   - Session management

5. **Internal Packages** (47 packages)
   - Guardrail enforcement (27 rules: R01-R27)
   - Event handling
   - State persistence
   - Policy management
   - CI integration

### Technical Architecture

#### Module Structure

```
java-harness-shared         - Shared utilities and constants
java-harness-foundation     - Data access and configuration
java-harness-protocol       - Event types and codecs (NEW: data models)
java-harness-security       - Security and validation
java-harness-workflow       - Workflow orchestration
java-harness-collaboration  - Skills and agents
java-harness-cli           - Command-line interface
java-harness-service       - Service layer
java-harness-tools         - Development tools
```

#### Language and Framework

- **Language**: Java 17+
- **Build**: Maven
- **CLI Framework**: picocli 4.7
- **Testing**: JUnit 5
- **JSON**: Jackson
- **YAML**: SnakeYAML

### Data Models

#### Core Entities

All core data models are defined in `java-harness-protocol`:
- `Task` - Workflow task representation
- `Status` - Task status enumeration
- `PlansDocument` - Plans.md container
- `HookEvent` - Hook event types
- `SessionContext` - Session state

### Integration Points

#### Claude Code Integration

- Hook system integration via `hooks.json`
- Session start/stop lifecycle management
- Tool use interception and validation
- State persistence in `.claude/state/`

#### External System Integration

- Git operations (worktree, commit, push)
- File system operations (guarded by rules)
- CI systems (GitHub Actions, GitLab CI)
- Package managers (Maven, Gradle)

### Quality Standards

#### Performance

- Hook processing: < 50ms per hook
- Workflow startup: < 100ms
- Simple workflow execution: < 1s
- Memory usage: Comparable to Go version

#### Reliability

- All hooks must have fallback paths
- Workflow state must be recoverable
- No data loss on crash
- Graceful degradation

#### Security

- Guardrail enforcement before all operations
- Permission checking for destructive actions
- Secret handling compliance
- Supply chain validation

### Success Criteria

#### Functional Parity

- [ ] All 13 Go hooks implemented and tested
- [ ] All 30+ CLI commands working
- [ ] Workflow engine feature-complete
- [ ] Agent coordination fully operational
- [ ] CI integration complete

#### Quality Gates

- [ ] 80%+ test coverage
- [ ] All integration tests passing
- [ ] Performance benchmarks met
- [ ] Security review passed

#### Documentation

- [ ] Complete API documentation
- [ ] Migration guide from Go version
- [ ] Architecture decision records
- [ ] Troubleshooting guide

### Migration from Go Version

The Java implementation MUST support:

1. **Configuration Compatibility**: All YAML configs must work identically
2. **Data Migration**: State files must be convertible
3. **Command Parity**: Same CLI interface and behavior
4. **Plugin Compatibility**: Same plugin interface

### Version Compatibility

This implementation targets Claude Code Harness v4 spec and aims for 100% compatibility with the Go reference implementation at `D:\go-project\claude-code-harness`.

---

**Specification Status**: Active  
**Last Updated**: 2026-08-01  
**Maintainer**: Java Harness Team  
**Reference Implementation**: Go version at `D:\go-project\claude-code-harness`
