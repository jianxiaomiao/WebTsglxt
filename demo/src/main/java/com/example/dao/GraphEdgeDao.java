package com.example.dao;

import com.example.entity.GraphEdge;
import java.util.List;

public interface GraphEdgeDao {
    Integer add(GraphEdge edge);
    void deleteById(Integer id);
    void updateById(GraphEdge edge);
    GraphEdge selectById(Integer id);
    List<GraphEdge> selectBySourceNode(Integer sourceNodeId);
    List<GraphEdge> selectByTargetNode(Integer targetNodeId);
    List<GraphEdge> selectByMapAndUser(String mapId, String mapUser);
}