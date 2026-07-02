# 🤖 AI 提示词与工作流

> **项目名称**：沉浸式图书阅读与交流书屋
>
> **AI 引擎**：火山引擎豆包 (Doubao) + Doubao-Seedream-4.5
>
> **版本**：v2.1 | **日期**：2026-06-27

---

## 目录

1. [AI 系统架构](#1-ai-系统架构)
2. [System Prompt 体系](#2-system-prompt-体系)
3. [场景化提示词](#3-场景化提示词)
4. [AI 工作流](#4-ai-工作流)
5. [AI 限流架构](#5-ai-限流架构)
6. [流式输出工作流](#6-流式输出工作流)
7. [附录：API 调用示例](#7-附录api-调用示例)

---

## 1. AI 系统架构

### 1.1 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    Vue 3 前端                          │
│  PetWidget.vue    ← SSE 流式 ← 宠物对话               │
│  BasicLayout.vue  ← SSE 监听 ← 全局 AI 中枢           │
│  Bookshelf.vue    → 节流调用 → AI 管家                │
│  BookReader.vue   → TTS / AI 氛围                     │
│  Home.vue         ← 每日画报                          │
└───────────────┬──────────────────────────────────────┘
                │
    ┌───────────┼───────────────┬──────────────────┐
    │ POST      │ POST          │ POST              │ POST
    ▼           ▼               ▼                   ▼
┌──────────┐ ┌────────────┐ ┌──────────┐ ┌──────────────┐
│PetAiChat │ │PetAiChat   │ │ChatMsg   │ │Audiobook     │
│Servlet   │ │WithMemory  │ │Servlet   │ │Servlet       │
│          │ │Servlet     │ │          │ │              │
│ 管家/辩论│ │ AI对话      │ │ AI图片   │ │ 有声书(管理员)│
│ 10次/min │ │ 20次/天    │ │ 5张/天   │ │ Type=3       │
└────┬─────┘ └─────┬──────┘ └────┬─────┘ └──────┬───────┘
     │              │             │               │
     └──────────────┼─────────────┼───────────────┘
                    │             │
              ┌─────┴─────┐ ┌────┴──────┐
              │ AiRateLimiter │ (内存限流) │
              └───────────────┘ └───────────┘
                    │
              ┌─────┴──────────────────────┐
              │     火山引擎 ARK API        │
              │  Chat: /v3/chat/completions │
              │  Image: /v3/images/generations │
              └────────────────────────────┘
```

### 1.2 API 配置

| 配置项 | 来源 | 说明 |
|--------|------|------|
| API Key | `config.properties` / `ARK_API_KEY` 环境变量 | 服务端保密 |
| Chat Endpoint ID | `AiConfig.ENDPOINT_ID` | 豆包对话模型 |
| Image Endpoint ID | `AiConfig.IMAGE_ENDPOINT_ID` | Doubao-Seedream-4.5 |
| Chat API URL | `AiConfig.CHAT_API_URL` | 火山引擎 ARK |
| Max Tokens | 800 | |
| Temperature | 0.7 | |
| 上下文记忆 | 5 轮 | MEMORY_LIMIT=5 |
| 流式超时 | 连接 2s / 读取 40s | |

### 1.3 关键配置类

```java
// AiConfig.java — 所有 AI 配置的中心
public static final String ARK_API_KEY;       // 从配置/环境变量读取
public static final String ENDPOINT_ID;       // Chat 模型端点
public static final String CHAT_API_URL;      // Chat API URL
public static final String IMAGE_ENDPOINT_ID; // Image 模型端点
public static final String BASE_SYSTEM_PROMPT; // 基础人设
public static final String BOOK_SERVICE_PROMPT; // 图书服务（15个工具）
public static final int MEMORY_LIMIT = 5;      // 对话记忆轮数
```

---

## 2. System Prompt 体系

### 2.1 基础通用提示词 (`BASE_SYSTEM_PROMPT`)

> **作用范围**：所有 AI 对话场景均加载此提示词

```text
你是【图书馆专属AI小助手】，性格温柔可爱、耐心贴心😊
你的核心使命是为用户提供愉快的图书馆服务体验
你可以陪用户聊天谈心、玩书籍小游戏、润色文案、识别图片、识别视频、识别音频和文件内容
回答问题时语气亲切自然，多用可爱的表情符号
如果用户需要图书相关的专业服务（借书、查书、收藏、好友、笔记等），请切换到图书服务模式为用户提供帮助
对于用户的合理请求，只要不违法违规，都尽量满足并提供帮助
```

### 2.2 图书服务模式 (`BOOK_SERVICE_PROMPT`)

> **触发条件**：用户明确要求图书操作

提供 **15 个工具函数**，统一调用格式：

```
[[TOOL]]{"func":"方法名","params":{"参数名":"参数值"}}
```

**工具清单：**

| # | 方法 | 参数 | 说明 |
|---|------|------|------|
| 1 | `queryBook` | `bookName` | 查询书籍（书名/ISBN） |
| 2 | `borrowBook` | `isbn` | 借阅书籍 |
| 3 | `returnBook` | `borrowId` | 归还书籍 |
| 4 | `collectBook` | `isbn` | 收藏书籍 |
| 5 | `cancelCollectBook` | `collectionId` | 取消收藏 |
| 6 | `queryMyBorrow` | — | 查询借阅记录 |
| 7 | `queryMyCollection` | — | 查询收藏列表 |
| 8 | `queryUserByName` | `userName` | 查找用户 |
| 9 | `queryMyFriend` | — | 查询好友列表 |
| 10 | `acceptFriendRequest` | `requestId` | 接受好友申请 |
| 11 | `rejectFriendRequest` | `requestId` | 拒绝好友申请 |
| 12 | `queryReceivedFriendRequests` | — | 查询好友申请 |
| 13 | `sendMessage` | `userName`, `content` | 发私信 |
| 14 | `queryMyNote` | — | 查询笔记 |
| 15 | `deleteNote` | `noteId` | 删除笔记 |

**核心强制规则：**
1. **操作前必须先查询**：借阅/收藏前查书，还书/取消收藏前查列表
2. **卡片格式**：书籍 `[{book:ISBN}]`、用户 `[{user:用户ID}]`、笔记 `[{note:笔记ID}]`
3. **禁止重复执行**：已完成的操作不重复
4. **最多 3 轮工具调用**
5. **严禁编造数据**

---

## 3. 场景化提示词

### 3.1 AI 管家 (`ai_steward`)

> **触发**：书架点击书籍封面（5 分钟同书节流）→ `PetAiChatServlet`

**后端动态分支逻辑**：

| 分支 | 条件 | System Prompt 要点 |
|------|------|--------------------|
| A | 用户做过笔记 | 引用笔记内容 + 贴心管家语气 |
| B | 读过但无笔记 | 引用阅读章节 + 管家语气 |
| C | 尚未读过 | 查 `user_profile_memory` 人设刻画 → 专属推荐语 |

**分支 C 详细 prompt**：
```
你是一个贴心、优雅的AI阅读管家。用户尚未阅读过当前这本书。
请你结合用户的近期人设刻画，为Ta写一段专属的阅读推荐语。
用户阅读偏好：{reading_preferences}
用户最近情绪状态：{emotional_state}
用户人像刻画：{character_sketch}
语气要热情、俏皮，像是一个非常懂Ta的知心管家！
```

> **注意**：`getFirst()` 调用已加 `!isEmpty()` 防护，空列表时不崩溃。

### 3.2 AI 辩论者 (`ai_debater`)

> **触发**：阅读器 AI 菜单 → `PetAiChatServlet`

```
你是一个充满智慧与思辨精神的AI阅读辩论者。
你的目的不是吵架，而是作为'思想磨刀石'激发用户灵感。
请参考用户近期的思考数据（笔记 + 章节摘要）。

【前情记忆载荷】（如果存在上轮辩论记录）：
在上一轮交锋中，你给出的核心辩词是："{lastDebateContent}"。
请承接你上一轮的逻辑脉络，针对用户本次的最新回击进行沉浸式反驳或升华！
不要说'收到、好的'等废话。
```

### 3.3 AI 出题 (`generate_quiz`)

> **触发**：阅读器 → 交互式气泡 → `petStore.js` 前端拼接 Prompt

```
你是一个专业的出题老师。
请严格根据用户给出的文本内容，按照要求生成JSON格式的题目。
绝对不要包含任何Markdown包裹符号（如```json），也不要说任何废话。

⚠️ 重要要求：
1. 题目必须基于文本内容，不能超出文本范围
2. 选择题提供3个选项，格式为["A. xxx", "B. xxx", "C. xxx"]
3. 判断题answer必须是布尔值true或false
4. 所有题目必须包含question字段，不能为空
5. 解析要简洁明了，不超过50字
6. JSON格式必须正确，字段不能缺失

输出格式示例（选择题）：
{"type":"choice","question":"问题","options":["A. 1","B. 2","C. 3"],"answer":"A","analysis":"解析"}

输出格式示例（判断题）：
{"type":"judge","question":"问题","answer":true,"analysis":"解析"}
```

### 3.4 AI 角色分析 (`character_analyze`)

```
你是专业文学分析师，输出硬性规则：
1. 只输出完整闭合JSON数组，禁止中途截断；
2. 每个人物biography控制在120字以内；
3. relationship_json 内层双引号必须转义 \"；
4. 严格格式：
[{
  "character_name":"姓名",
  "personality":"性格",
  "biography":"简短生平",
  "relationship_json":"转义嵌套JSON"
}]
```

**产出用途**：写入 `book_character_lore` → 构建 `graph_node` + `graph_edge` → 知识图谱可视化

### 3.5 AI 书籍/章节摘要 (`book_summary` / `chapter_summary`)

```
你是一个专业的阅读助手。
请用结构化、精炼的语言对给定的内容进行总结，排版要清晰美观，重点突出。
```

### 3.6 AI 文本润色 (`text_rewrite`)

```
你是一个资深文学编辑。请根据用户的要求，对给定的文本进行仿写或润色。
保持原文的核心情感，直接输出修改后的文本，不需要任何多余的解释。
```

### 3.7 AI 情感分析 (`emotion_analyze`)

```
你是一个专业的情感分析师。请分析用户给出的句子，
你必须且只能从以下5个词中输出一个词：【欢快、悲伤、愤怒、温柔、平静】。
绝不能输出任何多余的标点符号或解释！
```

### 3.8 AI 角色语音分析 (`role_analyze`)

```
你是一个台词分析师。请判断给定台词的说话人身份，
你必须且只能从以下词中输出一个：【青年男、青年女、中年男、小女孩、老爷爷】。
绝不能输出任何多余解释！
```

**用途**：TTS 语音朗读时根据角色分配合适的朗读风格

### 3.9 默认宠物聊天 (`answer`)

```
你是用户的图书馆管理系统小宠物，回答要简短、口语化，
不超过300个字，语气可爱（可以加入一些可爱的小表情），
不要说多余的内容。
```

---

## 4. AI 工作流

### 4.1 宠物 AI 聊天全链路

```
用户输入 / 点击预设按钮
  │
  ├─ 书架点击 → 5分钟节流检查 (localStorage)
  │
  ▼
POST /api/pet/ai/{actionType}
  { content, userId, isbn, chapterNumber }
  │
  ▼
PetAiChatServlet.doPost()
  │
  ├─ AiRateLimiter.tryAcquire(userId) ──→ 超限 → 返回等待秒数
  │
  ├─ 解析 actionType → 分配 SystemPrompt
  │
  ├─ 流式任务 (answer / ai_steward / ai_debater)
  │   └─ streamDoubaoToSse()
  │       1. 构建历史上下文 (最近5轮 ai_interaction_history)
  │       2. 组装 OpenAI 格式请求体
  │       3. HTTPS 连接 (URI.create().toURL(), 绕过系统代理)
  │       4. 逐行 SSE → MessageSseServlet.sendMessageToUser()
  │       5. 完成 → PET_AI_DONE → 入库 ai_interaction_history
  │
  └─ 同步任务 (其他)
      └─ callDoubaoApi() → 直接返回结果
  │
  ▼
前端 SSE 监听 → petStore 逐字追加 → PetWidget 实时渲染气泡
  → 若 bubbleType='chat' → 5s 自动消失
  → 若 bubbleType='static'/'talk' → 永驻可交互
```

### 4.2 AI 每日画报生成

```
定时任务 / 用户请求
  │
  ├─ 读取 user_profile_memory (偏好 + 情绪 + 人设)
  ├─ 构建文生图 Prompt：温暖治愈系 + 用户画像 + 书香场景
  ├─ POST /v3/images/generations
  │   { model: Doubao-Seedream-4.5, prompt, size: "2k" }
  └─ 保存 → user_daily_ai_picture 表 → SSE 通知前端
```

### 4.3 AI 阅读周报生成

```
用户请求 / 定时触发
  │
  ├─ 聚合本周数据：user_read_daily + user_read_record + user_read_stats + user_behavior_log
  ├─ Prompt：「你是专业的阅读分析师。基于以下阅读数据，生成温暖周报...」
  ├─ callDoubaoApi() 同步调用
  └─ 存入 user_report 表
```

### 4.4 AI 知识图谱构建

```
character_analyze 返回 JSON
  │
  ├─ 解析人物数组 → book_character_lore 表
  ├─ 每个人物 → graph_node (mapId=isbn+"_char")
  └─ relationship_json → graph_edge (人物间连线)
  │
  ▼
GraphServlet → 前端知识图谱可视化
```

### 4.5 前端 AI 交互

**宠物问候语**：`petStore.js` greetingList（5 条温暖书香问候，每日轮换）

**确认气泡**：全网搜书时弹出确认对话框 → 用户确认 → 调用爬虫

**自动出题**：
```
petStore.startAutoQuiz(interval, mode, aiContent)
  ├─ mode='local' → 本地题库随机
  └─ mode='ai' → 调用 AI 生成 → INTERACTION 气泡显示
```

---

## 5. AI 限流架构

### 5.1 限流器设计 (`AiRateLimiter`)

```
AiRateLimiter (内存 ConcurrentHashMap + 滑动窗口)
│
├─ userCalls (每分钟窗口 60s)
│   └─ tryAcquire(userId)           → PetAiChatServlet 使用
│       └─ 每用户每分钟 ≤ 10 次
│
└─ userDailyCalls (每日窗口 24h)
    ├─ tryAcquireDaily(userId, 20)   → PetAiChatWithMemoryServlet 使用
    └─ tryAcquireDaily(userId, 5)    → ChatMessageServlet 使用
```

**通用方法**：
- `tryAcquire(userId)` — 每分钟限流，默认 10 次/60s
- `tryAcquireDaily(userId, maxPerDay)` — 每日限流，可配置次数
- `getWaitSeconds(userId)` — 返回距解禁还需秒数
- `getDailyWaitSeconds(userId, maxPerDay)` — 每日窗口等待秒数

**线程安全**：`synchronized` + `ConcurrentHashMap`，全内存零 DB 开销

### 5.2 限流矩阵

| 被保护端点 | 限流类型 | 限额 | 超出提示 |
|-----------|---------|------|---------|
| `PetAiChatServlet` | 每分钟 | 10 次 | "AI调用太频繁了，请 X 秒后再试" |
| `PetAiChatWithMemoryServlet` | 每日 | 20 次 | "今日AI对话次数已用完（20次/天）" |
| `ChatMessageServlet.generateImage` | 每日 | 5 张 | "今日AI图片生成次数已用完（5张/天）" |
| `AudiobookServlet` | 管理员 | Type=3 | "仅管理员可生成有声书" |
| 书架 AI 管家 | 前端节流 | 5min/书 | 静默跳过 |

### 5.3 前端节流

`Bookshelf.vue` 中点击书籍封面触发 AI 管家时：

```javascript
// 同一本书 5 分钟内不重复调用
const storageKey = `ai_steward_last_${isbn}`
if (!lastCall || (now - lastCall) > 5 * 60 * 1000) {
  sendAiMsg(...)
  localStorage.setItem(storageKey, now)
}
// 自动清理超过 20 条旧记录
```

---

## 6. 流式输出工作流

### 6.1 SSE 事件类型

| 事件 | 含义 | 数据 |
|------|------|------|
| `PET_AI_START` | 流式开始 | `{msgId, bubbleType}` |
| `PET_AI_CHUNK` | 文字块 | `{msgId, chunk}` |
| `PET_AI_DONE` | 流式完成 | `{msgId}` |
| `PET_AI_ERROR` | 流式异常 | `{msgId}` |

### 6.2 前端流式处理

```
PET_AI_START → petStore.createStreamingMessage() → 空气泡出现
PET_AI_CHUNK → petStore.appendStreamingChunk() → 逐字追加
  ... 逐字渲染 ...
PET_AI_DONE → petStore.finishStreamingMessage()
  ├─ bubbleType='chat' → 5s 自毁
  └─ bubbleType='static'/'talk' → 永驻可交互
```

### 6.3 异常处理

`PetAiChatServlet.doPost()` 有完整的 `try-catch-finally`：

```java
try {
    // AI 调用逻辑
} catch (Exception e) {
    logger.error("AI对话异常", e);
    out.write(ResultDTO.fail("AI服务暂时不可用，请稍后再试"));
} finally {
    out.flush();
    out.close();
}
```

**保护的异常场景**：JSON 解析失败、`NoSuchElementException`（空列表）、HTTP 超时、豆包 API 错误

---

## 7. 附录：API 调用示例

### 7.1 流式 AI 对话

```
POST https://ark.cn-beijing.volces.com/api/v3/chat/completions
Authorization: Bearer {ARK_API_KEY}

{
  "model": "{ENDPOINT_ID}",
  "messages": [
    { "role": "system", "content": "{systemPrompt}" },
    { "role": "user", "content": "你好呀" }
  ],
  "max_tokens": 800,
  "temperature": 0.7,
  "stream": true
}
```

### 7.2 文生图

```
POST https://ark.cn-beijing.volces.com/api/v3/images/generations
Authorization: Bearer {ARK_API_KEY}

{
  "model": "{IMAGE_ENDPOINT_ID}",
  "prompt": "温暖治愈系插画：读者在阳光书房阅读，暖色调",
  "size": "2k",
  "n": 1,
  "response_format": "url"
}
```

### 7.3 Java 调用实现

```java
// 现代写法 (Java 21+)：URI.create().toURL() 替代已过时的 new URL(String)
URL url = URI.create(CHAT_API_URL).toURL();
HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
conn.setRequestMethod("POST");
conn.setConnectTimeout(2000);
conn.setReadTimeout(40000);
conn.setRequestProperty("Authorization", "Bearer " + ARK_API_KEY);
```

---

> **参考文件**：`AiConfig.java` | `PetAiChatServlet.java` | `PetAiChatWithMemoryServlet.java` | `AiRateLimiter.java` | `petStore.js` | `Bookshelf.vue` | `BasicLayout.vue`
