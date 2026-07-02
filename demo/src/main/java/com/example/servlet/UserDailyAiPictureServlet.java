package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserDailyAiPictureDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserDailyAiPicture;
import com.example.service.UserDailyAiPictureService;
import com.example.service.impl.UserDailyAiPictureServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet("/api/user/dailyAiPic")
public class UserDailyAiPictureServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserDailyAiPictureServlet.class);
    private UserDailyAiPictureService pictureService;

    @Override
    public void init() throws ServletException {
        // 初始化Service
        pictureService = new UserDailyAiPictureServiceImpl(new UserDailyAiPictureDaoImpl());
    }

    // 仅开放GET查询接口，POST/PUT/DELETE全部不实现，前端无法新增/修改/删除
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;

        try {
            // 获取前端传参
            // 获取前端传参，全部trim去空格
            String idStr = req.getParameter("id");
            String userId = req.getParameter("userId");
            String genDateStr = req.getParameter("genDate");
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");

            // 关键：去除首尾空白字符
            if(userId != null) userId = userId.trim();
            if(genDateStr != null) genDateStr = genDateStr.trim();
            if(idStr != null) idStr = idStr.trim();

            Integer pageNum = pageNumStr != null && !pageNumStr.isEmpty() ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = pageSizeStr != null && !pageSizeStr.isEmpty() ? Integer.parseInt(pageSizeStr) : 10;

            // 1. 根据主键id查询
            if (idStr != null && !idStr.isEmpty()) {
                BigInteger picId = new BigInteger(idStr);
                result = pictureService.queryById(picId);
                logger.info("查询AI插画，主键ID：{}", idStr);
            }
            // 2. 用户ID + 指定日期 查询单张（前端传今日，后端传昨日日期）
            else if (userId != null && !userId.isEmpty() && genDateStr != null && !genDateStr.isEmpty()) {
                LocalDate showDate = LocalDate.parse(genDateStr);
                // 关键：自动向前减一天，和定时任务生成逻辑对齐
                LocalDate realQueryDate = showDate.minusDays(1);
                result = pictureService.queryByUserIdAndDate(userId, realQueryDate);
                logger.info("查询用户{}，前端展示日期{}，实际查询数据库日期{}", userId, genDateStr, realQueryDate);
            }
            // 3. 用户ID分页查询全部历史插画
            else if (userId != null && !userId.isEmpty()) {
                result = pictureService.queryByUserIdPage(userId, pageNum, pageSize);
                logger.info("分页查询用户{}AI插画，页码：{}", userId, pageNum);
            }
            // 无参数
            else {
                result = ResultDTO.paramError("请传入userId、id或genDate查询条件");
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("数字参数转换异常", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("id、pageNum、pageSize必须为数字")));
        } catch (DateTimeParseException e) {
            logger.error("日期格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("genDate日期格式必须为 yyyy-MM-dd")));
        } catch (Exception e) {
            logger.error("查询用户每日AI插画接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 关闭新增、修改、删除接口，前端调用直接返回提示
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(JSON.toJSONString(ResultDTO.fail("AI插画生成仅后端内部调用，前端不支持提交新增")));
        out.flush();
        out.close();
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(JSON.toJSONString(ResultDTO.fail("不支持修改AI插画")));
        out.flush();
        out.close();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(JSON.toJSONString(ResultDTO.fail("不支持删除AI插画记录")));
        out.flush();
        out.close();
    }
}