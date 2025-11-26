package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

/**
 * 房间大厅界面 - 可视化列表版本
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
    private JLabel statsLabel; // 统计信息标签
    private JButton createRoomButton;
    private JButton quickJoinButton;
    private JButton refreshButton;
    private JButton logoutButton;
    private JPanel roomListPanel; // 房间列表容器
    private JScrollPane scrollPane;
    private Timer autoRefreshTimer; // 自动刷新定时器

    // 统一配色方案 - 黑白配
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // 深灰蓝
    private static final Color BG_COLOR = new Color(245, 247, 250); // 浅灰背景
    private static final Color TEXT_DARK = new Color(44, 62, 80); // 深色文字

    public RoomLobbyGUI(Client client, String username) {
        this.client = client;
        this.username = username;

        // 设置client的NetworkHandler为this
        if (client != null) {
            client.setNetworkHandler(this);
        }

        initComponents();
        requestRoomList();
        startAutoRefresh();
    }

    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        setTitle("五子棋大厅 - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        setSize(750, 600);
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

        // 中间房间列表区域
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // 统计信息导航栏
        JPanel statsPanel = createStatsPanel();
        centerPanel.add(statsPanel, BorderLayout.NORTH);

        // 房间列表标题行
        JPanel headerPanel = createHeaderPanel();

        // 房间列表内容区域
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(Color.WHITE);
        roomListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 添加滚动面板
        JPanel listContainer = new JPanel(new BorderLayout(0, 0));
        listContainer.setBackground(Color.WHITE);
        listContainer.add(headerPanel, BorderLayout.NORTH);
        listContainer.add(roomListPanel, BorderLayout.CENTER);

        scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 创建房间按钮
        createRoomButton = createStyledButton("创建房间");
        createRoomButton.addActionListener(e -> createRoom());
        bottomPanel.add(createRoomButton);

        // 通过房间ID加入按钮（放在创建房间右侧）
        JButton joinByIdButton = createStyledButton("查找房间");
        joinByIdButton.addActionListener(e -> joinByIdDialog());
        bottomPanel.add(joinByIdButton);

        // 刷新列表按钮
        refreshButton = createStyledButton("刷新列表");
        refreshButton.addActionListener(e -> requestRoomList());
        bottomPanel.add(refreshButton);

        // 退出登录按钮
        logoutButton = createStyledButton("退出登录");
        logoutButton.addActionListener(e -> logout());
        bottomPanel.add(logoutButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAutoRefresh();
                logout();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 创建统计信息面板
     */
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(new Color(236, 240, 244));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        statsLabel = new JLabel("房间总数: 0 | 正在加载...");
        statsLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        statsLabel.setForeground(TEXT_DARK);
        panel.add(statsLabel);

        // 快速加入按钮（放在房间总数右侧）
        JButton quickJoinBtn = new JButton("快速加入");
        quickJoinBtn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        quickJoinBtn.setBackground(Color.WHITE);
        quickJoinBtn.setForeground(TEXT_DARK);
        quickJoinBtn.setFocusPainted(false);
        quickJoinBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        quickJoinBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        quickJoinBtn.addActionListener(e -> quickJoin());
        panel.add(quickJoinBtn);

        return panel;
    }

    /**
     * 创建房间列表标题行
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        String[] headers = { "房间ID", "状态", "人数", "操作" };
        int[] widths = { 15, 40, 15, 15 }; // 百分比权重

        for (int i = 0; i < headers.length; i++) {
            JLabel label = new JLabel(headers[i], SwingConstants.CENTER);
            label.setFont(new Font("微软雅黑", Font.BOLD, 13));
            label.setForeground(TEXT_DARK);
            panel.add(label);
        }

        return panel;
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
     * 创建房间行面板
     */
    private JPanel createRoomPanel(String roomId, String status, int memberCount) {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

        // 房间ID
        JLabel idLabel = new JLabel(roomId, SwingConstants.CENTER);
        idLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        idLabel.setForeground(PRIMARY_COLOR);
        panel.add(idLabel);

        // 状态
        JLabel statusLabel = new JLabel(status, SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(100, 100, 100));
        panel.add(statusLabel);

        // 人数
        JLabel countLabel = new JLabel(memberCount + " 人", SwingConstants.CENTER);
        countLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        countLabel.setForeground(new Color(80, 80, 80));
        panel.add(countLabel);

        // 加入按钮
        JButton joinBtn = new JButton("加入");
        joinBtn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        joinBtn.setBackground(Color.WHITE);
        joinBtn.setForeground(TEXT_DARK);
        joinBtn.setFocusPainted(false);
        joinBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        joinBtn.setOpaque(true);
        joinBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        joinBtn.addActionListener(e -> joinRoomById(roomId));

        // 按钮悬停效果
        joinBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                joinBtn.setBackground(new Color(245, 245, 245));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                joinBtn.setBackground(Color.WHITE);
            }
        });

        JPanel btnContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnContainer.setBackground(Color.WHITE);
        btnContainer.add(joinBtn);
        panel.add(btnContainer);

        return panel;
    }

    /**
     * 启动自动刷新
     */
    private void startAutoRefresh() {
        // 每5秒自动刷新一次
        autoRefreshTimer = new Timer(5000, e -> requestRoomList());
        autoRefreshTimer.start();
    }

    /**
     * 停止自动刷新
     */
    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
    }

    /**
     * 创建房间
     */
    private void createRoom() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.CREATE_ROOM));
        }
    }

    /**
     * 快速加入房间
     */
    private void quickJoin() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.QUICK_JOIN));
        }
    }

    /**
     * 通过ID加入房间对话框
     */
    private void joinByIdDialog() {
        String roomId = JOptionPane.showInputDialog(this, "请输入房间ID:", "加入房间", JOptionPane.PLAIN_MESSAGE);
        if (roomId != null && !roomId.trim().isEmpty()) {
            joinRoomById(roomId.trim());
        }
    }

    /**
     * 通过ID加入房间
     */
    private void joinRoomById(String roomId) {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.JOIN_ROOM_BY_ID, roomId));
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
            System.out.println("DEBUG: 收到房间列表数据: " + roomListText);

            // 清空现有列表
            roomListPanel.removeAll();

            if (roomListText == null || roomListText.trim().isEmpty() || roomListText.equals("0")) {
                // 没有房间
                statsLabel.setText("房间总数: 0 | 暂无活跃房间");

                JLabel emptyLabel = new JLabel("当前没有活跃的房间", SwingConstants.CENTER);
                emptyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
                emptyLabel.setForeground(new Color(150, 150, 150));
                emptyLabel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
                roomListPanel.add(emptyLabel);

                JLabel tipLabel = new JLabel("点击\"创建房间\"开始新游戏！", SwingConstants.CENTER);
                tipLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                tipLabel.setForeground(new Color(120, 120, 120));
                roomListPanel.add(tipLabel);
            } else {
                // 解析房间列表 - 服务器格式: "count|roomID|status|count人|roomID|status|count人..."
                String[] parts = roomListText.split("\\|");
                System.out.println("DEBUG: 分割后parts长度: " + parts.length);

                if (parts.length > 0) {
                    try {
                        int roomCount = Integer.parseInt(parts[0].trim());
                        statsLabel.setText(String.format("房间总数: %d | 自动刷新中...", roomCount));
                        System.out.println("DEBUG: 房间数量: " + roomCount);

                        // 从索引1开始，每3个元素是一个房间：roomID, status, count人
                        for (int i = 1; i + 2 < parts.length; i += 3) {
                            String roomId = parts[i].trim();
                            String status = parts[i + 1].trim();
                            String countStr = parts[i + 2].replaceAll("[^0-9]", "");

                            if (!countStr.isEmpty()) {
                                int memberCount = Integer.parseInt(countStr);
                                System.out.println("DEBUG: 添加房间: " + roomId + ", " + status + ", " + memberCount);

                                JPanel roomPanel = createRoomPanel(roomId, status, memberCount);
                                roomListPanel.add(roomPanel);
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("解析房间列表失败: " + e.getMessage());
                        e.printStackTrace();
                        statsLabel.setText("房间总数: ? | 数据解析错误");

                        JLabel errorLabel = new JLabel("解析错误: " + e.getMessage(), SwingConstants.CENTER);
                        errorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                        errorLabel.setForeground(Color.RED);
                        roomListPanel.add(errorLabel);
                    }
                }
            }

            // 刷新显示
            roomListPanel.revalidate();
            roomListPanel.repaint();
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
            stopAutoRefresh();
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
            stopAutoRefresh();
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
        stopAutoRefresh();
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
                new GameGUI(client, username, roomId);// 关闭大厅
                closeLobby();
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