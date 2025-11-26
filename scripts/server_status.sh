#!/bin/bash

echo "========================================"
echo "五子棋服务器状态"
echo "========================================"
echo ""

if [ ! -f "server.pid" ]; then
    echo "状态: 未运行"
    echo "提示: 使用 ./run_server_background.sh 启动服务器"
    echo ""
    exit 0
fi

PID=$(cat server.pid)

if ps -p $PID > /dev/null 2>&1; then
    echo "状态: 运行中"
    echo "PID: $PID"
    echo ""
    
    # 显示进程信息
    echo "进程信息:"
    ps -p $PID -o pid,ppid,cmd,%mem,%cpu,etime
    echo ""
    
    # 显示最近的日志
    if [ -f "logs/server.log" ]; then
        echo "最近日志 (最后10行):"
        echo "----------------------------------------"
        tail -10 logs/server.log
        echo "----------------------------------------"
        echo ""
        echo "完整日志: tail -f logs/server.log"
    fi
else
    echo "状态: 已停止"
    echo "警告: PID 文件存在但进程不存在"
    echo "提示: 运行 ./stop_server.sh 清理 PID 文件"
    echo ""
fi

echo "========================================"