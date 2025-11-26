@echo off
chcp 65001 >nul
echo ========================================
echo 编译五子棋项目
echo ========================================

REM 创建输出目录
if not exist "bin" mkdir bin

REM 编译所有Java文件
echo 正在编译...
javac -encoding UTF-8 -d bin -sourcepath src src/common/*.java src/server/*.java src/client/*.java

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo 编译成功！
    echo ========================================
    echo.
    echo 使用以下命令运行：
    echo   启动服务器: run_server.bat
    echo   启动客户端: run_client.bat
    echo.
) else (
    echo.
    echo ========================================
    echo 编译失败！请检查错误信息
    echo ========================================
    echo.
)

pause