@echo off
chcp 65001>nul
echo ========================================
echo Starting FiveQi Server
echo ========================================

REM Switch to project root directory
cd /d "%~dp0.."
echo.

REM Check if compiled
if not exist "bin\server\Server.class" (
    echo Error: Project not compiled
    echo Please run compile.bat first
    echo.
    pause
    exit /b 1
)

REM Start server (default port 8888)
echo Server starting...
echo Default port: 8888
echo Press Ctrl+C to stop
echo.
echo ========================================
echo.

java -cp bin server.Server

pause