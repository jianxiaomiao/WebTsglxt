package com.example.dao.impl;

import com.example.dao.UserProfileMemoryDao;
import com.example.entity.UserProfileMemory;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProfileMemoryDaoImpl implements UserProfileMemoryDao {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileMemoryDaoImpl.class);

    @Override
    public void add(UserProfileMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("用户画像不能为空");
        }
        try {
            String sql = "INSERT INTO user_profile_memory(user_id, date, reading_preferences, emotional_state, character_sketch) VALUES (?,?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    memory.getUser_id(),
                    memory.getDate(),
                    memory.getReading_preferences(),
                    memory.getEmotional_state(),
                    memory.getCharacter_sketch());
            if (rows == 0) {
                throw new RuntimeException("新增用户画像失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增用户画像失败", e);
            throw new RuntimeException("新增用户画像异常", e);
        }
    }

    @Override
    public void update(UserProfileMemory memory) {
        if (memory == null || memory.getId() == null) {
            throw new IllegalArgumentException("画像对象/画像ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE user_profile_memory SET ");
            List<Object> params = new ArrayList<>();

            if (memory.getUser_id() != null) {
                sql.append("user_id=?, ");
                params.add(memory.getUser_id());
            }
            if (memory.getDate() != null) {
                sql.append("date=?, ");
                params.add(memory.getDate());
            }
            if (memory.getReading_preferences() != null) {
                sql.append("reading_preferences=?, ");
                params.add(memory.getReading_preferences());
            }
            if (memory.getEmotional_state() != null) {
                sql.append("emotional_state=?, ");
                params.add(memory.getEmotional_state());
            }
            if (memory.getCharacter_sketch() != null) {
                sql.append("character_sketch=?, ");
                params.add(memory.getCharacter_sketch());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(memory.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新画像 SQL:{} 参数:{}", sql, params);
            if (rows == 0) {
                throw new RuntimeException("更新失败，未匹配ID：" + memory.getId());
            }
        } catch (SQLException e) {
            logger.error("更新画像失败，ID:{}", memory.getId(), e);
            throw new RuntimeException("更新画像异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("画像ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM user_profile_memory WHERE id=?", id);
            if (rows == 0) {
                throw new RuntimeException("删除失败，未匹配ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除画像失败，ID:{}", id, e);
            throw new RuntimeException("删除画像异常", e);
        }
    }

    @Override
    public List<UserProfileMemory> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("画像ID不能为空");
        }
        try {
            String sql = "SELECT * FROM user_profile_memory WHERE id=?";
            return DBUtil.executeQuery(sql, rs -> {
                UserProfileMemory memory = new UserProfileMemory();
                memory.setId(rs.getInt("id"));
                memory.setUser_id(rs.getString("user_id"));
                memory.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
                memory.setReading_preferences(rs.getString("reading_preferences"));
                memory.setEmotional_state(rs.getString("emotional_state"));
                memory.setCharacter_sketch(rs.getString("character_sketch"));
                return memory;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询画像失败，ID:{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserProfileMemory> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            String sql = "SELECT * FROM user_profile_memory WHERE user_id=? ORDER BY date DESC";
            return DBUtil.executeQuery(sql, rs -> {
                UserProfileMemory memory = new UserProfileMemory();
                memory.setId(rs.getInt("id"));
                memory.setUser_id(rs.getString("user_id"));
                memory.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
                memory.setReading_preferences(rs.getString("reading_preferences"));
                memory.setEmotional_state(rs.getString("emotional_state"));
                memory.setCharacter_sketch(rs.getString("character_sketch"));
                return memory;
            }, userId);
        } catch (SQLException e) {
            logger.error("根据用户ID查询画像失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserProfileMemory> queryByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        try {
            String sql = "SELECT * FROM user_profile_memory WHERE user_id=? AND date BETWEEN ? AND ? ORDER BY date DESC";
            return DBUtil.executeQuery(sql, rs -> {
                UserProfileMemory memory = new UserProfileMemory();
                memory.setId(rs.getInt("id"));
                memory.setUser_id(rs.getString("user_id"));
                memory.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
                memory.setReading_preferences(rs.getString("reading_preferences"));
                memory.setEmotional_state(rs.getString("emotional_state"));
                memory.setCharacter_sketch(rs.getString("character_sketch"));
                return memory;
            }, userId, startDate, endDate);
        } catch (SQLException e) {
            logger.error("日期区间查询画像失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_profile_memory WHERE user_id=? AND date BETWEEN ? AND ?";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId, startDate, endDate);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计日期区间画像总数失败", e);
            return 0L;
        }
    }

    @Override
    public List<UserProfileMemory> queryLatestByUserId(String userId, Integer limit) {
        if (userId == null || userId.isEmpty() || limit == null || limit < 1) {
            throw new IllegalArgumentException("参数非法");
        }
        try {
            String sql = "SELECT * FROM user_profile_memory WHERE user_id=? ORDER BY date DESC LIMIT ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserProfileMemory memory = new UserProfileMemory();
                memory.setId(rs.getInt("id"));
                memory.setUser_id(rs.getString("user_id"));
                memory.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
                memory.setReading_preferences(rs.getString("reading_preferences"));
                memory.setEmotional_state(rs.getString("emotional_state"));
                memory.setCharacter_sketch(rs.getString("character_sketch"));
                return memory;
            }, userId, limit);
        } catch (SQLException e) {
            logger.error("查询用户最新N条画像失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserProfileMemory> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT * FROM user_profile_memory WHERE user_id=? ORDER BY date DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserProfileMemory memory = new UserProfileMemory();
                memory.setId(rs.getInt("id"));
                memory.setUser_id(rs.getString("user_id"));
                memory.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
                memory.setReading_preferences(rs.getString("reading_preferences"));
                memory.setEmotional_state(rs.getString("emotional_state"));
                memory.setCharacter_sketch(rs.getString("character_sketch"));
                return memory;
            }, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户画像失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM user_profile_memory WHERE user_id=?";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计用户画像总数失败，userId:{}", userId, e);
            return 0L;
        }
    }
}