
package client;

import javax.swing.*;
import java.awt.*;

/**
 * 自定义美化弹窗，替代JOptionPane
 */
public class CustomDialog extends JDialog {

    public static final int PLAIN_MESSAGE = -1;
    public static final int ERROR_MESSAGE = 0;
    public static final int INFORMATION_MESSAGE = 1;
    public static final int WARNING_MESSAGE = 2;
    public static final int QUESTION_MESSAGE = 3;

    public static final int DEFAULT_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;
    public static final int CANCEL_OPTION = 2;
    public static final int OK_OPTION = 0;
    public static final int CLOSED_OPTION = -1;

    private int selectedOption;

    private CustomDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setUndecorated(true); // 移除原生窗口边框
        setBackground(new Color(0, 0, 0, 0)); // 背景透明
        setLayout(new BorderLayout());
    }

    private void buildDialog(Component content, int messageType) {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));

        // 1. 标题栏
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Theme.PRIMARY_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel(getTitle());
        titleLabel.setFont(Theme.BOLD_FONT);
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 2. 内容区域
        JPanel contentPanel = new JPanel(new BorderLayout(15, 10));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 添加图标
        Icon icon = getIconForType(messageType);
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            contentPanel.add(iconLabel, BorderLayout.WEST);
        }

        // 添加消息
        if (content instanceof JComponent) {
            ((JComponent) content).setOpaque(false);
        }
        contentPanel.add(content, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 3. 按钮栏
        // (将在具体方法中实现)

        add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private Icon getIconForType(int messageType) {
        if (messageType < 0 || messageType > 3) return null;
        // 使用UIManager提供的标准图标
        String[] iconKeys = {"OptionPane.errorIcon", "OptionPane.informationIcon", "OptionPane.warningIcon", "OptionPane.questionIcon"};
        return UIManager.getIcon(iconKeys[messageType]);
    }

    /**
     * 显示一个简单的消息对话框
     */
    public static void showMessageDialog(Component owner, String message, String title, int messageType) {
        Frame frame = (owner instanceof Frame) ? (Frame) owner : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, owner);
        CustomDialog dialog = new CustomDialog(frame, title, true);

        JLabel messageLabel = new JLabel("<html>" + message.replaceAll("\n", "<br>") + "</html>");
        messageLabel.setFont(Theme.NORMAL_FONT);
        messageLabel.setForeground(Theme.TEXT_COLOR);

        dialog.buildDialog(messageLabel, messageType);

        // 按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JButton okButton = Theme.createPrimaryButton("确定");
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);

        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * 显示一个确认对话框
     */
    public static int showConfirmDialog(Component owner, String message, String title, int optionType) {
        Frame frame = (owner instanceof Frame) ? (Frame) owner : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, owner);
        CustomDialog dialog = new CustomDialog(frame, title, true);
        dialog.selectedOption = CLOSED_OPTION;

        JLabel messageLabel = new JLabel("<html>" + message.replaceAll("\n", "<br>") + "</html>");
        messageLabel.setFont(Theme.NORMAL_FONT);
        messageLabel.setForeground(Theme.TEXT_COLOR);

        dialog.buildDialog(messageLabel, QUESTION_MESSAGE);

        // 按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        if (optionType == YES_NO_OPTION || optionType == YES_NO_CANCEL_OPTION) {
            JButton yesButton = Theme.createPrimaryButton("是");
            yesButton.addActionListener(e -> {
                dialog.selectedOption = YES_OPTION;
                dialog.dispose();
            });
            buttonPanel.add(yesButton);

            JButton noButton = Theme.createSecondaryButton("否");
            noButton.addActionListener(e -> {
                dialog.selectedOption = NO_OPTION;
                dialog.dispose();
            });
            buttonPanel.add(noButton);
        }

        if (optionType == OK_CANCEL_OPTION || optionType == YES_NO_CANCEL_OPTION) {
             if (optionType == OK_CANCEL_OPTION) {
                JButton okButton = Theme.createPrimaryButton("确定");
                okButton.addActionListener(e -> {
                    dialog.selectedOption = OK_OPTION;
                    dialog.dispose();
                });
                buttonPanel.add(okButton);
            }
            JButton cancelButton = Theme.createSecondaryButton("取消");
            cancelButton.addActionListener(e -> {
                dialog.selectedOption = CANCEL_OPTION;
                dialog.dispose();
            });
            buttonPanel.add(cancelButton);
        }


        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true); // 阻塞直到dispose

        return dialog.selectedOption;
    }


    /**
     * 显示一个输入对话框
     */
    public static String showInputDialog(Component owner, String message, String title, int messageType) {
        Frame frame = (owner instanceof Frame) ? (Frame) owner : (Frame) SwingUtilities.getAncestorOfClass(Frame.class, owner);
        CustomDialog dialog = new CustomDialog(frame, title, true);
        dialog.selectedOption = CLOSED_OPTION;

        // 内容
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel messageLabel = new JLabel("<html>" + message.replaceAll("\n", "<br>") + "</html>");
        messageLabel.setFont(Theme.NORMAL_FONT);
        messageLabel.setForeground(Theme.TEXT_COLOR);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField inputField = new JTextField(20);
        inputField.setFont(Theme.NORMAL_FONT);
        inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 限制输入框高度
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, inputField.getPreferredSize().height));


        content.add(messageLabel);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(inputField);

        dialog.buildDialog(content, messageType);

        // 按钮
        final String[] result = {null};
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JButton okButton = Theme.createPrimaryButton("确定");
        okButton.addActionListener(e -> {
            result[0] = inputField.getText();
            dialog.selectedOption = OK_OPTION;
            dialog.dispose();
        });
        buttonPanel.add(okButton);

        JButton cancelButton = Theme.createSecondaryButton("取消");
        cancelButton.addActionListener(e -> {
            dialog.selectedOption = CANCEL_OPTION;
            dialog.dispose();
        });
        buttonPanel.add(cancelButton);

        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        if (dialog.selectedOption == OK_OPTION) {
            return result[0];
        } else {
            return null;
        }
    }
}
