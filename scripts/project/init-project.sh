#!/bin/bash
set -e

echo "==================================="
echo "Initializing Java Harness Project"
echo "==================================="

PROJECT_NAME="${1:-java-harness-project}"
PROJECT_DIR="${2:-.}"

echo ""
echo "Initializing project: $PROJECT_NAME"
echo "Target directory: $PROJECT_DIR"

# Check if directory exists
if [ ! -d "$PROJECT_DIR" ]; then
    echo "Creating project directory: $PROJECT_DIR"
    mkdir -p "$PROJECT_DIR"
fi

cd "$PROJECT_DIR"

# Create standard project structure
echo ""
echo "Creating project structure..."
mkdir -p src/main/java
mkdir -p src/main/resources
mkdir -p src/test/java
mkdir -p src/test/resources
mkdir -p .claude/state
mkdir -p .claude/scripts
mkdir -p docs

# Create basic pom.xml
echo ""
echo "Creating Maven pom.xml..."
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>harness-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Java Harness Project</name>
    <description>Project initialized with Java Harness</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# Create basic .claude configuration
echo ""
echo "Creating .claude configuration..."
cat > .claude/settings.json << 'EOF'
{
  "permissions": {
    "allowedCommands": [
      "mvn",
      "java",
      "git"
    ]
  },
  "hooks": {
    "enabled": true
  },
  "workflow": {
    "mode": "solo"
  }
}
EOF

# Create basic Plans.md template
echo ""
echo "Creating Plans.md template..."
cat > Plans.md << 'EOF'
# Project Plans

## Current Sprint

### Tasks

| Task | Description | Status | Priority |
|------|-------------|--------|----------|
| 1.1 | Initial project setup | TODO | High |
| 1.2 | Core functionality implementation | TODO | High |
| 1.3 | Testing and validation | TODO | Medium |

## Definition of Done

- [ ] All tasks completed
- [ ] Tests passing
- [ ] Documentation updated
- [ ] Code reviewed
EOF

# Create README.md
echo ""
echo "Creating README.md..."
cat > README.md << 'EOF'
# Java Harness Project

This project was initialized with Java Harness.

## Getting Started

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

## Project Structure

- `src/main/java` - Main source code
- `src/test/java` - Test code
- `.claude/` - Java Harness configuration
- `docs/` - Project documentation

## Configuration

See `.claude/settings.json` for Java Harness specific configuration.
EOF

echo ""
echo "✅ Project initialization completed successfully!"
echo ""
echo "Project location: $PROJECT_DIR"
echo ""
echo "Next steps:"
echo "  1. Review and customize pom.xml"
echo "  2. Update Plans.md with your tasks"
echo "  3. Configure .claude/settings.json"
echo "  4. Start development!"