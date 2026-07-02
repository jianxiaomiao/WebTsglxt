package com.example.service.impl;

import com.example.dao.BookTagRelationDao;
import com.example.dao.impl.BookTagRelationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookTagRelation;
import com.example.service.BookTagRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookTagRelationServiceImpl implements BookTagRelationService {
    private static final Logger logger = LoggerFactory.getLogger(BookTagRelationServiceImpl.class);
    private final BookTagRelationDao relationDao;

    public BookTagRelationServiceImpl() {
        this.relationDao = new BookTagRelationDaoImpl();
    }

    @Override
    public ResultDTO<Void> addRelation(BookTagRelation relation) {
        try {
            relationDao.add(relation);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("新增关联失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteRelation(Integer id) {
        try {
            relationDao.deleteById(id);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("删除关联失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<BookTagRelation> getRelationById(Integer id) {
        try {
            BookTagRelation relation = relationDao.selectById(id);
            return ResultDTO.success(relation);
        } catch (Exception e) {
            return ResultDTO.fail("查询关联失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookTagRelation>> getRelationsByIsbn(String isbn) {
        try {
            List<BookTagRelation> list = relationDao.selectByIsbn(isbn);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("根据ISBN查询关联失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookTagRelation>> getRelationsByTagId(Integer tagId) {
        try {
            List<BookTagRelation> list = relationDao.selectByTagId(tagId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("根据标签ID查询关联失败：" + e.getMessage());
        }
    }
}