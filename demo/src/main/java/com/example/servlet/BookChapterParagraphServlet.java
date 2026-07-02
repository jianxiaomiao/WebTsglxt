package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookChapterParagraphDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookChapterParagraph;
import com.example.service.BookChapterParagraphService;
import com.example.service.impl.BookChapterParagraphServiceImpl;
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

@WebServlet("/api/book/paragraph")
public class BookChapterParagraphServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookChapterParagraphServlet.class);
    private BookChapterParagraphDaoImpl paragraphDao;
    private BookChapterParagraphService paragraphService;

    @Override
    public void init() throws ServletException {
        paragraphDao = new BookChapterParagraphDaoImpl();
        paragraphService = new BookChapterParagraphServiceImpl(paragraphDao);
    }

    // GET查询：id / chapterId
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;
        try {
            String paragraphId = req.getParameter("id");
            String chapterId = req.getParameter("chapterId");

            if (paragraphId != null && !paragraphId.isEmpty()) {
                result = paragraphService.queryParagraphById(paragraphId);
                logger.info("查询段落，段落ID:{}", paragraphId);
            } else if (chapterId != null && !chapterId.isEmpty()) {
                result = paragraphService.queryParagraphByChapterId(chapterId);
                logger.info("查询章节全部段落，章节ID:{}", chapterId);
            } else {
                result = ResultDTO.paramError("请传入id或chapterId参数");
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("段落查询接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // POST 新增段落
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            BookChapterParagraph para = JSON.parseObject(req.getInputStream(), BookChapterParagraph.class);
            ResultDTO<Void> result = paragraphService.addParagraph(para);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增段落接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // PUT 更新段落
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            BookChapterParagraph para = JSON.parseObject(req.getInputStream(), BookChapterParagraph.class);
            ResultDTO<Void> result = paragraphService.updateParagraph(para);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新段落接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // DELETE 删除段落
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String paraId = req.getParameter("id");
            if (paraId == null || paraId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("段落id不能为空")));
                return;
            }
            ResultDTO<Void> result = paragraphService.deleteParagraph(paraId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除段落接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}