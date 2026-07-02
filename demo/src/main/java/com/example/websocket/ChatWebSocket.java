package com.example.websocket;

import com.alibaba.fastjson.JSON;
import jakarta.websocket.*;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(
        value = "/chat/ws/{userId}",
        configurator = ChatWebSocket.CorsConfigurator.class
)
public class ChatWebSocket {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocket.class);
    private static final Map<String, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        logger.info("用户 {} 建立WebSocket连接，SessionID: {}", userId, session.getId());
        ONLINE_SESSIONS.put(userId, session);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String userId) {
        logger.info("收到用户 {} 的消息：{}", userId, message);
        Map<String, String> msg = JSON.parseObject(message, Map.class);
        String toUserId = msg.get("toUserId");

        if (ONLINE_SESSIONS.containsKey(toUserId)) {
            Session targetSession = ONLINE_SESSIONS.get(toUserId);
            try {
                targetSession.getBasicRemote().sendText(message);
                logger.info("消息已推送给用户 {}", toUserId);
            } catch (IOException e) {
                logger.error("推送消息失败", e);
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        logger.info("用户 {} 断开WebSocket连接", userId);
        ONLINE_SESSIONS.remove(userId);
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("userId") String userId) {
        logger.error("用户 {} WebSocket连接出错", userId, throwable);
    }

    // ====================== 你要求保留的 主动推送方法 ======================
    public static void sendToUser(String toUserId, String message) {
        if (ONLINE_SESSIONS.containsKey(toUserId)) {
            Session session = ONLINE_SESSIONS.get(toUserId);
            try {
                session.getBasicRemote().sendText(message);
                logger.info("主动推送消息给用户 {}", toUserId);
            } catch (IOException e) {
                logger.error("主动推送消息失败", e);
            }
        }
    }
    // ====================================================================

    // 跨域配置（Tomcat 原生支持，无报错）
    public static class CorsConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            response.getHeaders().put("Access-Control-Allow-Origin", List.of("*"));
            response.getHeaders().put("Access-Control-Allow-Methods", List.of("GET", "POST"));
            response.getHeaders().put("Access-Control-Allow-Headers", List.of("Content-Type"));
            response.getHeaders().put("Access-Control-Allow-Credentials", List.of("true"));
        }
    }
}