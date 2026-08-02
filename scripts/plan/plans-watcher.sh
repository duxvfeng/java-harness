#!/bin/bash
set -e

echo "==================================="
echo "Plans Watcher"
echo "==================================="

WATCH_DIR="${1:-.}"
INTERVAL="${2:-5}"

echo "Watching directory: $WATCH_DIR"
echo "Check interval: $INTERVAL seconds"
echo ""
echo "Monitoring Plans.md for changes..."
echo "Press Ctrl+C to stop"

# Store initial file state
LAST_CHECK_TIME=$(date +%s)
LAST_CONTENT=""
LAST_HASH=""

if [ -f "$WATCH_DIR/Plans.md" ]; then
    LAST_CONTENT=$(cat "$WATCH_DIR/Plans.md")
    LAST_HASH=$(echo "$LAST_CONTENT" | md5sum | cut -d' ' -f1)
    echo "✅ Found Plans.md"
    echo "Initial hash: $LAST_HASH"
else
    echo "⚠️  Plans.md not found in $WATCH_DIR"
    echo "Creating watcher for future Plans.md creation..."
fi

echo ""
echo "Watching for changes..."
echo "================================="

while true; do
    sleep "$INTERVAL"

    CURRENT_TIME=$(date +%s)
    CHANGES_DETECTED=0

    # Check if Plans.md exists
    if [ -f "$WATCH_DIR/Plans.md" ]; then
        # Get current content and hash
        CURRENT_CONTENT=$(cat "$WATCH_DIR/Plans.md")
        CURRENT_HASH=$(echo "$CURRENT_CONTENT" | md5sum | cut -d' ' -f1)

        # Compare with last state
        if [ "$CURRENT_HASH" != "$LAST_HASH" ]; then
            echo ""
            echo "🔔 Change detected in Plans.md!"
            echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"

            # Get file modification time
            FILE_TIME=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$WATCH_DIR/Plans.md" 2>/dev/null || stat -c "%y" "$WATCH_DIR/Plans.md" 2>/dev/null | cut -d'.' -f1)
            echo "Modified: $FILE_TIME"

            # Show diff if available
            if command -v diff &> /dev/null && [ -n "$LAST_CONTENT" ]; then
                echo ""
                echo "Changes:"
                diff <(echo "$LAST_CONTENT") <(echo "$CURRENT_CONTENT") || true
            fi

            # Update state
            LAST_CONTENT="$CURRENT_CONTENT"
            LAST_HASH="$CURRENT_HASH"
            CHANGES_DETECTED=1

            # Trigger event hook if exists
            if [ -f ".claude/hooks/on-plans-change.sh" ]; then
                echo ""
                echo "Triggering on-plans-change hook..."
                ./.claude/hooks/on-plans-change.sh "$WATCH_DIR/Plans.md" || echo "Hook execution failed"
            fi

            # Log the change
            if [ -d ".claude/state" ]; then
                LOG_ENTRY="[$(date -u +%Y-%m-%dT%H:%M:%SZ)] Plans.md changed"
                echo "$LOG_ENTRY" >> .claude/state/plans-changelog.log
                echo "✅ Change logged to .claude/state/plans-changelog.log"
            fi

            echo ""
            echo "================================="
            echo "Watching for changes..."
            echo "================================="
        fi
    else
        # Check if Plans.md was created
        if [ -z "$LAST_CONTENT" ]; then
            echo ""
            echo "🎉 Plans.md created!"
            echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"

            LAST_CONTENT=$(cat "$WATCH_DIR/Plans.md")
            LAST_HASH=$(echo "$LAST_CONTENT" | md5sum | cut -d' ' -f1)
            CHANGES_DETECTED=1

            echo "Initial hash: $LAST_HASH"
            echo ""
            echo "================================="
            echo "Watching for changes..."
            echo "================================="
        fi
    fi

    # Periodic status update every 30 seconds
    TIME_DIFF=$((CURRENT_TIME - LAST_CHECK_TIME))
    if [ $TIME_DIFF -ge 30 ] && [ $CHANGES_DETECTED -eq 0 ]; then
        echo "[$(date '+%H:%M:%S')] Watching... (no changes)"
        LAST_CHECK_TIME=$CURRENT_TIME
    fi
done