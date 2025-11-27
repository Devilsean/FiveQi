@echo off
chcp 65001>nul
echo ========================================
echo Compiling FiveQi Project
echo ========================================

REM Switch to project root directory
cd /d "%~dp0.."

REM Create output directory
if not exist "bin" mkdir bin

REM Check Java environment
echo Checking Java environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo Error: Java not detected
    echo ========================================
    echo.
    echo Please install JDK 8 or higher
    echo.
    echo Download:
    echo   Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo   OpenJDK: https://adoptium.net/
    echo.
    echo After installation, add Java to system PATH
    echo.
    pause
    exit /b 1
)

javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo Error: javac compiler not detected
    echo ========================================
    echo.
    echo You may have JRE instead of JDK
    echo Please install full JDK
    echo.
    echo Download:
    echo   Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo   OpenJDK: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Java environment OK
for /f "tokens=*" %%i in ('java -version 2^>^&1^| findstr /i "version"') do echo %%i
echo.

REM Compile all Java files
echo Compiling...
javac -encoding UTF-8 -d bin -sourcepath src src\common\Protocol.java src\server\ChessRule.java src\server\ClientHandler.java src\server\GameSession.java src\server\Server.java src\client\Client.java src\client\GameGUI.java src\client\LocalGameGUI.java src\client\MainMenu.java src\client\NetworkHandler.java src\client\RoomLobbyGUI.java src\client\Theme.java

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Compilation Successful
    echo ========================================
    echo.
    echo Run with:
    echo   scripts\run_server.bat
    echo   scripts\run_client.bat
    echo.
) else (
    echo.
    echo ========================================
    echo Compilation Failed - Check errors above
    echo ========================================
    echo.
)

pause