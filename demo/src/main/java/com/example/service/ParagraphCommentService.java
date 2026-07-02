package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.ParagraphComment;
import java.util.List;

public interface ParagraphCommentService {
    // 新增评论（自动+1段落comment_count）
    ResultDTO<Void> addComment(ParagraphComment comment);
    // 更新评论（不改变段落计数）
    ResultDTO<Void> updateComment(ParagraphComment comment);
    // 删除评论（自动-1段落comment_count）
    ResultDTO<Void> deleteComment(Long commentId);
    // 根据评论ID查询（不分页）
    ResultDTO<List<ParagraphComment>> queryCommentById(Long commentId);
    // 根据用户ID分页查询
    ResultDTO<PageResultDTO<ParagraphComment>> queryCommentByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    // 根据段落ID分页查询
    ResultDTO<PageResultDTO<ParagraphComment>> queryCommentByParagraphIdPage(String paragraphId, Integer pageNum, Integer pageSize);
}