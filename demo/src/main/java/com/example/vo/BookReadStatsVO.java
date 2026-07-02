package com.example.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 书籍阅读统计VO（每本书总时长 + 书籍信息）
 * 专供前端书籍墙、阅读TOP3使用
 */
@Data
public class BookReadStatsVO {
    // 书籍基础信息
    private String isbn;
    private String bookname;
    private String author;
    private String pictureName;
    private String publisher;
    private String bookTypeName;
    // 阅读统计
    private Integer totalDuration; // 本书总阅读时长(秒)
    private LocalDateTime lastReadTime; // 最后阅读时间
}