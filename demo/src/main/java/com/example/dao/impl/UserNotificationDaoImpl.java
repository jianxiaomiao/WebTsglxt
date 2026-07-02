package com.example.dao.impl;

import com.example.dao.UserNotificationDao;
import com.example.entity.ForumImage;
import com.example.entity.UserNotification;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class UserNotificationDaoImpl implements UserNotificationDao {
    private static final Logger logger = LoggerFactory.getLogger(UserNotificationDaoImpl.class);

    // 注入图片DAO（确保你的项目已注入/实例化）
    private final ForumImageDaoImpl forumImageDAO = new ForumImageDaoImpl();

    @Override
    public void add(UserNotification notification) {
        try {
            DBUtil.executeUpdate(
                    "INSERT INTO user_notification(user_id, type, from_user_id, comment_id, reply_comment_id, create_time) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    notification.getUserId(),
                    notification.getType(),
                    notification.getFromUserId(),
                    notification.getCommentId(),
                    notification.getReplyCommentId(),
                    notification.getCreateTime()
            );
        } catch (SQLException e) {
            logger.error("新增系统通知失败", e);
            throw new RuntimeException("新增通知异常", e);
        }
    }

    // ====================== 核心优化：联表查询所有数据 ======================
    @Override
    public List<UserNotification> queryByUserId(String userId) {
        try {
            // 多表联查：通知表 + 用户表(发送者) + 评论表(内容)
            List<UserNotification> notifications = DBUtil.executeQuery(
                    "SELECT n.*, u.Name AS fromUserName, c.UserComment AS content " +
                            "FROM user_notification n " +
                            "LEFT JOIN user_information u ON n.from_user_id = u.UserId " +
                            "LEFT JOIN user_comment c ON n.comment_id = c.CommentId " +
                            "WHERE n.user_id=? ORDER BY n.create_time DESC",
                    rs -> {
                        UserNotification n = new UserNotification();
                        n.setId(rs.getInt("id"));
                        n.setUserId(rs.getString("user_id"));
                        n.setType(rs.getInt("type"));
                        n.setFromUserId(rs.getString("from_user_id"));
                        n.setCommentId(rs.getInt("comment_id"));
                        n.setReplyCommentId(rs.getInt("reply_comment_id"));
                        n.setIsRead(rs.getInt("is_read"));

                        // 时间处理
                        java.sql.Timestamp createTimeStamp = rs.getTimestamp("create_time");
                        n.setCreateTime(createTimeStamp != null ? createTimeStamp.toLocalDateTime() : LocalDateTime.now());

                        // 赋值联表查询的字段
                        n.setFromUserName(rs.getString("fromUserName"));
                        n.setContent(rs.getString("content"));
                        return n;
                    }, userId);

            // 组装每个通知对应的评论图片
            for (UserNotification notification : notifications) {
                Integer commentId = notification.getCommentId();
                List<ForumImage> images = forumImageDAO.queryByCommentId(commentId);
                notification.setImages(images != null ? images : Collections.emptyList());
            }

            return notifications;
        } catch (SQLException e) {
            logger.error("查询用户通知失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void markRead(Integer notificationId) {
        try {
            DBUtil.executeUpdate(
                    "UPDATE user_notification SET is_read=1 WHERE id=?",
                    notificationId
            );
        } catch (SQLException e) {
            logger.error("标记通知已读失败", e);
            throw new RuntimeException("标记通知异常", e);
        }
    }

    @Override
    public void batchMarkReadByUserId(String userId) {
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "UPDATE user_notification SET is_read = 1 WHERE user_id = ? AND is_read = 0",
                    userId
            );
            logger.info("批量标记通知已读：用户ID={}，影响行数={}", userId, affectedRows);
        } catch (SQLException e) {
            logger.error("批量标记通知已读失败", e);
            throw new RuntimeException("批量标记通知异常", e);
        }
    }
}