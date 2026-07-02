package com.example.service;

import com.example.dto.ReadStatsDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadDaily;
import java.time.LocalDate;
import java.util.List;

public interface UserReadDailyService {
    // 新增/更新阅读时长
    ResultDTO<Void> upsertReadDuration(UserReadDaily readDaily);
    // 获取用户某时间段阅读记录
    ResultDTO<List<UserReadDaily>> getReadRecords(String userId, LocalDate startDate, LocalDate endDate);
    // 获取用户阅读统计数据（总时长/连续天数/年阅读天数）
    ResultDTO<ReadStatsDTO> getReadStats(String userId);
    public void refreshAllUserStats();
}