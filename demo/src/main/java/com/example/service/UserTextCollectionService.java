package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserTextCollection;
import java.util.List;

public interface UserTextCollectionService {
    ResultDTO<List<UserTextCollection>> queryAllNotes();
    ResultDTO<List<UserTextCollection>> queryNoteById(Integer id);
    ResultDTO<List<UserTextCollection>> queryNoteByUserId(String userId);
    // 新增：分页查询用户阅读笔记
    ResultDTO<PageResultDTO<UserTextCollection>> queryNoteByUserIdPage(String userId, Integer pageNum, Integer pageSize);
    ResultDTO<List<UserTextCollection>> queryNoteByIsbn(String isbn);
    ResultDTO<Void> addNote(UserTextCollection note);
    ResultDTO<Void> deleteNote(Integer id);
    ResultDTO<Void> updateNote(UserTextCollection note);
    ResultDTO<List<UserTextCollection>> queryNoteByChapterId(String chapterId);
}