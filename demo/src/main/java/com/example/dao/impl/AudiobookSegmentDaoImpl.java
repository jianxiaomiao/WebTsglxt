package com.example.dao.impl;

import com.example.dao.AudiobookSegmentDao;
import com.example.entity.AudiobookSegment;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudiobookSegmentDaoImpl implements AudiobookSegmentDao {
    private static final Logger logger = LoggerFactory.getLogger(AudiobookSegmentDaoImpl.class);

    @Override
    public void batchInsert(List<AudiobookSegment> list) {
        if (list == null || list.isEmpty()) return;
        String sql = "INSERT INTO audiobook_segment (isbn, chapter_number, paragraph_id, sort_order, role_type, emotion, text_content, status) " +
                "VALUES (?,?,?,?,?,?,?,0)";
        try {
            DBUtil.executeTransaction(conn -> {
                for (AudiobookSegment seg : list) {
                    try {
                        DBUtil.executeUpdateWithConn(conn, sql,
                                seg.getIsbn(), seg.getChapterNumber(), seg.getParagraphId(),
                                seg.getSortOrder(), seg.getRoleType(), seg.getEmotion(),
                                seg.getTextContent());
                    } catch (SQLException e) {
                        throw new RuntimeException("批量插入有声书分段失败", e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("批量插入有声书分段异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateById(AudiobookSegment segment) {
        String sql = "UPDATE audiobook_segment SET audio_url=?, audio_duration=?, status=? WHERE id=?";
        try {
            DBUtil.executeUpdate(sql, segment.getAudioUrl(), segment.getAudioDuration(),
                    segment.getStatus(), segment.getId());
        } catch (SQLException e) {
            logger.error("更新有声书分段失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AudiobookSegment> queryByIsbnAndChapterPage(String isbn, Integer chapterNumber,
                                                              Integer pageNum, Integer pageSize) {
        if (isbn == null || isbn.isEmpty()) return Collections.emptyList();
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 1000) pageSize = 20;
        int offset = (pageNum - 1) * pageSize;

        String sql = "SELECT s.*, p.content AS paragraph_content, s.text_content " +
                "FROM audiobook_segment s " +
                "LEFT JOIN book_chapter_paragraph p ON s.paragraph_id = p.id " +
                "WHERE s.isbn=? AND s.chapter_number=? " +
                "ORDER BY s.sort_order ASC LIMIT ? OFFSET ?";
        try {
            return DBUtil.executeQuery(sql, this::buildSegment, isbn, chapterNumber, pageSize, offset);
        } catch (SQLException e) {
            logger.error("分页查询有声书分段失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int countGeneratedByChapter(String isbn, Integer chapterNumber) {
        String sql = "SELECT COUNT(*) FROM audiobook_segment WHERE isbn=? AND chapter_number=? AND status=1";
        try {
            return DBUtil.executeQueryScalar(sql, Integer.class, isbn, chapterNumber);
        } catch (SQLException e) {
            logger.error("统计已生成段数失败", e);
            return 0;
        }
    }

    @Override
    public int countTotalByChapter(String isbn, Integer chapterNumber) {
        String sql = "SELECT COUNT(*) FROM audiobook_segment WHERE isbn=? AND chapter_number=?";
        try {
            return DBUtil.executeQueryScalar(sql, Integer.class, isbn, chapterNumber);
        } catch (SQLException e) {
            logger.error("统计总段数失败", e);
            return 0;
        }
    }

    @Override
    public AudiobookSegment selectById(Long id) {
        if (id == null) return null;
        String sql = "SELECT s.*, p.content AS paragraph_content " +
                "FROM audiobook_segment s " +
                "LEFT JOIN book_chapter_paragraph p ON s.paragraph_id = p.id " +
                "WHERE s.id=?";
        try {
            List<AudiobookSegment> list = DBUtil.executeQuery(sql, this::buildSegment, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询有声书分段失败", e);
            return null;
        }
    }

    @Override
    public void deleteByChapter(String isbn, Integer chapterNumber) {
        String sql = "DELETE FROM audiobook_segment WHERE isbn=? AND chapter_number=?";
        try {
            DBUtil.executeUpdate(sql, isbn, chapterNumber);
        } catch (SQLException e) {
            logger.error("删除章节有声书分段失败", e);
            throw new RuntimeException(e);
        }
    }

    // ====================== 内部构建方法 ======================
    private AudiobookSegment buildSegment(ResultSet rs) throws SQLException {
        AudiobookSegment seg = new AudiobookSegment(
                rs.getLong("id"),
                rs.getString("isbn"),
                rs.getInt("chapter_number"),
                rs.getString("paragraph_id"),
                rs.getInt("sort_order"),
                rs.getString("role_type"),
                rs.getString("emotion"),
                rs.getString("audio_url"),
                rs.getObject("audio_duration") != null ? rs.getFloat("audio_duration") : null,
                rs.getInt("status"),
                rs.getObject("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null
        );
        try {
            seg.setParagraphContent(rs.getString("paragraph_content"));
        } catch (SQLException ignored) {}
        try {
            seg.setTextContent(rs.getString("text_content"));
        } catch (SQLException ignored) {}
        return seg;
    }
}
