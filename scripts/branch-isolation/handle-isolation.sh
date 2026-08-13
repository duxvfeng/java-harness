#!/bin/bash
# Branch Isolation User Interaction Handler
# Handles user interaction for branch isolation decisions

# Source the detection functions with error handling
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/detect-branch.sh" 2>/dev/null || {
    echo "ERROR: Failed to load branch detection functions" >&2
    exit 1
}

# State file management
STATE_DIR=".claude/state"
DECISION_FILE="${STATE_DIR}/branch-isolation-decision.json"

# User response types
RESPONSE_ISOLATE="isolate"        # Create isolated branch
RESPONSE_SKIP="skip"             # Skip isolation, continue on current branch
RESPONSE_CANCEL="cancel"         # Cancel execution entirely
RESPONSE_RETAIN="retain"         # Retain isolated branch after completion

# Decision record timestamps
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

##
# Initialize state directory and file
##
init_state_file() {
    mkdir -p "${STATE_DIR}" 2>/dev/null

    # Create empty decision file if it doesn't exist
    if [[ ! -f "${DECISION_FILE}" ]]; then
        cat > "${DECISION_FILE}" <<EOF
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": null,
  "codeStatus": null,
  "resetTriggers": {
    "autoResetCondition": "branch_clean_and_no_uncommitted_changes",
    "autoResetAfterHours": 4,
    "manualResetAvailable": true,
    "taskSeriesComplete": false,
    "autoResetEnabled": true
  },
  "decisionHistory": [],
  "metadata": {
    "createdAt": "${TIMESTAMP}",
    "updatedAt": "${TIMESTAMP}",
    "version": "2.0"
  }
}
EOF
    fi

    if command -v jq >/dev/null 2>&1; then
        jq -e '.version == "2.0" and .schemaType == "branch-isolation-state-v2"' \
            "${DECISION_FILE}" >/dev/null 2>&1
    elif command -v node >/dev/null 2>&1; then
        STATE_FILE="${DECISION_FILE}" node -e 'const fs=require("fs"); const s=JSON.parse(fs.readFileSync(process.env.STATE_FILE)); if(s.version!=="2.0"||s.schemaType!=="branch-isolation-state-v2") process.exit(1);'
    else
        echo "ERROR: ${DECISION_FILE} is not a valid v2 isolation state file" >&2
        return 1
    fi
}

##
# Record user decision in state file
# Arguments: strategy, user_response, [reason]
##
record_decision() {
    local strategy="$1"
    local user_response="$2"
    local reason="${3:-No reason provided}"
    local current_branch
    local worktree_path="${4:-N/A}"

    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    local temp_file="${DECISION_FILE}.tmp"

    if command -v jq >/dev/null 2>&1; then
        jq --arg timestamp "${TIMESTAMP}" \
           --arg branch "${current_branch}" \
           --arg strategy "${strategy}" \
           --arg user_choice "${user_response}" \
           --arg reason_value "${reason}" \
           --arg worktree "${worktree_path}" \
           '.decisionHistory += [{
              timestamp: $timestamp, seriesId: null, task: null,
              decision: $strategy,
              reason: ($reason_value + " [branch: " + $branch + "]"),
              interactionType: "shell", userChoice: $user_choice,
              worktreePath: $worktree
            }] | .metadata.updatedAt = $timestamp' \
           "${DECISION_FILE}" > "${temp_file}"
    elif command -v node >/dev/null 2>&1; then
        STATE_FILE="${DECISION_FILE}" \
        STATE_TEMP="${temp_file}" \
        DECISION_TIMESTAMP="${TIMESTAMP}" \
        DECISION_BRANCH="${current_branch}" \
        DECISION_STRATEGY="${strategy}" \
        DECISION_CHOICE="${user_response}" \
        DECISION_REASON="${reason}" \
        DECISION_WORKTREE="${worktree_path}" \
        node -e 'const fs=require("fs"); const s=JSON.parse(fs.readFileSync(process.env.STATE_FILE)); s.decisionHistory=s.decisionHistory||[]; s.decisionHistory.push({timestamp:process.env.DECISION_TIMESTAMP,seriesId:null,task:null,decision:process.env.DECISION_STRATEGY,reason:process.env.DECISION_REASON+" [branch: "+process.env.DECISION_BRANCH+"]",interactionType:"shell",userChoice:process.env.DECISION_CHOICE,worktreePath:process.env.DECISION_WORKTREE}); s.metadata=s.metadata||{}; s.metadata.updatedAt=process.env.DECISION_TIMESTAMP; fs.writeFileSync(process.env.STATE_TEMP,JSON.stringify(s,null,2)+"\\n");'
    else
        echo "ERROR: jq or node is required to update ${DECISION_FILE}" >&2
        return 1
    fi

    if [[ $? -eq 0 ]]; then
        mv "${temp_file}" "${DECISION_FILE}"
    else
        rm -f "${temp_file}"
        echo "ERROR: Failed to persist isolation decision" >&2
        return 1
    fi

    echo "Decision recorded: ${strategy} -> ${user_response}"
}

##
# Handle force isolation strategy (main branch)
# Returns: 0 for proceed, 1 for cancel
##
handle_force_isolation() {
    local current_branch
    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    echo "🔒 Main Branch Protection Activated"
    echo ""
    echo "You are currently on the '${current_branch}' branch."
    echo "For safety, work will be performed in an isolated branch."
    echo ""
    echo "The system will automatically create a feature branch to protect"
    echo "the main branch from unstable commits."
    echo ""

    # Record forced decision
    record_decision "force" "auto-isolate" "Main branch protection required"

    return 0
}

##
# Handle ask isolation strategy (feature branch)
# Returns: 0 for proceed, 1 for cancel
##
handle_ask_isolation() {
    local current_branch
    local user_choice
    local reason

    current_branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)

    echo "🔀 Feature Branch Detected"
    echo ""
    echo "You are currently on the '${current_branch}' feature branch."
    echo ""
    echo "Branch isolation protects your current branch from potential issues"
    echo "during development. You can choose:"
    echo ""
    echo "1) Isolate branch (recommended) - Create isolated worktree for this task"
    echo "2) Skip isolation - Continue directly on current branch"
    echo "3) Cancel - Stop execution and decide manually"
    echo ""

    # Get user choice
    read -p "Your choice [1/2/3]: " user_choice

    case "${user_choice}" in
        1|"isolate"|"yes"|"y")
            reason="User chose to isolate feature branch"
            record_decision "ask" "isolate" "${reason}"
            echo "✅ Branch isolation will be created"
            return 0
            ;;
        2|"skip"|"no"|"n")
            reason="User chose to skip isolation on feature branch"
            record_decision "ask" "skip" "${reason}"
            echo "⚠️  Proceeding without branch isolation"
            return 0
            ;;
        3|"cancel"|"c"|"q"|"quit")
            reason="User cancelled execution on feature branch"
            record_decision "ask" "cancel" "${reason}"
            echo "❌ Execution cancelled by user"
            return 1
            ;;
        *)
            echo "⚠️  Invalid choice. Defaulting to isolation (recommended)."
            record_decision "ask" "isolate" "Invalid choice, defaulted to isolation"
            return 0
            ;;
    esac
}

##
# Handle skip isolation strategy (already isolated)
# Returns: 0 for proceed
##
handle_skip_isolation() {
    local git_dir
    local worktree_name

    git_dir=$(git rev-parse --git-dir 2>/dev/null)

    # Try to get worktree name
    if [[ -d "${git_dir}/worktrees" ]]; then
        worktree_name=$(basename "$(pwd)")
    fi

    echo "✅ Already Isolated"
    echo ""
    echo "Current work is already isolated in a git worktree."
    echo "No additional isolation is needed."
    echo ""

    # Record skip decision
    record_decision "skip" "skip-already-isolated" "Already in worktree: ${worktree_name}"

    return 0
}

##
# Main handler function - determines appropriate interaction based on strategy
# Arguments: [strategy] (if not provided, will auto-detect)
# Returns: 0 for proceed, 1 for cancel/stop
##
handle_branch_isolation() {
    local strategy="${1:-}"
    local exit_code=0

    # Auto-detect strategy if not provided
    if [[ -z "${strategy}" ]]; then
        strategy=$(detect_branch_isolation_strategy) || {
            echo "❌ ERROR: Failed to detect isolation strategy"
            return 1
        }
    fi

    # Initialize state file
    init_state_file

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "   Branch Isolation Detection"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Handle based on strategy
    case "${strategy}" in
        "${ISOLATION_STRATEGY_FORCE}")
            handle_force_isolation
            exit_code=$?
            ;;
        "${ISOLATION_STRATEGY_ASK}")
            handle_ask_isolation
            exit_code=$?
            ;;
        "${ISOLATION_STRATEGY_SKIP}")
            handle_skip_isolation
            exit_code=0
            ;;
        *)
            echo "❌ ERROR: Unknown isolation strategy: ${strategy}"
            record_decision "unknown" "error" "Unknown strategy: ${strategy}"
            exit_code=1
            ;;
    esac

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    return ${exit_code}
}

##
# Get current isolation decision from state file
# Returns: Current decision JSON or empty if none
##
get_current_decision() {
    if [[ -f "${DECISION_FILE}" ]]; then
        cat "${DECISION_FILE}"
    else
        echo "{}"
    fi
}

##
# Clear all recorded decisions (for testing or reset)
##
clear_decisions() {
    init_state_file || return 1
    local temp_file="${DECISION_FILE}.tmp"
    cat > "${temp_file}" <<EOF
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": null,
  "codeStatus": null,
  "resetTriggers": {
    "autoResetCondition": "branch_clean_and_no_uncommitted_changes",
    "autoResetAfterHours": 4,
    "manualResetAvailable": true,
    "taskSeriesComplete": false,
    "autoResetEnabled": true
  },
  "decisionHistory": [],
  "metadata": {
    "createdAt": "${TIMESTAMP}",
    "updatedAt": "${TIMESTAMP}",
    "version": "2.0"
  }
}
EOF
    mv "${temp_file}" "${DECISION_FILE}" || {
        rm -f "${temp_file}"
        echo "ERROR: Failed to clear isolation decisions" >&2
        return 1
    }
    echo "All decisions cleared"
}

# Export functions for use in other scripts
export -f init_state_file
export -f record_decision
export -f handle_force_isolation
export -f handle_ask_isolation
export -f handle_skip_isolation
export -f handle_branch_isolation
export -f get_current_decision
export -f clear_decisions

# If script is executed directly (not sourced), run interaction
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Parse command line arguments
    case "${1:-}" in
        --strategy)
            # Handle specific strategy
            handle_branch_isolation "${2:-}"
            exit $?
            ;;
        --auto)
            # Auto-detect and handle strategy
            handle_branch_isolation
            exit $?
            ;;
        --clear)
            # Clear all decisions
            clear_decisions
            exit 0
            ;;
        --show)
            # Show current decision
            get_current_decision
            exit 0
            ;;
        *)
            # Default: auto-detect and handle
            handle_branch_isolation
            exit $?
            ;;
    esac
fi
