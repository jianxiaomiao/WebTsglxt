package com.example.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.dao.UserReportDao;
import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;
import com.example.dto.UserReportDTO;
import com.example.dto.UserStatsForAIDTO;
import com.example.entity.*;
import com.example.service.AiChatService;
import com.example.service.UserStatsService;
import com.example.service.UserReportService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserReportServiceImpl implements UserReportService {

    private final UserStatsService userStatsService;
    private final AiChatService aiChatService;
    // 注入报告缓存DAO
    private final UserReportDao userReportDao;

    // 构造注入新增 DAO
    public UserReportServiceImpl(UserStatsService userStatsService, AiChatService aiChatService, UserReportDao userReportDao) {
        this.userStatsService = userStatsService;
        this.aiChatService = aiChatService;
        this.userReportDao = userReportDao;
    }

    @Override
    public ResultDTO<UserReportDTO> generateUserReport(String userId, String type) {
        try {
            // 1. 生成时间段唯一标记
            String dateTag = generateDateTag(type);

            // 2. 先查缓存：存在直接返回
            List<UserReport> cacheList = userReportDao.selectByUserAndTypeAndDate(userId, type, dateTag);
            if (!cacheList.isEmpty()) {
                UserReport cache = cacheList.get(0);
                UserReportDTO report = JSON.parseObject(cache.getContent(), UserReportDTO.class);
                return ResultDTO.success(report);
            }

            // 3. 缓存不存在：生成【完整】统计数据（给前端展示用）
            ResultDTO<UserStatsDTO> statsResult = userStatsService.generateUserStats(userId, type);
            if (!statsResult.isSuccess()) {
                return ResultDTO.fail(statsResult.getMsg());
            }
            UserStatsDTO fullStats = statsResult.getData();

            // ===================== 新增：转换为【AI精简数据】 =====================
            UserStatsForAIDTO aiStats = convertToAIStats(fullStats);

            // 4. 生成AI评语（只传精简数据！）
            ResultDTO<String> commentResult = aiChatService.generateReportComment(aiStats);
            if (!commentResult.isSuccess()) {
                return ResultDTO.fail(commentResult.getMsg());
            }

            // 5. 组装完整报告（前端用完整数据，AI用精简数据）
            UserReportDTO report = new UserReportDTO();
            report.setStats(fullStats); // 前端：完整DTO
            report.setAiComment(commentResult.getData());
            report.setReportTitle(getReportTitle(type));
            report.setType(type);
            report.setDate(dateTag);
            report.setCreateTime(LocalDateTime.now());

            // 6. 保存完整报告到数据库
            UserReport saveReport = new UserReport();
            saveReport.setUserId(userId);
            saveReport.setType(type);
            saveReport.setContent(JSON.toJSONString(report));
            saveReport.setCreateTime(LocalDateTime.now());
            saveReport.setDate(dateTag);
            userReportDao.insert(saveReport);

            return ResultDTO.success(report);
        } catch (Exception e) {
            return ResultDTO.fail("生成报告异常：" + e.getMessage());
        }
    }

    /**
     * 核心转换：完整用户统计DTO → AI专用精简DTO
     * 严格按照需求：保留所有数字 + 各列表仅取前3条纯文本
     */
    private UserStatsForAIDTO convertToAIStats(UserStatsDTO fullStats) {
        UserStatsForAIDTO aiStats = new UserStatsForAIDTO();

        // ===================== 1. 复制所有统计数字（一字不差） =====================
        aiStats.setTotalReadSecond(fullStats.getTotalReadSecond());
        aiStats.setReadBookCount(fullStats.getReadBookCount());
        aiStats.setReadDayCount(fullStats.getReadDayCount());
        aiStats.setNewNoteCount(fullStats.getNewNoteCount());
        aiStats.setSendMsgCount(fullStats.getSendMsgCount());
        aiStats.setReceiveMsgCount(fullStats.getReceiveMsgCount());
        aiStats.setAiChatCount(fullStats.getAiChatCount());
        aiStats.setLikeCount(fullStats.getLikeCount());
        aiStats.setBookCommentCount(fullStats.getBookCommentCount());
        aiStats.setUserCommentCount(fullStats.getUserCommentCount());
        aiStats.setCollectionCount(fullStats.getCollectionCount());
        aiStats.setBorrowCount(fullStats.getBorrowCount());
        aiStats.setFriendCount(fullStats.getFriendCount());
        aiStats.setSentRequestCount(fullStats.getSentRequestCount());
        aiStats.setReceivedRequestCount(fullStats.getReceivedRequestCount());
        aiStats.setBookmarkCount(fullStats.getBookmarkCount());

        // ===================== 2. 提取所有列表前3条纯文本 =====================
        // 书籍阅读统计（书名+作者）
        aiStats.setBookReadStatsList(fullStats.getBookReadStatsList().stream()
                .limit(3)
                .map(vo -> vo.getBookname() + " - " + vo.getAuthor() + " - " + vo.getBookTypeName())
                .toList());

        // 每日阅读记录
        aiStats.setDailyReadList(fullStats.getDailyReadList().stream()
                .limit(31)
                .map(item -> item.getReadDate() + " 阅读" + item.getReadDuration() + "秒")
                .toList());

        // 阅读时长明细
        aiStats.setReadRecordList(fullStats.getReadRecordList().stream()
                .limit(3)
                .map(item -> "ISBN:" + item.getIsbn() + " 阅读" + item.getReadDuration() + "秒")
                .toList());

        // 笔记内容（纯文本）
        aiStats.setNoteList(fullStats.getNoteList().stream()
                .limit(3)
                .map(UserTextCollection::getText)
                .toList());

        // 书签内容
        aiStats.setBookmarkList(fullStats.getBookmarkList().stream()
                .limit(3)
                .map(Bookmark->"添加书签的书籍isbn" + Bookmark.getIsbn()) // 请对应你的书签实体文本字段
                .toList());

        // 发送的消息
        aiStats.setSendMsgList(fullStats.getSendMsgList().stream()
                .limit(3)
                .map(ChatMessage::getMessageContent)
                .toList());

        // 接收的消息
        aiStats.setReceiveMsgList(fullStats.getReceiveMsgList().stream()
                .limit(3)
                .map(ChatMessage::getMessageContent)
                .toList());

        // AI对话记录
        aiStats.setAiChatList(fullStats.getAiChatList().stream()
                .limit(3)
                .map(UserAiChat::getMessageContent)
                .toList());

        // 书籍评论
        aiStats.setBookCommentList(fullStats.getBookCommentList().stream()
                .limit(3)
                .map(BookComment::getComment)
                .toList());

        // 论坛评论
        aiStats.setUserCommentList(fullStats.getUserCommentList().stream()
                .limit(3)
                .map(UserComment::getUserComment)
                .toList());

        // 收藏记录
        aiStats.setCollectionList(fullStats.getCollectionList().stream()
                .limit(3)
                .map(item -> "收藏书籍isbn：" + item.getIsbn())
                .toList());

        // 借阅记录
        aiStats.setBorrowList(fullStats.getBorrowList().stream()
                .limit(3)
                .map(item -> "借阅书籍isbn：" + item.getISBN())
                .toList());

        // 好友列表
        aiStats.setFriendList(fullStats.getFriendList().stream()
                .limit(3)
                .map(Friend::getFriendId)
                .toList());

        // 发送好友请求
        aiStats.setSentRequestList(fullStats.getSentRequestList().stream()
                .limit(3)
                .map(item -> "请求添加：" + item.getFromUserId() + "请求内容：" + item.getRequestMsg())
                .toList());

        // 收到好友请求
        aiStats.setReceivedRequestList(fullStats.getReceivedRequestList().stream()
                .limit(3)
                .map(item -> "收到请求：" + item.getToUserId() + "请求内容：" + item.getRequestMsg())
                .toList());

        return aiStats;
    }

    // 生成时间段唯一标记
    private String generateDateTag(String type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case "DAY" -> now.minusDays(1).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "WEEK" -> now.with(java.time.DayOfWeek.MONDAY).minusDays(7).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "MONTH" -> now.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default -> "";
        };
    }

    private String getReportTitle(String type) {
        return switch (type) {
            case "DAY" -> "📅 日度阅读成长报告";
            case "WEEK" -> "📊 周度阅读成长周报";
            case "MONTH" -> "🏆 月度阅读成长月报";
            default -> "📖 用户阅读成长报告";
        };
    }
    // ==================== 新增：查询报告历史 ====================
    @Override
    public ResultDTO<List<UserReportDTO>> getReportHistory(String userId, String type, Integer pageNum, Integer pageSize) {
        try {
            // 1. 核心参数校验
            if (userId == null || userId.trim().isEmpty()) {
                return ResultDTO.fail("用户ID不能为空");
            }

            // 2. 调用DAO查询分页数据
            List<UserReport> reportList = userReportDao.selectReportHistory(userId, type, pageNum, pageSize);

            // 3. 转换：数据库实体 → DTO（解析JSON内容）
            List<UserReportDTO> dtoList = new ArrayList<>();
            for (UserReport report : reportList) {
                UserReportDTO dto = JSON.parseObject(report.getContent(), UserReportDTO.class);
                // 补充基础字段，方便前端展示
                dto.setType(report.getType());
                dto.setDate(report.getDate());
                dto.setCreateTime(report.getCreateTime());
                dtoList.add(dto);
            }
            return ResultDTO.success(dtoList);
        } catch (Exception e) {
            return ResultDTO.fail("查询报告历史失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<UserReportDTO> getReportByDate(String userId, String type, String date) {
        try {
            // 1. 参数校验
            if (userId == null || userId.trim().isEmpty()) {
                return ResultDTO.fail("用户ID不能为空");
            }
            if (type == null || type.trim().isEmpty()) {
                return ResultDTO.fail("报告类型不能为空");
            }
            if (date == null || date.trim().isEmpty()) {
                return ResultDTO.fail("报告日期不能为空");
            }

            // 2. 直接查询指定日期的报告
            List<UserReport> cacheList = userReportDao.selectByUserAndTypeAndDate(userId, type, date);
            if (cacheList.isEmpty()) {
                return ResultDTO.fail("未找到该日期的报告");
            }

            // 3. 转换为DTO
            UserReport cache = cacheList.get(0);
            UserReportDTO report = JSON.parseObject(cache.getContent(), UserReportDTO.class);
            // 补充报告基础信息
            report.setType(cache.getType());
            report.setDate(cache.getDate());
            report.setCreateTime(cache.getCreateTime());

            return ResultDTO.success(report);
        } catch (Exception e) {
            return ResultDTO.fail("查询报告失败：" + e.getMessage());
        }
    }
}