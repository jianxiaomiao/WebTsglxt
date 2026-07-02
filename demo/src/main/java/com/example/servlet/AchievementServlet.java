package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dto.ResultDTO;
import com.example.entity.UserInformation;
import com.example.service.AchievementService;
import com.example.service.impl.AchievementServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 成就系统 Servlet
 *
 * GET  /api/achievements?userId=xxx    → 获取用户成就列表 + 等级信息
 * POST /api/achievements/check          → 检测并解锁新成就
 */
@WebServlet("/api/achievements/*")
public class AchievementServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(AchievementServlet.class);
    private final AchievementService achievementService = new AchievementServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        if (!requireLogin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String userId = req.getParameter("userId");
            if (userId == null || userId.isBlank()) {
                // 尝试从 session 获取
                UserInformation currentUser = (UserInformation) req.getSession().getAttribute("currentUser");
                if (currentUser != null) {
                    userId = currentUser.getUserId();
                }
            }

            ResultDTO<Map<String, Object>> result = achievementService.getAchievements(userId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("获取成就列表异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常")));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        if (!requireLogin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String pathInfo = req.getPathInfo();
            if (!"/check".equals(pathInfo)) {
                out.write(JSON.toJSONString(ResultDTO.fail("未知路径: " + pathInfo)));
                return;
            }

            // 从 session 获取当前用户
            UserInformation currentUser = (UserInformation) req.getSession().getAttribute("currentUser");
            if (currentUser == null) {
                out.write(JSON.toJSONString(ResultDTO.fail("请先登录")));
                return;
            }

            ResultDTO<Map<String, Object>> result = achievementService.checkAndUnlock(currentUser.getUserId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("成就检测异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常")));
        }
    }
}
