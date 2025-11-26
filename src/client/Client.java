package client;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 五子棋客户端
 * 负责Socket连接、消息发送/接收
 */
public class Client {

    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private NetworkHandler handler;
    private Thread receiveThread;
    private volatile boolean connected;

    public Client(String serverHost, int serverPort, NetworkHandler handler) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.handler = handler;
        this.connected = false;
    }

    /**
     * 设置网络消息处理器
     */
    public void setNetworkHandler(NetworkHandler handler) {
        this.handler = handler;
    }

    /**
     * 连接到服务器
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);

            // 使用 UTF-8 编码
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true // 自动刷新
            );

            connected = true;

            // 启动接收消息线程
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            System.out.println("成功连接到服务器: " + serverHost + ":" + serverPort);
            return true;

        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        connected = false;

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

        System.out.println("已断开与服务器的连接");
    }

    /**
     * 发送消息到服务器
     */
    public void sendMessage(String message) {
        if (writer != null && connected) {
            writer.println(message.trim());
        }
    }

    /**
     * 接收消息线程
     */
    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("接收消息异常: " + e.getMessage());
            }
        } finally {
            if (connected && handler != null) {
                connected = false;
                handler.onDisconnected();
            }
        }
    }

    /**
     * 处理服务器消息
     */
    private void handleMessage(String message) {
        if (handler == null) {
            return;
        }

        String[] parts = Protocol.parseMessage(message);
        if (parts.length == 0) {
            return;
        }

        String command = parts[0];

        try {
            switch (command) {
                case Protocol.LOGIN_SUCCESS:
                    if (parts.length >= 2) {
                        handler.onLoginSuccess(parts[1]);
                    }
                    break;

                case Protocol.LOGIN_FAIL:
                    if (parts.length >= 2) {
                        handler.onLoginFail(parts[1]);
                    }
                    break;

                case Protocol.WAITING:
                    handler.onWaiting();
                    break;

                case Protocol.GAME_START:
                    if (parts.length >= 4) {
                        handler.onGameStart(parts[1], parts[2], parts[3]);
                    }
                    break;

                case Protocol.JOIN_ROOM:
                    if (parts.length >= 4) {
                        handler.onJoinRoom(parts[1], parts[2], parts[3]);
                    }
                    break;

                case Protocol.MOVE_SUCCESS:
                    if (parts.length >= 5) {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        handler.onMoveSuccess(x, y, parts[3], parts[4]);
                    }
                    break;

                case Protocol.MOVE_FAIL:
                    if (parts.length >= 2) {
                        handler.onMoveFail(parts[1]);
                    }
                    break;

                case Protocol.GAME_OVER:
                    if (parts.length >= 3) {
                        handler.onGameOver(parts[1], parts[2]);
                    }
                    break;

                case Protocol.CHAT_MSG:
                    if (parts.length >= 4) {
                        // 重新组合消息内容（可能包含分隔符）
                        StringBuilder msgBuilder = new StringBuilder();
                        for (int i = 3; i < parts.length; i++) {
                            if (i > 3) {
                                msgBuilder.append(Protocol.DELIMITER);
                            }
                            msgBuilder.append(parts[i]);
                        }
                        handler.onChatMessage(parts[1], parts[2], msgBuilder.toString());
                    }
                    break;

                case Protocol.ROLE_CHANGE:
                    if (parts.length >= 3) {
                        handler.onRoleChange(parts[1], parts[2]);
                    }
                    break;

                case Protocol.TAKEOVER_ASK:
                    if (parts.length >= 2) {
                        handler.onTakeoverAsk(parts[1]);
                    }
                    break;

                case Protocol.TAKEOVER_RESULT:
                    if (parts.length >= 3) {
                        boolean success = parts[1].equals("成功");
                        handler.onTakeoverResult(success, parts[2]);
                    }
                    break;

                case Protocol.SYSTEM:
                    if (parts.length >= 2) {
                        handler.onSystemMessage(parts[1]);
                    }
                    break;

                case Protocol.ERROR:
                    if (parts.length >= 2) {
                        handler.onError(parts[1]);
                    }
                    break;

                case Protocol.ROOM_CREATED:
                    if (parts.length >= 2) {
                        handler.onRoomCreated(parts[1]);
                    }
                    break;

                case Protocol.ROOM_LIST:
                    if (parts.length >= 2) {
                        handler.onRoomListUpdate(parts[1]);
                    }
                    break;

                case Protocol.WAITING_FOR_OPPONENT:
                    if (parts.length >= 2) {
                        handler.onWaitingForOpponent(parts[1]);
                    }
                    break;

                case Protocol.SEAT_UPDATE:
                    if (parts.length >= 4) {
                        int spectatorCount = Integer.parseInt(parts[3]);
                        handler.onSeatUpdate(parts[1], parts[2], spectatorCount);
                    }
                    break;

                case Protocol.BATTLE_INVITE_NOTIFY:
                    if (parts.length >= 2) {
                        handler.onBattleInviteNotify(parts[1]);
                    }
                    break;

                case Protocol.BATTLE_START:
                    handler.onBattleStart();
                    break;

                case Protocol.BOARD_RESET:
                    handler.onBoardReset();
                    break;

                default:
                    System.err.println("未知消息类型: " + command);
            }
        } catch (Exception e) {
            System.err.println("处理消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 发送指令的便捷方法 ====================

    /**
     * 发送登录请求
     */
    public void login(String username) {
        sendMessage(Protocol.buildMessage(Protocol.LOGIN, username));
    }

    /**
     * 发送落子请求
     */
    public void move(int x, int y) {
        sendMessage(Protocol.buildMessage(Protocol.MOVE, String.valueOf(x), String.valueOf(y)));
    }

    /**
     * 发送聊天消息
     */
    public void chat(String message) {
        sendMessage(Protocol.buildMessage(Protocol.CHAT, message));
    }

    /**
     * 发送休息请求
     */
    public void requestRest() {
        sendMessage(Protocol.buildMessage(Protocol.REST_REQUEST));
    }

    /**
     * 发送接手请求
     */
    public void requestTakeover() {
        sendMessage(Protocol.buildMessage(Protocol.TAKEOVER_REQUEST));
    }

    /**
     * 发送接手响应
     */
    public void respondTakeover(String spectatorName, boolean agree) {
        String response = agree ? Protocol.AGREE : Protocol.REFUSE;
        sendMessage(Protocol.buildMessage(Protocol.TAKEOVER_RESPONSE, spectatorName, response));
    }

    /**
     * 发送退出房间请求（不断开连接）
     */
    public void quit() {
        sendMessage(Protocol.buildMessage(Protocol.QUIT));
    }

    /**
     * 完全断开连接
     */
    public void disconnectCompletely() {
        sendMessage(Protocol.buildMessage(Protocol.QUIT));
        disconnect();
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}