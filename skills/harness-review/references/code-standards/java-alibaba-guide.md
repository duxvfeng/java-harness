# Java Code Standards - Alibaba Java Development Guide

## Overview

This reference document describes the integration of the Alibaba Java Development Guide (黄山版) with the harness-review skill for Java code review.

## Standards Source

**Alibaba Java Development Guide (黄山版)** v1.7.1
- Released: 2022.02.03
- Coverage: 7 major dimensions covering naming, exceptions, logging, testing, security, database, and design standards
- Skill: `alibaba-java-development-guide`

## Integration Method

The harness-review skill automatically invokes the `alibaba-java-development-guide` skill when reviewing Java code files (`.java` extension).

## Standards Categories

The Alibaba guide covers these major areas:

1. **Programming Standards** (编程规约)
   - Naming conventions
   - Constant definitions
   - Code formatting
   - OOP principles
   - Date/time handling
   - Collection processing
   - Concurrency handling
   - Control statements
   - Annotation conventions
   - Front-end/backend conventions

2. **Exception & Logging** (异常日志)
   - Error code systems
   - Exception handling
   - Try-catch-finally usage
   - NPE prevention
   - Logging framework/levels/output

3. **Unit Testing** (单元测试)
   - AIR principles
   - Test independence
   - Coverage targets
   - BCDE principles
   - Mock usage
   - Test directory structure

4. **Security Standards** (安全规约)
   - Permission validation
   - Sensitive data masking
   - SQL injection prevention
   - XSS/CSRF protection
   - Parameter validation
   - File upload security
   - Replay attack prevention

5. **MySQL Database** (MySQL数据库)
   - Table creation standards
   - Field type selection
   - Primary key/index naming
   - Composite indexes
   - Pagination optimization
   - COUNT/SUM optimization
   - ORM usage
   - ResultMap configuration

6. **Project Structure** (工程结构)
   - Application layering (Web/Service/Manager/DAO)
   - DO/DTO/BO/VO/Query definitions
   - Library dependency management
   - GAV coordinates
   - Server and JVM configuration

7. **Design Standards** (设计规约)
   - Storage solution review
   - Use case/state/sequence/class/activity diagrams
   - Weak dependency and degradation
   - SOLID/DRY principles
   - System design patterns

## Rule Severity Levels

Each rule in the Alibaba guide is classified by severity:

| Level | Meaning | Review Priority |
|-------|---------|-----------------|
| **【强制】** (Mandatory) | Must be strictly followed | Highest priority - violations can cause serious issues |
| **【推荐】** (Recommended) | Should be followed | Medium priority - helps improve code quality |
| **【参考】** (Reference) | Reference suggestions | Low priority - flexible application |

## Usage in Code Review

When the harness-review skill detects Java code, it:

1. **Language Detection**: Identifies `.java` files
2. **Standards Routing**: Routes to Alibaba Java Development Guide
3. **Rule Application**: Applies appropriate standards based on code context
4. **Issue Reporting**: Reports findings with Alibaba rule references

## Trigger Keywords

The Alibaba skill automatically activates for these Java-related keywords:

- JWT, OAuth2, Token, @PreAuthorize, authentication, authorization
- Data masking, encryption, sensitive data
- Thread pools, @Async, CompletableFuture
- Locks, synchronized, ReentrantLock, distributed locks
- Date/time, LocalDateTime, timezones
- Exceptions, NPE, try-catch, @ControllerAdvice
- Logging, @Slf4j, log levels
- Controllers, Services, DAOs, DDD
- DO, DTO, VO, BO, entity mapping
- Dependencies, Maven, Gradle, pom.xml

## Applicability Scope

| Scenario | Recommendation |
|----------|----------------|
| Production code development/review | ✅ **Full enable** - all rules apply |
| Quick prototypes, demos, one-time scripts | 🔸 **【推荐】rules can be exempted** - only follow 【强制】 |
| High-performance sensitive code | 🔸 Method length, complexity rules can be exempted with documentation |
| Public SDK/API design | ✅ **Full enable** - strictly follow naming and compatibility rules |
| Refactoring existing code | 🔸 Prioritize "preserve behavior" - align progressively |
| User explicitly "disable Alibaba standards" | 🛑 **Complete disable** - don't apply any rules |

## Conflict Resolution

When multiple rules conflict with each other:

1. **Security First**: Security rules have highest priority
2. **Mandatory > Recommended > Reference**: Mandatory rules override recommendations
3. **Business Exceptions**: Document with comments: `// Alibaba-Java: 豁免原因——<specific reason>`
4. **Progressive Improvement**: For refactoring, prioritize maintaining behavior over perfect compliance

## Examples of Rule Application

### Naming Convention (Mandatory)

**❌ Violation:**
```java
class user {
    String username;
    void getdata() {}
}
```

**✅ Correct:**
```java
class User {
    String username;
    void getData() {}
}
```

### Collection Processing (Mandatory)

**❌ Violation:**
```java
// SubList operations
List<String> subList = list.subList(0, 5);
subList.add("new"); // Modifies parent list
```

**✅ Correct:**
```java
List<String> subList = new ArrayList<>(list.subList(0, 5));
subList.add("new"); // Safe operation
```

### Exception Handling (Mandatory)

**❌ Violation:**
```java
try {
    // code
} catch (Exception e) {
    // Ignore exception
}
```

**✅ Correct:**
```java
try {
    // code
} catch (SpecificException e) {
    logger.error("Operation failed", e);
    // Handle or rethrow
}
```

## Performance Considerations

- **Large Codebases**: The skill uses lazy loading - only reads relevant sections when needed
- **Incremental Analysis**: Can analyze files individually without full project scan
- **Caching**: Frequently accessed standards are cached for performance

## Security & Privacy

- **No External APIs**: All standards are reference documents or local skills
- **Code Privacy**: Java code analysis stays within local environment
- **Skill Sandboxing**: External Alibaba skill runs in isolated context

## Configuration

The integration can be configured via `.claude/config/code-standards.config.json`:

```json
{
  "languageMapping": {
    "java": {
      "standards": ["alibaba-java-development-guide"],
      "extensions": [".java"],
      "defaultSeverity": "major",
      "reviewScope": "full"
    }
  }
}
```

## Future Enhancements

Potential future improvements:
1. Custom rule overrides for team-specific standards
2. Integration with static analysis tools
3. Automatic fix suggestions for common violations
4. Metrics and reporting on rule compliance

## References

- Original: [Alibaba Java Development Guide](https://github.com/alibaba/p3c)
- Skill: `alibaba-java-development-guide`
- Related documents: See `architecture.md` for overall multilingual standards architecture
