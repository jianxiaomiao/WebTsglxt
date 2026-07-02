package com.example.service;

import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;
import com.example.dto.UserStatsForAIDTO;
import com.example.entity.UserAiChat;
import java.util.List;

/**
 * AI 聊天 Service 接口
 */
public interface AiChatService {
    /**
     * 带记忆聊天
     */
    ResultDTO<String> chatWithMemory(String userId, String content);

    /**
     * 获取聊天历史
     */
    ResultDTO<List<UserAiChat>> getChatHistory(String userId);

    ResultDTO<List<UserAiChat>> getChatHistoryByPage(String userId, int page, int pageSize);
    /**
     * 无记忆、纯数据分析：根据用户统计数据生成AI评语
     */
    ResultDTO<String> generateReportComment(UserStatsForAIDTO stats);

    String generateImage(String prompt, Integer commentId, String referenceImage, Float strength);
}