# Branch Isolation State Management - User Guide

**Version**: 2.0
**Last Updated**: 2026-08-13
**Feature Phase**: Phase 17

## Overview

Branch Isolation State Management provides intelligent tracking of your isolation decisions across task series, eliminating redundant prompts while maintaining code safety.

## What's New in v2.0

### 🎯 Key Improvements

1. **Task Series Tracking** - Related tasks are recognized as a single series
2. **Intelligent Reset** - State automatically resets when work is complete
3. **Clear State Visibility** - Always see current isolation status
4. **Smart State Detection** - System knows when you're in the middle of work

### 🔄 State Lifecycle

```
[INITIAL] → [ISOLATED] → [ACTIVE USE] → [READY FOR RESET] → [RESET] → [INITIAL]
```

- **Initial**: No active isolation, starting fresh
- **Isolated**: Branch isolation created for task series
- **Active Use**: Working in isolated branch, no redundant prompts
- **Ready for Reset**: Work complete, ready to clean up
- **Reset**: State cleared, ready for next task series

## How It Works

### Scenario 1: New Task Series

```bash
/harness-work 17.1-17.10
```

**First Interaction**:
```
🔀 Branch Isolation State
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Current State: Initial
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔀 No active branch isolation
📋 First task in series - setup required

📊 Series Information
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Choose an option:
  1) Isolate branch (recommended) - Create isolated worktree for this task series
  2) Skip isolation - Continue directly on current branch
  3) Cancel - Stop execution and decide manually

Your choice [1/2/3]:
```

**After Choosing Isolation**: System creates isolated branch and remembers your decision.

### Scenario 2: Continuing Task Series

```bash
/harness-work 17.2  # Second task in series
```

**No Prompt Needed!**
```
✅ Already Isolated
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚡ Working in isolated branch for task series

📊 Series Information
  Series ID          : phase-17-task-series-20260813-143022
  Current Task       : 17.2
  Tasks Completed    : 1 of 10
  Completion         : [████████░░] 80%

📁 Code Status
  Branch Status      : ✅ Clean
  Commit Status      : ⚠️  Uncommitted changes
  Total Commits      : 3

Continuing with current isolation... ✅
```

### Scenario 3: Automatic Reset Detection

When you complete work and commit everything:

```bash
git add -A
git commit -m "feat: complete Phase 17 implementation"
/harness-work 18.1  # Next phase, first task
```

**System Detects Reset Conditions**:
```
🔄 Reset Recommended
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Based on current conditions, resetting isolation state is recommended:

✅ Branch is clean and no uncommitted changes
✅ Task series is complete (10 tasks completed)

State will be reset automatically for new task series.

🔄 Resetting isolation state... ✅
```

## User Interface

### Visual Indicators

| State | Icon | Meaning |
|-------|------|---------|
| Initial | 🔀 | No active isolation, ready for new series |
| Isolated | 🔒 | Branch isolation established |
| Active Use | ⚡ | Working in isolated branch |
| Ready for Reset | 🔄 | Work complete, ready to reset |
| Reset | ✅ | State cleared, ready for new series |

### Status Displays

**Code Status Indicators**:
- `✅ Clean` - No uncommitted changes
- `⚠️ Modified` - Has uncommitted changes
- `✅ All committed` - All changes committed

**Completion Progress Bar**:
```
Completion: [████████░░] 80%
```

## Configuration

### Default Settings

```json
{
  "branchIsolation": {
    "mainBranch": "force",      // Auto-isolate on main branch
    "featureBranch": "ask"     // Ask user on feature branches
  },
  "resetTriggers": {
    "autoResetEnabled": true,
    "autoResetAfterHours": 4,
    "autoResetCondition": "branch_clean_and_no_uncommitted_changes"
  }
}
```

### Customization

You can customize reset behavior in `.claude/settings.json`:

```json
{
  "harness-work": {
    "branchIsolation": {
      "resetInactivityHours": 8,  // Wait 8 hours before auto-reset
      "manualResetOnly": false      // Disable auto-reset
    }
  }
}
```

## Advanced Usage

### Manual Reset

If you want to manually reset isolation state:

```bash
# The system will offer reset option when appropriate
/harness-work <task>
# Choose option 2) Reset state when prompted
```

### Check Current State

```bash
# Current state is displayed at the start of each /harness-work command
# Or check the state file directly:
cat .claude/state/branch-isolation-decision.json
```

### Override Isolation Decision

```bash
# Force isolation even on feature branch
/harness-work 17.1 --isolate-branch

# Skip isolation even on main branch (not recommended)
/harness-work 17.1 --no-isolation
```

## Troubleshooting

### Issue: State Not Resetting

**Problem**: Isolation state persists even after work is complete.

**Solutions**:
1. Ensure all changes are committed: `git status`
2. Check branch cleanliness: `git diff --name-only`
3. Manually reset: Choose reset option in next interaction

### Issue: Repeated Prompts

**Problem**: Getting asked about isolation repeatedly.

**Solutions**:
1. Check if tasks are in same series (e.g., 17.1, 17.2, 17.3)
2. Verify state file exists: `.claude/state/branch-isolation-decision.json`
3. Look for error messages in state evaluation

### Issue: State File Corruption

**Problem**: State file becomes unreadable.

**Solutions**:
1. System will create new state file automatically
2. Previous decisions will be lost (not critical)
3. Continue working - system will recover gracefully

## Best Practices

### ✅ Recommended Practices

1. **Commit Work Regularly** - Helps system detect completion
2. **Use Task Series** - Related tasks (17.1-17.10) treated as single unit
3. **Let System Reset** - Automatic reset when work is complete
4. **Read State Displays** - Understand your current isolation context

### ⚠️ Things to Avoid

1. **Don't Cancel Mid-Series** - Interrupts task sequence tracking
2. **Don't Ignore State Displays** - Important information about current work
3. **Don't Manually Delete State File** - System manages it automatically
4. **Don't Mix Task Series** - Keep related tasks together (17.x, 18.x)

## Technical Details

### State File Location

```
.claude/state/branch-isolation-decision.json
```

### State File Format

```json
{
  "version": "2.0",
  "currentSeries": {
    "seriesId": "phase-17-task-series-20260813-143022",
    "taskSequence": [17.1, 17.2, 17.3],
    "isolationActive": true
  },
  "codeStatus": {
    "branchClean": true,
    "hasUncommittedChanges": false
  },
  "decisionHistory": [...]
}
```

### Reset Conditions

Automatic reset occurs when ANY condition is met:

1. **Branch Clean + No Uncommitted Changes**
   - All work committed and pushed
   - No active changes in working directory

2. **Task Series Complete**
   - All tasks in series marked as completed
   - Completion percentage reaches 100%

3. **Inactivity with Clean Branch**
   - No activity for 4+ hours (configurable)
   - Branch remains clean during inactivity

4. **Manual Reset Requested**
   - User explicitly chooses reset option
   - Immediate state cleanup

## Migration from v1.0

### What Changed

- **v1.0**: Each task treated independently, repeated prompts
- **v2.0**: Task series tracking, intelligent state management

### Automatic Migration

When you first use v2.0:
- Existing v1.0 state files automatically migrated
- Decision history preserved
- No manual intervention required

### Backward Compatibility

- All existing `--isolate-branch` flags work as before
- Phase 10 functionality fully compatible
- No breaking changes to existing workflows

## Examples

### Example 1: Complete Feature Implementation

```bash
# Start new feature
/harness-work 17.1-17.10
# → Choose isolation, creates isolated branch

# Work through tasks
/harness-work 17.2  # No prompt, continues in isolation
/harness-work 17.3  # No prompt, continues in isolation

# Complete and commit work
git add -A && git commit -m "feat: implement feature"

# Next feature phase
/harness-work 18.1-18.5
# → System detects previous work complete, auto-resets, prompts for new series
```

### Example 2: Bug Fix Session

```bash
# Quick bug fix, skip isolation
/harness-work fix-login-bug
# → Choose option 2) Skip isolation

# Make quick fix, commit
git add -A && git commit -m "fix: login bug"

# Next task
/harness-work next-task
# → No state carried over from bug fix, starts fresh
```

### Example 3: Long-Running Feature

```bash
# Start large feature
/harness-work 10.1
# → Choose isolation, creates feature branch

# Work over multiple sessions
# Session 1:
/harness-work 10.2-10.5
# → Continues in same isolation

# Session 2 (next day):
/harness-work 10.6-10.8
# → Still in same isolation, remembers from previous day

# Complete and test
git add -A && git commit -m "feat: complete large feature"
./run-tests.sh

# Clean up
/harness-work 11.1  # New phase
# → Auto-resets, ready for new feature
```

## FAQ

**Q: Will I lose my work if state resets?**
A: No. State reset only affects tracking, not your code. All commits remain in git.

**Q: Can I continue working after reset?**
A: Yes. State reset just clears tracking. You can continue working in your branch.

**Q: What if I want different tasks in same series?**
A: The system automatically groups tasks by number patterns (17.1, 17.2, 17.x).

**Q: Can I disable automatic reset?**
A: Yes, configure `"manualResetOnly": true` in settings.

**Q: How do I know current state without running /harness-work?**
A: Check `.claude/state/branch-isolation-decision.json` or run `/harness-work` to see state display.

## Support and Feedback

For issues or suggestions about branch isolation state management:
- Check this guide first
- Review troubleshooting section
- Check state file for diagnostic information
- Report issues through normal support channels

---

**Related Documentation**:
- [Branch Isolation Reference](../harness-work/references/branch-isolation.md)
- [Architecture Design](../../architecture/branch-isolation-state-persistence-architecture.md)
- [Analysis Report](../../analysis/branch-isolation-state-analysis.md)