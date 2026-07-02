package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.UserCollectionDao;
import com.example.dao.impl.UserBookshelfGroupDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserBookshelfGroup;
import com.example.service.UserBookshelfGroupService;
import com.example.service.impl.UserBookshelfGroupServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/user/bookshelf/group")
public class UserBookshelfGroupServlet extends BaseServlet {
    private UserBookshelfGroupDaoImpl groupDao;
    private UserBookshelfGroupService groupService;
    private UserCollectionDao collectionDao;
    private static final Logger logger = LoggerFactory.getLogger(UserBookshelfGroupServlet.class);

    @Override
    public void init() throws ServletException {
        groupDao = new UserBookshelfGroupDaoImpl();
        groupService = new UserBookshelfGroupServiceImpl(groupDao, collectionDao);
        logger.info("UserBookshelfGroupServlet 初始化完成");
    }

    /**
     * GET 查询：
     * 1. 传 groupId → 查询单条分组
     * 2. 传 userId → 查询该用户所有分组（分页）
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;
        try {
            String groupIdStr = req.getParameter("groupId");
            String userId = req.getParameter("userId");
            String pageStr = req.getParameter("page");
            String pageSizeStr = req.getParameter("pageSize");

            Integer page = pageStr == null ? 1 : Integer.parseInt(pageStr);
            Integer pageSize = pageSizeStr == null ? 10 : Integer.parseInt(pageSizeStr);

            if (groupIdStr != null && !groupIdStr.isBlank()) {
                // 根据分组ID查询
                Integer groupId = Integer.parseInt(groupIdStr);
                result = groupService.queryGroupById(groupId);
                logger.info("查询分组详情 groupId:{}", groupId);
            } else if (userId != null && !userId.isBlank()) {
                // 根据用户ID查询所有分组
                result = groupService.queryGroupByUserId(userId, page, pageSize);
                logger.info("查询用户{}的书架分组 page:{}", userId, page);
            } else {
                result = ResultDTO.paramError("必须传入groupId或userId参数");
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("书架分组查询异常", e);
            ResultDTO<String> err = ResultDTO.fail("查询失败：" + e.getMessage());
            out.write(JSON.toJSONString(err));
        } finally {
            out.flush();
            out.close();
        }
    }

    /** POST 新增分组 */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();
        try {
            UserBookshelfGroup group = JSON.parseObject(req.getInputStream(), UserBookshelfGroup.class);
            ResultDTO<Void> result = groupService.addGroup(group);
            logger.info("新增书架分组 userId:{} groupName:{}", group.getUserId(), group.getGroupName());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增分组异常", e);
            ResultDTO<String> err = ResultDTO.fail("新增失败：" + e.getMessage());
            out.write(JSON.toJSONString(err));
        } finally {
            out.flush();
            out.close();
        }
    }

    /** PUT 动态更新分组 */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=utf-8");
        PrintWriter out = resp.getWriter();
        try {
            UserBookshelfGroup group = JSON.parseObject(req.getInputStream(), UserBookshelfGroup.class);
            ResultDTO<Void> result = groupService.updateGroup(group);
            logger.info("更新书架分组 groupId:{}", group.getGroupId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新分组异常", e);
            ResultDTO<String> err = ResultDTO.fail("更新失败：" + e.getMessage());
            out.write(JSON.toJSONString(err));
        } finally {
            out.flush();
            out.close();
        }
    }

    /** DELETE 删除分组 */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=utf-8");
        PrintWriter out = resp.getWriter();
        try {
            String groupIdStr = req.getParameter("groupId");
            if (groupIdStr == null || groupIdStr.isBlank()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("分组ID不能为空")));
                return;
            }
            Integer groupId = Integer.parseInt(groupIdStr);
            ResultDTO<Void> result = groupService.deleteGroup(groupId);
            logger.info("删除书架分组 groupId:{}", groupId);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("删除分组异常", e);
            ResultDTO<String> err = ResultDTO.fail("删除失败：" + e.getMessage());
            out.write(JSON.toJSONString(err));
        } finally {
            out.flush();
            out.close();
        }
    }
}