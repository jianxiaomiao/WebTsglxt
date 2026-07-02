package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookCommentDaoImpl;
import com.example.dao.impl.BookInformationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookComment;
import com.example.entity.BookInformation;
import com.example.service.BookCommentService;
import com.example.service.BookService;
import com.example.service.impl.BookCommentServiceImpl;
import com.example.service.impl.BookServiceImpl;
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
import java.util.List;

@WebServlet("/api/book/comment")
public class BookCommentServlet extends BaseServlet {
    private BookCommentDaoImpl bookCommentDao;
    private BookCommentService bookCommentService;
    // ✅ 新增：注入书籍相关的Dao和Service
    private BookInformationDaoImpl bookDao;
    private BookService bookService;
    private static final Logger logger = LoggerFactory.getLogger(BookCommentServlet.class);

    @Override
    public void init() throws ServletException {
        // 初始化已有的BookCommentDaoImpl
        bookCommentDao = new BookCommentDaoImpl();
        bookCommentService = new BookCommentServiceImpl(bookCommentDao);
        // ✅ 新增：初始化书籍相关的Dao和Service
        bookDao = new BookInformationDaoImpl();
        bookService = new BookServiceImpl(bookDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String userId = req.getParameter("userId");
            String isbn = req.getParameter("isbn");
            String commentId = req.getParameter("commentId"); // 🔥 新增：获取commentId参数
            // 新增：分页参数
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");

            Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

            ResultDTO<?> result;
            // 🔥 新增：优先处理按commentId查询
            if (commentId != null && !commentId.isEmpty()) {
                result = bookCommentService.queryByCommentId(Integer.valueOf(commentId));
                logger.info("按ID[{}]查询书籍评论完成", commentId);
            }
            // 🔥 优先处理分页查询
            else if (userId != null && !userId.isEmpty() && pageNumStr != null) {
                result = bookCommentService.queryByUserIdPage(userId, pageNum, pageSize);
                logger.info("分页查询用户[{}]书籍评论完成，第{}页", userId, pageNum);
            }
            // 保留原有全量查询逻辑（兼容其他地方调用）
            else if (userId != null && !userId.isEmpty()) {
                result = bookCommentService.queryBookCommentByUserId(userId);
                logger.info("查询用户[{}]全部书籍评论完成", userId);
            }
            else if (isbn != null && !isbn.isEmpty()) {
                result = bookCommentService.queryBookCommentByBookId(isbn);
                logger.info("按ISBN[{}]查询书籍评论完成", isbn);
            }
            else {
                result = bookCommentService.queryAllBookComments();
                logger.info("查询全部书籍评论完成");
            }

            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("参数格式错误，请检查数字类型参数")));
        } catch (Exception e) {
            logger.error("书籍评论查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询书籍评论失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理新增请求（POST）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        // 解析请求体为BookComment对象
        BookComment comment = JSON.parseObject(req.getInputStream(), BookComment.class);

        try {

            ResultDTO<Void> result = bookCommentService.addBookComment(comment);
            // ✅ 新增：发表评论成功后，更新书籍评分
            if (result.getCode() == 200) {
                updateBookRating(comment.getISBN());
            out.write(JSON.toJSONString(result));
            }
            UserBehaviorLogger.logAsync(comment.getUserid(), 7, comment.getISBN(), null, comment.getComment());
        } catch (Exception e) {
            logger.error("新增书籍评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理删除请求（DELETE）
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
            // ✅ 正确做法：先根据commentId查询评论，获取ISBN
            List<BookComment> commentList = bookCommentDao.queryByCommentId(commentId);
            if (commentList == null || commentList.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.fail("未找到该评论")));
                out.flush();
                out.close();
                return;
            }
            BookComment comment = commentList.get(0);
            String isbn = comment.getISBN();

            // 执行删除操作
            ResultDTO<Void> result = bookCommentService.deleteBookComment(commentId);
            logger.info("删除评论[commentId:{}]请求处理完成，对应书籍ISBN:{}", commentId, isbn);

            // 删除成功后，更新书籍评分
            if (result.getCode() == 200 && isbn != null) {
                updateBookRating(isbn);
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除书籍评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理更新请求（PUT）
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        BookComment comment = JSON.parseObject(req.getInputStream(), BookComment.class);

        try {
            ResultDTO<Void> result = bookCommentService.updateBookComment(comment);
            if (result.getCode() == 200) {
                updateBookRating(comment.getISBN());
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新书籍评论异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * 通用方法：根据ISBN重新计算并更新书籍的平均评分
     * @param isbn 书籍ISBN
     */
    // 在 BookCommentServlet 的 updateBookRating 方法中修改
    private void updateBookRating(String isbn) {
        try {
            // 1. 查询该书籍的所有评论
            List<BookComment> comments = bookCommentDao.queryByBookId(isbn);

            // 2. 计算平均评分和评分人数
            float averageStar = 0.0f;
            int commentCount = comments.size();
            if (commentCount > 0) {
                int totalStar = 0;
                for (BookComment comment : comments) {
                    // 没有星级的评论默认按3星计算（和前端逻辑保持一致）
                    totalStar += (comment.getStar() == null || comment.getStar() < 1 || comment.getStar() > 5)
                            ? 3 : comment.getStar();
                }
                // 四舍五入保留1位小数
                averageStar = Math.round((totalStar * 1.0f / commentCount) * 10) / 10.0f;
            }

            // 3. 更新书籍表的star和ratingCount字段
            List<BookInformation> bookList = bookDao.queryByISBN(isbn);
            if (bookList != null && !bookList.isEmpty()) {
                BookInformation book = bookList.get(0);
                book.setStar(averageStar);
                book.setRatingCount(commentCount); // ✅ 新增：更新评分人数
                bookService.updateBookStock(book);
                logger.info("书籍[ISBN:{}]评分更新成功，新评分：{}，评分人数：{}", isbn, averageStar, commentCount);
            }
        } catch (Exception e) {
            // 重要：即使更新评分失败，也不能影响评论的增删改操作
            logger.error("更新书籍[ISBN:{}]评分失败", isbn, e);
        }
    }
}
