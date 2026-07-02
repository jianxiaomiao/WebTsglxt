package com.example.dao.impl;

import com.example.dao.BookBottlePickDao;
import com.example.entity.BookBottlePick;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class BookBottlePickDaoImpl implements BookBottlePickDao {
    private static final Logger logger = LoggerFactory.getLogger(BookBottlePickDaoImpl.class);

    // 公共结果集解析方法
    private BookBottlePick parsePick(java.sql.ResultSet rs) throws SQLException {
        BookBottlePick pick = new BookBottlePick();
        pick.setId(rs.getInt("id"));
        pick.setUserid(rs.getString("userid"));
        pick.setBottleId(rs.getInt("bottle_id"));
        pick.setReplycontent(rs.getString("replycontent"));
        pick.setCreatetime(rs.getTimestamp("createtime") == null ? null : rs.getTimestamp("createtime").toLocalDateTime());
        // 兼容联表查询的用户昵称
        try {
            pick.setUsername(rs.getString("username"));
        } catch (SQLException ignored) {}
        return pick;
    }

    @Override
    public void addPick(BookBottlePick pick) {
        if (pick == null) throw new IllegalArgumentException("捞取记录不能为空");
        try {
            int rows = DBUtil.executeUpdate(
                    "INSERT INTO book_bottle_pick(userid,bottle_id,replycontent,createtime) VALUES (?,?,?,?)",
                    pick.getUserid(), pick.getBottleId(), pick.getReplycontent(), pick.getCreatetime()
            );
            if (rows == 0) throw new RuntimeException("新增捞取记录失败，影响行数0");
        } catch (SQLException e) {
            logger.error("新增捞取记录异常", e);
            throw new RuntimeException("新增捞取记录数据库异常", e);
        }
    }

    @Override
    public boolean checkUserPicked(String userId, Integer bottleId) {
        if (userId == null || bottleId == null) return false;
        String sql = "SELECT COUNT(*) total FROM book_bottle_pick WHERE userid=? AND bottle_id=?";
        try {
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId, bottleId);
            return !res.isEmpty() && res.get(0) > 0;
        } catch (SQLException e) {
            logger.error("检查用户捞取状态异常，userId:{}, bottleId:{}", userId, bottleId, e);
            return false;
        }
    }

    @Override
    public List<BookBottlePick> queryByBottleId(Integer bottleId) {
        if (bottleId == null) return Collections.emptyList();
        String sql = "SELECT p.*, u.Name username " +
                "FROM book_bottle_pick p " +
                "LEFT JOIN user_information u ON p.userid = u.UserId " +
                "WHERE p.bottle_id=? ORDER BY p.createtime DESC";
        try {
            return DBUtil.executeQuery(sql, this::parsePick, bottleId);
        } catch (SQLException e) {
            logger.error("按瓶子ID查询捞取记录异常，bottleId:{}", bottleId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookBottlePick> queryByUserId(String userId) {
        if (userId == null || userId.isBlank()) return Collections.emptyList();
        String sql = "SELECT p.*, u.Name username " +
                "FROM book_bottle_pick p " +
                "LEFT JOIN user_information u ON p.userid = u.UserId " +
                "WHERE p.userid=? ORDER BY p.createtime DESC";
        try {
            return DBUtil.executeQuery(sql, this::parsePick, userId);
        } catch (SQLException e) {
            logger.error("按用户ID查询捞取记录异常，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateReply(Integer pickId, String replycontent) {
        if (pickId == null) throw new IllegalArgumentException("捞取记录ID不能为空");
        try {
            int rows = DBUtil.executeUpdate(
                    "UPDATE book_bottle_pick SET replycontent=? WHERE id=?",
                    replycontent, pickId
            );
            if (rows == 0) throw new RuntimeException("更新回复失败，无此记录ID：" + pickId);
        } catch (SQLException e) {
            logger.error("更新捞取回复异常，pickId:{}", pickId, e);
            throw new RuntimeException("更新捞取回复数据库异常", e);
        }
    }
}