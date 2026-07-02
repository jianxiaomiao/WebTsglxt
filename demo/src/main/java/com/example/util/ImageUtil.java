package com.example.util;

import com.example.config.AiConfig;
import com.example.entity.ForumImage;
import com.example.dao.ForumImageDao;
import com.example.dao.impl.ForumImageDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ImageUtil {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);
    private static final ForumImageDao forumImageDAO = new ForumImageDaoImpl();
    // 复用你原有的图片存储路径
   /* private static final String UPLOAD_PATH = "D:/WebTsglxt/forum_images/";*/
    // ImageUtil.java
    private static final String UPLOAD_PATH = "C:/WebTsglxt/forum_images/";

    /**
     * 🔥 公共方法：下载网络图片 → 保存本地 → 存入数据库 → 返回图片URL
     */
    public static String downloadAndSaveImage(String imageUrl, Integer commentId) throws Exception {
        // -------------- 以下代码 100% 复用你原有的 uploadByUrl 逻辑 --------------
        URI uri = URI.create(imageUrl);
        URL url = uri.toURL();

        // SSRF防护
        String host = url.getHost();
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.")) {
            throw new Exception("禁止访问内网地址");
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        if (conn.getResponseCode() != 200) {
            throw new Exception("图片下载失败");
        }

        // 获取图片后缀
        String contentType = conn.getContentType();
        String suffix = ".png";
        if (contentType.contains("jpeg")) suffix = ".jpg";
        else if (contentType.contains("webp")) suffix = ".webp";

        // 保存本地
        String newFileName = UUID.randomUUID() + suffix;
        String savePath = UPLOAD_PATH + newFileName;
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, Paths.get(savePath), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }

        // 存入数据库
        String dbImageUrl = "/forum_images/" + newFileName;
        ForumImage image = new ForumImage(commentId, dbImageUrl, suffix.substring(1));
        forumImageDAO.insert(image);

        logger.info("✅ 文生图保存成功：{}", dbImageUrl);
        return dbImageUrl;
    }
}
