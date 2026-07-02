package com.example.service.impl;

import com.example.dao.UserReadProgressDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadProgress;
import com.example.service.UserReadProgressService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserReadProgressServiceImpl implements UserReadProgressService {

    private final UserReadProgressDao userReadProgressDao;

    public UserReadProgressServiceImpl(UserReadProgressDao userReadProgressDao) {
        this.userReadProgressDao = userReadProgressDao;
    }

    @Override
    public ResultDTO<List<UserReadProgress>> queryAllProgress() {
        try {
            List<UserReadProgress> progressList = userReadProgressDao.queryAll();
            return ResultDTO.success(progressList);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有阅读进度失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadProgress>> queryProgressById(Integer id) {
        try {
            List<UserReadProgress> progressList = userReadProgressDao.queryById(id);
            return ResultDTO.success(progressList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadProgress>> queryProgressByUserId(String userId) {
        try {
            List<UserReadProgress> progressList = userReadProgressDao.queryByUserId(userId);
            return ResultDTO.success(progressList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadProgress>> queryProgressByIsbn(String isbn) {
        try {
            List<UserReadProgress> progressList = userReadProgressDao.queryByIsbn(isbn);
            return ResultDTO.success(progressList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addProgress(UserReadProgress userReadProgress) {
        try {
            userReadProgressDao.add(userReadProgress);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteProgress(Integer id) {
        try {
            userReadProgressDao.del(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateProgress(UserReadProgress userReadProgress) {
        try {
            userReadProgressDao.update(userReadProgress);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadProgress>> queryProgressByUserIdAndIsbn(String userId, String isbn) {
        try {
            List<UserReadProgress> progressList = userReadProgressDao.queryByUserIdAndIsbn(userId, isbn);
            return ResultDTO.success(progressList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }
}