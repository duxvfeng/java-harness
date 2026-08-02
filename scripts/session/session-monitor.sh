#!/bin/bash
set -e

echo "==================================="
echo "Session Monitor"
echo "==================================="

SESSION_DIR=".claude/session"
SESSION_STATE="$SESSION_DIR/state.json"
INTERVAL="${1:-5}"

# Check if session exists
if [ ! -f "$SESSION_STATE" ]; then
    echo "No active session found"
    echo "Start a new session with: ./session-init.sh"
    exit 1
fi

if command -v jq &> /dev/null; then
    SESSION_ID=$(jq -r '.session_id' "$SESSION_STATE" 2>/dev/null)
else
    SESSION_ID="unknown"
fi

echo "Monitoring session: $SESSION_ID"
echo "Check interval: $INTERVAL seconds"
echo "Press Ctrl+C to stop"
echo ""
echo "================================="

LAST_CHECK_CONTENT=""
LAST_CHECK_TIME=""

while true; do
    sleep "$INTERVAL"

    # Get current session state
    if [ -f "$SESSION_STATE" ]; then
        CURRENT_CONTENT=$(cat "$SESSION_STATE")
        CURRENT_TIME=$(date '+%Y-%m-%d %H:%M:%S')

        # Check for changes
        if [ "$CURRENT_CONTENT" != "$LAST_CHECK_CONTENT" ]; then
            echo "[$CURRENT_TIME] 🔄 Session state changed"

            # Show what changed
            if command -v jq &> /dev/null; then
                STATUS=$(jq -r '.status' "$SESSION_STATE" 2>/dev/null)
                TASKS_COMPLETED=$(jq -r '.tasks_completed' "$SESSION_STATE" 2>/dev/null)
                COMMANDS_EXECUTED=$(jq -r '.commands_executed' "$SESSION_STATE" 2>/dev/null)

                echo "  Status: $STATUS"
                echo "  Tasks completed: $TASKS_COMPLETED"
                echo "  Commands executed: $COMMANDS_EXECUTED"
            fi

            LAST_CHECK_CONTENT="$CURRENT_CONTENT"
        fi

        # Check session activity
        if command -v jq &> /dev/null; then
            LAST_ACTIVITY=$(jq -r '.last_activity' "$SESSION_STATE" 2>/dev/null)

            if command -v date &> /dev/null; then
                LAST_ACTIVITY_EPOCH=$(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$LAST_ACTIVITY" +%s 2>/dev/null || date -d "$LAST_ACTIVITY" +%s 2>/dev/null)
                CURRENT_EPOCH=$(date +%s)
                INACTIVITY=$((CURRENT_EPOCH - LAST_ACTIVITY_EPOCH))

                # Show inactivity warning
                if [ $INACTIVITY -gt 1800 ] && [ $INACTIVITY -lt 1900 ]; then
                    echo "[$CURRENT_TIME] ⚠️  Session inactive for 30+ minutes"
                fi

                # Show extended inactivity warning
                if [ $INACTIVITY -gt 7200 ] && [ $INACTIVITY -lt 7300 ]; then
                    echo "[$CURRENT_TIME] ⚠️  Session inactive for 2+ hours"
                fi
            fi
        fi

        # Check for errors
        if command -v jq &> /dev/null; then
            ERRORS=$(jq -r '.errors' "$SESSION_STATE" 2>/dev/null)
            if [ "$ERRORS" -gt 0 ] && [ "$ERRORS" -lt 6 ]; then
                echo "[$CURRENT_TIME] ❌ Errors detected: $ERRORS"
            fi
        fi

        # Periodic status update (every 5 intervals)
        if [ -z "$LAST_CHECK_TIME" ] || [ $((CURRENT_EPOCH - LAST_CHECK_TIME)) -ge $((INTERVAL * 5)) ]; then
            echo "[$CURRENT_TIME] ✓ Monitoring... (Session: $SESSION_ID)"
            LAST_CHECK_TIME=$CURRENT_EPOCH
        fi
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ Session state file disappeared"
        exit 1
    fi
done