@echo off
setlocal enabledelayedexpansion

:: Jira Platform Status Checker - HTTP Health Check Based
:: Checks actual service health instead of relying on PID files

title Jira Platform Status

cd /d "%~dp0"

echo.
echo  +================================================================+
echo  :                    JIRA PLATFORM STATUS                      :
echo  +================================================================+
echo.

:: Check Java
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    for /f "delims=" %%i in ('java -version 2^>^&1 ^| findstr "version"') do echo     Java: %%i
) else (
    echo     Java: NOT FOUND -ForegroundColor Red
)

echo.
echo   Service Status (via HTTP health check):
echo   -------------------------------------------------------------

:: Services to check
set "SERVICES=8081:auth-service:8081 8082:user-service:8082 8083:project-service:8083 8084:issue-service:8084 8085:workflow-service:8085 8086:comment-service:8086 8087:notification-service:8087 8088:search-service:8088 8089:audit-service:8089 8090:attachment-service:8090 8091:sprint-service:8091 8092:plan-service:8092 8093:admin-service:8093"

set GATEWAY_STATUS=0
set FRONTEND_STATUS=0

for %%S in (%SERVICES%) do (
    for /f "tokens=1,2 delims=:" %%A in ("%%S") do (
        set PORT=%%A
        set NAME=%%B

        :: Try HTTP health check
        curl -s -o nul -w "%%{http_code}" --connect-timeout 2 "http://localhost:%%A/actuator/health" >nul 2>&1
        set HTTP_STATUS=!ERRORLEVEL!

        if !HTTP_STATUS! equ 0 (
            echo     [RUNNING] %%B - Port %%A -ForegroundColor Green
        ) else (
            echo     [STOPPED] %%B - Port %%A -ForegroundColor Red
        )
    )
)

:: Check gateway
curl -s -o nul -w "" --connect-timeout 2 "http://localhost:8080/actuator/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo     [RUNNING] gateway - Port 8080 -ForegroundColor Green
) else (
    echo     [STOPPED] gateway - Port 8080 -ForegroundColor Red
)

:: Check frontend
curl -s -o nul -w "" --connect-timeout 2 "http://localhost:3000" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo     [RUNNING] frontend - Port 3000 -ForegroundColor Green
) else (
    echo     [STOPPED] frontend - Port 3000 -ForegroundColor Red
)

echo.
echo   Port Status:
echo   -------------------------------------------------------------

for %%P in (8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 3000 5432) do (
    netstat -ano | findstr :%%P | findstr LISTENING >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        for /f "tokens=5" %%A in ('netstat -ano ^| findstr :%%P ^| findstr LISTENING ^| findstr TCP') do (
            echo     Port %%P: IN USE ^(PID: %%A^)
        )
    ) else (
        echo     Port %%P: Available
    )
)

echo.
echo   Log Files:
echo   -------------------------------------------------------------

set "LOG_DIR=%~dp0platform-runtime\logs"
if exist "%LOG_DIR%" (
    for %%F in ("%LOG_DIR%\*.log") do (
        set "SIZE="
        for %%A in (%%~zF) do set "SIZE=%%A"
        echo     %%~nxF: !SIZE! bytes
    )
) else (
    echo     No log files yet
)

echo.
echo   +================================================================+
echo   :  To start: double-click start-platform.bat                      :
echo   :  To stop:  double-click stop-platform.bat                       :
echo   +================================================================+
echo.

pause