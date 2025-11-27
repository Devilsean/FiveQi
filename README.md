# 五子棋联机对战系统

一个基于 Java 的五子棋联机对战游戏，支持多人在线对战、观战和房间管理功能。

## 功能特性

- ✅ 联机对战：支持多人在线对战
- ✅ 房间系统：创建房间、加入房间、快速匹配
- ✅ 席位管理：黑棋席、白棋席、观战席自由切换
- ✅ 实时聊天：房间内聊天功能
- ✅ 游戏状态同步：实时同步棋盘状态
- ✅ 连续对战：游戏结束后可在同一房间继续对战

## 项目结构

```
FiveQi/
├── src/                    # 源代码
│   ├── client/            # 客户端代码
│   ├── server/            # 服务器代码
│   └── common/            # 公共代码
├── scripts/               # 脚本文件
│   ├── compile.bat        # Windows 编译脚本
│   ├── compile.sh         # Linux 编译脚本
│   ├── run_server.bat     # Windows 启动服务器
│   ├── run_server.sh      # Linux 启动服务器
│   ├── run_client.bat     # Windows 启动客户端
│   ├── run_client.sh      # Linux 启动客户端
│   ├── run_server_background.sh  # Linux 后台启动
│   ├── stop_server.sh     # Linux 停止服务器
│   ├── server_status.sh   # Linux 查看状态
│   └── fiveqi.service     # systemd 服务配置
├── bin/                   # 编译输出目录
├── logs/                  # 日志目录（运行时生成）
├── README.md              # 项目说明
└── DEPLOYMENT_GUIDE.md    # 部署指南
```

## 快速开始

### 环境要求

- **JDK 8 或更高版本**（必须是 JDK，不是 JRE）
- Windows 或 Linux 操作系统

### 依赖安装

#### Windows

1. **下载 JDK**
   - Oracle JDK: <https://www.oracle.com/java/technologies/downloads/>
   - OpenJDK (推荐): <https://adoptium.net/>

2. **安装 JDK**
   - 运行下载的安装程序
   - 记住安装路径（如 `C:\Program Files\Java\jdk-11`）

3. **配置环境变量**
   - 右键"此电脑" → "属性" → "高级系统设置" → "环境变量"
   - 新建系统变量 `JAVA_HOME`，值为JDK 安装路径
   - 编辑 `Path` 变量，添加 `%JAVA_HOME%\bin`

4. **验证安装**

   ```bash
   java -version
   javac -version
   ```

#### Linux

**Ubuntu/Debian:**

```bash
sudo apt update
sudo apt install openjdk-11-jdk
```

**CentOS/RHEL:**

```bash
sudo yum install java-11-openjdk-devel
```

**验证安装:**

```bash
java -version
javac -version
```

### 本地运行

> **注意**：
>
> - 所有脚本都支持从项目根目录或 scripts 目录运行
> - 编译脚本会自动检测 Java 环境，如未安装会给出安装提示

#### 1. 编译项目

**Windows:**

```bash
# 从项目根目录运行
scripts\compile.bat

# 或从 scripts 目录运行
cd scripts
compile.bat
```

**Linux:**

```bash
# 设置执行权限（首次运行）
chmod +x scripts/*.sh

# 从项目根目录运行
scripts/compile.sh

# 或从 scripts 目录运行
cd scripts
./compile.sh
```

#### 2. 启动服务器

**Windows:**

```bash
# 从项目根目录运行
scripts\run_server.bat

# 或从 scripts 目录运行
cd scripts
run_server.bat
```

**Linux:**

```bash
# 从项目根目录运行
scripts/run_server.sh

# 或从 scripts 目录运行
cd scripts
./run_server.sh
```

服务器默认端口：8888

#### 3. 启动客户端

**Windows:**

```bash
# 从项目根目录运行
scripts\run_client.bat

# 或从 scripts 目录运行
cd scripts
run_client.bat
```

**Linux:**

```bash
# 从项目根目录运行
scripts/run_client.sh

# 或从 scripts 目录运行
cd scripts
./run_client.sh
```

### 远程服务器部署

如需在 Linux 远程服务器上部署，请查看 **[部署指南](DEPLOYMENT_GUIDE.md)**

#### 快速部署（Linux）

```bash
# 1. 上传项目到服务器
scp -r FiveQi/ user@server-ip:/home/user/

# 2. SSH 连接并编译
ssh user@server-ip
cd /home/user/FiveQi
chmod +x scripts/*.sh
scripts/compile.sh

# 3. 后台启动服务器
scripts/run_server_background.sh

# 4. 查看状态
scripts/server_status.sh

# 5. 开放防火墙端口
sudo ufw allow 8888/tcp
```

## 使用说明

### 游戏流程

1. **登录** - 输入用户名连接服务器
2. **进入房间** - 创建房间或加入现有房间
3. **选择席位** - 坐下黑棋席或白棋席
4. **开始游戏** - 双方就位后发起对战
5. **继续对战** - 游戏结束后可继续下一局

### 房间操作

- **创建房间** - 创建新房间（房间ID：1000-9999）
- **快速加入** - 随机加入可用房间
- **指定加入** - 通过房间ID加入
- **刷新列表** - 更新房间列表

### 席位切换

- 观战席 → 对战席：点击"坐下黑棋席"或"坐下白棋席"
- 对战席 → 观战席：点击"进入观战席"
- 对战席互换：先进入观战席，再坐下另一席位

## 技术架构

- **架构模式**：客户端-服务器架构
- **通信协议**：TCP Socket + 文本协议
- **并发处理**：多线程，每个客户端独立线程
- **数据同步**：请求-响应 + 推送模式

## 开发说明

### 添加新功能

1. 在 `Protocol.java` 中定义协议常量
2. 在 `ClientHandler.java` 中添加服务器处理逻辑
3. 在 `Client.java` 中添加客户端消息解析
4. 在 GUI 类中实现界面功能

### 调试技巧

- 查看控制台 DEBUG 信息
- 检查服务器状态（每30秒自动打印）
- 使用日志文件追踪问题

## 常见问题

**Q: 如何在远程服务器上持续运行？**  
A: 使用 `scripts/run_server_background.sh` 后台启动，或配置 systemd 服务。详见 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

**Q: 客户端无法连接服务器？**  
A: 检查服务器IP、端口、防火墙设置。远程服务器需开放8888端口。

**Q: 如何修改服务器端口？**  
A: 修改 `src/server/Server.java` 中的端口配置并重新编译。

**Q: 如何查看服务器日志？**  
A: 后台运行时日志在 `logs/server.log`，使用 `tail -f logs/server.log` 实时查看。

## 许可证

本项目仅供学习交流使用。

## 联系方式

如有问题或建议，欢迎反馈。
