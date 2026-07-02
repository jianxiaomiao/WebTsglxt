package com.example.dao;

import com.example.entity.UserDailyAiPicture;
import java.time.LocalDate;
import java.math.BigInteger;
import java.util.List;

public interface UserDailyAiPictureDao {
    // 新增AI每日插画（INSERT IGNORE 避免唯一索引冲突）
    void add(UserDailyAiPicture picture);

    // 根据主键ID查询
    List<UserDailyAiPicture> queryById(BigInteger id);

    // 根据用户ID分页查询历史插画
    List<UserDailyAiPicture> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    // 统计用户总记录数（分页使用）
    Long countByUserId(String userId);

    // 根据用户ID+指定日期查询单条插画
    List<UserDailyAiPicture> queryByUserIdAndDate(String userId, LocalDate genDate);
}