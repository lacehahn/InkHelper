from __future__ import annotations
import argparse, sys
from afd_common import SUCCESS,POLICY_VIOLATION,ENVIRONMENT_FAILURE,emit,fail,git,schema_record
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("operation",choices=["inspect","create","restore"]);p.add_argument("--run-state",required=True);p.add_argument("--output",required=True);p.add_argument("--checkpoint");p.add_argument("--write-run-state",action="store_true");p.add_argument("--permit-checkpoint",action="store_true");a=p.parse_args()
 try:
  _,errors=schema_record(a.run_state,"run-state")
  if errors: raise ValueError("invalid run state")
  status=git(["status","--porcelain=v1"])
  dirty=bool(status.stdout.strip())
  if a.operation=="inspect":
   head=git(["rev-parse","HEAD"])
   if head.returncode:return ENVIRONMENT_FAILURE
   emit({"status":"passed","exit_code":SUCCESS,"dirty":dirty,"checkpoint":head.stdout.strip(),"diagnostics":[]},a.output);return SUCCESS
  if not a.permit_checkpoint or not a.write_run_state: emit({"status":"failed","exit_code":POLICY_VIOLATION,"diagnostics":[{"message":"checkpoint mutation requires explicit permission and state write authority"}]},a.output);return POLICY_VIOLATION
  if dirty: emit({"status":"failed","exit_code":POLICY_VIOLATION,"diagnostics":[{"message":"dirty worktree prevents checkpoint mutation"}]},a.output);return POLICY_VIOLATION
  if a.operation=="restore": emit({"status":"failed","exit_code":POLICY_VIOLATION,"diagnostics":[{"message":"restore is not implemented: policy defines no non-destructive restore command"}]},a.output);return POLICY_VIOLATION
  result=git(["commit","--allow-empty","-m","AFD checkpoint"])
  if result.returncode: emit({"status":"failed","exit_code":ENVIRONMENT_FAILURE,"diagnostics":[{"message":result.stderr[-1000:]}]},a.output);return ENVIRONMENT_FAILURE
  head=git(["rev-parse","HEAD"]);emit({"status":"passed","exit_code":SUCCESS,"dirty":False,"checkpoint":head.stdout.strip(),"diagnostics":[]},a.output);return SUCCESS
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
