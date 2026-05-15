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
  python launcher.py --status            Show running services and exit

Reads config.yaml by default. All service ports and credentials come from there.
Press Ctrl+C to gracefully stop all services.
"""
import subprocess
import time
import sys
import os
import webbrowser
import signal
import json
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
    """Load config.yaml, merge with Python defaults."""
    defaults = _get_defaults()
    cfg_file = config_path or DEFAULT_CONFIG
    if cfg_file and Path(cfg_file).exists():
        try:
            file_cfg = _parse_yaml_simple(Path(cfg_file).read_text(encoding="utf-8"))
            if file_cfg:
                defaults = _deep_merge(defaults, file_cfg)
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
        },
        "frontend": {"port": 3000, "dir": "jira-frontend", "open_browser": True},
        "startup": {
            "build_if_missing": True,
            "health_check_path": "/actuator/health",
            "health_timeout": 90,
            "health_poll_interval": 2,
            "shutdown_timeout": 15,
            "log_dir": "logs",
            "cleanup_on_start": True,
            "java_opts": "-Xms256m -Xmx512m",
            "maven_flags": "-DskipTests",
        },
    }

# ============================================================
# UTILITIES
# ============================================================
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

def check_health(port, path="/actuator/health", timeout=5):
    """Return True if service responds with 2xx on health endpoint."""
    try:
        url = f"http://localhost:{port}{path}"
        req = urllib.request.Request(url)
        req.add_header("User-Agent", "JiraPlatformLauncher/1.0")
        resp = urllib.request.urlopen(req, timeout=timeout)
        return 200 <= resp.status < 300
    except Exception:
        return False

def wait_for_health(port, path="/actuator/health", timeout=90, poll_interval=2):
    """Poll health endpoint until up or timeout."""
    start = time.time()
    try:
        timeout = int(str(timeout).strip())
    except (ValueError, TypeError):
        timeout = 90
    try:
        poll_interval = int(str(poll_interval).strip())
    except (ValueError, TypeError):
        poll_interval = 2
    remaining = timeout
    while remaining > 0:
        if check_health(port, path):
            elapsed = time.time() - start
            return True, round(elapsed, 1)
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
    """Get process info for what's listening on a port (Windows)."""
    if sys.platform != "win32":
        return None
    try:
        result = subprocess.run(
            ["powershell", "-NoProfile", "-Command",
             f"(Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | "
             f"Select-Object -First 1).OwningProcess | "
             f"Get-Process -ErrorAction SilentlyContinue | "
             f"Select-Object -ExpandProperty ProcessName"],
            capture_output=True, text=True, timeout=10,
        )
        return result.stdout.strip() or None
    except Exception:
        return None

def kill_port(port):
    """Kill whatever process is using a port."""
    if not check_port(port):
        return
    if sys.platform == "win32":
        try:
            subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | "
                 f"ForEach-Object {{ Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }}"],
                capture_output=True, timeout=15,
            )
        except Exception:
            pass
    else:
        # Unix
        try:
            pid = subprocess.run(
                f"lsof -ti:{port}", shell=True, capture_output=True, text=True, timeout=10
            ).stdout.strip()
            if pid:
                subprocess.run(f"kill -9 {pid}", shell=True, timeout=10)
        except Exception:
            pass

def build_service(dir_path, jar_name, maven_flags="-DskipTests"):
    """Build a service's JAR using Maven. Returns True on success."""
    service_dir = BASE_DIR / dir_path
    jar_path = service_dir / "target" / jar_name

    if jar_path.exists():
        return True

    log(f"  Building {dir_path} (first run)...", color="yellow")
    result = subprocess.run(
        ["mvn", "clean", "package", maven_flags],
        cwd=str(service_dir),
        capture_output=True,
        text=True,
        timeout=600,
    )
    if result.returncode != 0:
        log(f"  BUILD FAILED: {result.stderr[:400]}", color="red")
        return False
    log(f"  Built {dir_path}", color="green")
    return True

def is_docker_running():
    try:
        r = subprocess.run(["docker", "info"], capture_output=True, timeout=10)
        return r.returncode == 0
    except Exception:
        return False

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
        self.health_timeout = self.startup_cfg.get("health_timeout", 90)
        self.shutdown_timeout = self.startup_cfg.get("shutdown_timeout", 15)
        self.java_opts = self.startup_cfg.get("java_opts", "-Xms256m -Xmx512m")
        self.open_browser = self.frontend_cfg.get("open_browser", True)
        self.frontend_port = self.frontend_cfg.get("port", 3000)

        LOGS_DIR.mkdir(exist_ok=True)

    def start(self, build_only=False, no_browser=False):
        self.open_browser = self.open_browser and not no_browser

        # ---- Pre-flight checks ----
        self.check_prerequisites()
        self.cleanup_ports()

        # ---- PostgreSQL ----
        if not self.start_postgres():
            log("  PostgreSQL unavailable — services may fail to start", color="yellow")

        # ---- Build missing JARs ----
        if self.build_if_missing:
            self.build_all()

        if build_only:
            log("", color="green")
            log("Build complete.", color="green")
            return

        # ---- Service waves ----
        order = topo_sort(self.services_cfg)
        waves = group_by_wave(order, self.services_cfg)

        log("")
        log(f"  Starting {len(self.services_cfg)} services in {len(waves)} wave(s)...", color="gray")

        for wave_i, wave in enumerate(waves):
            wave_num = wave_i + 1
            wave_names = ", ".join(wave)
            log("")
            log(f"  Wave {wave_num}/{len(waves)}: {wave_names}", color="cyan")

            procs = []
            for name in wave:
                if name in self.healthy:
                    log(f"    {name:15s} already running", color="gray")
                    continue
                if name in self.failed:
                    log(f"    {name:15s} failed, skipping", color="red")
                    continue
                p = self._start_service(name)
                procs.append((name, p))

            # Wait for all in wave to become healthy
            for name, p in procs:
                if p is None:
                    continue
                port = self.services_cfg[name].get("port")
                ok, elapsed = wait_for_health(port, self.health_path, self.health_timeout)
                if ok:
                    self.healthy.add(name)
                    log(f"    {name:15s} ✓ (port {port}, {elapsed}s)", color="green")
                else:
                    self.failed.add(name)
                    log(f"    {name:15s} ✗ health check failed after {elapsed}s", color="red")

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
        try:
            result = subprocess.run(["mvn", "-version"], capture_output=True, text=True, timeout=10)
            for line in result.stdout.splitlines()[:1]:
                log(f"  Maven:   {line.strip()}", color="green")
                break
        except Exception:
            log("  Maven:   NOT FOUND", color="red")

        # Node
        try:
            result = subprocess.run(["node", "--version"], capture_output=True, text=True, timeout=10)
            log(f"  Node:    {result.stdout.strip()}", color="green")
        except Exception:
            log("  Node:    NOT FOUND", color="red")

        # PostgreSQL
        if check_port:
            log("  PostgreSQL: PORT 5432 — OK", color="green")
        elif is_docker_running():
            log("  PostgreSQL: Docker available, will auto-start", color="yellow")
        else:
            log("  PostgreSQL: NOT running (port 5432 closed)", color="red")

        separator()

    def cleanup_ports(self):
        if not self.startup_cfg.get("cleanup_on_start", True):
            return

        ports_in_use = []
        for name, cfg in self.services_cfg.items():
            port = cfg.get("port")
            if port and check_port(port):
                proc_name = get_process_on_port(port)
                ports_in_use.append((port, name, proc_name))

        # Frontend
        if check_port(self.frontend_port):
            ports_in_use.append((self.frontend_port, "frontend", get_process_on_port(self.frontend_port)))

        if not ports_in_use:
            return

        log("")
        log("  Ports in use — cleaning up...", color="yellow")
        for port, name, proc in ports_in_use:
            log(f"    Port {port} ({name}) — killing {proc or 'unknown process'}", color="gray")
            kill_port(port)

        time.sleep(2)

    def start_postgres(self):
        if check_port(5432):
            log("  PostgreSQL already on :5432", color="green")
            return True

        if not is_docker_running():
            log("  Docker not available — cannot auto-start PostgreSQL", color="yellow")
            return False

        log("  Starting PostgreSQL via Docker...", color="yellow")
        dbs = self.db_cfg.get("databases", [])
        multi_db = ",".join(dbs) if dbs else "auth_db,user_db,project_db,issue_db,workflow_db,comment_db,notification_db,search_db,audit_db,attachment_db,sprint_db"

        env = [
            "-e", f"POSTGRES_USER={self.db_cfg.get('username', 'jiraadmin')}",
            "-e", f"POSTGRES_PASSWORD={self.db_cfg.get('password', 'jirapass123')}",
            "-e", f"POSTGRES_MULTIPLE_DATABASES={multi_db}",
        ]

        # Remove existing container if any
        try:
            subprocess.run(["docker", "rm", "-f", "jira-postgres"],
                          capture_output=True, timeout=10)
        except Exception:
            pass

        log_file = open(str(LOGS_DIR / "postgres.log"), "w")
        proc = subprocess.Popen(
            ["docker", "run", "--rm", "-d",
             "--name", "jira-postgres",
             "-p", "5432:5432",
             "-e", "POSTGRES_INITDB_ARGS=--encoding=UTF8",
             "-v", "jira-postgres-data:/var/lib/postgresql/data"] +
            env +
            ["postgres:16-alpine"],
            stdout=log_file, stderr=subprocess.STDOUT,
        )

        ok, _ = wait_for_port(5432, 60)
        if ok:
            log("  PostgreSQL ready on :5432", color="green")
            self.running["postgres"] = {"proc": proc, "port": 5432, "log_file": log_file, "start_time": time.time()}
            return True
        else:
            log("  PostgreSQL failed to start — check logs/postgres.log", color="red")
            return False

    def build_all(self):
        log("")
        log("  BUILDING MISSING JARs", color="cyan")
        for name, cfg in self.services_cfg.items():
            jar = cfg.get("jar")
            dir_name = cfg.get("dir")
            if not jar or not dir_name:
                continue
            jar_path = BASE_DIR / dir_name / "target" / jar
            if jar_path.exists():
                continue
            if not build_service(dir_name, jar, self.startup_cfg.get("maven_flags", "-DskipTests")):
                log(f"    {name} build failed", color="red")

    def _build_env(self, name):
        """Build environment variables for a service."""
        db = self.db_cfg
        sec = self.sec_cfg
        cfg = self.services_cfg.get(name, {})

        env = {
            "DB_HOST": db.get("host", "localhost"),
            "DB_PORT": str(db.get("port", 5432)),
            "DB_USERNAME": db.get("username", "jiraadmin"),
            "DB_PASSWORD": db.get("password", "jirapass123"),
            "JWT_SECRET": sec.get("jwt_secret", ""),
            "SPRING_PROFILES_ACTIVE": "local",
        }

        # Inject upstream service URLs as env vars for gateway routing
        for svc_name, svc_cfg in self.services_cfg.items():
            key = f"{svc_name.upper().replace('-', '_')}_SERVICE_URL"
            port = svc_cfg.get("port", 0)
            if port:
                env[key] = f"http://localhost:{port}"

        return env

    def _start_service(self, name):
        cfg = self.services_cfg[name]
        jar_name = cfg.get("jar")
        dir_name = cfg.get("dir")
        port = cfg.get("port")

        if not jar_name or not dir_name:
            return None

        jar_path = BASE_DIR / dir_name / "target" / jar_name
        if not jar_path.exists():
            if self.build_if_missing:
                if not build_service(dir_name, jar_name):
                    self.failed.add(name)
                    return None
            else:
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

        proc = subprocess.Popen(
            ["npm", "run", "dev"],
            cwd=str(FRONTEND_DIR),
            stdout=log_file, stderr=subprocess.STDOUT,
            env={**os.environ, "PORT": str(self.frontend_port)},
        )
        self.running["frontend"] = {
            "proc": proc, "port": self.frontend_port,
            "log_file": log_file, "start_time": time.time(),
        }

        ok, _ = wait_for_port(self.frontend_port, 60)
        if ok:
            log(f"  Frontend ready on :{self.frontend_port}", color="green")
        else:
            log("  Frontend failed to start", color="red")

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
        log(f"  Healthy: {len(self.healthy)}/{len(self.services_cfg)} services", color="green" if self.healthy == set(self.services_cfg.keys()) else "yellow")
        log(f"  Frontend: http://localhost:{self.frontend_port}", color="cyan")
        log(f"  Gateway:  http://localhost:{self.services_cfg.get('gateway', {}).get('port', 8080)}", color="cyan")
        separator(char="═")

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
                if data["proc"].poll() is not None:
                    exit_code = data["proc"].poll()
                    log(f"  [!] {name} died (exit {exit_code}) — restarting...", color="yellow")
                    time.sleep(2)
                    p = self._start_service(name)
                    if p:
                        port = self.services_cfg.get(name, {}).get("port", 0)
                        ok, _ = wait_for_health(port, self.health_path, self.health_timeout)
                        if ok:
                            self.healthy.add(name)
                            self.failed.discard(name)
                            log(f"  [R] {name} restarted", color="green")

            # Periodic health check
            if check_count % 3 == 0:
                for name in list(self.healthy):
                    port = self.services_cfg.get(name, {}).get("port")
                    if port and not check_health(port, self.health_path):
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

    for arg in sys.argv[1:]:
        if arg in ("--help", "-h", "/?"):
            print("""Jira Platform Launcher
  python launcher.py              Start all services
  python launcher.py --config X   Use config file X
  python launcher.py --no-browser Skip browser auto-open
  python launcher.py --build-only Build JARs and exit
  python launcher.py --status     Show running services
  python launcher.py --help      Show this help""")
            return
        if arg == "--config" and len(sys.argv) > (i := sys.argv.index(arg) + 1):
            config_path = sys.argv[i]
        elif arg == "--build-only":
            build_only = True
        elif arg == "--no-browser":
            no_browser = True
        elif arg == "--status":
            show_status = True

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
