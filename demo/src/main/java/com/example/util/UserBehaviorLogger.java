package com.example.util;

import com.example.dao.UserBehaviorLogDao;
import com.example.dao.impl.UserBehaviorLogDaoImpl;
import com.example.entity.UserBehaviorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户行为日志异步记录工具类
 */
public class UserBehaviorLogger {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorLogger.class);
    private static final UserBehaviorLogDao logDao = new UserBehaviorLogDaoImpl();

    // 创建一个单线程池，专门用来写日志，不阻塞主业务
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    /**
     * 异步记录用户操作
     * @param userId      用户ID
     * @param actionType  操作类型 (1=click_book, 6=ai_summary, 9=ai_talk 等)
     * @param isbn        书籍ISBN (可为空)
     * @param chapterNum  章节号 (可为空)
     * @param snapshot    内容快照 (笔记内容、提问内容等)
     */
    //'操作类型：1=click_book点击书籍,2=collect_book收藏书籍,3=read阅读,4=listen听书,5=add_note添加笔记,6=ai_summary
    // AI总结,7=book_comment书籍评价,8=recommend_book推荐书籍,9=ai_talk AI对话，10=forum_comment 论坛评论，
    // 11=decollect_book取消收藏书籍', 12=post_booksquare求书or推书， 13=borrow_book借书, 14=send_msg发消息
    // 15=recall_msg发消息， 16=hide_msg删除消息， 17= send_request发送好友请求, 18=accept_request接收好友请求，
    // 19=reject_request接收好友请求,20=node创建节点 ,21=edge创建节点联系，22=树洞谈心 ，23=书籍游戏，
    // 24=文本优化， 25=书籍业务处理， 26=文件识别，27=默认对话，28=登录，29=注册,30=点赞/取消点赞评论，
    // 31=用户阅读时长累计， 32=阅读历史更新（阅读进度增加），33= 书籍当日阅读时长更新,34=ai管家
    // ，35=ai辩论小助手，36=ai出题，37=ai深度语音,38=捞取漂流瓶，39=投放漂流瓶）
    public static void logAsync(String userId, Integer actionType, String isbn, String chapterNum, String snapshot) {
        if (userId == null) return;

        logExecutor.submit(() -> {
            try {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setUser_id(userId);
                log.setAction_type(actionType);
                log.setBook_isbn(isbn);
                log.setChapter_number(chapterNum);
                log.setContent_snapshot(snapshot);
                log.setCreate_time(LocalDateTime.now());

                logDao.add(log); // 存入数据库
                logger.info("✅ 行为日志记录成功 | User:{} | Action:{}", userId, actionType);
            } catch (Exception e) {
                logger.error("❌ 行为日志记录失败", e);
            }
        });
    }
}