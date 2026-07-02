package com.example.entity;

import java.time.LocalDateTime;

public class UserTextCollection {
    private Integer id;
    private String userId;
    private String isbn;
    private String chapterId;
    private String text;
    private LocalDateTime createTime;
    private Integer Type;
    private String noteTypeName;
    private String readerComment;
    // ====================== 新增：书籍名称、书籍封面 ======================
    private String bookName;
    private String pictureName;

    public UserTextCollection() {
    }

    // 全参构造（新增书籍字段）
    public UserTextCollection(Integer id, String userId, String isbn, String chapterId, String text, LocalDateTime createTime, Integer Type, String noteTypeName, String readerComment, String bookName, String pictureName) {
        this.id = id;
        this.userId = userId;
        this.isbn = isbn;
        this.chapterId = chapterId;
        this.text = text;
        this.createTime = createTime;
        this.Type = Type;
        this.noteTypeName = noteTypeName;
        this.readerComment = readerComment;
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

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getType() {
        return Type;
    }

    public void setType(Integer Type) {
        this.Type = Type;
    }

    public String getNoteTypeName() {
        return noteTypeName;
    }

    public void setNoteTypeName(String noteTypeName) {
        this.noteTypeName = noteTypeName;
    }

    public String getReaderComment() {
        return readerComment;
    }

    public void setReaderComment(String readerComment) {
        this.readerComment = readerComment;
    }

    @Override
    public String toString() {
        return "UserTextCollection{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", chapterId='" + chapterId + '\'' +
                ", text='" + text + '\'' +
                ", createTime=" + createTime +
                ", Type=" + Type +
                ", noteTypeName='" + noteTypeName + '\'' +
                ", readerComment='" + readerComment + '\'' +
                ", bookName='" + bookName + '\'' +
                ", pictureName='" + pictureName + '\'' +
                '}';
    }
}