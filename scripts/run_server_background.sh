#!/bin/bash

echo "========================================"
echo "后台启动五子棋服务器"
echo "========================================"
echo ""

# 检查是否已编译
if [ ! -f "bin/server/Server.class" ]; then
    echo "错误：项目尚未编译！"
    echo "请先运行 ./compile.sh 编译项目"
    echo ""
    exit 1
fi

# 创建logs目录
mkdir -p logs

# 检查是否已经在运行
if [ -f "server.pid" ]; then
    PID=$(cat server.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo "错误：服务器已在运行 (PID: $PID)"
        echo "请先运行 ./stop_server.sh 停止服务器"
        exit 1
    fi
fi

# 后台启动服务器
echo "服务器后台启动中..."
echo "默认端口: 8888"
echo "日志文件: logs/server.log"
echo ""

nohup java -cp bin server.Server > logs/server.log 2>&1 &
echo $! > server.pid

echo "服务器已启动 (PID: $(cat server.pid))"
echo ""
echo "查看日志: tail -f logs/server.log"
echo "停止服务器: ./stop_server.sh"
echo "查看状态: ./server_status.sh"
echo "========================================"