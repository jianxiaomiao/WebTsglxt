package com.example.dao;

import com.example.entity.UserTextColType;
import java.util.List;

public interface UserTextColTypeDao {
    void add(UserTextColType type);
    void update(UserTextColType type);
    void del(Integer id);
    List<UserTextColType> queryAll();
    List<UserTextColType> queryById(Integer id);
}