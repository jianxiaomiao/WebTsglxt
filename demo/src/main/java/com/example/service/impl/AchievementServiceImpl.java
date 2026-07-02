package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.AchievementDefDao;
import com.example.dao.UserAchievementDao;
import com.example.dao.impl.AchievementDefDaoImpl;
import com.example.dao.impl.UserAchievementDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.AchievementDef;
import com.example.entity.UserAchievement;
import com.example.service.AchievementService;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class AchievementServiceImpl implements AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementServiceImpl.class);
    private final AchievementDefDao achievementDefDao;
    private final UserAchievementDao userAchievementDao;

    public AchievementServiceImpl() {
        this.achievementDefDao = new AchievementDefDaoImpl();
        this.userAchievementDao = new UserAchievementDaoImpl();
    }

    public AchievementServiceImpl(AchievementDefDao achievementDefDao, UserAchievementDao userAchievementDao) {
        this.achievementDefDao = achievementDefDao;
        this.userAchievementDao = userAchievementDao;
    }

    @Override
    public ResultDTO<Map<String, Object>> getAchievements(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResultDTO.paramError("用户ID不能为空");
            }

            // 1. 查询所有成就定义
            List<AchievementDef> allDefs = achievementDefDao.queryAll();
            // 2. 查询用户已解锁的成就
            List<UserAchievement> unlocked = userAchievementDao.queryByUserId(userId);
            Set<String> unlockedIds = new HashSet<>();
            Map<String, UserAchievement> unlockedMap = new HashMap<>();
            for (UserAchievement ua : unlocked) {
                unlockedIds.add(ua.getAchievementId());
                unlockedMap.put(ua.getAchievementId(), ua);
            }

            // 3. 组装徽章列表（含解锁状态）
            List<Map<String, Object>> badges = new ArrayList<>();
            for (AchievementDef def : allDefs) {
                Map<String, Object> badge = new LinkedHashMap<>();
                badge.put("id", def.getId());
                badge.put("name", def.getName());
                badge.put("description", def.getDescription());
                badge.put("category", def.getCategory());
                badge.put("rarity", def.getRarity());
                badge.put("expReward", def.getExpReward());
                if (unlockedIds.contains(def.getId())) {
                    badge.put("isUnlocked", true);
                    badge.put("unlockDate", unlockedMap.get(def.getId()).getUnlockedAt().toString());
                } else {
                    badge.put("isUnlocked", false);
                    badge.put("unlockDate", null);
                }
                badges.add(badge);
            }

            // 4. 计算等级（每100经验升1级，每解锁1个成就获得其exp_reward）
            int totalExp = unlocked.stream()
                    .mapToInt(ua -> {
                        AchievementDef def = findDef(allDefs, ua.getAchievementId());
                        return def != null ? def.getExpReward() : 0;
                    })
                    .sum();
            int level = Math.max(1, totalExp / 100 + 1);
            int nextExp = level * 100;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("badges", badges);
            result.put("level", level);
            result.put("exp", totalExp);
            result.put("nextExp", nextExp);

            // 附加阅读统计（供前端展示）
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalBooks", queryInt("SELECT COUNT(DISTINCT isbn) FROM user_read_record WHERE user_id=?", userId));
            stats.put("totalDuration", queryInt("SELECT COALESCE(total_duration,0) FROM user_read_stats WHERE user_id=?", userId));
            stats.put("consecutiveDays", queryInt("SELECT COALESCE(current_consecutive_days,0) FROM user_read_stats WHERE user_id=?", userId));
            result.put("stats", stats);

            return ResultDTO.success(result);
        } catch (RuntimeException e) {
            logger.error("获取成就列表异常 userId={}", userId, e);
            return ResultDTO.fail("获取成就列表失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Map<String, Object>> checkAndUnlock(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResultDTO.paramError("用户ID不能为空");
            }

            List<AchievementDef> allDefs = achievementDefDao.queryAll();
            if (allDefs.isEmpty()) {
                return ResultDTO.success(Map.of("newlyUnlocked", Collections.emptyList()));
            }

            // 收集用户各项统计
            int totalBooks = queryInt("SELECT COUNT(DISTINCT isbn) FROM user_read_record WHERE user_id=?", userId);
            int totalDuration = queryInt("SELECT COALESCE(total_duration,0) FROM user_read_stats WHERE user_id=?", userId);
            int consecutiveDays = queryInt("SELECT COALESCE(current_consecutive_days,0) FROM user_read_stats WHERE user_id=?", userId);
            int totalPosts = queryInt("SELECT COUNT(*) FROM user_comment WHERE UserId=?", userId);
            int totalLikes = queryInt(
                    "SELECT COUNT(*) FROM user_comment_like l INNER JOIN user_comment c ON l.comment_id=c.CommentId WHERE c.UserId=?",
                    userId);
            int totalFriends = queryInt("SELECT COUNT(*) FROM friend WHERE user_id=?", userId);
            int totalCollections = queryInt("SELECT COUNT(*) FROM user_collection WHERE UserId=?", userId);
            int totalGroups = queryInt("SELECT COUNT(*) FROM user_bookshelf_group WHERE UserId=?", userId);
            // AI 互动次数
            int aiInteractionCount = queryInt(
                    "SELECT COUNT(*) FROM ai_interaction_history WHERE user_id=? AND interaction_type=1", userId);

            List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

            for (AchievementDef def : allDefs) {
                // 已解锁则跳过
                if (userAchievementDao.exists(userId, def.getId())) continue;

                boolean met = checkCondition(def.getConditionJson(),
                        totalBooks, totalDuration, consecutiveDays,
                        totalPosts, totalLikes, totalFriends,
                        totalCollections, totalGroups, aiInteractionCount);

                if (met) {
                    UserAchievement ua = new UserAchievement(userId, def.getId());
                    userAchievementDao.add(ua);

                    Map<String, Object> badge = new LinkedHashMap<>();
                    badge.put("id", def.getId());
                    badge.put("name", def.getName());
                    badge.put("description", def.getDescription());
                    badge.put("rarity", def.getRarity());
                    badge.put("icon", "/assets/badges/" + def.getRarity() + "_" + def.getId() + ".png");
                    newlyUnlocked.add(badge);

                    logger.info("🏆 成就解锁 userId={} achievement={}", userId, def.getName());
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("newlyUnlocked", newlyUnlocked);
            return ResultDTO.success(result);
        } catch (RuntimeException e) {
            logger.error("检测成就异常 userId={}", userId, e);
            return ResultDTO.fail("成就检测失败：" + e.getMessage());
        }
    }

    // ====================== 条件检测 ======================

    private boolean checkCondition(String conditionJson, int totalBooks, int totalDuration,
                                    int consecutiveDays, int totalPosts, int totalLikes,
                                    int totalFriends, int totalCollections, int totalGroups,
                                    int aiInteractionCount) {
        if (conditionJson == null || conditionJson.isBlank()) return false;
        try {
            JSONObject cond = JSON.parseObject(conditionJson);
            String type = cond.getString("type");
            int threshold = cond.getIntValue("threshold");

            return switch (type) {
                case "total_books" -> totalBooks >= threshold;
                case "total_duration" -> totalDuration >= threshold;
                case "consecutive_days" -> consecutiveDays >= threshold;
                case "total_posts" -> totalPosts >= threshold;
                case "total_likes" -> totalLikes >= threshold;
                case "total_friends" -> totalFriends >= threshold;
                case "total_collections" -> totalCollections >= threshold;
                case "total_groups" -> totalGroups >= threshold;
                case "ai_interaction" -> aiInteractionCount >= threshold;
                default -> false;
            };
        } catch (Exception e) {
            logger.warn("解析成就条件失败 conditionJson={}", conditionJson, e);
            return false;
        }
    }

    // ====================== 工具方法 ======================

    private int queryInt(String sql, Object... params) {
        try {
            Integer val = DBUtil.executeQueryScalar(sql, Integer.class, params);
            return val != null ? val : 0;
        } catch (SQLException e) {
            logger.warn("统计查询异常 sql={}", sql, e);
            return 0;
        }
    }

    private AchievementDef findDef(List<AchievementDef> defs, String id) {
        for (AchievementDef d : defs) {
            if (d.getId().equals(id)) return d;
        }
        return null;
    }
}
