package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.UserCommentDao;
import com.example.dao.UserCommentLikeDao;
import com.example.dao.impl.UserCommentDaoImpl;
import com.example.dao.impl.UserCommentLikeDaoImpl;
import com.example.dto.ResultDTO;
import com.example.service.UserCommentLikeService;
import com.example.service.impl.UserCommentLikeServiceImpl;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/api/user/comment/like")
public class UserCommentLikeServlet extends BaseServlet {
    private UserCommentLikeService likeService;

    @Override
    public void init() {
        UserCommentLikeDao likeDao = new UserCommentLikeDaoImpl();
        UserCommentDao commentDao = new UserCommentDaoImpl();
        likeService = new UserCommentLikeServiceImpl(likeDao, commentDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=utf-8");
        try (PrintWriter out = resp.getWriter()) {
            String userId = req.getParameter("userId");
            if (userId == null || userId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                return;
            }
            // 调用service获取已点赞评论ID列表
            ResultDTO<List<Integer>> result = likeService.getLikedCommentIds(userId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=utf-8");
        try (PrintWriter out = resp.getWriter()) {
            String userId = req.getParameter("userId");
            String commentIdStr = req.getParameter("commentId");

            if (userId == null || commentIdStr == null) {
                out.write(JSON.toJSONString(ResultDTO.paramError("参数不能为空")));
                return;
            }

            Integer commentId = Integer.parseInt(commentIdStr);
            ResultDTO<Void> result = likeService.toggleLike(userId, commentId, LocalDateTime.now());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(userId, 30, null, null, "点赞/取消点赞评论" + commentIdStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}