# Multilingual Code Standards Feature

## Overview

The Multilingual Code Standards feature extends the Java Harness code review system to support multiple programming languages with automatic language detection and standards application. This feature enables developers to receive language-specific code review feedback based on industry-standard coding practices.

## Supported Languages

### Java
- **Standard**: Alibaba Java Development Guide (黄山版)
- **Files**: `.java`
- **Severity**: Major
- **Integration**: Full `alibaba-java-development-guide` skill integration

### Python
- **Standard**: PEP 8 + Python Best Practices
- **Files**: `.py`, `.pyi`
- **Severity**: Moderate
- **Focus**: Code style, naming conventions, best practices

### Vue.js
- **Standard**: Vue Style Guide
- **Files**: `.vue`
- **Severity**: Moderate
- **Focus**: Component structure, naming, patterns

### Go
- **Standard**: Effective Go + Go Code Review Comments
- **Files**: `.go`
- **Severity**: Major
- **Focus**: Idiomatic Go, error handling, concurrency

## Quick Start

### Basic Usage

```bash
# Review all changed files (automatic language detection)
/harness-review

# Review specific files
/harness-review src/main/java/UserService.java
/harness-review app/services/user_processor.py
/harness-review components/UserProfile.vue
/harness-review cmd/server/main.go
```

### Expected Output

When reviewing code files, the system will:

1. **Detect Language**: Identify the programming language from file extension/content
2. **Apply Standards**: Load and apply the appropriate coding standards
3. **Generate Findings**: Provide specific, actionable feedback
4. **Suggest Improvements**: Offer best practices and examples

Example output for Java code:
```
Language: java
Standards: alibaba-java-development-guide
Severity: major

Findings:
⚠️ [MAJOR] Class naming convention violated
  File: UserService.java:15
  Rule: Class names should use PascalCase
  Current: class userService
  Suggested: class UserService

⚠️ [MAJOR] Exception handling incomplete
  File: UserService.java:42
  Rule: Always handle exceptions, never ignore them
  Current: } catch (Exception e) {
  Suggested: } catch (SpecificException e) {
              logger.error("Operation failed", e);
              throw e;
```

## Configuration

### Configuration File

The main configuration file is located at `.claude/config/code-standards.config.json`:

```json
{
  "version": "1.0",
  "languageMapping": {
    "java": {
      "standards": ["alibaba-java-development-guide"],
      "extensions": [".java"],
      "defaultSeverity": "major",
      "reviewScope": "full",
      "skillIntegration": true,
      "skillName": "alibaba-java-development-guide"
    }
  }
}
```

### Customizing Standards

You can customize the standards for each language:

```json
{
  "languageMapping": {
    "python": {
      "standards": ["pep-8", "pylint", "black"],
      "defaultSeverity": "moderate",
      "reviewScope": "full",
      "exemptionPatterns": ["generated", "third-party"]
    }
  }
}
```

### Severity Levels

Configure severity levels for different rule categories:

```json
{
  "severityMapping": {
    "critical": {
      "level": 1,
      "description": "Must fix - causes security, safety, or correctness issues",
      "blockMerge": true,
      "icon": "🚨"
    },
    "major": {
      "level": 2,
      "description": "Should fix - violates important standards",
      "blockMerge": false,
      "icon": "⚠️"
    }
  }
}
```

## Integration with Planning Skills

### Harness Plan Integration

The planning skills now include brainstorming integration for creative exploration:

```bash
# Create a plan with brainstorming
/harness-plan create "Add real-time collaboration feature"

# The system will:
# 1. Detect this is a complex feature requiring creative exploration
# 2. Automatically invoke brainstorming skill
# 3. Generate multiple implementation approaches
# 4. Provide creative options and technical alternatives
# 5. Integrate results into the planning process
```

### Harness Plan Brief Enhancement

Plan brief generation now uses brainstorming to provide:

- **Multiple Implementation Options**: Different approaches to solving the problem
- **Risk Assessment**: Creative identification of potential risks
- **Technical Alternatives**: Various technology choices and architectures
- **Enhanced Decision Support**: Comprehensive information for decision-making

## Language-Specific Examples

### Java Code Review

**Good Example**:
```java
package com.example.service;

import org.springframework.stereotype.Service;
import java.util.List;

/**
 * User service for managing user operations.
 */
@Service
public class UserService {
    private static final int MAX_RETRY = 3;
    
    /**
     * Gets user by ID.
     * @param userId the user ID
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    public User getUserById(String userId) throws UserNotFoundException {
        // Implementation with proper error handling
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
```

**Common Issues Found**:
- Class naming not following PascalCase
- Missing JavaDoc comments
- Improper exception handling
- Magic numbers without constants
- Collection processing without proper checks

### Python Code Review

**Good Example**:
```python
"""
User service module for managing user operations.
"""

from typing import List, Optional
from dataclasses import dataclass


@dataclass
class User:
    """User data class."""
    id: str
    name: str
    email: str


class UserService:
    """Service class for user operations."""
    
    def get_user_by_id(self, user_id: str) -> Optional[User]:
        """Get user by ID.
        
        Args:
            user_id: The user ID to search for
            
        Returns:
            The user if found, None otherwise
            
        Raises:
            ValueError: If user_id is empty
        """
        if not user_id:
            raise ValueError("user_id cannot be empty")
            
        return self.repository.find_by_id(user_id)
```

**Common Issues Found**:
- Missing docstrings
- No type hints
- Import organization issues
- Line length violations
- Inconsistent naming conventions

### Vue Component Review

**Good Example**:
```vue
<template>
  <div class="user-profile">
    <UserProfileHeader :user="user" />
    <UserProfileStats :stats="userStats" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface User {
  id: string
  name: string
  email: string
}

interface Props {
  userId: string
}

const props = defineProps<Props>()
const user = ref<User | null>(null)

const userStats = computed(() => {
  if (!user.value) return null
  return {
    posts: user.value.posts?.length || 0,
    followers: user.value.followers?.length || 0
  }
})
</script>

<style scoped>
.user-profile {
  padding: 1rem;
  border-radius: 4px;
}
</style>
```

**Common Issues Found**:
- Single-word component names
- Missing `:key` with `v-for`
- `v-if` and `v-for` on same element
- Uncased component names in templates
- Missing type definitions

### Go Code Review

**Good Example**:
```go
// Package user provides user management functionality.
package user

import (
    "context"
    "fmt"
)

// UserService handles user operations.
type UserService struct {
    repository UserRepository
}

// GetUserByID retrieves a user by their ID.
func (s *UserService) GetUserByID(ctx context.Context, id string) (*User, error) {
    if id == "" {
        return nil, fmt.Errorf("user ID cannot be empty")
    }
    
    user, err := s.repository.FindByID(ctx, id)
    if err != nil {
        return nil, fmt.Errorf("finding user: %w", err)
    }
    
    if user == nil {
        return nil, fmt.Errorf("user not found: %s", id)
    }
    
    return user, nil
}
```

**Common Issues Found**:
- Ignoring errors
- Not using `defer` for cleanup
- Poor error messages
- Inefficient goroutine usage
- Missing context usage

## Advanced Usage

### Exemption Handling

For code that should be exempt from certain standards:

```java
// Alibaba-Java: 豁免原因——性能敏感代码，需要内联优化
public class PerformanceCriticalClass {
    // Implementation that deviates from standard
}
```

### Custom Rule Categories

Add custom rule categories in configuration:

```json
{
  "ruleCategories": {
    "security": {
      "description": "Security vulnerabilities and best practices",
      "languages": ["java", "python", "go"],
      "defaultSeverity": "critical"
    }
  }
}
```

### Multi-language Projects

For projects with multiple languages:

```bash
# Review all files (auto-detects each language)
/harness-review

# Results will be grouped by language:
# Language: java - 5 findings
# Language: python - 3 findings
# Language: vue - 2 findings
```

## Troubleshooting

### Language Detection Issues

**Problem**: Wrong language detected
**Solution**: Check file extensions and content patterns in configuration

```bash
# Verify language detection
bash tests/code-standards/test-detection.sh
```

### Standards Not Applied

**Problem**: Expected standards not being applied
**Solution**: Verify skill integration and reference documents

```bash
# Run validation
bash tests/code-standards/validate.sh
```

### Performance Issues

**Problem**: Slow review on large files
**Solution**: Check caching configuration and file size limits

## Best Practices

### For Development Teams

1. **Standardize Configuration**: Use the same configuration across all team members
2. **Gradual Adoption**: Start with major issues and gradually address minor ones
3. **Custom Rules**: Add project-specific standards to the configuration
4. **Regular Updates**: Keep standards and reference documents updated

### For Individual Developers

1. **Trust the System**: Let automatic detection work when possible
2. **Learn Standards**: Read the reference documents to understand rules
3. **Provide Feedback**: Report issues or suggest improvements
4. **Use Exemptions Wisely**: Only exempt when truly necessary

### For CI/CD Integration

1. **Automated Reviews**: Integrate into pull request workflows
2. **Quality Gates**: Use severity levels to enforce standards
3. **Metrics Tracking**: Track compliance over time
4. **Team Notifications**: Alert team to critical issues

## API and Extensions

### Programmatic Usage

```bash
# Review specific files programmatically
/harness-review --files="src/**/*.java" --output=json
```

### Custom Standards

Create custom standards by adding reference documents:

```bash
# Add custom standard
mkdir -p skills/harness-review/references/code-standards/
cp my-custom-standard.md skills/harness-review/references/code-standards/
```

Update configuration to reference custom standard.

## Performance Benchmarks

Based on testing with typical code files:

| Language | File Size | Detection Time | Review Time | Total Time |
|----------|-----------|----------------|-------------|------------|
| Java     | 500 lines | <0.1s          | 2-3s        | 2-3s       |
| Python   | 500 lines | <0.1s          | 1-2s        | 1-2s       |
| Vue      | 300 lines | <0.1s          | 1-2s        | 1-2s       |
| Go       | 400 lines | <0.1s          | 2-3s        | 2-3s       |

## Future Enhancements

Planned improvements:

- **Additional Languages**: Support for more programming languages
- **ML-Based Detection**: Improved language and pattern detection
- **Cross-Language Analysis**: Consistency checks across language boundaries
- **IDE Integration**: Real-time feedback in popular IDEs
- **Team Analytics**: Dashboard showing team compliance metrics

## Contributing

To contribute new language standards or improvements:

1. Follow the existing reference document structure
2. Include comprehensive examples and use cases
3. Add test cases to the test suite
4. Update configuration schema if needed
5. Run validation scripts before submitting

## License and Credits

- **Java Standards**: Based on Alibaba Java Development Guide (黄山版)
- **Python Standards**: Based on PEP 8 and Python community best practices
- **Vue Standards**: Based on official Vue.js Style Guide
- **Go Standards**: Based on Effective Go and Go Code Review Comments

## Support and Documentation

- **Issues**: Report problems via project issue tracker
- **Documentation**: See reference documents in `skills/harness-review/references/code-standards/`
- **Testing**: Run `bash tests/code-standards/validate.sh` for system checks

---

**Version**: 1.0  
**Last Updated**: 2025-01-09  
**Maintained By**: harness-core team
