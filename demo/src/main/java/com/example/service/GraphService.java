package com.example.service;

import com.example.dto.GraphDataDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookCharacterLore;
import com.example.entity.GraphEdge;
import com.example.entity.GraphNode;
import java.util.List;

public interface GraphService {
    ResultDTO<Integer> addNode(GraphNode node);
    ResultDTO<Void> deleteNode(Integer id);
    ResultDTO<Void> updateNode(GraphNode node);
    ResultDTO<GraphNode> getNodeById(Integer id);
    ResultDTO<List<GraphNode>> getNodesByMapId(String mapId);
    ResultDTO<List<GraphNode>> getNodesByMapUser(String mapUser);
    ResultDTO<List<GraphNode>> getNodesByType(Integer type);

    ResultDTO<Integer> addEdge(GraphEdge edge);
    ResultDTO<Void> deleteEdge(Integer id);
    ResultDTO<Void> updateEdge(GraphEdge edge);
    ResultDTO<GraphEdge> getEdgeById(Integer id);
    ResultDTO<List<GraphEdge>> getEdgesBySource(Integer sourceNodeId);
    ResultDTO<List<GraphEdge>> getEdgesByTarget(Integer targetNodeId);

    ResultDTO<GraphDataDTO> getFullGraph(String mapId, String mapUser);

    // 批量创建人物节点+关系连线，原子事务
    ResultDTO<Void> batchCreateRoleGraph(String mapId, String mapUser, List<BookCharacterLore> characterList);
}