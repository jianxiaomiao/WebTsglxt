package com.example.util;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 调用频率限制器（滑动窗口）
 * 每个用户在时间窗口内最多调用 N 次 AI
 */
public class AiRateLimiter {

    /** 每分钟最大调用次数 */
    private static final int MAX_CALLS_PER_MINUTE = 10;
    /** 时间窗口（毫秒） */
    private static final long WINDOW_MS = 60_000;
    /** 每日窗口（毫秒） */
    private static final long DAY_WINDOW_MS = 24 * 60 * 60 * 1000;

    /** userId → 每分钟调用时间戳队列 */
    private static final ConcurrentHashMap<String, LinkedList<Long>> userCalls = new ConcurrentHashMap<>();
    /** userId → 每日调用时间戳队列 */
    private static final ConcurrentHashMap<String, LinkedList<Long>> userDailyCalls = new ConcurrentHashMap<>();

    /**
     * 尝试获取一次 AI 调用许可（每分钟限流）
     * @param userId 用户标识
     * @return true=允许调用, false=限流中
     */
    public static synchronized boolean tryAcquire(String userId) {
        if (userId == null || userId.isBlank()) return true;
        return tryAcquireInternal(userId, MAX_CALLS_PER_MINUTE, WINDOW_MS, userCalls);
    }

    /**
     * 尝试获取一次每日调用许可
     * @param userId 用户标识
     * @param maxPerDay 每日最大次数
     * @return true=允许调用, false=限流中
     */
    public static synchronized boolean tryAcquireDaily(String userId, int maxPerDay) {
        if (userId == null || userId.isBlank()) return true;
        return tryAcquireInternal(userId, maxPerDay, DAY_WINDOW_MS, userDailyCalls);
    }

    private static boolean tryAcquireInternal(String userId, int maxCalls, long windowMs,
                                               ConcurrentHashMap<String, LinkedList<Long>> store) {
        long now = System.currentTimeMillis();
        LinkedList<Long> timestamps = store.computeIfAbsent(userId, k -> new LinkedList<>());

        // 清除窗口外的旧记录
        while (!timestamps.isEmpty() && now - timestamps.getFirst() > windowMs) {
            timestamps.removeFirst();
        }

        if (timestamps.size() >= maxCalls) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * 获取距离下次可调用还需要等待的秒数（每分钟窗口）
     * @return 等待秒数，0 表示现在就可以调用
     */
    public static long getWaitSeconds(String userId) {
        return getWaitSecondsInternal(userId, MAX_CALLS_PER_MINUTE, WINDOW_MS, userCalls);
    }

    /**
     * 获取距离下次每日调用还需要等待的秒数
     * @return 等待秒数，0 表示现在就可以调用
     */
    public static long getDailyWaitSeconds(String userId, int maxPerDay) {
        return getWaitSecondsInternal(userId, maxPerDay, DAY_WINDOW_MS, userDailyCalls);
    }

    private static long getWaitSecondsInternal(String userId, int maxCalls, long windowMs,
                                                ConcurrentHashMap<String, LinkedList<Long>> store) {
        if (userId == null || userId.isBlank()) return 0;

        LinkedList<Long> timestamps = store.get(userId);
        if (timestamps == null || timestamps.isEmpty()) return 0;

        long now = System.currentTimeMillis();
        synchronized (AiRateLimiter.class) {
            while (!timestamps.isEmpty() && now - timestamps.getFirst() > windowMs) {
                timestamps.removeFirst();
            }
        }

        if (timestamps.size() < maxCalls) return 0;

        long oldest = timestamps.getFirst();
        long remainingMs = windowMs - (now - oldest);
        return (long) Math.ceil(remainingMs / 1000.0);
    }
}
