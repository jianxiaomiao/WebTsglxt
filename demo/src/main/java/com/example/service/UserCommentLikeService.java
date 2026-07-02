package com.example.service;

import com.example.dto.ResultDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCommentLikeService {
    // 查询用户已点赞的评论ID列表
    ResultDTO<List<Integer>> getLikedCommentIds(String userId);
    // 点赞/取消点赞 统一接口
    ResultDTO<Void> toggleLike(String userId, Integer commentId, LocalDateTime createTime);
}