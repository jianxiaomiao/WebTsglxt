package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.dao.GraphEdgeDao;
import com.example.dao.GraphNodeDao;
import com.example.dao.impl.GraphEdgeDaoImpl;
import com.example.dao.impl.GraphNodeDaoImpl;
import com.example.dto.GraphDataDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookCharacterLore;
import com.example.entity.GraphEdge;
import com.example.entity.GraphNode;
import com.example.service.GraphService;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphServiceImpl implements GraphService {
    private static final Logger logger = LoggerFactory.getLogger(GraphServiceImpl.class);
    private final GraphNodeDao nodeDao;
    private final GraphEdgeDao edgeDao;

    public GraphServiceImpl() {
        this.nodeDao = new GraphNodeDaoImpl();
        this.edgeDao = new GraphEdgeDaoImpl();
    }

    @Override
    public ResultDTO<Integer> addNode(GraphNode node) {
        try {
            Integer newNodeId = nodeDao.add(node);
            return ResultDTO.success(newNodeId);
        } catch (Exception e) {
            return ResultDTO.fail("新增节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteNode(Integer id) {
        try {
            // 🔥 事务：先删除所有关联边，再删除节点（同一连接保证原子性）
            DBUtil.executeTransaction(conn -> {
                try {
                    // 1. 删除所有以该节点为 source 或 target 的边
                    String deleteEdgesSql = "delete from graph_edge where source_node_id=? or target_node_id=?";
                    DBUtil.executeUpdateWithConn(conn, deleteEdgesSql, id, id);

                    // 2. 删除节点本身（用事务连接，不用 DAO 以避免连接池不一致）
                    String deleteNodeSql = "delete from graph_node where id=?";
                    int rows = DBUtil.executeUpdateWithConn(conn, deleteNodeSql, id);
                    if (rows == 0) throw new RuntimeException("节点不存在或已被删除");
                } catch (Exception e) {
                    throw new RuntimeException("级联删除失败", e);
                }
            });
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("删除节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateNode(GraphNode node) {
        try {
            nodeDao.updateById(node);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("修改节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<GraphNode> getNodeById(Integer id) {
        try {
            GraphNode node = nodeDao.selectById(id);
            return ResultDTO.success(node);
        } catch (Exception e) {
            return ResultDTO.fail("查询节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<GraphNode>> getNodesByMapId(String mapId) {
        try {
            List<GraphNode> list = nodeDao.selectByMapId(mapId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("按ISBN查询节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<GraphNode>> getNodesByMapUser(String mapUser) {
        try {
            List<GraphNode> list = nodeDao.selectByMapUser(mapUser);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("按用户查询节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<GraphNode>> getNodesByType(Integer type) {
        try {
            List<GraphNode> list = nodeDao.selectByType(type);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("按类型查询节点失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Integer> addEdge(GraphEdge edge) {
        try {
            Integer newEdgeId =edgeDao.add(edge);
            return ResultDTO.success(newEdgeId);
        } catch (Exception e) {
            return ResultDTO.fail("新增连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteEdge(Integer id) {
        try {
            edgeDao.deleteById(id);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("删除连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateEdge(GraphEdge edge) {
        try {
            edgeDao.updateById(edge);
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.fail("修改连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<GraphEdge> getEdgeById(Integer id) {
        try {
            GraphEdge edge = edgeDao.selectById(id);
            return ResultDTO.success(edge);
        } catch (Exception e) {
            return ResultDTO.fail("查询连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<GraphEdge>> getEdgesBySource(Integer sourceNodeId) {
        try {
            List<GraphEdge> list = edgeDao.selectBySourceNode(sourceNodeId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("按起始节点查询连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<GraphEdge>> getEdgesByTarget(Integer targetNodeId) {
        try {
            List<GraphEdge> list = edgeDao.selectByTargetNode(targetNodeId);
            return ResultDTO.success(list);
        } catch (Exception e) {
            return ResultDTO.fail("按目标节点查询连线失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<GraphDataDTO> getFullGraph(String mapId, String mapUser) {
        try {
            List<GraphNode> nodes = nodeDao.selectByMapAndUser(mapId, mapUser);
            List<GraphEdge> edges = edgeDao.selectByMapAndUser(mapId, mapUser);
            GraphDataDTO data = new GraphDataDTO(nodes, edges);
            return ResultDTO.success(data);
        } catch (Exception e) {
            logger.error("查询完整图谱异常", e);
            return ResultDTO.fail("查询图谱失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> batchCreateRoleGraph(String mapId, String mapUser, List<BookCharacterLore> characterList) {
        Map<String, Integer> nameNodeMap = new HashMap<>();
        try {
            DBUtil.executeTransaction(conn -> {
                // 1. 批量创建人物节点
                for (BookCharacterLore lore : characterList) {
                    String charName = lore.getCharacter_name();
                    GraphNode exist = nodeDao.selectByMapUserAndHeader(mapId, mapUser, charName);
                    Integer nodeId;
                    if (exist == null) {
                        String sql = "insert into graph_node(map_id,map_user,type,content_header,content,position_x,position_y,style) values (?,?,?,?,?,?,?,?)";
                        String content = lore.getPersonality() + "\n" + lore.getBiography();
                        try {
                            nodeId = DBUtil.executeUpdateReturnId(sql, mapId, mapUser, 2, charName, content,
                                    150+Math.random()*300, 120+Math.random()*250, "");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        nodeId = exist.getId();
                    }
                    nameNodeMap.put(charName, nodeId);
                }
                // 2. 批量创建关系连线
                for (BookCharacterLore lore : characterList) {
                    Integer sourceId = nameNodeMap.get(lore.getCharacter_name());
                    JSONObject rel = JSON.parseObject(lore.getRelationship_json());
                    for (String targetName : rel.keySet()) {
                        Integer targetId = nameNodeMap.get(targetName);
                        if(targetId == null) continue;
                        String edgeSql = "insert into graph_edge(map_id,map_user,source_node_id,target_node_id,label) values (?,?,?,?,?)";
                        try {
                            DBUtil.executeUpdateWithConn(conn, edgeSql, mapId, mapUser, sourceId, targetId, rel.getString(targetName));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            return ResultDTO.success(null);
        } catch (SQLException e) {
            logger.error("批量创建人物图谱事务失败 mapId:{} user:{}", mapId, mapUser, e);
            return ResultDTO.fail("人物图谱生成失败，全部操作已回滚："+e.getMessage());
        }
    }
}