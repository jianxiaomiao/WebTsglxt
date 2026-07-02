package com.example.entity;

public class GraphNode {
    private Integer id;
    private String mapId;
    private String mapUser;
    private Integer type;
    private String contentHeader;
    private String content;
    private Double positionX;
    private Double positionY;
    private String style;

    public GraphNode() {}

    public GraphNode(Integer id, String mapId, String mapUser, Integer type,
                     String contentHeader, String content, Double positionX,
                     Double positionY, String style) {
        this.id = id;
        this.mapId = mapId;
        this.mapUser = mapUser;
        this.type = type;
        this.contentHeader = contentHeader;
        this.content = content;
        this.positionX = positionX;
        this.positionY = positionY;
        this.style = style;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }

    public String getMapUser() { return mapUser; }
    public void setMapUser(String mapUser) { this.mapUser = mapUser; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public String getContentHeader() { return contentHeader; }
    public void setContentHeader(String contentHeader) { this.contentHeader = contentHeader; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double getPositionX() { return positionX; }
    public void setPositionX(Double positionX) { this.positionX = positionX; }

    public Double getPositionY() { return positionY; }
    public void setPositionY(Double positionY) { this.positionY = positionY; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    @Override
    public String toString() {
        return "GraphNode{" +
                "id=" + id +
                ", mapId='" + mapId + '\'' +
                ", mapUser='" + mapUser + '\'' +
                ", type=" + type +
                ", contentHeader='" + contentHeader + '\'' +
                ", content='" + content + '\'' +
                ", positionX=" + positionX +
                ", positionY=" + positionY +
                ", style='" + style + '\'' +
                '}';
    }
}