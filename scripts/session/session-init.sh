#!/bin/bash
set -e

echo "==================================="
echo "Session Initialization"
echo "==================================="

SESSION_DIR=".claude/session"
SESSION_ID="${1:-$(date +%Y%m%d-%H%M%S)}"
SESSION_TYPE="${2:-development}"

mkdir -p "$SESSION_DIR"

echo ""
echo "Initializing session: $SESSION_ID"
echo "Session type: $SESSION_TYPE"
echo "Start time: $(date '+%Y-%m-%d %H:%M:%S')"

# Create session state file
SESSION_STATE="$SESSION_DIR/state.json"
SESSION_LOG="$SESSION_DIR/session.log"
SESSION_INFO="$SESSION_DIR/info.json"

cat > "$SESSION_STATE" << EOF
{
  "session_id": "$SESSION_ID",
  "session_type": "$SESSION_TYPE",
  "status": "active",
  "start_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "last_activity": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "tasks_completed": 0,
  "tasks_total": 0,
  "commands_executed": 0,
  "errors": 0
}
EOF

# Create session info file
cat > "$SESSION_INFO" << EOF
{
  "session_id": "$SESSION_ID",
  "session_type": "$SESSION_TYPE",
  "user": "$(whoami)",
  "hostname": "$(hostname)",
  "working_directory": "$(pwd)",
  "git_branch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "environment": {
    "java_version": "$(java -version 2>&1 | head -1 || echo 'unknown')",
    "maven_version": "$(mvn -version | head -1 2>/dev/null || echo 'unknown')"
  }
}
EOF

# Initialize session log
cat > "$SESSION_LOG" << EOF
# Session Log: $SESSION_ID
# Started: $(date '+%Y-%m-%d %H:%M:%S')
# Type: $SESSION_TYPE
# User: $(whoami)@$(hostname)

## Session Events

EOF

# Create session hooks directory
mkdir -p "$SESSION_DIR/hooks"
mkdir -p "$SESSION_DIR/backups"
mkdir -p "$SESSION_DIR/temp"

# Create cleanup hook
cat > "$SESSION_DIR/hooks/cleanup.sh" << 'EOF'
#!/bin/bash
# Session cleanup hook
# This script runs when session ends

echo "Running session cleanup..."

# Clean up temporary files
rm -rf .claude/session/temp/*

# Create final backup
if [ -d ".claude/session/backups" ]; then
    tar -czf ".claude/session/backups/session-final-$(date +%Y%m%d-%H%M%S).tar.gz" \
        Plans.md .claude/state/ 2>/dev/null || true
fi

echo "Session cleanup completed"
EOF

chmod +x "$SESSION_DIR/hooks/cleanup.sh"

# Set active session in project state
if [ -d ".claude/state" ]; then
    cat > ".claude/state/active-session.json" << EOF
{
  "session_id": "$SESSION_ID",
  "session_type": "$SESSION_TYPE",
  "started_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "status": "active"
}
EOF
fi

echo ""
echo "✅ Session initialized successfully!"
echo ""
echo "Session details:"
echo "  Session ID: $SESSION_ID"
echo "  Type: $SESSION_TYPE"
echo "  State file: $SESSION_STATE"
echo "  Log file: $SESSION_LOG"
echo ""
echo "Session commands:"
echo "  ./session-status.sh      - Show session status"
echo "  ./session-monitor.sh     - Monitor session activity"
echo "  ./session-cleanup.sh     - Clean up session resources"
echo "  ./session-history.sh    - Show session history"