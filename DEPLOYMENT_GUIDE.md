# 五子棋服务器部署指南

本指南详细介绍如何在 Linux 服务器上部署五子棋服务器。

## 目录

- [环境准备](#环境准备)
- [上传项目](#上传项目)
- [编译部署](#编译部署)
- [运行方式](#运行方式)
- [systemd 服务配置](#systemd-服务配置)
- [防火墙配置](#防火墙配置)
- [常见问题](#常见问题)

## 环境准备

### 1. 检查系统信息

```bash
# 查看系统版本
cat /etc/os-release

# 查看内核版本
uname -a
```

### 2. 安装 JDK

**Ubuntu/Debian:**

```bash
# 更新包列表
sudo apt update

# 安装 OpenJDK 11
sudo apt install openjdk-11-jdk -y

# 验证安装
java -version
javac -version
```

**CentOS/RHEL:**

```bash
# 安装 OpenJDK 11
sudo yum install java-11-openjdk-devel -y

# 验证安装
java -version
javac -version
```

### 3. 配置环境变量（可选）

```bash
# 编辑 ~/.bashrc 或 /etc/profile
vim ~/.bashrc

# 添加以下内容
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 使配置生效
source ~/.bashrc
```

## 上传项目

### 方式一：使用 SCP

```bash
# 在本地机器上执行
# 压缩项目
tar -czf FiveQi.tar.gz FiveQi/

# 上传到服务器
scp FiveQi.tar.gz user@server-ip:/home/user/

# SSH 登录服务器
ssh user@server-ip

# 解压项目
cd /home/user
tar -xzf FiveQi.tar.gz
cd FiveQi
```

### 方式二：使用 Git

```bash
# SSH 登录服务器
ssh user@server-ip

# 克隆项目
cd /home/user
git clone <your-repository-url> FiveQi
cd FiveQi
```

### 方式三：使用 SFTP

```bash
# 使用 FileZilla、WinSCP 等工具直接上传
# 目标路径：/home/user/FiveQi
```

## 编译部署

### 1. 设置脚本权限

```bash
cd /home/user/FiveQi
chmod +x scripts/*.sh
```

### 2. 编译项目

```bash
# 运行编译脚本（会自动检测 Java 环境）
./scripts/compile.sh

# 或从 scripts 目录运行
cd scripts
./compile.sh
```

### 3. 验证编译结果

```bash
# 检查 bin 目录
ls -la bin/
ls -la bin/server/
ls -la bin/client/
ls -la bin/common/

# 应该看到 .class 文件
```

## 运行方式

### 方式一：前台运行（测试用）

```bash
# 从项目根目录运行
./scripts/run_server.sh

# 或从 scripts 目录运行
cd scripts
./run_server.sh

# 停止：按 Ctrl+C
```

**特点：**

- ✅ 实时查看日志输出
- ✅ 方便调试
- ❌ SSH 断开后服务停止
- ❌ 不适合生产环境

### 方式二：后台运行（推荐）

```bash
# 启动服务器
./scripts/run_server_background.sh

# 查看状态
./scripts/server_status.sh

# 查看日志
tail -f logs/server.log

# 停止服务器
./scripts/stop_server.sh
```

**特点：**

- ✅ 后台运行
- ✅ SSH 断开后继续运行
- ✅ 日志保存到文件
- ✅ 方便管理
- ✅ 适合生产环境

### 方式三：使用 screen（备选）

```bash
# 安装 screen
sudo apt install screen  # Ubuntu/Debian
sudo yum install screen  # CentOS/RHEL

# 创建 screen 会话
screen -S fiveqi-server

# 启动服务器
cd /home/user/FiveQi
./scripts/run_server.sh

# 分离会话：按 Ctrl+A 然后按 D

# 重新连接会话
screen -r fiveqi-server

# 查看所有会话
screen -ls

# 终止会话（在会话内）
exit
```

### 方式四：使用 systemd 服务（最推荐）

见下一节详细说明。

## systemd 服务配置

### 1. 创建服务文件

项目已包含服务配置文件 `scripts/fiveqi.service`，需要根据实际路径修改：

```bash
# 编辑服务文件
vim scripts/fiveqi.service
```

修改以下内容：

```ini
[Unit]
Description=FiveQi Game Server
After=network.target

[Service]
Type=simple
User=your-username              # 改为实际用户名
WorkingDirectory=/home/user/FiveQi  # 改为实际项目路径
ExecStart=/usr/bin/java -cp /home/user/FiveQi/bin server.Server  # 改为实际路径
Restart=on-failure
RestartSec=10
StandardOutput=append:/home/user/FiveQi/logs/server.log  # 改为实际路径
StandardError=append:/home/user/FiveQi/logs/server.log   # 改为实际路径

[Install]
WantedBy=multi-user.target
```

### 2. 安装服务

```bash
# 复制服务文件到 systemd 目录
sudo cp scripts/fiveqi.service /etc/systemd/system/

# 重新加载 systemd 配置
sudo systemctl daemon-reload

# 启用服务（开机自启）
sudo systemctl enable fiveqi

# 启动服务
sudo systemctl start fiveqi
```

### 3. 管理服务

```bash
# 查看服务状态
sudo systemctl status fiveqi

# 启动服务
sudo systemctl start fiveqi

# 停止服务
sudo systemctl stop fiveqi

# 重启服务
sudo systemctl restart fiveqi

# 查看日志
sudo journalctl -u fiveqi -f

# 查看最近 100 行日志
sudo journalctl -u fiveqi -n 100

# 禁用开机自启
sudo systemctl disable fiveqi
```

### 4. 创建日志目录

```bash
# 确保日志目录存在
mkdir -p /home/user/FiveQi/logs
```

## 防火墙配置

### UFW (Ubuntu/Debian)

```bash
# 检查防火墙状态
sudo ufw status

# 开放 8888 端口
sudo ufw allow 8888/tcp

# 如果防火墙未启用，启用它
sudo ufw enable

# 再次检查状态
sudo ufw status
```

### firewalld (CentOS/RHEL)

```bash
# 检查防火墙状态
sudo firewall-cmd --state

# 开放 8888 端口
sudo firewall-cmd --permanent --add-port=8888/tcp

# 重新加载防火墙规则
sudo firewall-cmd --reload

# 查看已开放的端口
sudo firewall-cmd --list-ports
```

### iptables（传统方式）

```bash
# 开放 8888 端口
sudo iptables -A INPUT -p tcp --dport 8888 -j ACCEPT

# 保存规则
sudo iptables-save > /etc/iptables/rules.v4
```

### 云服务器安全组

如果使用华为云、阿里云、腾讯云、AWS 等云服务器，还需要在控制台配置安全组规则：

1. 登录云服务器控制台
2. 找到"安全组"设置
3. 添加入站规则：
   - 协议：TCP
   - 端口：8888
   - 源地址：0.0.0.0/0（允许所有 IP）或指定 IP 段

## 监控和维护

### 1. 查看服务器状态

```bash
# 使用脚本查看状态
./scripts/server_status.sh

# 查看进程
ps aux | grep java

# 查看端口占用
netstat -tlnp | grep 8888
# 或
ss -tlnp | grep 8888
```

### 2. 查看日志

```bash
# 实时查看日志
tail -f logs/server.log

# 查看最近 100 行
tail -n 100 logs/server.log

# 搜索错误
grep -i error logs/server.log

# 查看特定日期的日志
grep "2025-11-26" logs/server.log
```

### 3. 日志轮转（可选）

创建 logrotate 配置防止日志文件过大：

```bash
# 创建配置文件
sudo vim /etc/logrotate.d/fiveqi
```

添加以下内容：

```
/home/user/FiveQi/logs/server.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0644 your-username your-username
}
```

### 4. 性能监控

```bash
# 查看系统资源使用
top

# 查看 Java 进程资源使用
top -p $(pgrep -f 'server.Server')

# 查看内存使用
free -h

# 查看磁盘使用
df -h
```

## 常见问题

### 1. 端口被占用

```bash
# 查找占用 8888 端口的进程
sudo lsof -i :8888
sudo netstat -tlnp | grep 8888

# 杀死进程
sudo kill -9 <PID>
```

### 2. 权限问题

```bash
# 给予脚本执行权限
chmod +x scripts/*.sh

# 给予日志目录写权限
chmod 755 logs/
```

### 3. Java 版本问题

```bash
# 查看已安装的 Java 版本
update-alternatives --list java

# 切换 Java 版本
sudo update-alternatives --config java
```

### 4. 服务无法启动

```bash
# 查看详细错误日志
sudo journalctl -u fiveqi -n 50

# 检查服务文件语法
sudo systemd-analyze verify /etc/systemd/system/fiveqi.service

# 检查 Java 环境
which java
java -version
```

### 5. 客户端无法连接

**检查清单：**

1. 服务器是否正在运行？

   ```bash
   ./scripts/server_status.sh
   ```

2. 防火墙是否开放端口？

   ```bash
   sudo ufw status
   ```

3. 云服务器安全组是否配置？
   - 登录云服务器控制台检查

4. 客户端 IP 地址是否正确？
   - 应该使用服务器的公网 IP

5. 网络是否通畅？

   ```bash
   # 在客户端机器上测试
   telnet server-ip 8888
   # 或
   nc -zv server-ip 8888
   ```

## 更新部署

### 更新代码

```bash
# 方式一：Git 拉取
cd /home/user/FiveQi
git pull

# 方式二：重新上传
# 使用 SCP 或 SFTP 上传新代码
```

### 重新编译和部署

```bash
# 停止服务
sudo systemctl stop fiveqi
# 或
./scripts/stop_server.sh

# 重新编译
./scripts/compile.sh

# 启动服务
sudo systemctl start fiveqi
# 或
./scripts/run_server_background.sh
```

## 备份和恢复

### 备份

```bash
# 备份整个项目
cd /home/user
tar -czf fiveqi-backup-$(date +%Y%m%d).tar.gz FiveQi/

# 备份日志
tar -czf fiveqi-logs-$(date +%Y%m%d).tar.gz FiveQi/logs/
```

### 恢复

```bash
# 解压备份
tar -xzf fiveqi-backup-20240101.tar.gz

# 恢复权限
chmod +x FiveQi/scripts/*.sh
```

## 性能优化

### JVM 参数调优

编辑服务文件或启动脚本，添加 JVM 参数：

```bash
java -Xms512m -Xmx1024m -XX:+UseG1GC -cp bin server.Server
```

参数说明：

- `-Xms512m`：初始堆大小 512MB
- `-Xmx1024m`：最大堆大小 1024MB
- `-XX:+UseG1GC`：使用 G1 垃圾回收器

### 系统优化

```bash
# 增加文件描述符限制
sudo vim /etc/security/limits.conf

# 添加以下内容
* soft nofile 65536
* hard nofile 65536
```

## 联系支持

如遇到问题，请检查：

1. 服务器日志：`logs/server.log`
2. 系统日志：`sudo journalctl -u fiveqi`
3. 网络连接：防火墙和安全组设置

---

**祝您部署顺利！** 🎮
