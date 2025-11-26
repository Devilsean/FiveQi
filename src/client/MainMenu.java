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
    private JButton rulesButton;
    private JButton aboutButton;
    private JButton exitButton;

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
        setSize(600, 500);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 245));

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
        panel.setBackground(new Color(70, 130, 180));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        // 游戏标题
        JLabel titleLabel = new JLabel("五子棋联机对战");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 副标题
        JLabel subtitleLabel = new JLabel("Gomoku Online Battle");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        subtitleLabel.setForeground(new Color(230, 230, 230));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(10));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        // 联机对战按钮
        startButton = createMenuButton("🌐 联机对战", new Color(76, 175, 80));
        startButton.addActionListener(e -> startOnlineGame());
        gbc.gridy = 0;
        panel.add(startButton, gbc);

        // 本机对战按钮
        JButton localButton = createMenuButton("🎮 本机对战", new Color(255, 152, 0));
        localButton.addActionListener(e -> startLocalGame());
        gbc.gridy = 1;
        panel.add(localButton, gbc);

        // 游戏规则按钮
        rulesButton = createMenuButton("📖 游戏规则", new Color(33, 150, 243));
        rulesButton.addActionListener(e -> showRules());
        gbc.gridy = 2;
        panel.add(rulesButton, gbc);

        // 关于按钮
        aboutButton = createMenuButton("ℹ️ 关于游戏", new Color(156, 39, 176));
        aboutButton.addActionListener(e -> showAbout());
        gbc.gridy = 3;
        panel.add(aboutButton, gbc);

        // 退出按钮
        exitButton = createMenuButton("🚪 退出游戏", new Color(244, 67, 54));
        exitButton.addActionListener(e -> exitGame());
        gbc.gridy = 4;
        panel.add(exitButton, gbc);

        return panel;
    }

    /**
     * 创建菜单按钮
     */
    private JButton createMenuButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 20));
        button.setPreferredSize(new Dimension(300, 60));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    /**
     * 创建底部信息面板
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel footerLabel = new JLabel("版本 v1.3 | 支持房间ID系统、联机对战、本机对战、观战等功能");
        footerLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        footerLabel.setForeground(Color.GRAY);

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
                "   • 棋盘大小：15×15\n" +
                "   • 黑棋先手，白棋后手\n" +
                "   • 轮流在棋盘交叉点落子\n\n" +
                "2. 胜负判定\n" +
                "   • 横向、纵向或斜向连成5子获胜\n" +
                "   • 严格5子连线（不含6子及以上）\n" +
                "   • 棋盘下满无人获胜则平局\n\n" +
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