package com.example.entity;

import java.time.LocalDateTime;

/**
 * 有声书角色分段实体类
 * 对应表：audiobook_segment
 */
public class AudiobookSegment {
    private Long id;
    private String isbn;
    private Integer chapterNumber;
    private String paragraphId;
    private Integer sortOrder;
    private String roleType;     // 旁白/青年男/青年女/中年男/小女孩/老爷爷
    private String emotion;      // 欢快/悲伤/愤怒/温柔/平静
    private String audioUrl;     // 生成的音频文件URL
    private Float audioDuration; // 音频时长（秒）
    private Integer status;      // 0=待生成 1=已生成 2=生成失败
    private LocalDateTime createTime;

    // 联表查询扩展字段（不入库）
    private String paragraphContent; // 联表查询扩展字段（不入库），段落原文内容
    private String textContent;     // 🔥 AI生成的对话段原文内容（入库）

    public AudiobookSegment() {}

    public AudiobookSegment(Long id, String isbn, Integer chapterNumber, String paragraphId,
                            Integer sortOrder, String roleType, String emotion,
                            String audioUrl, Float audioDuration, Integer status,
                            LocalDateTime createTime) {
        this.id = id;
        this.isbn = isbn;
        this.chapterNumber = chapterNumber;
        this.paragraphId = paragraphId;
        this.sortOrder = sortOrder;
        this.roleType = roleType;
        this.emotion = emotion;
        this.audioUrl = audioUrl;
        this.audioDuration = audioDuration;
        this.status = status;
        this.createTime = createTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getParagraphId() { return paragraphId; }
    public void setParagraphId(String paragraphId) { this.paragraphId = paragraphId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Float getAudioDuration() { return audioDuration; }
    public void setAudioDuration(Float audioDuration) { this.audioDuration = audioDuration; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getParagraphContent() { return paragraphContent; }
    public void setParagraphContent(String paragraphContent) { this.paragraphContent = paragraphContent; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    @Override
    public String toString() {
        return "AudiobookSegment{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", chapterNumber=" + chapterNumber +
                ", sortOrder=" + sortOrder +
                ", roleType='" + roleType + '\'' +
                ", status=" + status +
                '}';
    }
}
