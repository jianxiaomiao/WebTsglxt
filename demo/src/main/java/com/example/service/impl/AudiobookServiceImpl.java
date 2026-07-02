package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.AudiobookSegmentDao;
import com.example.dao.BookChapterParagraphDao;
import com.example.dao.impl.AudiobookSegmentDaoImpl;
import com.example.dao.impl.BookChapterParagraphDaoImpl;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.AudiobookSegment;
import com.example.entity.BookChapterParagraph;
import com.example.service.AudiobookService;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.config.AiConfig.*;

public class AudiobookServiceImpl implements AudiobookService {
    private static final Logger logger = LoggerFactory.getLogger(AudiobookServiceImpl.class);

    private final AudiobookSegmentDao segmentDao = new AudiobookSegmentDaoImpl();
    private final BookChapterParagraphDao paragraphDao = new BookChapterParagraphDaoImpl();

    // edge-tts 可执行文件路径
    private static final String EDGE_TTS_PATH = "D:\\python\\Scripts\\edge-tts.exe";
    // 音频文件存储目录
    private static final String AUDIO_STORE_DIR = "D:/WebTsglxt/audiobook_audio";

    // edge-tts 音色映射（完整的 Microsoft 中文神经网络语音库）
    private static final Map<String, String> VOICE_MAP = new HashMap<>();
    static {
        // ======== 男声 ========
        VOICE_MAP.put("青年男", "zh-CN-YunxiNeural");      // Yunxi — 青年男声，活泼明亮
        VOICE_MAP.put("中年男", "zh-CN-YunjianNeural");    // Yunjian — 中年男声，沉稳厚重
        VOICE_MAP.put("中青年", "zh-CN-YunyangNeural");    // Yunyang — 中青年男声，自然流畅
        VOICE_MAP.put("老爷爷", "zh-CN-YunyangNeural");    // 老年用中青年音色
        VOICE_MAP.put("小孩男", "zh-CN-YunxiaNeural");     // Yunxia — 小男孩，活泼童趣

        // ======== 女声 ========
        VOICE_MAP.put("青年女", "zh-CN-XiaoxiaoNeural");   // Xiaoxiao — 青年女声，温柔清亮
        VOICE_MAP.put("小女孩", "zh-CN-XiaoyiNeural");     // Xiaoyi — 小女孩，可爱稚嫩
        VOICE_MAP.put("温柔女", "zh-CN-XiaobeiNeural");    // Xiaobei — 温柔女声，甜美亲切
        VOICE_MAP.put("活泼女", "zh-CN-XiaoniNeural");     // Xiaoni — 活泼女声，元气活力

        // ======== 旁白（默认） ========
        VOICE_MAP.put("旁白", "zh-CN-YunyangNeural");      // 旁白用中青年男声，自然阅读感

        // ======== 粤语 ========
        VOICE_MAP.put("粤语男", "zh-HK-WanLungNeural");    // WanLung — 粤语中青年男
        VOICE_MAP.put("粤语女", "zh-HK-HiuGaaiNeural");    // HiuGaai — 粤语女声
        VOICE_MAP.put("粤语温柔女", "zh-HK-HiuMaanNeural");// HiuMaan — 粤语温柔女声

        // ======== 台湾腔 ========
        VOICE_MAP.put("台湾男", "zh-TW-YunJheNeural");     // YunJhe — 台湾普通话中青年男
        VOICE_MAP.put("台湾女", "zh-TW-HsiaoChenNeural");  // HsiaoChen — 台湾普通话女声
        VOICE_MAP.put("台湾温柔女", "zh-TW-HsiaoYuNeural");// HsiaoYu — 台湾普通话温柔女声
    }

    @Override
    public ResultDTO<Map<String, Object>> generateSegments(String isbn, Integer chapterNumber, String userId) {
        try {
            // 1. 获取章节完整文本（拼接所有段落）
            logger.info("🎙 开始生成有声书分段 isbn={} chapter={}", isbn, chapterNumber);
            List<BookChapterParagraph> paragraphs = paragraphDao.queryByChapterId(
                    isbn + "-" + chapterNumber);
            if (paragraphs == null || paragraphs.isEmpty()) {
                logger.warn("🎙 该章节没有段落数据 isbn={} chapter={}", isbn, chapterNumber);
                return ResultDTO.fail("该章节没有段落数据");
            }
            logger.info("🎙 获取到 {} 个数据库段落，拼接为完整文本", paragraphs.size());

            // 拼接完整章节文本
            StringBuilder fullText = new StringBuilder();
            for (BookChapterParagraph p : paragraphs) {
                fullText.append(p.getContent());
            }

            // 2. AI 提示词：让 AI 切分对话段并标注角色
            String prompt = "你是一个专业的有声书制作导演。请将以下小说章节文本按\"说话人变化\"切分成朗读片段。\n\n" +
                    "切分规则：\n" +
                    "1. 叙述/描写/环境/心理活动 → 角色标注为「旁白」\n" +
                    "2. 引号内的对话 \"...\" → 根据上下文判断说话人角色：\n" +
                    "   青年男/青年女/中年男/中年女/小女孩/小男孩/老爷爷\n" +
                    "3. 每当说话人切换（旁白→某人、某人→旁白、某人→另一人），必须切为新段\n" +
                    "4. 一段内如果同时有旁白+对话，切分开\n" +
                    "5. 每段标注情感：欢快/悲伤/愤怒/温柔/平静\n" +
                    "6. ⚠️ 每段的text长度控制在50-300字之间，太长的段会被强制裁剪\n\n" +
                    "输出格式（严格JSON数组，每段包含text原文+role+emotion）：\n" +
                    "[{\"text\":\"原文片段1\",\"role\":\"旁白\",\"emotion\":\"平静\"},\n" +
                    " {\"text\":\"原文片段2\",\"role\":\"青年女\",\"emotion\":\"温柔\"}]\n\n" +
                    "章节文本：\n" + fullText.toString();

            // 3. 调用豆包 API（用更大的 token 限制，因为需要输出完整文本）
            logger.info("🎙 调用豆包AI分析角色并切分段... 文本总长度={} 字符", fullText.length());
            String aiResponse = callDoubaoApi(prompt,
                    "你是专业的有声书导演。输出严格JSON数组，每段包含text/role/emotion三个字段。text必须是原文片段不要改写。", 4000);
            logger.info("🎙 AI返回切分结果，长度={} 字符", aiResponse != null ? aiResponse.length() : 0);

            // 4. 解析AI返回的JSON
            JSONArray segmentsArray;
            try {
                String jsonStr = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
                if (!jsonStr.startsWith("[")) {
                    int start = jsonStr.indexOf('[');
                    int end = jsonStr.lastIndexOf(']') + 1;
                    if (start >= 0 && end > start) jsonStr = jsonStr.substring(start, end);
                }
                if (jsonStr.length() > 200) {
                    logger.info("🎙 清理后的JSON(前200字符): {}", jsonStr.substring(0, 200));
                }
                segmentsArray = JSON.parseArray(jsonStr);
                logger.info("🎙 成功解析，AI切分了 {} 个对话段", segmentsArray.size());
            } catch (Exception e) {
                logger.error("🎙 解析AI切分结果失败，降级为全文旁白", e);
                segmentsArray = new JSONArray();
                JSONObject fallback = new JSONObject();
                fallback.put("text", fullText.toString());
                fallback.put("role", "旁白");
                fallback.put("emotion", "温柔");
                segmentsArray.add(fallback);
            }

            // 5. 清除旧数据 + 批量插入新分段
            segmentDao.deleteByChapter(isbn, chapterNumber);
            Map<String, Integer> roleCounts = new HashMap<>();
            List<AudiobookSegment> segments = new ArrayList<>();

            for (int i = 0; i < segmentsArray.size(); i++) {
                JSONObject obj = segmentsArray.getJSONObject(i);
                String text = obj.getString("text");
                if (text == null || text.trim().isEmpty()) continue;

                AudiobookSegment seg = new AudiobookSegment();
                seg.setIsbn(isbn);
                seg.setChapterNumber(chapterNumber);
                seg.setParagraphId("ai_" + isbn + "_" + chapterNumber + "_" + (i + 1)); // AI生成的虚拟段落ID
                seg.setSortOrder(i + 1);
                seg.setRoleType(obj.getString("role") != null ? obj.getString("role") : "旁白");
                seg.setEmotion(obj.getString("emotion") != null ? obj.getString("emotion") : "温柔");
                seg.setStatus(0);
                segments.add(seg);
                roleCounts.merge(seg.getRoleType(), 1, Integer::sum);

                // 🔥 把AI切分的文本内容存到 textContent 字段（入库，供TTS使用）
                seg.setTextContent(text);
            }

            segmentDao.batchInsert(segments);
            logger.info("🎙 角色标注统计: {}", roleCounts);
            logger.info("🎙 批量插入完成，数据库7段→AI切分{}段", segments.size());

            Map<String, Object> result = new HashMap<>();
            result.put("totalSegments", segments.size());
            result.put("generatedCount", 0);
            result.put("message", "AI已切分为" + segments.size() + "个对话段，请生成音频");
            return ResultDTO.success(result);

        } catch (Exception e) {
            logger.error("🎙 生成有声书分段失败 isbn={} chapter={}", isbn, chapterNumber, e);
            return ResultDTO.fail("生成失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<AudiobookSegment>> queryByChapterPage(
            String isbn, Integer chapterNumber, Integer pageNum, Integer pageSize) {
        try {
            List<AudiobookSegment> list = segmentDao.queryByIsbnAndChapterPage(
                    isbn, chapterNumber, pageNum, pageSize);
            int total = segmentDao.countTotalByChapter(isbn, chapterNumber);
            return ResultDTO.success(PageResultDTO.success(
                    (long) total, pageNum, pageSize, list));
        } catch (Exception e) {
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<AudiobookSegment> generateAudio(Long segmentId) {
        try {
            AudiobookSegment segment = segmentDao.selectById(segmentId);
            if (segment == null) {
                logger.warn("🎵 分段不存在 segmentId={}", segmentId);
                return ResultDTO.fail("分段不存在");
            }
            logger.info("🎵 开始生成音频 segmentId={} role={} emotion={}", segmentId, segment.getRoleType(), segment.getEmotion());

            // 获取段落原文（优先 AI 切分的 textContent，否则从 paragraph 表查）
            String text = segment.getTextContent();
            if (text == null || text.trim().isEmpty()) {
                text = segment.getParagraphContent();
            }
            if (text == null || text.trim().isEmpty()) {
                // fallback: 从 paragraph 表直接查
                List<BookChapterParagraph> paras = paragraphDao.queryByChapterId(
                        segment.getIsbn() + "-" + segment.getChapterNumber());
                for (BookChapterParagraph p : paras) {
                    if (p.getId().equals(segment.getParagraphId())) {
                        text = p.getContent();
                        break;
                    }
                }
            }
            if (text == null || text.trim().isEmpty()) {
                logger.warn("🎵 段落文本为空 segmentId={} paragraphId={}", segmentId, segment.getParagraphId());
                segment.setStatus(2);
                segmentDao.updateById(segment);
                return ResultDTO.fail("段落文本为空");
            }

            // 选择音色
            String voice = VOICE_MAP.getOrDefault(segment.getRoleType(), "zh-CN-YunxiNeural");

            // 生成音频文件
            Path audioDir = Paths.get(AUDIO_STORE_DIR);
            if (!Files.exists(audioDir)) Files.createDirectories(audioDir);
            String fileName = "audiobook_" + segment.getIsbn() + "_" +
                    segment.getChapterNumber() + "_" + segment.getSortOrder() + ".mp3";
            Path audioPath = audioDir.resolve(fileName);

            // 调用 edge-tts 生成完整段落音频（使用 --file 避免命令行编码问题）
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("tts_", ".txt");
            try {
                java.nio.file.Files.writeString(tempFile, text);
                ProcessBuilder pb = new ProcessBuilder(
                        EDGE_TTS_PATH,
                        "--voice", voice,
                        "--file", tempFile.toString(),
                        "--write-media", audioPath.toString()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // 🔥 带超时的等待：60秒超时自动kill，标记为失败等重试
                boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.error("🎵 edge-tts 超过60秒未完成，已强制终止 segmentId={}", segmentId);
                    segment.setStatus(0);
                    segmentDao.updateById(segment);
                    return ResultDTO.fail("TTS 生成超时（>60秒），已标记为待重新生成");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorOutput = new String(process.getInputStream().readAllBytes());
                    logger.error("🎵 edge-tts 执行失败 退出码={} segmentId={} err={}", exitCode, segmentId, errorOutput);
                    segment.setStatus(2);
                    segmentDao.updateById(segment);
                    return ResultDTO.fail("TTS 生成失败，退出码: " + exitCode);
                }
            } finally {
                try { java.nio.file.Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }

            // 计算音频时长（近似：文件大小估算）
            long fileSize = Files.size(audioPath);
            float estimatedDuration = fileSize / 16000f; // 128kbps ≈ 16KB/s

            segment.setAudioUrl("/api/audiobook/audio?file=" + fileName);
            segment.setAudioDuration(estimatedDuration);
            segment.setStatus(1);
            segmentDao.updateById(segment);

            return ResultDTO.success(segment);

        } catch (Exception e) {
            logger.error("生成音频失败", e);
            return ResultDTO.fail("生成失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Map<String, Object>> getProgress(String isbn, Integer chapterNumber) {
        int total = segmentDao.countTotalByChapter(isbn, chapterNumber);
        int generated = segmentDao.countGeneratedByChapter(isbn, chapterNumber);
        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("generated", generated);
        progress.put("percent", total > 0 ? (int)((double)generated / total * 100) : 0);
        return ResultDTO.success(progress);
    }

    @Override
    public ResultDTO<PageResultDTO<Map<String, Object>>> listAudiobookBooks(String keyword, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 12;
        int offset = (pageNum - 1) * pageSize;

        StringBuilder sql = new StringBuilder(
                "SELECT bi.ISBN as isbn, bi.Bookname as bookName, bi.Author as author, " +
                "bi.PictureName as pictureName, bi.Information as information, " +
                "COUNT(DISTINCT aseg.chapter_number) as chapterCount " +
                "FROM audiobook_segment aseg " +
                "JOIN book_information bi ON aseg.isbn = bi.ISBN " +
                "WHERE aseg.status = 1 ");
        StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(DISTINCT bi.ISBN) " +
                "FROM audiobook_segment aseg " +
                "JOIN book_information bi ON aseg.isbn = bi.ISBN " +
                "WHERE aseg.status = 1 ");

        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            sql.append("AND (bi.Bookname LIKE ? OR bi.Author LIKE ?) ");
            countSql.append("AND (bi.Bookname LIKE ? OR bi.Author LIKE ?) ");
            params.add(kw);
            params.add(kw);
        }
        sql.append("GROUP BY bi.ISBN ORDER BY bi.ISBN DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        try {
            List<Object> countParams = new ArrayList<>(params.subList(0, params.size() - 2));
            int total = DBUtil.executeQueryScalar(countSql.toString(), Integer.class, countParams.toArray());
            List<Map<String, Object>> list = DBUtil.executeQuery(sql.toString(), rs -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("isbn", rs.getString("isbn"));
                map.put("bookName", rs.getString("bookName"));
                map.put("author", rs.getString("author"));
                map.put("pictureName", rs.getString("pictureName"));
                map.put("information", rs.getString("information"));
                map.put("chapterCount", rs.getInt("chapterCount"));
                return map;
            }, params.toArray());
            return ResultDTO.success(PageResultDTO.success((long) total, pageNum, pageSize, list));
        } catch (Exception e) {
            logger.error("查询有声书书籍列表失败", e);
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Map<String, Object>>> listAudiobookChapters(String isbn) {
        if (isbn == null || isbn.isEmpty()) return ResultDTO.fail("isbn不能为空");
        String sql = "SELECT DISTINCT aseg.chapter_number as chapterNumber, " +
                "COUNT(aseg.id) as segmentCount, " +
                "COALESCE(bc.Name, CONCAT('第', aseg.chapter_number, '章')) as chapterName " +
                "FROM audiobook_segment aseg " +
                "LEFT JOIN book_chapter bc ON bc.Isbn = aseg.isbn AND bc.Number = aseg.chapter_number " +
                "WHERE aseg.isbn = ? AND aseg.status = 1 " +
                "GROUP BY aseg.chapter_number, bc.Name ORDER BY aseg.chapter_number ASC";
        try {
            List<Map<String, Object>> list = DBUtil.executeQuery(sql, rs -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("chapterNumber", rs.getInt("chapterNumber"));
                map.put("segmentCount", rs.getInt("segmentCount"));
                map.put("chapterName", rs.getString("chapterName"));
                return map;
            }, isbn);
            return ResultDTO.success(list);
        } catch (Exception e) {
            logger.error("查询有声书章节列表失败", e);
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Long>> retryGenerateAudio(String isbn, Integer chapterNumber) {
        // 查询所有 status=0 且 textContent 不为空的段
        String sql = "SELECT id FROM audiobook_segment WHERE isbn=? AND chapter_number=? AND status=0 AND text_content IS NOT NULL AND text_content != ''";
        try {
            List<Long> pendingIds = DBUtil.executeQuery(sql, rs -> rs.getLong("id"), isbn, chapterNumber);
            if (pendingIds.isEmpty()) {
                // 也查一下 status=2 (失败的)
                String sql2 = "SELECT id FROM audiobook_segment WHERE isbn=? AND chapter_number=? AND status IN (0,2)";
                pendingIds = DBUtil.executeQuery(sql2, rs -> rs.getLong("id"), isbn, chapterNumber);
            }
            logger.info("📻 重试查询: isbn={} chapter={} → {} 段待生成", isbn, chapterNumber, pendingIds.size());
            return ResultDTO.success(pendingIds);
        } catch (Exception e) {
            logger.error("查询待生成段失败", e);
            return ResultDTO.fail("查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Map<String, Object>> generateAll(String isbn, String userId) {
        try {
            // 1. 查询该书所有章节
            String chapterSql = "SELECT Chapter_id, Number, Name FROM book_chapter WHERE Isbn=? ORDER BY Number";
            List<Map<String, Object>> chapters = DBUtil.executeQuery(chapterSql, rs -> {
                Map<String, Object> ch = new LinkedHashMap<>();
                ch.put("chapterId", rs.getString("Chapter_id"));
                ch.put("number", rs.getInt("Number"));
                ch.put("name", rs.getString("Name"));
                return ch;
            }, isbn);

            if (chapters.isEmpty()) {
                return ResultDTO.fail("该书没有章节数据");
            }
            logger.info("📻 批量生成有声书 isbn={} 共 {} 章", isbn, chapters.size());

            // 分段线程池（并行调用豆包 API 做 AI 分段） + 音频线程池
            ExecutorService segmentExecutor = Executors.newFixedThreadPool(3);
            ExecutorService audioExecutor = Executors.newFixedThreadPool(3);
            int[] totalSegments = {0};
            int[] successChapters = {0};
            int[] audioTasksSubmitted = {0};
            List<String> failedChapters = Collections.synchronizedList(new ArrayList<>());
            List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

            for (Map<String, Object> ch : chapters) {
                Integer chapterNumber = (Integer) ch.get("number");
                String chapterName = (String) ch.get("name");

                // 跳过已有分段的章节
                int existingCount = queryInt(
                        "SELECT COUNT(*) FROM audiobook_segment WHERE isbn=? AND chapter_number=?",
                        isbn, chapterNumber);
                if (existingCount > 0) {
                    logger.info("📻 第{}章「{}」已有 {} 段有声书，跳过", chapterNumber, chapterName, existingCount);
                    synchronized (successChapters) { successChapters[0]++; }
                    synchronized (totalSegments) { totalSegments[0] += existingCount; }
                    continue;
                }

                // 🔥 并行提交每章的分段任务
                java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        logger.info("📻 [并行] 开始第{}章「{}」AI分段...", chapterNumber, chapterName);
                        ResultDTO<Map<String, Object>> segResult = generateSegments(isbn, chapterNumber, userId);
                        if (!segResult.isSuccess()) {
                            failedChapters.add("第" + chapterNumber + "章「" + chapterName + "」: " + segResult.getMsg());
                            return;
                        }
                        synchronized (successChapters) { successChapters[0]++; }
                        Map<String, Object> data = segResult.getData();
                        int segCount = data != null && data.get("totalSegments") != null
                                ? ((Number) data.get("totalSegments")).intValue() : 0;
                        synchronized (totalSegments) { totalSegments[0] += segCount; }
                        logger.info("📻 [并行] 第{}章「{}」分段完成({}段)，立即启动音频生成...", chapterNumber, chapterName, segCount);

                        // 立即查询本章分段并提交音频生成
                        List<AudiobookSegment> newSegments = segmentDao.queryByIsbnAndChapterPage(isbn, chapterNumber, 1, 500);
                        for (AudiobookSegment seg : newSegments) {
                            if (seg.getStatus() == 0) {
                                final Long sid = seg.getId();
                                audioExecutor.submit(() -> {
                                    try {
                                        logger.info("🎵 并行生成音频 segmentId={} ch={}", sid, chapterNumber);
                                        generateAudio(sid);
                                    } catch (Exception e) {
                                        logger.error("🎵 音频生成异常 segmentId={}", sid, e);
                                    }
                                });
                                synchronized (audioTasksSubmitted) { audioTasksSubmitted[0]++; }
                            }
                        }
                    } catch (Exception e) {
                        failedChapters.add("第" + chapterNumber + "章「" + chapterName + "」: " + e.getMessage());
                        logger.error("📻 [并行] 第{}章分段异常", chapterNumber, e);
                    }
                }, segmentExecutor);
                futures.add(future);
            }

            // 等待所有分段任务完成
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
            segmentExecutor.shutdown();
            audioExecutor.shutdown();
            logger.info("📻 全书处理完毕: {}/{} 章成功, {} 段, 已提交 {} 个音频任务",
                    successChapters[0], chapters.size(), totalSegments[0], audioTasksSubmitted[0]);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalChapters", chapters.size());
            result.put("successChapters", successChapters[0]);
            result.put("totalSegments", totalSegments[0]);
            result.put("failedChapters", failedChapters);
            result.put("message", "已完成 " + successChapters[0] + "/" + chapters.size() + " 章的分段+音频生成，共 " + totalSegments[0] + " 个对话段");
            return ResultDTO.success(result);

        } catch (Exception e) {
            logger.error("📻 批量生成有声书失败 isbn={}", isbn, e);
            return ResultDTO.fail("批量生成失败：" + e.getMessage());
        }
    }

    // ====================== 工具方法 ======================
    private int queryInt(String sql, Object... params) {
        try {
            Integer val = DBUtil.executeQueryScalar(sql, Integer.class, params);
            return val != null ? val : 0;
        } catch (Exception e) {
            logger.warn("统计查询异常 sql={}", sql, e);
            return 0;
        }
    }

    // ====================== 豆包 API 调用 ======================
    private String callDoubaoApi(String userMessage, String systemPrompt) throws IOException {
        return callDoubaoApi(userMessage, systemPrompt, MAX_TOKENS);
    }

    private String callDoubaoApi(String userMessage, String systemPrompt, int maxTokens) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(CHAT_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(120000);

            JSONObject body = new JSONObject();
            body.put("model", ENDPOINT_ID);
            body.put("max_tokens", maxTokens);
            body.put("temperature", TEMPERATURE);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            body.put("messages", messages);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) response.append(line);
            }

            JSONObject resJson = JSON.parseObject(response.toString());
            return resJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
