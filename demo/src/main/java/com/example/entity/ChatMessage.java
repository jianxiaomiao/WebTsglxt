package com.example.entity;

import java.time.LocalDateTime;

/**
 * 私聊消息实体类
 */
public class ChatMessage {
    private Long id;
    private String fromUserId;   // 发送者ID
    private String toUserId;     // 接收者ID
    private Integer messageType; // 1:文本 2:图片 3:语音
    private String messageContent; // 消息内容
    private Integer isRead;      // 0:未读 1:已读
    private LocalDateTime createTime; // 发送时间
    // 新增字段
    private Long replyToId; // 引用的消息ID
    private Boolean hideFromSender; // 对发送者隐藏
    private Boolean hideFromReceiver; // 对接收者隐藏
    private Boolean isRecalled; // 是否被撤回
    private String originalContent; // 撤回前的原始内容
    // 新增字段
    private LocalDateTime recallTime; // 撤回时间

    // 构造方法
    public ChatMessage(String fromUserId, String toUserId, Integer messageType,
                       String messageContent, LocalDateTime createTime) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.messageType = messageType;
        this.messageContent = messageContent;
        this.createTime = createTime;
        this.isRead = 0; // 默认未读
        this.hideFromSender = false;
        this.hideFromReceiver = false;
        this.isRecalled = false;
    }

    public ChatMessage() {}

    // ====================== Getter & Setter ======================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public Integer getMessageType() { return messageType; }
    public void setMessageType(Integer messageType) { this.messageType = messageType; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }

    public Boolean getHideFromSender() { return hideFromSender; }
    public void setHideFromSender(Boolean hideFromSender) { this.hideFromSender = hideFromSender; }

    public Boolean getHideFromReceiver() { return hideFromReceiver; }
    public void setHideFromReceiver(Boolean hideFromReceiver) { this.hideFromReceiver = hideFromReceiver; }

    public Boolean getIsRecalled() { return isRecalled; }
    public void setIsRecalled(Boolean isRecalled) { this.isRecalled = isRecalled; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    // ====================== 新增Getter & Setter ======================
    public LocalDateTime getRecallTime() {
        return recallTime;
    }

    public void setRecallTime(LocalDateTime recallTime) {
        this.recallTime = recallTime;
    }
    @Override
    public String toString() {
        return "ChatMessage [id=" + id + ", fromUserId=" + fromUserId + ", toUserId=" + toUserId
                + ", messageType=" + messageType + ", messageContent=" + messageContent
                + ", isRead=" + isRead + ", createTime=" + createTime + "]";
    }
}