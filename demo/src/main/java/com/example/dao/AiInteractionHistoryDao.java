package com.example.dao;

import com.example.entity.AiInteractionHistory;
import java.time.LocalDateTime;
import java.util.List;

public interface AiInteractionHistoryDao {
    // 基础CRUD
    void add(AiInteractionHistory history);
    void update(AiInteractionHistory history);
    void del(Integer id);

    // 基础查询
    List<AiInteractionHistory> queryById(Integer id);
    List<AiInteractionHistory> queryByUserId(String userId);

    // 1. 用户ID + 时间区间 查询（不分页）
    List<AiInteractionHistory> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
    Long countByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    // 2. 用户ID 查询最新N条（不分页）
    List<AiInteractionHistory> queryLatestByUserId(String userId, Integer limit);

    // 3. 用户ID 分页查询最新数据（分页版）
    List<AiInteractionHistory> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    Long countByUserId(String userId);

    // 4. 根据 用户ID + ISBN 查询
    List<AiInteractionHistory> queryByUserIdAndIsbn(String userId, String isbn);

    // 5. 根据 记忆锚点 summary_tag 查询
    List<AiInteractionHistory> queryBySummaryTag(String summaryTag);
}