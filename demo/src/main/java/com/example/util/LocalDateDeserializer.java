package com.example.util;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 自定义LocalDate反序列化器（与现有LocalDateTimeDeserializer风格完全一致）
 */
public class LocalDateDeserializer implements ObjectDeserializer {

    // 支持所有常见日期格式
    private static final DateTimeFormatter FORMATTER_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object value = parser.parse();
        if (value == null) {
            return null;
        }

        try {
            // 字符串格式日期
            if (value instanceof String) {
                String dateStr = (String) value;
                // 尝试ISO格式
                try {
                    return (T) LocalDate.parse(dateStr, FORMATTER_ISO);
                } catch (DateTimeParseException e1) {
                    // 尝试yyyy-MM-dd标准格式
                    try {
                        return (T) LocalDate.parse(dateStr, FORMATTER_1);
                    } catch (DateTimeParseException e2) {
                        return null;
                    }
                }
            }
            // 其他类型返回null
            else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}