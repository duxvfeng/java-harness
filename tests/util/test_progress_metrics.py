import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
METRICS_SCRIPT = REPO_ROOT / "scripts" / "util" / "progress-metrics.py"
RENDER_SCRIPT = REPO_ROOT / "scripts" / "util" / "render-html.sh"


class ProgressMetricsTest(unittest.TestCase):
    def run_metrics(self, events, now="2026-08-11T11:00:00Z"):
        with tempfile.TemporaryDirectory() as directory:
            events_path = Path(directory) / "events.jsonl"
            events_path.write_text(
                "".join(json.dumps(event) + "\n" for event in events),
                encoding="utf-8",
            )
            result = subprocess.run(
                [
                    sys.executable,
                    str(METRICS_SCRIPT),
                    "--events",
                    str(events_path),
                    "--now",
                    now,
                ],
                check=True,
                capture_output=True,
                text=True,
            )
            return json.loads(result.stdout)

    def test_missing_events_returns_compatible_zero_metrics(self):
        result = subprocess.run(
            [
                sys.executable,
                str(METRICS_SCRIPT),
                "--events",
                str(Path(tempfile.gettempdir()) / "missing-progress-events.jsonl"),
                "--now",
                "2026-08-11T11:00:00Z",
            ],
            check=True,
            capture_output=True,
            text=True,
        )

        self.assertEqual(
            json.loads(result.stdout),
            {
                "upstream_speed_tasks_per_hour": 0,
                "downstream_blocked_tasks": 0,
                "downstream_blocked_minutes": 0,
                "process_time_minutes": 0,
                "lead_time_minutes": 0,
            },
        )

    def test_process_and_lead_time_are_calculated_from_events(self):
        metrics = self.run_metrics(
            [
                {"task": "13.8", "event": "started", "at": "2026-08-11T10:00:00Z"},
                {"task": "13.9", "event": "started", "at": "2026-08-11T10:05:00Z"},
                {"task": "13.8", "event": "completed", "at": "2026-08-11T10:45:00Z"},
                {"task": "13.9", "event": "completed", "at": "2026-08-11T11:00:00Z"},
            ]
        )

        self.assertEqual(metrics["process_time_minutes"], 100)
        self.assertEqual(metrics["lead_time_minutes"], 60)
        self.assertEqual(metrics["upstream_speed_tasks_per_hour"], 2)

    def test_blocked_time_and_current_blocked_tasks_are_calculated(self):
        metrics = self.run_metrics(
            [
                {"task": "13.9", "event": "started", "at": "2026-08-11T10:00:00Z"},
                {"task": "13.9", "event": "blocked", "at": "2026-08-11T10:10:00Z"},
                {"task": "13.9", "event": "unblocked", "at": "2026-08-11T10:25:00Z"},
                {"task": "13.8", "event": "blocked", "at": "2026-08-11T10:30:00Z"},
            ]
        )

        self.assertEqual(metrics["downstream_blocked_minutes"], 45)
        self.assertEqual(metrics["downstream_blocked_tasks"], 1)

    def test_html_renderer_declares_observable_consequence_metrics(self):
        renderer = RENDER_SCRIPT.read_text(encoding="utf-8")

        for label in (
            "Upstream Speed",
            "Downstream Blocked",
            "Blocked Time",
            "Process Time",
            "Lead Time",
        ):
            self.assertIn(label, renderer)


if __name__ == "__main__":
    unittest.main()