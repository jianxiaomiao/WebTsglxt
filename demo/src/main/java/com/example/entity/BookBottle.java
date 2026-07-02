package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

public class BookBottle {
    // ========== 数据库基础字段 ==========
    private Integer id;
    private String userid;
    private String isbn;
    private String chapter;
    private String content;
    private LocalDateTime createtime;
    private LocalDateTime updateTime;
    private Integer status;       // 0漂流中 2已过期 3用户删除（1已捞取状态废弃）
    private LocalDateTime expireTime;
    private Integer allowReply;
    private Integer isDeleted;

    // ========== 联表查询额外字段 ==========
    private String username;          // 发布瓶子用户昵称
    private List<BookBottlePick> pickList; // 该瓶子的所有捞取记录

    public BookBottle() {
    }

    public BookBottle(Integer id, String userid, String isbn, String content, LocalDateTime createtime, LocalDateTime updateTime, Integer status,
                      LocalDateTime expireTime, Integer allowReply, Integer isDeleted, String username) {
        this.id = id;
        this.userid = userid;
        this.isbn = isbn;
        this.content = content;
        this.createtime = createtime;
        this.updateTime = updateTime;
        this.status = status;
        this.expireTime = expireTime;
        this.allowReply = allowReply;
        this.isDeleted = isDeleted;
        this.username = username;
    }

    // ========== Getter / Setter ==========
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatetime() { return createtime; }
    public void setCreatetime(LocalDateTime createtime) { this.createtime = createtime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getAllowReply() { return allowReply; }
    public void setAllowReply(Integer allowReply) { this.allowReply = allowReply; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<BookBottlePick> getPickList() { return pickList; }
    public void setPickList(List<BookBottlePick> pickList) { this.pickList = pickList; }

    @Override
    public String toString() {
        return "BookBottle{" +
                "id=" + id +
                ", userid='" + userid + '\'' +
                ", isbn='" + isbn + '\'' +
                ", chapter='" + chapter + '\'' +
                ", content='" + content + '\'' +
                ", createtime=" + createtime +
                ", updateTime=" + updateTime +
                ", status=" + status +
                ", expireTime=" + expireTime +
                ", allowReply=" + allowReply +
                ", isDeleted=" + isDeleted +
                ", username='" + username + '\'' +
                ", pickList=" + pickList +
                '}';
    }
}