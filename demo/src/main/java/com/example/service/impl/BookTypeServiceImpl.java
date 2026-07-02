package com.example.service.impl;

import com.example.dao.BookTypeDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookType;
import com.example.service.BookTypeService;
import com.example.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookTypeServiceImpl implements BookTypeService {

    private final BookTypeDao bookTypeDao;
    private static final String CACHE_KEY = "dict:book_type:all";
    private static final int CACHE_TTL = 3600; // 1小时

    public BookTypeServiceImpl(BookTypeDao bookTypeDao) {
        this.bookTypeDao = bookTypeDao;
    }

    @Override
    public ResultDTO<List<BookType>> queryAllBookTypes() {
        try {
            // 🔥 Cache-Aside：先查 Redis，miss 再查 DB
            try {
                String cached = RedisUtil.get(CACHE_KEY);
                if (cached != null) {
                    return ResultDTO.success(JSON.parseArray(cached, BookType.class));
                }
            } catch (Exception ignored) { /* Redis 挂了降级查 DB */ }

            List<BookType> list = bookTypeDao.queryAll();
            try { RedisUtil.set(CACHE_KEY, JSON.toJSONString(list), CACHE_TTL); } catch (Exception ignored) {}
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有书籍种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookType>> queryBookTypeById(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("书籍种类ID不能为空");
            List<BookType> list = bookTypeDao.queryById(id);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询书籍种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addBookType(BookType bookType) {
        try {
            if (bookType == null) return ResultDTO.paramError("书籍种类信息不能为空");
            bookTypeDao.add(bookType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增书籍种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateBookType(BookType bookType) {
        try {
            if (bookType == null || bookType.getId() == null) return ResultDTO.paramError("书籍种类信息/ID不能为空");
            bookTypeDao.update(bookType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新书籍种类失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBookType(Integer id) {
        try {
            if (id == null) return ResultDTO.paramError("书籍种类ID不能为空");
            bookTypeDao.del(id);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除书籍种类失败：" + e.getMessage());
        }
    }
}
