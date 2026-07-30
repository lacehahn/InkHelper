from __future__ import annotations
import copy,json,subprocess,sys,tempfile,unittest
from pathlib import Path
HARNESS=Path(__file__).resolve().parents[1]; ROOT=HARNESS.parents[1];sys.path.insert(0,str(HARNESS))
import afd_state_machines as sm
CANON=json.loads((ROOT/".afd/schemas/state-machines.json").read_text(encoding="utf-8"))
class StateMachinesTests(unittest.TestCase):
 def bad(self,mutate):
  d=copy.deepcopy(CANON);mutate(d);return sm.validate(d)
 def test_canonical_valid(self):self.assertEqual([],sm.validate(CANON))
 def test_missing_machines(self):self.assertTrue(self.bad(lambda d:d["machines"].pop("task")))
 def test_missing_run_machine(self):self.assertTrue(self.bad(lambda d:d["machines"].pop("run")))
 def test_duplicate_state(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["states"].append("PLANNED")))
 def test_unknown_source(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["transitions"].update({"NOPE":[]})))
 def test_unknown_target(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["transitions"]["PLANNED"].append("NOPE")))
 def test_duplicate_transition(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["transitions"]["PLANNED"].append("IMPLEMENTING")))
 def test_terminal_outgoing(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["transitions"].update({"COMPLETED":["PLANNED"]})))
 def test_initial_undefined(self):self.assertTrue(self.bad(lambda d:d["machines"]["task"]["initial_states"].append("NOPE")))
 def test_task_transitions(self):
  t=CANON["machines"]["task"]["transitions"];self.assertIn("IMPLEMENTING",t["PLANNED"]);self.assertIn("APPROVED",t["REVIEWING"]);self.assertIn("VALIDATING",t["REPAIRING"]);self.assertNotIn("COMPLETED",t["IMPLEMENTING"]);self.assertNotIn("APPROVED",t["REPAIRING"]);self.assertNotIn("IMPLEMENTING",t["COMPLETED"])
 def test_run_transitions(self):
  t=CANON["machines"]["run"]["transitions"];self.assertIn("PLANNING",t["INITIALISING"]);self.assertIn("PACKAGING",t["REVIEWING"]);self.assertIn("COMPLETED",t["PACKAGING"]);self.assertNotIn("COMPLETED",t["INITIALISING"]);self.assertNotIn("COMPLETED",t["REPAIRING"]);self.assertNotIn("EXECUTING",t["BLOCKED"])
 def cli(self,*args):return subprocess.run([sys.executable,str(HARNESS/"afd_state_machines.py"),*args],text=True,capture_output=True)
 def test_cli_success_and_failure_json(self):
  ok=self.cli("validate");self.assertEqual(0,ok.returncode);self.assertEqual("passed",json.loads(ok.stdout)["status"])
  no=self.cli("can-transition","--machine","task","--from","IMPLEMENTING","--to","COMPLETED");self.assertNotEqual(0,no.returncode);self.assertFalse(json.loads(no.stdout)["allowed"])
 def test_cli_malformed_json(self):
  with tempfile.TemporaryDirectory(dir=ROOT) as d:
   p=Path(d)/"bad.json";p.write_text("{",encoding="utf-8");r=self.cli("validate","--input",p.relative_to(ROOT).as_posix());self.assertNotEqual(0,r.returncode);json.loads(r.stdout)
 def test_cli_unknown_command(self):
  r=self.cli("unknown");self.assertNotEqual(0,r.returncode)
if __name__=="__main__":unittest.main()
