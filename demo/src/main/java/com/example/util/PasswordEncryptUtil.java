package com.example.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.impl.UserInformationDaoImpl;

public class PasswordEncryptUtil {
    private static final Logger logger = LoggerFactory.getLogger(UserInformationDaoImpl.class);

    // 生成随机盐（每个用户注册时生成一个唯一的盐）
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16]; // 16字节的盐，足够安全
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes); // 转成字符串存库
    }

    // 密码加密（明文密码 + 盐 → 哈希后的密文）
    public static String encryptPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 把密码和盐拼接后加密
            byte[] hashBytes = digest.digest((password + salt).getBytes("UTF-8"));
            // 转成十六进制字符串（方便存库）
            StringBuilder hexStr = new StringBuilder();
            for (byte b : hashBytes) {
                hexStr.append(String.format("%02x", b));
            }
            return hexStr.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
             logger.error("密码哈希加密失败或不支持该编码", e);
            throw new RuntimeException("密码加密异常", e);
        }
    }
}
