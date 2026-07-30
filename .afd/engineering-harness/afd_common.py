"""Shared, standard-library support for deterministic AFD commands."""
from __future__ import annotations

import hashlib
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

SUCCESS = 0
VALIDATION_FAILURE, INVALID_INPUT, POLICY_VIOLATION = 10, 11, 12
SPEC_CONFLICT, ENVIRONMENT_FAILURE, INTERNAL_ERROR = 13, 14, 15
REPORT_VERSION = "1.0"
ROOT = Path(__file__).resolve().parents[2]

class HarnessError(Exception):
    def __init__(self, message: str, code: int = INVALID_INPUT):
        super().__init__(message); self.code = code

def rel_path(value: str, *, must_exist: bool = True) -> Path:
    path = Path(value)
    if path.is_absolute() or ".." in path.parts:
        raise HarnessError("path must be repository-relative and must not escape the repository")
    resolved = (ROOT / path).resolve()
    try: resolved.relative_to(ROOT.resolve())
    except ValueError: raise HarnessError("path escapes repository")
    if must_exist and not resolved.exists(): raise HarnessError(f"file not found: {value}")
    return resolved

def repo_name(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()

def read_json(value: str) -> Any:
    try:
        with rel_path(value).open(encoding="utf-8") as handle: return json.load(handle)
    except json.JSONDecodeError as error: raise HarnessError(f"invalid JSON in {value}: {error}")

def write_report(value: str, report: dict[str, Any]) -> None:
    path = rel_path(value, must_exist=False)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle: json.dump(report, handle, indent=2, sort_keys=True); handle.write("\n")

def emit(report: dict[str, Any], output: str | None = None) -> None:
    report.setdefault("report_version", REPORT_VERSION)
    if output: write_report(output, report)
    print(json.dumps(report, sort_keys=True, separators=(",", ":")))

def fail(error: Exception, output: str | None = None) -> int:
    code = error.code if isinstance(error, HarnessError) else INTERNAL_ERROR
    message = str(error)
    print(message, file=sys.stderr)
    emit({"status": "invalid" if code == INVALID_INPUT else "failed", "exit_code": code,
          "diagnostics": [{"message": message}]}, output)
    return code

def utc_now() -> str: return datetime.now(timezone.utc).isoformat()

def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(65536), b""): h.update(block)
    return h.hexdigest()

def run(args: list[str], *, cwd: Path = ROOT, timeout: float | None = None) -> subprocess.CompletedProcess[str]:
    try:
        return subprocess.run(args, cwd=cwd, text=True, capture_output=True, timeout=timeout, shell=False, check=False)
    except FileNotFoundError as error: raise HarnessError(f"unavailable executable: {args[0]}", ENVIRONMENT_FAILURE) from error
    except subprocess.TimeoutExpired as error: raise HarnessError(f"command timed out: {args[0]}", ENVIRONMENT_FAILURE) from error

def git(args: list[str]) -> subprocess.CompletedProcess[str]: return run(["git", *args])

def _resolve(schema: dict[str, Any], root: dict[str, Any]) -> dict[str, Any]:
    while "$ref" in schema:
        ref = schema["$ref"]
        if not ref.startswith("#/"): raise HarnessError("external schema references are unsupported")
        node: Any = root
        for item in ref[2:].split("/"): node = node[item]
        schema = node
    return schema

def validate(instance: Any, schema: dict[str, Any], root: dict[str, Any] | None = None, path: str = "$") -> list[str]:
    """Validate exactly the Draft 2020-12 constructs used by this repository's schemas."""
    root = root or schema; schema = _resolve(schema, root); errors: list[str] = []
    if "const" in schema and instance != schema["const"]: errors.append(f"{path}: must equal {schema['const']!r}")
    if "enum" in schema and instance not in schema["enum"]: errors.append(f"{path}: invalid value")
    if "anyOf" in schema and not any(not validate(instance, item, root, path) for item in schema["anyOf"]): errors.append(f"{path}: no permitted form")
    kind = schema.get("type")
    types = {"object": dict, "array": list, "string": str, "integer": int, "boolean": bool, "null": type(None)}
    if kind and (not isinstance(instance, types[kind]) or (kind == "integer" and isinstance(instance, bool))): return errors + [f"{path}: expected {kind}"]
    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]: errors.append(f"{path}: too short")
        if "pattern" in schema and not re.search(schema["pattern"], instance): errors.append(f"{path}: invalid format")
    if isinstance(instance, int) and not isinstance(instance, bool):
        if "minimum" in schema and instance < schema["minimum"]: errors.append(f"{path}: below minimum")
        if "maximum" in schema and instance > schema["maximum"]: errors.append(f"{path}: above maximum")
    if isinstance(instance, list):
        if schema.get("uniqueItems") and len({json.dumps(x, sort_keys=True) for x in instance}) != len(instance): errors.append(f"{path}: duplicate item")
        for i, item in enumerate(instance): errors += validate(item, schema.get("items", {}), root, f"{path}[{i}]")
        if "contains" in schema and not any(not validate(x, schema["contains"], root, path) for x in instance): errors.append(f"{path}: required item missing")
    if isinstance(instance, dict):
        for key in schema.get("required", []):
            if key not in instance: errors.append(f"{path}: missing {key}")
        props = schema.get("properties", {})
        if schema.get("additionalProperties") is False:
            for key in instance:
                if key not in props and not any(re.search(pattern, key) for pattern in schema.get("patternProperties", {})): errors.append(f"{path}: unexpected {key}")
        for key, value in instance.items():
            if key in props: errors += validate(value, props[key], root, f"{path}.{key}")
            for pattern, child in schema.get("patternProperties", {}).items():
                if re.search(pattern, key): errors += validate(value, child, root, f"{path}.{key}")
    if "not" in schema and not validate(instance, schema["not"], root, path): errors.append(f"{path}: forbidden value")
    if "if" in schema and not validate(instance, schema["if"], root, path): errors += validate(instance, schema.get("then", {}), root, path)
    return errors

def schema_record(value: str, name: str) -> tuple[dict[str, Any], list[str]]:
    record = read_json(value); schema = read_json(f".afd/schemas/{name}.schema.json")
    return record, validate(record, schema)
