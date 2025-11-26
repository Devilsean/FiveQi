#!/bin/bash

# 切换到项目根目录
cd "$(dirname "$0")/.." || exit 1

echo "========================================"
echo "停止五子棋服务器"
echo "========================================"
echo ""

if [ ! -f "server.pid" ]; then
    echo "错误：找不到 server.pid 文件"
    echo "服务器可能未在后台运行"
    exit 1
fi

PID=$(cat server.pid)

if ! ps -p $PID > /dev/null 2>&1; then
    echo "警告：进程 $PID 不存在"
    rm server.pid
    echo "已清理 PID 文件"
    exit 0
fi

echo "正在停止服务器 (PID: $PID)..."
kill $PID

# 等待进程结束
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "服务器已停止"
        rm server.pid
        echo "========================================"
        exit 0
    fi
    sleep 1
done

# 如果还未停止，强制终止
echo "强制终止服务器..."
kill -9 $PID
rm server.pid
echo "服务器已强制停止"
echo "========================================"