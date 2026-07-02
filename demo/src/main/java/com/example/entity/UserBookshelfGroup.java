package com.example.entity;

import java.sql.Timestamp;

public class UserBookshelfGroup {
    private Integer GroupId;
    private String UserId;
    private String GroupName;
    private Integer Sort;
    private Timestamp create_time;

    // 无参构造
    public UserBookshelfGroup() {
    }

    // 全参构造（可选）
    public UserBookshelfGroup(String userId, String groupName, Integer sort) {
        UserId = userId;
        GroupName = groupName;
        Sort = sort;
    }

    // Getter & Setter
    public Integer getGroupId() {
        return GroupId;
    }

    public void setGroupId(Integer groupId) {
        GroupId = groupId;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getGroupName() {
        return GroupName;
    }

    public void setGroupName(String groupName) {
        GroupName = groupName;
    }

    public Integer getSort() {
        return Sort;
    }

    public void setSort(Integer sort) {
        Sort = sort;
    }

    public Timestamp getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Timestamp create_time) {
        this.create_time = create_time;
    }

    @Override
    public String toString() {
        return "UserBookshelfGroup{" +
                "GroupId=" + GroupId +
                ", UserId='" + UserId + '\'' +
                ", GroupName='" + GroupName + '\'' +
                ", Sort=" + Sort +
                ", create_time=" + create_time +
                '}';
    }
}