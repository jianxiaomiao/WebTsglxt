package com.example.dao.impl;

import com.example.dao.UserDailyAiPictureDao;
import com.example.entity.UserDailyAiPicture;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class UserDailyAiPictureDaoImpl implements UserDailyAiPictureDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDailyAiPictureDaoImpl.class);

    @Override
    public void add(UserDailyAiPicture picture) {
        if (picture == null) {
            throw new IllegalArgumentException("AI每日插画数据不能为空");
        }
        if (picture.getUserId() == null || picture.getUserId().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (picture.getGenDate() == null) {
            throw new IllegalArgumentException("生成日期不能为空");
        }
        try {
            // INSERT IGNORE 利用唯一索引 uk_user_date，重复数据直接忽略不报错
            String sql = "insert ignore into user_daily_ai_picture(user_id, gen_date, img_url) values (?, ?, ?)";
            int affected = DBUtil.executeUpdate(sql,
                    picture.getUserId(),
                    Date.valueOf(picture.getGenDate()),
                    picture.getImgUrl()
            );
            logger.info("新增用户AI每日插画，受影响行数：{}，用户ID：{}，日期：{}", affected, picture.getUserId(), picture.getGenDate());
        } catch (SQLException e) {
            logger.error("新增AI每日插画失败，用户ID：{}", picture.getUserId(), e);
            throw new RuntimeException("新增AI插画数据库异常", e);
        }
    }

    @Override
    public List<UserDailyAiPicture> queryById(BigInteger id) {
        if (id == null) {
            throw new IllegalArgumentException("插画主键ID不能为空");
        }
        try {
            String sql = "select * from user_daily_ai_picture where id = ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserDailyAiPicture pic = new UserDailyAiPicture();
                pic.setId(BigInteger.valueOf(rs.getLong("id")));
                pic.setUserId(rs.getString("user_id"));
                Date date = rs.getDate("gen_date");
                pic.setGenDate(date != null ? date.toLocalDate() : null);
                pic.setImgUrl(rs.getString("img_url"));
                if (rs.getTimestamp("create_time") != null) {
                    pic.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return pic;
            }, id);
        } catch (SQLException e) {
            logger.error("根据ID查询AI插画异常，id：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDailyAiPicture> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        // 分页参数修正
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "select * from user_daily_ai_picture where user_id = ? order by gen_date desc limit ? offset ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserDailyAiPicture pic = new UserDailyAiPicture();
                pic.setId(BigInteger.valueOf(rs.getLong("id")));
                pic.setUserId(rs.getString("user_id"));
                Date date = rs.getDate("gen_date");
                pic.setGenDate(date != null ? date.toLocalDate() : null);
                pic.setImgUrl(rs.getString("img_url"));
                if (rs.getTimestamp("create_time") != null) {
                    pic.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return pic;
            }, userId, pageSize, offset);
        } catch (SQLException e) {
            logger.error("分页查询用户AI插画异常，userId：{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return 0L;
        }
        try {
            String sql = "select count(*) total from user_daily_ai_picture where user_id = ?";
            return DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId).get(0);
        } catch (SQLException e) {
            logger.error("统计用户AI插画总数异常，userId：{}", userId, e);
            return 0L;
        }
    }

    @Override
    public List<UserDailyAiPicture> queryByUserIdAndDate(String userId, LocalDate genDate) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (genDate == null) {
            throw new IllegalArgumentException("查询日期不能为空");
        }
        logger.info("DAO执行查询，userId={},genDate={}",userId,genDate);
        try {
            String sql = "select * from user_daily_ai_picture where user_id = ? and gen_date = ?";
            return DBUtil.executeQuery(sql, rs -> {
                UserDailyAiPicture pic = new UserDailyAiPicture();
                pic.setId(BigInteger.valueOf(rs.getLong("id")));
                pic.setUserId(rs.getString("user_id"));
                Date date = rs.getDate("gen_date");
                pic.setGenDate(date != null ? date.toLocalDate() : null);
                pic.setImgUrl(rs.getString("img_url"));
                if (rs.getTimestamp("create_time") != null) {
                    pic.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                }
                return pic;
            }, userId, Date.valueOf(genDate));
        } catch (SQLException e) {
            logger.error("根据用户+日期查询AI插画异常，userId：{}，date：{}", userId, genDate, e);
            return Collections.emptyList();
        }
    }
}