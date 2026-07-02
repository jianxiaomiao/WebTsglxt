package com.example.dao;

import com.example.entity.BookChapterParagraph;
import java.util.List;

public interface BookChapterParagraphDao {
    // 新增段落
    void add(BookChapterParagraph paragraph);
    // 动态更新段落信息
    void update(BookChapterParagraph paragraph);
    // 根据段落id删除
    void delete(String paragraphId);
    // 根据id查询段落（单条）
    List<BookChapterParagraph> queryById(String id);
    // 根据章节id查询该章节所有段落（无分页，chapter全局唯一）
    List<BookChapterParagraph> queryByChapterId(String chapterId);
    // 更新段落评论数量（新增评论+1，删除评论-1）
    void updateCommentCount(String paragraphId, int delta);
}