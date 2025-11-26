package common;

/**
 * 通信协议常量定义
 * 服务器与客户端使用统一的协议格式进行通信
 */
public class Protocol {

    // ==================== 客户端 -> 服务器 ====================

    /** 登录请求：LOGIN|用户名 */
    public static final String LOGIN = "LOGIN";

    /** 落子请求：MOVE|x|y */
    public static final String MOVE = "MOVE";

    /** 聊天消息：CHAT|消息内容 */
    public static final String CHAT = "CHAT";

    /** 申请休息：REST_REQUEST */
    public static final String REST_REQUEST = "REST_REQUEST";

    /** 请求接手：TAKEOVER_REQUEST */
    public static final String TAKEOVER_REQUEST = "TAKEOVER_REQUEST";

    /** 接手响应：TAKEOVER_RESPONSE|观战者用户名|同意/拒绝 */
    public static final String TAKEOVER_RESPONSE = "TAKEOVER_RESPONSE";

    /** 退出房间：QUIT */
    public static final String QUIT = "QUIT";

    /** 准备再来一局：READY_FOR_NEXT */
    public static final String READY_FOR_NEXT = "READY_FOR_NEXT";

    /** 请求观战：SPECTATE|房间ID */
    public static final String SPECTATE = "SPECTATE";

    /** 创建房间：CREATE_ROOM */
    public static final String CREATE_ROOM = "CREATE_ROOM";

    /** 快速加入房间：QUICK_JOIN */
    public static final String QUICK_JOIN = "QUICK_JOIN";

    /** 加入指定房间：JOIN_ROOM_BY_ID|房间ID */
    public static final String JOIN_ROOM_BY_ID = "JOIN_ROOM_BY_ID";

    /** 获取房间列表：GET_ROOM_LIST */
    public static final String GET_ROOM_LIST = "GET_ROOM_LIST";

    /** 请求房间状态：REQUEST_ROOM_STATE */
    public static final String REQUEST_ROOM_STATE = "REQUEST_ROOM_STATE";

    /** 坐下黑棋席：SIT_BLACK */
    public static final String SIT_BLACK = "SIT_BLACK";

    /** 坐下白棋席：SIT_WHITE */
    public static final String SIT_WHITE = "SIT_WHITE";

    /** 进入观战席：SIT_SPECTATOR */
    public static final String SIT_SPECTATOR = "SIT_SPECTATOR";

    /** 发起对战邀请：BATTLE_INVITE */
    public static final String BATTLE_INVITE = "BATTLE_INVITE";

    /** 响应对战邀请：BATTLE_RESPONSE|同意/拒绝 */
    public static final String BATTLE_RESPONSE = "BATTLE_RESPONSE";

    // ==================== 服务器 -> 客户端 ====================

    /** 登录成功：LOGIN_SUCCESS|用户名 */
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";

    /** 登录失败：LOGIN_FAIL|原因 */
    public static final String LOGIN_FAIL = "LOGIN_FAIL";

    /** 等待匹配：WAITING */
    public static final String WAITING = "WAITING";

    /** 游戏开始：GAME_START|房间ID|你的颜色(BLACK/WHITE)|对手用户名 */
    public static final String GAME_START = "GAME_START";

    /** 加入房间（观战）：JOIN_ROOM|房间ID|执棋者A|执棋者B */
    public static final String JOIN_ROOM = "JOIN_ROOM";

    /** 落子成功：MOVE_SUCCESS|x|y|颜色|落子者用户名 */
    public static final String MOVE_SUCCESS = "MOVE_SUCCESS";

    /** 落子失败：MOVE_FAIL|原因 */
    public static final String MOVE_FAIL = "MOVE_FAIL";

    /** 游戏结束：GAME_OVER|胜者颜色|原因(WIN/DRAW) */
    public static final String GAME_OVER = "GAME_OVER";

    /** 聊天消息广播：CHAT_MSG|发送者|时间戳|消息内容 */
    public static final String CHAT_MSG = "CHAT_MSG";

    /** 角色变更通知：ROLE_CHANGE|用户名|新角色(PLAYER_BLACK/PLAYER_WHITE/SPECTATOR) */
    public static final String ROLE_CHANGE = "ROLE_CHANGE";

    /** 接手请求通知：TAKEOVER_ASK|观战者用户名 */
    public static final String TAKEOVER_ASK = "TAKEOVER_ASK";

    /** 接手结果通知：TAKEOVER_RESULT|成功/失败|原因 */
    public static final String TAKEOVER_RESULT = "TAKEOVER_RESULT";

    /** 错误消息：ERROR|错误描述 */
    public static final String ERROR = "ERROR";

    /** 系统消息：SYSTEM|消息内容 */
    public static final String SYSTEM = "SYSTEM";

    /** 房间创建成功：ROOM_CREATED|房间ID */
    public static final String ROOM_CREATED = "ROOM_CREATED";

    /** 房间列表：ROOM_LIST|房间数量|房间1信息|房间2信息|... */
    public static final String ROOM_LIST = "ROOM_LIST";

    /** 等待对手加入：WAITING_FOR_OPPONENT|房间ID */
    public static final String WAITING_FOR_OPPONENT = "WAITING_FOR_OPPONENT";

    /** 席位状态更新：SEAT_UPDATE|黑棋席玩家名|白棋席玩家名|观战者数量 */
    public static final String SEAT_UPDATE = "SEAT_UPDATE";

    /** 对战邀请通知：BATTLE_INVITE_NOTIFY|发起者用户名 */
    public static final String BATTLE_INVITE_NOTIFY = "BATTLE_INVITE_NOTIFY";

    /** 对战开始：BATTLE_START */
    public static final String BATTLE_START = "BATTLE_START";

    /** 棋盘重置：BOARD_RESET */
    public static final String BOARD_RESET = "BOARD_RESET";

    // ==================== 常量定义 ====================

    /** 棋子颜色 - 黑色 */
    public static final String BLACK = "BLACK";

    /** 棋子颜色 - 白色 */
    public static final String WHITE = "WHITE";

    /** 角色 - 黑棋执棋者 */
    public static final String PLAYER_BLACK = "PLAYER_BLACK";

    /** 角色 - 白棋执棋者 */
    public static final String PLAYER_WHITE = "PLAYER_WHITE";

    /** 角色 - 观战者 */
    public static final String SPECTATOR = "SPECTATOR";

    /** 接手响应 - 同意 */
    public static final String AGREE = "AGREE";

    /** 接手响应 - 拒绝 */
    public static final String REFUSE = "REFUSE";

    /** 胜利原因 - 五子连线 */
    public static final String WIN = "WIN";

    /** 平局原因 - 棋盘下满 */
    public static final String DRAW = "DRAW";

    /** 消息分隔符 */
    public static final String DELIMITER = "|";

    /** 消息结束符（换行符） */
    public static final String MESSAGE_END = "\n";

    /** 棋盘大小 */
    public static final int BOARD_SIZE = 15;

    /** 默认服务器端口 */
    public static final int DEFAULT_PORT = 8888;

    /**
     * 构建协议消息
     * 
     * @param parts 消息各部分
     * @return 完整的协议消息（带换行符）
     */
    public static String buildMessage(String... parts) {
        return String.join(DELIMITER, parts) + MESSAGE_END;
    }

    /**
     * 解析协议消息
     * 
     * @param message 原始消息
     * @return 消息各部分数组
     */
    public static String[] parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return new String[0];
        }
        // 移除末尾的换行符
        message = message.trim();
        return message.split("\\" + DELIMITER);
    }
}
