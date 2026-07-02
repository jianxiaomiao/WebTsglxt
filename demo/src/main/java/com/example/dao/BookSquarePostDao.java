package com.example.dao;

import com.example.entity.BookSquarePost;
import java.util.List;

/**
 * 书荒广场帖子 DAO 接口
 */
public interface BookSquarePostDao {

    /**
     * 新增帖子
     */
    void add(BookSquarePost post);

    /**
     * 动态更新帖子（非空字段才更新）
     */
    void update(BookSquarePost post);

    /**
     * 逻辑删除：将status改为0（屏蔽/删除）
     */
    void logicDelete(Integer id);

    /**
     * 根据主键ID查询单条帖子（仅查正常状态）
     */
    BookSquarePost queryById(Integer id);

    // ====================== 分页查询 + 统计总数 ======================
    /**
     * 分页查询所有正常帖子
     */
    List<BookSquarePost> queryAllPage(Integer pageNum, Integer pageSize);

    /**
     * 统计所有正常帖子总数
     */
    Long countAll();

    /**
     * 分页查询指定用户发布的正常帖子
     */
    List<BookSquarePost> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    /**
     * 统计指定用户帖子总数
     */
    Long countByUserId(String userId);

    /**
     * 分页查询所有主帖（parent_id=0，正常状态）
     */
    List<BookSquarePost> queryMainPostPage(Integer pageNum, Integer pageSize);

    /**
     * 统计主帖总数
     */
    Long countMainPost();

    /**
     * 分页查询指定主帖下的子帖（正常状态）
     */
    List<BookSquarePost> querySubPostByParentIdPage(Integer parentId, Integer pageNum, Integer pageSize);

    /**
     * 统计指定主帖下子帖总数
     */
    Long countSubPostByParentId(Integer parentId);

    /**
     * 获取指定主帖的子帖数量（用于实体subPostCount赋值）
     */
    Integer getSubPostCount(Integer parentId);
}