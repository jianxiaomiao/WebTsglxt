package com.example.service;

import com.example.dto.ResultDTO;
import java.util.List;
import java.util.Map;

public interface AchievementService {
    /** 获取用户成就列表 + 等级信息 */
    ResultDTO<Map<String, Object>> getAchievements(String userId);
    /** 检测并解锁新成就，返回新解锁的列表 */
    ResultDTO<Map<String, Object>> checkAndUnlock(String userId);
}
