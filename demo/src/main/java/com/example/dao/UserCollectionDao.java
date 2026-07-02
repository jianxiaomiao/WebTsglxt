package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;
import com.example.entity.UserCollection;

public interface UserCollectionDao {
    void add(UserCollection userCollection);
    void update(UserCollection userCollection);
    void del(Integer collectionId);
    List<UserCollection> queryAll();
    List<UserCollection> queryByCollectionId(Integer collectionId);
    List<UserCollection> queryByUserId(String userId, Integer pageNum, Integer pageSize); // 外键查询：查某用户的所有收藏
    List<UserCollection> queryByIsbn(String isbn); // 外键查询：查某书被哪些用户收藏
    int countByUserId(String userId);
    Integer countCollectionByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
    /**
     * 根据用户ID + 时间段 查询收藏记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的收藏列表
     */
    List<UserCollection> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
    // 新增：根据用户ID+分组ID查询（支持分页）
    List<UserCollection> queryByUserIdAndGroupId(String userId, Integer groupId, Integer pageNum, Integer pageSize);
    // 新增：统计单个分组下书籍总数
    int countByUserIdAndGroupId(String userId, Integer groupId);

    // 批量清空指定用户、指定分组下所有收藏的GroupId为null
    void clearCollectionGroupByGroupId(String userId, Integer groupId);
}