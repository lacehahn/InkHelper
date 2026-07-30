from __future__ import annotations
import argparse,json,sys
from afd_common import SUCCESS,INVALID_INPUT,emit,fail,rel_path
def validate(d):
 e=[]; ms=d.get("machines",{})
 for n in ("task","run"):
  m=ms.get(n)
  if not isinstance(m,dict):e.append("missing machine: "+n);continue
  s=m.get("states",[]); t=m.get("transitions",{})
  if len(s)!=len(set(s)):e.append(n+": duplicate state")
  if not set(m.get("initial_states",[]))<=set(s):e.append(n+": undefined initial state")
  for a,b in t.items():
   if a not in s:e.append(n+": unknown source")
   if len(b)!=len(set(b)):e.append(n+": duplicate transition")
   if any(x not in s for x in b):e.append(n+": unknown target")
   if a in m.get("terminal_states",[]) and b:e.append(n+": terminal outgoing transition")
 return e
def main():
 p=argparse.ArgumentParser();p.add_argument("op",choices=["validate","can-transition"]);p.add_argument("--input",default=".afd/schemas/state-machines.json");p.add_argument("--machine");p.add_argument("--from",dest="source");p.add_argument("--to",dest="target");p.add_argument("--output");a=p.parse_args()
 try:
  with rel_path(a.input).open(encoding="utf-8") as f:d=json.load(f)
  e=validate(d)
  if e:emit({"status":"invalid","exit_code":11,"diagnostics":e},a.output);return 11
  ok=a.op=="validate" or bool(a.machine in d["machines"] and a.target in d["machines"][a.machine]["transitions"].get(a.source,[]));emit({"status":"passed"if ok else"failed","exit_code":0 if ok else 11,"allowed":ok,"diagnostics":[]},a.output);return 0 if ok else 11
 except Exception as x:return fail(x,a.output)
if __name__=="__main__":sys.exit(main())
