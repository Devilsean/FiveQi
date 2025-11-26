package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 客户端处理器
 * 负责单个客户端的消息读取、发送和指令解析
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private Server server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private String role; // PLAYER_BLACK, PLAYER_WHITE, SPECTATOR
    private GameSession gameSession;
    private volatile boolean running;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.running = true;

        try {
            // 使用 UTF-8 编码
            this.reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true // 自动刷新
            );
        } catch (IOException e) {
            System.err.println("创建客户端处理器失败: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = reader.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("客户端 " + username + " 连接异常: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    /**
     * 处理客户端消息
     */
    private void handleMessage(String message) {
        String[] parts = Protocol.parseMessage(message);
        if (parts.length == 0) {
            return;
        }

        String command = parts[0];

        try {
            switch (command) {
                case Protocol.LOGIN:
                    handleLogin(parts);
                    break;

                case Protocol.MOVE:
                    handleMove(parts);
                    break;

                case Protocol.CHAT:
                    handleChat(parts);
                    break;

                case Protocol.SIT_BLACK:
                    handleSitBlack();
                    break;

                case Protocol.SIT_WHITE:
                    handleSitWhite();
                    break;

                case Protocol.SIT_SPECTATOR:
                    handleSitSpectator();
                    break;

                case Protocol.BATTLE_INVITE:
                    handleBattleInvite();
                    break;

                case Protocol.BATTLE_RESPONSE:
                    handleBattleResponse(parts);
                    break;

                case Protocol.QUIT:
                    handleQuit();
                    break;

                case Protocol.READY_FOR_NEXT:
                    handleReadyForNext();
                    break;

                case Protocol.SPECTATE:
                    handleSpectate(parts);
                    break;

                case Protocol.CREATE_ROOM:
                    handleCreateRoom();
                    break;

                case Protocol.QUICK_JOIN:
                    handleQuickJoin();
                    break;

                case Protocol.JOIN_ROOM_BY_ID:
                    handleJoinRoomById(parts);
                    break;

                case Protocol.GET_ROOM_LIST:
                    handleGetRoomList();
                    break;

                case Protocol.REQUEST_ROOM_STATE:
                    handleRequestRoomState();
                    break;

                default:
                    sendMessage(Protocol.buildMessage(Protocol.ERROR, "未知指令: " + command));
            }
        } catch (Exception e) {
            System.err.println("处理消息异常: " + e.getMessage());
            e.printStackTrace();
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "服务器处理异常"));
        }
    }

    /**
     * 处理登录请求
     */
    private void handleLogin(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildMessage(Protocol.LOGIN_FAIL, "用户名不能为空"));
            return;
        }

        String requestedUsername = parts[1].trim();

        // 检查用户名是否为空
        if (requestedUsername.isEmpty()) {
            sendMessage(Protocol.buildMessage(Protocol.LOGIN_FAIL, "用户名不能为空"));
            return;
        }

        // 检查用户名是否已存在
        if (server.isUsernameExists(requestedUsername)) {
            sendMessage(Protocol.buildMessage(Protocol.LOGIN_FAIL, "用户名已存在"));
            return;
        }

        // 登录成功
        this.username = requestedUsername;
        sendMessage(Protocol.buildMessage(Protocol.LOGIN_SUCCESS, username));

        // 加入服务器
        server.addClient(this);

        System.out.println("用户 " + username + " 登录成功");
    }

    /**
     * 处理落子请求
     */
    private void handleMove(String[] parts) {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "未在游戏中"));
            return;
        }

        if (parts.length < 3) {
            sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "参数不足"));
            return;
        }

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            gameSession.handleMove(this, x, y);
        } catch (NumberFormatException e) {
            sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "坐标格式错误"));
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChat(String[] parts) {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在游戏中"));
            return;
        }

        if (parts.length < 2) {
            return;
        }

        // 重新组合消息内容（可能包含分隔符）
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) {
                messageBuilder.append(Protocol.DELIMITER);
            }
            messageBuilder.append(parts[i]);
        }

        gameSession.handleChat(this, messageBuilder.toString());
    }

    /**
     * 处理坐下黑棋席请求
     */
    private void handleSitBlack() {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        gameSession.handleSeatChange(this, Protocol.SIT_BLACK);
    }

    /**
     * 处理坐下白棋席请求
     */
    private void handleSitWhite() {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        gameSession.handleSeatChange(this, Protocol.SIT_WHITE);
    }

    /**
     * 处理进入观战席请求
     */
    private void handleSitSpectator() {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        gameSession.handleSeatChange(this, Protocol.SIT_SPECTATOR);
    }

    /**
     * 处理对战邀请
     */
    private void handleBattleInvite() {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        gameSession.handleBattleInvite(this);
    }

    /**
     * 处理对战邀请响应
     */
    private void handleBattleResponse(String[] parts) {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        if (parts.length < 2) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "参数不足"));
            return;
        }

        String response = parts[1];
        gameSession.handleBattleResponse(this, response);
    }

    /**
     * 处理退出房间请求（不断开连接）
     */
    private void handleQuit() {
        // 只从游戏会话中移除，不断开连接
        if (gameSession != null) {
            gameSession.removeMember(this);
            gameSession = null;
            sendMessage(Protocol.buildMessage(Protocol.SYSTEM, "已退出房间"));
            System.out.println("用户 " + username + " 退出房间");
        } else {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "您不在任何房间中"));
        }
    }

    /**
     * 处理准备再来一局请求（新系统中不需要此功能）
     */
    private void handleReadyForNext() {
        sendMessage(Protocol.buildMessage(Protocol.SYSTEM, "请退出房间后重新创建或加入"));
    }

    /**
     * 发送消息给客户端
     */
    public void sendMessage(String message) {
        if (writer != null && !socket.isClosed()) {
            writer.println(message.trim()); // 确保消息以换行符结束
        }
    }

    /**
     * 处理观战请求（已由 JOIN_ROOM_BY_ID 替代）
     */
    private void handleSpectate(String[] parts) {
        if (parts.length < 2) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "请指定房间ID"));
            return;
        }

        String roomId = parts[1];

        // 使用新的加入房间方法
        server.joinRoom(roomId, this);
    }

    /**
     * 处理创建房间请求
     */
    private void handleCreateRoom() {
        // 检查是否已在游戏中
        if (gameSession != null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "您已在房间中"));
            return;
        }

        // 调用服务器创建房间（GameSession构造函数会发送ROOM_CREATED，这里不需要重复发送）
        server.createEmptyRoom(this);

        System.out.println("用户 " + username + " 创建了房间");
    }

    /**
     * 处理快速加入请求
     */
    private void handleQuickJoin() {
        // 检查是否已在游戏中
        if (gameSession != null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "您已在房间中"));
            return;
        }

        // 尝试快速加入可用房间
        boolean success = server.quickJoinRoom(this);

        if (!success) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "没有可用的房间"));
        }
        // 如果成功，GameSession 会发送游戏开始消息

        System.out.println("用户 " + username + " 尝试快速加入房间: " + (success ? "成功" : "失败"));
    }

    /**
     * 处理通过ID加入房间请求
     */
    private void handleJoinRoomById(String[] parts) {
        // 检查是否已在游戏中
        if (gameSession != null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "您已在房间中"));
            return;
        }

        // 检查参数
        if (parts.length < 2) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "请输入房间ID"));
            return;
        }

        String roomId = parts[1].trim();

        // 检查房间ID格式
        if (roomId.isEmpty()) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "房间ID不能为空"));
            return;
        }

        // 尝试加入指定房间
        boolean success = server.joinRoom(roomId, this);

        if (!success) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "房间不存在或已满"));
        }
        // 如果成功，GameSession 会发送游戏开始消息

        System.out.println("用户 " + username + " 尝试加入房间 " + roomId + ": " + (success ? "成功" : "失败"));
    }

    /**
     * 处理获取房间列表请求
     */
    private void handleGetRoomList() {
        String roomList = server.getRoomList();
        sendMessage(Protocol.buildMessage(Protocol.ROOM_LIST, roomList));

        System.out.println("用户 " + username + " 请求房间列表");
    }

    /**
     * 处理请求房间状态
     */
    private void handleRequestRoomState() {
        if (gameSession == null) {
            sendMessage(Protocol.buildMessage(Protocol.ERROR, "未在房间中"));
            return;
        }

        // 请求GameSession发送完整的房间状态
        gameSession.sendRoomStateTo(this);

        System.out.println("用户 " + username + " 请求房间状态");
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        running = false;

        // 从游戏会话中移除
        if (gameSession != null) {
            gameSession.removeMember(this);
            gameSession = null;
        }

        // 从服务器中移除
        server.removeClient(this);

        // 关闭资源
        try {
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("关闭连接异常: " + e.getMessage());
        }

        System.out.println("用户 " + username + " 断开连接");
    }

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public boolean isInGame() {
        return gameSession != null;
    }
}
