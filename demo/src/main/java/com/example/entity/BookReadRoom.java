package com.example.entity;

import java.time.LocalDateTime;

public class BookReadRoom {
    // 数据库基础字段
    private Integer id;
    private LocalDateTime createTime;
    private Integer isPublic; // 1公开 0私密
    private String userId;
    private String isbn;

    // 联表扩展字段（查询时填充）
    private String bookName;
    private String userName;

    // 全参构造（数据库原生字段）
    public BookReadRoom(Integer id, LocalDateTime createTime, Integer isPublic, String userId, String isbn) {
        this.id = id;
        this.createTime = createTime;
        this.isPublic = isPublic;
        this.userId = userId;
        this.isbn = isbn;
    }

    // 无参构造（json反序列化必须）
    public BookReadRoom() {}

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer isPublic) {
        this.isPublic = isPublic;
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

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "BookReadRoom{" +
                "id=" + id +
                ", createTime=" + createTime +
                ", isPublic=" + isPublic +
                ", userId='" + userId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", bookName='" + bookName + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}