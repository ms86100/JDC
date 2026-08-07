#!/usr/bin/env python3
"""
Migration Field Gap Analysis Tool
==================================
Compares source API payload columns against the platform's mapped fields
to identify unmapped, empty, and missing columns.

Usage:
    python migration-gap-analysis.py --source payload.json --session <session-id>
    python migration-gap-analysis.py --source payload.json --mapped mapped-fields.json
    python migration-gap-analysis.py --csv import.csv --session <session-id>

Environment:
    MIGRATION_API_URL  - Base URL (default: http://localhost:8085)
"""

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Any
from urllib.request import Request, urlopen
from urllib.error import URLError

DEFAULT_API = "http://localhost:8085"

STANDARD_FIELDS = {
    "issueKey", "issueId", "summary", "description", "issuetype", "priority",
    "status", "resolution", "project", "projectName", "assignee", "reporter",
    "creator", "created", "updated", "dueDate", "resolutionDate", "environment",
    "labels", "components", "fixVersions", "affectsVersions", "securityLevel",
    "parent", "epicLink", "epicName", "storyPoints", "rank", "sprint",
    "originalEstimate", "remainingEstimate", "timeSpent", "votes", "watchers",
    "linkedIssues", "subtasks", "comments", "attachments", "worklog",
}


def flatten_keys(obj: Any, prefix: str = "") -> set[str]:
    """Recursively extract all keys from a nested JSON object."""
    keys = set()
    if isinstance(obj, dict):
        for k, v in obj.items():
            full = f"{prefix}.{k}" if prefix else k
            keys.add(full)
            keys |= flatten_keys(v, full)
    elif isinstance(obj, list) and obj:
        keys |= flatten_keys(obj[0], prefix)
    return keys


def load_source_columns(path: str) -> tuple[set[str], list[dict]]:
    """Load source columns from JSON or CSV file."""
    p = Path(path)
    if p.suffix.lower() == ".csv":
        with open(p, newline="", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            rows = list(reader)
            return set(reader.fieldnames or []), rows
    else:
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, list):
            all_keys: set[str] = set()
            for item in data:
                all_keys |= flatten_keys(item)
            return all_keys, data
        else:
            return flatten_keys(data), [data]


def get_mapped_fields_from_session(session_id: str, api_url: str) -> dict:
    """Fetch field mappings from a wizard session."""
    url = f"{api_url}/api/migration/wizard/sessions/{session_id}"
    try:
        req = Request(url, headers={"Accept": "application/json"})
        with urlopen(req, timeout=10) as resp:
            return json.loads(resp.read())
    except URLError as e:
        print(f"ERROR: Cannot reach migration API at {url}: {e}", file=sys.stderr)
        sys.exit(1)


def analyze_gaps(
    source_columns: set[str],
    source_rows: list[dict],
    mapped_fields: dict[str, str],
) -> dict:
    """Analyze gaps between source columns and mapped fields."""
    mapped_sources = set(mapped_fields.keys())
    mapped_targets = set(mapped_fields.values())

    # Columns with data in source but not mapped
    unmapped_with_data = []
    for col in sorted(source_columns - mapped_sources):
        has_data = any(
            row.get(col) not in (None, "", "null", "None")
            for row in source_rows[:50]  # sample first 50 rows
        )
        unmapped_with_data.append({"column": col, "has_data": has_data})

    # Mapped columns returning null/empty
    mapped_but_empty = []
    for src, tgt in sorted(mapped_fields.items()):
        values = [row.get(src) for row in source_rows[:50]]
        non_null = [v for v in values if v not in (None, "", "null", "None")]
        if not non_null:
            mapped_but_empty.append({"source": src, "target": tgt})

    # Standard fields completely missing from source
    missing_standard = sorted(STANDARD_FIELDS - mapped_targets - {""})

    return {
        "total_source_columns": len(source_columns),
        "total_mapped": len(mapped_sources),
        "total_unmapped": len(source_columns) - len(mapped_sources),
        "unmapped_columns": unmapped_with_data,
        "unmapped_with_data_count": sum(1 for u in unmapped_with_data if u["has_data"]),
        "mapped_but_empty": mapped_but_empty,
        "missing_standard_fields": missing_standard,
    }


def print_report(result: dict) -> None:
    """Print a formatted gap analysis report."""
    print("=" * 70)
    print("MIGRATION FIELD GAP ANALYSIS REPORT")
    print("=" * 70)
    print(f"\nSource columns total:     {result['total_source_columns']}")
    print(f"Mapped columns:           {result['total_mapped']}")
    print(f"Unmapped columns:         {result['total_unmapped']}")
    print(f"Unmapped WITH data:       {result['unmapped_with_data_count']}")
    print(f"Mapped but EMPTY:         {len(result['mapped_but_empty'])}")
    print(f"Missing standard fields:  {len(result['missing_standard_fields'])}")

    if result["unmapped_with_data_count"] > 0:
        print(f"\n{'─' * 70}")
        print("UNMAPPED COLUMNS WITH DATA (action needed — these have values):")
        for item in result["unmapped_columns"]:
            if item["has_data"]:
                print(f"  - {item['column']}")

    if result["mapped_but_empty"]:
        print(f"\n{'─' * 70}")
        print("MAPPED BUT EMPTY (source has no data for these):")
        for item in result["mapped_but_empty"]:
            print(f"  - {item['source']} -> {item['target']}")

    if result["missing_standard_fields"]:
        print(f"\n{'─' * 70}")
        print("STANDARD FIELDS NOT IN MAPPING (may need manual mapping):")
        for f in result["missing_standard_fields"]:
            print(f"  - {f}")

    print(f"\n{'=' * 70}")


def main():
    parser = argparse.ArgumentParser(description="Migration field gap analysis")
    parser.add_argument("--source", help="Source payload file (JSON or CSV)")
    parser.add_argument("--csv", help="Source CSV file (alias for --source)")
    parser.add_argument("--session", help="Wizard session ID to fetch mappings from API")
    parser.add_argument("--mapped", help="Mapped fields JSON file (alternative to --session)")
    parser.add_argument("--api", default=DEFAULT_API, help=f"Migration API base URL (default: {DEFAULT_API})")
    parser.add_argument("--json", action="store_true", help="Output as JSON instead of text")
    args = parser.parse_args()

    source_file = args.source or args.csv
    if not source_file:
        parser.error("--source or --csv is required")

    source_columns, source_rows = load_source_columns(source_file)
    print(f"Loaded {len(source_columns)} columns from {len(source_rows)} rows", file=sys.stderr)

    # Get mapped fields
    mapped_fields: dict[str, str] = {}
    if args.session:
        session_data = get_mapped_fields_from_session(args.session, args.api)
        raw_mappings = session_data.get("fieldMappings") or session_data.get("options", {}).get("fieldMappings", {})
        if isinstance(raw_mappings, dict):
            mapped_fields = raw_mappings
        elif isinstance(raw_mappings, list):
            mapped_fields = {m.get("sourceKey", ""): m.get("targetKey", "") for m in raw_mappings if m.get("targetKey")}
    elif args.mapped:
        with open(args.mapped, encoding="utf-8") as f:
            mapped_fields = json.load(f)
    else:
        # No mapping info — just show unmapped analysis
        mapped_fields = {}

    result = analyze_gaps(source_columns, source_rows, mapped_fields)

    if args.json:
        print(json.dumps(result, indent=2))
    else:
        print_report(result)


if __name__ == "__main__":
    main()
