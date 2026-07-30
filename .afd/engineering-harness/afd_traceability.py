from __future__ import annotations
import argparse, json, re, sys
from afd_common import ROOT,SUCCESS,VALIDATION_FAILURE,INVALID_INPUT,SPEC_CONFLICT,emit,fail,schema_record,rel_path,repo_name
def scenarios() -> set[str]:
 return {sid for f in ROOT.glob("spec/features/F-*/*.md") for sid in re.findall(r"<!--\s*S:([^ >]+)\s*-->",f.read_text(encoding="utf-8"))}
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("--task",required=True);p.add_argument("--review",required=True);p.add_argument("--evidence",required=True);p.add_argument("--output",required=True);a=p.parse_args()
 try:
  task,te=schema_record(a.task,"task"); review,re=schema_record(a.review,"review")
  if te or re: emit({"status":"invalid","exit_code":INVALID_INPUT,"diagnostics":[{"message":x}for x in te+re]},a.output);return INVALID_INPUT
  if task["task_id"]!=review["task_id"]: code=SPEC_CONFLICT; issues=["task and review identifiers disagree"]
  else:
   known=scenarios(); referenced=set(task.get("scenario_ids",[]))|set(review.get("reviewed_scenario_ids",[])); issues=sorted(referenced-known); code=VALIDATION_FAILURE if issues else SUCCESS
  evidence=[]
  for value in a.evidence.split(","):
   path=rel_path(value); evidence.append({"path":repo_name(path),"reference_exists":True,"semantic_coverage":"unverified"})
  emit({"status":"passed"if code==0 else"failed","exit_code":code,"evidence":evidence,"diagnostics":[{"message":x}for x in issues]},a.output);return code
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
