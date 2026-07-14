package com.example.dao.impl;

import com.example.dao.BookBottleDao;
import com.example.dao.BookBottlePickDao;
import com.example.entity.BookBottle;
import com.example.entity.BookBottlePick;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookBottleDaoImpl implements BookBottleDao {
    private static final Logger logger = LoggerFactory.getLogger(BookBottleDaoImpl.class);
    private final BookBottlePickDao pickDao = new BookBottlePickDaoImpl();

    @Override
    public void add(BookBottle bottle) {
        if (bottle == null) throw new IllegalArgumentException("漂流瓶数据不能为空");
        try {
            int rows = DBUtil.executeUpdate(
                    "INSERT INTO book_bottle(userid,isbn,chapter,content,expire_time,allow_reply,status,is_deleted) VALUES (?,?,?,?,?,?,0,0)",
                    bottle.getUserid(), bottle.getIsbn(), bottle.getChapter(), bottle.getContent(),
                    bottle.getExpireTime(), bottle.getAllowReply()
            );
            if (rows == 0) throw new RuntimeException("新增漂流瓶失败，影响行数0");
        } catch (SQLException e) {
            logger.error("新增漂流瓶异常", e);
            throw new RuntimeException("新增漂流瓶数据库异常", e);
        }
    }

    @Override
    public void update(BookBottle bottle) {
        if (bottle == null || bottle.getId() == null) throw new IllegalArgumentException("漂流瓶ID不能为空");
        try {
            StringBuilder sql = new StringBuilder("UPDATE book_bottle SET ");
            List<Object> params = new ArrayList<>();

            if (bottle.getUserid() != null) { sql.append("userid=?,"); params.add(bottle.getUserid()); }
            if (bottle.getIsbn() != null) { sql.append("isbn=?,"); params.add(bottle.getIsbn()); }
            if (bottle.getChapter() != null) { sql.append("chapter=?,"); params.add(bottle.getChapter()); }
            if (bottle.getContent() != null) { sql.append("content=?,"); params.add(bottle.getContent()); }
            if (bottle.getStatus() != null) { sql.append("status=?,"); params.add(bottle.getStatus()); }
            if (bottle.getExpireTime() != null) { sql.append("expire_time=?,"); params.add(bottle.getExpireTime()); }
            if (bottle.getAllowReply() != null) { sql.append("allow_reply=?,"); params.add(bottle.getAllowReply()); }
            if (bottle.getIsDeleted() != null) { sql.append("is_deleted=?,"); params.add(bottle.getIsDeleted()); }

            sql.deleteCharAt(sql.length() - 1);
            sql.append(" WHERE id=?");
            params.add(bottle.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新漂流瓶 SQL:{} 参数:{}", sql, params);
            if (rows == 0) throw new RuntimeException("未匹配到漂流瓶ID：" + bottle.getId());
        } catch (SQLException e) {
            logger.error("更新漂流瓶异常，ID:{}", bottle.getId(), e);
            throw new RuntimeException("更新漂流瓶数据库异常", e);
        }
    }

    @Override
    public void del(Integer bottleId) {
        if (bottleId == null) throw new IllegalArgumentException("漂流瓶ID不能为空");
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM book_bottle WHERE id=?", bottleId);
            if (rows == 0) throw new RuntimeException("删除失败，无此ID漂流瓶");
        } catch (SQLException e) {
            logger.error("物理删除漂流瓶异常，ID:{}", bottleId, e);
            throw new RuntimeException("删除漂流瓶数据库异常", e);
        }
    }

    // 公共结果集解析：仅保留发布人昵称联表
    private BookBottle parseBottle(java.sql.ResultSet rs) throws SQLException {
        BookBottle bottle = new BookBottle();
        bottle.setId(rs.getInt("id"));
        bottle.setUserid(rs.getString("userid"));
        bottle.setIsbn(rs.getString("isbn"));
        bottle.setChapter(rs.getString("chapter"));
        bottle.setContent(rs.getString("content"));
        bottle.setCreatetime(rs.getTimestamp("createtime") == null ? null : rs.getTimestamp("createtime").toLocalDateTime());
        bottle.setUpdateTime(rs.getTimestamp("update_time") == null ? null : rs.getTimestamp("update_time").toLocalDateTime());
        bottle.setStatus(rs.getInt("status"));
        bottle.setExpireTime(rs.getTimestamp("expire_time") == null ? null : rs.getTimestamp("expire_time").toLocalDateTime());
        bottle.setAllowReply(rs.getInt("allow_reply"));
        bottle.setIsDeleted(rs.getInt("is_deleted"));
        bottle.setUsername(rs.getString("username"));
        return bottle;
    }

    // 批量填充瓶子对应的捞取记录列表
    private void fillPickList(List<BookBottle> bottleList) {
        if (bottleList == null || bottleList.isEmpty()) return;
        Map<Integer, List<BookBottlePick>> pickMap = bottleList.stream()
                .map(BookBottle::getId)
                .collect(Collectors.toMap(id -> id, pickDao::queryByBottleId));
        for (BookBottle bottle : bottleList) {
            bottle.setPickList(pickMap.get(bottle.getId()));
        }
    }

    @Override
    public List<BookBottle> queryById(Integer bottleId) {
        if (bottleId == null) throw new IllegalArgumentException("漂流瓶ID不能为空");
        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.id = ?";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, bottleId);
            fillPickList(list);
            return list;
        } catch (SQLException e) {
            logger.error("按ID查询漂流瓶异常，ID:{}", bottleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookBottle> queryByIsbnPage(String isbn, int offset, int pageSize) {
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN不能为空");
        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.isbn=? AND b.status=0 AND b.is_deleted=0 AND b.expire_time > NOW() AND b.audit_status=1 " +
                "ORDER BY b.createtime DESC LIMIT ?,?";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, isbn, offset, pageSize);
            fillPickList(list);
            return list;
        } catch (SQLException e) {
            logger.error("ISBN分页查询漂流瓶异常，ISBN:{}", isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return 0L;
        String sql = "SELECT COUNT(*) total FROM book_bottle WHERE isbn=? AND status=0 AND is_deleted=0 AND expire_time > NOW() AND audit_status=1";
        try {
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), isbn);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计ISBN漂流瓶总数异常", e);
            return 0L;
        }
    }

    @Override
    public List<BookBottle> queryByChapterPage(String chapter, int offset, int pageSize) {
        if (chapter == null || chapter.isBlank()) throw new IllegalArgumentException("章节不能为空");
        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.chapter=? AND b.status=0 AND b.is_deleted=0 AND b.expire_time > NOW() AND b.audit_status=1 " +
                "ORDER BY b.createtime DESC LIMIT ?,?";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, chapter, offset, pageSize);
            fillPickList(list);
            return list;
        } catch (SQLException e) {
            logger.error("章节分页查询漂流瓶异常，章节:{}", chapter, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByChapter(String chapter) {
        if (chapter == null || chapter.isBlank()) return 0L;
        String sql = "SELECT COUNT(*) total FROM book_bottle WHERE chapter=? AND status=0 AND is_deleted=0 AND expire_time > NOW() AND audit_status=1";
        try {
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), chapter);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计章节漂流瓶总数异常", e);
            return 0L;
        }
    }

    @Override
    public List<BookBottle> queryByUserId(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("用户ID不能为空");
        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.userid=? ORDER BY b.createtime DESC";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, userId);
            fillPickList(list);
            return list;
        } catch (SQLException e) {
            logger.error("查询用户发布漂流瓶异常，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 【核心修改】随机捞取漂流瓶
     * 过滤规则：未删除、未过期、不是自己发布的、当前用户未捞取过
     */
    @Override
    public List<BookBottle> getRandomBottle(String isbn, String loginUserId) {
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN不能为空");
        if (loginUserId == null || loginUserId.isBlank()) throw new IllegalArgumentException("登录用户ID不能为空");

        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.isbn=? " +
                "  AND b.status=0 " +
                "  AND b.is_deleted=0 " +
                "  AND b.expire_time > NOW() AND b.audit_status=1 " +
                "  AND b.userid != ? " +
                "  AND NOT EXISTS (SELECT 1 FROM book_bottle_pick p WHERE p.bottle_id = b.id AND p.userid = ?) " +
                "ORDER BY RAND() LIMIT 1";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, isbn, loginUserId, loginUserId);
            fillPickList(list);
            return list;
        } catch (SQLException e) {
            logger.error("随机捞取漂流瓶异常，ISBN:{}, userId:{}", isbn, loginUserId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 分页查询可捞取的漂流瓶
     * 过滤条件：未删除、未过期、不是自己发的、当前用户未捞取过
     */
    @Override
    public List<BookBottle> queryAvailableBottleByIsbnPage(String isbn, String loginUserId, int offset, int pageSize) {
        if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN不能为空");
        if (loginUserId == null || loginUserId.isBlank()) throw new IllegalArgumentException("用户ID不能为空");

        String sql = "SELECT b.*, u1.Name username " +
                "FROM book_bottle b " +
                "LEFT JOIN user_information u1 ON b.userid = u1.UserId " +
                "WHERE b.isbn = ? " +
                "  AND b.status = 0 " +
                "  AND b.is_deleted = 0 " +
                "  AND b.expire_time > NOW() AND b.audit_status=1 " +
                "  AND b.userid != ? " +
                "  AND NOT EXISTS (SELECT 1 FROM book_bottle_pick p WHERE p.bottle_id = b.id AND p.userid = ?) " +
                "ORDER BY b.createtime DESC LIMIT ?,?";
        try {
            List<BookBottle> list = DBUtil.executeQuery(sql, this::parseBottle, isbn, loginUserId, loginUserId, offset, pageSize);
            fillPickList(list); // 自动填充捞取记录列表
            return list;
        } catch (SQLException e) {
            logger.error("分页查询可捞漂流瓶异常，ISBN:{}, userId:{}", isbn, loginUserId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 统计可捞取的漂流瓶总数
     */
    @Override
    public Long countAvailableByIsbn(String isbn, String loginUserId) {
        if (isbn == null || isbn.isBlank()) return 0L;
        if (loginUserId == null || loginUserId.isBlank()) return 0L;

        String sql = "SELECT COUNT(*) total FROM book_bottle b " +
                "WHERE b.isbn = ? " +
                "  AND b.status = 0 " +
                "  AND b.is_deleted = 0 " +
                "  AND b.expire_time > NOW() AND b.audit_status=1 " +
                "  AND b.userid != ? " +
                "  AND NOT EXISTS (SELECT 1 FROM book_bottle_pick p WHERE p.bottle_id = b.id AND p.userid = ?)";
        try {
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), isbn, loginUserId, loginUserId);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计可捞漂流瓶总数异常，ISBN:{}, userId:{}", isbn, loginUserId, e);
            return 0L;
        }
    }

    @Override
    public List<Integer> queryAllActiveBottleIds(String isbn) {
        if (isbn == null || isbn.isBlank()) return Collections.emptyList();
        String sql = "SELECT id FROM book_bottle WHERE isbn=? AND status=0 AND is_deleted=0 AND expire_time > NOW() AND audit_status=1";
        try {
            return DBUtil.executeQuery(sql, rs -> rs.getInt("id"), isbn);
        } catch (SQLException e) {
            logger.error("查询可捞取漂流瓶ID列表异常，ISBN:{}", isbn, e);
            return Collections.emptyList();
        }
    }
}