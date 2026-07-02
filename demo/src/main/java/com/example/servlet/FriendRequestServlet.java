package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.FriendDaoImpl;
import com.example.dao.impl.FriendRequestDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.FriendRequest;
import com.example.service.FriendRequestService;
import com.example.service.impl.FriendRequestServiceImpl;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/user/friend/request")
public class FriendRequestServlet extends BaseServlet {
    private FriendRequestDaoImpl friendRequestDao;
    private FriendDaoImpl friendDao;
    private FriendRequestService friendRequestService;
    private static final Logger logger = LoggerFactory.getLogger(FriendRequestServlet.class);

    @Override
    public void init() throws ServletException {
        friendRequestDao = new FriendRequestDaoImpl();
        friendDao = new FriendDaoImpl();
        friendRequestService = new FriendRequestServiceImpl(friendRequestDao, friendDao);
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
            if ("received".equals(action)) {
                // 查询我收到的申请
                ResultDTO<List<FriendRequest>> result = friendRequestService.getReceivedRequests(userId);
                out.write(JSON.toJSONString(result));
            } else if ("sent".equals(action)) {
                // 查询我发出的申请
                ResultDTO<List<FriendRequest>> result = friendRequestService.getSentRequests(userId);
                out.write(JSON.toJSONString(result));
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (Exception e) {
            logger.error("查询好友申请异常", e);
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
        String fromUserId = req.getParameter("fromUserId");
        String toUserId = req.getParameter("toUserId");
        String requestMsg = req.getParameter("requestMsg");
        String requestIdStr = req.getParameter("requestId");
        String userId = req.getParameter("userId");

        try {
            if ("send".equals(action)) {
                // 发送好友申请
                ResultDTO<Integer> result = friendRequestService.sendRequest(fromUserId, toUserId, requestMsg);
                out.write(JSON.toJSONString(result));
                // ========== 新增：推送好友申请通知给 接收方 ==========
                if (result.isSuccess()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "FRIEND_REQUEST"); // 标记消息类型
                    MessageSseServlet.sendMessageToUser(toUserId, JSON.toJSONString(msg));
                }
                UserBehaviorLogger.logAsync(fromUserId, 17, null, null, "发送好友请求");
            } else if ("accept".equals(action)) {
                // 同意好友申请
                Integer requestId = Integer.parseInt(requestIdStr);
                ResultDTO<Void> result = friendRequestService.acceptRequest(requestId, userId);
                out.write(JSON.toJSONString(result));
                // ========== 新增：推送通知给 申请人 ==========
                if (result.isSuccess()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "FRIEND_REQUEST");
                    // 先查询申请记录，获取fromUserId（申请人ID）
                    FriendRequest newreq = friendRequestDao.selectById(requestId);
                    MessageSseServlet.sendMessageToUser(newreq.getFromUserId(), JSON.toJSONString(msg));
                }
                UserBehaviorLogger.logAsync(fromUserId, 18, null, null, "同意好友请求");
            } else if ("reject".equals(action)) {
                // 拒绝好友申请
                Integer requestId = Integer.parseInt(requestIdStr);
                ResultDTO<Void> result = friendRequestService.rejectRequest(requestId);
                out.write(JSON.toJSONString(result));
                // ========== 新增：推送通知给 申请人 ==========
                if (result.isSuccess()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "FRIEND_REQUEST");
                    FriendRequest newreq = friendRequestDao.selectById(requestId);
                    MessageSseServlet.sendMessageToUser(newreq.getFromUserId(), JSON.toJSONString(msg));
                }
                UserBehaviorLogger.logAsync(fromUserId, 19, null, null, "拒绝好友请求");
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("ID格式错误")));
        } catch (Exception e) {
            logger.error("操作好友申请异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }


}