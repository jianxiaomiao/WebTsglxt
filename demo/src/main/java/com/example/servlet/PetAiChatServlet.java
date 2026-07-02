package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.*;
import com.example.dao.impl.*;
import com.example.dto.ResultDTO;
import com.example.entity.*;
import com.example.util.AiRateLimiter;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.config.AiConfig.*;

@WebServlet("/api/pet/ai/*")
public class PetAiChatServlet extends BaseServlet {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    private static final Logger logger = LoggerFactory.getLogger(PetAiChatServlet.class);

    private final BookCharacterLoreDao characterLoreDao = new BookCharacterLoreDaoImpl();
    private final AiInteractionHistoryDao interactionHistoryDao = new AiInteractionHistoryDaoImpl();

    // 新增：图谱节点、连线DAO
    private final GraphNodeDao graphNodeDao = new GraphNodeDaoImpl();
    private final GraphEdgeDao graphEdgeDao = new GraphEdgeDaoImpl();

    // 🔥 新增：引入用户行为日志和用户人设记忆的DAO
    private final UserBehaviorLogDao behaviorLogDao = new UserBehaviorLogDaoImpl();
    // 💡 提示：如果你的用户人设Dao名字不一样，请替换为你的实际类名
    private final UserProfileMemoryDao profileMemoryDao = new UserProfileMemoryDaoImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String actionType = null;
        String userId = null;

        try {
            JSONObject params = JSON.parseObject(req.getInputStream(), JSONObject.class);
            String question = params.getString("content");
            userId = params.getString("userId");
            actionType = params.getString("actionType");
            String isbn = params.getString("isbn");
            String chapterNumber = params.getString("chapterNumber");

            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1 && !pathInfo.equals("/chat")) {
                // 截掉斜杠，把 "/ai_steward" 强行覆盖为 actionType = "ai_steward" !
                actionType = pathInfo.substring(1);
            }

            if (actionType == null) actionType = "answer";

            // 🔥 AI 调用频率限制：每用户每分钟最多 10 次
            if (!AiRateLimiter.tryAcquire(userId)) {
                long waitSec = AiRateLimiter.getWaitSeconds(userId);
                out.write(JSON.toJSONString(ResultDTO.fail("AI调用太频繁了，请 " + waitSec + " 秒后再试")));
                return;
            }

            String systemPrompt;

            int logType = 9;
            // 1. ========== 🔥 核心：根据 Tag 动态分配 SystemPrompt 及查询数据 ==========
            switch (actionType) {
                // ================== ✨ 新增：AI 管家人设 ==================
                case "ai_steward":
                    // 查询用户是否读过该书 (action_type = 32)
                    UserBehaviorLog noteLog = behaviorLogDao.getLatestByUserIdAndIsbnAndType(userId, isbn, 5);
                    UserBehaviorLog readLog = behaviorLogDao.getLatestByUserIdAndIsbnAndType(userId, isbn, 32);
                    boolean hasRead = (readLog != null && isbn != null && isbn.equals(readLog.getBook_isbn())); // 注意实体类字段名映射
                    boolean hasNote = (noteLog != null && isbn != null && isbn.equals(noteLog.getBook_isbn()));
                    if(hasNote && hasRead){
                        systemPrompt = "你是一个贴心、优雅的AI阅读管家。用户已经阅读过当前这本书（且在历史记录中留下了足迹）。这是上一次用户留下的笔记和时间" +
                                noteLog.getContent_snapshot() + noteLog.getCreate_time() +
                                "和阅读的章节数(isbn-number)、时间" + readLog.getChapter_number() + readLog.getCreate_time() +
                                "请根据用户的阅读记录，结合Ta读过这本书的前提，给出贴心、温暖的管家式回复。语气要尊贵、亲切，带点小宠物的可爱和俏皮，不要说多余的废话。";
                    } else if (hasNote) {
                        systemPrompt = "你是一个贴心、优雅的AI阅读管家。用户已经阅读过当前这本书（且在历史记录中留下了足迹）。这是上一次用户留下的笔记和时间" +
                                noteLog.getContent_snapshot() + noteLog.getCreate_time() +
                                "请根据用户的阅读记录，结合Ta读过这本书的前提，给出贴心、温暖的管家式回复。语气要尊贵、亲切，带点小宠物的可爱和俏皮，不要说多余的废话。";
                    } else if (hasRead) {
                        systemPrompt = "你是一个贴心、优雅的AI阅读管家。用户已经阅读过当前这本书（且在历史记录中留下了足迹）。这是上一次用户阅读的章节数(isbn-number)、时间" +
                                readLog.getChapter_number() + readLog.getCreate_time() +
                                "请根据用户的阅读记录，结合Ta读过这本书的前提，给出贴心、温暖的管家式回复。语气要尊贵、亲切，带点小宠物的可爱和俏皮，不要说多余的废话。";
                    } else {
                        // 没读过，获取用户人设刻画
                        List<UserProfileMemory> profile = profileMemoryDao.queryByUserId(userId);
                        UserProfileMemory userprofile = (profile != null && !profile.isEmpty()) ? profile.getFirst() : null;
                        String userTraits = (userprofile != null && userprofile.getReading_preferences() != null)
                                ? "用户阅读偏好:" + userprofile.getReading_preferences() + "用户最近情绪状态:" + userprofile.getEmotional_state()
                                + "用户人像刻画:" + userprofile.getCharacter_sketch() : "喜欢探索新鲜知识、热爱阅读的宝藏读者";

                        systemPrompt = "你是一个贴心、优雅的AI阅读管家。用户尚未阅读过当前这本书。请你完成一件事：\n" +
                                "1. 结合用户的近期人设刻画（" + userTraits + "），为Ta写一段专属的阅读推荐语。\n" +
                                "语气要热情、俏皮，像是一个非常懂Ta的知心管家！";
                    }
                    logType = 34;
                    break;

                // ================== ✨ 新增：AI 辩论者人设 ==================
                case "ai_debater":
                    UserBehaviorLog latestNote = behaviorLogDao.getLatestByUserIdAndIsbnAndType(userId, isbn, 5);
                    UserBehaviorLog latestSummary = behaviorLogDao.getLatestByUserIdAndIsbnAndType(userId, isbn, 6);

                    // 🔥 优雅记忆外挂：尝试掏出上一轮 AI 辩论者自己的发言记录（假设 logType=35）
                    UserBehaviorLog lastDebate = behaviorLogDao.getLatestByUserIdAndIsbnAndType(userId, isbn, 35);

                    StringBuilder contextBuilder = new StringBuilder();
                    if (latestNote != null) contextBuilder.append("用户笔记灵感：").append(latestNote.getContent_snapshot()).append("；");
                    if (latestSummary != null) contextBuilder.append("关注的剧情：").append(latestSummary.getContent_snapshot()).append("；");

                    String basePrompt = "你是一个充满智慧与思辨精神的AI阅读辩论者。你的目的不是吵架，而是作为‘思想磨刀石’激发用户灵感。\n" +
                            "请参考用户近期的思考数据（" + (contextBuilder.length() > 0 ? contextBuilder.toString() : "无") + "）。";

                    // 🧠 如果抓到了上一轮的记忆，极其自然地承接上下文！
                    if (lastDebate != null && lastDebate.getContent_snapshot() != null) {
                        basePrompt += "\n\n【前情记忆载荷】在上一轮交锋中，你给出的核心辩词是：\"" + lastDebate.getContent_snapshot() + "\"。\n" +
                                "请承接你上一轮的逻辑脉络，针对用户本次的最新回击进行沉浸式反驳或升华！不要说‘收到、好的’等废话。";
                    }

                    systemPrompt = basePrompt;
                    logType = 35; // 确保下文 logAsync 存入的是35，方便下一轮抓取！
                    break;

                case "character_analyze":
                    systemPrompt = "你是专业文学分析师，输出硬性规则：\n" +
                            "1. 只输出完整闭合JSON数组，所有引号、大括号成对，禁止中途截断；\n" +
                            "2. 每个人物的biography生平控制在120字以内，精简文字，减少token占用；\n" +
                            "3. relationship_json 内层双引号必须转义 \\\"，示例 {\\\"翠翠\\\":\\\"孙女\\\"}；\n" +
                            "4. 严格格式：[{\"character_name\":\"姓名\",\"personality\":\"性格\",\"biography\":\"简短生平\",\"relationship_json\":\"转义嵌套JSON\"}]";
                    logType = 6;
                    break;
                case "book_summary":
                case "chapter_summary":
                    systemPrompt = "你是一个专业的阅读助手。请用结构化、精炼的语言对给定的内容进行总结，排版要清晰美观，重点突出。";
                    logType = 6;
                    break;
                case "text_rewrite":
                    systemPrompt = "你是一个资深文学编辑。请根据用户的要求，对给定的文本进行仿写或润色。保持原文的核心情感，直接输出修改后的文本，不需要任何多余的解释。";
                    logType = 24;
                    break;
                case "generate_quiz":
                    systemPrompt = "你是一个专业的出题老师。请严格根据用户给出的文本内容，按照要求生成JSON格式的题目。绝对不要包含任何Markdown包裹符号（如```json），也不要说任何废话。";
                    logType = 36;
                    break;
                case "emotion_analyze":
                    systemPrompt = "你是一个专业的情感分析师。请分析用户给出的句子，你必须且只能从以下5个词中输出一个词：【欢快、悲伤、愤怒、温柔、平静】。绝不能输出任何多余的标点符号或解释！";
                    logType = 37;
                    break;
                case "role_analyze":
                    systemPrompt = "你是一个台词分析师。请判断给定台词的说话人身份，你必须且只能从以下词中输出一个：【青年男、青年女、中年男、小女孩、老爷爷】。绝不能输出任何多余解释！";
                    logType = 37;
                    break;
                default:
                    systemPrompt = "你是用户的图书馆管理系统小宠物，回答要简短、口语化，不超过300个字，语气可爱（可以加入一些可爱的小表情），不要说多余的内容。";
                    break;
            }

// 2. ========== 🔥 终极双引擎分流枢纽（Traffic Cop） ==========
            // 判断当前任务是“需要流式陪伴”，还是“需要后台一口气憋完 JSON”
            boolean isStreamingTask = "answer".equals(actionType) || "ai_steward".equals(actionType) || "ai_debater".equals(actionType);

            if (isStreamingTask) {
                // 【引擎 A：跑车流式通道】（0.01秒光速挂断主线程，派子线程顺着SSE吐字）
                String sessionMsgId = "pet_ai_" + System.currentTimeMillis();
                String finalActionType = actionType; // 解决有效final编译限制

                String finalUserId = userId;
                CompletableFuture.runAsync(() -> {
                    try {
                        streamDoubaoToSse(finalUserId, question, systemPrompt, sessionMsgId, finalActionType, isbn, chapterNumber);
                    } catch (Exception e) {
                        logger.error("流式吐字全链路崩溃", e);
                        MessageSseServlet.sendMessageToUser(finalUserId, "{\"type\":\"PET_AI_ERROR\",\"msgId\":\"" + sessionMsgId + "\"}");
                    }
                });

                // 主线程光速给前端的 axios.post 返回回执，让前端输入框瞬间清空不卡顿！
                Map<String, Object> ack = new HashMap<>();
                ack.put("msgId", sessionMsgId);
                ack.put("status", "streaming_started");
                out.write(JSON.toJSONString(ResultDTO.success(ack)));

            } else {
                // 【引擎 B：卡车同步阻塞通道】（用于自动出题、人物JSON抽取、章节总结等后台硬活）
                String answer = callDoubaoApi(question, systemPrompt);

                // 传统总结类历史入库
                if ("book_summary".equals(actionType) || "chapter_summary".equals(actionType)) {
                    AiInteractionHistory history = new AiInteractionHistory();
                    history.setUser_id(userId);
                    history.setIsbn(isbn);
                    history.setInteraction_type(3);
                    history.setUser_input(question);
                    history.setAi_response(answer);
                    history.setSummary_tag(actionType);
                    interactionHistoryDao.add(history);
                }

                // 传统人物建表拦截
                if ("character_analyze".equals(actionType)) {
                    try {
                        String rawAnswer = answer;
                        // 1. 清除AI返回的markdown代码块标记
                        String tempJson = rawAnswer.replaceAll("```json", "").replaceAll("```", "").trim();
                        logger.info("原始AI人物JSON：{}", tempJson);

                        // ===================== 全新容错逻辑：从尾部逐步裁剪残缺内容，不再粗暴补括号/强制转义 =====================
                        String validJson = null;
                        int cutStep = 20;      // 每次从末尾砍掉20个字符
                        int maxCutLoop = 50;   // 最多裁剪50次，防止死循环，最多舍弃1000字符尾部残缺内容
                        for (int i = 0; i < maxCutLoop; i++) {
                            if (tempJson.isBlank()) break;
                            try {
                                // 尝试直接解析数组，能成功说明当前文本完整合法
                                JSON.parseArray(tempJson);
                                validJson = tempJson;
                                break;
                            } catch (Exception parseErr) {
                                // 解析失败，截断末尾残缺部分继续重试
                                if (tempJson.length() > cutStep) {
                                    tempJson = tempJson.substring(0, tempJson.length() - cutStep);
                                } else {
                                    break;
                                }
                            }
                        }

                        // 裁剪后仍无合法JSON，直接抛出异常进入捕获分支
                        if (validJson == null) {
                            throw new RuntimeException("裁剪尾部残缺内容后，仍未得到可解析的人物JSON");
                        }
                        logger.info("裁剪清洗后可用完整JSON：{}", validJson);

                        // 解析合法人物数组
                        List<BookCharacterLore> characters = JSON.parseArray(validJson, BookCharacterLore.class);

                        // ========== 1. 定义缓存：人物名称 -> 图谱节点ID ==========
                        Map<String, Integer> charNameToNodeId = new HashMap<>();
                        String targetIsbn = isbn;
                        String targetUserId = userId;
                        // 图谱人物节点类型（和前端GRAPH_NODE_TYPE.AI_ROLE对应）
                        final int ROLE_NODE_TYPE = 4;

                        // ========== 2. 第一层循环：创建所有人物节点，存入缓存 ==========
                        for (BookCharacterLore lore : characters) {
                            lore.setIsbn(targetIsbn);
                            // 校验并修复关系JSON（入库前兜底，防止MySQL JSON字段报错）
                            String rawRelJson = lore.getRelationship_json();
                            String safeRelJson = "{}";
                            if (rawRelJson != null && !rawRelJson.isBlank()) {
                                try {
                                    JSONObject relObj = JSON.parseObject(rawRelJson);
                                    safeRelJson = JSON.toJSONString(relObj);
                                } catch (Exception jsonErr) {
                                    logger.warn("单个人物关系JSON非法，重置为空对象，原始：{}", rawRelJson);
                                    safeRelJson = "{}";
                                }
                            }
                            lore.setRelationship_json(safeRelJson);
                            // 入库人物档案表
                            characterLoreDao.add(lore);

                            // 构建图谱节点：标题=人物名，内容=生平+性格
                            String charName = lore.getCharacter_name();
                            String nodeContent = "性格：" + lore.getPersonality() + "\n生平：" + lore.getBiography();
                            // 查询是否已存在该人物节点（防止重复创建）
                            GraphNode existNode = graphNodeDao.selectByMapUserAndHeader(targetIsbn, targetUserId, charName);
                            Integer nodeId;
                            if (existNode != null) {
                                // 已有节点，直接复用ID
                                nodeId = existNode.getId();
                            } else {
                                // 无节点，新建随机坐标人物节点
                                GraphNode newNode = new GraphNode();
                                newNode.setMapId(targetIsbn);
                                newNode.setMapUser(targetUserId);
                                newNode.setType(ROLE_NODE_TYPE);
                                newNode.setContentHeader(charName);
                                newNode.setContent(nodeContent);
                                newNode.setPositionX(150 + Math.random() * 300);
                                newNode.setPositionY(120 + Math.random() * 250);
                                newNode.setStyle("");
                                // 插入节点，获取自增ID
                                nodeId = graphNodeDao.add(newNode);
                            }
                            // 存入名称-节点ID映射
                            charNameToNodeId.put(charName, nodeId);
                        }

                        // ========== 3. 第二层循环：解析关系JSON，自动生成GraphEdge连线 ==========
                        for (BookCharacterLore lore : characters) {
                            String sourceCharName = lore.getCharacter_name();
                            Integer sourceNodeId = charNameToNodeId.get(sourceCharName);
                            if (sourceNodeId == null) continue;

                            // 解析当前人物的关系对象
                            JSONObject relObj = JSON.parseObject(lore.getRelationship_json());
                            for (String targetCharName : relObj.keySet()) {
                                Integer targetNodeId = charNameToNodeId.get(targetCharName);
                                // 对方人物不在本次数组内则跳过该条连线
                                if (targetNodeId == null) continue;
                                // 关系描述作为连线label
                                String relLabel = relObj.getString(targetCharName);

                                // 构建关系连线入库
                                GraphEdge edge = new GraphEdge();
                                edge.setMapId(targetIsbn);
                                edge.setMapUser(targetUserId);
                                edge.setSourceNodeId(sourceNodeId);
                                edge.setTargetNodeId(targetNodeId);
                                edge.setLabel(relLabel);
                                graphEdgeDao.add(edge);
                                logger.info("自动创建人物关系连线 {} -> {} 关系：{}", sourceCharName, targetCharName, relLabel);
                            }
                        }

                        answer = "✅ 人物性格分析完成，已自动收录人物档案并生成人物关系图谱！";
                    } catch (JSONException jsonErr) {
                        // 单独捕获JSON解析错误，打印完整日志，不阻断前端返回
                        logger.error("人物JSON格式残缺/语法错误，原始完整文本：{}", answer, jsonErr);
                        answer = "人物文字分析完成，但自动生成人物图谱失败（AI返回人物数据不完整），原始分析内容：\n" + answer;
                    } catch (Exception e) {
                        logger.error("人物建档/图谱生成通用业务异常", e);
                        answer = "人物文字分析完成，但自动建档、生成人物关系图谱失败，原始内容：\n" + answer;
                    }
                }

                UserBehaviorLogger.logAsync(userId, logType, isbn, chapterNumber, question);
                out.write(JSON.toJSONString(ResultDTO.success(answer)));
            }
        } catch (Exception e) {
            logger.error("🐱 AI对话异常 actionType={} userId={}", actionType, userId, e);
            try {
                out.write(JSON.toJSONString(ResultDTO.fail("AI服务暂时不可用，请稍后再试")));
            } catch (Exception ignored) {}
        } finally {
            out.flush();
            out.close();
        }
    }

        // =========================================================================
        // 🏎️ 跑车引擎核心：大模型 SSE 气泡流式泵（Stream Pump）
        // =========================================================================
        private void streamDoubaoToSse(String userId, String question, String systemPrompt,
                String sessionMsgId, String actionType, String isbn, String chapterNumber) throws IOException {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ENDPOINT_ID);
            requestBody.put("stream", true);       // 🔥 灵魂指令：让火山引擎吐泡泡！
            requestBody.put("reasoning_effort", "minimal");
            requestBody.put("max_tokens", 5000);
            requestBody.put("temperature", 0.7);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMap = new HashMap<>();
            sysMap.put("role", "system"); sysMap.put("content", systemPrompt);
            messages.add(sysMap);

            Map<String, String> userMap = new HashMap<>();
            userMap.put("role", "user"); userMap.put("content", question);
            messages.add(userMap);
            requestBody.put("messages", messages);

            URL url = URI.create(CHAT_API_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(40000);
            conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder fullTextBackup = new StringBuilder();

                // 1. 根据业务类型，智能判决前端应该弹什么气泡
                String targetBubbleType = "chat"; // 默认普通聊天（5秒自毁）
                if ("ai_debater".equals(actionType)) {
                    targetBubbleType = "talk";    // 辩论给talk框（带回复按钮）
                } else if ("book_summary".equals(actionType) || "chapter_summary".equals(actionType)) {
                    targetBubbleType = "static";  // 总结给static框（常驻点X关闭）
                }

                // 2. 广播①：通告前端建空盒，并把气泡执照（bubbleType）塞进 JSON 里下发！
                MessageSseServlet.sendMessageToUser(userId,
                        "{\"type\":\"PET_AI_START\",\"msgId\":\"" + sessionMsgId + "\",\"bubbleType\":\"" + targetBubbleType + "\"}");

                while ((line = br.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String rawData = line.substring(5).trim();
                        if ("[DONE]".equals(rawData)) break; // 豆包说：我吐完了。

                        try {
                            JSONObject chunk = JSON.parseObject(rawData);
                            com.alibaba.fastjson.JSONArray choices = chunk.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                if (delta != null && delta.containsKey("content")) {
                                    String piece = delta.getString("content");
                                    fullTextBackup.append(piece);

                                    // 广播②：把这 1 个字，顺着SSE网线瞬间砸进用户的收音机喇叭里！
                                    JSONObject chunkObj = new JSONObject();
                                    chunkObj.put("type", "PET_AI_CHUNK");
                                    chunkObj.put("msgId", sessionMsgId);
                                    chunkObj.put("content", piece);
                                    MessageSseServlet.sendMessageToUser(userId, chunkObj.toJSONString());
                                }
                            }
                        } catch (Exception e) { /* 某一个小碎片JSON报文残缺，直接放过 */ }
                    }
                }

                String finalCompleteAnswer = fullTextBackup.toString();

                // 广播③：全文完结，通知前端气泡开始5秒销毁倒计时
                JSONObject doneObj = new JSONObject();
                doneObj.put("type", "PET_AI_DONE");
                doneObj.put("msgId", sessionMsgId);
                doneObj.put("fullContent", finalCompleteAnswer);
                MessageSseServlet.sendMessageToUser(userId, doneObj.toJSONString());

                // 🔥 极其严密地把流式产生的数据，异步补录进你原有的交互历史表！
                if ("ai_steward".equals(actionType) || "ai_debater".equals(actionType)) {
                    AiInteractionHistory history = new AiInteractionHistory();
                    history.setUser_id(userId);
                    history.setIsbn(isbn);
                    history.setInteraction_type("ai_steward".equals(actionType) ? 1 : 2);
                    history.setUser_input(question);
                    history.setAi_response(finalCompleteAnswer);
                    history.setSummary_tag(actionType);
                    interactionHistoryDao.add(history);
                }

                UserBehaviorLogger.logAsync(userId, 9, isbn, chapterNumber, question);
            }
        }
    private String callDoubaoApi(String question, String systemPrompt) throws IOException {
        // ⏱️ 1. 按下秒表
        long apiStartTime = System.currentTimeMillis();
        logger.info("👉 [大模型计时开始] 发送给豆包的问题: '{}'", question);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);

        // 👊 重拳二：强行物理封印大模型的隐式思考能力，一毫秒内心戏都不准有！
        requestBody.put("reasoning_effort", "minimal");

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        URL url = URI.create(CHAT_API_URL).toURL();
        // 🔥 第二针强心剂：强行绕过操作系统的网络代理（防止你的Clash/v2ray偷偷拦截握手）
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);

        conn.setRequestMethod("POST");
        // 🔥 把连接超时从 5000 改成 2000！如果是死路，逼它 2 秒钟立刻投胎切换！
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(40000);
        // 让火山引擎的服务器把这条 TCP 通道给你留着，第二次提问连“握手”都免了！
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Keep-Alive", "timeout=60");
        conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // ⏱️ 2. 掐断秒表
        long apiCostTime = System.currentTimeMillis() - apiStartTime;
        logger.info("👈 [大模型计时结束] 收到火山引擎响应! 耗时: {} ms, HTTP状态码: {}", apiCostTime, responseCode);

        if (responseCode != 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) error.append(line);
                throw new IOException("API调用失败，响应码：" + responseCode);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);

            JSONObject json = JSON.parseObject(response.toString());

            // 👊 重拳三（终极测谎仪）：把大模型交代的 Token 账单原封不动打印出来！
            JSONObject usage = json.getJSONObject("usage");
            if (usage != null) {
                logger.info("📊 [豆包Token真话账单] 提问消耗: {}字, AI回答实际生成: {}字, 总计: {} tokens",
                        usage.getIntValue("prompt_tokens"),
                        usage.getIntValue("completion_tokens"),
                        usage.getIntValue("total_tokens"));
            }

            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }
}