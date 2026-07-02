package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserComment;
import java.util.List;
import java.util.Map;

public interface UserCommentService {
    // 查询所有论坛评论
    ResultDTO<List<UserComment>> queryAllUserComments();
    // 按用户ID查询论坛评论
    ResultDTO<List<UserComment>> queryUserCommentByUserId(String userId);

    ResultDTO<PageResultDTO<UserComment>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    ResultDTO<List<UserComment>> queryUserCommentByType(String sortType); // 新增
    // 新增论坛评论
    ResultDTO<Integer> addUserComment(UserComment comment);
    // 删除论坛评论（按评论ID）
    ResultDTO<Void> deleteUserComment(int commentId);
    // 更新论坛评论
    ResultDTO<Void> updateUserComment(UserComment comment);
    // 🔥 【新增】获取树形结构的评论（主评论带子评论列表）
    ResultDTO<Map<String, List<UserComment>>> getCommentTree(String sortType);
    // 🔥 新增：根据评论ID查询单条评论
    ResultDTO<List<UserComment>> queryByCommentId(Integer commentId);

    // ====================== 新增：分页获取主评论列表（带子评论总数） ======================
    ResultDTO<PageResultDTO<Map<String, Object>>> getMainCommentsByPage(Integer pageNum, Integer pageSize, String sortType);

    // ====================== 新增：分页获取指定主评论下的子评论 ======================
    ResultDTO<PageResultDTO<UserComment>> getSubCommentsByPage(Integer parentId, Integer pageNum, Integer pageSize, String sortType);

    // ====================== 新增：获取单个主评论的子评论总数 ======================
    ResultDTO<Long> getSubCommentCount(Integer parentId);
}
