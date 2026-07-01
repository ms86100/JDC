# -*- coding: utf-8 -*-
"""
Jira Platform — Log Monitor & Error Capture
==========================================
Real-time monitoring of all service logs.
Extracts errors, exceptions, warnings.
No manual log reading needed.
"""
import subprocess
import time
import re
import sys
import os
import threading
from pathlib import Path
from collections import deque
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
# CONFIGURATION
# ============================================================
# Only count errors within this time window (seconds)
ERROR_TIME_WINDOW_SECONDS = 300  # 5 minutes - errors older than this are ignored

# ============================================================
# ERROR PATTERNS
# ============================================================
# Patterns for detecting errors in logs
ERROR_PATTERNS = [
    # Spring/Java exceptions
    (r"Exception in thread", "java_exception"),
    (r"Caused by:", "java_exception"),
    (r"\.Exception\(", "java_exception"),
    (r"\.Error\(", "java_error"),
    (r"java\.lang\.", "java_lang_error"),
    (r"org\.springframework\.", "spring_error"),
    (r"at org\.springframework\.", "spring_stack"),
    (r"at com\.jira\.", "jira_stack"),

    # Compilation errors
    (r"\[ERROR\]", "maven_error"),
    (r"compilation failure", "compilation_error"),
    (r"cannot find symbol", "compilation_error"),
    (r"error:", "generic_error"),

    # API/HTTP errors
    (r"404\s+Not\s+Found", "http_404"),
    (r"500\s+Internal", "http_500"),
    (r"503\s+Service", "http_503"),
    (r"Connection refused", "connection_refused"),
    (r"Connection reset", "connection_reset"),

    # Database errors
    (r"SQLException", "db_error"),
    (r"PSQLException", "db_error"),
    (r"DataIntegrityViolationException", "db_constraint"),
    (r"JdbcSQL", "db_error"),
    (r"deadlock detected", "db_deadlock"),

    # Frontend errors
    (r"React", "react_error"),
    (r"useEffect", "react_hook_error"),
    (r"Uncaught TypeError", "js_error"),
    (r"TypeError:", "js_type_error"),
    (r"ReferenceError:", "js_ref_error"),
    (r"SyntaxError:", "js_syntax_error"),
    (r"Module not found", "module_error"),
    (r"Failed to compile", "webpack_error"),
    (r"Error: ", "generic_error"),
    (r"ERR_", "node_error"),
    (r"ENOENT", "fs_error"),
    (r"spawn ENOENT", "process_error"),

    # Port/Network errors
    (r"Address already in use", "port_conflict"),
    (r"Port \d+ is already being used", "port_conflict"),
    (r"bind failed", "port_bind_error"),
]

WARNING_PATTERNS = [
    (r"WARN", "warning"),
    (r"WARNING", "warning"),
    (r"Deprecation", "deprecation"),
    (r"deprecated", "deprecation"),
]


# ============================================================
# LOG MONITOR
# ============================================================
class LogMonitor:
    def __init__(self, logs_dir):
        self.logs_dir = Path(logs_dir)
        self.running = False
        self.watcher_thread = None
        self.errors = []
        self.warnings = []
        self.api_results = {}
        self.console_error_count = 0
        self.last_build_success = True
        self.last_api_results = {}
        self._file_positions = {}  # Track file positions for tail
        self._scan_start_time = None  # Track when monitoring started
        self._lock = threading.Lock()

    def start_watching(self):
        """Start monitoring logs in background thread."""
        if self.running:
            return
        self.running = True
        self._scan_start_time = time.time()  # Record when monitoring started
        # Reset file positions to only capture errors from this session
        self._file_positions = {}
        self.errors = []  # Clear previous errors
        self.warnings = []  # Clear previous warnings
        self.watcher_thread = threading.Thread(target=self._watch_loop, daemon=True)
        self.watcher_thread.start()

    def stop_watching(self):
        """Stop monitoring."""
        self.running = False
        if self.watcher_thread:
            self.watcher_thread.join(timeout=2)

    def _watch_loop(self):
        """Background monitoring loop."""
        while self.running:
            self._scan_all_logs()
            time.sleep(2)  # Scan every 2 seconds

    def _scan_all_logs(self):
        """Scan all log files for errors."""
        if not self.logs_dir.is_dir():
            return

        for log_file in self.logs_dir.glob("*.log"):
            self._scan_file(log_file)

        # Also scan frontend output
        frontend_log = self.logs_dir / "frontend.log"
        if frontend_log.exists():
            self._scan_file(frontend_log)

    def _scan_file(self, log_path):
        """Scan a single log file for errors."""
        try:
            if not log_path.is_file():
                return

            # Read from last position (or start fresh if too far back)
            pos = self._file_positions.get(str(log_path), 0)
            size = log_path.stat().st_size

            if size < pos:
                # File was rotated/truncated
                pos = 0

            with open(log_path, "r", encoding="utf-8", errors="replace") as f:
                f.seek(pos)
                new_lines = f.readlines()
                pos = f.tell()
                self._file_positions[str(log_path)] = pos

            service_name = log_path.stem

            # Filter out old/stale errors based on timestamp in log line
            now = datetime.now()
            scan_start = self._scan_start_time or time.time()

            for line in new_lines:
                # Try to extract timestamp from log line
                log_time = self._extract_timestamp_from_line(line)
                if log_time:
                    age_seconds = (now - log_time).total_seconds()
                    # Skip errors older than scan start or beyond the time window
                    if age_seconds > ERROR_TIME_WINDOW_SECONDS and log_time.timestamp() < scan_start:
                        continue  # Skip old errors

                self._process_line(line, service_name)

        except Exception:
            pass

    def _extract_timestamp_from_line(self, line):
        """Extract datetime from a log line if present."""
        # Common timestamp patterns: "2026-05-23T22:48:28" or "2026-05-23 22:48:28"
        patterns = [
            r"(\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2})",
            r"(\d{2}:\d{2}:\d{2})",  # HH:MM:SS at start of line
        ]
        for pattern in patterns:
            match = re.search(pattern, line)
            if match:
                ts_str = match.group(1)
                try:
                    if "T" in ts_str:
                        return datetime.fromisoformat(ts_str.replace(" ", "T"))
                    else:
                        # Assume today's date for HH:MM:SS format
                        today = datetime.now().strftime("%Y-%m-%d")
                        return datetime.fromisoformat(f"{today}T{ts_str}")
                except Exception:
                    pass
        return None

    def _process_line(self, line, service_name):
        """Process a single log line."""
        line_lower = line.lower()

        # Check for errors
        for pattern, error_type in ERROR_PATTERNS:
            if re.search(pattern, line, re.IGNORECASE):
                with self._lock:
                    self.errors.append({
                        "timestamp": datetime.now().isoformat(),
                        "service": service_name,
                        "type": error_type,
                        "message": line.strip(),
                        "raw": line,
                    })
                return  # Don't double-count

        # Check for warnings
        for pattern, warning_type in WARNING_PATTERNS:
            if re.search(pattern, line, re.IGNORECASE):
                with self._lock:
                    self.warnings.append({
                        "timestamp": datetime.now().isoformat(),
                        "service": service_name,
                        "type": warning_type,
                        "message": line.strip(),
                    })
                return

    def get_all_errors(self):
        """Get all captured errors."""
        with self._lock:
            return list(self.errors)

    def get_all_warnings(self):
        """Get all captured warnings."""
        with self._lock:
            return list(self.warnings)

    def get_error_summary(self):
        """Get error summary by type."""
        with self._lock:
            summary = {}
            for err in self.errors:
                t = err["type"]
                if t not in summary:
                    summary[t] = {"count": 0, "examples": []}
                summary[t]["count"] += 1
                if len(summary[t]["examples"]) < 3:
                    summary[t]["examples"].append(err["message"][:200])
            return summary

    def get_recent_errors(self, count=20):
        """Get most recent N errors."""
        with self._lock:
            return self.errors[-count:]

    def check_api_health(self, port, path="/actuator/health"):
        """Check if an API is healthy and record result."""
        import urllib.request

        result = {"ok": False, "port": port, "path": path, "error": None}
        try:
            url = f"http://127.0.0.1:{port}{path}"
            resp = urllib.request.urlopen(url, timeout=5)
            result["ok"] = 200 <= resp.status < 300
            result["status"] = resp.status
        except urllib.error.HTTPError as e:
            result["error"] = f"HTTP {e.code}"
        except Exception as e:
            result["error"] = str(e)[:50]

        with self._lock:
            self.last_api_results[result["url"]] = result

        return result["ok"]

    def scan_all_api_health(self):
        """Scan health of all configured services."""
        ports = [8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090, 8091, 8092, 8093, 8094]
        results = {}
        for port in ports:
            ok = self.check_api_health(port)
            results[port] = ok
        return results

    def check_build_result(self, maven_output):
        """Parse Maven output for success/failure."""
        if not maven_output:
            return True  # Assume success if no output
        output_lower = maven_output.lower()
        if "[error]" in output_lower or "compilation failure" in output_lower:
            self.last_build_success = False
            return False
        if "build success" in output_lower:
            self.last_build_success = True
            return True
        return True  # Assume success

    def get_health_dashboard(self):
        """Get a dashboard of all service health statuses."""
        with self._lock:
            return {
                "total_errors": len(self.errors),
                "total_warnings": len(self.warnings),
                "error_types": list(set(e["type"] for e in self.errors)),
                "services_with_errors": list(set(e["service"] for e in self.errors)),
                "last_build_success": self.last_build_success,
                "api_health": dict(self.last_api_results),
            }