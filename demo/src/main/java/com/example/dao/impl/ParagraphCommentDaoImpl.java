package com.example.dao.impl;

import com.example.dao.ParagraphCommentDao;
import com.example.entity.ParagraphComment;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParagraphCommentDaoImpl implements ParagraphCommentDao {
    private static final Logger logger = LoggerFactory.getLogger(ParagraphCommentDaoImpl.class);

    @Override
    public void add(ParagraphComment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("评论对象不能为空");
        }
        try {
            String sql = "INSERT INTO paragraph_comment(paragraph_id, user_id, content, create_time) VALUES (?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    comment.getParagraphId(),
                    comment.getUserId(),
                    comment.getContent(),
                    comment.getCreateTime()
            );
            if (rows == 0) {
                throw new RuntimeException("新增段落评论失败，受影响行数0");
            }
        } catch (SQLException e) {
            logger.error("新增段落评论异常，段落ID:{}", comment.getParagraphId(), e);
            throw new RuntimeException("新增评论数据库异常", e);
        }
    }

    @Override
    public void update(ParagraphComment comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("评论对象/评论主键ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE paragraph_comment SET ");
            List<Object> params = new ArrayList<>();

            if (comment.getParagraphId() != null) {
                sql.append("paragraph_id=?, ");
                params.add(comment.getParagraphId());
            }
            if (comment.getUserId() != null) {
                sql.append("user_id=?, ");
                params.add(comment.getUserId());
            }
            if (comment.getContent() != null) {
                sql.append("content=?, ");
                params.add(comment.getContent());
            }
            if (comment.getCreateTime() != null) {
                sql.append("create_time=?, ");
                params.add(comment.getCreateTime());
            }
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(comment.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            if (rows == 0) {
                throw new RuntimeException("更新评论失败，无匹配ID：" + comment.getId());
            }
        } catch (SQLException e) {
            logger.error("更新段落评论异常，评论ID:{}", comment.getId(), e);
            throw new RuntimeException("更新评论数据库异常", e);
        }
    }

    @Override
    public void delete(Long commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论主键ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM paragraph_comment WHERE id=?", commentId);
            if (rows == 0) {
                throw new RuntimeException("删除评论失败，无匹配ID：" + commentId);
            }
        } catch (SQLException e) {
            logger.error("删除段落评论异常，评论ID:{}", commentId, e);
            throw new RuntimeException("删除评论数据库异常", e);
        }
    }

    @Override
    public List<ParagraphComment> queryById(Long commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论ID不能为空");
        }
        try {
            return DBUtil.executeQuery("SELECT * FROM paragraph_comment WHERE id=?", rs -> {
                ParagraphComment c = new ParagraphComment();
                c.setId(rs.getLong("id"));
                c.setParagraphId(rs.getString("paragraph_id"));
                c.setUserId(rs.getString("user_id"));
                c.setContent(rs.getString("content"));
                if (rs.getTimestamp("create_time") != null) {
                    c.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return c;
            }, commentId);
        } catch (SQLException e) {
            logger.error("根据评论ID查询异常，commentId:{}", commentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ParagraphComment> queryByUserIdPage(String userId, int offset, int pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            String sql = "SELECT * FROM paragraph_comment WHERE user_id=? ORDER BY create_time DESC LIMIT ?,?";
            return DBUtil.executeQuery(sql, rs -> {
                ParagraphComment c = new ParagraphComment();
                c.setId(rs.getLong("id"));
                c.setParagraphId(rs.getString("paragraph_id"));
                c.setUserId(rs.getString("user_id"));
                c.setContent(rs.getString("content"));
                if (rs.getTimestamp("create_time") != null) {
                    c.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return c;
            }, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户评论异常，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ParagraphComment> queryByParagraphIdPage(String paragraphId, int offset, int pageSize) {
        if (paragraphId == null || paragraphId.isEmpty()) {
            throw new IllegalArgumentException("段落ID不能为空");
        }
        // 使用联合索引 idx_paragraph_id_time
        String sql = "SELECT * FROM paragraph_comment WHERE paragraph_id=? AND status=1 ORDER BY create_time DESC LIMIT ?,?";
        try {
            return DBUtil.executeQuery(sql, rs -> {
                ParagraphComment c = new ParagraphComment();
                c.setId(rs.getLong("id"));
                c.setParagraphId(rs.getString("paragraph_id"));
                c.setUserId(rs.getString("user_id"));
                c.setContent(rs.getString("content"));
                if (rs.getTimestamp("create_time") != null) {
                    c.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return c;
            }, paragraphId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询段落评论异常，paragraphId:{}", paragraphId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) return 0L;
        try {
            return DBUtil.executeQuery("SELECT COUNT(*) total FROM paragraph_comment WHERE user_id=?", rs -> rs.getLong("total"), userId).get(0);
        } catch (SQLException e) {
            logger.error("统计用户评论总数异常，userId:{}", userId, e);
            return 0L;
        }
    }

    @Override
    public Long countByParagraphId(String paragraphId) {
        if (paragraphId == null || paragraphId.isEmpty()) return 0L;
        try {
            return DBUtil.executeQuery("SELECT COUNT(*) total FROM paragraph_comment WHERE paragraph_id=? AND status=1", rs -> rs.getLong("total"), paragraphId).get(0);
        } catch (SQLException e) {
            logger.error("统计段落评论总数异常，paragraphId:{}", paragraphId, e);
            return 0L;
        }
    }
}