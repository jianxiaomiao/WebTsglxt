package com.example.service.impl;

import com.example.dao.BookTagDao;
import com.example.dao.impl.BookTagDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookTag;
import com.example.service.BookTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookTagServiceImpl implements BookTagService {
    private static final Logger logger = LoggerFactory.getLogger(BookTagServiceImpl.class);
    private final BookTagDao tagDao;

    public BookTagServiceImpl() {
        this.tagDao = new BookTagDaoImpl();
    }

    @Override
    public ResultDTO<Void> addTag(BookTag tag) {
        try {
            tagDao.add(tag);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("新增标签失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteTag(Integer id) {
        try {
            tagDao.deleteById(id);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("删除标签失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateTag(BookTag tag) {
        try {
            tagDao.updateById(tag);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("修改标签失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<BookTag> getTagById(Integer id) {
        try {
            BookTag tag = tagDao.selectById(id);
            return ResultDTO.success(tag);
        } catch (Exception e) {
            return ResultDTO.fail("查询标签失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookTag>> getAllTags() {
        try {
            List<BookTag> list = tagDao.selectAll();
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("查询所有标签失败：" + e.getMessage());
        }
    }
}