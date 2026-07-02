package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.dao.impl.UserReadProgressDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadProgress;
import com.example.service.UserReadProgressService;
import com.example.service.impl.UserReadProgressServiceImpl;
import com.example.util.LocalDateTimeDeserializer;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/api/user/progress")
public class UserReadProgressServlet extends BaseServlet {
    private UserReadProgressDaoImpl userReadProgressDao;
    private UserReadProgressService userReadProgressService;
    private static final Logger logger = LoggerFactory.getLogger(UserReadProgressServlet.class);

    @Override
    public void init() throws ServletException {
        userReadProgressDao = new UserReadProgressDaoImpl();
        userReadProgressService = new UserReadProgressServiceImpl(userReadProgressDao);
        // ✅ 配置FastJSON支持时间戳转LocalDateTime（解决lastReadTime为null的问题）
        JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteDateUseDateFormat.getMask();
        ParserConfig.getGlobalInstance().putDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        logger.info("UserReadProgressServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            String userId = req.getParameter("userId");
            String isbn = req.getParameter("isbn");

            ResultDTO<List<UserReadProgress>> result;
            // ✅ 优先处理同时按userId和isbn查询（优先级最高）
            if (userId != null && !userId.isEmpty() && isbn != null && !isbn.isEmpty()) {
                result = userReadProgressService.queryProgressByUserIdAndIsbn(userId, isbn);
                logger.info("按UserId[{}]和ISBN[{}]查询阅读进度完成", userId, isbn);
            }
            else if (id != null && !id.isEmpty()) {
                result = userReadProgressService.queryProgressById(Integer.parseInt(id));
                logger.info("按Id[{}]查询阅读进度完成", id);
            } else if (userId != null && !userId.isEmpty()) {
                result = userReadProgressService.queryProgressByUserId(userId);
                logger.info("按UserId[{}]查询阅读进度完成", userId);
            } else if (isbn != null && !isbn.isEmpty()) {
                result = userReadProgressService.queryProgressByIsbn(isbn);
                logger.info("按ISBN[{}]查询阅读进度完成", isbn);
            } else {
                result = userReadProgressService.queryAllProgress();
                logger.info("查询所有阅读进度完成");
            }


            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("阅读进度查询异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("查询阅读进度失败：" + e.getMessage());
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
        PrintWriter out = resp.getWriter();

        try {
            UserReadProgress userReadProgress = JSON.parseObject(req.getInputStream(), UserReadProgress.class);
            ResultDTO<Void> result = userReadProgressService.addProgress(userReadProgress);
            logger.info("新增阅读进度[UserId:{}, ISBN:{}]请求处理完成", userReadProgress.getUserId(), userReadProgress.getIsbn());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(userReadProgress.getUserId(), 32, userReadProgress.getIsbn(), userReadProgress.getPageNum(), "用户阅读进度增加（阅读历史）" );
        } catch (Exception e) {
            logger.error("新增阅读进度异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("新增阅读进度失败：" + e.getMessage());
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
                out.write(JSON.toJSONString(ResultDTO.paramError("阅读进度ID不能为空")));
                out.flush();
                out.close();
                return;
            }

            ResultDTO<Void> result = userReadProgressService.deleteProgress(Integer.parseInt(id));
            logger.info("删除阅读进度[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除阅读进度异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("删除阅读进度失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            UserReadProgress userReadProgress = JSON.parseObject(req.getInputStream(), UserReadProgress.class);
            ResultDTO<Void> result = userReadProgressService.updateProgress(userReadProgress);
            logger.info("更新阅读进度[Id:{}]请求处理完成", userReadProgress.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新阅读进度异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("更新阅读进度失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * PATCH请求：仅更新阅读进度（章节+页码+阅读时间）
     * 专门用于"下一章/上一章"场景，不需要传全量对象
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            UserReadProgress userReadProgress = JSON.parseObject(req.getInputStream(), UserReadProgress.class);
            // 这里复用update方法，因为PATCH也是更新，只是前端只传部分字段
            ResultDTO<Void> result = userReadProgressService.updateProgress(userReadProgress);
            logger.info("部分更新阅读进度[Id:{}, PageNum:{}]请求处理完成", userReadProgress.getId(), userReadProgress.getPageNum());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("部分更新阅读进度异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("更新阅读进度失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}