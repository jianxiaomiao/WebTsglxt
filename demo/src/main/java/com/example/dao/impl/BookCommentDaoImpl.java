package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.BookCommentDao;
import com.example.entity.BookComment;
import com.example.util.DBUtil;

public class BookCommentDaoImpl implements BookCommentDao {

    private static final Logger logger = LoggerFactory.getLogger(BookCommentDaoImpl.class);

    @Override
    public void add(BookComment bookComment) {
        if (bookComment == null ) {
            throw new IllegalArgumentException("书籍评论不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into book_comment(Comment, ISBN, UserId, Time, Star) values (?, ?, ?, ?, ?)",
                    bookComment.getComment(), bookComment.getISBN(), bookComment.getUserid(),
                    bookComment.getTime(), bookComment.getStar());
            if (affectedRows == 0) {
                throw new RuntimeException("新增书籍评论失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增书籍评论失败，评论ID：{}", bookComment.getCommentId(), e);
            throw new RuntimeException("新增书籍评论异常", e);
        }
    }

    @Override
    public void update(BookComment bookComment) {
        if (bookComment == null || bookComment.getCommentId() == null) {
            throw new IllegalArgumentException("书籍评论/评论ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update book_comment set ");
            List<Object> params = new ArrayList<>();

            if (bookComment.getComment() != null) {
                sql.append("Comment=?, ");
                params.add(bookComment.getComment());
            }
            if (bookComment.getISBN() != null) {
                sql.append("ISBN=?, ");
                params.add(bookComment.getISBN());
            }
            if (bookComment.getUserid() != null) {
                sql.append("UserId=?, ");
                params.add(bookComment.getUserid());
            }
            if (bookComment.getTime() != null) {
                sql.append("Time=?, ");
                params.add(bookComment.getTime());
            }
            if (bookComment.getStar() != null) {
                sql.append("Star=?, ");
                params.add(bookComment.getStar());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where CommentId=?");
            params.add(bookComment.getCommentId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新书籍评论】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新书籍评论失败，未匹配到评论ID：" + bookComment.getCommentId());
            }
        } catch (SQLException e) {
            logger.error("更新书籍评论失败，评论ID：{}", bookComment.getCommentId(), e);
            throw new RuntimeException("更新书籍评论异常", e);
        }
    }

    @Override
    public void del(Integer commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from book_comment where CommentId = ?", commentId);
            if (affectedRows == 0) {
                throw new RuntimeException("删除书籍评论失败，未匹配到评论ID：" + commentId);
            }
        } catch (SQLException e) {
            logger.error("删除书籍评论失败，评论ID：{}", commentId, e);
            throw new RuntimeException("删除书籍评论异常", e);
        }
    }

    // ====================== 联表查询：评论+书籍信息 ======================
    @Override
    public List<BookComment> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "ORDER BY c.CommentId DESC",
                    rs -> {
                        BookComment bookComment = null;
                        try {
                            String Comment = rs.getString("Comment");
                            String ISBN = rs.getString("ISBN");
                            String Userid = rs.getString("UserId");
                            LocalDateTime Time = rs.getTimestamp("Time") != null
                                    ? rs.getTimestamp("Time").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer Star = rs.getInt("Star");
                            bookComment = new BookComment(Comment, ISBN, Userid, Time, CommentId, Star);
                            // 赋值书籍信息
                            bookComment.setBookName(rs.getString("Bookname"));
                            bookComment.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析书籍评论数据异常", e);
                            throw new RuntimeException("解析书籍评论数据异常", e);
                        }
                        return bookComment;
                    });
        } catch (SQLException e) {
            logger.error("查询所有书籍评论异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookComment> queryByCommentId(Integer commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "WHERE c.CommentId=?",
                    rs -> {
                        BookComment bookComment = null;
                        try {
                            String Comment = rs.getString("Comment");
                            String ISBN = rs.getString("ISBN");
                            String Userid = rs.getString("UserId");
                            LocalDateTime Time = rs.getTimestamp("Time") != null
                                    ? rs.getTimestamp("Time").toLocalDateTime() : null;
                            Integer Star = rs.getInt("Star");
                            bookComment = new BookComment(Comment, ISBN, Userid, Time, commentId, Star);
                            // 赋值书籍信息
                            bookComment.setBookName(rs.getString("Bookname"));
                            bookComment.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析书籍评论数据异常，评论ID：{}", commentId, e);
                            throw new RuntimeException("解析书籍评论数据异常", e);
                        }
                        return bookComment;
                    }, commentId);
        } catch (SQLException e) {
            logger.error("查询书籍评论异常，评论ID：{}", commentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookComment> queryByBookId(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName, u.Name as userName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "LEFT JOIN user_information u ON c.UserId = u.UserId " +
                            "WHERE c.ISBN=? AND c.status=1 ORDER BY c.CommentId DESC",
                    rs -> {
                        BookComment bookComment = null;
                        try {
                            String Comment = rs.getString("Comment");
                            String Userid = rs.getString("UserId");
                            LocalDateTime Time = rs.getTimestamp("Time") != null
                                    ? rs.getTimestamp("Time").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer Star = rs.getInt("Star");
                            bookComment = new BookComment(Comment, isbn, Userid, Time, CommentId, Star);
                            bookComment.setBookName(rs.getString("Bookname"));
                            bookComment.setPictureName(rs.getString("PictureName"));
                            bookComment.setUserName(rs.getString("userName"));
                        } catch (SQLException e) {
                            logger.error("解析书籍评论数据异常，ISBN：{}", isbn, e);
                            throw new RuntimeException("解析书籍评论数据异常", e);
                        }
                        return bookComment;
                    }, isbn);
        } catch (SQLException e) {
            logger.error("查询书籍评论异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookComment> queryByUserId(String userId){
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "WHERE c.UserId=? ORDER BY c.Time DESC",
                    rs -> {
                        BookComment bookComment = null;
                        try {
                            String Comment = rs.getString("Comment");
                            LocalDateTime Time = rs.getTimestamp("Time") != null
                                    ? rs.getTimestamp("Time").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer Star = rs.getInt("Star");
                            String ISBN = rs.getString("ISBN");
                            bookComment = new BookComment(Comment, ISBN, userId, Time, CommentId, Star);
                            // 赋值书籍信息
                            bookComment.setBookName(rs.getString("Bookname"));
                            bookComment.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析书籍评论数据异常，userId：{}", userId, e);
                            throw new RuntimeException("解析书籍评论数据异常", e);
                        }
                        return bookComment;
                    }, userId);
        } catch (SQLException e) {
            logger.error("查询书籍评论异常，userId：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookComment> queryByBookIdPage(String isbn, int offset, int pageSize) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "WHERE c.ISBN=? AND c.status=1 ORDER BY c.CommentId DESC LIMIT ?, ?",
                    rs -> {
                        BookComment bookComment = new BookComment(
                                rs.getString("Comment"),
                                isbn,
                                rs.getString("UserId"),
                                rs.getTimestamp("Time") != null ? rs.getTimestamp("Time").toLocalDateTime() : null,
                                rs.getInt("CommentId"),
                                rs.getInt("Star")
                        );
                        // 赋值书籍信息
                        bookComment.setBookName(rs.getString("Bookname"));
                        bookComment.setPictureName(rs.getString("PictureName"));
                        return bookComment;
                    }, isbn, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询书籍评论异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookComment> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;

        int offset = (pageNum - 1) * pageSize;

        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "WHERE c.UserId = ? ORDER BY c.time DESC LIMIT ? OFFSET ?",
                    rs -> {
                        BookComment comment = new BookComment();
                        comment.setCommentId(rs.getInt("commentId"));
                        comment.setUserid(rs.getString("UserId"));
                        comment.setISBN(rs.getString("ISBN"));
                        comment.setComment(rs.getString("comment"));
                        comment.setStar(rs.getInt("star"));
                        comment.setTime(rs.getTimestamp("time") != null
                                ? rs.getTimestamp("time").toLocalDateTime() : null);
                        // 赋值书籍信息
                        comment.setBookName(rs.getString("Bookname"));
                        comment.setPictureName(rs.getString("PictureName"));
                        return comment;
                    },
                    userId, pageSize, offset
            );
        } catch (SQLException e) {
            logger.error("分页查询用户书籍评论异常，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0L;
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM book_comment WHERE UserId = ?",
                    rs -> rs.getLong("total"),
                    userId
            ).get(0);
        } catch (SQLException e) {
            logger.error("统计用户书籍评论总条数异常，用户ID：{}", userId, e);
            return 0L;
        }
    }

    @Override
    public Integer countBookCommentByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM book_comment WHERE UserId=? AND Time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户书籍评论异常", e);
            return 0;
        }
    }

    @Override
    public List<BookComment> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT c.*, b.Bookname, b.PictureName " +
                            "FROM book_comment c " +
                            "LEFT JOIN book_information b ON c.ISBN = b.ISBN " +
                            "WHERE c.UserId=? AND c.Time BETWEEN ? AND ? " +
                            "ORDER BY c.Time DESC",
                    rs -> {
                        BookComment bookComment = null;
                        try {
                            String Comment = rs.getString("Comment");
                            LocalDateTime Time = rs.getTimestamp("Time") != null
                                    ? rs.getTimestamp("Time").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer Star = rs.getInt("Star");
                            String ISBN = rs.getString("ISBN");
                            bookComment = new BookComment(Comment, ISBN, userId, Time, CommentId, Star);
                            // 赋值书籍信息
                            bookComment.setBookName(rs.getString("Bookname"));
                            bookComment.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析时间段书籍评论数据异常，userId：{}", userId, e);
                            throw new RuntimeException("解析时间段书籍评论数据异常", e);
                        }
                        return bookComment;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段书籍评论异常", userId, e);
            return Collections.emptyList();
        }
    }
}