package com.example.dao.impl;

import com.example.dao.UserReportDao;
import com.example.entity.UserReport;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserReportDaoImpl implements UserReportDao {

    private static final Logger logger = LoggerFactory.getLogger(UserReportDaoImpl.class);

    @Override
    public List<UserReport> selectByUserAndTypeAndDate(String userId, String type, String date) {
        try {
            return DBUtil.executeQuery(
                    "SELECT id, user_id, type, content, create_time, date FROM user_stats WHERE user_id=? AND type=? AND date=?",
                    rs -> {
                        UserReport stats = new UserReport();
                        stats.setId(rs.getInt("id"));
                        stats.setUserId(rs.getString("user_id"));
                        stats.setType(rs.getString("type"));
                        stats.setContent(rs.getString("content"));
                        stats.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        stats.setDate(rs.getString("date"));
                        return stats;
                    },
                    userId, type, date
            );
        } catch (SQLException e) {
            logger.error("查询用户统计缓存异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void insert(UserReport stats) {
        try {
            DBUtil.executeUpdate(
                    "INSERT INTO user_stats(user_id, type, content, create_time, date) VALUES (?,?,?,?,?)",
                    stats.getUserId(),
                    stats.getType(),
                    stats.getContent(),
                    stats.getCreateTime(),
                    stats.getDate()
            );
        } catch (SQLException e) {
            logger.error("保存用户统计缓存异常", e);
            throw new RuntimeException("保存统计数据失败");
        }
    }
    // ==================== 新增：分页查询历史 ====================
    @Override
    public List<UserReport> selectReportHistory(String userId, String type, Integer pageNum, Integer pageSize) {
        // 1. 分页参数默认值处理（严格遵循你的规范）
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        // 2. 动态构建SQL：type不传则查询所有类型（DAY/WEEK/MONTH）
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, user_id, type, content, create_time, date FROM user_stats WHERE user_id=? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        // 类型筛选（可选）
        if (type != null && !type.trim().isEmpty()) {
            sql.append("AND type=? ");
            params.add(type);
        }

        // 排序：最新日期在前 + 分页
        sql.append("ORDER BY date DESC, create_time DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);
        try {
            // 3. 执行查询
            return DBUtil.executeQuery(
                    sql.toString(),
                    rs -> {
                        UserReport report = new UserReport();
                        report.setId(rs.getInt("id"));
                        report.setUserId(rs.getString("user_id"));
                        report.setType(rs.getString("type"));
                        report.setContent(rs.getString("content"));
                        report.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                        report.setDate(rs.getString("date"));
                        return report;
                    },
                    params.toArray()
            );
        }
        catch(SQLException e){
            logger.error("查询用户统计历史缓存异常", e);
            return Collections.emptyList();
        }
    }
}