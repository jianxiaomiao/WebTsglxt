package com.example.dao;

import com.example.entity.UserReadStats;

public interface UserReadStatsDao {
    // 插入/更新缓存统计数据
    void upsertStats(UserReadStats stats);
    // 根据用户ID查询缓存
    UserReadStats getStatsByUserId(String userId);
}