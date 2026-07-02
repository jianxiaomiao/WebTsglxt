package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

public class BookChapter {
    private String Chapter_id;
    private String Isbn;
    private Integer Number;
    private String Name;
    // 移除原有大文本Content，替换为段落集合
    private List<BookChapterParagraph> paragraphs;
    private LocalDateTime Create_time;

    public BookChapter(String chapter_id, Integer number, String isbn, LocalDateTime create_time, String name, List<BookChapterParagraph> paragraphs) {
        Chapter_id = chapter_id;
        Number = number;
        Isbn = isbn;
        Create_time = create_time;
        Name = name;
        this.paragraphs = paragraphs;
    }

    public BookChapter() {
    }

    public String getChapter_id() {
        return Chapter_id;
    }

    public void setChapter_id(String chapter_id) {
        Chapter_id = chapter_id;
    }

    public String getIsbn() {
        return Isbn;
    }

    public void setIsbn(String isbn) {
        Isbn = isbn;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Integer getNumber() {
        return Number;
    }

    public void setNumber(Integer number) {
        Number = number;
    }

    public LocalDateTime getCreate_time() {
        return Create_time;
    }

    public void setCreate_time(LocalDateTime create_time) {
        Create_time = create_time;
    }

    public List<BookChapterParagraph> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<BookChapterParagraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    @Override
    public String toString() {
        return "BookChapter{" +
                "Chapter_id='" + Chapter_id + '\'' +
                ", Isbn='" + Isbn + '\'' +
                ", Number=" + Number +
                ", Name='" + Name + '\'' +
                ", paragraphs=" + paragraphs +
                ", Create_time=" + Create_time +
                '}';
    }
}