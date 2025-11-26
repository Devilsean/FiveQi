package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 五子棋游戏主菜单界面
 * 提供游戏入口、规则说明等功能
 */
public class MainMenu extends JFrame {

    private JButton startButton;
    private JButton aboutButton;
    private JButton exitButton;

    // 统一配色方案
    private static final Color PRIMARY_COLOR = new Color(52, 73, 94); // 深灰蓝
    private static final Color BG_COLOR = new Color(245, 247, 250); // 浅灰背景
    private static final Color TEXT_DARK = new Color(44, 62, 80); // 深色文字

    public MainMenu() {
        initComponents();
    }

    /**
     * 初始化界面组件
     */
    private void initComponents() {
        setTitle("五子棋联机对战");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 设置窗口大小
        setSize(650, 550);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // 标题面板
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // 底部信息面板
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    /**
     * 创建标题面板
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        // 游戏标题
        JLabel titleLabel = new JLabel("五子棋联机对战");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 52));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 副标题
        JLabel subtitleLabel = new JLabel("Gomoku Online Battle");
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(20));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(5));

        return panel;
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 20, 20)); // 2x2网格，间距20
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(45, 60, 45, 60));

        // 联机对战按钮
        startButton = createMenuButton("联机对战");
        startButton.addActionListener(e -> startOnlineGame());
        panel.add(startButton);

        // 本机对战按钮
        JButton localButton = createMenuButton("本机对战");
        localButton.addActionListener(e -> startLocalGame());
        panel.add(localButton);

        // 关于按钮
        aboutButton = createMenuButton("关于游戏");
        aboutButton.addActionListener(e -> showAbout());
        panel.add(aboutButton);

        // 退出按钮
        exitButton = createMenuButton("退出游戏");
        exitButton.addActionListener(e -> exitGame());
        panel.add(exitButton);

        return panel;
    }

    /**
     * 创建菜单按钮 - 简洁白色风格
     */
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 22));
        button.setPreferredSize(new Dimension(230, 90));
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

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
     * 创建底部信息面板
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 15, 20));

        JLabel footerLabel = new JLabel("版本 v1.3 | 支持房间系统、联机对战、本机对战、观战等功能");
        footerLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        footerLabel.setForeground(new Color(127, 140, 141));

        panel.add(footerLabel);

        return panel;
    }

    /**
     * 开始联机对战
     */
    private void startOnlineGame() {
        // 显示登录对话框
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField("8888");
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
                return;
            }

            try {
                int port = Integer.parseInt(portStr);

                // 创建客户端并连接
                Client client = new Client(host, port, null);
                if (client.connect()) {
                    client.login(username);

                    // 关闭主菜单
                    dispose();

                    // 打开房间大厅
                    SwingUtilities.invokeLater(() -> new RoomLobbyGUI(client, username));
                } else {
                    JOptionPane.showMessageDialog(this, "连接服务器失败！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "端口号格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 开始本机对战
     */
    private void startLocalGame() {
        // 关闭主菜单
        dispose();

        // 启动本机对战界面
        SwingUtilities.invokeLater(() -> new LocalGameGUI());
    }

    /**
     * 显示游戏规则
     */
    private void showRules() {
        String rules = "【五子棋游戏规则】\n\n" +
                "1. 基本规则\n" +
                "   •棋盘大小：15×15\n" +
                "   • 黑棋先手，白棋后手\n" +
                "   • 轮流在棋盘交叉点落子\n\n" +
                "2.胜负判定\n" +
                "   • 横向、纵向或斜向连成5子获胜\n" +
                "   • 严格5子连线（不含6子及以上）\n" +
                "   •棋盘下满无人获胜则平局\n\n" +
                "3. 特色功能\n" +
                "   • 支持多人联机对战\n" +
                "   • 支持观战功能\n" +
                "   • 支持执棋者休息、观战者接手\n" +
                "   • 游戏结束后可选择再来一局\n" +
                "   • 实时聊天功能\n\n" +
                "4. 操作说明\n" +
                "   • 点击棋盘交叉点落子\n" +
                "   • 鼠标悬停可预览棋子位置\n" +
                "   • 使用聊天框与对手交流\n" +
                "   • 执棋者可申请休息\n" +
                "   • 观战者可请求接手\n\n" +
                "祝你游戏愉快！🎮";

        JTextArea textArea = new JTextArea(rules);
        textArea.setEditable(false);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "游戏规则",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示关于信息
     */
    private void showAbout() {
        String about = "【关于五子棋联机对战】\n\n" +
                "项目名称：五子棋联机对战系统\n" +
                "版本：v1.3\n" +
                "开发语言：Java\n" +
                "GUI框架：Swing\n" +
                "网络协议：TCP/IP Socket\n\n" +
                "核心功能：\n" +
                "✅ 多人联机对战\n" +
                "✅ 自动匹配系统\n" +
                "✅ 观战功能\n" +
                "✅ 角色转换（接手/休息）\n" +
                "✅ 实时聊天\n" +
                "✅ 再来一局\n\n" +
                "技术特点：\n" +
                "• C/S 架构设计\n" +
                "• 多线程并发处理\n" +
                "• UTF-8 编码支持\n" +
                "• 协议化通信\n" +
                "• 完善的异常处理\n\n" +
                "开发时间：2025-11\n" +
                "适用场景：学习交流、休闲娱乐\n\n" +
                "感谢使用！❤️";

        JTextArea textArea = new JTextArea(about);
        textArea.setEditable(false);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 400));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "关于游戏",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 退出游戏
     */
    private void exitGame() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要退出游戏吗？",
                "退出确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动主菜单
        SwingUtilities.invokeLater(() -> new MainMenu());
    }
}