package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.BookInformationDao;
import com.example.dao.impl.*;
import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;
import com.example.service.UserStatsService;
import com.example.service.impl.UserStatsServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/user/stats")
public class UserStatsServlet extends BaseServlet {
    private UserStatsService userStatsService;

    @Override
    public void init() throws ServletException {
        // 1. 初始化所有DAO（补全最新Service需要的4个缺失DAO）
        UserReadDailyDaoImpl readDailyDao = new UserReadDailyDaoImpl();
        UserReadProgressDaoImpl readProgressDao = new UserReadProgressDaoImpl();
        UserReadRecordDaoImpl readRecordDao = new UserReadRecordDaoImpl(); // 新增
        UserTextCollectionDaoImpl noteDao = new UserTextCollectionDaoImpl();
        BookmarkDaoImpl bookmarkDao = new BookmarkDaoImpl(); // 新增
        ChatMessageDaoImpl chatDao = new ChatMessageDaoImpl();
        AiChatDaoImpl aiChatDao = new AiChatDaoImpl();
        BookCommentDaoImpl bookCommentDao = new BookCommentDaoImpl();
        UserCommentDaoImpl userCommentDao = new UserCommentDaoImpl();
        // 新增：评论点赞DAO
        UserCommentLikeDaoImpl userCommentLikeDao = new UserCommentLikeDaoImpl();
        UserCollectionDaoImpl collectionDao = new UserCollectionDaoImpl();
        BorrowInformationDaoImpl borrowDao = new BorrowInformationDaoImpl();
        FriendDaoImpl friendDao = new FriendDaoImpl(); // 新增
        FriendRequestDaoImpl friendRequestDao = new FriendRequestDaoImpl(); // 新增
        UserInformationDaoImpl userDao = new UserInformationDaoImpl();
        BookInformationDaoImpl bookInformationDao = new BookInformationDaoImpl();
        // 2. 严格按照Service构造器顺序注入（100%匹配最新Service）
        userStatsService = new UserStatsServiceImpl(
                readDailyDao,
                readProgressDao,
                readRecordDao,
                noteDao,
                bookmarkDao,
                chatDao,
                aiChatDao,
                bookCommentDao,
                userCommentDao,
                collectionDao,
                borrowDao,
                friendDao,
                friendRequestDao,
                // 新增：注入点赞DAO
                userCommentLikeDao,
                userDao,
                bookInformationDao
        );
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 调用父类跨域方法（完全复用BaseServlet，无修改）
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String userId = req.getParameter("userId");
            String type = req.getParameter("type");

            ResultDTO<UserStatsDTO> result = userStatsService.generateUserStats(userId, type);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            out.write(JSON.toJSONString(ResultDTO.fail("统计接口异常：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}