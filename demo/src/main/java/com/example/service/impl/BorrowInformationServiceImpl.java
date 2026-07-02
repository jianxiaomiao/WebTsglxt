package com.example.service.impl;

import com.example.dao.BorrowInformationDao;
import com.example.dto.ResultDTO;
import com.example.entity.BorrowInformation;
import com.example.service.BorrowInformationService;

import java.util.List;

public class BorrowInformationServiceImpl implements BorrowInformationService {
    private final BorrowInformationDao borrowInformationDao;

    public BorrowInformationServiceImpl(BorrowInformationDao borrowInformationDao) {
        this.borrowInformationDao = borrowInformationDao;
    }

    @Override
    public ResultDTO<List<BorrowInformation>> queryAllBorrows() {
        try {
            List<BorrowInformation> borrows = borrowInformationDao.queryAll();
            return ResultDTO.success(borrows);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BorrowInformation>> queryByNumber(int number) {
        try {
            List<BorrowInformation> borrows = borrowInformationDao.queryByNumber(number);
            return ResultDTO.success(borrows);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询指定数量借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BorrowInformation>> queryBorrowByISBN(String isbn) {
        try {
            if (isbn == null || isbn.trim().isEmpty()) {
                return ResultDTO.paramError("ISBN不能为空");
            }
            List<BorrowInformation> borrows = borrowInformationDao.queryByISBN(isbn);
            return ResultDTO.success(borrows);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ISBN查询借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BorrowInformation>> queryBorrowByUserId(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID必须是正整数");
            }
            List<BorrowInformation> borrows = borrowInformationDao.queryByUserId(userId);
            return ResultDTO.success(borrows);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按用户ID查询借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addBorrow(BorrowInformation borrow) {
        try {
            if (borrow == null) {
                return ResultDTO.paramError("借阅信息不能为空");
            }
            // 可扩展：借阅前校验图书库存、用户借阅上限等业务规则
            borrowInformationDao.add(borrow);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBorrow(Integer borrowId) {
        try {
            if (borrowId == null || borrowId <= 0) {
                return ResultDTO.paramError("借阅ID必须是正整数");
            }
            borrowInformationDao.del(borrowId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除借阅信息失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBorrow(BorrowInformation borrow) {
        try {
            if (borrow == null || borrow.getBorrowId() == null || borrow.getBorrowId() <= 0) {
                return ResultDTO.paramError("借阅信息不能为空，且借阅ID必须是正整数");
            }
            // 可扩展：还书时更新图书库存（now_book +1）
            borrowInformationDao.update(borrow);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新借阅信息失败：" + e.getMessage());
        }
    }
}
