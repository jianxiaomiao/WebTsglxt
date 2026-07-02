package com.example.dao;

import com.example.entity.BookReadRoom;
import java.util.List;

public interface BookReadRoomDao {
    // 新增房间
    BookReadRoom add(BookReadRoom room);

    // 动态拼接更新
    void update(BookReadRoom room);

    // 删除房间
    void del(Integer roomId);

    // 查询全部（联表书籍+用户）
    List<BookReadRoom> queryAll();

    // 根据房间id查询
    List<BookReadRoom> queryById(Integer roomId);

    // 根据创建者userId查询
    List<BookReadRoom> queryByUserId(String userId);

    // 根据书名模糊查询（联表book_information）
    List<BookReadRoom> queryByBookNameLike(String bookName);

    // 多条件分页：userId / 书名模糊 二选一，通用分页
    List<BookReadRoom> queryPage(String userId, String bookName, int offset, int pageSize);

    // 统计总条数（分页用）
    Long countTotal(String userId, String bookName);
}