@echo off
setlocal enabledelayedexpansion

:: Jira Platform Build Script
:: Builds all microservices and prepares the platform for startup

title Jira Platform Builder

echo.
echo  +================================================================+
echo  :                                                                :
echo  :          JIRA PLATFORM BUILD SCRIPT                           :
echo  :                                                                :
echo  +================================================================+
echo.

cd /d "%~dp0"

:: Check for Java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java JDK 11 or higher from https://adoptium.net/
    pause
    exit /b 1
)

echo [INFO] Java found
java -version 2>&1 | findstr "version"
echo.

:: Check for Maven
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo [INFO] Maven found
mvn -version | findstr "Apache"
echo.

:: Build each service
set "SERVICES=jira-auth-service jira-user-service jira-project-service jira-issue-service jira-workflow-service jira-comment-service jira-notification-service jira-search-service jira-audit-service jira-attachment-service jira-sprint-service jira-plan-service jira-admin-service jira-gateway jira-migration-service jira-test-service"

echo [INFO] Building all microservices...
echo.

set BUILD_SUCCESS=0
set BUILD_FAILED=0

for %%S in (%SERVICES%) do (
    echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    echo  [BUILD] %%S
    echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    if exist "%%S\pom.xml" (
        cd %%S
        call mvn clean package "-Dmaven.test.skip=true" -q
        if %ERRORLEVEL% equ 0 (
            echo [SUCCESS] %%S built successfully
            set /a BUILD_SUCCESS+=1
        ) else (
            echo [FAILED] %%S build failed
            set /a BUILD_FAILED+=1
        )
        cd ..
    ) else (
        echo [SKIP] %%S not found
    )
    echo.
)

echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo   BUILD SUMMARY
echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo.
echo   Successful: %BUILD_SUCCESS%
echo   Failed: %BUILD_FAILED%
echo.

:: Build frontend
echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo  [BUILD] Frontend (npm install)
echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

cd jira-frontend
call npm install --silent
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Frontend dependencies installed
) else (
    echo [WARN] Frontend npm install had issues (may be ok if already installed)
)
cd ..

echo.
echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo   BUILD COMPLETE
echo  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
echo.
echo   You can now run start-platform.bat to start all services
echo.

pause