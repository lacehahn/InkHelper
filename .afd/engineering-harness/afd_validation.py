from __future__ import annotations
import argparse, shutil, sys, time
from afd_common import ROOT,SUCCESS,VALIDATION_FAILURE,INVALID_INPUT,ENVIRONMENT_FAILURE,emit,fail,schema_record,run,utc_now

APPROVED = {
 "diff_check": ["git", "diff", "--check"],
 "unit_tests": [".\\gradlew.bat", ":app:testDebugUnitTest", "--console=plain"],
 "lint_debug": [".\\gradlew.bat", ":app:lintDebug", "--console=plain"],
 "assemble_debug": [".\\gradlew.bat", ":app:assembleDebug", "--console=plain"],
}
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("--task",required=True);p.add_argument("--run-state",required=True);p.add_argument("--output",required=True);p.add_argument("--validation");p.add_argument("--write-run-state",action="store_true");p.add_argument("--timeout",type=float,default=600);a=p.parse_args()
 try:
  task,errors=schema_record(a.task,"task"); _,run_errors=schema_record(a.run_state,"run-state")
  if errors+run_errors: emit({"status":"invalid","exit_code":INVALID_INPUT,"diagnostics":[{"message":x}for x in errors+run_errors]},a.output);return INVALID_INPUT
  requested=(a.validation.split(",") if a.validation else [r["reference"] for r in task["required_validation"] if r["kind"]=="command"])
  records=[]; code=SUCCESS
  for name in requested:
   command=APPROVED.get(name)
   if not command: records.append({"command_id":name,"result_state":"invalid","diagnostic":"not an approved command"});code=INVALID_INPUT;continue
   start=utc_now(); began=time.monotonic()
   try:
    result=run(command,timeout=a.timeout); state="passed" if result.returncode==0 else "failed"; c=SUCCESS if state=="passed" else VALIDATION_FAILURE
    records.append({"command_id":name,"executable":command[0],"arguments":command[1:],"working_directory":".","start_time":start,"end_time":utc_now(),"duration_seconds":round(time.monotonic()-began,3),"exit_status":result.returncode,"result_state":state,"output":(result.stdout+result.stderr)[-4000:]})
   except Exception as e: c=ENVIRONMENT_FAILURE;records.append({"command_id":name,"result_state":"unavailable","diagnostic":str(e)})
   if code==SUCCESS:code=c
  emit({"status":"passed"if code==0 else"failed","exit_code":code,"commands":records,"diagnostics":[]},a.output);return code
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
