package com.example.service.impl;

import com.example.dao.BookChapterDao;
import com.example.dao.BookChapterParagraphDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookChapter;
import com.example.entity.BookChapterParagraph;
import com.example.service.BookChapterService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookChapterServiceImpl implements BookChapterService {
    private final BookChapterDao bookChapterDao;
    private final BookChapterParagraphDao paragraphDao;

    public BookChapterServiceImpl(BookChapterDao bookChapterDao, BookChapterParagraphDao paragraphDao) {
        this.bookChapterDao = bookChapterDao;
        this.paragraphDao = paragraphDao;
    }

    // 核心新增：接收完整文本，自动分割段落批量插入
    @Override
    public ResultDTO<Void> addBookChapter(BookChapter chapter, String fullText) {
        try {
            // 填充创建时间
            chapter.setCreate_time(LocalDateTime.now());
            // 1. 插入章节主表
            bookChapterDao.add(chapter);
            // 2. 分割文本为段落
            List<BookChapterParagraph> paraList = splitTextToParagraphs(fullText, chapter.getChapter_id(), chapter.getIsbn());
            // 3. 批量插入段落
            if (!paraList.isEmpty()) {
                bookChapterDao.batchInsertParagraph(paraList);
            }
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增章节及段落失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBookChapter(BookChapter chapter) {
        try {
            bookChapterDao.update(chapter);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新章节失败：" + e.getMessage());
        }
    }

    // 删除章节：先删段落，再删章节
    @Override
    public ResultDTO<Void> deleteBookChapter(String chapterId) {
        try {
            // 级联删除段落
            bookChapterDao.deleteParagraphByChapterId(chapterId);
            // 删除章节
            bookChapterDao.del(chapterId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除章节失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookChapter>> queryAllBookChapters() {
        try {
            List<BookChapter> list = bookChapterDao.queryAll();
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询全部章节失败：" + e.getMessage());
        }
    }

    // 查询单章节，自动封装段落集合
    @Override
    public ResultDTO<BookChapter> queryBookChapterId(String chapterId) {
        try {
            BookChapter chapter = bookChapterDao.queryChapterWithParagraphs(chapterId);
            return ResultDTO.success(chapter);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询章节详情失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookChapter>> queryBookChapterIsbn(String isbn) {
        try {
            List<BookChapter> list = bookChapterDao.queryByBookId(isbn);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ISBN查询章节失败：" + e.getMessage());
        }
    }

    // 文本分割工具方法（内部私有）
    private List<BookChapterParagraph> splitTextToParagraphs(String sourceText, String chapterId, String isbn) {
        List<BookChapterParagraph> result = new ArrayList<>();
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return result;
        }
        String text = sourceText.trim();
        List<String> partList = new ArrayList<>();

        if (text.contains("\n") || text.contains("\r")) {
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                String trimLine = line.trim();
                if (!trimLine.isEmpty()) partList.add(trimLine);
            }
        } else {
            text = text.replace("……", "|");
            String[] sentences = text.split("[。！？|]");
            for (String sen : sentences) {
                String trimSen = sen.trim();
                if (!trimSen.isEmpty()) partList.add(trimSen);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < partList.size(); i++) {
            BookChapterParagraph para = new BookChapterParagraph();
            para.setId(chapterId + "_" + (i + 1));
            para.setChapterId(chapterId);
            para.setIsbn(isbn);
            para.setNumber(i + 1);
            para.setContent(partList.get(i));
            para.setCommentCount(0);
            para.setCreateTime(now);
            result.add(para);
        }
        return result;
    }
}