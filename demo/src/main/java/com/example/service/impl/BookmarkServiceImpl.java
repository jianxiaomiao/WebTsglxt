package com.example.service.impl;

import com.example.dao.BookmarkDao;
import com.example.dto.ResultDTO;
import com.example.entity.Bookmark;
import com.example.service.BookmarkService;

import java.util.List;

public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkDao bookmarkDao;

    // 构造注入DAO
    public BookmarkServiceImpl(BookmarkDao bookmarkDao) {
        this.bookmarkDao = bookmarkDao;
    }

    // 新增书签
    @Override
    public ResultDTO<Void> addBookmark(Bookmark bookmark) {
        try {
            if (bookmark == null) {
                return ResultDTO.paramError("书签信息不能为空");
            }
            // 自动填充创建时间
            if (bookmark.getCreateTime() == null) {
                bookmark.setCreateTime(java.time.LocalDateTime.now());
            }
            bookmarkDao.add(bookmark);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增书签失败：" + e.getMessage());
        }
    }

    // 修改书签
    @Override
    public ResultDTO<Void> updateBookmark(Bookmark bookmark) {
        try {
            if (bookmark == null || bookmark.getUserId() == null) {
                return ResultDTO.paramError("书签信息不能为空");
            }
            bookmarkDao.update(bookmark);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新书签失败：" + e.getMessage());
        }
    }

    // ===================== 🔥 新增：根据ID操作书签 =====================
    @Override
    public ResultDTO<Void> updateBookmarkById(Bookmark bookmark) {
        try {
            // 参数校验
            if (bookmark == null || bookmark.getId() == null || bookmark.getId() <= 0) {
                return ResultDTO.paramError("书签信息/书签ID不能为空");
            }
            // 调用DAO层根据ID更新
            bookmarkDao.updateById(bookmark);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据ID更新书签失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteBookmarkById(Long id) {
        try {
            // 参数校验
            if (id == null || id <= 0) {
                return ResultDTO.paramError("书签ID不能为空且必须大于0");
            }
            // 调用DAO层根据ID删除
            bookmarkDao.delById(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据ID删除书签失败：" + e.getMessage());
        }
    }
    // 删除书签
    @Override
    public ResultDTO<Void> deleteBookmark(String userId, String isbn, String chapterNumber) {
        try {
            if (userId == null || isbn == null || chapterNumber == null) {
                return ResultDTO.paramError("参数不能为空");
            }
            bookmarkDao.del(userId, isbn, chapterNumber);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除书签失败：" + e.getMessage());
        }
    }

    // =============== 查询 ===============
    @Override
    public ResultDTO<List<Bookmark>> queryByIsbn(String isbn) {
        try {
            List<Bookmark> list = bookmarkDao.queryByIsbn(isbn);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Bookmark>> queryByUserId(String userId) {
        try {
            List<Bookmark> list = bookmarkDao.queryByUserId(userId);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Bookmark>> queryByChapterNumber(String chapterNumber) {
        try {
            List<Bookmark> list = bookmarkDao.queryByChapterNumber(chapterNumber);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Bookmark>> queryByUserIdAndIsbn(String userId, String isbn) {
        try {
            List<Bookmark> list = bookmarkDao.queryByUserIdAndIsbn(userId, isbn);
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }
}