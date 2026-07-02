package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.Friend;
import java.util.List;

public interface FriendService {
    // 添加好友（双向插入）
    ResultDTO<Void> addFriend(String userId, String friendId);

    // 删除好友
    ResultDTO<Void> deleteFriend(String userId, String friendId);

    // 查询好友列表
    ResultDTO<List<Friend>> getFriendList(String userId);

    // 判断是否是好友
    ResultDTO<Boolean> isFriend(String userId, String friendId);

    // 更新好友备注
    ResultDTO<Void> updateRemark(String userId, String friendId, String remark);
}