package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dto.ResultDTO;
import com.example.util.DBUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;

/**
 * 管理员 Servlet — 内容审核 + 数据管理
 *
 * GET  /api/admin/pending-counts                                             → 各模块待审核数量
 * PUT  /api/admin/book-comment/audit?id=1&status=1                          → 审核书籍评论
 * PUT  /api/admin/paragraph-comment/audit?id=1&status=1                     → 审核段落评论
 * PUT  /api/admin/user-comment/audit?id=1&status=1                          → 审核论坛评论
 * PUT  /api/admin/bottle/audit?id=1&status=1                                → 审核漂流瓶
 * GET  /api/admin/book-comments?pageNum=1&pageSize=20&status=0              → 分页查书籍评论
 * GET  /api/admin/paragraph-comments?pageNum=1&pageSize=20&status=0         → 分页查段落评论
 * GET  /api/admin/user-comments?pageNum=1&pageSize=20&status=0              → 分页查论坛评论
 * GET  /api/admin/bottles?pageNum=1&pageSize=20&auditStatus=0               → 分页查漂流瓶
 * GET  /api/admin/users?pageNum=1&pageSize=20&keyword=                      → 用户管理
 * GET  /api/admin/books?pageNum=1&pageSize=20&keyword=                      → 书籍管理
 */
@WebServlet("/api/admin/*")
public class AdminServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String pathInfo = req.getPathInfo();
            int pageNum = parseInt(req.getParameter("pageNum"), 1);
            int pageSize = parseInt(req.getParameter("pageSize"), 20);
            if (pageSize > 100 || pageSize < 1) pageSize = 20;
            int offset = (pageNum - 1) * pageSize;

            if ("/pending-counts".equals(pathInfo)) {
                Map<String, Object> counts = new LinkedHashMap<>();
                counts.put("bookComments", countPending("book_comment", "status"));
                counts.put("paragraphComments", countPending("paragraph_comment", "status"));
                counts.put("userComments", countPending("user_comment", "status"));
                counts.put("bottles", countPending("book_bottle", "audit_status"));
                out.write(JSON.toJSONString(ResultDTO.success(counts)));

            } else if ("/users".equals(pathInfo)) {
                String keyword = req.getParameter("keyword");
                out.write(JSON.toJSONString(queryUsers(keyword, pageNum, pageSize, offset)));

            } else if ("/books".equals(pathInfo)) {
                String keyword = req.getParameter("keyword");
                out.write(JSON.toJSONString(queryBooks(keyword, pageNum, pageSize, offset)));

            } else if ("/book-comments".equals(pathInfo)) {
                int status = parseInt(req.getParameter("status"), 0);
                out.write(JSON.toJSONString(queryComments("book_comment", "status",
                        "CommentId,ISBN,UserId,Comment,Star,Time,status", "Time", status,
                        new String[]{"id","isbn","userId","comment","star","time","status"},
                        pageSize, offset)));

            } else if ("/paragraph-comments".equals(pathInfo)) {
                int status = parseInt(req.getParameter("status"), 0);
                out.write(JSON.toJSONString(queryComments("paragraph_comment", "status",
                        "id,paragraph_id,user_id,content,create_time,status", "create_time", status,
                        new String[]{"id","paragraphId","userId","content","createTime","status"},
                        pageSize, offset)));

            } else if ("/user-comments".equals(pathInfo)) {
                int status = parseInt(req.getParameter("status"), 0);
                ResultDTO<?> result = queryComments("user_comment", "status",
                        "CommentId,UserId,UserComment,CommentTime,prefer,parent_id,status", "CommentTime", status,
                        new String[]{"id","userId","userComment","commentTime","prefer","parentId","status"},
                        pageSize, offset);
                // 🔥 批量查询：一次性查所有评论的图片（N+1 → 1 次查询）
                if (result.isSuccess() && result.getData() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) result.getData();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
                    if (list != null && !list.isEmpty()) {
                        try {
                            // 收集所有评论 ID
                            StringBuilder idList = new StringBuilder();
                            Map<Object, List<Map<String, Object>>> imageMap = new LinkedHashMap<>();
                            for (Map<String, Object> comment : list) {
                                Object cid = comment.get("id");
                                if (cid != null) {
                                    if (idList.length() > 0) idList.append(",");
                                    idList.append(cid);
                                    imageMap.put(cid, new ArrayList<>());
                                }
                            }
                            // 一次 IN 查询
                            if (idList.length() > 0) {
                                List<Map<String, Object>> allImages = DBUtil.executeQuery(
                                    "SELECT id, comment_id, image_url FROM forum_image WHERE comment_id IN (" + idList + ") ORDER BY id",
                                    rs -> {
                                        Map<String, Object> img = new LinkedHashMap<>();
                                        img.put("id", rs.getInt("id"));
                                        img.put("imageUrl", rs.getString("image_url"));
                                        img.put("commentId", rs.getObject("comment_id"));
                                        return img;
                                    });
                                // 按 comment_id 分组
                                for (Map<String, Object> img : allImages) {
                                    Object cid = img.remove("commentId");
                                    List<Map<String, Object>> imgs = imageMap.get(cid);
                                    if (imgs != null) imgs.add(img);
                                }
                            }
                            // 回填到每条评论
                            for (Map<String, Object> comment : list) {
                                comment.put("images", imageMap.getOrDefault(comment.get("id"), Collections.emptyList()));
                            }
                        } catch (SQLException ignored) {}
                    }
                }
                out.write(JSON.toJSONString(result));

            } else if ("/bottles".equals(pathInfo)) {
                int auditStatus = parseInt(req.getParameter("auditStatus"), 0);
                out.write(JSON.toJSONString(queryComments("book_bottle", "audit_status",
                        "id,userid,isbn,content,createtime,audit_status", "createtime", auditStatus,
                        new String[]{"id","userId","isbn","content","createTime","auditStatus"},
                        pageSize, offset)));

            } else {
                out.write(JSON.toJSONString(ResultDTO.fail("未知路径: " + pathInfo)));
            }
        } catch (Exception e) {
            logger.error("管理员GET异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常")));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        if (!requireLogin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            String pathInfo = req.getPathInfo();

            // 🔥 用户管理封号/解冻：单独处理，不需要 id/status 参数
            if ("/user/toggle-freeze".equals(pathInfo)) {
                String userId = req.getParameter("userId");
                int canUse = parseInt(req.getParameter("canUse"), 1);
                if (userId == null || userId.isBlank()) {
                    out.write(JSON.toJSONString(ResultDTO.fail("缺少 userId 参数")));
                    return;
                }
                int rows = DBUtil.executeUpdate("UPDATE user_information SET Can_use=? WHERE UserId=?", canUse, userId);
                out.write(JSON.toJSONString(rows > 0 ? ResultDTO.success("操作成功") : ResultDTO.fail("用户不存在")));
                return;
            }

            // 审核操作统一需要 id 和 status
            int id = parseInt(req.getParameter("id"), -1);
            int status = parseInt(req.getParameter("status"), -1);
            if (id < 0 || status < 0) {
                out.write(JSON.toJSONString(ResultDTO.fail("缺少 id 或 status 参数")));
                return;
            }

            String table, col, idCol;
            if ("/book-comment/audit".equals(pathInfo)) {
                table = "book_comment"; col = "status"; idCol = "CommentId";
            } else if ("/paragraph-comment/audit".equals(pathInfo)) {
                table = "paragraph_comment"; col = "status"; idCol = "id";
            } else if ("/user-comment/audit".equals(pathInfo)) {
                table = "user_comment"; col = "status"; idCol = "CommentId";
            } else if ("/bottle/audit".equals(pathInfo)) {
                table = "book_bottle"; col = "audit_status"; idCol = "id";
            } else {
                out.write(JSON.toJSONString(ResultDTO.fail("未知路径: " + pathInfo)));
                return;
            }

            int rows = DBUtil.executeUpdate(
                    "UPDATE " + table + " SET " + col + "=? WHERE " + idCol + "=?", status, id);
            logger.info("🔧 审核操作 table={} id={} status={} rows={}", table, id, status, rows);
            out.write(JSON.toJSONString(rows > 0 ? ResultDTO.success("审核完成") : ResultDTO.fail("记录不存在")));

        } catch (Exception e) {
            logger.error("管理员PUT异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("服务器异常")));
        }
    }

    // ====================== 工具方法 ======================
    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private int countPending(String table, String col) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + col + "=0";
        return DBUtil.executeQueryScalar(sql, Integer.class);
    }

    private ResultDTO<?> queryComments(String table, String statusCol, String selectCols,
                                        String orderCol, int status, String[] fieldNames,
                                        int limit, int offset) throws SQLException {
        String sql = "SELECT " + selectCols + " FROM " + table +
                " WHERE " + statusCol + "=? ORDER BY " + orderCol + " DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM " + table + " WHERE " + statusCol + "=?";
        int total = DBUtil.executeQueryScalar(countSql, Integer.class, status);
        String[] dbCols = selectCols.split(",");
        List<Map<String, Object>> list = DBUtil.executeQuery(sql, rs -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < dbCols.length && i < fieldNames.length; i++) {
                try {
                    map.put(fieldNames[i].trim(), rs.getObject(dbCols[i].trim()));
                } catch (SQLException ignored) {}
            }
            return map;
        }, status, limit, offset);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total); result.put("pageNum", (offset / limit) + 1);
        result.put("pageSize", limit); result.put("list", list);
        return ResultDTO.success(result);
    }

    private ResultDTO<?> queryUsers(String keyword, int pageNum, int pageSize, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT UserId,Name,Sex,Dept_Type,Regdate,Type,Can_use,bio FROM user_information WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM user_information WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (Name LIKE ? OR UserId LIKE ?)");
            countSql.append(" AND (Name LIKE ? OR UserId LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw); params.add(kw);
        }
        sql.append(" ORDER BY Regdate DESC LIMIT ? OFFSET ?");
        params.add(pageSize); params.add(offset);
        List<Object> countParams = new ArrayList<>(params.subList(0, params.size() - 2));
        int total = DBUtil.executeQueryScalar(countSql.toString(), Integer.class, countParams.toArray());
        List<Map<String, Object>> list = DBUtil.executeQuery(sql.toString(), rs -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", rs.getString("UserId")); map.put("name", rs.getString("Name"));
            map.put("sex", rs.getString("Sex")); map.put("deptType", rs.getInt("Dept_Type"));
            map.put("regdate", rs.getDate("Regdate")); map.put("type", rs.getInt("Type"));
            map.put("canUse", rs.getInt("Can_use")); // 0=冻结 1+=正常
            map.put("bio", rs.getString("bio"));
            return map;
        }, params.toArray());
        return buildPageResult(total, pageNum, pageSize, list);
    }

    private ResultDTO<?> queryBooks(String keyword, int pageNum, int pageSize, int offset) throws SQLException {
        String sql = "SELECT ISBN,Bookname,Author,Publisher,Type,all_book,now_book,star,BorrowCount FROM book_information";
        String countSql = "SELECT COUNT(*) FROM book_information";
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += " WHERE Bookname LIKE ? OR Author LIKE ? OR ISBN LIKE ?";
            countSql += " WHERE Bookname LIKE ? OR Author LIKE ? OR ISBN LIKE ?";
            String kw = "%" + keyword + "%";
            params.add(kw); params.add(kw); params.add(kw);
        }
        sql += " ORDER BY ISBN DESC LIMIT ? OFFSET ?";
        params.add(pageSize); params.add(offset);
        List<Object> countParams = params.size() > 2 ? new ArrayList<>(params.subList(0, params.size() - 2)) : Collections.emptyList();
        int total = DBUtil.executeQueryScalar(countSql.toString(), Integer.class, countParams.toArray());
        List<Map<String, Object>> list = DBUtil.executeQuery(sql.toString(), rs -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("isbn", rs.getString("ISBN")); map.put("bookName", rs.getString("Bookname"));
            map.put("author", rs.getString("Author")); map.put("publisher", rs.getString("Publisher"));
            map.put("type", rs.getInt("Type")); map.put("star", rs.getFloat("star"));
            map.put("allBook", rs.getInt("all_book")); map.put("nowBook", rs.getInt("now_book"));
            return map;
        }, params.toArray());
        return buildPageResult(total, pageNum, pageSize, list);
    }

    private ResultDTO<Map<String, Object>> buildPageResult(int total, int pageNum, int pageSize, List<?> list) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total); result.put("pageNum", pageNum);
        result.put("pageSize", pageSize); result.put("list", list);
        return ResultDTO.success(result);
    }
}
