package com.example.service.impl;

import com.example.dao.*;
import com.example.dto.ResultDTO;
import com.example.dto.UserStatsDTO;
import com.example.entity.BookInformation;
import com.example.entity.UserInformation;
import com.example.entity.UserReadRecord;
import com.example.service.UserStatsService;
import com.example.vo.BookReadStatsVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserStatsServiceImpl implements UserStatsService {

    private final UserReadDailyDao userReadDailyDao;
    private final UserReadProgressDao userReadProgressDao;
    private final UserReadRecordDao userReadRecordDao;
    private final UserTextCollectionDao userTextCollectionDao;
    private final BookmarkDao bookmarkDao;
    private final ChatMessageDao chatMessageDao;
    private final AiChatDao userAiChatDao;
    private final BookCommentDao bookCommentDao;
    private final UserCommentDao userCommentDao;
    private final UserCollectionDao userCollectionDao;
    private final BorrowInformationDao borrowInformationDao;
    private final FriendDao friendDao;
    private final FriendRequestDao friendRequestDao;
    private final UserCommentLikeDao userCommentLikeDao;
    private final UserInformationDao userInformationDao;
    // 👇 注入书籍信息DAO
    private final BookInformationDao bookInformationDao;
    // 构造方法移除 userStatsDao
    public UserStatsServiceImpl(
            UserReadDailyDao userReadDailyDao,
            UserReadProgressDao userReadProgressDao,
            UserReadRecordDao userReadRecordDao,
            UserTextCollectionDao userTextCollectionDao,
            BookmarkDao bookmarkDao,
            ChatMessageDao chatMessageDao,
            AiChatDao userAiChatDao,
            BookCommentDao bookCommentDao,
            UserCommentDao userCommentDao,
            UserCollectionDao userCollectionDao,
            BorrowInformationDao borrowInformationDao,
            FriendDao friendDao,
            FriendRequestDao friendRequestDao,
            UserCommentLikeDao userCommentLikeDao,
            UserInformationDao userInformationDao,
            BookInformationDao bookInformationDao
    ) {
        this.userReadDailyDao = userReadDailyDao;
        this.userReadProgressDao = userReadProgressDao;
        this.userReadRecordDao = userReadRecordDao;
        this.userTextCollectionDao = userTextCollectionDao;
        this.bookmarkDao = bookmarkDao;
        this.chatMessageDao = chatMessageDao;
        this.userAiChatDao = userAiChatDao;
        this.bookCommentDao = bookCommentDao;
        this.userCommentDao = userCommentDao;
        this.userCollectionDao = userCollectionDao;
        this.borrowInformationDao = borrowInformationDao;
        this.friendDao = friendDao;
        this.friendRequestDao = friendRequestDao;
        this.userCommentLikeDao = userCommentLikeDao;
        this.userInformationDao = userInformationDao;
        this.bookInformationDao = bookInformationDao;
    }

    @Override
    public ResultDTO<UserStatsDTO> generateUserStats(String userId, String type) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID不能为空");
            }
            if (type == null || (!type.equals("DAY") && !type.equals("WEEK") && !type.equals("MONTH"))) {
                return ResultDTO.paramError("统计类型错误，仅支持：DAY/WEEK/MONTH");
            }

            LocalDateTime[] timeRange = getTimeRange(type);
            LocalDateTime startTime = timeRange[0];
            LocalDateTime endTime = timeRange[1];

            List<UserInformation> userList = userInformationDao.queryUserById(userId);
            if (userList == null || userList.isEmpty()) {
                return ResultDTO.fail("用户不存在");
            }
            UserInformation user = userList.get(0);

            UserStatsDTO dto = new UserStatsDTO();
            dto.setUserId(userId);
            dto.setUsername(user.getName());
            dto.setStatsType(type);
            dto.setStartTime(startTime);
            dto.setEndTime(endTime);

            dto.setTotalReadSecond(userReadDailyDao.sumReadSecondByUserIdAndTime(userId, startTime, endTime));
            dto.setReadBookCount(userReadProgressDao.countReadBooksByUserIdAndTime(userId, startTime, endTime));
            dto.setReadDayCount(userReadDailyDao.countReadDaysByUserIdAndTime(userId, startTime, endTime));
            dto.setNewNoteCount(userTextCollectionDao.countNewNotesByUserIdAndTime(userId, startTime, endTime));
            dto.setBookmarkCount(bookmarkDao.countBookmarkByTimeRange(userId, startTime, endTime));
            dto.setSendMsgCount(chatMessageDao.countSendMsgByUserIdAndTime(userId, startTime, endTime));
            dto.setReceiveMsgCount(chatMessageDao.countReceiveMsgByUserIdAndTime(userId, startTime, endTime));
            dto.setAiChatCount(userAiChatDao.countAiChatByUserIdAndTime(userId, startTime, endTime));
            dto.setBookCommentCount(bookCommentDao.countBookCommentByUserIdAndTime(userId, startTime, endTime));
            dto.setUserCommentCount(userCommentDao.countUserCommentByUserIdAndTime(userId, startTime, endTime));
            dto.setCollectionCount(userCollectionDao.countCollectionByUserIdAndTime(userId, startTime, endTime));
            dto.setBorrowCount(borrowInformationDao.countBorrowByUserIdAndTime(userId, startTime, endTime));
            dto.setFriendCount(friendDao.countFriendByTimeRange(userId, startTime, endTime));
            dto.setSentRequestCount(friendRequestDao.countSentRequestsByTimeRange(userId, startTime, endTime));
            dto.setReceivedRequestCount(friendRequestDao.countReceivedRequestsByTimeRange(userId, startTime, endTime));
            dto.setLikeCount(userCommentLikeDao.countCommentLikeByTimeRange(userId, startTime, endTime));

            LocalDate startDate = startTime.toLocalDate();
            LocalDate endDate = endTime.toLocalDate();
            dto.setDailyReadList(userReadDailyDao.queryByUserIdAndDateRange(userId, startDate, endDate));
            dto.setReadRecordList(userReadRecordDao.listBookReadDurationByTime(userId, startTime, endTime));
            dto.setNoteList(userTextCollectionDao.queryByTimeRange(startTime, endTime));
            dto.setBookmarkList(bookmarkDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setSendMsgList(chatMessageDao.querySendMessagesByTimeRange(userId, startTime, endTime));
            dto.setReceiveMsgList(chatMessageDao.queryReceiveMessagesByTimeRange(userId, startTime, endTime));
            dto.setAiChatList(userAiChatDao.selectHistoryByTimeRange(userId, startTime, endTime));
            dto.setBookCommentList(bookCommentDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setUserCommentList(userCommentDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setCollectionList(userCollectionDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setBorrowList(borrowInformationDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setFriendList(friendDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            dto.setSentRequestList(friendRequestDao.querySentRequestsByTimeRange(userId, startTime, endTime));
            dto.setReceivedRequestList(friendRequestDao.queryReceivedRequestsByTimeRange(userId, startTime, endTime));
            dto.setUserCommentLikeList(userCommentLikeDao.queryByUserIdAndTimeRange(userId, startTime, endTime));
            // ===================== 核心新增：封装书籍阅读统计列表 =====================
            List<BookReadStatsVO> bookReadStatsList = buildBookReadStatsList(dto.getReadRecordList());
            dto.setBookReadStatsList(bookReadStatsList);
            return ResultDTO.success(dto);

        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (Exception e) {
            return ResultDTO.fail("生成统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 构建书籍阅读统计列表：去重 + 查询书籍信息 + 封装VO
     */
    private List<BookReadStatsVO> buildBookReadStatsList(List<UserReadRecord> readRecordList) {
        if (readRecordList == null || readRecordList.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 按ISBN去重（保留第一条，已按总时长排序）
        Map<String, UserReadRecord> recordMap = readRecordList.stream()
                .collect(Collectors.toMap(
                        UserReadRecord::getIsbn,
                        record -> record,
                        (existing, replacement) -> existing // 重复ISBN保留第一条
                ));

        // 2. 封装VO + 查询书籍信息
        List<BookReadStatsVO> result = new ArrayList<>();
        for (Map.Entry<String, UserReadRecord> entry : recordMap.entrySet()) {
            String isbn = entry.getKey();
            UserReadRecord record = entry.getValue();

            BookReadStatsVO vo = new BookReadStatsVO();
            vo.setIsbn(isbn);
            vo.setTotalDuration(record.getReadDuration());
            vo.setLastReadTime(record.getCreateTime());

            // 3. 查询书籍详情
            List<BookInformation> bookInfoList = bookInformationDao.queryByISBN(isbn);
            if (bookInfoList != null && !bookInfoList.isEmpty()) {
                BookInformation book = bookInfoList.get(0);
                vo.setBookname(book.getBookname());
                vo.setAuthor(book.getAuthor());
                vo.setPictureName(book.getPictureName());
                vo.setPublisher(book.getPublisher());
                vo.setBookTypeName(book.getBookTypeName());
            }

            result.add(vo);
        }

        // 4. 按总时长降序排序（最终列表）
        result.sort((a, b) -> b.getTotalDuration().compareTo(a.getTotalDuration()));
        return result;
    }

    private LocalDateTime[] getTimeRange(String type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case "DAY" -> {
                LocalDate yesterday = now.minusDays(1).toLocalDate();
                LocalDateTime start = yesterday.atStartOfDay();
                LocalDateTime end = yesterday.atTime(23, 59, 59, 999999999);
                yield new LocalDateTime[]{start, end};
            }
            case "WEEK" -> {
                LocalDateTime thisMonday = now.with(java.time.DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
                LocalDateTime lastMonday = thisMonday.minusDays(7);
                LocalDateTime lastSunday = lastMonday.plusDays(6).with(LocalDateTime.MAX.toLocalTime());
                yield new LocalDateTime[]{lastMonday, lastSunday};
            }
            case "MONTH" -> {
                LocalDateTime lastMonth = now.minusMonths(1);
                LocalDateTime firstDayOfLastMonth = lastMonth.withDayOfMonth(1).toLocalDate().atStartOfDay();
                LocalDateTime lastDayOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth())
                        .with(LocalDateTime.MAX.toLocalTime());
                yield new LocalDateTime[]{firstDayOfLastMonth, lastDayOfLastMonth};
            }
            default -> throw new IllegalArgumentException("不支持的统计类型");
        };
    }
}