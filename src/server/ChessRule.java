package server;

import common.Protocol;

/**
 * 五子棋游戏规则引擎
 * 负责落子校验、胜负判定等核心逻辑
 */
public class ChessRule {
    
    private int[][] board; // 棋盘状态：0-空，1-黑棋，2-白棋
    private int moveCount; // 已落子数量
    
    public ChessRule() {
        board = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
        moveCount = 0;
    }
    
    /**
     * 检查位置是否为空
     */
    public boolean isEmpty(int x, int y) {
        if (!isValidPosition(x, y)) {
            return false;
        }
        return board[x][y] == 0;
    }
    
    /**
     * 检查坐标是否合法
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < Protocol.BOARD_SIZE && y >= 0 && y < Protocol.BOARD_SIZE;
    }
    
    /**
     * 落子
     * @param x 横坐标
     * @param y 纵坐标
     * @param color 棋子颜色（BLACK/WHITE）
     * @return 是否落子成功
     */
    public boolean placeStone(int x, int y, String color) {
        if (!isValidPosition(x, y) || !isEmpty(x, y)) {
            return false;
        }
        
        int stoneValue = color.equals(Protocol.BLACK) ? 1 : 2;
        board[x][y] = stoneValue;
        moveCount++;
        return true;
    }
    
    /**
     * 检查是否获胜（五子连线）
     * @param x 最后落子的横坐标
     * @param y 最后落子的纵坐标
     * @return 是否获胜
     */
    public boolean checkWin(int x, int y) {
        if (!isValidPosition(x, y) || board[x][y] == 0) {
            return false;
        }
        
        int stone = board[x][y];
        
        // 检查四个方向：横、竖、主对角线、副对角线
        return checkDirection(x, y, stone, 1, 0) ||  // 横向
               checkDirection(x, y, stone, 0, 1) ||  // 纵向
               checkDirection(x, y, stone, 1, 1) ||  // 主对角线
               checkDirection(x, y, stone, 1, -1);   // 副对角线
    }
    
    /**
     * 检查指定方向是否有五子连线
     * @param x 起始横坐标
     * @param y 起始纵坐标
     * @param stone 棋子类型
     * @param dx 横向增量
     * @param dy 纵向增量
     * @return 是否五子连线
     */
    private boolean checkDirection(int x, int y, int stone, int dx, int dy) {
        int count = 1; // 包含当前位置
        
        // 正方向计数
        int nx = x + dx;
        int ny = y + dy;
        while (isValidPosition(nx, ny) && board[nx][ny] == stone) {
            count++;
            nx += dx;
            ny += dy;
        }
        
        // 反方向计数
        nx = x - dx;
        ny = y - dy;
        while (isValidPosition(nx, ny) && board[nx][ny] == stone) {
            count++;
            nx -= dx;
            ny -= dy;
        }
        
        // 严格等于5子（不含6子及以上）
        return count == 5;
    }
    
    /**
     * 检查是否平局（棋盘下满）
     */
    public boolean checkDraw() {
        return moveCount >= Protocol.BOARD_SIZE * Protocol.BOARD_SIZE;
    }
    
    /**
     * 获取棋盘状态（用于同步）
     * @return 棋盘二维数组的副本
     */
    public int[][] getBoardState() {
        int[][] copy = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
        for (int i = 0; i < Protocol.BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, Protocol.BOARD_SIZE);
        }
        return copy;
    }
    
    /**
     * 获取指定位置的棋子
     * @return 0-空，1-黑棋，2-白棋
     */
    public int getStone(int x, int y) {
        if (!isValidPosition(x, y)) {
            return -1;
        }
        return board[x][y];
    }
    
    /**
     * 重置棋盘
     */
    public void reset() {
        board = new int[Protocol.BOARD_SIZE][Protocol.BOARD_SIZE];
        moveCount = 0;
    }
    
    /**
     * 获取已落子数量
     */
    public int getMoveCount() {
        return moveCount;
    }
}