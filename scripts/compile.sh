#!/bin/bash

# 切换到项目根目录
cd "$(dirname "$0")/.." || exit 1

echo "========================================"
echo "编译五子棋项目"
echo "========================================"

# 创建输出目录
if [ ! -d "bin" ]; then
    mkdir bin
fi

# 编译所有Java文件
echo "正在编译..."
javac -encoding UTF-8 -d bin -sourcepath src src/common/*.java src/server/*.java src/client/*.java

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "编译成功！"
    echo "========================================"
    echo ""
    echo "使用以下命令运行："
    echo "  启动服务器: ./run_server.sh"
    echo "  启动客户端: ./run_client.sh"
    echo ""
else
    echo ""
    echo "========================================"
    echo "编译失败！请检查错误信息"
    echo "========================================"
    echo ""
fi