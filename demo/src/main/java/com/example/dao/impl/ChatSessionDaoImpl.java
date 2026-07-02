package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.ChatSessionDao;
import com.example.entity.ChatSession;
import com.example.util.DBUtil;

public class ChatSessionDaoImpl implements ChatSessionDao {
    private static final Logger logger = LoggerFactory.getLogger(ChatSessionDaoImpl.class);

    @Override
    public void saveOrUpdate(ChatSession session) {
        if (session == null) {
            throw new IllegalArgumentException("会话不能为空");
        }
        try {
            // 先查询是否存在
            ChatSession existing = queryByUserPair(session.getUserId(), session.getTargetUserId());
            if (existing != null) {
                // 存在：更新
                DBUtil.executeUpdate(
                        "update chat_session set last_message = ?, last_message_time = ?, unread_count = ?, update_time = ? " +
                                "where user_id = ? and target_user_id = ?",
                        session.getLastMessage(),
                        session.getLastMessageTime(),
                        session.getUnreadCount(),
                        session.getUpdateTime(),
                        session.getUserId(),
                        session.getTargetUserId()
                );
                logger.info("更新会话成功，用户：{}，对方：{}", session.getUserId(), session.getTargetUserId());
            } else {
                // 不存在：插入
                DBUtil.executeUpdate(
                        "insert into chat_session(user_id, target_user_id, last_message, last_message_time, unread_count, update_time) " +
                                "values (?, ?, ?, ?, ?, ?)",
                        session.getUserId(),
                        session.getTargetUserId(),
                        session.getLastMessage(),
                        session.getLastMessageTime(),
                        session.getUnreadCount(),
                        session.getUpdateTime()
                );
                logger.info("新增会话成功，用户：{}，对方：{}", session.getUserId(), session.getTargetUserId());
            }
        } catch (SQLException e) {
            logger.error("保存会话异常", e);
            throw new RuntimeException("保存会话异常", e);
        }
    }

    @Override
    public List<ChatSession> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select id, user_id, target_user_id, last_message, last_message_time, unread_count, update_time " +
                            "from chat_session where user_id = ? order by update_time desc",
                    rs -> {
                        Long id = rs.getLong("id");
                        String uid = rs.getString("user_id");
                        String targetUid = rs.getString("target_user_id");
                        String lastMsg = rs.getString("last_message");
                        LocalDateTime lastMsgTime = rs.getTimestamp("last_message_time") != null
                                ? rs.getTimestamp("last_message_time").toLocalDateTime() : null;
                        Integer unread = rs.getInt("unread_count");
                        LocalDateTime updateTime = rs.getTimestamp("update_time") != null
                                ? rs.getTimestamp("update_time").toLocalDateTime() : null;

                        ChatSession session = new ChatSession(uid, targetUid, updateTime);
                        session.setId(id);
                        session.setLastMessage(lastMsg);
                        session.setLastMessageTime(lastMsgTime);
                        session.setUnreadCount(unread);
                        return session;
                    }, userId);
        } catch (SQLException e) {
            logger.error("查询用户会话列表异常，用户：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public ChatSession queryByUserPair(String userId, String targetUserId) {
        if (userId == null || targetUserId == null) {
            return null;
        }
        try {
            List<ChatSession> list = DBUtil.executeQuery(
                    "select id, user_id, target_user_id, last_message, last_message_time, unread_count, update_time " +
                            "from chat_session where user_id = ? and target_user_id = ? limit 1",
                    rs -> {
                        Long id = rs.getLong("id");
                        String uid = rs.getString("user_id");
                        String targetUid = rs.getString("target_user_id");
                        String lastMsg = rs.getString("last_message");
                        LocalDateTime lastMsgTime = rs.getTimestamp("last_message_time") != null
                                ? rs.getTimestamp("last_message_time").toLocalDateTime() : null;
                        Integer unread = rs.getInt("unread_count");
                        LocalDateTime updateTime = rs.getTimestamp("update_time") != null
                                ? rs.getTimestamp("update_time").toLocalDateTime() : null;

                        ChatSession session = new ChatSession(uid, targetUid, updateTime);
                        session.setId(id);
                        session.setLastMessage(lastMsg);
                        session.setLastMessageTime(lastMsgTime);
                        session.setUnreadCount(unread);
                        return session;
                    }, userId, targetUserId);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("查询特定会话异常", e);
            return null;
        }
    }

    @Override
    public void delete(String userId, String targetUserId) {
        if (userId == null || targetUserId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            DBUtil.executeUpdate(
                    "delete from chat_session where user_id = ? and target_user_id = ?",
                    userId, targetUserId
            );
            logger.info("删除会话成功，用户：{}，对方：{}", userId, targetUserId);
        } catch (SQLException e) {
            logger.error("删除会话异常", e);
            throw new RuntimeException("删除会话异常", e);
        }
    }
}