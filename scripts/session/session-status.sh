#!/bin/bash
set -e

echo "==================================="
echo "Session Status"
echo "==================================="

SESSION_DIR=".claude/session"
SESSION_STATE="$SESSION_DIR/state.json"
SESSION_INFO="$SESSION_DIR/info.json"

# Check if session exists
if [ ! -f "$SESSION_STATE" ]; then
    echo "No active session found"
    echo ""
    echo "Start a new session with: ./session-init.sh"
    exit 1
fi

echo ""
echo "## Session Overview"
echo "=================="

if command -v jq &> /dev/null; then
    # Display session information
    SESSION_ID=$(jq -r '.session_id' "$SESSION_STATE" 2>/dev/null)
    SESSION_TYPE=$(jq -r '.session_type' "$SESSION_STATE" 2>/dev/null)
    STATUS=$(jq -r '.status' "$SESSION_STATE" 2>/dev/null)
    START_TIME=$(jq -r '.start_time' "$SESSION_STATE" 2>/dev/null)
    LAST_ACTIVITY=$(jq -r '.last_activity' "$SESSION_STATE" 2>/dev/null)
    TASKS_COMPLETED=$(jq -r '.tasks_completed' "$SESSION_STATE" 2>/dev/null)
    TASKS_TOTAL=$(jq -r '.tasks_total' "$SESSION_STATE" 2>/dev/null)
    COMMANDS_EXECUTED=$(jq -r '.commands_executed' "$SESSION_STATE" 2>/dev/null)
    ERRORS=$(jq -r '.errors' "$SESSION_STATE" 2>/dev/null)

    echo "Session ID: $SESSION_ID"
    echo "Type: $SESSION_TYPE"
    echo "Status: $STATUS"
    echo "Started: $START_TIME"
    echo "Last Activity: $LAST_ACTIVITY"

    echo ""
    echo "## Session Progress"
    echo "==================="
    echo "Tasks Completed: $TASKS_COMPLETED / $TASKS_TOTAL"
    if [ "$TASKS_TOTAL" -gt 0 ]; then
        PROGRESS=$((TASKS_COMPLETED * 100 / TASKS_TOTAL))
        echo "Progress: $PROGRESS%"
    fi
    echo "Commands Executed: $COMMANDS_EXECUTED"
    echo "Errors: $ERRORS"

    # Calculate session duration
    if command -v date &> /dev/null; then
        START_EPOCH=$(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$START_TIME" +%s 2>/dev/null || date -d "$START_TIME" +%s 2>/dev/null)
        CURRENT_EPOCH=$(date +%s)
        DURATION=$((CURRENT_EPOCH - START_EPOCH))
        HOURS=$((DURATION / 3600))
        MINUTES=$(((DURATION % 3600) / 60))
        echo "Duration: ${HOURS}h ${MINUTES}m"
    fi

    echo ""
    echo "## Environment Information"
    echo "========================"
    if [ -f "$SESSION_INFO" ]; then
        USER=$(jq -r '.user' "$SESSION_INFO" 2>/dev/null)
        HOSTNAME=$(jq -r '.hostname' "$SESSION_INFO" 2>/dev/null)
        WORKING_DIR=$(jq -r '.working_directory' "$SESSION_INFO" 2>/dev/null)
        GIT_BRANCH=$(jq -r '.git_branch' "$SESSION_INFO" 2>/dev/null)

        echo "User: $USER@$HOSTNAME"
        echo "Working Directory: $WORKING_DIR"
        echo "Git Branch: $GIT_BRANCH"
    fi

else
    echo "jq not found, displaying raw session state:"
    cat "$SESSION_STATE"
fi

# Show recent session log entries
echo ""
echo "## Recent Activity"
echo "=================="
if [ -f "$SESSION_DIR/session.log" ]; then
    tail -5 "$SESSION_DIR/session.log" | grep -E "^(➜|✅|❌|⚠️)" || echo "No recent activity"
else
    echo "No activity log found"
fi

# Check session health
echo ""
echo "## Session Health"
echo "================="

HEALTH_ISSUES=0

# Check if session is recent (within 24 hours)
if command -v date &> /dev/null; then
    LAST_ACTIVITY_EPOCH=$(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$LAST_ACTIVITY" +%s 2>/dev/null || date -d "$LAST_ACTIVITY" +%s 2>/dev/null)
    CURRENT_EPOCH=$(date +%s)
    INACTIVITY=$((CURRENT_EPOCH - LAST_ACTIVITY_EPOCH))

    if [ $INACTIVITY -gt 86400 ]; then
        echo "⚠️  Session inactive for more than 24 hours"
        HEALTH_ISSUES=$((HEALTH_ISSUES + 1))
    else
        echo "✅ Session active"
    fi
fi

# Check error rate
if [ "$ERRORS" -gt 10 ]; then
    echo "⚠️  High error count: $ERRORS"
    HEALTH_ISSUES=$((HEALTH_ISSUES + 1))
else
    echo "✅ Error count acceptable"
fi

# Check for stale locks
if [ -f ".claude/session/.lock" ]; then
    LOCK_AGE=$(find .claude/session/lock -type f -mtime +1 2>/dev/null | wc -l | tr -d ' ')
    if [ "$LOCK_AGE" -gt 0 ]; then
        echo "⚠️  Stale lock file detected"
        HEALTH_ISSUES=$((HEALTH_ISSUES + 1))
    fi
else
    echo "✅ No lock issues"
fi

if [ $HEALTH_ISSUES -eq 0 ]; then
    echo ""
    echo "✅ Session health: Good"
else
    echo ""
    echo "⚠️  Session health: $HEALTH_ISSUES issue(s) found"
    echo "Consider running: ./session-cleanup.sh"
fi