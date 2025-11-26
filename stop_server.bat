@echo off
chcp 65001 >nul
echo ========================================
echo 停止五子棋服务器
echo ========================================
echo.

REM 查找并终止服务器进程
tasklist /FI "WINDOWTITLE eq 五子棋服务器*" 2>nul | find /I "java.exe" >nul
if %errorlevel% equ 0 (
    echo 找到服务器进程，正在停止...
    taskkill /FI "WINDOWTITLE eq 五子棋服务器*" /F >nul 2>&1
)

REM 通过命令行参数查找
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /FO LIST ^| find "PID:"') do (
    wmic process where "ProcessId=%%i" get CommandLine 2>nul | find "server.Server" >nul
    if not errorlevel 1 (
        echo 找到服务器进程 PID: %%i
        echo 正在停止服务器...
        taskkill /PID %%i /F >nul 2>&1
        if %errorlevel% equ 0 (
            echo.
            echo ========================================
            echo 服务器已成功停止
            echo ========================================
            echo.
            goto :end
        )
    )
)

echo 没有找到正在运行的服务器进程
echo.

:end
pause