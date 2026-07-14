# 📚 沉浸式图书阅读与交流书屋

> Immersive Book Reading & Communication Platform

面向图书爱好者的全栈沉浸式阅读平台，融合 **个人阅读管理**、**社交互动** 和 **AI 增强体验** 三大核心模块。

---

## 🛠️ 技术栈

| 层级 | 技术选型 |
|------|----------|
| **前端** | Vue 3 (Composition API) + Vite + Element Plus + Pinia + Vue Router 4 |
| **实时通信** | WebSocket (实时私聊 & 在线同步) + SSE (红点通知推送) |
| **知识图谱** | AntV X6 3.x + 8大官方插件 (Minimap / Snapline / History 等) |
| **后端** | Java 21 + Jakarta Servlet 6.0 + Tomcat 11 |
| **数据访问** | JDBC (HikariCP) + MyBatis |
| **缓存 / 内存库** | Redis (Jedis) — String (验证码/限流/图书缓存/TTS路径) + List (SSE历史/AI记忆) + Hash (未读计数红点) + Set (随机匹配池/全局在线集合) + Lua 脚本分布式锁 |
| **数据库** | MySQL 8.0 (`library_manager`, 42 张表, InnoDB) |
| **AI / TTS** | 火山引擎豆包 (Doubao) + Edge-TTS (微软神经网络语音, 15 种音色) |
| **构建** | Maven (后端) + Vite (前端) |

---

## ✨ 核心功能

### 📖 阅读管理
- **书架系统** — 分组管理、拖拽排序、右键菜单操作
- **沉浸式阅读器** — 分段阅读、翻页动画、字体/背景/护眼模式自定义
- **书签系统** — 添加/删除/定位书签，同步到知识图谱
- **阅读统计** — 每日时长、累计统计、GitHub 风格贡献热力图
- **AI 阅读周报** — 豆包 AI 分析阅读习惯，生成个性化周报
- **知识图谱** — X6 驱动的思维导图，支持拖拽添加节点、连线、自动布局、导出高清 PNG

### 💬 社交互动
- **实时私聊** — WebSocket 文本/图片/语音消息，支持撤回和引用回复
- **好友系统** — 搜索用户、添加/删除好友、处理申请
- **论坛社区** — 发帖、评论、点赞、图片上传，支持嵌套回复
- **漂流瓶** — 匿名社交，扔瓶子、捞瓶子、回复
- **共读舱** — 创建/加入公开或私密共读房间，实时同步阅读进度
- **SSE 通知** — 好友申请、新消息、评论回复实时推送

### 🤖 AI 增强体验
- **AI 宠物管家** — 流式对话、可爱语气，支持普通聊天/管家/辩论/角色分析多种模式
- **AI 图书服务** — 通过对话方式查询/借阅/收藏/归还书籍，15 个工具函数
- **有声书系统** — AI 自动识别角色/情感 → 分段切对话 → Edge-TTS 合成 → 逐段播放（15 种微软神经网络音色）
- **AI 每日画报** — 根据用户画像每日生成文生图 (Doubao-Seedream-4.5)
- **AI 出题互动** — 根据阅读内容生成选择题/判断题，支持自动出题模式
- **深度情感朗读** — 逐句分析情感和角色，分配不同音色朗读，优先复用有声书缓存

### 🎨 沉浸设计
- **动态壁纸引擎** — 模糊漫游 + 预设壁纸库
- **天气氛围系统** — 7 种天气 (晴/云/雨/雷/风/雪/夜)，动态粒子效果
- **暗黑模式** — View Transition API 圆形扩散动画
- **毛玻璃 / 液态玻璃** — 双材质 UI 一键切换
- **鼠标拖尾光绘** — Canvas 光绘层

### 🛡️ 管理员后台
- **内容审核** — 书籍评论/段落评论/论坛评论/漂流瓶审核（通过/拒绝）
- **用户管理** — 用户列表、搜索、冻结/解冻
- **书籍管理** — 书籍列表、搜索

---

## 📐 系统架构

```
┌────────────────────────────────────────────┐
│          浏览器 (Vue 3 SPA)                  │
│  Element Plus + Pinia + Vue Router + X6     │
│  WebSocket | SSE | Axios                   │
└──────────────────┬─────────────────────────┘
                   │  Vite Proxy /api
┌──────────────────┴─────────────────────────┐
│         Tomcat 11 (Jakarta Servlet 6.0)     │
│  ┌──────────┬──────────┬─────────────────┐ │
│  │ Servlet  │ Service  │ DAO (JDBC)       │ │
│  │ 40+ 控制器│ 30+ 服务  │ 45+ 数据访问     │ │
│  └──────────┴──────────┴─────────────────┘ │
│         HikariCP | MyBatis | Jedis         │
└──────────┬───────────────────────┬─────────┘
           │                       │
┌──────────┴─────────┐   ┌─────────┴─────────┐
│      MySQL 8.0     │   │      Redis 6+     │
│  library_manager   │   │  多维缓存与并发锁  │
│  (42 张表, InnoDB)  │   │  (String/List/Set)│
└──────────┬─────────┘   └───────────────────┘
           │
┌──────────┴──────────┬──────────────┐
│    火山引擎 (Doubao) │   Edge-TTS    │
│    Chat + Image API  │   15 种音色   │
└─────────────────────┴──────────────┘
```

---

## 🚀 快速开始

### 环境要求
- **JDK** 21+
- **Maven** 3.8+
- **MySQL** 8.0+
- **Node.js** 18+
- **Redis** 6+ (可选，缓存加速)
- **Python** 3.10+ (用于 Edge-TTS)
- **Tomcat** 11

### 后端启动
```bash
# 1. 创建数据库并导入表结构
mysql -u root -p < docs/数据库设计说明.md  -- 参考其中的 SQL 建表语句

# 2. 配置敏感信息（不要提交到 Git）
cp demo/src/main/resources/config.properties.example demo/src/main/resources/config.properties
# 编辑 config.properties，填入数据库密码、API Key 等

# 3. 编译打包
cd demo
mvn clean package -DskipTests

# 4. 部署到 Tomcat
cp target/tsglxt.war $TOMCAT_HOME/webapps/
```

### 前端开发
```bash
cd demo/frontend
npm install
npm run dev
```

### 可选：Redis 缓存
```bash
# Windows
memurai-cli
# 或 Docker
docker run -d -p 6379:6379 redis

# 在 config.properties 中添加
redis.host=127.0.0.1
redis.port=6379
redis.password=123456
```

### ⚡ Redis 缓存策略与 Key 规范

为了提升系统的并发吞吐量并减轻 MySQL 的访问压力，项目在各核心层深度集成了 Redis 缓存。以下为目前系统已上线的缓存键（Key）规范：

| 业务场景 | Redis 数据结构 | Redis 键（Key）规范 | 生效机制与过期时间 (TTL) |
|---|---|---|---|
| **邮箱验证码** | String | `EMAIL_CODE_<email>` | 5 分钟 (300s) 过期，注册成功主动删除 |
| **登录防暴破限流** | String (INCR) | `login:fail:<username>` / `login:lock:<username>` | 锁定状态 10 分钟自动解锁 |
| **图书详情旁路缓存** | String (JSON) | `book:isbn:<isbn>` | 缓存旁路 (Cache-Aside) 模式，增删改自动失效 |
| **借阅超卖并发锁** | String | `lock:borrow:book:<isbn>` | 分布式排它锁，防止超借，用完自动释放 |
| **共读舱聊天历史** | List | `chat:room:history:<roomId>` | 缓存最近 30 条消息，SSE 房间销毁时同步清理 |
| **未读消息计数红点** | Hash | `user:unread:<userId>` | `notifications` 与 `friendRequests` 字段，增量自增/单点已读清空 |
| **漂流瓶随机匹配池** | Set | `bottles:pool:<isbn>` | 预热加载非本人/可捞取的漂流瓶 ID 集合，随机匹配 |
| **AI 宠物聊天上下文** | List | `ai:chat:memory:<userId>` | 缓存最近 5 轮 (10 条) 对话记忆，30分钟过期 |
| **全局在线活跃状态** | Set | `online:users` | WebSocket 建立/断开时动态 `SADD`/`SREM` 追踪 |
| **TTS 语音生成路径** | String | `tts:cache:<MD5(text:voice)>` | 缓存生成的音频文件路径，避免 Edge-TTS 重复生成 |

---

## 📂 项目结构

```
WebTsglxt/
├── demo/frontend/src/               # Vue 3 前端源码
│   ├── components/                  # 公共组件 (17+)
│   │   ├── BasicLayout.vue         # 全局布局
│   │   ├── KnowledgeGraph.vue      # X6 知识图谱
│   │   ├── AudiobookPlayer.vue     # 有声书播放器
│   │   └── PetWidget.vue           # AI 宠物挂件
│   ├── views/                       # 页面视图 (25+)
│   │   ├── BookReader.vue          # 沉浸阅读器
│   │   ├── AudiobookLibrary.vue    # 有声书书架
│   │   └── AdminPanel.vue          # 管理员后台
│   ├── stores/                      # Pinia 状态 (7)
│   └── router/                      # 路由配置
│
├── demo/src/main/java/com/example/  # Java 后端源码
│   ├── entity/                      # 实体层 (35+)
│   ├── dao/impl/                    # 数据访问层 (45+)
│   ├── service/impl/                # 服务层 (30+)
│   ├── servlet/                     # 控制层 (40+)
│   ├── util/                        # 工具类 (15+)
│   │   ├── RedisUtil.java          # Redis 连接池
│   │   ├── DBUtil.java             # JDBC 工具
│   │   └── LoginRateLimiter.java   # 登录频率限制
│   ├── config/AiConfig.java        # AI 配置与 System Prompt
│   └── websocket/                   # WebSocket 实时通信
│
├── docs/                            # 项目文档
│   ├── 需求规格说明.md
│   ├── 原型设计说明.md
│   ├── 数据库设计说明.md
│   ├── AI提示词与工作流.md
│   └── *.sql                        # 数据库迁移脚本
│
└── CODE_WIKI.md                     # 代码知识库
```

---

## 📖 文档索引

| 文档 | 说明 |
|------|------|
| [CODE_WIKI.md](./CODE_WIKI.md) | 代码知识库 — 完整技术架构与文件索引 |
| [需求规格说明](./docs/需求规格说明.md) | 7 大功能模块 · 40+ 需求条目 |
| [原型设计说明](./docs/原型设计说明.md) | 页面线框图 · 组件树 · 交互规范 |
| [数据库设计说明](./docs/数据库设计说明.md) | 42 张表完整字段设计 · ER 图 · 索引 |
| [AI提示词与工作流](./docs/AI提示词与工作流.md) | 9 种 System Prompt · 6 大工作流 · API 示例 |

---

## 📄 License

MIT © 2025
