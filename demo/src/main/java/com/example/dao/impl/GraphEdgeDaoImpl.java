package com.example.dao.impl;

import com.example.dao.GraphEdgeDao;
import com.example.entity.GraphEdge;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphEdgeDaoImpl implements GraphEdgeDao {
    private static final Logger logger = LoggerFactory.getLogger(GraphEdgeDaoImpl.class);

    @Override
    public Integer add(GraphEdge edge) {
        // 原有参数校验保留
        if (edge == null || edge.getSourceNodeId() == null || edge.getTargetNodeId() == null) {
            throw new IllegalArgumentException("起止节点ID不能为空");
        }

        String sql = "insert into graph_edge(map_id, map_user, source_node_id, target_node_id, source_port, target_port, label) " +
                "values (?,?,?,?,?,?,?)";
        try {
            // 调用工具类获取自增ID
            Integer autoId = DBUtil.executeUpdateReturnId(sql,
                    edge.getMapId(),
                    edge.getMapUser(),
                    edge.getSourceNodeId(),
                    edge.getTargetNodeId(),
                    edge.getSourcePort(),
                    edge.getTargetPort(),
                    edge.getLabel());

            edge.setId(autoId);

            return autoId;
        } catch (SQLException e) {
            logger.error("新增图谱连线异常", e);
            throw new RuntimeException("新增连线数据库操作失败", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) throw new IllegalArgumentException("连线ID不能为空");
        try {
            String sql = "delete from graph_edge where id=?";
            int rows = DBUtil.executeUpdate(sql, id);
            if (rows == 0) throw new RuntimeException("删除连线失败，ID不存在");
        } catch (SQLException e) {
            logger.error("删除连线异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateById(GraphEdge edge) {
        // 1. 基础入参校验（保留原有逻辑）
        if (edge == null || edge.getId() == null) {
            throw new IllegalArgumentException("连线ID不能为空");
        }

        StringBuilder sql = new StringBuilder("update graph_edge set ");
        List<Object> paramList = new ArrayList<>();

        try {
            // 2. 逐个字段判断：非 null 才拼接 SQL & 加入参数
            if (edge.getSourceNodeId() != null) {
                sql.append("source_node_id = ?, ");
                paramList.add(edge.getSourceNodeId());
            }
            if (edge.getTargetNodeId() != null) {
                sql.append("target_node_id = ?, ");
                paramList.add(edge.getTargetNodeId());
            }
            if (edge.getSourcePort() != null) {
                sql.append("source_port = ?, ");
                paramList.add(edge.getSourcePort());
            }
            if (edge.getTargetPort() != null) {
                sql.append("target_port = ?, ");
                paramList.add(edge.getTargetPort());
            }
            if (edge.getLabel() != null) {
                sql.append("label = ?, ");
                paramList.add(edge.getLabel());
            }

            // 3. 容错：没有任何字段需要更新，直接抛出异常（避免 SQL 语法错误）
            if (paramList.isEmpty()) {
                throw new RuntimeException("未传入任何可更新的字段，无需执行更新");
            }

            // 4. 删除最后多余的逗号 + 拼接 WHERE 条件
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where id = ?");
            paramList.add(edge.getId());

            // 5. 执行更新
            int rows = DBUtil.executeUpdate(sql.toString(), paramList.toArray());

            if (rows == 0) {
                throw new RuntimeException("修改连线失败，未匹配到对应数据");
            }

        } catch (Exception e) {
            logger.error("修改连线异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public GraphEdge selectById(Integer id) {
        if (id == null) return null;
        try {
            String sql = "select * from graph_edge where id=?";
            List<GraphEdge> list = DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphEdge(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("source_node_id"),
                            rs.getInt("target_node_id"),
                            rs.getString("source_port"),
                            rs.getString("target_port"),
                            rs.getString("label")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询连线异常", e);
            return null;
        }
    }

    @Override
    public List<GraphEdge> selectBySourceNode(Integer sourceNodeId) {
        if (sourceNodeId == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_edge where source_node_id=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphEdge(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("source_node_id"),
                            rs.getInt("target_node_id"),
                            rs.getString("source_port"),
                            rs.getString("target_port"),
                            rs.getString("label")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, sourceNodeId);
        } catch (SQLException e) {
            logger.error("根据起始节点查询连线异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphEdge> selectByTargetNode(Integer targetNodeId) {
        if (targetNodeId == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_edge where target_node_id=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphEdge(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("source_node_id"),
                            rs.getInt("target_node_id"),
                            rs.getString("source_port"),
                            rs.getString("target_port"),
                            rs.getString("label")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, targetNodeId);
        } catch (SQLException e) {
            logger.error("根据目标节点查询连线异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphEdge> selectByMapAndUser(String mapId, String mapUser) {
        if (mapId == null || mapUser == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_edge where map_id=? and map_user=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphEdge(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("source_node_id"),
                            rs.getInt("target_node_id"),
                            rs.getString("source_port"),
                            rs.getString("target_port"),
                            rs.getString("label")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, mapId, mapUser);
        } catch (SQLException e) {
            logger.error("根据图谱+用户查询连线异常", e);
            return Collections.emptyList();
        }
    }
}