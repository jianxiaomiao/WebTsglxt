package com.example.entity;

public class GraphEdge {
    private Integer id;
    private String mapId;
    private String mapUser;
    private Integer sourceNodeId;
    private Integer targetNodeId;
    private String sourcePort;
    private String targetPort;
    private String label;

    public GraphEdge() {}

    public GraphEdge(Integer id, String mapId, String mapUser, Integer sourceNodeId,
                     Integer targetNodeId, String sourcePort, String targetPort, String label) {
        this.id = id;
        this.mapId = mapId;
        this.mapUser = mapUser;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.sourcePort = sourcePort;
        this.targetPort = targetPort;
        this.label = label;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }

    public String getMapUser() { return mapUser; }
    public void setMapUser(String mapUser) { this.mapUser = mapUser; }

    public Integer getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(Integer sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public Integer getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(Integer targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getSourcePort() { return sourcePort; }
    public void setSourcePort(String sourcePort) { this.sourcePort = sourcePort; }

    public String getTargetPort() { return targetPort; }
    public void setTargetPort(String targetPort) { this.targetPort = targetPort; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    @Override
    public String toString() {
        return "GraphEdge{" +
                "id=" + id +
                ", mapId='" + mapId + '\'' +
                ", mapUser='" + mapUser + '\'' +
                ", sourceNodeId=" + sourceNodeId +
                ", targetNodeId=" + targetNodeId +
                ", sourcePort='" + sourcePort + '\'' +
                ", targetPort='" + targetPort + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}