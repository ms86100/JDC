@echo off
setlocal enabledelayedexpansion

:: Jira Platform Windows Service Installation
:: Uses NSSM (Non-Sucking Service Manager) to install as a Windows service
:: This allows the platform to start automatically on machine boot

title Jira Platform - Service Install
color 0B

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║                                                              ║
echo  ║          JIRA PLATFORM - SERVICE INSTALLER                  ║
echo  ║                                                              ║
echo  ╚══════════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"

:: Check for NSSM
set "NSSM=%~dp0platform-runtime\nssm\nssm.exe"
set "NSSM_URL=https://nssm.cc/release/nssm-2.24.zip"

if not exist "%NSSM%" (
    echo [INFO] NSSM not found. Downloading...
    echo.

    :: Create nssm directory
    if not exist "%~dp0platform-runtime\nssm" mkdir "%~dp0platform-runtime\nssm"

    :: Download NSSM (using PowerShell)
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '%NSSM_URL%' -OutFile '%~dp0platform-runtime\nssm.zip'"

    :: Extract (basic extraction)
    powershell -NoProfile -Command "Expand-Archive -Path '%~dp0platform-runtime\nssm.zip' -DestinationPath '%~dp0platform-runtime\nssm-temp' -Force"

    :: Find and move nssm.exe
    for /r "%~dp0platform-runtime\nssm-temp" %%F in (nssm.exe) do (
        move "%%F" "%~dp0platform-runtime\nssm\nssm.exe" >nul 2>&1
    )

    :: Cleanup
    rmdir /s /q "%~dp0platform-runtime\nssm-temp" 2>nul
    del "%~dp0platform-runtime\nssm.zip" 2>nul

    if not exist "%NSSM%" (
        echo [ERROR] Failed to download NSSM
        echo Please download manually from https://nssm.cc/download
        pause
        exit /b 1
    )

    echo [SUCCESS] NSSM installed
)

echo.
echo [INFO] Installing Jira Platform as Windows service...
echo.

:: Check if already installed
sc query "JiraPlatform" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [WARN] Service already installed
    echo To reinstall, first run uninstall-service.bat
    pause
    exit /b 1
)

:: Install service
"%NSSM%" install "JiraPlatform" "powershell.exe" "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File \"%~dp0start-platform.ps1\""

:: Configure service
"%NSSM%" set "JiraPlatform" DisplayName "Jira Platform Enterprise Runtime"
"%NSSM%" set "JiraPlatform" Description "Jira Platform Microservices - Enterprise Runtime Environment"
"%NSSM%" set "JiraPlatform" StartMode Automatic
"%NSSM%" set "JiraPlatform" ObjectName "LocalSystem"

:: Configure recovery actions
"%NSSM%" set "JiraPlatform" AppRestartDelay 60000

echo.
echo [SUCCESS] Service installed successfully!
echo.
echo  Service Name: JiraPlatform
echo  Startup: Automatic (starts with Windows)
echo.
echo  Commands:
echo    - net start JiraPlatform   (start service)
echo    - net stop JiraPlatform    (stop service)
echo    - uninstall-service.bat    (uninstall service)
echo.

pause