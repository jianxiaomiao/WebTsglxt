package com.example.dao;

import com.example.entity.BookBottle;
import java.util.List;

public interface BookBottleDao {
    // 新增漂流瓶
    void add(BookBottle bottle);
    // 动态更新漂流瓶（前端传什么更新什么）
    void update(BookBottle bottle);
    // 物理删除（备用，业务推荐软删update isDeleted=1）
    void del(Integer bottleId);

    // 1. 根据ID查询瓶子（联用户表）
    List<BookBottle> queryById(Integer bottleId);
    // 2. 根据ISBN分页查询【仅漂流中status=0、未删除、未过期】
    List<BookBottle> queryByIsbnPage(String isbn, int offset, int pageSize);
    // 统计当前ISBN下漂流瓶总数（分页用）
    Long countByIsbn(String isbn);
    // 3. 根据章节分页查询【仅漂流中status=0、未删除、未过期】
    List<BookBottle> queryByChapterPage(String chapter, int offset, int pageSize);
    // 统计当前章节下漂流瓶总数（分页用）
    Long countByChapter(String chapter);

    // 扩展刚需：根据发布人userId查询自己所有瓶子（不分状态）
    List<BookBottle> queryByUserId(String userId);
    // 捞瓶专用：随机获取一条同本书可捞取瓶子
    List<BookBottle> getRandomBottle(String isbn, String loginUserId);

    /**
     * 分页查询当前用户可捞取的漂流瓶（排除已捞+自己发布+过期删除）
     */
    List<BookBottle> queryAvailableBottleByIsbnPage(String isbn, String loginUserId, int offset, int pageSize);

    /**
     * 统计当前用户可捞取的漂流瓶总数（用于分页）
     */
    Long countAvailableByIsbn(String isbn, String loginUserId);
}