package com.example.service;

import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookReadRoom;
import java.util.List;

public interface BookReadRoomService {
    // 新增房间
    ResultDTO<BookReadRoom> addRoom(BookReadRoom room);

    // 更新房间
    ResultDTO<Void> updateRoom(BookReadRoom room);

    // 删除房间
    ResultDTO<Void> deleteRoom(Integer roomId);

    // 查询全部
    ResultDTO<List<BookReadRoom>> listAll();

    // 根据房间id查询单条
    ResultDTO<List<BookReadRoom>> getRoomById(Integer roomId, String loginUserId);

    // 用户创建的全部房间
    ResultDTO<List<BookReadRoom>> listByUserId(String userId);

    // 书名模糊查询
    ResultDTO<List<BookReadRoom>> listByBookName(String bookName);

    // 分页查询（支持userId/书名模糊筛选）
    ResultDTO<PageResultDTO<BookReadRoom>> pageQuery(String userId, String bookName, Integer pageNum, Integer pageSize);
}