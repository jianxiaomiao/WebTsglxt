package com.example.dao;

import com.example.entity.BookChapter;
import com.example.entity.BookChapterParagraph;

import java.util.List;

public interface BookChapterDao {
    void add(BookChapter bookchapter);
    void update(BookChapter bookchapter);
    void del(String chapterId);
    List<BookChapter> queryAll();
    List<BookChapter> queryByChapterId(String chapterId);
    List<BookChapter> queryByBookId(String isbn);

    BookChapter queryChapterWithParagraphs(String chapterId);
    void batchInsertParagraph(List<BookChapterParagraph> paraList);
    void deleteParagraphByChapterId(String chapterId);
}
