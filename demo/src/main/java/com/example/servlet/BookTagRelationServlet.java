package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.entity.BookTagRelation;
import com.example.service.BookTagRelationService;
import com.example.service.impl.BookTagRelationServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/book/tag/relation")
public class BookTagRelationServlet extends BaseServlet {
    private BookTagRelationService relationService;

    @Override
    public void init() {
        relationService = new BookTagRelationServiceImpl();
    }

    // 查询：ID/ISBN/TagID
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            String isbn = req.getParameter("isbn");
            String tagId = req.getParameter("tagId");
            ResultDTO<?> result;

            if (id != null) {
                result = relationService.getRelationById(Integer.parseInt(id));
            } else if (isbn != null) {
                result = relationService.getRelationsByIsbn(isbn);
            } else if (tagId != null) {
                result = relationService.getRelationsByTagId(Integer.parseInt(tagId));
            } else {
                result = ResultDTO.paramError("参数不能为空");
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 新增关联
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            BookTagRelation relation = JSON.parseObject(req.getInputStream(), BookTagRelation.class);
            ResultDTO<Void> result = relationService.addRelation(relation);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除关联
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            ResultDTO<Void> result = relationService.deleteRelation(Integer.parseInt(id));
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}