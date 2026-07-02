package com.example.dao;

import com.example.entity.UserReadDaily;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface UserReadDailyDao {
    // 新增/更新用户某天阅读时长（支持同一天多次累加）
    void upsertReadDuration(UserReadDaily readDaily);
    // 查询用户某时间段阅读记录
    List<UserReadDaily> queryByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate);
    // 查询用户所有阅读日期（用于计算连续天数）
    List<LocalDate> queryReadDatesByUserId(String userId);
    // 统计用户总阅读时长
    Integer sumTotalDuration(String userId);

    Integer sumReadSecondByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    Integer countReadDaysByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
}