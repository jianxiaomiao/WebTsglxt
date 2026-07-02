package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookInformationDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.service.BookService;
import com.example.service.impl.BookServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * 图书模块CRUD Servlet
 * 接口路径：/api/book
 * 支持方法：GET(查询)、POST(新增)、DELETE(删除)、PUT(更新)
 * 与用户/借阅/评论模块保持完全一致的设计规范
 */
@WebServlet("/api/book")
public class BookServlet extends BaseServlet  {
    // 1. 成员变量：DAO+Service（与其他模块命名/初始化逻辑一致）
    private BookInformationDaoImpl bookDao;
    private BookService bookService;
    private static final Logger logger = LoggerFactory.getLogger(BookServlet.class);

    /**
     * 初始化方法：仅加载一次，注入DAO和Service（与其他模块一致）
     */
    @Override
    public void init() throws ServletException {
        bookDao = new BookInformationDaoImpl();
        bookService = new BookServiceImpl(bookDao);
        logger.info("BookServlet初始化完成，已加载BookService和BookInformationDaoImpl");
    }

    /**
     * GET请求：处理图书查询
     * 支持：1. 无参数→查所有  2. ?isbn=xxx→按ISBN查  3. ?name=xxx→按书名模糊查
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);

        PrintWriter out = resp.getWriter();


        try {
            // 获取查询参数（与其他模块参数传递方式一致）
            String isbn = req.getParameter("isbn");
            String bookName = req.getParameter("name");
            String type = req.getParameter("type");
            String userId = req.getParameter("userId"); // 前端传用户ID
            String number = req.getParameter("number");

            // ===================== 🔥 新增：联想搜索专用参数 =====================
            String suggest = req.getParameter("suggest"); // 标识是否为联想请求
            String keyword = req.getParameter("keyword"); // 联想搜索关键词
            String searchType = req.getParameter("searchType"); // 联想类型：bookName/isbn

            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");
            Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 12;
            String typeId = req.getParameter("typeId");

            ResultDTO<List<BookInformation>> result;
            // 分支查询逻辑（与其他模块查询逻辑结构一致）

            // ===================== 🔥 核心：优先处理联想搜索请求（新增分支） =====================
            if ("true".equals(suggest) && keyword != null && !keyword.isEmpty()) {
                if ("bookName".equals(searchType)) {
                    // 复用你现有的 书名模糊查询（做联想）
                    result = bookService.queryBookByName(keyword);
                    logger.info("书名联想搜索[{}]完成", keyword);
                } else if ("isbn".equals(searchType)) {
                    // 复用你现有的 ISBN精确查询（做联想）
                    result = bookService.queryBookByISBN(keyword);
                    logger.info("ISBN联想搜索[{}]完成", keyword);
                } else {
                    result = ResultDTO.success(Collections.emptyList());
                }
                // 联想搜索只返回最多10条，优化下拉框性能
                if (result.getData() != null && result.getData().size() > 10) {
                    result.setData(result.getData().subList(0, 10));
                }

                // ===================== 以下是你【原有所有逻辑】，完全不动 =====================
            }
            // 🔥 分页查询推荐书籍
            else if ("recommend".equals(type) && pageNumStr != null) {
                ResultDTO<PageResultDTO<BookInformation>> newresult = bookService.getHybridRecommendBooksPage(userId, pageNum, pageSize);
                out.write(JSON.toJSONString(newresult));
                return;
            }
            // 🔥 分页查询热门书籍
            else if ("hot".equals(type) && pageNumStr != null) {
                ResultDTO<PageResultDTO<BookInformation>> newresult = bookService.queryByHotDescPage(pageNum, pageSize);
                out.write(JSON.toJSONString(newresult));
                return;
            }// ====================== 🆕 新增：按类型分页查询（分类书籍） ======================
            else if ("category".equals(type) && typeId != null) {
                Integer typeIdInt = Integer.parseInt(typeId);
                ResultDTO<PageResultDTO<BookInformation>> newresult = bookService.queryByTypePage(typeIdInt, pageNum, pageSize);
                out.write(JSON.toJSONString(newresult));
                return;
            }
            else if (isbn != null && !isbn.isEmpty()) {
                // 按ISBN查询
                result = bookService.queryBookByISBN(isbn);
                logger.info("按ISBN[{}]查询图书完成", isbn);
            } else if (bookName != null && !bookName.isEmpty()) {
                // 按书名模糊查询
                result = bookService.queryBookByName(bookName);
                logger.info("按书名[{}]模糊查询图书完成", bookName);
            } else {
                // 个性化混合推荐 / 默认热度榜
                if ("recommend".equals(type)) {
                    result = bookService.getHybridRecommendBooks(userId, Integer.parseInt(number));
                } else {
                    result = bookService.queryByHotDesc(Integer.parseInt(number));
                }
                logger.info("查询所有图书完成");
            }

            // 统一返回JSON（与其他模块一致）
            out.write(JSON.toJSONString(result));

        } catch (Exception e) {
            // 统一异常处理（日志+错误返回，与其他模块一致）
            logger.error("图书查询异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("查询图书失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            // 资源释放（与其他模块一致）
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * POST请求：处理 普通添加 / 爬虫添加 图书
     * action=add → 手动添加
     * action=crawl → 爬虫添加
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            // ===================== 【关键：获取action区分请求】 =====================
            String action = req.getParameter("action");

            // 👇 分支1：爬虫添加图书（新增）
            if ("crawl".equals(action)) {
                // 获取前端传的 书籍名称
                String bookName = req.getParameter("bookName");
                ResultDTO<String> result = bookService.crawlAndAddBook(bookName);
                logger.info("爬虫添加图书请求完成，书名：{}", bookName);
                out.write(JSON.toJSONString(result));

            }
            else {
                // 👇 分支2：你原来的 普通手动添加图书（保留不动）
                BookInformation book = JSON.parseObject(req.getInputStream(), BookInformation.class);
                ResultDTO<Void> result = bookService.addBook(book);
                logger.info("手动新增图书[ISBN:{}]请求处理完成", book.getISBN());
                out.write(JSON.toJSONString(result));
            }
        } catch (Exception e) {
            logger.error("图书添加异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("添加失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * DELETE请求：处理删除图书（按ISBN删除，与其他模块删除逻辑一致）
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");


        PrintWriter out = resp.getWriter();
        try {
            // 获取删除参数（ISBN，与其他模块参数传递方式一致）
            String isbn = req.getParameter("isbn");
            if (isbn == null || isbn.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("图书ISBN不能为空")));
                out.flush();
                out.close();
                return;
            }

            // 调用业务层删除
            ResultDTO<Void> result = bookService.deleteBook(isbn);
            logger.info("删除图书[ISBN:{}]请求处理完成", isbn);

            out.write(JSON.toJSONString(result));

        } catch (Exception e) {
            logger.error("删除图书异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("删除图书失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * PUT请求：处理更新图书（与其他模块更新逻辑一致）
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");
        // 专门更新AI总结
        if ("updateAiSummary".equals(action)) {
            String isbn = req.getParameter("isbn");
            String aiSummary = req.getParameter("aiSummary");
            try {
                bookDao.updateAiSummary(isbn, aiSummary);
                out.write(JSON.toJSONString(ResultDTO.success("更新成功")));
            } catch (Exception e) {
                logger.error("更新AI总结失败", e);
                out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
            }finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
            return;
        }
        try {
            // 解析请求体为BookInformation对象
            BookInformation book = JSON.parseObject(req.getInputStream(), BookInformation.class);

            // 调用业务层更新
            ResultDTO<Void> result = bookService.updateBook(book);
            logger.info("更新图书[ISBN:{}]请求处理完成", book.getISBN());

            out.write(JSON.toJSONString(result));

        } catch (Exception e) {
            logger.error("更新图书异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("更新图书失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    // BookServlet.java 新增doPatch方法（或新增一个PUT子路径）
    /**
     * PATCH请求：仅更新书籍库存（用于借阅/归还场景）
     */
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        try {
            // 解析请求体为BookInformation对象
            BookInformation book = JSON.parseObject(req.getInputStream(), BookInformation.class);

            // 调用业务层更新
            ResultDTO<Void> result = bookService.updateBookStock(book);
            logger.info("更新图书[ISBN:{}]请求处理完成", book.getISBN());

            out.write(JSON.toJSONString(result));

        } catch (Exception e) {
            logger.error("更新图书异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("更新图书失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}
