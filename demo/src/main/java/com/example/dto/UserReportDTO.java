package com.example.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReportDTO {
    private UserStatsDTO stats;         // 原始统计数据
    private String aiComment;           // AI生成的评语/总结
    private String reportTitle;         // 报告标题：今日阅读日报/本周阅读周报
    // 新增字段
    private String type;       // 报告类型：DAY/WEEK/MONTH
    private String date;       // 报告日期标签
    private LocalDateTime createTime; // 创建时间

    // 生成 getter/setter
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
    public String getDate() {return date;}
    public void setDate(String date) {this.date = date;}
    public LocalDateTime getCreateTime() {return createTime;}
    public void setCreateTime(LocalDateTime createTime) {this.createTime = createTime;}

}