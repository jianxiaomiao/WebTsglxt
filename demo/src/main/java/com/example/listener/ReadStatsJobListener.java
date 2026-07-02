package com.example.listener;

import com.example.dao.UserInformationDao;
import com.example.dao.UserReadDailyDao;
import com.example.dao.UserReadStatsDao;
import com.example.dao.impl.UserInformationDaoImpl;
import com.example.dao.impl.UserReadDailyDaoImpl;
import com.example.dao.impl.UserReadStatsDaoImpl;
import com.example.dto.ReadStatsDTO; // 🔥 补充导入DTO
import com.example.entity.UserInformation;
import com.example.entity.UserReadStats;
import com.example.service.impl.UserReadDailyServiceImpl;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@WebListener
public class ReadStatsJobListener implements ServletContextListener {
    private Timer timer;
    private UserReadDailyServiceImpl readService;
    private UserReadStatsDao statsDao;
    private UserInformationDao userDao;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 初始化依赖
        // 🔥 修正：给UserReadDailyServiceImpl传入两个Dao参数
        readService = new UserReadDailyServiceImpl(
                new UserReadDailyDaoImpl(),
                new UserReadStatsDaoImpl()
        );
        statsDao = new UserReadStatsDaoImpl();
        userDao = new UserInformationDaoImpl();

        // 🔥 修正：Java中Timer的正确写法，去掉Kotlin式的isDaemon: true
        timer = new Timer(true); // true表示守护线程，容器关闭时自动终止
        // 每日凌晨1点执行
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 遍历所有用户，更新阅读统计缓存
                List<UserInformation> userList = userDao.queryAll();
                for (UserInformation user : userList) {
                    try {
                        // 🔥 兼容低版本Java，把var改成明确类型
                        ReadStatsDTO statsDTO = readService.getReadStats(user.getUserId()).getData();
                        UserReadStats stats = new UserReadStats(user.getUserId());
                        // 🔥 修复：null值兜底为0
                        stats.setTotalDuration(statsDTO.getTotalDuration() == null ? 0 : statsDTO.getTotalDuration());
                        stats.setMaxConsecutiveDays(statsDTO.getMaxConsecutiveDays() == null ? 0 : statsDTO.getMaxConsecutiveDays());
                        stats.setCurrentConsecutiveDays(statsDTO.getCurrentConsecutiveDays() == null ? 0 : statsDTO.getCurrentConsecutiveDays());
                        stats.setYearReadDays(statsDTO.getYearReadDays() == null ? 0 : statsDTO.getYearReadDays());
                        statsDao.upsertStats(stats);
                    } catch (Exception e) {
                        // 单个用户异常不影响整体
                        e.printStackTrace(); // 建议加上打印，方便排查问题
                    }
                }
            }
        }, cal.getTime(), 24 * 60 * 60 * 1000); // 24小时执行一次
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (timer != null) {
            timer.cancel();
            timer.purge(); // 建议加上，清除任务队列，避免内存泄漏
        }
    }
}