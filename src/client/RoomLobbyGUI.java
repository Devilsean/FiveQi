package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 房间大厅界面
 * 玩家登录后可以选择创建房间或加入房间
 */
public class RoomLobbyGUI extends JFrame implements NetworkHandler {

    private Client client;
    private String username;

    // 等待创建GameGUI的房间ID
    private String pendingRoomId;
    private boolean waitingForRoomState;

    // GUI组件
    private JLabel welcomeLabel;
    private JButton createRoomButton;
    private JButton quickJoinButton;
    private JButton joinByIdButton;
    private JButton refreshButton;
    private JButton logoutButton;
    private JTextArea roomListArea;
    private JScrollPane scrollPane;

    // 统一配色方案
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // 深灰蓝
    private static final Color ACCENT_COLOR = new Color(41, 128, 185); // 蓝色强调
    private static final Color BG_COLOR = new Color(245, 247, 250); // 浅灰背景
    private static final Color TEXT_DARK = new Color(44, 62, 80); // 深色文字
    private static final Color BUTTON_HOVER = new Color(52, 152, 219); // 悬停色
    private static final Color SECONDARY_BTN = new Color(127, 140, 141); // 次要按钮

    public RoomLobbyGUI(Client client, String username) {
        this.client = client;
        this.username = username;

        // 设置client的NetworkHandler为this
        if (client != null) {
            client.setNetworkHandler(this);
        }

        initComponents();
        requestRoomList();
    }

    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        setTitle("五子棋大厅 - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        setSize(650, 550);
        getContentPane().setBackground(BG_COLOR);

        // 顶部欢迎面板
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        topPanel.setBackground(PRIMARY_COLOR);

        welcomeLabel = new JLabel("欢迎，" + username + "！", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        topPanel.add(welcomeLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // 中间房间列表面板
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel listLabel = new JLabel("当前房间列表");
        listLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        listLabel.setForeground(TEXT_DARK);
        centerPanel.add(listLabel, BorderLayout.NORTH);

        roomListArea = new JTextArea();
        roomListArea.setEditable(false);
        roomListArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        roomListArea.setLineWrap(true);
        roomListArea.setWrapStyleWord(true);
        roomListArea.setText("正在加载房间列表...");
        roomListArea.setBackground(Color.WHITE);
        roomListArea.setForeground(new Color(60, 60, 60));
        roomListArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(roomListArea);
        scrollPane.setPreferredSize(new Dimension(0, 220));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new GridLayout(3, 2, 12, 12));
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 创建房间按钮
        createRoomButton = createStyledButton("创建房间");
        createRoomButton.addActionListener(e -> createRoom());
        bottomPanel.add(createRoomButton);

        // 快速加入按钮
        quickJoinButton = createStyledButton("快速加入");
        quickJoinButton.addActionListener(e -> quickJoin());
        bottomPanel.add(quickJoinButton);

        // 输入ID加入按钮
        joinByIdButton = createStyledButton("输入ID加入");
        joinByIdButton.addActionListener(e -> joinById());
        bottomPanel.add(joinByIdButton);

        // 刷新列表按钮
        refreshButton = createStyledButton("刷新列表");
        refreshButton.addActionListener(e -> requestRoomList());
        bottomPanel.add(refreshButton);

        // 观战按钮
        JButton spectateButton = createStyledButton("观战房间");
        spectateButton.addActionListener(e -> spectateRoom());
        bottomPanel.add(spectateButton);

        // 退出登录按钮
        logoutButton = createStyledButton("退出登录");
        logoutButton.addActionListener(e -> logout());
        bottomPanel.add(logoutButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 创建样式化按钮 - 简洁白色风格
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 15));
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 50));

        // 鼠标悬停效果 - 轻微变灰
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(250, 250, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    /**
     * 创建房间
     */
    private void createRoom() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.CREATE_ROOM));
            JOptionPane.showMessageDialog(this,
                    "正在创建房间，请稍候...",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 快速加入房间
     */
    private void quickJoin() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.QUICK_JOIN));
            JOptionPane.showMessageDialog(this,
                    "正在寻找可用房间...",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 通过ID加入房间
     */
    private void joinById() {
        String roomId = JOptionPane.showInputDialog(this,
                "请输入房间ID（4位数字）：",
                "加入房间",
                JOptionPane.PLAIN_MESSAGE);

        if (roomId != null && !roomId.trim().isEmpty()) {
            roomId = roomId.trim();
            if (roomId.matches("\\d{4}")) {
                if (client != null && client.isConnected()) {
                    client.sendMessage(Protocol.buildMessage(Protocol.JOIN_ROOM_BY_ID, roomId));
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "房间ID格式错误！请输入4位数字。",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 观战房间
     */
    private void spectateRoom() {
        String roomId = JOptionPane.showInputDialog(this,
                "请输入要观战的房间ID（4位数字）：",
                "观战房间",
                JOptionPane.PLAIN_MESSAGE);

        if (roomId != null && !roomId.trim().isEmpty()) {
            roomId = roomId.trim();
            if (roomId.matches("\\d{4}")) {
                if (client != null && client.isConnected()) {
                    client.sendMessage(Protocol.buildMessage(Protocol.SPECTATE, roomId));
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "房间ID格式错误！请输入4位数字。",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 请求房间列表
     */
    private void requestRoomList() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.GET_ROOM_LIST));
        }
    }

    /**
     * 更新房间列表显示
     */
    public void updateRoomList(String roomListText) {
        SwingUtilities.invokeLater(() -> {
            if (roomListText == null || roomListText.trim().isEmpty()) {
                roomListArea.setText("当前没有活跃的房间\n\n点击\"创建房间\"开始新游戏！");
            } else {
                roomListArea.setText(roomListText);
            }
        });
    }

    /**
     * 显示消息
     */
    public void showMessage(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        });
    }

    /**
     * 关闭大厅界面
     */
    public void closeLobby() {
        SwingUtilities.invokeLater(() -> {
            dispose();
        });
    }

    /**
     * 等待对手加入
     */
    public void waitingForOpponent(String roomId) {
        SwingUtilities.invokeLater(() -> {
            welcomeLabel.setText("房间 " + roomId + " 已创建，等待对手加入...");
        });
    }

    /**
     * 退出登录
     */
    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要退出登录吗？",
                "确认",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            if (client != null && client.isConnected()) {
                client.disconnectCompletely();
            }
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenu());
        }
    }

    // ==================== NetworkHandler 接口实现 ====================

    @Override
    public void onLoginSuccess(String username) {
        // 登录成功，已在构造函数中处理
    }

    @Override
    public void onLoginFail(String reason) {
        showMessage("登录失败: " + reason, "错误", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onWaiting() {
        // 大厅不需要处理等待匹配
    }

    @Override
    public void onGameStart(String roomId, String myColor, String opponentName) {
        // 游戏开始，关闭大厅并打开游戏界面
        SwingUtilities.invokeLater(() -> {
            closeLobby();
            new GameGUI(client, username, roomId);
        });
    }

    @Override
    public void onJoinRoom(String roomId, String player1, String player2) {
        // 加入房间，先请求房间状态，等收到SEAT_UPDATE后再创建GameGUI
        this.pendingRoomId = roomId;
        this.waitingForRoomState = true;

        // 请求房间状态
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.REQUEST_ROOM_STATE));
            System.out.println("DEBUG RoomLobby: 加入房间成功，已请求房间状态");
        }
    }

    @Override
    public void onMoveSuccess(int x, int y, String color, String username) {
        // 大厅不处理落子
    }

    @Override
    public void onMoveFail(String reason) {
        // 大厅不处理落子
    }

    @Override
    public void onGameOver(String winnerColor, String reason) {
        // 大厅不处理游戏结束
    }

    @Override
    public void onChatMessage(String sender, String timestamp, String message) {
        // 大厅不处理聊天
    }

    @Override
    public void onRoleChange(String username, String newRole) {
        // 大厅不处理角色变更
    }

    @Override
    public void onTakeoverAsk(String spectatorName) {
        // 大厅不处理接手请求
    }

    @Override
    public void onTakeoverResult(boolean success, String reason) {
        // 大厅不处理接手结果
    }

    @Override
    public void onSystemMessage(String message) {
        // 显示系统消息
        SwingUtilities.invokeLater(() -> {
            updateRoomList(message);
        });
    }

    @Override
    public void onError(String error) {
        showMessage("错误: " + error, "错误", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onDisconnected() {
        showMessage("与服务器断开连接", "提示", JOptionPane.WARNING_MESSAGE);
        SwingUtilities.invokeLater(() -> {
            dispose();
            new MainMenu();
        });
    }

    @Override
    public void onRoomCreated(String roomId) {
        // 房间创建成功，先请求房间状态，等收到SEAT_UPDATE后再创建GameGUI
        this.pendingRoomId = roomId;
        this.waitingForRoomState = true;

        // 请求房间状态
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.REQUEST_ROOM_STATE));
            System.out.println("DEBUG RoomLobby: 房间创建成功，已请求房间状态");
        }
    }

    @Override
    public void onRoomListUpdate(String roomListText) {
        updateRoomList(roomListText);
    }

    @Override
    public void onWaitingForOpponent(String roomId) {
        waitingForOpponent(roomId);
    }

    @Override
    public void onSeatUpdate(String blackSeat, String whiteSeat, int spectatorCount) {
        // 如果正在等待房间状态，收到SEAT_UPDATE后创建GameGUI
        if (waitingForRoomState && pendingRoomId != null) {
            System.out.println("DEBUG RoomLobby: 收到席位更新，创建GameGUI");

            final String roomId = pendingRoomId;
            waitingForRoomState = false;
            pendingRoomId = null;

            SwingUtilities.invokeLater(() -> {
                // 创建GameGUI（这会设置新的NetworkHandler）
                new GameGUI(client, username, roomId);
                // 关闭大厅
                dispose();
            });
        }
    }

    @Override
    public void onBattleInviteNotify(String inviterName) {
        // 大厅不处理对战邀请
    }

    @Override
    public void onBattleStart() {
        // 大厅不处理对战开始
    }

    @Override
    public void onBoardReset() {
        // 大厅不处理棋盘重置
    }
}