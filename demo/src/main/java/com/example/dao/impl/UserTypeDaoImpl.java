package com.example.dao.impl;

import com.example.dao.UserTypeDao;
import com.example.entity.UserType;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserTypeDaoImpl implements UserTypeDao {

    private static final Logger logger = LoggerFactory.getLogger(UserTypeDaoImpl.class);

    @Override
    public void add(UserType userType) {
        if (userType == null || userType.getUserType() == null) {
            throw new IllegalArgumentException("用户种类信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_type(user_type) values (?)",
                    userType.getUserType()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增用户种类失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增用户种类失败", e);
            throw new RuntimeException("新增用户种类异常", e);
        }
    }

    @Override
    public void update(UserType userType) {
        if (userType == null || userType.getId() == null) {
            throw new IllegalArgumentException("用户种类信息/ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_type set ");
            List<Object> params = new ArrayList<>();

            if (userType.getUserType() != null) {
                sql.append("user_type=?");
                params.add(userType.getUserType());
            }

            sql.append(" where id=?");
            params.add(userType.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            if (affectedRows == 0) {
                throw new RuntimeException("更新用户种类失败，未匹配到ID：" + userType.getId());
            }
        } catch (SQLException e) {
            logger.error("更新用户种类失败，ID：{}", userType.getId(), e);
            throw new RuntimeException("更新用户种类异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("用户种类ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "delete from user_type where id = ?",
                    id
            );
            if (affectedRows == 0) {
                throw new RuntimeException("删除用户种类失败，未匹配到ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除用户种类失败，ID：{}", id, e);
            throw new RuntimeException("删除用户种类异常", e);
        }
    }

    @Override
    public List<UserType> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "select id, user_type from user_type",
                    rs -> {
                        UserType userType = null;
                        try {
                            Integer id = rs.getInt("id");
                            String type = rs.getString("user_type");
                            userType = new UserType(id, type);
                        } catch (SQLException e) {
                            logger.error("解析用户种类数据异常", e);
                            throw new RuntimeException("解析用户种类数据异常", e);
                        }
                        return userType;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有用户种类异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserType> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("用户种类ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select id, userType from user_type where id=?",
                    rs -> {
                        UserType userType = null;
                        try {
                            String type = rs.getString("userType");
                            userType = new UserType(id, type);
                        } catch (SQLException e) {
                            logger.error("解析用户种类数据异常，ID：{}", id, e);
                            throw new RuntimeException("解析用户种类数据异常", e);
                        }
                        return userType;
                    },
                    id
            );
        } catch (SQLException e) {
            logger.error("查询用户种类异常，ID：{}", id, e);
            return Collections.emptyList();
        }
    }
}