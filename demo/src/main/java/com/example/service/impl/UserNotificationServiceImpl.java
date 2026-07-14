package com.example.service.impl;

import com.example.dao.UserNotificationDao;
import com.example.dao.impl.UserNotificationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserNotification;
import com.example.service.UserNotificationService;
import com.example.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class UserNotificationServiceImpl implements UserNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(UserNotificationServiceImpl.class);
    private final UserNotificationDao notificationDao;

    public UserNotificationServiceImpl() {
        this.notificationDao = new UserNotificationDaoImpl();
    }

    @Override
    public ResultDTO<List<UserNotification>> getNotificationsByUserId(String userId) {
        try {
            List<UserNotification> list = notificationDao.queryByUserId(userId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("查询通知失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> markNotificationRead(Integer notificationId) {
        try {
            notificationDao.markRead(notificationId);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("标记已读失败：" + e.getMessage());
        }
    }

    @Override
    public void addNotification(String toUserId, Integer type, String fromUserId,
                                Integer commentId, Integer replyCommentId) {
        // 自己操作不发通知
        if (toUserId.equals(fromUserId)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(toUserId);
        notification.setType(type); // 1=回复 2=点赞
        notification.setFromUserId(fromUserId);
        notification.setCommentId(commentId);
        notification.setReplyCommentId(replyCommentId);
        // 🔥 关键修复：插入时主动设置当前时间
        notification.setCreateTime(LocalDateTime.now());
        notificationDao.add(notification);
        // 核心优化：如果自增成功，说明缓存已经初始化；如果缓存不存在（exists为false），则不急于操作，让下一次 fetchUnreadCount 从 DB 重新加载
        try {
            String hashKey = "user:unread:" + toUserId;
            if (RedisUtil.exists(hashKey)) {
                RedisUtil.hincrBy(hashKey, "notifications", 1);
            }
        } catch (Exception e) {
            logger.warn("Redis 自增未读通知数异常", e);
        }
    }
    @Override
    public ResultDTO<Void> batchMarkAllRead(String userId) {
        try {
            notificationDao.batchMarkReadByUserId(userId);
            try {
                String hashKey = "user:unread:" + userId;
                if (RedisUtil.exists(hashKey)) {
                    RedisUtil.hset(hashKey, "notifications", "0");
                }
            } catch (Exception e) {
                logger.warn("Redis 重置未读通知数异常", e);
            }
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("批量标记已读失败：" + e.getMessage());
        }
    }
}