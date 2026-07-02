package com.example.entity;

import java.time.LocalDateTime;

/**
 * 好友申请实体类
 */
public class FriendRequest {
    private Integer id;
    private String fromUserId;   // 发起者ID
    private String toUserId;     // 接收者ID
    private String requestMsg;   // 申请留言
    private Integer status;      // 0:待处理 1:已同意 2:已拒绝 3:已过期
    private LocalDateTime createTime; // 申请时间
    private LocalDateTime handleTime; // 处理时间
    // ====================== 新增：用户名称 ======================
    private String userName;

    // 构造方法
    public FriendRequest(String fromUserId, String toUserId, String requestMsg, LocalDateTime createTime) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.requestMsg = requestMsg;
        this.createTime = createTime;
        this.status = 0; // 默认待处理
    }

    public FriendRequest() {}

    // ====================== Getter & Setter ======================
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getRequestMsg() { return requestMsg; }
    public void setRequestMsg(String requestMsg) { this.requestMsg = requestMsg; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }

    // ====================== 新增用户名 Getter/Setter ======================
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "FriendRequest [id=" + id + ", fromUserId=" + fromUserId + ", toUserId=" + toUserId
                + ", requestMsg=" + requestMsg + ", status=" + status + ", createTime=" + createTime + ", userName=" + userName + "]";
    }
}