package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.Bookmark;
import java.util.List;

public interface BookmarkService {
    // 新增书签
    ResultDTO<Void> addBookmark(Bookmark bookmark);

    // 修改书签（原有：联合条件）
    ResultDTO<Void> updateBookmark(Bookmark bookmark);

    // 🔥 新增：根据自增ID修改书签
    ResultDTO<Void> updateBookmarkById(Bookmark bookmark);

    // 删除书签（原有：联合条件）
    ResultDTO<Void> deleteBookmark(String userId, String isbn, String chapterNumber);

    // 🔥 新增：根据自增ID删除书签
    ResultDTO<Void> deleteBookmarkById(Long id);

    // 根据ISBN查询
    ResultDTO<List<Bookmark>> queryByIsbn(String isbn);

    // 根据用户ID查询
    ResultDTO<List<Bookmark>> queryByUserId(String userId);

    // 根据章节号查询
    ResultDTO<List<Bookmark>> queryByChapterNumber(String chapterNumber);

    // 根据用户ID+ISBN查询
    ResultDTO<List<Bookmark>> queryByUserIdAndIsbn(String userId, String isbn);
}