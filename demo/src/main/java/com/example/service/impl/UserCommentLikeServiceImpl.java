package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.dao.UserCommentDao;
import com.example.dao.UserCommentLikeDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserComment;
import com.example.entity.UserCommentLike;
import com.example.service.UserCommentLikeService;
import com.example.service.UserNotificationService;
import com.example.service.impl.UserNotificationServiceImpl;
import com.example.servlet.MessageSseServlet;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserCommentLikeServiceImpl implements UserCommentLikeService {
    private final UserCommentLikeDao likeDao;
    private final UserCommentDao commentDao;
    // 新增：通知服务
    private final UserNotificationService notificationService;

    public UserCommentLikeServiceImpl(UserCommentLikeDao likeDao, UserCommentDao commentDao) {
        this.likeDao = likeDao;
        this.commentDao = commentDao;
        this.notificationService = new UserNotificationServiceImpl(); // 初始化通知服务
    }

    @Override
    public ResultDTO<List<Integer>> getLikedCommentIds(String userId) {
        try {
            List<Integer> list = likeDao.selectLikedCommentIdsByUserId(userId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("查询点赞记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> toggleLike(String userId, Integer commentId, LocalDateTime createTime) {
        try {
            // 1. 判断是否已点赞
            boolean liked = likeDao.isLiked(userId, commentId);

            if (liked) {
                // 已点赞 → 取消
                likeDao.deleteLike(userId, commentId);
                commentDao.updatePrefer(commentId, -1);
                return ResultDTO.success(null);
            } else {
                // 未点赞 → 新增
                likeDao.addLike(new UserCommentLike(userId, commentId, createTime));
                commentDao.updatePrefer(commentId, 1);

                // 🔥 核心新增：点赞后生成通知
                // 查询被点赞评论的作者
                List<UserComment> commentList = commentDao.queryByCommentId(commentId);
                if (!commentList.isEmpty()) {
                    UserComment comment = commentList.get(0);
                    String toUserId = comment.getUserid();
                    // 自己点赞自己不推送
                    if (!toUserId.equals(userId)) {
                        notificationService.addNotification(toUserId, 2, userId, commentId, 0);

                        // ====================== 🔥 关键新增：SSE 实时推送 ======================
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("type", "INTERACTION"); // 前端监听的类型
                        msg.put("msg", "你收到一条点赞通知");
                        MessageSseServlet.sendMessageToUser(toUserId, JSON.toJSONString(msg));
                    }
                }

                return ResultDTO.success(null);
            }
        } catch (Exception e) {
            return ResultDTO.fail("操作失败：" + e.getMessage());
        }
    }
}