package com.example.entity;

import java.time.LocalDate;

/**
 * 用户数字画像实体
 */
public class UserProfileMemory {
    private Integer id;
    private String user_id;
    private LocalDate date;
    private String reading_preferences;
    private String emotional_state;
    private String character_sketch;

    /**
     * 有参构造：仅包含非空字段
     */
    public UserProfileMemory(Integer id, String user_id, LocalDate date) {
        this.id = id;
        this.user_id = user_id;
        this.date = date;
    }

    /**
     * 无参构造
     */
    public UserProfileMemory() {}

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReading_preferences() {
        return reading_preferences;
    }

    public void setReading_preferences(String reading_preferences) {
        this.reading_preferences = reading_preferences;
    }

    public String getEmotional_state() {
        return emotional_state;
    }

    public void setEmotional_state(String emotional_state) {
        this.emotional_state = emotional_state;
    }

    public String getCharacter_sketch() {
        return character_sketch;
    }

    public void setCharacter_sketch(String character_sketch) {
        this.character_sketch = character_sketch;
    }

    @Override
    public String toString() {
        return "UserProfileMemory{" +
                "id=" + id +
                ", user_id='" + user_id + '\'' +
                ", date=" + date +
                ", reading_preferences='" + reading_preferences + '\'' +
                ", emotional_state='" + emotional_state + '\'' +
                ", character_sketch='" + character_sketch + '\'' +
                '}';
    }
}