package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.dao.UserReadRecordDao;
import com.example.dao.impl.UserReadRecordDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadRecord;
import com.example.service.UserReadRecordService;
import com.example.service.impl.UserReadRecordServiceImpl;
import com.example.util.LocalDateTimeDeserializer;
import com.example.util.UserBehaviorLogger;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户阅读记录Servlet
 * 接口路径：/api/user/record
 */
@WebServlet("/api/user/record")
public class UserReadRecordServlet extends BaseServlet {
    private UserReadRecordDaoImpl userReadRecordDao;
    private UserReadRecordService userReadRecordService;
    private static final Logger logger = LoggerFactory.getLogger(UserReadRecordServlet.class);

    @Override
    public void init() throws ServletException {
        userReadRecordDao = new UserReadRecordDaoImpl();
        userReadRecordService = new UserReadRecordServiceImpl(userReadRecordDao);

        // 配置FastJSON日期序列化格式
        JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteDateUseDateFormat.getMask();

        // 🔥 全部使用你自己写的反序列化器（100%兼容你的现有逻辑）
        ParserConfig.getGlobalInstance().putDeserializer(
                LocalDateTime.class,
                new com.example.util.LocalDateTimeDeserializer() // 你原来的
        );
        ParserConfig.getGlobalInstance().putDeserializer(
                LocalDate.class,
                new com.example.util.LocalDateDeserializer() // 新增的，和你风格一致
        );

        logger.info("UserReadRecordServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            String userId = req.getParameter("userId");
            String isbn = req.getParameter("isbn");
            String readDateStr = req.getParameter("readDate"); // 🔥 新增：阅读日期参数
            String startTimeStr = req.getParameter("startTime");
            String endTimeStr = req.getParameter("endTime");
            String type = req.getParameter("type");

            // 🔥 1️⃣ 最高优先级：按【用户+ISBN+日期】查询（精确到天）
            if (userId != null && !userId.isEmpty()
                    && isbn != null && !isbn.isEmpty()
                    && readDateStr != null && !readDateStr.isEmpty()) {

                LocalDate readDate = LocalDate.parse(readDateStr);
                ResultDTO<List<UserReadRecord>> result = userReadRecordService.queryByUserIdIsbnAndDate(userId, isbn, readDate);
                logger.info("按UserId[{}]、ISBN[{}]和日期[{}]查询阅读记录完成", userId, isbn, readDate);
                out.write(JSON.toJSONString(result));
                return;
            }

            // 2️⃣ 查询【书籍阅读时长排行】
            if ("bookDuration".equals(type) && userId != null && !userId.isEmpty()
                    && startTimeStr != null && !startTimeStr.isEmpty()
                    && endTimeStr != null && !endTimeStr.isEmpty()) {

                LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
                LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
                ResultDTO<List<UserReadRecord>> result = userReadRecordService.listBookReadDuration(userId, startTime, endTime);
                logger.info("查询用户[{}]时间段内书籍阅读时长排行完成", userId);
                out.write(JSON.toJSONString(result));
                return;
            }

            // 3️⃣ 统计【总阅读时长】
            if (userId != null && !userId.isEmpty() && startTimeStr != null && !startTimeStr.isEmpty() && endTimeStr != null && !endTimeStr.isEmpty()) {
                LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
                LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
                ResultDTO<Integer> result = userReadRecordService.sumReadDuration(userId, startTime, endTime);
                logger.info("统计用户[{}]从{}到{}的阅读时长完成", userId, startTime, endTime);
                out.write(JSON.toJSONString(result));
                return;
            }

            // 4️⃣ 其他通用查询
            ResultDTO<List<UserReadRecord>> result;
            if (userId != null && !userId.isEmpty() && isbn != null && !isbn.isEmpty()) {
                result = userReadRecordService.queryRecordsByUserIdAndIsbn(userId, isbn);
                logger.info("按UserId[{}]和ISBN[{}]查询阅读记录完成", userId, isbn);
            } else if (id != null && !id.isEmpty()) {
                result = userReadRecordService.queryRecordById(Integer.parseInt(id));
                logger.info("按Id[{}]查询阅读记录完成", id);
            } else if (userId != null && !userId.isEmpty()) {
                result = userReadRecordService.queryRecordsByUserId(userId);
                logger.info("按UserId[{}]查询阅读记录完成", userId);
            } else if (isbn != null && !isbn.isEmpty()) {
                result = userReadRecordService.queryRecordsByIsbn(isbn);
                logger.info("按ISBN[{}]查询阅读记录完成", isbn);
            } else {
                result = userReadRecordService.queryAllRecords();
                logger.info("查询所有阅读记录完成");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("阅读记录查询异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("查询阅读记录失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            UserReadRecord userReadRecord = JSON.parseObject(req.getInputStream(), UserReadRecord.class);

            // 🔥 自动补全字段（前端不传时后端自动生成）
            if (userReadRecord.getCreateTime() == null) {
                userReadRecord.setCreateTime(LocalDateTime.now());
            }
            // 🔥 核心：自动补全阅读日期（默认当天）
            if (userReadRecord.getReadDate() == null) {
                userReadRecord.setReadDate(LocalDate.now());
            }

            // 现在add方法已经实现了【新增+更新二合一】的原子操作
            ResultDTO<Void> result = userReadRecordService.addRecord(userReadRecord);
            logger.info("原子性更新阅读记录[UserId:{}, ISBN:{}, 日期:{}, 新增时长:{}秒]完成",
                    userReadRecord.getUserId(), userReadRecord.getIsbn(),
                    userReadRecord.getReadDate(), userReadRecord.getReadDuration());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(userReadRecord.getUserId(), 33, userReadRecord.getIsbn(), null, "用户今日阅读书籍"+ userReadRecord.getIsbn() + "时长增加" );
        } catch (Exception e) {
            logger.error("新增/更新阅读记录异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("新增/更新阅读记录失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            if (id == null || id.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("阅读记录ID不能为空")));
                return;
            }

            ResultDTO<Void> result = userReadRecordService.deleteRecord(Integer.parseInt(id));
            logger.info("删除阅读记录[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除阅读记录异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("删除阅读记录失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}