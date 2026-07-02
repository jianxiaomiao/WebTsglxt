package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookChapterParagraphDaoImpl;
import com.example.dao.impl.ParagraphCommentDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.ParagraphComment;
import com.example.service.ParagraphCommentService;
import com.example.service.impl.ParagraphCommentServiceImpl;
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

@WebServlet("/api/book/paragraph/comment")
public class ParagraphCommentServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(ParagraphCommentServlet.class);
    private ParagraphCommentDaoImpl commentDao;
    private BookChapterParagraphDaoImpl paragraphDao;
    private ParagraphCommentService commentService;

    @Override
    public void init() throws ServletException {
        commentDao = new ParagraphCommentDaoImpl();
        paragraphDao = new BookChapterParagraphDaoImpl();
        commentService = new ParagraphCommentServiceImpl(commentDao, paragraphDao);
    }

    // GET查询：单条评论id / 用户分页 / 段落分页
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;
        try {
            String commentIdStr = req.getParameter("commentId");
            String userId = req.getParameter("userId");
            String paragraphId = req.getParameter("paragraphId");
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");

            Integer pageNum = pageNumStr == null ? null : Integer.parseInt(pageNumStr);
            Integer pageSize = pageSizeStr == null ? null : Integer.parseInt(pageSizeStr);

            // 1. 根据评论ID单条查询（不分页）
            if (commentIdStr != null && !commentIdStr.isEmpty()) {
                Long cid = Long.parseLong(commentIdStr);
                result = commentService.queryCommentById(cid);
                logger.info("查询单条评论，commentId:{}", cid);
            }
            // 2. 用户ID分页查询
            else if (userId != null && !userId.isEmpty()) {
                result = commentService.queryCommentByUserIdPage(userId, pageNum, pageSize);
                logger.info("分页查询用户评论，userId:{}", userId);
            }
            // 3. 段落ID分页查询
            else if (paragraphId != null && !paragraphId.isEmpty()) {
                result = commentService.queryCommentByParagraphIdPage(paragraphId, pageNum, pageSize);
                logger.info("分页查询段落评论，paragraphId:{}", paragraphId);
            }
            else {
                result = ResultDTO.paramError("参数错误：传入commentId/userId/paragraphId其中一个");
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("分页数字参数格式错误")));
        } catch (Exception e) {
            logger.error("段落评论查询接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // POST 新增评论（自动更新段落comment_count）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            ParagraphComment comment = JSON.parseObject(req.getInputStream(), ParagraphComment.class);
            // 前端不传创建时间，后端自动填充当前时间
            comment.setCreateTime(java.time.LocalDateTime.now());
            ResultDTO<Void> result = commentService.addComment(comment);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增段落评论接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增评论失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // PUT 更新评论
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            ParagraphComment comment = JSON.parseObject(req.getInputStream(), ParagraphComment.class);
            ResultDTO<Void> result = commentService.updateComment(comment);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新段落评论接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新评论失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // DELETE 删除评论（自动减少段落comment_count）
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String commentIdStr = req.getParameter("commentId");
            if (commentIdStr == null || commentIdStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("评论ID不能为空")));
                return;
            }
            Long cid = Long.parseLong(commentIdStr);
            ResultDTO<Void> result = commentService.deleteComment(cid);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("评论ID必须为数字")));
        } catch (Exception e) {
            logger.error("删除段落评论接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除评论失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}