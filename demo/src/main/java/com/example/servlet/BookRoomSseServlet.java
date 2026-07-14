package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookReadRoomDaoImpl;
import com.example.dto.ResultDTO;
import com.example.service.impl.BookReadRoomServiceImpl;
import com.example.util.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

@WebServlet("/api/book/room/sse")
public class BookRoomSseServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookRoomSseServlet.class);

    // 1. 在线会话池：Map<房间号, Map<用户ID, SSE输出流>>
    private static final Map<Integer, Map<String, PrintWriter>> ROOM_SESSIONS = new ConcurrentHashMap<>();

    // 🌟 优化二：房间历史消息缓存池大小（每个房间保留最近 30 条）
    private static final int MAX_HISTORY_SIZE = 30;

    // 🌟 优化一：空房间延时销毁调度器（防抖线程池）
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private static final Map<Integer, ScheduledFuture<?>> PENDING_DELETES = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        String roomIdStr = req.getParameter("roomId");
        String userId = req.getParameter("userId");

        if (roomIdStr == null || userId == null || roomIdStr.isBlank() || userId.isBlank()) return;

        int roomId = Integer.parseInt(roomIdStr);
        PrintWriter out = resp.getWriter();

        // 1. 注册进房间池
        ROOM_SESSIONS.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, out);

        logger.info("用户 [{}] 加入房间 #{}, 当前人数: {}", userId, roomId, ROOM_SESSIONS.get(roomId).size());

        // 🔥 极其优雅的防抖拦截：如果该房间正处于 10 秒死亡倒计时中，立刻取消行刑！
        ScheduledFuture<?> deathTask = PENDING_DELETES.remove(roomId);
        if (deathTask != null) {
            deathTask.cancel(false);
            logger.info("🛡️ 房间 #{} 在销毁倒计时内迎来了书友重连，已取消销毁！", roomId);
        }

        // 2. 广播最新名单
        broadcastRoomMembers(roomId);

        // 🌟 优化二落地：向该新连入的用户，推送最近的历史消息！
        sendHistoryMessagesToUser(roomId, out);

        try {
            while (true) {
                out.write(":ping\n\n");
                out.flush();
                if (out.checkError()) {
                    Map<String, PrintWriter> roomMap = ROOM_SESSIONS.get(roomId);
                    if (roomMap != null) {
                        roomMap.remove(userId);
                        logger.info("用户 [{}] 断开了房间 #{} 的连接", userId, roomId);

                        broadcastRoomMembers(roomId);

                        // 🌟 优化一落地：房间人数归零，启动 10 秒死亡倒计时！
                        if (roomMap.isEmpty()) {
                            ROOM_SESSIONS.remove(roomId);

                            ScheduledFuture<?> future = SCHEDULER.schedule(() -> {
                                // 10秒后复查，如果依然没人连进来，正式物理毁灭！
                                if (!ROOM_SESSIONS.containsKey(roomId)) {
                                    try {
                                        // 像 BookReadRoomServlet 里那样初始化 Service
                                        BookReadRoomDaoImpl roomDao = new BookReadRoomDaoImpl();
                                        BookReadRoomServiceImpl roomService = new BookReadRoomServiceImpl(roomDao);
                                        roomService.deleteRoom(roomId);
                                        logger.info("💥 房间 #{} 连续10秒无人连接，已自动从数据库物理删除！", roomId);
                                        // 清理 Redis 历史记录
                                        RedisUtil.del("chat:room:history:" + roomId);
                                    } catch (Exception e) {
                                        logger.error("自动删房与清理缓存失败", e);
                                    }
                                    PENDING_DELETES.remove(roomId);
                                }
                            }, 10, TimeUnit.SECONDS);

                            PENDING_DELETES.put(roomId, future);
                            logger.info("⏳ 房间 #{} 人数归零，已进入 10 秒销毁倒计时...", roomId);
                        }
                    }
                    break;
                }
                Thread.sleep(4000); // 睡眠从 20 秒缩短为 4 秒微心跳
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 广播普通聊天/书摘，并记入历史缓存池
    public static void broadcastToRoom(int roomId, String jsonMessage) {
        // 过滤：只有聊天和书摘卡片记入历史，系统名单包(ROOM_META)不记入
        if (jsonMessage.contains("\"CHAT\"") || jsonMessage.contains("\"NOTE_CARD\"")) {
            String historyKey = "chat:room:history:" + roomId;
            try {
                RedisUtil.rpush(historyKey, jsonMessage);
                RedisUtil.ltrim(historyKey, -MAX_HISTORY_SIZE, -1);
                // 设置 1 天过期，防止僵尸房间占用内存
                RedisUtil.expire(historyKey, 86400);
            } catch (Exception e) {
                logger.warn("保存聊天消息到 Redis 历史缓存失败", e);
            }
        }

        Map<String, PrintWriter> roomUsers = ROOM_SESSIONS.get(roomId);
        if (roomUsers == null || roomUsers.isEmpty()) return;

        roomUsers.forEach((uid, writer) -> {
            writer.write("data:" + jsonMessage + "\n\n");
            writer.flush();
        });
    }

    // 🔥 向新连接用户单独倾倒历史消息，并打上 isHistory 补丁
    private void sendHistoryMessagesToUser(int roomId, PrintWriter out) {
        String historyKey = "chat:room:history:" + roomId;
        List<String> history = null;
        try {
            history = RedisUtil.lrange(historyKey, 0, -1);
        } catch (Exception e) {
            logger.warn("从 Redis 获取聊天历史失败", e);
        }

        if (history == null || history.isEmpty()) return;

        for (String msgStr : history) {
            try {
                // 安全解析原消息，强行塞入 "isHistory": true
                Map<String, Object> map = JSON.parseObject(msgStr, Map.class);
                map.put("isHistory", true);
                out.write("data:" + JSON.toJSONString(map) + "\n\n");
            } catch (Exception e) {}
        }
        out.flush();
    }

    // 广播人员名单
    private static void broadcastRoomMembers(int roomId) {
        Map<String, PrintWriter> roomUsers = ROOM_SESSIONS.get(roomId);
        if (roomUsers == null) return;

        Set<String> activeUsers = roomUsers.keySet();
        String metaPayload = JSON.toJSONString(Map.of(
                "type", "ROOM_META",
                "onlineCount", activeUsers.size(),
                "onlineUsers", activeUsers,
                "timestamp", System.currentTimeMillis()
        ));
        broadcastToRoom(roomId, metaPayload);
    }
}