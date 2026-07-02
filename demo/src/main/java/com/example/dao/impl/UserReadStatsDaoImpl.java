package com.example.dao.impl;

import com.example.dao.UserReadStatsDao;
import com.example.entity.UserReadStats;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class UserReadStatsDaoImpl implements UserReadStatsDao {
    private static final Logger logger = LoggerFactory.getLogger(UserReadStatsDaoImpl.class);

    @Override
    public void upsertStats(UserReadStats stats) {
        try {
            String sql = "INSERT INTO user_read_stats(user_id, total_duration, max_consecutive_days, current_consecutive_days, year_read_days, last_calc_time) " +
                    "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                    "total_duration=?, max_consecutive_days=?, current_consecutive_days=?, year_read_days=?, last_calc_time=?";
            DBUtil.executeUpdate(sql,
                    stats.getUserId(), stats.getTotalDuration(), stats.getMaxConsecutiveDays(),
                    stats.getCurrentConsecutiveDays(), stats.getYearReadDays(), LocalDateTime.now(),
                    stats.getTotalDuration(), stats.getMaxConsecutiveDays(),
                    stats.getCurrentConsecutiveDays(), stats.getYearReadDays(), LocalDateTime.now()
            );
        } catch (SQLException e) {
            logger.error("更新阅读统计缓存失败", e);
        }
    }

    @Override
    public UserReadStats getStatsByUserId(String userId) {
        try {
            String sql = "SELECT * FROM user_read_stats WHERE user_id=?";
            return DBUtil.executeQuery(sql, rs -> {
                UserReadStats s = new UserReadStats();
                s.setUserId(rs.getString("user_id"));
                s.setTotalDuration(rs.getInt("total_duration"));
                s.setMaxConsecutiveDays(rs.getInt("max_consecutive_days"));
                s.setCurrentConsecutiveDays(rs.getInt("current_consecutive_days"));
                s.setYearReadDays(rs.getInt("year_read_days"));
                s.setLastCalcTime(rs.getTimestamp("last_calc_time").toLocalDateTime());
                return s;
            }, userId).stream().findFirst().orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }
}