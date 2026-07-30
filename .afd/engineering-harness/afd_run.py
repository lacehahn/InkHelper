"""Deterministic revisioned autonomous-execution state enforcement."""
from __future__ import annotations
import argparse, json, os, sys, tempfile
from pathlib import Path
from afd_common import ROOT, SUCCESS, INVALID_INPUT, POLICY_VIOLATION, VALIDATION_FAILURE, emit, fail, rel_path, read_json

TERMINAL={"completed","blocked","failed","deferred","cancelled"}
NEXT={"planned":{"ready"},"ready":{"implementing","deferred","cancelled"},"implementing":{"validating","repairing","blocked","failed"},"validating":{"awaiting_review","repairing","blocked","failed"},"awaiting_review":{"checkpointing","repairing","blocked"},"repairing":{"revalidating","blocked","failed"},"revalidating":{"awaiting_review","repairing","blocked"},"checkpointing":{"completed","blocked","failed"}}
DEFAULT={"implementation_attempts":3,"repair_attempts":3,"review_cycles":3,"validation_retries":2,"checkpoint_attempts":2,"packaging_attempts":2,"root_cause_retries":2,"no_progress_cycles":2,"total_task_attempts":100,"total_review_operations":100,"run_duration_seconds":28800}
def plan_errors(plan):
 ids=[x.get("task_id") for x in plan.get("tasks",[])]; errors=[]
 if len(ids)!=len(set(ids)): errors.append("duplicate task identifier")
 graph={x.get("task_id"):x.get("dependencies",[]) for x in plan.get("tasks",[])}
 def visit(node,seen):
  if node in seen:return True
  return any(visit(n,seen|{node}) for n in graph.get(node,[]))
 if any(visit(n,set()) for n in graph):errors.append("cyclic dependency")
 return errors
def atomic(path,data):
 fd,tmp=tempfile.mkstemp(dir=path.parent,prefix=".afd-");
 try:
  with os.fdopen(fd,"w",encoding="utf-8") as f:json.dump(data,f,sort_keys=True);f.flush();os.fsync(f.fileno())
  os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
def main():
 p=argparse.ArgumentParser(); sub=p.add_subparsers(dest="op",required=True)
 for name in ("validate-plan","inspect","transition","check-budget"):
  q=sub.add_parser(name);q.add_argument("--input",required=True);q.add_argument("--output",required=True)
  if name=="transition":q.add_argument("--task",required=True);q.add_argument("--target",required=True);q.add_argument("--revision",type=int,required=True);q.add_argument("--evidence",required=True)
 a=p.parse_args()
 try:
  data=read_json(a.input)
  if a.op=="validate-plan":
   e=plan_errors(data);code=SUCCESS if not e else INVALID_INPUT;emit({"status":"passed"if not e else"invalid","exit_code":code,"diagnostics":e},a.output);return code
  if a.op=="inspect":emit({"status":"passed","exit_code":0,"revision":data.get("revision"),"run_status":data.get("run_status")},a.output);return 0
  if a.op=="check-budget":
   bad=[k for k,v in data.get("budget_consumption",{}).items() if v>=data.get("budget_configuration",DEFAULT).get(k,0)]
   code=POLICY_VIOLATION if bad else SUCCESS;emit({"status":"failed"if bad else"passed","exit_code":code,"exhausted":bad},a.output);return code
  if data.get("revision")!=a.revision:raise Exception("stale state revision")
  current=data.get("task_states",{}).get(a.task,"planned")
  if a.target not in NEXT.get(current,set()):raise Exception(f"illegal transition {current} -> {a.target}")
  if not read_json(a.evidence):raise Exception("missing transition evidence")
  data["task_states"][a.task]=a.target;data["revision"]+=1;data.setdefault("transition_history",[]).append({"task_id":a.task,"from":current,"to":a.target,"evidence":a.evidence})
  atomic(rel_path(a.input),data);emit({"status":"passed","exit_code":0,"revision":data["revision"]},a.output);return 0
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
