package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.BookReadRoomDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.BookReadRoom;
import com.example.service.BookReadRoomService;
import com.example.service.impl.BookReadRoomServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/book/room")
public class BookReadRoomServlet extends BaseServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookReadRoomServlet.class);
    private BookReadRoomService roomService;

    @Override
    public void init() throws ServletException {
        // 初始化service
        BookReadRoomDaoImpl roomDao = new BookReadRoomDaoImpl();
        roomService = new BookReadRoomServiceImpl(roomDao);
    }

    // GET：查询（单房间id / 用户id / 书名模糊 / 分页）
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        ResultDTO<?> result;

        // 获取前端参数
        String roomIdStr = req.getParameter("roomId");
        String userId = req.getParameter("userId");
        String bookName = req.getParameter("bookName");
        String pageNumStr = req.getParameter("pageNum");
        String pageSizeStr = req.getParameter("pageSize");

        Integer pageNum = null;
        Integer pageSize = null;
        if (pageNumStr != null && !pageNumStr.isBlank()) pageNum = Integer.parseInt(pageNumStr);
        if (pageSizeStr != null && !pageSizeStr.isBlank()) pageSize = Integer.parseInt(pageSizeStr);

        try {
            // 1. 根据房间id查单个房间
            if (roomIdStr != null && !roomIdStr.isBlank()) {
                Integer rid = Integer.parseInt(roomIdStr);
                result = roomService.getRoomById(rid, userId);
                logger.info("查询房间详情 roomId:{}", rid);
            }
            // 2. 分页查询（带筛选条件 userId / bookName）
            else if (pageNum != null) {
                result = roomService.pageQuery(userId, bookName, pageNum, pageSize);
                logger.info("分页查询房间 pageNum:{} userId:{} bookName:{}", pageNum, userId, bookName);
            }
            // 3. 按创建者userId查询全部房间
            else if (userId != null && !userId.isBlank()) {
                result = roomService.listByUserId(userId);
                logger.info("查询用户创建房间 userId:{}", userId);
            }
            // 4. 书名模糊匹配
            else if (bookName != null && !bookName.isBlank()) {
                result = roomService.listByBookName(bookName);
                logger.info("书名模糊查询 bookName:{}", bookName);
            }
            // 5. 查询所有房间
            else {
                result = roomService.listAll();
                logger.info("查询全部共读房间");
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("数字参数格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("roomId/pageNum/pageSize必须为数字")));
        } catch (Exception e) {
            logger.error("共读房间查询接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // POST：新建共读房间
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            BookReadRoom room = JSON.parseObject(req.getInputStream(), BookReadRoom.class);
            // 现在返回 ResultDTO<BookReadRoom>
            ResultDTO<BookReadRoom> res = roomService.addRoom(room);
            out.write(JSON.toJSONString(res));
            logger.info("创建共读房间 userId:{} isbn:{} 生成房间ID:{}", room.getUserId(), room.getIsbn(), room.getId());
        } catch (Exception e) {
            logger.error("新增房间接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("创建房间失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // PUT：动态更新房间
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            BookReadRoom room = JSON.parseObject(req.getInputStream(), BookReadRoom.class);
            ResultDTO<Void> res = roomService.updateRoom(room);
            out.write(JSON.toJSONString(res));
            logger.info("更新共读房间 roomId:{}", room.getId());
        } catch (Exception e) {
            logger.error("更新房间接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新房间失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // DELETE：删除房间
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String roomIdStr = req.getParameter("roomId");

        try {
            if (roomIdStr == null || roomIdStr.isBlank()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("roomId不能为空")));
                return;
            }
            Integer rid = Integer.parseInt(roomIdStr);
            ResultDTO<Void> res = roomService.deleteRoom(rid);
            out.write(JSON.toJSONString(res));
            logger.info("删除共读房间 roomId:{}", rid);
        } catch (NumberFormatException e) {
            out.write(JSON.toJSONString(ResultDTO.paramError("roomId必须是数字")));
        } catch (Exception e) {
            logger.error("删除房间接口异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除房间失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}