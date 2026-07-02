package com.example.dao.impl;

import com.example.dao.BookTypeDao;
import com.example.entity.BookType;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class BookTypeDaoImpl implements BookTypeDao {

    private static final Logger logger = LoggerFactory.getLogger(BookTypeDaoImpl.class);

    @Override
    public void add(BookType bookType) {
        if (bookType == null || bookType.getBookType() == null) {
            throw new IllegalArgumentException("书籍种类信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into book_type(bookType) values (?)",
                    bookType.getBookType()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增书籍种类失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增书籍种类失败", e);
            throw new RuntimeException("新增书籍种类异常", e);
        }
    }

    @Override
    public void update(BookType bookType) {
        if (bookType == null || bookType.getId() == null) {
            throw new IllegalArgumentException("书籍种类信息/ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update book_type set ");
            List<Object> params = new java.util.ArrayList<>();

            if (bookType.getBookType() != null) {
                sql.append("bookType=?");
                params.add(bookType.getBookType());
            }

            sql.append(" where id=?");
            params.add(bookType.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            if (affectedRows == 0) {
                throw new RuntimeException("更新书籍种类失败，未匹配到ID：" + bookType.getId());
            }
        } catch (SQLException e) {
            logger.error("更新书籍种类失败，ID：{}", bookType.getId(), e);
            throw new RuntimeException("更新书籍种类异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("书籍种类ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "delete from book_type where id = ?",
                    id
            );
            if (affectedRows == 0) {
                throw new RuntimeException("删除书籍种类失败，未匹配到ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除书籍种类失败，ID：{}", id, e);
            throw new RuntimeException("删除书籍种类异常", e);
        }
    }

    @Override
    public List<BookType> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "select id, bookType from book_type",
                    rs -> {
                        BookType bookType = null;
                        try {
                            Integer id = rs.getInt("id");
                            String type = rs.getString("bookType");
                            bookType = new BookType(id, type);
                        } catch (SQLException e) {
                            logger.error("解析书籍种类数据异常", e);
                            throw new RuntimeException("解析书籍种类数据异常", e);
                        }
                        return bookType;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有书籍种类异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookType> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("书籍种类ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select id, bookType from book_type where id=?",
                    rs -> {
                        BookType bookType = null;
                        try {
                            String type = rs.getString("bookType");
                            bookType = new BookType(id, type);
                        } catch (SQLException e) {
                            logger.error("解析书籍种类数据异常，ID：{}", id, e);
                            throw new RuntimeException("解析书籍种类数据异常", e);
                        }
                        return bookType;
                    },
                    id
            );
        } catch (SQLException e) {
            logger.error("查询书籍种类异常，ID：{}", id, e);
            return Collections.emptyList();
        }
    }
}