package com.example.service;

import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;

/**
 * 用户统计服务接口
 */
public interface UserStatsService {
    /**
     * 生成用户统计数据 (日报/周报/月报)
     * @param userId 用户ID
     * @param type 类型：DAY/WEEK/MONTH
     * @return 封装好的统计DTO
     */
    ResultDTO<UserStatsDTO> generateUserStats(String userId, String type);
}