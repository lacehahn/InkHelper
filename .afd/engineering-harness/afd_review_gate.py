from __future__ import annotations
import argparse, sys
from afd_common import SUCCESS, VALIDATION_FAILURE, INVALID_INPUT, SPEC_CONFLICT, emit, fail, schema_record
def main() -> int:
 p=argparse.ArgumentParser(); p.add_argument("--review",required=True); p.add_argument("--output",required=True); p.add_argument("--task"); p.add_argument("--write-run-state",action="store_true"); a=p.parse_args()
 try:
  review,errors=schema_record(a.review,"review")
  if errors: emit({"status":"invalid","exit_code":INVALID_INPUT,"gate":"stop","diagnostics":[{"message":x} for x in errors]},a.output); return INVALID_INPUT
  if a.task:
   task,task_errors=schema_record(a.task,"task")
   if task_errors or task["task_id"]!=review["task_id"]: emit({"status":"failed","exit_code":SPEC_CONFLICT,"gate":"stop","diagnostics":[{"message":"task and review identity conflict"}]},a.output); return SPEC_CONFLICT
  code={"approved":SUCCESS,"changes_required":VALIDATION_FAILURE,"blocked":SPEC_CONFLICT}[review["verdict"]]
  emit({"status":"passed" if code==0 else "failed","exit_code":code,"gate":"continue" if code==0 else "stop","verdict":review["verdict"],"diagnostics":[]},a.output); return code
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
