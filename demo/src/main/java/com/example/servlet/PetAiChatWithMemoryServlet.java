package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.*;
import com.example.dao.impl.*;
import com.example.dto.ResultDTO;
import com.example.entity.*;
import com.example.service.AiChatService;
import com.example.service.BookService;
import com.example.service.ChatMessageService;
import com.example.service.impl.AiChatServiceImpl;
import com.example.service.impl.BookServiceImpl;
import com.example.service.impl.ChatMessageServiceImpl;
import com.example.util.AiRateLimiter;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.config.AiConfig.*;

@WebServlet("/api/pet/ai/chat/history")
public class PetAiChatWithMemoryServlet extends BaseServlet {
    private final AiChatDao aiChatDao = new AiChatDaoImpl();
    private final AiChatService aiChatService = new AiChatServiceImpl(aiChatDao);

    private final UserInformationDao userDao = new UserInformationDaoImpl();

    private final BookInformationDao bookDao = new BookInformationDaoImpl();

    // 🔥 【新增这行】注入BookService（才能调用爬虫）
    private BookService bookService;
    private final BorrowInformationDao borrowDao = new BorrowInformationDaoImpl();

    private final UserCollectionDao collectionDao = new UserCollectionDaoImpl();

    private final UserTextCollectionDao noteDao = new UserTextCollectionDaoImpl();

    private final FriendDao friendDao = new FriendDaoImpl();

    private final AiOperationLogDao aiOperationLogDao = new AiOperationLogDaoImpl();

    // 新增业务DAO
    private final FriendRequestDao friendRequestDao = new FriendRequestDaoImpl();
    private final ChatMessageDao chatMessageDao = new ChatMessageDaoImpl();
    private final ChatSessionDao chatSessionDao = new ChatSessionDaoImpl();

    private static final Logger logger = LoggerFactory.getLogger(PetAiChatWithMemoryServlet.class);
    private static final String TOOL_PREFIX = "[[TOOL]]";

    // 添加到Servlet成员变量
    private static final ExecutorService TOOL_EXECUTOR = Executors.newFixedThreadPool(5);
    // 在Servlet成员变量里添加
    private final ChatMessageService chatMessageService = new ChatMessageServiceImpl(chatMessageDao, chatSessionDao);

    // 🔥 放在 DATE_FORMATTER 下面
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // 新增这个

    // ====================== 新增：用户读书喜好记忆缓存 ======================
    private static final Map<String, UserPreference> USER_PREFERENCE_MAP = new ConcurrentHashMap<>();

    private final Tika tika = new Tika();

    // 新增：偏好实体类（内部类）
    private static class UserPreference {
        String favoriteBooks;     // 喜欢的书
        String favoriteAuthors;   // 喜欢的作者
        String readStyle;         // 阅读风格（科幻/言情/推理等）

        public UserPreference() {
            this.favoriteBooks = "无";
            this.favoriteAuthors = "无";
            this.readStyle = "默认";
        }
    }
    @Override
    public void init() throws ServletException {
        this.bookService = new BookServiceImpl(bookDao);
    }

    // GET 获取AI聊天历史（分页版）
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String userId = req.getParameter("userId");
        String pageStr = req.getParameter("page");
        String pageSizeStr = req.getParameter("pageSize");

        try {
            // 默认参数
            int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
            int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 20;

            // 调用分页接口
            ResultDTO<List<UserAiChat>> result = aiChatService.getChatHistoryByPage(userId, page, pageSize);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("页码参数格式错误")));
        } catch (Exception e) {
            logger.error("获取AI聊天历史异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("获取历史失败")));
        } finally {
            out.flush();
            out.close();
        }
    }

    // ✅ 恢复正常 POST JSON 接口，不改动响应格式！
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String userId = null;
        try {
            byte[] bytes = req.getInputStream().readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            JSONObject params = JSON.parseObject(json);
            userId = params.getString("userId");
            String content = params.getString("content").trim();

            // 🔥 每日限流：每用户每天最多 20 次 AI 对话
            if (!AiRateLimiter.tryAcquireDaily(userId, 20)) {
                long waitSec = AiRateLimiter.getDailyWaitSeconds(userId, 20);
                out.write(JSON.toJSONString(ResultDTO.fail("今日AI对话次数已用完（20次/天），请 " + waitSec + " 秒后再试")));
                return;
            }

            // 🔥 新增：接收前端传递的场景参数
            String scene = params.getString("scene");
            // 默认值：如果前端没传，就用图书服务
            if (scene == null || scene.isEmpty()) scene = "default";

            Integer messageType = params.getInteger("messageType");
            if (messageType == null || messageType < 1 || messageType > 5) {
                messageType = 1; // 非法兜底默认文字
            }

            logger.info("===== 新聊天请求 =====");
            logger.info("用户ID：{}，用户问题：{}", userId, content);
            aiChatDao.add(new UserAiChat(userId, content, messageType, LocalDateTime.now(),0));
            extractUserPreference(userId, content);
            // ❌ 删除这行重复的初始推送
            // pushThinking(userId, "🤔 正在理解你的问题");

            // 🔥 把 scene 传给 handleChat
            String finalReply = handleChat(userId, content, scene, messageType);

            // 🔥 第二步：推送思考完成
            pushThinkingDone(userId);

            // 保存聊天记录
            aiChatDao.add(new UserAiChat(userId, finalReply, 1, LocalDateTime.now(),1));

            // ✅ 正常返回最终JSON结果
            out.write(JSON.toJSONString(ResultDTO.success(finalReply)));

            // 🔥 注入场景指令，强制AI只做当前场景的事
            switch(scene) {
                case "treehole" -> UserBehaviorLogger.logAsync(userId, 22, null, null, "树洞谈心" + content);
                case "game" -> UserBehaviorLogger.logAsync(userId, 23, null, null, "书籍游戏" + content);
                case "write" -> UserBehaviorLogger.logAsync(userId, 24, null, null, "文本优化" + content);
                case "book" -> UserBehaviorLogger.logAsync(userId, 25, null, null, "书籍业务处理" + content);
                case "file" ->UserBehaviorLogger.logAsync(userId, 26, null, null, "文件识别");
                default -> UserBehaviorLogger.logAsync(userId, 27, null, null, "默认对话"+ content);
            };
        } catch (Exception e) {
            logger.error("AI聊天异常", e);
            pushThinking(userId, "❌ 小宠物暂时休息啦~");
            out.write(JSON.toJSONString(ResultDTO.fail("小宠物暂时休息啦~")));
        } finally {
            out.flush();
            out.close();
        }
    }

    // ======================================
    // ✅ 调用你现成的 SSE 推送思考过程
    // ======================================
    private void pushThinking(String userId, String text) {
        // 直接调用你已有的SSE工具方法！
        MessageSseServlet.sendMessageToUser(userId, text);
    }

    private void pushThinkingAppend(String userId, String text) {
        JSONObject appendMsg = new JSONObject();
        appendMsg.put("type", "AI_THINKING_APPEND");
        appendMsg.put("content", text);
        MessageSseServlet.sendMessageToUser(userId, appendMsg.toJSONString());
    }

    // 思考完成时推送完成通知
    private void pushThinkingDone(String userId) {
        JSONObject doneMsg = new JSONObject();
        doneMsg.put("type", "AI_THINKING_DONE");
        MessageSseServlet.sendMessageToUser(userId, doneMsg.toJSONString());
    }

    // 🔥 新增：最大工具调用轮次，防止死循环
    private static final int MAX_TOOL_ROUNDS = 3;

    private String handleChat(String userId, String question, String scene, Integer messageType) throws Exception {
        List<UserAiChat> memory = aiChatDao.selectRecentMemory(userId, MEMORY_LIMIT);
        String currentInput = question;
        boolean isFirstRound = true;
        // 🔥 新增：记录已执行的工具调用历史，传给AI避免重复
        List<String> executedTools = new ArrayList<>();
        // 🔥 新增：记录最近的上下文实体（书籍ISBN、用户ID、借阅ID等）
        Map<String, Object> contextEntities = new HashMap<>();

        // 🔥 把无限循环改成有限循环，最多3轮
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            // 阶段1：推送思考状态
            if (isFirstRound) {
                if (messageType == 2) {
                    pushThinking(userId, "🤔 正在识别图片内容...");
                } else if(messageType == 3){
                    pushThinking(userId, "🤔 正在识别文件内容，请稍候...");
                }
                else if(messageType == 4){
                    pushThinking(userId, "🤔 正在识别视频内容，请稍候...");
                }else{
                    pushThinking(userId, "🤔 正在分析你的问题...");
                }
                isFirstRound = false;
            } else {
                pushThinkingAppend(userId, "\n\n🤔 正在分析查询结果...");
            }

            // 🔥 注入场景指令，强制AI只做当前场景的事
            String scenePrompt = switch(scene) {
                case "treehole" -> "【当前模式：读书树洞】现在请切换到温柔倾听模式，只陪用户谈心、共情、聊读书感悟，做用户的知心朋友，不要调用任何工具！";
                case "game" -> "【当前模式：书籍小游戏】现在请立刻开启猜书名/猜人物/答题游戏模式，陪用户玩有趣的书籍知识小游戏，不要调用任何工具！";
                case "write" -> "【当前模式：话术润色】现在请切换到文案优化大师模式，只专注于优化用户提供的文案，支持幽默/温柔/正式三种风格，直接返回润色结果，不要调用任何工具！";
                case "book" -> "【当前模式：图书服务】现在请切换到专业图书管理助手模式，根据用户需求正确调用对应的工具完成借书/查书/笔记/好友等操作！";
                case "file" -> "【当前模式：文件识别】现在请专注于分析用户上传的文件内容，进行总结、提取关键信息、回答文件相关问题，不要调用无关工具！";
                default -> "【当前模式：智能助手】请根据用户的需求，灵活切换到合适的模式为用户提供帮助。如果用户需要图书相关服务，请使用图书服务模式；如果用户只是想聊天、玩游戏或润色文案，请直接友好回复，不要调用任何工具！";
            };

            // 🔥 传给AI的输入：加上已执行工具历史+上下文实体
            String userPreference = getUserPreferenceInfo(userId);
            String enhancedInput =
                    // 1. 前置：用户偏好 + 场景（系统指令，不影响图片）
                    "【用户专属偏好】：" + userPreference + "\n" + scenePrompt + "\n\n" +
                            // 2. 中间：工具执行上下文
                            buildEnhancedInput(currentInput, executedTools, contextEntities);
            String aiRes = callAiBase(enhancedInput, memory, messageType, scene);
            logger.info("----- AI 原始回复（第{}轮）-----", round + 1);
            logger.info(aiRes);

            // 清理不可见控制字符
            aiRes = cleanInvisibleChars(aiRes);

            // 识别工具调用
            List<String> toolCalls = new ArrayList<>();
            String[] parts = aiRes.split("\\[\\[TOOL\\]\\]");
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                int newlineIndex = part.indexOf("\n");
                String jsonStr = newlineIndex > 0 ? part.substring(0, newlineIndex).trim() : part.trim();
                if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                    toolCalls.add(jsonStr);
                }
            }

            // 没有工具调用，直接返回最终回复
            if (toolCalls.isEmpty()) {
                pushThinkingAppend(userId, "\n\n🤔 正在为你生成回复...");
                return aiRes;
            }

            // 🔥 记录已执行的工具，避免重复
            for (String toolJson : toolCalls) {
                try {
                    JSONObject toolObj = JSON.parseObject(toolJson);
                    executedTools.add(toolObj.getString("func"));
                } catch (Exception e) {
                    logger.warn("记录工具调用失败", e);
                }
            }

            // 阶段2：推送正在执行的工具
            List<String> toolDescs = new ArrayList<>();
            for (String toolJson : toolCalls) {
                try {
                    JSONObject toolObj = JSON.parseObject(toolJson);
                    String funcName = toolObj.getString("func");
                    toolDescs.add(getToolDesc(funcName));
                } catch (Exception e) {
                    toolDescs.add("数据查询");
                }
            }
            pushThinkingAppend(userId, "\n\n🔍 正在执行：" + String.join("、", toolDescs));

            // 并行执行所有工具
            List<Future<String>> futures = new ArrayList<>();
            for (String toolJson : toolCalls) {
                futures.add(TOOL_EXECUTOR.submit(() -> {
                    try {
                        JSONObject toolObj = JSON.parseObject(toolJson);
                        String funcName = toolObj.getString("func");
                        JSONObject args = toolObj.getJSONObject("params");
                        // 🔥 自动补全参数：从上下文实体里拿缺失的参数
                        JSONObject enhancedArgs = autoCompleteArgs(funcName, args, contextEntities);
                        logger.info("执行工具：{}，参数：{}", funcName, enhancedArgs);
                        return executeTool(funcName, enhancedArgs, userId);
                    } catch (Exception e) {
                        logger.error("单个工具执行失败，JSON：{}", toolJson, e);
                        return "工具执行失败";
                    }
                }));
            }

            // 等待所有工具执行完成
            StringBuilder allResults = new StringBuilder();
            for (int i = 0; i < futures.size(); i++) {
                try {
                    allResults.append("工具").append(i+1).append("结果：\n")
                            .append(futures.get(i).get())
                            .append("\n\n");
                } catch (Exception e) {
                    logger.error("获取工具结果失败", e);
                    allResults.append("工具").append(i+1).append("结果：执行失败\n\n");
                }
            }

            // 🔥 从工具结果里提取上下文实体，更新到contextEntities
            extractEntitiesFromResults(allResults.toString(), contextEntities);

            logger.info("-----所有工具执行完成，总结果：\n{}", allResults);

            // 阶段3：推送完整工具结果
            pushThinkingAppend(userId, "\n\n✅ 工具执行完成，查询结果如下：\n\n" + allResults.toString());

            // 把结果喂给AI，进入下一轮思考
            currentInput = "以下是所有查询结果，请根据这些数据，用可爱的语气整理回答：\n" + allResults;
        }

        // 3轮后仍未得到结果，返回默认提示
        pushThinkingAppend(userId, "\n\n🤔 正在为你生成回复...");
        return "😥 小宠物暂时有点忙，你可以换个问题试试哦~";
    }

    // ====================== 新增：获取用户偏好信息 ======================
    private String getUserPreferenceInfo(String userId) {
        UserPreference pref = USER_PREFERENCE_MAP.computeIfAbsent(userId, k -> new UserPreference());
        return "喜欢的书：" + pref.favoriteBooks +
                " | 喜欢的作者：" + pref.favoriteAuthors +
                " | 阅读风格：" + pref.readStyle;
    }

    // ====================== 新增：自动提取用户偏好（从对话里学习） ======================
    private void extractUserPreference(String userId, String content) {
        UserPreference pref = USER_PREFERENCE_MAP.computeIfAbsent(userId, k -> new UserPreference());
        // 简单关键词提取（AI会自动理解，这里做辅助增强）
        if (content.contains("喜欢") || content.contains("爱看") || content.contains("最爱")) {
            if (content.contains("《")) {
                int start = content.indexOf("《");
                int end = content.indexOf("》");
                if (start > 0 && end > start) {
                    String book = content.substring(start, end + 1);
                    pref.favoriteBooks = book;
                }
            }
        }
    }
    // ===================== 【核心1】上下文实体提取（适配新场景正则） =====================
    private void extractEntitiesFromResults(String results, Map<String, Object> contextEntities) {
        // 1. 提取书籍ISBN
        Pattern isbnPattern = Pattern.compile("ISBN：(\\d{13})");
        Matcher isbnMatcher = isbnPattern.matcher(results);
        if (isbnMatcher.find()) {
            contextEntities.put("lastBookISBN", isbnMatcher.group(1));
        }

        // 2. 提取用户ID / 用户名
        Pattern userIdPattern = Pattern.compile("\\{user:(\\w+)\\}");
        Matcher userIdMatcher = userIdPattern.matcher(results);
        if (userIdMatcher.find()) {
            contextEntities.put("lastUserId", userIdMatcher.group(1));
        }
        Pattern userNamePattern = Pattern.compile("@(\\w+)\\(");
        Matcher userNameMatcher = userNamePattern.matcher(results);
        if (userNameMatcher.find()) {
            contextEntities.put("lastUserName", userNameMatcher.group(1));
        }

        // 3. 提取借阅ID
        Pattern borrowIdPattern = Pattern.compile("借阅ID：(\\d+)");
        Matcher borrowIdMatcher = borrowIdPattern.matcher(results);
        if (borrowIdMatcher.find()) {
            contextEntities.put("lastBorrowId", Integer.parseInt(borrowIdMatcher.group(1)));
        }

        // 4. 提取收藏ID
        Pattern collectIdPattern = Pattern.compile("收藏ID：(\\d+)");
        Matcher collectIdMatcher = collectIdPattern.matcher(results);
        if (collectIdMatcher.find()) {
            contextEntities.put("lastCollectId", Integer.parseInt(collectIdMatcher.group(1)));
        }

        // 5. 提取好友申请ID
        Pattern requestIdPattern = Pattern.compile("申请ID：(\\d+)");
        Matcher requestIdMatcher = requestIdPattern.matcher(results);
        if (requestIdMatcher.find()) {
            contextEntities.put("lastRequestId", Integer.parseInt(requestIdMatcher.group(1)));
        }

        // 6. 提取笔记ID
        Pattern noteIdPattern = Pattern.compile("\\{note:(\\d+)\\}");
        Matcher noteIdMatcher = noteIdPattern.matcher(results);
        if (noteIdMatcher.find()) {
            contextEntities.put("lastNoteId", Integer.parseInt(noteIdMatcher.group(1)));
        }

        // ==============================================
        // 🔥 解析好友列表格式 【序号】 @用户名(用户ID) + 名称脱敏
        // ==============================================
        Pattern friendItemPattern = Pattern.compile("【(\\d+)】 @([^()]+)\\((\\w+)\\)");
        Matcher friendMatcher = friendItemPattern.matcher(results);

        Map<String, String> friendNameToId = new HashMap<>();  // 纯文本用户名 -> ID（已脱敏）
        Map<Integer, String> friendIndexToId = new HashMap<>();// 序号 -> ID

        while (friendMatcher.find()) {
            String indexStr = friendMatcher.group(1);
            String rawFriendName = friendMatcher.group(2).trim(); // 原始名称（带表情）
            String friendUid = friendMatcher.group(3).trim();

            // 关键：名称脱敏 → 去除表情，生成纯文本Key
            String pureName = removeEmoji(rawFriendName);
            friendNameToId.put(pureName, friendUid);

            // 解析序号
            try {
                int index = Integer.parseInt(indexStr);
                friendIndexToId.put(index, friendUid);
            } catch (NumberFormatException e) {
                // 忽略序号解析异常
            }
        }

        // 将脱敏后的映射表存入上下文
        if (!friendNameToId.isEmpty()) {
            contextEntities.put("friendNameMap", friendNameToId);
        }
        if (!friendIndexToId.isEmpty()) {
            contextEntities.put("friendIndexMap", friendIndexToId);
        }
    }

    // ===================== 【核心2】自动参数补全（复用上下文） =====================
    private JSONObject autoCompleteArgs(String funcName, JSONObject args, Map<String, Object> contextEntities) {
        JSONObject enhancedArgs = new JSONObject(args);
        // 借阅：自动补ISBN
        if ("borrowBook".equals(funcName) && !enhancedArgs.containsKey("isbn") && contextEntities.containsKey("lastBookISBN")) {
            enhancedArgs.put("isbn", contextEntities.get("lastBookISBN"));
        }
        // 归还书籍：自动补borrowId
        if ("returnBook".equals(funcName) && !enhancedArgs.containsKey("borrowId") && contextEntities.containsKey("lastBorrowId")) {
            enhancedArgs.put("borrowId", contextEntities.get("lastBorrowId"));
        }
        // 取消收藏：自动补collectionId
        if ("cancelCollectBook".equals(funcName) && !enhancedArgs.containsKey("collectionId") && contextEntities.containsKey("lastCollectId")) {
            enhancedArgs.put("collectionId", contextEntities.get("lastCollectId"));
        }
        // 接受/拒绝好友申请：自动补requestId
        if (("acceptFriendRequest".equals(funcName) || "rejectFriendRequest".equals(funcName))
                && !enhancedArgs.containsKey("requestId") && contextEntities.containsKey("lastRequestId")) {
            enhancedArgs.put("requestId", contextEntities.get("lastRequestId"));
        }
        // ==============================================
        // 🔥 sendMessage：名称脱敏后匹配映射表，优先注入精准ID
        // ==============================================
        if ("sendMessage".equals(funcName)) {
            Map<String, String> nameMap = (Map<String, String>) contextEntities.get("friendNameMap");
            Map<Integer, String> indexMap = (Map<Integer, String>) contextEntities.get("friendIndexMap");

            String rawTargetName = enhancedArgs.getString("userName");
            // 入参名称脱敏（去除表情）
            String pureTargetName = removeEmoji(rawTargetName);

            // 情况1：根据脱敏后的用户名匹配ID
            if (pureTargetName != null && nameMap != null && nameMap.containsKey(pureTargetName)) {
                enhancedArgs.put("targetUserId", nameMap.get(pureTargetName));
            }
            // 情况2：根据序号匹配ID（如：第5位好友）
            Integer index = enhancedArgs.getInteger("index");
            if (index != null && indexMap != null && indexMap.containsKey(index)) {
                enhancedArgs.put("targetUserId", indexMap.get(index));
            }
        }
        // 收藏书籍：自动补ISBN
        if ("collectBook".equals(funcName) && !enhancedArgs.containsKey("isbn") && contextEntities.containsKey("lastBookISBN")) {
            enhancedArgs.put("isbn", contextEntities.get("lastBookISBN"));
        }
        return enhancedArgs;
    }

    // 🔥 构建增强版输入：加上已执行工具历史+上下文实体
    private String buildEnhancedInput(String originalInput, List<String> executedTools, Map<String, Object> contextEntities) {
        StringBuilder sb = new StringBuilder();
        sb.append("【已执行的工具历史】：").append(JSON.toJSONString(executedTools)).append("\n");
        sb.append("【最近上下文实体】：").append(JSON.toJSONString(contextEntities)).append("\n");
        sb.append("【用户问题】：").append(originalInput);
        return sb.toString();
    }

    // 最终正确代码：仅删除看不见的ASCII乱码控制符，Emoji、颜文字、换行\n\r、所有符号全部放行
    private String cleanInvisibleChars(String str) {
        if (str == null) return "";
    /*
    黑名单：只剔除无用不可见控制字符（\0、退格、换页等乱码）
    \x00-\x08,\x0B,\x0C,\x0E-\x1F = 需要删除的无效控制字符
    \t(\x09) \n(\x0A) \r(\x0D) 全部保留不删除
    除此以外：Emoji、颜文字、日文符号、特殊标点、中英文、换行全部原样保留
    */
        return str.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    // ===================== 【重点】工具执行器：删除所有评论分支 + 统一参数校验 =====================
    private String executeTool(String funcName, JSONObject args, String userId) {
        try {
            return switch (funcName) {
                // 书籍&查询基础工具
                case "queryBook" -> {
                    String bookName = args.getString("bookName");
                    if (bookName == null || bookName.isBlank()) {
                        yield "🤔 你想查询哪本书呀？告诉我书名或者ISBN就可以啦~";
                    }
                    yield queryBook(bookName);
                }
                case "queryMyNote" -> queryMyNote(userId);
                // 🔥 替换原来的queryMyBorrowAndCollect
                case "queryMyCollection" -> queryMyCollection(userId);
                case "queryMyFriend" -> queryMyFriend(userId);
                case "queryUserByName" -> {
                    String userName = args.getString("userName");
                    if (userName == null || userName.isBlank()) {
                        yield "🤔 你想查找哪个用户呀？告诉我用户名就可以啦~";
                    }
                    yield queryUserByName(userName);
                }

                // 好友申请/消息
                case "acceptFriendRequest" -> {
                    String requestId = args.getString("requestId");
                    if (requestId == null || requestId.isBlank()) {
                        yield "🤔 你想接受哪条好友申请呀？告诉我申请ID就可以啦~";
                    }
                    yield acceptFriendRequest(args, userId);
                }
                case "rejectFriendRequest" -> {
                    String requestId = args.getString("requestId");
                    if (requestId == null || requestId.isBlank()) {
                        yield "🤔 你想拒绝哪条好友申请呀？告诉我申请ID就可以啦~";
                    }
                    yield rejectFriendRequest(args, userId);
                }
                case "queryReceivedFriendRequests" -> queryReceivedFriendRequests(userId);
                case "sendMessage" -> {
                    String userName = args.getString("userName");
                    String content = args.getString("content");
                    if (userName == null || userName.isBlank()) {
                        yield "🤔 你想给哪位好友发消息呀？告诉我用户名就可以啦~";
                    }
                    if (content == null || content.isBlank()) {
                        yield "🤔 你想发送什么消息内容呀？告诉我消息就可以啦~";
                    }
                    yield sendMessage(args, userId);
                }

                case "collectBook" -> {
                    String isbn = args.getString("isbn");
                    if (isbn == null || isbn.isBlank()) {
                        yield "🤔 你想收藏哪本书呀？告诉我ISBN就可以啦~";
                    }
                    yield collectBook(args, userId);
                }
                case "cancelCollectBook" -> {
                    String collectionId = args.getString("collectionId");
                    if (collectionId == null || collectionId.isBlank()) {
                        yield "🤔 你想取消哪本书的收藏呀？告诉我收藏ID就可以啦~";
                    }
                    yield cancelCollectBook(args, userId);
                }

                // 笔记&阅读进度
                case "deleteNote" -> {
                    String noteId = args.getString("noteId");
                    if (noteId == null || noteId.isBlank()) {
                        yield "🤔 你想删除哪条笔记呀？告诉我笔记ID就可以啦~";
                    }
                    yield deleteNote(args, userId);
                }

                // 未知工具
                default -> "未知工具：" + funcName;
            };
        } catch (Exception e) {
            logger.error("工具执行失败：{}", funcName, e);
            return "工具执行失败：" + funcName;
        }
    }

    // ===================== 【重点】工具描述：删除所有评论相关描述 =====================
    private String getToolDesc(String funcName) {
        return switch (funcName) {
            case "queryBook" -> "查询书籍信息";
            case "queryUserByName" -> "查找用户信息";
            case "queryMyNote" -> "读取你的笔记";
            // 🔥 更新工具描述
            case "queryMyBorrow" -> "查询你的借阅记录";
            case "queryMyCollection" -> "查询你的收藏列表";
            case "queryMyFriend" -> "加载你的好友列表";
            case "sendFriendRequest" -> "发送好友申请";
            case "acceptFriendRequest" -> "接受好友申请";
            case "rejectFriendRequest" -> "拒绝好友申请";
            case "queryReceivedFriendRequests" -> "查询收到的好友申请";
            case "sendMessage" -> "发送私信";
            case "borrowBook" -> "借阅书籍";
            case "returnBook" -> "归还书籍";
            case "collectBook" -> "收藏书籍";
            case "cancelCollectBook" -> "取消收藏";
            case "addNote" -> "添加读书笔记";
            case "deleteNote" -> "删除读书笔记";
            case "updateReadProgress" -> "更新阅读进度";
            default -> "数据处理中";
        };
    }

    // ======================================
    // AI 基础调用（不变）
    // ======================================

    // ======================================
    // 🔥 新增：文件处理工具方法（适配Doubao-Seed-2.0-lite）
    // ======================================
    /**
     * 提取文件中的纯文本内容
     * @param filePath 本地文件绝对路径
     * @return 提取后的纯文本，失败返回null
     */
    private String extractTextFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                logger.error("文件不存在或不可读：{}", filePath);
                return null;
            }

            // 限制单文件最大提取文本长度（防止token爆炸）
            String text = tika.parseToString(file);
            if (text.length() > 15000) { // Seed-2.0-lite上下文窗口约16k token
                text = text.substring(0, 15000) + "\n\n⚠️ 文件内容过长，已截取前15000字";
            }

            logger.info("文件文本提取成功，长度：{}字符", text.length());
            return text;
        } catch (IOException | TikaException  e) {
            logger.error("文件文本提取失败：{}", filePath, e);
            return null;
        }
    }
    /**
     * 解析文件消息内容
     * @param content 前端发送的消息内容（格式：文字|||文件路径|||文件名|||文件大小 或 纯文件路径|||文件名|||大小）
     * @return 数组：[0]=文字内容，[1]=文件路径，[2]=文件名，[3]=文件大小（没有则为null）
     */
    private String[] parseFileMessage(String content) {
        String[] result = new String[4];
        String[] parts = content.split("\\|\\|\\|");

        if (parts.length == 4) {
            // 格式：文字|||路径|||名称|||大小
            result[0] = parts[0].trim();
            result[1] = parts[1].trim();
            result[2] = parts[2].trim();
            result[3] = parts[3].trim();
        } else if (parts.length == 3) {
            // 格式：纯文件 路径|||名称|||大小
            result[0] = "";
            result[1] = parts[0].trim();
            result[2] = parts[1].trim();
            result[3] = parts[2].trim();
        } else {
            // 异常兜底
            result[0] = content.trim();
            result[1] = null;
            result[2] = null;
            result[3] = null;
        }
        return result;
    }

    /**
     * 读取本地文件，转换为base64编码（支持Doubao支持的所有格式）
     * @param filePath 数据库存储的文件相对路径（如：/chat_files/xxx.docx）
     * @return base64编码的文件字符串（包含data:xxx;base64,前缀）
     */
    private String fileToBase64(String filePath) {
        try {
            // ✅ 适配你的真实存储路径：D:/WebTsglxt + 数据库路径
            String fullPath = "D:/WebTsglxt" + filePath;
            File file = new File(fullPath);

            // 等待文件写入完成（防止上传后立即读取失败）
            int retryCount = 0;
            final int MAX_RETRY = 15;
            final long WAIT_TIME = 200;
            while (retryCount < MAX_RETRY && (!file.exists() || !file.canRead())) {
                logger.warn("文件尚未可读，等待{}ms重试，路径：{}", WAIT_TIME, fullPath);
                Thread.sleep(WAIT_TIME);
                file = new File(fullPath);
                retryCount++;
            }

            if (!file.exists() || !file.isFile()) {
                logger.error("文件最终不可读：{}", fullPath);
                throw new FileNotFoundException("文件不可读：" + fullPath);
            }

            // 🔥 Doubao-Seed-2.0-lite 单文件最大支持100MB
            if (file.length() > 100 * 1024 * 1024) {
                logger.error("文件过大：{}，大小：{}字节", fullPath, file.length());
                return null;
            }

            // 读取文件并转换为base64
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 自动识别文件MIME类型（Doubao要求必须正确）
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                // 根据后缀兜底
                String suffix = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
                contentType = switch (suffix) {
                    case "pdf" -> "application/pdf";
                    case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "doc" -> "application/msword";
                    case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    case "xls" -> "application/vnd.ms-excel";
                    case "txt" -> "text/plain";
                    case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    case "ppt" -> "application/vnd.ms-powerpoint";
                    default -> "application/octet-stream";
                };
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            logger.error("文件转换为base64失败：{}", filePath, e);
            return null;
        }
    }

    /**
     * 判断文件类型是否被Doubao支持
     */
    private boolean isSupportedFileType(String filePath) {
        if (filePath == null) return false;
        String suffix = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        // Doubao-Seed-2.0-lite 支持的文件格式
        Set<String> supportedTypes = Set.of(
                "pdf", "docx", "doc", "xlsx", "xls", "txt", "pptx", "ppt",
                "md", "csv", "json", "xml", "html"
        );
        return supportedTypes.contains(suffix);
    }

    // ======================================
    // 🔥 多模态图片处理工具方法（适配你的真实存储路径）
    // ======================================
    // ====================== 🔥 批量多模态解析工具（适配你的格式） ======================
    /**
     * 批量解析图片消息 [image]url[image]url[text]文字
     */
    private List<Map<String, String>> parseBatchImages(String content) {
        List<Map<String, String>> list = new ArrayList<>();
        if (!content.contains("[image]")) return list;

        String[] parts = content.split("\\[text\\]");
        String imagePart = parts[0];

        // 提取所有图片
        String[] urls = imagePart.split("\\[image\\]");
        // 🔥 修正：urls[0] 是 [image] 标签前的内容（即用户偏好和场景指令）
        String prefixPrompt = urls[0];
        String userQuestionAndHistory = parts.length > 1 ? parts[1].trim() : "";
        // 🔥 重新拼接：把偏好/指令和用户问题合并，防止 AI 丢失偏好和模式
        String fullTextForAi = prefixPrompt + "\n" + userQuestionAndHistory;

        // 🔥 关键：从索引 1 开始循环，彻底跳过 urls[0] 的指令污染
        for (int i = 1; i < urls.length; i++) {
            String url = urls[i].trim();
            if (url.isEmpty()) continue;
            Map<String, String> map = new HashMap<>();
            map.put("url", url);
            map.put("text", fullTextForAi);
            list.add(map);
        }
        return list;
    }

    /**
     * 批量解析文件消息 [document]url|||name|||size[document]url[text]文字
     */
    private List<Map<String, String>> parseBatchFiles(String content) {
        List<Map<String, String>> list = new ArrayList<>();
        if (!content.contains("[document]")) return list;

        String[] parts = content.split("\\[text\\]");
        String filePart = parts[0];

        String[] items = filePart.split("\\[document\\]");
        // 🔥 同样跳过 items[0] 并回收前缀指令
        String prefixPrompt = items[0];
        String userQuestionAndHistory = parts.length > 1 ? parts[1].trim() : "";
        String fullTextForAi = prefixPrompt + "\n" + userQuestionAndHistory;

        for (int i = 1; i < items.length; i++) {
            String item = items[i].trim();
            if (item.isEmpty()) continue;
            String[] info = item.split("\\|\\|\\|");
            Map<String, String> map = new HashMap<>();
            map.put("url", info[0].trim());
            map.put("name", info.length > 1 ? info[1].trim() : "文件");
            map.put("size", info.length > 2 ? info[2].trim() : "");
            map.put("text", fullTextForAi);
            list.add(map);
        }
        return list;
    }

    /**
     * 批量解析视频消息 [video]url[video]url[text]文字
     */
    private List<Map<String, String>> parseBatchVideos(String content) {
        List<Map<String, String>> list = new ArrayList<>();
        if (!content.contains("[video]")) return list;

        String[] parts = content.split("\\[text\\]");
        String videoPart = parts[0];

        String[] urls = videoPart.split("\\[video\\]");
        // 🔥 同样跳过 urls[0] 并回收前缀指令
        String prefixPrompt = urls[0];
        String userQuestionAndHistory = parts.length > 1 ? parts[1].trim() : "";
        String fullTextForAi = prefixPrompt + "\n" + userQuestionAndHistory;

        for (int i = 1; i < urls.length; i++) {
            String url = urls[i].trim();
            if (url.isEmpty()) continue;
            Map<String, String> map = new HashMap<>();
            map.put("url", url);
            map.put("text", fullTextForAi);
            list.add(map);
        }
        return list;
    }

    /**
     * 批量解析语音消息 [voice]url|||duration[text]文字
     */
    private List<Map<String, String>> parseBatchVoices(String content) {
        List<Map<String, String>> list = new ArrayList<>();
        if (!content.contains("[voice]")) return list;

        String[] parts = content.split("\\[text\\]");
        String voicePart = parts[0];

        String[] items = voicePart.split("\\[voice\\]");
        // 🔥 同样跳过 items[0] 并回收前缀指令
        String prefixPrompt = items[0];
        String userQuestionAndHistory = parts.length > 1 ? parts[1].trim() : "";
        String fullTextForAi = prefixPrompt + "\n" + userQuestionAndHistory;

        for (int i = 1; i < items.length; i++) {
            String item = items[i].trim();
            if (item.isEmpty()) continue;
            String[] info = item.split("\\|\\|\\|");
            Map<String, String> map = new HashMap<>();
            map.put("url", info[0].trim());
            map.put("duration", info.length > 1 ? info[1].trim() : "0");
            map.put("text", fullTextForAi);
            list.add(map);
        }
        return list;
    }

    /**
     * 🔥 通用：本地媒体文件转 Base64（支持图片、视频、语音、文件，无代理版）
     * @param mediaPath 数据库存储的相对路径（如：/videos/xxx.mp4 /voices/xxx.mp3）
     * @param maxSizeMB 最大文件大小(MB)
     * @return 带前缀的 Base64 字符串，失败返回 null
     */
    private String mediaToBase64(String mediaPath, int maxSizeMB) {
        try {
            // 🔥 固定你的本地存储根路径（和图片/文件完全一致）
            String fullPath = "D:/WebTsglxt" + mediaPath;
            File file = new File(fullPath);

            // 等待文件可读（重试机制）
            int retryCount = 0;
            final int MAX_RETRY = 15;
            final long WAIT_TIME = 200;
            while (retryCount < MAX_RETRY && (!file.exists() || !file.canRead())) {
                logger.warn("媒体文件等待可读：{}ms，路径：{}", WAIT_TIME, fullPath);
                Thread.sleep(WAIT_TIME);
                retryCount++;
            }

            if (!file.exists() || !file.isFile()) {
                logger.error("媒体文件不存在：{}", fullPath);
                return null;
            }

            // 大小限制
            long maxSize = (long) maxSizeMB * 1024 * 1024;
            if (file.length() > maxSize) {
                logger.error("媒体文件过大(>{}MB)：{}", maxSizeMB, fullPath);
                return null;
            }

            // 读取并转 Base64
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 自动识别 MIME 类型
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                // 兜底格式
                String suffix = mediaPath.substring(mediaPath.lastIndexOf(".") + 1).toLowerCase();
                contentType = switch (suffix) {
                    case "mp4", "mov", "avi" -> "video/mp4";
                    case "mp3", "wav", "amr" -> "audio/mpeg";
                    case "jpg", "jpeg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> "application/octet-stream";
                };
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            logger.error("媒体文件转Base64失败：{}", mediaPath, e);
            return null;
        }
    }
    /**
     * 读取本地图片文件，转换为base64编码
     * @param imagePath 数据库存储的图片相对路径（如：/forum_images/xxx.jpg）
     * @return base64编码的图片字符串（包含data:image/jpeg;base64,前缀）
     */
    private String imageToBase64(String imagePath) {
        try {
            // ✅ 适配你的真实存储路径：D:/WebTsglxt + 数据库路径
            String fullPath = "D:/WebTsglxt" + imagePath;
            File file = new File(fullPath);

            int retryCount = 0;
            final int MAX_RETRY = 15;
            final long WAIT_TIME = 200;

            // 同时判断：存在 + 可读
            while (retryCount < MAX_RETRY && (!file.exists() || !file.canRead())) {
                logger.warn("图片尚未可读，等待{}ms重试，路径：{}", WAIT_TIME, fullPath);
                Thread.sleep(WAIT_TIME);
                file = new File(fullPath);
                retryCount++;
            }

            if (!file.exists() || !file.isFile()) {
                logger.error("图片文件最终不可读：{}", fullPath);
                throw new FileNotFoundException("图片文件不可读：" + fullPath);
            }

            // 检查文件大小（Doubao限制单张图片不超过10MB）
            if (file.length() > 10 * 1024 * 1024) {
                logger.error("图片文件过大：{}，大小：{}字节", fullPath, file.length());
                return null;
            }

            // 读取文件并转换为base64
            byte[] bytes = Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 自动识别图片类型
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg"; // 默认值
            }

            return "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            logger.error("图片转换为base64失败：{}", imagePath, e);
            return null;
        }
    }

    // ======================================
    // 🔥 最终版：批量多模态AI调用（文字+图片+文件+视频+音频）
    // ======================================
    private String callAiBase(String question, List<UserAiChat> memoryList, Integer messageType, String scene) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("temperature", 0.7);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> system = new HashMap<>();
        system.put("role", "system");
        StringBuilder systemPrompt = new StringBuilder(BASE_SYSTEM_PROMPT);
        if ("book".equals(scene)) systemPrompt.append(BOOK_SERVICE_PROMPT);
        system.put("content", systemPrompt.toString());
        messages.add(system);

        // 历史记忆处理
        List<UserAiChat> memoryCopy = new ArrayList<>(memoryList != null ? memoryList : new ArrayList<>());
        if (memoryCopy.size() > 8) memoryCopy = memoryCopy.subList(memoryCopy.size() - 2, memoryCopy.size());
        Collections.reverse(memoryCopy);
        for (UserAiChat chat : memoryCopy) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", chat.getFormType() == 0 ? "user" : "assistant");
            msg.put("content", chat.getMessageContent() != null ? chat.getMessageContent() : "");
            messages.add(msg);
        }

        // 🔥 核心：批量多模态用户消息
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        List<Map<String, Object>> contentList = new ArrayList<>();
        String userText = "";

        // 1. 批量图片 messageType=2
        if (messageType == 2) {
            List<Map<String, String>> images = parseBatchImages(question);
            // 🔥 兜底：如果进入了第 2/3 轮工具调用，标签已消失，转为纯文本处理
            if (images.isEmpty()) {
                userMsg.put("content", question);
            } else {
                userText = images.get(0).get("text");
                if (!userText.isBlank()) {
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("type", "text");
                    textPart.put("text", userText);
                    contentList.add(textPart);
                }
                for (Map<String, String> img : images) {
                    String base64 = imageToBase64(img.get("url"));
                    if (base64 != null) {
                        Map<String, Object> imagePart = new HashMap<>();
                        imagePart.put("type", "image_url");
                        Map<String, String> urlMap = new HashMap<>();
                        urlMap.put("url", base64);
                        imagePart.put("image_url", urlMap);
                        contentList.add(imagePart);
                    }
                }
            }
        }

        // 2. 批量文件 messageType=3
        else if (messageType == 3) {
            List<Map<String, String>> files = parseBatchFiles(question);
            // 🔥 兜底：第 2/3 轮转换为纯文本处理
            if (files.isEmpty()) {
                userMsg.put("content", question);
            } else {
                StringBuilder fullPrompt = new StringBuilder("用户上传了批量文件：\n");
                for (Map<String, String> file : files) {
                    String path = file.get("url");
                    String name = file.get("name");
                    userText = file.get("text");
                    fullPrompt.append("- ").append(name).append("\n");
                    String text = extractTextFromFile("D:/WebTsglxt" + path);
                    if (text != null) fullPrompt.append("内容：").append(text).append("\n\n");
                }
                if (!userText.isBlank()) fullPrompt.append("用户问题：").append(userText);
                userMsg.put("content", fullPrompt.toString());
            }
        }

        // 3. 批量视频 messageType=4
        else if (messageType == 4) {
            List<Map<String, String>> videos = parseBatchVideos(question);
            // 🔥 兜底：第 2/3 轮转换为纯文本处理
            if (videos.isEmpty()) {
                userMsg.put("content", question);
            } else {
                userText = videos.get(0).get("text");
                if (!userText.isBlank()) {
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("type", "text");
                    textPart.put("text", userText);
                    contentList.add(textPart);
                }
                for (Map<String, String> vid : videos) {
                    String base64 = mediaToBase64(vid.get("url"), 50);
                    if (base64 != null) {
                        Map<String, Object> videoPart = new HashMap<>();
                        videoPart.put("type", "video_url");
                        Map<String, Object> videoUrlMap = new HashMap<>();
                        videoUrlMap.put("url", base64);
                        videoUrlMap.put("fps", 1.0f);
                        videoPart.put("video_url", videoUrlMap);
                        contentList.add(videoPart);
                    }
                }
            }
        }

        // 4. 批量语音 messageType=5 【修改为：本地Base64识别，无代理】
        else if (messageType == 5) {
            List<Map<String, String>> voices = parseBatchVoices(question);

            // 🔥 兜底：第 2/3 轮转换为纯文本处理
            if (voices.isEmpty()) {
                userMsg.put("content", question);
            } else{
                userText = voices.isEmpty() ? "" : voices.get(0).get("text");

                // 添加用户文字问题
                if (!userText.isBlank()) {
                    Map<String, Object> textPart = new HashMap<>();
                    textPart.put("type", "text");
                    textPart.put("text", userText);
                    contentList.add(textPart);
                }

                // 本地语音转Base64，AI直接识别
                for (Map<String, String> voice : voices) {
                    String base64 = mediaToBase64(voice.get("url"), 10); // 语音最大10MB
                    if (base64 != null) {
                        Map<String, Object> voicePart = new HashMap<>();
                        voicePart.put("type", "audio_url"); // ✅ 正确type
                        Map<String, String> audioUrlMap = new HashMap<>();
                        audioUrlMap.put("url", base64);
                        voicePart.put("audio_url", audioUrlMap);
                        contentList.add(voicePart);
                    }
                }
            }
        }


        // 5. 纯文字
        else {
            userMsg.put("content", question);
        }

        if (!contentList.isEmpty()) userMsg.put("content", contentList);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        // 发送请求
        URL url = new URL(CHAT_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder res = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) res.append(line);

        JSONObject json = JSON.parseObject(res.toString());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    // 🔥 常量定义（放在类顶部，与原有代码风格保持一致）
    private static final int MAX_DISPLAY_BOOKS = 3; // 最多显示3本匹配书籍
    private static final int RECOMMEND_BOOKS_COUNT = 3; // 每本书推荐3本相似书
    private String queryBook(String keyword) {
        try {
            // 🔍 智能查询逻辑：先按ISBN精确查，无结果再按书名模糊查
            List<BookInformation> books = new ArrayList<>();

            // 第一步：尝试按ISBN精确查询（用户可能输入的是ISBN）
            List<BookInformation> isbnResults = bookDao.queryByISBN(keyword.trim());
            if (!isbnResults.isEmpty()) {
                books.addAll(isbnResults);
            }

            // 第二步：如果ISBN没查到，按书名模糊查询
            if (books.isEmpty()) {
                List<BookInformation> nameResults = bookDao.queryByName(keyword.trim());
                if (!nameResults.isEmpty()) {
                    books.addAll(nameResults);
                }
            }

            // 无结果返回友好提示
            if (books.isEmpty()) {
                // 3. 调用BookService里的爬虫方法（你之前写的 crawlAndAddBook）
                ResultDTO<String> crawlResult = bookService.crawlAndAddBook(keyword.trim());
                logger.info("爬虫结果：{}", crawlResult.getMsg());

                // 4. 爬虫完成后，再次查询书籍
                List<BookInformation> nameResults = bookDao.queryByName(keyword.trim());
                if (!nameResults.isEmpty()) {
                    books.addAll(nameResults);
                }

                // 5. 爬虫仍失败 → 返回提示
                if (books.isEmpty()) {
                    return "😥 很抱歉，全网也没有找到《" + keyword.trim() + "》这本书哦~ 换一本书试试吧！";
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("📚 为你找到 ").append(books.size()).append(" 本相关书籍：\n\n");

            // 只显示前MAX_DISPLAY_BOOKS本，避免信息过载
            int showCount = Math.min(books.size(), MAX_DISPLAY_BOOKS);
            for (int i = 0; i < showCount; i++) {
                BookInformation book = books.get(i);
                // 格式化单本书籍信息
                appendFullBookInfo(result, book, i + 1);

                // 🎯 自动添加相似书籍推荐（使用你现有的DAO方法）
                List<BookInformation> similarBooks = bookDao.listSimilarBooksByContent(
                        Collections.singletonList(book.getISBN()),
                        RECOMMEND_BOOKS_COUNT
                );
                if (!similarBooks.isEmpty()) {
                    appendSimilarBooks(result, similarBooks);
                }

                // 书籍之间加分隔线
                if (i < showCount - 1) {
                    result.append("\n---\n\n");
                }
            }

            // 如果有更多结果，提示用户
            if (books.size() > MAX_DISPLAY_BOOKS) {
                result.append("\n\n📌 还有 ").append(books.size() - MAX_DISPLAY_BOOKS)
                        .append(" 本相关书籍，输入更精确的书名或ISBN可以查看更多~");
            }

            return result.toString();
        } catch (Exception e) {
            logger.error("书籍查询失败，关键词：{}", keyword, e);
            return "😥 书籍查询出错了，请稍后再试~";
        }
    }
    // 🔥 提取书籍信息格式化方法（复用性强）
    private void appendFullBookInfo(StringBuilder sb, BookInformation book, int index) {
        sb.append("【").append(index).append("】 ").append(book.getBookname()).append("\n");
        sb.append("✍️ 作者：").append(book.getAuthor()).append("\n");
        sb.append("📖 ISBN：").append(book.getISBN()).append("\n");
        sb.append("🏢 出版社：").append(book.getPublisher() != null ? book.getPublisher() : "未知").append("\n");

        // 出版日期格式化
        if (book.getPublishDate() != null) {
            sb.append("📅 出版日期：").append(book.getPublishDate().format(DATE_FORMATTER)).append("\n");
        } else {
            sb.append("📅 出版日期：未知\n");
        }

        sb.append("🏷️ 分类：").append(book.getBookTypeName() != null ? book.getBookTypeName() : "未知分类").append("\n");
        sb.append("⭐ 评分：").append(String.format("%.1f", book.getStar()))
                .append("（").append(book.getRatingCount()).append("人评价）").append("\n");
        sb.append("📊 馆藏：共").append(book.getAll_book()).append("本 | ");

        // 库存状态高亮显示
        if (book.getNow_book() > 0) {
            sb.append("✅ 可借：").append(book.getNow_book()).append("本");
        } else {
            sb.append("❌ 暂无库存");
        }
        sb.append("\n");
        sb.append("🔗 查看详情：[{book:").append(book.getISBN()).append("}]\n");
    }

    // 🔥 提取相似书籍格式化方法
    private void appendSimilarBooks(StringBuilder sb, List<BookInformation> similarBooks) {
        sb.append("\n💡 你可能也喜欢：\n");
        for (BookInformation similar : similarBooks) {
            sb.append("  • ").append(similar.getBookname())
                    .append(" - ").append(similar.getAuthor())
                    .append("（").append(similar.getBookTypeName() != null ? similar.getBookTypeName() : "未知").append("）\n")
                    .append("🔗 查看详情：[{book:").append(similar.getISBN()).append("}]\n");
        }
    }

    // 🔥 直接替换你原来的queryUserByName方法即可
    private String queryUserByName(String userName) {
        try {
            List<UserInformation> users = userDao.queryByName(userName);
            if (users.isEmpty()) {
                return "😢 未找到名为「" + userName + "」的用户";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🔍 找到 ").append(users.size()).append(" 位匹配用户：\n\n");

            int showCount = Math.min(5, users.size());
            for (int i = 0; i < showCount; i++) {
                UserInformation u = users.get(i);
                // 🔥 核心修改：用户名格式化为 @用户名(用户ID)，支持前端点击跳转
                sb.append("【").append(i + 1).append("】 @").append(u.getName()).append("(").append(u.getUserId()).append(")\n");
                sb.append("👤 用户ID：").append(u.getUserId()).append("\n");
                sb.append("⚧️ 性别：").append(u.getSex() != null ? u.getSex() : "未知").append("\n");

                // 生日格式化
                if (u.getBirthday() != null) {
                    sb.append("🎂 生日：").append(u.getBirthday().format(DATE_FORMATTER)).append("\n");
                } else {
                    sb.append("🎂 生日：未知\n");
                }

                // 系别（联表查询结果）
                sb.append("🏫 系别：").append(u.getDeptTypeName() != null ? u.getDeptTypeName() : "未知").append("\n");

                // 用户类型（联表查询结果）
                sb.append("🎖️ 身份：").append(u.getTypeName() != null ? u.getTypeName() : "未知").append("\n");

                // 注册日期格式化
                if (u.getRegdate() != null) {
                    sb.append("📅 注册日期：").append(u.getRegdate().format(DATE_FORMATTER)).append("\n");
                } else {
                    sb.append("📅 注册日期：未知\n");
                }

                // 🔥 修正：阅读时长格式化（秒 → 小时+分钟）
                Integer readTimeSeconds = u.getRead_time_long();
                if (readTimeSeconds == null || readTimeSeconds <= 0) {
                    sb.append("📚 累计阅读：暂无记录\n");
                } else {
                    // 核心修正：秒转小时和分钟
                    int hours = readTimeSeconds / 3600;
                    int minutes = (readTimeSeconds % 3600) / 60;

                    if (hours > 0) {
                        sb.append("📚 累计阅读：").append(hours).append("小时").append(minutes).append("分钟\n");
                    } else {
                        sb.append("📚 累计阅读：").append(minutes).append("分钟\n");
                    }
                }

                // 个人介绍
                String bio = u.getBio();
                if (bio != null && !bio.isBlank()) {
                    // 个人介绍过长时截断，避免信息过载
                    String shortBio = bio.length() > 50 ? bio.substring(0, 50) + "..." : bio;
                    sb.append("📝 个人介绍：").append(shortBio).append("\n");
                } else {
                    sb.append("📝 个人介绍：暂无\n");
                }

                sb.append("🔗 查看详情：[{user:").append(u.getUserId()).append("}]\n");
                // 用户之间加分隔线
                if (i < showCount - 1) {
                    sb.append("\n---\n\n");
                }
            }

            // 超过5条时提示
            if (users.size() > 5) {
                sb.append("\n\n📌 还有 ").append(users.size() - 5).append(" 位用户，输入更精确的用户名可以查看更多~");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询用户失败，用户名：{}", userName, e);
            return "😥 用户查询出错了，请稍后再试~";
        }
    }


    // 🔥 直接替换你原来的queryMyNote方法即可
    private String queryMyNote(String userId) {
        try {
            List<UserTextCollection> notes = noteDao.queryByUserId(userId);
            if (notes.isEmpty()) {
                return "📝 你还没有写过任何读书笔记哦~\n💡 阅读书籍时点击「添加笔记」就能记录你的想法啦~";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📝 我的读书笔记（共").append(notes.size()).append("条）：\n\n");

            int showCount = Math.min(3, notes.size());
            // 笔记时间格式化器（精确到分钟）
            DateTimeFormatter noteTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (int i = 0; i < showCount; i++) {
                UserTextCollection n = notes.get(i);
                sb.append("【笔记").append(i + 1).append("】\n");
                sb.append("📖 书籍ISBN：").append(n.getIsbn()).append("\n");

                // 章节信息
                String chapterId = n.getChapterId();
                if (chapterId != null && !chapterId.isBlank()) {
                    sb.append("📑 章节：").append(chapterId).append("\n");
                } else {
                    sb.append("📑 章节：全书\n");
                }

                // 笔记类型
                String noteType = n.getNoteTypeName();
                if (noteType != null && !noteType.isBlank()) {
                    sb.append("🏷️ 类型：").append(noteType).append("\n");
                } else {
                    sb.append("🏷️ 类型：普通笔记\n");
                }

                // 创建时间
                if (n.getCreateTime() != null) {
                    sb.append("⏰ 创建时间：").append(n.getCreateTime().format(noteTimeFormatter)).append("\n");
                }

                // 笔记内容（过长自动截断）
                String text = n.getText();
                if (text != null && !text.isBlank()) {
                    String shortText = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                    sb.append("💭 笔记内容：").append(shortText).append("\n");
                } else {
                    sb.append("💭 笔记内容：暂无\n");
                }

                // 🔥 新增：读者批注
                String readerComment = n.getReaderComment();
                if (readerComment != null && !readerComment.isBlank()) {
                    String shortComment = readerComment.length() > 50 ? readerComment.substring(0, 50) + "..." : readerComment;
                    sb.append("✍️ 我的批注：").append(shortComment).append("\n");
                } else {
                    sb.append("✍️ 我的批注：暂无\n");
                }

                sb.append("🔗 查看详情：[{note:").append(n.getId()).append("}]\n");
                // 笔记之间加分隔线
                if (i < showCount - 1) {
                    sb.append("\n---\n\n");
                }
            }

            // 超过3条时提示
            if (notes.size() > 3) {
                sb.append("\n\n📌 还有 ").append(notes.size() - 3).append(" 条历史笔记，进入个人中心查看全部~");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询个人笔记失败，用户ID：{}", userId, e);
            return "😥 笔记查询出错了，请稍后再试~";
        }
    }

    private String queryMyBorrowAndCollect(String userId) {
        try {
            List<BorrowInformation> borrows = borrowDao.queryByUserId(userId);
            List<UserCollection> collects = collectionDao.queryByUserId(userId,1 ,5);

            StringBuilder sb = new StringBuilder();
            DateTimeFormatter dateFormatter = DATE_FORMATTER; // 复用全局日期格式化器（yyyy-MM-dd）
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // ====================== 借阅部分 ======================
            sb.append("📖 我的借阅（共").append(borrows.size()).append("本）：\n\n");
            if (borrows.isEmpty()) {
                sb.append("✅ 你暂无借阅中的书籍\n");
            } else {
                int showBorrowCount = Math.min(3, borrows.size());
                for (int i = 0; i < showBorrowCount; i++) {
                    BorrowInformation borrow = borrows.get(i);
                    String isbn = borrow.getISBN();
                    String bookName = "未知书籍";

                    // 通过ISBN查询书籍名称
                    try {
                        List<BookInformation> books = bookDao.queryByISBN(isbn);
                        if (!books.isEmpty()) {
                            bookName = books.get(0).getBookname();
                        }
                    } catch (Exception e) {
                        logger.warn("查询借阅书籍信息失败，ISBN：{}", isbn, e);
                    }

                    sb.append("【").append(i + 1).append("】 ").append(bookName).append("\n");
                    sb.append("📖 ISBN：").append(isbn).append("\n");
                    sb.append("📅 借阅日期：").append(borrow.getBorrowDate() != null
                            ? borrow.getBorrowDate().format(dateFormatter) : "未知").append("\n");
                    sb.append("📅 应还日期：").append(borrow.getReturnDate() != null
                            ? borrow.getReturnDate().format(dateFormatter) : "未知").append("\n");

                    // 罚款金额处理
                    Float fine = borrow.getFine();
                    if (fine == null || fine <= 0) {
                        sb.append("💰 罚款：无\n");
                    } else {
                        sb.append("💰 罚款：¥").append(String.format("%.2f", fine)).append("\n");
                    }

                    // 🔥 新增：书籍详情链接（和queryBook完全一致）
                    sb.append("🔗 查看书籍：[{book:").append(isbn).append("}]\n");

                    if (i < showBorrowCount - 1) {
                        sb.append("\n");
                    }
                }

                if (borrows.size() > 3) {
                    sb.append("\n📌 还有 ").append(borrows.size() - 3).append(" 本借阅书籍，进入个人中心查看全部~");
                }
            }

            // 分隔线
            sb.append("\n\n---\n\n");

            // ====================== 收藏部分 ======================
            sb.append("❤️ 我的收藏（共").append(collects.size()).append("本）：\n\n");
            if (collects.isEmpty()) {
                sb.append("💔 你暂无收藏的书籍\n");
            } else {
                int showCollectCount = Math.min(3, collects.size());
                for (int i = 0; i < showCollectCount; i++) {
                    UserCollection collect = collects.get(i);
                    String isbn = collect.getIsbn();
                    String bookName = "未知书籍";

                    // 通过ISBN查询书籍名称
                    try {
                        List<BookInformation> books = bookDao.queryByISBN(isbn);
                        if (!books.isEmpty()) {
                            bookName = books.get(0).getBookname();
                        }
                    } catch (Exception e) {
                        logger.warn("查询收藏书籍信息失败，ISBN：{}", isbn, e);
                    }

                    sb.append("【").append(i + 1).append("】 ").append(bookName).append("\n");
                    sb.append("📖 ISBN：").append(isbn).append("\n");

                    // 最后阅读时间处理（Timestamp转LocalDateTime）
                    Timestamp lastReadTime = collect.getLastReadTime();
                    if (lastReadTime != null) {
                        sb.append("⏰ 最后阅读：").append(lastReadTime.toLocalDateTime().format(timeFormatter)).append("\n");
                    } else {
                        sb.append("⏰ 最后阅读：暂无记录\n");
                    }

                    // 🔥 新增：书籍详情链接（和queryBook完全一致）
                    sb.append("🔗 查看书籍：[{book:").append(isbn).append("}]\n");

                    if (i < showCollectCount - 1) {
                        sb.append("\n");
                    }
                }


                if (collects.size() > 3) {
                    sb.append("\n📌 还有 ").append(collects.size() - 3).append(" 本收藏书籍，进入个人中心查看全部~");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询借阅收藏失败，用户ID：{}", userId, e);
            return "😥 借阅收藏查询出错了，请稍后再试~";
        }
    }

    // ======================================
// 🔥 拆分后的借阅查询方法
// ======================================
    private String queryMyBorrow(String userId) {
        try {
            List<BorrowInformation> borrows = borrowDao.queryByUserId(userId);
            DateTimeFormatter dateFormatter = DATE_FORMATTER;

            StringBuilder sb = new StringBuilder();
            sb.append("📖 我的借阅（共").append(borrows.size()).append("本）：\n\n");

            if (borrows.isEmpty()) {
                sb.append("✅ 你暂无借阅中的书籍\n");
                sb.append("💡 输入「借《书名》」就能借阅你喜欢的书啦~");
                return sb.toString();
            }

            int showBorrowCount = Math.min(5, borrows.size());
            for (int i = 0; i < showBorrowCount; i++) {
                BorrowInformation borrow = borrows.get(i);
                String isbn = borrow.getISBN();
                String bookName = "未知书籍";

                // 通过ISBN查询书籍名称
                try {
                    List<BookInformation> books = bookDao.queryByISBN(isbn);
                    if (!books.isEmpty()) {
                        bookName = books.get(0).getBookname();
                    }
                } catch (Exception e) {
                    logger.warn("查询借阅书籍信息失败，ISBN：{}", isbn, e);
                }

                sb.append("【").append(i + 1).append("】 ").append(bookName).append("\n");
                sb.append("📖 ISBN：").append(isbn).append("\n");
                sb.append("📅 借阅日期：").append(borrow.getBorrowDate() != null
                        ? borrow.getBorrowDate().format(dateFormatter) : "未知").append("\n");
                sb.append("📅 应还日期：").append(borrow.getReturnDate() != null
                        ? borrow.getReturnDate().format(dateFormatter) : "未知").append("\n");

                // 罚款金额处理
                Float fine = borrow.getFine();
                if (fine == null || fine <= 0) {
                    sb.append("💰 罚款：无\n");
                } else {
                    sb.append("💰 罚款：¥").append(String.format("%.2f", fine)).append("\n");
                }

                // 🔥 新增：借阅ID（用于还书操作）
                sb.append("📌 借阅ID：").append(borrow.getBorrowId()).append("\n");
                sb.append("🔗 查看书籍：[{book:").append(isbn).append("}]\n");

                if (i < showBorrowCount - 1) {
                    sb.append("\n---\n\n");
                }
            }

            if (borrows.size() > 5) {
                sb.append("\n\n📌 还有 ").append(borrows.size() - 5).append(" 本借阅书籍，进入个人中心查看全部~");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询借阅记录失败，用户ID：{}", userId, e);
            return "😥 借阅记录查询出错了，请稍后再试~";
        }
    }

    // ======================================
    // 🔥 拆分后的收藏查询方法
    // ======================================
    private String queryMyCollection(String userId) {
        try {
            List<UserCollection> collects = collectionDao.queryByUserId(userId, 1, Integer.MAX_VALUE);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            StringBuilder sb = new StringBuilder();
            sb.append("❤️ 我的收藏（共").append(collects.size()).append("本）：\n\n");

            if (collects.isEmpty()) {
                sb.append("💔 你暂无收藏的书籍\n");
                sb.append("💡 输入「收藏《书名》」就能把喜欢的书加入收藏夹啦~");
                return sb.toString();
            }

            int showCollectCount = Math.min(5, collects.size());
            for (int i = 0; i < showCollectCount; i++) {
                UserCollection collect = collects.get(i);
                String isbn = collect.getIsbn();
                String bookName = "未知书籍";

                // 通过ISBN查询书籍名称
                try {
                    List<BookInformation> books = bookDao.queryByISBN(isbn);
                    if (!books.isEmpty()) {
                        bookName = books.get(0).getBookname();
                    }
                } catch (Exception e) {
                    logger.warn("查询收藏书籍信息失败，ISBN：{}", isbn, e);
                }

                sb.append("【").append(i + 1).append("】 ").append(bookName).append("\n");
                sb.append("📖 ISBN：").append(isbn).append("\n");

                // 最后阅读时间处理
                Timestamp lastReadTime = collect.getLastReadTime();
                if (lastReadTime != null) {
                    sb.append("⏰ 最后阅读：").append(lastReadTime.toLocalDateTime().format(timeFormatter)).append("\n");
                } else {
                    sb.append("⏰ 最后阅读：暂无记录\n");
                }

                // 🔥 新增：收藏ID（用于取消收藏操作）
                sb.append("📌 收藏ID：").append(collect.getCollectionId()).append("\n");
                sb.append("🔗 查看书籍：[{book:").append(isbn).append("}]\n");

                if (i < showCollectCount - 1) {
                    sb.append("\n---\n\n");
                }
            }

            if (collects.size() > 5) {
                sb.append("\n\n📌 还有 ").append(collects.size() - 5).append(" 本收藏书籍，进入个人中心查看全部~");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询收藏列表失败，用户ID：{}", userId, e);
            return "😥 收藏列表查询出错了，请稍后再试~";
        }
    }

    private String queryMyFriend(String userId) {
        try {
            List<Friend> friends = friendDao.queryByUserId(userId);
            if (friends.isEmpty()) {
                return "👥 你还没有添加任何好友哦~\n💡 去论坛逛逛，认识更多爱读书的小伙伴吧~";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("👥 我的好友（共").append(friends.size()).append("位）：\n\n");

            int showCount = Math.min(5, friends.size());
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int i = 0; i < showCount; i++) {
                Friend f = friends.get(i);
                List<UserInformation> fu = userDao.queryUserById(f.getFriendId());
                if (fu.isEmpty()) continue;

                UserInformation u = fu.get(0);
                sb.append("【").append(i + 1).append("】 @").append(u.getName()).append("(").append(u.getUserId()).append(")\n");

                // 好友备注
                String remark = f.getFriendRemark();
                if (remark != null && !remark.isBlank()) {
                    sb.append("✏️ 备注：").append(remark).append("\n");
                } else {
                    sb.append("✏️ 备注：无\n");
                }

                // 成为好友的时间
                LocalDateTime createTime = f.getCreateTime();
                if (createTime != null) {
                    sb.append("📅 成为好友：").append(createTime.format(dateFormatter)).append("\n");
                } else {
                    sb.append("📅 成为好友：未知\n");
                }

                // 额外展示好友的系别和身份
                String dept = u.getDeptTypeName();
                String type = u.getTypeName();
                if (dept != null && !dept.isBlank() && type != null && !type.isBlank()) {
                    sb.append("🏫 ").append(dept).append(" | ").append(type).append("\n");
                }

                sb.append("🔗 查看详情：[{user:").append(u.getUserId()).append("}]\n");
                // 好友之间加分隔线
                if (i < showCount - 1) {
                    sb.append("\n---\n\n");
                }
            }

            // 超过5条时提示
            if (friends.size() > 5) {
                sb.append("\n\n📌 还有 ").append(friends.size() - 5).append(" 位好友，进入个人中心查看全部~");
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询好友列表失败，用户ID：{}", userId, e);
            return "😥 好友查询出错了，请稍后再试~";
        }
    }

    // ======================================
// 🔥 借阅链路工具实现（放在 queryMyFriend 方法下面）
// ======================================
    /**
     * 借阅书籍：先查书籍→查库存→创建借阅记录→更新书籍库存→更新用户可借阅数量
     */
    private String borrowBook(JSONObject args, String userId) {
        try {
            // 1. 从AI参数中提取ISBN
            String isbn = args.getString("isbn");
            if (isbn == null || isbn.isBlank()) {
                return "❌ 借阅失败：请提供书籍的ISBN号";
            }

            // 2. 查询书籍是否存在
            List<BookInformation> books = bookDao.queryByISBN(isbn.trim());
            if (books.isEmpty()) {
                return "❌ 借阅失败：未找到ISBN为「" + isbn + "」的书籍";
            }
            BookInformation book = books.get(0);

            // 3. 检查库存
            if (book.getNow_book() <= 0) {
                return "❌ 借阅失败：《" + book.getBookname() + "》目前暂无库存，已被借完啦~";
            }

            // 4. 检查用户是否已借阅该书（防止重复借阅）
            List<BorrowInformation> userBorrows = borrowDao.queryByUserId(userId);
            boolean alreadyBorrowed = userBorrows.stream()
                    .anyMatch(b -> b.getISBN().equals(isbn) && b.getReturnDate() == null);
            if (alreadyBorrowed) {
                return "❌ 你已经借阅了《" + book.getBookname() + "》，请勿重复借阅哦~";
            }

            // 5. 检查用户可借阅数量（你要求的逻辑）
            UserInformation user = userDao.queryUserById(userId).get(0);
            if (user.getCan_use() <= 0) {
                return "❌ 借阅失败：你已没有借阅余额了，请先归还部分书籍~";
            }

            // 6. 创建借阅记录（默认借阅30天）
            BorrowInformation borrow = new BorrowInformation();
            borrow.setUserid(userId);
            borrow.setISBN(isbn);
            borrow.setBorrowDate(LocalDate.now());
            borrow.setReturnDate(LocalDate.now().plusDays(30));
            borrow.setFine(0.0f);
            borrowDao.add(borrow); // 调用你提供的BorrowInformationDao.add方法

            // 7. 更新书籍库存（减1）
            book.setNow_book(book.getNow_book() - 1);
            bookDao.update(book);

            // 8. 更新用户可借阅数量（如果你的User表有borrow_count字段）
             user.setCan_use(user.getCan_use() - 1);
             userDao.update(user);

            // 9. 记录审计日志
            AiOperationLog log = new AiOperationLog(
                    userId,
                    "ADD",
                    "BORROW_INFORMATION",
                    JSON.toJSONString(borrow),
                    "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            return "✅ 成功借阅《" + book.getBookname() + "》！\n" +
                    "📅 应还日期：" + borrow.getReturnDate().format(DATE_FORMATTER) + "\n" +
                    "🔗 查看书籍：[{book:" + isbn + "}]";
        } catch (Exception e) {
            logger.error("借阅书籍失败，参数：{}", args, e);
            AiOperationLog log = new AiOperationLog(
                    userId,
                    "ADD",
                    "BORROW_INFORMATION",
                    args.toJSONString(),
                    "失败：" + e.getMessage()
            );
            aiOperationLogDao.insertLog(log);
            return "❌ 借阅书籍失败：" + e.getMessage();
        }
    }

    /**
     * 归还书籍：查借阅记录→删除借阅记录→更新书籍库存→更新用户可借阅数量
     */
    private String returnBook(JSONObject args, String userId) {
        try {
            // 1. 从AI参数中提取借阅ID
            Integer borrowId = args.getInteger("borrowId");
            if (borrowId == null) {
                return "❌ 归还失败：请提供借阅记录的ID";
            }

            // 2. 查询该用户的所有借阅记录
            List<BorrowInformation> userBorrows = borrowDao.queryByUserId(userId);

            // 3. 找到对应的借阅记录（防止用户归还别人的书）
            BorrowInformation targetBorrow = userBorrows.stream()
                    .filter(b -> b.getBorrowId().equals(borrowId))
                    .findFirst()
                    .orElse(null);

            if (targetBorrow == null) {
                return "❌ 归还失败：你没有ID为「" + borrowId + "」的借阅记录";
            }

            // 4. 删除借阅记录（调用你提供的BorrowInformationDao.del方法）
            borrowDao.del(borrowId);

            // 5. 更新书籍库存（加1）
            String isbn = targetBorrow.getISBN();
            List<BookInformation> books = bookDao.queryByISBN(isbn);
            if (!books.isEmpty()) {
                BookInformation book = books.get(0);
                book.setNow_book(book.getNow_book() + 1);
                bookDao.update(book);
            }

            // 6. 更新用户可借阅数量
             UserInformation user = userDao.queryUserById(userId).get(0);
             user.setCan_use(user.getCan_use() + 1);
             userDao.update(user);

            // 7. 记录审计日志
            AiOperationLog log = new AiOperationLog(
                    userId,
                    "DELETE",
                    "BORROW_INFORMATION",
                    "{\"borrowId\":" + borrowId + ",\"isbn\":\"" + isbn + "\"}",
                    "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            String bookName = books.isEmpty() ? "未知书籍" : books.get(0).getBookname();
            return "✅ 成功归还《" + bookName + "》！\n" +
                    "🔗 查看书籍：[{book:" + isbn + "}]";
        } catch (Exception e) {
            logger.error("归还书籍失败，参数：{}", args, e);
            AiOperationLog log = new AiOperationLog(
                    userId,
                    "DELETE",
                    "BORROW_INFORMATION",
                    args.toJSONString(),
                    "失败：" + e.getMessage()
            );
            aiOperationLogDao.insertLog(log);
            return "❌ 归还书籍失败：" + e.getMessage();
        }
    }

    // 接受好友申请
    private String acceptFriendRequest(JSONObject args, String userId) {
        try {
            Integer requestId = args.getInteger("requestId");
            if (requestId == null) {
                return "❌ 接受失败：申请ID不能为空";
            }

            // 1. 查询申请详情
            List<FriendRequest> requests = friendRequestDao.queryReceivedRequests(userId);
            FriendRequest targetRequest = requests.stream()
                    .filter(r -> r.getId().equals(requestId))
                    .findFirst()
                    .orElse(null);

            if (targetRequest == null) {
                return "❌ 接受失败：未找到该好友申请";
            }
            if (targetRequest.getStatus() != 0) {
                return "❌ 该申请已被处理过啦~";
            }

            // 2. 更新申请状态为已接受（1-已接受）
            friendRequestDao.updateStatus(requestId, 1, LocalDateTime.now());

            // 3. 双向建立好友关系
            Friend friend1 = new Friend(userId, targetRequest.getFromUserId(), LocalDateTime.now());
            Friend friend2 = new Friend(targetRequest.getFromUserId(), userId, LocalDateTime.now());
            friendDao.add(friend1);
            friendDao.add(friend2);

            // 4. 记录日志
            AiOperationLog log = new AiOperationLog(
                    userId, "UPDATE", "FRIEND_REQUEST",
                    JSON.toJSONString(targetRequest), "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            // 5. 查询对方用户信息返回
            List<UserInformation> fromUser = userDao.queryUserById(targetRequest.getFromUserId());
            String userName = fromUser.isEmpty() ? "未知用户" : fromUser.get(0).getName();
            return "✅ 已成功接受@" + userName + "(" + targetRequest.getFromUserId() + ")的好友申请！\n" +
                    "🎉 现在你们可以开始聊天啦~";
        } catch (Exception e) {
            logger.error("接受好友申请失败", e);
            return "❌ 接受好友申请失败：" + e.getMessage();
        }
    }

    // 拒绝好友申请
    private String rejectFriendRequest(JSONObject args, String userId) {
        try {
            Integer requestId = args.getInteger("requestId");
            if (requestId == null) {
                return "❌ 拒绝失败：申请ID不能为空";
            }

            friendRequestDao.updateStatus(requestId, 2, LocalDateTime.now()); // 2-已拒绝

            AiOperationLog log = new AiOperationLog(
                    userId, "UPDATE", "FRIEND_REQUEST",
                    "{\"requestId\":" + requestId + ",\"status\":2}", "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            return "✅ 已拒绝该好友申请";
        } catch (Exception e) {
            logger.error("拒绝好友申请失败", e);
            return "❌ 拒绝好友申请失败：" + e.getMessage();
        }
    }

    // 查询收到的好友申请
    private String queryReceivedFriendRequests(String userId) {
        try {
            List<FriendRequest> requests = friendRequestDao.queryReceivedRequests(userId);
            if (requests.isEmpty()) {
                return "📭 你暂无收到的好友申请~";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📬 你收到的好友申请（共").append(requests.size()).append("条）：\n\n");

            for (int i = 0; i < requests.size(); i++) {
                FriendRequest r = requests.get(i);
                List<UserInformation> fromUser = userDao.queryUserById(r.getFromUserId());
                String userName = fromUser.isEmpty() ? "未知用户" : fromUser.get(0).getName();

                sb.append("【申请").append(i+1).append("】\n");
                sb.append("👤 申请人：@").append(userName).append("(").append(r.getFromUserId()).append(")\n");
                sb.append("💬 申请留言：").append(r.getRequestMsg() == null ? "无" : r.getRequestMsg()).append("\n");
                sb.append("⏰ 申请时间：").append(r.getCreateTime().format(DATETIME_FORMATTER)).append("\n");
                sb.append("📌 申请ID：").append(r.getId()).append("\n");

                String status = switch (r.getStatus()) {
                    case 0 -> "⏳ 待处理";
                    case 1 -> "✅ 已接受";
                    case 2 -> "❌ 已拒绝";
                    default -> "未知状态";
                };
                sb.append("状态：").append(status).append("\n");

                if (i < requests.size() - 1) {
                    sb.append("\n---\n\n");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            logger.error("查询好友申请失败", e);
            return "❌ 查询好友申请失败：" + e.getMessage();
        }
    }

    /**
     * 去除字符串中的 Emoji 表情、特殊符号，仅保留中文/英文/数字
     * 用于用户名脱敏匹配
     */
    private String removeEmoji(String str) {
        if (str == null || str.isBlank()) {
            return "";
        }
        // 正则匹配并剔除所有Emoji、特殊表情符号
        String emojiRegex = "[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF\uD83D-\uDE00\uDC00-\uDFFF\uFE0F]";
        return str.replaceAll(emojiRegex, "").trim();
    }
    // 发送私信（最终稳定版：上下文ID优先 + 防重名 + 修复NPE）
    private String sendMessage(JSONObject args, String userId) {
        try {
            String targetUserName = args.getString("userName");
            String content = args.getString("content");
            // 🔥 从自动补参中获取【上下文解析出的精准用户ID】
            String targetUserId = args.getString("targetUserId");

            // 基础参数校验
            if (content == null || content.isBlank()) {
                return "❌ 消息内容不能为空";
            }
            if (targetUserName == null || targetUserName.isBlank()) {
                return "❌ 目标用户名不能为空";
            }

            // 1. 先查询当前用户的好友列表（仅做权限校验，不再用来匹配名称）
            List<Friend> friends = friendDao.queryByUserId(userId);
            if (friends.isEmpty()) {
                return "❌ 你目前还没有好友，无法发送私信~";
            }

            // ==============================================
            // 🔥 核心分支1：存在上下文精准ID（99%场景走这里，解决重名）
            // ==============================================
            if (targetUserId != null && !targetUserId.isBlank()) {
                // 校验：该ID是否属于当前用户的好友（防越权）
                String finalTargetUserId = targetUserId;
                boolean isFriend = friends.stream()
                        .anyMatch(f -> f.getFriendId().equals(finalTargetUserId));
                if (!isFriend) {
                    return "❌ 对方不是你的好友，无法发送私信~";
                }
            }
            // ==============================================
            // 分支2：无上下文ID（兜底场景：直接输入用户名，未查好友列表）
            // ==============================================
            else {
                // 入参名称脱敏
                String pureInputName = removeEmoji(targetUserName);
                boolean match = false;
                // 遍历好友列表，双方名称脱敏后再匹配
                for (Friend f : friends) {
                    List<UserInformation> fu = userDao.queryUserById(f.getFriendId());
                    if (fu.isEmpty()) {
                        continue;
                    }
                    // 数据库原始名称脱敏
                    String pureDbName = removeEmoji(fu.get(0).getName());
                    // 纯文本对比，忽略表情差异
                    if (pureInputName.equals(pureDbName)) {
                        targetUserId = f.getFriendId();
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return "❌ 你还不是@" + targetUserName + "的好友，无法发送私信~";
                }
            }

            // ==============================================
            // 统一构建消息对象（修复NPE：初始化所有布尔字段）
            // ==============================================
            ChatMessage message = new ChatMessage();
            message.setFromUserId(userId);
            message.setToUserId(targetUserId);
            message.setMessageType(1);
            message.setMessageContent(content);
            message.setIsRead(0);
            message.setCreateTime(LocalDateTime.now());
            message.setOriginalContent(content);
            // 强制初始化布尔类型字段，杜绝NullPointerException
            message.setHideFromSender(false);
            message.setHideFromReceiver(false);
            message.setIsRecalled(false);

            // ==============================================
            // 复用原有业务Service（统一会话、SSE、日志逻辑）
            // ==============================================
            ResultDTO<Long> result = chatMessageService.sendMessage(message);
            if (!result.isSuccess()) {
                return "❌ 发送消息失败：" + result.getMsg();
            }

            // 记录操作日志
            AiOperationLog log = new AiOperationLog(
                    userId, "ADD", "CHAT_MESSAGE",
                    JSON.toJSONString(message), "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            return "✅ 已成功发送消息给@" + targetUserName + "(" + targetUserId + ")！\n" +
                    "💬 消息内容：" + content;
        } catch (Exception e) {
            logger.error("发送消息异常", e);
            return "❌ 发送消息失败：" + e.getMessage();
        }
    }

    // 收藏书籍
    private String collectBook(JSONObject args, String userId) {
        try {
            String isbn = args.getString("isbn");
            if (isbn == null || isbn.isBlank()) {
                return "❌ 收藏失败：ISBN不能为空";
            }

            // 检查书籍是否存在
            List<BookInformation> books = bookDao.queryByISBN(isbn);
            if (books.isEmpty()) {
                return "❌ 收藏失败：未找到ISBN为" + isbn + "的书籍";
            }

            // 检查是否已收藏
            List<UserCollection> collects = collectionDao.queryByUserId(userId, 1, Integer.MAX_VALUE);
            boolean alreadyCollected = collects.stream().anyMatch(c -> c.getIsbn().equals(isbn));
            if (alreadyCollected) {
                return "❌ 你已经收藏了这本书啦~";
            }

            UserCollection collection = new UserCollection();
            collection.setUserId(userId);
            collection.setIsbn(isbn);
            collection.setLastReadTime(Timestamp.valueOf(LocalDateTime.now()));
            collectionDao.add(collection);

            AiOperationLog log = new AiOperationLog(
                    userId, "ADD", "USER_COLLECTION",
                    JSON.toJSONString(collection), "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            String bookName = books.get(0).getBookname();
            return "✅ 成功收藏《" + bookName + "》！\n" +
                    "🔗 查看书籍：[{book:" + isbn + "}]";
        } catch (Exception e) {
            logger.error("收藏书籍失败", e);
            return "❌ 收藏书籍失败：" + e.getMessage();
        }
    }

    // 取消收藏
    private String cancelCollectBook(JSONObject args, String userId) {
        try {
            Integer collectionId = args.getInteger("collectionId");
            if (collectionId == null) {
                return "❌ 取消收藏失败：收藏ID不能为空";
            }

            collectionDao.del(collectionId);

            AiOperationLog log = new AiOperationLog(
                    userId, "DELETE", "USER_COLLECTION",
                    "{\"collectionId\":" + collectionId + "}", "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            return "✅ 已取消收藏";
        } catch (Exception e) {
            logger.error("取消收藏失败", e);
            return "❌ 取消收藏失败：" + e.getMessage();
        }
    }

    // 删除读书笔记
    private String deleteNote(JSONObject args, String userId) {
        try {
            Integer noteId = args.getInteger("noteId");
            if (noteId == null) {
                return "❌ 删除笔记失败：笔记ID不能为空";
            }

            noteDao.del(noteId);

            AiOperationLog log = new AiOperationLog(
                    userId, "DELETE", "USER_TEXT_COLLECTION",
                    "{\"noteId\":" + noteId + "}", "SUCCESS"
            );
            aiOperationLogDao.insertLog(log);

            return "✅ 笔记删除成功";
        } catch (Exception e) {
            logger.error("删除笔记失败", e);
            return "❌ 删除笔记失败：" + e.getMessage();
        }
    }

}