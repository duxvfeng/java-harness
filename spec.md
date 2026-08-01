# Java Harness Project Specification

## Product Identity

**Name**: Java Harness  
**Version**: 4.0.0-java-SNAPSHOT  
**Purpose**: Java implementation of Claude Code Harness v4  
**Reference**: Go implementation at `D:\go-project\claude-code-harness`

## Product Contract

### Core Purpose

Java Harness is the Java-native implementation of Claude Code Harness v4, providing the complete functionality of the Go version with Java ecosystem integration. It serves as the foundation for Java-based AI agent workflows and Claude Code integration.

### Functional Completeness Requirements

The Java implementation MUST achieve functional parity with the Go version across these dimensions:

1. **Hook System** (13 hooks)
   - Pre-tool use guardrails
   - Post-tool tampering detection  
   - Permission management
   - Session lifecycle hooks
   - CI status integration
   - Subagent tracking

2. **CLI Commands** (30+ commands)
   - Core workflow commands (init, plan, work, review)
   - Maintenance commands (doctor, validate, sync)
   - Inbox management
   - Memory operations
   - CI integration

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
   - Guardrail enforcement
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
