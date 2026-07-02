package com.example.dao;

import com.example.entity.BookBottlePick;
import java.util.List;

public interface BookBottlePickDao {
    // 新增捞取记录
    void addPick(BookBottlePick pick);
    // 检查用户是否已捞取该瓶子
    boolean checkUserPicked(String userId, Integer bottleId);
    // 根据瓶子ID查询所有捞取记录（联用户昵称）
    List<BookBottlePick> queryByBottleId(Integer bottleId);
    // 根据用户ID查询所有捞取记录
    List<BookBottlePick> queryByUserId(String userId);
    // 更新捞取记录的回复内容
    void updateReply(Integer pickId, String replycontent);
}