package com.example.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserReport {
    private Integer id;
    private String userId;
    private String type;
    private String content;
    private LocalDateTime createTime;
    private String date;

    public UserReport() {}

    public UserReport(String userId, String type, String content, LocalDateTime createTime, String date) {
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.createTime = createTime;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}