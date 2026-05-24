# -*- coding: utf-8 -*-
"""
Jira Platform — Maven Build Automation
=====================================
Handles parallel Maven builds, compilation checking.
No manual mvn commands needed.
"""
import subprocess
import time
import sys
import os
import shutil
from pathlib import Path
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
# UTILITIES
# ============================================================
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

def jar_is_runnable(jar_path):
    try:
        return jar_path.exists() and jar_path.stat().st_size >= 5_000_000
    except OSError:
        return False

def jar_is_stale(jar_path, service_dir):
    """True when source is newer than JAR."""
    if not jar_path.exists():
        return True
    try:
        jar_mtime = jar_path.stat().st_mtime
    except OSError:
        return True
    pom = service_dir / "pom.xml"
    if pom.is_file() and pom.stat().st_mtime > jar_mtime:
        return True
    src_root = service_dir / "src"
    if not src_root.is_dir():
        return False
    for path in src_root.rglob("*"):
        if path.is_file() and path.suffix.lower() in {".java", ".xml", ".yml", ".yaml", ".properties"}:
            try:
                if path.stat().st_mtime > jar_mtime:
                    return True
            except OSError:
                continue
    return False

def get_default_services():
    """Get default service list."""
    return [
        ("auth-service", "jira-auth-service"),
        ("user-service", "jira-user-service"),
        ("project-service", "jira-project-service"),
        ("issue-service", "jira-issue-service"),
        ("workflow-service", "jira-workflow-service"),
        ("comment-service", "jira-comment-service"),
        ("notification-service", "jira-notification-service"),
        ("search-service", "jira-search-service"),
        ("audit-service", "jira-audit-service"),
        ("attachment-service", "jira-attachment-service"),
        ("sprint-service", "jira-sprint-service"),
        ("plan-service", "jira-plan-service"),
        ("admin-service", "jira-admin-service"),
        ("migration-service", "jira-migration-service"),
        ("gateway", "jira-gateway"),
    ]

# ============================================================
# BUILD FUNCTIONS
# ============================================================
def build_service(service_name, service_dir, maven_flags="-Dmaven.test.skip=true", verbose=False):
    """Build a single service with Maven. Returns (success, output)."""
    mvn = resolve_executable("mvn")
    jar_path = service_dir / "target"

    # Try to find JAR name from pom or target
    jar_files = list(jar_path.glob("*.jar")) if jar_path.is_dir() else []
    jar_files = [j for j in jar_files if "original" not in j.name and "classes" not in j.name]
    jar_name = jar_files[0].name if jar_files else None

    log_msg = f"  Building {service_name}"
    if verbose:
        log_msg += f" ({service_dir})"
    print(log_msg, flush=True)

    cmd = [mvn, "package", *maven_flags.split()]

    result = subprocess.run(
        cmd,
        cwd=str(service_dir),
        capture_output=True,
        text=True,
        timeout=300,
    )

    success = result.returncode == 0
    output = (result.stdout or "") + "\n" + (result.stderr or "")

    if success:
        print(f"  ✓ {service_name}", flush=True)
    else:
        print(f"  ✗ {service_name}", flush=True)
        if verbose:
            # Print last 500 chars of error
            error_tail = output[-500:] if len(output) > 500 else output
            for line in error_tail.splitlines()[-20:]:
                if line.strip():
                    print(f"      {line}", flush=True)

    return success, output

def build_services(skip_tests=True, force=False, services_cfg=None, base_dir=None, verbose=False):
    """Build all services in parallel."""
    if base_dir is None:
        base_dir = Path(__file__).parent

    # Get service list
    if services_cfg:
        services = []
        for name, cfg in services_cfg.items():
            if cfg.get("jar") and cfg.get("dir"):
                services.append((name, base_dir / cfg["dir"]))
    else:
        services = get_default_services()
        services = [(name, base_dir / dir_name) for name, dir_name in services]

    # Filter to only services that need building
    to_build = []
    for name, service_dir in services:
        if not service_dir.exists():
            continue

        # Find JAR
        target_dir = service_dir / "target"
        if target_dir.is_dir():
            jars = [j for j in target_dir.glob("*.jar")
                    if "original" not in j.name and "classes" not in j.name]
            jar = jars[0] if jars else None
        else:
            jar = None

        if force or not jar or not jar_is_runnable(jar) or jar_is_stale(jar, service_dir):
            to_build.append((name, service_dir))

    if not to_build:
        print("  All JARs up to date", flush=True)
        return True

    print(f"  Building {len(to_build)} service(s)...", flush=True)

    # Build in parallel (max 4 concurrent)
    success_count = 0
    maven_flags = "-Dmaven.test.skip=true -q"
    if skip_tests:
        maven_flags += " -DskipTests"

    with ThreadPoolExecutor(max_workers=4) as pool:
        futures = {
            pool.submit(build_service, name, service_dir, maven_flags, verbose): name
            for name, service_dir in to_build
        }
        for fut in as_completed(futures):
            name = futures[fut]
            try:
                success, output = fut.result()
                if success:
                    success_count += 1
            except Exception as e:
                print(f"  ✗ {name}: {e}", flush=True)

    print(f"  Build complete: {success_count}/{len(to_build)} successful", flush=True)
    return success_count == len(to_build)

def check_maven_available():
    """Check if Maven is available."""
    mvn = resolve_executable("mvn")
    try:
        result = subprocess.run([mvn, "-version"], capture_output=True, text=True, timeout=10)
        return result.returncode == 0
    except Exception:
        return False

def clean_build(service_dir):
    """Run mvn clean on a service."""
    mvn = resolve_executable("mvn")
    result = subprocess.run(
        [mvn, "clean"],
        cwd=str(service_dir),
        capture_output=True,
        text=True,
        timeout=120,
    )
    return result.returncode == 0

def get_build_errors(maven_output):
    """Extract error lines from Maven output."""
    errors = []
    for line in maven_output.splitlines():
        if "[ERROR]" in line or "error:" in line.lower():
            errors.append(line.strip())
    return errors

def check_compilation_in_service(service_dir):
    """Quick compile check without packaging."""
    mvn = resolve_executable("mvn")
    result = subprocess.run(
        [mvn, "compile", "-Dmaven.test.skip=true"],
        cwd=str(service_dir),
        capture_output=True,
        text=True,
        timeout=180,
    )
    return result.returncode == 0, (result.stdout or "") + (result.stderr or "")