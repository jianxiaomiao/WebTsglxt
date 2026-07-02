package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.FriendDao;
import com.example.entity.Friend;
import com.example.util.DBUtil;

public class FriendDaoImpl implements FriendDao {
    private static final Logger logger = LoggerFactory.getLogger(FriendDaoImpl.class);

    @Override
    public Integer add(Friend friend) {
        if (friend == null) {
            throw new IllegalArgumentException("好友关系不能为空");
        }
        try {
            return DBUtil.executeUpdateReturnId(
                    "insert into friend(user_id, friend_id, friend_remark, create_time) values (?, ?, ?, ?)",
                    friend.getUserId(),
                    friend.getFriendId(),
                    friend.getFriendRemark(),
                    friend.getCreateTime()
            );
        } catch (SQLException e) {
            logger.error("新增好友关系失败，用户ID：{}，好友ID：{}", friend.getUserId(), friend.getFriendId(), e);
            throw new RuntimeException("新增好友关系异常", e);
        }
    }

    @Override
    public void delete(String userId, String friendId) {
        if (userId == null || friendId == null) {
            throw new IllegalArgumentException("用户ID和好友ID不能为空");
        }
        try {
            // 删除双向关系
            DBUtil.executeUpdate(
                    "delete from friend where (user_id = ? and friend_id = ?) or (user_id = ? and friend_id = ?)",
                    userId, friendId, friendId, userId
            );
            logger.info("删除好友关系成功，用户ID：{}，好友ID：{}", userId, friendId);
        } catch (SQLException e) {
            logger.error("删除好友关系失败，用户ID：{}，好友ID：{}", userId, friendId, e);
            throw new RuntimeException("删除好友关系异常", e);
        }
    }

    // ====================== 联表查询：好友关系+用户名 ======================
    @Override
    public List<Friend> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("查询用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT f.*, u.Name as userName " +
                            "FROM friend f " +
                            "LEFT JOIN user_information u ON f.friend_id = u.UserId " +
                            "WHERE f.user_id = ? order by f.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String uid = rs.getString("user_id");
                        String fid = rs.getString("friend_id");
                        String remark = rs.getString("friend_remark");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        // 读取用户名
                        String userName = rs.getString("userName");

                        Friend friend = new Friend(uid, fid, createTime);
                        friend.setId(id);
                        friend.setFriendRemark(remark);
                        friend.setUserName(userName); // 赋值用户名
                        return friend;
                    }, userId);
        } catch (SQLException e) {
            logger.error("查询用户好友列表异常，用户ID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isFriend(String userId, String friendId) {
        if (userId == null || friendId == null) {
            return false;
        }
        try {
            List<Friend> list = DBUtil.executeQuery(
                    "select id from friend where user_id = ? and friend_id = ? limit 1",
                    rs -> {
                        Friend friend = new Friend();
                        friend.setId(rs.getInt("id"));
                        return friend;
                    }, userId, friendId);
            return !list.isEmpty();
        } catch (SQLException e) {
            logger.error("查询好友关系异常", e);
            return false;
        }
    }

    @Override
    public void updateRemark(String userId, String friendId, String remark) {
        if (userId == null || friendId == null) {
            throw new IllegalArgumentException("用户ID和好友ID不能为空");
        }
        try {
            DBUtil.executeUpdate(
                    "update friend set friend_remark = ? where user_id = ? and friend_id = ?",
                    remark, userId, friendId
            );
            logger.info("更新好友备注成功，用户ID：{}，好友ID：{}", userId, friendId);
        } catch (SQLException e) {
            logger.error("更新好友备注失败", e);
            throw new RuntimeException("更新好友备注异常", e);
        }
    }

    /**
     * 根据用户ID + 时间段 查询好友列表
     */
    @Override
    public List<Friend> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT f.*, u.Name as userName " +
                            "FROM friend f " +
                            "LEFT JOIN user_information u ON f.friend_id = u.UserId " +
                            "WHERE f.user_id = ? and f.create_time BETWEEN ? AND ? " +
                            "order by f.create_time desc",
                    rs -> {
                        Integer id = rs.getInt("id");
                        String uid = rs.getString("user_id");
                        String fid = rs.getString("friend_id");
                        String remark = rs.getString("friend_remark");
                        LocalDateTime createTime = rs.getTimestamp("create_time") != null
                                ? rs.getTimestamp("create_time").toLocalDateTime() : null;
                        // 读取用户名
                        String userName = rs.getString("userName");

                        Friend friend = new Friend(uid, fid, createTime);
                        friend.setId(id);
                        friend.setFriendRemark(remark);
                        friend.setUserName(userName); // 赋值用户名
                        return friend;
                    }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段好友列表异常", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 统计用户指定时间段内添加的好友数量
     */
    @Override
    public Integer countFriendByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            return 0;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM friend WHERE user_id = ? AND create_time BETWEEN ? AND ?";
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), userId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户{}指定时间段好友数量异常", userId, e);
            return 0;
        }
    }
}