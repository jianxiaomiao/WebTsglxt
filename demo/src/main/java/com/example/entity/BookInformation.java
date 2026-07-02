package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

public class BookInformation {
    private String Bookname;
    private String Author;
    private String ISBN;
    private String Publisher;
    private LocalDateTime PublishDate;
    // 数据库真实字段：书籍类型ID（int）
    private Integer Type;
    // AI 生成的书籍总结（缓存复用）
    private String aiSummary;
    // 前端显示用：书籍类型名称（不入库，联表查询填充）
    private String bookTypeName;
    private Integer all_book;
    private Integer now_book;
    private String PictureName;
    private String Information;
    private Float star;
    private Integer ratingCount;
    private Integer CollectionCount;
    private Integer BorrowCount;
    private List<String> tagNames;

    public BookInformation() {
    }

    // 完整构造函数（末尾追加 aiSummary 参数）
    public BookInformation(String Bookname, String Author, String ISBN, String Publisher, LocalDateTime PublishDate, Integer Type, String bookTypeName, Integer all_book, Integer now_book, String PictureName, String Information, Float star, Integer ratingCount, Integer CollectionCount, Integer BorrowCount, Double hotScore, String aiSummary) {
        this.Bookname = Bookname;
        this.Author = Author;
        this.ISBN = ISBN;
        this.Publisher = Publisher;
        this.PublishDate = PublishDate;
        this.Type = Type;
        this.bookTypeName = bookTypeName;
        this.all_book = all_book;
        this.now_book = now_book;
        this.PictureName = PictureName;
        this.Information = Information;
        this.star = star;
        this.ratingCount = ratingCount;
        this.CollectionCount = CollectionCount;
        this.BorrowCount = BorrowCount;
        this.hotScore = hotScore;
        this.aiSummary = aiSummary; // 新增这一行
    }


    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
    // Getter & Setter
    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }

    // Getter & Setter
    public String getBookname() {
        return Bookname;
    }

    public void setBookname(String Bookname) {
        this.Bookname = Bookname;
    }

    public String getAuthor() {
        return Author;
    }

    public void setAuthor(String Author) {
        this.Author = Author;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public String getPublisher() {
        return Publisher;
    }

    public void setPublisher(String Publisher) {
        this.Publisher = Publisher;
    }

    public LocalDateTime getPublishDate() {
        return PublishDate;
    }

    public void setPublishDate(LocalDateTime PublishDate) {
        this.PublishDate = PublishDate;
    }

    public Integer getType() {
        return Type;
    }

    public void setType(Integer Type) {
        this.Type = Type;
    }

    public String getBookTypeName() {
        return bookTypeName;
    }

    public void setBookTypeName(String bookTypeName) {
        this.bookTypeName = bookTypeName;
    }

    public Integer getAll_book() {
        return all_book;
    }

    public void setAll_book(Integer all_book) {
        this.all_book = all_book;
    }

    public Integer getNow_book() {
        return now_book;
    }

    public void setNow_book(Integer now_book) {
        this.now_book = now_book;
    }

    public String getPictureName() {
        return PictureName;
    }

    public void setPictureName(String PictureName) {
        this.PictureName = PictureName;
    }

    public String getInformation() {
        return Information;
    }

    public void setInformation(String Information) {
        this.Information = Information;
    }

    public Float getStar() {
        return star;
    }

    public void setStar(Float star) {
        this.star = star;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getCollectionCount() {
        return CollectionCount;
    }

    public void setCollectionCount(Integer CollectionCount) {
        this.CollectionCount = CollectionCount;
    }

    public Integer getBorrowCount() {
        return BorrowCount;
    }

    public void setBorrowCount(Integer BorrowCount) {
        this.BorrowCount = BorrowCount;
    }

    // 推荐算法专用：热度得分
    private Double hotScore;

    // Getter & Setter
    public Double getHotScore() {
        return hotScore;
    }
    public void setHotScore(Double hotScore) {
        this.hotScore = hotScore;
    }

    @Override
    public String toString() {
        return "BookInformation{" +
                "Bookname='" + Bookname + '\'' +
                ", Author='" + Author + '\'' +
                ", ISBN='" + ISBN + '\'' +
                ", Publisher='" + Publisher + '\'' +
                ", PublishDate=" + PublishDate +
                ", Type=" + Type +
                ", bookTypeName='" + bookTypeName + '\'' +
                ", all_book=" + all_book +
                ", now_book=" + now_book +
                ", PictureName='" + PictureName + '\'' +
                ", Information='" + Information + '\'' +
                ", star=" + star +
                ", ratingCount=" + ratingCount +
                ", CollectionCount=" + CollectionCount +
                ", BorrowCount=" + BorrowCount +
                ", tagNames=" + tagNames +
                ", hotScore=" + hotScore +
                ", aiSummary='" + aiSummary + '\'' +
                '}';
    }
}