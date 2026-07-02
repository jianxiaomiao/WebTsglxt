package com.example.entity;

import java.time.LocalDateTime;

/**
 * 段落评论表实体
 */
public class ParagraphComment {
    // 评论自增主键ID
    private Long id;
    // 关联段落ID
    private String paragraphId;
    // 评论人用户ID
    private String userId;
    // 评论内容
    private String content;
    // 评论创建时间
    private LocalDateTime createTime;

    public ParagraphComment() {
    }

    public ParagraphComment(Long id, String paragraphId, String userId, String content, LocalDateTime createTime) {
        this.id = id;
        this.paragraphId = paragraphId;
        this.userId = userId;
        this.content = content;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @Override
    public String toString() {
        return "ParagraphComment{" +
                "id=" + id +
                ", paragraphId='" + paragraphId + '\'' +
                ", userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}