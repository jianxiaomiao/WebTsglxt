package com.example.dao;

import com.example.entity.FriendRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface FriendRequestDao {
    // 新增好友申请
    Integer add(FriendRequest request);

    // 更新申请状态（同意/拒绝）
    void updateStatus(Integer id, Integer status, java.time.LocalDateTime handleTime);

    // 查询我收到的申请
    List<FriendRequest> queryReceivedRequests(String toUserId);

    // 查询我发出的申请
    List<FriendRequest> querySentRequests(String fromUserId);

    // 查询是否已有待处理的申请（避免重复发送）
    boolean hasPendingRequest(String fromUserId, String toUserId);

    FriendRequest selectById(Integer id);

    // 新增：批量更新两个用户之间所有待处理的好友申请状态
    void batchUpdatePendingStatus(String uid1, String uid2, Integer status, LocalDateTime handleTime);

    /**
     * 查询用户指定时间段内发送的好友请求
     */
    List<FriendRequest> querySentRequestsByTimeRange(String fromUserId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询用户指定时间段内收到的好友请求
     */
    List<FriendRequest> queryReceivedRequestsByTimeRange(String toUserId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户指定时间段内发送的好友请求数量
     */
    Integer countSentRequestsByTimeRange(String fromUserId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户指定时间段内收到的好友请求数量
     */
    Integer countReceivedRequestsByTimeRange(String toUserId, LocalDateTime startTime, LocalDateTime endTime);
}