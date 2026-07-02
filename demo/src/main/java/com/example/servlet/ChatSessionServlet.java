package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.ChatSessionDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.ChatSession;
import com.example.service.ChatSessionService;
import com.example.service.impl.ChatSessionServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/chat/session")
public class ChatSessionServlet extends BaseServlet {
    private ChatSessionDaoImpl chatSessionDao;
    private ChatSessionService chatSessionService;
    private static final Logger logger = LoggerFactory.getLogger(ChatSessionServlet.class);

    @Override
    public void init() throws ServletException {
        chatSessionDao = new ChatSessionDaoImpl();
        chatSessionService = new ChatSessionServiceImpl(chatSessionDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");
        String userId = req.getParameter("userId");

        try {
            if ("list".equals(action)) {
                // 查询会话列表
                ResultDTO<List<ChatSession>> result = chatSessionService.getSessionList(userId);
                out.write(JSON.toJSONString(result));
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (Exception e) {
            logger.error("查询会话异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String userId = req.getParameter("userId");
        String targetUserId = req.getParameter("targetUserId");

        try {
            if (userId == null || targetUserId == null) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                return;
            }
            ResultDTO<Void> result = chatSessionService.deleteSession(userId, targetUserId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除会话异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}