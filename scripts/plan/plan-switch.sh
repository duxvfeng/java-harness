#!/bin/bash
set -e

echo "==================================="
echo "Plan Switcher"
echo "==================================="

if [ -z "$1" ]; then
    echo "Error: Plan ID or name required"
    echo "Usage: ./plan-switch.sh <plan-id|plan-name>"
    echo ""
    echo "Available plans:"
    ./plan-registry.sh list
    exit 1
fi

PLAN_IDENTIFIER="$1"
PLAN_REGISTRY=".claude/plans/registry.json"

if [ ! -f "$PLAN_REGISTRY" ]; then
    echo "Error: Plan registry not found"
    exit 1
fi

echo ""
echo "Searching for plan: $PLAN_IDENTIFIER"

# Try to find plan by ID or name
if command -v jq &> /dev/null; then
    # Try by ID first
    PLAN=$(jq -r --arg id "$PLAN_IDENTIFIER" '.plans[] | select(.id == $id) | .file' "$PLAN_REGISTRY" 2>/dev/null)

    # If not found, try by name
    if [ -z "$PLAN" ]; then
        PLAN=$(jq -r --arg name "$PLAN_IDENTIFIER" '.plans[] | select(.name == $name) | .file' "$PLAN_REGISTRY" 2>/dev/null)
    fi

    if [ -z "$PLAN" ]; then
        echo "❌ Plan not found: $PLAN_IDENTIFIER"
        echo ""
        echo "Available plans:"
        ./plan-registry.sh list
        exit 1
    fi

    PLAN_ID=$(jq -r --arg file "$PLAN" '.plans[] | select(.file == $file) | .id' "$PLAN_REGISTRY" 2>/dev/null)
    PLAN_NAME=$(jq -r --arg file "$PLAN" '.plans[] | select(.file == $file) | .name' "$PLAN_REGISTRY" 2>/dev/null)

    echo "Found plan: $PLAN_NAME (ID: $PLAN_ID)"
    echo "Plan file: $PLAN"
else
    echo "Error: jq not found. Please install jq."
    exit 1
fi

# Check if plan file exists
if [ ! -f "$PLAN" ]; then
    echo "❌ Plan file does not exist: $PLAN"
    exit 1
fi

# Backup current Plans.md if it exists
if [ -f "Plans.md" ]; then
    BACKUP_FILE="Plans.md.backup.$(date +%Y%m%d-%H%M%S)"
    echo ""
    echo "Backing up current Plans.md to $BACKUP_FILE"
    cp "Plans.md" "$BACKUP_FILE"
fi

# Switch to new plan
echo ""
echo "Switching to plan: $PLAN"
cp "$PLAN" "Plans.md"

# Update registry
echo ""
echo "Updating plan registry..."
./plan-registry.sh activate "$PLAN_ID"

echo ""
echo "✅ Plan switched successfully!"
echo ""
echo "Current plan: $PLAN_NAME"
echo "Source file: $PLAN"

if [ -n "$BACKUP_FILE" ]; then
    echo "Backup file: $BACKUP_FILE"
fi