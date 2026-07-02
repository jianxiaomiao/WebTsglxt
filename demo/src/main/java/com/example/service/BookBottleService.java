package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookBottle;
import java.util.List;

public interface BookBottleService {
    // 新增扔瓶子
    ResultDTO<Void> addBottle(BookBottle bottle);
    // 动态更新瓶子
    ResultDTO<Void> updateBottle(BookBottle bottle);
    // 物理删除瓶子
    ResultDTO<Void> deleteBottle(Integer bottleId);

    // 根据ID查询瓶子
    ResultDTO<List<BookBottle>> queryBottleById(Integer bottleId);
    // ISBN分页查询漂流中瓶子
    ResultDTO<PageResultDTO<BookBottle>> queryBottleByIsbnPage(String isbn, Integer pageNum, Integer pageSize);
    // 章节分页查询漂流中瓶子
    ResultDTO<PageResultDTO<BookBottle>> queryBottleByChapterPage(String chapter, Integer pageNum, Integer pageSize);
    // 查询用户自己发布的所有瓶子
    ResultDTO<List<BookBottle>> queryBottleByUserId(String userId);
    // 随机捞取同本书瓶子
    ResultDTO<BookBottle> randomGetBottle(String isbn, String loginUserId);

    /**
     * 分页获取当前用户可捞取的漂流瓶列表
     */
    ResultDTO<PageResultDTO<BookBottle>> randomGetBottle(String isbn, String loginUserId, Integer pageNum, Integer pageSize);

    /**
     * 用户捞取漂流瓶
     * @param loginUserId 捞取用户ID
     * @param bottleId 瓶子ID
     * @param replycontent 捞取时的回复内容（可为空）
     * @return 漂流瓶对应的ISBN，用于日志记录
     */
    ResultDTO<String> pickBottle(String loginUserId, Integer bottleId, String replycontent);
}