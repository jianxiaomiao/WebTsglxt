package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserReadDaily {
    private Integer id;
    private String userId;
    private LocalDate readDate;
    private Integer readDuration; // 单位：秒 ✅
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 构造器
    public UserReadDaily() {}
    public UserReadDaily(String userId, LocalDate readDate, Integer readDuration) {
        this.userId = userId;
        this.readDate = readDate;
        this.readDuration = readDuration;
    }

    // Getter & Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getReadDate() { return readDate; }
    public void setReadDate(LocalDate readDate) { this.readDate = readDate; }
    public Integer getReadDuration() { return readDuration; }
    public void setReadDuration(Integer readDuration) { this.readDuration = readDuration; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "UserReadDaily{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", readDate=" + readDate +
                ", readDuration=" + readDuration +
                '}';
    }
}