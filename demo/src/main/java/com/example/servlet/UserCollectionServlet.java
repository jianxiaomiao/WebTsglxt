package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookInformationDaoImpl;
import com.example.dao.impl.UserCollectionDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.entity.UserCollection;
import com.example.service.BookService;
import com.example.service.UserCollectionService;
import com.example.service.impl.BookServiceImpl;
import com.example.service.impl.UserCollectionServiceImpl;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/collection")
public class UserCollectionServlet extends BaseServlet {
    private UserCollectionDaoImpl userCollectionDao;
    private UserCollectionService userCollectionService;
    // ✅ 新增：注入书籍相关的Dao和Service
    private BookInformationDaoImpl bookDao;
    private BookService bookService;
    private static final Logger logger = LoggerFactory.getLogger(UserCollectionServlet.class);

    @Override
    public void init() throws ServletException {
        userCollectionDao = new UserCollectionDaoImpl();
        userCollectionService = new UserCollectionServiceImpl(userCollectionDao);
        // ✅ 初始化书籍相关的Dao和Service
        bookDao = new BookInformationDaoImpl();
        bookService = new BookServiceImpl(bookDao);
        logger.info("UserCollectionServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String collectionId = req.getParameter("collectionId");
            String userId = req.getParameter("userId");
            String isbn = req.getParameter("isbn");
            // ========== 新增分组参数 ==========
            String groupIdStr = req.getParameter("groupId");

            // 🔥 新增：接收分页参数
            String pageStr = req.getParameter("page");
            String pageSizeStr = req.getParameter("pageSize");
            Integer page = pageStr == null ? 1 : Integer.parseInt(pageStr);
            Integer pageSize = pageSizeStr == null ? 10 : Integer.parseInt(pageSizeStr);

            ResultDTO<?> result;
            if (collectionId != null && !collectionId.isEmpty()) {
                result = userCollectionService.queryCollectionById(Integer.parseInt(collectionId));
                logger.info("按CollectionId[{}]查询收藏完成", collectionId);
            }else if (userId != null && !userId.isEmpty() && groupIdStr != null && !groupIdStr.isEmpty()) {
                // 分支：传入groupId → 查询【该分组全部书籍】
                Integer groupId = Integer.parseInt(groupIdStr);
                // 不传pageSize默认99999，一次性加载分组所有书
                result = userCollectionService.queryCollectionByUserAndGroup(userId, groupId, page, pageSize);
                logger.info("按用户{}分组{}查询藏书，page={}", userId, groupId, page);
            }else if (userId != null && !userId.isEmpty()) {
                // 🔥 调用分页接口
                result = userCollectionService.queryCollectionByUserId(userId, page, pageSize);
                logger.info("按UserId[{}]查询收藏完成，分页：page={}, pageSize={}", userId, page, pageSize);
            } else if (isbn != null && !isbn.isEmpty()) {
                result = userCollectionService.queryCollectionByIsbn(isbn);
                logger.info("按ISBN[{}]查询收藏完成", isbn);
            } else {
                result = userCollectionService.queryAllCollections();
                logger.info("查询所有收藏完成");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("收藏查询异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("查询收藏失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            UserCollection userCollection = JSON.parseObject(req.getInputStream(), UserCollection.class);
            ResultDTO<Void> result = userCollectionService.addCollection(userCollection);

            // ✅ 新增：收藏成功后，更新书籍收藏量+1
            if (result.getCode() == 200) {
                updateBookCollectionCount(userCollection.getIsbn(), 1);
            }

            logger.info("新增收藏[UserId:{}, ISBN:{}, GroupId:{}]请求处理完成", userCollection.getUserId(), userCollection.getIsbn(), userCollection.getGroupId());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(userCollection.getUserId(), 2, userCollection.getIsbn(), null, "收藏书籍" + userCollection.getBookName());
        } catch (Exception e) {
            logger.error("新增收藏异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("新增收藏失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String collectionId = req.getParameter("collectionId");
            // ✅ 新增：从请求参数获取ISBN（前端删除时需要传递）
            String isbn = req.getParameter("iSBN");

            if (collectionId == null || collectionId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("收藏ID不能为空")));
                out.flush();
                out.close();
                return;
            }
            if(isbn == null || isbn.isEmpty()){
                out.write(JSON.toJSONString(ResultDTO.paramError("isbn不能为空")));
                out.flush();
                out.close();
                return;
            }

            ResultDTO<Void> result = userCollectionService.deleteCollection(Integer.parseInt(collectionId));

            // ✅ 新增：取消收藏成功后，更新书籍收藏量-1
            if (result.getCode() == 200 && isbn != null && !isbn.isEmpty()) {
                updateBookCollectionCount(isbn, -1);
            }

            logger.info("删除收藏[CollectionId:{}]请求处理完成", collectionId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除收藏异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("删除收藏失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            UserCollection userCollection = JSON.parseObject(req.getInputStream(), UserCollection.class);
            ResultDTO<Void> result = userCollectionService.updateCollection(userCollection);
            logger.info("更新收藏[CollectionId:{}, GroupId:{}]请求处理完成", userCollection.getCollectionId(), userCollection.getGroupId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新收藏异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("更新收藏失败：" + e.getMessage());
            out.write(JSON.toJSONString(errorResult));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * 通用方法：更新书籍收藏量
     * @param isbn 书籍ISBN
     * @param delta 变化量（+1 收藏，-1 取消收藏）
     */
    private void updateBookCollectionCount(String isbn, int delta) {
        try {
            List<BookInformation> bookList = bookDao.queryByISBN(isbn);
            if (bookList != null && !bookList.isEmpty()) {
                BookInformation book = bookList.get(0);
                // 确保收藏量不会变成负数
                int newCount = Math.max(0, book.getCollectionCount() + delta);
                book.setCollectionCount(newCount);
                bookService.updateBookStock(book);
                logger.info("书籍[ISBN:{}]收藏量更新成功，新收藏量：{}", isbn, newCount);
            }
        } catch (Exception e) {
            // 重要：即使更新收藏量失败，也不能影响收藏/取消收藏操作
            logger.error("更新书籍[ISBN:{}]收藏量失败", isbn, e);
        }
    }
}