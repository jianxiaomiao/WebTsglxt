# 📚 沉浸式图书阅读与交流书屋 — Code Wiki

> 项目名称：**WebTsglxt**（图书管理系统）
> 全栈沉浸式阅读平台 · Vue 3 前端 + Java Servlet 后端

---

## 📋 目录

1. [项目总览](#1-项目总览)
2. [目录结构](#2-目录结构)
3. [前端架构 (Vue 3)](#3-前端架构-vue-3)
   - 3.1 [核心配置](#31-核心配置)
   - 3.2 [路由系统](#32-路由系统)
   - 3.3 [状态管理 (Pinia Store)](#33-状态管理-pinia-store)
   - 3.4 [视图页面](#34-视图页面)
   - 3.5 [公共组件](#35-公共组件)
   - 3.6 [工具函数](#36-工具函数)
   - 3.7 [全局样式与动画](#37-全局样式与动画)
4. [后端架构 (Java Servlet)](#4-后端架构-java-servlet)
   - 4.1 [实体层 (Entity)](#41-实体层-entity)
   - 4.2 [数据访问层 (DAO)](#42-数据访问层-dao)
   - 4.3 [服务层 (Service)](#43-服务层-service)
   - 4.4 [控制层 (Servlet)](#44-控制层-servlet)
   - 4.5 [数据传输对象 (DTO)](#45-数据传输对象-dto)
   - 4.6 [工具类 (Util)](#46-工具类-util)
   - 4.7 [WebSocket 实时通信](#47-websocket-实时通信)
   - 4.8 [监听器 (Listener)](#48-监听器-listener)
5. [AI 功能模块](#5-ai-功能模块)
   - 5.1 [豆包 API 集成](#51-豆包-api-集成)
   - 5.2 [AI 聊天场景](#52-ai-聊天场景)
   - 5.3 [AI 出题系统](#53-ai-出题系统)
   - 5.4 [AI 每日画报](#54-ai-每日画报)
   - 5.5 [TTS 语音朗读](#55-tts-语音朗读)
6. [数据库设计](#6-数据库设计)
7. [配置与环境](#7-配置与环境)
8. [功能模块全景图](#8-功能模块全景图)

---

## 1. 项目总览

### 1.1 项目简介

一个面向图书爱好者的全栈沉浸式阅读平台，融合 **个人阅读管理**、**社交互动** 和 **AI 增强体验** 三大核心模块。

### 1.2 技术栈

| 层级 | 技术选型 |
|------|----------|
| **前端框架** | Vue 3 (Composition API + `<script setup>`) + Vite |
| **状态管理** | Pinia (with persist plugin) |
| **UI 组件库** | Element Plus (自动导入) |
| **HTTP 客户端** | Axios (封装 request.js) |
| **路由** | Vue Router 4 (History 模式) |
| **实时通信** | WebSocket (聊天) + SSE (通知) |
| **用户引导** | Driver.js |
| **后端** | Java 21 + Jakarta Servlet 6.0 |
| **数据访问** | JDBC 封装 + MyBatis (仅 BookMapper) |
| **JSON 处理** | FastJSON 2 + Jackson |
| **连接池** | HikariCP (主) + Druid |
| **数据库** | MySQL 8.0 (`library_manager`) |
| **构建工具** | Maven (后端) / Vite (前端) |
| **部署** | Tomcat 11 (WAR 包) |

### 1.3 核心功能矩阵

```
┌─────────────────────────────────────────────┐
│           沉浸式图书阅读交流平台               │
│  ┌─────────┬──────────┬──────────────────┐   │
│  │ 阅读管理  │ 社交互动  │   AI 增强体验    │   │
│  ├─────────┼──────────┼──────────────────┤   │
│  │ 书架管理 │ 实时聊天  │ AI 聊天助手      │   │
│  │ 阅读器   │ 好友系统  │ AI 出题互动      │   │
│  │ 书签系统 │ 论坛社区  │ AI 每日画报      │   │
│  │ 阅读统计 │ 漂流瓶   │ TTS 语音朗读     │   │
│  │ 借阅管理 │ 共读舱   │ AI 书籍总结      │   │
│  │ 笔记系统 │ 通知系统  │ 宠物 AI 管家     │   │
│  │ 知识图谱 │ 用户主页  │ AI 周报生成      │   │
│  └─────────┴──────────┴──────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## 2. 目录结构

```
WebTsglxt/
├── demo/                                  # ★ 主项目
│   ├── frontend/                          # 前端项目 (Vue 3 + Vite)
│   │   ├── public/dict/                   # 静态字典文件
│   │   ├── src/
│   │   │   ├── assets/                    # 静态资源（图片等）
│   │   │   ├── components/               # 公共组件
│   │   │   │   ├── BasicLayout.vue       # 全局布局外壳
│   │   │   │   ├── CoReadingRoom.vue     # ✨ 共读舱组件
│   │   │   │   ├── GlobalContextMenu.vue  # 全局右键菜单
│   │   │   │   ├── HelloWorld.vue        # 示例组件
│   │   │   │   ├── PetWidget.vue         # 宠物 AI 挂件
│   │   │   │   └── WeatherAtmosphere.vue # 天气氛围组件
│   │   │   ├── constants/                # 常量定义
│   │   │   ├── router/
│   │   │   │   └── index.js              # 路由配置 + 导航守卫
│   │   │   ├── stores/                   # Pinia 状态仓库
│   │   │   │   ├── appSettingsStore.js   # ✨ 应用设置（壁纸/控件开关）
│   │   │   │   ├── petStore.js           # 宠物 AI 交互状态
│   │   │   │   ├── readerStore.js        # 阅读器状态
│   │   │   │   └── userStore.js          # 用户认证 & 全局状态
│   │   │   ├── utils/
│   │   │   │   ├── appGuide.js           # Driver.js 新手引导
│   │   │   │   ├── request.js            # Axios 封装
│   │   │   │   └── routerHelper.js       # 路由辅助（预加载导航）
│   │   │   ├── views/                    # ★ 21个页面视图
│   │   │   │   ├── Login.vue / Register.vue     # 登录/注册
│   │   │   │   ├── Home.vue                     # 首页（推荐/日签/塔罗）
│   │   │   │   ├── Bookshelf.vue                # 书架管理
│   │   │   │   ├── BookDetail.vue               # 书籍详情
│   │   │   │   ├── BookReader.vue               # ★ 沉浸式阅读器
│   │   │   │   ├── BookSquare.vue / ...Detail   # 书广场
│   │   │   │   ├── Forum.vue                    # 论坛社区
│   │   │   │   ├── Chat.vue                     # 实时聊天
│   │   │   │   ├── Message.vue                  # 消息中心
│   │   │   │   ├── Profile.vue                  # 个人中心
│   │   │   │   ├── UserInfo.vue                 # 用户信息编辑
│   │   │   │   ├── UserProfile.vue              # 对外用户主页
│   │   │   │   ├── BorrowInfo.vue               # 借阅记录
│   │   │   │   ├── MyNotesPage.vue              # 我的笔记
│   │   │   │   ├── MyBookComments.vue           # 书籍评论
│   │   │   │   ├── MyForumComments.vue          # 论坛评论
│   │   │   │   ├── ReadHistoryGallery.vue       # 阅读历史画廊
│   │   │   │   ├── WeeklyReport.vue             # 阅读周报
│   │   │   │   ├── ShareRedirect.vue            # 分享短链重定向
│   │   │   │   └── Settings.vue                 # ✨ 设置页
│   │   │   ├── App.vue                   # 根组件（壁纸/天气/宠物/共读舱）
│   │   │   ├── main.js                   # 入口（Element Plus + Pinia）
│   │   │   └── style.css                 # 全局样式（1000+行）
│   │   ├── index.html
│   │   ├── vite.config.js                # Vite 配置（代理/自动导入）
│   │   ├── package.json
│   │   ├── tsconfig.json / tsconfig.node.json
│   │   └── README.md
│   │
│   ├── src/                              # ★ 后端项目 (Java)
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── config/
│   │   │   │   │   └── AiConfig.java     # 豆包 API 配置 + System Prompt
│   │   │   │   ├── entity/               # ★ 35+ 实体类
│   │   │   │   ├── dao/                  # DAO 接口 (40+)
│   │   │   │   │   └── impl/             # DAO 实现类 (40+)
│   │   │   │   ├── dto/                  # 数据传输对象 (10)
│   │   │   │   ├── service/              # Service 接口 (30+)
│   │   │   │   │   └── impl/             # Service 实现类 (30+)
│   │   │   │   ├── servlet/              # ★ 40+ Servlet 控制器
│   │   │   │   │   ├── BaseServlet.java  # 基础 Servlet (CORS)
│   │   │   │   │   ├── RegisterAndLoginServlet.java  # 登录注册
│   │   │   │   │   ├── BookServlet.java  # 书籍 CRUD
│   │   │   │   │   ├── BookChapterServlet.java
│   │   │   │   │   ├── BookChapterParagraphServlet.java
│   │   │   │   │   ├── BookCommentServlet.java
│   │   │   │   │   ├── BookSquarePostServlet.java
│   │   │   │   │   ├── BorrowInformationServlet.java
│   │   │   │   │   ├── BookBottleServlet.java
│   │   │   │   │   ├── BookReadRoomServlet.java       # ✨ 共读舱
│   │   │   │   │   ├── BookRoomChatServlet.java       # ✨ 共读聊天
│   │   │   │   │   ├── BookRoomSseServlet.java        # ✨ 共读 SSE
│   │   │   │   │   ├── ChatMessageServlet.java        # 聊天消息
│   │   │   │   │   ├── ChatSessionServlet.java        # 聊天会话
│   │   │   │   │   ├── ChatFileServlet.java           # 聊天文件
│   │   │   │   │   ├── FriendServlet.java / FriendRequestServlet.java
│   │   │   │   │   ├── ForumImageServlet.java
│   │   │   │   │   ├── GraphServlet.java              # 知识图谱
│   │   │   │   │   ├── MessageSseServlet.java         # SSE 通知
│   │   │   │   │   ├── NotificationServlet.java
│   │   │   │   │   ├── PetAiChatServlet.java          # ★ AI 聊天
│   │   │   │   │   ├── PetAiChatWithMemoryServlet.java# ★ AI 带记忆聊天
│   │   │   │   │   ├── TtsServlet.java                # TTS 语音
│   │   │   │   │   ├── UserDailyAiPictureServlet.java # AI 每日画报
│   │   │   │   │   ├── UserInformationServlet.java    # 用户信息
│   │   │   │   │   ├── UserReadRecord/Daily/Progress/StatsServlet.java
│   │   │   │   │   ├── UserReportServlet.java         # 周报
│   │   │   │   │   ├── UserComment/LikeServlet.java
│   │   │   │   │   ├── UserCollectionServlet.java
│   │   │   │   │   ├── UserTextColType/TextCollectionServlet.java
│   │   │   │   │   ├── UserBehaviorLogServlet.java
│   │   │   │   │   ├── BookmarkServlet.java
│   │   │   │   │   ├── BookTag/TagRelation/TypeServlet.java
│   │   │   │   │   ├── DeptTypeServlet.java
│   │   │   │   │   ├── DictionaryServlet.java
│   │   │   │   │   ├── SendEmailServlet.java
│   │   │   │   │   └── UserTypeServlet.java
│   │   │   │   ├── util/                 # 工具类
│   │   │   │   │   ├── DBUtil.java       # JDBC 工具（HikariCP）
│   │   │   │   │   ├── DBExceptionHandler.java
│   │   │   │   │   ├── MyBatisUtil.java
│   │   │   │   │   ├── EmailUtil.java    # 邮件发送
│   │   │   │   │   ├── PasswordEncryptUtil.java  # 密码加密
│   │   │   │   │   ├── ParamValidator.java       # 参数校验
│   │   │   │   │   ├── ImageUtil.java    # 图片处理
│   │   │   │   │   ├── EmojiUtils.java
│   │   │   │   │   ├── RowMap.java / RowMapper.java  # JDBC 行映射
│   │   │   │   │   ├── MingZhuNovelCrawler.java     # 小说爬虫
│   │   │   │   │   ├── UserBehaviorLogger.java      # 行为日志
│   │   │   │   │   ├── LocalDateDeserializer.java
│   │   │   │   │   └── LocalDateTimeDeserializer.java
│   │   │   │   ├── vo/
│   │   │   │   │   └── BookReadStatsVO.java
│   │   │   │   └── websocket/
│   │   │   │       └── ChatWebSocket.java           # WebSocket 聊天
│   │   │   ├── resources/
│   │   │   │   ├── db.properties          # 数据库连接配置
│   │   │   │   ├── mybatis-config.xml     # MyBatis 配置
│   │   │   │   └── mapper/               # MyBatis Mapper
│   │   │   └── webapp/WEB-INF/
│   │   │       └── web.xml               # Servlet 6.0 配置
│   │   └── test/java/
│   │
│   ├── web/WEB-INF/web.xml               # 备用的 web.xml
│   ├── pom.xml                           # Maven 构建配置
│   └── demo.iml
│
├── chat_files/                           # 聊天媒体文件 (MP4)
├── forum_images/                         # 论坛图片资源
├── .vscode/ / .idea/                     # IDE 配置
├── CODE_WIKI.md                          # ★ 本文档
├── README.md
└── package-lock.json
```

> **注：** ✨ 标记的是骨架文件中未包含但实际存在的文件

---

## 3. 前端架构 (Vue 3)

### 3.1 核心配置

**入口文件** `main.js`：
```js
// 注册顺序：Element Plus → Pinia → Router → mount
app.use(ElementPlus)
app.use(pinia)          // 含 pinia-plugin-persistedstate
app.use(router)
app.mount('#app')
```

**Vite 配置** `vite.config.js` 要点：
- 开发代理 `/api` → `http://localhost:8082/demo_war_exploded`
- 代理 `/chat/ws` → WebSocket 代理
- 代理 `/forum_images`、`/chat_files` → 静态资源
- 环境变量注入：`VITE_API_BASE_URL`、`VITE_IMAGE_BASE_URL`、`VITE_FILE_BASE_URL`
- Element Plus 自动导入（AutoImport + Components 插件）

**请求封装** `utils/request.js`：
- Axios 实例，带 `withCredentials: true`
- 响应拦截器：统一处理 `ResultDTO` 的 `code === 200` 判断
- 错误提示使用 `ElMessage.error()`

### 3.2 路由系统

`router/index.js` — 采用 **静态同步导入**（非懒加载，解决首屏闪烁）：

| 路由分组 | 路径 | 说明 |
|---------|------|------|
| 独立路由 | `/login`、`/register` | 无 BasicLayout 包裹 |
| 主布局子路由 | `/home`、`/bookshelf`、`/forum`、`/chat`、`/message`、`/profile` 等 | 共享 BasicLayout |
| 阅读相关 | `/book/detail`、`/book/reader` | 书籍详情与阅读器 |
| 用户相关 | `/userInfo`、`/borrowInfo`、`/user/profile` | 用户信息与借阅 |
| 个人中心子页 | `/profile/notes`、`/profile/forum-comments`、`/profile/book-comments` | 笔记与评论 |
| 其他 | `/weeklyReport`、`/read-gallery`、`/bookSquare`、`/s/:isbn` | 周报/历史/书广场/分享 |

**导航守卫** (`beforeEach`)：
1. **静默自动登录**：App 初始化时从 `localStorage` 读取 `autoLoginToken`，调用 `/user/info?token=xxx` 恢复登录态
2. **Token 拦截**：每次路由变化检查 token 有效性
3. **白名单**：`/login`、`/register` 公开访问

### 3.3 状态管理 (Pinia Store)

#### `userStore.js` — 用户认证 & 全局状态

| 状态 | 类型 | 说明 |
|------|------|------|
| `userId` | string | 用户ID |
| `userType` | string | 用户类型（学生/管理员） |
| `userInfo` | object | 完整用户信息 |
| `isLogin` | boolean | 登录状态 |
| `token` | string | 登录令牌 |
| `currentReadingIsbn` | string | 当前阅读的书籍 ISBN |
| `currentReadingChapter` | string | 当前阅读的章节 |
| `graphPanelWidth` | number | 知识图谱面板宽度 |
| `isDark` | boolean | 暗黑模式 |
| `currentWeather` | string | 当前天气（sunny/cloudy/rain/thunder/wind/snow/night） |
| `isExpanded` | boolean | 天气面板展开状态 |
| `glassPreset` | string | 玻璃材质（frosted/liquid） |
| `coReadDrawerWidth` | number | 共读舱抽屉宽度 |

**持久化**：`sessionStorage`（通过 `pinia-plugin-persistedstate`）

#### `readerStore.js` — 阅读器状态

| 模块 | 状态 |
|------|------|
| 章节缓存 | `chapterCache` (Map)、`cachedIsbn` |
| TTS 朗读 | `audioPlayer`、`audioCache`、`isReading`、`readingRate` |
| AI 朗读模式 | `isAiMode`、`textChunksForAi` |
| 漂流瓶 | `bubbleTimer`、`floatingBubbles`、`bottleQueue` |
| 阅读计时 | `readTimer` |

**关键方法**：`cleanupAllResources()` — 一键清理所有定时器、TTS 资源、缓存、漂流瓶

#### `petStore.js` — 宠物 AI 交互

消息类型系统：
| 类型 | 行为 |
|------|------|
| `CHAT` | 普通消息，5秒自动消失 |
| `THINKING` | AI 思考气泡，常驻 |
| `INTERACTION` | 互动答题，常驻 |
| `CONFIRM` | 确认气泡，常驻 |
| `STATIC` | 静态长文，常驻 |
| `TALK` | 可连续对话，常驻 |

**流式打字机**：`createStreamingMessage` → `appendStreamingChunk` → `finishStreamingMessage`

**自动出题系统**：
- 本地题库（4道文学题目）
- AI 生成题目（调用豆包 API）
- 可配置间隔、模式切换

#### `appSettingsStore.js` — 应用设置

- 控件开关矩阵：宠物/共读舱/图谱/引导/天气等
- 壁纸引擎：预设壁纸库 + 自定义壁纸
- **按用户持久化**：`localStorage` key = `app_settings_{userId}`

### 3.4 视图页面

| 页面 | 文件 | 功能 |
|------|------|------|
| **登录/注册** | Login.vue / Register.vue | 表单验证、验证码、邮箱注册 |
| **首页** | Home.vue | 热门书籍、AI 每日画报、塔罗牌推荐、知识图谱 |
| **书架** | Bookshelf.vue | 分组管理、右键归还 |
| **阅读器** | BookReader.vue | 沉浸阅读、章节切换、TTS 朗读、笔记、书签 |
| **书籍详情** | BookDetail.vue | 书籍信息、评论、收藏、借阅 |
| **论坛** | Forum.vue | 帖子列表、发布、评论 |
| **聊天** | Chat.vue | WebSocket 实时聊天、文件传输 |
| **消息** | Message.vue | 通知列表 |
| **个人中心** | Profile.vue | 头像、阅读统计、贡献热力图 |
| **借阅记录** | BorrowInfo.vue | 借阅/归还历史 |
| **书广场** | BookSquare.vue | 用户书评广场 |
| **阅读周报** | WeeklyReport.vue | AI 生成的阅读周报 |
| **阅读历史** | ReadHistoryGallery.vue | 时间线画廊 |
| **笔记** | MyNotesPage.vue | 读书笔记管理 |
| **设置** | Settings.vue | ✨ 应用设置 |

### 3.5 公共组件

| 组件 | 功能 |
|------|------|
| `BasicLayout.vue` | 全局布局外壳（导航栏、侧边栏、内容区） |
| `PetWidget.vue` | 宠物 AI 挂件（消息气泡、互动答题、流式输出） |
| `WeatherAtmosphere.vue` | 天气氛围动画（雨/雪/雷/风/晴/夜） |
| `CoReadingRoom.vue` | ✨ 共读舱（多人实时共读） |
| `GlobalContextMenu.vue` | 全局右键菜单 |
| `HelloWorld.vue` | 示例组件 |

### 3.6 工具函数

| 文件 | 功能 |
|------|------|
| `request.js` | Axios 封装（统一错误处理 + Cookie 携带） |
| `routerHelper.js` | `navigateWithPreload()` — 带数据预加载的页面跳转 |
| `appGuide.js` | 基于 Driver.js 的新手引导（6个页面配置） |

### 3.7 全局样式与动画

`style.css` (1000+行) 特色：
- **流体壁纸引擎**：模糊背景 + 缓慢漫游动画（`fluid-flow` 25s）
- **昼夜切换动画**：`document.startViewTransition` 圆形扩散效果
- **毛玻璃/液态玻璃**：双材质系统（`preset-frosted` / `preset-liquid`）
- **天气氛围**：7种天气 CSS 类名（`weather-sunny` / `weather-rain` 等）
- **鼠标拖尾**：Canvas 光绘层（`z-index: 99998`）
- **性能优化**：页面后台时 `suspend-animations` 冻结所有 CSS 动画
- **移动端适配**：`@media (max-width: 768px)` 全屏+自适应

---

## 4. 后端架构 (Java Servlet)

### 4.1 实体层 (Entity)

**图书相关：**
| 实体 | 说明 | 关键字段 |
|------|------|---------|
| `BookInformation` | 书籍信息 | ISBN, Bookname, Author, Publisher, Type, star, aiSummary |
| `BookChapter` | 章节 | Chapter_id, Isbn, Number, Name, paragraphs (List) |
| `BookChapterParagraph` | 段落 | Paragraph_id, ChapterId, Content, ParagraphOrder |
| `BookComment` | 书籍评论 | CommentId, Isbn, UserId, Content, Star |
| `BookType` | 书籍分类 | TypeId, TypeName |
| `BookTag` / `BookTagRelation` | 标签/关联 | TagId, Isbn |
| `BookSquarePost` | 书评广场 | PostId, UserId, Isbn, Content |
| `BookBottle` | 漂流瓶 | Id, Userid, Isbn, Content, Status, ExpireTime |
| `BookBottlePick` | 捞取记录 | PickId, BottleId, UserId |
| `BookCharacterLore` | 角色设定 | CharacterId, Isbn, Name, Description |
| `BookReadRoom` | ✨ 共读舱 | Id, UserId, Isbn, IsPublic |

**用户相关：**
| 实体 | 说明 | 关键字段 |
|------|------|---------|
| `UserInformation` | 用户信息 | UserId, Name, Password(salt), DeptType, Type, bio, login_token |
| `UserType` | 用户类型 | TypeId, TypeName |
| `DeptType` | 系别 | DeptId, DeptName |
| `UserReadRecord` | 阅读记录 | RecordId, UserId, Isbn, ChapterNum, ReadTime |
| `UserReadProgress` | 阅读进度 | ProgressId, UserId, Isbn, Progress |
| `UserReadStats` | 阅读统计 | StatsId, UserId, TotalReadTime, TotalBooks |
| `UserReadDaily` | 每日阅读 | DailyId, UserId, Date, ReadDuration |
| `UserBehaviorLog` | 行为日志 | LogId, UserId, ActionType, Isbn |
| `UserProfileMemory` | AI 人设记忆 | MemoryId, UserId, MemoryContent |
| `UserDailyAiPicture` | AI 每日画报 | PictureId, UserId, Date, ImageUrl, Content |
| `UserReport` | 周报 | ReportId, UserId, WeekStart, Content |
| `UserNotification` | 通知 | NotificationId, UserId, Content, IsRead |

**社交相关：**
| 实体 | 说明 |
|------|------|
| `ChatMessage` | 私聊消息（支持文本/图片/语音，撤回/引用） |
| `ChatSession` | 聊天会话 |
| `ChatFile` | 聊天文件 |
| `Friend` | 好友关系 |
| `FriendRequest` | 好友申请 |
| `UserComment` / `UserCommentLike` | 论坛评论/点赞 |
| `ParagraphComment` | 段落评论（阅读器内） |
| `ForumImage` | 论坛图片 |
| `UserCollection` | 收藏 |
| `UserTextColType` / `UserTextCollection` | 文本收藏分类/内容 |

**AI 相关：**
| 实体 | 说明 |
|------|------|
| `UserAiChat` | AI 聊天记录 |
| `AiInteractionHistory` | AI 交互历史 |
| `AiOperationLog` | AI 操作日志 |
| `GraphNode` / `GraphEdge` | 知识图谱节点/连线 |

### 4.2 数据访问层 (DAO)

**架构模式**：接口 + 实现类

- **`DBUtil`**：基于 HikariCP 连接池的 JDBC 工具类
- **`MyBatisUtil`**：MyBatis 会话工厂（用于 BookMapper）
- **`RowMap` / `RowMapper`**：JDBC 结果集映射工具
- 大多数 DAO 使用 JDBC 手写 SQL，`BookMapper.xml` 使用 MyBatis

### 4.3 服务层 (Service)

同样采用 **接口 + 实现类** 模式，封装业务逻辑。关键服务：

| 服务 | 功能 |
|------|------|
| `BookService` | 书籍 CRUD、搜索、热门推荐 |
| `BookChapterService` | 章节管理（分段加载） |
| `BorrowInformationService` | 借阅/归还/罚款 |
| `ChatMessageService` | 聊天消息（发送/撤回/历史） |
| `ChatSessionService` | 会话管理 |
| `FriendService` / `FriendRequestService` | 好友/申请 |
| `GraphService` | 知识图谱构建和查询 |
| `UserStatsService` | 阅读统计数据 |
| `UserReportService` | AI 周报生成 |
| `UserDailyAiPictureService` | AI 画报管理 |
| `AiChatService` | AI 对话（豆包 API 调用） |
| `UserReadProgressService` | 阅读进度同步 |
| `UserBookshelfGroupService` | ✨ 书架分组 |

### 4.4 控制层 (Servlet)

**`BaseServlet`** — 所有 Servlet 的基类：
- 统一 CORS 处理（`setCorsHeader`）
- 支持 `OPTIONS` 预检请求

**路由模式**：`@WebServlet("/api/xxx/*")` 注解 + `pathInfo` 分发

**核心 Servlet 列表**（40+）：

| 分组 | Servlet | 端点 |
|------|---------|------|
| 认证 | `RegisterAndLoginServlet` | `/api/user/register`, `/api/user/login` |
| 用户 | `UserInformationServlet` | `/api/user/info` |
| 书籍 | `BookServlet` | `/api/book/*` |
| 章节 | `BookChapterServlet` | `/api/book/chapter/*` |
| 段落 | `BookChapterParagraphServlet` | `/api/book/paragraph/*` |
| 借阅 | `BorrowInformationServlet` | `/api/borrow/*` |
| 评论 | `BookCommentServlet` | `/api/book/comment/*` |
| 聊天 | `ChatMessageServlet` | `/api/chat/message/*` |
| 聊天 | `ChatSessionServlet` | `/api/chat/session/*` |
| 好友 | `FriendServlet` | `/api/friend/*` |
| 图谱 | `GraphServlet` | `/api/graph/*` |
| AI | `PetAiChatServlet` | `/api/pet/ai/*` |
| AI | `PetAiChatWithMemoryServlet` | `/api/pet/ai/memory/*` |
| TTS | `TtsServlet` | `/api/tts/*` |
| 画报 | `UserDailyAiPictureServlet` | `/api/pet/daily/*` |
| 周报 | `UserReportServlet` | `/api/user/report/*` |

### 4.5 数据传输对象 (DTO)

| DTO | 用途 |
|-----|------|
| `ResultDTO<T>` | 通用前后端交互格式：`{ code, msg, data }` |
| `LoginDTO` | 登录请求参数 |
| `RegisterDTO` | 注册请求参数 |
| `UserInfoDTO` | 用户信息传输 |
| `PageResultDTO` | 分页结果 |
| `GraphDataDTO` | 图谱数据 |
| `ReadStatsDTO` | 阅读统计数据 |
| `UserStatsDTO` | 用户统计 |
| `UserStatsForAIDTO` | AI 用用户统计 |
| `UserReportDTO` | 周报数据 |

### 4.6 工具类 (Util)

| 工具 | 功能 |
|------|------|
| `DBUtil` | HikariCP 连接池管理 + JDBC 操作模板 |
| `MyBatisUtil` | MyBatis SqlSession 工厂 |
| `PasswordEncryptUtil` | MD5 + Salt 密码加密 |
| `EmailUtil` | Jakarta Mail 邮箱发送（验证码） |
| `ParamValidator` | 参数合法性校验 |
| `ImageUtil` | 图片压缩/处理 |
| `EmojiUtils` | Emoji 处理 |
| `MingZhuNovelCrawler` | 明珠小说爬虫（Jsoup） |
| `UserBehaviorLogger` | 用户行为异步日志 |
| `RowMap` / `RowMapper` | JDBC 行 → 对象映射 |
| `DBExceptionHandler` | 数据库异常统一处理 |
| `LocalDateDeserializer` / `LocalDateTimeDeserializer` | FastJSON 日期反序列化 |

### 4.7 WebSocket 实时通信

**`ChatWebSocket.java`**
- 端点：`/chat/ws/{userId}`
- 使用 `ConcurrentHashMap` 维护在线会话
- 支持点对点消息转发
- `sendToUser()` 静态方法用于主动推送
- 内置 CORS 配置器

**前端代理**：Vite 配置 `ws: true` 代理 `/chat/ws`

### 4.8 监听器 (Listener)

| 监听器 | 功能 |
|--------|------|
| `NightlyAiTaskListener` | 夜间 AI 定时任务（生成每日画报/清理数据） |
| `ReadStatsJobListener` | 阅读统计任务 |

---

## 5. AI 功能模块

### 5.1 豆包 API 集成

**`AiConfig.java`** — 集中配置：

| 配置项 | 值 |
|--------|-----|
| API Key | `ark-571673bd-...-e1bf9` |
| Chat Endpoint | `ep-20260428120801-8fd26` |
| Image Endpoint | `ep-20260517234841-fnwb6` |
| API URL | `https://ark.cn-beijing.volces.com/api/v3/chat/completions` |
| Image URL | `https://ark.cn-beijing.volces.com/api/v3/images/generations` |

**双重 System Prompt 架构**：
1. **`BASE_SYSTEM_PROMPT`**：图书馆 AI 助手人设（始终加载）
2. **`BOOK_SERVICE_PROMPT`**：图书服务工具模式（仅 book 场景加载）
   - 15 个工具函数：queryBook / borrowBook / returnBook / collectBook / ...
   - 严格的调用规则、链式调用、防重复机制

### 5.2 AI 聊天场景

通过 `PetAiChatServlet` 的 `actionType` 参数分发：

| actionType | 场景 | 说明 |
|-----------|------|------|
| `answer` | 普通问答 | BASE_PROMPT 通用对话 |
| `ai_steward` | AI 管家 | + 书籍角色设定（characterLore） |
| `generate_quiz` | AI 出题 | + 阅读内容相关题目生成 |
| `chat` | 直接聊天 | 宠物对话模式 |
| `summary` | 书籍总结 | 生成 AI 书籍摘要 |
| `translate` | 翻译 | 文本翻译 |

### 5.3 AI 出题系统

**前端** (`petStore.js`)：
- `generateQuestionFromContent(content, type)` → 调用豆包 API
- 支持选择题/判断题混合生成
- 解析失败降级到本地题库
- 自动出题模式（可配置间隔和模式）

**后端** (`PetAiChatServlet` → `generate_quiz`)：
- 专属 System Prompt 压制 AI 废话
- 返回严格 JSON 格式题目

### 5.4 AI 每日画报

- **后端**：`UserDailyAiPictureServlet` + `UserDailyAiPictureService`
- 调用豆包文生图 API (`Doubao-Seedream-4.5`)
- 尺寸：2K
- **前端**：首页展示，支持历史回溯

### 5.5 TTS 语音朗读

- **后端**：`TtsServlet`
- **前端**：`readerStore` 管理音频资源
- `AbortController` 控制请求取消
- `Blob URL` 管理音频缓存
- 支持倍速播放（`readingRate`）

---

## 6. 数据库设计

- **数据库名**：`library_manager`
- **连接**：`jdbc:mysql://localhost:3306/library_manager`
- **用户**：`library_user` / `NewPass123!`

### 核心表清单

| 表名（推测） | 对应实体 | 说明 |
|-------------|---------|------|
| `book_information` | BookInformation | 书籍主表 |
| `book_chapter` | BookChapter | 章节表 |
| `book_chapter_paragraph` | BookChapterParagraph | 段落表（分段落存储） |
| `book_type` | BookType | 书籍分类 |
| `book_tag` / `book_tag_relation` | BookTag / BookTagRelation | 标签系统 |
| `book_comment` | BookComment | 书籍评论 |
| `borrow_information` | BorrowInformation | 借阅记录 |
| `user_information` | UserInformation | 用户主表 |
| `user_type` / `dept_type` | UserType / DeptType | 用户类型/系别 |
| `user_read_record` / `_progress` / `_stats` / `_daily` | UserRead* | 阅读四件套 |
| `chat_message` / `chat_session` / `chat_file` | Chat* | 聊天系统 |
| `friend` / `friend_request` | Friend / FriendRequest | 好友系统 |
| `user_comment` / `user_comment_like` | UserComment / UserCommentLike | 论坛评论 |
| `forum_image` | ForumImage | 论坛图片 |
| `book_square_post` | BookSquarePost | 书评广场 |
| `book_bottle` / `book_bottle_pick` | BookBottle / BookBottlePick | 漂流瓶 |
| `book_read_room` | BookReadRoom | ✨ 共读舱 |
| `graph_node` / `graph_edge` | GraphNode / GraphEdge | 知识图谱 |
| `user_notification` | UserNotification | 通知 |
| `user_behavior_log` | UserBehaviorLog | 行为日志 |
| `user_daily_ai_picture` | UserDailyAiPicture | AI 每日画报 |
| `user_report` | UserReport | 周报 |
| `user_profile_memory` | UserProfileMemory | AI 人设记忆 |
| `book_character_lore` | BookCharacterLore | 角色设定 |
| `ai_interaction_history` / `ai_operation_log` | AiInteraction* / AiOperationLog | AI 日志 |
| `user_ai_chat` | UserAiChat | AI 聊天记录 |
| `bookmark` | Bookmark | 书签 |
| `user_collection` | UserCollection | 收藏 |
| `user_text_col_type` / `user_text_collection` | UserTextCol* | 文本收藏 |
| `user_bookshelf_group` | UserBookshelfGroup | ✨ 书架分组 |

---

## 7. 配置与环境

### 开发环境

| 配置项 | 值 |
|--------|-----|
| Java | 21 |
| Tomcat | 11 (Servlet 6.0) |
| Node | 由 frontend-maven-plugin 管理 |
| MySQL | 8.0 |

### 前端环境变量

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | API 请求基址（代理为 `/api`） |
| `VITE_IMAGE_BASE_URL` | 图片基址 |
| `VITE_FILE_BASE_URL` | 文件基址 |

### 后端 Maven 依赖

核心依赖（除 Servlet/WebSocket 外）：
- **Spring Boot Web** 3.2.2（仅用于 Jackson 和工具类）
- **HikariCP** 5.0.1（连接池）
- **MyBatis** 3.5.13
- **FastJSON** 2.0.25
- **Jsoup** 1.17.2（爬虫）
- **Apache Tika** 2.9.1（文件内容提取）
- **Volcengine Java SDK** 2.0.8（豆包 API）
- **Jakarta Mail** / Eclipse Angus（邮件发送）
- **Lombok** 1.18.30
- **SLF4J + Logback**

### 构建流程

```
Maven 构建：
1. compile 阶段 → frontend-maven-plugin 构建前端
2. resources-plugin 复制 frontend/dist → src/main/webapp/frontend
3. war-plugin 打包为 tsglxt.war

最终 WAR 结构：
demo.war
├── frontend/          # 编译后的前端资源
├── WEB-INF/
│   ├── classes/       # Java 编译 + 资源配置
│   └── web.xml
```

---

## 8. 功能模块全景图

```
┌──────────────────────────────────────────────────────────────────────┐
│                         🔐 认证系统                                   │
│  登录/注册 → Token 自动登录 → 导航守卫 → 邮箱验证码                    │
├──────────────────────────────────────────────────────────────────────┤
│                         📖 阅读管理                                   │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐    │
│  │ 书架分组  │ │ 沉浸阅读器│ │ 书签系统 │ │ 阅读统计  │ │ 借阅管理  │    │
│  │ 右键菜单  │ │ 分段阅读  │ │ 笔记高亮 │ │ 阅读日历  │ │ 借/还书   │    │
│  │ 拖拽排序  │ │ 章节导航  │ │ 段落评论 │ │ 热力图    │ │ 罚款计算  │    │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘ └──────────┘    │
├──────────────────────────────────────────────────────────────────────┤
│                         💬 社交互动                                   │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐    │
│  │ 实时聊天  │ │ 好友系统  │ │ 论坛社区 │ │ 漂流瓶   │ │ 共读舱   │    │
│  │ WebSocket│ │ 添加/删除│ │ 发帖评论 │ │ 扔/捞瓶子│ │ 实时共读  │    │
│  │ 文件传输  │ │ 好友申请  │ │ 点赞收藏 │ │ 匿名社交  │ │ SSE 同步 │    │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘ └──────────┘    │
├──────────────────────────────────────────────────────────────────────┤
│                         🤖 AI 增强体验                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐   │
│  │ AI 管家   │ │ AI 出题   │ │ 每日画报  │ │ TTS 朗读│ │ 阅读周报  │   │
│  │ 图书服务  │ │ 自动出题  │ │ 文生图    │ │ 倍速播放 │ │ AI 生成   │   │
│  │ 15种工具  │ │ 互动答题  │ │ 历史回溯  │ │ 进度同步 │ │ 阅读分析  │   │
│  └──────────┘ └──────────┘ └──────────┘ └─────────┘ └──────────┘   │
├──────────────────────────────────────────────────────────────────────┤
│                         🎨 沉浸体验                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐   │
│  │ 天气氛围  │ │ 暗黑模式  │ │ 壁纸引擎  │ │ 宠物挂件│ │ 知识图谱  │   │
│  │ 7种天气   │ │ 圆形扩散  │ │ 预设壁纸  │ │ AI 气泡 │ │ 节点连线  │   │
│  │ 动态粒子  │ │ 动画过渡  │ │ 模糊漫游  │ │ 流式输出│ │ 可视化   │   │
│  └──────────┘ └──────────┘ └──────────┘ └─────────┘ └──────────┘   │
└──────────────────────────────────────────────────────────────────────┘
```

---

> **文档版本**：v1.0
> **最后更新**：2025-06-26
> **生成方式**：基于实际代码的完整探索
