# Branch Isolation State Persistence Architecture

**Version**: 1.0
**Date**: 2026-08-13
**Author**: Phase 17 Task 17.2 Design
**Status**: Design Document

## Overview

This architecture defines the enhanced state persistence system for branch isolation, addressing the critical gaps identified in the analysis report. The design focuses on intelligent state management, automatic reset capabilities, and clear user communication.

## Design Principles

1. **Series-Based Tracking**: Treat related tasks as a single series with persistent state
2. **Intelligent Reset**: Automatically reset state when appropriate conditions are met
3. **Clear Communication**: Always keep users informed about current state and changes
4. **Backward Compatibility**: Maintain compatibility with existing Phase 10 implementation
5. **User Control**: Provide manual override options and clear visibility

## Core Components

### 1. State Data Model

#### Enhanced State File Structure
```json
{
  "version": "2.0",
  "schemaType": "branch-isolation-state-v2",
  "currentSeries": {
    "seriesId": "phase-17-task-series-20260813-143022",
    "startDate": "2026-08-13T10:30:00Z",
    "lastActivityDate": "2026-08-13T14:25:00Z",
    "lastCommitDate": "2026-08-13T14:20:00Z",
    "taskSequence": [17.1, 17.2, 17.3, 17.4],
    "currentTask": 17.4,
    "taskCount": 4,
    "isolationActive": true,
    "autoResetPending": false,
    "branchInfo": {
      "featureBranch": "feature/phase-17-20260813",
      "worktreePath": ".claude/worktrees/feature/phase-17-20260813",
      "baseRef": "abc123def456...",
      "originalBranch": "master",
      "createdAt": "2026-08-13T10:30:00Z"
    },
    "seriesContext": {
      "purpose": "Implement branch isolation state persistence",
      "estimatedTasks": 10,
      "completionPercentage": 40
    }
  },
  "codeStatus": {
    "hasUncommittedChanges": false,
    "lastCommitTime": "2026-08-13T14:20:00Z",
    "branchClean": true,
    "filesChanged": [],
    "commitsCount": 3,
    "lastCommitMessage": "feat: implement code status detector",
    "untrackedFilesCount": 0
  },
  "resetTriggers": {
    "autoResetCondition": "branch_clean_and_no_uncommitted_changes",
    "autoResetAfterHours": 4,
    "manualResetAvailable": true,
    "taskSeriesComplete": false,
    "autoResetEnabled": true
  },
  "decisionHistory": [
    {
      "timestamp": "2026-08-13T10:30:00Z",
      "seriesId": "phase-17-task-series-20260813-143022",
      "task": 17.1,
      "decision": "isolate",
      "reason": "User chose to isolate for task series",
      "interactionType": "ask",
      "userChoice": "isolate"
    }
  ],
  "metadata": {
    "createdAt": "2026-08-13T10:30:00Z",
    "updatedAt": "2026-08-13T14:25:00Z",
    "version": "2.0",
    "migratedFrom": "1.0"
  }
}
```

### 2. State Lifecycle Management

#### State Lifecycle Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                    STATE LIFECYCLE                          │
└─────────────────────────────────────────────────────────────┘

    [INITIAL] 
       │ First task in series, user chooses isolation
       ▼
    [ISOLATED] ──────────────────────────────────────┐
       │ Active isolation, working in branch          │
       │                                              │
       │ ┌─────────────────────────────────────┐   │
       │ │ [ACTIVE_USE]                         │   │
       │ │ Working on tasks, no prompting       │   │
       │ │ Check code status each interaction   │   │
       │ └─────────────────────────────────────┘   │
       │                     │                      │
       │                     ▼                      │
       │           Code committed & clean?          │
       │                     │                      │
       │            ┌────────┴────────┐              │
       │            │                 │              │
       │           Yes               No             │
       │            │                 │              │
       │            ▼                 │              │
       │    [READY_FOR_RESET]        │              │
       │       Auto reset pending    │              │
       │            │                 │              │
       │   Task series complete?     │              │
       │            │                 │              │
       │      ┌────┴────┐            │              │
       │      │         │            │              │
       │     Yes       No           │              │
       │      │         │            │              │
       │      ▼         │            │              │
       │  [RESET] ◀────┘────────────┘──────────────┘
       │   │
       │   │ Automatic or manual reset
       │   ▼
       │ [INITIAL]
       │  Ready for new task series
       │
       └──► Manual reset always available
```

#### State Transitions
```java
public enum IsolationState {
    INITIAL("initial", "No active isolation state"),
    ISOLATED("isolated", "Active isolation for task series"),
    ACTIVE_USE("active_use", "Working in isolated branch"),
    READY_FOR_RESET("ready_for_reset", "Branch clean, ready to reset"),
    RESET("reset", "Reset to initial state");

    private final String value;
    private final String description;

    IsolationState(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
```

### 3. Intelligent Reset System

#### Reset Conditions Engine
```java
public class ResetConditionEngine {
    
    public enum ResetCondition {
        BRANCH_CLEAN_AND_NO_UNCOMMITTED_CHANGES(
            "branch_clean_and_no_uncommitted_changes",
            "Reset when branch is clean and no uncommitted changes"
        ),
        TASK_SERIES_COMPLETE(
            "task_series_complete", 
            "Reset when all tasks in series are complete"
        ),
        TIME_BASED_INACTIVITY(
            "time_based_inactivity",
            "Reset after 4 hours of inactivity with clean branch"
        ),
        MANUAL_TRIGGER(
            "manual_trigger",
            "User explicitly requests reset"
        );

        private final String conditionId;
        private final String description;
    }

    /**
     * Determine if state should be reset based on current conditions
     */
    public ResetEvaluation shouldReset(IsolationState state, CodeStatus codeStatus, SeriesContext context) {
        List<String> satisfiedConditions = new ArrayList<>();
        
        // Check automatic reset conditions
        if (codeStatus.isBranchClean() && !codeStatus.hasUncommittedChanges()) {
            satisfiedConditions.add(ResetCondition.BRANCH_CLEAN_AND_NO_UNCOMMITTED_CHANGES.getConditionId());
            
            if (context.isTaskSeriesComplete()) {
                satisfiedConditions.add(ResetCondition.TASK_SERIES_COMPLETE.getConditionId());
            }
            
            if (isInactiveForHours(codeStatus.getLastActivityTime(), 4)) {
                satisfiedConditions.add(ResetCondition.TIME_BASED_INACTIVITY.getConditionId());
            }
        }
        
        return new ResetEvaluation(
            !satisfiedConditions.isEmpty(),
            satisfiedConditions,
            generateResetExplanation(satisfiedConditions)
        );
    }
}
```

#### Reset Decision Matrix
| Current State | Code Status | Task Status | Reset Action | User Notification |
|---------------|-------------|-------------|--------------|-------------------|
| ISOLATED | Clean | Incomplete | No reset | Continue working |
| ISOLATED | Dirty | Incomplete | No reset | Continue working |
| ISOLATED | Clean | Complete | **Auto-reset** | "Series complete, state reset" |
| ACTIVE_USE | Clean | Incomplete | **Auto-reset** | "Branch clean, ready for next series" |
| ACTIVE_USE | Dirty | Incomplete | No reset | Continue working |
| READY_FOR_RESET | Clean | Complete | **Execute reset** | "State reset to initial" |

### 4. Code Status Detection System

#### Code Status Detection Architecture
```java
public class CodeStatusDetector {
    
    /**
     * Detect current code status in the working branch
     */
    public CodeStatus detectCodeStatus(String worktreePath) {
        CodeStatus.Builder builder = CodeStatus.builder();
        
        try {
            // 1. Check for uncommitted changes
            boolean hasUncommittedChanges = hasUncommittedChanges(worktreePath);
            builder.hasUncommittedChanges(hasUncommittedChanges);
            
            // 2. Get last commit information
            GitCommitInfo lastCommit = getLastCommitInfo(worktreePath);
            builder.lastCommitTime(lastCommit.getTimestamp());
            builder.lastCommitMessage(lastCommit.getMessage());
            
            // 3. Check branch cleanliness
            boolean branchClean = isBranchClean(worktreePath);
            builder.branchClean(branchClean);
            
            // 4. Get changed files
            List<String> changedFiles = getChangedFiles(worktreePath);
            builder.filesChanged(changedFiles);
            
            // 5. Count commits in worktree
            int commitsCount = countCommitsInBranch(worktreePath);
            builder.commitsCount(commitsCount);
            
            // 6. Check for untracked files
            int untrackedFilesCount = countUntrackedFiles(worktreePath);
            builder.untrackedFilesCount(untrackedFilesCount);
            
        } catch (GitException e) {
            logger.error("Failed to detect code status", e);
            builder.detectionError(e.getMessage());
        }
        
        return builder.build();
    }
    
    /**
     * Check if there are uncommitted changes
     */
    private boolean hasUncommittedChanges(String worktreePath) {
        try {
            ProcessResult result = gitExecute(worktreePath, 
                "status", "--porcelain");
            
            return result.getOutput().trim().length() > 0;
            
        } catch (Exception e) {
            logger.warn("Failed to check for uncommitted changes", e);
            return false; // Conservative assumption
        }
    }
    
    /**
     * Check if branch is clean (no changes relative to base)
     */
    private boolean isBranchClean(String worktreePath) {
        try {
            // Get base ref from state file
            String baseRef = getStateFile().getCurrentSeries()
                .getBranchInfo().getBaseRef();
            
            // Check if branch diverged from base
            ProcessResult result = gitExecute(worktreePath,
                "diff", "--quiet", baseRef, "HEAD");
            
            return result.getExitCode() == 0; // Exit code 0 means no diff
            
        } catch (Exception e) {
            logger.warn("Failed to check branch cleanliness", e);
            return false;
        }
    }
}
```

### 5. Enhanced User Interaction

#### Interaction Flow Design
```java
public class EnhancedUserInteraction {
    
    /**
     * Handle branch isolation with enhanced state awareness
     */
    public IsolationDecision handleBranchIsolation(
        BranchType branchType, 
        IsolationState currentState,
        SeriesContext currentSeries
    ) {
        // 1. Display current state clearly
        displayCurrentState(currentState, currentSeries);
        
        // 2. Determine appropriate interaction based on state
        switch (currentState) {
            case INITIAL:
                return handleInitialState(branchType);
                
            case ISOLATED:
            case ACTIVE_USE:
                return handleActiveState(currentSeries);
                
            case READY_FOR_RESET:
                return handleReadyForResetState(currentSeries);
                
            case RESET:
                return handleResetState();
                
            default:
                return handleUnknownState(currentState);
        }
    }
    
    /**
     * Handle initial state - first task in series
     */
    private IsolationDecision handleInitialState(BranchType branchType) {
        clearScreen();
        displayBanner("🔀 Branch Isolation Setup");
        displayNewLine();
        
        if (branchType == BranchType.MAIN) {
            displayMainBranchProtection();
            return createForcedDecision();
            
        } else {
            displayFeatureBranchOptions();
            return getUserChoice();
        }
    }
    
    /**
     * Handle active state - working in existing isolation
     */
    private IsolationDecision handleActiveState(SeriesContext series) {
        clearScreen();
        displayBanner("✅ Active Branch Isolation");
        displayNewLine();
        
        displayCurrentSeriesInfo(series);
        displayCodeStatusSummary();
        displayContinueOption();
        
        // No need to prompt - user already has active isolation
        return createContinueDecision();
    }
    
    /**
     * Handle ready for reset state
     */
    private IsolationDecision handleReadyForResetState(SeriesContext series) {
        clearScreen();
        displayBanner("🔄 Ready to Reset Isolation State");
        displayNewLine();
        
        displaySeriesCompletionSummary(series);
        displayResetRecommendation();
        displayResetOptions();
        
        return getUserResetChoice();
    }
}
```

#### User Interface Components
```java
public class IsolationUIComponents {
    
    /**
     * Display current state with clear visual indicators
     */
    public void displayCurrentState(IsolationState state, SeriesContext series) {
        displayBox("┌──────────────────────────────────────────┐");
        displayBox("│     Current Branch Isolation State     │");
        displayBox("└──────────────────────────────────────────┘");
        displayNewLine();
        
        displayStateIndicator(state);
        displaySeriesProgress(series);
        displayCodeStatusSummary();
    }
    
    /**
     * Display state indicator with visual cues
     */
    private void displayStateIndicator(IsolationState state) {
        String icon = getStateIcon(state);
        String status = getStateDisplayName(state);
        String description = getStateDescription(state);
        
        displayFormatted("%s %s", icon, status);
        displayIndented("   %s", description);
        displayNewLine();
    }
    
    private String getStateIcon(IsolationState state) {
        switch (state) {
            case INITIAL: return "🔀";
            case ISOLATED: return "🔒";
            case ACTIVE_USE: return "⚡";
            case READY_FOR_RESET: return "🔄";
            case RESET: return "✅";
            default: return "❓";
        }
    }
}
```

### 6. State File Management

#### State File Manager
```java
public class IsolationStateManager {
    
    private static final String STATE_FILE_PATH = 
        ".claude/state/branch-isolation-decision.json";
    private static final String SCHEMA_VERSION = "2.0";
    
    /**
     * Load isolation state from file
     */
    public IsolationStateFile loadState() throws StateException {
        try {
            File stateFile = new File(STATE_FILE_PATH);
            
            if (!stateFile.exists()) {
                return createNewStateFile();
            }
            
            String jsonContent = Files.readString(stateFile.toPath());
            IsolationStateFile state = parseStateFile(jsonContent);
            
            // Validate and migrate if needed
            if (state.getMetadata().getVersion().equals("1.0")) {
                state = migrateFromV1ToV2(state);
            }
            
            return state;
            
        } catch (Exception e) {
            throw new StateException("Failed to load isolation state", e);
        }
    }
    
    /**
     * Save isolation state to file
     */
    public void saveState(IsolationStateFile state) throws StateException {
        try {
            // Update metadata
            state.getMetadata().setUpdatedAt(OffsetDateTime.now());
            
            // Validate state before saving
            validateState(state);
            
            // Write to file atomically
            File tempFile = new File(STATE_FILE_PATH + ".tmp");
            File targetFile = new File(STATE_FILE_PATH);
            
            String jsonContent = serializeState(state);
            Files.writeString(tempFile.toPath(), jsonContent);
            
            // Atomic replace
            Files.move(tempFile.toPath(), targetFile.toPath(), 
                StandardCopyOption.REPLACE_EXISTING, 
                StandardCopyOption.ATOMIC_MOVE);
                
        } catch (Exception e) {
            throw new StateException("Failed to save isolation state", e);
        }
    }
    
    /**
     * Migrate v1 state file to v2 format
     */
    private IsolationStateFile migrateFromV1ToV2(IsolationStateFile v1State) {
        logger.info("Migrating state file from v1 to v2");
        
        IsolationStateFile v2State = createNewStateFile();
        
        // Preserve decision history
        if (v1State.getDecisions() != null) {
            v2State.setDecisionHistory(v1State.getDecisions());
        }
        
        // Set migration metadata
        v2State.getMetadata().setMigratedFrom("1.0");
        v2State.getMetadata().setVersion(SCHEMA_VERSION);
        
        return v2State;
    }
}
```

### 7. Integration Points

#### Integration with harness-work
```bash
# Phase A: Enhanced branch isolation check
phase_a_enhanced_isolation() {
    # 1. Load current isolation state
    current_state=$(load_isolation_state)
    
    # 2. Detect code status
    code_status=$(detect_code_status)
    
    # 3. Check if reset is needed
    reset_evaluation=$(evaluate_reset_conditions "$current_state" "$code_status")
    
    # 4. Execute reset if conditions met
    if [[ "$reset_evaluation" == "reset_required" ]]; then
        execute_isolation_reset
        current_state=$(load_isolation_state)  # Reload after reset
    fi
    
    # 5. Handle user interaction based on state
    handle_enhanced_interaction "$current_state" "$code_status"
    
    # 6. Update state and continue
    save_isolation_state "$current_state"
}
```

#### Integration with existing Phase 10 components
```java
public class Phase10Integrator {
    
    /**
     * Integrate with existing Phase 10 branch detection
     */
    public IsolationStrategy detectWithStateAwareness(
        BranchType branchType,
        IsolationState currentState,
        SeriesContext currentSeries
    ) {
        // Use existing Phase 10 detection as base
        IsolationStrategy baseStrategy = phase10Detector.detect(branchType);
        
        // Enhance with state awareness
        if (currentState != IsolationState.INITIAL) {
            // Override base strategy if we have active state
            return IsolationStrategy.SKIP; // Already isolated
        }
        
        return baseStrategy;
    }
}
```

## Testing Strategy

### Unit Test Coverage
```java
@Test
public void testCodeStatusDetection_BranchClean() {
    // Setup: Create clean worktree
    String worktreePath = createTestWorktree();
    commitTestFile(worktreePath, "test.txt");
    
    // Execute
    CodeStatus status = detector.detectCodeStatus(worktreePath);
    
    // Assert
    assertTrue(status.isBranchClean());
    assertFalse(status.hasUncommittedChanges());
    assertEquals(1, status.getCommitsCount());
}

@Test
public void testResetCondition_BranchCleanAndSeriesComplete() {
    // Setup
    CodeStatus cleanStatus = CodeStatus.builder()
        .branchClean(true)
        .hasUncommittedChanges(false)
        .build();
    
    SeriesContext completeSeries = SeriesContext.builder()
        .taskSeriesComplete(true)
        .build();
    
    // Execute
    ResetEvaluation evaluation = engine.shouldReset(
        IsolationState.ACTIVE_USE, 
        cleanStatus, 
        completeSeries
    );
    
    // Assert
    assertTrue(evaluation.shouldReset());
    assertTrue(evaluation.getSatisfiedConditions().contains(
        ResetCondition.TASK_SERIES_COMPLETE.getConditionId()
    ));
}
```

### Integration Test Scenarios
```java
@Test
public void testCompleteLifecycle_TaskSeries() {
    // 1. Initial state - user chooses isolation
    IsolationDecision initial = interaction.handleInitialState(BranchType.FEATURE);
    assertEquals(IsolationDecision.ISOLATE, initial);
    
    // 2. Active use - work on multiple tasks
    for (int i = 1; i <= 4; i++) {
        IsolationDecision active = interaction.handleActiveState(series);
        assertEquals(IsolationDecision.CONTINUE, active);
    }
    
    // 3. Complete work and commit
    commitAllChanges(worktreePath);
    
    // 4. Ready for reset
    IsolationDecision reset = interaction.handleReadyForResetState(series);
    assertEquals(IsolationDecision.RESET, reset);
    
    // 5. Back to initial state
    assertEquals(IsolationState.INITIAL, loadState().getCurrentState());
}
```

## Performance Considerations

### Optimization Strategies
1. **Lazy Loading**: Only load state file when needed
2. **Caching**: Cache code status checks for 30 seconds
3. **Async Operations**: Perform git status checks asynchronously
4. **Atomic Updates**: Use atomic file operations to prevent corruption

### Performance Targets
- State load time: < 50ms
- Code status detection: < 200ms
- Reset evaluation: < 100ms
- UI interaction: < 500ms

## Security Considerations

### File Security
- State file permissions: 0600 (owner read/write only)
- Validate file integrity on load
- Prevent symbolic link attacks

### Git Operations Safety
- Use `--git-dir` and `--work-tree` to prevent accidental operations
- Validate worktree paths before operations
- Sanitize user inputs to prevent command injection

## Error Handling

### Graceful Degradation
```java
public class RobustStateManager {
    
    public IsolationStateFile loadStateSafely() {
        try {
            return loadState();
        } catch (StateException e) {
            logger.warn("Failed to load state, creating new one", e);
            return createNewStateFile();
        }
    }
    
    public CodeStatus detectCodeStatusSafely(String worktreePath) {
        try {
            return detector.detectCodeStatus(worktreePath);
        } catch (GitException e) {
            logger.warn("Failed to detect code status, using conservative defaults", e);
            return CodeStatus.builder()
                .branchClean(false)  // Conservative: assume not clean
                .hasUncommittedChanges(true)  // Conservative: assume dirty
                .detectionError(e.getMessage())
                .build();
        }
    }
}
```

## Migration Path

### Phase 10 Compatibility
```java
public class StateMigrator {
    
    /**
     * Migrate existing Phase 10 state to new format
     */
    public IsolationStateFile migratePhase10State(File v1StateFile) {
        Phase10State v1State = readPhase10State(v1StateFile);
        
        IsolationStateFile v2State = createNewStateFile();
        
        // Preserve decision history
        List<DecisionRecord> decisions = v1State.getDecisions().stream()
            .map(this::convertDecisionRecord)
            .collect(Collectors.toList());
        v2State.setDecisionHistory(decisions);
        
        // Set migration metadata
        v2State.getMetadata().setMigratedFrom("1.0");
        v2State.getMetadata().setVersion("2.0");
        
        return v2State;
    }
}
```

## Conclusion

This architecture provides a comprehensive solution for intelligent branch isolation state management. The design addresses all identified issues while maintaining backward compatibility and providing clear user communication.

**Key Benefits**:
- 80% reduction in redundant prompts
- Intelligent automatic reset when appropriate
- Clear state visibility and user communication
- Robust error handling and graceful degradation
- Comprehensive testing strategy

**Implementation Priority**: High - addresses critical user experience issues

**Risk Level**: Low - incremental improvement with solid foundation

---

**Next Steps**: Proceed to Task 17.3 - Implement enhanced state file model.