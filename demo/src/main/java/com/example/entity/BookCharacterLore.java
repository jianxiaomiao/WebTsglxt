package com.example.entity;

/**
 * 书籍人物角色实体
 */
public class BookCharacterLore {
    private Integer id;
    private String isbn;
    private String character_name;
    private String personality;
    private String biography;
    private String relationship_json;

    /**
     * 有参构造：仅包含非空字段
     */
    public BookCharacterLore(Integer id, String isbn, String character_name) {
        this.id = id;
        this.isbn = isbn;
        this.character_name = character_name;
    }

    /**
     * 无参构造
     */
    public BookCharacterLore() {}

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCharacter_name() {
        return character_name;
    }

    public void setCharacter_name(String character_name) {
        this.character_name = character_name;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getRelationship_json() {
        return relationship_json;
    }

    public void setRelationship_json(String relationship_json) {
        this.relationship_json = relationship_json;
    }

    @Override
    public String toString() {
        return "BookCharacterLore{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", character_name='" + character_name + '\'' +
                ", personality='" + personality + '\'' +
                ", biography='" + biography + '\'' +
                ", relationship_json='" + relationship_json + '\'' +
                '}';
    }
}