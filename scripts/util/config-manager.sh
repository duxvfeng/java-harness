#!/bin/bash
set -e

echo "==================================="
echo "Configuration Manager"
echo "==================================="

CONFIG_DIR=".claude/config"
mkdir -p "$CONFIG_DIR"

case "${1:-list}" in
    "list")
        echo ""
        echo "## Available Configurations"
        echo "=========================="

        if [ -d "$CONFIG_DIR" ]; then
            echo "Configuration files in $CONFIG_DIR:"
            ls -la "$CONFIG_DIR" 2>/dev/null || echo "No configuration files found"
        else
            echo "No configuration directory found"
        fi

        echo ""
        echo "Active configuration:"
        if [ -f ".claude/settings.json" ]; then
            echo "✅ .claude/settings.json exists"
        else
            echo "❌ No active configuration"
        fi
        ;;

    "create")
        if [ -z "$2" ]; then
            echo "Error: Configuration name required"
            echo "Usage: ./config-manager.sh create <name> [environment]"
            exit 1
        fi

        CONFIG_NAME="$2"
        ENVIRONMENT="${3:-dev}"
        CONFIG_FILE="$CONFIG_DIR/${CONFIG_NAME}.json"

        if [ -f "$CONFIG_FILE" ]; then
            echo "Error: Configuration already exists: $CONFIG_NAME"
            exit 1
        fi

        echo ""
        echo "Creating configuration: $CONFIG_NAME"
        echo "Environment: $ENVIRONMENT"

        cat > "$CONFIG_FILE" << EOF
{
  "name": "$CONFIG_NAME",
  "environment": "$ENVIRONMENT",
  "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "settings": {
    "permissions": {
      "allowedCommands": ["mvn", "java", "git"]
    },
    "hooks": {
      "enabled": true
    },
    "workflow": {
      "mode": "solo"
    }
  }
}
EOF

        echo "✅ Configuration created: $CONFIG_FILE"
        echo ""
        echo "Apply this configuration with:"
        echo "  ./config-manager.sh apply $CONFIG_NAME"
        ;;

    "apply")
        if [ -z "$2" ]; then
            echo "Error: Configuration name required"
            echo "Usage: ./config-manager.sh apply <name>"
            exit 1
        fi

        CONFIG_NAME="$2"
        CONFIG_FILE="$CONFIG_DIR/${CONFIG_NAME}.json"

        if [ ! -f "$CONFIG_FILE" ]; then
            echo "Error: Configuration not found: $CONFIG_NAME"
            exit 1
        fi

        echo ""
        echo "Applying configuration: $CONFIG_NAME"

        # Backup current configuration
        if [ -f ".claude/settings.json" ]; then
            BACKUP_FILE=".claude/settings.backup.$(date +%Y%m%d-%H%M%S).json"
            cp ".claude/settings.json" "$BACKUP_FILE"
            echo "Backup saved: $BACKUP_FILE"
        fi

        # Apply new configuration
        cp "$CONFIG_FILE" ".claude/settings.json"

        echo "✅ Configuration applied"
        echo ""
        echo "Current configuration:"
        cat ".claude/settings.json"
        ;;

    "validate")
        echo ""
        echo "## Configuration Validation"
        echo "============================"

        if [ -f ".claude/settings.json" ]; then
            echo "Validating .claude/settings.json..."

            if command -v jq &> /dev/null; then
                if jq empty ".claude/settings.json" 2>/dev/null; then
                    echo "✅ Configuration is valid JSON"
                else
                    echo "❌ Configuration contains invalid JSON"
                fi

                echo ""
                echo "Configuration structure:"
                jq -r '. | keys[]' ".claude/settings.json" 2>/dev/null || echo "Could not parse structure"
            else
                echo "jq not found, skipping detailed validation"
            fi
        else
            echo "❌ No configuration file found"
        fi
        ;;

    "show")
        if [ -z "$2" ]; then
            if [ -f ".claude/settings.json" ]; then
                echo "Current configuration:"
                cat ".claude/settings.json"
            else
                echo "No current configuration found"
            fi
        else
            CONFIG_NAME="$2"
            CONFIG_FILE="$CONFIG_DIR/${CONFIG_NAME}.json"

            if [ -f "$CONFIG_FILE" ]; then
                echo "Configuration: $CONFIG_NAME"
                cat "$CONFIG_FILE"
            else
                echo "Error: Configuration not found: $CONFIG_NAME"
                exit 1
            fi
        fi
        ;;

    "remove")
        if [ -z "$2" ]; then
            echo "Error: Configuration name required"
            echo "Usage: ./config-manager.sh remove <name>"
            exit 1
        fi

        CONFIG_NAME="$2"
        CONFIG_FILE="$CONFIG_DIR/${CONFIG_NAME}.json"

        if [ ! -f "$CONFIG_FILE" ]; then
            echo "Error: Configuration not found: $CONFIG_NAME"
            exit 1
        fi

        echo ""
        echo "Removing configuration: $CONFIG_NAME"

        rm "$CONFIG_FILE"

        echo "✅ Configuration removed"
        ;;

    *)
        echo ""
        echo "Configuration Manager"
        echo "Usage: ./config-manager.sh <command> [args]"
        echo ""
        echo "Commands:"
        echo "  list              - List all configurations"
        echo "  create <name>     - Create new configuration"
        echo "  apply <name>      - Apply configuration"
        echo "  validate          - Validate current configuration"
        echo "  show [name]       - Show configuration"
        echo "  remove <name>     - Remove configuration"
        echo ""
        exit 1
        ;;
esac