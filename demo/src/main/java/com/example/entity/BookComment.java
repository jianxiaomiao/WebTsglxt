package com.example.entity;

import java.time.LocalDateTime;

public class BookComment {
    private String Comment;
    private String ISBN;
    private String Userid;
    private LocalDateTime Time;
    private Integer CommentId;
    private Integer Star;
    // ====================== 新增：书籍名称、书籍封面 ======================
    private String bookName;
    private String pictureName;
    private String userName;  // 🔥 JOIN user_information 获取评论者昵称

    public BookComment(String Comment, String ISBN, String Userid, LocalDateTime Time, Integer CommentId,Integer Star ) {
        this.Comment = Comment;
        this.CommentId = CommentId;
        this.ISBN = ISBN;
        this.Time = Time;
        this.Userid = Userid;
        this.Star = Star;
    }

    public BookComment() {
    }

    public String getComment() {
        return Comment;
    }

    public void setComment(String Comment) {
        this.Comment = Comment;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public String getUserid() {
        return Userid;
    }

    public void setUserid(String Userid) {
        this.Userid = Userid;
    }

    public LocalDateTime getTime() {
        return Time;
    }

    public void setTime(LocalDateTime Time) {
        this.Time = Time;
    }

    public Integer getCommentId() {
        return CommentId;
    }

    public void setCommentId(Integer CommentId) {
        this.CommentId = CommentId;
    }

    public Integer getStar() {
        return Star;
    }

    public void setStar(Integer Star) {
        this.Star = Star;
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

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Override
    public String toString() {
        return "BookComment [Comment=" + Comment + ", ISBN=" + ISBN + ", Userid=" + Userid + ", Time=" + Time
                + ", CommentId=" + CommentId + "]";
    }
}