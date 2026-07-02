-- ============================================
-- 数据库迁移：内容审核系统
-- 版本：v3.0 · 日期：2025-06-26
-- ============================================

-- 1. 书籍评论审核
ALTER TABLE `book_comment` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝';
-- 2. 段落评论审核
ALTER TABLE `paragraph_comment` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝';
-- 3. 论坛评论审核
ALTER TABLE `user_comment` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝';
-- 4. 漂流瓶审核（status已被漂流状态占用，用audit_status）
ALTER TABLE `book_bottle` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待审核 1=通过 2=拒绝';

-- 历史数据全部设为通过
UPDATE `book_comment` SET `status` = 1;
UPDATE `paragraph_comment` SET `status` = 1;
UPDATE `user_comment` SET `status` = 1;
UPDATE `book_bottle` SET `audit_status` = 1;
