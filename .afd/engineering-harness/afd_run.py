"""Revision-guarded AFD run records using the canonical state machines."""
from __future__ import annotations
import argparse, json, os, sys, tempfile
from afd_common import SUCCESS, INVALID_INPUT, emit, fail, rel_path, read_json

def machines(): return read_json(".afd/schemas/state-machines.json")["machines"]
def atomic(path, value):
 fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".afd-")
 try:
  with os.fdopen(fd, "w", encoding="utf-8") as out: json.dump(value, out, sort_keys=True); out.flush(); os.fsync(out.fileno())
  os.replace(tmp, path)
 finally:
  if os.path.exists(tmp): os.unlink(tmp)
def main():
 p=argparse.ArgumentParser(); p.add_argument("operation",choices=["inspect","transition"]);p.add_argument("--input",required=True);p.add_argument("--output",required=True);p.add_argument("--machine",choices=["task","run"]);p.add_argument("--task");p.add_argument("--target");p.add_argument("--revision",type=int);a=p.parse_args()
 try:
  state=read_json(a.input)
  if a.operation=="inspect": emit({"status":"passed","exit_code":0,"revision":state["revision"],"run_state":state["run_status"],"task_states":state["task_states"]},a.output);return 0
  if None in (a.machine,a.target,a.revision): raise ValueError("transition requires machine, target, and revision")
  if state["revision"]!=a.revision: raise ValueError("stale state revision")
  m=machines()[a.machine]; current=state["task_states"].get(a.task,"PLANNED") if a.machine=="task" else state["run_status"]
  if a.target not in m["transitions"].get(current,[]): raise ValueError(f"illegal transition {current} -> {a.target}")
  if a.machine=="task": state["task_states"][a.task]=a.target
  else: state["run_status"]=a.target
  state["revision"]+=1;state["transition_history"].append({"machine":a.machine,"task_id":a.task,"from":current,"to":a.target,"revision":state["revision"]})
  atomic(rel_path(a.input),state);emit({"status":"passed","exit_code":0,"revision":state["revision"]},a.output);return 0
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
