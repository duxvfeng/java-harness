#!/usr/bin/env python3
"""Calculate observable delivery-flow metrics from progress events."""

from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


METRIC_NAMES = (
    "upstream_speed_tasks_per_hour",
    "downstream_blocked_tasks",
    "downstream_blocked_minutes",
    "process_time_minutes",
    "lead_time_minutes",
)
VALID_EVENTS = {"started", "completed", "blocked", "unblocked"}


def parse_timestamp(value: Any) -> datetime | None:
    if not isinstance(value, str) or not value.strip():
        return None
    normalized = value.strip()
    if normalized.endswith("Z"):
        normalized = normalized[:-1] + "+00:00"
    try:
        parsed = datetime.fromisoformat(normalized)
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def zero_metrics() -> dict[str, int | float]:
    return {name: 0 for name in METRIC_NAMES}


def rounded(value: float) -> int | float:
    result = round(value, 2)
    if result.is_integer():
        return int(result)
    return result


def load_events(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []

    events: list[dict[str, Any]] = []
    with path.open(encoding="utf-8") as source:
        for line in source:
            if not line.strip():
                continue
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            if not isinstance(event, dict):
                continue
            task = event.get("task")
            event_type = event.get("event")
            timestamp = parse_timestamp(event.get("at"))
            if not isinstance(task, str) or not task.strip():
                continue
            if event_type not in VALID_EVENTS or timestamp is None:
                continue
            events.append({"task": task.strip(), "event": event_type, "at": timestamp})
    return sorted(events, key=lambda item: item["at"])


def calculate_metrics(events: list[dict[str, Any]], now: datetime) -> dict[str, int | float]:
    if not events:
        return zero_metrics()

    starts: defaultdict[str, list[datetime]] = defaultdict(list)
    blocked_since: dict[str, datetime] = {}
    blocked_minutes = 0.0
    process_minutes = 0.0
    completed_tasks: set[str] = set()
    completion_times: list[datetime] = []

    for item in events:
        task = item["task"]
        event_type = item["event"]
        timestamp = item["at"]

        if event_type == "started":
            starts[task].append(timestamp)
        elif event_type == "completed":
            completion_times.append(timestamp)
            completed_tasks.add(task)
            if starts[task]:
                process_minutes += (timestamp - starts[task].pop(0)).total_seconds() / 60
            if task in blocked_since:
                blocked_minutes += (timestamp - blocked_since.pop(task)).total_seconds() / 60
        elif event_type == "blocked":
            if task not in blocked_since:
                blocked_since[task] = timestamp
        elif event_type == "unblocked":
            if task in blocked_since:
                blocked_minutes += (timestamp - blocked_since.pop(task)).total_seconds() / 60

    for timestamp in blocked_since.values():
        blocked_minutes += (now - timestamp).total_seconds() / 60

    active_tasks = {task for task, task_starts in starts.items() if task_starts}
    flow_is_open = bool(active_tasks or blocked_since)
    first_event = events[0]["at"]
    last_event = now if flow_is_open else events[-1]["at"]
    lead_minutes = max(0.0, (last_event - first_event).total_seconds() / 60)

    upstream_speed = 0.0
    if completed_tasks and completion_times:
        completion_window = max(0.0, (max(completion_times) - first_event).total_seconds() / 3600)
        if completion_window > 0:
            upstream_speed = len(completed_tasks) / completion_window

    return {
        "upstream_speed_tasks_per_hour": rounded(upstream_speed),
        "downstream_blocked_tasks": len(blocked_since),
        "downstream_blocked_minutes": rounded(max(0.0, blocked_minutes)),
        "process_time_minutes": rounded(max(0.0, process_minutes)),
        "lead_time_minutes": rounded(lead_minutes),
    }


def parse_now(value: str | None) -> datetime:
    parsed = parse_timestamp(value) if value else None
    if parsed is None:
        return datetime.now(timezone.utc)
    return parsed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--events",
        default=".claude/state/progress-events.jsonl",
        type=Path,
        help="JSONL event file; missing files produce zero metrics",
    )
    parser.add_argument("--now", help="UTC reference timestamp, primarily for tests")
    args = parser.parse_args()

    metrics = calculate_metrics(load_events(args.events), parse_now(args.now))
    json.dump(metrics, sys.stdout, ensure_ascii=False, separators=(",", ":"))
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())