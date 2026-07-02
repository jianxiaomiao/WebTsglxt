package com.example.dao;

import com.example.entity.UserBookshelfGroup;
import java.util.List;

public interface UserBookshelfGroupDao {
    // 新增分组
    void add(UserBookshelfGroup group);

    // 动态更新分组
    void update(UserBookshelfGroup group);

    // 根据GroupId删除分组
    void delete(Integer groupId);

    // 根据GroupId查询单条
    List<UserBookshelfGroup> queryById(Integer groupId);

    // 根据UserId分页查询用户所有分组，排序：Sort DESC, create_time DESC
    List<UserBookshelfGroup> queryByUserId(String userId, Integer pageNum, Integer pageSize);

    // 统计用户分组总数
    int countByUserId(String userId);

}