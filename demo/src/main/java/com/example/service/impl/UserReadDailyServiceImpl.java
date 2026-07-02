package com.example.service.impl;

import com.example.dao.UserInformationDao;
import com.example.dao.UserReadDailyDao;
// 🔥 新增导入缓存Dao和实体
import com.example.dao.UserReadStatsDao;
import com.example.dao.impl.UserInformationDaoImpl;
import com.example.dao.impl.UserReadStatsDaoImpl;
import com.example.dto.ReadStatsDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserInformation;
import com.example.entity.UserReadDaily;
import com.example.entity.UserReadStats;
import com.example.service.UserReadDailyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class UserReadDailyServiceImpl implements UserReadDailyService {
    private static final Logger logger = LoggerFactory.getLogger(UserReadDailyServiceImpl.class);
    private final UserReadDailyDao userReadDailyDao;
    // 🔥 新增：注入阅读统计缓存Dao
    private final UserReadStatsDao userReadStatsDao;

    // 🔥 改造构造方法，传入两个Dao
    public UserReadDailyServiceImpl(UserReadDailyDao userReadDailyDao, UserReadStatsDao userReadStatsDao) {
        this.userReadDailyDao = userReadDailyDao;
        this.userReadStatsDao = userReadStatsDao;
    }

    @Override
    public ResultDTO<Void> upsertReadDuration(UserReadDaily readDaily) {
        try {
            // 🔥 修复：先判断 readDuration 是否为 null，再判断大小
            if (readDaily == null
                    || readDaily.getUserId() == null
                    || readDaily.getReadDuration() == null
                    || readDaily.getReadDuration() <= 0) {
                return ResultDTO.paramError("用户ID、阅读日期和有效时长(秒)不能为空");
            }
            userReadDailyDao.upsertReadDuration(readDaily);
            return ResultDTO.success(null);
        } catch (RuntimeException e) {
            logger.error("新增/更新阅读时长失败", e);
            return ResultDTO.fail("新增/更新阅读时长失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadDaily>> getReadRecords(String userId, LocalDate startDate, LocalDate endDate) {
        try {
            if (userId == null) return ResultDTO.paramError("用户ID不能为空");
            // 默认查近30天
            if (startDate == null) startDate = LocalDate.now().minusDays(30);
            if (endDate == null) endDate = LocalDate.now();
            List<UserReadDaily> records = userReadDailyDao.queryByUserIdAndDateRange(userId, startDate, endDate);
            return ResultDTO.success(records);
        } catch (RuntimeException e) {
            logger.error("查询用户阅读记录失败", e);
            return ResultDTO.fail("查询用户阅读记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<ReadStatsDTO> getReadStats(String userId) {
        try {
            if (userId == null) return ResultDTO.paramError("用户ID不能为空");
            ReadStatsDTO stats = new ReadStatsDTO();

            // ===================== 🔥 核心修改：优先读取缓存 =====================
            UserReadStats cacheStats = userReadStatsDao.getStatsByUserId(userId);
            if (cacheStats != null) {
                // 缓存存在 → 直接用缓存数据，不重复计算（性能拉满）
                stats.setTotalDuration(cacheStats.getTotalDuration());
                stats.setMaxConsecutiveDays(cacheStats.getMaxConsecutiveDays());
                stats.setCurrentConsecutiveDays(cacheStats.getCurrentConsecutiveDays());
                stats.setYearReadDays(cacheStats.getYearReadDays());
                // 近30天记录不缓存，实时查询（保证图表最新）
                stats.setRecentReadRecords(userReadDailyDao.queryByUserIdAndDateRange(userId, LocalDate.now().minusDays(30), LocalDate.now()));
                return ResultDTO.success(stats);
            }
            // ==================================================================

            // 缓存不存在 → 执行原有实时计算逻辑
            // 1. 总阅读时长(秒)
            stats.setTotalDuration(userReadDailyDao.sumTotalDuration(userId));

            // 2. 阅读日期列表
            List<LocalDate> readDates = userReadDailyDao.queryReadDatesByUserId(userId);
            if (readDates.isEmpty()) return ResultDTO.success(stats);

            // 3. 最长连续阅读天数
            stats.setMaxConsecutiveDays(calculateMaxConsecutiveDays(readDates));

            // 4. 最近连续阅读天数（断签则为0）
            stats.setCurrentConsecutiveDays(calculateCurrentConsecutiveDays(readDates));

            // 5. 最近一年阅读天数
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            long yearDays = readDates.stream().filter(d -> d.isAfter(oneYearAgo)).count();
            stats.setYearReadDays((int) yearDays);

            // 6. 近30天阅读记录
            stats.setRecentReadRecords(userReadDailyDao.queryByUserIdAndDateRange(userId, LocalDate.now().minusDays(30), LocalDate.now()));

            return ResultDTO.success(stats);
        } catch (RuntimeException e) {
            logger.error("获取用户阅读统计失败", e);
            return ResultDTO.fail("获取用户阅读统计失败：" + e.getMessage());
        }
    }

    // 工具方法：计算最长连续阅读天数
    private int calculateMaxConsecutiveDays(List<LocalDate> dates) {
        int max = 1, current = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).equals(dates.get(i-1))) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 1;
            }
        }
        return max;
    }

    // 工具方法：计算最近连续阅读天数（断签返回0）
    private int calculateCurrentConsecutiveDays(List<LocalDate> dates) {
        LocalDate lastDate = dates.get(dates.size()-1);
        LocalDate today = LocalDate.now();
        // 断签条件：最后一次阅读在昨天之前
        if (lastDate.isBefore(today.minusDays(1))) return 0;

        int current = 1;
        for (int i = dates.size()-2; i >= 0; i--) {
            if (dates.get(i).plusDays(1).equals(dates.get(i+1))) {
                current++;
            } else break;
        }
        return current;
    }
    public void refreshAllUserStats() {
        UserInformationDao userDao = new UserInformationDaoImpl();
        UserReadStatsDao statsDao = new UserReadStatsDaoImpl();
        List<UserInformation> userList = userDao.queryAll();

        for (UserInformation user : userList) {
            try {
                // 先获取统计结果
                ResultDTO<ReadStatsDTO> statsResult = getReadStats(user.getUserId());
                if (!statsResult.isSuccess() || statsResult.getData() == null) {
                    logger.error("用户[{}]阅读统计获取失败，跳过缓存更新", user.getUserId());
                    continue;
                }

                ReadStatsDTO statsDTO = statsResult.getData();
                UserReadStats stats = new UserReadStats(user.getUserId());
                // 🔥 修复：null值兜底为0
                stats.setTotalDuration(statsDTO.getTotalDuration() == null ? 0 : statsDTO.getTotalDuration());
                stats.setMaxConsecutiveDays(statsDTO.getMaxConsecutiveDays() == null ? 0 : statsDTO.getMaxConsecutiveDays());
                stats.setCurrentConsecutiveDays(statsDTO.getCurrentConsecutiveDays() == null ? 0 : statsDTO.getCurrentConsecutiveDays());
                stats.setYearReadDays(statsDTO.getYearReadDays() == null ? 0 : statsDTO.getYearReadDays());

                statsDao.upsertStats(stats);
                logger.info("手动刷新用户[{}]阅读统计缓存成功", user.getUserId());
            } catch (Exception e) {
                logger.error("手动刷新用户[{}]统计缓存失败", user.getUserId(), e);
                // 单个用户失败不影响整体循环
            }
        }
    }
}