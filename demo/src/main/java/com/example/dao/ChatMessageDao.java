package com.example.dao;

import com.example.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageDao {
    // 新增消息
    Long add(ChatMessage message);

    // 查询两个人的聊天记录（分页）
    List<ChatMessage> queryChatHistory(String userId1, String userId2, int offset, int limit);

    // 标记对方发来的消息为已读
    void markAsRead(String fromUserId, String toUserId);

    // 查询未读消息总数
    int countUnread(String userId);

    // 查询与某个人的未读消息数
    int countUnreadWithUser(String userId, String targetUserId);

    int recallMessage(Long messageId, String userId);

    int hideMessage(Long messageId, String userId);

    ChatMessage queryById(Long messageId);

    Integer countSendMsgByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    Integer countReceiveMsgByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
    /**
     * 查询指定时间段内 用户发送的所有消息
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    List<ChatMessage> querySendMessagesByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定时间段内 用户接收的所有消息
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息列表
     */
    List<ChatMessage> queryReceiveMessagesByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}