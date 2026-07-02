package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.AiChatDaoImpl;
import com.example.dao.impl.ChatMessageDaoImpl;
import com.example.dao.impl.ChatSessionDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.ChatMessage;
import com.example.entity.UserAiChat;
import com.example.service.AiChatService;
import com.example.service.ChatMessageService;
import com.example.service.impl.AiChatServiceImpl;
import com.example.service.impl.ChatMessageServiceImpl;
import com.example.util.AiRateLimiter;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/user/chat/message")
public class ChatMessageServlet extends BaseServlet {
    private ChatMessageDaoImpl chatMessageDao;
    private ChatSessionDaoImpl chatSessionDao;
    private ChatMessageService chatMessageService;

    private AiChatDaoImpl aiChatDao;
    private AiChatService aiChatService;
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServlet.class);

    @Override
    public void init() throws ServletException {
        chatMessageDao = new ChatMessageDaoImpl();
        chatSessionDao = new ChatSessionDaoImpl();
        chatMessageService = new ChatMessageServiceImpl(chatMessageDao, chatSessionDao);

        aiChatDao = new AiChatDaoImpl();
        aiChatService = new AiChatServiceImpl(aiChatDao);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");
        String userId = req.getParameter("userId");
        String targetUserId = req.getParameter("targetUserId");
        String pageStr = req.getParameter("page");
        String pageSizeStr = req.getParameter("pageSize");

        try {
            if ("history".equals(action)) {
                // 查询聊天记录
                int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
                int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 20;
                ResultDTO<List<ChatMessage>> result = chatMessageService.getChatHistory(userId, targetUserId, page, pageSize);
                out.write(JSON.toJSONString(result));
            } else if ("unreadCount".equals(action)) {
                // 查询未读消息总数
                ResultDTO<Integer> result = chatMessageService.getUnreadCount(userId);
                out.write(JSON.toJSONString(result));
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("参数格式错误")));
        } catch (Exception e) {
            logger.error("查询聊天消息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String action = req.getParameter("action");

        try {
            if ("send".equals(action)) {
                String fromUserId = req.getParameter("fromUserId");
                String toUserId = req.getParameter("toUserId");
                Integer messageType = Integer.parseInt(req.getParameter("messageType"));
                String messageContent = req.getParameter("messageContent");
                String replyToIdStr = req.getParameter("replyToId");

                ChatMessage message = new ChatMessage(fromUserId, toUserId, messageType, messageContent, LocalDateTime.now());
                if (replyToIdStr != null && !replyToIdStr.isEmpty()) {
                    message.setReplyToId(Long.parseLong(replyToIdStr));
                }

                ResultDTO<Long> result = chatMessageService.sendMessage(message);

                if (result.isSuccess()) {
                    String msgJson = JSON.toJSONString(message);
                    MessageSseServlet.sendMessageToUser(toUserId, msgJson);
                }

                out.write(JSON.toJSONString(result));
                UserBehaviorLogger.logAsync(fromUserId, 14, null, null, messageContent);
            }
            // ===================== 🔥 修正版：文生图发送消息 Servlet 分支 =====================
            else if ("generateImage".equals(action)) {
                String fromUserId = req.getParameter("fromUserId");
                String toUserId = req.getParameter("toUserId");

                String prompt = req.getParameter("prompt");
                String imagePath = req.getParameter("imagePath"); // 此时能正常接收到前端传来的 "[image]url1[image]url2"
                String strengthStr = req.getParameter("strength");
                Float strength = strengthStr != null ? Float.parseFloat(strengthStr) : 0.5f;

                if ((prompt == null || prompt.isBlank()) && (imagePath == null || imagePath.isBlank())) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("提示词和参考图不能同时为空")));
                    return;
                }

                // 🔥 每日限流：每用户每天最多生成 5 张 AI 图片
                if (!AiRateLimiter.tryAcquireDaily(fromUserId, 5)) {
                    long waitSec = AiRateLimiter.getDailyWaitSeconds(fromUserId, 5);
                    out.write(JSON.toJSONString(ResultDTO.fail("今日AI图片生成次数已用完（5张/天），请 " + waitSec + " 秒后再试")));
                    return;
                }

                // 调用文生图服务
                String imageUrl = aiChatService.generateImage(prompt, 0, imagePath, strength);
                if (imageUrl == null) {
                    out.write(JSON.toJSONString(ResultDTO.fail("图片生成失败")));
                    return;
                }

                // ✅ 修正1：安全拼接用户消息内容（去掉多余的 "[image]" 前缀，防止格式重复）
                String userMessageContent;
                Integer userMessageType = 1; // 默认纯文字
                if (imagePath != null && !imagePath.isBlank()) {
                    userMessageType = 2; // 图文混合
                    // 因为前端传来的 imagePath 已经是 "[image]url1..." 格式，直接拼接 [text] 即可
                    userMessageContent = imagePath + "[text]" + (prompt != null ? prompt : "");
                } else {
                    userMessageContent = "[text]" + (prompt != null ? prompt : "");
                }

                // ✅ 保存用户消息
                UserAiChat userPrompt = new UserAiChat(fromUserId, userMessageContent, userMessageType, LocalDateTime.now(), 0);
                aiChatDao.add(userPrompt);

                // 保存AI生成的图片消息
                UserAiChat aiImage = new UserAiChat(fromUserId, "[image]" + imageUrl + "[text]", 2, LocalDateTime.now(), 1);
                aiChatDao.add(aiImage);

                // ✅ SSE 推送及实时返回对象保持一致
                Map<String, Object> aiImageMessage = new HashMap<>();
                aiImageMessage.put("id", aiImage.getId());
                aiImageMessage.put("fromUserId", "doubao_ai");
                aiImageMessage.put("messageType", 2);
                aiImageMessage.put("formType", 1);
                aiImageMessage.put("messageContent", "[image]" + imageUrl + "[text]");
                aiImageMessage.put("createTime", LocalDateTime.now().toString());
                aiImageMessage.put("isNew", true);

                String msgJson = JSON.toJSONString(aiImageMessage);
                MessageSseServlet.sendMessageToUser(fromUserId, msgJson);

                out.write(JSON.toJSONString(ResultDTO.success(aiImageMessage)));
            }
            else if ("markRead".equals(action)) {
                String fromUserId = req.getParameter("fromUserId");
                String toUserId = req.getParameter("toUserId");
                ResultDTO<Void> result = chatMessageService.markAsRead(fromUserId, toUserId);
                out.write(JSON.toJSONString(result));
            } else if ("recall".equals(action)) {
                // 新增：撤回消息
                Long messageId = Long.parseLong(req.getParameter("messageId"));
                String userId = req.getParameter("userId");
                ResultDTO<ChatMessage> result = chatMessageService.recallMessage(messageId, userId);
                out.write(JSON.toJSONString(result));
                UserBehaviorLogger.logAsync(userId, 15, null, null, "撤回消息");
            } else if ("hide".equals(action)) {
                // 新增：隐藏消息
                Long messageId = Long.parseLong(req.getParameter("messageId"));
                String userId = req.getParameter("userId");
                ResultDTO<Void> result = chatMessageService.hideMessage(messageId, userId);
                out.write(JSON.toJSONString(result));
                UserBehaviorLogger.logAsync(userId, 16, null, null, "发送消息");
            } else {
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("参数格式错误")));
        } catch (Exception e) {
            logger.error("操作聊天消息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}