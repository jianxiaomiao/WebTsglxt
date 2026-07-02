package com.example.dao;

import com.example.entity.Bookmark;

import java.time.LocalDateTime;
import java.util.List;

public interface BookmarkDao {
    // 新增书签
    void add(Bookmark bookmark);

    // 修改书签
    void update(Bookmark bookmark);

    // 根据自增ID删除书签
    void delById(Long id);
    // 根据自增ID修改书签
    void updateById(Bookmark bookmark);

    // 删除书签（根据 用户ID + ISBN + 章节号 唯一删除）
    void del(String userId, String isbn, String chapterNumber);

    // 根据 ISBN 查询
    List<Bookmark> queryByIsbn(String isbn);

    // 根据 用户ID 查询（按创建时间倒序）
    List<Bookmark> queryByUserId(String userId);

    // 根据 章节号 查询
    List<Bookmark> queryByChapterNumber(String chapterNumber);

    // 根据 用户ID + ISBN 查询
    List<Bookmark> queryByUserIdAndIsbn(String userId, String isbn);

    /**
     * 统计用户指定时间段内的书签数量
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 书签总数
     */
    Integer countBookmarkByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询用户指定时间段内的书签列表
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 书签列表 List<Bookmark>
     */
    List<Bookmark> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}