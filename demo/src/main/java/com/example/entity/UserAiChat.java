package com.example.entity;

import java.time.LocalDateTime;

/**
 * AI 聊天记录实体类
 */
public class UserAiChat {
    private Long id;
    private String userId;          // 用户ID
    private String messageContent;   // 消息内容
    private Integer messageType;    // 【内容类型】1=文字 2=图片 3=文件
    private LocalDateTime createTime; // 创建时间
    private Integer formType;       // 【发送方】0=用户 1=AI

    // 无参构造
    public UserAiChat() {}



    // 带参构造
    public UserAiChat(String userId, String messageContent, Integer messageType, LocalDateTime createTime, Integer formType) {
        this.userId = userId;
        this.messageContent = messageContent;
        this.messageType = messageType;
        this.createTime = createTime;
        this.formType = formType;
    }

    // Getter & Setter

    public Integer getFormType() {
        return formType;
    }

    public void setFormType(Integer formType) {
        this.formType = formType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    // 🔥 补上 formType，否则日志看不到
    @Override
    public String toString() {
        return "UserAiChat{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", messageContent='" + messageContent + '\'' +
                ", messageType=" + messageType +
                ", formType=" + formType +
                ", createTime=" + createTime +
                '}';
    }
}