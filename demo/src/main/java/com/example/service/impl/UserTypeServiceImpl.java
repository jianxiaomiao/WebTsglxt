package com.example.service.impl;

import com.example.dao.UserTypeDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserType;
import com.example.service.UserTypeService;
import com.example.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTypeServiceImpl implements UserTypeService {

    private final UserTypeDao userTypeDao;
    private static final String CACHE_KEY = "dict:user_type:all";
    private static final int CACHE_TTL = 3600;

    public UserTypeServiceImpl(UserTypeDao userTypeDao) {
        this.userTypeDao = userTypeDao;
    }

    @Override
    public ResultDTO<List<UserType>> queryAllUserTypes() {
        try {
            try {
                String cached = RedisUtil.get(CACHE_KEY);
                if (cached != null) {
                    return ResultDTO.success(JSON.parseArray(cached, UserType.class));
                }
            } catch (Exception ignored) {}

            List<UserType> list = userTypeDao.queryAll();
            try { RedisUtil.set(CACHE_KEY, JSON.toJSONString(list), CACHE_TTL); } catch (Exception ignored) {}
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有用户种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserType>> queryUserTypeById(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("用户种类ID不能为空");
            List<UserType> list = userTypeDao.queryById(id);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询用户种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addUserType(UserType userType) {
        try {
            if (userType == null) return ResultDTO.paramError("用户种类信息不能为空");
            userTypeDao.add(userType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增用户种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUserType(UserType userType) {
        try {
            if (userType == null || userType.getId() == null) return ResultDTO.paramError("用户种类信息/ID不能为空");
            userTypeDao.update(userType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新用户种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteUserType(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("用户种类ID不能为空");
            userTypeDao.del(id);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除用户种类失败：" + e.getMessage());
        }
    }
}
