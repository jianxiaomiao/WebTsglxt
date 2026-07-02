package com.example.util;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeDeserializer implements ObjectDeserializer {
    // 支持所有常见时间格式
    private static final DateTimeFormatter FORMATTER_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_DATE_TIME;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object value = parser.parse();
        if (value == null) {
            return null;
        }

        try {
            // 1. 数字时间戳（毫秒）
            if (value instanceof Number) {
                long timestamp = ((Number) value).longValue();
                return (T) LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestamp),
                        ZoneId.systemDefault()
                );
            }
            // 2. 字符串格式时间
            else if (value instanceof String) {
                String dateStr = (String) value;
                // 尝试ISO格式（Element Plus默认）
                try {
                    return (T) LocalDateTime.parse(dateStr, FORMATTER_ISO);
                } catch (DateTimeParseException e1) {
                    // 尝试yyyy-MM-dd HH:mm:ss
                    try {
                        return (T) LocalDateTime.parse(dateStr, FORMATTER_1);
                    } catch (DateTimeParseException e2) {
                        // 尝试yyyy-MM-dd
                        try {
                            return (T) LocalDateTime.parse(dateStr + " 00:00:00", FORMATTER_1);
                        } catch (DateTimeParseException e3) {
                            return null;
                        }
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