package client;

import common.Protocol;
import javax.swing.*;
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
    private ChessBoardPanel boardPanel;
    private JTextArea systemArea; // 系统消息区域
    private JTextArea chatArea; // 聊天消息区域
    private JTextField chatInput;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel seatStatusLabel; // 席位状态显示
    private JPanel buttonPanel;
    private Map<String, JButton> actionButtons;

    // 常量
    private static final int CELL_SIZE = 40;
    private static final int BOARD_MARGIN = 30;
    private static final int STONE_RADIUS = 16;

    // 统一配色方案
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // 深灰蓝
    private static final Color ACCENT_COLOR = new Color(41, 128, 185); // 蓝色强调
    private static final Color BG_COLOR = new Color(245, 247, 250); // 浅灰背景
    private static final Color TEXT_DARK = new Color(44, 62, 80); // 深色文字
    private static final Color SECONDARY_BTN = new Color(127, 140, 141); // 次要按钮

    /**
     * 默认构造函数（用于独立启动，显示登录对话框）
     */
    public GameGUI() {
        initComponents();
        initBoard();
        showLoginDialog();
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

        // 先初始化棋盘数据结构
        initBoard();

        // 再初始化GUI组件
        initComponents();

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

    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        setTitle("五子棋联机对战");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_COLOR);

        // 创建顶部导航栏面板 - 横跨整个窗口
        JPanel navPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        navPanel.setBackground(BG_COLOR);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // 状态标签 - 现代化设计
        statusLabel = new JLabel("未连接", SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        statusLabel.setForeground(new Color(60, 60, 60));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        navPanel.add(statusLabel);

        // 席位状态标签 - 渐变色设计
        seatStatusLabel = new JLabel("席位状态：加载中...", SwingConstants.CENTER);
        seatStatusLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        seatStatusLabel.setForeground(Color.WHITE);
        seatStatusLabel.setOpaque(true);
        seatStatusLabel.setBackground(Color.WHITE);
        seatStatusLabel.setForeground(TEXT_DARK);
        seatStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        navPanel.add(seatStatusLabel);

        add(navPanel, BorderLayout.NORTH);

        // 创建中间内容面板（棋盘+功能区）
        JPanel contentPanel = new JPanel(new BorderLayout(15, 0));
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        // 创建棋盘面板（带阴影效果）
        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.setBackground(BG_COLOR);

        boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2,
                Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        boardContainer.add(boardPanel, BorderLayout.CENTER);
        contentPanel.add(boardContainer, BorderLayout.CENTER);

        // 创建右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setBackground(BG_COLOR);

        // 中间面板：系统消息 + 聊天室
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        centerPanel.setBackground(BG_COLOR);

        // 系统消息区域 - 优化边框
        JPanel systemPanel = new JPanel(new BorderLayout(5, 5));
        systemPanel.setBackground(Color.WHITE);
        systemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        "系统消息",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("微软雅黑", Font.BOLD, 12),
                        TEXT_DARK)));

        systemArea = new JTextArea();
        systemArea.setEditable(false);
        systemArea.setLineWrap(true);
        systemArea.setWrapStyleWord(true);
        systemArea.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        systemArea.setBackground(new Color(250, 250, 250));
        systemArea.setForeground(new Color(60, 60, 60));
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
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(5, 5, 5, 5),
                        "聊天室",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("微软雅黑", Font.BOLD, 12),
                        TEXT_DARK)));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        chatArea.setBackground(new Color(250, 250, 250));
        chatArea.setForeground(new Color(60, 60, 60));
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
        chatInput.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        chatInput.addActionListener(e -> sendChat());

        sendButton = new JButton("发送");
        sendButton.setFont(new Font("微软雅黑", Font.BOLD, 12));
        sendButton.setBackground(Color.WHITE);
        sendButton.setForeground(TEXT_DARK);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        sendButton.setOpaque(true);
        sendButton.setPreferredSize(new Dimension(60, 32));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendChat());

        // 发送按钮悬停效果
        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(250, 250, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(Color.WHITE);
            }
        });

        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        centerPanel.add(chatPanel);

        rightPanel.add(centerPanel, BorderLayout.CENTER);

        // 功能按钮面板 - 固定3个按钮
        buttonPanel = new JPanel(new GridLayout(3, 1, 6, 6));
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        actionButtons = new HashMap<>();

        // 创建3个动态按钮
        createActionButton("按钮1", e -> handleButton1());
        createActionButton("按钮2", e -> handleButton2());
        createActionButton("退出房间", e -> quitGame());

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        contentPanel.add(rightPanel, BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);

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
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 40));

        // 根据按钮类型设置统一颜色
        if (text.contains("黑棋")) {
            button.setBackground(PRIMARY_COLOR);
            button.setForeground(Color.WHITE);
        } else if (text.contains("白棋")) {
            button.setBackground(new Color(236, 240, 241));
            button.setForeground(PRIMARY_COLOR);
        } else if (text.contains("观战")) {
            button.setBackground(PRIMARY_COLOR);
            button.setForeground(Color.WHITE);
        } else if (text.contains("对战")) {
            button.setBackground(ACCENT_COLOR);
            button.setForeground(Color.WHITE);
        } else if (text.contains("退出")) {
            button.setBackground(SECONDARY_BTN);
            button.setForeground(Color.WHITE);
        }

        // 添加鼠标悬停效果- 轻微变暗
        Color originalBg = button.getBackground();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(darken(originalBg, 0.9f));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        button.addActionListener(listener);
        button.setVisible(false);
        actionButtons.put(text, button);
        buttonPanel.add(button);
    }

    /**
     * 使颜色变暗
     */
    private Color darken(Color color, float factor) {
        int r = (int) (color.getRed() * factor);
        int g = (int) (color.getGreen() * factor);
        int b = (int) (color.getBlue() * factor);
        return new Color(r, g, b);
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
     * 显示登录对话框
     */
    private void showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT));
        JTextField usernameField = new JTextField();

        panel.add(new JLabel("服务器地址:"));
        panel.add(hostField);
        panel.add(new JLabel("端口:"));
        panel.add(portField);
        panel.add(new JLabel("用户名:"));
        panel.add(usernameField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "连接到服务器",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String username = usernameField.getText().trim();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                showLoginDialog();
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                connectToServer(host, port, username);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "端口号格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                showLoginDialog();
            }
        } else {
            System.exit(0);
        }
    }

    /**
     * 连接到服务器
     */
    private void connectToServer(String host, int port, String username) {
        client = new Client(host, port, this);

        if (client.connect()) {
            myUsername = username;
            client.login(username);
            updateStatus("正在登录...");
        } else {
            JOptionPane.showMessageDialog(this, "连接服务器失败！", "错误", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
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
     * 添加聊天消息
     */
    private void addChatMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
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
        int result = JOptionPane.showConfirmDialog(
                this, "确定要退出房间吗？",
                "确认", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // 发送退出房间消息（但不断开连接）
            if (client != null && client.isConnected()) {
                client.sendMessage(Protocol.buildMessage(Protocol.QUIT));
            }

            // 关闭当前窗口
            dispose();

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
        button.setForeground(TEXT_DARK);
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
            JOptionPane.showMessageDialog(this, "登录失败: " + reason, "错误", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
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
            JOptionPane.showMessageDialog(this, "落子失败: " + reason, "提示", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, finalMessage, "游戏结束", JOptionPane.INFORMATION_MESSAGE);
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
        addChatMessage("[" + timestamp + "] " + sender + ": " + message);
    }

    @Override
    public void onRoleChange(String username, String newRole) {
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
            int result = JOptionPane.showConfirmDialog(
                    this,
                    spectatorName + " 请求接手你的位置，是否同意？",
                    "接手请求",
                    JOptionPane.YES_NO_OPTION);

            if (client != null && client.isConnected()) {
                client.respondTakeover(spectatorName, result == JOptionPane.YES_OPTION);
            }
        });
    }

    @Override
    public void onTakeoverResult(boolean success, String reason) {
        String message = success ? "接手成功：" + reason : "接手失败：" + reason;
        addSystemMessage(">>> " + message);

        if (!success) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    @Override
    public void onSystemMessage(String message) {
        addSystemMessage(">>> " + message);
    }

    @Override
    public void onError(String error) {
        addSystemMessage("!!! 错误: " + error);
    }

    @Override
    public void onDisconnected() {
        updateStatus("已断开连接");
        addSystemMessage("=== 与服务器断开连接 ===");

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "与服务器断开连接", "提示", JOptionPane.WARNING_MESSAGE);
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
            int result = JOptionPane.showConfirmDialog(
                    this,
                    inviterName + " 发起了对战邀请，是否同意开始游戏？",
                    "对战邀请",
                    JOptionPane.YES_NO_OPTION);

            if (client != null && client.isConnected()) {
                String response = (result == JOptionPane.YES_OPTION) ? Protocol.AGREE : Protocol.REFUSE;
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
            setBackground(new Color(220, 179, 92));

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
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));

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
            int[] starPoints = { 3, 7, 11 };
            for (int i : starPoints) {
                for (int j : starPoints) {
                    int x = BOARD_MARGIN + i * CELL_SIZE;
                    int y = BOARD_MARGIN + j * CELL_SIZE;
                    g2d.fillOval(x - 3, y - 3, 6, 6);
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

            if (isBlack) {
                g2d.setColor(Color.BLACK);
            } else {
                g2d.setColor(Color.WHITE);
            }

            g2d.fillOval(px - STONE_RADIUS, py - STONE_RADIUS, STONE_RADIUS * 2, STONE_RADIUS * 2);

            g2d.setColor(Color.BLACK);
            g2d.drawOval(px - STONE_RADIUS, py - STONE_RADIUS,
                    STONE_RADIUS * 2, STONE_RADIUS * 2);
        }

        /**
         * 绘制预览棋子
         */
        private void drawPreviewStone(Graphics2D g2d, int x, int y) {
            int px = BOARD_MARGIN + x * CELL_SIZE;
            int py = BOARD_MARGIN + y * CELL_SIZE;

            boolean isBlack = myColor.equals(Protocol.BLACK);

            if (isBlack) {
                g2d.setColor(new Color(0, 0, 0, 100));
            } else {
                g2d.setColor(new Color(255, 255, 255, 150));
            }

            g2d.fillOval(px - STONE_RADIUS, py - STONE_RADIUS,
                    STONE_RADIUS * 2, STONE_RADIUS * 2);

            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.drawOval(px - STONE_RADIUS, py - STONE_RADIUS,
                    STONE_RADIUS * 2, STONE_RADIUS * 2);
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