package com.example.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BaseServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeader(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 是预检标准返回
    }

    // ==================== 🔒 登录检查 ====================
    /**
     * 检查用户是否已登录。未登录返回 401 并写入 JSON 错误信息。
     * @return true=已登录, false=已拦截响应
     */
    protected boolean requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object currentUser = request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
            return false;
        }
        // 🔥 检查账号是否被冻结
        try {
            com.example.entity.UserInformation user = (com.example.entity.UserInformation) currentUser;
            if (user.getCan_use() != null && user.getCan_use() == 0) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"账号已被冻结，请联系管理员\"}");
                return false;
            }
        } catch (Exception ignored) {}
        return true;
    }

    /**
     * 统一设置跨域头（必须传入request，动态获取Origin）
     */
    protected void setCorsHeader(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, If-None-Match");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setContentType("application/json;charset=utf-8");
    }

    /**
     * ETag 缓存检查 — 如果客户端 If-None-Match 与当前数据 hash 匹配，返回 304
     * @param data 要比较的数据
     * @return true=已返回304, false=正常处理
     */
    protected boolean checkETag(HttpServletRequest request, HttpServletResponse response, String data) {
        String etag = "\"" + data.hashCode() + "\"";
        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "no-cache");
        String ifNone = request.getHeader("If-None-Match");
        if (etag.equals(ifNone)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        return false;
    }
}