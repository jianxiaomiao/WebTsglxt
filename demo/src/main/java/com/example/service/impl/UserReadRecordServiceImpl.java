package com.example.service.impl;

import com.example.dao.UserReadRecordDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadRecord;
import com.example.service.UserReadRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户阅读记录Service实现类
 */
@Service
public class UserReadRecordServiceImpl implements UserReadRecordService {

    private final UserReadRecordDao userReadRecordDao;

    public UserReadRecordServiceImpl(UserReadRecordDao userReadRecordDao) {
        this.userReadRecordDao = userReadRecordDao;
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryAllRecords() {
        try {
            List<UserReadRecord> recordList = userReadRecordDao.queryAll();
            return ResultDTO.success(recordList);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有阅读记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryRecordById(Integer id) {
        try {
            List<UserReadRecord> recordList = userReadRecordDao.queryById(id);
            return ResultDTO.success(recordList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryRecordsByUserId(String userId) {
        try {
            List<UserReadRecord> recordList = userReadRecordDao.queryByUserId(userId);
            return ResultDTO.success(recordList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryRecordsByIsbn(String isbn) {
        try {
            List<UserReadRecord> recordList = userReadRecordDao.queryByIsbn(isbn);
            return ResultDTO.success(recordList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryRecordsByUserIdAndIsbn(String userId, String isbn) {
        try {
            List<UserReadRecord> recordList = userReadRecordDao.queryByUserIdAndIsbn(userId, isbn);
            return ResultDTO.success(recordList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addRecord(UserReadRecord userReadRecord) {
        try {
            userReadRecordDao.add(userReadRecord);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteRecord(Integer id) {
        try {
            userReadRecordDao.del(id);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> queryByUserIdIsbnAndDate(String userId, String isbn, LocalDate readDate) {
        try {
            List<UserReadRecord> records = userReadRecordDao.queryByUserIdIsbnAndDate(userId, isbn, readDate);
            return ResultDTO.success(records);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail(e.getMessage());
        }
    }

    @Override
    public ResultDTO<Integer> sumReadDuration(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Integer total = userReadRecordDao.sumReadDurationByUserIdAndTime(userId, startTime, endTime);
            return ResultDTO.success(total);
        } catch (RuntimeException e) {
            return ResultDTO.fail("统计阅读时长失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserReadRecord>> listBookReadDuration(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<UserReadRecord> list = userReadRecordDao.listBookReadDurationByTime(userId, startTime, endTime);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("统计书籍阅读时长失败：" + e.getMessage());
        }
    }
}