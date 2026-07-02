package com.example.dao.impl;

import com.example.dao.UserTextColTypeDao;
import com.example.entity.UserTextColType;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserTextColTypeDaoImpl implements UserTextColTypeDao {

    private static final Logger logger = LoggerFactory.getLogger(UserTextColTypeDaoImpl.class);

    @Override
    public void add(UserTextColType type) {
        if (type == null || type.getUserTextColType() == null) throw new IllegalArgumentException("笔记种类信息不能为空");
        try {
            int affectedRows = DBUtil.executeUpdate("insert into user_text_col_type(userTextColType) values (?)", type.getUserTextColType());
            if (affectedRows == 0) throw new RuntimeException("新增笔记种类失败");
        } catch (SQLException e) {
            logger.error("新增笔记种类失败", e);
            throw new RuntimeException("新增笔记种类异常", e);
        }
    }

    @Override
    public void update(UserTextColType type) {
        if (type == null || type.getId() == null) throw new IllegalArgumentException("笔记种类信息/ID不能为空");
        try {
            StringBuilder sql = new StringBuilder("update user_text_col_type set ");
            List<Object> params = new java.util.ArrayList<>();
            if (type.getUserTextColType() != null) {
                sql.append("userTextColType=?");
                params.add(type.getUserTextColType());
            }
            sql.append(" where id=?");
            params.add(type.getId());
            DBUtil.executeUpdate(sql.toString(), params.toArray());
        } catch (SQLException e) {
            logger.error("更新笔记种类失败", e);
            throw new RuntimeException("更新笔记种类异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) throw new IllegalArgumentException("笔记种类ID不能为空");
        try {
            DBUtil.executeUpdate("delete from user_text_col_type where id=?", id);
        } catch (SQLException e) {
            logger.error("删除笔记种类失败", e);
            throw new RuntimeException("删除笔记种类异常", e);
        }
    }

    @Override
    public List<UserTextColType> queryAll() {
        try {
            // 🔥 完全按照你 BookType 的格式编写，无任何自定义循环
            return DBUtil.executeQuery(
                    "select id, userTextColType from user_text_col_type",
                    rs -> {
                        UserTextColType type = null;
                        try {
                            Integer id = rs.getInt("id");
                            String userTextColType = rs.getString("userTextColType");
                            type = new UserTextColType(id, userTextColType);
                        } catch (SQLException e) {
                            logger.error("解析笔记种类数据异常", e);
                            throw new RuntimeException("解析笔记种类数据异常", e);
                        }
                        return type;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有笔记种类异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserTextColType> queryById(Integer id) {
        if (id == null) throw new IllegalArgumentException("笔记种类ID不能为空");
        try {
            return DBUtil.executeQuery(
                    "select id, userTextColType from user_text_col_type where id=?",
                    rs -> {
                        UserTextColType type = null;
                        try {
                            Integer typeId = rs.getInt("id");
                            String userTextColType = rs.getString("userTextColType");
                            type = new UserTextColType(typeId, userTextColType);
                        } catch (SQLException e) {
                            logger.error("解析笔记种类数据异常", e);
                            throw new RuntimeException("解析笔记种类数据异常", e);
                        }
                        return type;
                    },
                    id
            );
        } catch (SQLException e) {
            logger.error("查询笔记种类失败", e);
            return Collections.emptyList();
        }
    }
}