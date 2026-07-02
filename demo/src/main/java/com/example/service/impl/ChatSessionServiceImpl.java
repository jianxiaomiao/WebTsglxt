package com.example.service.impl;

import java.util.List;

import com.example.dao.ChatSessionDao;
import com.example.dto.ResultDTO;
import com.example.entity.ChatSession;
import com.example.service.ChatSessionService;

public class ChatSessionServiceImpl implements ChatSessionService {
    private final ChatSessionDao chatSessionDao;

    public ChatSessionServiceImpl(ChatSessionDao chatSessionDao) {
        this.chatSessionDao = chatSessionDao;
    }

    @Override
    public ResultDTO<List<ChatSession>> getSessionList(String userId) {
        try {
            List<ChatSession> sessions = chatSessionDao.queryByUserId(userId);
            return ResultDTO.success(sessions);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询会话列表失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteSession(String userId, String targetUserId) {
        try {
            chatSessionDao.delete(userId, targetUserId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除会话失败：" + e.getMessage());
        }
    }
}