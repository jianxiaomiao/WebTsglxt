package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigInteger;

public class UserDailyAiPicture {
    private BigInteger id;
    private String userId;
    private LocalDate genDate;
    private String imgUrl;
    private LocalDateTime createTime;

    // 无参构造
    public UserDailyAiPicture() {}

    // 全参构造
    public UserDailyAiPicture(BigInteger id, String userId, LocalDate genDate, String imgUrl, LocalDateTime createTime) {
        this.id = id;
        this.userId = userId;
        this.genDate = genDate;
        this.imgUrl = imgUrl;
        this.createTime = createTime;
    }

    // Getter & Setter
    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getGenDate() {
        return genDate;
    }

    public void setGenDate(LocalDate genDate) {
        this.genDate = genDate;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "UserDailyAiPicture{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", genDate=" + genDate +
                ", imgUrl='" + imgUrl + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}