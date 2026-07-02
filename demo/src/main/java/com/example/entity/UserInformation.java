package com.example.entity;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore; // 🔥 引入注解
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.james.mime4j.dom.datetime.DateTime;

public class UserInformation {
    private String UserId;
    private String Name;
    private String Sex;
    private LocalDate Birthday;
    // 🔥 数据库真实字段：系别ID（int，外键绑定user_dept_type.id）
    private Integer DeptType;
    // 🔥 前端显示用：系别名称（不入库，联表查询填充）
    private String deptTypeName;
    private LocalDate Regdate;
    // 数据库真实字段：用户类型ID（int）
    private Integer Type;
    // 前端显示用：用户类型名称（不入库，联表查询填充）
    private String typeName;
    private Integer Can_use;
    private String Root;
    @JSONField(serialize = false)
    private String Password;
    @JSONField(serialize = false)
    private String Salt;
    private Integer read_time_long;
    // 1. 新增属性
    private String bio;
    private String login_token;
    private LocalDate token_expire;

    public UserInformation() {
    }

    // 🔥 全参构造器：已更新DeptType和deptTypeName的位置
    public UserInformation(
            String UserId, String Name, String Sex, LocalDate Birthday,
            Integer DeptType, String deptTypeName, LocalDate Regdate,
            Integer Type, String typeName,
            Integer Can_use, String Root, String Password, String Salt, Integer read_time_long,
                    String bio
    ) {
        this.UserId = UserId;
        this.Name = Name;
        this.Sex = Sex;
        this.Birthday = Birthday;
        this.DeptType = DeptType;
        this.deptTypeName = deptTypeName;
        this.Regdate = Regdate;
        this.Type = Type;
        this.typeName = typeName;
        this.Can_use = Can_use;
        this.Root = Root;
        this.Password = Password;
        this.Salt = Salt;
        this.read_time_long = read_time_long;
        this.bio = bio;
    }

    public LocalDate getToken_expire() {
        return token_expire;
    }

    public void setToken_expire(LocalDate token_expire) {
        this.token_expire = token_expire;
    }

    public String getLogin_token() {
        return login_token;
    }

    @Override
    public String toString() {
        return "UserInformation{" +
                "UserId='" + UserId + '\'' +
                ", Name='" + Name + '\'' +
                ", Sex='" + Sex + '\'' +
                ", Birthday=" + Birthday +
                ", DeptType=" + DeptType +
                ", deptTypeName='" + deptTypeName + '\'' +
                ", Regdate=" + Regdate +
                ", Type=" + Type +
                ", typeName='" + typeName + '\'' +
                ", Can_use=" + Can_use +
                ", Root='" + Root + '\'' +
                ", Password=***" +
                ", Salt=***" +
                ", read_time_long=" + read_time_long +
                ", bio='" + bio + '\'' +
                ", login_token=***" +
                ", token_expire=" + token_expire +
                '}';
    }

    public void setLogin_token(String login_token) {
        this.login_token = login_token;
    }

    // Getter & Setter
    public String getUserId() {
        return UserId;
    }
    public void setUserId(String UserId) {
        this.UserId = UserId;
    }

    public String getName() {
        return Name;
    }
    public void setName(String Name) {
        this.Name = Name;
    }

    public String getSex() {
        return Sex;
    }
    public void setSex(String Sex) {
        this.Sex = Sex;
    }

    public LocalDate getBirthday() {
        return Birthday;
    }
    public void setBirthday(LocalDate Birthday) {
        this.Birthday = Birthday;
    }

    // 🔥 系别ID相关Getter/Setter
    public Integer getDeptType() {
        return DeptType;
    }
    public void setDeptType(Integer DeptType) {
        this.DeptType = DeptType;
    }

    // 🔥 系别名称相关Getter/Setter
    public String getDeptTypeName() {
        return deptTypeName;
    }
    public void setDeptTypeName(String deptTypeName) {
        this.deptTypeName = deptTypeName;
    }

    public LocalDate getRegdate() {
        return Regdate;
    }
    public void setRegdate(LocalDate Regdate) {
        this.Regdate = Regdate;
    }

    public Integer getType() {
        return Type;
    }
    public void setType(Integer Type) {
        this.Type = Type;
    }

    public String getTypeName() {
        return typeName;
    }
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getCan_use() {
        return Can_use;
    }
    public void setCan_use(Integer Can_use) {
        this.Can_use = Can_use;
    }

    public String getRoot() {
        return Root;
    }
    public void setRoot(String Root) {
        this.Root = Root;
    }

    public String getPassword() {
        return Password;
    }
    public void setPassword(String Password) {
        this.Password = Password;
    }

    public String getSalt() {
        return Salt;
    }
    public void setSalt(String Salt) {
        this.Salt = Salt;
    }

    public Integer getRead_time_long() {
        return read_time_long;
    }
    public void setRead_time_long(Integer read_time_long) {
        this.read_time_long = read_time_long;
    }

    // 3. 新增Getter/Setter
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

}