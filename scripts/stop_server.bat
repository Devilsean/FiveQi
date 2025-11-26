@echo off
chcp 65001>nul
echo ========================================
echo Stopping FiveQi Server
echo ========================================
echo.

REM Find and kill Java process running server.Server
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo list ^| findstr /i "PID"') do (
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | findstr /i "server.Server" >nul
    if not errorlevel 1 (
        echo Found server process: PID %%i
        taskkill /PID %%i /F
        echo Server stopped
        goto :done
    )
)

echo No server process found
echo.
echo Note: This only works if server is running as java.exe process

:done
echo.
echo ========================================
pause