package com.example.dao;

import com.example.entity.UserTextCollection;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTextCollectionDao {
    void add(UserTextCollection note);
    void update(UserTextCollection note);
    void del(Integer id);
    List<UserTextCollection> queryAll();
    List<UserTextCollection> queryById(Integer id);
    List<UserTextCollection> queryByUserId(String userId);
    // 新增：分页查询用户阅读笔记
    List<UserTextCollection> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    // 新增：统计用户阅读笔记总数
    Long countByUserId(String userId);
    List<UserTextCollection> queryByIsbn(String isbn);
    List<UserTextCollection> queryByChapterId(String chapterId);
    Integer countNewNotesByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间段查询阅读笔记
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 笔记列表
     */
    List<UserTextCollection> queryByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
}