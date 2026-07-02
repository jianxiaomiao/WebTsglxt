package com.example.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

public class EmailUtil {
    private static final Logger logger = LoggerFactory.getLogger(EmailUtil.class);

    // 从配置文件或环境变量加载
    private static final String SENDER_EMAIL;
    private static final String AUTH_CODE;

    static {
        Properties emailProps = new Properties();
        String sender = null;
        String auth = null;

        try (InputStream in = EmailUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                emailProps.load(in);
                sender = emailProps.getProperty("email.sender");
                auth = emailProps.getProperty("email.auth.code");
                logger.info("✅ 已从 config.properties 加载邮箱配置");
            }
        } catch (Exception e) {
            logger.warn("⚠️  无法加载 config.properties，尝试从环境变量读取", e);
        }

        SENDER_EMAIL = coalesce(sender, System.getenv("EMAIL_SENDER"));
        AUTH_CODE = coalesce(auth, System.getenv("EMAIL_AUTH_CODE"));

        if (AUTH_CODE == null || AUTH_CODE.isBlank()) {
            logger.error("❌ 未找到邮箱授权码！请在 config.properties 或环境变量 EMAIL_AUTH_CODE 中配置");
        }
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    /**
     * 生成6位数字验证码
     */
    public static String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * 发送邮件验证码（QQ邮箱 465 SSL 模式，本地开发最稳定）
     */
    public static void sendEmailCode(String targetEmail, String code) throws MessagingException {
        Properties props = new Properties();
        // QQ SMTP 服务器地址
        props.put("mail.smtp.host", "smtp.qq.com");
        // SSL 端口 465
        props.put("mail.smtp.port", "465");
        // 开启身份验证
        props.put("mail.smtp.auth", "true");
        // 开启SSL加密
        props.put("mail.smtp.ssl.enable", "true");
        // SSL 套接字配置（修复握手异常）
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.socketFactory.port", "465");
        // 强制EHLO主机名，避免本地主机名被风控
        props.put("mail.smtp.localhost", "localhost");
        // 关闭8bit编码（兼容老旧邮件服务器）
        props.put("mail.smtp.use8bit", "false");

        Session session = Session.getInstance(props);
        session.setDebug(true); // 上线后可关闭调试

        // 构建邮件
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail));
        message.setSubject("注册验证码");
        // 纯英文内容优先（进一步降低风控概率）
        message.setText("Your verification code: " + code + ". Valid for 5 minutes.");

        // 手动连接、发送（Tomcat 稳定写法）
        Transport transport = session.getTransport("smtp");
        try {
            transport.connect(SENDER_EMAIL, AUTH_CODE);
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            transport.close();
        }
    }
}