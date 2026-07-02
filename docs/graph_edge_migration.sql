-- ============================================
-- 数据库迁移：graph_edge 表添加连接桩字段
-- 版本：v2.1
-- 日期：2025-06-26
-- ============================================

-- 在 graph_edge 表添加 source_port 和 target_port 列
-- 用于存储连线从源节点/目标节点的哪个端口连接
-- 端口ID：p_top / p_bottom / p_left / p_right

ALTER TABLE `graph_edge`
    ADD COLUMN `source_port` VARCHAR(32) NULL COMMENT '源节点连接桩ID（p_top/p_bottom/p_left/p_right）' AFTER `target_node_id`,
    ADD COLUMN `target_port` VARCHAR(32) NULL COMMENT '目标节点连接桩ID' AFTER `source_port`;
