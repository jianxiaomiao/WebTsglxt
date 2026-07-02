package com.example.servlet;

import com.alibaba.fastjson.JSON;

import com.example.dao.impl.UserTextColTypeDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserTextColType;

import com.example.service.UserTextColTypeService;
import com.example.service.impl.UserTextColTypeServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/note/type")
public class UserTextColTypeServlet extends BaseServlet{
    private UserTextColTypeDaoImpl userTextColTypeDao;
    private UserTextColTypeService userTextColTypeService;
    private static final Logger logger = LoggerFactory.getLogger(UserTextColTypeServlet.class);

    @Override
    public void init() throws ServletException {
        userTextColTypeDao = new UserTextColTypeDaoImpl();
        userTextColTypeService = new UserTextColTypeServiceImpl(userTextColTypeDao);
        logger.info("UserTextColTypeServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            ResultDTO<List<UserTextColType>> result;
            if (id != null && !id.isEmpty()) {
                result = userTextColTypeService.queryUserTextColTypeId(Integer.parseInt(id));
            } else {
                result = userTextColTypeService.queryAllUserTextColType();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("用户笔记种类查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询用户笔记种类失败：" + e.getMessage())));
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
            UserTextColType userTextColType = JSON.parseObject(req.getInputStream(), UserTextColType.class);
            ResultDTO<Void> result = userTextColTypeService.addUserTextColType(userTextColType);
            logger.info("新增用户笔记种类[{}]请求处理完成", userTextColType.getUserTextColType());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增用户笔记种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增用户笔记种类失败：" + e.getMessage())));
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
            UserTextColType userTextColType = JSON.parseObject(req.getInputStream(), UserTextColType.class);
            ResultDTO<Void> result = userTextColTypeService.updateUserTextColType(userTextColType);
            logger.info("更新用户笔记种类[Id:{}]请求处理完成", userTextColType.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新用户笔记种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新用户笔记种类失败：" + e.getMessage())));
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
                out.write(JSON.toJSONString(ResultDTO.paramError("用户笔记种类ID不能为空")));
                return;
            }
            ResultDTO<Void> result = userTextColTypeService.deleteUserTextColType(Integer.parseInt(id));
            logger.info("删除用户笔记种类[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除用户笔记种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除用户笔记种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}
