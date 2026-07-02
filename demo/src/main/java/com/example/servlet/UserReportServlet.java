package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.BookInformationDao;
import com.example.dao.impl.*;
import com.example.dto.ResultDTO;
import com.example.dto.UserReportDTO;
import com.example.service.AiChatService;
import com.example.service.UserReportService;
import com.example.service.UserStatsService;
import com.example.service.impl.AiChatServiceImpl;
import com.example.service.impl.UserReportServiceImpl;
import com.example.service.impl.UserStatsServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/user/report")
public class UserReportServlet extends BaseServlet {
    private UserReportService userReportService;

    @Override
    public void init() throws ServletException {
        // 1. 初始化所有DAO
        UserReadDailyDaoImpl readDailyDao = new UserReadDailyDaoImpl();
        UserReadProgressDaoImpl readProgressDao = new UserReadProgressDaoImpl();
        UserReadRecordDaoImpl readRecordDao = new UserReadRecordDaoImpl();
        UserTextCollectionDaoImpl noteDao = new UserTextCollectionDaoImpl();
        BookmarkDaoImpl bookmarkDao = new BookmarkDaoImpl();
        ChatMessageDaoImpl chatDao = new ChatMessageDaoImpl();
        AiChatDaoImpl aiChatDao = new AiChatDaoImpl();
        BookCommentDaoImpl bookCommentDao = new BookCommentDaoImpl();
        UserCommentDaoImpl userCommentDao = new UserCommentDaoImpl();
        UserCommentLikeDaoImpl userCommentLikeDao = new UserCommentLikeDaoImpl();
        UserCollectionDaoImpl collectionDao = new UserCollectionDaoImpl();
        BorrowInformationDaoImpl borrowDao = new BorrowInformationDaoImpl();
        FriendDaoImpl friendDao = new FriendDaoImpl();
        FriendRequestDaoImpl friendRequestDao = new FriendRequestDaoImpl();
        UserInformationDaoImpl userDao = new UserInformationDaoImpl();
        // 新增：报告缓存DAO
        UserReportDaoImpl userReportDao = new UserReportDaoImpl();
// 👇 注入书籍信息DAO
        BookInformationDaoImpl bookInformationDao = new BookInformationDaoImpl();
        // 2. 初始化Service
        UserStatsService statsService = new UserStatsServiceImpl(
                readDailyDao, readProgressDao, readRecordDao, noteDao, bookmarkDao,
                chatDao, aiChatDao, bookCommentDao, userCommentDao, collectionDao,
                borrowDao, friendDao, friendRequestDao, userCommentLikeDao, userDao,
                bookInformationDao
        );
        AiChatService aiChatService = new AiChatServiceImpl(aiChatDao);
        // 构造方法传入 reportDAO
        userReportService = new UserReportServiceImpl(statsService, aiChatService, userReportDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String userId = req.getParameter("userId");
            String type = req.getParameter("type");
            String date = req.getParameter("date"); // ✅ 新增：接收date参数

            // 2. ✅ 获取分页参数（字符串转数字）
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");
            Integer pageNum = pageNumStr == null ? null : Integer.parseInt(pageNumStr);
            Integer pageSize = pageSizeStr == null ? null : Integer.parseInt(pageSizeStr);

            ResultDTO<?> result;
            // ✅ 新增：如果传入date，直接查询指定日期的报告
            if (date != null && !date.trim().isEmpty()) {
                result = userReportService.getReportByDate(userId, type, date);
            }
            // 原有逻辑：分页查询历史
            else if (pageNum != null || pageSize != null) {
                result = userReportService.getReportHistory(userId, type, pageNum, pageSize);
            }
            // 原有逻辑：生成新报告
            else {
                result = userReportService.generateUserReport(userId, type);
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            out.write(JSON.toJSONString(ResultDTO.fail("生成报告异常：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}