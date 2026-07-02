package com.example.entity;

import java.time.LocalDateTime;

public class UserReadStats {
    private Integer id;
    private String userId;
    // 🔥 核心修复：数值字段默认赋值0，永不null
    private Integer totalDuration = 0;
    private Integer maxConsecutiveDays = 0;
    private Integer currentConsecutiveDays = 0;
    private Integer yearReadDays = 0;
    private LocalDateTime lastCalcTime; // 最后统计时间

    // 无参/全参构造
    public UserReadStats() {}
    public UserReadStats(String userId) {
        this.userId = userId;
        this.totalDuration = 0;
        this.maxConsecutiveDays = 0;
        this.currentConsecutiveDays = 0;
        this.yearReadDays = 0;
        this.lastCalcTime = LocalDateTime.now();
    }

    // Getter & Setter
    public Integer getId() {return id;}
    public void setId(Integer id) {this.id = id;}
    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}
    public Integer getTotalDuration() {return totalDuration;}
    public void setTotalDuration(Integer totalDuration) {this.totalDuration = totalDuration;}
    public Integer getMaxConsecutiveDays() {return maxConsecutiveDays;}
    public void setMaxConsecutiveDays(Integer maxConsecutiveDays) {this.maxConsecutiveDays = maxConsecutiveDays;}
    public Integer getCurrentConsecutiveDays() {return currentConsecutiveDays;}
    public void setCurrentConsecutiveDays(Integer currentConsecutiveDays) {this.currentConsecutiveDays = currentConsecutiveDays;}
    public Integer getYearReadDays() {return yearReadDays;}
    public void setYearReadDays(Integer yearReadDays) {this.yearReadDays = yearReadDays;}
    public LocalDateTime getLastCalcTime() {return lastCalcTime;}
    public void setLastCalcTime(LocalDateTime lastCalcTime) {this.lastCalcTime = lastCalcTime;}
}