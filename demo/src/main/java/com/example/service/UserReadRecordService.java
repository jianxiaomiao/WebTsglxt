package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.UserReadRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户阅读记录Service接口
 */
public interface UserReadRecordService {
    ResultDTO<List<UserReadRecord>> queryAllRecords();
    ResultDTO<List<UserReadRecord>> queryRecordById(Integer id);
    ResultDTO<List<UserReadRecord>> queryRecordsByUserId(String userId);
    ResultDTO<List<UserReadRecord>> queryRecordsByIsbn(String isbn);
    ResultDTO<List<UserReadRecord>> queryRecordsByUserIdAndIsbn(String userId, String isbn);
    ResultDTO<Void> addRecord(UserReadRecord userReadRecord);
    ResultDTO<Void> deleteRecord(Integer id);
    ResultDTO<List<UserReadRecord>> queryByUserIdIsbnAndDate(String userId, String isbn, LocalDate readDate);
    ResultDTO<Integer> sumReadDuration(String userId, LocalDateTime startTime, LocalDateTime endTime);
    ResultDTO<List<UserReadRecord>> listBookReadDuration(String userId, LocalDateTime startTime, LocalDateTime endTime);
}