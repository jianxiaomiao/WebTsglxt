package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dto.ResultDTO;
import com.example.util.EmailUtil;
import com.example.util.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/user/sendEmail")
public class SendEmailServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(SendEmailServlet.class);
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // SendEmailServlet.doPost 里添加
        logger.info("发验证码 SessionID {} " , req.getSession().getId());

        // 替换 SendEmailServlet 里读流+解析的代码
        JSONObject json = JSON.parseObject(req.getInputStream(), JSONObject.class);
        String email = json.getString("email");

        // 邮箱格式校验
        if (email == null || email.isBlank() || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            resp.getWriter().write(JSON.toJSONString(ResultDTO.paramError("邮箱格式不正确")));
            return;
        }

        try {
            String code = EmailUtil.generateCode();
            EmailUtil.sendEmailCode(email, code);

            // 存入 Redis，5分钟有效期（300秒）
            String emailKey = email.trim().toLowerCase();
            RedisUtil.set("EMAIL_CODE_" + emailKey, code, 300);

            resp.getWriter().write(JSON.toJSONString(ResultDTO.success("验证码发送成功")));
        } catch (Exception e) {
            // 打印完整异常堆栈，方便定位问题
            e.printStackTrace();
            resp.getWriter().write(JSON.toJSONString(ResultDTO.fail("邮件发送失败，请稍后再试")));
        }
    }

    // 处理OPTIONS跨域预检请求（必加，前端Vue跨域必备）
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}