#!/bin/bash
# render-html.sh
# Render HTML from template and JSON data

set -euo pipefail

TEMPLATE=""
DATA_FILE=""
OUTPUT_PATH=""

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --template)
      TEMPLATE="$2"
      shift 2
      ;;
    --data)
      DATA_FILE="$2"
      shift 2
      ;;
    --out)
      OUTPUT_PATH="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$TEMPLATE" ]] || [[ -z "$DATA_FILE" ]] || [[ -z "$OUTPUT_PATH" ]]; then
  echo "Error: Missing required arguments" >&2
  exit 1
fi

if [[ ! -f "$DATA_FILE" ]]; then
  echo "Error: Data file not found: $DATA_FILE" >&2
  exit 1
fi

# Read JSON data
DATA=$(cat "$DATA_FILE")

# Extract values using jq
PROJECT=$(echo "$DATA" | jq -r '.project')
CURRENT_TASK=$(echo "$DATA" | jq -r '.current_task')
PROGRESS_PCT=$(echo "$DATA" | jq -r '.progress_pct')
TODO_COUNT=$(echo "$DATA" | jq -r '.todo_tasks | length')
WIP_COUNT=$(echo "$DATA" | jq -r '.wip_tasks | length')
DONE_COUNT=$(echo "$DATA" | jq -r '.done_tasks | length')
ELAPSED_MINUTES=$(echo "$DATA" | jq -r '.elapsed_minutes')
ESTIMATED_TOTAL_MINUTES=$(echo "$DATA" | jq -r '.estimated_total_minutes')
COST_SO_FAR=$(echo "$DATA" | jq -r '.cost_so_far_usd')
COST_ESTIMATE=$(echo "$DATA" | jq -r '.cost_estimate_usd')
GENERATED_AT=$(echo "$DATA" | jq -r '.generated_at')

# Format task lists
TODO_ITEMS=$(echo "$DATA" | jq -r '.todo_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' | tr '\n' ' ')
WIP_ITEMS=$(echo "$DATA" | jq -r '.wip_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' | tr '\n' ' ')
DONE_ITEMS=$(echo "$DATA" | jq -r '.done_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' | tr '\n' ' ')

# Calculate time display
if [[ $ELAPSED_MINUTES -gt 0 ]]; then
  HOURS=$((ELAPSED_MINUTES / 60))
  MINS=$((ELAPSED_MINUTES % 60))
  TIME_DISPLAY="${HOURS}h ${MINS}m"
else
  TIME_DISPLAY="0m"
fi

# Generate HTML
cat > "$OUTPUT_PATH" <<EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Progress Snapshot - $PROJECT</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .container {
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            max-width: 900px;
            width: 100%;
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }

        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            font-weight: 700;
        }

        .header .subtitle {
            font-size: 1.1em;
            opacity: 0.9;
        }

        .content {
            padding: 30px;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }

        .stat-card {
            background: #f8f9fa;
            border-radius: 15px;
            padding: 20px;
            text-align: center;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }

        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
        }

        .stat-card .label {
            font-size: 0.9em;
            color: #6c757d;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 10px;
        }

        .stat-card .value {
            font-size: 2em;
            font-weight: 700;
            color: #2d3748;
        }

        .progress-bar-container {
            margin: 30px 0;
        }

        .progress-bar-label {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
            font-weight: 600;
            color: #2d3748;
        }

        .progress-bar {
            height: 30px;
            background: #e9ecef;
            border-radius: 15px;
            overflow: hidden;
            position: relative;
        }

        .progress-bar-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            border-radius: 15px;
            transition: width 0.6s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: 600;
            font-size: 0.9em;
        }

        .current-task {
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 20px;
            margin: 20px 0;
            border-radius: 8px;
        }

        .current-task .label {
            font-weight: 600;
            color: #856404;
            margin-bottom: 8px;
            text-transform: uppercase;
            font-size: 0.85em;
            letter-spacing: 1px;
        }

        .current-task .task {
            font-size: 1.1em;
            color: #2d3748;
        }

        .tasks-section {
            margin: 30px 0;
        }

        .tasks-section h3 {
            margin-bottom: 15px;
            color: #2d3748;
            font-size: 1.3em;
        }

        .task-list {
            list-style: none;
        }

        .task-list li {
            padding: 12px 15px;
            margin-bottom: 8px;
            border-radius: 8px;
            background: #f8f9fa;
            display: flex;
            align-items: center;
            transition: background 0.2s ease;
        }

        .task-list li:hover {
            background: #e9ecef;
        }

        .task-number {
            background: #667eea;
            color: white;
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 0.85em;
            font-weight: 600;
            margin-right: 12px;
            min-width: 50px;
            text-align: center;
        }

        .tasks-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
        }

        .task-column h3 {
            color: #2d3748;
            margin-bottom: 15px;
            font-size: 1.1em;
            display: flex;
            align-items: center;
        }

        .task-column h3 .count {
            background: #e9ecef;
            color: #2d3748;
            padding: 2px 8px;
            border-radius: 10px;
            font-size: 0.8em;
            margin-left: 10px;
        }

        .wip-column h3 {
            color: #ffc107;
        }

        .done-column h3 {
            color: #28a745;
        }

        .footer {
            text-align: center;
            padding: 20px;
            color: #6c757d;
            font-size: 0.9em;
            border-top: 1px solid #e9ecef;
        }

        .empty-state {
            text-align: center;
            padding: 40px;
            color: #6c757d;
        }

        .empty-state svg {
            width: 80px;
            height: 80px;
            margin-bottom: 20px;
            opacity: 0.5;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 $PROJECT Progress</h1>
            <div class="subtitle">Real-time project status dashboard</div>
        </div>

        <div class="content">
EOF

# Add current task if exists
if [[ -n "$CURRENT_TASK" ]]; then
    cat >> "$OUTPUT_PATH" <<EOF
            <div class="current-task">
                <div class="label">🔨 Currently Working On</div>
                <div class="task">$CURRENT_TASK</div>
            </div>

EOF
fi

# Add progress bar
cat >> "$OUTPUT_PATH" <<EOF
            <div class="progress-bar-container">
                <div class="progress-bar-label">
                    <span>Overall Progress</span>
                    <span>$PROGRESS_PCT%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-bar-fill" style="width: $PROGRESS_PCT%">
                        $PROGRESS_PCT%
                    </div>
                </div>
            </div>

            <div class="stats-grid">
                <div class="stat-card">
                    <div class="label">Total Tasks</div>
                    <div class="value">$((TODO_COUNT + WIP_COUNT + DONE_COUNT))</div>
                </div>
                <div class="stat-card">
                    <div class="label">To Do</div>
                    <div class="value" style="color: #6c757d;">$TODO_COUNT</div>
                </div>
                <div class="stat-card">
                    <div class="label">In Progress</div>
                    <div class="value" style="color: #ffc107;">$WIP_COUNT</div>
                </div>
                <div class="stat-card">
                    <div class="label">Completed</div>
                    <div class="value" style="color: #28a745;">$DONE_COUNT</div>
                </div>
            </div>

EOF

# Add timing info if available
if [[ $ELAPSED_MINUTES -gt 0 ]]; then
    cat >> "$OUTPUT_PATH" <<EOF
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="label">Time Elapsed</div>
                    <div class="value">$TIME_DISPLAY</div>
                </div>
                <div class="stat-card">
                    <div class="label">Cost So Far</div>
                    <div class="value">\$$(printf "%.2f" $COST_SO_FAR)</div>
                </div>
            </div>

EOF
fi

# Add tasks section if there are any tasks
if [[ $((TODO_COUNT + WIP_COUNT + DONE_COUNT)) -gt 0 ]]; then
    cat >> "$OUTPUT_PATH" <<EOF
            <div class="tasks-grid">
EOF

    # TODO column
    if [[ $TODO_COUNT -gt 0 ]]; then
        cat >> "$OUTPUT_PATH" <<EOF
                <div class="task-column">
                    <h3>📋 To Do <span class="count">$TODO_COUNT</span></h3>
                    <ul class="task-list">
EOF
        echo "$DATA" | jq -r '.todo_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' >> "$OUTPUT_PATH"
        cat >> "$OUTPUT_PATH" <<EOF
                    </ul>
                </div>
EOF
    fi

    # WIP column
    if [[ $WIP_COUNT -gt 0 ]]; then
        cat >> "$OUTPUT_PATH" <<EOF
                <div class="task-column wip-column">
                    <h3>🔨 In Progress <span class="count">$WIP_COUNT</span></h3>
                    <ul class="task-list">
EOF
        echo "$DATA" | jq -r '.wip_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' >> "$OUTPUT_PATH"
        cat >> "$OUTPUT_PATH" <<EOF
                    </ul>
                </div>
EOF
    fi

    # Done column
    if [[ $DONE_COUNT -gt 0 ]]; then
        cat >> "$OUTPUT_PATH" <<EOF
                <div class="task-column done-column">
                    <h3>✅ Completed <span class="count">$DONE_COUNT</span></h3>
                    <ul class="task-list">
EOF
        echo "$DATA" | jq -r '.done_tasks[] | "<li><span class=\"task-number\">\(.number)</span> \(.title)</li>"' >> "$OUTPUT_PATH"
        cat >> "$OUTPUT_PATH" <<EOF
                    </ul>
                </div>
EOF
    fi

    cat >> "$OUTPUT_PATH" <<EOF
            </div>

EOF
fi

# Add footer
cat >> "$OUTPUT_PATH" <<EOF
        </div>

        <div class="footer">
            Generated at $GENERATED_AT | Schema: progress-snapshot.v1
        </div>
    </div>
</body>
</html>
EOF

echo "HTML generated: $OUTPUT_PATH"