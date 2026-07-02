package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;
import com.example.entity.UserReadProgress;

public interface UserReadProgressDao {
    void add(UserReadProgress userReadProgress);
    void update(UserReadProgress userReadProgress);
    void del(Integer id);
    List<UserReadProgress> queryAll();
    List<UserReadProgress> queryById(Integer id);
    List<UserReadProgress> queryByUserId(String userId); // 外键查询：查某用户的所有阅读进度
    List<UserReadProgress> queryByIsbn(String isbn); // 外键查询：查某书的所有阅读进度
    List<UserReadProgress> queryByUserIdAndIsbn(String userId, String isbn);
    Integer countReadBooksByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
}