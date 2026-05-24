# Jira Platform — Autonomous Development Workflow

## Overview

This project includes an **Autonomous DevLoop** system that eliminates manual build/test/restart cycles. The AI system builds, starts services, monitors logs, captures errors, and generates fix requests — all without human intervention.

## Components

| File | Purpose |
|------|---------|
| `autonomous-devloop.py` | Main orchestrator — runs the full build→start→monitor→fix loop |
| `devloop-runner.py` | Service lifecycle: start, stop, port cleanup, health checks |
| `devloop-monitor.py` | Real-time log monitoring and error pattern detection |
| `devloop-builder.py` | Maven build automation with parallel compilation |
| `devloop-reporter.py` | Error aggregation, categorization, and AI fix prompts |
| `devloop.bat` | Windows launcher script |

## Usage

### Quick Start

```powershell
python autonomous-devloop.py
```

### Options

```powershell
# Custom feature file
python autonomous-devloop.py --feature my-feature.md

# Limit iterations
python autonomous-devloop.py --max-iterations 5

# Watch mode (skip build, only monitor running services)
python autonomous-devloop.py --watch-only

# Detailed output
python autonomous-devloop.py --verbose
```

### Windows Launcher

```powershell
# Run the devloop
devloop.bat

# Or directly
devloop.bat run
```

## Workflow

```
feature.md
    ↓
[1] BUILD (Maven compile + package)
    ↓
[2] START (all services in waves)
    ↓
[3] MONITOR (30s log observation)
    ↓
[4] REPORT (aggregate errors by type)
    ↓
[5] FIX REQUEST (AI reads errors, generates fix)
    ↓
[6] REPEAT until acceptance criteria met
```

## How It Works

### Step 1: Build
- Maven parallel build of all 16 services
- Detects stale JARs (source newer than binary)
- Skips tests by default for speed
- Captures compilation errors

### Step 2: Start Services
- Wave-based startup (backends → gateway → frontend)
- Automatic port cleanup before starting
- Health check with 60s timeout per service
- All logs redirected to `logs/` directory

### Step 3: Monitor
- Background thread scans all `.log` files every 2 seconds
- Pattern matching for:
  - Java/Spring exceptions
  - Maven compilation errors
  - HTTP API errors (404, 500, 503)
  - Database errors (SQLException, deadlocks)
  - Frontend errors (React, webpack)
  - Port conflicts
- No browser interaction required

### Step 4: Report
- Groups errors by type and service
- Identifies root causes from exception chains
- Generates structured fix prompt for AI

### Step 5: Fix Loop
- Writes fix request to `.devloop-fix-request.md`
- AI reads the file, implements fixes
- Loop repeats until errors are resolved or max iterations reached

## Acceptance Criteria

The loop continues until:

1. **Build succeeds** — Maven returns exit code 0
2. **Services start** — All health endpoints return 200
3. **No critical errors** — Zero compilation/runtime exceptions
4. **Feature criteria met** — Checks `feature.md` acceptance criteria

## Logs

All logs are written to:
- `logs/*.log` — Individual service logs
- `logs/platform-runtime.log` — Aggregated platform log
- `.devloop-fix-request.md` — AI fix prompts
- `.devloop-state.json` — Iteration state for resume

## Port Assignments

| Service | Port |
|---------|------|
| PostgreSQL | 5432 |
| Gateway | 8080 |
| Auth | 8081 |
| User | 8082 |
| Project | 8083 |
| Issue | 8084 |
| Workflow | 8085 |
| Comment | 8086 |
| Notification | 8087 |
| Search | 8088 |
| Audit | 8089 |
| Attachment | 8090 |
| Sprint | 8091 |
| Plan | 8092 |
| Admin | 8093 |
| Migration | 8094 |
| Frontend | 3000 |

## For AI Assistants

When fixing errors from `.devloop-fix-request.md`:

1. Read the error summary and detailed errors
2. Read `feature.md` for requirements
3. Analyze root causes (often the "Caused by:" line)
4. Make minimum necessary fixes only
5. DO NOT refactor unrelated code
6. After fixing, output:
```json
{
  "fixed": ["list of fixes made"],
  "files_modified": ["list of files changed"]
}
```

## Manual Fallback

If autonomous mode fails:

```powershell
# Build manually
mvn clean package -DskipTests

# Start platform
python launcher.py

# Or use PowerShell
.\start-platform.ps1
```

## Troubleshooting

**Port conflicts:** Run `kill-ports.bat` or `devloop-runner.py` handles this automatically.

**Build failures:** Check `logs/mvn-*.log` for details.

**Services won't start:** Check health endpoints manually:
```powershell
curl http://localhost:8081/actuator/health
```

**Frontend issues:** Check `logs/frontend.log` for React/webpack errors.