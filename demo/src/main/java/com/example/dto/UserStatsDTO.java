package com.example.dto;

import com.example.entity.*;
import com.example.vo.BookReadStatsVO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户全维度统计DTO（整合所有时间段数据：数量+明细列表）
 */
@Data
public class UserStatsDTO {
    // 基础信息
    private String userId;          // 用户ID
    private String username;        // 用户名
    private String statsType;       // 统计类型 DAY/WEEK/MONTH
    private LocalDateTime startTime;// 开始时间
    private LocalDateTime endTime;  // 结束时间

    // ===================== 【统计数量】所有数字字段 =====================
    // 阅读统计
    private Integer totalReadSecond;   // 总阅读时长(秒)
    private Integer readBookCount;      // 阅读书籍数量
    private Integer readDayCount;       // 阅读天数
    // 笔记统计
    private Integer newNoteCount;       // 新增笔记数量
    // 消息统计
    private Integer sendMsgCount;       // 发送消息数
    private Integer receiveMsgCount;    // 接收消息数
    // AI聊天统计
    private Integer aiChatCount;        // AI对话消息数
    // ===================== 新增：评论点赞统计 =====================
    private Integer likeCount;                    // 评论点赞数量
    private List<UserCommentLike> userCommentLikeList; // 评论点赞记录列表
    // 评论统计
    private Integer bookCommentCount;   // 书籍评论数
    private Integer userCommentCount;   // 论坛评论数
    // 收藏/借阅统计
    private Integer collectionCount;    // 书籍收藏数
    private Integer borrowCount;        // 书籍借阅数
    // 好友统计
    private Integer friendCount;        // 添加好友数
    // 好友请求统计
    private Integer sentRequestCount;   // 发送好友请求数
    private Integer receivedRequestCount;// 收到好友请求数
    // 书签统计
    private Integer bookmarkCount;      // 书签数量

    // ===================== 【明细列表】所有DAO返回的集合 =====================
    // ===================== 新增：书籍阅读统计列表（去重+书籍信息） =====================
    private List<BookReadStatsVO> bookReadStatsList;
    // 阅读
    private List<UserReadDaily> dailyReadList;        // 每日阅读记录
    private List<UserReadRecord> readRecordList;      // 阅读时长明细
    // 笔记
    private List<UserTextCollection> noteList;         // 阅读笔记列表
    // 书签
    private List<Bookmark> bookmarkList;               // 书签列表
    // 普通消息
    private List<ChatMessage> sendMsgList;            // 发送的消息
    private List<ChatMessage> receiveMsgList;         // 接收的消息
    // AI聊天
    private List<UserAiChat> aiChatList;               // AI对话记录
    // 评论
    private List<BookComment> bookCommentList;        // 书籍评论
    private List<UserComment> userCommentList;        // 论坛评论
    // 收藏/借阅
    private List<UserCollection> collectionList;       // 收藏列表
    private List<BorrowInformation> borrowList;       // 借阅列表
    // 好友
    private List<Friend> friendList;                   // 新增好友列表
    // 好友请求
    private List<FriendRequest> sentRequestList;      // 发送的好友请求
    private List<FriendRequest> receivedRequestList; // 收到的好友请求
}