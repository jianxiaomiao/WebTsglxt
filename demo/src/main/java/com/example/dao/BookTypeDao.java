package com.example.dao;

import com.example.entity.BookType;
import java.util.List;

public interface BookTypeDao {
    void add(BookType bookType);
    void update(BookType bookType);
    void del(Integer id);
    List<BookType> queryAll();
    List<BookType> queryById(Integer id);
}