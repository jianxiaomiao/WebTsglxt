package com.example.dao;

import com.example.entity.UserReport;

import java.util.List;

public interface UserReportDao {
    // 根据用户+类型+时间标记查询缓存
    List<UserReport> selectByUserAndTypeAndDate(String userId, String type, String date);

    // 新增缓存数据
    void insert(UserReport stats);

    // ✅ 新增：分页查询报告历史（支持类型筛选+分页）
    List<UserReport> selectReportHistory(String userId, String type, Integer pageNum, Integer pageSize);
}