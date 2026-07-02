package com.example.dao.impl;

import com.example.dao.BookTagRelationDao;
import com.example.entity.BookTagRelation;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class BookTagRelationDaoImpl implements BookTagRelationDao {
    private static final Logger logger = LoggerFactory.getLogger(BookTagRelationDaoImpl.class);

    @Override
    public void add(BookTagRelation relation) {
        if (relation == null || relation.getIsbn() == null || relation.getTagId() == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            String sql = "insert into book_tag_relation(isbn, tag_id) values (?, ?)";
            int rows = DBUtil.executeUpdate(sql, relation.getIsbn(), relation.getTagId());
            if (rows == 0) throw new RuntimeException("新增关联失败");
        } catch (SQLException e) {
            logger.error("新增标签关联异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) throw new IllegalArgumentException("关联ID不能为空");
        try {
            String sql = "delete from book_tag_relation where id=?";
            int rows = DBUtil.executeUpdate(sql, id);
            if (rows == 0) throw new RuntimeException("删除关联失败");
        } catch (SQLException e) {
            logger.error("删除标签关联异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BookTagRelation selectById(Integer id) {
        if (id == null) return null;
        try {
            String sql = "select * from book_tag_relation where id=?";
            List<BookTagRelation> list = DBUtil.executeQuery(sql, rs -> {
                try {
                    BookTagRelation r = new BookTagRelation();
                    r.setId(rs.getInt("id"));
                    r.setIsbn(rs.getString("isbn"));
                    r.setTagId(rs.getInt("tag_id"));
                    return r;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询关联异常", e);
            return null;
        }
    }

    // 联表查询：根据ISBN获取标签名称
    @Override
    public List<BookTagRelation> selectByIsbn(String isbn) {
        if (isbn == null) return Collections.emptyList();
        try {
            String sql = "SELECT r.*, t.tag_name FROM book_tag_relation r " +
                    "LEFT JOIN book_tag t ON r.tag_id = t.id WHERE r.isbn=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    BookTagRelation r = new BookTagRelation();
                    r.setId(rs.getInt("id"));
                    r.setIsbn(rs.getString("isbn"));
                    r.setTagId(rs.getInt("tag_id"));
                    r.setTagName(rs.getString("tag_name"));
                    return r;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, isbn);
        } catch (SQLException e) {
            logger.error("根据ISBN查询标签关联异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BookTagRelation> selectByTagId(Integer tagId) {
        if (tagId == null) return Collections.emptyList();
        try {
            String sql = "select * from book_tag_relation where tag_id=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    BookTagRelation r = new BookTagRelation();
                    r.setId(rs.getInt("id"));
                    r.setIsbn(rs.getString("isbn"));
                    r.setTagId(rs.getInt("tag_id"));
                    return r;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, tagId);
        } catch (SQLException e) {
            logger.error("根据标签ID查询关联异常", e);
            return Collections.emptyList();
        }
    }
}