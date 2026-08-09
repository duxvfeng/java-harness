# Multilingual Code Standards Architecture

## Overview

This document describes the architecture for multilingual code standards support in the Java Harness code review system. The architecture enables language-specific code review standards to be applied automatically based on detected programming languages in the codebase.

## Design Principles

1. **Language-Aware Detection**: Automatically detect programming languages from file extensions and content analysis
2. **Standards Routing**: Route code review requests to appropriate language-specific standards
3. **Configuration-Driven**: Use declarative configuration files for standards mapping
4. **Extensibility**: Easy to add new language standards without core code changes
5. **Skill Integration**: Integrate with existing Claude Skills framework for specialized standards

## Architecture Components

### 1. Language Detection Layer

```
LanguageDetector
├── Extension-based Detection
│   ├── .java → Java
│   ├── .py → Python
│   ├── .vue → Vue
│   ├── .go → Go
│   └── ... (extensible)
├── Content-based Detection (fallback)
│   ├── Shebang analysis
│   ├── Syntax pattern matching
│   └── Framework heuristics
└── Multi-language File Handling
    ├── .vue files (HTML + CSS + JS)
    ├── .md files with code blocks
    └── Template files
```

### 2. Standards Mapping System

```
StandardsMapper
├── Language → Standards Reference
│   ├── Java → Alibaba Java Development Guidelines
│   ├── Python → PEP 8 + Python Best Practices
│   ├── Vue → Vue Style Guide
│   ├── Go → Effective Go + Go Code Review Comments
│   └── JavaScript/TypeScript → StandardJS/Airbnb (future)
├── File Type → Review Scope
│   ├── Source files → Full standards review
│   ├── Test files → Test-specific standards
│   ├── Config files → Minimal review
│   └── Documentation → Style check only
└── Priority Resolution
    ├── Multiple standards per language
    ├── Project-specific overrides
    └── User preferences
```

### 3. Rule Application Framework

```
RuleEngine
├── Standard Loader
│   ├── Load reference documents
│   ├── Parse rule definitions
│   └── Cache for performance
├── Rule Executor
│   ├── Apply language-specific checks
│   ├── Severity classification
│   └── Context-aware evaluation
└── Integration Layer
    ├── harness-review skill integration
    ├── External skill integration (Alibaba, brainstorming)
    └── Custom rule hooks
```

### 4. Configuration Structure

**Optimized Approach: SKILL.md-Based Routing** (Updated 2026-08-09)

Instead of external configuration files, the architecture uses SKILL.md file routing:

```yaml
# In skills/harness-review/SKILL.md
---
name: harness-review
description: "HAR: Multi-angle code review with auto language detection. Supports: Java (Alibaba), Python (PEP 8), Vue (Style Guide), Go (Effective Go). Auto-detects .java/.py/.vue/.go files and applies language-specific standards."
description-zh: "HAR：多角度代码审查，自动检测语言并应用标准。支持：Java（阿里巴巴）、Python（PEP 8）、Vue（风格指南）、Go（Effective Go）。"
trigger: "review, code review, java review, python review, vue review, go review"

# Language Standards Support (embedded in SKILL.md)
language_standards:
  enabled: true
  auto_detect: true
  routing_method: "skill-description-based"

  java:
    standard: "alibaba-java-development-guide"
    extensions: [".java"]
    default_severity: "major"
    review_scope: "full"
    skill_integration: "alibaba-java-development-guide"

  python:
    standard: "pep8-best-practices"
    extensions: [".py", ".pyi"]
    default_severity: "moderate"
    review_scope: "full"
    reference_doc: "references/code-standards/python-pep8.md"

  vue:
    standard: "vue-style-guide"
    extensions: [".vue"]
    default_severity: "moderate"
    review_scope: "component"
    reference_doc: "references/code-standards/vue-style-guide.md"

  go:
    standard: "effective-go"
    extensions: [".go"]
    default_severity: "major"
    review_scope: "full"
    reference_doc: "references/code-standards/go-effective-go.md"
---
```

**Benefits of SKILL.md-Based Approach**:
- ✅ **Self-contained**: All configuration in one place
- ✅ **No external files**: Eliminates configuration file maintenance
- ✅ **Skill framework compliant**: Uses existing skill mechanisms
- ✅ **Simplified architecture**: Reduces complexity and dependencies

## Implementation Strategy

### Phase 1: Core Infrastructure
1. Create language detection system
2. Implement standards mapper
3. Set up configuration file structure
4. Build integration hooks with harness-review

### Phase 2: Language Standards Integration
1. Java: Integrate alibaba-java-development-guide skill
2. Python: Create PEP 8 reference document
3. Vue: Create Vue Style Guide reference document
4. Go: Create Effective Go reference document

### Phase 3: Planning Enhancement
1. Integrate brainstorming into harness-plan
2. Integrate brainstorming into harness-plan-brief
3. Enable creative exploration during planning phases

### Phase 4: Testing and Validation
1. Test each language standard application
2. Verify language detection accuracy
3. Validate standards routing logic
4. Performance testing

## Integration Points

### With harness-review Skill (Updated 2026-08-09)
- **Input**: File list, language detection
- **Process**: Apply language-specific standards via SKILL.md routing
- **Output**: Language-aware review findings
- **Configuration**: SKILL.md description-based routing (no external config files)
- **Detection Method**: File extension → content pattern → standard mapping
- **Standards Storage**: references/code-standards/ directory

### With harness-plan Skill
- **Trigger**: Before planning complex features
- **Process**: Call brainstorming for creative exploration
- **Output**: Enhanced plan with creative alternatives
- **Configuration**: Skill metadata modification

### With External Skills
- **Alibaba Java Development Guide**: Direct integration via Claude Skills framework
- **Brainstorming**: Integration via skill orchestration
- **Future Standards**: Extensible via configuration

## File Structure (Updated 2026-08-09)

**Optimized Structure - SKILL.md-Based Routing**:

```
skills/harness-review/
├── SKILL.md (modified for multilingual support via description routing)
├── references/
│   ├── code-standards/
│   │   ├── architecture.md (this file) ✅ COMPLETED
│   │   ├── java-alibaba-guide.md (to be created)
│   │   ├── python-pep8.md (to be created)
│   │   ├── vue-style-guide.md (to be created)
│   │   └── go-effective-go.md (to be created)
│   └── [existing reference files]
│
│   NO CONFIGURATION FILES NEEDED - All routing via SKILL.md
│

skills/harness-plan/
├── SKILL.md (to be modified for brainstorming integration)
└── references/
    └── [existing reference files]

skills/harness-plan-brief/
├── SKILL.md (to be modified for brainstorming integration)
└── references/
    └── [existing reference files]
```

**Key Changes**:
- ❌ Removed `.claude/config/code-standards.config.json` (not needed)
- ✅ All routing through SKILL.md description and metadata
- ✅ Reference documents for detailed standards information
- ✅ External skill integration (alibaba-java-development-guide)

## Language Detection Logic

```python
class LanguageDetector:
    def detect_language(self, file_path, file_content):
        # Primary: File extension mapping
        ext = self.get_extension(file_path)
        if ext in EXTENSION_MAP:
            return EXTENSION_MAP[ext]

        # Secondary: Content analysis
        return self.analyze_content(file_content)

    def analyze_content(self, content):
        # Shebang detection
        if content.startswith('#!'):
            return self.parse_shebang(content)

        # Framework patterns
        if 'import java.' in content:
            return 'java'
        elif 'import ' in content or 'from ' in content:
            return 'python'
        elif 'package main' in content:
            return 'go'
        elif '<template>' in content:
            return 'vue'

        return 'unknown'
```

## Standards Routing Flow

```
1. File Input → Language Detection
2. Language Lookup → Standards Configuration
3. Standards Load → Reference Documents/Skill Integration
4. Rule Application → Code Review Analysis
5. Findings Generation → Language-Specific Issues
6. Report Output → Categorized by Language and Severity
```

## Error Handling and Fallbacks

1. **Unknown Language**: Apply general code quality standards
2. **Missing Standards**: Use minimal checks with warning
3. **Configuration Errors**: Fallback to hardcoded defaults
4. **Skill Unavailable**: Continue with reference documents only

## Performance Considerations

1. **Caching**: Cache language detection results
2. **Lazy Loading**: Load standards only when needed
3. **Parallel Processing**: Process multiple languages concurrently
4. **Incremental Updates**: Update only changed language configurations

## Security and Privacy

1. **No External API Calls**: All standards are reference documents or local skills
2. **File Content Privacy**: Code analysis stays within local environment
3. **Configuration Validation**: Validate all configuration inputs
4. **Skill Sandboxing**: External skills run in isolated context

## Future Extensibility

1. **New Languages**: Add to configuration file without code changes
2. **Custom Standards**: Support project-specific rule sets
3. **Machine Learning**: Future integration with ML-based code analysis
4. **IDE Integration**: Potential for real-time language-specific feedback

## Monitoring and Metrics

1. **Language Detection Accuracy**: Track correct detections
2. **Standards Application Rate**: Measure how often standards are applied
3. **False Positive Rate**: Monitor incorrect standard applications
4. **Performance Metrics**: Track detection and routing speed

This architecture provides a robust, extensible foundation for multilingual code standards support while maintaining backward compatibility with existing harness functionality.
