package com.example.dto;

import lombok.Data;
import java.util.List;

/**
 * AI生成报告专用DTO（严格匹配需求：全量数字字段 + 各列表前3条文本）
 */
@Data
public class UserStatsForAIDTO {
    // ===================== 【统计数量】所有数字字段（完整保留） =====================
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
    // 评论点赞统计
    private Integer likeCount;          // 评论点赞数量
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

    // ===================== 【明细列表】仅保留前3条纯文本内容 =====================
    // 书籍阅读统计（书名+作者，AI需要知道读了什么书）
    private List<String> bookReadStatsList;
    // 每日阅读记录
    private List<String> dailyReadList;
    // 阅读时长明细
    private List<String> readRecordList;
    // 笔记内容（前3条文本）
    private List<String> noteList;
    // 书签内容（前3条文本）
    private List<String> bookmarkList;
    // 发送消息（前3条文本）
    private List<String> sendMsgList;
    // 接收消息（前3条文本）
    private List<String> receiveMsgList;
    // AI对话记录（前3条文本）
    private List<String> aiChatList;
    // 书籍评论（前3条文本）
    private List<String> bookCommentList;
    // 论坛评论（前3条文本）
    private List<String> userCommentList;
    // 收藏记录（前3条文本）
    private List<String> collectionList;
    // 借阅记录（前3条文本）
    private List<String> borrowList;
    // 好友列表（前3条文本）
    private List<String> friendList;
    // 发送好友请求（前3条文本）
    private List<String> sentRequestList;
    // 收到好友请求（前3条文本）
    private List<String> receivedRequestList;
}