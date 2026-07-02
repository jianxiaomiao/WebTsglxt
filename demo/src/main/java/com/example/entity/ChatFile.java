package com.example.entity;

import java.time.LocalDateTime;

public class ChatFile {
    private Integer id;
    private String fileType;         // 文件后缀：txt/mp3/docx/ppt等
    private String documentUrl;      // 数据库存储访问路径 /chat_files/xxx.xxx
    private LocalDateTime uploadTime;// 上传时间
    private String userId;           // 上传人id varchar(20)
    private String originalName;     // 原始文件名
    private Long fileSize;           // 文件大小(字节)

    // 无参构造
    public ChatFile() {}

    // 上传用构造器（插入数据库使用）
    public ChatFile(String fileType, String documentUrl, String userId, String originalName, Long fileSize) {
        this.fileType = fileType;
        this.documentUrl = documentUrl;
        this.userId = userId;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.uploadTime = LocalDateTime.now();
    }

    // Getter & Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}