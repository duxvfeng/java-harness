#!/bin/bash
set -e

echo "==================================="
echo "Documentation Generator"
echo "==================================="

DOC_TYPE="${1:-api}"
OUTPUT_DIR="docs/generated"
mkdir -p "$OUTPUT_DIR"

case "$DOC_TYPE" in
    "api")
        echo ""
        echo "## API Documentation Generation"
        echo "=============================="

        echo "Generating API documentation..."

        if command -v javadoc &> /dev/null; then
            echo "Running JavaDoc..."
            javadoc -d "$OUTPUT_DIR/api" \
                -sourcepath src/main/java \
                -subpackages com.chachamaru.harness \
                -quiet 2>/dev/null || echo "JavaDoc generation had warnings"

            echo "✅ API documentation generated: $OUTPUT_DIR/api"
        else
            echo "⚠️  JavaDoc not found, creating basic API docs..."

            # Create basic API documentation
            find src/main/java -name "*.java" | while read -r java_file; do
                class_name=$(basename "$java_file" .java)
                package=$(grep "^package " "$java_file" | sed 's/package //;s/;//')

                echo "## $package.$class_name" >> "$OUTPUT_DIR/api.md"
                echo "" >> "$OUTPUT_DIR/api.md"
                grep -A10 "/\*\*" "$java_file" | sed 's/\*\///g' | sed 's/^ \* //g' >> "$OUTPUT_DIR/api.md" || true
                echo "" >> "$OUTPUT_DIR/api.md"
            done

            echo "✅ Basic API documentation generated: $OUTPUT_DIR/api.md"
        fi
        ;;

    "readme")
        echo ""
        echo "## README Generation"
        echo "===================="

        README_FILE="README.md"

        {
            echo "# Java Harness"
            echo ""
            echo "Claude Code Harness - Java implementation"
            echo ""
            echo "## Quick Start"
            echo ""
            echo "\`\`\`bash"
            echo "# Build the project"
            echo "./build.sh"
            echo ""
            echo "# Run tests"
            echo "./test.sh"
            echo ""
            echo "# Package"
            echo "./package.sh"
            echo "\`\`\`"
            echo ""
            echo "## Project Structure"
            echo ""
            echo "- \`src/main/java\` - Main source code"
            echo "- \`src/test/java\` - Test code"
            echo "- \`.claude/\` - Java Harness configuration"
            echo "- \`docs/\` - Project documentation"
            echo ""
            echo "## Configuration"
            echo ""
            echo "See \`.claude/settings.json\` for configuration options."
            echo ""
            echo "## License"
            echo ""
            echo "See LICENSE file for details."
            echo ""
            echo "---"
            echo "*Generated: $(date '+%Y-%m-%d %H:%M:%S')*"

        } > "$README_FILE"

        echo "✅ README generated: $README_FILE"
        ;;

    "changelog")
        echo ""
        echo "## Changelog Generation"
        echo "======================"

        CHANGELOG_FILE="CHANGELOG.md"

        {
            echo "# Changelog"
            echo ""
            echo "All notable changes to this project will be documented in this file."
            echo ""
            echo "## [Unreleased]"
            echo ""
            echo "### Added"
            echo "- New features"
            echo ""
            echo "### Changed"
            echo "- Changed features"
            echo ""
            echo "### Fixed"
            echo "- Bug fixes"
            echo ""
            echo "## ["$(date '+%Y-%m-%d')"]"
            echo ""
            echo "### Added"
            echo "- Initial release features"
            echo ""
            echo "---"
            echo "*Generated: $(date '+%Y-%m-%d %H:%M:%S')*"

        } > "$CHANGELOG_FILE"

        echo "✅ Changelog generated: $CHANGELOG_FILE"
        ;;

    "architecture")
        echo ""
        echo "## Architecture Documentation"
        echo "=============================="

        ARCH_FILE="$OUTPUT_DIR/architecture.md"

        {
            echo "# Architecture Documentation"
            echo ""
            echo "## Overview"
            echo ""
            echo "Java Harness is structured as a multi-module Maven project."
            echo ""
            echo "## Module Structure"
            echo ""
            echo "### Foundation Layer"
            echo "- Core utilities and base classes"
            echo ""
            echo "### Protocol Layer"
            echo "- Communication protocols and interfaces"
            echo ""
            echo "### Security Layer"
            echo "- Security features and guardrails"
            echo ""
            echo "### Workflow Layer"
            echo "- Workflow execution and management"
            echo ""
            echo "### Collaboration Layer"
            echo "- Multi-agent coordination"
            echo ""
            echo "### CLI Layer"
            echo "- Command-line interface"
            echo ""
            echo "### Service Layer"
            echo "- Service implementations"
            echo ""
            echo "### CI Layer"
            echo "- CI/CD integration"
            echo ""
            echo "## Data Flow"
            echo ""
            echo "\`\`\`"
            echo "User Input → CLI → Protocol → Workflow → Execution → Results"
            echo "\`\`\`"
            echo ""
            echo "---"
            echo "*Generated: $(date '+%Y-%m-%d %H:%M:%S')*"

        } > "$ARCH_FILE"

        echo "✅ Architecture documentation generated: $ARCH_FILE"
        ;;

    "all")
        echo ""
        echo "## Complete Documentation Generation"
        echo "===================================="

        ./doc-generator.sh api
        ./doc-generator.sh readme
        ./doc-generator.sh changelog
        ./doc-generator.sh architecture

        echo ""
        echo "✅ All documentation generated"
        echo "Generated files:"
        find "$OUTPUT_DIR" -type f
        echo "README.md"
        echo "CHANGELOG.md"
        ;;

    *)
        echo ""
        echo "Documentation Generator"
        echo "Usage: ./doc-generator.sh <type>"
        echo ""
        echo "Types:"
        echo "  api           - Generate API documentation"
        echo "  readme        - Generate README.md"
        echo "  changelog     - Generate CHANGELOG.md"
        echo "  architecture  - Generate architecture documentation"
        echo "  all           - Generate all documentation"
        echo ""
        exit 1
        ;;
esac