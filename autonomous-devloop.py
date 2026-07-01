# -*- coding: utf-8 -*-
"""
Jira Platform — Autonomous DevLoop Orchestrator
================================================
Continuous build → start → monitor → report → fix cycle.
No human in the loop until acceptance criteria are met.

Usage:
  python autonomous-devloop.py                       Normal mode
  python autonomous-devloop.py --feature feature.md  Custom feature file
  python autonomous-devloop.py --max-iterations 5    Iteration limit
  python autonomous-devloop.py --watch-only          Monitor only, no build
  python autonomous-devloop.py --skip-tests          Skip Maven tests
  python autonomous-devloop.py --verbose             Detailed output
"""
import subprocess
import sys
import os
import time
import json
import re
import signal
from pathlib import Path

# Force UTF-8 on Windows
if sys.platform == "win32":
    import io
    try:
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")
    except Exception:
        pass  # Already wrapped or closed

# Add script directory to path for local module imports
_SCRIPT_DIR = Path(__file__).parent.resolve()
if str(_SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPT_DIR))

from devloop_runner import ServiceManager
from devloop_monitor import LogMonitor
from devloop_reporter import ErrorReporter
from devloop_builder import build_services

# ============================================================
# ANSI COLORS
# ============================================================
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
# CONFIG
# ============================================================
BASE_DIR = Path(__file__).parent
FEATURE_DEFAULT = BASE_DIR / "feature.md"
LOGS_DIR = BASE_DIR / "logs"
AUTONOMOUS_STATE = BASE_DIR / ".devloop-state.json"

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

def separator(char="=", width=72, color="cyan"):
    log(char * width, color=color)

# ============================================================
# STATE PERSISTENCE
# ============================================================
def load_state():
    if AUTONOMOUS_STATE.exists():
        try:
            return json.loads(AUTONOMOUS_STATE.read_text(encoding="utf-8"))
        except Exception:
            pass
    return {"iteration": 0, "feature_mtime": 0, "last_errors": [], "success": False}

def save_state(state):
    try:
        AUTONOMOUS_STATE.write_text(json.dumps(state, indent=2), encoding="utf-8")
    except Exception:
        pass

# ============================================================
# ACCEPTANCE CRITERIA CHECKER
# ============================================================
def load_feature(path):
    """Load feature.md and extract acceptance criteria."""
    if not path.exists():
        return {"content": "", "criteria": []}
    content = path.read_text(encoding="utf-8", errors="replace")
    criteria = []
    in_criteria = False
    for line in content.splitlines():
        if re.match(r"(?i)(acceptance|given|when|then|verify|check)", line):
            in_criteria = True
        if in_criteria:
            stripped = line.strip()
            if stripped:
                criteria.append(stripped)
    return {"content": content, "criteria": criteria}

def check_acceptance(monitor, feature_path, services_healthy):
    """Check if acceptance criteria are met."""
    feat = load_feature(feature_path)
    if not feat["criteria"]:
        return None  # No criteria = don't know, keep going

    passed = []
    failed = []

    for criterion in feat["criteria"]:
        c_lower = criterion.lower()

        # Build/compilation check
        if any(k in c_lower for k in ["compile", "build", "maven"]):
            if monitor.last_build_success:
                passed.append(criterion)
            else:
                failed.append(criterion)

        # Service startup check
        elif any(k in c_lower for k in ["start", "launch", "running"]):
            if services_healthy:
                passed.append(criterion)
            else:
                failed.append(criterion)

        # API endpoint check
        elif any(k in c_lower for k in ["api", "endpoint", "http"]):
            api_ok = all(
                monitor.last_api_results.get(f"http://localhost:{port}/actuator/health", {}).get("ok", False)
                for port in [8080, 8081, 8082, 8083, 8084]
            )
            if api_ok:
                passed.append(criterion)
            else:
                failed.append(criterion)

        # UI/no-error check
        elif any(k in c_lower for k in ["no error", "white screen", "black screen", "render", "ui"]):
            if monitor.console_error_count == 0 and services_healthy:
                passed.append(criterion)
            else:
                failed.append(criterion)

        # Default: can't evaluate
        else:
            passed.append(f"[UNKNOWN] {criterion}")

    return {"passed": passed, "failed": failed}

# ============================================================
# MAIN ORCHESTRATOR
# ============================================================
class AutonomousDevLoop:
    def __init__(self, feature_path=None, max_iterations=10, skip_tests=True,
                 watch_only=False, verbose=False):
        self.feature_path = Path(feature_path) if feature_path else FEATURE_DEFAULT
        self.max_iterations = max_iterations
        self.skip_tests = skip_tests
        self.watch_only = watch_only
        self.verbose = verbose
        self.state = load_state()
        self.running = True
        self.iteration = self.state.get("iteration", 0)
        self.fix_prompt_history = []

        # Components
        self.service_manager = None
        self.log_monitor = None
        self.error_reporter = None

        signal.signal(signal.SIGINT, self._on_signal)
        signal.signal(signal.SIGTERM, self._on_signal)

    def _on_signal(self, signum, frame):
        log("\n  Interrupt received — stopping devloop...", color="yellow")
        self.running = False
        if self.service_manager:
            self.service_manager.stop_all()
        sys.exit(0)

    def banner(self):
        separator(char="=", color="cyan")
        log("       AUTONOMOUS DEVLOOP — JIRA PLATFORM", color="cyan", bold=True)
        log("       Build → Start → Monitor → Fix → Repeat", color="gray")
        separator(char="=", color="cyan")
        log(f"  Feature:  {self.feature_path}", color="gray")
        log(f"  Max iter: {self.max_iterations}", color="gray")
        log(f"  Logs:     {LOGS_DIR}", color="gray")
        separator()

    def step1_build(self):
        """Build all Maven services."""
        log("")
        log("─" * 72, color="blue")
        log("  STEP 1: BUILD", color="blue", bold=True)
        log("─" * 72, color="blue")

        success = build_services(
            skip_tests=self.skip_tests,
            force=False,
            services_cfg=None,
            base_dir=BASE_DIR,
            verbose=self.verbose,
        )

        if success:
            log("  BUILD SUCCESS", color="green")
        else:
            log("  BUILD FAILED", color="red")

        return success

    def step2_start_services(self):
        """Start all platform services."""
        log("")
        log("─" * 72, color="blue")
        log("  STEP 2: START SERVICES", color="blue", bold=True)
        log("─" * 72, color="blue")

        if self.service_manager:
            self.service_manager.stop_all()

        self.service_manager = ServiceManager(base_dir=BASE_DIR, logs_dir=LOGS_DIR)
        started, healthy = self.service_manager.start_all()

        log("")
        if healthy >= 3:
            log(f"  SERVICES STARTED: {healthy} healthy", color="green")
        elif healthy > 0:
            log(f"  SERVICES PARTIAL: {healthy} healthy, some may recover", color="yellow")
        else:
            log("  SERVICES FAILED TO START", color="red")

        return healthy >= 1, healthy

    def step3_monitor(self):
        """Monitor logs and capture errors."""
        log("")
        log("─" * 72, color="blue")
        log("  STEP 3: MONITOR (30s observation window)", color="blue", bold=True)
        log("─" * 72, color="blue")

        self.log_monitor = LogMonitor(logs_dir=LOGS_DIR)
        self.log_monitor.start_watching()

        # Give services time to settle and expose startup errors
        time.sleep(30)

        self.log_monitor.stop_watching()
        errors = self.log_monitor.get_all_errors()

        log("")
        log(f"  Log errors captured: {len(errors)}", color="cyan")

        return errors

    def step4_report(self, errors):
        """Generate fix prompt for AI."""
        log("")
        log("─" * 72, color="blue")
        log("  STEP 4: ERROR ANALYSIS", color="blue", bold=True)
        log("─" * 72, color="blue")

        if not errors:
            log("  No errors detected — system appears healthy!", color="green")
            return None

        self.error_reporter = ErrorReporter(errors=errors, logs_dir=LOGS_DIR)
        report = self.error_reporter.generate_fix_prompt(
            feature_path=self.feature_path,
            verbose=self.verbose,
        )

        return report

    def step5_interact(self, report):
        """Send report to AI for fixing (using Claude Code subprocess)."""
        if not report:
            return True  # No errors = success

        log("")
        log("─" * 72, color="yellow")
        log("  STEP 5: REQUESTING AI FIX", color="yellow", bold=True)
        log("─" * 72, color="yellow")

        # Construct AI prompt
        prompt = f"""You are fixing errors in the Jira Platform monorepo.

## Current Errors:
{report['summary']}

## Detailed Errors:
{report['detailed']}

## Instructions:
1. Read the error details above carefully
2. Read the feature requirements: {self.feature_path}
3. Analyze the source code and identify root causes
4. Make the minimum fixes necessary to resolve the errors
5. Do NOT refactor unrelated code
6. After fixing, return a JSON summary:
{{"fixed": ["list of fixes made"], "files_modified": ["list of files"]}}

## Root Causes Identified:
{report.get('root_causes', 'See detailed errors above')}
"""

        # Write fix request to temp file for AI to read
        fix_request_file = BASE_DIR / ".devloop-fix-request.md"
        fix_request_file.write_text(prompt, encoding="utf-8")

        log(f"  Fix request written to: {fix_request_file}", color="gray")
        log("  Awaiting AI response... (implement your AI integration here)", color="cyan")
        log("")
        log("  " + "=" * 70, color="gray")
        log("  IMPORTANT: Open a Claude Code session and run:", color="yellow")
        log(f"  $ claude --no-input < {fix_request_file}", color="cyan")
        log("  " + "=" * 70, color="gray")

        return False  # Return False to continue loop

    def check_break_conditions(self):
        """Check if we should stop iterating."""
        if self.iteration >= self.max_iterations:
            log(f"\n  Max iterations ({self.max_iterations}) reached", color="red")
            return True

        state = self.state
        if state.get("success"):
            return True

        return False

    def run(self):
        """Main autonomous loop."""
        self.banner()

        # Initial build check
        if not self.watch_only:
            if not self.step1_build():
                log("  Initial build failed. Check Maven configuration.", color="red")
                log("  Run manually: mvn clean package -DskipTests", color="gray")
                return

        while self.running and self.iteration < self.max_iterations:
            self.iteration += 1
            self.state["iteration"] = self.iteration
            save_state(self.state)

            separator(char="─", color="magenta")
            log(f"  ITERATION {self.iteration}/{self.max_iterations}", color="magenta", bold=True)
            separator(char="─", color="magenta")

            # Step 2: Start services
            services_ok, healthy = self.step2_start_services()
            if not services_ok:
                log("  Services failed to start. Attempting rebuild...", color="yellow")
                if self.step1_build():
                    services_ok, healthy = self.step2_start_services()

            # Step 3: Monitor
            errors = self.step3_monitor()

            # Step 4: Check acceptance criteria
            if services_ok:
                acceptance = check_acceptance(self.log_monitor, self.feature_path, services_ok)
                if acceptance:
                    passed = acceptance.get("passed", [])
                    failed = acceptance.get("failed", [])
                    if not failed:
                        log("")
                        log("  ALL ACCEPTANCE CRITERIA MET!", color="green", bold=True)
                        self.state["success"] = True
                        save_state(self.state)
                        break
                    else:
                        log(f"  {len(passed)} criteria passed, {len(failed)} failed", color="yellow")

            # Step 5: Report and interact
            report = self.step4_report(errors)
            done = self.step5_interact(report)

            if done:
                break

            # Check break conditions
            if self.check_break_conditions():
                break

            # Brief pause before next iteration
            log("")
            log("  Next iteration in 5s (Ctrl+C to stop)...", color="gray")
            time.sleep(5)

        # Final status
        self._final_report()

    def _final_report(self):
        separator(char="=", color="cyan")
        log("  AUTONOMOUS DEVLOOP — FINAL REPORT", color="cyan", bold=True)
        separator(char="=", color="cyan")
        log(f"  Total iterations: {self.iteration}", color="gray")
        log(f"  Feature: {self.feature_path}", color="gray")

        if self.state.get("success"):
            log("  Status: SUCCESS — All criteria met", color="green", bold=True)
        elif self.iteration >= self.max_iterations:
            log(f"  Status: MAX ITERATIONS ({self.max_iterations})", color="red")
            log("  Review the fix requests and implement manually or increase --max-iterations", color="gray")
        else:
            log("  Status: INTERRUPTED", color="yellow")

        # Log health summary
        if self.service_manager:
            log("")
            log("  Service Health Summary:", color="cyan")
            for name, status in self.service_manager.get_health_summary().items():
                mark = "✓" if status == "healthy" else "✗"
                clr = "green" if status == "healthy" else "red"
                log(f"    {mark} {name}: {status}", color=clr)

        separator(char="=", color="cyan")

# ============================================================
# MAIN
# ============================================================
def main():
    import argparse
    parser = argparse.ArgumentParser(description="Jira Platform — Autonomous DevLoop")
    parser.add_argument("--feature", default=str(FEATURE_DEFAULT),
                        help="Path to feature.md")
    parser.add_argument("--max-iterations", type=int, default=10)
    parser.add_argument("--skip-tests", action="store_true", default=True,
                        help="Skip Maven tests during build")
    parser.add_argument("--no-skip-tests", action="store_false", dest="skip_tests")
    parser.add_argument("--watch-only", action="store_true",
                        help="Skip build, only monitor running services")
    parser.add_argument("--verbose", action="store_true")

    args = parser.parse_args()

    loop = AutonomousDevLoop(
        feature_path=args.feature,
        max_iterations=args.max_iterations,
        skip_tests=args.skip_tests,
        watch_only=args.watch_only,
        verbose=args.verbose,
    )
    loop.run()

if __name__ == "__main__":
    main()