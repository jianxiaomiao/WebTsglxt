package com.example.dao.impl;

import com.example.dao.GraphNodeDao;
import com.example.entity.GraphNode;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphNodeDaoImpl implements GraphNodeDao {
    private static final Logger logger = LoggerFactory.getLogger(GraphNodeDaoImpl.class);

    @Override
    /**
     * 新增节点，返回数据库自增主键ID
     */
    public Integer add(GraphNode node) {
        // 原有参数校验保留
        if (node == null || node.getMapId() == null || node.getMapUser() == null) {
            throw new IllegalArgumentException("图谱ID和用户ID不能为空");
        }

        String sql = "insert into graph_node(map_id, map_user, type, content_header, content, position_x, position_y, style) " +
                "values (?,?,?,?,?,?,?,?)";
        try {
            // 调用你DBUtil中【插入并返回自增ID】的方法
            Integer autoId = DBUtil.executeUpdateReturnId(sql,
                    node.getMapId(),
                    node.getMapUser(),
                    node.getType(),
                    node.getContentHeader(),
                    node.getContent(),
                    node.getPositionX(),
                    node.getPositionY(),
                    node.getStyle());


            // 主键回填到实体（可选，后续业务复用实体时用到）
            node.setId(autoId);

            return autoId;
        } catch (SQLException e) {
            logger.error("新增图谱节点异常", e);
            throw new RuntimeException("新增节点数据库操作失败", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) throw new IllegalArgumentException("节点ID不能为空");
        try {
            String sql = "delete from graph_node where id=?";
            int rows = DBUtil.executeUpdate(sql, id);
            if (rows == 0) throw new RuntimeException("删除节点失败，ID不存在");
        } catch (SQLException e) {
            logger.error("删除节点异常", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateById(GraphNode node) {
        if (node == null || node.getId() == null) {
            throw new IllegalArgumentException("节点/id不能为空");
        }
        try{
            StringBuilder sql = new StringBuilder("UPDATE graph_node SET ");
            List<Object> params = new ArrayList<>();

            // 逐个判断，非空才加入更新语句
            if (node.getType() != null) {
                sql.append("type = ?, ");
                params.add(node.getType());
            }
            if (node.getContentHeader() != null) {
                sql.append("content_header = ?, ");
                params.add(node.getContentHeader());
            }
            if (node.getContent() != null) {
                sql.append("content = ?, ");
                params.add(node.getContent());
            }
            if (node.getPositionX() != null) {
                sql.append("position_x = ?, ");
                params.add(node.getPositionX());
            }
            if (node.getPositionY() != null) {
                sql.append("position_y = ?, ");
                params.add(node.getPositionY());
            }
            if (node.getStyle() != null) {
                sql.append("style = ?, ");
                params.add(node.getStyle());
            }

            // 去掉最后一个逗号，拼接 WHERE
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id = ?");
            params.add(node.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException();
            }
        }
        catch (SQLException e) {
            logger.error("更新节点信息失败，节点id：{}", node.getId(), e);
            throw new RuntimeException("更新节点信息异常", e);
        }

    }

    @Override
    public GraphNode selectById(Integer id) {
        if (id == null) return null;
        try {
            String sql = "select * from graph_node where id=?";
            List<GraphNode> list = DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphNode(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("type"),
                            rs.getString("content_header"),
                            rs.getString("content"),
                            rs.getDouble("position_x"),
                            rs.getDouble("position_y"),
                            rs.getString("style")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询节点异常", e);
            return null;
        }
    }

    @Override
    public List<GraphNode> selectByMapId(String mapId) {
        if (mapId == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_node where map_id=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphNode(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("type"),
                            rs.getString("content_header"),
                            rs.getString("content"),
                            rs.getDouble("position_x"),
                            rs.getDouble("position_y"),
                            rs.getString("style")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, mapId);
        } catch (SQLException e) {
            logger.error("根据ISBN查询节点异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphNode> selectByMapUser(String mapUser) {
        if (mapUser == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_node where map_user=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphNode(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("type"),
                            rs.getString("content_header"),
                            rs.getString("content"),
                            rs.getDouble("position_x"),
                            rs.getDouble("position_y"),
                            rs.getString("style")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, mapUser);
        } catch (SQLException e) {
            logger.error("根据用户ID查询节点异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphNode> selectByType(Integer type) {
        if (type == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_node where type=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphNode(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("type"),
                            rs.getString("content_header"),
                            rs.getString("content"),
                            rs.getDouble("position_x"),
                            rs.getDouble("position_y"),
                            rs.getString("style")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, type);
        } catch (SQLException e) {
            logger.error("根据类型查询节点异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GraphNode> selectByMapAndUser(String mapId, String mapUser) {
        if (mapId == null || mapUser == null) return Collections.emptyList();
        try {
            String sql = "select * from graph_node where map_id=? and map_user=?";
            return DBUtil.executeQuery(sql, rs -> {
                try {
                    return new GraphNode(
                            rs.getInt("id"),
                            rs.getString("map_id"),
                            rs.getString("map_user"),
                            rs.getInt("type"),
                            rs.getString("content_header"),
                            rs.getString("content"),
                            rs.getDouble("position_x"),
                            rs.getDouble("position_y"),
                            rs.getString("style")
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, mapId, mapUser);
        } catch (SQLException e) {
            logger.error("根据图谱+用户查询节点异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public GraphNode selectByMapUserAndHeader(String mapId, String mapUser, String contentHeader) {
        if (mapId == null || mapUser == null || contentHeader == null) return null;
        try {
            String sql = "SELECT * FROM graph_node WHERE map_id=? AND map_user=? AND content_header=?";
            List<GraphNode> list = DBUtil.executeQuery(sql, rs -> {
                return new GraphNode(
                        rs.getInt("id"),
                        rs.getString("map_id"),
                        rs.getString("map_user"),
                        rs.getInt("type"),
                        rs.getString("content_header"),
                        rs.getString("content"),
                        rs.getDouble("position_x"),
                        rs.getDouble("position_y"),
                        rs.getString("style")
                );
            }, mapId, mapUser, contentHeader);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("查询人物节点失败 mapId:{},user:{},name:{}", mapId, mapUser, contentHeader, e);
            return null;
        }
    }
}