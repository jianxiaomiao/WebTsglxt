package com.example.listener;

import com.example.dto.ResultDTO;
import com.example.entity.AudiobookSegment;
import com.example.service.AudiobookService;
import com.example.service.impl.AudiobookServiceImpl;
import com.example.util.DBUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 有声书启动重试监听器
 * 应用启动时检查 audiobook_segment 表中 status=0(待生成) 或 status=2(失败) 的记录，
 * 异步重新触发 TTS 音频生成。
 */
@WebListener
public class AudiobookRetryListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AudiobookRetryListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("📻 有声书重试监听器启动，检查待生成音频...");

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "audiobook-retry");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> {
            try {
                Thread.sleep(10000);
                retryPendingSegments();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("📻 有声书重试异常", e);
            }
        });

        executor.shutdown();
    }

    private void retryPendingSegments() {
        try {
            String sql = "SELECT id FROM audiobook_segment " +
                    "WHERE (status = 0 AND text_content IS NOT NULL AND text_content != '') OR status = 2 " +
                    "ORDER BY isbn, chapter_number, sort_order";

            List<Long> pendingIds = DBUtil.executeQuery(sql, rs -> rs.getLong("id"));

            if (pendingIds.isEmpty()) {
                logger.info("📻 没有待生成的有声书音频段");
                return;
            }

            logger.info("📻 发现 {} 段待生成音频，开始逐个生成...", pendingIds.size());

            // 复用已有的 AudiobookServiceImpl（使用已配置好的 VOICE_MAP + edge-tts 逻辑）
            AudiobookService service = new AudiobookServiceImpl();
            int success = 0;
            int failed = 0;

            for (Long segmentId : pendingIds) {
                try {
                    logger.info("📻 生成音频 segmentId={}", segmentId);
                    ResultDTO<AudiobookSegment> result = service.generateAudio(segmentId);
                    if (result.isSuccess()) {
                        success++;
                    } else {
                        logger.warn("📻 音频生成失败 segmentId={} msg={}", segmentId, result.getMsg());
                        failed++;
                    }
                } catch (Exception e) {
                    logger.error("📻 音频生成异常 segmentId={}", segmentId, e);
                    failed++;
                }
            }

            logger.info("📻 有声书重试完成: 成功 {} 段, 失败 {} 段", success, failed);

        } catch (SQLException e) {
            logger.error("📻 查询待生成段失败", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("📻 有声书重试监听器关闭");
    }
}
