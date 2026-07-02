package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookType;
import com.example.entity.UserTextColType;

import java.util.List;

public interface UserTextColTypeService {
    ResultDTO<List<UserTextColType>> queryAllUserTextColType();
    ResultDTO<List<UserTextColType>> queryUserTextColTypeId(Integer id);
    ResultDTO<Void> addUserTextColType(UserTextColType userTextColType);
    ResultDTO<Void> updateUserTextColType(UserTextColType userTextColType);
    ResultDTO<Void> deleteUserTextColType(Integer id);
}
