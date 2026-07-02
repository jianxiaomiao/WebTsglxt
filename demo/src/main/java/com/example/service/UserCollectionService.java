package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserCollection;

import java.util.List;
import java.util.Map;

public interface UserCollectionService {
    ResultDTO<List<UserCollection>> queryAllCollections();
    ResultDTO<List<UserCollection>> queryCollectionById(Integer collectionId);
    ResultDTO<Map<String, Object>> queryCollectionByUserId(String userId, Integer page, Integer pageSize);
    ResultDTO<List<UserCollection>> queryCollectionByIsbn(String isbn);
    ResultDTO<Void> addCollection(UserCollection userCollection);
    ResultDTO<Void> deleteCollection(Integer collectionId);
    ResultDTO<Void> updateCollection(UserCollection userCollection);

    // 新增：查询单个分组下所有书籍（分页）
    ResultDTO<Map<String, Object>> queryCollectionByUserAndGroup(String userId, Integer groupId, Integer page, Integer pageSize);
}