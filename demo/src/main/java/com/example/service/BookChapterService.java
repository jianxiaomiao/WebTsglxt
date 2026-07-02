package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookChapter;

import java.util.List;

public interface BookChapterService {
    // 新增章节，传入完整文本自动分段入库
    ResultDTO<Void> addBookChapter(BookChapter chapter, String fullText);
    ResultDTO<Void> updateBookChapter(BookChapter chapter);
    ResultDTO<Void> deleteBookChapter(String chapterId);

    ResultDTO<List<BookChapter>> queryAllBookChapters();
    // 查询单章节，携带段落列表
    ResultDTO<BookChapter> queryBookChapterId(String chapterId);
    ResultDTO<List<BookChapter>> queryBookChapterIsbn(String isbn);
}
