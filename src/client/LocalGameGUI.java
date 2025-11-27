package client;

import common.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 本机对战GUI界面
 * 支持黑白双方在同一台电脑上交替下棋
 */
public class LocalGameGUI extends JFrame {

    // 游戏状态
    private int[][] board; // 0-空，1-黑棋，2-白棋
    private String currentTurn; // 当前回合（BLACK/WHITE）
    private boolean gameOver; // 游戏是否结束
    private Point previewStone; // 预览棋子位置

    // GUI组件
    private ChessBoardPanel boardPanel;
    private JLabel statusLabel;
    private JButton restartButton;
    private JButton backButton;

    // 游戏规则引擎
    private server.ChessRule chessRule;

    // 常量
    private static final int CELL_SIZE = 40;
    private static final int BOARD_MARGIN = 30;
    private static final int STONE_RADIUS = 16;

    public LocalGameGUI() {
        initComponents();
        initGame();
    }

    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        setTitle("五子棋 - 本机对战");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Theme.BG_COLOR);

        // 创建棋盘面板
        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.setBackground(Theme.BG_COLOR);

        boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(
                Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2,
                Protocol.BOARD_SIZE * CELL_SIZE + BOARD_MARGIN * 2));
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_COLOR.darker(), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        boardContainer.add(boardPanel, BorderLayout.CENTER);
        add(boardContainer, BorderLayout.CENTER);

        // 创建右侧面板
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setPreferredSize(new Dimension(250, 0));
        rightPanel.setBackground(Theme.BG_COLOR);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 状态标签
        statusLabel = new JLabel("黑棋先手", SwingConstants.CENTER);
        statusLabel.setFont(Theme.H2_FONT);
        statusLabel.setForeground(Theme.TEXT_COLOR);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setBorder(Theme.createShadowBorder());
        rightPanel.add(statusLabel, BorderLayout.NORTH);

        // 信息面板
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Theme.BG_COLOR);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                null, "游戏信息",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                Theme.BOLD_FONT, Theme.TEXT_COLOR));

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(Theme.SMALL_FONT);
        infoArea.setBackground(Theme.READONLY_BG);
        infoArea.setForeground(Theme.TEXT_COLOR);
        infoArea.setText(
                "本机对战模式\n\n" +
                        "规则：\n" +
                        "• 黑棋先手\n" +
                        "• 轮流落子\n" +
                        "• 五子连线获胜\n\n" +
                        "操作：\n" +
                        "• 点击棋盘落子\n" +
                        "• 鼠标悬停预览\n");
        infoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(infoArea);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBorder(null);
        infoPanel.add(scrollPane);

        rightPanel.add(infoPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBackground(Theme.BG_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        restartButton = Theme.createPrimaryButton("重新开始");
        restartButton.addActionListener(e -> restartGame());

        backButton = Theme.createSecondaryButton("返回主菜单");
        backButton.addActionListener(e -> backToMenu());

        buttonPanel.add(restartButton);
        buttonPanel.add(backButton);

        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 初始化游戏
     */
    private void initGame() {
        board = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
        chessRule = new server.ChessRule();
        currentTurn = Protocol.BLACK;
        gameOver = false;
        previewStone = null;
        updateStatus();
        boardPanel.repaint();
    }

    /**
     * 更新状态标签
     */
    private void updateStatus() {
        if (gameOver) {
            return;
        }

        String turnText = currentTurn.equals(Protocol.BLACK) ? "黑棋" : "白棋";
        statusLabel.setText("轮到 " + turnText + " 落子");
    }

    /**
     * 处理落子
     */
    private void handleMove(int x, int y) {
        if (gameOver) {
            return;
        }

        // 检查位置是否为空
        if (!chessRule.isEmpty(x, y)) {
            JOptionPane.showMessageDialog(this, "该位置已有棋子！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 落子
        if (chessRule.placeStone(x, y, currentTurn)) {
            int stoneValue = currentTurn.equals(Protocol.BLACK) ? 1 : 2;
            board[x][y] = stoneValue;
            previewStone = null;
            boardPanel.repaint();

            // 检查胜负
            if (chessRule.checkWin(x, y)) {
                gameOver = true;
                String winner = currentTurn.equals(Protocol.BLACK) ? "黑棋" : "白棋";
                statusLabel.setText(winner + " 获胜！");

                SwingUtilities.invokeLater(() -> {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            winner + " 获胜！\n\n是否再来一局？",
                            "游戏结束",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);

                    if (result == JOptionPane.YES_OPTION) {
                        restartGame();
                    }
                });
            } else if (chessRule.checkDraw()) {
                gameOver = true;
                statusLabel.setText("平局！");

                SwingUtilities.invokeLater(() -> {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "平局！棋盘已下满。\n\n是否再来一局？",
                            "游戏结束",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);

                    if (result == JOptionPane.YES_OPTION) {
                        restartGame();
                    }
                });
            } else {
                // 切换回合
                currentTurn = currentTurn.equals(Protocol.BLACK) ? Protocol.WHITE : Protocol.BLACK;
                updateStatus();
            }
        }
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要重新开始吗？",
                "确认",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            initGame();
        }
    }

    /**
     * 返回主菜单
     */
    private void backToMenu() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要返回主菜单吗？",
                "确认",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new MainMenu());
        }
    }

    // ==================== 棋盘面板 ====================

    /**
     * 棋盘绘制面板
     */
    private class ChessBoardPanel extends JPanel {

        public ChessBoardPanel() {
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
            if (previewStone != null && !gameOver) {
                drawPreviewStone(g2d, previewStone.x, previewStone.y);
            }
        }

        /**
         * 绘制棋盘网格
         */
        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(Theme.BOARD_LINE_COLOR);
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
            g2d.setColor(Theme.BOARD_LINE_COLOR);
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
            int px = BOARD_MARGIN + x * CELL_SIZE - STONE_RADIUS;
            int py = BOARD_MARGIN + y * CELL_SIZE - STONE_RADIUS;
            Theme.drawStone(g2d, px, py, STONE_RADIUS, isBlack);
        }

        /**
         * 绘制预览棋子
         */
        private void drawPreviewStone(Graphics2D g2d, int x, int y) {
            int px = BOARD_MARGIN + x * CELL_SIZE - STONE_RADIUS;
            int py = BOARD_MARGIN + y * CELL_SIZE - STONE_RADIUS;

            boolean isBlack = currentTurn.equals(Protocol.BLACK);

            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            Theme.drawStone(g2d, px, py, STONE_RADIUS, isBlack);
            g2d.setComposite(originalComposite);
        }

        /**
         * 处理棋盘点击
         */
        private void handleBoardClick(int px, int py) {
            if (gameOver) {
                return;
            }

            Point pos = pixelToBoard(px, py);
            if (pos != null && board[pos.x][pos.y] == 0) {
                handleMove(pos.x, pos.y);
            }
        }

        /**
         * 处理鼠标悬停
         */
        private void handleBoardHover(int px, int py) {
            if (gameOver) {
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
    }

    /**
     * 主方法（用于测试）
     */
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 启动本机对战GUI
        SwingUtilities.invokeLater(() -> new LocalGameGUI());
    }
}