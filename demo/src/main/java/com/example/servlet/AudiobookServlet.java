package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.AudiobookSegment;
import com.example.entity.UserInformation;
import com.example.service.AudiobookService;
import com.example.service.impl.AudiobookServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 有声书 Servlet
 * GET  /api/audiobook/progress?isbn=xxx&chapter=3      → 查询生成进度
 * GET  /api/audiobook/segments?isbn=xxx&chapter=3&pageNum=1&pageSize=20 → 分页查询分段
 * POST /api/audiobook/generate?isbn=xxx&chapter=3       → 生成角色分段
 * POST /api/audiobook/generateAudio?segmentId=1          → 为单段生成TTS音频
 */
@WebServlet("/api/audiobook/*")
public class AudiobookServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(AudiobookServlet.class);
    private final AudiobookService audiobookService = new AudiobookServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);

        String pathInfo = req.getPathInfo();

        // 🔥 /audio 路径不需要登录校验
        if ("/audio".equals(pathInfo)) {
            String fileName = req.getParameter("file");
            logger.info("🎵 请求音频文件: {}", fileName);
            if (fileName == null || fileName.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少 file 参数");
                return;
            }
            Path audioPath = Paths.get("D:/WebTsglxt/audiobook_audio", fileName);
            if (!Files.exists(audioPath)) {
                logger.warn("🎵 音频文件不存在: {}", audioPath);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "音频文件不存在");
                return;
            }
            long fileSize = Files.size(audioPath);
            logger.info("🎵 开始流式传输音频: {} ({} bytes)", fileName, fileSize);
            resp.setContentType("audio/mpeg");
            resp.setContentLengthLong(fileSize);
            try (OutputStream os = resp.getOutputStream()) {
                Files.copy(audioPath, os);
            }
            logger.info("🎵 音频传输完毕: {}", fileName);
            return;
        }

        // 其他路径需要登录
        if (!requireLogin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String isbn = req.getParameter("isbn");
            String chapterStr = req.getParameter("chapter");
            Integer chapterNumber = (chapterStr != null && !chapterStr.isEmpty()) ? Integer.parseInt(chapterStr) : null;
            logger.info("📻 有声书GET请求 path={}, isbn={}, chapter={}", pathInfo, isbn, chapterNumber);

            if ("/progress".equals(pathInfo)) {
                // GET /api/audiobook/progress?isbn=xxx&chapter=3
                ResultDTO<Map<String, Object>> result = audiobookService.getProgress(isbn, chapterNumber);
                logger.info("📻 生成进度查询结果: code={}", result.getCode());
                out.write(JSON.toJSONString(result));

            } else if ("/segments".equals(pathInfo)) {
                // GET /api/audiobook/segments?isbn=xxx&chapter=3&pageNum=1&pageSize=20
                String pageNumStr = req.getParameter("pageNum");
                String pageSizeStr = req.getParameter("pageSize");
                Integer pageNum = (pageNumStr != null) ? Integer.parseInt(pageNumStr) : 1;
                Integer pageSize = (pageSizeStr != null) ? Integer.parseInt(pageSizeStr) : 20;
                logger.info("📻 分段查询 pageNum={}, pageSize={}", pageNum, pageSize);

                ResultDTO<PageResultDTO<AudiobookSegment>> result =
                        audiobookService.queryByChapterPage(isbn, chapterNumber, pageNum, pageSize);
                logger.info("📻 分段查询结果: code={}, total={}", result.getCode(),
                        result.isSuccess() && result.getData() != null ? result.getData().getTotal() : 0);
                out.write(JSON.toJSONString(result));

            } else if ("/books".equals(pathInfo)) {
                // GET /api/audiobook/books?pageNum=1&pageSize=12&keyword=活着
                String pageNumStr = req.getParameter("pageNum");
                String pageSizeStr = req.getParameter("pageSize");
                String keyword = req.getParameter("keyword");
                Integer pageNum = (pageNumStr != null) ? Integer.parseInt(pageNumStr) : 1;
                Integer pageSize = (pageSizeStr != null) ? Integer.parseInt(pageSizeStr) : 12;
                logger.info("📚 有声书书籍列表 pageNum={} keyword={}", pageNum, keyword);
                ResultDTO<PageResultDTO<Map<String, Object>>> result =
                        audiobookService.listAudiobookBooks(keyword, pageNum, pageSize);
                out.write(JSON.toJSONString(result));

            } else if ("/chapters".equals(pathInfo)) {
                // GET /api/audiobook/chapters?isbn=xxx
                logger.info("📚 有声书章节列表 isbn={}", isbn);
                ResultDTO<List<Map<String, Object>>> result =
                        audiobookService.listAudiobookChapters(isbn);
                out.write(JSON.toJSONString(result));

            } else {
                logger.warn("📻 未知的GET请求路径: {}", pathInfo);
                out.write(JSON.toJSONString(ResultDTO.fail("未知的请求路径: " + pathInfo)));
            }
        } catch (Exception e) {
            logger.error("📻 有声书GET请求异常 path={}", pathInfo, e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常：" + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        if (!requireLogin(req, resp)) return;

        // 🔥 只有管理员(Type=3)才能生成有声书
        UserInformation currentUser = (UserInformation) req.getSession().getAttribute("currentUser");
        if (currentUser == null || currentUser.getType() == null || currentUser.getType() != 3) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(JSON.toJSONString(ResultDTO.fail("仅管理员可生成有声书")));
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");

        PrintWriter out = resp.getWriter();

        try {
            JSONObject params = JSON.parseObject(req.getInputStream(), JSONObject.class);
            String pathInfo = req.getPathInfo();
            String isbn = params.getString("isbn");
            Integer chapterNumber = params.getInteger("chapter");
            String userId = params.getString("userId");
            logger.info("📻 有声书POST请求 path={}, isbn={}, chapter={}, userId={}", pathInfo, isbn, chapterNumber, userId);

            if (userId == null || userId.isEmpty()) {
                // 从 session 获取 userId
                if (currentUser != null) {
                    userId = currentUser.getUserId();
                }
            }

            if ("/generate".equals(pathInfo)) {
                // POST /api/audiobook/generate  → 生成角色分段
                if (isbn == null || chapterNumber == null) {
                    logger.warn("📻 generate 缺少参数 isbn={} chapter={}", isbn, chapterNumber);
                    out.write(JSON.toJSONString(ResultDTO.fail("isbn 和 chapter 参数不能为空")));
                    out.flush();
                    return;
                }
                logger.info("📻 开始生成有声书角色分段 isbn={} chapter={}", isbn, chapterNumber);
                long startTime = System.currentTimeMillis();
                ResultDTO<Map<String, Object>> result =
                        audiobookService.generateSegments(isbn, chapterNumber, userId);
                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("📻 角色分段生成完毕 code={} 耗时={}ms", result.getCode(), elapsed);
                if (result.isSuccess()) {
                    logger.info("📻 生成结果: totalSegments={}", result.getData().get("totalSegments"));
                }
                out.write(JSON.toJSONString(result));

            } else if ("/generateAudio".equals(pathInfo)) {
                // POST /api/audiobook/generateAudio  → 为单段生成TTS
                Long segmentId = params.getLong("segmentId");
                if (segmentId == null) {
                    logger.warn("📻 generateAudio 缺少 segmentId");
                    out.write(JSON.toJSONString(ResultDTO.fail("segmentId 不能为空")));
                    out.flush();
                    return;
                }
                logger.info("📻 开始生成单段音频 segmentId={}", segmentId);
                long startTime = System.currentTimeMillis();
                ResultDTO<AudiobookSegment> result = audiobookService.generateAudio(segmentId);
                long elapsed = System.currentTimeMillis() - startTime;
                if (result.isSuccess()) {
                    logger.info("📻 音频生成完毕 segmentId={} audioUrl={} duration={}s 耗时={}ms",
                            segmentId, result.getData().getAudioUrl(),
                            result.getData().getAudioDuration(), elapsed);
                } else {
                    logger.warn("📻 音频生成失败 segmentId={} msg={} 耗时={}ms",
                            segmentId, result.getMsg(), elapsed);
                }
                out.write(JSON.toJSONString(result));

            } else if ("/retryGenerate".equals(pathInfo)) {
                // POST /api/audiobook/retryGenerate → 重试生成所有未完成的音频
                if (isbn == null || chapterNumber == null) {
                    out.write(JSON.toJSONString(ResultDTO.fail("isbn 和 chapter 参数不能为空")));
                    out.flush();
                    return;
                }
                logger.info("📻 重试生成音频 isbn={} chapter={}", isbn, chapterNumber);
                ResultDTO<List<Long>> result = audiobookService.retryGenerateAudio(isbn, chapterNumber);
                logger.info("📻 重试列表: {} 段待生成", result.isSuccess() && result.getData() != null ? result.getData().size() : 0);
                out.write(JSON.toJSONString(result));

            } else if ("/generateAll".equals(pathInfo)) {
                // POST /api/audiobook/generateAll → 批量生成整本书所有章节
                if (isbn == null) {
                    out.write(JSON.toJSONString(ResultDTO.fail("isbn 参数不能为空")));
                    out.flush();
                    return;
                }
                logger.info("📻 批量生成整本书有声书 isbn={}", isbn);
                long startTime = System.currentTimeMillis();
                ResultDTO<Map<String, Object>> result = audiobookService.generateAll(isbn, userId);
                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("📻 全书批量生成完毕 耗时={}ms", elapsed);
                out.write(JSON.toJSONString(result));

            } else {
                logger.warn("📻 未知的POST请求路径: {}", pathInfo);
                out.write(JSON.toJSONString(ResultDTO.fail("未知的请求路径: " + pathInfo)));
            }
        } catch (Exception e) {
            logger.error("📻 有声书POST请求异常 path={}", req.getPathInfo(), e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常：" + e.getMessage())));
        } finally {
            out.flush();
        }
    }
}
