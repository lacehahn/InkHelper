from __future__ import annotations
import argparse, re, sys
from pathlib import Path
from afd_common import ROOT, SUCCESS, INVALID_INPUT, POLICY_VIOLATION, SPEC_CONFLICT, HarnessError, emit, fail, rel_path, repo_name

FEATURE = re.compile(r"^F-\d{4}$"); FEATURE_DIRECTORY = re.compile(r"^(F-\d{4})(?:-.+)?$"); SCENARIO = re.compile(r"^F-\d{4}-S\d{2}$")
def main() -> int:
    parser=argparse.ArgumentParser(); parser.add_argument("--spec-root", required=True); parser.add_argument("--output", required=True); parser.add_argument("--task")
    args=parser.parse_args(); findings=[]; features={}; scenarios={}
    try:
        root=rel_path(args.spec_root); feature_root=root / "features"
        if not feature_root.is_dir(): raise HarnessError("spec root has no features directory")
        for file in sorted(feature_root.glob("F-*/*.md")):
            text=file.read_text(encoding="utf-8"); rel=repo_name(file); match=FEATURE_DIRECTORY.fullmatch(file.parent.name)
            if not match: findings.append({"path":rel,"rule":"feature_id","message":"invalid feature directory identifier"}); continue
            fid=match.group(1)
            if fid in features: findings.append({"path":rel,"rule":"feature_id","message":"duplicate feature identifier"})
            features[fid]=rel
            for heading in ("## Purpose", "## Requirements", "## Scenarios"):
                if heading not in text: findings.append({"path":rel,"rule":"heading","message":f"missing {heading}"})
            found=re.findall(r"<!--\s*S:([^ >]+)\s*-->", text)
            for sid in found:
                if not SCENARIO.fullmatch(sid) or not sid.startswith(fid + "-"): findings.append({"path":rel,"rule":"scenario_id","message":f"invalid or foreign scenario {sid}"})
                elif sid in scenarios: findings.append({"path":rel,"rule":"scenario_id","message":f"duplicate scenario {sid}"})
                else: scenarios[sid]=rel
            for block in re.split(r"#### Scenario:.*?<!--\s*S:[^ >]+\s*-->", text)[1:]:
                steps=re.findall(r"^- \*\*(\w+)\*\*\s+.+$", block, re.M)
                if not all(x in {"GIVEN","WHEN","THEN","AND"} for x in steps) or not {"GIVEN","WHEN","THEN"}.issubset(steps): findings.append({"path":rel,"rule":"scenario_steps","message":"scenario requires GIVEN, WHEN, THEN and only allowed step keywords"})
        code = SUCCESS if not findings else POLICY_VIOLATION
        emit({"status":"passed" if code==0 else "failed","exit_code":code,"features":features,"scenarios":scenarios,"diagnostics":findings},args.output); return code
    except Exception as error: return fail(error,args.output)
if __name__ == "__main__": sys.exit(main())
