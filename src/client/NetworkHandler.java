package client;

import common.Protocol;

/**
 * 网络消息处理器接口
 * 客户端GUI实现此接口来处理服务器消息
 */
public interface NetworkHandler {

    /**
     * 登录成功
     */
    void onLoginSuccess(String username);

    /**
     * 登录失败
     */
    void onLoginFail(String reason);

    /**
     * 等待匹配
     */
    void onWaiting();

    /**
     * 游戏开始
     * 
     * @param roomId       房间ID
     * @param myColor      自己的颜色（BLACK/WHITE）
     * @param opponentName 对手用户名
     */
    void onGameStart(String roomId, String myColor, String opponentName);

    /**
     * 加入房间（观战）
     * 
     * @param roomId  房间ID
     * @param player1 执棋者A
     * @param player2 执棋者B
     */
    void onJoinRoom(String roomId, String player1, String player2);

    /**
     * 落子成功
     * 
     * @param x        横坐标
     * @param y        纵坐标
     * @param color    棋子颜色
     * @param username 落子者用户名
     */
    void onMoveSuccess(int x, int y, String color, String username);

    /**
     * 落子失败
     * 
     * @param reason 失败原因
     */
    void onMoveFail(String reason);

    /**
     * 游戏结束
     * 
     * @param winnerColor 胜者颜色（NONE表示平局）
     * @param reason      结束原因
     */
    void onGameOver(String winnerColor, String reason);

    /**
     * 聊天消息
     * 
     * @param sender    发送者
     * @param timestamp 时间戳
     * @param message   消息内容
     */
    void onChatMessage(String sender, String timestamp, String message);

    /**
     * 角色变更通知
     * 
     * @param username 用户名
     * @param newRole  新角色
     */
    void onRoleChange(String username, String newRole);

    /**
     * 接手请求通知
     * 
     * @param spectatorName 观战者用户名
     */
    void onTakeoverAsk(String spectatorName);

    /**
     * 接手结果通知
     * 
     * @param success 是否成功
     * @param reason  原因
     */
    void onTakeoverResult(boolean success, String reason);

    /**
     * 系统消息
     * 
     * @param message 消息内容
     */
    void onSystemMessage(String message);

    /**
     * 错误消息
     * 
     * @param error 错误描述
     */
    void onError(String error);

    /**
     * 连接断开
     */
    /**
     * 房间创建成功
     * 
     * @param roomId 房间ID
     */
    void onRoomCreated(String roomId);

    /**
     * 房间列表更新
     * 
     * @param roomListText 房间列表文本
     */
    void onRoomListUpdate(String roomListText);

    /**
     * 等待对手加入房间
     * 
     * @param roomId 房间ID
     */
    void onWaitingForOpponent(String roomId);

    /**
     * 席位状态更新
     *
     * @param blackSeat      黑棋席玩家名（"空"表示无人）
     * @param whiteSeat      白棋席玩家名（"空"表示无人）
     * @param spectatorCount 观战者数量
     */
    void onSeatUpdate(String blackSeat, String whiteSeat, int spectatorCount);

    /**
     * 对战邀请通知
     *
     * @param inviterName 发起者用户名
     */
    void onBattleInviteNotify(String inviterName);

    /**
     * 对战开始
     */
    void onBattleStart();

    /**
     * 棋盘重置（游戏结束后）
     */
    void onBoardReset();

    void onDisconnected();
}
