package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookBottleDaoImpl;
import com.example.dao.impl.BookBottlePickDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookBottle;
import com.example.service.BookBottleService;
import com.example.service.impl.BookBottleServiceImpl;
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
import java.util.List;

@WebServlet("/api/book/bottle")
public class BookBottleServlet extends BaseServlet {
    private BookBottleService bottleService;
    private static final Logger logger = LoggerFactory.getLogger(BookBottleServlet.class);

    @Override
    public void init() throws ServletException {
        bottleService = new BookBottleServiceImpl(new BookBottleDaoImpl(), new BookBottlePickDaoImpl());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;

        try {
            // 获取所有查询参数
            String bottleIdStr = req.getParameter("id");
            String isbn = req.getParameter("isbn");
            String chapter = req.getParameter("chapter");
            String userId = req.getParameter("userId");
            String getRandom = req.getParameter("random");
            String loginUid = req.getParameter("loginUserId");

            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");
            Integer pageNum = pageNumStr == null ? 1 : Integer.parseInt(pageNumStr);
            Integer pageSize = pageSizeStr == null ? 10 : Integer.parseInt(pageSizeStr);

            // 1. 分页获取可捞取的漂流瓶列表
            if ("1".equals(getRandom) && isbn != null && loginUid != null) {
                result = bottleService.randomGetBottle(isbn, loginUid, pageNum, pageSize);
                logger.info("用户{}查询ISBN{}的可捞漂流瓶列表，第{}页", loginUid, isbn, pageNum);
            }
            // 2. 根据ID查询单个瓶子
            else if (bottleIdStr != null) {
                Integer bid = Integer.parseInt(bottleIdStr);
                result = bottleService.queryBottleById(bid);
                logger.info("查询漂流瓶ID:{}", bid);
            }
            // 3. 章节分页查询漂流瓶
            else if (chapter != null && isbn != null) {
                result = bottleService.queryBottleByChapterPage(chapter, pageNum, pageSize);
                logger.info("分页查询章节{}漂流瓶，第{}页", chapter, pageNum);
            }
            // 4. ISBN分页查询漂流瓶
            else if (isbn != null) {
                result = bottleService.queryBottleByIsbnPage(isbn, pageNum, pageSize);
                logger.info("分页查询ISBN{}漂流瓶，第{}页", isbn, pageNum);
            }
            // 5. 查询当前用户投放的所有瓶子
            else if (userId != null) {
                result = bottleService.queryBottleByUserId(userId);
                logger.info("查询用户{}投放的全部漂流瓶", userId);
            }
            else {
                result = ResultDTO.paramError("缺少查询参数：id / isbn / chapter / userId / random");
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("分页/ID数字参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("数字参数格式错误")));
        } catch (Exception e) {
            logger.error("漂流瓶查询接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String action = req.getParameter("action");

            // 新增：用户捞取瓶子（支持传入回复内容）
            if ("pick".equals(action)) {
                String loginUserId = req.getParameter("loginUserId");
                String bottleIdStr = req.getParameter("bottleId");
                String replycontent = req.getParameter("replycontent"); // 接收前端回复内容

                if (bottleIdStr == null || bottleIdStr.isBlank()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("bottleId不能为空")));
                    return;
                }
                Integer bottleId = Integer.parseInt(bottleIdStr);
                ResultDTO<String> res = bottleService.pickBottle(loginUserId, bottleId, replycontent);

                // 记录捞取行为日志，携带书籍ISBN
                if (res.getCode() == 200) {
                    String isbn = res.getData();
                    UserBehaviorLogger.logAsync(loginUserId, 38, isbn, null, "捞取漂流瓶");
                }
                out.write(JSON.toJSONString(res));
            }
            // 原有：投放新漂流瓶
            else {
                BookBottle bottle = JSON.parseObject(req.getInputStream(), BookBottle.class);
                ResultDTO<Void> res = bottleService.addBottle(bottle);
                if (res.getCode() == 200) {
                    UserBehaviorLogger.logAsync(bottle.getUserid(), 39, bottle.getIsbn(), null, "投放漂流瓶");
                }
                out.write(JSON.toJSONString(res));
            }
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("ID参数格式错误")));
        } catch (Exception e) {
            logger.error("漂流瓶POST接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // PUT：动态更新漂流瓶（编辑内容/手动修改状态）
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            BookBottle bottle = JSON.parseObject(req.getInputStream(), BookBottle.class);
            ResultDTO<Void> res = bottleService.updateBottle(bottle);
            out.write(JSON.toJSONString(res));
        } catch (Exception e) {
            logger.error("更新漂流瓶接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // DELETE：删除漂流瓶（软删除标记is_deleted=1）
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            String bidStr = req.getParameter("id");
            if (bidStr == null || bidStr.isBlank()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("漂流瓶id不能为空")));
                return;
            }
            Integer bid = Integer.parseInt(bidStr);
            ResultDTO<Void> res = bottleService.deleteBottle(bid);
            out.write(JSON.toJSONString(res));
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("漂流瓶id必须为数字")));
        } catch (Exception e) {
            logger.error("删除漂流瓶接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}