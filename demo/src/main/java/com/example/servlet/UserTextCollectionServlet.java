package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserTextCollectionDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserTextCollection;
import com.example.service.UserTextCollectionService;
import com.example.service.impl.UserTextCollectionServiceImpl;
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

@WebServlet("/api/user/note")
public class UserTextCollectionServlet extends BaseServlet {
    private UserTextCollectionDaoImpl userTextCollectionDao;
    private UserTextCollectionService userTextCollectionService;
    private static final Logger logger = LoggerFactory.getLogger(UserTextCollectionServlet.class);

    @Override
    public void init() throws ServletException {
        userTextCollectionDao = new UserTextCollectionDaoImpl();
        userTextCollectionService = new UserTextCollectionServiceImpl(userTextCollectionDao);
        logger.info("UserTextCollectionServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            String userId = req.getParameter("userId");
            String isbn = req.getParameter("isbn");
            // 新增：接收章节ID参数
            String chapterId = req.getParameter("chapterId");

            // 🔥 新增：接收分页参数
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");
            Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;


            ResultDTO<List<UserTextCollection>> result;
            if (id != null && !id.isEmpty()) {
                result = userTextCollectionService.queryNoteById(Integer.parseInt(id));
                logger.info("按Id[{}]查询阅读笔记完成", id);
            }
            // 🔥 优先：分页查询用户笔记
            else if (userId != null && !userId.isEmpty() && pageNumStr != null) {
                ResultDTO<PageResultDTO<UserTextCollection>> pageResult =
                        userTextCollectionService.queryNoteByUserIdPage(userId, pageNum, pageSize);
                out.write(JSON.toJSONString(pageResult));
                logger.info("分页查询用户[{}]阅读笔记完成，第{}页", userId, pageNum);
                return;
            }
            else if (userId != null && !userId.isEmpty()) {
                result = userTextCollectionService.queryNoteByUserId(userId);
                logger.info("按UserId[{}]查询阅读笔记完成", userId);
            } else if (isbn != null && !isbn.isEmpty()) {
                result = userTextCollectionService.queryNoteByIsbn(isbn);
                logger.info("按ISBN[{}]查询阅读笔记完成", isbn);
            }
            // 新增：按章节ID查询
            else if (chapterId != null && !chapterId.isEmpty()) {
                result = userTextCollectionService.queryNoteByChapterId(chapterId);
                logger.info("按chapterId[{}]查询阅读笔记完成", chapterId);
            } else {
                result = userTextCollectionService.queryAllNotes();
                logger.info("查询所有阅读笔记完成");
            }

            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("阅读笔记查询异常", e);
            ResultDTO<String> errorResult = ResultDTO.fail("查询阅读笔记失败：" + e.getMessage());
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
            // 自动解析chapterId字段（FastJSON会自动映射）
            UserTextCollection note = JSON.parseObject(req.getInputStream(), UserTextCollection.class);
            ResultDTO<Void> result = userTextCollectionService.addNote(note);
            // doPost 方法的日志修改
            logger.info("新增阅读笔记[UserId:{}, ISBN:{}, chapterId:{}, Type:{}]请求处理完成",
                    note.getUserId(), note.getIsbn(), note.getChapterId(), note.getType());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync( note.getUserId(), 5, note.getIsbn(), note.getChapterId(), "用户添加笔记：" + note.getText() + ",用户批注:" +note.getReaderComment());
        } catch (Exception e) {
            logger.error("新增阅读笔记异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增阅读笔记失败：" + e.getMessage())));
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
            String id = req.getParameter("id");
            if (id == null || id.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("阅读笔记ID不能为空")));
                out.flush();
                out.close();
                return;
            }

            ResultDTO<Void> result = userTextCollectionService.deleteNote(Integer.parseInt(id));
            logger.info("删除阅读笔记[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除阅读笔记异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除阅读笔记失败：" + e.getMessage())));
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
            // 自动解析chapterId字段
            UserTextCollection note = JSON.parseObject(req.getInputStream(), UserTextCollection.class);
            ResultDTO<Void> result = userTextCollectionService.updateNote(note);
            logger.info("更新阅读笔记[Id:{}]请求处理完成", note.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新阅读笔记异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新阅读笔记失败：" + e.getMessage())));
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}