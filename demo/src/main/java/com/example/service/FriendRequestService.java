package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.FriendRequest;
import java.util.List;

public interface FriendRequestService {
    // 发送好友申请
    ResultDTO<Integer> sendRequest(String fromUserId, String toUserId, String requestMsg);

    // 同意好友申请
    ResultDTO<Void> acceptRequest(Integer requestId, String userId);

    // 拒绝好友申请
    ResultDTO<Void> rejectRequest(Integer requestId);

    // 查询我收到的申请
    ResultDTO<List<FriendRequest>> getReceivedRequests(String toUserId);

    // 查询我发出的申请
    ResultDTO<List<FriendRequest>> getSentRequests(String fromUserId);
}