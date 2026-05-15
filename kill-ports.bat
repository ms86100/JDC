@echo off
setlocal enabledelayedexpansion

:: Emergency Port Kill - Kills ANY process on specific ports
:: Use this if stop-platform.bat doesn't work

title Emergency Kill

echo.
echo  ================================================================
echo  :  EMERGENCY PORT KILL                                          :
echo  :  This will force-kill any process on these ports             :
echo  ================================================================
echo.

for %%P in (8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 3000) do (
    echo Checking port %%P...
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr :%%P ^| findstr LISTENING') do (
        echo .
        echo *** FOUND: Port %%P used by PID %%A ***
        for /f "tokens=*" %%N in ('tasklist /FI "PID eq %%A" /NH') do echo *** Process: %%N ***
        echo [KILLING] taskkill /F /PID %%A
        taskkill /F /PID %%A
        echo .
    )
)

echo.
echo ================================================================
echo  Done. Run status.bat to verify.
echo ================================================================
pause