package com.example.service.impl;

import com.example.dao.UserNotificationDao;
import com.example.dao.impl.UserNotificationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserNotification;
import com.example.service.UserNotificationService;

import java.time.LocalDateTime;
import java.util.List;

public class UserNotificationServiceImpl implements UserNotificationService {
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
    }
    @Override
    public ResultDTO<Void> batchMarkAllRead(String userId) {
        try {
            notificationDao.batchMarkReadByUserId(userId);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("批量标记已读失败：" + e.getMessage());
        }
    }
}