package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookSquarePostDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookSquarePost;
import com.example.service.BookSquarePostService;
import com.example.service.impl.BookSquarePostServiceImpl;
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

/**
 * 书荒广场帖子 Servlet
 * 接口地址：/api/book/square/post
 */
@WebServlet("/api/book/square/post")
public class BookSquarePostServlet extends BaseServlet {

    private static final Logger logger = LoggerFactory.getLogger(BookSquarePostServlet.class);
    private BookSquarePostDaoImpl postDao;
    private BookSquarePostService postService;

    @Override
    public void init() throws ServletException {
        // 初始化DAO、Service
        postDao = new BookSquarePostDaoImpl();
        postService = new BookSquarePostServiceImpl(postDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;

        try {
            // 接收请求参数
            String idStr = req.getParameter("id");
            String userId = req.getParameter("userId");
            String parentIdStr = req.getParameter("parentId");
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");

            // 分页默认值
            Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

            // 优先级：单条查询 > 子帖查询 > 用户帖子查询 > 主帖查询 > 全部帖子查询
            if (idStr != null && !idStr.isEmpty()) {
                // 1. 根据ID查询单条帖子
                Integer postId = Integer.parseInt(idStr);
                result = postService.getPostById(postId);
                logger.info("查询帖子详情，ID:{}", postId);
            } else if (parentIdStr != null && !parentIdStr.isEmpty()) {
                // 2. 根据主帖ID查询子帖
                Integer parentId = Integer.parseInt(parentIdStr);
                result = postService.querySubPostByParentIdPage(parentId, pageNum, pageSize);
                logger.info("分页查询子帖，主帖ID:{}，第{}页", parentId, pageNum);
            } else if (userId != null && !userId.isEmpty()) {
                // 3. 根据用户ID查询帖子
                result = postService.queryByUserIdPage(userId, pageNum, pageSize);
                logger.info("分页查询用户帖子，userId:{}，第{}页", userId, pageNum);
            } else {
                // 4. 无参数：查询所有主帖（parent_id=0）
                result = postService.queryMainPostPage(pageNum, pageSize);
                logger.info("分页查询所有主帖，第{}页", pageNum);
            }

            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("数字类型参数格式错误")));
        } catch (Exception e) {
            logger.error("帖子查询接口异常", e);
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

        try {
            // 解析JSON请求体
            BookSquarePost post = JSON.parseObject(req.getInputStream(), BookSquarePost.class);
            ResultDTO<Void> result = postService.addPost(post);
            out.write(JSON.toJSONString(result));
            logger.info("新增帖子请求处理完成");
            UserBehaviorLogger.logAsync(post.getUserId(), 12, null, null, post.getContent());
        } catch (Exception e) {
            logger.error("新增帖子接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
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

        try {
            BookSquarePost post = JSON.parseObject(req.getInputStream(), BookSquarePost.class);
            ResultDTO<Void> result = postService.updatePost(post);
            out.write(JSON.toJSONString(result));
            logger.info("更新帖子请求处理完成，ID:{}", post.getId());
        } catch (Exception e) {
            logger.error("更新帖子接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * doDelete：逻辑删除（更新status=0，不物理删除）
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String idStr = req.getParameter("id");
            // 参数非空校验
            if (idStr == null || idStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("帖子ID不能为空")));
                out.flush();
                out.close();
                return;
            }
            Integer postId;
            try {
                postId = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                out.write(JSON.toJSONString(ResultDTO.paramError("帖子ID必须为数字")));
                out.flush();
                out.close();
                return;
            }

            // 执行逻辑删除
            ResultDTO<Void> result = postService.logicDeletePost(postId);
            out.write(JSON.toJSONString(result));
            logger.info("逻辑删除帖子完成，ID:{}", postId);
        } catch (Exception e) {
            logger.error("逻辑删除帖子接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}