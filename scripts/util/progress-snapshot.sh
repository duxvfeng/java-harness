#!/bin/bash
# progress-snapshot.sh
# Generate progress snapshot JSON from Plans.md
# Outputs progress-snapshot.v1 schema compliant JSON

set -euo pipefail

# Default values
PLANS_FILE="Plans.md"
PROJECT_NAME="current"
EVENTS_FILE=".claude/state/progress-events.jsonl"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --plans)
      PLANS_FILE="$2"
      shift 2
      ;;
    --project)
      PROJECT_NAME="$2"
      shift 2
      ;;
    --events)
      EVENTS_FILE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

# Check if Plans.md exists
if [[ ! -f "$PLANS_FILE" ]]; then
  echo "Error: Plans.md not found at $PLANS_FILE" >&2
  exit 1
fi

# Calculate optional event-based flow metrics. Missing event files are valid.

# Calculate optional event-based flow metrics. Missing event files are valid.
if command -v python3 >/dev/null 2>&1; then
  PYTHON_BIN="${PYTHON_BIN:-python3}"
elif command -v python >/dev/null 2>&1; then
  PYTHON_BIN="${PYTHON_BIN:-python}"
else
  METRICS_JSON='{"upstream_speed_tasks_per_hour":0,"downstream_blocked_tasks":0,"downstream_blocked_minutes":0,"process_time_minutes":0,"lead_time_minutes":0}'
fi
if [[ -z "${METRICS_JSON:-}" ]]; then
  METRICS_JSON=$("$PYTHON_BIN" "$SCRIPT_DIR/progress-metrics.py" --events "$EVENTS_FILE")
fi

# Temporary file for JSON output
TMP_OUTPUT=$(mktemp)

# Use awk to parse the markdown table and generate JSON
awk -v project="$PROJECT_NAME" -v metrics_json="$METRICS_JSON" '
BEGIN {
    todo_count = 0
    wip_count = 0
    done_count = 0
    in_table = 0
    current_task = ""
}

# Detect table start
/^[[:space:]]*\|[[:space:]]*Task[[:space:]]*\|[[:space:]]*.*[[:space:]]*\|[[:space:]]*DoD[[:space:]]*\|[[:space:]]*Depends[[:space:]]*\|[[:space:]]*Status/ {
    in_table = 1
    next
}

# Skip separator lines
/^[[:space:]]*\|[-[:space:]]*\|[[:space:]]*[-[:space:]]*\|[[:space:]]*[-[:space:]]*\|[[:space:]]*[-[:space:]]*\|[[:space:]]*[-[:space:]]*\|[[:space:]]*[-[:space:]]*/ {
    next
}

# Exit table on empty line or section break
/^(---[[:space:]]*$|#[[:space:]]+)/ {
    in_table = 0
}

# Process table rows
in_table && /^[[:space:]]*\|/ {
    # Remove leading/trailing whitespace and split by |
    line = $0
    gsub(/^[[:space:]]*\|/, "", line)
    gsub(/\|[[:space:]]*$/, "", line)

    # Split by |
    n = split(line, cols, /\|[[:space:]]*/)

    if (n >= 5) {
        task_num = cols[1]
        title = cols[2]
        status = cols[n]

        # Clean up title
        gsub(/\*\*/, "", title)
        gsub(/\[.*\]/, "", title)
        gsub(/`/, "", title)
        gsub(/^[[:space:]]+/, "", title)
        gsub(/[[:space:]]+$/, "", title)

        # Extract commit hash if present
        commit = ""
        if (match(status, /([a-f0-9]{7,8})/)) {
            commit = substr(status, RSTART, RLENGTH)
        }

        # Determine status
        if (status ~ /cc:TODO/) {
            todo[todo_count] = sprintf("{\"number\":\"%s\",\"title\":\"%s\"}", task_num, title)
            todo_count++
        } else if (status ~ /cc:WIP/) {
            wip[wip_count] = sprintf("{\"number\":\"%s\",\"title\":\"%s\"}", task_num, title)
            if (wip_count == 0) {
                current_task = title
            }
            wip_count++
        } else if (status ~ /cc:completed/ || status ~ /cc:完工/ || status ~ /cc:完成/ || status ~ /✅/) {
            done[done_count] = sprintf("{\"number\":\"%s\",\"title\":\"%s\",\"commit\":\"%s\"}", task_num, title, commit)
            done_count++
        }
    }
}

END {
    total = todo_count + wip_count + done_count
    progress_pct = 0
    if (total > 0) {
        progress_pct = int((done_count * 100) / total)
    }

    printf "{\n"
    printf "  \"schema\": \"progress-snapshot.v1\",\n"
    printf "  \"project\": \"%s\",\n", project
    printf "  \"current_task\": \"%s\",\n", current_task
    printf "  \"progress_pct\": %d,\n", progress_pct

    printf "  \"todo_tasks\": ["
    for (i = 0; i < todo_count; i++) {
        if (i > 0) printf ", "
        printf "%s", todo[i]
    }
    printf "],\n"

    printf "  \"wip_tasks\": ["
    for (i = 0; i < wip_count; i++) {
        if (i > 0) printf ", "
        printf "%s", wip[i]
    }
    printf "],\n"

    printf "  \"done_tasks\": ["
    for (i = 0; i < done_count; i++) {
        if (i > 0) printf ", "
        printf "%s", done[i]
    }
    printf "],\n"

    # Get current timestamp
    cmd = "date -u +\"%Y-%m-%dT%H:%M:%SZ\""
    cmd | getline timestamp
    close(cmd)

    printf "  \"elapsed_minutes\": 0,\n"
    printf "  \"estimated_total_minutes\": 0,\n"
    printf "  \"cost_so_far_usd\": 0,\n"
    printf "  \"cost_estimate_usd\": 0,\n"
    printf "  \"metrics\": %s,\n", metrics_json
    printf "  \"alerts\": [],\n"
    printf "  \"generated_at\": \"%s\"\n", timestamp
    printf "}\n"
}
' "$PLANS_FILE" > "$TMP_OUTPUT"

cat "$TMP_OUTPUT"
rm -f "$TMP_OUTPUT"
