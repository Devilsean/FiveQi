package server;

import common.Protocol;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;

/**
 * 游戏会话类 - 重新设计支持席位系统
 * 房间内有三种席位：黑棋席、白棋席、观战席
 * 玩家可以自由切换席位，黑白席都有人后可发起对战邀请
 */
public class GameSession {

    private String roomId; // 房间ID
    private ChessRule chessRule; // 游戏规则引擎

    // 席位管理
    private ClientHandler blackSeat; // 黑棋席位（可能为空）
    private ClientHandler whiteSeat; // 白棋席位（可能为空）
    private Map<String, ClientHandler> spectators; // 观战席

    // 游戏状态
    private boolean battleStarted; // 对战是否已开始
    private boolean gameOver; // 游戏是否结束
    private String currentTurn; // 当前回合（BLACK/WHITE）
    private ClientHandler battleInviter; // 对战邀请发起者

    private SimpleDateFormat dateFormat; // 时间戳格式

    /**
     * 创建房间（创建者自动进入观战席）
     */
    public GameSession(String roomId, ClientHandler creator) {
        this.roomId = roomId;
        this.chessRule = new ChessRule();
        this.spectators = new ConcurrentHashMap<>();
        this.battleStarted = false;
        this.gameOver = false;
        this.currentTurn = Protocol.BLACK;
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");

        // 创建者进入观战席
        spectators.put(creator.getUsername(), creator);
        creator.setRole(Protocol.SPECTATOR);
        creator.setGameSession(this);

        // 通知创建者房间已创建（客户端收到后会主动请求房间状态）
        creator.sendMessage(Protocol.buildMessage(
                Protocol.ROOM_CREATED,
                roomId));

        System.out.println("房间 " + roomId + " 已创建，创建者：" + creator.getUsername());
    }

    /**
     * 玩家加入房间（自动进入观战席）
     */
    public synchronized void addMember(ClientHandler member) {
        spectators.put(member.getUsername(), member);
        member.setRole(Protocol.SPECTATOR);
        member.setGameSession(this);

        // 通知加入成功（客户端收到后会主动请求房间状态）
        member.sendMessage(Protocol.buildMessage(
                Protocol.JOIN_ROOM,
                roomId,
                getBlackSeatName(),
                getWhiteSeatName()));

        // 广播席位更新给其他人（不包括新加入的玩家）
        broadcastSeatUpdateExcept(member);
        broadcastSystem(member.getUsername() + " 加入了房间");
    }

    /**
     * 处理席位切换请求
     */
    public synchronized void handleSeatChange(ClientHandler member, String targetSeat) {
        // 检查游戏是否已开始
        if (battleStarted && !gameOver) {
            member.sendMessage(Protocol.buildMessage(Protocol.ERROR, "对战进行中，无法切换席位"));
            return;
        }

        String currentRole = member.getRole();

        switch (targetSeat) {
            case Protocol.SIT_BLACK:
                handleSitBlack(member, currentRole);
                break;
            case Protocol.SIT_WHITE:
                handleSitWhite(member, currentRole);
                break;
            case Protocol.SIT_SPECTATOR:
                handleSitSpectator(member, currentRole);
                break;
            default:
                member.sendMessage(Protocol.buildMessage(Protocol.ERROR, "未知的席位类型"));
        }
    }

    /**
     * 坐下黑棋席
     */
    private void handleSitBlack(ClientHandler member, String currentRole) {
        if (blackSeat != null) {
            member.sendMessage(Protocol.buildMessage(Protocol.ERROR, "黑棋席已被占用"));
            return;
        }

        // 从当前席位移除
        removeFromCurrentSeat(member, currentRole);

        // 坐下黑棋席
        blackSeat = member;
        member.setRole(Protocol.PLAYER_BLACK);

        // 通知该玩家角色变更
        member.sendMessage(Protocol.buildMessage(Protocol.ROLE_CHANGE, member.getUsername(), Protocol.PLAYER_BLACK));

        broadcastSeatUpdate();
        broadcastSystem(member.getUsername() + " 坐下了黑棋席");
    }

    /**
     * 坐下白棋席
     */
    private void handleSitWhite(ClientHandler member, String currentRole) {
        if (whiteSeat != null) {
            member.sendMessage(Protocol.buildMessage(Protocol.ERROR, "白棋席已被占用"));
            return;
        }

        // 从当前席位移除
        removeFromCurrentSeat(member, currentRole);

        // 坐下白棋席
        whiteSeat = member;
        member.setRole(Protocol.PLAYER_WHITE);

        // 通知该玩家角色变更
        member.sendMessage(Protocol.buildMessage(Protocol.ROLE_CHANGE, member.getUsername(), Protocol.PLAYER_WHITE));

        broadcastSeatUpdate();
        broadcastSystem(member.getUsername() + " 坐下了白棋席");
    }

    /**
     * 进入观战席
     */
    private void handleSitSpectator(ClientHandler member, String currentRole) {
        // 从当前席位移除
        removeFromCurrentSeat(member, currentRole);

        // 进入观战席
        spectators.put(member.getUsername(), member);
        member.setRole(Protocol.SPECTATOR);

        // 通知该玩家角色变更
        member.sendMessage(Protocol.buildMessage(Protocol.ROLE_CHANGE, member.getUsername(), Protocol.SPECTATOR));

        broadcastSeatUpdate();
        broadcastSystem(member.getUsername() + " 进入了观战席");
    }

    /**
     * 从当前席位移除
     */
    private void removeFromCurrentSeat(ClientHandler member, String currentRole) {
        if (currentRole.equals(Protocol.PLAYER_BLACK)) {
            blackSeat = null;
        } else if (currentRole.equals(Protocol.PLAYER_WHITE)) {
            whiteSeat = null;
        } else if (currentRole.equals(Protocol.SPECTATOR)) {
            spectators.remove(member.getUsername());
        }
    }

    /**
     * 处理对战邀请
     */
    public synchronized void handleBattleInvite(ClientHandler inviter) {
        // 检查是否是黑白席玩家
        if (inviter != blackSeat && inviter != whiteSeat) {
            inviter.sendMessage(Protocol.buildMessage(Protocol.ERROR, "只有黑白席玩家可以发起对战邀请"));
            return;
        }

        // 检查黑白席是否都有人
        if (blackSeat == null || whiteSeat == null) {
            inviter.sendMessage(Protocol.buildMessage(Protocol.ERROR, "黑白席必须都有人才能发起对战"));
            return;
        }

        // 检查是否已有邀请
        if (battleInviter != null) {
            inviter.sendMessage(Protocol.buildMessage(Protocol.ERROR, "已有对战邀请等待响应"));
            return;
        }

        // 记录邀请发起者
        battleInviter = inviter;

        // 通知对方
        ClientHandler opponent = (inviter == blackSeat) ? whiteSeat : blackSeat;
        opponent.sendMessage(Protocol.buildMessage(
                Protocol.BATTLE_INVITE_NOTIFY,
                inviter.getUsername()));

        broadcastSystem(inviter.getUsername() + " 发起了对战邀请");
    }

    /**
     * 处理对战邀请响应
     */
    public synchronized void handleBattleResponse(ClientHandler responder, String response) {
        // 检查是否有待响应的邀请
        if (battleInviter == null) {
            responder.sendMessage(Protocol.buildMessage(Protocol.ERROR, "当前没有对战邀请"));
            return;
        }

        // 检查响应者是否是对方
        ClientHandler expectedResponder = (battleInviter == blackSeat) ? whiteSeat : blackSeat;
        if (responder != expectedResponder) {
            responder.sendMessage(Protocol.buildMessage(Protocol.ERROR, "你不是被邀请的对象"));
            return;
        }

        if (response.equals(Protocol.AGREE)) {
            // 同意对战，开始游戏
            startBattle();
        } else {
            // 拒绝对战
            battleInviter.sendMessage(Protocol.buildMessage(
                    Protocol.SYSTEM,
                    responder.getUsername() + " 拒绝了你的对战邀请"));
            broadcastSystem(responder.getUsername() + " 拒绝了对战邀请");
            battleInviter = null;
        }
    }

    /**
     * 开始对战
     */
    private void startBattle() {
        battleStarted = true;
        gameOver = false;
        battleInviter = null;
        currentTurn = Protocol.BLACK;

        // 重置棋盘（清空上一局的棋子）
        chessRule.reset();

        // 先通知客户端清空棋盘
        broadcastToAll(Protocol.buildMessage(Protocol.BOARD_RESET));

        // 然后通知对战开始
        broadcastToAll(Protocol.buildMessage(Protocol.BATTLE_START));
        broadcastSystem("对战开始！黑棋先手：" + blackSeat.getUsername());
    }

    /**
     * 处理落子请求
     */
    public synchronized void handleMove(ClientHandler player, int x, int y) {
        // 检查对战是否已开始
        if (!battleStarted) {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "对战尚未开始"));
            return;
        }

        // 检查游戏是否已结束
        if (gameOver) {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "游戏已结束"));
            return;
        }

        // 检查是否是执棋者
        if (player != blackSeat && player != whiteSeat) {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "观战者无法落子"));
            return;
        }

        // 检查是否轮到该玩家
        String playerColor = getPlayerColor(player);
        if (!currentTurn.equals(playerColor)) {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "还未轮到你"));
            return;
        }

        // 检查位置是否合法
        if (!chessRule.isEmpty(x, y)) {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "该位置已有棋子"));
            return;
        }

        // 落子
        if (chessRule.placeStone(x, y, playerColor)) {
            // 广播落子成功
            broadcastMove(x, y, playerColor, player.getUsername());

            // 检查胜负
            if (chessRule.checkWin(x, y)) {
                handleGameEnd(playerColor, Protocol.WIN);
            } else if (chessRule.checkDraw()) {
                handleGameEnd(null, Protocol.DRAW);
            } else {
                // 切换回合
                currentTurn = currentTurn.equals(Protocol.BLACK) ? Protocol.WHITE : Protocol.BLACK;
            }
        } else {
            player.sendMessage(Protocol.buildMessage(Protocol.MOVE_FAIL, "落子失败"));
        }
    }

    /**
     * 处理聊天消息
     */
    public void handleChat(ClientHandler sender, String message) {
        String timestamp = dateFormat.format(new Date());
        String chatMsg = Protocol.buildMessage(
                Protocol.CHAT_MSG,
                sender.getUsername(),
                timestamp,
                message);
        broadcastToAll(chatMsg);
    }

    /**
     * 移除成员
     */
    public synchronized void removeMember(ClientHandler member) {
        String role = member.getRole();

        if (member == blackSeat) {
            blackSeat = null;
            if (battleStarted && !gameOver) {
                handleGameEnd(Protocol.WHITE, "黑棋玩家离开");
            }
            broadcastSystem(member.getUsername() + " 离开了黑棋席");
        } else if (member == whiteSeat) {
            whiteSeat = null;
            if (battleStarted && !gameOver) {
                handleGameEnd(Protocol.BLACK, "白棋玩家离开");
            }
            broadcastSystem(member.getUsername() + " 离开了白棋席");
        } else {
            spectators.remove(member.getUsername());
            broadcastSystem(member.getUsername() + " 离开了房间");
        }

        // 广播席位更新
        broadcastSeatUpdate();
    }

    /**
     * 处理游戏结束
     * 游戏结束后保留棋盘供复盘，只在新对战开始时才重置
     */
    private void handleGameEnd(String winnerColor, String reason) {
        gameOver = true;
        battleStarted = false;
        battleInviter = null;

        // 广播游戏结束
        broadcastGameOver(winnerColor, reason);

        // 不重置棋盘，保留供复盘
        // chessRule.reset() 将在 startBattle() 中调用

        broadcastSystem("游戏结束。棋盘保留供复盘，黑白席玩家可以发起新的对战邀请。");

        System.out.println("房间 " + roomId + " 游戏结束，棋盘保留供复盘");
    }

    /**
     * 广播席位状态更新
     */
    private void broadcastSeatUpdate() {
        String message = Protocol.buildMessage(
                Protocol.SEAT_UPDATE,
                getBlackSeatName(),
                getWhiteSeatName(),
                String.valueOf(spectators.size()));
        broadcastToAll(message);
    }

    /**
     * 广播席位状态更新（排除指定客户端）
     */
    private void broadcastSeatUpdateExcept(ClientHandler except) {
        String message = Protocol.buildMessage(
                Protocol.SEAT_UPDATE,
                getBlackSeatName(),
                getWhiteSeatName(),
                String.valueOf(spectators.size()));

        if (blackSeat != null && blackSeat != except) {
            blackSeat.sendMessage(message);
        }
        if (whiteSeat != null && whiteSeat != except) {
            whiteSeat.sendMessage(message);
        }
        for (ClientHandler spectator : spectators.values()) {
            if (spectator != except) {
                spectator.sendMessage(message);
            }
        }
    }

    /**
     * 同步棋盘状态给指定客户端
     */
    private void syncBoardState(ClientHandler client) {
        int[][] board = chessRule.getBoardState();
        for (int i = 0; i < Protocol.BOARD_SIZE; i++) {
            for (int j = 0; j < Protocol.BOARD_SIZE; j++) {
                if (board[i][j] != 0) {
                    String color = board[i][j] == 1 ? Protocol.BLACK : Protocol.WHITE;
                    client.sendMessage(Protocol.buildMessage(
                            Protocol.MOVE_SUCCESS,
                            String.valueOf(i),
                            String.valueOf(j),
                            color,
                            "系统"));
                }
            }
        }
    }

    /**
     * 广播落子消息
     */
    private void broadcastMove(int x, int y, String color, String username) {
        String message = Protocol.buildMessage(
                Protocol.MOVE_SUCCESS,
                String.valueOf(x),
                String.valueOf(y),
                color,
                username);
        broadcastToAll(message);
    }

    /**
     * 广播游戏结束
     */
    private void broadcastGameOver(String winnerColor, String reason) {
        String message = Protocol.buildMessage(
                Protocol.GAME_OVER,
                winnerColor != null ? winnerColor : "NONE",
                reason);
        broadcastToAll(message);
    }

    /**
     * 广播系统消息
     */
    private void broadcastSystem(String content) {
        String message = Protocol.buildMessage(Protocol.SYSTEM, content);
        broadcastToAll(message);
    }

    /**
     * 向房间所有成员广播消息
     */
    private void broadcastToAll(String message) {
        if (blackSeat != null) {
            blackSeat.sendMessage(message);
        }
        if (whiteSeat != null) {
            whiteSeat.sendMessage(message);
        }
        for (ClientHandler spectator : spectators.values()) {
            spectator.sendMessage(message);
        }
    }

    /**
     * 获取玩家颜色
     */
    private String getPlayerColor(ClientHandler player) {
        if (player == blackSeat) {
            return Protocol.BLACK;
        } else if (player == whiteSeat) {
            return Protocol.WHITE;
        }
        return null;
    }

    /**
     * 获取黑棋席玩家名称
     */
    private String getBlackSeatName() {
        return blackSeat != null ? blackSeat.getUsername() : "空";
    }

    /**
     * 获取白棋席玩家名称
     */
    private String getWhiteSeatName() {
        return whiteSeat != null ? whiteSeat.getUsername() : "空";
    }

    /**
     * 获取房间ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * 检查房间是否为空
     */
    public boolean isEmpty() {
        return blackSeat == null && whiteSeat == null && spectators.isEmpty();
    }

    /**
     * 获取房间成员数量
     */
    public int getMemberCount() {
        int count = 0;
        if (blackSeat != null)
            count++;
        if (whiteSeat != null)
            count++;
        count += spectators.size();
        return count;
    }

    /**
     * 获取房间状态文本
     */
    public String getStatusText() {
        if (battleStarted && !gameOver) {
            return "对战中: " + getBlackSeatName() + " vs " + getWhiteSeatName();
        } else if (gameOver) {
            return "游戏已结束";
        } else if (blackSeat != null && whiteSeat != null) {
            return "等待开始: " + getBlackSeatName() + " & " + getWhiteSeatName();
        } else {
            return "等待玩家中...";
        }
    }

    /**
     * 检查游戏是否已结束
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * 获取观战者数量
     */
    public int getSpectatorCount() {
        return spectators.size();
    }

    /**
     * 发送完整的房间状态给指定客户端
     * 包括席位状态、游戏状态、棋盘状态
     */
    public synchronized void sendRoomStateTo(ClientHandler client) {
        // 1. 发送席位状态
        client.sendMessage(Protocol.buildMessage(
                Protocol.SEAT_UPDATE,
                getBlackSeatName(),
                getWhiteSeatName(),
                String.valueOf(spectators.size())));

        // 2. 如果游戏已开始，发送游戏状态和棋盘状态
        if (battleStarted) {
            System.out.println("DEBUG: 同步游戏状态和棋盘给 " + client.getUsername());
            // 通知游戏已开始
            client.sendMessage(Protocol.buildMessage(Protocol.BATTLE_START));
            // 同步所有已下的棋子
            syncBoardState(client);
        }

        System.out.println("已发送完整房间状态给 " + client.getUsername());
    }
}