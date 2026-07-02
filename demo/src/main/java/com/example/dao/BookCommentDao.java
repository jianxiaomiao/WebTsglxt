package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.example.entity.BookComment;

public interface BookCommentDao {
    void add(BookComment bookComment);
    void update(BookComment bookComment);
    void del(Integer commentId);
    List<BookComment> queryAll();
    List<BookComment> queryByCommentId(Integer commentId);
    List<BookComment> queryByBookId(String isbn);
    List<BookComment> queryByUserId(String userId);
    List<BookComment> queryByBookIdPage(String isbn, int offset, int pageSize);

    // ====================== 新增：分页查询指定用户的书籍评论 ======================
    List<BookComment> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    // ====================== 新增：统计指定用户的书籍评论总条数 ======================
    Long countByUserId(String userId);

    Integer countBookCommentByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID + 时间段 查询书籍评论
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的书籍评论列表
     */
    List<BookComment> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}