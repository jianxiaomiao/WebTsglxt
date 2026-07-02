package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookInformationDaoImpl;
import com.example.dao.impl.BorrowInformationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.entity.BorrowInformation;
import com.example.service.BookService;
import com.example.service.BorrowInformationService;
import com.example.service.impl.BookServiceImpl;
import com.example.service.impl.BorrowInformationServiceImpl;
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
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/book/borrow")
public class BorrowInformationServlet extends BaseServlet {
    private BorrowInformationDaoImpl borrowInformationDao;
    private BorrowInformationService borrowInformationService;
    // ✅ 新增：注入书籍相关的Dao和Service
    private BookInformationDaoImpl bookDao;
    private BookService bookService;
    private static final Logger logger = LoggerFactory.getLogger(BorrowInformationServlet.class);

    @Override
    public void init() throws ServletException {
        borrowInformationDao = new BorrowInformationDaoImpl();
        borrowInformationService = new BorrowInformationServiceImpl(borrowInformationDao);
        // ✅ 初始化书籍相关的Dao和Service
        bookDao = new BookInformationDaoImpl();
        bookService = new BookServiceImpl(bookDao);
    }

    // 处理查询请求（GET）：查所有/按ISBN/按用户ID
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String isbn = req.getParameter("isbn");
        String userIdStr = req.getParameter("userId");

        try {
            ResultDTO<List<BorrowInformation>> result;
            if (isbn != null && !isbn.isEmpty()) {
                // 按ISBN查询
                result = borrowInformationService.queryBorrowByISBN(isbn);
            } else if (userIdStr != null && !userIdStr.isEmpty()) {
                // 按用户ID查询
                result = borrowInformationService.queryBorrowByUserId(userIdStr);
            } else {
                // 查询所有借阅信息
                result = borrowInformationService.queryAllBorrows();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("查询数量格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("查询数量格式错误")));
        } catch (Exception e) {
            logger.error("查询借阅信息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理新增借阅（借书）请求（POST）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        BorrowInformation borrow = JSON.parseObject(req.getInputStream(), BorrowInformation.class);

        try {
            // 2. 自动填充“借阅时间”为当前系统时间
            borrow.setBorrowDate(LocalDate.now());
            borrow.setReturnDate(LocalDate.now().plusMonths(2));
            borrow.setFine(0.0f);
            ResultDTO<Void> result = borrowInformationService.addBorrow(borrow);

            // ✅ 新增：借阅成功后，更新书籍借阅量+1
            if (result.getCode() == 200) {
                updateBookBorrowCount(borrow.getISBN());
            }

            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(borrow.getUserid(), 13, borrow.getISBN(), null, null);
        } catch (Exception e) {
            logger.error("新增借阅信息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("借书失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理删除借阅信息请求（DELETE）
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String borrowIdStr = req.getParameter("borrowId");
        // 新增：获取当前用户ID（和查询借阅列表的userId一致）
        String userId = req.getParameter("userId");

        try {
            if (borrowIdStr == null || borrowIdStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("借阅ID不能为空")));
                out.flush();
                out.close();
                return;
            }
            Integer borrowId = Integer.parseInt(borrowIdStr);
            borrowInformationService.deleteBorrow(borrowId);

            // 2. 重新查询该用户的完整借阅列表（这样返回结果就包含最新的借阅数据）
            ResultDTO<List<BorrowInformation>> result = borrowInformationService.queryBorrowByUserId(userId);
            logger.info("删除借阅记录[{}]后，返回用户[{}]的最新借阅列表", borrowId, userId);

            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("借阅ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("借阅ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除借阅信息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理更新借阅信息（还书）请求（PUT）
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        BorrowInformation borrow = JSON.parseObject(req.getInputStream(), BorrowInformation.class);

        try {
            ResultDTO<Void> result = borrowInformationService.updateBorrow(borrow);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新借阅信息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("还书失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * 通用方法：更新书籍借阅量+1
     * @param isbn 书籍ISBN
     */
    /**
     * 正常版：更新书籍借阅量+1
     */
    private void updateBookBorrowCount(String isbn) {
        try {
            List<BookInformation> bookList = bookDao.queryByISBN(isbn);
            if (bookList != null && !bookList.isEmpty()) {
                BookInformation book = bookList.get(0);
                int newCount = Math.max(0, book.getBorrowCount() + 1);
                book.setBorrowCount(newCount);
                bookService.updateBookStock(book); // 用专门的统计更新方法
                logger.info("书籍[ISBN:{}]借阅量更新成功，新借阅量：{}", isbn, newCount);
            }
        } catch (Exception e) {
            logger.error("更新书籍[ISBN:{}]借阅量失败", isbn, e);
        }
    }
}