package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.entity.UserInformation;
import com.example.util.DBUtil;
import com.example.util.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

/**
 * 聚合接口 — 一次请求拉取多个模块数据，减少 HTTP 请求数
 *
 * GET /api/aggregate/init?modules=user,unread,stats,hotBooks
 */
@WebServlet("/api/aggregate/*")
public class AggregateServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(AggregateServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        if (!requireLogin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String modulesParam = req.getParameter("modules");
            if (modulesParam == null || modulesParam.isBlank()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("缺少 modules 参数")));
                return;
            }

            UserInformation user = (UserInformation) req.getSession().getAttribute("currentUser");
            String userId = user.getUserId();
            Map<String, Object> result = new LinkedHashMap<>();
            String[] modules = modulesParam.split(",");

            for (String m : modules) {
                try {
                    switch (m.trim()) {
                        case "user" -> result.put("user", fetchUserInfo(user));
                        case "unread" -> result.put("unread", fetchUnreadCount(userId));
                        case "pending" -> result.put("pending", fetchPendingCounts());
                        case "bookTypes" -> result.put("bookTypes", fetchBookTypes());
                        case "noteTypes" -> result.put("noteTypes", fetchNoteTypes());
                        case "tags" -> result.put("tags", fetchAllTags());
                        case "userTypes" -> result.put("userTypes", fetchUserTypes());
                        case "deptTypes" -> result.put("deptTypes", fetchDeptTypes());
                        case "friends" -> result.put("friends", fetchFriends(userId));
                        case "friendRequests" -> result.put("friendRequests", fetchFriendRequests(userId));
                        case "notifications" -> result.put("notifications", fetchNotifications(userId));
                        default -> result.put(m.trim(), Map.of("msg", "unknown module"));
                    }
                } catch (Exception e) {
                    logger.warn("聚合接口模块 {} 查询失败", m, e);
                    result.put(m.trim(), null);
                }
            }

            out.write(JSON.toJSONString(ResultDTO.success(result)));
        } catch (Exception e) {
            logger.error("聚合接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常")));
        }
    }

    // ====================== 子模块查询 ======================

    private Map<String, Object> fetchUserInfo(UserInformation user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", user.getUserId());
        info.put("name", user.getName());
        info.put("sex", user.getSex());
        info.put("type", user.getType());
        info.put("canUse", user.getCan_use());
        info.put("readTimeLong", user.getRead_time_long());
        info.put("bio", user.getBio());
        return info;
    }

    private Map<String, Object> fetchUnreadCount(String userId) {
        Map<String, Object> unread = new LinkedHashMap<>();
        String hashKey = "user:unread:" + userId;
        try {
            // 如果缓存不存在，从数据库查询并初始化 Redis
            if (!RedisUtil.exists(hashKey)) {
                int notifyCount = DBUtil.executeQueryScalar(
                        "SELECT COUNT(*) FROM user_notification WHERE user_id=? AND is_read=0",
                        Integer.class, userId);
                int requestCount = DBUtil.executeQueryScalar(
                        "SELECT COUNT(*) FROM friend_request WHERE to_user_id=? AND status=0",
                        Integer.class, userId);
                RedisUtil.hset(hashKey, "notifications", String.valueOf(notifyCount));
                RedisUtil.hset(hashKey, "friendRequests", String.valueOf(requestCount));
                RedisUtil.expire(hashKey, 86400); // 缓存 1 天自动失效

                unread.put("notifications", notifyCount);
                unread.put("friendRequests", requestCount);
                unread.put("total", notifyCount + requestCount);
                return unread;
            }

            // 从 Redis 获取未读计数
            String notifyStr = RedisUtil.hget(hashKey, "notifications");
            String requestStr = RedisUtil.hget(hashKey, "friendRequests");
            int notifyCount = notifyStr != null ? Integer.parseInt(notifyStr) : 0;
            int requestCount = requestStr != null ? Integer.parseInt(requestStr) : 0;

            unread.put("notifications", notifyCount);
            unread.put("friendRequests", requestCount);
            unread.put("total", notifyCount + requestCount);
        } catch (Exception e) {
            logger.warn("从 Redis 获取未读数失败，降级查询数据库", e);
            try {
                int notifyCount = DBUtil.executeQueryScalar(
                        "SELECT COUNT(*) FROM user_notification WHERE user_id=? AND is_read=0",
                        Integer.class, userId);
                int requestCount = DBUtil.executeQueryScalar(
                        "SELECT COUNT(*) FROM friend_request WHERE to_user_id=? AND status=0",
                        Integer.class, userId);
                unread.put("notifications", notifyCount);
                unread.put("friendRequests", requestCount);
                unread.put("total", notifyCount + requestCount);
            } catch (SQLException sqle) {
                logger.error("数据库降级查询未读数也失败了", sqle);
            }
        }
        return unread;
    }

    private Map<String, Object> fetchPendingCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        try {
            counts.put("bookComments", DBUtil.executeQueryScalar(
                    "SELECT COUNT(*) FROM book_comment WHERE status=0", Integer.class));
            counts.put("paraComments", DBUtil.executeQueryScalar(
                    "SELECT COUNT(*) FROM paragraph_comment WHERE status=0", Integer.class));
            counts.put("userComments", DBUtil.executeQueryScalar(
                    "SELECT COUNT(*) FROM user_comment WHERE status=0", Integer.class));
            counts.put("bottles", DBUtil.executeQueryScalar(
                    "SELECT COUNT(*) FROM book_bottle WHERE audit_status=0", Integer.class));
        } catch (SQLException e) {
            logger.warn("查询待审核数失败", e);
        }
        return counts;
    }

    // ====================== 🔥 缓存辅助方法 ======================
    /**
     * Cache-Aside 包装：先查 Redis（key→JSON→List），miss 才走 DB。
     * Redis 挂了自动降级，不为 null 的缓存直接返回。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCachedOrQuery(String cacheKey, Supplier<List<Map<String, Object>>> dbQuery) {
        try {
            String cached = RedisUtil.get(cacheKey);
            if (cached != null) {
                logger.info("缓存命中成功{}", cacheKey);
                return (List<Map<String, Object>>) (List<?>) JSON.parseArray(cached);
            }
        } catch (Exception e) {logger.warn("缓存查询失败"); }
        List<Map<String, Object>> data = dbQuery.get();
        try { RedisUtil.set(cacheKey, JSON.toJSONString(data), 3600); } catch (Exception e) {}
        return data;
    }
    // ====================== 参考数据模块（5个固定数据 + Redis 缓存） ======================

    private List<Map<String, Object>> fetchBookTypes() {
        return getCachedOrQuery("agg:bookTypes", () -> {
            try {
                return DBUtil.executeQuery("SELECT id, bookType FROM book_type ORDER BY id",
                        rs -> Map.of("id", rs.getInt("id"), "typeName", rs.getString("bookType")));
            } catch (SQLException e) { return List.of(); }
        });
    }

    private List<Map<String, Object>> fetchNoteTypes() {
        return getCachedOrQuery("agg:noteTypes", () -> {
            try {
                return DBUtil.executeQuery("SELECT id, userTextColType FROM user_text_col_type ORDER BY id",
                        rs -> Map.of("id", rs.getInt("id"), "typeName", rs.getString("userTextColType")));
            } catch (SQLException e) { return List.of(); }
        });
    }

    private List<Map<String, Object>> fetchAllTags() {
        return getCachedOrQuery("agg:tags", () -> {
            try {
                return DBUtil.executeQuery("SELECT id, tag_name FROM book_tag ORDER BY id",
                        rs -> Map.of("id", rs.getInt("id"), "name", rs.getString("tag_name")));
            } catch (SQLException e) { return List.of(); }
        });
    }

    private List<Map<String, Object>> fetchUserTypes() {
        return getCachedOrQuery("agg:userTypes", () -> {
            try {
                return DBUtil.executeQuery("SELECT id, user_type FROM user_type ORDER BY id",
                        rs -> Map.of("id", rs.getInt("id"), "typeName", rs.getString("user_type")));
            } catch (SQLException e) { return List.of(); }
        });
    }

    private List<Map<String, Object>> fetchDeptTypes() {
        return getCachedOrQuery("agg:deptTypes", () -> {
            try {
                return DBUtil.executeQuery("SELECT id, DeptType FROM user_dept_type ORDER BY id",
                        rs -> Map.of("id", rs.getInt("id"), "typeName", rs.getString("DeptType")));
            } catch (SQLException e) { return List.of(); }
        });
    }

    private List<Map<String, Object>> fetchFriends(String userId) {
        try {
            return DBUtil.executeQuery(
                    "SELECT f.friend_id, u.Name FROM friend f LEFT JOIN user_information u ON f.friend_id=u.UserId WHERE f.user_id=?",
                    rs -> Map.of("friendId", rs.getString("friend_id"), "userName", rs.getString("Name")), userId);
        } catch (SQLException e) { return List.of(); }
    }

    private List<Map<String, Object>> fetchFriendRequests(String userId) {
        try {
            return DBUtil.executeQuery(
                    "SELECT fr.id, fr.from_user_id, fr.to_user_id, fr.request_msg, fr.create_time, u.Name as fromUserName " +
                            "FROM friend_request fr LEFT JOIN user_information u ON fr.from_user_id=u.UserId " +
                            "WHERE fr.to_user_id=? AND fr.status=0 ORDER BY fr.create_time DESC",
                    rs -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", rs.getInt("id"));
                        m.put("fromUserId", rs.getString("from_user_id"));
                        m.put("toUserId", rs.getString("to_user_id"));
                        m.put("requestMsg", rs.getString("request_msg"));
                        m.put("createTime", rs.getTimestamp("create_time"));
                        m.put("fromUserName", rs.getString("fromUserName"));
                        return m;
                    }, userId);
        } catch (SQLException e) { return List.of(); }
    }

    private List<Map<String, Object>> fetchNotifications(String userId) {
        try {
            return DBUtil.executeQuery(
                    "SELECT n.id, n.type, n.from_user_id, n.comment_id, n.is_read, n.create_time, " +
                    "u.Name as fromUserName FROM user_notification n " +
                    "LEFT JOIN user_information u ON n.from_user_id=u.UserId " +
                    "WHERE n.user_id=? ORDER BY n.create_time DESC LIMIT 50",
                    rs -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", rs.getInt("id"));
                        m.put("type", rs.getInt("type"));
                        m.put("fromUserId", rs.getString("from_user_id"));
                        m.put("commentId", rs.getInt("comment_id"));
                        m.put("isRead", rs.getInt("is_read"));
                        m.put("createTime", rs.getTimestamp("create_time"));
                        m.put("fromUserName", rs.getString("fromUserName"));
                        return m;
                    }, userId);
        } catch (SQLException e) { return List.of(); }
    }
}
