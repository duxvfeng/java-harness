#!/bin/bash
set -e

echo "==================================="
echo "Plan Registry Management"
echo "==================================="

PLAN_REGISTRY_DIR=".claude/plans"
PLAN_REGISTRY_FILE="$PLAN_REGISTRY_DIR/registry.json"

# Create registry directory if it doesn't exist
mkdir -p "$PLAN_REGISTRY_DIR"

# Initialize registry if it doesn't exist
if [ ! -f "$PLAN_REGISTRY_FILE" ]; then
    echo "Initializing plan registry..."
    cat > "$PLAN_REGISTRY_FILE" << 'EOF'
{
  "plans": [],
  "active_plan": null,
  "created_at": null,
  "updated_at": null
}
EOF
fi

case "${1:-list}" in
    "list")
        echo ""
        echo "## Registered Plans"
        echo "==================="
        if [ -f "$PLAN_REGISTRY_FILE" ]; then
            if command -v jq &> /dev/null; then
                PLANS=$(jq -r '.plans[] | "- \(.name) (ID: \(.id), Status: \(.status))"' "$PLAN_REGISTRY_FILE" 2>/dev/null || echo "No plans registered")
                if [ -z "$PLANS" ]; then
                    echo "No plans registered"
                else
                    echo "$PLANS"
                fi
                ACTIVE=$(jq -r '.active_plan // "None"' "$PLAN_REGISTRY_FILE" 2>/dev/null)
                echo ""
                echo "Active plan: $ACTIVE"
            else
                echo "jq not found, displaying raw registry:"
                cat "$PLAN_REGISTRY_FILE"
            fi
        else
            echo "Registry not found"
        fi
        ;;

    "add")
        if [ -z "$2" ]; then
            echo "Error: Plan name required"
            echo "Usage: ./plan-registry.sh add <plan-name> [plan-file]"
            exit 1
        fi

        PLAN_NAME="$2"
        PLAN_FILE="${3:-Plans.md}"
        PLAN_ID="$(date +%Y%m%d)-$(echo $PLAN_NAME | tr ' ' '-' | tr '[:upper:]' '[:lower:]')"

        echo ""
        echo "Adding plan: $PLAN_NAME"
        echo "Plan ID: $PLAN_ID"
        echo "Plan file: $PLAN_FILE"

        if [ ! -f "$PLAN_FILE" ]; then
            echo "Warning: Plan file does not exist: $PLAN_FILE"
        fi

        if command -v jq &> /dev/null; then
            # Add plan to registry
            jq --arg name "$PLAN_NAME" \
               --arg id "$PLAN_ID" \
               --arg file "$PLAN_FILE" \
               --arg status "active" \
               --arg created "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
               '.plans += [{
                   id: $id,
                   name: $name,
                   file: $file,
                   status: $status,
                   created_at: $created,
                   updated_at: $created
               }] | .created_at = (if .created_at == null then $created else .created_at end) | .updated_at = $created' \
               "$PLAN_REGISTRY_FILE" > "${PLAN_REGISTRY_FILE}.tmp" && mv "${PLAN_REGISTRY_FILE}.tmp" "$PLAN_REGISTRY_FILE"

            echo "✅ Plan added successfully"
        else
            echo "Error: jq not found. Please install jq to use plan registry."
            exit 1
        fi
        ;;

    "remove")
        if [ -z "$2" ]; then
            echo "Error: Plan ID required"
            echo "Usage: ./plan-registry.sh remove <plan-id>"
            exit 1
        fi

        PLAN_ID="$2"
        echo ""
        echo "Removing plan: $PLAN_ID"

        if command -v jq &> /dev/null; then
            jq --arg id "$PLAN_ID" \
               '.plans = (.plans | map(select(.id != $id))) | .updated_at = "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"' \
               "$PLAN_REGISTRY_FILE" > "${PLAN_REGISTRY_FILE}.tmp" && mv "${PLAN_REGISTRY_FILE}.tmp" "$PLAN_REGISTRY_FILE"

            echo "✅ Plan removed successfully"
        else
            echo "Error: jq not found. Please install jq to use plan registry."
            exit 1
        fi
        ;;

    "activate")
        if [ -z "$2" ]; then
            echo "Error: Plan ID required"
            echo "Usage: ./plan-registry.sh activate <plan-id>"
            exit 1
        fi

        PLAN_ID="$2"
        echo ""
        echo "Activating plan: $PLAN_ID"

        if command -v jq &> /dev/null; then
            jq --arg id "$PLAN_ID" \
               '.active_plan = $id | .plans = (.plans | map(if .id == $id then .status = "active" else .status |= . end)) | .updated_at = "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"' \
               "$PLAN_REGISTRY_FILE" > "${PLAN_REGISTRY_FILE}.tmp" && mv "${PLAN_REGISTRY_FILE}.tmp" "$PLAN_REGISTRY_FILE"

            echo "✅ Plan activated successfully"
        else
            echo "Error: jq not found. Please install jq to use plan registry."
            exit 1
        fi
        ;;

    "status")
        echo ""
        echo "## Plan Registry Status"
        echo "======================"
        if [ -f "$PLAN_REGISTRY_FILE" ]; then
            if command -v jq &> /dev/null; then
                echo "Total plans: $(jq '.plans | length' "$PLAN_REGISTRY_FILE")"
                echo "Active plan: $(jq -r '.active_plan // "None"' "$PLAN_REGISTRY_FILE")"
                echo "Last updated: $(jq -r '.updated_at // "Unknown"' "$PLAN_REGISTRY_FILE")"
                echo ""
                echo "Plan details:"
                jq -r '.plans[] | "  - \(.name) (\(.id)): \(.status)"' "$PLAN_REGISTRY_FILE"
            else
                cat "$PLAN_REGISTRY_FILE"
            fi
        else
            echo "Registry not found"
        fi
        ;;

    "validate")
        echo ""
        echo "## Plan Validation"
        echo "================="

        VALID=1
        if [ -f "$PLAN_REGISTRY_FILE" ]; then
            if command -v jq &> /dev/null; then
                PLANS=$(jq -r '.plans[].file' "$PLAN_REGISTRY_FILE" 2>/dev/null)
                for PLAN_FILE in $PLANS; do
                    if [ -f "$PLAN_FILE" ]; then
                        echo "✅ $PLAN_FILE exists"
                    else
                        echo "❌ $PLAN_FILE missing"
                        VALID=0
                    fi
                done

                if [ $VALID -eq 1 ]; then
                    echo ""
                    echo "✅ All plan files are valid"
                else
                    echo ""
                    echo "❌ Some plan files are missing"
                fi
            else
                echo "Error: jq not found"
                exit 1
            fi
        else
            echo "Registry not found"
            exit 1
        fi
        ;;

    *)
        echo ""
        echo "Plan Registry Management"
        echo "Usage: ./plan-registry.sh <command> [args]"
        echo ""
        echo "Commands:"
        echo "  list                    - List all registered plans"
        echo "  add <name> [file]       - Add a new plan"
        echo "  remove <id>             - Remove a plan"
        echo "  activate <id>           - Activate a plan"
        echo "  status                  - Show registry status"
        echo "  validate                - Validate plan files"
        echo ""
        exit 1
        ;;
esac