@echo off
REM ============================================
REM Jira Platform - Database Setup Script
REM ============================================

echo.
echo ============================================
echo Jira Platform - Database Setup
echo ============================================
echo.

REM Get database credentials from environment or use defaults
set DB_HOST=%DB_HOST%
set DB_PORT=%DB_PORT%
set DB_USER=%DB_USER%
set DB_PASS=%DB_PASS%

REM Set defaults if not provided
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_USER%"=="" set DB_USER=jiraadmin
if "%DB_PASS%"=="" set DB_PASS=jirapass123

echo Database Configuration:
echo   Host: %DB_HOST%:%DB_PORT%
echo   User: %DB_USER%
echo   Database: jira_platform
echo.

REM Check if psql is available
where psql >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: psql command not found. Please install PostgreSQL client.
    echo You can download it from: https://www.postgresql.org/download/
    exit /b 1
)

echo Step 1: Creating jira_platform database...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -c "DROP DATABASE IF EXISTS jira_platform;" 2>nul
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -c "CREATE DATABASE jira_platform;"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to create database. Check your PostgreSQL connection.
    exit /b 1
)
echo Database created successfully.
echo.

echo Step 2: Running consolidated migration...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d jira_platform -f "%~dp0consolidated-migration\V1__consolidated_init.sql"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to run migration. Check the error messages above.
    exit /b 1
)
echo Migration completed successfully.
echo.

echo ============================================
echo Setup Complete!
echo ============================================
echo.
echo Next steps:
echo   1. Rebuild all services: .\build-all.bat
echo   2. Start the platform: .\start-platform.bat
echo.
pause