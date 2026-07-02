package com.example.dao.impl;

import com.example.dao.UserAchievementDao;
import com.example.entity.UserAchievement;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class UserAchievementDaoImpl implements UserAchievementDao {

    private static final Logger logger = LoggerFactory.getLogger(UserAchievementDaoImpl.class);

    @Override
    public void add(UserAchievement ua) {
        if (ua == null || ua.getUserId() == null || ua.getAchievementId() == null) {
            throw new IllegalArgumentException("用户ID或成就ID不能为空");
        }
        try {
            String sql = "INSERT INTO user_achievement(user_id, achievement_id, unlocked_at) VALUES (?, ?, ?)";
            DBUtil.executeUpdate(sql, ua.getUserId(), ua.getAchievementId(),
                    ua.getUnlockedAt() != null ? ua.getUnlockedAt() : LocalDateTime.now());
        } catch (SQLException e) {
            logger.error("插入用户成就异常 userId={} achievementId={}", ua.getUserId(), ua.getAchievementId(), e);
            throw new RuntimeException("插入用户成就异常", e);
        }
    }

    @Override
    public List<UserAchievement> queryByUserId(String userId) {
        if (userId == null) return Collections.emptyList();
        try {
            String sql = "SELECT id, user_id, achievement_id, unlocked_at FROM user_achievement WHERE user_id=? ORDER BY unlocked_at DESC";
            return DBUtil.executeQuery(sql, this::mapRow, userId);
        } catch (SQLException e) {
            logger.error("查询用户成就异常 userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean exists(String userId, String achievementId) {
        if (userId == null || achievementId == null) return false;
        try {
            String sql = "SELECT COUNT(*) FROM user_achievement WHERE user_id=? AND achievement_id=?";
            Integer count = DBUtil.executeQueryScalar(sql, Integer.class, userId, achievementId);
            return count != null && count > 0;
        } catch (SQLException e) {
            logger.error("查询成就是否存在异常 userId={} achievementId={}", userId, achievementId, e);
            return false;
        }
    }

    @Override
    public int countByUserId(String userId) {
        if (userId == null) return 0;
        try {
            String sql = "SELECT COUNT(*) FROM user_achievement WHERE user_id=?";
            Integer count = DBUtil.executeQueryScalar(sql, Integer.class, userId);
            return count != null ? count : 0;
        } catch (SQLException e) {
            logger.error("统计用户成就数异常 userId={}", userId, e);
            return 0;
        }
    }

    private UserAchievement mapRow(java.sql.ResultSet rs) throws SQLException {
        UserAchievement ua = new UserAchievement();
        ua.setId(rs.getInt("id"));
        ua.setUserId(rs.getString("user_id"));
        ua.setAchievementId(rs.getString("achievement_id"));
        ua.setUnlockedAt(rs.getTimestamp("unlocked_at").toLocalDateTime());
        return ua;
    }
}
