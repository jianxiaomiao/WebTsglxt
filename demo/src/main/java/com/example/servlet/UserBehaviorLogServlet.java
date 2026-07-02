package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.impl.UserBehaviorLogDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.UserBehaviorLog;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为日志 Servlet
 * GET：根据用户ID分页查询行为日志
 * POST：新增用户行为日志
 */
@WebServlet("/api/user/behavior/log")
public class UserBehaviorLogServlet extends BaseServlet {

    private UserBehaviorLogDaoImpl behaviorLogDao;
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorLogServlet.class);

    @Override
    public void init() throws ServletException {
        // 初始化DAO对象
        behaviorLogDao = new UserBehaviorLogDaoImpl();
    }

    /**
     * GET 请求：根据 userId + 分页参数 查询用户行为日志
     * 请求参数：userId(必传)、pageNum(可选)、pageSize(可选)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // 1. 获取请求参数
            String userId = req.getParameter("userId");
            String pageNumStr = req.getParameter("pageNum");
            String pageSizeStr = req.getParameter("pageSize");

            // 2. 分页参数默认值
            Integer pageNum = (pageNumStr != null && !pageNumStr.isEmpty()) ? Integer.parseInt(pageNumStr) : 1;
            Integer pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

            // 3. 必传参数校验
            if (userId == null || userId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID[userId]不能为空")));
                return;
            }

            // 4. 分页查询 + 统计总数
            Long total = behaviorLogDao.countByUserId(userId);
            List<UserBehaviorLog> dataList = behaviorLogDao.queryLatestByUserIdPage(userId, pageNum, pageSize);

            // 5. 组装分页DTO，再包装统一返回结果
            PageResultDTO<UserBehaviorLog> pageResult = PageResultDTO.success(total, pageNum, pageSize, dataList);
            ResultDTO<PageResultDTO<UserBehaviorLog>> result = ResultDTO.success(pageResult);

            logger.info("分页查询用户[{}]行为日志完成，第{}页，每页{}条", userId, pageNum, pageSize);
            out.write(JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            logger.error("分页参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("分页参数必须为数字类型")));
        } catch (Exception e) {
            logger.error("查询用户行为日志异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * POST 请求：新增用户行为日志
     * 前端请求体JSON格式参考：
     * {
     *     "userId": "xxx",
     *     "actionType": 1,
     *     "isbn": "xxx",
     *     "chapterNumber": "xxx",
     *     "content": "xxx"
     * }
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // 1. 解析前端JSON请求体
            JSONObject jsonObj = JSON.parseObject(req.getInputStream(), JSONObject.class);

            // 2. 提取前端参数
            String userId = jsonObj.getString("userId");
            String actionTypeStr = jsonObj.getString("actionType");
            String isbn = jsonObj.getString("isbn");
            String chapterNumber = jsonObj.getString("chapterNumber");
            String content = jsonObj.getString("content");

            // 3. 必传参数校验
            if (userId == null || userId.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID[userId]不能为空")));
                return;
            }
            if (actionTypeStr == null || actionTypeStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("操作类型[actionType]不能为空")));
                return;
            }

            // 4. 类型转换（操作类型为数字）
            Integer actionType = Integer.parseInt(actionTypeStr);

            // 5. 封装实体类，create_time 自动赋值为当前时间
            UserBehaviorLog log = new UserBehaviorLog();
            log.setUser_id(userId);
            log.setAction_type(actionType);
            log.setBook_isbn(isbn);
            log.setChapter_number(chapterNumber);
            log.setContent_snapshot(content);
            log.setCreate_time(LocalDateTime.now());

            // 6. 调用DAO新增数据
            behaviorLogDao.add(log);

            // 7. 返回成功结果
            ResultDTO<Void> result = ResultDTO.success(null);
            logger.info("新增用户[{}]行为日志完成，操作类型：{}", userId, actionType);
            out.write(JSON.toJSONString(result));

        } catch (NumberFormatException e) {
            logger.error("操作类型参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("操作类型[actionType]必须为数字")));
        } catch (Exception e) {
            logger.error("新增用户行为日志异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}