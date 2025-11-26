#!/bin/bash

# 切换到项目根目录
cd "$(dirname "$0")/.." || exit 1

echo "========================================"
echo "编译五子棋项目"
echo "========================================"

# 检查 Java 是否安装
echo "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo ""
    echo "========================================"
    echo "错误- 未检测到 Java"
    echo "========================================"
    echo ""
    echo "请先安装 JDK 8 或更高版本"
    echo ""
    echo "安装方法："
    echo ""
    echo "Ubuntu/Debian:"
    echo "  sudo apt update"
    echo "  sudo apt install openjdk-11-jdk"
    echo ""
    echo "CentOS/RHEL:"
    echo "  sudo yum install java-11-openjdk-devel"
    echo ""
    echo "macOS:"
    echo "  brew install openjdk@11"
    echo ""
    echo "或从官网下载："
    echo "  Oracle JDK: https://www.oracle.com/java/technologies/downloads/"
    echo "  OpenJDK: https://adoptium.net/"
    echo ""
    exit 1
fi

if ! command -v javac &> /dev/null; then
    echo ""
    echo "========================================"
    echo "错误 - 未检测到 javac 编译器"
    echo "========================================"
    echo ""
    echo "您安装的可能是 JRE 而非 JDK"
    echo "请安装完整的 JDK 开发工具包"
    echo ""
    echo "安装方法："
    echo ""
    echo "Ubuntu/Debian:"
    echo "  sudo apt install openjdk-11-jdk"
    echo ""
    echo "CentOS/RHEL:"
    echo "  sudo yum install java-11-openjdk-devel"
    echo ""
    echo "macOS:"
    echo "  brew install openjdk@11"
    echo ""
    exit 1
fi

echo "Java 环境检测通过"
java -version 2>&1 | head -1
echo ""

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
    echo "  ./scripts/run_server.sh  启动服务器"
    echo "  ./scripts/run_client.sh  启动客户端"
    echo ""
else
    echo ""
    echo "========================================"
    echo "编译失败！请检查错误信息"
    echo "========================================"
    echo ""
    exit 1
fi