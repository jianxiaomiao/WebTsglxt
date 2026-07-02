package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookChapterDaoImpl;
import com.example.dao.impl.BookChapterParagraphDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookChapter;
import com.example.service.BookChapterService;
import com.example.service.impl.BookChapterServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@WebServlet("/api/book/chapter")
public class BookChapterServlet extends BaseServlet {
    private BookChapterDaoImpl bookChapterDao;
    private BookChapterParagraphDaoImpl paragraphDao;
    private BookChapterService bookChapterService;
    private static final Logger logger = LoggerFactory.getLogger(BookChapterServlet.class);

    @Override
    public void init() throws ServletException {
        bookChapterDao = new BookChapterDaoImpl();
        paragraphDao = new BookChapterParagraphDaoImpl();
        // 注入两个DAO
        bookChapterService = new BookChapterServiceImpl(bookChapterDao, paragraphDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String chapterId = req.getParameter("chapter_id");
        String isbn = req.getParameter("isbn");

        try {
            ResultDTO<?> result;
            if (chapterId != null && !chapterId.isEmpty()) {
                // 单章节查询：携带段落列表
                result = bookChapterService.queryBookChapterId(chapterId);
            } else if (isbn != null && !isbn.isEmpty()) {
                result = bookChapterService.queryBookChapterIsbn(isbn);
            } else {
                result = bookChapterService.queryAllBookChapters();
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("查询书籍章节异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 解析请求体Map，分离章节基础信息与完整文本
        Map<String, Object> reqMap = JSON.parseObject(req.getInputStream(), Map.class);
        String fullText = (String) reqMap.get("fullText");
        // 转成章节实体
        BookChapter chapter = JSON.parseObject(JSON.toJSONString(reqMap), BookChapter.class);

        try {
            ResultDTO<Void> result = bookChapterService.addBookChapter(chapter, fullText);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增书籍章节异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String chapterId = req.getHeader("chapter_id");

        try {
            if (chapterId == null || chapterId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("请求头chapter_id不能为空")));
                return;
            }
            ResultDTO<Void> result = bookChapterService.deleteBookChapter(chapterId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除书籍章节异常", e);
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

        BookChapter chapter = JSON.parseObject(req.getInputStream(), BookChapter.class);

        try {
            ResultDTO<Void> result = bookChapterService.updateBookChapter(chapter);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新书籍章节异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}