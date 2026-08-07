#!/bin/sh
# Cross-platform hook wrapper for java-harness
# Usage: Put this in your project root and update hooks.json to call it
# Example: "command": "bash .claude-plugin/hook-wrapper.sh hook pre-tool"

set -e

# Find git repository root
GIT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)

if [ -z "$GIT_ROOT" ]; then
  echo "[java-harness] Error: Not in a git repository" >&2
  exit 1
fi

# Path to harness binary
HARNESS_SCRIPT="$GIT_ROOT/bin/harness"

if [ ! -f "$HARNESS_SCRIPT" ]; then
  echo "[java-harness] Error: harness not found at $HARNESS_SCRIPT" >&2
  echo "[java-harness] Please run: mvn package" >&2
  exit 1
fi

# Execute harness with all arguments
exec "$HARNESS_SCRIPT" "$@"
