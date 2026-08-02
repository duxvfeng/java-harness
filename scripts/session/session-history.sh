#!/bin/bash
set -e

echo "==================================="
echo "Session History"
echo "==================================="

SESSION_BACKUP_DIR=".claude/session/backups"
SESSION_LOG=".claude/session/session.log"

case "${1:-show}" in
    "show")
        echo ""
        echo "## Session History"
        echo "=================="

        # Show current session log if exists
        if [ -f "$SESSION_LOG" ]; then
            echo "### Current Session Activity"
            echo "============================"
            cat "$SESSION_LOG"
        else
            echo "No current session log found"
        fi

        echo ""
        echo "### Session Backups"
        echo "=================="

        if [ -d "$SESSION_BACKUP_DIR" ]; then
            BACKUP_COUNT=$(find "$SESSION_BACKUP_DIR" -name "session-*.tar.gz" | wc -l | tr -d ' ')

            if [ "$BACKUP_COUNT" -eq 0 ]; then
                echo "No session backups found"
            else
                echo "Found $BACKUP_COUNT backup(s):"
                echo ""

                for BACKUP in "$SESSION_BACKUP_DIR"/session-*.tar.gz; do
                    if [ -f "$BACKUP" ]; then
                        BACKUP_NAME=$(basename "$BACKUP")
                        BACKUP_SIZE=$(du -h "$BACKUP" | cut -f1)
                        BACKUP_DATE=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$BACKUP" 2>/dev/null || stat -c "%y" "$BACKUP" 2>/dev/null | cut -d'.' -f1)

                        echo "  📦 $BACKUP_NAME"
                        echo "     Size: $BACKUP_SIZE"
                        echo "     Date: $BACKUP_DATE"
                        echo ""
                    fi
                done
            fi
        else
            echo "No backups directory found"
        fi
        ;;

    "restore")
        if [ -z "$2" ]; then
            echo "Error: Backup file required"
            echo "Usage: ./session-history.sh restore <backup-file>"
            echo ""
            echo "Available backups:"
            find "$SESSION_BACKUP_DIR" -name "session-*.tar.gz" -exec basename {} \;
            exit 1
        fi

        BACKUP_FILE="$SESSION_BACKUP_DIR/$2"

        if [ ! -f "$BACKUP_FILE" ]; then
            echo "Error: Backup file not found: $BACKUP_FILE"
            exit 1
        fi

        echo ""
        echo "Restoring session from: $BACKUP_FILE"

        # Create current state backup
        if [ -f ".claude/session/state.json" ]; then
            BACKUP_BEFORE="backup-before-restore-$(date +%Y%m%d-%H%M%S).tar.gz"
            echo "Creating pre-restore backup: $BACKUP_BEFORE"
            tar -czf "$SESSION_BACKUP_DIR/$BACKUP_BEFORE" .claude/session/ 2>/dev/null || true
        fi

        # Restore from backup
        echo "Restoring session files..."
        tar -xzf "$BACKUP_FILE" -C / 2>/dev/null || echo "Restore failed"

        echo "✅ Session restored from backup"
        echo ""
        echo "Check session status: ./session-status.sh"
        ;;

    "export")
        EXPORT_FILE="${2:-session-export-$(date +%Y%m%d-%H%M%S).json}"

        echo ""
        echo "Exporting session history to: $EXPORT_FILE"

        # Create export with session data
        if command -v jq &> /dev/null; then
            # Export session state and logs
            jq -n \
                --arg exported_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
                --arg session_log "$(cat "$SESSION_LOG" 2>/dev/null || echo "")" \
                '{exported_at: $exported_at, session_log: $session_log}' > "$EXPORT_FILE"

            echo "✅ Session history exported"
            echo "Export file: $EXPORT_FILE"
        else
            echo "Error: jq not found. Please install jq."
            exit 1
        fi
        ;;

    "analyze")
        echo ""
        echo "## Session Analytics"
        echo "===================="

        if [ -f "$SESSION_LOG" ]; then
            TOTAL_LINES=$(wc -l < "$SESSION_LOG" | tr -d ' ')
            COMPLETED_TASKS=$(grep -c "✅" "$SESSION_LOG" 2>/dev/null || echo "0")
            ERRORS=$(grep -c "❌" "$SESSION_LOG" 2>/dev/null || echo "0")
            WARNINGS=$(grep -c "⚠️" "$SESSION_LOG" 2>/dev/null || echo "0")

            echo "Total log entries: $TOTAL_LINES"
            echo "Completed tasks: $COMPLETED_TASKS"
            echo "Errors: $ERRORS"
            echo "Warnings: $WARNINGS"

            echo ""
            echo "### Activity Summary"
            echo "===================="

            # Show recent activity patterns
            echo "Recent patterns:"
            tail -20 "$SESSION_LOG" | grep -E "^(✅|❌|⚠️)" | sort | uniq -c | sort -rn || echo "No patterns found"

        else
            echo "No session log found for analysis"
        fi

        # Backup statistics
        echo ""
        echo "### Backup Statistics"
        echo "===================="

        if [ -d "$SESSION_BACKUP_DIR" ]; then
            TOTAL_BACKUPS=$(find "$SESSION_BACKUP_DIR" -name "session-*.tar.gz" | wc -l | tr -d ' ')
            TOTAL_SIZE=$(du -sh "$SESSION_BACKUP_DIR" | cut -f1)

            echo "Total backups: $TOTAL_BACKUPS"
            echo "Total size: $TOTAL_SIZE"

            echo ""
            echo "Oldest backup:"
            find "$SESSION_BACKUP_DIR" -name "session-*.tar.gz" -printf "%T+ %p\n" 2>/dev/null | sort | head -1 || echo "N/A"

            echo "Newest backup:"
            find "$SESSION_BACKUP_DIR" -name "session-*.tar.gz" -printf "%T+ %p\n" 2>/dev/null | sort | tail -1 || echo "N/A"
        fi
        ;;

    *)
        echo ""
        echo "Session History Management"
        echo "Usage: ./session-history.sh <command> [args]"
        echo ""
        echo "Commands:"
        echo "  show                    - Show session history (default)"
        echo "  restore <backup>        - Restore session from backup"
        echo "  export [file]          - Export session history to JSON"
        echo "  analyze                - Show session analytics"
        echo ""
        ;;
esac