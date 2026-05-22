# -*- coding: utf-8 -*-
"""
Jira Platform - Enterprise Launcher
====================================
Single-command startup for all backend services and frontend.

Usage:
  python launcher.py                    Start everything
  python launcher.py --config path.yaml  Use custom config
  python launcher.py --no-browser        Skip browser auto-open
  python launcher.py --build-only        Build JARs and exit
  python launcher.py --only project,gateway,auth   Start subset of services (deps auto-included)
  python launcher.py --no-build          Skip Maven builds (use existing JARs)
  python launcher.py --rebuild             Force full rebuild (mvn clean package)
  python launcher.py --status            Show running services and exit

Reads config.yaml by default. All service ports and credentials come from there.
Press Ctrl+C to gracefully stop all services.
"""
import subprocess
import time
import sys
import os
import re
import webbrowser
import signal
import json
import shutil
import urllib.request
import urllib.error
from pathlib import Path
from collections import deque

# Force UTF-8 on Windows
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

# ============================================================
# CONFIG
# ============================================================
BASE_DIR = Path(__file__).parent
DEFAULT_CONFIG = BASE_DIR / "config.yaml"
FRONTEND_DIR = BASE_DIR / "jira-frontend"
LOGS_DIR = BASE_DIR / "logs"

# ANSI colors (Windows-safe)
C_RESET = C_END = C_BOLD = ""
C_GREEN = C_RED = C_YELLOW = C_CYAN = C_BLUE = C_GRAY = C_WHITE = ""
C_DIM = ""

if sys.platform != "win32" or os.environ.get("ANSICON") or os.environ.get("WT_SESSION"):
    C_RESET = "\033[0m"
    C_END = "\033[0m"
    C_BOLD = "\033[1m"
    C_GREEN = "\033[92m"
    C_RED = "\033[91m"
    C_YELLOW = "\033[93m"
    C_CYAN = "\033[96m"
    C_BLUE = "\033[94m"
    C_GRAY = "\033[90m"
    C_WHITE = "\033[97m"
    C_DIM = "\033[2m"

# ============================================================
# LOGGING
# ============================================================
def log(msg="", color="white", bold=False, dim=False):
    prefix = ""
    if bold: prefix += C_BOLD
    if dim: prefix += C_DIM
    clr = {"green": C_GREEN, "red": C_RED, "yellow": C_YELLOW,
           "cyan": C_CYAN, "blue": C_BLUE, "gray": C_GRAY, "white": C_WHITE}.get(color, C_WHITE)
    try:
        print(f"{prefix}{clr}{msg}{C_RESET}", flush=True)
    except (UnicodeEncodeError, OSError):
        print(msg.encode("ascii", "ignore").decode("ascii"), flush=True)

def separator(char="=", width=70, color="cyan"):
    log(char * width, color=color)

# ============================================================
# CONFIG LOADER
# Config.yaml is declarative only — Python defaults are the
# authoritative source of truth. Override individual values
# via environment variables or by editing config.yaml.
# ============================================================

def _deep_merge(base, overrides):
    """Merge overrides into base dict (shallow)."""
    result = dict(base)
    for k, v in overrides.items():
        if k in result and isinstance(result[k], dict) and isinstance(v, dict):
            result[k] = _deep_merge(result[k], v)
        else:
            result[k] = v
    return result

def _parse_yaml_simple(text):
    """Parse YAML into nested dicts/lists. Simple robust implementation."""
    lines = text.splitlines()
    lines = [l.rstrip() for l in lines]

    # Build a tree of nodes: {indent: (type, key, value_or_children)}
    result = {}
    stack = []  # list of (indent, dict_ref)

    for raw in lines:
        stripped = raw.strip()
        if not stripped or stripped.startswith("#"):
            continue

        indent = len(raw) - len(raw.lstrip())
        content = stripped

        # Pop stack until we find a parent at lower indent
        while stack and stack[-1][0] >= indent:
            stack.pop()

        if content.startswith("- "):
            # List item
            value = content[2:].strip()
            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            elif value.startswith("'") and value.endswith("'"):
                value = value[1:-1]
            elif value.isdigit():
                value = int(value)
            if stack:
                parent = stack[-1][1]
                if isinstance(parent, list):
                    parent.append(value)
                elif isinstance(parent, dict):
                    # Inline list item with dict value
                    pass
            continue

        if ":" in content:
            idx = content.index(":")
            key = content[:idx].strip()
            rest = content[idx + 1:].strip()

            val = rest
            if (val.startswith('"') and val.endswith('"')) or (val.startswith("'") and val.endswith("'")):
                val = val[1:-1]
            elif val.lower() in ("true",):
                val = True
            elif val.lower() in ("false",):
                val = False
            elif val.isdigit():
                val = int(val)

            if rest == "":
                # Block (nested dict)
                new_dict = {}
                if stack:
                    parent = stack[-1][1]
                    if isinstance(parent, list):
                        parent.append(new_dict)
                    else:
                        parent[key] = new_dict
                else:
                    result[key] = new_dict
                stack.append((indent, new_dict))
            else:
                # Scalar value
                if stack:
                    parent = stack[-1][1]
                    if isinstance(parent, list):
                        if parent and isinstance(parent[-1], dict):
                            parent[-1][key] = val
                        else:
                            parent.append({key: val})
                    else:
                        parent[key] = val
                else:
                    result[key] = val

    return result

def load_config(config_path=None):
    """Load config.yaml, merge with Python defaults.

    When config.yaml defines ``services:``, that block is authoritative — extra
    entries from Python defaults (e.g. duplicate ports) are not merged in.
    """
    defaults = _get_defaults()
    cfg_file = config_path or DEFAULT_CONFIG
    if cfg_file and Path(cfg_file).exists():
        try:
            file_cfg = _parse_yaml_simple(Path(cfg_file).read_text(encoding="utf-8"))
            if file_cfg:
                defaults = _deep_merge(defaults, file_cfg)
                if file_cfg.get("services"):
                    defaults["services"] = dict(file_cfg["services"])
        except Exception as e:
            log(f"  Warning: could not parse config.yaml ({e}) — using defaults", color="yellow")

    # Override with environment variables
    for key in ["JIRA_DB_PASSWORD", "JIRA_DB_HOST", "JIRA_DB_PORT",
                "JIRA_DB_USERNAME", "JIRA_JWT_SECRET", "JIRA_FRONTEND_PORT"]:
        env_key = key.replace("JIRA_", "")
        env_key_camel = env_key[0].lower() + env_key[1:]
        if key == "JIRA_DB_PASSWORD":
            defaults["database"]["password"] = os.environ.get(key, defaults["database"]["password"])
        elif key == "JIRA_DB_HOST":
            defaults["database"]["host"] = os.environ.get(key, defaults["database"]["host"])
        elif key == "JIRA_DB_PORT":
            defaults["database"]["port"] = int(os.environ.get(key, defaults["database"]["port"]))
        elif key == "JIRA_DB_USERNAME":
            defaults["database"]["username"] = os.environ.get(key, defaults["database"]["username"])
        elif key == "JIRA_JWT_SECRET":
            defaults["security"]["jwt_secret"] = os.environ.get(key, defaults["security"]["jwt_secret"])
        elif key == "JIRA_FRONTEND_PORT":
            defaults["frontend"]["port"] = int(os.environ.get(key, defaults["frontend"]["port"]))

    return defaults

def _get_defaults():
    """Default service configuration — source of truth."""
    return {
        "database": {
            "host": "localhost",
            "port": 5432,
            "username": "jiraadmin",
            "password": "jirapass123",
            "databases": [
                "auth_db", "user_db", "project_db", "issue_db",
                "workflow_db", "comment_db", "notification_db",
                "search_db", "audit_db", "attachment_db",
                "sprint_db", "plan_db", "migration_db",
            ],
        },
        "security": {
            "jwt_secret": "jira-platform-super-secret-key-that-is-at-least-256-bits-long",
        },
        "services": {
            "postgres": {
                "docker": True,
                "image": "postgres:16-alpine",
                "container_name": "jira-postgres",
                "port": 5432,
            },
            "auth":       {"port": 8081, "dir": "jira-auth-service",         "jar": "jira-auth-service-1.0.0.jar",          "deps": []},
            "gateway":    {"port": 8080, "dir": "jira-gateway",            "jar": "jira-gateway-1.0.0.jar",               "deps": ["auth"]},
            "user":       {"port": 8082, "dir": "jira-user-service",       "jar": "jira-user-service-1.0.0.jar",          "deps": ["auth"]},
            "project":    {"port": 8083, "dir": "jira-project-service",     "jar": "jira-project-service-1.0.0.jar",      "deps": ["auth"]},
            "issue":      {"port": 8084, "dir": "jira-issue-service",       "jar": "jira-issue-service-1.0.0.jar",         "deps": ["auth"]},
            "workflow":   {"port": 8085, "dir": "jira-workflow-service",    "jar": "jira-workflow-service-1.0.0.jar",     "deps": ["auth"]},
            "comment":    {"port": 8086, "dir": "jira-comment-service",    "jar": "jira-comment-service-1.0.0.jar",      "deps": ["auth"]},
            "notification": {"port": 8087, "dir": "jira-notification-service", "jar": "jira-notification-service-1.0.0.jar", "deps": ["auth"]},
            "search":     {"port": 8088, "dir": "jira-search-service",      "jar": "jira-search-service-1.0.0.jar",       "deps": ["auth"]},
            "audit":      {"port": 8089, "dir": "jira-audit-service",      "jar": "jira-audit-service-1.0.0.jar",       "deps": ["auth"]},
            "attachment": {"port": 8090, "dir": "jira-attachment-service",  "jar": "jira-attachment-service-1.0.0.jar",  "deps": ["auth"]},
            "sprint":     {"port": 8091, "dir": "jira-sprint-service",      "jar": "jira-sprint-service-1.0.0.jar",      "deps": ["auth"]},
            "plan":       {"port": 8092, "dir": "jira-plan-service",        "jar": "jira-plan-service-1.0.0.jar",        "deps": ["auth"]},
            "admin":      {"port": 8093, "dir": "jira-admin-service",       "jar": "jira-admin-service-1.0.0.jar",        "deps": ["auth"]},
            "migration":  {"port": 8094, "dir": "jira-migration-service",  "jar": "jira-migration-service-1.0.0.jar",   "deps": ["auth"]},
            "test":       {"port": 8095, "dir": "jira-test-service",       "jar": "jira-test-service-1.0.0.jar",        "deps": ["auth"]},
        },
        "frontend": {"port": 3000, "dir": "jira-frontend", "open_browser": True},
        "startup": {
            "build_if_missing": True,
            "health_check_path": "/actuator/health",
            "health_timeout": 180,
            "health_poll_interval": 2,
            "max_parallel": 3,
            "protected_ports": [5432],
            "shutdown_timeout": 15,
            "log_dir": "logs",
            "cleanup_on_start": True,
            "max_restart_attempts": 3,
            "restart_cooldown_seconds": 30,
            "java_opts": "-Xms128m -Xmx384m -XX:+UseG1GC",
            "maven_flags": "-Dmaven.test.skip=true -q",
            "maven_parallel": True,
            "max_parallel_builds": 4,
        },
    }

# ============================================================
# UTILITIES
# ============================================================
PROTECTED_PORTS_DEFAULT = {5432}


def resolve_executable(name):
    """Resolve CLI on Windows (npm.cmd, mvn.cmd) and Unix."""
    if os.path.isabs(name) and os.path.isfile(name):
        return name
    found = shutil.which(name)
    if found:
        return found
    if sys.platform == "win32":
        for suffix in (".cmd", ".exe", ".bat"):
            found = shutil.which(name + suffix)
            if found:
                return found
    return name


def check_port(port):
    """Return True if port is open on localhost."""
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        r = s.connect_ex(("localhost", port))
        return r == 0
    except Exception:
        return False
    finally:
        s.close()

def check_health(port, path="/actuator/health", timeout=5, paths=None):
    """Return True if service responds with 2xx on a health endpoint."""
    candidates = paths if paths else [path]
    if path and path not in candidates:
        candidates = [path] + list(candidates)
    for candidate in candidates:
        try:
            url = f"http://localhost:{port}{candidate}"
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "JiraPlatformLauncher/1.0")
            resp = urllib.request.urlopen(req, timeout=timeout)
            if 200 <= resp.status < 300:
                return True
        except Exception:
            continue
    return False


def get_pid_on_port(port):
    """Return PID of process listening on TCP port, or None."""
    try:
        port = int(port)
    except (TypeError, ValueError):
        return None
    if sys.platform == "win32":
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"(Get-NetTCPConnection -LocalPort {port} -State Listen "
                 f"-ErrorAction SilentlyContinue | Select-Object -First 1 "
                 f"-ExpandProperty OwningProcess)"],
                capture_output=True, text=True, timeout=10,
            )
            raw = (result.stdout or "").strip()
            return int(raw) if raw.isdigit() else None
        except Exception:
            return None
    try:
        result = subprocess.run(
            ["lsof", "-ti", f":{port}", "-sTCP:LISTEN"],
            capture_output=True, text=True, timeout=10,
        )
        raw = (result.stdout or "").strip().splitlines()
        return int(raw[0]) if raw and raw[0].isdigit() else None
    except Exception:
        return None


def get_process_tree_pids(root_pid):
    """Return root_pid and descendant PIDs (Windows Java often listens in a child)."""
    try:
        root_pid = int(root_pid)
    except (TypeError, ValueError):
        return set()
    pids = {root_pid}
    if sys.platform == "win32":
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"$root = {root_pid}; "
                 f"$all = @($root); "
                 f"$queue = @($root); "
                 f"while ($queue.Count -gt 0) {{ "
                 f"  $p = $queue[0]; $queue = $queue[1..($queue.Count-1)]; "
                 f"  Get-CimInstance Win32_Process -Filter \"ParentProcessId = $p\" "
                 f"-ErrorAction SilentlyContinue | ForEach-Object {{ "
                 f"    if ($all -notcontains $_.ProcessId) {{ "
                 f"      $all += $_.ProcessId; $queue += $_.ProcessId "
                 f"    }} "
                 f"  }} "
                 f"}}; $all -join ' '"],
                capture_output=True, text=True, timeout=15,
            )
            for part in (result.stdout or "").split():
                if part.isdigit():
                    pids.add(int(part))
        except Exception:
            pass
        return pids
    try:
        result = subprocess.run(
            ["pgrep", "-P", str(root_pid)],
            capture_output=True, text=True, timeout=10,
        )
        for part in (result.stdout or "").split():
            if part.isdigit():
                pids.add(int(part))
                pids.update(get_process_tree_pids(int(part)))
    except Exception:
        pass
    return pids


def get_process_command_line(pid):
    """Return command line for a PID, or empty string."""
    try:
        pid = int(pid)
    except (TypeError, ValueError):
        return ""
    if sys.platform == "win32":
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"(Get-CimInstance Win32_Process -Filter 'ProcessId = {pid}' "
                 f"-ErrorAction SilentlyContinue).CommandLine"],
                capture_output=True, text=True, timeout=10,
            )
            return (result.stdout or "").strip()
        except Exception:
            return ""
    try:
        result = subprocess.run(
            ["ps", "-p", str(pid), "-o", "args="],
            capture_output=True, text=True, timeout=10,
        )
        return (result.stdout or "").strip()
    except Exception:
        return ""


def process_owns_port(proc, port, jar_marker=None):
    """True if proc (or its children) owns the listening port, or runs the expected JAR."""
    if proc is None:
        return False
    owner = get_pid_on_port(port)
    if owner is None:
        return False
    tree = get_process_tree_pids(proc.pid)
    if owner in tree:
        return True
    if jar_marker:
        cmd = get_process_command_line(owner)
        if jar_marker in cmd.replace("\\", "/"):
            return True
    return False


def wait_for_port_free(port, timeout=15, poll_interval=0.5):
    """Block until nothing is listening on port (after cleanup)."""
    try:
        port = int(port)
    except (TypeError, ValueError):
        return True
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not check_port(port):
            return True
        time.sleep(poll_interval)
    return not check_port(port)


def check_health_for_process(port, proc, path="/actuator/health", timeout=5, paths=None, jar_marker=None):
    """Health check that also verifies our process owns the listening port."""
    if proc is not None and proc.poll() is not None:
        return False
    if not check_health(port, path=path, timeout=timeout, paths=paths):
        return False
    if proc is None:
        return True
    return process_owns_port(proc, port, jar_marker=jar_marker)


def wait_for_health(port, path="/actuator/health", timeout=90, poll_interval=2, paths=None, proc=None, jar_marker=None):
    """Poll until health is up and (when proc given) owned by that process."""
    start = time.time()
    try:
        timeout = int(str(timeout).strip())
    except (ValueError, TypeError):
        timeout = 90
    try:
        poll_interval = int(str(poll_interval).strip())
    except (ValueError, TypeError):
        poll_interval = 2
    health_paths = paths or [path, "/actuator/health/liveness", "/actuator/health"]
    remaining = timeout
    while remaining > 0:
        if proc is not None and proc.poll() is not None:
            return False, round(time.time() - start, 1)
        if check_health_for_process(
            port, proc, path=path, timeout=5, paths=health_paths, jar_marker=jar_marker
        ):
            elapsed = time.time() - start
            return True, round(elapsed, 1)
        # Port healthy but owned by another JVM — stop waiting (caller will retry or fail fast)
        if proc is not None and check_health(port, path=path, timeout=3, paths=health_paths):
            owner = get_pid_on_port(port)
            if owner and not process_owns_port(proc, port, jar_marker=jar_marker):
                return False, round(time.time() - start, 1)
        time.sleep(poll_interval)
        remaining -= poll_interval
    return False, timeout

def wait_for_port(port, timeout=30, poll_interval=1):
    """Wait for a port to become available."""
    start = time.time()
    while time.time() - start < timeout:
        if check_port(port):
            return True, time.time() - start
        time.sleep(poll_interval)
    return False, timeout

def get_process_on_port(port):
    """Get process name for what's listening on a port."""
    pid = get_pid_on_port(port)
    if pid is None:
        return None
    if sys.platform == "win32":
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"(Get-Process -Id {pid} -ErrorAction SilentlyContinue).ProcessName"],
                capture_output=True, text=True, timeout=10,
            )
            return result.stdout.strip() or str(pid)
        except Exception:
            return str(pid)
    try:
        result = subprocess.run(
            ["ps", "-p", str(pid), "-o", "comm="],
            capture_output=True, text=True, timeout=10,
        )
        return (result.stdout or "").strip() or str(pid)
    except Exception:
        return str(pid)

def kill_port(port):
    """Kill whatever process is using a port."""
    try:
        port = int(port)
    except (TypeError, ValueError):
        return
    if not check_port(port):
        return
    if sys.platform == "win32":
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"$pids = Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | "
                 f"Select-Object -ExpandProperty OwningProcess -Unique; "
                 f"foreach ($p in $pids) {{ if ($p) {{ taskkill /F /PID $p 2>$null }} }}"],
                capture_output=True, timeout=20,
            )
            if result.returncode != 0:
                subprocess.run(
                    ["powershell", "-NoProfile", "-Command",
                     f"Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | "
                     f"ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force "
                     f"-ErrorAction SilentlyContinue }}"],
                    capture_output=True, timeout=15,
                )
        except Exception:
            pass
    else:
        try:
            pid = subprocess.run(
                f"lsof -ti:{port}", shell=True, capture_output=True, text=True, timeout=10
            ).stdout.strip()
            if pid:
                subprocess.run(f"kill -9 {pid}", shell=True, timeout=10)
        except Exception:
            pass
    wait_for_port_free(port, timeout=10)

def jar_is_runnable(jar_path):
    """Spring Boot fat JARs are typically multi-MB; thin JARs fail at runtime."""
    try:
        return jar_path.exists() and jar_path.stat().st_size >= 5_000_000
    except OSError:
        return False


def build_service(dir_path, jar_name, maven_flags="-Dmaven.test.skip=true -q", force_rebuild=False):
    """Build a service's JAR using Maven. Returns True on success."""
    service_dir = BASE_DIR / dir_path
    jar_path = service_dir / "target" / jar_name

    if jar_is_runnable(jar_path) and not force_rebuild:
        return True

    mvn = resolve_executable("mvn")
    goal = "clean package" if force_rebuild else "package"
    log(f"  Building {dir_path} ({goal})...", color="yellow")
    mvn_args = [mvn, *goal.split(), *maven_flags.split()]
    result = subprocess.run(
        mvn_args,
        cwd=str(service_dir),
        capture_output=True,
        text=True,
        timeout=600,
    )
    if result.returncode != 0:
        detail = (result.stdout or "") + (result.stderr or "")
        log(f"  BUILD FAILED: {detail[-800:]}", color="red")
        return False
    log(f"  Built {dir_path}", color="green")
    return True

def is_docker_running():
    """True only when the Docker daemon responds (CLI alone is not enough)."""
    try:
        r = subprocess.run(
            ["docker", "info"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        if r.returncode != 0:
            return False
        err = (r.stderr or "").lower()
        return "cannot connect" not in err and "error during connect" not in err
    except Exception:
        return False


def find_windows_postgres():
    """Return (pg_ctl.exe path, data directory) for a local PostgreSQL install, or (None, None)."""
    if sys.platform != "win32":
        return None, None
    candidates = []
    program_files = os.environ.get("ProgramFiles", r"C:\Program Files")
    pg_root = Path(program_files) / "PostgreSQL"
    if pg_root.is_dir():
        for ver_dir in sorted(pg_root.iterdir(), reverse=True):
            pg_ctl = ver_dir / "bin" / "pg_ctl.exe"
            data = ver_dir / "data"
            if pg_ctl.is_file() and data.is_dir():
                candidates.append((str(pg_ctl), str(data)))
    pg_ctl_shim = shutil.which("pg_ctl")
    if pg_ctl_shim:
        bin_dir = Path(pg_ctl_shim).parent
        for data in (bin_dir.parent / "data", Path(program_files) / "PostgreSQL" / "17" / "data"):
            if data.is_dir():
                candidates.append((pg_ctl_shim, str(data)))
    return candidates[0] if candidates else (None, None)


def get_windows_postgres_service_name():
    """Return the name of an installed PostgreSQL Windows service, if any."""
    if sys.platform != "win32":
        return None
    try:
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             "(Get-Service -Name '*postgres*' -ErrorAction SilentlyContinue | "
             "Select-Object -First 1 -ExpandProperty Name)"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        name = (result.stdout or "").strip()
        return name or None
    except Exception:
        return None


def start_postgres_local_windows(log_path=None):
    """Start local PostgreSQL via pg_ctl or Windows service. Returns True if port 5432 opens."""
    if check_port(5432):
        return True

    pg_ctl, data_dir = find_windows_postgres()
    log_file = None
    if log_path:
        Path(log_path).parent.mkdir(parents=True, exist_ok=True)
        log_file = open(log_path, "a", encoding="utf-8")

    if pg_ctl and data_dir:
        try:
            status = subprocess.run(
                [pg_ctl, "status", "-D", data_dir],
                capture_output=True,
                text=True,
                timeout=15,
            )
            if "server is running" in (status.stdout or "").lower():
                return True
        except Exception:
            pass
        try:
            log("  Starting local PostgreSQL (pg_ctl)...", color="yellow")
            args = [pg_ctl, "start", "-D", data_dir, "-w", "-t", "45"]
            if log_path:
                args.extend(["-l", str(log_path)])
            subprocess.run(args, capture_output=True, text=True, timeout=60)
            ok, _ = wait_for_port(5432, 30)
            if ok:
                return True
        except Exception as e:
            if log_file:
                log_file.write(f"pg_ctl start failed: {e}\n")

    svc = get_windows_postgres_service_name()
    if svc:
        try:
            log(f"  Starting Windows service '{svc}' (may require Administrator)...", color="yellow")
            subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"Start-Service -Name '{svc}' -ErrorAction Stop"],
                capture_output=True,
                text=True,
                timeout=30,
            )
            ok, _ = wait_for_port(5432, 30)
            if ok:
                return True
        except Exception as e:
            if log_file:
                log_file.write(f"Start-Service failed: {e}\n")

    if log_file:
        log_file.close()
    return check_port(5432)


def start_postgres_docker(db_cfg, logs_dir):
    """Start jira-postgres via docker compose or docker run. Returns True if port opens."""
    compose_file = BASE_DIR / "docker-compose.yml"
    if compose_file.exists():
        try:
            log("  Starting PostgreSQL via docker compose...", color="yellow")
            subprocess.run(
                ["docker", "compose", "-f", str(compose_file), "up", "-d", "postgres"],
                cwd=str(BASE_DIR),
                capture_output=True,
                text=True,
                timeout=120,
            )
            ok, _ = wait_for_port(5432, 90)
            if ok:
                return True
        except Exception:
            pass

    dbs = db_cfg.get("databases", [])
    multi_db = ",".join(dbs) if dbs else (
        "auth_db,user_db,project_db,issue_db,workflow_db,comment_db,"
        "notification_db,search_db,audit_db,attachment_db,sprint_db"
    )
    env = [
        "-e", f"POSTGRES_USER={db_cfg.get('username', 'jiraadmin')}",
        "-e", f"POSTGRES_PASSWORD={db_cfg.get('password', 'jirapass123')}",
        "-e", f"POSTGRES_MULTIPLE_DATABASES={multi_db}",
    ]
    init_sql = BASE_DIR / "postgres" / "init" / "init-schemas.sql"
    volumes = ["-v", "jira-postgres-data:/var/lib/postgresql/data"]
    if init_sql.exists():
        volumes.extend(["-v", f"{init_sql}:/docker-entrypoint-initdb.d/init-schemas.sql:ro"])

    log("  Starting PostgreSQL via Docker...", color="yellow")
    try:
        subprocess.run(["docker", "rm", "-f", "jira-postgres"], capture_output=True, timeout=10)
    except Exception:
        pass

    log_path = logs_dir / "postgres.log"
    log_file = open(str(log_path), "w", encoding="utf-8")
    subprocess.Popen(
        ["docker", "run", "--rm", "-d",
         "--name", "jira-postgres",
         "-p", "5432:5432",
         "-e", "POSTGRES_INITDB_ARGS=--encoding=UTF8"] + env + volumes +
        ["postgres:16-alpine"],
        stdout=log_file,
        stderr=subprocess.STDOUT,
    )
    ok, _ = wait_for_port(5432, 60)
    if not ok:
        log_file.close()
    return ok

# ============================================================
# TOPOLOGICAL SORT (Kahn's algorithm)
# ============================================================
def topo_sort(services_dict):
    """Return ordered list of service names based on deps."""
    services = set(services_dict.keys())
    in_degree = {s: 0 for s in services}
    adj = {s: [] for s in services}

    for name, cfg in services_dict.items():
        for dep in cfg.get("deps", []):
            if dep in services:
                adj[dep].append(name)
                in_degree[name] += 1

    # Kahn's algorithm
    queue = deque([s for s in services if in_degree[s] == 0])
    order = []

    while queue:
        node = queue.popleft()
        order.append(node)
        for nxt in adj[node]:
            in_degree[nxt] -= 1
            if in_degree[nxt] == 0:
                queue.append(nxt)

    if len(order) != len(services):
        log("  WARNING: Circular dependency detected!", color="yellow")
        # Fall back: return all remaining
        remaining = [s for s in services if s not in order]
        order.extend(remaining)

    return order

def group_by_wave(order, services_dict):
    """Group services into startup waves (same dep level = same wave)."""
    waves = []
    remaining = set(order)

    while remaining:
        wave = []
        for name in list(remaining):
            deps = set(services_dict.get(name, {}).get("deps", []))
            ready = deps.issubset(set(wave)) or not deps.intersection(remaining - {name})
            if not deps or deps.issubset(set(wave)):
                wave.append(name)

        if not wave:
            # Deadlock — add remaining as one wave
            wave = list(remaining)
            remaining = set()
        else:
            for n in wave:
                remaining.discard(n)
        waves.append(wave)

    return waves


def validate_port_conflicts(services_cfg):
    """Return list of (port, [service names]) for duplicate HTTP ports."""
    by_port = {}
    for name, cfg in services_cfg.items():
        if not cfg.get("jar") or not cfg.get("dir"):
            continue
        port = cfg.get("port")
        if port is None:
            continue
        by_port.setdefault(port, []).append(name)
    return [(port, names) for port, names in sorted(by_port.items()) if len(names) > 1]


def validate_services_config(services_cfg):
    """Validate every configured JVM service has unique port, dir, and JAR."""
    errors = []
    warnings = []
    port_pat = re.compile(r"^\s*port:\s*(\d+)", re.MULTILINE)

    for name, cfg in sorted(services_cfg.items()):
        if not cfg.get("jar") or not cfg.get("dir"):
            continue
        port = cfg.get("port")
        dir_name = cfg.get("dir")
        jar_name = cfg.get("jar")
        if port is None:
            errors.append(f"{name}: missing port in config.yaml")
            continue
        service_dir = BASE_DIR / dir_name
        jar_path = service_dir / "target" / jar_name
        if not service_dir.is_dir():
            errors.append(f"{name}: directory not found ({dir_name})")
        elif not jar_is_runnable(jar_path):
            warnings.append(f"{name}: JAR missing or too small ({jar_path.name}) — will build")

        yml = service_dir / "src" / "main" / "resources" / "application.yml"
        if yml.exists():
            for match in port_pat.finditer(yml.read_text(encoding="utf-8", errors="replace")):
                yml_port = int(match.group(1))
                if yml_port != int(port):
                    warnings.append(
                        f"{name}: application.yml port {yml_port} != config.yaml {port} "
                        f"(launcher uses config + --server.port)"
                    )
                break

    return errors, warnings

# ============================================================
# SERVICE MANAGER
# ============================================================
class ServiceManager:
    def __init__(self, config):
        self.cfg = config
        self.services_cfg = config.get("services", {})
        self.db_cfg = config.get("database", {})
        self.sec_cfg = config.get("security", {})
        self.frontend_cfg = config.get("frontend", {})
        self.startup_cfg = config.get("startup", {})

        self.running = {}   # name -> {proc, port, log_file, start_time}
        self.failed = set()
        self.healthy = set()
        self.build_if_missing = self.startup_cfg.get("build_if_missing", True)
        self.health_path = self.startup_cfg.get("health_check_path", "/actuator/health")
        try:
            self.health_timeout = int(self.startup_cfg.get("health_timeout", 180))
        except (TypeError, ValueError):
            self.health_timeout = 180
        try:
            self.health_poll_interval = int(self.startup_cfg.get("health_poll_interval", 2))
        except (TypeError, ValueError):
            self.health_poll_interval = 2
        try:
            self.max_parallel = int(self.startup_cfg.get("max_parallel", 3))
        except (TypeError, ValueError):
            self.max_parallel = 3
        raw_ports = self.startup_cfg.get("protected_ports", [5432])
        self.protected_ports = set()
        for p in raw_ports if isinstance(raw_ports, (list, tuple)) else [raw_ports]:
            try:
                self.protected_ports.add(int(p))
            except (TypeError, ValueError):
                pass
        if not self.protected_ports:
            self.protected_ports = {5432}
        self.shutdown_timeout = self.startup_cfg.get("shutdown_timeout", 15)
        self.java_opts = self.startup_cfg.get("java_opts", "-Xms128m -Xmx384m -XX:+UseG1GC")
        self.open_browser = self.frontend_cfg.get("open_browser", True)
        self.frontend_port = self.frontend_cfg.get("port", 3000)
        self.maven_parallel = self.startup_cfg.get("maven_parallel", True)
        try:
            self.max_parallel_builds = int(self.startup_cfg.get("max_parallel_builds", 4))
        except (TypeError, ValueError):
            self.max_parallel_builds = 4
        self.force_rebuild = False
        self.skip_build = False
        self.only_services = None  # set of service names to start
        self._restart_counts = {}
        try:
            self._max_restart_attempts = int(self.startup_cfg.get("max_restart_attempts", 3))
        except (TypeError, ValueError):
            self._max_restart_attempts = 3
        try:
            self._restart_cooldown = int(self.startup_cfg.get("restart_cooldown_seconds", 30))
        except (TypeError, ValueError):
            self._restart_cooldown = 30

        LOGS_DIR.mkdir(exist_ok=True)

    def _resolve_start_set(self, startable):
        """Apply --only filter and include transitive dependencies."""
        if not self.only_services:
            return startable

        names = {n.strip().lower() for n in self.only_services if n.strip()}
        selected = set()

        def add_with_deps(name):
            if name not in startable or name in selected:
                return
            selected.add(name)
            for dep in startable[name].get("deps", []):
                add_with_deps(dep)

        for name in names:
            if name in startable:
                add_with_deps(name)
            else:
                log(f"  Unknown service '{name}' — ignored", color="yellow")

        if not selected:
            log("  --only matched no services; starting all", color="yellow")
            return startable

        return {k: v for k, v in startable.items() if k in selected}

    def start(self, build_only=False, no_browser=False):
        self.open_browser = self.open_browser and not no_browser

        # ---- Pre-flight checks ----
        self.check_prerequisites()
        conflicts = validate_port_conflicts(self.services_cfg)
        if conflicts:
            log("  PORT CONFLICTS (fix config.yaml before starting):", color="red")
            for port, names in conflicts:
                log(f"    Port {port}: {', '.join(names)}", color="red")
            sys.exit(1)

        svc_errors, svc_warnings = validate_services_config(self.services_cfg)
        if svc_errors:
            log("  SERVICE CONFIG ERRORS:", color="red")
            for msg in svc_errors:
                log(f"    {msg}", color="red")
            sys.exit(1)
        for msg in svc_warnings:
            log(f"  Warning: {msg}", color="yellow")

        self.cleanup_ports()

        # ---- PostgreSQL ----
        if not self.start_postgres():
            log("  PostgreSQL unavailable — services may fail to start", color="yellow")

        # ---- Build missing JARs ----
        if self.build_if_missing and not self.skip_build:
            self.build_all()
        elif self.skip_build:
            log("  Skipping Maven builds (--no-build)", color="gray")

        if build_only:
            log("", color="green")
            log("Build complete.", color="green")
            return

        # ---- Service waves (only JVM services with a JAR) ----
        startable = {
            name: cfg for name, cfg in self.services_cfg.items()
            if cfg.get("jar") and cfg.get("dir")
        }
        startable = self._resolve_start_set(startable)
        if self.only_services:
            log(f"  --only: {', '.join(sorted(startable.keys()))}", color="cyan")
        order = topo_sort(startable)
        waves = group_by_wave(order, startable)

        log("")
        log(f"  Starting {len(startable)} services in {len(waves)} wave(s)...", color="gray")

        for wave_i, wave in enumerate(waves):
            wave_num = wave_i + 1
            wave_names = ", ".join(wave)
            log("")
            log(f"  Wave {wave_num}/{len(waves)}: {wave_names}", color="cyan")

            # Start at most max_parallel JVMs at a time to avoid native OOM
            pending = [n for n in wave if n not in self.healthy and n not in self.failed]
            for chunk_start in range(0, len(pending), self.max_parallel):
                chunk = pending[chunk_start:chunk_start + self.max_parallel]
                if len(chunk) < len(pending):
                    log(f"    batch {chunk_start // self.max_parallel + 1}: {', '.join(chunk)}", color="gray")

                procs = []
                for name in chunk:
                    if name in self.healthy:
                        log(f"    {name:15s} already running", color="gray")
                        continue
                    if name in self.failed:
                        log(f"    {name:15s} failed, skipping", color="red")
                        continue
                    p = self._start_service(name)
                    procs.append((name, p))

                for name, p in procs:
                    if p is None:
                        continue
                    port = startable[name].get("port")
                    health_path = startable[name].get("health", self.health_path)
                    svc_timeout = startable[name].get("health_timeout", self.health_timeout)
                    try:
                        svc_timeout = int(svc_timeout)
                    except (TypeError, ValueError):
                        svc_timeout = self.health_timeout
                    if p.poll() is not None:
                        self.failed.add(name)
                        log(f"    {name:15s} ✗ exited before health check — see logs/{name}.log", color="red")
                        self._log_tail(name)
                        self._stop_process(name, p, port)
                        continue
                    jar_marker = startable[name].get("jar", "")
                    ok, elapsed = wait_for_health(
                        port, health_path, svc_timeout, self.health_poll_interval,
                        proc=p, jar_marker=jar_marker,
                    )
                    if ok:
                        self.healthy.add(name)
                        self._restart_counts[name] = 0
                        log(f"    {name:15s} ✓ (port {port}, {elapsed}s)", color="green")
                    else:
                        self.failed.add(name)
                        owner = get_pid_on_port(port)
                        if owner and not process_owns_port(p, port, jar_marker=jar_marker):
                            log(
                                f"    {name:15s} ✗ port {port} owned by PID {owner}, not this service "
                                f"— see logs/{name}.log",
                                color="red",
                            )
                        else:
                            log(f"    {name:15s} ✗ health check failed after {elapsed}s — see logs/{name}.log", color="red")
                        self._log_tail(name)
                        self._stop_process(name, p, port)

        # ---- Frontend ----
        self.start_frontend()

        # ---- Browser ----
        if self.open_browser:
            url = f"http://localhost:{self.frontend_port}"
            log("")
            log(f"  Opening browser: {url}", color="yellow")
            try:
                webbrowser.open(url)
            except Exception as e:
                log(f"  Could not open browser: {e}", color="yellow")

        # ---- Status Dashboard ----
        self.print_status()

        # ---- Monitor ----
        self.monitor()

    def check_prerequisites(self):
        log("")
        separator()
        log("  PREREQUISITES CHECK", color="cyan")

        # Java
        try:
            result = subprocess.run(["java", "-version"], capture_output=True, text=True, timeout=10)
            for line in result.stderr.splitlines()[:1]:
                log(f"  Java:    {line.strip()}", color="green")
                break
        except Exception:
            log("  Java:    NOT FOUND — install Java 21+", color="red")

        # Maven
        mvn = resolve_executable("mvn")
        try:
            result = subprocess.run([mvn, "-version"], capture_output=True, text=True, timeout=10)
            for line in (result.stdout or result.stderr).splitlines()[:1]:
                log(f"  Maven:   {line.strip()}", color="green")
                break
        except Exception:
            log("  Maven:   NOT FOUND — install Maven or add to PATH", color="red")

        # Node
        try:
            result = subprocess.run(["node", "--version"], capture_output=True, text=True, timeout=10)
            log(f"  Node:    {result.stdout.strip()}", color="green")
        except Exception:
            log("  Node:    NOT FOUND", color="red")

        # PostgreSQL
        if check_port(5432):
            log("  PostgreSQL: PORT 5432 — OK", color="green")
        elif is_docker_running():
            log("  PostgreSQL: not running — will auto-start via Docker", color="yellow")
        elif find_windows_postgres()[0]:
            log("  PostgreSQL: not running — will auto-start local PostgreSQL 17", color="yellow")
        else:
            log("  PostgreSQL: NOT running (port 5432 closed)", color="red")
            log("    Start Docker Desktop, or run: Start-Service postgresql-x64-17 (Admin)", color="gray")

        separator()

    def cleanup_ports(self):
        if not self.startup_cfg.get("cleanup_on_start", True):
            return

        ports_in_use = []
        for name, cfg in self.services_cfg.items():
            port = cfg.get("port")
            if port is None:
                continue
            try:
                port_num = int(port)
            except (TypeError, ValueError):
                continue
            if port_num in self.protected_ports:
                continue
            if check_port(port_num):
                proc_name = get_process_on_port(port)
                ports_in_use.append((port_num, name, proc_name))

        # Frontend
        try:
            fe_port = int(self.frontend_port)
        except (TypeError, ValueError):
            fe_port = 3000
        for fe_port in {fe_port, 3001}:
            if fe_port in self.protected_ports:
                continue
            if check_port(fe_port):
                ports_in_use.append((fe_port, "frontend", get_process_on_port(fe_port)))

        if not ports_in_use:
            return

        log("")
        log("  Ports in use — cleaning up...", color="yellow")
        for port, name, proc in ports_in_use:
            log(f"    Port {port} ({name}) — killing {proc or 'unknown process'}", color="gray")
            kill_port(port)
            if check_port(port):
                log(f"    Port {port} still busy — retrying kill...", color="yellow")
                kill_port(port)

        time.sleep(1)

    def start_postgres(self):
        if check_port(5432):
            log("  PostgreSQL already on :5432", color="green")
            return True

        ok = False
        if is_docker_running():
            ok = start_postgres_docker(self.db_cfg, LOGS_DIR)
        elif find_windows_postgres()[0]:
            ok = start_postgres_local_windows(str(LOGS_DIR / "postgres-local.log"))
        else:
            log("  PostgreSQL: no Docker daemon and no local install found", color="red")
            log("    Install PostgreSQL or start Docker Desktop, then re-run launcher.py", color="gray")
            return False

        if ok:
            log("  PostgreSQL ready on :5432", color="green")
            return True

        log("  PostgreSQL failed to start — see logs/postgres.log or logs/postgres-local.log", color="red")
        if sys.platform == "win32" and get_windows_postgres_service_name():
            log(
                "    Try (Admin PowerShell): Start-Service postgresql-x64-17",
                color="gray",
            )
        elif not is_docker_running() and shutil.which("docker"):
            log("    Docker CLI found but daemon is down — start Docker Desktop first", color="gray")
        return False

    def build_all(self):
        log("")
        log("  BUILDING MISSING JARs", color="cyan")
        maven_flags = self.startup_cfg.get("maven_flags", "-Dmaven.test.skip=true -q")
        if self.maven_parallel:
            maven_flags = f"{maven_flags} -T 1C"

        to_build = []
        for name, cfg in self.services_cfg.items():
            jar = cfg.get("jar")
            dir_name = cfg.get("dir")
            if not jar or not dir_name:
                continue
            if self.only_services:
                startable = self._resolve_start_set({
                    n: c for n, c in self.services_cfg.items() if c.get("jar") and c.get("dir")
                })
                if name not in startable:
                    continue
            jar_path = BASE_DIR / dir_name / "target" / jar
            if jar_is_runnable(jar_path) and not self.force_rebuild:
                continue
            to_build.append((name, dir_name, jar))

        if not to_build:
            log("  All JARs up to date", color="green")
            return

        if self.maven_parallel and len(to_build) > 1:
            from concurrent.futures import ThreadPoolExecutor, as_completed

            log(f"  Parallel build ({len(to_build)} services, max {self.max_parallel_builds})...", color="gray")
            with ThreadPoolExecutor(max_workers=self.max_parallel_builds) as pool:
                futures = {
                    pool.submit(build_service, d, j, maven_flags, self.force_rebuild): n
                    for n, d, j in to_build
                }
                for fut in as_completed(futures):
                    name = futures[fut]
                    try:
                        if not fut.result():
                            log(f"    {name} build failed", color="red")
                    except Exception as e:
                        log(f"    {name} build error: {e}", color="red")
        else:
            for name, dir_name, jar in to_build:
                if not build_service(dir_name, jar, maven_flags, self.force_rebuild):
                    log(f"    {name} build failed", color="red")

    def _build_env(self, name):
        """Build environment variables for a service."""
        db = self.db_cfg
        sec = self.sec_cfg
        cfg = self.services_cfg.get(name, {})

        port = cfg.get("port")
        env = {
            "DB_HOST": db.get("host", "localhost"),
            "DB_PORT": str(db.get("port", 5432)),
            "DB_USERNAME": db.get("username", "jiraadmin"),
            "DB_PASSWORD": db.get("password", "jirapass123"),
            "JWT_SECRET": sec.get("jwt_secret", ""),
            "SPRING_PROFILES_ACTIVE": "local",
            "MAIL_HEALTH_ENABLED": "false",
        }
        if port is not None:
            env["SERVER_PORT"] = str(port)

        # Inject upstream service URLs as env vars for gateway routing
        for svc_name, svc_cfg in self.services_cfg.items():
            key = f"{svc_name.upper().replace('-', '_')}_SERVICE_URL"
            port = svc_cfg.get("port", 0)
            if port:
                env[key] = f"http://localhost:{port}"

        return env

    def _stop_process(self, name, proc, port=None):
        """Terminate a failed or orphaned service process and free its port."""
        if proc and proc.poll() is None:
            try:
                proc.terminate()
                proc.wait(timeout=5)
            except Exception:
                try:
                    proc.kill()
                except Exception:
                    pass
        if port:
            try:
                kill_port(int(port))
            except (TypeError, ValueError):
                pass
        self.running.pop(name, None)

    def _start_service(self, name):
        cfg = self.services_cfg[name]
        jar_name = cfg.get("jar")
        dir_name = cfg.get("dir")
        port = cfg.get("port")

        if not jar_name or not dir_name:
            return None

        jar_path = BASE_DIR / dir_name / "target" / jar_name
        if not jar_is_runnable(jar_path):
            if self.build_if_missing:
                if not build_service(dir_name, jar_name):
                    self.failed.add(name)
                    return None
            else:
                log(f"    {name:15s} missing or invalid JAR: {jar_path}", color="red")
                self.failed.add(name)
                return None

        try:
            port_num = int(port)
        except (TypeError, ValueError):
            log(f"    {name:15s} invalid port: {port}", color="red")
            self.failed.add(name)
            return None

        health_path = cfg.get("health", self.health_path)
        owner = get_pid_on_port(port_num)
        if owner is not None:
            if check_health(port_num, health_path, timeout=3) and jar_name in get_process_command_line(owner):
                log(f"    {name:15s} ✓ already healthy on port {port_num} (PID {owner})", color="green")
                self.healthy.add(name)
                return None
            log(f"    Port {port_num} busy (PID {owner}) — freeing...", color="yellow")
            kill_port(port_num)
            if not wait_for_port_free(port_num, timeout=15):
                log(
                    f"    {name:15s} ✗ port {port_num} still in use after cleanup (PID {get_pid_on_port(port_num)})",
                    color="red",
                )
                self.failed.add(name)
                return None

        log_file = open(str(LOGS_DIR / f"{name}.log"), "w")
        env = self._build_env(name)

        env_list = []
        for k, v in env.items():
            env_list.append(f"{k}={v}")

        java_cmd = ["java"] + self.java_opts.split() + [
            "-jar", str(jar_path),
            f"--server.port={port}",
            "--spring.profiles.active=local",
            "--management.health.mail.enabled=false",
        ]

        proc = subprocess.Popen(
            java_cmd,
            stdout=log_file, stderr=subprocess.STDOUT,
            cwd=str(BASE_DIR),
            env={**os.environ, **dict(e.split("=", 1) for e in env_list)},
        )

        self.running[name] = {
            "proc": proc, "port": port,
            "log_file": log_file, "start_time": time.time(),
        }
        return proc

    def start_frontend(self):
        if check_port(self.frontend_port):
            log(f"  Frontend already on :{self.frontend_port}", color="green")
            return

        log(f"  Starting frontend (port {self.frontend_port})...", color="yellow")
        log_file = open(str(LOGS_DIR / "frontend.log"), "w")

        npm = resolve_executable("npm")
        proc = subprocess.Popen(
            [npm, "run", "dev", "--", "--port", str(self.frontend_port), "--strictPort"],
            cwd=str(FRONTEND_DIR),
            stdout=log_file, stderr=subprocess.STDOUT,
            env={**os.environ, "PORT": str(self.frontend_port)},
            shell=(sys.platform == "win32" and npm.endswith((".cmd", ".bat"))),
        )
        self.running["frontend"] = {
            "proc": proc, "port": self.frontend_port,
            "log_file": log_file, "start_time": time.time(),
        }

        ok = False
        for _ in range(45):
            if check_health(self.frontend_port, "/", timeout=3, paths=["/"]):
                ok = True
                break
            if check_port(self.frontend_port):
                ok = True
                break
            if proc.poll() is not None:
                break
            time.sleep(2)
        if ok:
            log(f"  Frontend ready on :{self.frontend_port}", color="green")
            self.healthy.add("frontend")
        else:
            log("  Frontend failed to start — see logs/frontend.log", color="red")
            self.failed.add("frontend")

    def print_status(self):
        log("")
        separator(char="═")
        log("  SERVICE STATUS", color="cyan")
        separator(char="─")

        healthy = sorted(self.healthy)
        failed = sorted(self.failed)

        # Header
        log(f"  {'Service':<15} {'Port':<6} {'Status':<12} {'Startup':<8} {'PID':<8}", color="gray")
        log("  " + "-" * 52, color="gray")

        for name, data in sorted(self.running.items()):
            if name == "postgres":
                port = 5432
            else:
                cfg = self.services_cfg.get(name, {})
                port = cfg.get("port", data.get("port", "-"))

            status = "✓ RUNNING" if name in self.healthy else "✗ FAILED"
            clr = "green" if name in self.healthy else "red"
            elapsed = ""
            pid = str(data["proc"].pid) if data["proc"].pid else "-"
            log(f"  {name:<15} {port:<6} {status:<12} {elapsed:<8} {pid:<8}", color=clr)

        separator(char="─")
        startable_names = {n for n, c in self.services_cfg.items() if c.get("jar")}
        jvm_healthy = len(self.healthy & startable_names)
        log(
            f"  Healthy: {jvm_healthy}/{len(startable_names)} services",
            color="green" if jvm_healthy >= len(startable_names) else "yellow",
        )
        log(f"  Frontend: http://localhost:{self.frontend_port}", color="cyan")
        log(f"  Gateway:  http://localhost:{self.services_cfg.get('gateway', {}).get('port', 8080)}", color="cyan")
        separator(char="═")

    def _log_tail(self, name, lines=8):
        """Print last lines of a service log after a crash."""
        log_path = LOGS_DIR / f"{name}.log"
        if not log_path.exists():
            return
        try:
            tail = log_path.read_text(encoding="utf-8", errors="replace").splitlines()[-lines:]
            for line in tail:
                log(f"      | {line}", color="gray", dim=True)
        except Exception:
            pass

    def monitor(self):
        """Watch for crashes and restart critical services."""
        log("")
        log("  Monitoring services (Ctrl+C to stop)...", color="gray")
        check_count = 0
        while True:
            time.sleep(20)
            check_count += 1

            # Check if any process died
            for name, data in list(self.running.items()):
                if name == "frontend":
                    continue
                if data["proc"].poll() is None:
                    continue

                exit_code = data["proc"].poll()
                attempts = self._restart_counts.get(name, 0) + 1
                self._restart_counts[name] = attempts

                log(f"  [!] {name} died (exit {exit_code})", color="yellow")
                self._log_tail(name)

                if attempts > self._max_restart_attempts:
                    log(
                        f"  [!] {name} exceeded {self._max_restart_attempts} restarts — "
                        f"see logs/{name}.log (not restarting)",
                        color="red",
                    )
                    self.healthy.discard(name)
                    self.failed.add(name)
                    continue

                log(f"  [!] {name} restart {attempts}/{self._max_restart_attempts} in {self._restart_cooldown}s...", color="yellow")
                time.sleep(self._restart_cooldown)
                if name == "frontend":
                    self.start_frontend()
                    if "frontend" in self.healthy:
                        self._restart_counts[name] = 0
                        log(f"  [R] {name} restarted", color="green")
                    continue
                p = self._start_service(name)
                if p:
                    port = self.services_cfg.get(name, {}).get("port", 0)
                    jar_marker = self.services_cfg.get(name, {}).get("jar", "")
                    ok, _ = wait_for_health(
                        port, self.health_path, self.health_timeout, proc=p, jar_marker=jar_marker
                    )
                    if ok:
                        self.healthy.add(name)
                        self.failed.discard(name)
                        self._restart_counts[name] = 0
                        log(f"  [R] {name} restarted", color="green")
                    else:
                        log(f"  [!] {name} health check failed after restart — see logs/{name}.log", color="red")
                        self._log_tail(name)

            # Periodic health check (every JVM service we started)
            if check_count % 3 == 0:
                for name in list(self.healthy):
                    if name in ("frontend", "postgres"):
                        continue
                    data = self.running.get(name)
                    port = self.services_cfg.get(name, {}).get("port")
                    proc = data["proc"] if data else None
                    jar_marker = self.services_cfg.get(name, {}).get("jar", "")
                    if port and not check_health_for_process(
                        port, proc, path=self.health_path, jar_marker=jar_marker
                    ):
                        log(f"  [!] {name} not responding — marking unhealthy", color="yellow")
                        self.healthy.discard(name)

    def stop(self):
        log("")
        separator()
        log("  SHUTDOWN", color="red")

        # Reverse startup order
        order = list(reversed(list(self.running.keys())))
        for name in order:
            data = self.running.get(name)
            if not data:
                continue
            proc = data["proc"]
            try:
                log(f"  Stopping {name}...", color="yellow")
                proc.terminate()
                try:
                    proc.wait(timeout=self.shutdown_timeout)
                except subprocess.TimeoutExpired:
                    log(f"  Force-killing {name}...", color="red")
                    proc.kill()
            except Exception as e:
                log(f"  Error stopping {name}: {e}", color="red")

            try:
                data["log_file"].close()
            except Exception:
                pass

        # Docker postgres
        try:
            subprocess.run(["docker", "stop", "jira-postgres"],
                          capture_output=True, timeout=20)
        except Exception:
            pass

        self.running.clear()
        log("  All services stopped", color="green")
        separator()

# ============================================================
# MAIN
# ============================================================
def main():
    # Parse args
    config_path = None
    build_only = False
    no_browser = False
    show_status = False
    no_build = False
    force_rebuild = False
    only_services = None

    args = sys.argv[1:]
    i = 0
    while i < len(args):
        arg = args[i]
        if arg in ("--help", "-h", "/?"):
            print("""Jira Platform Launcher
  python launcher.py              Start all services
  python launcher.py --config X   Use config file X
  python launcher.py --no-browser Skip browser auto-open
  python launcher.py --build-only Build JARs and exit
  python launcher.py --only project,gateway,auth   Start subset (+ deps)
  python launcher.py --no-build     Skip Maven (fast restart)
  python launcher.py --rebuild    Force mvn clean package
  python launcher.py --status     Show running services
  python launcher.py --help      Show this help""")
            return
        if arg == "--config" and i + 1 < len(args):
            config_path = args[i + 1]
            i += 2
            continue
        elif arg == "--only" and i + 1 < len(args):
            only_services = args[i + 1]
            i += 2
            continue
        elif arg == "--build-only":
            build_only = True
        elif arg == "--no-browser":
            no_browser = True
        elif arg == "--no-build":
            no_build = True
        elif arg == "--rebuild":
            force_rebuild = True
        elif arg == "--status":
            show_status = True
        i += 1

    # Load config
    config = load_config(config_path or DEFAULT_CONFIG)

    # Banner
    print()
    log("╔══════════════════════════════════════════════════════════════╗", color="cyan")
    log("║              JIRA PLATFORM — ENTERPRISE LAUNCHER          ║", color="cyan")
    log("╚══════════════════════════════════════════════════════════════╝", color="cyan")
    print()
    log(f"  Config: {config_path or DEFAULT_CONFIG}", color="gray")
    log(f"  Services: {len(config.get('services', {}))}", color="gray")
    log(f"  Build if missing: {config.get('startup', {}).get('build_if_missing', True)}", color="gray")

    # Status mode
    if show_status:
        for name, cfg in config.get("services", {}).items():
            port = cfg.get("port", 0)
            h = check_health(port, config.get("startup", {}).get("health_check_path", "/actuator/health"))
            mark = "✓" if h else "✗"
            clr = "green" if h else "gray"
            log(f"  {mark} {name:<15} :{port}", color=clr)
        return

    # Signal handlers
    manager = ServiceManager(config)
    manager.skip_build = no_build
    manager.force_rebuild = force_rebuild
    if only_services:
        manager.only_services = [s.strip() for s in only_services.split(",")]

    def on_signal(signum, frame):
        log("\n  Interrupt received — shutting down...", color="yellow")
        manager.stop()
        sys.exit(0)

    signal.signal(signal.SIGINT, on_signal)
    signal.signal(signal.SIGTERM, on_signal)

    try:
        manager.start(build_only=build_only, no_browser=no_browser)
    except KeyboardInterrupt:
        manager.stop()

if __name__ == "__main__":
    main()
