package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService {
    // 发送消息（同时更新会话）
    ResultDTO<Long> sendMessage(ChatMessage message);

    // 查询聊天记录（分页）
    ResultDTO<List<ChatMessage>> getChatHistory(String userId1, String userId2, int page, int pageSize);

    // 标记消息已读
    ResultDTO<Void> markAsRead(String fromUserId, String toUserId);

    // 查询未读消息总数
    ResultDTO<Integer> getUnreadCount(String userId);

    ResultDTO<Void> hideMessage(Long messageId, String userId);

    ResultDTO<ChatMessage> recallMessage(Long messageId, String userId);
}