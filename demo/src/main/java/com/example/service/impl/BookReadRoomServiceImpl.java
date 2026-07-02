package com.example.service.impl;

import com.example.dao.BookReadRoomDao;
import com.example.dto.PageResultDTO;
import com.example.dto.ResultDTO;
import com.example.entity.BookReadRoom;
import com.example.service.BookReadRoomService;

import java.util.List;

public class BookReadRoomServiceImpl implements BookReadRoomService {
    private final BookReadRoomDao roomDao;

    // 构造注入dao
    public BookReadRoomServiceImpl(BookReadRoomDao roomDao) {
        this.roomDao = roomDao;
    }

    @Override
    public ResultDTO<BookReadRoom> addRoom(BookReadRoom room) {
        try {
            // dao现在返回带id的完整room对象
            BookReadRoom newRoom = roomDao.add(room);
            return ResultDTO.success(newRoom);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("创建共读房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateRoom(BookReadRoom room) {
        try {
            roomDao.update(room);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新共读房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteRoom(Integer roomId) {
        try {
            roomDao.del(roomId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除共读房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookReadRoom>> listAll() {
        try {
            List<BookReadRoom> list = roomDao.queryAll();
            return ResultDTO.success(list);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询全部房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookReadRoom>> getRoomById(Integer roomId, String loginUserId) {
        try {
            List<BookReadRoom> roomList = roomDao.queryById(roomId);
            // 权限校验：房间存在且是私密，且登录用户不是房主 → 清空数据
            if (!roomList.isEmpty()) {
                BookReadRoom room = roomList.get(0);
                if (room.getIsPublic() == 0 && !room.getUserId().equals(loginUserId)) {
                    roomList.clear();
                }
            }
            return ResultDTO.success(roomList);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据房间id查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookReadRoom>> listByUserId(String userId) {
        try {
            List<BookReadRoom> list = roomDao.queryByUserId(userId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询用户创建房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<BookReadRoom>> listByBookName(String bookName) {
        try {
            List<BookReadRoom> list = roomDao.queryByBookNameLike(bookName);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("书名模糊查询房间失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<PageResultDTO<BookReadRoom>> pageQuery(String userId, String bookName, Integer pageNum, Integer pageSize) {
        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            List<BookReadRoom> data = roomDao.queryPage(userId, bookName, offset, pageSize);
            Long total = roomDao.countTotal(userId, bookName);
            PageResultDTO<BookReadRoom> page = PageResultDTO.success(total, pageNum, pageSize, data);
            return ResultDTO.success(page);
        } catch (RuntimeException e) {
            return ResultDTO.fail("分页查询共读房间失败：" + e.getMessage());
        }
    }
}