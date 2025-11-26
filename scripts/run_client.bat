@echo off
chcp 65001>nul
echo ========================================
echo Starting FiveQi Client
echo ========================================

REM Switch to project root directory
cd /d "%~dp0.."
echo.

REM Check if compiled
if not exist "bin\client\GameGUI.class" (
    echo Error: Project not compiled
    echo Please run compile.bat first
    echo.
    pause
    exit /b 1
)

REM Start client main menu
echo Client starting...
echo.

java -cp bin client.MainMenu

REM Pause if client exits with error
if %errorlevel% neq 0 (
    echo.
    echo Client exited abnormally
    pause
)