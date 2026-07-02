package com.example.entity;

import java.time.LocalDateTime;

/**
 * 用户成就解锁记录表实体
 */
public class UserAchievement {
    private Integer id;
    private String userId;
    private String achievementId;
    private LocalDateTime unlockedAt;

    public UserAchievement() {}

    public UserAchievement(String userId, String achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.unlockedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    @Override
    public String toString() {
        return "UserAchievement [id=" + id + ", userId=" + userId + ", achievementId=" + achievementId + "]";
    }
}
