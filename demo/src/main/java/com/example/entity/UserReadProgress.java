package com.example.entity;

import java.time.LocalDateTime;

public class UserReadProgress {

    private Integer id;
    private String userId;
    private String isbn;
    private String pageNum;
    private LocalDateTime lastReadTime;
    // 书籍扩展字段
    private String bookName;
    private String pictureName;
    private String author;
    // 新增：书籍类型ID、类型名称（三表联查 book_type 获取）
    private Integer type;
    private String typeName;

    // 无参构造函数
    public UserReadProgress() {
    }

    // 基础构造
    public UserReadProgress(String userId, String isbn, String pageNum, LocalDateTime lastReadTime) {
        this.userId = userId;
        this.isbn = isbn;
        this.pageNum = pageNum;
        this.lastReadTime = lastReadTime;
    }

    // Getter & Setter
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

    public String getPageNum() {
        return pageNum;
    }
    public void setPageNum(String pageNum) {
        this.pageNum = pageNum;
    }

    public LocalDateTime getLastReadTime() {
        return lastReadTime;
    }
    public void setLastReadTime(LocalDateTime lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

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

    // 作者
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    // ===== 新增 类型ID =====
    public Integer getType() {
        return type;
    }
    public void setType(Integer type) {
        this.type = type;
    }

    // ===== 新增 类型文字名称 =====
    public String getTypeName() {
        return typeName;
    }
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "UserReadProgress{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", pageNum='" + pageNum + '\'' +
                ", lastReadTime=" + lastReadTime +
                ", bookName='" + bookName + '\'' +
                ", pictureName='" + pictureName + '\'' +
                ", author='" + author + '\'' +
                ", type=" + type +
                ", typeName='" + typeName + '\'' +
                '}';
    }
}