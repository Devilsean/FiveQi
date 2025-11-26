#!/bin/bash

echo "========================================"
echo "启动五子棋客户端"
echo "========================================"
echo ""

# 检查是否已编译
if [ ! -f "bin/client/GameGUI.class" ]; then
    echo "错误：项目尚未编译！"
    echo "请先运行 ./compile.sh 编译项目"
    echo ""
    exit 1
fi

# 启动客户端主菜单
echo "客户端启动中..."
echo ""

java -cp bin client.MainMenu

# 如果客户端异常退出，显示消息
if [ $? -ne 0 ]; then
    echo ""
    echo "客户端异常退出"
fi