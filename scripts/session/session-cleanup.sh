#!/bin/bash
set -e

echo "==================================="
echo "Session Cleanup"
echo "==================================="

SESSION_DIR=".claude/session"
SESSION_STATE="$SESSION_DIR/state.json"

# Check if session exists
if [ ! -f "$SESSION_STATE" ]; then
    echo "No active session found"
    echo ""
    echo "Available actions:"
    echo "  --all           - Clean up all session data"
    echo "  --temp         - Clean up only temporary files"
    echo "  --logs         - Clean up session logs"
    echo "  --backups      - Clean up session backups"
    exit 0
fi

# Get session info
if command -v jq &> /dev/null; then
    SESSION_ID=$(jq -r '.session_id' "$SESSION_STATE" 2>/dev/null)
    STATUS=$(jq -r '.status' "$SESSION_STATE" 2>/dev/null)
else
    SESSION_ID="unknown"
    STATUS="unknown"
fi

ACTION="${1:---help}"

case "$ACTION" in
    "--all")
        echo ""
        echo "Cleaning up all session data for: $SESSION_ID"

        # Run cleanup hook if exists
        if [ -f "$SESSION_DIR/hooks/cleanup.sh" ]; then
            echo "Running cleanup hook..."
            "$SESSION_DIR/hooks/cleanup.sh" || echo "Hook execution failed"
        fi

        # Archive session data
        BACKUP_FILE="$SESSION_DIR/backups/session-${SESSION_ID}-cleanup-$(date +%Y%m%d-%H%M%S).tar.gz"
        echo "Creating backup: $BACKUP_FILE"
        tar -czf "$BACKUP_FILE" "$SESSION_DIR" 2>/dev/null || echo "Backup creation failed"

        # Clean up session files
        echo "Removing session files..."
        rm -rf "$SESSION_DIR/temp"
        rm -f "$SESSION_DIR"/session.log
        rm -f "$SESSION_DIR"/.lock

        # Update session status
        if command -v jq &> /dev/null; then
            jq '.status = "cleaned" | .end_time = "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"' "$SESSION_STATE" > "${SESSION_STATE}.tmp" && mv "${SESSION_STATE}.tmp" "$SESSION_STATE"
        fi

        # Clear active session
        rm -f ".claude/state/active-session.json"

        echo "✅ Session cleanup completed"
        echo "Backup saved: $BACKUP_FILE"
        ;;

    "--temp")
        echo ""
        echo "Cleaning up temporary files..."

        TEMP_SIZE=$(du -sh "$SESSION_DIR/temp" 2>/dev/null | cut -f1 || echo "unknown")
        echo "Temp directory size: $TEMP_SIZE"

        rm -rf "$SESSION_DIR/temp"/*
        echo "✅ Temporary files cleaned"
        ;;

    "--logs")
        echo ""
        echo "Cleaning up session logs..."

        LOG_SIZE=$(du -sh "$SESSION_DIR/session.log" 2>/dev/null | cut -f1 || echo "0")
        echo "Log file size: $LOG_SIZE"

        # Archive log before cleaning
        if [ -f "$SESSION_DIR/session.log" ]; then
            cp "$SESSION_DIR/session.log" "$SESSION_DIR/backups/session-$(date +%Y%m%d-%H%M%S).log"
        fi

        > "$SESSION_DIR/session.log"
        echo "✅ Session logs cleaned"
        ;;

    "--backups")
        echo ""
        echo "Cleaning up old session backups..."

        # Remove backups older than 7 days
        if [ -d "$SESSION_DIR/backups" ]; then
            REMOVED_COUNT=$(find "$SESSION_DIR/backups" -name "session-*.tar.gz" -mtime +7 2>/dev/null | wc -l | tr -d ' ')
            find "$SESSION_DIR/backups" -name "session-*.tar.gz" -mtime +7 -delete 2>/dev/null

            echo "✅ Removed $REMOVED_COUNT old backup(s)"
        else
            echo "No backups directory found"
        fi
        ;;

    "--status")
        echo ""
        echo "Session cleanup status:"
        echo "======================="

        if [ -d "$SESSION_DIR" ]; then
            TEMP_COUNT=$(find "$SESSION_DIR/temp" -type f 2>/dev/null | wc -l | tr -d ' ')
            LOG_SIZE=$(du -sh "$SESSION_DIR/session.log" 2>/dev/null | cut -f1 || echo "0")
            BACKUP_COUNT=$(find "$SESSION_DIR/backups" -name "session-*.tar.gz" 2>/dev/null | wc -l | tr -d ' ')

            echo "Temp files: $TEMP_COUNT"
            echo "Log size: $LOG_SIZE"
            echo "Backups: $BACKUP_COUNT"
        fi
        ;;

    "--help"|*)
        echo ""
        echo "Session Cleanup Help"
        echo "===================="
        echo ""
        echo "Usage: ./session-cleanup.sh [action]"
        echo ""
        echo "Actions:"
        echo "  --all       - Clean up all session data (default if session ending)"
        echo "  --temp      - Clean up only temporary files"
        echo "  --logs      - Clean up session logs"
        echo "  --backups   - Clean up old session backups (>7 days)"
        echo "  --status    - Show cleanup status"
        echo "  --help      - Show this help message"
        echo ""
        echo "Current session: $SESSION_ID ($STATUS)"
        ;;
esac