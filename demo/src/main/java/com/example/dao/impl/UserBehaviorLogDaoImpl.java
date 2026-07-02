package com.example.dao.impl;

import com.example.dao.UserBehaviorLogDao;
import com.example.entity.UserBehaviorLog;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserBehaviorLogDaoImpl implements UserBehaviorLogDao {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorLogDaoImpl.class);

    /**
     * 封装ResultSet转UserBehaviorLog实体，统一复用
     */
    private UserBehaviorLog buildLog(ResultSet rs) throws SQLException {
        UserBehaviorLog log = new UserBehaviorLog();
        log.setId(rs.getInt("id"));
        log.setUser_id(rs.getString("user_id"));
        log.setAction_type(rs.getInt("action_type"));
        log.setBook_isbn(rs.getString("book_isbn"));
        log.setChapter_number(rs.getString("chapter_number"));
        log.setContent_snapshot(rs.getString("content_snapshot"));
        Timestamp time = rs.getTimestamp("create_time");
        log.setCreate_time(time != null ? time.toLocalDateTime() : null);
        return log;
    }

    @Override
    public void add(UserBehaviorLog log) {
        if (log == null) {
            throw new IllegalArgumentException("用户行为日志不能为空");
        }
        try {
            String sql = "INSERT INTO user_behavior_log(user_id, action_type, book_isbn, chapter_number, content_snapshot, create_time) VALUES (?,?,?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    log.getUser_id(),
                    log.getAction_type(),
                    log.getBook_isbn(),
                    log.getChapter_number(),
                    log.getContent_snapshot(),
                    log.getCreate_time());
            if (rows == 0) {
                throw new RuntimeException("新增用户行为日志失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增用户行为日志失败", e);
            throw new RuntimeException("新增用户行为日志异常", e);
        }
    }

    @Override
    public void update(UserBehaviorLog log) {
        if (log == null || log.getId() == null) {
            throw new IllegalArgumentException("日志对象/日志ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE user_behavior_log SET ");
            List<Object> params = new ArrayList<>();

            if (log.getUser_id() != null) {
                sql.append("user_id=?, ");
                params.add(log.getUser_id());
            }
            if (log.getAction_type() != null) {
                sql.append("action_type=?, ");
                params.add(log.getAction_type());
            }
            if (log.getBook_isbn() != null) {
                sql.append("book_isbn=?, ");
                params.add(log.getBook_isbn());
            }
            if (log.getChapter_number() != null) {
                sql.append("chapter_number=?, ");
                params.add(log.getChapter_number());
            }
            if (log.getContent_snapshot() != null) {
                sql.append("content_snapshot=?, ");
                params.add(log.getContent_snapshot());
            }
            if (log.getCreate_time() != null) {
                sql.append("create_time=?, ");
                params.add(log.getCreate_time());
            }

            // 移除最后多余逗号
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(log.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新行为日志 SQL:{} 参数:{}", sql, params);
            if (rows == 0) {
                throw new RuntimeException("更新失败，未匹配ID：" + log.getId());
            }
        } catch (SQLException e) {
            logger.error("更新行为日志失败，ID:{}", log.getId(), e);
            throw new RuntimeException("更新行为日志异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("日志ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM user_behavior_log WHERE id=?", id);
            if (rows == 0) {
                throw new RuntimeException("删除失败，未匹配ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除行为日志失败，ID:{}", id, e);
            throw new RuntimeException("删除行为日志异常", e);
        }
    }

    @Override
    public List<UserBehaviorLog> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("日志ID不能为空");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE id=?";
            return DBUtil.executeQuery(sql, rs -> {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getInt("id"));
                log.setUser_id(rs.getString("user_id"));
                log.setAction_type(rs.getInt("action_type"));
                log.setBook_isbn(rs.getString("book_isbn"));
                log.setChapter_number(rs.getString("chapter_number"));
                log.setContent_snapshot(rs.getString("content_snapshot"));
                log.setCreate_time(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                return log;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询行为日志失败，ID:{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserBehaviorLog> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, rs -> {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getInt("id"));
                log.setUser_id(rs.getString("user_id"));
                log.setAction_type(rs.getInt("action_type"));
                log.setBook_isbn(rs.getString("book_isbn"));
                log.setChapter_number(rs.getString("chapter_number"));
                log.setContent_snapshot(rs.getString("content_snapshot"));
                log.setCreate_time(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                return log;
            }, userId);
        } catch (SQLException e) {
            logger.error("根据用户ID查询行为日志失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserBehaviorLog> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? AND create_time BETWEEN ? AND ? ORDER BY create_time DESC";
            return DBUtil.executeQuery(sql, rs -> {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getInt("id"));
                log.setUser_id(rs.getString("user_id"));
                log.setAction_type(rs.getInt("action_type"));
                log.setBook_isbn(rs.getString("book_isbn"));
                log.setChapter_number(rs.getString("chapter_number"));
                log.setContent_snapshot(rs.getString("content_snapshot"));
                log.setCreate_time(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                return log;
            }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("时间段查询行为日志失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || startTime == null || endTime == null) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_behavior_log WHERE user_id=? AND create_time BETWEEN ? AND ?";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId, startTime, endTime);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计时间段日志总数失败", e);
            return 0L;
        }
    }

    @Override
    public List<UserBehaviorLog> queryLatestByUserId(String userId, Integer limit) {
        if (userId == null || userId.isEmpty() || limit == null || limit < 1) {
            throw new IllegalArgumentException("参数非法");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? ORDER BY create_time DESC LIMIT ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getInt("id"));
                log.setUser_id(rs.getString("user_id"));
                log.setAction_type(rs.getInt("action_type"));
                log.setBook_isbn(rs.getString("book_isbn"));
                log.setChapter_number(rs.getString("chapter_number"));
                log.setContent_snapshot(rs.getString("content_snapshot"));
                log.setCreate_time(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                return log;
            }, userId, limit);
        } catch (SQLException e) {
            logger.error("查询用户最新N条日志失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserBehaviorLog> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? ORDER BY create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getInt("id"));
                log.setUser_id(rs.getString("user_id"));
                log.setAction_type(rs.getInt("action_type"));
                log.setBook_isbn(rs.getString("book_isbn"));
                log.setChapter_number(rs.getString("chapter_number"));
                log.setContent_snapshot(rs.getString("content_snapshot"));
                log.setCreate_time(rs.getTimestamp("create_time") != null ? rs.getTimestamp("create_time").toLocalDateTime() : null);
                return log;
            }, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户日志失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_behavior_log WHERE user_id=?";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计用户日志总数失败，userId:{}", userId, e);
            return 0L;
        }
    }

    @Override
    public UserBehaviorLog getLatestByUserIdAndType(String userId, Integer actionType) {
        // 参数校验
        if (userId == null || userId.isEmpty() || actionType == null) {
            throw new IllegalArgumentException("用户ID、行为类型不能为空");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? AND action_type=? ORDER BY create_time DESC LIMIT 1";
            List<UserBehaviorLog> list = DBUtil.executeQuery(sql, this::buildLog, userId, actionType);
            // 有数据返回第一条，无数据返回null
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("查询用户指定类型最新行为日志失败 userId:{},type:{}", userId, actionType, e);
            return null;
        }
    }

    @Override
    public List<UserBehaviorLog> pageByUserIdAndType(String userId, Integer actionType, Integer pageNum, Integer pageSize) {
        // 参数校验
        if (userId == null || userId.isEmpty() || actionType == null) {
            throw new IllegalArgumentException("用户ID、行为类型不能为空");
        }
        // 分页参数容错，和原有分页逻辑保持一致
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? AND action_type=? ORDER BY create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::buildLog, userId, actionType, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户指定类型行为日志失败 userId:{},type:{}", userId, actionType, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> queryActiveUserIds(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("起止时间不能为空");
        }
        try {
            // DISTINCT 去重，只拿唯一用户ID，减少循环重复处理
            String sql = "SELECT DISTINCT user_id FROM user_behavior_log WHERE create_time BETWEEN ? AND ?";
            return DBUtil.executeQuery(sql, rs -> rs.getString("user_id"), startTime, endTime);
        } catch (SQLException e) {
            logger.error("查询时间段活跃用户ID失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public UserBehaviorLog getLatestByUserIdAndIsbnAndType(String userId, String isbn, Integer actionType) {
        // 参数校验：userId、actionType不可为空，isbn允许null
        if (userId == null || userId.isEmpty() || actionType == null) {
            throw new IllegalArgumentException("用户ID、行为类型不能为空");
        }
        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? AND book_isbn=? AND action_type=? ORDER BY create_time DESC LIMIT 1";
            List<UserBehaviorLog> list = DBUtil.executeQuery(sql, this::buildLog, userId, isbn, actionType);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("查询用户+书籍+类型最新日志失败 userId:{},isbn:{},type:{}", userId, isbn, actionType, e);
            return null;
        }
    }

    @Override
    public List<UserBehaviorLog> pageByUserIdAndIsbnAndType(String userId, String isbn, Integer actionType, Integer pageNum, Integer pageSize) {
        // 参数校验
        if (userId == null || userId.isEmpty() || actionType == null) {
            throw new IllegalArgumentException("用户ID、行为类型不能为空");
        }
        // 分页参数容错逻辑与原有保持一致
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT * FROM user_behavior_log WHERE user_id=? AND book_isbn=? AND action_type=? ORDER BY create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::buildLog, userId, isbn, actionType, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户+书籍+类型日志失败 userId:{},isbn:{},type:{}", userId, isbn, actionType, e);
            return Collections.emptyList();
        }
    }
}