package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.entity.GraphEdge;
import com.example.entity.GraphNode;
import com.example.service.GraphService;
import com.example.service.impl.GraphServiceImpl;
import com.example.util.UserBehaviorLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/graph")
public class GraphServlet extends BaseServlet {
    private GraphService graphService;

    @Override
    public void init() {
        graphService = new GraphServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String action = req.getParameter("action");
            ResultDTO<?> result;

            if ("full".equals(action)) {
                String mapId = req.getParameter("map_id");
                String mapUser = req.getParameter("map_user");
                result = graphService.getFullGraph(mapId, mapUser);
            }
            else if ("node".equals(action)) {
                String id = req.getParameter("id");
                result = graphService.getNodeById(Integer.parseInt(id));
            }
            else if ("node_map".equals(action)) {
                String mapId = req.getParameter("map_id");
                result = graphService.getNodesByMapId(mapId);
            }
            else if ("node_user".equals(action)) {
                String mapUser = req.getParameter("map_user");
                result = graphService.getNodesByMapUser(mapUser);
            }
            else if ("node_type".equals(action)) {
                String type = req.getParameter("type");
                result = graphService.getNodesByType(Integer.parseInt(type));
            }
            else if ("edge".equals(action)) {
                String id = req.getParameter("id");
                result = graphService.getEdgeById(Integer.parseInt(id));
            }
            else if ("edge_source".equals(action)) {
                String sourceId = req.getParameter("source_node_id");
                result = graphService.getEdgesBySource(Integer.parseInt(sourceId));
            }
            else if ("edge_target".equals(action)) {
                String targetId = req.getParameter("target_node_id");
                result = graphService.getEdgesByTarget(Integer.parseInt(targetId));
            }
            else {
                result = ResultDTO.fail("参数错误");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String action = req.getParameter("action");
            ResultDTO<?> result;

            if ("node".equals(action)) {
                GraphNode node = JSON.parseObject(req.getInputStream(), GraphNode.class);
                result = graphService.addNode(node);
                UserBehaviorLogger.logAsync(node.getMapUser(), 20, node.getMapId(), null, "创建图图表节点");
            }
            else if ("edge".equals(action)) {
                GraphEdge edge = JSON.parseObject(req.getInputStream(), GraphEdge.class);
                result = graphService.addEdge(edge);
                UserBehaviorLogger.logAsync(edge.getMapUser(), 21, edge.getMapId(), null, "创建图图表联系,from: "+ edge.getSourceNodeId() + ",to: " +edge.getTargetNodeId());
            }
            else {
                result = ResultDTO.fail("参数错误");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String action = req.getParameter("action");
            ResultDTO<?> result;

            if ("node".equals(action)) {
                GraphNode node = JSON.parseObject(req.getInputStream(), GraphNode.class);
                result = graphService.updateNode(node);
            }
            else if ("edge".equals(action)) {
                GraphEdge edge = JSON.parseObject(req.getInputStream(), GraphEdge.class);
                result = graphService.updateEdge(edge);
            }
            else {
                result = ResultDTO.fail("参数错误");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        try (PrintWriter out = resp.getWriter()) {
            String action = req.getParameter("action");
            String id = req.getParameter("id");
            ResultDTO<?> result;

            if ("node".equals(action)) {
                result = graphService.deleteNode(Integer.parseInt(id));
            }
            else if ("edge".equals(action)) {
                result = graphService.deleteEdge(Integer.parseInt(id));
            }
            else {
                result = ResultDTO.fail("参数错误");
            }

            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}