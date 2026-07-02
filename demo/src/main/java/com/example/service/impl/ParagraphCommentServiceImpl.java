package com.example.service.impl;

import com.example.dao.BookChapterParagraphDao;
import com.example.dao.ParagraphCommentDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.ParagraphComment;
import com.example.service.ParagraphCommentService;

import java.util.List;

public class ParagraphCommentServiceImpl implements ParagraphCommentService {
    private final ParagraphCommentDao commentDao;
    private final BookChapterParagraphDao paragraphDao;

    // 构造注入两个DAO
    public ParagraphCommentServiceImpl(ParagraphCommentDao commentDao, BookChapterParagraphDao paragraphDao) {
        this.commentDao = commentDao;
        this.paragraphDao = paragraphDao;
    }

    @Override
    public ResultDTO<Void> addComment(ParagraphComment comment) {
        try {
            // 1. 新增评论
            commentDao.add(comment);
            // 2. 对应段落评论数 +1
            paragraphDao.updateCommentCount(comment.getParagraphId(), 1);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增段落评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateComment(ParagraphComment comment) {
        try {
            commentDao.update(comment);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新段落评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteComment(Long commentId) {
        try {
            // 1. 先查询评论获取关联段落ID
            List<ParagraphComment> commentList = commentDao.queryById(commentId);
            if (commentList.isEmpty()) {
                return ResultDTO.fail("未找到该评论，删除失败");
            }
            String paragraphId = commentList.get(0).getParagraphId();
            // 2. 删除评论
            commentDao.delete(commentId);
            // 3. 段落评论数 -1
            paragraphDao.updateCommentCount(paragraphId, -1);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除段落评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<ParagraphComment>> queryCommentById(Long commentId) {
        try {
            List<ParagraphComment> list = commentDao.queryById(commentId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据评论ID查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<ParagraphComment>> queryCommentByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            // 分页参数默认值处理
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
            int offset = (pageNum - 1) * pageSize;

            List<ParagraphComment> data = commentDao.queryByUserIdPage(userId, offset, pageSize);
            Long total = commentDao.countByUserId(userId);
            PageResultDTO<ParagraphComment> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询用户评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<ParagraphComment>> queryCommentByParagraphIdPage(String paragraphId, Integer pageNum, Integer pageSize) {
        try {
            pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
            pageSize = pageSize == null || pageSize < 1 || pageSize > 50 ? 10 : pageSize;
            int offset = (pageNum - 1) * pageSize;

            List<ParagraphComment> data = commentDao.queryByParagraphIdPage(paragraphId, offset, pageSize);
            Long total = commentDao.countByParagraphId(paragraphId);
            PageResultDTO<ParagraphComment> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询段落评论失败：" + e.getMessage());
        }
    }
}