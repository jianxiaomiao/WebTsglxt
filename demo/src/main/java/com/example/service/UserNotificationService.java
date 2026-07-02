package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserNotification;

import java.util.List;

public interface UserNotificationService {
    // 查询用户的所有通知（按时间倒序）
    ResultDTO<List<UserNotification>> getNotificationsByUserId(String userId);

    // 标记通知为已读
    ResultDTO<Void> markNotificationRead(Integer notificationId);

    // 新增通知（供评论/点赞调用）
    void addNotification(String toUserId, Integer type, String fromUserId,
                         Integer commentId, Integer replyCommentId);
    // 🔥 新增：批量标记当前用户所有通知为已读
    ResultDTO<Void> batchMarkAllRead(String userId);
}