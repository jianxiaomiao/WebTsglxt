package com.example.entity;

import java.time.LocalDateTime;

/**
 * 聊天会话实体类（用于消息页列表）
 */
public class ChatSession {
    private Long id;
    private String userId;         // 当前用户ID
    private String targetUserId;   // 对方用户ID
    private String lastMessage;    // 最后一条消息（截取前50字）
    private LocalDateTime lastMessageTime; // 最后一条消息时间
    private Integer unreadCount;   // 未读消息数
    private LocalDateTime updateTime; // 最后更新时间

    // 构造方法
    public ChatSession(String userId, String targetUserId, LocalDateTime updateTime) {
        this.userId = userId;
        this.targetUserId = targetUserId;
        this.updateTime = updateTime;
        this.unreadCount = 0; // 默认未读数0
    }

    public ChatSession() {}

    // ====================== Getter & Setter ======================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "ChatSession [id=" + id + ", userId=" + userId + ", targetUserId=" + targetUserId
                + ", lastMessage=" + lastMessage + ", unreadCount=" + unreadCount + "]";
    }
}