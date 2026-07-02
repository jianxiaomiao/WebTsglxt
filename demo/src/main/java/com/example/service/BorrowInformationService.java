package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BorrowInformation;
import java.util.List;

public interface BorrowInformationService {
    // 查询所有借阅信息
    ResultDTO<List<BorrowInformation>> queryAllBorrows();
    // 按ISBN查询借阅信息
    ResultDTO<List<BorrowInformation>> queryBorrowByISBN(String isbn); // ISBN通常是字符串
    // 按用户ID查询借阅信息
    ResultDTO<List<BorrowInformation>> queryBorrowByUserId(String userId);
    // 新增借阅信息
    ResultDTO<Void> addBorrow(BorrowInformation borrow);
    // 删除借阅信息（按借阅ID）
    ResultDTO<Void> deleteBorrow(Integer borrowId);
    // 更新借阅信息（比如还书时更新状态）
    ResultDTO<Void> updateBorrow(BorrowInformation borrow);

    ResultDTO<List<BorrowInformation>> queryByNumber(int number);
}