package com.example.servlet;

import com.example.dto.ResultDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 实时消息推送 Servlet
 * 继承 BaseServlet，统一跨域、异常处理规范
 */
@WebServlet("/api/message/sse")
public class MessageSseServlet extends BaseServlet {
    // 日志组件（和你的项目统一）
    private static final Logger logger = LoggerFactory.getLogger(MessageSseServlet.class);

    // 全局存储：用户ID -> SSE输出流（所有在线用户连接）
    private static final Map<String, PrintWriter> SSE_CONNECTIONS = new ConcurrentHashMap<>();

    /**
     * SSE 仅支持 GET 请求（标准协议）
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 统一跨域配置（继承自 BaseServlet）
        setCorsHeader(req, resp);
        PrintWriter out = null;

        try {
            // ====================== SSE 固定响应头（必须配置） ======================
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader("Connection", "keep-alive");
            // 禁止缓存，保证实时推送
            resp.setHeader("X-Accel-Buffering", "no");

            // 获取前端传递的用户ID
            String userId = req.getParameter("userId");
            if (userId == null || userId.trim().isEmpty()) {
                logger.error("SSE连接失败：用户ID不能为空");
                resp.getWriter().write(ResultDTO.paramError("用户ID不能为空").toString());
                return;
            }

            // 获取输出流并保存连接
            out = resp.getWriter();
            SSE_CONNECTIONS.put(userId, out);
            logger.info("SSE连接建立成功，用户ID：{}", userId);

            // ====================== 保持长连接（核心） ======================
            // 监听客户端断开连接，自动清理资源
            while (true) {
                // 客户端断开连接时，checkError() 会返回 true
                if (out.checkError()) {
                    SSE_CONNECTIONS.remove(userId);
                    logger.info("SSE连接断开，用户ID：{}", userId);
                    break;
                }
                // 30秒心跳，防止连接超时
                Thread.sleep(30000);
            }

        } catch (NumberFormatException e) {
            logger.error("SSE参数格式错误", e);
            resp.getWriter().write(ResultDTO.paramError("参数格式错误").toString());
        } catch (InterruptedException e) {
            logger.error("SSE连接心跳异常", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("SSE连接处理异常", e);
            resp.getWriter().write(ResultDTO.fail("SSE连接异常：" + e.getMessage()).toString());
        }
    }

    /**
     * 【核心工具方法】
     * 给指定用户推送 JSON 格式的消息
     * 在 发消息Servlet 中直接调用
     * @param userId 接收消息的用户ID
     * @param messageJson 消息JSON字符串
     */
    public static void sendMessageToUser(String userId, String messageJson) {
        PrintWriter writer = SSE_CONNECTIONS.get(userId);
        if (writer != null) {
            // SSE 标准格式：data: 消息内容\n\n
            writer.write("data:" + messageJson + "\n\n");
            writer.flush();
            logger.info("SSE消息推送成功，用户ID：{}，消息内容：{}", userId, messageJson);
        } else {
            logger.warn("SSE消息推送失败，用户不在线：{}", userId);
        }
    }
}