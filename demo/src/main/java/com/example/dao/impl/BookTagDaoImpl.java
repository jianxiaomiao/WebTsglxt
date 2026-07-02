package com.example.dao.impl;

import com.example.dao.BookTagDao;
import com.example.entity.BookTag;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class BookTagDaoImpl implements BookTagDao {
    private static final Logger logger = LoggerFactory.getLogger(BookTagDaoImpl.class);

    @Override
    public void add(BookTag tag) {
        if (tag == null || tag.getTagName() == null) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        try {
            String sql = "insert into book_tag(tag_name) values (?)";
            int rows = DBUtil.executeUpdate(sql, tag.getTagName());
            if (rows == 0) throw new RuntimeException("新增标签失败");
        } catch (SQLException e) {
            logger.error("新增标签异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) throw new IllegalArgumentException("标签ID不能为空");
        try {
            String sql = "delete from book_tag where id=?";
            int rows = DBUtil.executeUpdate(sql, id);
            if (rows == 0) throw new RuntimeException("删除标签失败，ID不存在");
        } catch (SQLException e) {
            logger.error("删除标签异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateById(BookTag tag) {
        if (tag == null || tag.getId() == null || tag.getTagName() == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            String sql = "update book_tag set tag_name=? where id=?";
            int rows = DBUtil.executeUpdate(sql, tag.getTagName(), tag.getId());
            if (rows == 0) throw new RuntimeException("修改标签失败");
        } catch (SQLException e) {
            logger.error("修改标签异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BookTag selectById(Integer id) {
        if (id == null) return null;
        try {
            String sql = "select * from book_tag where id=?";
            List<BookTag> list = DBUtil.executeQuery(sql, rs -> {
                try {
                    return new BookTag(rs.getInt("id"), rs.getString("tag_name"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询标签异常", e);
            return null;
        }
    }

    @Override
    public List<BookTag> selectAll() {
        try {
            String sql = "select * from book_tag";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new BookTag(rs.getInt("id"), rs.getString("tag_name"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (SQLException e) {
            logger.error("查询所有标签异常", e);
            return Collections.emptyList();
        }
    }
}