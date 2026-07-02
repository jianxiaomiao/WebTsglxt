package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.UserReadProgressDao;
import com.example.entity.UserReadProgress;
import com.example.util.DBUtil;

public class UserReadProgressDaoImpl implements UserReadProgressDao {

    private static final Logger logger = LoggerFactory.getLogger(UserReadProgressDaoImpl.class);

    @Override
    public void add(UserReadProgress userReadProgress) {
        if (userReadProgress == null || userReadProgress.getUserId() == null || userReadProgress.getIsbn() == null) {
            throw new IllegalArgumentException("阅读进度信息/用户ID/ISBN不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_read_progress(UserId, Isbn, Page_Num, Last_read_time) values (?, ?, ?, ?)",
                    userReadProgress.getUserId(), userReadProgress.getIsbn(),
                    userReadProgress.getPageNum(), userReadProgress.getLastReadTime());
            if (affectedRows == 0) {
                throw new RuntimeException("新增阅读进度失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增阅读进度失败，UserId：{}，Isbn：{}", userReadProgress.getUserId(), userReadProgress.getIsbn(), e);
            throw new RuntimeException("新增阅读进度异常", e);
        }
    }

    @Override
    public void update(UserReadProgress userReadProgress) {
        if (userReadProgress == null || userReadProgress.getId() == null) {
            throw new IllegalArgumentException("阅读进度信息/Id不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_read_progress set ");
            List<Object> params = new ArrayList<>();

            if (userReadProgress.getUserId() != null) {
                sql.append("UserId=?, ");
                params.add(userReadProgress.getUserId());
            }
            if (userReadProgress.getIsbn() != null) {
                sql.append("Isbn=?, ");
                params.add(userReadProgress.getIsbn());
            }
            if (userReadProgress.getPageNum() != null) {
                sql.append("Page_Num=?, ");
                params.add(userReadProgress.getPageNum());
            }
            if (userReadProgress.getLastReadTime() != null) {
                sql.append("Last_read_time=?, ");
                params.add(userReadProgress.getLastReadTime());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where Id=?");
            params.add(userReadProgress.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新阅读进度】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新阅读进度失败，未匹配到Id：" + userReadProgress.getId());
            }
        } catch (SQLException e) {
            logger.error("更新阅读进度失败，Id：{}", userReadProgress.getId(), e);
            throw new RuntimeException("更新阅读进度异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from user_read_progress where Id = ?", id);
            if (affectedRows == 0) {
                throw new RuntimeException("删除阅读进度失败，未匹配到Id：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除阅读进度失败，Id：{}", id, e);
            throw new RuntimeException("删除阅读进度异常", e);
        }
    }

    // ====================== 联表查询：阅读进度+书籍信息+书籍类型名称 ======================
    @Override
    public List<UserReadProgress> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT p.*, b.Bookname, b.PictureName, b.Author, b.Type, bt.bookType AS typeName " +
                            "FROM user_read_progress p " +
                            "LEFT JOIN book_information b ON p.Isbn = b.ISBN " +
                            "LEFT JOIN book_type bt ON b.Type = bt.id " +
                            "ORDER BY p.Last_read_time DESC",
                    rs -> {
                        UserReadProgress userReadProgress = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String pageNum = rs.getString("Page_Num");
                            LocalDateTime lastReadTime = rs.getTimestamp("Last_read_time") != null
                                    ? rs.getTimestamp("Last_read_time").toLocalDateTime() : null;
                            userReadProgress = new UserReadProgress(userId, isbn, pageNum, lastReadTime);
                            userReadProgress.setId(id);
                            // 书籍基础信息
                            userReadProgress.setBookName(rs.getString("Bookname"));
                            userReadProgress.setPictureName(rs.getString("PictureName"));
                            userReadProgress.setAuthor(rs.getString("Author"));
                            // 新增：类型ID + 类型文字名称
                            userReadProgress.setType(rs.getObject("Type") == null ? null : rs.getInt("Type"));
                            userReadProgress.setTypeName(rs.getString("typeName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读进度数据异常", e);
                            throw new RuntimeException("解析阅读进度数据异常", e);
                        }
                        return userReadProgress;
                    });
        } catch (SQLException e) {
            logger.error("查询所有阅读进度异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadProgress> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT p.*, b.Bookname, b.PictureName, b.Author, b.Type, bt.bookType AS typeName " +
                            "FROM user_read_progress p " +
                            "LEFT JOIN book_information b ON p.Isbn = b.ISBN " +
                            "LEFT JOIN book_type bt ON b.Type = bt.id " +
                            "WHERE p.Id=?",
                    rs -> {
                        UserReadProgress userReadProgress = null;
                        try {
                            Integer rid = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String pageNum = rs.getString("Page_Num");
                            LocalDateTime lastReadTime = rs.getTimestamp("Last_read_time") != null
                                    ? rs.getTimestamp("Last_read_time").toLocalDateTime() : null;
                            userReadProgress = new UserReadProgress(userId, isbn, pageNum, lastReadTime);
                            userReadProgress.setId(rid);
                            // 书籍基础信息
                            userReadProgress.setBookName(rs.getString("Bookname"));
                            userReadProgress.setPictureName(rs.getString("PictureName"));
                            userReadProgress.setAuthor(rs.getString("Author"));
                            // 类型字段
                            userReadProgress.setType(rs.getObject("Type") == null ? null : rs.getInt("Type"));
                            userReadProgress.setTypeName(rs.getString("typeName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读进度数据异常，Id：{}", id, e);
                            throw new RuntimeException("解析阅读进度数据异常", e);
                        }
                        return userReadProgress;
                    }, id);
        } catch (SQLException e) {
            logger.error("查询阅读进度异常，Id：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadProgress> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT p.*, b.Bookname, b.PictureName, b.Author, b.Type, bt.bookType AS typeName " +
                            "FROM user_read_progress p " +
                            "LEFT JOIN book_information b ON p.Isbn = b.ISBN " +
                            "LEFT JOIN book_type bt ON b.Type = bt.id " +
                            "WHERE p.UserId=? ORDER BY p.Last_read_time DESC",
                    rs -> {
                        UserReadProgress userReadProgress = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String uid = rs.getString("UserId");
                            String isbn = rs.getString("Isbn");
                            String pageNum = rs.getString("Page_Num");
                            LocalDateTime lastReadTime = rs.getTimestamp("Last_read_time") != null
                                    ? rs.getTimestamp("Last_read_time").toLocalDateTime() : null;
                            userReadProgress = new UserReadProgress(uid, isbn, pageNum, lastReadTime);
                            userReadProgress.setId(id);
                            // 书籍基础信息
                            userReadProgress.setBookName(rs.getString("Bookname"));
                            userReadProgress.setPictureName(rs.getString("PictureName"));
                            userReadProgress.setAuthor(rs.getString("Author"));
                            // 类型字段
                            userReadProgress.setType(rs.getObject("Type") == null ? null : rs.getInt("Type"));
                            userReadProgress.setTypeName(rs.getString("typeName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读进度数据异常，UserId：{}", userId, e);
                            throw new RuntimeException("解析阅读进度数据异常", e);
                        }
                        return userReadProgress;
                    }, userId);
        } catch (SQLException e) {
            logger.error("查询阅读进度异常，UserId：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadProgress> queryByIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT p.*, b.Bookname, b.PictureName, b.Author, b.Type, bt.bookType AS typeName " +
                            "FROM user_read_progress p " +
                            "LEFT JOIN book_information b ON p.Isbn = b.ISBN " +
                            "LEFT JOIN book_type bt ON b.Type = bt.id " +
                            "WHERE p.Isbn=?",
                    rs -> {
                        UserReadProgress userReadProgress = null;
                        try {
                            Integer id = rs.getInt("Id");
                            String userId = rs.getString("UserId");
                            String ib = rs.getString("Isbn");
                            String pageNum = rs.getString("Page_Num");
                            LocalDateTime lastReadTime = rs.getTimestamp("Last_read_time") != null
                                    ? rs.getTimestamp("Last_read_time").toLocalDateTime() : null;
                            userReadProgress = new UserReadProgress(userId, ib, pageNum, lastReadTime);
                            userReadProgress.setId(id);
                            // 书籍基础信息
                            userReadProgress.setBookName(rs.getString("Bookname"));
                            userReadProgress.setPictureName(rs.getString("PictureName"));
                            userReadProgress.setAuthor(rs.getString("Author"));
                            // 类型字段
                            userReadProgress.setType(rs.getObject("Type") == null ? null : rs.getInt("Type"));
                            userReadProgress.setTypeName(rs.getString("typeName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读进度数据异常，Isbn：{}", isbn, e);
                            throw new RuntimeException("解析阅读进度数据异常", e);
                        }
                        return userReadProgress;
                    }, isbn);
        } catch (SQLException e) {
            logger.error("查询阅读进度异常，Isbn：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadProgress> queryByUserIdAndIsbn(String userId, String isbn) {
        if (userId == null || userId.isEmpty() || isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("UserId和ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT p.*, b.Bookname, b.PictureName, b.Author, b.Type, bt.bookType AS typeName " +
                            "FROM user_read_progress p " +
                            "LEFT JOIN book_information b ON p.Isbn = b.ISBN " +
                            "LEFT JOIN book_type bt ON b.Type = bt.id " +
                            "WHERE p.UserId=? and p.Isbn=?",
                    rs -> {
                        UserReadProgress userReadProgress = new UserReadProgress();
                        try {
                            userReadProgress.setId(rs.getInt("Id"));
                            userReadProgress.setUserId(rs.getString("UserId"));
                            userReadProgress.setIsbn(rs.getString("Isbn"));
                            userReadProgress.setPageNum(rs.getString("Page_Num"));
                            userReadProgress.setLastReadTime(
                                    rs.getTimestamp("Last_read_time") != null
                                            ? rs.getTimestamp("Last_read_time").toLocalDateTime()
                                            : null
                            );
                            // 书籍基础信息
                            userReadProgress.setBookName(rs.getString("Bookname"));
                            userReadProgress.setPictureName(rs.getString("PictureName"));
                            userReadProgress.setAuthor(rs.getString("Author"));
                            // 类型字段
                            userReadProgress.setType(rs.getObject("Type") == null ? null : rs.getInt("Type"));
                            userReadProgress.setTypeName(rs.getString("typeName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读进度数据异常，UserId：{}，ISBN：{}", userId, isbn, e);
                            throw new RuntimeException("解析阅读进度数据异常", e);
                        }
                        return userReadProgress;
                    }, userId, isbn);
        } catch (SQLException e) {
            logger.error("查询阅读进度异常，UserId：{}，ISBN：{}", userId, isbn, e);
            return Collections.emptyList();
        }
    }

    // ====================== 统计阅读书籍数量 ======================
    public Integer countReadBooksByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(DISTINCT Isbn) AS total FROM user_read_progress WHERE UserId=? AND Last_read_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户阅读书籍数量异常", e);
            return 0;
        }
    }
}