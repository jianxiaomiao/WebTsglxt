package com.example.service.impl;

import com.example.dao.BookTypeDao;
import com.example.dao.UserTextColTypeDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookType;
import com.example.entity.UserTextColType;
import com.example.service.UserTextColTypeService;

import java.util.List;

public class UserTextColTypeServiceImpl implements UserTextColTypeService
{
    private final UserTextColTypeDao userTextColTypeDao;

    public UserTextColTypeServiceImpl(UserTextColTypeDao userTextColTypeDao) {
        this.userTextColTypeDao = userTextColTypeDao;
    }
    @Override
    public ResultDTO<List<UserTextColType>> queryAllUserTextColType() {
        try {
            List<UserTextColType> list = userTextColTypeDao.queryAll();
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有用户笔记种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserTextColType>> queryUserTextColTypeId(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("用户笔记种类ID不能为空");
            List<UserTextColType> list = userTextColTypeDao.queryById(id);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询用户笔记种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addUserTextColType(UserTextColType userTextColType) {
        try {
            if (userTextColType == null) return ResultDTO.paramError("用户笔记种类信息不能为空");
            userTextColTypeDao.add(userTextColType);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增用户笔记种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUserTextColType(UserTextColType userTextColType) {
        try {
            if (userTextColType == null || userTextColType.getId() == null) return ResultDTO.paramError("用户笔记种类信息/ID不能为空");
            userTextColTypeDao.update(userTextColType);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新用户笔记种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteUserTextColType(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("用户笔记种类ID不能为空");
            userTextColTypeDao.del(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除用户笔记种类失败：" + e.getMessage());
        }
    }
}
