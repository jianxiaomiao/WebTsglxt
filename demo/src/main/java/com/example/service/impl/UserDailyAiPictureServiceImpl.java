package com.example.service.impl;

import com.example.dao.UserDailyAiPictureDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserDailyAiPicture;
import com.example.service.UserDailyAiPictureService;
import com.example.servlet.UserDailyAiPictureServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public class UserDailyAiPictureServiceImpl implements UserDailyAiPictureService {
    private final UserDailyAiPictureDao pictureDao;
    private static final Logger logger = LoggerFactory.getLogger(UserDailyAiPictureServiceImpl.class);
    // 构造注入Dao
    public UserDailyAiPictureServiceImpl(UserDailyAiPictureDao pictureDao) {
        this.pictureDao = pictureDao;
    }

    @Override
    public ResultDTO<Void> addPicture(UserDailyAiPicture picture) {
        try {
            pictureDao.add(picture);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增每日AI插画失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<UserDailyAiPicture> queryById(BigInteger id) {
        try {
            List<UserDailyAiPicture> list = pictureDao.queryById(id);
            if (list.isEmpty()) {
                return ResultDTO.fail("未查询到对应插画数据");
            }
            return ResultDTO.success(list.get(0));
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据ID查询插画失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<UserDailyAiPicture>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            List<UserDailyAiPicture> dataList = pictureDao.queryByUserIdPage(userId, pageNum, pageSize);
            Long total = pictureDao.countByUserId(userId);
            PageResultDTO<UserDailyAiPicture> pageData = PageResultDTO.success(total, pageNum, pageSize, dataList);
            return ResultDTO.success(pageData);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询用户AI插画失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<UserDailyAiPicture> queryByUserIdAndDate(String userId, LocalDate genDate) {
        try {
            // 新增日志：打印最终查询条件
            logger.info("按用户+日期查询插画，入参userId={}, targetDate={}", userId, genDate);
            List<UserDailyAiPicture> list = pictureDao.queryByUserIdAndDate(userId, genDate);
            logger.info("查询结果条数：{}", list.size()); // 打印查出多少条数据
            if (list.isEmpty()) {
                // 日志打印缺失数据的完整条件，方便核对数据库
                logger.warn("用户{} 日期{} 未查询到AI插画记录", userId, genDate);
                return ResultDTO.fail("该用户当日无AI插画记录");
            }
            return ResultDTO.success(list.get(0));
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("按用户+日期查询插画异常，userId={},date={}",userId,genDate,e);
            return ResultDTO.fail("按用户+日期查询插画失败：" + e.getMessage());
        }
    }
}