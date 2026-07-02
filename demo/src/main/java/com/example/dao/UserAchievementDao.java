package com.example.dao;

import com.example.entity.UserAchievement;
import java.util.List;

public interface UserAchievementDao {
    /** 插入解锁记录 */
    void add(UserAchievement ua);
    /** 查询某用户已解锁的所有成就 */
    List<UserAchievement> queryByUserId(String userId);
    /** 查询某用户是否已解锁某成就 */
    boolean exists(String userId, String achievementId);
    /** 统计某用户已解锁成就数 */
    int countByUserId(String userId);
}
