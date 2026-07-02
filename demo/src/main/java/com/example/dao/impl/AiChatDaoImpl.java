package com.example.dao.impl;

import com.example.dao.AiChatDao;
import com.example.entity.UserAiChat;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class AiChatDaoImpl implements AiChatDao {
    private static final Logger logger = LoggerFactory.getLogger(AiChatDaoImpl.class);

    // 🔥 1. 插入：添加 form_type 字段
    @Override
    public Integer add(UserAiChat aiChat) {
        if (aiChat == null) {
            throw new IllegalArgumentException("AI聊天记录不能为空");
        }
        try {
            return DBUtil.executeUpdateReturnId(
                    "insert into user_ai_chat(user_id, message_content, message_type, create_time, form_type) values (?, ?, ?, ?, ?)",
                    aiChat.getUserId(),
                    aiChat.getMessageContent(),
                    aiChat.getMessageType(),
                    aiChat.getCreateTime(),
                    aiChat.getFormType() // 新增
            );
        } catch (SQLException e) {
            logger.error("保存AI聊天记录失败，用户ID：{}", aiChat.getUserId(), e);
            throw new RuntimeException("保存聊天记录异常", e);
        }
    }

    // 🔥 2. 所有查询都加上 form_type 字段，并映射
    @Override
    public List<UserAiChat> selectRecentMemory(String userId, int limit) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT id, user_id, message_content, message_type, create_time, form_type FROM user_ai_chat WHERE user_id = ? ORDER BY id DESC LIMIT ?",
                    rs -> {
                        UserAiChat chat = new UserAiChat();
                        chat.setId(rs.getLong("id"));
                        chat.setUserId(rs.getString("user_id"));
                        chat.setMessageContent(rs.getString("message_content"));
                        chat.setMessageType(rs.getInt("message_type"));
                        chat.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        chat.setFormType(rs.getInt("form_type")); // 新增
                        return chat;
                    }, userId, limit);
        } catch (SQLException e) {
            logger.error("查询AI记忆失败，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserAiChat> selectAllHistory(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT id, user_id, message_content, message_type, create_time, form_type FROM user_ai_chat WHERE user_id = ? ORDER BY id DESC",
                    rs -> {
                        UserAiChat chat = new UserAiChat();
                        chat.setId(rs.getLong("id"));
                        chat.setUserId(rs.getString("user_id"));
                        chat.setMessageContent(rs.getString("message_content"));
                        chat.setMessageType(rs.getInt("message_type"));
                        chat.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        chat.setFormType(rs.getInt("form_type")); // 新增
                        return chat;
                    }, userId);
        } catch (SQLException e) {
            logger.error("查询AI聊天历史失败，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserAiChat> selectHistoryByPage(String userId, int offset, int limit) {
        logger.info("分页查询参数：userId={}, offset={}, limit={}", userId, offset, limit);
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT id, user_id, message_content, message_type, create_time, form_type " +
                            "FROM user_ai_chat " +
                            "WHERE user_id = ? " +
                            "ORDER BY id DESC " +
                            "LIMIT ? OFFSET ?",
                    rs -> {
                        UserAiChat chat = new UserAiChat();
                        chat.setId(rs.getLong("id"));
                        chat.setUserId(rs.getString("user_id"));
                        chat.setMessageContent(rs.getString("message_content"));
                        chat.setMessageType(rs.getInt("message_type"));
                        chat.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        chat.setFormType(rs.getInt("form_type")); // 新增
                        return chat;
                    }, userId, limit, offset);
        } catch (SQLException e) {
            logger.error("分页查询AI聊天历史失败，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public String selectLastUserQuestion(String userId) {
        String sql = "SELECT message_content FROM user_ai_chat WHERE user_id = ? AND form_type = 0 ORDER BY create_time DESC LIMIT 1";
        try {
            List<String> temp = DBUtil.executeQuery(sql, rs -> rs.next() ? rs.getString("message_content") : null, userId);
            return temp.isEmpty() ? null : temp.getFirst();
        } catch (Exception e) {
            logger.error("查询用户最后问题失败，用户ID：{}", userId, e);
            return null;
        }
    }

    @Override
    public Integer countAiChatByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_ai_chat WHERE user_id = ? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"), userId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户AI对话消息异常", e);
            return 0;
        }
    }

    @Override
    public List<UserAiChat> selectHistoryByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT id, user_id, message_content, message_type, create_time, form_type " +
                            "FROM user_ai_chat " +
                            "WHERE user_id = ? AND create_time BETWEEN ? AND ? ORDER BY id DESC",
                    rs -> {
                        UserAiChat chat = new UserAiChat();
                        chat.setId(rs.getLong("id"));
                        chat.setUserId(rs.getString("user_id"));
                        chat.setMessageContent(rs.getString("message_content"));
                        chat.setMessageType(rs.getInt("message_type"));
                        chat.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        chat.setFormType(rs.getInt("form_type")); // 新增
                        return chat;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段AI聊天记录异常", userId, e);
            return Collections.emptyList();
        }
    }
}