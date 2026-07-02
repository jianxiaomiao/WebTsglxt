package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.config.AiConfig;
import com.example.dao.AiChatDao;
import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;
import com.example.dto.UserStatsForAIDTO;
import com.example.entity.UserAiChat;
import com.example.service.AiChatService;
import com.example.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.config.AiConfig.*;

public class AiChatServiceImpl implements AiChatService {
    private final AiChatDao aiChatDao;
    private static final Logger logger = LoggerFactory.getLogger(AiChatServiceImpl.class);


    public AiChatServiceImpl(AiChatDao aiChatDao) {
        this.aiChatDao = aiChatDao;
    }

    @Override
    public ResultDTO<String> chatWithMemory(String userId, String content) {
        try {
            // 1. 参数校验
            if (userId == null || content == null || content.isBlank()) {
                return ResultDTO.paramError("用户ID和消息内容不能为空");
            }

            // 2. 获取历史记忆
            List<UserAiChat> memoryList = aiChatDao.selectRecentMemory(userId, MEMORY_LIMIT);
            // ✅ 先保存用户消息（无论AI是否成功，用户消息都应该存在）
            aiChatDao.add(new UserAiChat(userId, content, 1, LocalDateTime.now(), 0));

            // 再调用AI
            String aiReply = callDoubaoApi(content, memoryList);

            // 最后保存AI回复
            aiChatDao.add(new UserAiChat(userId, aiReply, 1, LocalDateTime.now(), 1));

            return ResultDTO.success(aiReply);
        } catch (Exception e) {
            logger.error("AI聊天异常，用户ID：{}，问题：{}", userId, content, e); // ✅ 日志打印详细信息
            return ResultDTO.fail("AI服务暂时不可用，请稍后再试"); // ✅ 前端只显示通用信息
        }
    }

    @Override
    public ResultDTO<List<UserAiChat>> getChatHistory(String userId) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            List<UserAiChat> history = aiChatDao.selectAllHistory(userId);
            return ResultDTO.success(history);
        } catch (Exception e) {
            logger.error("查询AI历史异常", e);
            return ResultDTO.fail("查询聊天历史失败");
        }
    }

    /**
     * 调用豆包API（带记忆）
     */
    private String callDoubaoApi(String question, List<UserAiChat> memoryList) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", AiConfig.ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", AiConfig.MAX_TOKENS);
        requestBody.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", AiConfig.BASE_SYSTEM_PROMPT);
            messages.add(system);


        // ======================================
        // 安全处理记忆：创建副本 + 反转
        // ======================================
        List<UserAiChat> memoryCopy = new ArrayList<>(memoryList != null ? memoryList : new ArrayList<>());
        Collections.reverse(memoryCopy);
        for (UserAiChat chat : memoryCopy) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", chat.getMessageType() == 1 ? "user" : "assistant");
            msg.put("content", chat.getMessageContent());
            messages.add(msg);
        }

        // 当前用户问题
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", question);
        messages.add(user);

        requestBody.put("messages", messages);

        // 发送请求（代码完全不变）
        URL url = new URL(AiConfig.CHAT_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Authorization", "Bearer " + AiConfig.ARK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder res = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            res.append(line);
        }

        JSONObject json = JSON.parseObject(res.toString());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    @Override
    public ResultDTO<List<UserAiChat>> getChatHistoryByPage(String userId, int page, int pageSize) {
        try {
            if (userId == null || userId.isBlank()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            if (page < 1) {
                page = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 20; // 默认每页20条，最大100条
            }
            int offset = (page - 1) * pageSize;
            List<UserAiChat> history = aiChatDao.selectHistoryByPage(userId, offset, pageSize);
            return ResultDTO.success(history);
        } catch (Exception e) {
            logger.error("分页查询AI历史异常", e);
            return ResultDTO.fail("查询聊天历史失败");
        }
    }
    /**
     * 无记忆生成阅读报告评语（纯数据分析，不带历史聊天）
     */
    @Override
    public ResultDTO<String> generateReportComment(UserStatsForAIDTO stats) {
        try {
            if (stats == null) {
                return ResultDTO.paramError("统计数据不能为空");
            }
            // 1. 将统计数据转为字符串，传给AI
            String statsContent = JSON.toJSONString(stats);
            // 2. 无记忆调用AI生成评语
            String comment = callDoubaoApiForReport(statsContent);
            return ResultDTO.success(comment);
        } catch (Exception e) {
            logger.error("生成报告评语异常", e);
            return ResultDTO.fail("生成报告失败，请稍后再试");
        }
    }

    /**
     * 专用：无记忆调用豆包API生成阅读报告（独立方法，不影响原有聊天）
     */
    private String callDoubaoApiForReport(String statsJson) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 400);
        requestBody.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();
        // 专属系统提示词：生成阅读报告评语，可爱风格，基于统计数据
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content",
                "你是图书馆专属阅读小助手🥰\n" +
                        "用户会给你【用户阅读/社交/行为全维度统计数据】\n" +
                        "你的任务：\n" +
                        "1. 基于数据生成一段可爱、鼓励、暖心的评语\n" +
                        "2. 总结阅读时长、书籍数、互动行为等亮点\n" +
                        "3. 语气活泼可爱，简短精炼（100-300字）\n" +
                        "4. 绝对不要输出工具格式、代码、多余符号\n" +
                        "5. 纯文字评语，直接输出即可");
        messages.add(system);

        // 仅传入统计数据，无任何历史记忆
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", "请根据我的阅读统计数据生成评语：" + statsJson);
        messages.add(user);

        requestBody.put("messages", messages);

        // 发送HTTP请求（复用原有请求逻辑）
        URL url = new URL(CHAT_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        // 处理响应
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            logger.error("报告API调用失败，状态码：{}", responseCode);
            throw new Exception("AI报告服务异常");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) res.append(line);
            JSONObject json = JSON.parseObject(res.toString());
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } finally {
            conn.disconnect();
        }
    }

    // ===================== 🔥 最终修复版：本地Base64批量参考图生图（适配Doubao-Seedream-4.5） =====================
    @Override
    public String generateImage(String prompt, Integer commentId, String imagePath, Float strength) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", AiConfig.IMAGE_ENDPOINT_ID);
            requestBody.put("prompt", prompt != null ? prompt : "生成可爱的宠物图片");
            requestBody.put("size", AiConfig.IMAGE_SIZE);
            requestBody.put("n", AiConfig.IMAGE_NUM);
            requestBody.put("response_format", AiConfig.IMAGE_FORMAT);
            requestBody.put("watermark", true);
            requestBody.put("stream", false);

            // 🔥 核心：批量解析出本地图片相对路径（如：/forum_images/xxx.png）
            List<String> batchImages = parseBatchReferenceImages(imagePath);

            if (!batchImages.isEmpty()) {
                // ------------------ 单张参考图处理 ------------------
                if (batchImages.size() == 1) {
                    // 调用带有重试检测机制的方法，转换为 base64
                    String base64Data = imageToBase64(batchImages.get(0));
                    if (base64Data != null) {
                        requestBody.put("image", base64Data); // 传入 base64 字符串
                        requestBody.put("strength", strength != null ? strength : 0.5f);
                    } else {
                        logger.error("单张参考图转换为Base64失败，路径：{}", batchImages.get(0));
                    }
                }
                // ------------------ 🔥 批量多张参考图处理 ------------------
                else {
                    List<String> base64List = new ArrayList<>();
                    for (String img : batchImages) {
                        String base64Data = imageToBase64(img);
                        if (base64Data != null) {
                            base64List.add(base64Data);
                        } else {
                            logger.warn("批量参考图其中一张转换Base64失败，跳过。路径：{}", img);
                        }
                    }

                    if (!base64List.isEmpty()) {
                        requestBody.put("images", base64List); // 批量用 images 字段，传入 base64 数组
                        requestBody.put("strength", strength != null ? strength : 0.5f);
                    }
                }
            }

            // 发送请求
            URL url = new URL(AiConfig.IMAGE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                BufferedReader errorBr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder errorRes = new StringBuilder();
                String errorLine;
                while ((errorLine = errorBr.readLine()) != null) errorRes.append(errorLine);
                errorBr.close();
                logger.error("文生图报错：{}", errorRes);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) res.append(line);
            br.close();

            JSONObject json = JSON.parseObject(res.toString());
            logger.info("文生图数据{}", json);
            String imageUrl = json.getJSONArray("data").getJSONObject(0).getString("url");
            return ImageUtil.downloadAndSaveImage(imageUrl, commentId);

        } catch (Exception e) {
            logger.error("批量图生图失败", e);
            return null;
        }
    }

    /**
     * 读取本地图片文件，转换为带有重试和检测机制的 base64 编码
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

            // 🔥 同时判断：文件存在 + 且系统可读（完美规避并发写入未完成问题）
            while (retryCount < MAX_RETRY && (!file.exists() || !file.canRead())) {
                logger.warn("图片尚未可读，等待{}ms重试，路径：{}，当前重试次数：{}", WAIT_TIME, fullPath, retryCount + 1);
                Thread.sleep(WAIT_TIME);
                file = new File(fullPath);
                retryCount++;
            }

            if (!file.exists() || !file.isFile()) {
                logger.error("图片文件最终不可读：{}", fullPath);
                return null; // 这里改成返回null，在主方法中打印错误，防止抛异常阻断整批图
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

    /**
     * 批量解析参考图 [image]url[image]url[text]文字
     */
    private List<String> parseBatchReferenceImages(String imagePath) {
        List<String> imageUrls = new ArrayList<>();
        if (imagePath == null || !imagePath.contains("[image]")) return imageUrls;

        String[] parts = imagePath.split("\\[text\\]");
        String imagePart = parts[0];
        String[] urls = imagePart.split("\\[image\\]");

        for (String url : urls) {
            if (url.trim().isEmpty()) continue;
            imageUrls.add(url.trim());
        }
        return imageUrls;
    }
}