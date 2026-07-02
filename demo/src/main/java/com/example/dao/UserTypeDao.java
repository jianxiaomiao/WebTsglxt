package com.example.dao;

import com.example.entity.UserType;
import java.util.List;

public interface UserTypeDao {
    void add(UserType userType);
    void update(UserType userType);
    void del(Integer id);
    List<UserType> queryAll();
    List<UserType> queryById(Integer id);
}