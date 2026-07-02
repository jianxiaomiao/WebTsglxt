package com.example.dao.impl;

import com.example.dao.ForumImageDao;
import com.example.entity.ForumImage;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ForumImageDaoImpl implements ForumImageDao {
    private static final Logger logger = LoggerFactory.getLogger(ForumImageDaoImpl.class);
    @Override
    public List<ForumImage> queryAll(){
        try {
            return DBUtil.executeQuery(
                    "select * from forum_image ",
                    rs -> {
                        ForumImage image = new ForumImage();
                        image.setId(rs.getInt("id"));
                        image.setCommentId(rs.getInt("comment_id"));
                        image.setImageUrl(rs.getString("image_url"));
                        image.setImageType(rs.getString("image_type"));
                        image.setUploadTime(rs.getTimestamp("upload_time").toLocalDateTime());
                        return image;
                    });
        } catch (SQLException e) {
            logger.error("查询所有评论图片异常", e);
            return Collections.emptyList();
        }
    }
    @Override
    public List<ForumImage> queryByCommentId(Integer commentId) {
        if (commentId == null) return Collections.emptyList();
        try {
            return DBUtil.executeQuery(
                    "select id, comment_id, image_url, image_type, upload_time from forum_image where comment_id=?",
                    rs -> {
                        ForumImage image = new ForumImage();
                        image.setId(rs.getInt("id"));
                        image.setCommentId(rs.getInt("comment_id"));
                        image.setImageUrl(rs.getString("image_url"));
                        image.setImageType(rs.getString("image_type"));
                        image.setUploadTime(rs.getTimestamp("upload_time").toLocalDateTime());
                        return image;
                    }, commentId);
        } catch (SQLException e) {
            logger.error("查询评论图片异常，评论ID：{}", commentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public int insert(ForumImage image) {
        if (image == null || image.getImageUrl() == null) {
            throw new IllegalArgumentException("图片参数不能为空");
        }
        try {
            // 🔥 新增：获取当前时间，设置到image对象里
            image.setUploadTime(java.time.LocalDateTime.now());

            // 🔥 修改SQL，加上upload_time字段
            Integer imageId = DBUtil.executeUpdateReturnId(
                    "insert into forum_image(comment_id, image_url, image_type, upload_time) values(?, ?, ?, ?)",
                    image.getCommentId(),
                    image.getImageUrl(),
                    image.getImageType(),
                    image.getUploadTime() // 传入当前时间
            );
            // 🔥 把自增ID回写到image对象里，返回给前端
            image.setId(imageId);
            return  1;
        } catch (SQLException e) {
            logger.error("新增评论图片异常", e);
            return 0;
        }
    }

    @Override
    public int deleteById(Integer id) {
        if (id == null) return 0;
        try {
            return DBUtil.executeUpdate("delete from forum_image where id=?", id);
        } catch (SQLException e) {
            logger.error("删除单张图片异常，图片ID：{}", id, e);
            return 0;
        }
    }

    @Override
    public int deleteByCommentId(Integer commentId) {
        if (commentId == null) return 0;
        try {
            return DBUtil.executeUpdate("delete from forum_image where comment_id=?", commentId);
        } catch (SQLException e) {
            logger.error("删除评论下所有图片异常，评论ID：{}", commentId, e);
            return 0;
        }
    }

    @Override
    public int updateCommentId(Integer imageId, Integer newCommentId) {
        try {
            return DBUtil.executeUpdate(
                    "update forum_image set comment_id=? where id=?",
                    newCommentId, imageId
            );
        } catch (Exception e) {
            logger.error("更新图片评论ID异常", e);
            return 0;
        }
    }

    @Override
    public ForumImage queryById(Integer id) {
        try {
            List<ForumImage> list = DBUtil.executeQuery(
                    "select * from forum_image where id=?",
                    rs -> {
                        ForumImage image = new ForumImage();
                        image.setId(rs.getInt("id"));
                        image.setCommentId(rs.getObject("comment_id", Integer.class));
                        image.setImageUrl(rs.getString("image_url"));
                        image.setImageType(rs.getString("image_type"));
                        // 处理时间（如果为null则不设置）
                        java.sql.Timestamp ts = rs.getTimestamp("upload_time");
                        if (ts != null) {
                            image.setUploadTime(ts.toLocalDateTime());
                        }
                        return image;
                    },
                    id
            );
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询图片失败，id={}", id, e);
            return null;
        }
    }
}

