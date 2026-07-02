package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserTypeDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserType;
import com.example.service.UserTypeService;
import com.example.service.impl.UserTypeServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/type")
public class UserTypeServlet extends BaseServlet {
    private UserTypeDaoImpl userTypeDao;
    private UserTypeService userTypeService;
    private static final Logger logger = LoggerFactory.getLogger(UserTypeServlet.class);

    @Override
    public void init() throws ServletException {
        userTypeDao = new UserTypeDaoImpl();
        userTypeService = new UserTypeServiceImpl(userTypeDao);
        logger.info("UserTypeServlet初始化完成");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String id = req.getParameter("id");
            ResultDTO<List<UserType>> result;
            if (id != null && !id.isEmpty()) {
                result = userTypeService.queryUserTypeById(Integer.parseInt(id));
            } else {
                result = userTypeService.queryAllUserTypes();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("用户种类查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询用户种类失败：" + e.getMessage())));
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
            UserType userType = JSON.parseObject(req.getInputStream(), UserType.class);
            ResultDTO<Void> result = userTypeService.addUserType(userType);
            logger.info("新增用户种类[{}]请求处理完成", userType.getUserType());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增用户种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增用户种类失败：" + e.getMessage())));
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
            UserType userType = JSON.parseObject(req.getInputStream(), UserType.class);
            ResultDTO<Void> result = userTypeService.updateUserType(userType);
            logger.info("更新用户种类[Id:{}]请求处理完成", userType.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新用户种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新用户种类失败：" + e.getMessage())));
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
                out.write(JSON.toJSONString(ResultDTO.paramError("用户种类ID不能为空")));
                return;
            }
            ResultDTO<Void> result = userTypeService.deleteUserType(Integer.parseInt(id));
            logger.info("删除用户种类[Id:{}]请求处理完成", id);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除用户种类异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除用户种类失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}