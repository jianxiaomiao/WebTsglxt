package com.example.entity;

import java.time.LocalDateTime;

/**
 * 章节段落表实体
 */
public class BookChapterParagraph {
    // 段落ID: {chapter_id}_{number}
    private String id;
    // 图书ISBN
    private String isbn;
    // 章节ID
    private String chapterId;
    // 段落序号(1,2,3...)
    private Integer number;
    // 段落纯文本内容
    private String content;
    // 该段落评论数量
    private Integer commentCount;
    // 创建时间
    private LocalDateTime createTime;

    public BookChapterParagraph() {
    }

    public BookChapterParagraph(String id, String isbn, String chapterId, Integer number, String content, Integer commentCount, LocalDateTime createTime) {
        this.id = id;
        this.isbn = isbn;
        this.chapterId = chapterId;
        this.number = number;
        this.content = content;
        this.commentCount = commentCount;
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "BookChapterParagraph{" +
                "id='" + id + '\'' +
                ", isbn='" + isbn + '\'' +
                ", chapterId='" + chapterId + '\'' +
                ", number=" + number +
                ", content='" + content + '\'' +
                ", commentCount=" + commentCount +
                ", createTime=" + createTime +
                '}';
    }
}