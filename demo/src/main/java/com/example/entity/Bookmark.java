package com.example.entity;

import java.time.LocalDateTime;

/**
 * 书籍书签实体类
 * 对应数据库表：user_bookmark（自增ID主键）
 */
public class Bookmark {
    // 🔥 自增主键 ID
    private Long id;
    // 用户ID
    private String userId;
    // 书籍ISBN
    private String isbn;
    // 章节号
    private String chapterNumber;
    // 阅读进度（整数）
    private Integer readProgress;
    // 创建时间
    private LocalDateTime createTime;

    // 无参构造
    public Bookmark() {
    }

    // 🔥 全参构造器（包含自增ID）
    public Bookmark(Long id, String userId, String isbn, String chapterNumber, Integer readProgress, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.isbn = isbn;
        this.chapterNumber = chapterNumber;
        this.readProgress = readProgress;
        this.createTime = createTime;
    }

    // 🔥 业务新增构造器（不含ID，ID自增）
    public Bookmark(String userId, String isbn, String chapterNumber, Integer readProgress, LocalDateTime createTime) {
        this.userId = userId;
        this.isbn = isbn;
        this.chapterNumber = chapterNumber;
        this.readProgress = readProgress;
        this.createTime = createTime;
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(String chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public Integer getReadProgress() {
        return readProgress;
    }

    public void setReadProgress(Integer readProgress) {
        this.readProgress = readProgress;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    // toString
    @Override
    public String toString() {
        return "Bookmark{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", chapterNumber='" + chapterNumber + '\'' +
                ", readProgress=" + readProgress +
                ", createTime=" + createTime +
                '}';
    }
}