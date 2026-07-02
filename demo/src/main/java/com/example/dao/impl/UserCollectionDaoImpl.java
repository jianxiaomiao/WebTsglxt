package com.example.dao.impl;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.UserCollectionDao;
import com.example.entity.UserCollection;
import com.example.util.DBUtil;

public class UserCollectionDaoImpl implements UserCollectionDao {

    private static final Logger logger = LoggerFactory.getLogger(UserCollectionDaoImpl.class);

    @Override
    public void add(UserCollection userCollection) {
        if (userCollection == null || userCollection.getUserId() == null || userCollection.getIsbn() == null) {
            throw new IllegalArgumentException("收藏信息/用户ID/ISBN不能为空");
        }
        try {
            // 新增GroupId插入
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_collection(UserId, Isbn, GroupId) values (?, ?, ?)",
                    userCollection.getUserId(), userCollection.getIsbn(), userCollection.getGroupId());
            if (affectedRows == 0) {
                throw new RuntimeException("新增收藏失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增收藏失败，UserId：{}，Isbn：{}", userCollection.getUserId(), userCollection.getIsbn(), e);
            throw new RuntimeException("新增收藏异常", e);
        }
    }

    @Override
    public void update(UserCollection userCollection) {
        if (userCollection == null || userCollection.getCollectionId() == null) {
            throw new IllegalArgumentException("收藏信息/CollectionId不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_collection set ");
            List<Object> params = new ArrayList<>();

            if (userCollection.getUserId() != null) {
                sql.append("UserId=?, ");
                params.add(userCollection.getUserId());
            }
            if (userCollection.getIsbn() != null) {
                sql.append("Isbn=?, ");
                params.add(userCollection.getIsbn());
            }
            // ========== 动态拼接GroupId ==========
            if (userCollection.getGroupId() != null) {
                sql.append("GroupId=?, ");
                params.add(userCollection.getGroupId());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where CollectionId=?");
            params.add(userCollection.getCollectionId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新收藏信息】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新收藏失败，未匹配到CollectionId：" + userCollection.getCollectionId());
            }
        } catch (SQLException e) {
            logger.error("更新收藏失败，CollectionId：{}", userCollection.getCollectionId(), e);
            throw new RuntimeException("更新收藏异常", e);
        }
    }

    @Override
    public void del(Integer collectionId) {
        if (collectionId == null) {
            throw new IllegalArgumentException("CollectionId不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from user_collection where CollectionId = ?", collectionId);
            if (affectedRows == 0) {
                throw new RuntimeException("删除收藏失败，未匹配到CollectionId：" + collectionId);
            }
        } catch (SQLException e) {
            logger.error("删除收藏失败，CollectionId：{}", collectionId, e);
            throw new RuntimeException("删除收藏异常", e);
        }
    }

    // 通用结果集解析（统一封装GroupId、GroupName）
    private UserCollection parseRs(java.sql.ResultSet rs) throws SQLException {
        Integer collectionId = rs.getInt("CollectionId");
        String userId = rs.getString("UserId");
        String isbn = rs.getString("Isbn");
        Timestamp lastReadTime = rs.getTimestamp("Last_read_time");
        String bookName = rs.getString("Bookname");
        String information = rs.getString("Information");
        String pictureName = rs.getString("PictureName");
        // 分组字段
        Integer groupId = rs.getObject("GroupId") == null ? null : rs.getInt("GroupId");
        String groupName = rs.getString("GroupName");

        UserCollection userCollection = new UserCollection(userId, isbn);
        userCollection.setCollectionId(collectionId);
        userCollection.setLastReadTime(lastReadTime);
        userCollection.setBookName(bookName);
        userCollection.setInformation(information);
        userCollection.setPictureName(pictureName);
        userCollection.setGroupId(groupId);
        userCollection.setGroupName(groupName);
        return userCollection;
    }

    @Override
    public List<UserCollection> queryAll() {
        try {
            // 联表分组表 + 统一排序规则
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON c.UserId = rp.UserId AND c.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "ORDER BY g.Sort DESC, uc.create_time DESC";
            return DBUtil.executeQuery(sql, this::parseRs);
        } catch (SQLException e) {
            logger.error("查询所有收藏异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserCollection> queryByCollectionId(Integer collectionId) {
        if (collectionId == null) {
            throw new IllegalArgumentException("CollectionId不能为空");
        }
        try {
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON uc.UserId = rp.UserId AND uc.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "WHERE uc.CollectionId=?";
            return DBUtil.executeQuery(sql, this::parseRs, collectionId);
        } catch (SQLException e) {
            logger.error("查询收藏异常，CollectionId：{}", collectionId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserCollection> queryByUserId(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) throw new IllegalArgumentException("用户ID不能为空");

        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int start = (page - 1) * size;

        try {
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON uc.UserId = rp.UserId AND uc.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "WHERE uc.UserId = ? " +
                    "ORDER BY g.Sort DESC, uc.create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::parseRs, userId, start, size);
        } catch (SQLException e) {
            logger.error("按用户查询收藏异常", e);
            return Collections.emptyList();
        }
    }

    // ========== 新增：根据用户+分组ID分页查询 ==========
    @Override
    public List<UserCollection> queryByUserIdAndGroupId(String userId, Integer groupId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty() || groupId == null) throw new IllegalArgumentException("用户ID/分组ID不能为空");
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 99999 : pageSize; // 默认超大页，一次性取出分组全部
        int start = (page - 1) * size;

        try {
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON uc.UserId = rp.UserId AND uc.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "WHERE uc.UserId = ? AND uc.GroupId = ? " +
                    "ORDER BY g.Sort DESC, uc.create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::parseRs, userId, groupId, start, size);
        } catch (SQLException e) {
            logger.error("按用户+分组查询收藏异常 userId:{}, groupId:{}", userId, groupId, e);
            return Collections.emptyList();
        }
    }

    // ========== 新增：统计单个分组书籍总数 ==========
    @Override
    public int countByUserIdAndGroupId(String userId, Integer groupId) {
        if (userId == null || userId.isEmpty() || groupId == null) return 0;
        try {
            List<Integer> countList = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_collection WHERE UserId = ? AND GroupId = ?",
                    rs -> rs.getInt("total"),
                    userId, groupId
            );
            return countList.isEmpty() ? 0 : countList.get(0);
        } catch (SQLException e) {
            logger.error("统计分组书籍数量异常", e);
            return 0;
        }
    }

    @Override
    public int countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) throw new IllegalArgumentException("用户ID不能为空");
        try {
            List<Integer> countList = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_collection WHERE UserId = ?",
                    rs -> rs.getInt("total"),
                    userId
            );
            return countList.isEmpty() ? 0 : countList.get(0);
        } catch (SQLException e) {
            logger.error("统计用户收藏数量异常", e);
            return 0;
        }
    }

    @Override
    public List<UserCollection> queryByIsbn(String isbn) {
        if (isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("ISBN不能为空");
        }
        try {
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON uc.UserId = rp.UserId AND uc.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "WHERE uc.Isbn=?";
            return DBUtil.executeQuery(sql, this::parseRs, isbn);
        } catch (SQLException e) {
            logger.error("查询收藏异常，Isbn：{}", isbn, e);
            return Collections.emptyList();
        }
    }

    public Integer countCollectionByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_collection WHERE UserId=? AND create_time BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户新增收藏异常", e);
            return 0;
        }
    }

    @Override
    public List<UserCollection> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            String sql = "SELECT uc.*, g.GroupName, rp.Last_read_time, b.Bookname, b.Information, b.PictureName " +
                    "FROM user_collection uc " +
                    "LEFT JOIN user_bookshelf_group g ON g.UserId = uc.UserId AND g.GroupId = uc.GroupId " +
                    "LEFT JOIN user_read_progress rp ON uc.UserId = rp.UserId AND uc.Isbn = rp.Isbn " +
                    "LEFT JOIN book_information b ON b.ISBN = uc.Isbn " +
                    "WHERE uc.UserId=? AND uc.create_time BETWEEN ? AND ? ORDER BY g.Sort DESC, uc.create_time DESC";
            return DBUtil.executeQuery(sql, this::parseRs, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段收藏异常", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearCollectionGroupByGroupId(String userId, Integer groupId) {
        if (userId == null || groupId == null) {
            throw new IllegalArgumentException("用户ID、分组ID不能为空");
        }
        String sql = "UPDATE user_collection SET GroupId = NULL WHERE UserId = ? AND GroupId = ?";
        try {
            // 此处不单独执行，交由上层Service事务统一调度，本方法仅定义SQL
            logger.info("准备清空分组绑定书籍，userId:{}, groupId:{}", userId, groupId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}