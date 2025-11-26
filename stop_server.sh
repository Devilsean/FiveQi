#!/bin/bash

echo "========================================"
echo "停止五子棋服务器"
echo "========================================"
echo ""

# 查找服务器进程
SERVER_PID=$(ps aux | grep "java -cp bin server.Server" | grep -v grep | awk '{print $2}')

if [ -z "$SERVER_PID" ]; then
    echo "没有找到正在运行的服务器进程"
    echo ""
    exit 0
fi

echo "找到服务器进程: PID $SERVER_PID"
echo "正在停止服务器..."

# 尝试优雅关闭
kill $SERVER_PID

# 等待最多5秒
for i in {1..5}; do
    sleep 1
    if ! ps -p $SERVER_PID > /dev/null 2>&1; then
        echo ""
        echo "========================================"
        echo "服务器已成功停止"
        echo "========================================"
        echo ""
        exit 0
    fi
    echo "等待服务器关闭... ($i/5)"
done

# 如果还没关闭，强制终止
if ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "服务器未响应，强制终止..."
    kill -9 $SERVER_PID
    sleep 1
    
    if ! ps -p $SERVER_PID > /dev/null 2>&1; then
        echo ""
        echo "========================================"
        echo "服务器已强制停止"
        echo "========================================"
        echo ""
    else
        echo ""
        echo "========================================"
        echo "错误：无法停止服务器进程"
        echo "========================================"
        echo ""
        exit 1
    fi
fi