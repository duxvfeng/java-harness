#!/bin/bash
# Installation script for java-harness hooks
# This script configures hooks.json to work correctly on any platform

set -e

echo "=== java-harness Hooks Installation ==="
echo ""

# Find git repository root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ Error: Not in a git repository"
  exit 1
fi

echo "✓ Found project root: $PROJECT_ROOT"

# Check if wrapper exists
WRAPPER_SCRIPT="$PROJECT_ROOT/.claude-plugin/hook-wrapper.sh"
if [ ! -f "$WRAPPER_SCRIPT" ]; then
  echo "❌ Error: Wrapper script not found at $WRAPPER_SCRIPT"
  exit 1
fi

echo "✓ Found wrapper script"

# Check if hooks.json exists
HOOKS_FILE="$PROJECT_ROOT/hooks/hooks.json"
if [ ! -f "$HOOKS_FILE" ]; then
  echo "❌ Error: hooks.json not found at $HOOKS_FILE"
  exit 1
fi

echo "✓ Found hooks.json"

# Create backup
BACKUP_FILE="$HOOKS_FILE.backup-$(date +%Y%m%d-%H%M%S)"
cp "$HOOKS_FILE" "$BACKUP_FILE"
echo "✓ Created backup: $BACKUP_FILE"

# Detect OS and choose appropriate sed command
if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS
  SED_INPLACE="sed -i ''"
else
  # Linux and others
  SED_INPLACE="sed -i"
fi

# Update hooks.json with project-specific absolute path to wrapper
# Using bash -c to ensure the wrapper can be called from any directory
TEMP_FILE=$(mktemp)
sed "s|\"command\": \"bash \.claude-plugin/hook-wrapper\.sh|\"command\": \"bash $WRAPPER_SCRIPT|g" "$HOOKS_FILE" > "$TEMP_FILE"
mv "$TEMP_FILE" "$HOOKS_FILE"

echo "✓ Updated hooks.json with absolute path to wrapper"

# Verify JSON syntax
if command -v python &> /dev/null; then
  python -m json.tool "$HOOKS_FILE" > /dev/null && echo "✓ JSON syntax valid" || echo "⚠ Warning: JSON syntax may be invalid"
elif command -v python3 &> /dev/null; then
  python3 -m json.tool "$HOOKS_FILE" > /dev/null && echo "✓ JSON syntax valid" || echo "⚠ Warning: JSON syntax may be invalid"
fi

echo ""
echo "=== Installation Complete ==="
echo "✓ Hooks configured successfully!"
echo ""
echo "Next steps:"
echo "1. Test the hooks: Read any file in the project"
echo "2. If hooks fail, check: $BACKUP_FILE"
echo "3. For issues, see: docs/install/TROUBLESHOOTING.md"
