@echo off
setlocal enabledelayedexpansion

:: Jira Platform Windows Service Uninstall
:: Removes the Windows service

title Jira Platform - Service Uninstall
color 0C

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║                                                              ║
echo  ║          JIRA PLATFORM - SERVICE UNINSTALLER                 ║
echo  ║                                                              ║
echo  ╚══════════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"

:: Stop service if running
echo [INFO] Stopping service...
net stop "JiraPlatform" >nul 2>&1

:: Remove service
set "NSSM=%~dp0platform-runtime\nssm\nssm.exe"
if exist "%NSSM%" (
    "%NSSM%" remove "JiraPlatform" confirm
) else (
    sc delete "JiraPlatform"
)

echo.
echo [SUCCESS] Service uninstalled
echo.

pause