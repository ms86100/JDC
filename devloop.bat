@echo off
setlocal EnableDelayedExpansion

:: Jira Platform — DevLoop Launcher
:: Uses Python's -m flag to avoid import issues on Windows

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

:: Find Python
set "PYTHON=python"
where python >nul 2>&1 || set "PYTHON=py"

:: Default settings
set "FEATURE_FILE=feature.md"
set "MAX_ITER=10"
set "PYTHONPATH=%SCRIPT_DIR%"
set "PY_ARGS="

:: Parse arguments
:parse
if "%~1"=="" goto :run
if /i "%~1"=="run" goto :run
if /i "%~1"=="build" goto :build
if /i "%~1"=="monitor" goto :monitor
if /i "%~1"=="status" goto :status
if /i "%~1"=="stop" goto :stop
if /i "%~1"=="--help" goto :help
if /i "%~1"=="-h" goto :help
if /i "%~1"=="--feature" (set "FEATURE_FILE=%~2"& shift & shift & goto :parse)
if /i "%~1"=="--max-iterations" (set "MAX_ITER=%~2"& shift & shift & goto :parse)
if /i "%~1"=="--verbose" (set "PY_ARGS=%PY_ARGS% --verbose"& shift & goto :parse)
if /i "%~1"=="--quiet" (set "PY_ARGS=%PY_ARGS% --quiet"& shift & goto :parse)
if /i "%~1"=="--watch-only" (set "PY_ARGS=%PY_ARGS% --watch-only"& shift & goto :parse)
if /i "%~1"=="--skip-tests" (set "PY_ARGS=%PY_ARGS% --skip-tests"& shift & goto :parse)
shift & goto :parse

:run
echo.
echo ============================================================
echo   JIRA PLATFORM — AUTONOMOUS DEVLOOP
echo ============================================================
echo.
echo Feature: %FEATURE_FILE%
echo Max iterations: %MAX_ITER%
echo.
%PYTHON% -Xutf8 "%SCRIPT_DIR%autonomous-devloop.py" --feature "%FEATURE_FILE%" --max-iterations %MAX_ITER% %PY_ARGS%
goto :end

:build
echo.
echo Building all services...
echo.
%PYTHON% -Xutf8 -c "import sys; sys.path.insert(0, '.'); from devloop_builder import build_services; build_services(verbose=True)"
goto :end

:monitor
echo.
echo Monitoring services for 60 seconds...
echo.
%PYTHON% -Xutf8 -c "import sys; sys.path.insert(0, '.'); from devloop_monitor import LogMonitor; from pathlib import Path; m = LogMonitor(Path('logs')); m.start_watching(); import time; time.sleep(60)"
goto :end

:status
echo.
echo Checking service health...
echo.
%PYTHON% -Xutf8 "%SCRIPT_DIR%check-status.py"
goto :end

:stop
echo.
echo Stopping all services...
echo.
%PYTHON% -Xutf8 -c "import sys; sys.path.insert(0, '.'); from devloop_runner import ServiceManager; m=ServiceManager(__import__('pathlib').Path('.'), __import__('pathlib').Path('logs')); m.stop_all()"
goto :end

:help
echo.
echo Jira Platform — Autonomous DevLoop
echo.
echo Usage: devloop.bat [action] [options]
echo.
echo Actions:
echo   run        Start the autonomous devloop ^(default^)
echo   build      Build all services and exit
echo   monitor    Monitor running services for 60s
echo   status     Show service health status
echo   stop       Stop all services
echo.
echo Options:
echo   --feature FILE       Feature file ^(default: feature.md^)
echo   --max-iterations N  Max loop iterations ^(default: 10^)
echo   --verbose           Detailed output
echo   --quiet             Minimal output
echo   --watch-only        Monitor only, skip build
echo   --skip-tests        Skip Maven tests during build
echo.
echo Examples:
echo   devloop.bat
echo   devloop.bat run --feature my-feature.md --max-iterations 5
echo   devloop.bat build --verbose
echo.
goto :end

:end
endlocal