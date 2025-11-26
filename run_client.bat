@echo off
chcp 65001 >nul
echo ========================================
echo 启动五子棋客户端
echo ========================================
echo.

REM 检查是否已编译
if not exist "bin\client\GameGUI.class" (
    echo 错误：项目尚未编译！
    echo 请先运行 compile.bat 编译项目
    echo.
    pause
    exit /b 1
)

REM 启动客户端主菜单
echo 客户端启动中...
echo.

java -cp bin client.MainMenu

REM 如果客户端关闭，暂停以查看可能的错误信息
if %errorlevel% neq 0 (
    echo.
    echo 客户端异常退出
    pause
)