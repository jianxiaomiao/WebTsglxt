package com.example.service.impl;

import com.example.dao.UserCollectionDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserCollection;
import com.example.service.UserCollectionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCollectionServiceImpl implements UserCollectionService {

    private final UserCollectionDao userCollectionDao;

    public UserCollectionServiceImpl(UserCollectionDao userCollectionDao) {
        this.userCollectionDao = userCollectionDao;
    }

    @Override
    public ResultDTO<List<UserCollection>> queryAllCollections() {
        try {
            List<UserCollection> collections = userCollectionDao.queryAll();
            return ResultDTO.success(collections);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有收藏失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserCollection>> queryCollectionById(Integer collectionId) {
        try {
            List<UserCollection> collections = userCollectionDao.queryByCollectionId(collectionId);
            return ResultDTO.success(collections);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Map<String, Object>> queryCollectionByUserId(String userId, Integer page, Integer pageSize) {
        try {
            // 1. 查询分页数据
            List<UserCollection> collections = userCollectionDao.queryByUserId(userId, page, pageSize);
            // 2. 查询总条数
            int total = userCollectionDao.countByUserId(userId);

            // 封装分页结果（前端需要用total判断是否还有数据）
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("list", collections);    // 当前页数据
            resultMap.put("total", total);          // 总条数
            resultMap.put("page", page);            // 当前页
            resultMap.put("pageSize", pageSize);    // 每页条数

            return ResultDTO.success(resultMap);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    // ========== 新增：按用户+分组查询 ==========
    @Override
    public ResultDTO<Map<String, Object>> queryCollectionByUserAndGroup(String userId, Integer groupId, Integer page, Integer pageSize) {
        try {
            List<UserCollection> collections = userCollectionDao.queryByUserIdAndGroupId(userId, groupId, page, pageSize);
            int total = userCollectionDao.countByUserIdAndGroupId(userId, groupId);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("list", collections);
            resultMap.put("total", total);
            resultMap.put("page", page);
            resultMap.put("pageSize", pageSize);

            return ResultDTO.success(resultMap);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserCollection>> queryCollectionByIsbn(String isbn) {
        try {
            List<UserCollection> collections = userCollectionDao.queryByIsbn(isbn);
            return ResultDTO.success(collections);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addCollection(UserCollection userCollection) {
        try {
            userCollectionDao.add(userCollection);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteCollection(Integer collectionId) {
        try {
            userCollectionDao.del(collectionId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateCollection(UserCollection userCollection) {
        try {
            userCollectionDao.update(userCollection);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }
}