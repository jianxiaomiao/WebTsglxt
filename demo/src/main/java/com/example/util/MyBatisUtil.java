package com.example.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class MyBatisUtil {
    // 全局唯一的SqlSessionFactory
    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            // 加载MyBatis核心配置文件
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("初始化MyBatis失败", e);
        }
    }

    // 获取SqlSession（自动提交事务）
    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession(true); // true=自动提交
    }

    // 获取SqlSessionFactory
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}