package com.example.entity;

public class BookTagRelation {
    private Integer id;
    private String isbn;
    private Integer tagId;
    // 联表查询用：标签名称（不入库）
    private String tagName;

    public BookTagRelation() {}

    public BookTagRelation(String isbn, Integer tagId) {
        this.isbn = isbn;
        this.tagId = tagId;
    }

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

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String toString() {
        return "BookTagRelation{" +
                "id=" + id +
                ", isbn='" + isbn + '\'' +
                ", tagId=" + tagId +
                ", tagName='" + tagName + '\'' +
                '}';
    }
}