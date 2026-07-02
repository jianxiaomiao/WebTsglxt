package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserReadDailyDaoImpl;
import com.example.dao.impl.UserReadStatsDaoImpl;
import com.example.dto.ReadStatsDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserReadDaily;
import com.example.service.impl.UserReadDailyServiceImpl;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/user/read/*")
public class UserReadDailyServlet extends BaseServlet {
    private UserReadDailyServiceImpl userReadDailyService;
    private static final Logger logger = LoggerFactory.getLogger(UserReadDailyServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
        userReadDailyService = new UserReadDailyServiceImpl(
                new UserReadDailyDaoImpl(),
                new UserReadStatsDaoImpl()
        );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String path = req.getPathInfo();
        String userId = req.getParameter("userId");

        try {
            if ("/stats".equals(path)) {
                // 获取阅读统计数据（前端Gitee图表用）
                ResultDTO<ReadStatsDTO> result = userReadDailyService.getReadStats(userId);
                out.write(JSON.toJSONString(result));
            } else if ("/records".equals(path)) {
                // 获取时间段阅读记录
                LocalDate startDate = req.getParameter("startDate") != null ? LocalDate.parse(req.getParameter("startDate")) : null;
                LocalDate endDate = req.getParameter("endDate") != null ? LocalDate.parse(req.getParameter("endDate")) : null;
                ResultDTO<List<UserReadDaily>> result = userReadDailyService.getReadRecords(userId, startDate, endDate);
                out.write(JSON.toJSONString(result));
            }  // ====================== 🆕 新增：手动刷新阅读统计缓存（测试专用） ======================
            else if ("/refresh".equals(path)) {
                userReadDailyService.refreshAllUserStats();
                out.write(JSON.toJSONString(ResultDTO.success("手动刷新阅读统计缓存成功！")));
            }else {
                out.write(JSON.toJSONString(ResultDTO.fail("请求路径错误")));
            }
        } catch (Exception e) {
            logger.error("处理阅读查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            UserReadDaily readDaily = JSON.parseObject(req.getInputStream(), UserReadDaily.class);
            ResultDTO<Void> result = userReadDailyService.upsertReadDuration(readDaily);
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(readDaily.getUserId(), 31, null, null, "用户" + readDaily.getReadDate() + "阅读时长更新~");
        } catch (Exception e) {
            logger.error("新增/更新阅读时长异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增/更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}