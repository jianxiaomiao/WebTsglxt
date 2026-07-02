package com.example.dao.impl;

import com.example.dao.BookReadRoomDao;
import com.example.entity.BookReadRoom;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookReadRoomDaoImpl implements BookReadRoomDao {
    private static final Logger logger = LoggerFactory.getLogger(BookReadRoomDaoImpl.class);

    // 联表基础SQL：房间+书籍名+创建者用户名
    private static final String JOIN_BASE_SQL = "SELECT r.*, b.Bookname, u.Name " +
            "FROM book_read_room r " +
            "LEFT JOIN book_information b ON r.isbn = b.ISBN " +
            "LEFT JOIN user_information u ON r.user_id = u.UserId ";

    @Override
    public BookReadRoom add(BookReadRoom room) {
        if (room == null) {
            throw new IllegalArgumentException("共读房间数据不能为空");
        }
        try {
            String insertSql = "INSERT INTO book_read_room(is_public, user_id, isbn) VALUES (?, ?, ?)";
            // 获取数据库生成的自增id
            Integer newRoomId = DBUtil.executeUpdateReturnId(
                    insertSql,
                    room.getIsPublic(), room.getUserId(), room.getIsbn()
            );
            // 把自增ID回填到实体
            room.setId(newRoomId);
            return room;
        } catch (SQLException e) {
            logger.error("新增共读房间异常", e);
            throw new RuntimeException("新增共读房间数据库异常", e);
        }
    }

    @Override
    public void update(BookReadRoom room) {
        if (room == null || room.getId() == null) {
            throw new IllegalArgumentException("房间对象/房间id不能为空");
        }
        StringBuilder sql = new StringBuilder("UPDATE book_read_room SET ");
        List<Object> params = new ArrayList<>();

        // 动态拼接非空字段
        if (room.getIsPublic() != null) {
            sql.append("is_public=?, ");
            params.add(room.getIsPublic());
        }
        if (room.getUserId() != null) {
            sql.append("user_id=?, ");
            params.add(room.getUserId());
        }
        if (room.getIsbn() != null) {
            sql.append("isbn=?, ");
            params.add(room.getIsbn());
        }
        // 移除末尾逗号空格
        sql.deleteCharAt(sql.length() - 2);
        sql.append(" WHERE id=?");
        params.add(room.getId());

        try {
            int row = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新房间SQL: {} 参数:{}", sql, params);
            if (row == 0) {
                throw new RuntimeException("更新失败，未匹配房间id：" + room.getId());
            }
        } catch (SQLException e) {
            logger.error("更新房间异常，id:{}", room.getId(), e);
            throw new RuntimeException("更新共读房间异常", e);
        }
    }

    @Override
    public void del(Integer roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("房间id不能为空");
        }
        try {
            int row = DBUtil.executeUpdate("DELETE FROM book_read_room WHERE id = ?", roomId);
            if (row == 0) {
                throw new RuntimeException("删除失败，无该房间id：" + roomId);
            }
        } catch (SQLException e) {
            logger.error("删除房间异常，id:{}", roomId, e);
            throw new RuntimeException("删除共读房间异常", e);
        }
    }

    // 【改动1：查询全部房间，只展示公开房间】
    @Override
    public List<BookReadRoom> queryAll() {
        // 新增 WHERE r.is_public = 1 过滤私密房间
        String sql = JOIN_BASE_SQL + " WHERE r.is_public = 1 ORDER BY r.create_time DESC";
        try {
            return DBUtil.executeQuery(sql, this::convertRoom);
        } catch (SQLException e) {
            logger.error("查询全部房间异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookReadRoom> queryById(Integer roomId) {
        String sql = JOIN_BASE_SQL + " WHERE r.id = ?";
        try {
            return DBUtil.executeQuery(sql, this::convertRoom, roomId);
        } catch (SQLException e) {
            logger.error("根据房间id查询异常，id:{}", roomId, e);
            return Collections.emptyList();
        }
    }

    // 本人创建房间，公开私密全部返回，无需过滤is_public
    @Override
    public List<BookReadRoom> queryByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        String sql = JOIN_BASE_SQL + " WHERE r.user_id = ? ORDER BY r.create_time DESC";
        try {
            return DBUtil.executeQuery(sql, this::convertRoom, userId);
        } catch (SQLException e) {
            logger.error("查询用户创建房间异常，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    // 【改动2：书名全网模糊搜索，仅展示公开房间】
    @Override
    public List<BookReadRoom> queryByBookNameLike(String bookName) {
        if (bookName == null || bookName.isBlank()) {
            throw new IllegalArgumentException("书名关键字不能为空");
        }
        // 新增 r.is_public = 1 限制，私密房间不参与书名检索
        String sql = JOIN_BASE_SQL + " WHERE r.is_public = 1 AND b.Bookname LIKE CONCAT('%', ?, '%') ORDER BY r.create_time DESC";
        try {
            return DBUtil.executeQuery(sql, this::convertRoom, bookName);
        } catch (SQLException e) {
            logger.error("书名模糊查询房间异常，关键字:{}", bookName, e);
            return Collections.emptyList();
        }
    }

    // 【改动3：分页查询，区分是否传入userId过滤私密房间】
    @Override
    public List<BookReadRoom> queryPage(String userId, String bookName, int offset, int pageSize) {
        StringBuilder sql = new StringBuilder(JOIN_BASE_SQL);
        List<Object> args = new ArrayList<>();
        boolean hasWhere = false;

        // 1. 传入userId：查询本人所有房间（公开+私密，不限制is_public）
        if (userId != null && !userId.isBlank()) {
            sql.append(" WHERE r.user_id = ?");
            args.add(userId);
            hasWhere = true;
        }

        // 2. 书名模糊条件
        if (bookName != null && !bookName.isBlank()) {
            if (hasWhere) sql.append(" AND ");
            else sql.append(" WHERE ");
            sql.append("b.Bookname LIKE CONCAT('%', ?, '%')");
            args.add(bookName);
            hasWhere = true;
        }

        // 3. 未传入userId = 全网检索，强制只查公开房间
        if (userId == null || userId.isBlank()) {
            if (hasWhere) sql.append(" AND ");
            else sql.append(" WHERE ");
            sql.append("r.is_public = 1");
            hasWhere = true;
        }

        sql.append(" ORDER BY r.create_time DESC LIMIT ?, ?");
        args.add(offset);
        args.add(pageSize);

        try {
            return DBUtil.executeQuery(sql.toString(), this::convertRoom, args.toArray());
        } catch (SQLException e) {
            logger.error("分页查询房间异常", e);
            return Collections.emptyList();
        }
    }

    // 【改动4：分页统计总数，同步is_public过滤逻辑】
    @Override
    public Long countTotal(String userId, String bookName) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) total FROM book_read_room r");
        List<Object> args = new ArrayList<>();
        boolean hasWhere = false;

        if (userId != null && !userId.isBlank()) {
            sql.append(" WHERE r.user_id = ?");
            args.add(userId);
            hasWhere = true;
        }
        if (bookName != null && !bookName.isBlank()) {
            if (hasWhere) sql.append(" AND ");
            else sql.append(" WHERE ");
            sql.append("r.isbn IN (SELECT ISBN FROM book_information WHERE Bookname LIKE CONCAT('%', ?, '%'))");
            args.add(bookName);
            hasWhere = true;
        }

        // 无userId全网统计，只统计公开房间
        if (userId == null || userId.isBlank()) {
            if (hasWhere) sql.append(" AND ");
            else sql.append(" WHERE ");
            sql.append("r.is_public = 1");
        }

        try {
            return DBUtil.executeQuery(sql.toString(), rs -> rs.getLong("total"), args.toArray()).get(0);
        } catch (SQLException e) {
            logger.error("统计房间总数异常", e);
            return 0L;
        }
    }

    // 结果集转实体通用方法
    private BookReadRoom convertRoom(java.sql.ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        LocalDateTime createTime = rs.getTimestamp("create_time") == null
                ? null : rs.getTimestamp("create_time").toLocalDateTime();
        Integer isPublic = rs.getInt("is_public");
        String userId = rs.getString("user_id");
        String isbn = rs.getString("isbn");

        BookReadRoom room = new BookReadRoom(id, createTime, isPublic, userId, isbn);
        // 联表扩展字段
        room.setBookName(rs.getString("Bookname"));
        room.setUserName(rs.getString("Name"));
        return room;
    }
}