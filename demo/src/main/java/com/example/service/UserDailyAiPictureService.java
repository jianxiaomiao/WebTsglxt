package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserDailyAiPicture;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface UserDailyAiPictureService {
    // 新增插画（后台AI生成内部调用，前端不暴露）
    ResultDTO<Void> addPicture(UserDailyAiPicture picture);

    // 根据id查询
    ResultDTO<UserDailyAiPicture> queryById(BigInteger id);

    // 用户分页查询历史插画
    ResultDTO<PageResultDTO<UserDailyAiPicture>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize);

    // 用户+指定日期查询单条插画
    ResultDTO<UserDailyAiPicture> queryByUserIdAndDate(String userId, LocalDate genDate);
}