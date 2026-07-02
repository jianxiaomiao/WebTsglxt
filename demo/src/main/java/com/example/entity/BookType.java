package com.example.entity;

public class BookType {
    private Integer id;
    private String bookType;

    public BookType() {
    }

    public BookType(Integer id, String bookType) {
        this.id = id;
        this.bookType = bookType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBookType() {
        return bookType;
    }

    public void setBookType(String bookType) {
        this.bookType = bookType;
    }

    @Override
    public String toString() {
        return "BookType{" +
                "id=" + id +
                ", bookType='" + bookType + '\'' +
                '}';
    }
}