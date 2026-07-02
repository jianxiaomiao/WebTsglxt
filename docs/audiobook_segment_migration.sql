-- ============================================
-- 数据库迁移：有声书角色分段表
-- 版本：v2.2
-- 日期：2025-06-26
-- ============================================

CREATE TABLE IF NOT EXISTS `audiobook_segment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `isbn` VARCHAR(20) NOT NULL COMMENT '书籍ISBN',
  `chapter_number` INT NOT NULL COMMENT '章节号',
  `paragraph_id` VARCHAR(64) NOT NULL COMMENT '段落ID（关联 book_chapter_paragraph.id）',
  `sort_order` INT NOT NULL COMMENT '播放顺序',
  `role_type` VARCHAR(20) NOT NULL COMMENT 'AI标注的角色：旁白/青年男/青年女/中年男/小女孩/老爷爷',
  `emotion` VARCHAR(10) DEFAULT NULL COMMENT '情感：欢快/悲伤/愤怒/温柔/平静',
  `audio_url` VARCHAR(500) DEFAULT NULL COMMENT '生成的音频文件URL',
  `audio_duration` FLOAT DEFAULT NULL COMMENT '音频时长（秒）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待生成 1=已生成 2=生成失败',
  `text_content` TEXT DEFAULT NULL COMMENT 'AI切分的对话段原文内容（供TTS使用）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_isbn_chapter` (`isbn`, `chapter_number`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='有声书角色分段表';
