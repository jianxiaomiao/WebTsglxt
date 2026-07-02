package com.example.entity;

import java.time.LocalDateTime;

public class UserCommentLike {
    private Integer id;
    private String userId;
    private Integer commentId;
    private LocalDateTime createTime;

    public UserCommentLike() {}
    public UserCommentLike(String userId, Integer commentId, LocalDateTime createTime) {
        this.userId = userId;
        this.commentId = commentId;
        this.createTime = createTime;
    }

    // Getter + Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}