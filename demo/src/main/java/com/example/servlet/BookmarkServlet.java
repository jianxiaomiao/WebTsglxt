package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookmarkDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.Bookmark;
import com.example.service.BookmarkService;
import com.example.service.impl.BookmarkServiceImpl;
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

@WebServlet("/api/bookmark")
public class BookmarkServlet extends BaseServlet {
    private BookmarkDaoImpl bookmarkDao;
    private BookmarkService bookmarkService;
    private static final Logger logger = LoggerFactory.getLogger(BookmarkServlet.class);

    @Override
    public void init() throws ServletException {
        bookmarkDao = new BookmarkDaoImpl();
        bookmarkService = new BookmarkServiceImpl(bookmarkDao);
    }

    // 查询：GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String userId = req.getParameter("userId");
        String isbn = req.getParameter("isbn");
        String chapterNumber = req.getParameter("chapterNumber");

        try {
            ResultDTO<?> result;
            // 优先级：用户ID+ISBN > 用户ID > ISBN > 章节号
            if (userId != null && isbn != null) {
                result = bookmarkService.queryByUserIdAndIsbn(userId, isbn);
            } else if (userId != null) {
                result = bookmarkService.queryByUserId(userId);
            } else if (isbn != null) {
                result = bookmarkService.queryByIsbn(isbn);
            } else if (chapterNumber != null) {
                result = bookmarkService.queryByChapterNumber(chapterNumber);
            } else {
                result = ResultDTO.paramError("请传入查询参数");
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("查询书签异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 新增：POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        Bookmark bookmark = JSON.parseObject(req.getInputStream(), Bookmark.class);

        try {
            ResultDTO<Void> result = bookmarkService.addBookmark(bookmark);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增书签异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 修改：PUT
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        Bookmark bookmark = JSON.parseObject(req.getInputStream(), Bookmark.class);


        try {
            ResultDTO<Void> result;
            Long bookmarkId = bookmark.getId();
            // 判断：如果id存在且有效 → 走主键更新；否则走原有的联合条件更新
            if (bookmarkId != null && bookmarkId > 0) {
                result = bookmarkService.updateBookmarkById(bookmark);
            } else {
                result = bookmarkService.updateBookmark(bookmark);
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新书签异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 删除：DELETE
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String userId = req.getParameter("userId");
        String isbn = req.getParameter("isbn");
        String chapterNumber = req.getParameter("chapterNumber");
        String id = req.getParameter("id");

        try {
            ResultDTO<Void> result;
            if (id != null && !id.isEmpty()) {
                result = bookmarkService.deleteBookmarkById(Long.parseLong(id));
            } else {
                result = bookmarkService.deleteBookmark(userId, isbn, chapterNumber);
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除书签异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}