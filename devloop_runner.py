# -*- coding: utf-8 -*-
"""
Jira Platform — Service Lifecycle Manager
=======================================
Handles: start, stop, port cleanup, health checking.
No manual intervention needed.
"""
import subprocess
import time
import json
import sys
import os
import shutil
from pathlib import Path
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

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
# LOGGING
# ============================================================
def log(msg="", color="white", bold=False, dim=False):
    """Simple logging function."""
    try:
        print(msg, flush=True)
    except Exception:
        print(msg.encode("ascii", "ignore").decode("ascii"), flush=True)

# ============================================================
# UTILITIES
# ============================================================
PROTECTED_PORTS = {5432}  # Don't kill postgres

def resolve_executable(name):
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

def check_port(port, host="127.0.0.1"):
    import socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(2)
    try:
        return s.connect_ex((host, int(port))) == 0
    except Exception:
        return False
    finally:
        s.close()

def get_pid_on_port(port):
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
    return None

def kill_port(port):
    try:
        port = int(port)
    except (TypeError, ValueError):
        return
    if port in PROTECTED_PORTS:
        return
    if not check_port(port):
        return
    if sys.platform == "win32":
        try:
            subprocess.run(
                ["powershell", "-NoProfile", "-Command",
                 f"$pids = Get-NetTCPConnection -LocalPort {port} -ErrorAction SilentlyContinue | "
                 f"Select-Object -ExpandProperty OwningProcess -Unique; "
                 f"foreach ($p in $pids) {{ if ($p) {{ taskkill /F /PID $p 2>$null }} }}"],
                capture_output=True, timeout=20,
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
    time.sleep(0.5)

def check_health(port, path="/actuator/health", timeout=10):
    import urllib.request
    candidates = [path, "/actuator/health/liveness", "/actuator/health"]
    seen = set()
    ordered = []
    for c in candidates:
        if c not in seen:
            seen.add(c)
            ordered.append(c)
    for candidate in ordered:
        try:
            url = f"http://127.0.0.1:{port}{candidate}"
            resp = urllib.request.urlopen(url, timeout=timeout)
            if 200 <= resp.status < 300:
                return True
        except Exception:
            continue
    return False

def wait_for_health(port, path="/actuator/health", timeout=90, poll_interval=3):
    start = time.time()
    while time.time() - start < timeout:
        if check_health(port, path=path, timeout=5):
            return True, round(time.time() - start, 1)
        time.sleep(poll_interval)
    return False, timeout

def wait_for_port_free(port, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if not check_port(port):
            return True
        time.sleep(0.5)
    return not check_port(port)

def find_jar(service_dir, jar_pattern=None):
    """Find the fat JAR in target directory."""
    target_dir = service_dir / "target"
    if not target_dir.is_dir():
        return None
    jars = list(target_dir.glob("*.jar"))
    jars = [j for j in jars if "original" not in j.name and "classes" not in j.name]
    if not jars:
        return None
    # Sort by size descending (fat JAR is largest)
    jars.sort(key=lambda j: j.stat().st_size, reverse=True)
    return jars[0]

def jar_is_runnable(jar_path):
    try:
        return jar_path.exists() and jar_path.stat().st_size >= 5_000_000
    except OSError:
        return False

# ============================================================
# SERVICE CONFIG
# ============================================================
def load_services_config(base_dir):
    """Load services from config/services.json."""
    config_path = base_dir / "config" / "services.json"
    if config_path.exists():
        try:
            data = json.loads(config_path.read_text(encoding="utf-8"))
            return data.get("services", [])
        except Exception:
            pass
    # Fallback: hardcoded defaults
    return _default_services()

def _default_services():
    return [
        {"name": "auth-service", "port": 8081, "type": "backend", "jarName": "jira-auth-service-1.0.0.jar", "memory": "256m"},
        {"name": "user-service", "port": 8082, "type": "backend", "jarName": "jira-user-service-1.0.0.jar", "memory": "256m"},
        {"name": "project-service", "port": 8083, "type": "backend", "jarName": "jira-project-service-1.0.0.jar", "memory": "256m"},
        {"name": "issue-service", "port": 8084, "type": "backend", "jarName": "jira-issue-service-1.0.0.jar", "memory": "512m"},
        {"name": "workflow-service", "port": 8085, "type": "backend", "jarName": "jira-workflow-service-1.0.0.jar", "memory": "256m"},
        {"name": "comment-service", "port": 8086, "type": "backend", "jarName": "jira-comment-service-1.0.0.jar", "memory": "256m"},
        {"name": "notification-service", "port": 8087, "type": "backend", "jarName": "jira-notification-service-1.0.0.jar", "memory": "256m"},
        {"name": "search-service", "port": 8088, "type": "backend", "jarName": "jira-search-service-1.0.0.jar", "memory": "256m"},
        {"name": "audit-service", "port": 8089, "type": "backend", "jarName": "jira-audit-service-1.0.0.jar", "memory": "256m"},
        {"name": "attachment-service", "port": 8090, "type": "backend", "jarName": "jira-attachment-service-1.0.0.jar", "memory": "512m"},
        {"name": "sprint-service", "port": 8091, "type": "backend", "jarName": "jira-sprint-service-1.0.0.jar", "memory": "256m"},
        {"name": "plan-service", "port": 8092, "type": "backend", "jarName": "jira-plan-service-1.0.0.jar", "memory": "256m"},
        {"name": "admin-service", "port": 8093, "type": "backend", "jarName": "jira-admin-service-1.0.0.jar", "memory": "256m"},
        {"name": "migration-service", "port": 8094, "type": "backend", "jarName": "jira-migration-service-1.0.0.jar", "memory": "512m"},
        {"name": "gateway", "port": 8080, "type": "gateway", "jarName": "jira-gateway-1.0.0.jar", "memory": "512m"},
        {"name": "frontend", "port": 3000, "type": "frontend", "memory": "256m"},
    ]

# ============================================================
# SERVICE MANAGER
# ============================================================
class ServiceManager:
    def __init__(self, base_dir, logs_dir):
        self.base_dir = base_dir
        self.logs_dir = logs_dir
        self.services_cfg = load_services_config(base_dir)
        self.running = {}  # name -> {proc, port, log_file, service_cfg}
        self.proc_log_files = []

    def _get_service_dir(self, cfg):
        """Get service module directory."""
        name = cfg.get("name", "")
        if name == "gateway":
            return self.base_dir / "jira-gateway"
        if name == "frontend":
            return self.base_dir / "jira-frontend"
        # jira-XXX-service
        short = name.replace("-service", "").replace("jira-", "")
        return self.base_dir / f"jira-{short}-service"

    def _build_env(self, cfg):
        """Build environment variables for a service."""
        env = dict(os.environ)
        if cfg.get("environment"):
            for k, v in cfg["environment"].items():
                env[k] = str(v) if v is not None else ""
        # Always inject these
        env["DB_HOST"] = str(env.get("DB_HOST", "localhost"))
        env["DB_PORT"] = str(env.get("DB_PORT", "5432"))
        env["DB_USERNAME"] = str(env.get("DB_USERNAME", "jiraadmin"))
        env["DB_PASSWORD"] = str(env.get("DB_PASSWORD", "jirapass123"))
        env["JWT_SECRET"] = str(env.get("JWT_SECRET", "jira-platform-super-secret-key-that-is-at-least-256-bits-long"))
        env["SPRING_PROFILES_ACTIVE"] = "local"
        env["MAIL_HEALTH_ENABLED"] = "false"
        return env

    def stop_all(self):
        """Stop all running services."""
        # Stop in reverse order
        for name in list(self.running.keys()):
            self._stop_service(name)
        # Kill any stray processes on our ports
        for cfg in self.services_cfg:
            port = cfg.get("port")
            if port and port not in PROTECTED_PORTS:
                kill_port(port)
        # Close log files
        for f in self.proc_log_files:
            try:
                f.close()
            except Exception:
                pass
        self.proc_log_files.clear()
        self.running.clear()

    def _stop_service(self, name):
        """Stop a single service."""
        if name not in self.running:
            return
        data = self.running[name]
        proc = data.get("proc")
        log_file = data.get("log_file")
        if proc:
            try:
                proc.terminate()
                proc.wait(timeout=5)
            except Exception:
                try:
                    proc.kill()
                except Exception:
                    pass
        if log_file:
            try:
                log_file.close()
            except Exception:
                pass
        del self.running[name]

    def _kill_port_processes(self):
        """Kill processes on service ports and clear old logs."""
        for cfg in self.services_cfg:
            port = cfg.get("port")
            if port and port not in PROTECTED_PORTS:
                kill_port(port)
        # Clear old log files to prevent stale errors from being counted
        try:
            for log_file in self.logs_dir.glob("*.log"):
                if log_file.is_file():
                    try:
                        with open(log_file, "w", encoding="utf-8") as f:
                            f.write(f"# Log cleared at {datetime.now().isoformat()}\n")
                    except Exception:
                        pass
        except Exception:
            pass
        time.sleep(1)

    def _start_backend_service(self, cfg):
        """Start a single JVM backend service."""
        name = cfg.get("name", "unknown")
        port = cfg.get("port")
        memory = cfg.get("memory", "256m")
        jar_name = cfg.get("jarName")

        service_dir = self._get_service_dir(cfg)

        # Find JAR
        if jar_name:
            jar_path = service_dir / "target" / jar_name
        else:
            jar_path = find_jar(service_dir)

        if not jar_path or not jar_is_runnable(jar_path):
            return None, "JAR not found or invalid"

        # Kill existing process on port
        if port:
            kill_port(port)

        # Setup logging
        log_path = self.logs_dir / f"{name}.log"
        try:
            log_file = open(log_path, "w", encoding="utf-8")
            self.proc_log_files.append(log_file)
        except Exception:
            log_file = subprocess.DEVNULL

        # Build command
        java_opts = f"-Xms{memory} -Xmx{memory} -XX:+UseG1GC"
        cmd = ["java"] + java_opts.split() + [
            "-jar", str(jar_path),
            f"--server.port={port}",
            "--spring.profiles.active=local",
            "--management.endpoint.health.probes.enabled=true",
            "--management.health.mail.enabled=false",
        ]

        env = self._build_env(cfg)

        try:
            proc = subprocess.Popen(
                cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                cwd=str(self.base_dir),
                env=env,
            )
        except Exception as e:
            return None, str(e)

        return proc, None

    def _start_frontend_service(self, cfg):
        """Start the React frontend."""
        name = cfg.get("name", "frontend")
        port = cfg.get("port", 3000)
        frontend_dir = self.base_dir / "jira-frontend"

        if not frontend_dir.is_dir():
            return None, "Frontend directory not found"

        # Kill existing
        kill_port(port)

        log_path = self.logs_dir / "frontend.log"
        try:
            log_file = open(log_path, "w", encoding="utf-8")
            self.proc_log_files.append(log_file)
        except Exception:
            log_file = subprocess.DEVNULL

        npm = resolve_executable("npm")
        cmd = [npm, "run", "dev", "--", "--port", str(port)]

        try:
            proc = subprocess.Popen(
                cmd,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                cwd=str(frontend_dir),
                env={**os.environ, "PORT": str(port)},
                shell=(sys.platform == "win32" and npm.endswith((".cmd", ".bat"))),
            )
        except Exception as e:
            return None, str(e)

        return proc, None

    def start_all(self):
        """Start all services with proper ordering."""
        log("  Cleaning ports...", color="gray")
        self._kill_port_processes()

        started = 0
        healthy = 0
        failed = []

        # Phase 1: Backend services (parallel)
        backend_services = [s for s in self.services_cfg if s.get("type") in ("backend", "gateway")]
        frontend_service = [s for s in self.services_cfg if s.get("type") == "frontend"]

        log(f"  Starting {len(backend_services)} backend services...", color="cyan")

        # Start backends in parallel batches
        for cfg in backend_services:
            name = cfg.get("name")
            port = cfg.get("port")
            proc, err = self._start_backend_service(cfg)
            if proc:
                self.running[name] = {"proc": proc, "port": port, "log_file": None, "service_cfg": cfg}
                started += 1
            else:
                failed.append(f"{name}: {err}")

        # Wait for backends to become healthy
        log("  Waiting for backend health checks (60s)...", color="gray")
        time.sleep(30)  # Give services time to initialize

        for name, data in list(self.running.items()):
            if data["service_cfg"].get("type") == "frontend":
                continue
            port = data["port"]
            ok, elapsed = wait_for_health(port, timeout=60)
            if ok:
                healthy += 1
            else:
                failed.append(f"{name}: health check timeout")

        # Phase 2: Frontend
        if frontend_service:
            log("  Starting frontend...", color="cyan")
            cfg = frontend_service[0]
            name = cfg.get("name")
            port = cfg.get("port", 3000)
            proc, err = self._start_frontend_service(cfg)
            if proc:
                self.running[name] = {"proc": proc, "port": port, "log_file": None, "service_cfg": cfg}
                started += 1
                # Give frontend time to start
                time.sleep(10)
                if check_port(port):
                    healthy += 1
                else:
                    failed.append("frontend: port not responding")
            else:
                failed.append(f"frontend: {err}")

        # Summary
        log("")
        for f in failed:
            log(f"    FAIL: {f}", color="red")

        return started, healthy

    def get_health_summary(self):
        """Return health status for all services."""
        summary = {}
        for name, data in self.running.items():
            port = data.get("port")
            if port:
                ok = check_health(port)
                summary[name] = "healthy" if ok else "starting"
            else:
                summary[name] = "unknown"
        return summary