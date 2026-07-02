package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookComment;
import java.util.List;

public interface BookCommentService {
    // 查询所有书籍评论
    ResultDTO<List<BookComment>> queryAllBookComments();
    // 按图书ID查询评论
    ResultDTO<List<BookComment>> queryBookCommentByBookId(String bookId);
    ResultDTO<List<BookComment>> queryBookCommentByUserId(String userId);
    ResultDTO<List<BookComment>> queryByCommentId(Integer commentId);
    // 新增书籍评论
    ResultDTO<Void> addBookComment(BookComment comment);
    // 删除书籍评论（按评论ID）
    ResultDTO<Void> deleteBookComment(int commentId);
    // 更新书籍评论
    ResultDTO<Void> updateBookComment(BookComment comment);
    ResultDTO<List<BookComment>> queryBookCommentByBookId(String bookId, int page, int pageSize);

    // ====================== 新增：分页查询指定用户的书籍评论 ======================
    ResultDTO<PageResultDTO<BookComment>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);
}
