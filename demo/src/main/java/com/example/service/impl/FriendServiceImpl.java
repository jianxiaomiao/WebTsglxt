package com.example.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.example.dao.FriendDao;
import com.example.dto.ResultDTO;
import com.example.entity.Friend;
import com.example.service.FriendService;
import com.example.util.RedisUtil;

public class FriendServiceImpl implements FriendService {
    private final FriendDao friendDao;

    public FriendServiceImpl(FriendDao friendDao) {
        this.friendDao = friendDao;
    }

    @Override
    public ResultDTO<Void> addFriend(String userId, String friendId) {
        try {
            // 1. 检查是否已是好友
            if (friendDao.isFriend(userId, friendId)) {
                return ResultDTO.fail("你们已经是好友了");
            }

            // 2. 插入双向好友关系
            LocalDateTime now = LocalDateTime.now();
            // 我 -> 对方
            Friend f1 = new Friend(userId, friendId, now);
            friendDao.add(f1);
            // 对方 -> 我
            Friend f2 = new Friend(friendId, userId, now);
            friendDao.add(f2);

            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("添加好友失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteFriend(String userId, String friendId) {
        try {
            friendDao.delete(userId, friendId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除好友失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<Friend>> getFriendList(String userId) {
        try {
            List<Friend> friends = friendDao.queryByUserId(userId);
            if (friends != null && !friends.isEmpty()) {
                for (Friend friend : friends) {
                    try {
                        boolean isOnline = RedisUtil.sismember("online:users", friend.getFriendId());
                        friend.setIsOnline(isOnline);
                    } catch (Exception e) {
                        friend.setIsOnline(false); // 降级默认离线
                    }
                }
            }
            return ResultDTO.success(friends);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询好友列表失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Boolean> isFriend(String userId, String friendId) {
        try {
            boolean isFriend = friendDao.isFriend(userId, friendId);
            return ResultDTO.success(isFriend);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询好友关系失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateRemark(String userId, String friendId, String remark) {
        try {
            friendDao.updateRemark(userId, friendId, remark);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新备注失败：" + e.getMessage());
        }
    }
}