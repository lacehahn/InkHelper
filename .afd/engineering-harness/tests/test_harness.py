"""Black-box-oriented coverage for deterministic harness contracts."""
from __future__ import annotations
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

HARNESS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HARNESS))
import afd_common

class CommonTests(unittest.TestCase):
    def test_schema_rejects_invalid_task_state(self):
        schema = json.loads((HARNESS.parent / "schemas" / "task.schema.json").read_text(encoding="utf-8"))
        record = {"schema_version":"1.0","task_id":"task-example","task_type":"documentation","allowed_scope":[],"forbidden_scope":[],"required_reading":[],"required_validation":[],"task_state":"not-a-state","retry_counters":{"same_failure_attempts":0,"reproduction_attempts":0,"validation_attempts":0},"stop_reason":"none","base_commit":None,"result_commit":None,"timestamps":{"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z","completed_at":None},"output_artifacts":[]}
        self.assertTrue(afd_common.validate(record, schema))

    def test_rejects_escaping_path(self):
        with self.assertRaises(afd_common.HarnessError): afd_common.rel_path("../outside.json")

    def test_unavailable_command_is_environment_failure(self):
        with self.assertRaises(afd_common.HarnessError) as raised: afd_common.run(["definitely-not-an-afd-command"])
        self.assertEqual(afd_common.ENVIRONMENT_FAILURE, raised.exception.code)

    def test_subprocess_has_no_shell_interpolation(self):
        result = afd_common.run([sys.executable, "-c", "print('ok')"])
        self.assertEqual("ok", result.stdout.strip())

class CommandSurfaceTests(unittest.TestCase):
    def test_every_command_supports_help(self):
        for name in ("afd_check.py", "afd_spec_check.py", "afd_traceability.py", "afd_validation.py", "afd_review_gate.py", "afd_checkpoint.py", "afd_package.py"):
            result = subprocess.run([sys.executable, str(HARNESS / name), "--help"], text=True, capture_output=True, check=False)
            self.assertEqual(0, result.returncode, name)

    def test_spec_check_detects_duplicate_scenarios(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary); feature = root / "spec" / "features" / "F-0001"; feature.mkdir(parents=True)
            (feature / "one.md").write_text("## Purpose\n## Requirements\n## Scenarios\n#### Scenario: A <!-- S:F-0001-S01 -->\n- **GIVEN** x\n- **WHEN** y\n- **THEN** z\n#### Scenario: B <!-- S:F-0001-S01 -->\n- **GIVEN** x\n- **WHEN** y\n- **THEN** z\n", encoding="utf-8")
            import afd_spec_check
            with patch.object(afd_common, "ROOT", root), patch.object(afd_spec_check, "ROOT", root), patch.object(sys, "argv", ["afd_spec_check.py", "--spec-root", "spec", "--output", "report.json"]):
                self.assertEqual(afd_common.POLICY_VIOLATION, afd_spec_check.main())

if __name__ == "__main__": unittest.main()
