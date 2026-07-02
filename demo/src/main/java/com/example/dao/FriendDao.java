package com.example.dao;

import com.example.entity.Friend;

import java.time.LocalDateTime;
import java.util.List;

public interface FriendDao {
    // 新增好友关系（双向插入时调用）
    Integer add(Friend friend);

    // 删除好友关系（双向删除）
    void delete(String userId, String friendId);

    // 查询用户的所有好友
    List<Friend> queryByUserId(String userId);

    // 查询是否已是好友
    boolean isFriend(String userId, String friendId);

    // 更新好友备注
    void updateRemark(String userId, String friendId, String remark);

    /**
     * 根据用户ID + 时间段 查询好友列表
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段内添加的好友列表
     */
    List<Friend> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户指定时间段内添加的好友数量
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 好友数量
     */
    Integer countFriendByTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
}