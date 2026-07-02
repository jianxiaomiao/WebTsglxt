package com.example.dao.impl;

import com.example.dao.UserBookshelfGroupDao;
import com.example.entity.UserBookshelfGroup;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserBookshelfGroupDaoImpl implements UserBookshelfGroupDao {
    private static final Logger logger = LoggerFactory.getLogger(UserBookshelfGroupDaoImpl.class);

    // 结果集统一解析方法
    private UserBookshelfGroup parseRs(java.sql.ResultSet rs) throws SQLException {
        UserBookshelfGroup group = new UserBookshelfGroup();
        group.setGroupId(rs.getInt("GroupId"));
        group.setUserId(rs.getString("UserId"));
        group.setGroupName(rs.getString("GroupName"));
        group.setSort(rs.getInt("Sort"));
        group.setCreate_time(rs.getTimestamp("create_time"));
        return group;
    }

    @Override
    public void add(UserBookshelfGroup group) {
        if (group == null || group.getUserId() == null || group.getGroupName() == null) {
            throw new IllegalArgumentException("用户ID、分组名称不能为空");
        }
        try {
            String sql = "INSERT INTO user_bookshelf_group(UserId, GroupName, Sort) VALUES (?, ?, ?)";
            int rows = DBUtil.executeUpdate(sql, group.getUserId(), group.getGroupName(), group.getSort());
            if (rows == 0) {
                throw new RuntimeException("新增书架分组失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增分组失败，UserId:{}", group.getUserId(), e);
            throw new RuntimeException("新增书架分组数据库异常", e);
        }
    }

    @Override
    public void update(UserBookshelfGroup group) {
        if (group == null || group.getGroupId() == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE user_bookshelf_group SET ");
            List<Object> params = new ArrayList<>();

            // 动态拼接非空字段
            if (group.getUserId() != null) {
                sql.append("UserId=?, ");
                params.add(group.getUserId());
            }
            if (group.getGroupName() != null) {
                sql.append("GroupName=?, ");
                params.add(group.getGroupName());
            }
            if (group.getSort() != null) {
                sql.append("Sort=?, ");
                params.add(group.getSort());
            }

            // 去除末尾逗号
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE GroupId=?");
            params.add(group.getGroupId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新分组SQL:{} 参数:{}", sql, params);
            if (rows == 0) {
                throw new RuntimeException("更新失败，未匹配GroupId:" + group.getGroupId());
            }
        } catch (SQLException e) {
            logger.error("更新分组失败 GroupId:{}", group.getGroupId(), e);
            throw new RuntimeException("更新书架分组数据库异常", e);
        }
    }

    @Override
    public void delete(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        try {
            String sql = "DELETE FROM user_bookshelf_group WHERE GroupId=?";
            int rows = DBUtil.executeUpdate(sql, groupId);
            if (rows == 0) {
                throw new RuntimeException("删除失败，未匹配GroupId:" + groupId);
            }
        } catch (SQLException e) {
            logger.error("删除分组失败 GroupId:{}", groupId, e);
            throw new RuntimeException("删除书架分组数据库异常", e);
        }
    }

    @Override
    public List<UserBookshelfGroup> queryById(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        try {
            String sql = "SELECT * FROM user_bookshelf_group WHERE GroupId=?";
            return DBUtil.executeQuery(sql, this::parseRs, groupId);
        } catch (SQLException e) {
            logger.error("根据GroupId查询分组失败 groupId:{}", groupId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserBookshelfGroup> queryByUserId(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 分页默认值处理
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int start = (page - 1) * size;

        try {
            // 排序规则：Sort越大越靠前，同Sort创建时间越晚越靠前
            String sql = "SELECT * FROM user_bookshelf_group " +
                    "WHERE UserId=? " +
                    "ORDER BY Sort DESC, create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::parseRs, userId, start, size);
        } catch (SQLException e) {
            logger.error("根据UserId查询分组失败 userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public int countByUserId(String userId) {
        if (userId == null || userId.isBlank()) return 0;
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_bookshelf_group WHERE UserId=?";
            List<Integer> res = DBUtil.executeQuery(sql, rs -> rs.getInt("total"), userId);
            return res.isEmpty() ? 0 : res.get(0);
        } catch (SQLException e) {
            logger.error("统计用户分组数量失败 userId:{}", userId, e);
            return 0;
        }
    }
}