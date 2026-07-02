package com.example.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存登录频率限制器
 * 同一账号/IP 连续失败 5 次后锁定 15 分钟
 */
public class LoginRateLimiter {
    // 最大失败次数
    public static final int MAX_FAILURES = 5;
    // 锁定时长（毫秒）
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    private static final Map<String, FailRecord> records = new ConcurrentHashMap<>();

    private static class FailRecord {
        int count;
        long firstFailTime;
        long lockedUntil;

        FailRecord() {
            this.count = 1;
            this.firstFailTime = System.currentTimeMillis();
            this.lockedUntil = 0;
        }
    }

    /**
     * 记录一次失败，返回 true 表示已被锁定
     */
    public static synchronized boolean recordFailureAndCheckLocked(String key) {
        FailRecord record = records.computeIfAbsent(key, k -> new FailRecord());
        long now = System.currentTimeMillis();

        // 如果锁定期已过，重置计数
        if (record.lockedUntil > 0 && now > record.lockedUntil) {
            record.count = 0;
            record.firstFailTime = now;
            record.lockedUntil = 0;
        }

        if (record.lockedUntil > 0 && now <= record.lockedUntil) {
            // 仍在锁定中
            return true;
        }

        record.count++;
        if (record.count >= MAX_FAILURES) {
            record.lockedUntil = now + LOCK_DURATION_MS;
            return true; // 触发锁定
        }
        return false; // 未锁定，但已记录失败
    }

    /**
     * 登录成功后清除该key的失败记录
     */
    public static void clearOnSuccess(String key) {
        records.remove(key);
    }

    /**
     * 检查是否被锁定（不增加计数）
     */
    public static synchronized boolean isLocked(String key) {
        FailRecord record = records.get(key);
        if (record == null) return false;
        long now = System.currentTimeMillis();
        if (record.lockedUntil > 0 && now > record.lockedUntil) {
            records.remove(key);
            return false;
        }
        return record.lockedUntil > 0;
    }

    /**
     * 获取剩余锁定秒数（用于提示用户）
     */
    public static synchronized long getRemainingLockSeconds(String key) {
        FailRecord record = records.get(key);
        if (record == null || record.lockedUntil <= 0) return 0;
        long remaining = (record.lockedUntil - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }
}
