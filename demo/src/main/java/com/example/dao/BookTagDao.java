package com.example.dao;

import com.example.entity.BookTag;
import java.util.List;

public interface BookTagDao {
    // 新增标签
    void add(BookTag tag);
    // 根据ID删除
    void deleteById(Integer id);
    // 根据ID修改
    void updateById(BookTag tag);
    // 根据ID查询
    BookTag selectById(Integer id);
    // 查询所有标签
    List<BookTag> selectAll();
}