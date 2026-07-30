from __future__ import annotations
import argparse, sys
from afd_common import SUCCESS,VALIDATION_FAILURE,INVALID_INPUT,emit,fail,schema_record,read_json
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("--task",required=True);p.add_argument("--run-state",required=True);p.add_argument("--output",required=True);p.add_argument("--checks",default="schema");p.add_argument("--validation-evidence");p.add_argument("--review");a=p.parse_args()
 try:
  task=read_json(a.task)
  results=[];code=SUCCESS
  for name in a.checks.split(","):
   if name=="schema":
    _,one=schema_record(a.task,"task");_,two=schema_record(a.run_state,"run-state");ok=not(one+two);results.append({"check":"schema","status":"passed"if ok else"invalid","diagnostics":one+two});code=SUCCESS if ok else INVALID_INPUT
   elif name=="validation":
    evidence=read_json(a.validation_evidence) if a.validation_evidence else None
    ok=bool(evidence and evidence.get("status")=="passed" and all(x.get("result")=="passed" for x in evidence.get("commands",[])))
    results.append({"check":"validation","required":True,"status":"passed" if ok else "failed","diagnostics":[] if ok else ["missing or failed validation evidence"]});code=SUCCESS if ok and code==SUCCESS else VALIDATION_FAILURE
   elif name=="review":
    review=read_json(a.review) if a.review else None
    ok=bool(review and review.get("verdict")=="approved" and review.get("task_id")==task["task_id"])
    results.append({"check":"review","required":True,"status":"passed" if ok else "failed","diagnostics":[] if ok else ["missing, stale, or unapproved review evidence"]});code=SUCCESS if ok and code==SUCCESS else VALIDATION_FAILURE
   else:
    results.append({"check":name,"required":True,"status":"skipped","diagnostics":["required check is not coordinated"]})
    code=VALIDATION_FAILURE
  emit({"status":"passed"if code==0 else"failed","exit_code":code,"checks":results,"diagnostics":[]},a.output);return code
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
