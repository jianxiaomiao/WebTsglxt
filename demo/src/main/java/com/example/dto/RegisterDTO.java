package com.example.dto;

import com.example.entity.UserInformation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

// 注册表单专用DTO（包含UserInformation没有的confirmPassword）
public class RegisterDTO {
    private String userid;
    private String username;
    private String password;
    private String confirmPassword;
    private String sex;
    private String birthday;
    // 🔥 1. 改为包装类，避免默认值0；2. 字段名保持dept_type不变
    private Integer dept_type;
    private Integer user_type;
    private int read_time_long;
    private String emailCode;

    // DTO转实体
    public UserInformation toUserInfo(String encryptedPwd, String salt) {
        UserInformation user = new UserInformation();
        user.setUserId(this.userid);
        user.setName(this.username);
        user.setSex(this.sex);

        if (this.birthday != null && !this.birthday.trim().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withResolverStyle(ResolverStyle.SMART);
                LocalDate birthday = LocalDate.parse(this.birthday.trim(), formatter);
                user.setBirthday(birthday);
            } catch (DateTimeParseException ignored) {}
        }

        user.setDeptType(1); // 🔥 默认系别=1（未知），前端不再显示系别选择
        user.setRegdate(LocalDate.now());
        user.setType(1);      // 🔥 强制注册为普通读者(Type=1)，不允许前端选择管理员
        user.setCan_use(1);   // 🔥 新用户默认活跃(1)，由管理员审核后解冻
        user.setRoot(this.userid);
        user.setPassword(encryptedPwd);
        user.setSalt(salt);
        user.setRead_time_long(read_time_long);
        return user;
    }

    // ===================== 🔥 核心修复：getter/setter 匹配前端 deptType =====================
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    // 🔥 原来的 getDept/setDept → 改为 getDeptType/setDeptType，匹配前端字段
    public Integer getDeptType() { return dept_type; }
    public void setDeptType(Integer dept_type) { this.dept_type = dept_type; }

    public Integer getUserType() { return user_type; }
    public void setUserType(Integer user_type) { this.user_type = user_type; }
    public int getRead_time_long() { return read_time_long; }
    public void setRead_time_long(int read_time_long) { this.read_time_long = read_time_long; }

    public String getEmailCode() {
        return emailCode;
    }
    public void setEmailCode(String emailCode){
        this.emailCode = emailCode;
    }
}