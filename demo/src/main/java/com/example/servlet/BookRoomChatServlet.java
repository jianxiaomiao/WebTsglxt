package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/book/room/chat")
public class BookRoomChatServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");

        try {
            Map<String, Object> body = JSON.parseObject(req.getInputStream(), Map.class);
            Integer roomId = Integer.parseInt(body.get("roomId").toString());
            String userId = body.get("userId").toString();
            String contentStr = body.get("content").toString();

            String msgPayload;

            // 🔥 智能载荷嗅探：如果传进来的 content 本身就是一段标准的 JSON 卡片（如 NOTE_CARD）
            if (contentStr.trim().startsWith("{") && contentStr.contains("\"NOTE_CARD\"")) {
                // 极其霸道：绝对不准套娃，原封不动直接转交 SSE 广播！
                msgPayload = contentStr;
            } else {
                // 普通纯文本聊天，才帮它穿上 CHAT 的外衣
                msgPayload = JSON.toJSONString(Map.of(
                        "type", "CHAT",
                        "userId", userId,
                        "content", contentStr,
                        "timestamp", System.currentTimeMillis()
                ));
            }

            // 触发 SSE 引擎，全房间广播！
            BookRoomSseServlet.broadcastToRoom(roomId, msgPayload);

            resp.getWriter().write(JSON.toJSONString(ResultDTO.success("发送成功")));
        } catch (Exception e) {
            resp.getWriter().write(JSON.toJSONString(ResultDTO.fail("发送失败: " + e.getMessage())));
        }
    }
}