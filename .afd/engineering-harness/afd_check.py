from __future__ import annotations
import argparse, sys
from afd_common import SUCCESS,INVALID_INPUT,emit,fail,schema_record
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("--task",required=True);p.add_argument("--run-state",required=True);p.add_argument("--output",required=True);p.add_argument("--checks",default="schema");a=p.parse_args()
 try:
  results=[];code=SUCCESS
  for name in a.checks.split(","):
   if name=="schema":
    _,one=schema_record(a.task,"task");_,two=schema_record(a.run_state,"run-state");ok=not(one+two);results.append({"check":"schema","status":"passed"if ok else"invalid","diagnostics":one+two});code=SUCCESS if ok else INVALID_INPUT
   else: results.append({"check":name,"status":"skipped","diagnostics":["not coordinated by this aggregate interface"]})
  emit({"status":"passed"if code==0 else"failed","exit_code":code,"checks":results,"diagnostics":[]},a.output);return code
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
