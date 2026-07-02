package com.example.dao;

import java.util.List;

import com.example.entity.UserInformation;

public interface UserInformationDao {
    void add(UserInformation department);
    void update(UserInformation department);
    void del(String id);
    List<UserInformation> queryAll();
    List<UserInformation> queryUserById(String id);
    List<UserInformation> queryByNumber(int number);
    List<UserInformation> queryByName(String name);

    UserInformation queryByToken(String token);
    void updateToken(String userId, String token, java.time.LocalDateTime expireTime);
}
