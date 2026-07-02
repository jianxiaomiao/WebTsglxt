package com.example.dao.impl;

import com.example.dao.UserTextCollectionDao;
import com.example.entity.UserTextCollection;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserTextCollectionDaoImpl implements UserTextCollectionDao {

    private static final Logger logger = LoggerFactory.getLogger(UserTextCollectionDaoImpl.class);

    @Override
    public void add(UserTextCollection note) {
        if (note == null) {
            throw new IllegalArgumentException("阅读笔记信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_text_collection(UserId, Isbn, chapterId, Text, createTime, Type, reader_comment) values (?, ?, ?, ?, ?, ?, ?)",
                    note.getUserId(),
                    note.getIsbn(),
                    note.getChapterId(),
                    note.getText(),
                    note.getCreateTime(),
                    note.getType(),
                    note.getReaderComment()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增阅读笔记失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增阅读笔记失败", e);
            throw new RuntimeException("新增阅读笔记异常", e);
        }
    }

    @Override
    public void update(UserTextCollection note) {
        if (note == null || note.getId() == null) {
            throw new IllegalArgumentException("阅读笔记信息/ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_text_collection set ");
            List<Object> params = new ArrayList<>();

            if (note.getUserId() != null) {
                sql.append("UserId=?, ");
                params.add(note.getUserId());
            }
            if (note.getIsbn() != null) {
                sql.append("Isbn=?, ");
                params.add(note.getIsbn());
            }
            if (note.getChapterId() != null) {
                sql.append("chapterId=?, ");
                params.add(note.getChapterId());
            }
            if (note.getText() != null) {
                sql.append("Text=?, ");
                params.add(note.getText());
            }
            if (note.getCreateTime() != null) {
                sql.append("createTime=?, ");
                params.add(note.getCreateTime());
            }
            if (note.getType() != null) {
                sql.append("Type=?, ");
                params.add(note.getType());
            }
            if (note.getReaderComment() != null) {
                sql.append("reader_comment=?, ");
                params.add(note.getReaderComment());
            }
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where Id=?");
            params.add(note.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新阅读笔记】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新阅读笔记失败，未匹配到ID：" + note.getId());
            }
        } catch (SQLException e) {
            logger.error("更新阅读笔记失败，ID：{}", note.getId(), e);
            throw new RuntimeException("更新阅读笔记异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("阅读笔记ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "delete from user_text_collection where Id = ?",
                    id
            );
            if (affectedRows == 0) {
                throw new RuntimeException("删除阅读笔记失败，未匹配到ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除阅读笔记失败，ID：{}", id, e);
            throw new RuntimeException("删除阅读笔记异常", e);
        }
    }

    // ====================== 联表查询：笔记+类型+书籍信息 ======================
    @Override
    public List<UserTextCollection> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            // 读取书籍信息
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析阅读笔记数据异常", e);
                            throw new RuntimeException("解析阅读笔记数据异常", e);
                        }
                        return note;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有阅读笔记异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserTextCollection> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("阅读笔记ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.Id=?",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析阅读笔记数据异常，ID：{}", id, e);
                            throw new RuntimeException("解析阅读笔记数据异常", e);
                        }
                        return note;
                    }, id
            );
        } catch (SQLException e) {
            logger.error("查询阅读笔记异常，ID：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserTextCollection> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.UserId=? ORDER BY utc.Id DESC",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String isbn = rs.getString("Isbn");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析用户阅读笔记数据异常，用户ID：{}", userId, e);
                            throw new RuntimeException("解析用户阅读笔记数据异常", e);
                        }
                        return note;
                    }, userId
            );
        } catch (SQLException e) {
            logger.error("查询用户阅读笔记异常，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserTextCollection> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.UserId=? ORDER BY utc.Id DESC LIMIT ? OFFSET ?",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String isbn = rs.getString("Isbn");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析用户阅读笔记数据异常，用户ID：{}", userId, e);
                            throw new RuntimeException("解析用户阅读笔记数据异常", e);
                        }
                        return note;
                    }, userId, pageSize, offset
            );
        } catch (SQLException e) {
            logger.error("分页查询用户阅读笔记异常，用户ID：{}", userId, e);
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
                    "SELECT COUNT(*) AS total FROM user_text_collection utc WHERE utc.UserId=?",
                    rs -> rs.getLong("total"),
                    userId
            ).get(0);
        } catch (SQLException e) {
            logger.error("统计用户阅读笔记总数异常，用户ID：{}", userId, e);
            return 0L;
        }
    }

    @Override
    public List<UserTextCollection> queryByIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("书籍ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.Isbn=?",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析书籍阅读笔记数据异常，ISBN：{}", isbn, e);
                            throw new RuntimeException("解析书籍阅读笔记数据异常", e);
                        }
                        return note;
                    }, isbn
            );
        } catch (SQLException e) {
            logger.error("查询书籍阅读笔记异常，ISBN：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserTextCollection> queryByChapterId(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) {
            throw new IllegalArgumentException("章节ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.chapterId=?",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析章节阅读笔记数据异常，章节ID：{}", chapterId, e);
                            throw new RuntimeException("解析章节阅读笔记数据异常", e);
                        }
                        return note;
                    }, chapterId
            );
        } catch (SQLException e) {
            logger.error("查询章节阅读笔记异常，章节ID：{}", chapterId, e);
            return Collections.emptyList();
        }
    }

    public Integer countNewNotesByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_text_collection WHERE UserId=? AND createTime BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户新增笔记异常", e);
            return 0;
        }
    }

    @Override
    public List<UserTextCollection> queryByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT utc.*, t.userTextColType AS noteTypeName, b.Bookname, b.PictureName " +
                            "FROM user_text_collection utc " +
                            "LEFT JOIN user_text_col_type t ON utc.Type = t.id " +
                            "LEFT JOIN book_information b ON utc.Isbn = b.ISBN " +
                            "WHERE utc.createTime BETWEEN ? AND ? ORDER BY utc.createTime DESC",
                    rs -> {
                        UserTextCollection note = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String chapterId = rs.getString("chapterId");
                            String text = rs.getString("Text");
                            LocalDateTime createTime = rs.getTimestamp("createTime") != null
                                    ? rs.getTimestamp("createTime").toLocalDateTime() : null;
                            Integer Type = rs.wasNull() ? null : rs.getInt("Type");
                            String noteTypeName = rs.getString("noteTypeName") != null ? rs.getString("noteTypeName") : "普通笔记";
                            String readerComment = rs.getString("reader_comment");
                            String bookName = rs.getString("Bookname");
                            String pictureName = rs.getString("PictureName");

                            note = new UserTextCollection(id, userId, isbn, chapterId, text, createTime, Type, noteTypeName, readerComment, bookName, pictureName);
                        } catch (SQLException e) {
                            logger.error("解析时间段阅读笔记数据异常", e);
                            throw new RuntimeException("解析时间段阅读笔记数据异常", e);
                        }
                        return note;
                    }, startTime, endTime
            );
        } catch (SQLException e) {
            logger.error("按时间段查询阅读笔记异常", e);
            return Collections.emptyList();
        }
    }
}