package com.example.dto;

import com.example.entity.UserReadDaily;
import java.util.List;

public class ReadStatsDTO {
    private Integer totalDuration; // 总阅读时长（分钟）
    private Integer maxConsecutiveDays; // 最长连续阅读天数
    private Integer currentConsecutiveDays; // 最近连续阅读天数
    private Integer yearReadDays; // 最近一年阅读天数
    private List<UserReadDaily> recentReadRecords; // 近30天阅读记录

    // Getter & Setter
    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }
    public Integer getMaxConsecutiveDays() { return maxConsecutiveDays; }
    public void setMaxConsecutiveDays(Integer maxConsecutiveDays) { this.maxConsecutiveDays = maxConsecutiveDays; }
    public Integer getCurrentConsecutiveDays() { return currentConsecutiveDays; }
    public void setCurrentConsecutiveDays(Integer currentConsecutiveDays) { this.currentConsecutiveDays = currentConsecutiveDays; }
    public Integer getYearReadDays() { return yearReadDays; }
    public void setYearReadDays(Integer yearReadDays) { this.yearReadDays = yearReadDays; }
    public List<UserReadDaily> getRecentReadRecords() { return recentReadRecords; }
    public void setRecentReadRecords(List<UserReadDaily> recentReadRecords) { this.recentReadRecords = recentReadRecords; }
}