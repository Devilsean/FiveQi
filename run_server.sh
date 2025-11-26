#!/bin/bash

echo "========================================"
echo "启动五子棋服务器"
echo "========================================"
echo ""

# 检查是否已编译
if [ ! -f "bin/server/Server.class" ]; then
    echo "错误：项目尚未编译！"
    echo "请先运行 ./compile.sh 编译项目"
    echo ""
    exit 1
fi

# 启动服务器（默认端口8888）
echo "服务器启动中..."
echo "默认端口: 8888"
echo "按 Ctrl+C 停止服务器"
echo ""
echo "========================================"
echo ""

java -cp bin server.Server