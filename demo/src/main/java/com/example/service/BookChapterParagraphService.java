package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.BookChapterParagraph;
import java.util.List;

public interface BookChapterParagraphService {
    ResultDTO<Void> addParagraph(BookChapterParagraph paragraph);
    ResultDTO<Void> updateParagraph(BookChapterParagraph paragraph);
    ResultDTO<Void> deleteParagraph(String paragraphId);
    ResultDTO<List<BookChapterParagraph>> queryParagraphById(String id);
    ResultDTO<List<BookChapterParagraph>> queryParagraphByChapterId(String chapterId);
}