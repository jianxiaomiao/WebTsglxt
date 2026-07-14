package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet("/api/tts/*")
public class TtsServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(TtsServlet.class);
    private static final String EDGE_TTS_PATH = "D:\\python\\Scripts\\edge-tts.exe";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // GET /api/tts/cache?file=xxx.mp3 → 流式输出缓存音频
        setCorsHeader(req, resp);
        String pathInfo = req.getPathInfo();
        if ("/cache".equals(pathInfo)) {
            String fileName = req.getParameter("file");
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Path audioPath = Paths.get("D:/WebTsglxt/tts_cache", fileName);
            if (!Files.exists(audioPath)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType("audio/mpeg");
            resp.setContentLengthLong(Files.size(audioPath));
            try (OutputStream os = resp.getOutputStream()) {
                Files.copy(audioPath, os);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);

        String pathInfo = req.getPathInfo();

        try {
            // 🔥 批量模式：POST /api/tts/batch  [{text, voice}] → 并行生成 → [{url, text}]
            if ("/batch".equals(pathInfo)) {
                handleBatchGenerate(req, resp);
                return;
            }

            // 读取请求体，兼容数组格式（自动转为 batch 处理）
            byte[] bodyBytes = req.getInputStream().readAllBytes();
            String bodyStr = new String(bodyBytes).trim();
            if (bodyStr.startsWith("[")) {
                // 前端可能发送了数组格式的批量请求到非 /batch 路径，兜底处理
                logger.info("【TTS】检测到数组格式请求，自动转为批量处理");
                handleBatchGenerateWithBody(resp, bodyBytes);
                return;
            }

            JSONObject params = JSON.parseObject(bodyStr);
            String text = params.getString("text");
            String voice = params.getString("voice");
            boolean cacheMode = params.getBoolean("cache");

            if (voice == null || voice.isEmpty()) {
                voice = "zh-CN-YunxiNeural";
            }
            if (text == null || text.trim().isEmpty()) {
                logger.warn("【TTS 后端】前端传来的文本为空！");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            logger.info("【TTS 后端】生成语音: voice={}, text长度={}, cache={}", voice, text.length(), cacheMode);

            if (cacheMode) {
                String md5 = getMd5(text + ":" + voice);
                String cacheKey = "tts:cache:" + md5;
                try {
                    if (com.example.util.RedisUtil.exists(cacheKey)) {
                        String cachedUrl = com.example.util.RedisUtil.get(cacheKey);
                        String cachedFileName = cachedUrl.substring(cachedUrl.indexOf("file=") + 5);
                        Path cachedFilePath = Paths.get("D:/WebTsglxt/tts_cache", cachedFileName);
                        if (Files.exists(cachedFilePath) && Files.size(cachedFilePath) > 0) {
                            logger.info("⚡ 命中 TTS 单段音频缓存: {}", cachedUrl);
                            JSONObject result = new JSONObject();
                            result.put("url", cachedUrl);
                            result.put("size", Files.size(cachedFilePath));
                            resp.setContentType("application/json;charset=UTF-8");
                            resp.getWriter().write(result.toJSONString());
                            return;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("从 Redis 获取 TTS 缓存失败", e);
                }

                // 🔥 缓存模式：生成文件存到磁盘，返回URL（使用 --file 避免编码问题）
                String fileName = "tts_" + text.hashCode() + "_" + System.currentTimeMillis() + ".mp3";
                Path audioDir = Paths.get("D:/WebTsglxt/tts_cache");
                if (!Files.exists(audioDir)) Files.createDirectories(audioDir);
                Path audioPath = audioDir.resolve(fileName);

                // 写入临时文件，用 --file 代替 --text
                Path tempFile = Files.createTempFile("tts_cache_", ".txt");
                try {
                    Files.writeString(tempFile, text);
                    ProcessBuilder pb = new ProcessBuilder(
                            EDGE_TTS_PATH,
                            "--voice", voice,
                            "--file", tempFile.toString(),
                            "--write-media", audioPath.toString()
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0 && Files.size(audioPath) > 0) {
                        String url = "/api/tts/cache?file=" + fileName;
                        try {
                            com.example.util.RedisUtil.set(cacheKey, url);
                            com.example.util.RedisUtil.expire(cacheKey, 2592000); // 30天过期
                        } catch (Exception e) {
                            logger.warn("写入 Redis TTS 缓存失败", e);
                        }

                        JSONObject result = new JSONObject();
                        result.put("url", url);
                        result.put("size", Files.size(audioPath));
                        resp.setContentType("application/json;charset=UTF-8");
                        resp.getWriter().write(result.toJSONString());
                    } else {
                        String err = new String(process.getInputStream().readAllBytes());
                        logger.error("【TTS】缓存模式失败 exit={} err={}", exitCode, err);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } finally {
                    try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                }
                return;
            }

            // 流式模式：使用 --file + --write-media - 输出到 stdout
            resp.setContentType("audio/mpeg");
            resp.setHeader("Transfer-Encoding", "chunked");

            Path tempFile2 = Files.createTempFile("tts_stream_", ".txt");
            try {
                Files.writeString(tempFile2, text);
                ProcessBuilder pb = new ProcessBuilder(
                        EDGE_TTS_PATH,
                        "--voice", voice,
                        "--file", tempFile2.toString(),
                        "--write-media", "-"
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();
                logger.info("【TTS 后端】流式模式已启动");

                int totalBytes = 0;
                try (InputStream is = process.getInputStream();
                     OutputStream os = resp.getOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        os.flush();
                        totalBytes += bytesRead;
                    }
                }

                int exitCode = process.waitFor();
                logger.info("【TTS 后端】流式完成 exitCode={} totalBytes={}", exitCode, totalBytes);

                if (exitCode != 0) {
                    logger.error("【TTS 后端】edge-tts 执行失败 exitCode={}", exitCode);
                } else if (totalBytes == 0) {
                    logger.error("【TTS 后端】生成 0 字节音频数据");
                }
            } finally {
                try { Files.deleteIfExists(tempFile2); } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            logger.error("【TTS 后端】TTS 音频流生成时发生系统异常！", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String getMd5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.math.BigInteger no = new java.math.BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /** POST /api/tts/batch — 并行批量生成多段 TTS 音频，按原序返回 */
    private void handleBatchGenerate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleBatchGenerateWithBody(resp, req.getInputStream().readAllBytes());
    }

    private void handleBatchGenerateWithBody(HttpServletResponse resp, byte[] body) throws IOException {
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        java.io.PrintWriter sseOut = resp.getWriter();

        try {
            com.alibaba.fastjson.JSONArray segments = JSON.parseArray(new String(body));
            if (segments == null || segments.isEmpty()) {
                sseOut.write("event: error\ndata: empty\n\n");
                sseOut.flush();
                return;
            }

            int total = segments.size();
            logger.info("【TTS 批量 SSE】收到 {} 段文本，流式推送中...", total);

            // 固定 3 线程池，避免 ForkJoinPool 无限并行导致 edge-tts 打架
            ExecutorService ttsPool = Executors.newFixedThreadPool(3);
            Path audioDir = Paths.get("D:/WebTsglxt/tts_cache");
            if (!Files.exists(audioDir)) Files.createDirectories(audioDir);
            java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < total; i++) {
                final int index = i;
                com.alibaba.fastjson.JSONObject seg = segments.getJSONObject(index);
                String text = seg.getString("text");
                String voice = seg.getString("voice");
                if (voice == null || voice.isEmpty()) voice = "zh-CN-XiaoxiaoNeural";

                if (text == null || text.trim().isEmpty()) {
                    // 空文本直接跳过
                    sseOut.write("event: segment\ndata: {\"index\":" + index + ",\"url\":null}\n\n");
                    sseOut.flush();
                    completed.incrementAndGet();
                    continue;
                }

                final String fVoice = voice;
                final String fText = text;
                java.util.concurrent.Future<?> f = ttsPool.submit(() -> {
                    try {
                        String md5 = getMd5(fText + ":" + fVoice);
                        String cacheKey = "tts:cache:" + md5;
                        try {
                            if (com.example.util.RedisUtil.exists(cacheKey)) {
                                String cachedUrl = com.example.util.RedisUtil.get(cacheKey);
                                String cachedFileName = cachedUrl.substring(cachedUrl.indexOf("file=") + 5);
                                Path cachedFilePath = audioDir.resolve(cachedFileName);
                                if (Files.exists(cachedFilePath) && Files.size(cachedFilePath) > 0) {
                                    logger.info("⚡ 命中 TTS 批量音频缓存, 段{}: {}", index, cachedUrl);
                                    synchronized (sseOut) {
                                        sseOut.write("event: segment\ndata: {\"index\":" + index +
                                                ",\"url\":\"" + cachedUrl + "\"}\n\n");
                                        sseOut.flush();
                                    }
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("从 Redis 获取 TTS 批量缓存失败", e);
                        }

                        String fileName = "tts_batch_" + fText.hashCode() + "_" + index + "_" +
                                System.currentTimeMillis() + ".mp3";
                        Path audioPath = audioDir.resolve(fileName);
                        Path tempFile = Files.createTempFile("tts_batch_", ".txt");
                        try {
                            Files.writeString(tempFile, fText);
                            ProcessBuilder pb = new ProcessBuilder(
                                    EDGE_TTS_PATH, "--voice", fVoice,
                                    "--file", tempFile.toString(),
                                    "--write-media", audioPath.toString()
                            );
                            pb.redirectErrorStream(true);
                            Process process = pb.start();
                            int exitCode = process.waitFor();

                            String url = null;
                            if (exitCode == 0 && Files.size(audioPath) > 0) {
                                url = "/api/tts/cache?file=" + fileName;
                                try {
                                    com.example.util.RedisUtil.set(cacheKey, url);
                                    com.example.util.RedisUtil.expire(cacheKey, 2592000); // 30天过期
                                } catch (Exception e) {
                                    logger.warn("写入 Redis TTS 缓存失败", e);
                                }
                            }
                            // SSE 推送：生成完一段立即发送
                            synchronized (sseOut) {
                                sseOut.write("event: segment\ndata: {\"index\":" + index +
                                        ",\"url\":" + (url != null ? "\"" + url + "\"" : "null") + "}\n\n");
                                sseOut.flush();
                            }
                        } finally {
                            try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                        }
                    } catch (Exception e) {
                        synchronized (sseOut) {
                            sseOut.write("event: segment\ndata: {\"index\":" + index + ",\"url\":null}\n\n");
                            sseOut.flush();
                        }
                        logger.error("【TTS 批量】段{}异常", index, e);
                    } finally {
                        int done = completed.incrementAndGet();
                        if (done >= total) {
                            synchronized (sseOut) {
                                sseOut.write("event: done\ndata: {\"total\":" + total + "}\n\n");
                                sseOut.flush();
                            }
                        }
                    }
                });
                futures.add(f);
            }

            // 等待所有任务完成
            for (java.util.concurrent.Future<?> f : futures) {
                try { f.get(); } catch (Exception ignored) {}
            }
            ttsPool.shutdown();

        } catch (Exception e) {
            logger.error("【TTS 批量】异常", e);
            sseOut.write("event: error\ndata: " + e.getMessage() + "\n\n");
            sseOut.flush();
        } finally {
            sseOut.close();
        }
    }
}