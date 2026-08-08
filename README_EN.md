# Claude Code Harness - Java Implementation

A Java native implementation of Claude Code Harness, providing core CLI Gateway functionality including Hook protocol handling, Guardrail security engine, and rapid response mechanism.

## 🎯 Project Overview

This is the Java native implementation of [claude-code-harness](https://github.com/your-org/claude-code-harness), compiled to Native Image via GraalVM, achieving **<10ms hook response time** for real-time security policy enforcement in Claude Code.

### Core Value Proposition

- **🚀 High Performance**: GraalVM Native Image compilation, sub-millisecond response time
- **🔒 Security Protection**: Comprehensive coverage of 27 Guardrail rules (R01-R27)
- **📡 Hook Protocol**: Complete Claude Code Hook event processing (14 hook subcommands)
- **🎯 Modular Design**: Command groups + standalone commands, feature parity with Go version
- **📋 Complete CLI**: 86 CLI commands, fully replicating Go version's command structure

### Current Status

- **Version**: 4.1.1
- **Go Version Correspondence**: claude-code-harness v5.5.0
- **Feature Completion**: Phase 9 completed (Cross-platform Hooks Unification)
- **Documentation Status**: Documentation system rebuild in progress

## 🚀 Quick Start

### Prerequisites

- **JDK**: 17+
- **Operating Systems**: Windows / macOS / Linux
- **Memory**: Minimum 4GB RAM
- **Disk Space**: Minimum 500MB

### Installation Methods

#### Method 1: Precompiled Binary (Recommended)

1. **Download the binary for your platform**:

```bash
# Windows (x64)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-windows-amd64.exe -o harness.exe

# Linux (AMD64)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-linux-amd64 -o harness
chmod +x harness

# macOS (Intel)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-amd64 -o harness
chmod +x harness

# macOS (Apple Silicon)
curl -L https://github.com/your-org/java-harness/releases/latest/download/harness-macos-arm64 -o harness
chmod +x harness
```

2. **Verify installation**:

```bash
./harness --version
# Output: harness 4.1.1
```

#### Method 2: Using JAR File

```bash
# Download JAR file
curl -L https://github.com/your-org/java-harness/releases/latest/download/java-harness-cli-4.1.1.jar -o harness.jar

# Run
java -jar harness.jar --version
```

#### Method 3: Build from Source

```bash
# Clone repository
git clone https://github.com/your-org/java-harness.git
cd java-harness

# Build project
mvn clean package

# Run
java -cp java-harness-cli/target/java-harness-cli-4.1.1.jar \
     com.chachamaru.harness.cli.HarnessCli --version
```

### 5-Minute Quick Experience

```bash
# 1. Verify installation
harness --version

# 2. View help information
harness --help

# 3. Test Hook functionality
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"ls -la"}}' | \
  harness hook pre-tool

# 4. Generate project configuration
harness init

# 5. View project status
harness status
```

### Claude Marketplace Installation

If you're using Claude Code, you can install from the Gitee repository using command line:

```bash
# 1. Add plugin source
/plugins marketplace add https://gitee.com/duxvfeng/java-harness.git

# 2. Install plugin
/plugin install
```

**Installation Notes**:
- The first command adds Java Harness to Claude Code's plugin marketplace source
- The second command executes plugin installation, automatically downloading and configuring required components
- After installation, restart Claude Code to start using it
- To update the plugin, simply re-run the above commands

## 📖 Core Features

### Hook System

Complete Claude Code Hook protocol implementation, supporting 14 hook subcommands:

| Hook Subcommand | Description |
|----------------|-------------|
| `hook pre-tool` | Security checks before tool use |
| `hook post-tool` | Tampering detection after tool use |
| `hook permission` | Permission request auto-approval |
| `hook session-start` | Session start environment setup |
| `hook session-init` | Session initialization + Plans.md summary |
| `hook session-cleanup` | Session end temporary file cleanup |
| `hook session-monitor` | Project state collection + session.json |
| `hook session-summary` | Session summary to session-log.md |
| `hook ci-status` | CI status check after push/PR |
| `hook subagent-start` | Agent lifecycle tracking start |
| `hook subagent-stop` | Agent lifecycle tracking stop |
| `hook notification` | Notification event logging |
| `hook permission-denied` | Permission denied event logging |

### CLI Commands

Provides 86 CLI commands covering the following functional categories:

**Core Workflow**:
- `plan` - Generate plan prompt for host execution
- `work <taskID>` - Generate work prompt + task context
- `review <taskID>` - Generate review prompt + task context
- `release` - Generate release prompt for host execution

**Plan Management**:
- `plans check-deps` - Verify completed tasks only depend on closed tasks
- `sprint-contract` - Generate sprint contract from Plans.md

**Evidence Collection**:
- `evidence collect` - Collect evidence (test results, build logs)

**System Management**:
- `doctor` - Health check + migration status/report
- `validate` - Validate SKILL.md / agent frontmatter
- `sync [root]` - Generate CC files from harness.toml
- `init [root]` - Create harness.toml template in project root

### Security Rules (Guardrails)

Comprehensive coverage of 27 security rules (R01-R27):

**System Security**:
- R01: Block privilege escalation commands (sudo, su)
- R02: Protect sensitive paths (/etc, /sys, /proc)
- R03: Block redirection bypass (|, nul)
- R04: Project path boundary checks
- R05: Prevent recursive deletion (rm -rf)

**Git Security**:
- R06: Block forced push (git push --force)
- R11: Hard reset protection (git reset --hard)
- R12: Main branch push protection

**File Security**:
- R07: Codex write monitoring
- R08: Breezing write monitoring
- R09: Key file protection (*.pem, *.key)
- R13: Package file monitoring (package.json, pom.xml)
- R18: Configuration file write protection

**Production Environment Protection**:
- R15: Production deployment protection (kubectl, docker)
- R16: Database write protection
- R17: Container management protection
- R19: Executable download protection
- R20: Network exposure protection
- R25: Service restart protection

## 🏗️ Architecture Design

### Module Structure

```
java-harness/
├── java-harness-cli/              # CLI module (main entry point)
│   └── command/                   # 86 command classes
├── java-harness-shared/           # Shared module
├── java-harness-foundation/       # Foundation module
├── java-harness-protocol/         # Protocol module
├── java-harness-security/         # Security module
├── java-harness-workflow/         # Workflow module
├── java-harness-tools/            # Tools module
├── java-harness-collaboration/    # Collaboration module
├── java-harness-ci/               # CI module
├── java-harness-service/          # Service module
└── java-harness-distribution/     # Distribution module
```

### Technology Stack

- **Language**: Java 17+
- **Build Tool**: Maven
- **CLI Framework**: picocli 4.7
- **JSON Processing**: Jackson 2.15.2
- **YAML Processing**: SnakeYAML
- **Logging**: SLF4J 2.0.9 + Logback 1.4.11
- **Testing**: JUnit 5.10.0
- **Native Compilation**: GraalVM 23.1.0+

### Performance Targets

| Metric | Target | Actual |
|-------|--------|--------|
| Hook Response Time | < 10ms (95th) | ~8ms |
| Workflow Startup | < 100ms | ~85ms |
| Simple Workflow Execution | < 1s | ~0.9s |
| Memory Footprint (Native) | < 50MB | ~42MB |
| Startup Time (Native) | < 100ms | ~75ms |

## 💡 Usage Examples

### Hook Input/Output Example

**Input (stdin)**:
```json
{
  "session_id": "test-session-20260808",
  "transcript_path": "/project/.claude/transcript.jsonl",
  "cwd": "/project",
  "permission_mode": "default",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": {
    "command": "sudo rm -rf /etc/passwd"
  },
  "plugin_root": "/plugin"
}
```

**Output (stdout)**:
```json
{
  "hookEventName": "PreToolUse",
  "permissionDecision": "block",
  "permissionDecisionReason": "R01: Block privilege escalation - sudo usage detected",
  "additionalContext": {
    "ruleId": "R01",
    "ruleName": "Block Privilege Escalation",
    "matched": true
  }
}
```

### Command Usage Examples

```bash
# 1. Generate project configuration
harness init
# Creates .claude/harness.toml configuration file

# 2. Validate configuration
harness validate
# Validates SKILL.md and agent frontmatter formats

# 3. Check dependencies
harness plans check-deps
# Verifies task dependencies in Plans.md

# 4. Generate sprint contract
harness sprint-contract
# Generate sprint-contract.json from Plans.md

# 5. View system status
harness status
# Display all tracked agent states

# 6. Health check
harness doctor
# Run complete system health check
```

## 🧪 Testing

### Running Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=HookCodecTest

# Run specific test method
mvn test -Dtest=HookCodecTest#testDecodePreToolUse
```

### Integration Testing

```bash
# Simulate Hook input test
echo '{"session_id":"test","hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"sudo rm -rf /"}}' | \
  harness hook pre-tool

# Expected output: {"permissionDecision":"block","permissionDecisionReason":"R01: Block privilege escalation"}
```

## 🔧 Development Guide

### Adding New Guardrail Rules

1. Create new rule class in `java-harness-security/src/main/java/com/chachamaru/harness/security/guardrail/rules/`
2. Implement `Rule` interface
3. Register in `GuardrailRegistry`

```java
package com.chachamaru.harness.security.guardrail.rules;

public class R28CustomRule implements Rule {
    @Override
    public String getId() {
        return "R28";
    }

    @Override
    public String getName() {
        return "Custom Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        // Matching condition
        return input.getToolName().equals("CustomTool");
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        // Evaluation logic
        return GuardrailResult.blocked("R28: Custom rule blocked");
    }
}
```

### Adding New CLI Commands

1. Create command class in `java-harness-cli/src/main/java/com/chachamaru/harness/cli/command/`
2. Use picocli annotations to define commands
3. Register in `CommandRegistry`

```java
package com.chachamaru.harness.cli.command;

@Command(name = "my-command", mixinStandardHelpOptions = true,
        description = "My custom command")
public class MyCommand implements Runnable {

    @Override
    public void run() {
        // Command implementation
        System.out.println("Hello from my command!");
    }
}
```

## 📚 Documentation

### Core Documentation

- **[Installation Guide](docs/user-guide/installation.md)** - Detailed installation steps and system requirements
- **[Architecture Documentation](docs/developer-guide/architecture.md)** - Complete architecture design and module description
- **[API Reference](docs/reference/api-reference.md)** - API interface detailed documentation
- **[Documentation Index](docs/README.md)** - Complete documentation navigation

### Reference Documentation

- **[Technical Documentation Backup](docs/reference/backup/)** - Historical technical documentation archive
- **[Superpowers Documentation](docs/reference/superpowers-archive/)** - Temporary documentation archive

## 🤝 Contributing Guidelines

### Development Standards

1. **Follow Project Standards**: Refer to development guidelines in CLAUDE.md
2. **Code Style**: Adhere to Java coding standards
3. **Test Coverage**: Ensure new features have corresponding unit tests
4. **Documentation Updates**: Update relevant documentation to reflect new features

### Submission Process

1. Fork the project
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add some feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Create Pull Request

### Commit Conventions

Follow Conventional Commits specification:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation update
- `style:` Code formatting adjustments
- `refactor:` Code refactoring
- `test:` Testing related
- `chore:` Build process or auxiliary tool changes

## 📄 License

This project maintains the same license as the claude-code-harness parent project.

## 📞 Contact

- **Parent Project**: https://github.com/your-org/claude-code-harness
- **Issue Reporting**: [GitHub Issues](https://github.com/your-org/java-harness/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/java-harness/discussions)

## 🙏 Acknowledgments

Thanks to the claude-code-harness parent project for providing design specifications and technical guidance.

---

**Version**: 4.1.1
**Last Updated**: 2026-08-08
**Maintainer**: Java Harness Team
