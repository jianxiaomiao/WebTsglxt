package com.example.dao;

import com.example.entity.UserProfileMemory;
import java.time.LocalDate;
import java.util.List;

public interface UserProfileMemoryDao {
    // 基础CRUD
    void add(UserProfileMemory memory);
    void update(UserProfileMemory memory);
    void del(Integer id);

    // 基础查询
    List<UserProfileMemory> queryById(Integer id);
    List<UserProfileMemory> queryByUserId(String userId);

    // 1. 用户ID + 日期区间 查询（不分页）
    List<UserProfileMemory> queryByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate);
    // 统计 用户ID+日期区间 总数
    Long countByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate);

    // 2. 用户ID 查询最新N条（不分页，指定条数）
    List<UserProfileMemory> queryLatestByUserId(String userId, Integer limit);

    // 3. 用户ID 分页查询最新数据（分页版）
    List<UserProfileMemory> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    // 统计 用户ID 总条数（分页用）
    Long countByUserId(String userId);
}