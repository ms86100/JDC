@echo off
REM Start PostgreSQL for Jira Platform (local install or Docker)
cd /d "%~dp0"
python -c "from launcher import check_port, start_postgres_local_windows, start_postgres_docker, is_docker_running, load_config, find_windows_postgres; import sys; from pathlib import Path; BASE=Path(r'%~dp0').resolve(); LOGS=BASE/'logs'; LOGS.mkdir(exist_ok=True); ok=check_port(5432); 
if not ok and is_docker_running(): ok=start_postgres_docker(load_config(), LOGS)
if not ok and find_windows_postgres()[0]: ok=start_postgres_local_windows(str(LOGS/'postgres-local.log'))
sys.exit(0 if ok or check_port(5432) else 1)"
if %ERRORLEVEL% equ 0 (
  echo PostgreSQL is running on port 5432.
) else (
  echo Failed to start PostgreSQL. Try Admin: Start-Service postgresql-x64-17
  exit /b 1
)
