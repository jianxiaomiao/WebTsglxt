package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserReadProgress;

import java.util.List;

public interface UserReadProgressService {
    ResultDTO<List<UserReadProgress>> queryAllProgress();
    ResultDTO<List<UserReadProgress>> queryProgressById(Integer id);
    ResultDTO<List<UserReadProgress>> queryProgressByUserId(String userId);
    ResultDTO<List<UserReadProgress>> queryProgressByIsbn(String isbn);
    ResultDTO<Void> addProgress(UserReadProgress userReadProgress);
    ResultDTO<Void> deleteProgress(Integer id);
    ResultDTO<Void> updateProgress(UserReadProgress userReadProgress);
    ResultDTO<List<UserReadProgress>> queryProgressByUserIdAndIsbn(String userId, String isbn);
}