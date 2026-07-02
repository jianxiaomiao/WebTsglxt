package com.example.dao;

import com.example.entity.ParagraphComment;
import java.util.List;

public interface ParagraphCommentDao {
    // 新增评论
    void add(ParagraphComment comment);
    // 更新评论
    void update(ParagraphComment comment);
    // 删除评论
    void delete(Long commentId);
    // 根据评论id查询（不分页）
    List<ParagraphComment> queryById(Long commentId);
    // 根据用户id分页查询评论
    List<ParagraphComment> queryByUserIdPage(String userId, int offset, int pageSize);
    // 根据段落id分页查询评论（按创建时间倒序）
    List<ParagraphComment> queryByParagraphIdPage(String paragraphId, int offset, int pageSize);
    // 统计用户评论总条数
    Long countByUserId(String userId);
    // 统计某段落评论总条数
    Long countByParagraphId(String paragraphId);
}