package com.example.dao;

import com.example.entity.UserBehaviorLog;
import java.time.LocalDateTime;
import java.util.List;

public interface UserBehaviorLogDao {
    // 基础CRUD
    void add(UserBehaviorLog log);
    void update(UserBehaviorLog log);
    void del(Integer id);

    // 基础查询
    List<UserBehaviorLog> queryById(Integer id);
    List<UserBehaviorLog> queryByUserId(String userId);

    // 1. 用户ID + 时间区间 查询（不分页）
    List<UserBehaviorLog> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
    // 统计 用户ID+时间区间 总数
    Long countByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    // 2. 用户ID 查询最新N条（不分页，指定条数）
    List<UserBehaviorLog> queryLatestByUserId(String userId, Integer limit);

    // 3. 用户ID 分页查询最新数据（分页版）
    List<UserBehaviorLog> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    // 统计 用户ID 总条数（分页用）
    Long countByUserId(String userId);

    /**
     * 根据用户ID、行为类型查询最新一条行为日志
     * @param userId 用户ID
     * @param actionType 行为类型
     * @return 最新单条日志，无数据返回null
     */
    UserBehaviorLog getLatestByUserIdAndType(String userId, Integer actionType);

    /**
     * 根据用户ID、行为类型分页查询行为日志（按创建时间倒序）
     * @param userId 用户ID
     * @param actionType 行为类型
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页日志列表
     */
    List<UserBehaviorLog> pageByUserIdAndType(String userId, Integer actionType, Integer pageNum, Integer pageSize);

    /**
     * 查询指定时间段内有行为日志的所有去重用户ID（昨日活跃用户）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 去重userId列表
     */
    List<String> queryActiveUserIds(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID、书籍ISBN、行为类型查询最新一条行为日志
     * @param userId 用户ID
     * @param isbn 书籍ISBN
     * @param actionType 行为类型
     * @return 最新单条日志，无数据返回null
     */
    UserBehaviorLog getLatestByUserIdAndIsbnAndType(String userId, String isbn, Integer actionType);

    /**
     * 根据用户ID、书籍ISBN、行为类型分页查询行为日志（按创建时间倒序）
     * @param userId 用户ID
     * @param isbn 书籍ISBN
     * @param actionType 行为类型
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页日志列表
     */
    List<UserBehaviorLog> pageByUserIdAndIsbnAndType(String userId, String isbn, Integer actionType, Integer pageNum, Integer pageSize);
}