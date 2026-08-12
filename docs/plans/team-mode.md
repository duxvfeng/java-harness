# Team Mode: Role Slots and Access Policy

Team mode is an opt-in execution protocol for the six-stage delivery loop. It
does not replace `Plans.md`: `Plans.md` remains the source of truth for task
scope, dependencies, DoD, stage tags, and status.

## Operating principles

- Human participants own product intent, business boundaries, acceptance, and
  the final decision to advance a gate.
- Agents may prepare artifacts, implement approved work, and collect evidence,
  but must not expand scope, weaken the DoD, or bypass a gate on their own.
- Access is least-privilege. A role may read the upstream evidence it needs and
  write only the artifact or task fields assigned to that stage.
- A handoff must identify the source artifact, its producer, its timestamp, and
  any unresolved risk. The receiving stage reads the handoff instead of
  replaying the entire conversation.
- `[stage:*]` describes the delivery position and `[lane:*]` describes the
  execution path or risk; both may be present on the same task.

## Role slots by stage

| Stage | Human slot | Agent slot | Human access and authority | Agent access and limits | Gate owner |
|---|---|---|---|---|---|
| `kickoff` | Product Owner / Requester | Planner | Read and approve the Story Card; define user, boundary, non-goals, and acceptance evidence; freeze or reject the scope. May edit the Story Card and approve the transition to `understand`. | Read the request and repository context; draft the Story Card and surface ambiguity. May not silently change business scope or freeze the card. | Human Product Owner |
| `understand` | Domain Expert | Analyst | Read the frozen Story Card and relevant evidence; resolve domain questions; approve the shared context and scenarios. May correct domain assumptions and block progression when evidence is insufficient. | Read the Story Card, specs, repository evidence, and prior handoffs; produce an analysis/model and list unknowns. May not turn an assumption into a requirement without human confirmation. | Human Domain Expert |
| `tasking` | Tech Lead | Architect | Read approved understanding; approve task boundaries, dependencies, DoD, test strategy, and baseline. May edit or approve `Plans.md` and decide whether tasks are ready for `pair`. | Draft task decomposition, dependency graph, DoD, and test plan. May propose `Plans.md` changes but may not approve its own scope or remove a gate. | Human Tech Lead |
| `pair` | Reviewer / Pair | Worker | Read the task contract and handoff; review diffs, tests, and evidence; approve or request rework. May approve a task as complete only when the DoD is evidenced. | Read the assigned task and allowed source files; write implementation and tests in the approved scope; publish evidence and status updates. May not modify unrelated tasks or mark work complete without verification. | Human Reviewer |
| `showcase` | Acceptance Owner | Presenter / Review Agent | Read the completed evidence and observable result; perform business acceptance, classify findings, and approve the ship decision. May reject the result or return it to `pair`. | Prepare the demonstration and review summary from collected evidence; report gaps and risks. May not declare business acceptance or waive a finding. | Human Acceptance Owner |
| `respond` | Operator / Owner | Observer / Retro Agent | Read release/runtime feedback and decide follow-up, rollback, closeout, or new scope. May approve status synchronization and the next work item. | Read operational signals, progress snapshots, and prior evidence; calculate flow metrics and draft the retrospective/next-step recommendation. May not close an incident or create expanded scope without human approval. | Human Operator / Owner |

## Shared access policy

### Read

Each stage may read its own inputs and the previous stage's handoff. Human
owners may inspect all project evidence needed for a decision. Agents should
prefer the shared evidence feed and targeted files; broad context reads are a
fallback, not a requirement for every handoff.

### Write

Agents write drafts, implementation files, tests, evidence records, and
progress events only within the assigned task scope. Human owners may correct
business decisions, approvals, and status. Changes to `spec.md`, `Plans.md`,
the Story Card, or acceptance criteria require the relevant Human gate owner;
an agent can prepare the patch but cannot self-approve it.

### Approve and advance

Only the gate owner can advance a stage. An approval must point to evidence
that satisfies the stage DoD. If evidence is missing, contradictory, or stale,
the receiving owner keeps the task at the current stage or sends it back with
an explicit reason.

### Emergency handling

An agent may stop work and mark a handoff as blocked when it detects a safety,
data-loss, security, or reproducibility risk. This is a protective stop, not a
scope decision. The Human owner must resolve the block before the workflow can
advance.

## Minimal handoff record

Every transition should include:

1. `from_stage` and `to_stage`;
2. the task identifier and current `Plans.md` status;
3. evidence references and verification commands;
4. open risks, blocks, and assumptions;
5. the Human approval or explicit return/rework decision.
## Progress event format

`harness-progress` accepts optional JSONL events at
`.claude/state/progress-events.jsonl` (or a path supplied with
`--events`). Each valid line contains:

```json
{"task":"13.8","event":"started","at":"2026-08-11T10:00:00Z"}
```

Supported events are `started`, `completed`, `blocked`, and `unblocked`.
Malformed lines and a missing event file are ignored; the snapshot remains
valid and reports zero-valued flow metrics when no events are available.

The snapshot exposes these metrics under `metrics`:

- `upstream_speed_tasks_per_hour`: distinct completed tasks per hour from the
  first event to the completion window;
- `downstream_blocked_tasks`: tasks currently in a blocked interval;
- `downstream_blocked_minutes`: completed blocked intervals plus time spent in
  currently open blocked intervals;
- `process_time_minutes`: summed `started` → `completed` time for completed
  tasks;
- `lead_time_minutes`: elapsed delivery window from the first event to the
  last completed event, or to the reference time while work remains open.