package com.example.service.impl;

import com.example.dao.BookBottlePickDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookBottlePick;
import com.example.service.BookBottlePickService;

import java.time.LocalDateTime;
import java.util.List;

public class BookBottlePickServiceImpl implements BookBottlePickService {
    private final BookBottlePickDao pickDao;

    public BookBottlePickServiceImpl(BookBottlePickDao pickDao) {
        this.pickDao = pickDao;
    }

    @Override
    public ResultDTO<Void> addPick(BookBottlePick pick) {
        try {
            pick.setCreatetime(LocalDateTime.now());
            pickDao.addPick(pick);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增捞取记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Boolean> checkUserPicked(String userId, Integer bottleId) {
        try {
            boolean picked = pickDao.checkUserPicked(userId, bottleId);
            return ResultDTO.success(picked);
        } catch (Exception e) {
            return ResultDTO.fail("检查捞取状态失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookBottlePick>> queryByBottleId(Integer bottleId) {
        try {
            List<BookBottlePick> list = pickDao.queryByBottleId(bottleId);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询瓶子捞取记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookBottlePick>> queryByUserId(String userId) {
        try {
            List<BookBottlePick> list = pickDao.queryByUserId(userId);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询用户捞取记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateReply(Integer pickId, String replycontent) {
        try {
            pickDao.updateReply(pickId, replycontent);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新回复失败：" + e.getMessage());
        }
    }
}