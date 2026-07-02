package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.ChatMessageDao;
import com.example.entity.ChatMessage;
import com.example.util.DBUtil;

public class ChatMessageDaoImpl implements ChatMessageDao {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageDaoImpl.class);

    @Override
    public Long add(ChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("消息不能为空");
        }
        try {
            Integer id = DBUtil.executeUpdateReturnId(
                    "insert into chat_message(from_user_id, to_user_id, message_type, message_content, is_read, create_time, reply_to_id, hide_from_sender, hide_from_receiver, is_recalled, original_content) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    message.getFromUserId(),
                    message.getToUserId(),
                    message.getMessageType(),
                    message.getMessageContent(),
                    message.getIsRead(),
                    message.getCreateTime(),
                    message.getReplyToId(),
                    // 关键：加null判断，默认false
                    message.getHideFromSender() != null && message.getHideFromSender() ? 1 : 0,
                    message.getHideFromReceiver() != null && message.getHideFromReceiver() ? 1 : 0,
                    message.getIsRecalled() != null && message.getIsRecalled() ? 1 : 0,
                    message.getOriginalContent()
            );
            return id != null ? id.longValue() : null;
        } catch (SQLException e) {
            logger.error("新增聊天消息失败，发送者：{}，接收者：{}", message.getFromUserId(), message.getToUserId(), e);
            throw new RuntimeException("新增聊天消息异常", e);
        }
    }

    // 2. 修改queryChatHistory方法，过滤隐藏消息（双字段版本）
    @Override
    public List<ChatMessage> queryChatHistory(String userId1, String userId2, int offset, int limit) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select id, from_user_id, to_user_id, message_type, message_content, is_read, create_time, reply_to_id, hide_from_sender, hide_from_receiver, is_recalled, original_content, recall_time " +
                            "from chat_message where ((from_user_id = ? and to_user_id = ?) or (from_user_id = ? and to_user_id = ?)) " +
                            "and ( " +
                            "  (from_user_id = ? and hide_from_sender = 0) " +
                            "  or " +
                            "  (to_user_id = ? and hide_from_receiver = 0) " +
                            ") " +
                            "order by create_time desc limit ? offset ?",
                    rs -> {
                        Long id = rs.getLong("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        Integer type = rs.getInt("message_type");
                        String content = rs.getString("message_content");
                        Integer isRead = rs.getInt("is_read");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        Long replyToId = rs.getLong("reply_to_id");
                        Boolean hideFromSender = rs.getBoolean("hide_from_sender");
                        Boolean hideFromReceiver = rs.getBoolean("hide_from_receiver");
                        Boolean isRecalled = rs.getBoolean("is_recalled");
                        String originalContent = rs.getString("original_content");
                        LocalDateTime recallTime = rs.getTimestamp("recall_time") != null
                                ? rs.getTimestamp("recall_time").toLocalDateTime() : null;

                        ChatMessage message = new ChatMessage(fromUid, toUid, type, content, createTime);
                        message.setId(id);
                        message.setIsRead(isRead);
                        message.setReplyToId(replyToId == 0 ? null : replyToId);
                        message.setHideFromSender(hideFromSender);
                        message.setHideFromReceiver(hideFromReceiver);
                        message.setIsRecalled(isRecalled);
                        message.setOriginalContent(originalContent);
                        message.setRecallTime(recallTime);
                        return message;
                    }, userId1, userId2, userId2, userId1, userId1, userId1, limit, offset);
        } catch (SQLException e) {
            logger.error("查询聊天记录异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    // 3. 撤回消息（不变）
    public int recallMessage(Long messageId, String userId) {
        try {
            int affected = DBUtil.executeUpdate(
                    "update chat_message set is_recalled = 1, original_content = message_content, message_content = '对方撤回了一条消息', recall_time = now() " +
                            "where id = ? and from_user_id = ? and timestampdiff(minute, create_time, now()) <= 2",
                    messageId, userId
            );
            logger.info("撤回消息结果，消息ID：{}，用户ID：{}，影响行数：{}", messageId, userId, affected);
            return affected;
        } catch (SQLException e) {
            logger.error("撤回消息失败，消息ID：{}，用户ID：{}", messageId, userId, e);
            throw new RuntimeException("撤回消息异常", e);
        }
    }

    @Override
    // 4. 隐藏消息（双字段版本，彻底解决JSON问题）
    public int hideMessage(Long messageId, String userId) {
        try {
            // 先查询消息，判断用户是发送者还是接收者
            ChatMessage message = queryById(messageId);
            if (message == null) {
                logger.warn("隐藏消息失败，消息不存在：{}", messageId);
                return 0;
            }

            int affected;
            if (userId.equals(message.getFromUserId())) {
                // 用户是发送者 → 更新hide_from_sender
                affected = DBUtil.executeUpdate(
                        "update chat_message set hide_from_sender = 1 where id = ? and hide_from_sender = 0",
                        messageId
                );
                logger.info("对发送者隐藏消息，消息ID：{}，用户ID：{}，影响行数：{}", messageId, userId, affected);
            } else if (userId.equals(message.getToUserId())) {
                // 用户是接收者 → 更新hide_from_receiver
                affected = DBUtil.executeUpdate(
                        "update chat_message set hide_from_receiver = 1 where id = ? and hide_from_receiver = 0",
                        messageId
                );
                logger.info("对接收者隐藏消息，消息ID：{}，用户ID：{}，影响行数：{}", messageId, userId, affected);
            } else {
                logger.warn("隐藏消息失败，用户{}不是消息{}的参与者", userId, messageId);
                return 0;
            }

            return affected;
        } catch (SQLException e) {
            logger.error("隐藏消息失败，消息ID：{}，用户ID：{}", messageId, userId, e);
            throw new RuntimeException("隐藏消息异常", e);
        }
    }

    @Override
    // 5. 根据ID查询单条消息（更新字段）
    public ChatMessage queryById(Long messageId) {
        try {
            List<ChatMessage> list = DBUtil.executeQuery(
                    "select id, from_user_id, to_user_id, message_type, message_content, is_read, create_time, reply_to_id, hide_from_sender, hide_from_receiver, is_recalled, original_content, recall_time " +
                            "from chat_message where id = ? limit 1",
                    rs -> {
                        Long id = rs.getLong("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        Integer type = rs.getInt("message_type");
                        String content = rs.getString("message_content");
                        Integer isRead = rs.getInt("is_read");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        Long replyToId = rs.getLong("reply_to_id");
                        Boolean hideFromSender = rs.getBoolean("hide_from_sender");
                        Boolean hideFromReceiver = rs.getBoolean("hide_from_receiver");
                        Boolean isRecalled = rs.getBoolean("is_recalled");
                        String originalContent = rs.getString("original_content");
                        LocalDateTime recallTime = rs.getTimestamp("recall_time") != null
                                ? rs.getTimestamp("recall_time").toLocalDateTime() : null;

                        ChatMessage message = new ChatMessage(fromUid, toUid, type, content, createTime);
                        message.setId(id);
                        message.setIsRead(isRead);
                        message.setReplyToId(replyToId == 0 ? null : replyToId);
                        message.setHideFromSender(hideFromSender);
                        message.setHideFromReceiver(hideFromReceiver);
                        message.setIsRecalled(isRecalled);
                        message.setOriginalContent(originalContent);
                        message.setRecallTime(recallTime);
                        return message;
                    }, messageId);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("查询消息失败", e);
            return null;
        }
    }

    @Override
    public void markAsRead(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            DBUtil.executeUpdate(
                    "update chat_message set is_read = 1 where from_user_id = ? and to_user_id = ? and is_read = 0",
                    fromUserId, toUserId
            );
            logger.info("标记消息已读成功，发送者：{}，接收者：{}", fromUserId, toUserId);
        } catch (SQLException e) {
            logger.error("标记消息已读失败", e);
            throw new RuntimeException("标记消息已读异常", e);
        }
    }

    @Override
    public int countUnread(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0;
        }
        try {
            List<Integer> counts = DBUtil.executeQuery(
                    "select count(*) as cnt from chat_message where to_user_id = ? and is_read = 0",
                    rs -> rs.getInt("cnt"),
                    userId);
            return counts.isEmpty() ? 0 : counts.get(0);
        } catch (SQLException e) {
            logger.error("查询未读消息总数异常，用户：{}", userId, e);
            return 0;
        }
    }

    @Override
    public int countUnreadWithUser(String userId, String targetUserId) {
        if (userId == null || targetUserId == null) {
            return 0;
        }
        try {
            List<Integer> counts = DBUtil.executeQuery(
                    "select count(*) as cnt from chat_message where from_user_id = ? and to_user_id = ? and is_read = 0",
                    rs -> rs.getInt("cnt"),
                    targetUserId, userId);
            return counts.isEmpty() ? 0 : counts.get(0);
        } catch (SQLException e) {
            logger.error("查询与某人的未读消息数异常", e);
            return 0;
        }
    }

    // ====================== 统计发送消息数 ======================
    public Integer countSendMsgByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM chat_message WHERE from_user_id=? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户发送消息异常", e);
            return 0;
        }
    }

    // ====================== 统计接收消息数 ======================
    public Integer countReceiveMsgByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM chat_message WHERE to_user_id=? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户接收消息异常", e);
            return 0;
        }
    }

    /**
     * 查询指定时间段内 用户发送的所有消息
     */
    @Override
    public List<ChatMessage> querySendMessagesByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            // 过滤：自己发送的 + 未对自己隐藏 + 时间范围 + 按时间倒序
            return DBUtil.executeQuery(
                    "select id, from_user_id, to_user_id, message_type, message_content, is_read, create_time, " +
                            "reply_to_id, hide_from_sender, hide_from_receiver, is_recalled, original_content, recall_time " +
                            "from chat_message " +
                            "where from_user_id = ? " +
                            "and create_time BETWEEN ? AND ? " +
                            "and hide_from_sender = 0 " +
                            "order by create_time desc",
                    // 完全复用你原有的结果集解析代码
                    rs -> {
                        Long id = rs.getLong("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        Integer type = rs.getInt("message_type");
                        String content = rs.getString("message_content");
                        Integer isRead = rs.getInt("is_read");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        Long replyToId = rs.getLong("reply_to_id");
                        Boolean hideFromSender = rs.getBoolean("hide_from_sender");
                        Boolean hideFromReceiver = rs.getBoolean("hide_from_receiver");
                        Boolean isRecalled = rs.getBoolean("is_recalled");
                        String originalContent = rs.getString("original_content");
                        LocalDateTime recallTime = rs.getTimestamp("recall_time") != null
                                ? rs.getTimestamp("recall_time").toLocalDateTime() : null;

                        ChatMessage message = new ChatMessage(fromUid, toUid, type, content, createTime);
                        message.setId(id);
                        message.setIsRead(isRead);
                        message.setReplyToId(replyToId == 0 ? null : replyToId);
                        message.setHideFromSender(hideFromSender);
                        message.setHideFromReceiver(hideFromReceiver);
                        message.setIsRecalled(isRecalled);
                        message.setOriginalContent(originalContent);
                        message.setRecallTime(recallTime);
                        return message;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段发送消息异常", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询指定时间段内 用户接收的所有消息
     */
    @Override
    public List<ChatMessage> queryReceiveMessagesByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            // 过滤：自己接收的 + 未对自己隐藏 + 时间范围 + 按时间倒序
            return DBUtil.executeQuery(
                    "select id, from_user_id, to_user_id, message_type, message_content, is_read, create_time, " +
                            "reply_to_id, hide_from_sender, hide_from_receiver, is_recalled, original_content, recall_time " +
                            "from chat_message " +
                            "where to_user_id = ? " +
                            "and create_time BETWEEN ? AND ? " +
                            "and hide_from_receiver = 0 " +
                            "order by create_time desc",
                    // 完全复用你原有的结果集解析代码
                    rs -> {
                        Long id = rs.getLong("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        Integer type = rs.getInt("message_type");
                        String content = rs.getString("message_content");
                        Integer isRead = rs.getInt("is_read");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        Long replyToId = rs.getLong("reply_to_id");
                        Boolean hideFromSender = rs.getBoolean("hide_from_sender");
                        Boolean hideFromReceiver = rs.getBoolean("hide_from_receiver");
                        Boolean isRecalled = rs.getBoolean("is_recalled");
                        String originalContent = rs.getString("original_content");
                        LocalDateTime recallTime = rs.getTimestamp("recall_time") != null
                                ? rs.getTimestamp("recall_time").toLocalDateTime() : null;

                        ChatMessage message = new ChatMessage(fromUid, toUid, type, content, createTime);
                        message.setId(id);
                        message.setIsRead(isRead);
                        message.setReplyToId(replyToId == 0 ? null : replyToId);
                        message.setHideFromSender(hideFromSender);
                        message.setHideFromReceiver(hideFromReceiver);
                        message.setIsRecalled(isRecalled);
                        message.setOriginalContent(originalContent);
                        message.setRecallTime(recallTime);
                        return message;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段接收消息异常", userId, e);
            return Collections.emptyList();
        }
    }
}