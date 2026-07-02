package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserBookshelfGroup;
import java.util.List;
import java.util.Map;

public interface UserBookshelfGroupService {
    // 新增分组
    ResultDTO<Void> addGroup(UserBookshelfGroup group);

    // 更新分组
    ResultDTO<Void> updateGroup(UserBookshelfGroup group);

    // 删除分组
    ResultDTO<Void> deleteGroup(Integer groupId);

    // 根据ID查询
    ResultDTO<List<UserBookshelfGroup>> queryGroupById(Integer groupId);

    // 根据用户ID分页查询，返回分页数据+总数
    ResultDTO<Map<String, Object>> queryGroupByUserId(String userId, Integer page, Integer pageSize);
}