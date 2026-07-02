package com.example.dao;

import com.example.entity.BookTagRelation;
import java.util.List;

public interface BookTagRelationDao {
    // 新增关联
    void add(BookTagRelation relation);
    // 根据ID删除
    void deleteById(Integer id);
    // 根据ID查询
    BookTagRelation selectById(Integer id);
    // 根据书籍ISBN查询（联表带标签名）
    List<BookTagRelation> selectByIsbn(String isbn);
    // 根据标签ID查询
    List<BookTagRelation> selectByTagId(Integer tagId);
}