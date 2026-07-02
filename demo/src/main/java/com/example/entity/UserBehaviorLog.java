package com.example.entity;

import java.time.LocalDateTime;

/**
 * 用户行为日志实体
 */
public class UserBehaviorLog {
    private Integer id;
    private String user_id;
    private Integer action_type;
    private String book_isbn;
    private String chapter_number;
    private String content_snapshot;
    private LocalDateTime create_time;

    /**
     * 有参构造：仅包含非空字段
     */
    public UserBehaviorLog(Integer id, String user_id, Integer action_type, LocalDateTime create_time) {
        this.id = id;
        this.user_id = user_id;
        this.action_type = action_type;
        this.create_time = create_time;
    }

    /**
     * 无参构造
     */
    public UserBehaviorLog() {}

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Integer getAction_type() {
        return action_type;
    }

    public void setAction_type(Integer action_type) {
        this.action_type = action_type;
    }

    public String getBook_isbn() {
        return book_isbn;
    }

    public void setBook_isbn(String book_isbn) {
        this.book_isbn = book_isbn;
    }

    public String getChapter_number() {
        return chapter_number;
    }

    public void setChapter_number(String chapter_number) {
        this.chapter_number = chapter_number;
    }

    public String getContent_snapshot() {
        return content_snapshot;
    }

    public void setContent_snapshot(String content_snapshot) {
        this.content_snapshot = content_snapshot;
    }

    public LocalDateTime getCreate_time() {
        return create_time;
    }

    public void setCreate_time(LocalDateTime create_time) {
        this.create_time = create_time;
    }

    @Override
    public String toString() {
        return "UserBehaviorLog{" +
                "id=" + id +
                ", user_id='" + user_id + '\'' +
                ", action_type=" + action_type +
                ", book_isbn='" + book_isbn + '\'' +
                ", chapter_number='" + chapter_number + '\'' +
                ", content_snapshot='" + content_snapshot + '\'' +
                ", create_time=" + create_time +
                '}';
    }
}