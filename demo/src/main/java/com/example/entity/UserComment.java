package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

public class UserComment {
    private String Userid;
    private String UserComment;
    private LocalDateTime CommentTime;
    private Integer CommentId;
    private Integer prefer;
    private Integer parentId;
    private List<ForumImage> images;
    private Integer replyToCommentId;
    private String replyToUserId;
    private String replyToUserName;
    // ====================== 新增：评论发布者的用户名（核心！） ======================
    private String userName;

    // 构造方法（不变）
    public UserComment(String Userid, String UserComment, LocalDateTime CommentTime, Integer CommentId) {
        this.CommentId = CommentId;
        this.CommentTime = CommentTime;
        this.UserComment = UserComment;
        this.Userid = Userid;
        this.prefer = 0;
        this.parentId = 0;
    }

    public UserComment() {}

    // ====================== 新增：用户名 Getter/Setter ======================
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // ====================== 原有 Getter/Setter（完全不变） ======================
    public Integer getReplyToCommentId() { return replyToCommentId; }
    public void setReplyToCommentId(Integer replyToCommentId) { this.replyToCommentId = replyToCommentId; }
    public String getReplyToUserId() { return replyToUserId; }
    public void setReplyToUserId(String replyToUserId) { this.replyToUserId = replyToUserId; }
    public String getReplyToUserName() { return replyToUserName; }
    public void setReplyToUserName(String replyToUserName) { this.replyToUserName = replyToUserName; }
    public String getUserid() { return Userid; }
    public void setUserid(String userid) { Userid = userid; }
    public String getUserComment() { return UserComment; }
    public void setUserComment(String userComment) { UserComment = userComment; }
    public LocalDateTime getCommentTime() { return CommentTime; }
    public void setCommentTime(LocalDateTime commentTime) { CommentTime = commentTime; }
    public Integer getCommentId() { return CommentId; }
    public void setCommentId(Integer commentId) { CommentId = commentId; }
    public Integer getPrefer() { return prefer; }
    public void setPrefer(Integer prefer) { this.prefer = prefer; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public List<ForumImage> getImages() { return images; }
    public void setImages(List<ForumImage> images) { this.images = images; }

    @Override
    public String toString() {
        return "UserComment [Userid=" + Userid + ", UserComment=" + UserComment + ", CommentTime=" + CommentTime
                + ", CommentId=" + CommentId + ", prefer=" + prefer + ", parentId=" + parentId + "]";
    }
}