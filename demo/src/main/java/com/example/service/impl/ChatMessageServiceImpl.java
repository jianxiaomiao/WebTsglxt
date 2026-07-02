package com.example.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.example.dao.ChatMessageDao;
import com.example.dao.ChatSessionDao;
import com.example.dto.ResultDTO;
import com.example.entity.ChatMessage;
import com.example.entity.ChatSession;
import com.example.service.ChatMessageService;
import com.example.servlet.MessageSseServlet;
import com.example.websocket.ChatWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessageServiceImpl implements ChatMessageService {
    // 新增：日志打印
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    private final ChatMessageDao chatMessageDao;
    private final ChatSessionDao chatSessionDao;

    public ChatMessageServiceImpl(ChatMessageDao chatMessageDao, ChatSessionDao chatSessionDao) {
        this.chatMessageDao = chatMessageDao;
        this.chatSessionDao = chatSessionDao;
    }

    @Override
    public ResultDTO<Long> sendMessage(ChatMessage message) {
        try {
            Long messageId = chatMessageDao.add(message);

            LocalDateTime now = LocalDateTime.now();
            String contentPreview;

            // 🔥 核心修复：根据消息类型生成正确的预览内容
            if (message.getMessageType() == 2) {
                // 图片消息 → 固定显示"[图片]"
                contentPreview = "[图片]";
            }
            else if (message.getMessageType() == 3) {
                // 图片消息 → 固定显示"[图片]"
                contentPreview = "[文件]";
            }
            else if (message.getMessageType() == 4) {
                // 图片消息 → 固定显示"[图片]"
                contentPreview = "[视频]";
            }
            else if (message.getMessageType() == 5) {
                // 图片消息 → 固定显示"[图片]"
                contentPreview = "[语音]";
            }else {
                // 文本消息 → 显示前50字
                contentPreview = message.getMessageContent().length() > 50
                        ? message.getMessageContent().substring(0, 50) + "..."
                        : message.getMessageContent();
            }

            // 如果是引用消息，添加前缀
            if (message.getReplyToId() != null) {
                contentPreview = "[引用消息] " + contentPreview;
            }

            // 更新会话逻辑不变...
            ChatSession mySession = chatSessionDao.queryByUserPair(message.getFromUserId(), message.getToUserId());
            if (mySession == null) {
                mySession = new ChatSession(message.getFromUserId(), message.getToUserId(), now);
            }
            mySession.setLastMessage(contentPreview);
            mySession.setLastMessageTime(message.getCreateTime());
            mySession.setUpdateTime(now);
            chatSessionDao.saveOrUpdate(mySession);

            ChatSession targetSession = chatSessionDao.queryByUserPair(message.getToUserId(), message.getFromUserId());
            if (targetSession == null) {
                targetSession = new ChatSession(message.getToUserId(), message.getFromUserId(), now);
                targetSession.setUnreadCount(1);
            } else {
                targetSession.setUnreadCount(targetSession.getUnreadCount() + 1);
            }
            targetSession.setLastMessage(contentPreview);
            targetSession.setLastMessageTime(message.getCreateTime());
            targetSession.setUpdateTime(now);
            chatSessionDao.saveOrUpdate(targetSession);

            pushMessageToUser(message);
            return ResultDTO.success(messageId);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("发送消息失败：" + e.getMessage());
        }
    }
    @Override
    // 2. 新增：撤回消息
    public ResultDTO<ChatMessage> recallMessage(Long messageId, String userId) {
        try {
            ChatMessage message = chatMessageDao.queryById(messageId);
            if (message == null) {
                return ResultDTO.fail("消息不存在");
            }
            if (!message.getFromUserId().equals(userId)) {
                return ResultDTO.fail("只能撤回自己发送的消息");
            }

            int affected = chatMessageDao.recallMessage(messageId, userId);
            if (affected == 0) {
                return ResultDTO.fail("撤回失败，消息已超过2分钟或不存在");
            }

            // 重新查询更新后的消息
            ChatMessage updatedMessage = chatMessageDao.queryById(messageId);

            // 🔥 核心修复：更新双方的会话最后一条消息
            LocalDateTime now = LocalDateTime.now();

            // 更新发送者的会话（显示"你撤回了一条消息"）
            ChatSession senderSession = chatSessionDao.queryByUserPair(message.getFromUserId(), message.getToUserId());
            if (senderSession != null) {
                senderSession.setLastMessage("你撤回了一条消息");
                senderSession.setLastMessageTime(now);
                senderSession.setUpdateTime(now);
                chatSessionDao.saveOrUpdate(senderSession);
            }

            // 更新接收者的会话（显示"对方撤回了一条消息"）
            ChatSession receiverSession = chatSessionDao.queryByUserPair(message.getToUserId(), message.getFromUserId());
            if (receiverSession != null) {
                receiverSession.setLastMessage("对方撤回了一条消息");
                receiverSession.setLastMessageTime(now);
                receiverSession.setUpdateTime(now);
                chatSessionDao.saveOrUpdate(receiverSession);
            }

            // 推送撤回通知给双方
            pushRecallMessageToUser(updatedMessage, message.getFromUserId());
            pushRecallMessageToUser(updatedMessage, message.getToUserId());

            return ResultDTO.success(updatedMessage);
        } catch (Exception e) {
            logger.error("撤回消息失败", e);
            return ResultDTO.fail("撤回失败：" + e.getMessage());
        }
    }
    // 新增：专门推送撤回消息的方法
    private void pushRecallMessageToUser(ChatMessage message, String targetUserId) {
        try {
            // 构造前端可识别的撤回消息格式
            Map<String, Object> pushMsg = new HashMap<>();
            pushMsg.put("type", "MESSAGE_RECALLED"); // 标记为撤回事件
            pushMsg.put("data", message); // 推送完整的更新后的消息

            // 调用SSE推送（你用的是SSE不是WebSocket）
            String jsonMsg = JSON.toJSONString(pushMsg);
            MessageSseServlet.sendMessageToUser(targetUserId, jsonMsg);
            logger.info("推送撤回通知成功，接收用户：{}", targetUserId);
        } catch (Exception e) {
            logger.error("推送撤回通知失败，接收用户：{}", targetUserId, e);
        }
    }
    @Override
    // 3. 新增：隐藏消息
    public ResultDTO<Void> hideMessage(Long messageId, String userId) {
        try {
            chatMessageDao.hideMessage(messageId, userId);
            return ResultDTO.success(null);
        } catch (Exception e) {
            logger.error("隐藏消息失败", e);
            return ResultDTO.fail("隐藏失败：" + e.getMessage());
        }
    }

    // ===================== 新增：推送消息给目标用户 =====================
    private void pushMessageToUser(ChatMessage message) {
        try {
            // 构造前端可识别的消息格式
            Map<String, Object> pushMsg = new HashMap<>();
            pushMsg.put("id", message.getId()); // 🔥 新增：推送消息ID，确保前端能正确匹配
            pushMsg.put("fromUserId", message.getFromUserId());
            pushMsg.put("toUserId", message.getToUserId());
            pushMsg.put("messageType", message.getMessageType());
            pushMsg.put("messageContent", message.getMessageContent());
            pushMsg.put("createTime", message.getCreateTime());
            pushMsg.put("replyToId", message.getReplyToId()); // 🔥 核心修复：推送引用消息ID
            pushMsg.put("isRecalled", message.getIsRecalled());
            pushMsg.put("originalContent", message.getOriginalContent());
            pushMsg.put("type", "NEW_MESSAGE"); // 标记为新消息

            String jsonMsg = JSON.toJSONString(pushMsg);
            MessageSseServlet.sendMessageToUser(message.getToUserId(), jsonMsg);
            logger.info("实时推送消息成功，接收用户：{}", message.getToUserId());
        } catch (Exception e) {
            logger.error("推送消息失败，接收用户：{}", message.getToUserId(), e);
        }
    }

    // ===================== 以下所有方法，完全保留你原有代码，无任何修改 =====================
    @Override
    public ResultDTO<List<ChatMessage>> getChatHistory(String userId1, String userId2, int page, int pageSize) {
        try {
            int offset = (page - 1) * pageSize;
            List<ChatMessage> messages = chatMessageDao.queryChatHistory(userId1, userId2, offset, pageSize);
            return ResultDTO.success(messages);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询聊天记录失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> markAsRead(String fromUserId, String toUserId) {
        try {
            // 1. 标记消息为已读
            chatMessageDao.markAsRead(fromUserId, toUserId);

            // 2. 更新会话的未读数为0
            ChatSession session = chatSessionDao.queryByUserPair(toUserId, fromUserId);
            if (session != null) {
                session.setUnreadCount(0);
                session.setUpdateTime(LocalDateTime.now());
                chatSessionDao.saveOrUpdate(session);
            }

            // 🔥 核心：推送已读通知给发送者（实时更新对方的Badge）
            pushMessageReadToUser(fromUserId, toUserId);

            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("标记已读失败：" + e.getMessage());
        }
    }

    // 🔥 新增：推送已读通知给发送者
    private void pushMessageReadToUser(String fromUserId, String toUserId) {
        try {
            // 构造已读通知消息
            Map<String, Object> pushMsg = new HashMap<>();
            pushMsg.put("type", "MESSAGE_READ"); // 标记为已读事件
            pushMsg.put("fromUserId", toUserId); // 标记已读的用户（接收方）
            pushMsg.put("toUserId", fromUserId); // 被标记已读的用户（发送方）

            String jsonMsg = JSON.toJSONString(pushMsg);
            // 推送给发送者，让他知道消息已被阅读
            MessageSseServlet.sendMessageToUser(fromUserId, jsonMsg);
            logger.info("推送已读通知成功，发送者：{}，接收者：{}", fromUserId, toUserId);
        } catch (Exception e) {
            logger.error("推送已读通知失败", e);
        }
    }

    @Override
    public ResultDTO<Integer> getUnreadCount(String userId) {
        try {
            int count = chatMessageDao.countUnread(userId);
            return ResultDTO.success(count);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询未读消息数失败：" + e.getMessage());
        }
    }
}