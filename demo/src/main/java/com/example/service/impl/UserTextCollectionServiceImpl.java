package com.example.service.impl;

import com.example.dao.UserTextCollectionDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserTextCollection;
import com.example.service.UserTextCollectionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTextCollectionServiceImpl implements UserTextCollectionService {

    private final UserTextCollectionDao userTextCollectionDao;

    public UserTextCollectionServiceImpl(UserTextCollectionDao userTextCollectionDao) {
        this.userTextCollectionDao = userTextCollectionDao;
    }

    @Override
    public ResultDTO<List<UserTextCollection>> queryAllNotes() {
        try {
            List<UserTextCollection> notes = userTextCollectionDao.queryAll();
            return ResultDTO.success(notes);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserTextCollection>> queryNoteById(Integer id) {
        try {
            if (id == null) {
                return ResultDTO.paramError("阅读笔记ID不能为空");
            }
            List<UserTextCollection> notes = userTextCollectionDao.queryById(id);
            return ResultDTO.success(notes);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserTextCollection>> queryNoteByUserId(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            List<UserTextCollection> notes = userTextCollectionDao.queryByUserId(userId);
            return ResultDTO.success(notes);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按用户ID查询阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<UserTextCollection>> queryNoteByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            // 查询分页数据
            List<UserTextCollection> list = userTextCollectionDao.queryByUserIdPage(userId, pageNum, pageSize);
            // 查询总条数
            Long total = userTextCollectionDao.countByUserId(userId);
            // 封装分页返回对象
            PageResultDTO<UserTextCollection> pageResult = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(pageResult);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserTextCollection>> queryNoteByIsbn(String isbn) {
        try {
            if (isbn == null || isbn.isEmpty()) {
                return ResultDTO.paramError("书籍ISBN不能为空");
            }
            List<UserTextCollection> notes = userTextCollectionDao.queryByIsbn(isbn);
            return ResultDTO.success(notes);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ISBN查询阅读笔记失败：" + e.getMessage());
        }
    }

    // ===================== 新增：按章节ID查询 =====================
    @Override
    public ResultDTO<List<UserTextCollection>> queryNoteByChapterId(String chapterId) {
        try {
            if (chapterId == null || chapterId.isEmpty()) {
                return ResultDTO.paramError("章节ID不能为空");
            }
            List<UserTextCollection> notes = userTextCollectionDao.queryByChapterId(chapterId);
            return ResultDTO.success(notes);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按章节ID查询阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addNote(UserTextCollection note) {
        try {
            if (note == null) {
                return ResultDTO.paramError("阅读笔记信息不能为空");
            }
            userTextCollectionDao.add(note);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteNote(Integer id) {
        try {
            if (id == null) {
                return ResultDTO.paramError("阅读笔记ID不能为空");
            }
            userTextCollectionDao.del(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除阅读笔记失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateNote(UserTextCollection note) {
        try {
            if (note == null || note.getId() == null) {
                return ResultDTO.paramError("阅读笔记信息/ID不能为空");
            }
            userTextCollectionDao.update(note);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新阅读笔记失败：" + e.getMessage());
        }
    }
}