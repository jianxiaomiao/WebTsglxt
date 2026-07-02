package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookTypeDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookType;
import com.example.service.BookTypeService;
import com.example.service.impl.BookTypeServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/book/type")
public class BookTypeServlet extends BaseServlet {
    private BookTypeDaoImpl bookTypeDao;
    private BookTypeService bookTypeService;
    private static final Logger logger = LoggerFactory.getLogger(BookTypeServlet.class);

    @Override
    public void init() throws ServletException {
        bookTypeDao = new BookTypeDaoImpl();
        bookTypeService = new BookTypeServiceImpl(bookTypeDao);
        logger.info("BookTypeServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            ResultDTO<List<BookType>> result;
            if (id != null && !id.isEmpty()) {
                result = bookTypeService.queryBookTypeById(Integer.parseInt(id));
            } else {
                result = bookTypeService.queryAllBookTypes();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("书籍种类查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询书籍种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            BookType bookType = JSON.parseObject(req.getInputStream(), BookType.class);
            ResultDTO<Void> result = bookTypeService.addBookType(bookType);
            logger.info("新增书籍种类[{}]请求处理完成", bookType.getBookType());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增书籍种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增书籍种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            BookType bookType = JSON.parseObject(req.getInputStream(), BookType.class);
            ResultDTO<Void> result = bookTypeService.updateBookType(bookType);
            logger.info("更新书籍种类[Id:{}]请求处理完成", bookType.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新书籍种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新书籍种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            if (id == null || id.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("书籍种类ID不能为空")));
                return;
            }
            ResultDTO<Void> result = bookTypeService.deleteBookType(Integer.parseInt(id));
            logger.info("删除书籍种类[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除书籍种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除书籍种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}