package com.example.dao;

import com.example.entity.UserNotification;
import java.util.List;

public interface UserNotificationDao {
    // 新增通知
    void add(UserNotification notification);
    // 查询用户的所有通知
    List<UserNotification> queryByUserId(String userId);
    // 标记通知为已读
    void markRead(Integer notificationId);
    void batchMarkReadByUserId(String userId);
}