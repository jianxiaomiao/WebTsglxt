package com.example.dao;

import com.example.entity.UserAiChat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 聊天记录 Dao 接口
 */
public interface AiChatDao {
    /**
     * 保存聊天记录
     */
    Integer add(UserAiChat aiChat);

    /**
     * 查询用户最近 N 条聊天记忆（用于上下文）
     */
    List<UserAiChat> selectRecentMemory(String userId, int limit);

    /**
     * 查询用户全部聊天历史（用于前端渲染）
     */
    List<UserAiChat> selectAllHistory(String userId);

    List<UserAiChat> selectHistoryByPage(String userId, int offset, int limit);

    String selectLastUserQuestion(String userId);

    Integer countAiChatByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID + 时间段 查询AI聊天记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的AI聊天记录列表
     */
    List<UserAiChat> selectHistoryByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}