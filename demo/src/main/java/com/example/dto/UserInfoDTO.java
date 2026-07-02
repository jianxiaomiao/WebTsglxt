package com.example.dto;

import com.example.entity.UserInformation;

import java.time.LocalDate;

// 专门给前端返回的用户信息（无敏感字段）
public class UserInfoDTO {
    // 只包含前端需要的字段
    private String userId;
    private String name;
    private String sex;
    private LocalDate birthday;
    private Integer deptType;
    private String deptTypeName;
    private LocalDate regdate;
    private Integer type;
    private String typeName;
    private Integer canUse;
    private String root;
    private Integer readTimeLong;
    // 1. 新增属性
    private String bio;
    // 🔥 构造方法：把User对象转成DTO
    public UserInfoDTO(UserInformation user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.sex = user.getSex();
        this.birthday = user.getBirthday();
        this.deptType = user.getDeptType();
        this.deptTypeName = user.getDeptTypeName();
        this.regdate = user.getRegdate();
        this.type = user.getType();
        this.typeName = user.getTypeName();
        this.canUse = user.getCan_use();
        this.root = user.getRoot();
        this.readTimeLong = user.getRead_time_long();
        this.bio = user.getBio();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public Integer getDeptType() {
        return deptType;
    }

    public void setDeptType(Integer deptType) {
        this.deptType = deptType;
    }

    public String getDeptTypeName() {
        return deptTypeName;
    }

    public void setDeptTypeName(String deptTypeName) {
        this.deptTypeName = deptTypeName;
    }

    public LocalDate getRegdate() {
        return regdate;
    }

    public void setRegdate(LocalDate regdate) {
        this.regdate = regdate;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getCanUse() {
        return canUse;
    }

    public void setCanUse(Integer canUse) {
        this.canUse = canUse;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public Integer getReadTimeLong() {
        return readTimeLong;
    }

    public void setReadTimeLong(Integer readTimeLong) {
        this.readTimeLong = readTimeLong;
    }

    // 3. 新增Getter/Setter
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
