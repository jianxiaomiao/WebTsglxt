package com.example.entity;

import java.time.LocalDateTime;

public class ForumImage {
    private Integer id;
    private Integer commentId;
    private String imageUrl;
    private String imageType;
    private LocalDateTime uploadTime;

    // 构造方法
    public ForumImage() {}
    public ForumImage(Integer commentId, String imageUrl, String imageType) {
        this.commentId = commentId;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
    }

    // Getter & Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
}