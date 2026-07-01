@echo off
:: Jira Platform Fast Enterprise Startup

title Jira Platform - Fast Start

echo.
echo  ================================================================
echo  :                                                              :
echo  :           JIRA PLATFORM - FAST STARTUP                        :
echo  :                                                              :
echo  ================================================================
echo.

cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quick-start.ps1"

echo.
echo Press any key to exit...
pause >nul