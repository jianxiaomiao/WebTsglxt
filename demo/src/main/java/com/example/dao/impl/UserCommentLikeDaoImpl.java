package com.example.dao.impl;

import com.example.dao.UserCommentLikeDao;
import com.example.entity.UserCommentLike;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class UserCommentLikeDaoImpl implements UserCommentLikeDao {
    private static final Logger logger = LoggerFactory.getLogger(UserCommentLikeDaoImpl.class);

    @Override
    public List<Integer> selectLikedCommentIdsByUserId(String userId) {
        try {
            return DBUtil.executeQuery(
                    "SELECT comment_id FROM user_comment_like WHERE user_id = ?",
                    rs -> rs.getInt("comment_id"),
                    userId
            );
        } catch (SQLException e) {
            logger.error("查询用户点赞评论ID异常", e);
            return Collections.emptyList();
        }
    }
    @Override
    public boolean isLiked(String userId, Integer commentId) {
        try {
            Integer count = DBUtil.executeQueryScalar(
                    "SELECT COUNT(*) FROM user_comment_like WHERE user_id=? AND comment_id=?",
                    Integer.class, userId, commentId
            );
            return count != null && count > 0;
        } catch (SQLException e) {
            logger.error("判断点赞状态异常", e);
            return false;
        }
    }

    @Override
    public void addLike(UserCommentLike like) {
        try {
            DBUtil.executeUpdate(
                    "INSERT INTO user_comment_like(user_id, comment_id, create_time) VALUES (?,?,?)",
                    like.getUserId(), like.getCommentId(), like.getCreateTime()
            );
        } catch (SQLException e) {
            logger.error("新增点赞异常", e);
            throw new RuntimeException("点赞失败");
        }
    }

    @Override
    public void deleteLike(String userId, Integer commentId) {
        try {
            DBUtil.executeUpdate(
                    "DELETE FROM user_comment_like WHERE user_id=? AND comment_id=?",
                    userId, commentId
            );
        } catch (SQLException e) {
            logger.error("取消点赞异常", e);
            throw new RuntimeException("取消点赞失败");
        }
    }

    /**
     * 统计用户指定时间段内的评论点赞数量
     */
    @Override
    public Integer countCommentLikeByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            return 0;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_comment_like WHERE user_id=? AND create_time BETWEEN ? AND ?";
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), userId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户{}指定时间段评论点赞数量异常", userId, e);
            return 0;
        }
    }

    /**
     * 查询用户指定时间段内的评论点赞记录
     */
    @Override
    public List<UserCommentLike> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            return Collections.emptyList();
        }
        try {
            String sql = "SELECT user_id, comment_id, create_time FROM user_comment_like " +
                    "WHERE user_id=? AND create_time BETWEEN ? AND ? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, rs -> {
                UserCommentLike like = new UserCommentLike();
                like.setUserId(rs.getString("user_id"));
                like.setCommentId(rs.getInt("comment_id"));
                like.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                return like;
            }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段评论点赞记录异常", userId, e);
            return Collections.emptyList();
        }
    }
}