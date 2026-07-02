package com.example.entity;

import java.time.LocalDateTime;

/**
 * AI对话交互记录实体
 */
public class AiInteractionHistory {
    private Integer id;
    private String user_id;
    private String isbn;
    private Integer interaction_type;
    private String user_input;
    private String ai_response;
    private String summary_tag;

    /**
     * 有参构造：仅包含非空字段
     */
    public AiInteractionHistory(Integer id, String user_id, Integer interaction_type) {
        this.id = id;
        this.user_id = user_id;
        this.interaction_type = interaction_type;
    }

    /**
     * 无参构造
     */
    public AiInteractionHistory() {}

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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getInteraction_type() {
        return interaction_type;
    }

    public void setInteraction_type(Integer interaction_type) {
        this.interaction_type = interaction_type;
    }

    public String getUser_input() {
        return user_input;
    }

    public void setUser_input(String user_input) {
        this.user_input = user_input;
    }

    public String getAi_response() {
        return ai_response;
    }

    public void setAi_response(String ai_response) {
        this.ai_response = ai_response;
    }

    public String getSummary_tag() {
        return summary_tag;
    }

    public void setSummary_tag(String summary_tag) {
        this.summary_tag = summary_tag;
    }

    @Override
    public String toString() {
        return "AiInteractionHistory{" +
                "id=" + id +
                ", user_id='" + user_id + '\'' +
                ", isbn='" + isbn + '\'' +
                ", interaction_type=" + interaction_type +
                ", user_input='" + user_input + '\'' +
                ", ai_response='" + ai_response + '\'' +
                ", summary_tag='" + summary_tag + '\'' +
                '}';
    }
}