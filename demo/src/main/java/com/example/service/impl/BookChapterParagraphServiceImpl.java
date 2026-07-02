package com.example.service.impl;

import com.example.dao.BookChapterParagraphDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookChapterParagraph;
import com.example.service.BookChapterParagraphService;

import java.util.List;

public class BookChapterParagraphServiceImpl implements BookChapterParagraphService {
    private final BookChapterParagraphDao paragraphDao;

    public BookChapterParagraphServiceImpl(BookChapterParagraphDao paragraphDao) {
        this.paragraphDao = paragraphDao;
    }

    @Override
    public ResultDTO<Void> addParagraph(BookChapterParagraph paragraph) {
        try {
            paragraphDao.add(paragraph);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增段落失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateParagraph(BookChapterParagraph paragraph) {
        try {
            paragraphDao.update(paragraph);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新段落失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteParagraph(String paragraphId) {
        try {
            paragraphDao.delete(paragraphId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除段落失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookChapterParagraph>> queryParagraphById(String id) {
        try {
            List<BookChapterParagraph> list = paragraphDao.queryById(id);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据ID查询段落失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookChapterParagraph>> queryParagraphByChapterId(String chapterId) {
        try {
            List<BookChapterParagraph> list = paragraphDao.queryByChapterId(chapterId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据章节ID查询段落失败：" + e.getMessage());
        }
    }
}