package com.example.util;

/**
 * 基于 Redis 的登录频率限制器（分布式支持，防暴力破解）
 * 同一账号连续失败 5 次后锁定 15 分钟
 */
public class LoginRateLimiter {
    // 最大失败次数
    public static final int MAX_FAILURES = 5;
    
    // 锁定时长（秒）
    private static final int LOCK_DURATION_SECS = 15 * 60; 

    /**
     * 记录一次失败，返回 true 表示已被锁定
     */
    public static boolean recordFailureAndCheckLocked(String key) {
        String failKey = "login:fail:" + key;
        String lockKey = "login:lock:" + key;

        if (isLocked(key)) {
            return true;
        }

        long count = 1;
        try {
            count = RedisUtil.incr(failKey);
            if (count == 1) {
                RedisUtil.expire(failKey, LOCK_DURATION_SECS);
            }
        } catch (Exception e) {
            // Redis 异常时降级，不阻断正常登录流程
        }

        if (count >= MAX_FAILURES) {
            try {
                RedisUtil.set(lockKey, "locked", LOCK_DURATION_SECS);
                RedisUtil.del(failKey);
            } catch (Exception e) {
                // Redis 异常时降级
            }
            return true; // 触发锁定
        }
        return false; // 未锁定，但已记录失败
    }

    /**
     * 登录成功后清除该key的失败记录和锁状态
     */
    public static void clearOnSuccess(String key) {
        try {
            RedisUtil.del("login:fail:" + key);
            RedisUtil.del("login:lock:" + key);
        } catch (Exception e) {
            // Redis 异常时降级
        }
    }

    /**
     * 检查是否被锁定（不增加计数）
     */
    public static boolean isLocked(String key) {
        try {
            return RedisUtil.exists("login:lock:" + key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取剩余锁定秒数（用于提示用户）
     */
    public static long getRemainingLockSeconds(String key) {
        try {
            long remaining = RedisUtil.ttl("login:lock:" + key);
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
