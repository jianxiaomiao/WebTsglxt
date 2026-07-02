package com.example.dao.impl;

import com.example.dao.AiInteractionHistoryDao;
import com.example.entity.AiInteractionHistory;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI智能对话与对冲记录 DAO 实现类
 */
public class AiInteractionHistoryDaoImpl implements AiInteractionHistoryDao {
    private static final Logger logger = LoggerFactory.getLogger(AiInteractionHistoryDaoImpl.class);

    @Override
    public void add(AiInteractionHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("AI交互记录不能为空");
        }
        try {
            String sql = "INSERT INTO ai_interaction_history(user_id, isbn, interaction_type, user_input, ai_response, summary_tag) VALUES (?,?,?,?,?,?)";
            int rows = DBUtil.executeUpdate(sql,
                    history.getUser_id(),
                    history.getIsbn(),
                    history.getInteraction_type(),
                    history.getUser_input(),
                    history.getAi_response(),
                    history.getSummary_tag());
            if (rows == 0) {
                throw new RuntimeException("新增AI交互记录失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增AI交互记录失败", e);
            throw new RuntimeException("新增AI交互记录异常", e);
        }
    }

    @Override
    public void update(AiInteractionHistory history) {
        if (history == null || history.getId() == null) {
            throw new IllegalArgumentException("交互记录/记录ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE ai_interaction_history SET ");
            List<Object> params = new ArrayList<>();

            if (history.getUser_id() != null) {
                sql.append("user_id=?, ");
                params.add(history.getUser_id());
            }
            if (history.getIsbn() != null) {
                sql.append("isbn=?, ");
                params.add(history.getIsbn());
            }
            if (history.getInteraction_type() != null) {
                sql.append("interaction_type=?, ");
                params.add(history.getInteraction_type());
            }
            if (history.getUser_input() != null) {
                sql.append("user_input=?, ");
                params.add(history.getUser_input());
            }
            if (history.getAi_response() != null) {
                sql.append("ai_response=?, ");
                params.add(history.getAi_response());
            }
            if (history.getSummary_tag() != null) {
                sql.append("summary_tag=?, ");
                params.add(history.getSummary_tag());
            }

            // 剔除最后多余逗号与空格
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(history.getId());

            int rows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新AI交互记录 SQL:{} 参数:{}", sql, params);

            if (rows == 0) {
                throw new RuntimeException("更新失败，未匹配ID：" + history.getId());
            }
        } catch (SQLException e) {
            logger.error("更新AI交互记录失败，ID:{}", history.getId(), e);
            throw new RuntimeException("更新AI交互记录异常", e);
        }
    }

    @Override
    public void del(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("记录ID不能为空");
        }
        try {
            int rows = DBUtil.executeUpdate("DELETE FROM ai_interaction_history WHERE id=?", id);
            if (rows == 0) {
                throw new RuntimeException("删除失败，未匹配ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除AI交互记录失败，ID:{}", id, e);
            throw new RuntimeException("删除AI交互记录异常", e);
        }
    }

    @Override
    public List<AiInteractionHistory> queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("记录ID不能为空");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE id=?";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询AI交互记录失败，ID:{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AiInteractionHistory> queryByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE user_id=? ORDER BY id DESC";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, userId);
        } catch (SQLException e) {
            logger.error("根据用户ID查询AI交互记录失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AiInteractionHistory> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE user_id=? AND create_time BETWEEN ? AND ? ORDER BY id DESC";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, userId, startTime, endTime);
        } catch (SQLException e) {
            logger.error("时间段查询AI交互记录失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || startTime == null || endTime == null) {
            return 0L;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM ai_interaction_history WHERE user_id=? AND create_time BETWEEN ? AND ?";
            List<Long> result = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId, startTime, endTime);
            return result.isEmpty() ? 0L : result.get(0);
        } catch (SQLException e) {
            logger.error("统计时间段AI交互记录总数失败", e);
            return 0L;
        }
    }

    @Override
    public List<AiInteractionHistory> queryLatestByUserId(String userId, Integer limit) {
        if (userId == null || userId.isEmpty() || limit == null || limit < 1) {
            throw new IllegalArgumentException("参数非法，用户ID或条数不合法");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE user_id=? ORDER BY id DESC LIMIT ?";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, userId, limit);
        } catch (SQLException e) {
            logger.error("查询用户最新N条AI交互记录失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AiInteractionHistory> queryLatestByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 分页参数容错
        pageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        pageSize = (pageSize == null || pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE user_id=? ORDER BY id DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户AI交互记录失败，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0L;
        }
        try {
            String sql = "SELECT COUNT(*) AS total FROM ai_interaction_history WHERE user_id=?";
            List<Long> result = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId);
            return result.isEmpty() ? 0L : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户AI交互记录总数失败，userId:{}", userId, e);
            return 0L;
        }
    }

    @Override
    public List<AiInteractionHistory> queryByUserIdAndIsbn(String userId, String isbn) {
        if (userId == null || userId.isEmpty() || isbn == null || isbn.isEmpty()) {
            throw new IllegalArgumentException("用户ID和ISBN不能为空");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE user_id=? AND isbn=? ORDER BY id DESC";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, userId, isbn);
        } catch (SQLException e) {
            logger.error("根据用户ID+ISBN查询AI交互记录失败，userId:{},isbn:{}", userId, isbn, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AiInteractionHistory> queryBySummaryTag(String summaryTag) {
        if (summaryTag == null || summaryTag.isEmpty()) {
            throw new IllegalArgumentException("记忆锚点summary_tag不能为空");
        }
        try {
            String sql = "SELECT * FROM ai_interaction_history WHERE summary_tag=? ORDER BY id DESC";
            return DBUtil.executeQuery(sql, rs -> {
                AiInteractionHistory history = new AiInteractionHistory();
                history.setId(rs.getInt("id"));
                history.setUser_id(rs.getString("user_id"));
                history.setIsbn(rs.getString("isbn"));
                history.setInteraction_type(rs.getInt("interaction_type"));
                history.setUser_input(rs.getString("user_input"));
                history.setAi_response(rs.getString("ai_response"));
                history.setSummary_tag(rs.getString("summary_tag"));
                return history;
            }, summaryTag);
        } catch (SQLException e) {
            logger.error("根据记忆锚点查询AI交互记录失败，summaryTag:{}", summaryTag, e);
            return Collections.emptyList();
        }
    }
}