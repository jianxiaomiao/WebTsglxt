package com.example.entity;

public class BookTag {
    private Integer id;
    private String tagName;

    public BookTag() {}

    public BookTag(String tagName) {
        this.tagName = tagName;
    }

    public BookTag(Integer id, String tagName) {
        this.id = id;
        this.tagName = tagName;
    }

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String toString() {
        return "BookTag{" +
                "id=" + id +
                ", tagName='" + tagName + '\'' +
                '}';
    }
}