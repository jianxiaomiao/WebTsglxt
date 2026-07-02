package com.example.dto;

import com.example.entity.GraphEdge;
import com.example.entity.GraphNode;
import java.util.List;

public class GraphDataDTO {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    public GraphDataDTO() {}

    public GraphDataDTO(List<GraphNode> nodes, List<GraphEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }
}