package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/**
 * Redis 工具类 — Jedis 连接池封装（生产级优化版）
 * <p>
 * 适配你 redis.conf 的配置：requirepass 123456 / db 0
 * <p>
 * 用法示例：
 * <pre>{@code
 *   // 读取缓存
 *   try {
 *     String data = RedisUtil.get("book:9787");
 *     if (data != null) return data;
 *   } catch (JedisException e) {
 *     // Redis 挂了，走降级（直接查 DB）
 *   }
 *
 *   // 写入缓存
 *   RedisUtil.set("book:9787", bookJson, 1800);
 *
 *   // 分布式锁（安全释放）
 *   String lockVal = RedisUtil.tryLock("order:lock", 30);
 *   if (lockVal != null) {
 *     try { /* 业务
}
*        finally { RedisUtil.unlock("order:lock", lockVal); }
 *   }
 * }</pre>
 * */

public class RedisUtil {
    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private static JedisPool jedisPool;

    // ====== 全局常量 ======
    private static final int DEFAULT_DB = 0;
    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final int MAX_WAIT_MILLIS = 3000;  // 获取连接最长等待 3 秒

    static {
        initPool();
    }

    /**
     * 初始化连接池。
     * 读取 config.properties → 环境变量覆盖 → 默认值。
     * 初始化失败直接抛 RuntimeException，阻止程序在无 Redis 状态下运行。
     */
    private static void initPool() {
        try {
            Properties props = new Properties();
            String host = "localhost";
            int port = 6379;
            String password = null;
            int maxTotal = 10;
            int maxIdle = 5;
            int minIdle = 1;
            int timeout = DEFAULT_CONNECT_TIMEOUT;
            int db = DEFAULT_DB;

            // 1. 读取配置文件
            try (InputStream in = RedisUtil.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (in != null) {
                    props.load(in);
                    host = props.getProperty("redis.host", host);
                    port = Integer.parseInt(props.getProperty("redis.port", String.valueOf(port)));
                    String pwd = props.getProperty("redis.password");
                    if (pwd != null && !pwd.isBlank()) password = pwd;  // 过滤空字符串
                    maxTotal = Integer.parseInt(props.getProperty("redis.maxTotal", String.valueOf(maxTotal)));
                    maxIdle = Integer.parseInt(props.getProperty("redis.maxIdle", String.valueOf(maxIdle)));
                    timeout = Integer.parseInt(props.getProperty("redis.timeout", String.valueOf(timeout)));
                    db = Integer.parseInt(props.getProperty("redis.db", String.valueOf(db)));
                }
            } catch (Exception ignored) {
                logger.warn("读取 config.properties 失败，使用默认 Redis 配置");
            }

            // 2. 环境变量覆盖
            host = System.getenv().getOrDefault("REDIS_HOST", host);
            port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", String.valueOf(port)));
            String envPwd = System.getenv("REDIS_PASSWORD");
            if (envPwd != null && !envPwd.isBlank()) password = envPwd;
            String envDb = System.getenv("REDIS_DB");
            if (envDb != null && !envDb.isBlank()) db = Integer.parseInt(envDb);

            // 3. 生产级连接池配置
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMinIdle(minIdle);
            poolConfig.setTestOnBorrow(false);                     // 不每次借连接都 PING
            poolConfig.setTestWhileIdle(true);                     // 空闲时自动校验
            poolConfig.setTimeBetweenEvictionRunsMillis(30000);    // 30 秒检测一次
            poolConfig.setMinEvictableIdleTimeMillis(60000);       // 空闲 1 分钟回收
            poolConfig.setMaxWait(Duration.ofMillis(MAX_WAIT_MILLIS)); // 拿连接超时不阻塞

            jedisPool = new JedisPool(poolConfig, host, port, timeout, password, db);

            // 4. 启动时 PING 验证连通性
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            logger.info("✅ Redis 连接池初始化成功 {}:{} db={}", host, port, db);
        } catch (Exception e) {
            logger.error("❌ Redis 连接池初始化失败，缓存功能完全不可用", e);
            jedisPool = null;
            throw new RuntimeException("Redis 初始化失败，程序无法启动", e);
        }
    }

    // ====== 私有工具：获取连接（判空保护） ======
    private static Jedis getJedis() {
        if (jedisPool == null) {
            throw new JedisConnectionException("Redis 连接池未初始化");
        }
        return jedisPool.getResource();
    }

    // ====================== 字符串操作 ======================

    /** 写入缓存（带过期时间，秒） */
    public static void set(String key, String value, int ttlSeconds) {
        try (Jedis jedis = getJedis()) {
            jedis.setex(key, ttlSeconds, value);
        } catch (JedisException e) {
            logger.error("Redis SETEX 失败 key={}", key, e);
            throw e;  // 抛出异常，上游降级
        }
    }

    /** 写入缓存（永不过期） */
    public static void set(String key, String value) {
        try (Jedis jedis = getJedis()) {
            jedis.set(key, value);
        } catch (JedisException e) {
            logger.error("Redis SET 失败 key={}", key, e);
            throw e;
        }
    }

    /**
     * 读取缓存。
     * @return 缓存值；null 表示 key 不存在
     * @throws JedisException Redis 服务故障（业务可用此区分 "key 不存在" vs "服务挂了"）
     */
    public static String get(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.get(key);
        } catch (JedisException e) {
            logger.error("Redis GET 失败 key={}", key, e);
            throw e;
        }
    }

    /** 删除缓存 */
    public static void del(String key) {
        try (Jedis jedis = getJedis()) {
            jedis.del(key);
        } catch (JedisException e) {
            logger.error("Redis DEL 失败 key={}", key, e);
            throw e;
        }
    }

    /** 判断 key 是否存在 */
    public static boolean exists(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.exists(key);
        } catch (JedisException e) {
            logger.error("Redis EXISTS 失败 key={}", key, e);
            throw e;
        }
    }

    /** 设置过期时间（秒） */
    public static void expire(String key, int ttlSeconds) {
        try (Jedis jedis = getJedis()) {
            jedis.expire(key, ttlSeconds);
        } catch (JedisException e) {
            logger.error("Redis EXPIRE 失败 key={}", key, e);
            throw e;
        }
    }

    // ====================== 计数器 ======================

    /** 自增 +1，返回新值 */
    public static long incr(String key) {
        try (Jedis jedis = getJedis()) {
            return jedis.incr(key);
        } catch (JedisException e) {
            logger.error("Redis INCR 失败 key={}", key, e);
            throw e;
        }
    }

    /** 自增指定值 */
    public static long incrBy(String key, long delta) {
        try (Jedis jedis = getJedis()) {
            return jedis.incrBy(key, delta);
        } catch (JedisException e) {
            logger.error("Redis INCRBY 失败 key={}", key, e);
            throw e;
        }
    }

    // ====================== 分布式锁 ======================

    /**
     * 获取分布式锁。
     * @param lockKey    锁 key
     * @param ttlSeconds 锁自动释放时间（秒）
     * @return 锁的唯一标识（释放时必须传入），null 表示获取失败
     */
    public static String tryLock(String lockKey, int ttlSeconds) {
        String lockValue = UUID.randomUUID().toString();  // 唯一标识，防止误删
        try (Jedis jedis = getJedis()) {
            String res = jedis.set(lockKey, lockValue, SetParams.setParams().nx().ex(ttlSeconds));
            return "OK".equals(res) ? lockValue : null;
        } catch (JedisException e) {
            logger.error("Redis tryLock 失败 lockKey={}", lockKey, e);
            return null;
        }
    }

    /**
     * 安全释放锁。
     * 使用 Lua 脚本原子验证：只有持有者才能删自己的锁。
     * @param lockKey   锁 key
     * @param lockValue tryLock 返回的唯一标识
     */
    public static void unlock(String lockKey, String lockValue) {
        String luaScript = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;
        try (Jedis jedis = getJedis()) {
            jedis.eval(luaScript, 1, lockKey, lockValue);
        } catch (JedisException e) {
            logger.error("Redis unlock 失败 lockKey={}", lockKey, e);
        }
    }

    // ====================== 生命周期 ======================

    /** 关闭连接池（应用关闭时调用） */
    public static void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("🛑 Redis 连接池已关闭");
        }
    }
}
