package com.example.entity;

import java.time.LocalDateTime;

public class BookBottlePick {
    // 数据库基础字段
    private Integer id;
    private String userid;      // 捞取用户ID
    private Integer bottleId;   // 关联漂流瓶ID
    private String replycontent;// 捞取者回复内容
    private LocalDateTime createtime; // 捞取时间

    // 联表查询额外字段
    private String username;    // 捞取用户昵称

    public BookBottlePick() {
    }

    public BookBottlePick(Integer id, String userid, Integer bottleId, String replycontent, LocalDateTime createtime) {
        this.id = id;
        this.userid = userid;
        this.bottleId = bottleId;
        this.replycontent = replycontent;
        this.createtime = createtime;
    }

    // ========== Getter / Setter ==========
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    public Integer getBottleId() { return bottleId; }
    public void setBottleId(Integer bottleId) { this.bottleId = bottleId; }
    public String getReplycontent() { return replycontent; }
    public void setReplycontent(String replycontent) { this.replycontent = replycontent; }
    public LocalDateTime getCreatetime() { return createtime; }
    public void setCreatetime(LocalDateTime createtime) { this.createtime = createtime; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() {
        return "BookBottlePick{" +
                "id=" + id +
                ", userid='" + userid + '\'' +
                ", bottleId=" + bottleId +
                ", replycontent='" + replycontent + '\'' +
                ", createtime=" + createtime +
                ", username='" + username + '\'' +
                '}';
    }
}