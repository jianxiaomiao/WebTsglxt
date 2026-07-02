package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.entity.BookTag;
import com.example.service.BookTagService;
import com.example.service.impl.BookTagServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/book/tag")
public class BookTagServlet extends BaseServlet {
    private BookTagService tagService;

    @Override
    public void init() {
        tagService = new BookTagServiceImpl();
    }

    // 查询：根据ID/查所有
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            ResultDTO<?> result;
            if (id != null) {
                result = tagService.getTagById(Integer.parseInt(id));
            } else {
                result = tagService.getAllTags();
            }
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 新增标签
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            BookTag tag = JSON.parseObject(req.getInputStream(), BookTag.class);
            ResultDTO<Void> result = tagService.addTag(tag);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 修改标签
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            BookTag tag = JSON.parseObject(req.getInputStream(), BookTag.class);
            ResultDTO<Void> result = tagService.updateTag(tag);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除标签
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String id = req.getParameter("id");
            ResultDTO<Void> result = tagService.deleteTag(Integer.parseInt(id));
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}