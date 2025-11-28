package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * 五子棋服务器
 * 负责监听端口、管理客户端连接、管理游戏房间
 */
public class Server {

    private int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, ClientHandler> clients; // 用户名 -> ClientHandler
    private Map<String, GameSession> gameSessions; // 房间ID -> GameSession
    private volatile boolean running;
    private int roomIdCounter; // 房间ID计数器
    private ScheduledExecutorService cleanupScheduler; // 房间清理调度器

    public Server(int port) {
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
        this.gameSessions = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.cleanupScheduler = Executors.newScheduledThreadPool(1);
        this.running = false;
        this.roomIdCounter = 1000;
    }

    /**
     * 启动服务器
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("服务器启动成功，监听端口: " + port);

            // 启动房间清理任务（每1秒检查一次）
            cleanupScheduler.scheduleAtFixedRate(
                    this::cleanupEmptyRooms,
                    1, 1, TimeUnit.SECONDS);

            // 接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("新客户端连接: " + clientSocket.getInetAddress());

                    // 创建客户端处理器并提交到线程池
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.execute(handler);

                } catch (IOException e) {
                    if (running) {
                        System.err.println("接受连接异常: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        running = false;

        // 关闭清理调度器
        cleanupScheduler.shutdown();

        // 关闭所有客户端连接
        for (ClientHandler client : clients.values()) {
            client.disconnect();
        }

        // 关闭线程池
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        // 关闭服务器套接字
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭服务器套接字异常: " + e.getMessage());
        }

        System.out.println("服务器已停止");
    }

    /**
     * 添加客户端
     */
    public synchronized void addClient(ClientHandler client) {
        clients.put(client.getUsername(), client);
        System.out.println("用户 " + client.getUsername() + " 已连接");
    }

    /**
     * 移除客户端
     */
    public synchronized void removeClient(ClientHandler client) {
        if (client.getUsername() != null) {
            clients.remove(client.getUsername());
            System.out.println("用户 " + client.getUsername() + " 已移除");
        }
    }

    /**
     * 检查用户名是否存在
     */
    public boolean isUsernameExists(String username) {
        return clients.containsKey(username);
    }

    /**
     * 生成房间ID（4位数字）
     */
    private synchronized String generateRoomId() {
        // 生成4位数字ID，从1000开始
        String roomId = String.format("%04d", roomIdCounter);
        roomIdCounter++;

        // 如果超过9999，重置为1000
        if (roomIdCounter > 9999) {
            roomIdCounter = 1000;
        }

        return roomId;
    }

    /**
     * 获取游戏会话
     */
    public GameSession getGameSession(String roomId) {
        return gameSessions.get(roomId);
    }

    /**
     * 移除游戏会话
     */
    public synchronized void removeGameSession(String roomId) {
        GameSession session = gameSessions.remove(roomId);
        if (session != null) {
            System.out.println("房间 " + roomId + " 已关闭");
        }
    }

    /**
     * 清理空房间
     */
    public synchronized void cleanupEmptyRooms() {
        gameSessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            if (session.isEmpty()) {
                System.out.println("自动清理空房间: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 获取在线客户端数量
     */
    public int getOnlineCount() {
        return clients.size();
    }

    /**
     * 获取活跃房间数量
     */
    public int getActiveRoomCount() {
        return gameSessions.size();
    }

    /**
     * 打印服务器状态
     */
    public void printStatus() {
        System.out.println("========== 服务器状态 ==========");
        System.out.println("在线用户数: " + getOnlineCount());
        System.out.println("活跃房间: " + getActiveRoomCount());
        System.out.println("==============================");
    }

    /**
     * 创建房间
     */
    public synchronized String createEmptyRoom(ClientHandler creator) {
        String roomId = generateRoomId();
        GameSession session = new GameSession(roomId, creator);
        gameSessions.put(roomId, session);

        System.out.println("用户 " + creator.getUsername() + " 创建房间: " + roomId);

        return roomId;
    }

    /**
     * 加入指定房间
     */
    public synchronized boolean joinRoom(String roomId, ClientHandler joiner) {
        GameSession session = gameSessions.get(roomId);

        if (session == null) {
            // 不在这里发送错误消息，由调用方决定
            return false;
        }

        // 添加成员到房间
        session.addMember(joiner);
        System.out.println("用户 " + joiner.getUsername() + " 加入房间: " + roomId);

        return true;
    }

    /**
     * 快速加入可用房间
     */
    public synchronized boolean quickJoinRoom(ClientHandler joiner) {
        // 查找第一个有空位的房间
        for (Map.Entry<String, GameSession> entry : gameSessions.entrySet()) {
            GameSession session = entry.getValue();
            // 加入房间
            session.addMember(joiner);
            System.out.println("用户 " + joiner.getUsername() + " 快速加入房间: " + entry.getKey());
            return true;
        }

        return false;
    }

    /**
     * 获取房间列表
     */
    public String getRoomList() {
        if (gameSessions.isEmpty()) {
            return "0"; // 没有房间
        }

        StringBuilder sb = new StringBuilder();
        sb.append(gameSessions.size()); // 房间数量

        for (Map.Entry<String, GameSession> entry : gameSessions.entrySet()) {
            String roomId = entry.getKey();
            GameSession session = entry.getValue();
            String status = session.getStatusText();
            int memberCount = session.getMemberCount();

            sb.append(Protocol.DELIMITER);
            sb.append(String.format("%s|%s|%d人", roomId, status, memberCount));
        }

        return sb.toString();
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;

        // 从命令行参数获取端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("无效的端口号，使用默认端口: " + Protocol.DEFAULT_PORT);
            }
        }

        // 创建并启动服务器
        Server server = new Server(port);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭服务器...");
            server.stop();
        }));

        // 启动状态监控线程
        Thread statusThread = new Thread(() -> {
            while (server.running) {
                try {
                    Thread.sleep(30000); // 每30秒打印一次状态
                    server.printStatus();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statusThread.setDaemon(true);
        statusThread.start();

        // 启动服务器（阻塞）
        server.start();
    }
}
