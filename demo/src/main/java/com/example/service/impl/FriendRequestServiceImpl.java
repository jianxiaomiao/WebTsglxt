package com.example.service.impl;

import com.example.dao.FriendDao;
import com.example.dao.FriendRequestDao;
import com.example.dto.ResultDTO;
import com.example.entity.Friend;
import com.example.entity.FriendRequest;
import com.example.service.FriendRequestService;
import com.example.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class FriendRequestServiceImpl implements FriendRequestService {
    private static final Logger logger = LoggerFactory.getLogger(FriendRequestServiceImpl.class);
    private final FriendRequestDao friendRequestDao;
    private final FriendDao friendDao; // 注入FriendDao，同意申请时添加好友

    public FriendRequestServiceImpl(FriendRequestDao friendRequestDao, FriendDao friendDao) {
        this.friendRequestDao = friendRequestDao;
        this.friendDao = friendDao;
    }

    @Override
    public ResultDTO<Integer> sendRequest(String fromUserId, String toUserId, String requestMsg) {
        try {
            // 1. 检查是否已是好友
            if (friendDao.isFriend(fromUserId, toUserId)) {
                return ResultDTO.fail("你们已经是好友了");
            }

            // 2. 检查是否已有待处理的申请
            if (friendRequestDao.hasPendingRequest(fromUserId, toUserId)) {
                return ResultDTO.fail("已发送过好友申请，请等待对方处理");
            }

            // 3. 发送申请
            FriendRequest request = new FriendRequest(fromUserId, toUserId, requestMsg, LocalDateTime.now());
            Integer requestId = friendRequestDao.add(request);
            // 核心优化：自增 Redis 中的未读申请数
            try {
                String hashKey = "user:unread:" + toUserId;
                if (RedisUtil.exists(hashKey)) {
                    RedisUtil.hincrBy(hashKey, "friendRequests", 1);
                }
            } catch (Exception e) {
                logger.warn("Redis 自增未读好友申请数异常", e);
            }
            return ResultDTO.success(requestId);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("发送好友申请失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> acceptRequest(Integer requestId, String userId) {
        try {
            // 1. 查询申请信息
            List<FriendRequest> requests = friendRequestDao.queryReceivedRequests(userId);
            FriendRequest request = requests.stream()
                    .filter(r -> r.getId().equals(requestId))
                    .findFirst()
                    .orElse(null);

            if (request == null) {
                return ResultDTO.fail("申请不存在或无权处理");
            }

            // 2. 更新申请状态为已同意
            friendRequestDao.updateStatus(requestId, 1, LocalDateTime.now());

            // 3. 添加双向好友关系
            LocalDateTime now = LocalDateTime.now();
            Friend f1 = new Friend(userId, request.getFromUserId(), now);
            friendDao.add(f1);
            Friend f2 = new Friend(request.getFromUserId(), userId, now);
            friendDao.add(f2);

            // ====================== 🔥 关键新增 ======================
            // 批量关闭 两人之间 所有未处理的好友申请
            friendRequestDao.batchUpdatePendingStatus(
                    userId,
                    request.getFromUserId(),
                    1,  // 状态改为已同意
                    now
            );

            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("同意好友申请失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> rejectRequest(Integer requestId) {
        try {
            friendRequestDao.updateStatus(requestId, 2, LocalDateTime.now());
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("拒绝好友申请失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<FriendRequest>> getReceivedRequests(String toUserId) {
        try {
            List<FriendRequest> requests = friendRequestDao.queryReceivedRequests(toUserId);
            return ResultDTO.success(requests);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询收到的申请失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<FriendRequest>> getSentRequests(String fromUserId) {
        try {
            List<FriendRequest> requests = friendRequestDao.querySentRequests(fromUserId);
            return ResultDTO.success(requests);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询发出的申请失败：" + e.getMessage());
        }
    }
}