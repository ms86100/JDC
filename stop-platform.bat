@echo off
setlocal enabledelayedexpansion

:: Jira Platform Enterprise Runtime Stopper
:: This script stops all services forcefully

title Jira Platform - Stopping

echo.
echo  ================================================================
echo  :                                                              :
echo  :          JIRA PLATFORM - SHUTTING DOWN                       :
echo  :                                                              :
echo  ================================================================
echo.

cd /d "%~dp0"

set "PID_DIR=%~dp0platform-runtime\pids"

echo [INFO] Aggressive cleanup - killing all service processes...
echo.

:: Step 1: Kill all Java processes (including ours)
echo [1/4] Stopping Java services...
taskkill /F /IM "java.exe" /T >nul 2>&1

:: Step 2: Kill Node.js and npm (frontend)
echo [2/4] Stopping Node.js/Frontend...
taskkill /F /IM "node.exe" >nul 2>&1
taskkill /F /IM "node" >nul 2>&1
taskkill /F /IM "npm.cmd" >nul 2>&1
taskkill /F /IM "npm" >nul 2>&1

:: Step 3: Kill processes by PID files
echo [3/4] Cleaning PID files...
if exist "%PID_DIR%" (
    for %%F in ("%PID_DIR%\*.pid") do (
        for /f "delims=" %%A in ('type "%%F"') do (
            echo [CLEANUP] Killing PID %%A...
            taskkill /F /PID %%A >nul 2>&1
        )
    )
    del /q "%PID_DIR%\*.pid" 2>nul
)

:: Step 4: Force kill anything on our ports
echo [4/4] Checking ports...
for %%P in (5432 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 3000) do (
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr :%%P ^| findstr LISTENING') do (
        echo [PORT %%P] Killing PID %%A...
        taskkill /F /PID %%A >nul 2>&1
    )
)

:: Final aggressive cleanup
timeout /t 1 /nobreak >nul
taskkill /F /IM "java.exe" /T >nul 2>&1
taskkill /F /IM "node.exe" >nul 2>&1

echo.
echo ================================================================
echo :                                                              :
echo :          All services stopped                                :
echo :                                                              :
echo :          Platform shutdown complete.                          :
echo :                                                              :
echo ================================================================
echo.

pause