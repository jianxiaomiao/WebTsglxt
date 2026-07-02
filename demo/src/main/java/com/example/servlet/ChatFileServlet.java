package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.ChatFileDao;
import com.example.dao.impl.ChatFileDaoImpl;
import com.example.entity.ChatFile;
import com.example.dto.ResultDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@WebServlet("/api/chat/file")
@MultipartConfig(maxFileSize = 1024 * 1024 * 50) // 单文件最大50MB
public class ChatFileServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(ChatFileServlet.class);
    private final ChatFileDao chatFileDao = new ChatFileDaoImpl();

    // 文件保存本地路径（对标图片D:/WebTsglxt/forum_images/）
    private static final String UPLOAD_PATH;
    // 支持的文件后缀白名单（加上了视频格式 .mp4, .mov, .webm, .avi）
    private static final Set<String> ALLOW_EXT = new HashSet<>(
            Arrays.asList(".txt", ".pdf", ".doc", ".docx", ".xls", ".xlsx",
                    ".ppt", ".pptx", ".mp3", ".wav", ".ogg", ".jpg", ".png", ".gif",
                    ".jpeg", ".bmp", ".webp", ".mp4", ".mov", ".webm", ".avi")
    );

    static {
        /*UPLOAD_PATH = "D:/WebTsglxt/chat_files/";*/
        UPLOAD_PATH = "C:/WebTsglxt/chat_files/";
        File dir = new File(UPLOAD_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("创建聊天文件存储目录：{}", UPLOAD_PATH);
        }
    }

    // ✅ 修复1：重写doGet，调用doPost → 支持GET请求（window.open/axios.get都能用）
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        // 解析multipart
        if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {
            req.getParts();
        }
        String action = req.getParameter("action");
        logger.info("聊天文件接口 action={}", action);

        // ========== 1. 文件上传 ==========
        if ("upload".equals(action)) {
            // 仅当前分支需要JSON输出
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            Part filePart = req.getPart("file");
            String userId = req.getParameter("userId");
            if (userId == null || userId.isBlank()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("上传用户ID不能为空")));
                return;
            }
            if (filePart == null || filePart.getSize() <= 0) {
                out.write(JSON.toJSONString(ResultDTO.paramError("请选择文件")));
                return;
            }
            String originalName = filePart.getSubmittedFileName();
            String suffix = originalName.substring(originalName.lastIndexOf("."));
            String suffixLow = suffix.toLowerCase();
            // 校验后缀白名单
            if (!ALLOW_EXT.contains(suffixLow)) {
                out.write(JSON.toJSONString(ResultDTO.paramError("不支持此文件格式")));
                return;
            }
            // UUID重命名
            String newName = UUID.randomUUID() + suffixLow;
            String saveFullPath = UPLOAD_PATH + newName;
            String dbUrl = "/chat_files/" + newName;

            // 落地本地
            try {
                filePart.write(saveFullPath);
                logger.info("文件落地成功：{}", saveFullPath);
            } catch (Exception e) {
                logger.error("文件写入磁盘失败", e);
                out.write(JSON.toJSONString(ResultDTO.fail("文件保存失败")));
                return;
            }
            // 入库chat_file
            ChatFile file = new ChatFile(
                    suffixLow.replace(".", ""),
                    dbUrl,
                    userId,
                    originalName,
                    filePart.getSize()
            );
            int row = chatFileDao.insert(file);
            if (row > 0) {
                out.write(JSON.toJSONString(ResultDTO.success(file)));
            } else {
                new File(saveFullPath).delete();
                out.write(JSON.toJSONString(ResultDTO.fail("数据库保存失败")));
            }
            out.flush();
            out.close();
        }
        // ========== 2. 根据url查询文件详情 ==========
        else if ("getByUrl".equals(action)) {
            // 仅当前分支需要JSON输出
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            String url = req.getParameter("url");
            ChatFile file = chatFileDao.queryByUrl(url);
            if (file == null) {
                out.write(JSON.toJSONString(ResultDTO.fail("文件不存在")));
            } else {
                out.write(JSON.toJSONString(ResultDTO.success(file)));
            }
            out.flush();
            out.close();
        }
        // ========== 3. 文件下载（无JSON，纯二进制流） ==========
        else if ("preview".equals(action)) {
            String url = req.getParameter("url");
            if (url == null || url.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // 查询文件信息
            ChatFile chatFile = chatFileDao.queryByUrl(url);
            if (chatFile == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 拼接本地文件路径
            String fileName = chatFile.getOriginalName();
            String suffix = chatFile.getFileType();
            String filePath = UPLOAD_PATH + url.substring(url.lastIndexOf("/") + 1);
            File file = new File(filePath);

            if (!file.exists()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // MIME类型配置
            Map<String, String> mimeMap = new HashMap<>();
            mimeMap.put("pdf", "application/pdf");
            mimeMap.put("txt", "text/plain");
            mimeMap.put("doc", "application/msword");
            mimeMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            mimeMap.put("xls", "application/vnd.ms-excel");
            mimeMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            mimeMap.put("ppt", "application/vnd.ms-powerpoint");
            mimeMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            mimeMap.put("jpg", "image/jpeg");
            mimeMap.put("png", "image/png");
            mimeMap.put("gif", "image/gif");

            // 设置响应头
            String contentType = mimeMap.getOrDefault(suffix, "application/octet-stream");
            resp.setContentType(contentType);
            String encodeName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            // 全部文件强制下载
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodeName + "\"");

            // 输出文件流
            try (java.io.FileInputStream in = new java.io.FileInputStream(file);
                 java.io.OutputStream outStream = resp.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                outStream.flush();
            }
            return;
        }
        // ========== 4. 非法操作 ==========
        else {
            resp.setContentType("application/json;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.write(JSON.toJSONString(ResultDTO.paramError("非法操作")));
            out.flush();
            out.close();
        }
    }
}