package com.example.dao;

import com.example.entity.BookCharacterLore;
import java.util.List;

public interface BookCharacterLoreDao {
    // 基础CRUD
    void add(BookCharacterLore lore);
    void update(BookCharacterLore lore);
    void del(Integer id);

    // 基础查询
    List<BookCharacterLore> queryById(Integer id);

    // 业务专属查询
    List<BookCharacterLore> queryByIsbn(String isbn);
    List<BookCharacterLore> queryByCharacterName(String characterName);
    List<BookCharacterLore> queryByIsbnAndCharacterName(String isbn, String characterName);
}