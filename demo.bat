@echo off
title Systems and Avionics - Demo Launch
cd /d "%~dp0"
echo.
echo  ================================================================
echo  :   SYSTEMS AND AVIONICS - DEMO LAUNCHER                       :
echo  :                                                              :
echo  :   Step 1: Stop any existing services                         :
echo  :   Step 2: Start all backend services (external DB)           :
echo  :   Step 3: Health check all services                          :
echo  :   Step 4: Open browser                                       :
echo  ================================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo-launch.ps1"

echo.
echo Press any key to exit...
pause >nul
