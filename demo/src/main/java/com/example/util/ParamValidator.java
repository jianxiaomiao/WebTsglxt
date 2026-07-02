package com.example.util;

// 需要引入的包：
import java.util.Objects;

/**
 * 参数校验工具类
 */
public class ParamValidator {
    /**
     * 校验对象非空
     * @param obj 要校验的对象
     * @param paramName 参数名（用于错误提示）
     */
    public static void checkNotNull(Object obj, String paramName) {
        if (Objects.isNull(obj)) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
    }

    /**
     * 校验字符串非空且非空白
     * @param str 要校验的字符串
     * @param paramName 参数名（用于错误提示）
     */
    public static void checkNotBlank(String str, String paramName) {
        checkNotNull(str, paramName);
        if (str.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空白");
        }
    }
}
