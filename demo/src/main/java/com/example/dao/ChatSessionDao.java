package com.example.dao;

import com.example.entity.ChatSession;
import java.util.List;

public interface ChatSessionDao {
    // 新增或更新会话（如果存在就更新，不存在就插入）
    void saveOrUpdate(ChatSession session);

    // 查询用户的所有会话
    List<ChatSession> queryByUserId(String userId);

    // 查询特定会话
    ChatSession queryByUserPair(String userId, String targetUserId);

    // 删除会话
    void delete(String userId, String targetUserId);
}