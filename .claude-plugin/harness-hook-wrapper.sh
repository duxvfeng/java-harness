#!/bin/sh
# Hook wrapper for java-harness
# This script finds the git repository root and calls bin/harness from there
# This ensures hooks work correctly from any subdirectory in the project

set -e

# Find git repository root
GIT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)

if [ -z "$GIT_ROOT" ]; then
  echo "[java-harness-hook] Error: Not in a git repository" >&2
  exit 1
fi

# Path to harness binary
HARNESS_SCRIPT="$GIT_ROOT/bin/harness"

if [ ! -f "$HARNESS_SCRIPT" ]; then
  echo "[java-harness-hook] Error: harness not found at $HARNESS_SCRIPT" >&2
  echo "[java-harness-hook] Run: mvn package" >&2
  exit 1
fi

# Execute harness with all arguments
exec "$HARNESS_SCRIPT" "$@"
