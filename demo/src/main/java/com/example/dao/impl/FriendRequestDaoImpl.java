package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.FriendRequestDao;
import com.example.entity.FriendRequest;
import com.example.util.DBUtil;

public class FriendRequestDaoImpl implements FriendRequestDao {
    private static final Logger logger = LoggerFactory.getLogger(FriendRequestDaoImpl.class);

    @Override
    public Integer add(FriendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("好友申请不能为空");
        }
        try {
            return DBUtil.executeUpdateReturnId(
                    "insert into friend_request(from_user_id, to_user_id, request_msg, status, create_time) values (?, ?, ?, ?, ?)",
                    request.getFromUserId(),
                    request.getToUserId(),
                    request.getRequestMsg(),
                    request.getStatus(),
                    request.getCreateTime()
            );
        } catch (SQLException e) {
            logger.error("新增好友申请失败，发起者：{}，接收者：{}", request.getFromUserId(), request.getToUserId(), e);
            throw new RuntimeException("新增好友申请异常", e);
        }
    }

    @Override
    public void updateStatus(Integer id, Integer status, LocalDateTime handleTime) {
        if (id == null) {
            throw new IllegalArgumentException("申请ID不能为空");
        }
        try {
            DBUtil.executeUpdate(
                    "update friend_request set status = ?, handle_time = ? where id = ?",
                    status, handleTime, id
            );
            logger.info("更新好友申请状态成功，申请ID：{}，状态：{}", id, status);
        } catch (SQLException e) {
            logger.error("更新好友申请状态失败，申请ID：{}", id, e);
            throw new RuntimeException("更新好友申请状态异常", e);
        }
    }

    // ====================== 查询收到的申请：联表查询【发起者】用户名 ======================
    @Override
    public List<FriendRequest> queryReceivedRequests(String toUserId) {
        if (toUserId == null || toUserId.isEmpty()) {
            throw new IllegalArgumentException("接收者ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT fr.*, u.Name as userName " +
                            "FROM friend_request fr " +
                            "LEFT JOIN user_information u ON fr.from_user_id = u.UserId " +
                            "WHERE fr.to_user_id = ? order by fr.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        String msg = rs.getString("request_msg");
                        Integer status = rs.getInt("status");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        LocalDateTime handleTime = rs.getTimestamp("handle_time") != null
                                ? rs.getTimestamp("handle_time").toLocalDateTime() : null;
                        // 获取用户名
                        String userName = rs.getString("userName");

                        FriendRequest request = new FriendRequest(fromUid, toUid, msg, createTime);
                        request.setId(id);
                        request.setStatus(status);
                        request.setHandleTime(handleTime);
                        request.setUserName(userName); // 赋值用户名
                        return request;
                    }, toUserId);
        } catch (SQLException e) {
            logger.error("查询收到的好友申请异常，接收者：{}", toUserId, e);
            return Collections.emptyList();
        }
    }

    // ====================== 查询发出的申请：联表查询【接收者】用户名 ======================
    @Override
    public List<FriendRequest> querySentRequests(String fromUserId) {
        if (fromUserId == null || fromUserId.isEmpty()) {
            throw new IllegalArgumentException("发起者ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT fr.*, u.Name as userName " +
                            "FROM friend_request fr " +
                            "LEFT JOIN user_information u ON fr.to_user_id = u.UserId " +
                            "WHERE fr.from_user_id = ? order by fr.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        String msg = rs.getString("request_msg");
                        Integer status = rs.getInt("status");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        LocalDateTime handleTime = rs.getTimestamp("handle_time") != null
                                ? rs.getTimestamp("handle_time").toLocalDateTime() : null;
                        // 获取用户名
                        String userName = rs.getString("userName");

                        FriendRequest request = new FriendRequest(fromUid, toUid, msg, createTime);
                        request.setId(id);
                        request.setStatus(status);
                        request.setHandleTime(handleTime);
                        request.setUserName(userName); // 赋值用户名
                        return request;
                    }, fromUserId);
        } catch (SQLException e) {
            logger.error("查询发出的好友申请异常，发起者：{}", fromUserId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasPendingRequest(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null) {
            return false;
        }
        try {
            List<FriendRequest> list = DBUtil.executeQuery(
                    "select id from friend_request where from_user_id = ? and to_user_id = ? and status = 0 limit 1",
                    rs -> {
                        FriendRequest request = new FriendRequest();
                        request.setId(rs.getInt("id"));
                        return request;
                    }, fromUserId, toUserId);
            return !list.isEmpty();
        } catch (SQLException e) {
            logger.error("查询待处理申请异常", e);
            return false;
        }
    }

    // ====================== 根据ID查询：联表查询发起者用户名 ======================
    @Override
    public FriendRequest selectById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("申请ID不能为空");
        }
        try {
            List<FriendRequest> list = DBUtil.executeQuery(
                    "SELECT fr.*, u.Name as userName " +
                            "FROM friend_request fr " +
                            "LEFT JOIN user_information u ON fr.from_user_id = u.UserId " +
                            "WHERE fr.id = ?",
                    rs -> {
                        FriendRequest request = new FriendRequest();
                        request.setId(rs.getInt("id"));
                        request.setFromUserId(rs.getString("from_user_id"));
                        request.setToUserId(rs.getString("to_user_id"));
                        request.setRequestMsg(rs.getString("request_msg"));
                        request.setStatus(rs.getInt("status"));
                        request.setCreateTime(rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                        request.setHandleTime(rs.getTimestamp("handle_time") != null
                                ? rs.getTimestamp("handle_time").toLocalDateTime() : null);
                        request.setUserName(rs.getString("userName")); // 赋值用户名
                        return request;
                    }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询好友申请异常，ID：{}", id, e);
            return null;
        }
    }

    @Override
    public void batchUpdatePendingStatus(String uid1, String uid2, Integer status, LocalDateTime handleTime) {
        try {
            DBUtil.executeUpdate(
                    "UPDATE friend_request SET status = ?, handle_time = ? " +
                            "WHERE status = 0 AND ((from_user_id = ? AND to_user_id = ?) OR (from_user_id = ? AND to_user_id = ?))",
                    status, handleTime, uid1, uid2, uid2, uid1
            );
        } catch (SQLException e) {
            logger.error("批量更新好友申请状态异常", e);
            throw new RuntimeException("批量更新申请状态异常", e);
        }
    }

    // ====================== 时间段查询：发出的申请（接收者用户名） ======================
    @Override
    public List<FriendRequest> querySentRequestsByTimeRange(String fromUserId, LocalDateTime startTime, LocalDateTime endTime) {
        if (fromUserId == null || fromUserId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT fr.*, u.Name as userName " +
                            "FROM friend_request fr " +
                            "LEFT JOIN user_information u ON fr.to_user_id = u.UserId " +
                            "WHERE fr.from_user_id = ? and fr.create_time BETWEEN ? AND ? order by fr.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        String msg = rs.getString("request_msg");
                        Integer status = rs.getInt("status");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        LocalDateTime handleTime = rs.getTimestamp("handle_time") != null
                                ? rs.getTimestamp("handle_time").toLocalDateTime() : null;
                        String userName = rs.getString("userName");

                        FriendRequest request = new FriendRequest(fromUid, toUid, msg, createTime);
                        request.setId(id);
                        request.setStatus(status);
                        request.setHandleTime(handleTime);
                        request.setUserName(userName);
                        return request;
                    }, fromUserId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询指定时间段发出的好友申请异常，发起者：{}", fromUserId, e);
            return Collections.emptyList();
        }
    }

    // ====================== 时间段查询：收到的申请（发起者用户名） ======================
    @Override
    public List<FriendRequest> queryReceivedRequestsByTimeRange(String toUserId, LocalDateTime startTime, LocalDateTime endTime) {
        if (toUserId == null || toUserId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT fr.*, u.Name as userName " +
                            "FROM friend_request fr " +
                            "LEFT JOIN user_information u ON fr.from_user_id = u.UserId " +
                            "WHERE fr.to_user_id = ? and fr.create_time BETWEEN ? AND ? order by fr.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String fromUid = rs.getString("from_user_id");
                        String toUid = rs.getString("to_user_id");
                        String msg = rs.getString("request_msg");
                        Integer status = rs.getInt("status");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        LocalDateTime handleTime = rs.getTimestamp("handle_time") != null
                                ? rs.getTimestamp("handle_time").toLocalDateTime() : null;
                        String userName = rs.getString("userName");

                        FriendRequest request = new FriendRequest(fromUid, toUid, msg, createTime);
                        request.setId(id);
                        request.setStatus(status);
                        request.setHandleTime(handleTime);
                        request.setUserName(userName);
                        return request;
                    }, toUserId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询指定时间段收到的好友申请异常，接收者：{}", toUserId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Integer countSentRequestsByTimeRange(String fromUserId, LocalDateTime startTime, LocalDateTime endTime) {
        if (fromUserId == null || fromUserId.isEmpty() || startTime == null || endTime == null) {
            return 0;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM friend_request WHERE from_user_id = ? AND create_time BETWEEN ? AND ?";
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), fromUserId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计指定时间段发送的好友申请数量异常，发起者：{}", fromUserId, e);
            return 0;
        }
    }

    @Override
    public Integer countReceivedRequestsByTimeRange(String toUserId, LocalDateTime startTime, LocalDateTime endTime) {
        if (toUserId == null || toUserId.isEmpty() || startTime == null || endTime == null) {
            return 0;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM friend_request WHERE to_user_id = ? AND create_time BETWEEN ? AND ?";
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), toUserId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计指定时间段收到的好友申请数量异常，接收者：{}", toUserId, e);
            return 0;
        }
    }
}