package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;

/**
 * 数据库异常处理工具类
 */
public class DBExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DBExceptionHandler.class);

    /**
     * 处理SQL异常
     * @param e SQL异常
     * @param operation 操作描述（如“新增图书”）
     */
    public static void handleSQLException(SQLException e, String operation) {
        logger.error("{}失败，原因：{}", operation, e.getMessage(), e);
        throw new RuntimeException(operation + "失败，请联系管理员", e);
    }
}
