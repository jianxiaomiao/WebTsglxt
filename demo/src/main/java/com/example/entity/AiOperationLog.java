// com.example.entity.AiOperationLog
package com.example.entity;

import java.time.LocalDateTime;

public class AiOperationLog {
    private Long id;
    private String userId;
    private String actionType;
    private String targetEntity;
    private String actionData;
    private String executeResult;
    private LocalDateTime createTime;

    // 构造方法
    public AiOperationLog(String userId, String actionType, String targetEntity, String actionData, String executeResult) {
        this.userId = userId;
        this.actionType = actionType;
        this.targetEntity = targetEntity;
        this.actionData = actionData;
        this.executeResult = executeResult;
        this.createTime = LocalDateTime.now();
    }

    public AiOperationLog() {
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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getActionData() {
        return actionData;
    }

    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    public String getExecuteResult() {
        return executeResult;
    }

    public void setExecuteResult(String executeResult) {
        this.executeResult = executeResult;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "AiOperationLog{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", actionType='" + actionType + '\'' +
                ", targetEntity='" + targetEntity + '\'' +
                ", actionData='" + actionData + '\'' +
                ", executeResult='" + executeResult + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}