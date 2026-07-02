package com.example.dao.impl;

import com.example.dao.BookmarkDao;
import com.example.entity.Bookmark;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookmarkDaoImpl implements BookmarkDao {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkDaoImpl.class);

    // 新增书签
    @Override
    public void add(Bookmark bookmark) {
        if (bookmark == null || bookmark.getUserId() == null || bookmark.getIsbn() == null) {
            throw new IllegalArgumentException("书签信息/用户ID/ISBN不能为空");
        }
        try {
            String sql = "insert into user_bookmark(user_id, isbn, chapter_number, read_progress, create_time) values (?, ?, ?, ?, ?)";
            int affectedRows = DBUtil.executeUpdate(sql,
                    bookmark.getUserId(),
                    bookmark.getIsbn(),
                    bookmark.getChapterNumber(),
                    bookmark.getReadProgress(),
                    bookmark.getCreateTime()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增书签失败");
            }
        } catch (SQLException e) {
            logger.error("新增书签异常", e);
            throw new RuntimeException("新增书签异常", e);
        }
    }

    // 修改书签
    @Override
    public void update(Bookmark bookmark) {
        if (bookmark == null || bookmark.getUserId() == null || bookmark.getIsbn() == null) {
            throw new IllegalArgumentException("书签信息不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_bookmark set ");
            List<Object> params = new ArrayList<>();

            if (bookmark.getReadProgress() != null) {
                sql.append("read_progress=?, ");
                params.add(bookmark.getReadProgress());
            }
            if (bookmark.getChapterNumber() != null) {
                sql.append("chapter_number=?, ");
                params.add(bookmark.getChapterNumber());
            }
            // 移除最后一个逗号
            sql.deleteCharAt(sql.length() - 2);
            // 联合唯一条件：用户ID + ISBN + 章节号
            sql.append(" where user_id=? and isbn=? and chapter_number=?");
            params.add(bookmark.getUserId());
            params.add(bookmark.getIsbn());
            params.add(bookmark.getChapterNumber());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            if (affectedRows == 0) {
                throw new RuntimeException("更新书签失败，未找到匹配数据");
            }
        } catch (SQLException e) {
            logger.error("更新书签异常", e);
            throw new RuntimeException("更新书签异常", e);
        }
    }
    @Override
    public void delById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("书签ID不能为空");
        }
        try {
            String sql = "delete from user_bookmark where id=?";
            DBUtil.executeUpdate(sql, id);
        } catch (SQLException e) {
            logger.error("根据ID删除书签异常", e);
            throw new RuntimeException("删除异常", e);
        }
    }

    @Override
    public void updateById(Bookmark bookmark) {
        if (bookmark == null || bookmark.getId() == null) {
            throw new IllegalArgumentException("书签ID不能为空");
        }
        try {
            String sql = "update user_bookmark set read_progress=?,chapter_number=? where id=?";
            DBUtil.executeUpdate(sql, bookmark.getReadProgress(), bookmark.getChapterNumber(), bookmark.getId());
        } catch (SQLException e) {
            logger.error("根据ID更新书签异常", e);
            throw new RuntimeException("更新异常", e);
        }
    }
    // 删除书签
    @Override
    public void del(String userId, String isbn, String chapterNumber) {
        if (userId == null || isbn == null || chapterNumber == null) {
            throw new IllegalArgumentException("删除参数不能为空");
        }
        try {
            String sql = "delete from user_bookmark where user_id=? and isbn=? and chapter_number=?";
            int affectedRows = DBUtil.executeUpdate(sql, userId, isbn, chapterNumber);
            if (affectedRows == 0) {
                throw new RuntimeException("删除书签失败，未找到匹配数据");
            }
        } catch (SQLException e) {
            logger.error("删除书签异常", e);
            throw new RuntimeException("删除书签异常", e);
        }
    }

    // =============== 查询方法（统一按创建时间倒序） ===============
    // 根据 ISBN 查询
    @Override
    public List<Bookmark> queryByIsbn(String isbn) {
        if (isbn == null) return Collections.emptyList();
        try {
            String sql = "SELECT * FROM user_bookmark WHERE isbn=? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, this::mapRow, isbn);
        } catch (SQLException e) {
            logger.error("根据ISBN查询书签异常", e);
            return Collections.emptyList();
        }
    }

    // 根据 用户ID 查询
    @Override
    public List<Bookmark> queryByUserId(String userId) {
        if (userId == null) return Collections.emptyList();
        try {
            String sql = "SELECT * FROM user_bookmark WHERE user_id=? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, this::mapRow, userId);
        } catch (SQLException e) {
            logger.error("根据用户ID查询书签异常", e);
            return Collections.emptyList();
        }
    }

    // 根据 章节号 查询
    @Override
    public List<Bookmark> queryByChapterNumber(String chapterNumber) {
        if (chapterNumber == null) return Collections.emptyList();
        try {
            String sql = "SELECT * FROM user_bookmark WHERE chapter_number=? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, this::mapRow, chapterNumber);
        } catch (SQLException e) {
            logger.error("根据章节号查询书签异常", e);
            return Collections.emptyList();
        }
    }

    // 根据 用户ID + ISBN 查询
    @Override
    public List<Bookmark> queryByUserIdAndIsbn(String userId, String isbn) {
        if (userId == null || isbn == null) return Collections.emptyList();
        try {
            String sql = "SELECT * FROM user_bookmark WHERE user_id=? AND isbn=? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, this::mapRow, userId, isbn);
        } catch (SQLException e) {
            logger.error("根据用户ID+ISBN查询书签异常", e);
            return Collections.emptyList();
        }
    }

    // 结果集映射（新增自增ID解析）
    private Bookmark mapRow(java.sql.ResultSet rs) throws SQLException {
        // 🔥 读取自增主键
        Long id = rs.getLong("id");
        String userId = rs.getString("user_id");
        String isbn = rs.getString("isbn");
        String chapterNumber = rs.getString("chapter_number");
        Integer readProgress = rs.getInt("read_progress");
        if (rs.wasNull()) readProgress = null;
        LocalDateTime createTime = rs.getTimestamp("create_time").toLocalDateTime();

        // 🔥 传入自增ID
        return new Bookmark(id, userId, isbn, chapterNumber, readProgress, createTime);
    }

    /**
     * 统计用户指定时间段内的书签数量
     */
    @Override
    public Integer countBookmarkByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            return 0;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_bookmark WHERE user_id=? AND create_time BETWEEN ? AND ?";
            List<Integer> result = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), userId, startTime, endTime);
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户指定时间段书签数量异常", e);
            return 0;
        }
    }

    /**
     * 查询用户指定时间段内的书签列表
     */
    @Override
    public List<Bookmark> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            return Collections.emptyList();
        }
        try {
            String sql = "SELECT * FROM user_bookmark WHERE user_id=? AND create_time BETWEEN ? AND ? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, this::mapRow, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户指定时间段书签列表异常", e);
            return Collections.emptyList();
        }
    }
}