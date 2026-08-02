#!/bin/bash
set -e

echo "==================================="
echo "Setting Up Existing Project for Java Harness"
echo "==================================="

PROJECT_DIR="${1:-.}"

if [ ! -d "$PROJECT_DIR" ]; then
    echo "Error: Project directory does not exist: $PROJECT_DIR"
    exit 1
fi

cd "$PROJECT_DIR"

echo ""
echo "Setting up Java Harness in: $PROJECT_DIR"

# Check if it's already a Java Harness project
if [ -d ".claude" ]; then
    echo "⚠️  This directory already contains .claude configuration"
    read -p "Continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Detect project type
echo ""
echo "Detecting project type..."
if [ -f "pom.xml" ]; then
    PROJECT_TYPE="maven"
    echo "✅ Detected Maven project"
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    PROJECT_TYPE="gradle"
    echo "✅ Detected Gradle project"
else
    PROJECT_TYPE="generic"
    echo "⚠️  No specific build system detected, using generic setup"
fi

# Create Java Harness directory structure
echo ""
echo "Creating Java Harness directory structure..."
mkdir -p .claude/state
mkdir -p .claude/scripts
mkdir -p docs

# Create settings.json
echo ""
echo "Creating .claude/settings.json..."
cat > .claude/settings.json << 'EOF'
{
  "permissions": {
    "allowedCommands": []
  },
  "hooks": {
    "enabled": true,
    "preToolUse": {
      "enabled": true
    },
    "postToolUse": {
      "enabled": true
    }
  },
  "workflow": {
    "mode": "solo"
  },
  "testing": {
    "framework": "junit"
  }
}
EOF

# Create Plans.md if it doesn't exist
if [ ! -f "Plans.md" ]; then
    echo ""
    echo "Creating Plans.md..."
    cat > Plans.md << 'EOF'
# Project Plans

## Project Analysis

**Project Type**: Auto-detected during setup
**Setup Date**: $(date +%Y-%m-%d)

## Current Tasks

| Task | Description | Status | Priority |
|------|-------------|--------|----------|
| 1.1 | Analyze existing codebase | TODO | High |
| 1.2 | Set up Java Harness integration | TODO | High |
| 1.3 | Configure hooks and workflows | TODO | Medium |

## Notes

This Plans.md was auto-generated during Java Harness setup.
Update it with your actual project tasks and requirements.

## Definition of Done

- [ ] Project analysis completed
- [ ] Java Harness fully integrated
- [ ] All workflows configured
- [ ] Team trained on Java Harness usage
EOF
fi

# Create .gitignore entries
echo ""
echo "Updating .gitignore..."
if [ -f ".gitignore" ]; then
    if ! grep -q ".claude/state/" .gitignore; then
        echo "" >> .gitignore
        echo "# Java Harness state files" >> .gitignore
        echo ".claude/state/" >> .gitignore
        echo ".claude/scripts/" >> .gitignore
        echo "session-log.md" >> .gitignore
    fi
else
    cat > .gitignore << 'EOF'
# Java Harness state files
.claude/state/
.claude/scripts/
session-log.md

# Build outputs
target/
*.jar

# IDE
.idea/
.vscode/
*.iml
EOF
fi

# Analyze dependencies
echo ""
echo "Analyzing project dependencies..."
if [ "$PROJECT_TYPE" = "maven" ]; then
    echo "Maven dependencies found:"
    mvn dependency:list 2>/dev/null | grep "INFO" | grep -v "BUILD SUCCESS" | head -10 || echo "Could not analyze Maven dependencies"
elif [ "$PROJECT_TYPE" = "gradle" ]; then
    echo "Gradle dependencies found:"
    ./gradlew dependencies 2>/dev/null | head -10 || echo "Could not analyze Gradle dependencies"
fi

# Create setup documentation
echo ""
echo "Creating setup documentation..."
cat > docs/JAVA_HARNESS_SETUP.md << 'EOF'
# Java Harness Setup Guide

This project has been set up with Java Harness.

## Quick Start

1. **Review Configuration**: Check `.claude/settings.json`
2. **Update Plans**: Edit `Plans.md` with your project tasks
3. **Start Working**: Use Java Harness workflows

## Available Scripts

Java Harness provides several scripts for project management:

- `build.sh` - Build the project
- `test.sh` - Run tests
- `clean.sh` - Clean build artifacts

## Configuration Files

- `.claude/settings.json` - Main configuration
- `Plans.md` - Project tasks and plans
- `.gitignore` - Updated with Java Harness patterns

## Next Steps

1. Customize `.claude/settings.json` for your needs
2. Update `Plans.md` with your actual tasks
3. Configure workflows and hooks
4. Start using Java Harness for your development

## Support

For more information, see the Java Harness documentation.
EOF

echo ""
echo "✅ Setup completed successfully!"
echo ""
echo "Summary:"
echo "  - Project type: $PROJECT_TYPE"
echo "  - .claude directory: Created"
echo "  - Plans.md: Created"
echo "  - .gitignore: Updated"
echo "  - Documentation: Created"
echo ""
echo "Next steps:"
echo "  1. Review .claude/settings.json"
echo "  2. Update Plans.md with your tasks"
echo "  3. Start using Java Harness!"