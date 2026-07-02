package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户阅读记录实体类
 * 对应表：user_read_record
 */
public class UserReadRecord {
    // 主键ID
    private Integer id;
    // 用户ID
    private String userId;
    // 书籍ISBN
    private String isbn;
    // 单次阅读时长（单位：秒）
    private Integer readDuration;
    // 记录创建时间（阅读时间）
    private LocalDateTime createTime;
    // 阅读日期
    private LocalDate readDate;
    // ====================== 新增：书籍名称、书籍封面 ======================
    private String bookName;
    private String pictureName;

    // 无参构造函数
    public UserReadRecord() {
    }

    // 全参构造（新增书籍字段）
    public UserReadRecord(Integer id, String userId, String isbn, Integer readDuration, LocalDateTime createTime, LocalDate readDate, String bookName, String pictureName) {
        this.id = id;
        this.userId = userId;
        this.isbn = isbn;
        this.readDuration = readDuration;
        this.createTime = createTime;
        this.readDate = readDate;
        this.bookName = bookName;
        this.pictureName = pictureName;
    }

    // ====================== 新增 Getter/Setter ======================
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getPictureName() {
        return pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    // ====================== 原有 Getter/Setter（不变） ======================
    public LocalDate getReadDate() {
        return readDate;
    }

    public void setReadDate(LocalDate readDate) {
        this.readDate = readDate;
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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getReadDuration() {
        return readDuration;
    }

    public void setReadDuration(Integer readDuration) {
        this.readDuration = readDuration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}