package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookSquarePost;

/**
 * 书荒广场帖子 Service 接口
 */
public interface BookSquarePostService {

    /**
     * 新增帖子
     */
    ResultDTO<Void> addPost(BookSquarePost post);

    /**
     * 更新帖子
     */
    ResultDTO<Void> updatePost(BookSquarePost post);

    /**
     * 逻辑删除帖子
     */
    ResultDTO<Void> logicDeletePost(Integer id);

    /**
     * 根据ID查询单条帖子
     */
    ResultDTO<BookSquarePost> getPostById(Integer id);

    /**
     * 分页查询所有帖子
     */
    ResultDTO<PageResultDTO<BookSquarePost>> queryAllPage(Integer pageNum, Integer pageSize);

    /**
     * 分页查询指定用户帖子
     */
    ResultDTO<PageResultDTO<BookSquarePost>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    /**
     * 分页查询所有主帖
     */
    ResultDTO<PageResultDTO<BookSquarePost>> queryMainPostPage(Integer pageNum, Integer pageSize);

    /**
     * 分页查询指定主帖的子帖
     */
    ResultDTO<PageResultDTO<BookSquarePost>> querySubPostByParentIdPage(Integer parentId, Integer pageNum, Integer pageSize);
}