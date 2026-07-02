package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.entity.UserNotification;
import com.example.service.UserNotificationService;
import com.example.service.impl.UserNotificationServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/notifications")
public class NotificationServlet extends BaseServlet {
    private UserNotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(NotificationServlet.class);

    @Override
    public void init() throws ServletException {
        notificationService = new UserNotificationServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req,resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String userId = req.getParameter("userId");
        String action = req.getParameter("action");

        try {
            if (userId == null || userId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                return;
            }

            // 1. 查询用户所有通知
            if ("list".equals(action) || action == null) {
                ResultDTO<List<UserNotification>> result = notificationService.getNotificationsByUserId(userId);
                out.write(JSON.toJSONString(result));
            }
            // 2. 标记通知已读
            else if ("read".equals(action)) {
                String notificationIdStr = req.getParameter("notificationId");
                if (notificationIdStr == null) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("通知ID不能为空")));
                    return;
                }
                Integer notificationId = Integer.parseInt(notificationIdStr);
                ResultDTO<Void> result = notificationService.markNotificationRead(notificationId);
                out.write(JSON.toJSONString(result));
            }
            // 🔥 3. 批量标记所有未读通知为已读（核心新增）
            else if ("readAll".equals(action)) {
                ResultDTO<Void> result = notificationService.batchMarkAllRead(userId);
                out.write(JSON.toJSONString(result));
            }
            else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知操作类型")));
            }
        } catch (Exception e) {
            logger.error("处理通知请求异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

}