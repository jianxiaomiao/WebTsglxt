-- ============================================
-- 成就系统初始数据
-- 版本：v1.0 · 日期：2026-06-27
-- ============================================

-- 成就定义表（字典数据，系统初始化时执行一次即可）
INSERT INTO `achievement_def` (`id`, `name`, `description`, `category`, `rarity`, `condition_json`, `exp_reward`) VALUES
-- 阅读里程类
('ACH_READ_01', '初窥门径', '读完第1本书', 'reading', 'bronze', '{"type":"total_books","threshold":1}', 20),
('ACH_READ_02', '渐入佳境', '读完10本书', 'reading', 'silver', '{"type":"total_books","threshold":10}', 50),
('ACH_READ_03', '万卷书海', '读完50本书', 'reading', 'gold', '{"type":"total_books","threshold":50}', 150),

-- 阅读时长类
('ACH_TIME_01', '沉浸时刻', '累计阅读达到10小时', 'reading', 'bronze', '{"type":"total_duration","threshold":600}', 20),
('ACH_TIME_02', '忘我之境', '累计阅读达到100小时', 'reading', 'gold', '{"type":"total_duration","threshold":6000}', 120),

-- 连续打卡类
('ACH_STREAK_01', '黄金周', '连续7天产生阅读记录', 'reading', 'silver', '{"type":"consecutive_days","threshold":7}', 40),
('ACH_STREAK_02', '月度恒星', '连续30天产生阅读记录', 'reading', 'gold', '{"type":"consecutive_days","threshold":30}', 100),

-- 社交类
('ACH_SOCIAL_01', '破冰者', '论坛首次发帖或评论', 'social', 'bronze', '{"type":"total_posts","threshold":1}', 15),
('ACH_SOCIAL_02', '魅力之源', '评论/帖子累计获100赞', 'social', 'silver', '{"type":"total_likes","threshold":100}', 60),
('ACH_SOCIAL_03', '桃园结义', '交到5个好友', 'social', 'bronze', '{"type":"total_friends","threshold":5}', 25),

-- 收藏类
('ACH_COLLECT_01', '藏书家', '收藏达到10本书', 'collection', 'bronze', '{"type":"total_collections","threshold":10}', 20),
('ACH_COLLECT_02', '整理大师', '创建3个自定义书架分组', 'collection', 'silver', '{"type":"total_groups","threshold":3}', 40),

-- 互动类
('ACH_INTERACT_01', '知识探索', '首次通过AI管家互动', 'social', 'bronze', '{"type":"ai_interaction","threshold":1}', 15),

-- 隐藏成就
('ACH_HIDDEN_01', '🦉 深夜猫头鹰', '你曾在深夜与书为伴…', 'hidden', 'gold', '{"type":"late_night_reader","threshold":1}', 200),
('ACH_HIDDEN_02', '🐛 疯狂书虫', '24小时内读完3本书（传说级别）', 'hidden', 'legendary', '{"type":"speed_reader","threshold":3}', 500);
