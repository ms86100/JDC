@echo off
setlocal enabledelayedexpansion

:: Jira Platform Complete Launch
:: Builds (if needed), starts all services, opens browser
:: NO DOCKER - Pure Java/Node microservices

title Jira Platform
color 0A

echo.
echo  ================================================================
echo  ||                                                          ||
echo  ||              JIRA PLATFORM ENTERPRISE                    ||
echo  ||                                                          ||
echo  ||              Enterprise Runtime Environment              ||
echo  ||                                                          ||
echo  ================================================================
echo.

cd /d "%~dp0"

set "RUNTIME_DIR=%~dp0platform-runtime"
set "LOG_DIR=%RUNTIME_DIR%\logs"

:: Create directories
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: Log file for this session
set "SESSION_LOG=%LOG_DIR%\launch-%date:~-4%%date:~4,2%%date:~7,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "SESSION_LOG=%SESSION_LOG: =0%"

echo [INFO] Session log: %SESSION_LOG%

:: ============================================================
:: STEP 1: Check Prerequisites
:: ============================================================
echo [STEP 1/5] Checking prerequisites...

:: Check Java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found!
    echo Please install Java JDK 11+ from https://adoptium.net/
    pause
    exit /b 1
)
echo         - Java: OK

:: Check Node.js
where node >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [WARN] Node.js not found - frontend will not start
) else (
    echo         - Node.js: OK
)

:: Check Maven
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [WARN] Maven not found - build step will be skipped
) else (
    echo         - Maven: OK
)

:: ============================================================
:: STEP 2: Stop any existing services
:: ============================================================
echo.
echo [STEP 2/5] Stopping existing services...

call stop-platform.bat >nul 2>&1

timeout /t 3 /nobreak >nul
echo         - Cleanup complete

:: ============================================================
:: STEP 3: Build if needed
:: ============================================================
echo.
echo [STEP 3/5] Checking services...

:: Check if JARs exist
set JAR_MISSING=0

for %%S in (auth user project issue workflow comment notification search audit attachment sprint plan admin gateway migration) do (
    set "JAR_FOUND=0"
    for /r "jira-%%S-service\target" %%F in (*.jar) do (
        if not "%%~nF"=="" set "JAR_FOUND=1"
    )
    if "!JAR_FOUND!"=="0" (
        echo         - jira-%%S-service: JAR not found (run mvn package)
        set JAR_MISSING=1
    )
)

if "%JAR_MISSING%"=="1" (
    echo.
    echo [INFO] Some JARs are missing. Run build-all.bat to build all services.
    echo [INFO] Starting with available JARs...
)

:: ============================================================
:: STEP 4: Start Runtime Manager
:: ============================================================
echo.
echo [STEP 4/5] Starting runtime manager...

:: Use PowerShell to launch runtime manager in background
start "" /min cmd /c "powershell -NoProfile -ExecutionPolicy Bypass -File \"%~dp0start-platform.ps1\""

echo         - Runtime manager launched (check logs)

:: ============================================================
:: STEP 5: Wait and open browser
:: ============================================================
echo.
echo [STEP 5/5] Initializing services...
echo.

echo  ================================================================
echo  ||                                                          ||
echo  ||  Platform is starting in the background...               ||
echo  ||                                                          ||
echo  ||  Log files: platform-runtime\logs\                      ||
echo  ||  Status: double-click status.bat                        ||
echo  ||  Stop: double-click stop-platform.bat                   ||
echo  ||                                                          ||
echo  ||  Opening browser in 5 seconds...                         ||
echo  ||                                                          ||
echo  ================================================================

:: Wait for services to be ready
timeout /t 5 /nobreak >nul

:: Check if gateway is ready
set GATEWAY_READY=0
for /L %%I in (1,1,30) do (
    curl -s -o nul -w "%%{http_code}" http://localhost:8080/actuator/health >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set GATEWAY_READY=1
        goto :gateway_found
    )
    timeout /t 2 /nobreak >nul
)

:gateway_found
if "%GATEWAY_READY%"=="1" (
    echo.
    echo [SUCCESS] Gateway is ready!
    start http://localhost:8080
) else (
    echo.
    echo [INFO] Services starting - browser will open when ready
    echo [INFO] Manually open: http://localhost:8080
)

echo.
echo [DONE] Platform launcher started
echo.
echo Press any key to close this window...
timeout /t 10 /nobreak >nul

exit