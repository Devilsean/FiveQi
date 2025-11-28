package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

    // 统一配色方案 - 使用Theme类
    // private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // Removed
    // private static final Color BG_COLOR = new Color(245, 247, 250); // Removed
    // private static final Color TEXT_DARK = new Color(44, 62, 80); // Removed

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
        setSize(900, 650); // 稍微加大尺寸
        getContentPane().setBackground(Theme.BG_COLOR);

        // 顶部欢迎面板
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        topPanel.setBackground(Theme.PRIMARY_COLOR);

        welcomeLabel = new JLabel("欢迎，" + username + "！", SwingConstants.LEFT);
        welcomeLabel.setFont(Theme.TITLE_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        topPanel.add(welcomeLabel, BorderLayout.CENTER);

        // 顶部右侧添加一些装饰或时间（可选），暂时留空或放Logo
        JLabel logoLabel = new JLabel("FiveQi Online");
        logoLabel.setFont(Theme.SUBTITLE_FONT);
        logoLabel.setForeground(new Color(255, 255, 255, 200));
        topPanel.add(logoLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 中间房间列表区域
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setBackground(Theme.BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        // 统计信息导航栏
        JPanel statsPanel = createStatsPanel();
        centerPanel.add(statsPanel, BorderLayout.NORTH);

        // 房间列表容器（包含标题和列表）
        JPanel listContainer = new JPanel(new BorderLayout(0, 0));
        listContainer.setBackground(Color.WHITE);
        listContainer.setBorder(Theme.createShadowBorder()); // 使用阴影边框

        // 房间列表标题行
        JPanel headerPanel = createHeaderPanel();
        listContainer.add(headerPanel, BorderLayout.NORTH);

        // 房间列表内容区域
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(Color.WHITE);
        // roomListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        scrollPane = new JScrollPane(roomListPanel);
        scrollPane.setBorder(null); // 移除JScrollPane默认边框，使用外部容器的边框
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Color.WHITE);
        listContainer.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(listContainer, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottomPanel.setBackground(Theme.BG_COLOR);
        // bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // 刷新列表按钮
        refreshButton = Theme.createSecondaryButton("刷新列表");
        refreshButton.setPreferredSize(new Dimension(120, 45));
        refreshButton.addActionListener(e -> requestRoomList());
        bottomPanel.add(refreshButton);

        // 通过房间ID加入按钮
        JButton joinByIdButton = Theme.createSecondaryButton("查找房间");
        joinByIdButton.setPreferredSize(new Dimension(120, 45));
        joinByIdButton.addActionListener(e -> joinByIdDialog());
        bottomPanel.add(joinByIdButton);

        // 创建房间按钮
        createRoomButton = Theme.createPrimaryButton("创建房间");
        createRoomButton.setPreferredSize(new Dimension(140, 45));
        createRoomButton.addActionListener(e -> createRoom());
        bottomPanel.add(createRoomButton);

        // 退出登录按钮 (放在左下角或单独处理，这里为了布局简单放在右侧最左)
        logoutButton = Theme.createDangerButton("退出登录");
        logoutButton.setPreferredSize(new Dimension(120, 45));
        logoutButton.addActionListener(e -> logout());

        // 使用BorderLayout将退出按钮放左边
        JPanel footerContainer = new JPanel(new BorderLayout());
        footerContainer.setBackground(Theme.BG_COLOR);
        footerContainer.setBorder(BorderFactory.createEmptyBorder(0, 30, 20, 30));

        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 20));
        leftFooter.setBackground(Theme.BG_COLOR);
        leftFooter.add(logoutButton);

        footerContainer.add(leftFooter, BorderLayout.WEST);
        footerContainer.add(bottomPanel, BorderLayout.CENTER); // bottomPanel已经是FlowLayout.RIGHT

        add(footerContainer, BorderLayout.SOUTH);

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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        statsLabel = new JLabel("房间总数: 0 | 正在加载...");
        statsLabel.setFont(Theme.NORMAL_FONT);
        statsLabel.setForeground(Theme.TEXT_COLOR);
        panel.add(statsLabel, BorderLayout.WEST);

        // 快速加入按钮
        JButton quickJoinBtn = Theme.createPrimaryButton("快速加入");
        quickJoinBtn.setPreferredSize(new Dimension(100, 35));
        quickJoinBtn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        quickJoinBtn.addActionListener(e -> quickJoin());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(Theme.BG_COLOR);
        rightPanel.add(quickJoinBtn);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建房间列表标题行
     */
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        String[] headers = { "房间ID", "状态", "人数", "操作" };

        for (int i = 0; i < headers.length; i++) {
            JLabel label = new JLabel(headers[i], SwingConstants.CENTER);
            label.setFont(Theme.BOLD_FONT);
            label.setForeground(Theme.TEXT_COLOR);
            panel.add(label);
        }

        return panel;
    }

    // Removed createStyledButton as we use Theme methods now

    /**
     * 创建房间行面板
     */
    private JPanel createRoomPanel(String roomId, String status, int memberCount) {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
        panel.setBackground(Color.WHITE);
        // 使用更淡的分割线
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(245, 245, 245)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 房间ID
        JLabel idLabel = new JLabel(roomId, SwingConstants.CENTER);
        idLabel.setFont(Theme.BOLD_FONT);
        idLabel.setForeground(Theme.PRIMARY_COLOR);
        panel.add(idLabel);

        // 状态
        JLabel statusLabel = new JLabel(status, SwingConstants.CENTER);
        statusLabel.setFont(Theme.NORMAL_FONT);

        if ("游戏进行中".equals(status) || "Playing".equalsIgnoreCase(status)) {
            statusLabel.setForeground(Theme.DANGER_COLOR);
        } else {
            statusLabel.setForeground(Theme.SECONDARY_COLOR); // Green for waiting
        }
        panel.add(statusLabel);

        // 人数
        JLabel countLabel = new JLabel(memberCount + " / 2", SwingConstants.CENTER);
        countLabel.setFont(Theme.NORMAL_FONT);
        countLabel.setForeground(Theme.TEXT_SECONDARY);
        panel.add(countLabel);

        // 加入按钮
        JButton joinBtn = Theme.createSecondaryButton("加入");
        joinBtn.setPreferredSize(new Dimension(80, 30));
        joinBtn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        joinBtn.addActionListener(e -> joinRoomById(roomId));

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
        String roomId = CustomDialog.showInputDialog(this, "请输入房间ID:", "加入房间", CustomDialog.PLAIN_MESSAGE);
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
            CustomDialog.showMessageDialog(this, message, title, messageType);
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
        int result = CustomDialog.showConfirmDialog(this,
                "确定要退出登录吗？",
                "确认",
                CustomDialog.YES_NO_OPTION);

        if (result == CustomDialog.YES_OPTION) {
            stopAutoRefresh();
            // 先关闭窗口，避免接收后续的服务器消息
            dispose();
            // 断开连接
            if (client != null && client.isConnected()) {
                client.disconnectCompletely();
            }
            // 返回主菜单
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
        // RoomLobbyGUI不显示错误弹窗，避免重复
        System.err.println("RoomLobby收到错误: " + error);
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