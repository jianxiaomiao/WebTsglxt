package com.example.entity;

import java.time.LocalDateTime;

/**
 * 好友关系实体类（双向好友关系）
 */
public class Friend {
    private Integer id;
    private String userId;       // 用户ID
    private String friendId;     // 好友ID
    private String friendRemark; // 好友备注名
    private LocalDateTime createTime; // 成为好友的时间
    // ====================== 新增：好友用户名 ======================
    private String userName;

    // 构造方法
    public Friend(String userId, String friendId, LocalDateTime createTime) {
        this.userId = userId;
        this.friendId = friendId;
        this.createTime = createTime;
        this.friendRemark = null; // 默认无备注
    }

    public Friend() {}

    // ====================== Getter & Setter ======================
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public String getFriendRemark() { return friendRemark; }
    public void setFriendRemark(String friendRemark) { this.friendRemark = friendRemark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // ====================== 新增用户名 Getter/Setter ======================
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Friend [id=" + id + ", userId=" + userId + ", friendId=" + friendId
                + ", friendRemark=" + friendRemark + ", createTime=" + createTime + ", userName=" + userName + "]";
    }
}