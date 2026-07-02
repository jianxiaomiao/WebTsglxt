package com.example.dao;

import com.example.entity.UserCommentLike;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCommentLikeDao {
    // 根据用户ID，查询该用户点赞过的所有评论ID
    List<Integer> selectLikedCommentIdsByUserId(String userId);
    // 判断用户是否已点赞
    boolean isLiked(String userId, Integer commentId);
    // 新增点赞
    void addLike(UserCommentLike like);
    // 删除点赞（取消）
    void deleteLike(String userId, Integer commentId);

    /**
     * 统计用户指定时间段内的评论点赞数量
     */
    Integer countCommentLikeByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询用户指定时间段内的评论点赞记录
     */
    List<UserCommentLike> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}