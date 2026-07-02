package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.FriendDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.Friend;
import com.example.service.FriendService;
import com.example.service.impl.FriendServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/friend")
public class FriendServlet extends BaseServlet {
    private FriendDaoImpl friendDao;
    private FriendService friendService;
    private static final Logger logger = LoggerFactory.getLogger(FriendServlet.class);

    @Override
    public void init() throws ServletException {
        friendDao = new FriendDaoImpl();
        friendService = new FriendServiceImpl(friendDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");
        String userId = req.getParameter("userId");
        String friendId = req.getParameter("friendId");

        try {
            if ("list".equals(action)) {
                // 查询好友列表
                ResultDTO<List<Friend>> result = friendService.getFriendList(userId);
                out.write(JSON.toJSONString(result));
            } else if ("isFriend".equals(action)) {
                // 判断是否是好友
                ResultDTO<Boolean> result = friendService.isFriend(userId, friendId);
                out.write(JSON.toJSONString(result));
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (Exception e) {
            logger.error("查询好友异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");
        String userId = req.getParameter("userId");
        String friendId = req.getParameter("friendId");
        String remark = req.getParameter("remark");

        try {
            if ("add".equals(action)) {
                // 添加好友
                ResultDTO<Void> result = friendService.addFriend(userId, friendId);
                out.write(JSON.toJSONString(result));
            } else if ("updateRemark".equals(action)) {
                // 更新备注
                ResultDTO<Void> result = friendService.updateRemark(userId, friendId, remark);
                out.write(JSON.toJSONString(result));
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (Exception e) {
            logger.error("操作好友异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
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
        String friendId = req.getParameter("friendId");

        try {
            if (userId == null || friendId == null) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID和好友ID不能为空")));
                return;
            }
            ResultDTO<Void> result = friendService.deleteFriend(userId, friendId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除好友异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}