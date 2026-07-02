package com.example.dao.impl;

import com.example.dao.UserReadRecordDao;
import com.example.entity.UserReadRecord;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户阅读记录DAO实现类
 */
public class UserReadRecordDaoImpl implements UserReadRecordDao {

    private static final Logger logger = LoggerFactory.getLogger(UserReadRecordDaoImpl.class);

    @Override
    public void add(UserReadRecord userReadRecord) {
        // 参数校验
        if (userReadRecord == null || userReadRecord.getUserId() == null
                || userReadRecord.getIsbn() == null || userReadRecord.getReadDuration() == null
                || userReadRecord.getReadDate() == null) {
            throw new IllegalArgumentException("阅读记录信息/用户ID/ISBN/阅读时长/阅读日期不能为空");
        }

        try {
            int affectedRows = DBUtil.executeUpdate("""
            INSERT INTO user_read_record(user_id, isbn, read_date, read_duration, create_time) 
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                read_duration = read_duration + VALUES(read_duration),
                create_time = VALUES(create_time)
            """,
                    userReadRecord.getUserId(),
                    userReadRecord.getIsbn(),
                    userReadRecord.getReadDate(),
                    userReadRecord.getReadDuration(),
                    userReadRecord.getCreateTime()
            );

            if (affectedRows == 0) {
                throw new RuntimeException("新增/更新阅读记录失败，受影响行数为0");
            }

            logger.info("【原子性更新阅读记录】用户:{}, 书籍:{}, 日期:{}, 新增时长:{}秒",
                    userReadRecord.getUserId(), userReadRecord.getIsbn(),
                    userReadRecord.getReadDate(), userReadRecord.getReadDuration());

        } catch (SQLException e) {
            logger.error("新增/更新阅读记录失败，UserId：{}，Isbn：{}",
                    userReadRecord.getUserId(), userReadRecord.getIsbn(), e);
            throw new RuntimeException("新增/更新阅读记录异常", e);
        }
    }


    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("DELETE FROM user_read_record WHERE id = ?", id);
            if (affectedRows == 0) {
                throw new RuntimeException("删除阅读记录失败，未匹配到Id：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除阅读记录失败，Id：{}", id, e);
            throw new RuntimeException("删除阅读记录异常", e);
        }
    }

    // ====================== 联表查询：阅读记录+书籍信息 ======================
    @Override
    public List<UserReadRecord> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT r.*, b.Bookname, b.PictureName " +
                            "FROM user_read_record r " +
                            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
                            "ORDER BY r.create_time DESC",
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date") != null ? rs.getDate("read_date").toLocalDate() : null);
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            // 赋值书籍信息
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常", e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有阅读记录异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadRecord> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT r.*, b.Bookname, b.PictureName " +
                            "FROM user_read_record r " +
                            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
                            "WHERE r.id=?",
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date") != null ? rs.getDate("read_date").toLocalDate() : null);
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常，Id：{}", id, e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }, id
            );
        } catch (SQLException e) {
            logger.error("查询阅读记录异常，Id：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadRecord> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT r.*, b.Bookname, b.PictureName " +
                            "FROM user_read_record r " +
                            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
                            "WHERE r.user_id=? ORDER BY r.create_time DESC",
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date") != null ? rs.getDate("read_date").toLocalDate() : null);
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常，UserId：{}", userId, e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }, userId
            );
        } catch (SQLException e) {
            logger.error("查询阅读记录异常，UserId：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadRecord> queryByIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT r.*, b.Bookname, b.PictureName " +
                            "FROM user_read_record r " +
                            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
                            "WHERE r.isbn=?",
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date") != null ? rs.getDate("read_date").toLocalDate() : null);
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常，Isbn：{}", isbn, e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }, isbn
            );
        } catch (SQLException e) {
            logger.error("查询阅读记录异常，Isbn：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadRecord> queryByUserIdAndIsbn(String userId, String isbn) {
        if (userId == null || userId.isEmpty() || isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("UserId和ISBN不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT r.*, b.Bookname, b.PictureName " +
                            "FROM user_read_record r " +
                            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
                            "WHERE r.user_id=? AND r.isbn=? ORDER BY r.read_date DESC",
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date") != null ? rs.getDate("read_date").toLocalDate() : null);
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常，UserId：{}，ISBN：{}", userId, isbn, e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }, userId, isbn
            );
        } catch (SQLException e) {
            logger.error("查询阅读记录异常，UserId：{}，ISBN：{}", userId, isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserReadRecord> queryByUserIdIsbnAndDate(String userId, String isbn, LocalDate readDate) {
        if (userId == null || userId.isEmpty() || isbn == null || isbn.isEmpty() || readDate == null) {
            throw new IllegalArgumentException("UserId、ISBN和阅读日期不能为空");
        }
        try {
            return DBUtil.executeQuery("""
                            SELECT r.*, b.Bookname, b.PictureName 
                            FROM user_read_record r 
                            LEFT JOIN book_information b ON r.isbn = b.ISBN 
                            WHERE r.user_id=? AND r.isbn=? AND r.read_date=?
                            """,
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setId(rs.getInt("id"));
                            record.setUserId(rs.getString("user_id"));
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDate(rs.getDate("read_date").toLocalDate());
                            record.setReadDuration(rs.getInt("read_duration"));
                            record.setCreateTime(rs.getTimestamp("create_time") != null
                                    ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析阅读记录数据异常", e);
                            throw new RuntimeException("解析阅读记录数据异常", e);
                        }
                        return record;
                    }, userId, isbn, readDate);
        } catch (SQLException e) {
            logger.error("查询阅读记录异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Integer sumReadDurationByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COALESCE(SUM(read_duration), 0) AS total_duration FROM user_read_record WHERE user_id=? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total_duration"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户阅读时长异常", e);
            return 0;
        }
    }

    @Override
    public List<UserReadRecord> listBookReadDurationByTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId不能为空");
        }
        try {
            LocalDate startDate = startTime.toLocalDate();
            LocalDate endDate = endTime.toLocalDate();

            return DBUtil.executeQuery("""
                            SELECT 
                                r.isbn, 
                                SUM(r.read_duration) AS total_duration,
                                MAX(r.create_time) AS last_read_time,
                                b.Bookname,
                                b.PictureName
                            FROM user_read_record r
                            LEFT JOIN book_information b ON r.isbn = b.ISBN
                            WHERE r.user_id=? AND r.read_date BETWEEN ? AND ? 
                            GROUP BY r.isbn, b.Bookname, b.PictureName
                            ORDER BY total_duration DESC
                            """,
                    rs -> {
                        UserReadRecord record = new UserReadRecord();
                        try {
                            record.setUserId(userId);
                            record.setIsbn(rs.getString("isbn"));
                            record.setReadDuration(rs.getInt("total_duration"));
                            record.setCreateTime(rs.getTimestamp("last_read_time") != null ? rs.getTimestamp("last_read_time").toLocalDateTime() : null);
                            // 赋值书籍信息
                            record.setBookName(rs.getString("Bookname"));
                            record.setPictureName(rs.getString("PictureName"));
                        } catch (SQLException e) {
                            logger.error("解析书籍阅读时长数据异常", e);
                            throw new RuntimeException("解析书籍阅读时长数据异常", e);
                        }
                        return record;
                    }, userId, startDate, endDate);

        } catch (SQLException e) {
            logger.error("统计时间段内书籍阅读时长异常，UserId：{}", userId, e);
            return Collections.emptyList();
        }
    }
}