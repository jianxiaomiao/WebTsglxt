package com.example.dao;

import com.example.entity.GraphNode;
import java.util.List;

public interface GraphNodeDao {
    Integer add(GraphNode node);
    void deleteById(Integer id);
    void updateById(GraphNode node);
    GraphNode selectById(Integer id);
    List<GraphNode> selectByMapId(String mapId);
    List<GraphNode> selectByMapUser(String mapUser);
    List<GraphNode> selectByType(Integer type);
    List<GraphNode> selectByMapAndUser(String mapId, String mapUser);

    // 根据图谱ID、用户ID、节点标题(人物名)查询已有节点，用于判断人物节点是否已存在
    GraphNode selectByMapUserAndHeader(String mapId, String mapUser, String contentHeader);
}