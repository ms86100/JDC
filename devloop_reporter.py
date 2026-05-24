# -*- coding: utf-8 -*-
"""
Jira Platform — Error Reporter & AI Fix Generator
================================================
Aggregates all errors, categorizes, generates fix prompts.
No manual copy-pasting needed.
"""
import subprocess
import time
import re
import sys
import os
from pathlib import Path
from collections import defaultdict
from datetime import datetime

# Force UTF-8 on Windows
if sys.platform == "win32":
    import io
    _already_wrapped = getattr(sys.stdout, '_wrapped', False) or isinstance(sys.stdout, io.TextIOWrapper)
    try:
        if not _already_wrapped and hasattr(sys.stdout, 'buffer'):
            sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
            sys.stdout._wrapped = True
        _already_wrapped = getattr(sys.stderr, '_wrapped', False) or isinstance(sys.stderr, io.TextIOWrapper)
        if not _already_wrapped and hasattr(sys.stderr, 'buffer'):
            sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
            sys.stderr._wrapped = True
    except Exception:
        pass

# ============================================================
# ERROR TIME FILTER
# ============================================================
# Only count errors from the last N seconds
ERROR_LOOKBACK_SECONDS = 300  # 5 minutes

# ============================================================
# ERROR CATEGORIZER
# ============================================================
CATEGORIES = {
    "java_exception": "Java/Spring Exceptions",
    "spring_error": "Spring Framework Errors",
    "compilation_error": "Maven Compilation Errors",
    "maven_error": "Maven Build Errors",
    "http_404": "API 404 Not Found",
    "http_500": "API 500 Internal Server Error",
    "http_503": "API 503 Service Unavailable",
    "connection_refused": "Connection Refused",
    "db_error": "Database Errors",
    "db_constraint": "Database Constraint Violations",
    "db_deadlock": "Database Deadlocks",
    "js_error": "JavaScript Errors",
    "react_error": "React/Frontend Errors",
    "webpack_error": "Webpack/Build Errors",
    "port_conflict": "Port Conflicts",
    "module_error": "Module Not Found",
    "generic_error": "Generic Errors",
}

PRIORITY_ORDER = [
    "compilation_error",
    "maven_error",
    "java_exception",
    "spring_error",
    "db_error",
    "db_deadlock",
    "db_constraint",
    "http_500",
    "http_503",
    "connection_refused",
    "port_conflict",
    "react_error",
    "webpack_error",
    "js_error",
    "module_error",
    "http_404",
    "generic_error",
]

# ============================================================
# ERROR REPORTER
# ============================================================
class ErrorReporter:
    def __init__(self, errors=None, logs_dir=None):
        self.errors = errors or []
        self.logs_dir = Path(logs_dir) if logs_dir else Path("logs")
        self.categorized = defaultdict(list)
        self.root_causes = []
        # Filter to only recent errors
        self.errors = self._filter_recent_errors(self.errors)

    def _filter_recent_errors(self, errors):
        """Filter to only errors within the lookback window."""
        import time
        now = time.time()
        cutoff = now - ERROR_LOOKBACK_SECONDS
        filtered = []
        for err in errors:
            ts_str = err.get("timestamp", "")
            if ts_str:
                try:
                    from datetime import datetime
                    ts = datetime.fromisoformat(ts_str)
                    if ts.timestamp() < cutoff:
                        continue  # Skip old errors
                except Exception:
                    pass
            filtered.append(err)
        return filtered

    def categorize_errors(self):
        """Group errors by type."""
        self.categorized = defaultdict(list)
        for err in self.errors:
            err_type = err.get("type", "generic_error")
            self.categorized[err_type].append(err)

        # Sort by priority
        sorted_categories = sorted(
            self.categorized.keys(),
            key=lambda x: PRIORITY_ORDER.index(x) if x in PRIORITY_ORDER else 999
        )
        return sorted_categories

    def find_root_causes(self):
        """Identify root causes from exception chains."""
        root_causes = []

        # Group by service
        by_service = defaultdict(list)
        for err in self.errors:
            by_service[err.get("service", "unknown")].append(err)

        for service, service_errors in by_service.items():
            # Find the actual root cause (usually in "Caused by" lines)
            cause_lines = []
            for err in service_errors:
                msg = err.get("message", "")
                if "Caused by:" in msg:
                    # Extract cause
                    for line in msg.split("\n"):
                        if line.strip().startswith("Caused by:"):
                            cause_lines.append(line.strip())
                elif err.get("type") == "compilation_error":
                    # Extract compilation error
                    cause_lines.append(msg[:200])

            if cause_lines:
                root_causes.append({
                    "service": service,
                    "causes": cause_lines[:5],  # Top 5 causes
                })

        return root_causes

    def generate_summary(self):
        """Generate a concise error summary."""
        if not self.errors:
            return "No errors detected."

        self.categorize_errors()

        lines = []
        total = len(self.errors)
        lines.append(f"Total Errors: {total}")

        sorted_categories = sorted(
            self.categorized.keys(),
            key=lambda x: PRIORITY_ORDER.index(x) if x in PRIORITY_ORDER else 999
        )

        for cat in sorted_categories:
            errors_in_cat = self.categorized[cat]
            cat_name = CATEGORIES.get(cat, cat)
            lines.append(f"  [{len(errors_in_cat)}] {cat_name}")

        return "\n".join(lines)

    def generate_detailed_report(self):
        """Generate detailed error report."""
        if not self.errors:
            return "No errors to report."

        self.categorize_errors()

        lines = []
        lines.append("=" * 70)
        lines.append("DETAILED ERROR REPORT")
        lines.append("=" * 70)

        sorted_categories = sorted(
            self.categorized.keys(),
            key=lambda x: PRIORITY_ORDER.index(x) if x in PRIORITY_ORDER else 999
        )

        for cat in sorted_categories:
            errors_in_cat = self.categorized[cat]
            cat_name = CATEGORIES.get(cat, cat)

            lines.append("")
            lines.append(f"## {cat_name} ({len(errors_in_cat)} errors)")
            lines.append("-" * 50)

            # Group by service within category
            by_service = defaultdict(list)
            for err in errors_in_cat:
                by_service[err.get("service", "unknown")].append(err)

            for service, service_errors in by_service.items():
                lines.append(f"\n  Service: {service}")
                for err in service_errors[:3]:  # Max 3 per service
                    msg = err.get("message", "")[:300]
                    lines.append(f"    → {msg}")
                    if len(err.get("message", "")) > 300:
                        lines.append(f"      ... (truncated)")

        return "\n".join(lines)

    def generate_fix_prompt(self, feature_path=None, verbose=False):
        """Generate an AI prompt for fixing errors."""
        if not self.errors:
            return None

        self.categorize_errors()
        self.root_causes = self.find_root_causes()

        lines = []
        lines.append("=" * 70)
        lines.append("AUTONOMOUS DEVLOOP — FIX REQUEST")
        lines.append("=" * 70)
        lines.append(f"Generated: {datetime.now().isoformat()}")
        lines.append("")

        # Summary
        lines.append("## ERROR SUMMARY")
        lines.append(self.generate_summary())
        lines.append("")

        # Root causes
        if self.root_causes:
            lines.append("## ROOT CAUSES (by service)")
            for rc in self.root_causes:
                lines.append(f"\n### {rc['service']}")
                for cause in rc['causes']:
                    lines.append(f"  {cause}")
            lines.append("")

        # Detailed errors
        lines.append("## DETAILED ERRORS")
        lines.append(self.generate_detailed_report())
        lines.append("")

        # Feature context
        if feature_path and Path(feature_path).exists():
            lines.append("## FEATURE REQUIREMENTS")
            lines.append(f"Source: {feature_path}")
            try:
                content = Path(feature_path).read_text(encoding="utf-8", errors="replace")
                lines.append(content[:1000])  # First 1000 chars
                if len(content) > 1000:
                    lines.append("\n... (truncated)")
            except Exception:
                pass
            lines.append("")

        # Instructions
        lines.append("## FIX INSTRUCTIONS")
        lines.append("""
1. Read the error details above
2. Read the feature requirements
3. Analyze the root causes
4. Make minimum necessary fixes
5. DO NOT refactor unrelated code
6. After fixing, output:
{
  "fixed": ["list of fixes"],
  "files_modified": ["list of files"]
}
""")

        return {
            "summary": self.generate_summary(),
            "detailed": "\n".join(lines),
            "root_causes": self.root_causes,
            "total_errors": len(self.errors),
            "categories": dict(self.categorized),
        }

    def generate_markdown_report(self, output_path=None):
        """Generate a full markdown report file."""
        report = {
            "summary": self.generate_summary(),
            "detailed": self.generate_detailed_report(),
            "root_causes": self.find_root_causes(),
        }

        lines = []
        lines.append("# DevLoop Error Report")
        lines.append(f"\nGenerated: {datetime.now().isoformat()}")
        lines.append(f"\nTotal Errors: {len(self.errors)}")
        lines.append("")
        lines.append("## Summary")
        lines.append(report["summary"])
        lines.append("")
        lines.append("## Root Causes")
        for rc in report["root_causes"]:
            lines.append(f"\n### {rc['service']}")
            for cause in rc['causes']:
                lines.append(f"- {cause}")
        lines.append("")
        lines.append("## Detailed Errors")
        lines.append(report["detailed"])

        content = "\n".join(lines)

        if output_path:
            Path(output_path).write_text(content, encoding="utf-8")

        return content

    def get_files_to_fix(self):
        """Extract file paths from error messages."""
        files = set()
        file_pattern = re.compile(r"([a-zA-Z]:\\[^<>:\"/\\|?*]+\.(java|yml|yaml|properties|xml|ts|tsx|js|jsx))")

        for err in self.errors:
            msg = err.get("message", "")
            matches = file_pattern.findall(msg)
            for match in matches:
                files.add(match[0])

        return list(files)

    def get_high_priority_errors(self):
        """Get errors in priority order."""
        priority_errors = []
        for cat in PRIORITY_ORDER:
            if cat in self.categorized:
                priority_errors.extend(self.categorized[cat])
        # Add any unclassified
        for err in self.errors:
            if err.get("type") not in PRIORITY_ORDER:
                priority_errors.append(err)
        return priority_errors