package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserCommentDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserComment;
import com.example.service.UserCommentService;
import com.example.service.impl.UserCommentServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@WebServlet("/api/user/comment")
public class UserCommentServlet extends BaseServlet {
    private UserCommentDaoImpl userCommentDao;
    private UserCommentService userCommentService;
    private static final Logger logger = LoggerFactory.getLogger(UserCommentServlet.class);

    @Override
    public void init() throws ServletException {
        userCommentDao = new UserCommentDaoImpl();
        userCommentService = new UserCommentServiceImpl(userCommentDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String commentId = req.getParameter("commentId");
        String userId = req.getParameter("userId");
        String sortType = req.getParameter("sort");
        String action = req.getParameter("action");

        // 新增：分页参数
        String pageNumStr = req.getParameter("pageNum");
        String pageSizeStr = req.getParameter("pageSize");
        String parentIdStr = req.getParameter("parentId");

        // 分页参数默认值
        Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
        Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 5;

        try {
            // ====================== 新增：分页查询主评论 ======================
            if ("mainPage".equals(action)) {
                ResultDTO<PageResultDTO<Map<String, Object>>> result = userCommentService.getMainCommentsByPage(pageNum, pageSize, sortType);
                out.write(JSON.toJSONString(result));
                return;
            }

            // ====================== 新增：分页查询子评论 ======================
            if ("subPage".equals(action) && parentIdStr != null && !parentIdStr.isEmpty()) {
                Integer parentId = Integer.parseInt(parentIdStr);
                // 子评论默认每页3条，和前端需求一致
                if (pageSizeStr == null || pageSizeStr.isEmpty()) {
                    pageSize = 3;
                }
                ResultDTO<PageResultDTO<UserComment>> result = userCommentService.getSubCommentsByPage(parentId, pageNum, pageSize, sortType);
                out.write(JSON.toJSONString(result));
                return;
            }

            // ====================== 新增：获取子评论总数 ======================
            if ("subCount".equals(action) && parentIdStr != null && !parentIdStr.isEmpty()) {
                Integer parentId = Integer.parseInt(parentIdStr);
                ResultDTO<Long> result = userCommentService.getSubCommentCount(parentId);
                out.write(JSON.toJSONString(result));
                return;
            }

            // 原有逻辑保持不变
            if ("tree".equals(action)) {
                ResultDTO<Map<String, List<UserComment>>> result = userCommentService.getCommentTree(sortType);
                out.write(JSON.toJSONString(result));
                return;
            }

            ResultDTO<List<UserComment>> result;
            if (commentId != null && !commentId.isEmpty()) {
                result = userCommentService.queryByCommentId(Integer.valueOf(commentId));
            } else if (userId != null && !userId.isEmpty()) {
                // 🔥 优先走分页
                if (pageNumStr != null && !pageNumStr.isEmpty()) {
                    ResultDTO<PageResultDTO<UserComment>> newresult = userCommentService.queryByUserIdPage(userId, pageNum, pageSize);
                    out.write(JSON.toJSONString(newresult));
                    return;
                } else {
                    // 兼容旧接口
                    result = userCommentService.queryUserCommentByUserId(userId);
                }
            } else if (sortType != null && !sortType.isEmpty()) {
                result = userCommentService.queryUserCommentByType(sortType);
            } else {
                result = userCommentService.queryAllUserComments();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("参数格式错误，请检查数字类型参数")));
        } catch (Exception e) {
            logger.error("查询论坛评论异常", e);
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
        UserComment comment = JSON.parseObject(req.getInputStream(), UserComment.class);

        try {
            ResultDTO<Integer> result = userCommentService.addUserComment(comment);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增论坛评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
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

        try {
            String commentIdStr = req.getParameter("commentId");
            // 非空校验（和删除书籍的ISBN校验逻辑一致）
            if (commentIdStr == null || commentIdStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("评论ID不能为空")));
                out.flush();
                out.close();
                return;
            }
            int commentId;
            try {
                commentId = Integer.parseInt(commentIdStr);
            } catch (NumberFormatException e) {
                out.write(JSON.toJSONString(ResultDTO.paramError("评论ID必须是数字")));
                out.flush();
                out.close();
                return;
            }
            ResultDTO<Void> result = userCommentService.deleteUserComment(commentId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除论坛评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        UserComment comment = JSON.parseObject(req.getInputStream(), UserComment.class);

        try {
            ResultDTO<Void> result = userCommentService.updateUserComment(comment);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新论坛评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}
