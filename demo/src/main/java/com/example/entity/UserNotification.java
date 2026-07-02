package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

public class UserNotification {
    private Integer id;
    private String userId;
    private Integer type;
    private String fromUserId;
    private Integer commentId;
    private Integer replyCommentId;
    private Integer isRead;
    private LocalDateTime createTime;

    // ====================== 新增核心字段 ======================
    private String fromUserName;  // 发送者用户名
    private String content;       // 评论内容
    private List<ForumImage> images; // 评论图片

    public UserNotification() {
    }

    public UserNotification(Integer id, String userId, Integer type, String fromUserId, Integer commentId, Integer replyCommentId, Integer isRead, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.fromUserId = fromUserId;
        this.commentId = commentId;
        this.replyCommentId = replyCommentId;
        this.isRead = isRead;
        this.createTime = createTime;
    }

    // ====================== 原有Getter/Setter ======================
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }
    public Integer getReplyCommentId() { return replyCommentId; }
    public void setReplyCommentId(Integer replyCommentId) { this.replyCommentId = replyCommentId; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // ====================== 新增Getter/Setter ======================
    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<ForumImage> getImages() { return images; }
    public void setImages(List<ForumImage> images) { this.images = images; }
}