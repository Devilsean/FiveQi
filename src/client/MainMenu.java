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
        setSize(700, 580);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Theme.BG_COLOR);

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
        panel.setBackground(Theme.PRIMARY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(50, 20, 50, 20));

        // 游戏标题
        JLabel titleLabel = new JLabel("五子棋联机对战");
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 副标题
        JLabel subtitleLabel = new JLabel("Gomoku Online Battle");
        subtitleLabel.setFont(Theme.SUBTITLE_FONT);
        subtitleLabel.setForeground(Theme.TEXT_LIGHT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(subtitleLabel);

        return panel;
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout()); // 使用GridBagLayout居中
        panel.setBackground(Theme.BG_COLOR);

        JPanel container = new JPanel(new GridLayout(2, 2, 30, 30));
        container.setBackground(Theme.BG_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // 联机对战按钮
        startButton = Theme.createPrimaryButton("联机对战");
        startButton.setPreferredSize(new Dimension(200, 80));
        startButton.addActionListener(e -> startOnlineGame());
        container.add(startButton);

        // 本机对战按钮
        JButton localButton = Theme.createSecondaryButton("本机对战");
        localButton.setPreferredSize(new Dimension(200, 80));
        localButton.addActionListener(e -> startLocalGame());
        container.add(localButton);

        // 关于按钮
        aboutButton = Theme.createStyledButton("关于游戏", Theme.PRIMARY_DARK, Color.WHITE);
        aboutButton.setPreferredSize(new Dimension(200, 80));
        aboutButton.addActionListener(e -> showAbout());
        container.add(aboutButton);

        // 退出按钮
        exitButton = Theme.createDangerButton("退出游戏");
        exitButton.setPreferredSize(new Dimension(200, 80));
        exitButton.addActionListener(e -> exitGame());
        container.add(exitButton);

        panel.add(container);

        return panel;
    }

    /**
     * 创建底部信息面板
     */
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Theme.BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JLabel footerLabel = new JLabel("版本 v1.4 | Design by FiveQi Team");
        footerLabel.setFont(Theme.SMALL_FONT);
        footerLabel.setForeground(Color.GRAY);

        panel.add(footerLabel);

        return panel;
    }

    /**
     * 开始联机对战
     */
    private void startOnlineGame() {
        // 显示登录对话框
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
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
     * 显示关于信息
     */
    private void showAbout() {
        String about = "【关于五子棋联机对战】\n\n" +
                "项目名称：五子棋联机对战系统\n" +
                "版本：v1.4\n" +
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
                "适用场景：学习交流、休闲娱乐\n\n" +
                "感谢使用！❤️";

        JTextArea textArea = new JTextArea(about);
        textArea.setEditable(false);
        textArea.setFont(Theme.NORMAL_FONT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(Theme.READONLY_BG);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

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
