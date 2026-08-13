# Intelligent Branch Isolation Detection

## Overview

The Intelligent Branch Isolation Detection system automatically protects your main branch by detecting the current Git context and applying appropriate isolation strategies. This prevents unstable commits from directly affecting your main development branches.

## Features

- **Automatic Detection**: Identifies branch types (main, feature, worktree) and applies appropriate strategies
- **Smart Configuration**: Customizable policies for different branch types
- **User Interaction**: Prompts for user choice when appropriate
- **Comprehensive Error Handling**: Detailed diagnostics and recovery suggestions
- **State Tracking**: Records all decisions for audit and debugging

## Isolation Strategies

The system supports three isolation strategies:

### Force Strategy (`force`)
- **Applies to**: Main branches (master, main, develop, production, staging)
- **Behavior**: Automatically creates isolated worktree without user confirmation
- **Purpose**: Protect critical branches from accidental commits
- **User Interaction**: None (automatic)

### Ask Strategy (`ask`)
- **Applies to**: Feature branches
- **Behavior**: Prompts user to choose between isolation or proceeding directly
- **Purpose**: Provide flexibility while encouraging best practices
- **User Interaction**: Required

### Skip Strategy (`skip`)
- **Applies to**: Already isolated environments (git worktrees)
- **Behavior**: No additional isolation needed
- **Purpose**: Avoid redundant isolation in already isolated contexts
- **User Interaction**: None (automatic)

## Usage Examples

### Automatic Detection (Recommended)
```bash
# Automatically detect and apply appropriate strategy
bash scripts/branch-isolation/handle-isolation.sh --auto
```

### Specific Strategy
```bash
# Force isolation regardless of branch type
bash scripts/branch-isolation/handle-isolation.sh --strategy force

# Ask user for decision
bash scripts/branch-isolation/handle-isolation.sh --strategy ask

# Skip isolation
bash scripts/branch-isolation/handle-isolation.sh --strategy skip
```

### Detection Only
```bash
# Check current branch type and strategy without executing
bash scripts/branch-isolation/detect-branch.sh --strategy

# Get detailed branch information
bash scripts/branch-isolation/detect-branch.sh --info

# Validate Git environment
bash scripts/branch-isolation/detect-branch.sh --validate
```

### Worktree Management
```bash
# Create isolated worktree
bash scripts/branch-isolation/worktree-manager.sh --create .claude/worktrees/my-task

# List all worktrees
bash scripts/branch-isolation/worktree-manager.sh --list

# Cleanup worktree
bash scripts/branch-isolation/worktree-manager.sh --cleanup .claude/worktrees/my-task isolated-work/my-task
```

## Configuration

### Configuration File
Configure branch isolation behavior in `.claude/settings.json`:

```json
{
  "branchIsolation": {
    "mainBranch": "force",
    "featureBranch": "ask"
  }
}
```

### Configuration Options

#### `mainBranch`
- **Values**: `force`, `ask`, `skip`
- **Default**: `force`
- **Description**: Isolation strategy for main branches
- **Recommendation**: Keep as `force` for maximum protection

#### `featureBranch`
- **Values**: `force`, `ask`, `skip`
- **Default**: `ask`
- **Description**: Isolation strategy for feature branches
- **Recommendation**: Use `ask` for flexibility

## Usage Scenarios

### Scenario 1: Main Branch Development
```
Current Branch: master
Detected Strategy: force
Behavior: Automatic worktree creation
User Interaction: None
```

**Example Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Branch Isolation Detection
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔒 Main Branch Protection Activated

You are currently on the 'master' branch.
For safety, work will be performed in an isolated branch.

The system will automatically create a feature branch to protect
the main branch from unstable commits.

Decision recorded: force -> auto-isolate

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Scenario 2: Feature Branch Development
```
Current Branch: feature/new-functionality
Detected Strategy: ask
Behavior: User choice prompt
User Interaction: Required
```

**Example Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Branch Isolation Detection
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔀 Feature Branch Detected

You are currently on the 'feature/new-functionality' feature branch.

Branch isolation protects your current branch from potential issues
during development. You can choose:

1) Isolate branch (recommended) - Create isolated worktree for this task
2) Skip isolation - Continue directly on current branch
3) Cancel - Stop execution and decide manually

Your choice [1/2/3]:
```

### Scenario 3: Already Isolated
```
Current Branch: isolated-work/task-123-20250101-120000-1234
Detected Strategy: skip
Behavior: No additional action needed
User Interaction: None
```

**Example Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Branch Isolation Detection
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Already Isolated

Current work is already isolated in a git worktree.
No additional isolation is needed.

Decision recorded: skip -> skip-already-isolated

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## State File Management

### Location
State files are stored in: `.claude/state/branch-isolation-decision.json`

### Format
```json
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": null,
  "codeStatus": null,
  "resetTriggers": {},
  "decisionHistory": [
    {
      "timestamp": "2025-01-01T12:00:00Z",
      "decision": "force",
      "reason": "Main branch protection required",
      "interactionType": "shell",
      "userChoice": "auto-isolate"
    }
  ],
  "metadata": {}
}
```

### Viewing Decisions
```bash
# Show current decision
bash scripts/branch-isolation/handle-isolation.sh --show

# Clear all decisions
bash scripts/branch-isolation/handle-isolation.sh --clear
```

## Error Handling and Diagnostics

### Environment Diagnostics
```bash
# Run comprehensive Git environment diagnostics
bash scripts/branch-isolation/git-error-handler.sh
```

**Example Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Git Environment Diagnostics
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Test 1: Git Installation
✅ Git is installed: git version 2.39.5
   Version: 2.39
✅ Worktree support: Available

Test 2: Repository Access
✅ Repository accessible: .git

Test 3: Branch Detection
✅ Current branch: master

Test 4: Worktree Support
✅ Worktree command works: 1 worktree(s) found

Test 5: File Permissions
✅ Repository directory is writable

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ All diagnostic tests passed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Common Errors and Solutions

#### Error: Not in a git repository
**Cause**: Working directory is not a Git repository
**Solution**:
- Navigate to your project root: `cd /path/to/your/project`
- Initialize repository if needed: `git init`

#### Error: Cannot read current branch
**Cause**: Git HEAD is corrupted or repository is in invalid state
**Solution**:
- Check repository status: `git status`
- Fix HEAD reference: `git symbolic-ref HEAD refs/heads/master`
- Verify repository integrity: `git fsck --full`

#### Error: Worktree creation failed
**Cause**: Target directory exists, insufficient permissions, or invalid branch reference
**Solution**:
- Check existing worktrees: `git worktree list`
- Clean up stale worktrees: `git worktree prune`
- Verify disk space and permissions
- Try alternative worktree path

## Best Practices

### 1. Main Branch Protection
Always keep `mainBranch` strategy set to `force` for critical branches:
```json
{
  "branchIsolation": {
    "mainBranch": "force"
  }
}
```

### 2. Feature Branch Flexibility
Use `ask` strategy for feature branches to maintain flexibility while encouraging safe practices:
```json
{
  "branchIsolation": {
    "featureBranch": "ask"
  }
}
```

### 3. Worktree Cleanup
Regularly clean up completed worktrees to maintain a clean environment:
```bash
# List worktrees
bash scripts/branch-isolation/worktree-manager.sh --list

# Remove completed worktrees
bash scripts/branch-isolation/worktree-manager.sh --cleanup <path> <branch>

# Prune stale metadata
bash scripts/branch-isolation/worktree-manager.sh --prune
```

### 4. State File Review
Periodically review the state file to understand isolation patterns and decisions:
```bash
cat .claude/state/branch-isolation-decision.json | jq '.decisionHistory[]'
```

### 5. Integration with Harness Work
The intelligent branch detection is automatically integrated into `harness-work` Phase A preparation, ensuring automatic protection for all task executions.

## Testing

### Run Unit Tests
```bash
bash tests/branch-isolation/test-branch-isolation-unit.sh
```

### Run Integration Tests
```bash
bash tests/branch-isolation/test-branch-isolation-integration.sh
```

## Troubleshooting

### Issue: Detection not working correctly
**Check**:
1. Git version (requires 2.7+ for worktree support)
2. Repository is valid: `git status`
3. Permissions on `.git` directory
4. Run diagnostics: `bash scripts/branch-isolation/git-error-handler.sh`

### Issue: Configuration not being applied
**Check**:
1. Configuration file exists: `.claude/settings.json`
2. JSON syntax is valid
3. Configuration keys match expected format

### Issue: Worktree creation fails
**Check**:
1. Target directory doesn't already exist
2. Sufficient disk space available
3. Directory permissions are correct
4. Base reference (branch/commit) is valid

## Technical Details

### Branch Type Detection
The system classifies branches using the following logic:

1. **Main branches**: `master`, `main`, `develop`, `production`, `staging`
2. **Worktrees**: Detected via `.git/worktrees` directory presence
3. **Feature branches**: All other branches

### Strategy Determination
Strategy is determined by combining:
- Current branch type
- Configuration file settings
- Default values (force for main, ask for features)

### State Management
All decisions are recorded with:
- ISO 8601 timestamp
- Branch name
- Applied strategy
- User response
- Reason for decision
- Worktree path (if created)

## Integration with Other Tools

### Harness Work Integration
The branch isolation system is automatically integrated into `harness-work` Phase A, running before task execution begins.

### Manual Invocation
For standalone use outside of harness-work:
```bash
# Automatic detection and handling
bash scripts/branch-isolation/handle-isolation.sh --auto

# Get current status only
bash scripts/branch-isolation/detect-branch.sh --info
```

## Support and Contributing

For issues, questions, or contributions related to the intelligent branch isolation system, please refer to the main project documentation and issue tracker.

## Version History

- **v1.0** (2025-01-09): Initial implementation with automatic detection, user interaction, and comprehensive error handling
