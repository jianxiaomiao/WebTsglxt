package com.example.entity;

import java.sql.Timestamp;

public class UserCollection {

    private Integer CollectionId;
    private String UserId;
    private String Isbn;
    private Timestamp lastReadTime;
    private String bookName;
    private String information;
    // 书籍封面
    private String pictureName;

    // ========== 新增书架分组字段 ==========
    private Integer GroupId;       // 数据库真实存储，关联user_bookshelf_group
    private String GroupName;      // 仅联表查询返回，数据库无此字段，前端展示分组名

    public UserCollection() {
    }

    public UserCollection(String userId, String isbn) {
        this.UserId = userId;
        this.Isbn = isbn;
    }

    // ========== 原有 Getter/Setter ==========
    public String getPictureName() {
        return pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public Integer getCollectionId() {
        return CollectionId;
    }

    public void setCollectionId(Integer collectionId) {
        CollectionId = collectionId;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        this.UserId = userId;
    }

    public String getIsbn() {
        return Isbn;
    }

    public void setIsbn(String isbn) {
        this.Isbn = isbn;
    }

    public Timestamp getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(Timestamp lastReadTime) { this.lastReadTime = lastReadTime; }

    // ========== 新增分组 Getter/Setter ==========
    public Integer getGroupId() {
        return GroupId;
    }

    public void setGroupId(Integer groupId) {
        GroupId = groupId;
    }

    public String getGroupName() {
        return GroupName;
    }

    public void setGroupName(String groupName) {
        GroupName = groupName;
    }

    @Override
    public String toString() {
        return "UserCollection{" +
                "CollectionId=" + CollectionId +
                ", UserId='" + UserId + '\'' +
                ", Isbn='" + Isbn + '\'' +
                ", lastReadTime=" + lastReadTime +
                ", bookName='" + bookName + '\'' +
                ", information='" + information + '\'' +
                ", pictureName='" + pictureName + '\'' +
                ", GroupId=" + GroupId +
                ", GroupName='" + GroupName + '\'' +
                '}';
    }
}