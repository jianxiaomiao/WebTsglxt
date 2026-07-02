package com.example.service;

import com.example.dto.ResultDTO;
import com.example.dto.UserReportDTO;

import java.util.List;

public interface UserReportService {
    /**
     * 生成用户完整报告（统计数据 + AI评语 + 标题）
     */
    ResultDTO<UserReportDTO> generateUserReport(String userId, String type);

    ResultDTO<List<UserReportDTO>> getReportHistory(String userId, String type, Integer pageNum, Integer pageSize);

    ResultDTO<UserReportDTO> getReportByDate(String userId, String type, String date);
}