package com.example.service.impl;

import com.example.dao.BookSquarePostDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookSquarePost;
import com.example.service.BookSquarePostService;

import java.util.List;

/**
 * 书荒广场帖子 Service 实现类
 */
public class BookSquarePostServiceImpl implements BookSquarePostService {

    private final BookSquarePostDao postDao;

    // 构造器注入DAO
    public BookSquarePostServiceImpl(BookSquarePostDao postDao) {
        this.postDao = postDao;
    }

    @Override
    public ResultDTO<Void> addPost(BookSquarePost post) {
        try {
            postDao.add(post);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updatePost(BookSquarePost post) {
        try {
            postDao.update(post);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> logicDeletePost(Integer id) {
        try {
            postDao.logicDelete(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<BookSquarePost> getPostById(Integer id) {
        try {
            BookSquarePost post = postDao.queryById(id);
            return ResultDTO.success(post);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookSquarePost>> queryAllPage(Integer pageNum, Integer pageSize) {
        try {
            List<BookSquarePost> list = postDao.queryAllPage(pageNum, pageSize);
            Long total = postDao.countAll();
            PageResultDTO<BookSquarePost> pageData = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(pageData);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookSquarePost>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            List<BookSquarePost> list = postDao.queryByUserIdPage(userId, pageNum, pageSize);
            Long total = postDao.countByUserId(userId);
            PageResultDTO<BookSquarePost> pageData = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(pageData);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询用户帖子失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookSquarePost>> queryMainPostPage(Integer pageNum, Integer pageSize) {
        try {
            List<BookSquarePost> list = postDao.queryMainPostPage(pageNum, pageSize);
            Long total = postDao.countMainPost();
            PageResultDTO<BookSquarePost> pageData = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(pageData);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询主帖失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookSquarePost>> querySubPostByParentIdPage(Integer parentId, Integer pageNum, Integer pageSize) {
        try {
            List<BookSquarePost> list = postDao.querySubPostByParentIdPage(parentId, pageNum, pageSize);
            Long total = postDao.countSubPostByParentId(parentId);
            PageResultDTO<BookSquarePost> pageData = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(pageData);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询子帖失败：" + e.getMessage());
        }
    }
}