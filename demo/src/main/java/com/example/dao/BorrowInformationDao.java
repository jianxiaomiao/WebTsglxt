package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.example.entity.BorrowInformation;

public interface BorrowInformationDao {
    void add(BorrowInformation borrowInfo);
    void update(BorrowInformation borrowInfo);
    void del(Integer borrowId);
    List<BorrowInformation> queryAll();
    List<BorrowInformation> queryByBorrowId(Integer borrowId);
    List<BorrowInformation> queryByUserId(String id);
    List<BorrowInformation> queryByISBN(String isbn);
    List<BorrowInformation> queryByNumber(int number);
    Integer countBorrowByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
    /**
     * 根据用户ID + 借阅时间段 查询借阅记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的借阅记录列表
     */
    List<BorrowInformation> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}