package com.example.entity;

import java.time.LocalDateTime;

/**
 * 书荒广场帖子实体类
 * 对应表：book_square_post
 */
public class BookSquarePost {
    // 数据库原生字段
    private Integer id;
    private Integer parentId;
    private String userId;
    private String content;
    private Integer type;
    private Integer status;
    private LocalDateTime createTime;

    // 扩展字段（前端展示，不存入数据库，不加入构造函数）
    private String userName;
    private Integer subPostCount;

    // 无参构造
    public BookSquarePost() {
    }

    // 有参构造：仅包含数据库原生字段（扩展字段不加入）
    public BookSquarePost(Integer id, Integer parentId, String userId, String content,
                          Integer type, Integer status, LocalDateTime createTime) {
        this.id = id;
        this.parentId = parentId;
        this.userId = userId;
        this.content = content;
        this.type = type;
        this.status = status;
        this.createTime = createTime;
    }

    // ====================== Getter & Setter 原生字段 ======================
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    // ====================== Getter & Setter 扩展字段 ======================
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getSubPostCount() {
        return subPostCount;
    }

    public void setSubPostCount(Integer subPostCount) {
        this.subPostCount = subPostCount;
    }

    // 重写toString
    @Override
    public String toString() {
        return "BookSquarePost{" +
                "id=" + id +
                ", parentId=" + parentId +
                ", userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createTime=" + createTime +
                ", userName='" + userName + '\'' +
                ", subPostCount=" + subPostCount +
                '}';
    }
}