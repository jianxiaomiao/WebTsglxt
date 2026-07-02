// com.example.dao.impl.AiOperationLogDaoImpl
package com.example.dao.impl;

import com.example.dao.AiOperationLogDao;
import com.example.entity.AiOperationLog;
import com.example.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AiOperationLogDaoImpl implements AiOperationLogDao {
    @Override
    public Integer insertLog(AiOperationLog log) {
        String sql = "INSERT INTO ai_operation_log(user_id, action_type, target_entity, action_data, execute_result, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getUserId());
            pstmt.setString(2, log.getActionType());
            pstmt.setString(3, log.getTargetEntity());
            pstmt.setString(4, log.getActionData());
            pstmt.setString(5, log.getExecuteResult());
            pstmt.setTimestamp(6, Timestamp.valueOf(log.getCreateTime()));
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<AiOperationLog> selectLogsByUser(String userId, int limit) {
        String sql = "SELECT * FROM ai_operation_log WHERE user_id = ? ORDER BY create_time DESC LIMIT ?";
        List<AiOperationLog> logs = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                AiOperationLog log = new AiOperationLog(
                        rs.getString("user_id"),
                        rs.getString("action_type"),
                        rs.getString("target_entity"),
                        rs.getString("action_data"),
                        rs.getString("execute_result")
                );
                log.setId(rs.getLong("id"));
                log.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}