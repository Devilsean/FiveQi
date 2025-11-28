package client;

import common.Protocol;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 五子棋游戏GUI界面
 * 实现NetworkHandler接口处理服务器消息
 */
public class GameGUI extends JFrame implements NetworkHandler {

    // 网络客户端
    private Client client;

    // 游戏状态
    private String myUsername;
    private String myColor;
    private String myRole;
    private String roomId;
    private boolean gameStarted;
    private int[][] board; // 0-空，1-黑棋，2-白棋
    private Point previewStone; // 预览棋子位置

    // GUI组件
    private CardLayout cardLayout;
    private JPanel mainPanel; // 主面板，用于视图切换
    private ChessBoardPanel boardPanel;
    private JTextArea systemArea; // 系统消息区域
    private JTextPane chatArea; // 聊天消息区域
    private StyledDocument chatDoc; // 聊天文档
    private JTextField chatInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel seatStatusLabel; // 席位状态显示
    private JPanel buttonPanel;
    private Map<String, JButton> actionButtons;
    private Map<String, String> playerRoles; // 玩家角色映射表
    private long lastMessageTime = 0; // 上一条消息的时间戳

    // 常量
    private static final int CELL_SIZE = 40;
    private static final int BOARD_MARGIN = 30;
    private static final int STONE_RADIUS = 16;

    // 统一配色方案 - 使用Theme类
    // private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // Removed
    // private static final Color ACCENT_COLOR = new Color(41, 128, 185); // Removed
    // private static final Color BG_COLOR = new Color(245, 247, 250); // Removed
    // private static final Color TEXT_DARK = new Color(44, 62, 80); // Removed
    // private static final Color SECONDARY_BTN = new Color(127, 140, 141); //
    // Removed

    /**
     * 默认构造函数（用于独立启动，显示登录界面）
     */
    public GameGUI() {
        initBoard();
        initComponents(true);
        // 显示登录面板（已在initComponents中处理，默认显示login视图）
    }

    /**
     * 从房间大厅进入的构造函数
     * 注意：此时RoomLobbyGUI已经请求并接收了房间状态（SEAT_UPDATE），
     * 所以GameGUI创建后会立即收到后续的房间状态更新
     */
    public GameGUI(Client client, String username, String roomId) {
        this.client = client;
        this.myUsername = username;
        this.roomId = roomId;
        this.myRole = Protocol.SPECTATOR; // 默认进入观战席
        this.playerRoles = new HashMap<>(); // 初始化角色映射

        // 先初始化棋盘数据结构
        initBoard();

        // 再初始化GUI组件（不显示登录界面）
        initComponents(false);

        // 切换到游戏面板
        cardLayout.show(mainPanel, "game");

        // 设置NetworkHandler（此时RoomLobbyGUI已经获取了初始房间状态）
        client.setNetworkHandler(this);

        // 更新初始状态
        updateStatus("房间 " + roomId + " | 我的角色:观战者");
        addSystemMessage("=== 欢迎进入房间 " + roomId + " ===");
        addSystemMessage(">>> 你当前在观战席，可以选择黑棋席或白棋席");

        // 再次请求房间状态，确保获取最新数据（包括棋盘状态）
        client.sendMessage(Protocol.buildMessage(Protocol.REQUEST_ROOM_STATE));
        System.out.println("DEBUG GameGUI: 已请求房间状态");

        // 使用 SwingUtilities.invokeLater 确保在 GUI 完全初始化后更新按钮
        SwingUtilities.invokeLater(() -> {
            updateButtons();
            System.out.println("DEBUG: 初始化按钮，myRole=" + myRole + ", gameStarted=" + gameStarted);
        });
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel(new BorderLayout());
        loginPanel.setBackground(Theme.BG_COLOR);

        // 顶部标题区域
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(Theme.PRIMARY_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(50, 20, 50, 20));

        JLabel titleLabel = new JLabel("五子棋联机对战");
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("连接到服务器");
        subtitleLabel.setFont(Theme.SUBTITLE_FONT);
        subtitleLabel.setForeground(Theme.TEXT_LIGHT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(15));
        titlePanel.add(subtitleLabel);

        loginPanel.add(titlePanel, BorderLayout.NORTH);

        // 中间表单区域
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Theme.BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        // 创建输入字段
        JTextField hostField = new JTextField("localhost");
        hostField.setFont(Theme.NORMAL_FONT);
        hostField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT));
        portField.setFont(Theme.NORMAL_FONT);
        portField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JTextField usernameField = new JTextField();
        usernameField.setFont(Theme.NORMAL_FONT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // 添加标签和输入框
        centerPanel.add(createLabeledField("服务器地址:", hostField), gbc);
        centerPanel.add(createLabeledField("端口:", portField), gbc);
        centerPanel.add(createLabeledField("用户名:", usernameField), gbc);

        // 间隔
        gbc.insets = new Insets(20, 0, 20, 0);
        centerPanel.add(Box.createVerticalStrut(20), gbc);
        gbc.insets = new Insets(10, 0, 10, 0);

        // 连接按钮
        JButton connectButton = Theme.createPrimaryButton("连接");
        connectButton.setPreferredSize(new Dimension(150, 45));
        connectButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String username = usernameField.getText().trim();

            if (host.isEmpty() || portStr.isEmpty() || username.isEmpty()) {
                CustomDialog.showMessageDialog(this, "所有字段都不能为空！", "错误", CustomDialog.ERROR_MESSAGE);
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                connectToServer(host, port, username);
            } catch (NumberFormatException ex) {
                CustomDialog.showMessageDialog(this, "端口号格式错误！", "错误", CustomDialog.ERROR_MESSAGE);
            }
        });
        centerPanel.add(connectButton, gbc);

        loginPanel.add(centerPanel, BorderLayout.CENTER);

        // 底部信息
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Theme.BG_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JLabel footerLabel = new JLabel("版本 v1.5 ");
        footerLabel.setFont(Theme.SMALL_FONT);
        footerLabel.setForeground(Color.GRAY);
        footerPanel.add(footerLabel);

        loginPanel.add(footerPanel, BorderLayout.SOUTH);

        return loginPanel;
    }

    /**
     * 创建带标签的输入字段
     */
    private JPanel createLabeledField(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.BOLD_FONT);
        label.setForeground(Theme.TEXT_COLOR);
        label.setPreferredSize(new Dimension(100, 30));

        panel.add(label, BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout(0, 0));
        gamePanel.setBackground(Theme.BG_COLOR);

        // 创建顶部导航栏面板 - 横跨整个窗口
        JPanel navPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        navPanel.setBackground(Theme.BG_COLOR);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // 状态标签 - 现代化设计
        statusLabel = new JLabel("未连接", SwingConstants.CENTER);
        statusLabel.setFont(Theme.BOLD_FONT);
        statusLabel.setForeground(Theme.TEXT_COLOR);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setBorder(Theme.createShadowBorder());
        navPanel.add(statusLabel);

        // 席位状态标签 - 渐变色设计
        seatStatusLabel = new JLabel("席位状态：加载中...", SwingConstants.CENTER);
        seatStatusLabel.setFont(Theme.NORMAL_FONT);
        seatStatusLabel.setForeground(Theme.TEXT_COLOR);
        seatStatusLabel.setOpaque(true);
        seatStatusLabel.setBackground(Color.WHITE);
        seatStatusLabel.setBorder(Theme.createShadowBorder());
        navPanel.add(seatStatusLabel);

        gamePanel.add(navPanel, BorderLayout.NORTH);

        // 创建中间内容面板（棋盘+功能区）
        JPanel contentPanel = new JPanel(new BorderLayout(15, 0));
        contentPanel.setBackground(Theme.BG_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        // 创建棋盘面板
        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.setBackground(Theme.BG_COLOR);

        boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2,
                Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR.darker(), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        boardContainer.add(boardPanel, BorderLayout.CENTER);
        contentPanel.add(boardContainer, BorderLayout.CENTER);

        // 创建右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setBackground(Theme.BG_COLOR);

        // 中间面板：系统消息 + 聊天室
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        centerPanel.setBackground(Theme.BG_COLOR);

        // 系统消息区域 - 优化边框
        JPanel systemPanel = new JPanel(new BorderLayout(5, 5));
        systemPanel.setBackground(Color.WHITE);
        systemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        "系统消息",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        Theme.NORMAL_FONT,
                        Theme.TEXT_COLOR)));

        systemArea = new JTextArea();
        systemArea.setEditable(false);
        systemArea.setLineWrap(true);
        systemArea.setWrapStyleWord(true);
        systemArea.setFont(Theme.SMALL_FONT);
        systemArea.setBackground(Theme.READONLY_BG);
        systemArea.setForeground(Theme.TEXT_COLOR);
        systemArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane systemScroll = new JScrollPane(systemArea);
        systemScroll.setBorder(null);
        systemScroll.setPreferredSize(new Dimension(0, 100));
        systemPanel.add(systemScroll, BorderLayout.CENTER);

        centerPanel.add(systemPanel);

        // 聊天区域 - 优化边框
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        "聊天室",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        Theme.NORMAL_FONT,
                        Theme.TEXT_COLOR)));

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatDoc = chatArea.getStyledDocument();
        chatArea.setBackground(Theme.READONLY_BG);
        chatArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatScroll.setPreferredSize(new Dimension(0, 100));
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // 聊天输入 - 现代化设计
        JPanel chatInputPanel = new JPanel(new BorderLayout(8, 0));
        chatInputPanel.setBackground(Color.WHITE);
        chatInputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        chatInput = new JTextField();
        chatInput.setFont(Theme.NORMAL_FONT);
        chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        chatInput.addActionListener(e -> sendChat());

        sendButton = Theme.createPrimaryButton("发送");
        sendButton.setPreferredSize(new Dimension(80, 34));
        sendButton.addActionListener(e -> sendChat());

        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        centerPanel.add(chatPanel);

        rightPanel.add(centerPanel, BorderLayout.CENTER);

        // 功能按钮面板 - 固定3个按钮
        buttonPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        buttonPanel.setBackground(Theme.BG_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        actionButtons = new HashMap<>();

        // 创建3个动态按钮
        createActionButton("按钮1", e -> handleButton1());
        createActionButton("按钮2", e -> handleButton2());
        createActionButton("退出房间", e -> quitGame());

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        contentPanel.add(rightPanel, BorderLayout.EAST);

        gamePanel.add(contentPanel, BorderLayout.CENTER);

        return gamePanel;
    }

    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        initComponents(true);
    }

    /**
     * 初始化GUI组件
     * 
     * @param showLogin 是否显示登录界面
     */
    private void initComponents(boolean showLogin) {
        setTitle("五子棋联机对战");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel loginPanel = createLoginPanel();
        JPanel gamePanel = createGamePanel();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(gamePanel, "game");

        add(mainPanel);

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null && client.isConnected()) {
                    client.quit();
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 创建功能按钮 - 现代化设计
     */
    private void createActionButton(String text, ActionListener listener) {
        JButton button;
        if (text.contains("退出")) {
            button = Theme.createDangerButton(text);
        } else {
            button = Theme.createPrimaryButton(text);
        }

        button.addActionListener(listener);
        button.setVisible(false);
        actionButtons.put(text, button);
        buttonPanel.add(button);
    }

    /**
     * 初始化棋盘
     */
    private void initBoard() {
        board = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
        previewStone = null;
        gameStarted = false;
    }

    /**
     * 连接到服务器
     */
    private void connectToServer(String host, int port, String username) {
        client = new Client(host, port, this);
        playerRoles = new HashMap<>(); // 初始化角色映射

        if (client.connect()) {
            myUsername = username;
            client.login(username);
            // 切换到游戏面板
            cardLayout.show(mainPanel, "game");
            updateStatus("正在登录...");
        } else {
            CustomDialog.showMessageDialog(this, "连接服务器失败！", "错误", CustomDialog.ERROR_MESSAGE);
            // 连接失败，保持在登录界面
        }
    }

    /**
     * 更新状态标签
     */
    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    /**
     * 添加系统消息
     */
    private void addSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            systemArea.append(message + "\n");
            systemArea.setCaretPosition(systemArea.getDocument().getLength());
        });
    }

    /**
     * 添加聊天消息（带时间戳）
     */
    private void addChatMessage(String sender, String timestamp, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                long currentTime = System.currentTimeMillis();

                // 如果距离上一条消息超过5分钟，显示时间戳
                if (currentTime - lastMessageTime > 5 * 60 * 1000) {
                    addTimestamp(timestamp);
                }
                lastMessageTime = currentTime;

                boolean isMe = sender.equals(myUsername);
                String role = getPlayerRole(sender);

                // 创建消息样式
                Style msgStyle = chatArea.addStyle("msg", null);
                StyleConstants.setFontFamily(msgStyle, "微软雅黑");
                StyleConstants.setFontSize(msgStyle, 12);

                // 根据角色设置特殊样式
                if (role.contains("黑棋")) {
                    // 黑棋玩家：黑色粗体，浅灰背景
                    StyleConstants.setForeground(msgStyle, Color.BLACK);
                    StyleConstants.setBold(msgStyle, true);
                    StyleConstants.setBackground(msgStyle, new Color(240, 240, 240));
                } else if (role.contains("白棋")) {
                    // 白棋玩家：深灰色，白色背景带边框效果
                    StyleConstants.setForeground(msgStyle, new Color(60, 60, 60));
                    StyleConstants.setBold(msgStyle, true);
                    StyleConstants.setBackground(msgStyle, new Color(252, 252, 252));
                } else if (isMe) {
                    // 观战者且是自己：蓝色
                    StyleConstants.setForeground(msgStyle, new Color(41, 128, 185));
                } else {
                    // 其他观战者：普通黑色
                    StyleConstants.setForeground(msgStyle, Theme.TEXT_COLOR);
                }

                // 设置对齐方式
                if (isMe) {
                    StyleConstants.setAlignment(msgStyle, StyleConstants.ALIGN_RIGHT);
                } else {
                    StyleConstants.setAlignment(msgStyle, StyleConstants.ALIGN_LEFT);
                }

                // 插入消息
                int offset = chatDoc.getLength();
                String roleTag = role.isEmpty() ? "" : "[" + role + "] ";
                String fullMsg = (isMe ? "" : sender + ": ") + roleTag + message + "\n";
                chatDoc.insertString(offset, fullMsg, msgStyle);
                chatDoc.setParagraphAttributes(offset, fullMsg.length(), msgStyle, false);

                chatArea.setCaretPosition(chatDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 添加时间戳（居中显示）
     */
    private void addTimestamp(String timestamp) {
        try {
            Style timeStyle = chatArea.addStyle("time", null);
            StyleConstants.setAlignment(timeStyle, StyleConstants.ALIGN_CENTER);
            StyleConstants.setFontFamily(timeStyle, "微软雅黑");
            StyleConstants.setFontSize(timeStyle, 10);
            StyleConstants.setForeground(timeStyle, new Color(150, 150, 150));

            int offset = chatDoc.getLength();
            String timeMsg = "—— " + timestamp + " ——\n";
            chatDoc.insertString(offset, timeMsg, timeStyle);
            chatDoc.setParagraphAttributes(offset, timeMsg.length(), timeStyle, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家角色标识
     */
    private String getPlayerRole(String username) {
        if (username.equals(myUsername)) {
            if (Protocol.PLAYER_BLACK.equals(myRole))
                return "黑棋";
            if (Protocol.PLAYER_WHITE.equals(myRole))
                return "白棋";
            return "观战";
        }

        // 从角色映射表中查找
        String role = playerRoles.get(username);
        if (role != null) {
            if (Protocol.PLAYER_BLACK.equals(role))
                return "黑棋";
            if (Protocol.PLAYER_WHITE.equals(role))
                return "白棋";
            return "观战";
        }

        return "";
    }

    /**
     * 发送聊天消息
     */
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && client != null && client.isConnected()) {
            client.chat(message);
            chatInput.setText("");
        }
    }

    /**
     * 坐下黑棋席
     */
    private void sitBlack() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.SIT_BLACK));
        }
    }

    /**
     * 坐下白棋席
     */
    private void sitWhite() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.SIT_WHITE));
        }
    }

    /**
     * 进入观战席
     */
    private void sitSpectator() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.SIT_SPECTATOR));
        }
    }

    /**
     * 发起对战邀请
     */
    private void inviteBattle() {
        if (client != null && client.isConnected()) {
            client.sendMessage(Protocol.buildMessage(Protocol.BATTLE_INVITE));
        }
    }

    /**
     * 退出房间，返回房间大厅
     */
    private void quitGame() {
        int result = CustomDialog.showConfirmDialog(
                this, "确定要退出房间吗？",
                "确认", CustomDialog.YES_NO_OPTION);

        if (result == CustomDialog.YES_OPTION) {
            // 先关闭当前窗口，避免接收后续的服务器消息
            dispose();

            // 只有在房间中时才发送退出房间消息
            if (client != null && client.isConnected() && roomId != null) {
                client.sendMessage(Protocol.buildMessage(Protocol.QUIT));
            }

            // 返回房间大厅
            if (client != null && myUsername != null) {
                SwingUtilities.invokeLater(() -> new RoomLobbyGUI(client, myUsername));
            } else {
                SwingUtilities.invokeLater(() -> new MainMenu());
            }
        }
    }

    /**
     * 更新按钮显示和功能
     */
    private void updateButtons() {
        SwingUtilities.invokeLater(() -> {
            JButton btn1 = actionButtons.get("按钮1");
            JButton btn2 = actionButtons.get("按钮2");

            boolean isSpectator = Protocol.SPECTATOR.equals(myRole);
            boolean isBlackPlayer = Protocol.PLAYER_BLACK.equals(myRole);
            boolean isWhitePlayer = Protocol.PLAYER_WHITE.equals(myRole);

            System.out.println("DEBUG updateButtons: myRole=" + myRole + ", gameStarted=" + gameStarted);

            if (gameStarted) {
                // 游戏进行中，隐藏前两个按钮
                btn1.setVisible(false);
                btn2.setVisible(false);
            } else {
                // 游戏未开始，根据角色显示不同按钮
                btn1.setVisible(true);
                btn2.setVisible(true);

                if (isSpectator) {
                    // 观战席：显示坐下黑棋席和坐下白棋席（都是白色）
                    updateButton(btn1, "坐下黑棋席");
                    updateButton(btn2, "坐下白棋席");
                } else if (isBlackPlayer) {
                    // 黑棋席：根据白棋席是否有人决定按钮
                    updateButton(btn2, "进入观战席");
                    // btn1根据席位状态在onSeatUpdate中更新
                } else if (isWhitePlayer) {
                    // 白棋席：根据黑棋席是否有人决定按钮
                    updateButton(btn2, "进入观战席");
                    // btn1根据席位状态在onSeatUpdate中更新
                }
            }

            // 退出按钮始终可见
            actionButtons.get("退出房间").setVisible(true);
        });
    }

    /**
     * 更新按钮文本（统一白色背景）
     */
    private void updateButton(JButton button, String text) {
        button.setText(text);
        button.setBackground(Color.WHITE);
        button.setForeground(Theme.TEXT_COLOR);
    }

    /**
     * 处理按钮1的点击
     */
    private void handleButton1() {
        String text = actionButtons.get("按钮1").getText();
        if (text.equals("坐下黑棋席")) {
            sitBlack();
        } else if (text.equals("坐下白棋席")) {
            sitWhite();
        } else if (text.equals("发起对战")) {
            inviteBattle();
        }
    }

    /**
     * 处理按钮2的点击
     */
    private void handleButton2() {
        String text = actionButtons.get("按钮2").getText();
        if (text.equals("坐下白棋席")) {
            sitWhite();
        } else if (text.equals("坐下黑棋席")) {
            sitBlack();
        } else if (text.equals("进入观战席")) {
            sitSpectator();
        }
    }

    // ==================== NetworkHandler 接口实现 ====================

    @Override
    public void onLoginSuccess(String username) {
        updateStatus("登录成功，等待匹配...");
        addSystemMessage("=== 欢迎 " + username + " ===");
    }

    @Override
    public void onLoginFail(String reason) {
        SwingUtilities.invokeLater(() -> {
            CustomDialog.showMessageDialog(this, "登录失败: " + reason, "错误", CustomDialog.ERROR_MESSAGE);
            // 切换回登录界面
            cardLayout.show(mainPanel, "login");
        });
    }

    @Override
    public void onWaiting() {
        updateStatus("等待匹配中...");
    }

    @Override
    public void onGameStart(String roomId, String myColor, String opponentName) {
        this.roomId = roomId;
        this.myColor = myColor;
        this.myRole = myColor.equals(Protocol.BLACK) ? Protocol.PLAYER_BLACK : Protocol.PLAYER_WHITE;
        this.gameStarted = true;

        String colorText = myColor.equals(Protocol.BLACK) ? "黑棋" : "白棋";
        updateStatus("房间ID: " + roomId + " | 你是" + colorText + " | 对手: " + opponentName);
        addSystemMessage("=== 游戏开始！你是" + colorText + " ===");
        addSystemMessage(">>> 房间ID: " + roomId + " (分享此ID邀请观战)");

        updateButtons();
        boardPanel.repaint();
    }

    @Override
    public void onJoinRoom(String roomId, String player1, String player2) {
        this.roomId = roomId;
        this.myRole = Protocol.SPECTATOR;
        this.gameStarted = true;

        updateStatus("房间ID: " + roomId + " | 观战模式 | " + player1 + " vs " + player2);
        addSystemMessage("=== 进入观战模式 ===");
        addSystemMessage(">>> 房间ID: " + roomId);

        updateButtons();
        boardPanel.repaint();
    }

    @Override
    public void onMoveSuccess(int x, int y, String color, String username) {
        System.out.println("DEBUG GameGUI.onMoveSuccess: (" + x + "," + y + ") " + color + " by " + username);

        int stoneValue = color.equals(Protocol.BLACK) ? 1 : 2;
        board[x][y] = stoneValue;
        previewStone = null;

        SwingUtilities.invokeLater(() -> boardPanel.repaint());

        String colorText = color.equals(Protocol.BLACK) ? "黑棋" : "白棋";
        addSystemMessage(username + " 落子 " + colorText + " (" + x + "," + y + ")");
    }

    @Override
    public void onMoveFail(String reason) {
        previewStone = null;
        SwingUtilities.invokeLater(() -> {
            boardPanel.repaint();
            CustomDialog.showMessageDialog(this, "落子失败: " + reason, "提示", CustomDialog.WARNING_MESSAGE);
        });
    }

    @Override
    public void onGameOver(String winnerColor, String reason) {
        gameStarted = false;

        String message;
        if (winnerColor.equals("NONE")) {
            message = "游戏结束：平局";
        } else {
            String colorText = winnerColor.equals(Protocol.BLACK) ? "黑棋" : "白棋";
            boolean iWin = winnerColor.equals(myColor);
            message = "游戏结束：" + colorText + "获胜！" + (iWin ? "恭喜你赢了！" : "");
        }

        addSystemMessage("=== " + message + " ===");
        addSystemMessage(">>> 棋盘保留供复盘，可重新发起对战。");

        // 弹窗通知游戏结果
        final String finalMessage = message;
        SwingUtilities.invokeLater(() -> {
            CustomDialog.showMessageDialog(this, finalMessage, "游戏结束", CustomDialog.INFORMATION_MESSAGE);
            updateButtons();
        });
    }

    @Override
    public void onBoardReset() {
        // 收到服务器的棋盘重置消息（新对战开始前）
        System.out.println("DEBUG GameGUI.onBoardReset: 收到棋盘重置消息");

        SwingUtilities.invokeLater(() -> {
            // 只重置棋盘数据，不改变游戏状态
            board = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
            previewStone = null;
            // 注意：不重置 gameStarted，因为对战即将开始

            boardPanel.repaint();

            addSystemMessage("=== 棋盘已清空，准备新对战 ===");
            System.out.println("DEBUG: 棋盘已重置，gameStarted=" + gameStarted);
        });
    }

    @Override
    public void onChatMessage(String sender, String timestamp, String message) {
        addChatMessage(sender, timestamp, message);
    }

    @Override
    public void onRoleChange(String username, String newRole) {
        // 更新角色映射表
        playerRoles.put(username, newRole);

        if (username.equals(myUsername)) {
            myRole = newRole;
            if (newRole.equals(Protocol.PLAYER_BLACK)) {
                myColor = Protocol.BLACK;
            } else if (newRole.equals(Protocol.PLAYER_WHITE)) {
                myColor = Protocol.WHITE;
            }
            updateButtons();
        }

        String roleText = getRoleText(newRole);
        addSystemMessage(">>> " + username + " 角色变更为: " + roleText);
    }

    @Override
    public void onTakeoverAsk(String spectatorName) {
        SwingUtilities.invokeLater(() -> {
            int result = CustomDialog.showConfirmDialog(
                    this,
                    spectatorName + " 请求接手你的位置，是否同意？",
                    "接手请求",
                    CustomDialog.YES_NO_OPTION);

            if (client != null && client.isConnected()) {
                client.respondTakeover(spectatorName, result == CustomDialog.YES_OPTION);
            }
        });
    }

    @Override
    public void onTakeoverResult(boolean success, String reason) {
        String message = success ? "接手成功：" + reason : "接手失败：" + reason;
        addSystemMessage(">>> " + message);

        if (!success) {
            SwingUtilities.invokeLater(() -> {
                CustomDialog.showMessageDialog(this, message, "提示", CustomDialog.INFORMATION_MESSAGE);
            });
        }
    }

    @Override
    public void onSystemMessage(String message) {
        addSystemMessage(">>> " + message);
    }

    @Override
    public void onError(String error) {
        // 忽略"您不在任何房间中"的错误（退出房间时的正常响应）
        if (error.contains("您不在任何房间中")) {
            return;
        }
        addSystemMessage("!!! 错误: " + error);
    }

    @Override
    public void onDisconnected() {
        updateStatus("已断开连接");
        addSystemMessage("=== 与服务器断开连接 ===");

        SwingUtilities.invokeLater(() -> {
            CustomDialog.showMessageDialog(this, "与服务器断开连接", "提示", CustomDialog.WARNING_MESSAGE);
        });
    }

    /**
     * 获取角色文本
     */
    private String getRoleText(String role) {
        switch (role) {
            case Protocol.PLAYER_BLACK:
                return "黑棋执棋者";
            case Protocol.PLAYER_WHITE:
                return "白棋执棋者";
            case Protocol.SPECTATOR:
                return "观战者";
            default:
                return "未知";
        }
    }

    @Override
    public void onRoomCreated(String roomId) {
        // 房间创建成功，GameGUI不处理此消息（由RoomLobbyGUI处理）
    }

    @Override
    public void onRoomListUpdate(String roomListText) {
        // 房间列表更新，GameGUI不处理此消息（由RoomLobbyGUI处理）
    }

    @Override
    public void onWaitingForOpponent(String roomId) {
        // 等待对手，GameGUI不处理此消息（由RoomLobbyGUI处理）
    }

    @Override
    public void onSeatUpdate(String blackSeat, String whiteSeat, int spectatorCount) {
        // 更新角色映射表
        if (!blackSeat.equals("空")) {
            playerRoles.put(blackSeat, Protocol.PLAYER_BLACK);
        }
        if (!whiteSeat.equals("空")) {
            playerRoles.put(whiteSeat, Protocol.PLAYER_WHITE);
        }

        // 添加调试日志
        System.out.println(
                "DEBUG GameGUI.onSeatUpdate:黑棋=" + blackSeat + ", 白棋=" + whiteSeat + ", 观战=" + spectatorCount);

        // 更新席位状态显示
        SwingUtilities.invokeLater(() -> {
            // 更新主状态标签
            String status = String.format("房间 %s | 我的角色: %s",
                    roomId, getRoleText(myRole));
            updateStatus(status);

            // 更新席位状态标签（用不同颜色显示空席和已占用席位）
            String seatStatus = String.format("<html><b>席位状态：</b>" +
                    "<font color='%s'>黑棋席: %s</font> | " +
                    "<font color='%s'>白棋席: %s</font> | " +
                    "观战: %d人</html>",
                    blackSeat.equals("空") ? "green" : "red", blackSeat,
                    whiteSeat.equals("空") ? "green" : "red", whiteSeat,
                    spectatorCount);
            seatStatusLabel.setText(seatStatus);

            System.out.println("DEBUG: 席位状态标签已更新: " + seatStatus);

            addSystemMessage(">>> 席位更新: 黑棋-" + blackSeat + "白棋-" + whiteSeat);

            // 根据席位状态更新按钮1
            if (!gameStarted) {
                JButton btn1 = actionButtons.get("按钮1");
                boolean isBlackPlayer = Protocol.PLAYER_BLACK.equals(myRole);
                boolean isWhitePlayer = Protocol.PLAYER_WHITE.equals(myRole);

                if (isBlackPlayer) {
                    // 黑棋席玩家：如果白棋席有人则显示"发起对战"，否则显示"坐下白棋席"
                    if (whiteSeat.equals("空")) {
                        updateButton(btn1, "坐下白棋席");
                    } else {
                        updateButton(btn1, "发起对战");
                    }
                } else if (isWhitePlayer) {
                    // 白棋席玩家：如果黑棋席有人则显示"发起对战"，否则显示"坐下黑棋席"
                    if (blackSeat.equals("空")) {
                        updateButton(btn1, "坐下黑棋席");
                    } else {
                        updateButton(btn1, "发起对战");
                    }
                }
            }

            // 重要：更新按钮显示
            updateButtons();
        });
    }

    @Override
    public void onBattleInviteNotify(String inviterName) {
        // 收到对战邀请
        SwingUtilities.invokeLater(() -> {
            int result = CustomDialog.showConfirmDialog(
                    this,
                    inviterName + " 发起了对战邀请，是否同意开始游戏？",
                    "对战邀请",
                    CustomDialog.YES_NO_OPTION);

            if (client != null && client.isConnected()) {
                String response = (result == CustomDialog.YES_OPTION) ? Protocol.AGREE : Protocol.REFUSE;
                client.sendMessage(Protocol.buildMessage(Protocol.BATTLE_RESPONSE, response));
            }
        });
    }

    @Override
    public void onBattleStart() {
        System.out.println("DEBUG GameGUI.onBattleStart: 对战开始");

        // 对战开始
        gameStarted = true;
        updateStatus("对战进行中 | 房间 " + roomId);
        addSystemMessage("=== 对战开始！===");

        // 更新按钮（隐藏席位选择按钮）
        updateButtons();

        SwingUtilities.invokeLater(() -> boardPanel.repaint());
    }

    // ==================== 棋盘面板 ====================

    /**
     * 棋盘绘制面板
     */
    private class ChessBoardPanel extends JPanel {

        public ChessBoardPanel() {
            // 背景色交由paintComponent处理
            setOpaque(true);

            // 添加鼠标监听
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleBoardClick(e.getX(), e.getY());
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleBoardHover(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制棋盘背景
            Theme.drawBoardBackground(g2d, getWidth(), getHeight());

            // 绘制棋盘网格
            drawGrid(g2d);

            // 绘制棋子
            drawStones(g2d);

            // 绘制预览棋子
            if (previewStone != null && canPlaceStone()) {
                drawPreviewStone(g2d, previewStone.x, previewStone.y);
            }
        }

        /**
         * 绘制棋盘网格
         */
        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(Theme.BOARD_LINE_COLOR);
            g2d.setStroke(new BasicStroke(1.5f));

            for (int i = 0; i < Protocol.BOARD_SIZE; i++) {
                // 横线
                int y = BOARD_MARGIN + i * CELL_SIZE;
                g2d.drawLine(BOARD_MARGIN, y,
                        BOARD_MARGIN + (Protocol.BOARD_SIZE - 1) * CELL_SIZE, y);

                // 竖线
                int x = BOARD_MARGIN + i * CELL_SIZE;
                g2d.drawLine(x, BOARD_MARGIN,
                        x, BOARD_MARGIN + (Protocol.BOARD_SIZE - 1) * CELL_SIZE);
            }

            // 绘制天元和星位
            g2d.setColor(Theme.BOARD_LINE_COLOR);
            int[] starPoints = { 3, 7, 11 };
            for (int i : starPoints) {
                for (int j : starPoints) {
                    int x = BOARD_MARGIN + i * CELL_SIZE;
                    int y = BOARD_MARGIN + j * CELL_SIZE;
                    g2d.fillOval(x - 4, y - 4, 8, 8);
                }
            }
        }

        /**
         * 绘制棋子
         */
        private void drawStones(Graphics2D g2d) {
            for (int i = 0; i < Protocol.BOARD_SIZE; i++) {
                for (int j = 0; j < Protocol.BOARD_SIZE; j++) {
                    if (board[i][j] != 0) {
                        drawStone(g2d, i, j, board[i][j] == 1);
                    }
                }
            }
        }

        /**
         * 绘制单个棋子
         */
        private void drawStone(Graphics2D g2d, int x, int y, boolean isBlack) {
            int px = BOARD_MARGIN + x * CELL_SIZE;
            int py = BOARD_MARGIN + y * CELL_SIZE;
            Theme.drawStone(g2d, px - STONE_RADIUS, py - STONE_RADIUS, STONE_RADIUS, isBlack);
        }

        /**
         * 绘制预览棋子
         */
        private void drawPreviewStone(Graphics2D g2d, int x, int y) {
            int px = BOARD_MARGIN + x * CELL_SIZE;
            int py = BOARD_MARGIN + y * CELL_SIZE;

            boolean isBlack = myColor.equals(Protocol.BLACK);

            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));

            Theme.drawStone(g2d, px - STONE_RADIUS, py - STONE_RADIUS, STONE_RADIUS, isBlack);

            g2d.setComposite(originalComposite);
        }

        /**
         * 处理棋盘点击
         */
        private void handleBoardClick(int px, int py) {
            if (!canPlaceStone()) {
                return;
            }

            Point pos = pixelToBoard(px, py);
            if (pos != null && board[pos.x][pos.y] == 0) {
                // 发送落子请求
                if (client != null && client.isConnected()) {
                    client.move(pos.x, pos.y);
                }
            }
        }

        /**
         * 处理鼠标悬停
         */
        private void handleBoardHover(int px, int py) {
            if (!canPlaceStone()) {
                if (previewStone != null) {
                    previewStone = null;
                    repaint();
                }
                return;
            }

            Point pos = pixelToBoard(px, py);
            if (pos != null && board[pos.x][pos.y] == 0) {
                if (previewStone == null || !previewStone.equals(pos)) {
                    previewStone = pos;
                    repaint();
                }
            } else {
                if (previewStone != null) {
                    previewStone = null;
                    repaint();
                }
            }
        }

        /**
         * 像素坐标转棋盘坐标
         */
        private Point pixelToBoard(int px, int py) {
            int x = (px - BOARD_MARGIN + CELL_SIZE / 2) / CELL_SIZE;
            int y = (py - BOARD_MARGIN + CELL_SIZE / 2) / CELL_SIZE;

            if (x >= 0 && x < Protocol.BOARD_SIZE && y >= 0 && y < Protocol.BOARD_SIZE) {
                return new Point(x, y);
            }
            return null;
        }

        /**
         * 检查是否可以落子
         */
        private boolean canPlaceStone() {
            return gameStarted &&
                    (Protocol.PLAYER_BLACK.equals(myRole) || Protocol.PLAYER_WHITE.equals(myRole));
        }
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动GUI
        SwingUtilities.invokeLater(() -> new GameGUI());
    }
}