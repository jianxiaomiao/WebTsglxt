package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.DeptTypeDao;
import com.example.dao.impl.BookTypeDaoImpl;
import com.example.dao.impl.DeptTypeDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.DeptType;
import com.example.service.DeptTypeService;
import com.example.service.impl.DeptTypeServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/user/deptType")
public class DeptTypeServlet extends BaseServlet {

    private DeptTypeDao deptTypeDao;
    private DeptTypeServiceImpl deptTypeService;
    private static final Logger logger = LoggerFactory.getLogger(DeptTypeServlet.class);

    // 初始化：对标BookTypeServlet
    @Override
    public void init() throws ServletException {
        deptTypeDao = new DeptTypeDaoImpl();
        deptTypeService = new DeptTypeServiceImpl(deptTypeDao);
        logger.info("DeptTypeServlet初始化完成");
    }

    // GET 查询所有 / 根据ID查询
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String idStr = req.getParameter("id");
            ResultDTO<List<DeptType>> result;

            if (idStr != null && !idStr.isEmpty()) {
                result = deptTypeService.queryDeptTypeById(Integer.parseInt(idStr));
            } else {
                result = deptTypeService.queryAllDeptTypes();
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("系别类型ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("系别类型查询异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询系别类型失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // POST 新增
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            DeptType deptType = JSON.parseObject(req.getInputStream(), DeptType.class);
            ResultDTO<Void> result = deptTypeService.addDeptType(deptType);
            logger.info("新增系别类型[{}]请求处理完成", deptType.getDeptType());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增系别类型异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增系别类型失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // PUT 修改
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            DeptType deptType = JSON.parseObject(req.getInputStream(), DeptType.class);
            ResultDTO<Void> result = deptTypeService.updateDeptType(deptType);
            logger.info("更新系别类型[Id:{}]请求处理完成", deptType.getId());
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新系别类型异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新系别类型失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // DELETE 删除
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        PrintWriter out = resp.getWriter();

        try {
            String idStr = req.getParameter("id");
            if (idStr == null || idStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("系别类型ID不能为空")));
                return;
            }
            ResultDTO<Void> result = deptTypeService.deleteDeptType(Integer.parseInt(idStr));
            logger.info("删除系别类型[Id:{}]请求处理完成", idStr);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("系别类型ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除系别类型异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除系别类型失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}