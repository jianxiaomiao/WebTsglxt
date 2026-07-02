package com.example.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DBUtil {
    private static final HikariDataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

    static {
        // 从 config.properties 读取数据库配置
        Properties dbProps = new Properties();
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                dbProps.load(in);
                logger.info("✅ 已从 config.properties 加载数据库配置");
            }
        } catch (Exception e) {
            logger.warn("⚠️  无法加载 config.properties，尝试从环境变量读取", e);
        }

        String url = coalesce(
                dbProps.getProperty("db.url"),
                System.getenv("DB_URL"),
                "jdbc:mysql://localhost:3306/library_manager?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        String driver = coalesce(
                dbProps.getProperty("db.driver"),
                System.getenv("DB_DRIVER"),
                "com.mysql.cj.jdbc.Driver");
        String username = coalesce(
                dbProps.getProperty("db.username"),
                System.getenv("DB_USERNAME"),
                "library_user");
        String password = coalesce(
                dbProps.getProperty("db.password"),
                System.getenv("DB_PASSWORD"),
                "NewPass123!");

        // 配置连接池
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driver);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(5000);
        config.setMaxLifetime(1200000);

        dataSource = new HikariDataSource(config);
    }

    /** 取第一个非空非null的值 */
    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    // 获取连接
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // 关闭连接（实际是放回连接池）
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close(); // 连接池的close()是将连接放回池，而非真正关闭
            } catch (SQLException e) {
               logger.error("关闭数据库连接失败", e);
            }
        }
    }

    public static int executeUpdate(String sql, Object... params) throws SQLException{
        int i = 0;
        Connection con = getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            // 为占位符赋值
            if (null != params) {
                for (int k = 0; k < params.length; k++) {
                    pstmt.setObject(k + 1, params[k]);
                }
            }
            i = pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("更新资源失败", e);
                throw e;
        } finally {
            try {
                if (null != pstmt)
                    pstmt.close();
                if (null != con)
                    closeConnection(con);
            } catch (SQLException e) {
                logger.error("关闭资源失败", e);
            }
        }
        return i;
    }

    // ====================== 🔥 新增：执行INSERT并返回自增主键ID ======================
    public static Integer executeUpdateReturnId(String sql, Object... params) throws SQLException {
        Connection con = getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // 🔥 关键：加上 Statement.RETURN_GENERATED_KEYS，告诉JDBC我们需要自增ID
            pstmt = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);

            // 为占位符赋值（和你原来的executeUpdate保持一致）
            if (null != params) {
                for (int k = 0; k < params.length; k++) {
                    pstmt.setObject(k + 1, params[k]);
                }
            }

            // 执行更新
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("插入失败，没有行受到影响");
            }

            // 获取自增主键
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // 返回第一个自增ID
            } else {
                throw new SQLException("插入失败，未获取到自增ID");
            }
        } catch (SQLException e) {
            logger.error("插入并获取自增ID失败", e);
            throw e;
        } finally {
            // 关闭资源（和你原来的DBUtil保持完全一致）
            try {
                if (null != rs)
                    rs.close();
                if (null != pstmt)
                    pstmt.close();
                if (null != con)
                    closeConnection(con);
            } catch (SQLException e) {
                logger.error("关闭资源失败", e);
            }
        }
    }

    public static <T> List<T> executeQuery(String sql, RowMap<T> rowMap,  Object... params) throws SQLException{
        List<T> list = new ArrayList<>();
        Connection con = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            if (null != params) {
                for (int k = 0; k < params.length; k++) {
                    pstmt.setObject(k + 1, params[k]);
                }
            }
            rs = pstmt.executeQuery();
            while (rs.next()) {
                // 从数据表中 循环每一行 拿到每一列的数据 将数据保存到entity对应的属性上
                T t = rowMap.rowMapping(rs);
                // 再将对象保存到list中
                list.add(t);
            }
        } catch (SQLException e) {
            logger.error("查询资源失败", e);
            throw e;
        } finally {
            try {
                if (null != rs)
                    rs.close();
                if (null != pstmt)
                    pstmt.close();
                if (null != con)
                    closeConnection(con);
            } catch (SQLException e) {
                logger.error("关闭资源失败", e);
            }
        }
        return list;
    }
    // ====================== 🔥 新增标量查询方法（解决COUNT(*)查询问题） ======================
    public static <T> T executeQueryScalar(String sql, Class<T> type, Object... params) throws SQLException {
        Connection con = getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            if (params != null) {
                for (int k = 0; k < params.length; k++) {
                    pstmt.setObject(k + 1, params[k]);
                }
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getObject(1, type);
            }
            return null;
        } catch (SQLException e) {
            logger.error("标量查询失败", e);
            throw e;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                closeConnection(con);
            } catch (SQLException e) {
                logger.error("关闭资源失败", e);
            }
        }
    }

    // ====================== 🔥 新增：多SQL共用连接事务执行方法 ======================
    /**
     * 开启事务，同一连接执行多条更新SQL，全部成功才提交，异常自动回滚
     * @param batchTask 内部执行多条 executeUpdateWithConn（共用conn）
     * @throws SQLException 任意SQL失败抛出异常，自动回滚
     */
    public static void executeTransaction(Consumer<Connection> batchTask) throws SQLException {
        Connection con = getConnection();
        try {
            con.setAutoCommit(false); // 关闭自动提交，开启事务
            batchTask.accept(con);    // 把连接传给任务，执行多条SQL
            con.commit();             // 全部执行成功，统一提交
        } catch (Exception e) {
            con.rollback();           // 任意报错，全部回滚
            logger.error("事务执行失败，已自动回滚", e);
            throw new SQLException("事务操作失败，数据已回滚：" + e.getMessage(), e);
        } finally {
            closeConnection(con); // 归还连接到连接池
        }
    }

    /**
     * 事务内专用更新方法：复用外部传入的连接，不自动关闭
     * 仅在 executeTransaction 内部调用，禁止单独使用
     */
    public static int executeUpdateWithConn(Connection con, String sql, Object... params) throws SQLException {
        int affectedRows = 0;
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            if (null != params) {
                for (int k = 0; k < params.length; k++) {
                    pstmt.setObject(k + 1, params[k]);
                }
            }
            affectedRows = pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("事务内执行SQL失败，sql:{}", sql, e);
            throw e;
        } finally {
            if (null != pstmt) pstmt.close(); // 只关闭语句，不关闭连接
        }
        return affectedRows;
    }
}
