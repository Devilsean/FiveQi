package client;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * 统一的主题和样式工具类
 */
public class Theme {

    // 配色方案
    public static final Color PRIMARY_COLOR = new Color(52, 152, 219); // 亮蓝
    public static final Color PRIMARY_DARK = new Color(41, 128, 185); // 深蓝
    public static final Color SECONDARY_COLOR = new Color(46, 204, 113); // 翠绿
    public static final Color ACCENT_COLOR = new Color(231, 76, 60); // 红色
    public static final Color DANGER_COLOR = new Color(231, 76, 60); // 危险/错误颜色(红色)
    public static final Color BG_COLOR = new Color(245, 246, 250); // 浅灰背景
    public static final Color TEXT_COLOR = new Color(44, 62, 80); // 深色文字
    public static final Color TEXT_LIGHT = new Color(236, 240, 241); // 浅色文字
    public static final Color TEXT_SECONDARY = new Color(127, 140, 141); // 次要文字颜色(灰色)
    public static final Color BOARD_COLOR = new Color(222, 184, 135); // 木纹色
    public static final Color BOARD_LINE_COLOR = new Color(80, 60, 40); // 棋盘线条颜色
    public static final Color BORDER_COLOR = new Color(220, 220, 220); // 边框颜色
    public static final Color READONLY_BG = new Color(250, 250, 250); // 只读文本框背景

    // 字体
    public static final Font TITLE_FONT = new Font("微软雅黑", Font.BOLD, 36);
    public static final Font SUBTITLE_FONT = new Font("微软雅黑", Font.PLAIN, 18);
    public static final Font H1_FONT = new Font("微软雅黑", Font.BOLD, 24);
    public static final Font H2_FONT = new Font("微软雅黑", Font.BOLD, 18);
    public static final Font BOLD_FONT = new Font("微软雅黑", Font.BOLD, 14); // Added
    public static final Font NORMAL_FONT = new Font("微软雅黑", Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font("微软雅黑", Font.PLAIN, 12);
    public static final Font BUTTON_FONT = new Font("微软雅黑", Font.BOLD, 16);

    /**
     * 创建主按钮 (蓝色)
     */
    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY_COLOR, Color.WHITE);
    }

    /**
     * 创建次要按钮 (绿色)
     */
    public static JButton createSecondaryButton(String text) {
        return createStyledButton(text, SECONDARY_COLOR, Color.WHITE);
    }

    /**
     * 创建危险/退出按钮 (红色)
     */
    public static JButton createDangerButton(String text) {
        return createStyledButton(text, ACCENT_COLOR, Color.WHITE);
    }

    /**
     * 创建通用样式按钮
     */
    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color currentBg = getBackground();

                if (getModel().isPressed()) {
                    g2d.setColor(currentBg.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(currentBg.brighter());
                } else {
                    g2d.setColor(currentBg);
                }

                // 绘制圆角矩形
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                // 绘制文字
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);

                g2d.dispose();
            }
        };

        button.setFont(BUTTON_FONT);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));

        return button;
    }

    /**
     * 创建带阴影的面板
     */
    public static JPanel createShadowPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 简单的阴影效果可以根据需要添加
            }
        };
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    /**
     * 创建阴影边框
     */
    public static Border createShadowBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    /**
     * 绘制棋盘背景（木纹效果）
     */
    public static void drawBoardBackground(Graphics2D g2d, int width, int height) {
        // 填充底色
        g2d.setColor(BOARD_COLOR);
        g2d.fillRect(0, 0, width, height);

        // 可以在这里添加噪点或线条模拟木纹，这里暂时用简单的颜色
        // 添加边缘阴影
        g2d.setColor(new Color(0, 0, 0, 20));
        g2d.fillRect(width - 5, 5, 5, height);
        g2d.fillRect(5, height - 5, width, 5);
    }

    /**
     * 绘制3D棋子
     */
    public static void drawStone(Graphics2D g2d, int x, int y, int radius, boolean isBlack) {
        // 阴影
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x + 2, y + 2, radius * 2, radius * 2);

        // 棋子主体
        if (isBlack) {
            // 黑棋：径向渐变
            RadialGradientPaint paint = new RadialGradientPaint(
                    new Point(x + radius - radius / 3, y + radius - radius / 3),
                    radius * 2,
                    new float[] { 0.0f, 1.0f },
                    new Color[] { new Color(80, 80, 80), Color.BLACK });
            g2d.setPaint(paint);
        } else {
            // 白棋：径向渐变
            RadialGradientPaint paint = new RadialGradientPaint(
                    new Point(x + radius - radius / 3, y + radius - radius / 3),
                    radius * 2,
                    new float[] { 0.0f, 1.0f },
                    new Color[] { Color.WHITE, new Color(200, 200, 200) });
            g2d.setPaint(paint);
        }
        g2d.fillOval(x, y, radius * 2, radius * 2);
    }
}
