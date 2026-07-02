package com.example.dao.impl;

import com.example.dao.BookChapterParagraphDao;
import com.example.entity.BookChapterParagraph;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookChapterParagraphDaoImpl implements BookChapterParagraphDao {
    private static final Logger logger = LoggerFactory.getLogger(BookChapterParagraphDaoImpl.class);

    @Override
    public void add(BookChapterParagraph paragraph) {
        if (paragraph == null) {
            throw new IllegalArgumentException("段落信息不能为空");
        }
        try {
            String sql = "INSERT INTO book_chapter_paragraph(id, isbn, chapter_id, number, content, comment_count, create_time) VALUES (?,?,?,?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    paragraph.getId(),
                    paragraph.getIsbn(),
                    paragraph.getChapterId(),
                    paragraph.getNumber(),
                    paragraph.getContent(),
                    paragraph.getCommentCount(),
                    paragraph.getCreateTime()
            );
            if (rows == 0) {
                throw new RuntimeException("新增段落失败，受影响行数0");
            }
        } catch (SQLException e) {
            logger.error("新增段落异常，段落ID:{}", paragraph.getId(), e);
            throw new RuntimeException("新增段落数据库异常", e);
        }
    }

    @Override
    public void update(BookChapterParagraph paragraph) {
        if (paragraph == null || paragraph.getId() == null) {
            throw new IllegalArgumentException("段落对象/段落ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE book_chapter_paragraph SET ");
            List<Object> params = new ArrayList<>();

            if (paragraph.getIsbn() != null) {
                sql.append("isbn=?, ");
                params.add(paragraph.getIsbn());
            }
            if (paragraph.getChapterId() != null) {
                sql.append("chapter_id=?, ");
                params.add(paragraph.getChapterId());
            }
            if (paragraph.getNumber() != null) {
                sql.append("number=?, ");
                params.add(paragraph.getNumber());
            }
            if (paragraph.getContent() != null) {
                sql.append("content=?, ");
                params.add(paragraph.getContent());
            }
            if (paragraph.getCommentCount() != null) {
                sql.append("comment_count=?, ");
                params.add(paragraph.getCommentCount());
            }
            if (paragraph.getCreateTime() != null) {
                sql.append("create_time=?, ");
                params.add(paragraph.getCreateTime());
            }
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(paragraph.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            if (rows == 0) {
                throw new RuntimeException("更新段落失败，无匹配ID：" + paragraph.getId());
            }
        } catch (SQLException e) {
            logger.error("更新段落异常，段落ID:{}", paragraph.getId(), e);
            throw new RuntimeException("更新段落数据库异常", e);
        }
    }

    @Override
    public void delete(String paragraphId) {
        if (paragraphId == null || paragraphId.isEmpty()) {
            throw new IllegalArgumentException("段落ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM book_chapter_paragraph WHERE id=?", paragraphId);
            if (rows == 0) {
                throw new RuntimeException("删除段落失败，无匹配ID：" + paragraphId);
            }
        } catch (SQLException e) {
            logger.error("删除段落异常，段落ID:{}", paragraphId, e);
            throw new RuntimeException("删除段落数据库异常", e);
        }
    }

    @Override
    public List<BookChapterParagraph> queryById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("段落ID不能为空");
        }
        try {
            return DBUtil.executeQuery("SELECT * FROM book_chapter_paragraph WHERE id=?", rs -> {
                BookChapterParagraph p = new BookChapterParagraph();
                p.setId(rs.getString("id"));
                p.setIsbn(rs.getString("isbn"));
                p.setChapterId(rs.getString("chapter_id"));
                p.setNumber(rs.getInt("number"));
                p.setContent(rs.getString("content"));
                p.setCommentCount(rs.getInt("comment_count"));
                if (rs.getTimestamp("create_time") != null) {
                    p.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return p;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询段落异常，ID:{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookChapterParagraph> queryByChapterId(String chapterId) {
        if (chapterId == null || chapterId.isEmpty()) {
            throw new IllegalArgumentException("章节ID不能为空");
        }
        try {
            return DBUtil.executeQuery("SELECT * FROM book_chapter_paragraph WHERE chapter_id=? ORDER BY number ASC", rs -> {
                BookChapterParagraph p = new BookChapterParagraph();
                p.setId(rs.getString("id"));
                p.setIsbn(rs.getString("isbn"));
                p.setChapterId(rs.getString("chapter_id"));
                p.setNumber(rs.getInt("number"));
                p.setContent(rs.getString("content"));
                p.setCommentCount(rs.getInt("comment_count"));
                if (rs.getTimestamp("create_time") != null) {
                    p.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return p;
            }, chapterId);
        } catch (SQLException e) {
            logger.error("根据章节ID查询段落异常，chapterId:{}", chapterId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateCommentCount(String paragraphId, int delta) {
        if (paragraphId == null || paragraphId.isEmpty()) {
            throw new IllegalArgumentException("段落ID不能为空");
        }
        try {
            String sql = "UPDATE book_chapter_paragraph SET comment_count = comment_count + ? WHERE id=?";
            DBUtil.executeUpdate(sql, delta, paragraphId);
        } catch (SQLException e) {
            logger.error("更新段落评论计数失败，段落ID:{},变化量:{}", paragraphId, delta, e);
            throw new RuntimeException("更新评论计数异常", e);
        }
    }
}