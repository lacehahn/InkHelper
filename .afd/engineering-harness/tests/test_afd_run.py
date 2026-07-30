from __future__ import annotations
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / ".afd/engineering-harness/afd_run.py"

class AfdRunTests(unittest.TestCase):
    def run_command(self, *args):
        return subprocess.run([sys.executable, str(SCRIPT), *args], cwd=ROOT, text=True, capture_output=True)

    def test_task_transition_is_revision_guarded(self):
        with tempfile.TemporaryDirectory(dir=ROOT) as directory:
            path = Path(directory) / "state.json"
            path.write_text((ROOT / ".afd/run/run-state.template.json").read_text(encoding="utf-8"), encoding="utf-8")
            rel = path.relative_to(ROOT).as_posix()
            result = self.run_command("transition", "--input", rel, "--output", "NUL", "--machine", "task", "--task", "task-example", "--target", "IMPLEMENTING", "--revision", "0")
            self.assertEqual(0, result.returncode)
            self.assertEqual("IMPLEMENTING", json.loads(path.read_text(encoding="utf-8"))["task_states"]["task-example"])
            stale = self.run_command("transition", "--input", rel, "--output", "NUL", "--machine", "task", "--task", "task-example", "--target", "VALIDATING", "--revision", "0")
            self.assertNotEqual(0, stale.returncode)
            self.assertEqual("IMPLEMENTING", json.loads(path.read_text(encoding="utf-8"))["task_states"]["task-example"])

    def test_illegal_transition_is_rejected(self):
        with tempfile.TemporaryDirectory(dir=ROOT) as directory:
            path = Path(directory) / "state.json"
            path.write_text((ROOT / ".afd/run/run-state.template.json").read_text(encoding="utf-8"), encoding="utf-8")
            result = self.run_command("transition", "--input", path.relative_to(ROOT).as_posix(), "--output", "NUL", "--machine", "task", "--task", "task-example", "--target", "COMPLETED", "--revision", "0")
            self.assertNotEqual(0, result.returncode)
            self.assertEqual("PLANNED", json.loads(path.read_text(encoding="utf-8"))["task_states"]["task-example"])

if __name__ == "__main__":
    unittest.main()
