package com.example.entity;

import java.time.LocalDate;

public class BorrowInformation {
    private String Userid;
    private String ISBN;
    private LocalDate BorrowDate;
    private LocalDate ReturnDate;
    private Float Fine;
    private Integer BorrowId;
    private String bookName;
    private String information;
    // ====================== 新增：书籍封面 ======================
    private String pictureName;

    // 原有构造方法不变
    public BorrowInformation(String Userid,String ISBN, LocalDate BorrowDate,  LocalDate ReturnDate, Float Fine, Integer BorrowId) {
        this.BorrowDate = BorrowDate;
        this.BorrowId = BorrowId;
        this.Fine = Fine;
        this.ISBN = ISBN;
        this.ReturnDate = ReturnDate;
        this.Userid = Userid;
    }

    // ====================== 新增封面 Getter/Setter ======================
    public String getPictureName() {
        return pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    // ====================== 原有 Getter/Setter（不变） ======================
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public String getUserid() {
        return Userid;
    }

    public void setUserid(String userid) {
        Userid = userid;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String iSBN) {
        ISBN = iSBN;
    }

    public LocalDate getBorrowDate() {
        return BorrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        BorrowDate = borrowDate;
    }

    public LocalDate getReturnDate() {
        return ReturnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        ReturnDate = returnDate;
    }

    public Float getFine() {
        return Fine;
    }

    public void setFine(Float fine) {
        Fine = fine;
    }

    public Integer getBorrowId() {
        return BorrowId;
    }

    public void setBorrowId(Integer borrowId) {
        BorrowId = borrowId;
    }

    public BorrowInformation() {
    }

    @Override
    public String toString() {
        return "BorrowInformation{" +
                "Userid='" + Userid + '\'' +
                ", ISBN='" + ISBN + '\'' +
                ", BorrowDate=" + BorrowDate +
                ", ReturnDate=" + ReturnDate +
                ", Fine=" + Fine +
                ", BorrowId=" + BorrowId +
                ", bookName='" + bookName + '\'' +
                ", information='" + information + '\'' +
                ", pictureName='" + pictureName + '\'' +
                '}';
    }
}