package com.example.util;

// 需要引入的包：
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用结果集映射接口
 * @param <T> 实体类类型
 */
public interface RowMapper<T> {
    /**
     * 将ResultSet的一行映射为实体类
     * @param rs 结果集
     * @return 实体类对象
     * @throws SQLException SQL异常
     */
    T mapRow(ResultSet rs) throws SQLException;
}