package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.ChatSession;
import java.util.List;

public interface ChatSessionService {
    // 查询用户的所有会话
    ResultDTO<List<ChatSession>> getSessionList(String userId);

    // 删除会话
    ResultDTO<Void> deleteSession(String userId, String targetUserId);
}