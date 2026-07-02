package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.entity.UserComment;

public interface UserCommentDao {
    Integer add(UserComment userComment);

    void update(UserComment userComment);

    void updatePrefer(Integer commentId, int delta);

    void del(Integer commentId);

    List<UserComment> queryAll();

    List<UserComment> queryByCommentId(Integer commentId);

    List<UserComment> queryByUserId(String userId);

    // 分页查询 用户的论坛评论
    List<UserComment> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    // 统计 用户的论坛评论总数
    Long countByUserId(String userId);

    List<UserComment> queryByType(String sortType);

    // ====================== 新增：分页查询主评论（parentId=0） ======================
    List<UserComment> queryMainCommentsByPage(Integer pageNum, Integer pageSize, String sortType);

    // ====================== 新增：查询主评论总条数 ======================
    Long countMainComments();

    // ====================== 新增：分页查询指定主评论下的子评论 ======================
    List<UserComment> querySubCommentsByPage(Integer parentId, Integer pageNum, Integer pageSize, String sortType);

    // ====================== 新增：查询指定主评论下的子评论总条数 ======================
    Long countSubCommentsByParentId(Integer parentId);

    // ====================== 新增：批量统计多个主评论的子评论总数（性能优化） ======================
    Map<Integer, Long> batchCountSubComments(List<Integer> parentIds);

    Integer countUserCommentByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID + 时间段 查询论坛评论
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内的论坛评论列表
     */
    List<UserComment> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}