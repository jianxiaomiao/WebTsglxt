package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.entity.BorrowInformation;
import com.example.entity.UserInformation;

import java.time.LocalDateTime;
import java.util.List;

public interface UserInformationService {
    // 查询所有用户信息
    ResultDTO<List<UserInformation>> queryAllUsers();
    // 按用户ID查询用户信息
    ResultDTO<List<UserInformation>> queryUserById(String userId);
    ResultDTO<List<UserInformation>> queryUserByName(String name);
    // 新增用户信息
    ResultDTO<Void> addUser(UserInformation user);
    // 删除用户信息（按用户ID）
    ResultDTO<Void> deleteUser(String userId);
    // 更新用户信息
    ResultDTO<Void> updateUser(UserInformation user);

    ResultDTO<List<UserInformation>> queryByNumber(int number);

    public ResultDTO<Void> updateUserStock(UserInformation user);

    // ====================== 新增：Token 相关方法 ======================
    /**
     * 根据登录Token查询用户
     */
    ResultDTO<UserInformation> getUserByToken(String token);

    /**
     * 更新用户登录Token和过期时间
     */
    ResultDTO<Void> updateUserToken(String userId, String token, LocalDateTime expireTime);
}