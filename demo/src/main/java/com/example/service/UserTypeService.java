package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserType;
import java.util.List;

public interface UserTypeService {
    ResultDTO<List<UserType>> queryAllUserTypes();
    ResultDTO<List<UserType>> queryUserTypeById(Integer id);
    ResultDTO<Void> addUserType(UserType userType);
    ResultDTO<Void> updateUserType(UserType userType);
    ResultDTO<Void> deleteUserType(Integer id);
}