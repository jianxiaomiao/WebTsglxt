package com.example.dao;

import com.example.entity.UserReadRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户阅读记录DAO接口
 */
public interface UserReadRecordDao {
    /**
     * 新增阅读记录
     * @param userReadRecord 阅读记录实体
     */
    void add(UserReadRecord userReadRecord);


    /**
     * 删除阅读记录
     * @param id 记录ID
     */
    void del(Integer id);

    /**
     * 查询所有阅读记录（按创建时间倒序）
     * @return 阅读记录列表
     */
    List<UserReadRecord> queryAll();

    /**
     * 根据ID查询阅读记录
     * @param id 记录ID
     * @return 阅读记录列表
     */
    List<UserReadRecord> queryById(Integer id);

    /**
     * 根据用户ID查询阅读记录（按创建时间倒序）
     * @param userId 用户ID
     * @return 阅读记录列表
     */
    List<UserReadRecord> queryByUserId(String userId);

    /**
     * 根据ISBN查询阅读记录
     * @param isbn 书籍ISBN
     * @return 阅读记录列表
     */
    List<UserReadRecord> queryByIsbn(String isbn);

    /**
     * 根据用户ID+ISBN查询阅读记录
     * @param userId 用户ID
     * @param isbn 书籍ISBN
     * @return 阅读记录列表
     */
    List<UserReadRecord> queryByUserIdAndIsbn(String userId, String isbn);

    List<UserReadRecord> queryByUserIdIsbnAndDate(String userId, String isbn, LocalDate readDate);
    /**
     * 统计用户在指定时间段内的总阅读时长（秒）
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总阅读时长（秒）
     */
    Integer sumReadDurationByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    List<UserReadRecord> listBookReadDurationByTime(String userId, LocalDateTime startTime, LocalDateTime endTime);
}