package com.example.dao;

import com.example.entity.ForumImage;

import java.util.List;

public interface ForumImageDao {
    List<ForumImage> queryAll();
    // 根据评论ID查询所有图片
    List<ForumImage> queryByCommentId(Integer commentId);
    // 新增图片
    int insert(ForumImage image);
    // 删除单张图片（按ID）
    int deleteById(Integer id);
    // 删除评论下的所有图片
    int deleteByCommentId(Integer commentId);
    // 更新图片关联的评论ID
    int updateCommentId(Integer imageId, Integer newCommentId);
    // 根据ID查询单张图片
    ForumImage queryById(Integer id);
}
