// Entity: UserTextColType.java
package com.example.entity;

public class UserTextColType {
    private Integer id;
    private String userTextColType;

    public UserTextColType() {
    }

    public UserTextColType(Integer id, String userTextColType) {
        this.id = id;
        this.userTextColType = userTextColType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserTextColType() {
        return userTextColType;
    }

    public void setUserTextColType(String userTextColType) {
        this.userTextColType = userTextColType;
    }

    @Override
    public String toString() {
        return "UserTextColType{" +
                "id=" + id +
                ", userTextColType='" + userTextColType + '\'' +
                '}';
    }
}