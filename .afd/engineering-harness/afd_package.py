from __future__ import annotations
import argparse, sys
from afd_common import SUCCESS,VALIDATION_FAILURE,INVALID_INPUT,digest,emit,fail,rel_path,repo_name,schema_record
def main()->int:
 p=argparse.ArgumentParser();p.add_argument("--task",required=True);p.add_argument("--artifact",required=True);p.add_argument("--evidence",required=True);p.add_argument("--output",required=True);a=p.parse_args()
 try:
  _,errors=schema_record(a.task,"task")
  artifact=rel_path(a.artifact)
  if not artifact.is_file() or "build" not in artifact.parts: raise ValueError("artifact must be a file in a permitted build output location")
  evidence=[repo_name(rel_path(x)) for x in a.evidence.split(",")]
  if errors: emit({"status":"invalid","exit_code":INVALID_INPUT,"diagnostics":[{"message":x}for x in errors]},a.output);return INVALID_INPUT
  kind="release" if "release" in artifact.parts else "debug" if "debug" in artifact.parts else "unknown"
  emit({"status":"passed","exit_code":SUCCESS,"artifact":{"type":kind,"path":repo_name(artifact),"size":artifact.stat().st_size,"sha256":digest(artifact)},"evidence":evidence,"device_validation":"unverified","diagnostics":[]},a.output);return SUCCESS
 except ValueError as e: emit({"status":"failed","exit_code":VALIDATION_FAILURE,"diagnostics":[{"message":str(e)}]},a.output);return VALIDATION_FAILURE
 except Exception as e:return fail(e,a.output)
if __name__=="__main__":sys.exit(main())
