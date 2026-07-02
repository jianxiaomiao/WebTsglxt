package com.example.dao.impl;

import com.example.dao.UserReadDailyDao;
import com.example.entity.UserReadDaily;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class UserReadDailyDaoImpl implements UserReadDailyDao {
    private static final Logger logger = LoggerFactory.getLogger(UserReadDailyDaoImpl.class);

    @Override
    public void upsertReadDuration(UserReadDaily readDaily) {
        try {
            // MySQL的ON DUPLICATE KEY UPDATE：存在则累加时长，不存在则新增
            String sql = "INSERT INTO user_read_daily(user_id, read_date, read_duration) " +
                    "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE read_duration = read_duration + ?";
            DBUtil.executeUpdate(sql,
                    readDaily.getUserId(), readDaily.getReadDate(), readDaily.getReadDuration(), readDaily.getReadDuration()
            );
        } catch (SQLException e) {
            logger.error("新增/更新阅读时长失败", e);
            throw new RuntimeException("新增/更新阅读时长异常", e);
        }
    }

    @Override
    public List<UserReadDaily> queryByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT * FROM user_read_daily WHERE user_id = ? AND read_date BETWEEN ? AND ? ORDER BY read_date";
            return DBUtil.executeQuery(sql, rs -> {
                UserReadDaily readDaily = new UserReadDaily();
                readDaily.setId(rs.getInt("id"));
                readDaily.setUserId(rs.getString("user_id"));
                readDaily.setReadDate(rs.getDate("read_date").toLocalDate());
                readDaily.setReadDuration(rs.getInt("read_duration"));
                readDaily.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                readDaily.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
                return readDaily;
            }, userId, startDate, endDate);
        } catch (SQLException e) {
            logger.error("查询用户时间段阅读记录失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<LocalDate> queryReadDatesByUserId(String userId) {
        try {
            String sql = "SELECT DISTINCT read_date FROM user_read_daily WHERE user_id = ? ORDER BY read_date";
            return DBUtil.executeQuery(sql, rs -> rs.getDate("read_date").toLocalDate(), userId);
        } catch (SQLException e) {
            logger.error("查询用户阅读日期列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Integer sumTotalDuration(String userId) {
        try {
            String sql = "SELECT IFNULL(SUM(read_duration), 0) FROM user_read_daily WHERE user_id = ?";
            // 用IFNULL强制返回0，避免NULL
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt(1), userId);
            // 安全处理空List
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户总阅读时长失败", e);
            return 0;
        }
    }

    // ====================== 每日阅读时长统计（求和） ======================
    public Integer sumReadSecondByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT SUM(read_duration) AS total FROM user_read_daily WHERE user_id = ? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() || result.get(0) == null ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户阅读时长异常", e);
            return 0;
        }
    }

    // ====================== 统计阅读天数 ======================
    public Integer countReadDaysByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(DISTINCT read_date) AS total FROM user_read_daily WHERE user_id = ? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户阅读天数异常", e);
            return 0;
        }
    }
}