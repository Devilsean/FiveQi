package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;

/**
 * 五子棋游戏主菜单界面
 * 提供游戏入口、规则说明等功能
 */
public class MainMenu extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private static final Preferences prefs = Preferences.userNodeForPackage(MainMenu.class);

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
        setSize(800, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel menuView = createMenuView();
        JPanel loginView = createLoginView();

        mainPanel.add(menuView, "Menu");
        mainPanel.add(loginView, "Login");

        add(mainPanel);
        setVisible(true);
    }

    /**
     * 创建主菜单视图
     */
    private JPanel createMenuView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createTitlePanel(), BorderLayout.NORTH);
        panel.add(createButtonPanel(), BorderLayout.CENTER);
        panel.add(createFooterPanel(), BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 创建登录视图 - 美化版本
     */
    private JPanel createLoginView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_COLOR);

        // 顶部标题区域
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(Theme.BG_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel titleLabel = new JLabel("连接服务器");
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Theme.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Connect to Server");
        subtitleLabel.setFont(Theme.SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(120, 120, 120));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(15));
        titlePanel.add(subtitleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);

        // 中间表单区域 - 卡片式设计
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Theme.BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 30, 30));

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                Theme.createShadowBorder(),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        // 设置表单卡片的最大宽度，与主页面按钮区域宽度一致
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        // 表单标题
        JLabel formTitle = new JLabel("登录信息");
        formTitle.setFont(Theme.H2_FONT);
        formTitle.setForeground(Theme.TEXT_COLOR);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(15));

        // 输入字段 - 从配置加载
        JTextField hostField = createStyledTextField(prefs.get("server_host", "localhost"));
        JTextField portField = createStyledTextField(prefs.get("server_port", "8888"));
        JTextField usernameField = createStyledTextField("");

        // 使用两列布局减少垂直空间
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 20, 12));
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        fieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        fieldsPanel.add(createFormField(" 服务器地址", hostField, "请输入服务器IP或域名"));
        fieldsPanel.add(createFormField(" 端口号", portField, "默认端口 8888"));
        fieldsPanel.add(createFormField(" 用户名", usernameField, "输入您的游戏昵称"));

        // 添加记住配置选项
        JPanel rememberPanel = new JPanel();
        rememberPanel.setLayout(new BoxLayout(rememberPanel, BoxLayout.Y_AXIS));
        rememberPanel.setBackground(Color.WHITE);
        rememberPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rememberLabel = new JLabel(" ");
        rememberLabel.setFont(Theme.BOLD_FONT);
        rememberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox rememberCheckBox = new JCheckBox("记住服务器配置");
        rememberCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        rememberCheckBox.setBackground(Color.WHITE);
        rememberCheckBox.setForeground(Theme.TEXT_COLOR);
        rememberCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        rememberCheckBox.setSelected(prefs.getBoolean("remember_config", true));

        rememberPanel.add(rememberLabel);
        rememberPanel.add(Box.createVerticalStrut(9));
        rememberPanel.add(new JLabel(" "));
        rememberPanel.add(Box.createVerticalStrut(4));
        rememberPanel.add(rememberCheckBox);

        fieldsPanel.add(rememberPanel);

        formCard.add(fieldsPanel);
        formCard.add(Box.createVerticalStrut(20));

        // 按钮区域
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginButton = Theme.createPrimaryButton("连 接");
        loginButton.setPreferredSize(new Dimension(0, 42));
        loginButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        loginButton.addActionListener(e -> {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String username = usernameField.getText().trim();

            if (username.isEmpty()) {
                CustomDialog.showMessageDialog(this, "用户名不能为空！", "错误", CustomDialog.ERROR_MESSAGE);
                usernameField.requestFocus();
                return;
            }

            if (host.isEmpty()) {
                CustomDialog.showMessageDialog(this, "服务器地址不能为空！", "错误", CustomDialog.ERROR_MESSAGE);
                hostField.requestFocus();
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) {
                    CustomDialog.showMessageDialog(this, "端口号必须在 1-65535 之间！", "错误", CustomDialog.ERROR_MESSAGE);
                    portField.requestFocus();
                    return;
                }

                // 保存配置
                if (rememberCheckBox.isSelected()) {
                    prefs.put("server_host", host);
                    prefs.put("server_port", portStr);
                    prefs.putBoolean("remember_config", true);
                } else {
                    prefs.remove("server_host");
                    prefs.remove("server_port");
                    prefs.putBoolean("remember_config", false);
                }

                Client client = new Client(host, port, null);
                if (client.connect()) {
                    client.login(username);
                    dispose();
                    SwingUtilities.invokeLater(() -> new RoomLobbyGUI(client, username));
                } else {
                    CustomDialog.showMessageDialog(this, "连接服务器失败！\n请检查服务器地址和端口是否正确。", "错误",
                            CustomDialog.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                CustomDialog.showMessageDialog(this, "端口号格式错误！\n请输入1-65535之间的数字。", "错误", CustomDialog.ERROR_MESSAGE);
                portField.requestFocus();
            }
        });

        JButton backButton = Theme.createSecondaryButton("返 回");
        backButton.setPreferredSize(new Dimension(0, 42));
        backButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Menu"));

        buttonPanel.add(loginButton);
        buttonPanel.add(backButton);
        formCard.add(buttonPanel);

        // 提示信息
        formCard.add(Box.createVerticalStrut(15));
        JLabel tipLabel = new JLabel("<html><center> 提示：首次连接请确保服务器已启动</center></html>");
        tipLabel.setFont(Theme.SMALL_FONT);
        tipLabel.setForeground(new Color(120, 120, 120));
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formCard.add(tipLabel);

        centerPanel.add(formCard, gbc);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建样式化的文本输入框
     */
    private JTextField createStyledTextField(String defaultText) {
        JTextField textField = new JTextField(defaultText);
        textField.setFont(Theme.NORMAL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        // 添加焦点效果
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.PRIMARY_COLOR, 2),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });

        return textField;
    }

    /**
     * 创建表单字段（带图标和提示）
     */
    private JPanel createFormField(String label, JTextField textField, String placeholder) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(Theme.BOLD_FONT);
        labelComponent.setForeground(Theme.TEXT_COLOR);
        labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel placeholderLabel = new JLabel(placeholder);
        placeholderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        placeholderLabel.setForeground(new Color(150, 150, 150));
        placeholderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(labelComponent);
        panel.add(Box.createVerticalStrut(4));
        panel.add(placeholderLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(textField);

        return panel;
    }

    /**
     * 创建标题面板
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel titleLabel = new JLabel("五子棋联机对战");
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Theme.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gomoku Online Battle");
        subtitleLabel.setFont(Theme.SUBTITLE_FONT);
        subtitleLabel.setForeground(new Color(120, 120, 120));
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG_COLOR);

        JPanel container = new JPanel(new GridLayout(2, 2, 30, 30));
        container.setBackground(Theme.BG_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JButton startButton = Theme.createPrimaryButton("联机对战");
        startButton.setPreferredSize(new Dimension(200, 80));
        startButton.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
        container.add(startButton);

        JButton localButton = Theme.createSecondaryButton("本机对战");
        localButton.setPreferredSize(new Dimension(200, 80));
        localButton.addActionListener(e -> startLocalGame());
        container.add(localButton);

        JButton aboutButton = Theme.createStyledButton("关于游戏", Theme.PRIMARY_DARK, Color.WHITE);
        aboutButton.setPreferredSize(new Dimension(200, 80));
        aboutButton.addActionListener(e -> showAbout());
        container.add(aboutButton);

        JButton exitButton = Theme.createDangerButton("退出游戏");
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

        JLabel footerLabel = new JLabel("版本 v1.5");
        footerLabel.setFont(Theme.SMALL_FONT);
        footerLabel.setForeground(Color.GRAY);

        panel.add(footerLabel);

        return panel;
    }

    private void startLocalGame() {
        dispose();
        SwingUtilities.invokeLater(LocalGameGUI::new);
    }

    private void showAbout() {
        String aboutText = "<html><body style='width: 300px;'>"
                + "<h1>关于五子棋联机对战</h1>"
                + "<p>这是一款基于Java开发的五子棋游戏，支持联机对战和本机对战。</p>"
                + "<p><b>版本:</b> 1.5</p>"
                + "</html>";
        CustomDialog.showMessageDialog(this, aboutText, "关于", CustomDialog.INFORMATION_MESSAGE);
    }

    private void exitGame() {
        int result = CustomDialog.showConfirmDialog(this, "确定要退出游戏吗？", "退出", CustomDialog.YES_NO_OPTION);
        if (result == CustomDialog.YES_OPTION) {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        // 使用SwingUtilities.invokeLater确保线程安全
        SwingUtilities.invokeLater(MainMenu::new);
    }
}
