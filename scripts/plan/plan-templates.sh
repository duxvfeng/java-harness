#!/bin/bash
set -e

echo "==================================="
echo "Plan Template Manager"
echo "==================================="

PLAN_TEMPLATES_DIR=".claude/plans/templates"
mkdir -p "$PLAN_TEMPLATES_DIR"

case "${1:-list}" in
    "list")
        echo ""
        echo "## Available Plan Templates"
        echo "=========================="

        if [ -d "$PLAN_TEMPLATES_DIR" ]; then
            TEMPLATE_COUNT=$(find "$PLAN_TEMPLATES_DIR" -name "*.md" | wc -l | tr -d ' ')

            if [ "$TEMPLATE_COUNT" -eq 0 ]; then
                echo "No templates found"
                echo ""
                echo "Create a template with: ./plan-templates.sh create <name>"
            else
                echo "Found $TEMPLATE_COUNT template(s):"
                echo ""
                for TEMPLATE in "$PLAN_TEMPLATES_DIR"/*.md; do
                    if [ -f "$TEMPLATE" ]; then
                        TEMPLATE_NAME=$(basename "$TEMPLATE" .md)
                        echo "  📄 $TEMPLATE_NAME"
                    fi
                done
            fi
        else
            echo "Templates directory not found"
        fi
        ;;

    "create")
        if [ -z "$2" ]; then
            echo "Error: Template name required"
            echo "Usage: ./plan-templates.sh create <template-name>"
            exit 1
        fi

        TEMPLATE_NAME="$2"
        TEMPLATE_FILE="$PLAN_TEMPLATES_DIR/${TEMPLATE_NAME}.md"

        if [ -f "$TEMPLATE_FILE" ]; then
            echo "Error: Template already exists: $TEMPLATE_NAME"
            exit 1
        fi

        echo ""
        echo "Creating template: $TEMPLATE_NAME"

        # Create basic template structure
        cat > "$TEMPLATE_FILE" << EOF
# $TEMPLATE_NAME Plan Template

## Overview
**Purpose**: ${TEMPLATE_NAME} project plan
**Created**: $(date +%Y-%m-%d)
**Status**: Draft

## Phase 1: Planning (2 weeks)

| Task | Description | Priority | Status |
|------|-------------|----------|--------|
| 1.1 | Initial requirements gathering | High | TODO |
| 1.2 | Technical design and architecture | High | TODO |
| 1.3 | Resource allocation | Medium | TODO |

## Phase 2: Implementation (4 weeks)

| Task | Description | Priority | Status |
|------|-------------|----------|--------|
| 2.1 | Core feature development | High | TODO |
| 2.2 | Integration and testing | High | TODO |
| 2.3 | Documentation | Medium | TODO |

## Phase 3: Deployment (1 week)

| Task | Description | Priority | Status |
|------|-------------|----------|--------|
| 3.1 | Production deployment | High | TODO |
| 3.2 | Monitoring setup | Medium | TODO |
| 3.3 | User training | Low | TODO |

## Definition of Done

- [ ] All phases completed
- [ ] All tasks marked as completed
- [ ] Testing completed successfully
- [ ] Documentation updated
- [ ] Team review conducted

## Notes

This is a template plan. Customize it according to your project needs.
EOF

        echo "✅ Template created: $TEMPLATE_FILE"
        echo ""
        echo "Use this template with:"
        echo "  ./plan-templates.sh use $TEMPLATE_NAME <output-file>"
        ;;

    "use")
        if [ -z "$2" ]; then
            echo "Error: Template name required"
            echo "Usage: ./plan-templates.sh use <template-name> <output-file>"
            exit 1
        fi

        if [ -z "$3" ]; then
            echo "Error: Output file required"
            echo "Usage: ./plan-templates.sh use <template-name> <output-file>"
            exit 1
        fi

        TEMPLATE_NAME="$2"
        OUTPUT_FILE="$3"
        TEMPLATE_FILE="$PLAN_TEMPLATES_DIR/${TEMPLATE_NAME}.md"

        if [ ! -f "$TEMPLATE_FILE" ]; then
            echo "Error: Template not found: $TEMPLATE_NAME"
            echo ""
            echo "Available templates:"
            ./plan-templates.sh list
            exit 1
        fi

        echo ""
        echo "Creating plan from template: $TEMPLATE_NAME"
        echo "Output file: $OUTPUT_FILE"

        cp "$TEMPLATE_FILE" "$OUTPUT_FILE"

        echo "✅ Plan created from template"
        echo ""
        echo "Next steps:"
        echo "  1. Edit $OUTPUT_FILE with your project details"
        echo "  2. Register the plan: ./plan-registry.sh add \"$TEMPLATE_NAME\" \"$OUTPUT_FILE\""
        ;;

    "edit")
        if [ -z "$2" ]; then
            echo "Error: Template name required"
            echo "Usage: ./plan-templates.sh edit <template-name>"
            exit 1
        fi

        TEMPLATE_NAME="$2"
        TEMPLATE_FILE="$PLAN_TEMPLATES_DIR/${TEMPLATE_NAME}.md"

        if [ ! -f "$TEMPLATE_FILE" ]; then
            echo "Error: Template not found: $TEMPLATE_NAME"
            exit 1
        fi

        echo ""
        echo "Opening template for editing: $TEMPLATE_NAME"
        echo "File: $TEMPLATE_FILE"
        echo ""

        # Use default editor or fallback to vi
        ${EDITOR:-vi} "$TEMPLATE_FILE"

        echo "✅ Template updated"
        ;;

    "remove")
        if [ -z "$2" ]; then
            echo "Error: Template name required"
            echo "Usage: ./plan-templates.sh remove <template-name>"
            exit 1
        fi

        TEMPLATE_NAME="$2"
        TEMPLATE_FILE="$PLAN_TEMPLATES_DIR/${TEMPLATE_NAME}.md"

        if [ ! -f "$TEMPLATE_FILE" ]; then
            echo "Error: Template not found: $TEMPLATE_NAME"
            exit 1
        fi

        echo ""
        echo "Removing template: $TEMPLATE_NAME"

        rm "$TEMPLATE_FILE"

        echo "✅ Template removed"
        ;;

    *)
        echo ""
        echo "Plan Template Manager"
        echo "Usage: ./plan-templates.sh <command> [args]"
        echo ""
        echo "Commands:"
        echo "  list                    - List available templates"
        echo "  create <name>           - Create a new template"
        echo "  use <name> <output>     - Create plan from template"
        echo "  edit <name>             - Edit existing template"
        echo "  remove <name>           - Remove a template"
        echo ""
        exit 1
        ;;
esac