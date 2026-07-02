package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.ForumImageDao;
import com.example.dao.impl.ForumImageDaoImpl;
import com.example.entity.ForumImage;
import com.example.dto.ResultDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@WebServlet("/api/user/comment/image")
@MultipartConfig(maxFileSize = 1024 * 1024 * 5) // 单张图片最大5MB
public class ForumImageServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(ForumImageServlet.class);
    private final ForumImageDao forumImageDAO = new ForumImageDaoImpl();
    private static final String UPLOAD_PATH;

    // 允许的图片后缀（白名单）
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
    );

    // 静态初始化块：自动判断环境并创建目录
    static {
        // ✅ 开发环境：强制使用D盘路径，覆盖自动判断
        /*UPLOAD_PATH = "D:/WebTsglxt/forum_images/";*/
        UPLOAD_PATH = "C:/WebTsglxt/forum_images/";
        logger.warn("【开发环境】强制使用本地图片路径：{}", UPLOAD_PATH);

        // 自动创建目录（如果不存在）
        File uploadDir = new File(UPLOAD_PATH);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            logger.info("自动创建图片存储目录：{}", UPLOAD_PATH);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // 🔥 核心修复：先判断是否是multipart请求，是就先解析
            String contentType = req.getContentType();
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                // 只要是multipart格式，先调用getParts()解析所有参数
                req.getParts();
                logger.info("已解析multipart/form-data请求");
            }

            // 🔥 现在再获取action，100%能拿到！
            String action = req.getParameter("action");
            logger.info("收到图片接口请求，action={}", action);

            // 1. 上传本地图片
            if ("upload".equals(action)) {
                Part filePart = req.getPart("image");
                String commentIdStr = req.getParameter("commentId");

                if (commentIdStr == null || commentIdStr.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("评论ID不能为空")));
                    return;
                }
                Integer commentId = Integer.parseInt(commentIdStr);

                if (filePart == null || filePart.getSize() == 0) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("请选择上传文件")));
                    return;
                }

                String originalFileName = filePart.getSubmittedFileName();
                String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));

                // ✅ 新增：本地图片后缀白名单验证
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(suffix.toLowerCase())) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("不支持的文件类型，仅支持jpg、jpeg、png、gif、bmp、webp")));
                    return;
                }

                String newFileName = UUID.randomUUID() + suffix;
                String imageUrl = "/forum_images/" + newFileName;

                File uploadDir = new File(UPLOAD_PATH);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                logger.info("正在写入文件：{}", UPLOAD_PATH + newFileName);
                try {
                    filePart.write(UPLOAD_PATH + newFileName);
                    logger.info("✅ 文件写入成功！路径：{}", UPLOAD_PATH + newFileName);
                } catch (Exception e) {
                    logger.error("❌ 文件写入失败！路径：{}", UPLOAD_PATH + newFileName, e);
                    out.write(JSON.toJSONString(ResultDTO.fail("文件写入失败")));
                    return;
                }
                ForumImage image = new ForumImage(commentId, imageUrl, suffix.substring(1));
                int rows = forumImageDAO.insert(image);

                if (rows > 0) {
                    ResultDTO<ForumImage> result = ResultDTO.success(image);
                    out.write(JSON.toJSONString(result));
                } else {
                    out.write(JSON.toJSONString(ResultDTO.fail("上传失败")));
                }
            }

            // 2. 删除单张图片
            else if ("delete".equals(action)) {
                String imageIdStr = req.getParameter("imageId");
                logger.info("删除图片，imageId={}", imageIdStr);

                if (imageIdStr == null || imageIdStr.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("图片ID不能为空")));
                    return;
                }
                Integer imageId = Integer.parseInt(imageIdStr);

                // ✅ 新增：删除数据库记录的同时删除本地文件
                ForumImage image = forumImageDAO.queryById(imageId);
                if (image != null) {
                    File imageFile = new File(UPLOAD_PATH + image.getImageUrl().replace("/forum_images/", ""));
                    if (imageFile.exists()) {
                        if (imageFile.delete()) {
                            logger.info("✅ 本地图片文件删除成功：{}", imageFile.getAbsolutePath());
                        } else {
                            logger.warn("⚠️ 本地图片文件删除失败：{}", imageFile.getAbsolutePath());
                        }
                    }
                }

                int rows = forumImageDAO.deleteById(imageId);

                if (rows > 0) {
                    out.write(JSON.toJSONString(ResultDTO.success("删除成功")));
                } else {
                    out.write(JSON.toJSONString(ResultDTO.fail("删除失败，图片不存在")));
                }
            }

            // 3. 更新图片关联的评论ID
            else if ("updateCommentId".equals(action)) {
                String imageIdStr = req.getParameter("imageId");
                String newCommentIdStr = req.getParameter("newCommentId");
                logger.info("更新图片评论ID，imageId={}, newCommentId={}", imageIdStr, newCommentIdStr);

                if (imageIdStr == null || newCommentIdStr == null) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("参数不能为空")));
                    return;
                }

                Integer imageId = Integer.parseInt(imageIdStr);
                Integer newCommentId = Integer.parseInt(newCommentIdStr);
                int rows = forumImageDAO.updateCommentId(imageId, newCommentId);

                if (rows > 0) {
                    out.write(JSON.toJSONString(ResultDTO.success("更新成功")));
                } else {
                    out.write(JSON.toJSONString(ResultDTO.fail("更新失败")));
                }
            }

            // 4. 通过URL上传网络图片（用于分享书籍封面等）
            else if ("uploadByUrl".equals(action)) {
                String commentIdStr = req.getParameter("commentId");
                String imageUrl = req.getParameter("imageUrl");

                if (commentIdStr == null || imageUrl == null || imageUrl.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("参数不能为空")));
                    return;
                }
                Integer commentId = Integer.parseInt(commentIdStr);

                logger.info("开始下载网络图片：{}", imageUrl);

                try {
                    // ✅ 优化1：使用URI解析URL，防止URL编码问题
                    URI uri = URI.create(imageUrl);
                    URL url = uri.toURL();

                    // ✅ 优化2：SSRF防护 - 禁止请求内网地址
                    String host = url.getHost();
                    if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.16.")) {
                        logger.warn("❌ 禁止请求内网地址：{}", host);
                        out.write(JSON.toJSONString(ResultDTO.paramError("不支持的图片地址")));
                        return;
                    }

                    // ✅ 优化3：只允许HTTP和HTTPS协议
                    if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                        logger.warn("❌ 不支持的协议：{}", url.getProtocol());
                        out.write(JSON.toJSONString(ResultDTO.paramError("不支持的图片地址")));
                        return;
                    }

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        logger.error("❌ 图片下载失败，HTTP状态码：{}", responseCode);
                        out.write(JSON.toJSONString(ResultDTO.fail("图片下载失败，服务器返回错误")));
                        return;
                    }

                    // ✅ 优化4：验证Content-Type是图片
                     contentType = conn.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        logger.error("❌ 不是有效的图片文件，Content-Type：{}", contentType);
                        out.write(JSON.toJSONString(ResultDTO.paramError("不是有效的图片文件")));
                        return;
                    }

                    // ✅ 优化5：使用try-with-resources自动关闭流，防止资源泄漏
                    try (InputStream in = conn.getInputStream()) {
                        // 从URL获取后缀，没有的话从Content-Type推断
                        String suffix = imageUrl.substring(imageUrl.lastIndexOf("."));
                        if (suffix.length() > 5 || !ALLOWED_IMAGE_EXTENSIONS.contains(suffix.toLowerCase())) {
                            // 从Content-Type推断后缀
                            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                                suffix = ".jpg";
                            } else if (contentType.contains("png")) {
                                suffix = ".png";
                            } else if (contentType.contains("gif")) {
                                suffix = ".gif";
                            } else if (contentType.contains("bmp")) {
                                suffix = ".bmp";
                            } else if (contentType.contains("webp")) {
                                suffix = ".webp";
                            } else {
                                suffix = ".jpg"; // 默认jpg
                            }
                        }

                        String newFileName = UUID.randomUUID() + suffix;
                        String savePath = UPLOAD_PATH + newFileName;

                        // 保存到本地
                        Files.copy(in, Paths.get(savePath), StandardCopyOption.REPLACE_EXISTING);
                        logger.info("✅ 网络图片下载成功，保存到：{}", savePath);

                        // 保存到数据库
                        String dbImageUrl = "/forum_images/" + newFileName;
                        ForumImage image = new ForumImage(commentId, dbImageUrl, suffix.substring(1));
                        int rows = forumImageDAO.insert(image);

                        if (rows > 0) {
                            ResultDTO<ForumImage> result = ResultDTO.success(image);
                            out.write(JSON.toJSONString(result));
                        } else {
                            // 数据库插入失败，删除本地文件
                            new File(savePath).delete();
                            out.write(JSON.toJSONString(ResultDTO.fail("上传失败")));
                        }
                    } finally {
                        conn.disconnect();
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("❌ 无效的图片URL：{}", imageUrl, e);
                    out.write(JSON.toJSONString(ResultDTO.paramError("无效的图片地址")));
                } catch (Exception e) {
                    logger.error("❌ 下载网络图片失败", e);
                    out.write(JSON.toJSONString(ResultDTO.fail("下载图片失败：" + e.getMessage())));
                }
            }

            // 5. 未知action
            else {
                logger.error("未知的操作类型：action={}", action);
                out.write(JSON.toJSONString(ResultDTO.paramError("未知的操作类型")));
            }

        } catch (Exception e) {
            logger.error("图片接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("操作失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}