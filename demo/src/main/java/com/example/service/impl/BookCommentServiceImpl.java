package com.example.service.impl;

import com.example.dao.BookCommentDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookComment;
import com.example.service.BookCommentService;

import java.util.List;

public class BookCommentServiceImpl implements BookCommentService {
    // 注入已有的BookCommentDao实现类（通过构造函数传入）
    private final BookCommentDao bookCommentDao;

    public BookCommentServiceImpl(BookCommentDao bookCommentDao) {
        this.bookCommentDao = bookCommentDao;
    }

    @Override
    public ResultDTO<List<BookComment>> queryAllBookComments() {
        try {
            List<BookComment> comments = bookCommentDao.queryAll();
            return ResultDTO.success(comments);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有书籍评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookComment>> queryBookCommentByBookId(String bookId) {
        try {
            List<BookComment> comments = bookCommentDao.queryByBookId(bookId);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按图书ID查询评论失败：" + e.getMessage());
        }
    }

    // 实现类
    @Override
    public ResultDTO<List<BookComment>> queryByCommentId(Integer commentId) {
        try {
            List<BookComment> comments = bookCommentDao.queryByCommentId(commentId);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询评论失败：" + e.getMessage());
        }
    }
    // BookCommentServiceImpl.java
    @Override
    public ResultDTO<List<BookComment>> queryBookCommentByUserId(String userId) {
        try {
            List<BookComment> list = bookCommentDao.queryByUserId(userId);
            // 按时间倒序（最新在前）
            return ResultDTO.success(list);
        }  catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按userID查询评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addBookComment(BookComment comment) {
        try {
            bookCommentDao.add(comment);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增书籍评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBookComment(int commentId) {
        try {
            bookCommentDao.del(commentId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除书籍评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBookComment(BookComment comment) {
        try {
            bookCommentDao.update(comment);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新书籍评论失败：" + e.getMessage());
        }
    }
    // ===================== 修改：分页查询书籍评论 =====================
    @Override
    public ResultDTO<List<BookComment>> queryBookCommentByBookId(String bookId, int page, int pageSize) {
        try {
            // 计算分页偏移量：(页码-1)*每页条数
            int offset = (page - 1) * pageSize;
            // 调用分页DAO方法
            List<BookComment> comments = bookCommentDao.queryByBookIdPage(bookId, offset, pageSize);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookComment>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            List<BookComment> comments = bookCommentDao.queryByUserIdPage(userId, pageNum, pageSize);
            Long total = bookCommentDao.countByUserId(userId);
            PageResultDTO<BookComment> pageResult = PageResultDTO.success(total, pageNum, pageSize, comments);
            return ResultDTO.success(pageResult);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询书籍评论失败：" + e.getMessage());
        }
    }
}