package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.dao.UserCommentDao;
import com.example.dao.UserNotificationDao;
import com.example.dao.impl.UserNotificationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.dto.PageResultDTO;
import com.example.entity.UserComment;
import com.example.entity.UserNotification;
import com.example.service.UserCommentService;
import com.example.servlet.BookChapterServlet;
import com.example.servlet.MessageSseServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class UserCommentServiceImpl implements UserCommentService {
    private  UserCommentDao userCommentDao;
    private static final Logger logger = LoggerFactory.getLogger(UserCommentServiceImpl.class);
    // 注入通知DAO
    private final UserNotificationDao notificationDao = new UserNotificationDaoImpl();
    public UserCommentServiceImpl(UserCommentDao userCommentDao) {
        this.userCommentDao = userCommentDao;
    }

    @Override
    public ResultDTO<List<UserComment>> queryAllUserComments() {
        try {
            List<UserComment> comments = userCommentDao.queryAll();
            return ResultDTO.success(comments);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有论坛评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserComment>> queryUserCommentByUserId(String userId) {
        try {
            List<UserComment> comments = userCommentDao.queryByUserId(userId);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按用户ID查询论坛评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<UserComment>> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        try {
            List<UserComment> list = userCommentDao.queryByUserIdPage(userId, pageNum, pageSize);
            Long total = userCommentDao.countByUserId(userId);
            PageResultDTO<UserComment> result = PageResultDTO.success(total, pageNum, pageSize, list);
            return ResultDTO.success(result);
        } catch (Exception e) {
            logger.error("分页查询用户论坛评论失败", e);
            return ResultDTO.fail("分页查询失败");
        }
    }

    @Override
    public ResultDTO<List<UserComment>> queryUserCommentByType(String sortType) {
        try {
            List<UserComment> comments = userCommentDao.queryByType(sortType);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ISBN查询评论失败：" + e.getMessage());
        }
    }
    @Override
    public ResultDTO<List<UserComment>> queryByCommentId(Integer commentId) {
        try {
            List<UserComment> comments = userCommentDao.queryByCommentId(commentId);
            return ResultDTO.success(comments);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据评论ID查询失败：" + e.getMessage());
        }
    }
    @Override
    public ResultDTO<Integer> addUserComment(UserComment comment) {
        try {
            int comment_id = userCommentDao.add(comment);
            // 🔥 核心新增：如果是回复评论（parentId != 0），生成「评论被回复」通知
            if (comment.getParentId() != null && comment.getParentId() != 0) {
                // 1. 查询被回复的那条评论
                List<UserComment> parentList = userCommentDao.queryByCommentId(comment.getParentId());
                if (parentList.isEmpty()) {
                    return ResultDTO.success(comment_id);
                }
                UserComment parentComment = parentList.get(0);

                // 2. 接收通知的用户（被回复的人）
                String toUserId = parentComment.getUserid();
                // 如果回复的是自己的评论，不生成通知
                if (!toUserId.equals(comment.getUserid())) {
                    // 3. 插入系统通知
                    UserNotification notification = new UserNotification();
                    notification.setUserId(toUserId);
                    notification.setType(1); // 1=评论被回复
                    notification.setFromUserId(comment.getUserid());
                    notification.setCommentId(parentComment.getCommentId());
                    notification.setReplyCommentId(comment_id); // 回复的这条评论的ID
                    notification.setCreateTime(LocalDateTime.now());
                    notificationDao.add(notification);
                    // ====================== 🔥 关键新增：SSE 实时推送 ======================
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("type", "INTERACTION"); // 前端监听的类型
                    msg.put("msg", "你收到一条评论回复通知");
                    MessageSseServlet.sendMessageToUser(toUserId, JSON.toJSONString(msg));

                }
            }
            return ResultDTO.success(comment_id);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增论坛评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteUserComment(int commentId) {
        try {
            userCommentDao.del(commentId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除论坛评论失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUserComment(UserComment comment) {
        try {
            userCommentDao.update(comment);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新论坛评论失败：" + e.getMessage());
        }
    }

    // 🔥 【新增】核心：把评论列表组装成「主评论ID -> 子评论列表」的Map
    @Override
    public ResultDTO<Map<String, List<UserComment>>> getCommentTree(String sortType) {
        try {
            List<UserComment> allComments = userCommentDao.queryAll();

            // 1. 按parentId分组：key=parentId, value=该parentId下的子评论列表
            Map<String, List<UserComment>> commentMap = allComments.stream()
                    .collect(Collectors.groupingBy(
                            comment -> String.valueOf(comment.getParentId() == null ? 0 : comment.getParentId()),
                            Collectors.toList()
                    ));
            // 🔥 核心修复：对【主评论】进行排序（time / prefer）
            List<UserComment> mainComments = commentMap.getOrDefault("0", new ArrayList<>());
            if ("prefer".equals(sortType)) {
                // 按点赞数降序
                mainComments.sort(Comparator.comparingInt(UserComment::getPrefer).reversed());
            } else {
                // 默认 / time：按时间降序（最新在前）
                mainComments.sort(Comparator.comparing(UserComment::getCommentTime).reversed());
            }
            // 重新放回map
            commentMap.put("0", mainComments);

            // 🔥 关键修复：子评论也按sortType排序
            for (Map.Entry<String, List<UserComment>> entry : commentMap.entrySet()) {
                if (!"0".equals(entry.getKey())) {
                    List<UserComment> subComments = entry.getValue();
                    if ("prefer".equals(sortType)) {
                        // 按点赞数降序
                        subComments.sort(Comparator.comparingInt(UserComment::getPrefer).reversed());
                    } else {
                        // 按时间降序
                        subComments.sort(Comparator.comparing(UserComment::getCommentTime).reversed());
                    }
                    entry.setValue(subComments);
                }
            }

            return ResultDTO.success(commentMap);
        } catch (RuntimeException e) {
            return ResultDTO.fail("获取评论树失败：" + e.getMessage());
        }
    }

    // ====================== 新增：分页获取主评论列表（带子评论总数） ======================
    @Override
    public ResultDTO<PageResultDTO<Map<String, Object>>> getMainCommentsByPage(Integer pageNum, Integer pageSize, String sortType) {
        try {
            // 1. 分页查询主评论
            List<UserComment> mainComments = userCommentDao.queryMainCommentsByPage(pageNum, pageSize, sortType);
            // 2. 查询主评论总条数
            Long total = userCommentDao.countMainComments();

            // 3. 批量统计所有主评论的子评论总数（性能优化，一次查询搞定）
            List<Integer> mainCommentIds = mainComments.stream()
                    .map(UserComment::getCommentId)
                    .collect(Collectors.toList());
            Map<Integer, Long> subCountMap = userCommentDao.batchCountSubComments(mainCommentIds);

            // 4. 组装返回数据（主评论 + 子评论总数）
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (UserComment comment : mainComments) {
                Map<String, Object> item = new HashMap<>();
                // 主评论本身
                item.put("comment", comment);
                // 该主评论下的子评论总数
                item.put("subTotal", subCountMap.getOrDefault(comment.getCommentId(), 0L));
                resultList.add(item);
            }

            // 5. 构建分页结果
            PageResultDTO<Map<String, Object>> pageResult = PageResultDTO.success(total, pageNum, pageSize, resultList);
            return ResultDTO.success(pageResult);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("分页获取主评论失败", e);
            return ResultDTO.fail("分页获取主评论失败：" + e.getMessage());
        }
    }

    // ====================== 新增：分页获取指定主评论下的子评论 ======================
    @Override
    public ResultDTO<PageResultDTO<UserComment>> getSubCommentsByPage(Integer parentId, Integer pageNum, Integer pageSize, String sortType) {
        try {
            // 1. 分页查询子评论
            List<UserComment> subComments = userCommentDao.querySubCommentsByPage(parentId, pageNum, pageSize, sortType);
            // 2. 查询子评论总条数
            Long total = userCommentDao.countSubCommentsByParentId(parentId);

            // 3. 构建分页结果
            PageResultDTO<UserComment> pageResult = PageResultDTO.success(total, pageNum, pageSize, subComments);
            return ResultDTO.success(pageResult);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("分页获取子评论失败，父评论ID：{}", parentId, e);
            return ResultDTO.fail("分页获取子评论失败：" + e.getMessage());
        }
    }

    // ====================== 新增：获取单个主评论的子评论总数 ======================
    @Override
    public ResultDTO<Long> getSubCommentCount(Integer parentId) {
        try {
            Long count = userCommentDao.countSubCommentsByParentId(parentId);
            return ResultDTO.success(count);
        } catch (RuntimeException e) {
            logger.error("获取子评论总数失败，父评论ID：{}", parentId, e);
            return ResultDTO.fail("获取子评论总数失败：" + e.getMessage());
        }
    }
}