package com.example.listener;

import com.example.config.AiConfig;
import com.example.service.UserDailyAiPictureService;
import com.example.service.impl.UserDailyAiPictureServiceImpl;
import com.example.dao.impl.UserDailyAiPictureDaoImpl;
import com.example.entity.UserDailyAiPicture;
import com.example.util.ImageUtil;
import java.time.format.DateTimeFormatter;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.dao.UserBehaviorLogDao;
import com.example.dao.UserProfileMemoryDao;
import com.example.dao.impl.UserBehaviorLogDaoImpl;
import com.example.dao.impl.UserProfileMemoryDaoImpl;
import com.example.entity.UserBehaviorLog;
import com.example.entity.UserProfileMemory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.example.config.AiConfig.*;
import java.time.Duration;
import java.util.Random;
@WebListener
public class NightlyAiTaskListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(NightlyAiTaskListener.class);
    private ScheduledExecutorService scheduler;

    private final UserBehaviorLogDao behaviorDao = new UserBehaviorLogDaoImpl();
    private final UserProfileMemoryDao profileDao = new UserProfileMemoryDaoImpl();
    // 引入插画服务
    private final UserDailyAiPictureService pictureService = new UserDailyAiPictureServiceImpl(new UserDailyAiPictureDaoImpl());
    // 文本截断常量配置
    private static final int MAX_RAW_LENGTH = 200;
    private static final int HALF_CUT = 100;

    // 画像任务固定System提示词：新增说明日志携带操作类型标签
    private static final String PROFILE_SYSTEM_PROMPT = "你是专业用户阅读行为分析师。用户提供的每条行为日志均标注了【操作类型】，请结合行为分类与行为详情综合分析用户。" +
            "仅返回纯JSON，禁止额外解释、换行、注释、markdown标记。" +
            "JSON固定三个字符串字段：reading_preferences(阅读偏好总结)、emotional_state(用户当日情绪倾向)、character_sketch(完整用户人物画像速写)。";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // 计算距离今日/明日凌晨1点的延迟分钟数
        LocalDateTime now = LocalDateTime.now();
        // 今天凌晨1点
        LocalDateTime targetToday = LocalDate.now().atTime(1, 0);
        LocalDateTime target;
        if (now.isAfter(targetToday)) {
            // 当前时间已过凌晨1点 → 等到明天凌晨1点
            target = targetToday.plusDays(1);
        } else {
            // 还没到今日凌晨1点 → 今日凌晨1点执行
            target = targetToday;
        }
        // 时间差转总分钟
        long delayMinutes = Duration.between(now, target).toMinutes();

        // 延迟delayMinutes后首次执行，之后每24小时循环
        scheduler.scheduleAtFixedRate(this::generateDailyProfile, delayMinutes, 24 * 60, TimeUnit.MINUTES);
        logger.info("🌙 夜间AI画像总结定时任务已启动，下次执行时间：{}，周期24小时", target);
    }

   /* @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // ===================== 任务1：服务启动1分钟后，立即执行一次（仅运行一次） =====================
        long firstRunDelay = 60; // 单位：秒，延迟1分钟启动首次任务
        scheduler.schedule(this::generateDailyProfile, firstRunDelay, TimeUnit.SECONDS);
        logger.info("🚀 服务启动一次性任务已注册，将在1分钟后执行首次用户画像生成");

        // ===================== 任务2：每日凌晨1点循环执行（原有定时逻辑不变） =====================
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetToday = LocalDate.now().atTime(1, 0);
        LocalDateTime target;
        if (now.isAfter(targetToday)) {
            target = targetToday.plusDays(1);
        } else {
            target = targetToday;
        }
        long dailyDelayMinutes = Duration.between(now, target).toMinutes();
        // 每24小时循环执行
        scheduler.scheduleAtFixedRate(this::generateDailyProfile, dailyDelayMinutes, 24 * 60, TimeUnit.MINUTES);
        logger.info("🌙 每日凌晨1点循环定时任务已启动，下次定点执行时间：{}，周期24小时", target);
    }*/

    /**
     * 文本截断工具：超过200字则保留前100 + ... + 后100
     */
    private String truncateSnapshot(String origin) {
        if (origin == null || origin.isEmpty()) return "";
        int len = origin.length();
        if (len <= MAX_RAW_LENGTH) return origin;
        String prefix = origin.substring(0, HALF_CUT);
        String suffix = origin.substring(len - HALF_CUT);
        return prefix + "......[内容过长已截断]......" + suffix;
    }

    /**
     * 根据action_type数字转换中文操作描述，方便AI理解行为分类
     */
    private String getActionDesc(Integer actionType) {
        if (actionType == null) return "未知操作";
        return switch (actionType) {
            case 1 -> "点击书籍";
            case 2 -> "收藏书籍";
            case 3 -> "阅读";
            case 4 -> "听书";
            case 5 -> "添加笔记";
            case 6 -> "AI总结";
            case 7 -> "书籍评价";
            case 8 -> "推荐书籍";
            case 9 -> "AI对话";
            case 10 -> "论坛评论";
            case 11 -> "取消收藏书籍";
            case 12 -> "书广场发帖(求书/推书)";
            case 13 -> "借书";
            case 14 -> "发消息";
            case 15 -> "撤回消息";
            case 16 -> "删除消息";
            case 17 -> "发送好友请求";
            case 18 -> "接收好友请求";
            case 19 -> "拒绝好友请求";
            case 20 -> "创建节点";
            case 21 -> "创建节点联系";
            case 22 -> "树洞谈心";
            case 23 -> "书籍游戏";
            case 24 -> "文本优化";
            case 25 -> "书籍业务处理";
            case 26 -> "文件识别";
            case 27 -> "默认对话";
            case 28 -> "登录";
            case 29 -> "注册";
            case 30 -> "点赞/取消点赞评论";
            case 31 -> "阅读时长累计";
            case 32 -> "更新阅读进度";
            case 33 -> "更新书籍当日阅读时长";
            case 34 -> "AI管家";
            case 35 -> "AI辩论小助手";
            case 36 -> "AI出题";
            case 37 -> "AI深度语音";
            case 38 -> "捞取漂流瓶";
            case 39 -> "投放漂流瓶";
            default -> "未知操作类型(" + actionType + ")";
        };
    }

    private void generateDailyProfile() {
        try {
            logger.info("===== 开始执行每日用户画像提炼任务 =====");
            LocalDateTime startTime = LocalDate.now().minusDays(1).atStartOfDay();
            LocalDateTime endTime = LocalDate.now().atStartOfDay();
            LocalDate targetDate = LocalDate.now().minusDays(1);
            logger.info("统计日期范围：{} ~ {}", startTime, endTime);

            List<String> activeUserIds = behaviorDao.queryActiveUserIds(startTime, endTime);
            if (activeUserIds.isEmpty()) {
                logger.info("昨日无活跃用户，任务直接结束");
                return;
            }
            logger.info("昨日活跃用户总数：{}", activeUserIds.size());

            for (String userId : activeUserIds) {
                try {
                    handleSingleUserProfile(userId, startTime, endTime, targetDate);
                } catch (Exception e) {
                    logger.error("处理用户{}画像失败，跳过该用户", userId, e);
                }
            }
            logger.info("===== 每日用户画像提炼任务全部完成 =====");
        } catch (Exception e) {
            logger.error("夜间AI主任务全局异常", e);
        }
    }

    /**
     * 处理单个用户全流程：查日志→添加行为类型批注+截断→调用AI→入库
     */
    private void handleSingleUserProfile(String userId, LocalDateTime start, LocalDateTime end, LocalDate logDate) throws IOException {
        List<UserBehaviorLog> userLogs = behaviorDao.queryByUserIdAndTimeRange(userId, start, end);
        if (userLogs.isEmpty()) {
            logger.warn("用户{}昨日无行为日志，跳过", userId);
            return;
        }

        // 核心改动：每条日志拼接【操作类型：xxx】批注，再拼接截断后的行为详情
        String fullLogText = userLogs.stream()
                .map(log -> {
                    String actionName = getActionDesc(log.getAction_type());
                    String snapshot = truncateSnapshot(log.getContent_snapshot());
                    // 单条日志固定格式，AI可读性更强
                    return "【操作类型：" + actionName + "】行为详情：" + snapshot;
                })
                .collect(Collectors.joining("\n=====================\n"));

        logger.info("用户{}日志条数：{}，拼接后带类型批注文本长度：{}", userId, userLogs.size(), fullLogText.length());

        // 调用豆包API，传入固定systemPrompt + 带行为标注的完整日志
        String aiJsonResult = callDoubaoApi(fullLogText, PROFILE_SYSTEM_PROMPT);
        logger.info("用户{} AI返回画像原始内容：{}", userId, aiJsonResult);

        // 解析JSON并入库
        UserProfileMemory memory = parseAiJsonToProfile(aiJsonResult);
        memory.setUser_id(userId);
        memory.setDate(logDate);
        profileDao.add(memory);
        logger.info("用户{}画像数据入库成功", userId);
        // ==================== 🔥 新增：触发每日治愈插画生成 ====================
        try {
            generateAndSaveDailyPicture(userId, logDate, memory);
        } catch (Exception e) {
            logger.error("用户{}每日插画生成失败，但不影响文字画像流程", userId, e);
        }
    }

    // ===================== 豆包API调用方法（无改动，保留原有连接关闭逻辑） =====================
    private String callDoubaoApi(String question, String systemPrompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ENDPOINT_ID);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 800);
        requestBody.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        URL url = new URL(CHAT_API_URL);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(40000);
            conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = JSON.toJSONString(requestBody).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                StringBuilder errorMsg = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) errorMsg.append(line);
                }
                throw new IOException("ARK接口调用失败，响应码:" + responseCode + " 错误详情:" + errorMsg);
            }

            // 读取正常返回
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                JSONObject json = JSON.parseObject(response.toString());
                return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            }
        } finally {
            // 强制关闭连接，防止端口泄漏
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析AI返回JSON封装画像实体
     */
    private UserProfileMemory parseAiJsonToProfile(String jsonStr) {
        UserProfileMemory memory = new UserProfileMemory();
        JSONObject json = JSON.parseObject(jsonStr);
        memory.setReading_preferences(json.getString("reading_preferences"));
        memory.setEmotional_state(json.getString("emotional_state"));
        memory.setCharacter_sketch(json.getString("character_sketch"));
        return memory;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            logger.info("定时任务线程池已关闭");
        }
    }

    // 新增全局随机器
    private static final Random RANDOM = new Random();
    // 新增：动物主体随机池（每次二选一，强化画面随机性）
    private static final String[] ANIMAL_POOL = {"圆滚滚短毛小奶猫", "短腿垂耳小奶狗"};
    // ========== 三套主题全部随机变量池 ==========
// 套装A 书香雅韵池
    private static final String[] A_CLOTHES = {"古风浅青襦裙配粉色飘带","素白棉麻读书斗篷","宽松浅棕书房居家睡衣","刺绣文人短褂"};
    private static final String[] A_PROP = {"抱着鎏金琵琶","捧着线装古籍诗集","手持木质书签与毛笔","膝上摊开读书笔记卷轴","指尖托一盏青瓷茶盏"};
    private static final String[] A_SCENE = {"月夜柳树彩灯戏台","星空窗边书桌","古风竹林溪水","飘窗堆满书籍","暖光书房木书架"};
    private static final String[] A_ATMOS = {"漫天细碎星光+流星","彩色小串灯","飘落桃花花瓣","漂浮墨色云纹","宣纸毛笔小字点缀"};

    // 套装B 山野漫游池
    private static final String[] B_CLOTHES = {"浅绿古风游侠短装配斜挎布包","蓝白条纹水手航海服+贝雷帽","宽松户外浅卡其登山外套","棉麻野餐连衣裙"};
    private static final String[] B_PROP = {"铺开仿古藏宝地图","复古单筒望远镜","小木行李箱","沙滩小渔网","野餐藤编篮子"};
    private static final String[] B_SCENE = {"青山瀑布桃花溪谷","海边落日沙滩渔船","山间古石桥水车","湖边草坪露营","云海观景台"};
    private static final String[] B_ATMOS = {"漫天春日细雨","海边橘色晚霞","山间薄雾流云","沙滩小螃蟹","飘落柳叶"};

    // 套装C 活力元气池
    private static final String[] C_CLOTHES = {"印哑铃图案浅蓝运动T恤短裤+黄跑鞋","电竞战队黑橙冲锋外套护目镜","宽松休闲篮球卫衣","拳击运动速干上衣"};
    private static final String[] C_PROP = {"红色拳击手套","电竞金色法杖","健身哑铃","电脑办公前台","彩色派对气球彩带"};
    private static final String[] C_SCENE = {"明亮阳光健身房","电竞舞台领奖台","阳光落地窗边办公桌","派对星空舞台","室内运动场馆"};
    private static final String[] C_ATMOS = {"阳光斜射光束","彩色气球彩带","金色放射光芒","运动海报","赛场亮片碎彩"};

    // 全新猫狗手绘统一画风基底（已移除王者灵宝）
    private static final String BASE_LINGBAO_STYLE = "【固定画风：治愈Q版手账淡水彩手绘，完全匹配参考卡通插画质感】画面主体随机为两种形象之一：圆滚滚短毛小奶猫 / 短腿垂耳小奶狗，团子型矮胖身躯，" +
            "全身无任何尖锐棱角；粗圆润黑色描边线条，哑光软塑胶玩偶质感，低饱和马卡龙薄涂水彩平上色，全局柔和漫射柔光，无硬阴影、无立体3D渲染；横版宽幅日记卡片构图，画面四周浅黄复古牛皮纸细边框，" +
            "大面积留白，干净简约无杂乱堆砌元素，童趣蜡笔涂鸦笔触，2D纯平面插画，不出现人类、人形角色、王者灵宝形象。";
    // 统一负面提示词
    private static final String UNIFY_NEGATIVE = "写实、真人照片、3D建模、厚重立体光影、暗黑色调、丑陋畸形五官、尖锐棱角、浓艳高饱和色彩、多人物、水印、文字遮挡主体" +
            "、恐怖血腥、金属尖锐反光、复杂堆砌杂物、卡通不是王者灵宝、长尖耳朵、人形长腿、写实皮肤纹理";

    /**
     * 生成每日卡通插画并入库
     */
    /**
     * 生成每日卡通插画并入库
     */
    private void generateAndSaveDailyPicture(String userId, LocalDate logDate, UserProfileMemory memory) throws Exception {
        // 1. 基础信息截取不变
        String emotion = memory.getEmotional_state() != null ? memory.getEmotional_state() : "平静温和";
        String pref = memory.getReading_preferences() != null ? memory.getReading_preferences() : "综合阅读";
        if (emotion.length() > 100) emotion = emotion.substring(0, 100);
        if (pref.length() > 100) pref = pref.substring(0, 100);
        String dateStr = logDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));

        // 智能匹配动物：安静内敛→猫咪，活泼外向→小狗，无特征则随机
        String mainAnimal;
        if (emotion.contains("安静") || emotion.contains("独处") || emotion.contains("内向") || pref.contains("阅读")) {
            mainAnimal = ANIMAL_POOL[0]; // 小奶猫
        } else if (emotion.contains("开朗") || emotion.contains("活力") || pref.contains("社交") || pref.contains("运动")) {
            mainAnimal = ANIMAL_POOL[1]; // 小奶狗
        } else {
            mainAnimal = ANIMAL_POOL[RANDOM.nextInt(ANIMAL_POOL.length)];
        }

        // ========== 2. 随机选择三套主题，可替换为画像智能匹配逻辑 ==========
        int themeType = RANDOM.nextInt(3);
        String clothes, prop, scene, atmos, diaryTitle;
        switch (themeType) {
            case 0:
                // 套装A 书香雅韵
                clothes = A_CLOTHES[RANDOM.nextInt(A_CLOTHES.length)];
                prop = A_PROP[RANDOM.nextInt(A_PROP.length)];
                scene = A_SCENE[RANDOM.nextInt(A_SCENE.length)];
                atmos = A_ATMOS[RANDOM.nextInt(A_ATMOS.length)];
                diaryTitle = "阅读心语日记";
                break;
            case 1:
                // 套装B 山野漫游
                clothes = B_CLOTHES[RANDOM.nextInt(B_CLOTHES.length)];
                prop = B_PROP[RANDOM.nextInt(B_PROP.length)];
                scene = B_SCENE[RANDOM.nextInt(B_SCENE.length)];
                atmos = B_ATMOS[RANDOM.nextInt(B_ATMOS.length)];
                diaryTitle = "漫游治愈日记";
                break;
            default:
                // 套装C 活力元气
                clothes = C_CLOTHES[RANDOM.nextInt(C_CLOTHES.length)];
                prop = C_PROP[RANDOM.nextInt(C_PROP.length)];
                scene = C_SCENE[RANDOM.nextInt(C_SCENE.length)];
                atmos = C_ATMOS[RANDOM.nextInt(C_ATMOS.length)];
                diaryTitle = "活力日常日记";
                break;
        }

        // ========== 拼接最终正向提示词（插入随机猫狗主体） ==========
        String positivePrompt = String.format(
                "%s\n画面顶部手写温柔字体标注日期：%s，侧边手绘小字「%s」；\n" +
                        "画面C位是%s，穿搭：%s，神态：%s，贴合用户阅读偏好：%s；\n" +
                        "核心道具：%s；背景场景：%s；氛围装饰：%s；\n整体画面柔和治愈，低饱和马卡龙配色，无多余杂物。",
                BASE_LINGBAO_STYLE, dateStr, diaryTitle, mainAnimal, clothes, emotion, pref, prop, scene, atmos
        );
        String negativePrompt = UNIFY_NEGATIVE;

        // 下方原有文生图、下载、入库逻辑完全不用改动
        String imageUrl = callPureTextToImageApi(positivePrompt, negativePrompt);
        if (imageUrl == null) {
            throw new Exception("API未返回有效图片URL");
        }
        String localDbPath = ImageUtil.downloadAndSaveImage(imageUrl, null);
        UserDailyAiPicture picture = new UserDailyAiPicture();
        picture.setUserId(userId);
        picture.setGenDate(logDate);
        picture.setImgUrl(localDbPath);
        pictureService.addPicture(picture);
        logger.info("✅ 用户{}的每日猫狗治愈插画已成功生成并入库: {}", userId, localDbPath);
    }

    /**
     * 极简版纯文生图请求 (去除文件检测，专为定时任务打造)
     */
    /**
     * 极简版纯文生图请求 (对齐正常可用的generateImage参数规范，修复400报错)
     * @param positivePrompt 正向绘画提示词
     * @param negativePrompt 负面规避提示词
     * @return 图片CDN链接
     */
    private String callPureTextToImageApi(String positivePrompt, String negativePrompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            // 1. 补齐所有和成功generateImage一致的必填参数
            requestBody.put("model", AiConfig.IMAGE_ENDPOINT_ID);
            requestBody.put("prompt", positivePrompt);
            requestBody.put("negative_prompt", negativePrompt); // 独立负面字段，不拼接
            requestBody.put("size", "2560x1440"); // 替换为合规分辨率：2048*2048，在2560*1440 ~ 4096*4096区间内
            requestBody.put("n", AiConfig.IMAGE_NUM);
            requestBody.put("response_format", AiConfig.IMAGE_FORMAT);
            requestBody.put("watermark", true); // 缺失参数1
            requestBody.put("stream", false);    // 缺失参数2

            URL url = new URL(AiConfig.IMAGE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                String reqJson = JSON.toJSONString(requestBody);
                logger.info("定时任务文生图请求体：{}", reqJson); // 打印请求体方便调试
                os.write(reqJson.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // 新增：读取错误流，打印接口返回的精准报错信息
                StringBuilder errorSb = new StringBuilder();
                try (BufferedReader errBr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errLine;
                    while ((errLine = errBr.readLine()) != null) {
                        errorSb.append(errLine);
                    }
                }
                logger.error("文生图API报错，HTTP状态码：{}，接口返回错误详情：{}", responseCode, errorSb);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) res.append(line);
            br.close();

            JSONObject json = JSON.parseObject(res.toString());
            return json.getJSONArray("data").getJSONObject(0).getString("url");
        } catch (Exception e) {
            logger.error("定时任务文生图网络请求失败", e);
            return null;
        }
    }
}