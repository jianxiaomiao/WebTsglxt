package com.example.dao.impl;

import com.example.dao.AchievementDefDao;
import com.example.entity.AchievementDef;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class AchievementDefDaoImpl implements AchievementDefDao {

    private static final Logger logger = LoggerFactory.getLogger(AchievementDefDaoImpl.class);

    @Override
    public List<AchievementDef> queryAll() {
        try {
            String sql = "SELECT id, name, description, category, rarity, condition_json, exp_reward FROM achievement_def ORDER BY id";
            return DBUtil.executeQuery(sql, this::mapRow);
        } catch (SQLException e) {
            logger.error("查询所有成就定义异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public AchievementDef queryById(String id) {
        if (id == null) return null;
        try {
            String sql = "SELECT id, name, description, category, rarity, condition_json, exp_reward FROM achievement_def WHERE id=?";
            List<AchievementDef> list = DBUtil.executeQuery(sql, this::mapRow, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询成就定义异常 id={}", id, e);
            return null;
        }
    }

    private AchievementDef mapRow(java.sql.ResultSet rs) throws SQLException {
        return new AchievementDef(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("rarity"),
                rs.getString("condition_json"),
                rs.getInt("exp_reward")
        );
    }
}
