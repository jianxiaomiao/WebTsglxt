package com.example.dao.impl;

import com.example.dao.DeptTypeDao;
import com.example.entity.DeptType;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeptTypeDaoImpl implements DeptTypeDao {

    private static final Logger logger = LoggerFactory.getLogger(DeptTypeDaoImpl.class);

    @Override
    public void add(DeptType deptType) {
        if (deptType == null || deptType.getDeptType() == null) {
            throw new IllegalArgumentException("系别类型信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_dept_type(DeptType) values (?)",
                    deptType.getDeptType()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增系别类型失败");
            }
        } catch (SQLException e) {
            logger.error("新增系别类型异常", e);
            throw new RuntimeException("新增系别类型异常", e);
        }
    }

    @Override
    public void update(DeptType deptType) {
        if (deptType == null || deptType.getId() == null) {
            throw new IllegalArgumentException("系别类型ID/信息不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "update user_dept_type set DeptType=? where id=?",
                    deptType.getDeptType(),
                    deptType.getId()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("修改系别类型失败，未匹配到ID");
            }
        } catch (SQLException e) {
            logger.error("修改系别类型异常，ID：{}", deptType.getId(), e);
            throw new RuntimeException("修改系别类型异常", e);
        }
    }

    @Override
    public void delete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("系别类型ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate(
                    "delete from user_dept_type where id=?",
                    id
            );
            if (affectedRows == 0) {
                throw new RuntimeException("删除系别类型失败，未匹配到ID");
            }
        } catch (SQLException e) {
            logger.error("删除系别类型异常，ID：{}", id, e);
            throw new RuntimeException("删除系别类型异常", e);
        }
    }

    @Override
    public List<DeptType> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "select id, DeptType from user_dept_type",
                    rs -> {
                        DeptType deptType = null;
                        try {
                            Integer id = rs.getInt("id");
                            String typeName = rs.getString("DeptType");
                            deptType = new DeptType(id, typeName);
                        } catch (SQLException e) {
                            logger.error("解析系别类型数据异常", e);
                            throw new RuntimeException("解析数据异常", e);
                        }
                        return deptType;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有系别类型异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DeptType> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("系别类型ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "select id, DeptType from user_dept_type where id=?",
                    rs -> {
                        DeptType deptType = null;
                        try {
                            String typeName = rs.getString("DeptType");
                            deptType = new DeptType(id, typeName);
                        } catch (SQLException e) {
                            logger.error("解析系别类型数据异常，ID：{}", id, e);
                            throw new RuntimeException("解析数据异常", e);
                        }
                        return deptType;
                    },
                    id
            );
        } catch (SQLException e) {
            logger.error("根据ID查询系别类型异常，ID：{}", id, e);
            return Collections.emptyList();
        }
    }
}