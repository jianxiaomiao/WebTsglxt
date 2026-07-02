# 🎧 有声书播放器前端组件 — Gemini 提示词

> 将此提示词发给 Gemini，让它帮你生成 `AudiobookPlayer.vue` 组件

---

## 任务

为「沉浸式图书阅读与交流书屋」项目创建一个有声书播放器 Vue 3 组件：
**文件路径**：`src/components/AudiobookPlayer.vue`

---

## 技术栈

- **框架**：Vue 3 (Composition API, `<script setup>`)
- **UI库**：Element Plus (使用 `ElButton`, `ElSlider`, `ElProgress`, `ElTag`, `ElSkeleton`)
- **HTTP**：`import request from '../utils/request'`（Axios 实例，返回 `{ code, msg, data }`）
- **样式**：毛玻璃 `glass-panel` class + 暗黑模式 `.dark-mode` 适配

---

## 后端 API（已写好）

| 接口 | 说明 |
|------|------|
| `GET /api/audiobook/segments?isbn=xxx&chapter=3&pageNum=1&pageSize=20` | 分页获取有声书分段列表 |
| `POST /api/audiobook/generate` `{ isbn, chapter, userId }` | 触发AI角色标注生成分段 |
| `GET /api/audiobook/progress?isbn=xxx&chapter=3` | 查询生成进度 `{ total, generated, percent }` |
| `POST /api/audiobook/generateAudio` `{ segmentId }` | 为单段生成TTS音频，返回 `{ audioUrl, audioDuration }` |
| `POST /api/tts/generate` `{ text, voice, cache: true }` | TTS 缓存模式（整段文本→MP3文件），返回 `{ url, size }` |

**返回的分段数据结构**：
```json
{
  "id": 1,
  "isbn": "xxx",
  "chapterNumber": 3,
  "paragraphId": "xxx",
  "sortOrder": 1,
  "roleType": "青年女",
  "emotion": "温柔",
  "audioUrl": "/audiobook_audio/xxx.mp3",
  "audioDuration": 12.5,
  "status": 1,
  "paragraphContent": "原文内容..."
}
```

---

## 组件设计要求

### Props
```
isbn: String       (书籍ISBN)
chapterNumber: Number (章节号)
isDark: Boolean    (暗黑模式)
```

### 布局结构

```
┌─────────────────────────────────────────────────┐
│  🎧 《活着》第3章 · 有声书           [✕ 关闭]  │
├─────────────────────────────────────────────────┤
│  ████████████████░░░░░░░░  12:34 / 45:20       │
│  [⏮ 上段] [▶ 播放/⏸ 暂停] [⏭ 下段]         │
│  [🐢 1.0x] [🔊]  [⚙ 音色: 青年女]            │
├─────────────────────────────────────────────────┤
│  分段列表（当前播放高亮）:                        │
│  ┌──────────────────────────────────────────┐  │
│  │ 🧑 中年男 · 旁白 · 😊温柔      00:34 ▶  │  │
│  │ "福贵说到这里，停了下来，看着..."         │  │
│  ├──────────────────────────────────────────┤  │
│  │ 👩 青年女 · 家珍 · 😢悲伤      02:15     │  │
│  │ "福贵，你回来就好，我们不治病了..."       │  │
│  └──────────────────────────────────────────┘  │
│                    [加载更多 ▼]                 │
└─────────────────────────────────────────────────┘
```

### 功能需求

1. **打开时检查进度**：调 `/api/audiobook/progress`。若 `total=0`（未生成），显示「生成有声书」按钮，点击调 `/api/audiobook/generate`
2. **生成状态**：生成中显示 `ElProgress` 进度条 + 分段列表骨架屏 (`ElSkeleton`)
3. **逐段生成音频**：分段列表加载后，自动从第一段开始调用 `generateAudio`，顺序生成（非并行）。已生成(status=1)的跳过
4. **播放控制**：
   - 播放/暂停按钮切换
   - 上一段/下一段（自动切段 + 重置进度）
   - 当前播放段落高亮
   - 音频使用 HTML5 `<audio>` 元素
5. **音色选择器**：下拉菜单选择当前音色，可选：青年男/青年女/中年男/小女孩/老爷爷。切换后对有声书记录生效
6. **暗黑模式**：所有 `.glass-panel` 区域加 `:class="{ 'dark-mode': isDark }"`
7. **滚动加载**：分段列表支持触底加载更多（分页）
8. **角色emoji映射**：旁白→🧑, 青年男→🧑, 青年女→👩, 中年男→👨, 小女孩→👧, 老爷爷→👴
9. **情感emoji映射**：欢快→😊, 悲伤→😢, 愤怒→😡, 温柔→💕, 平静→😌

### 样式要求

- 整体为毛玻璃面板（`backdrop-filter: blur(20px) saturate(150%)`）
- 播放进度条用自定义样式，当前播放段落加 `background: rgba(64,158,255,0.1)` 高亮
- 分段列表项 hover 效果
- 按钮用 Element Plus 圆形按钮，排列紧凑
- 移动端适配：`@media (max-width: 768px)` 下全宽铺满
- 暗黑模式下：背景 `rgba(30,30,30,0.6)`，文字 `#e5e7eb`，border `rgba(255,255,255,0.1)`

### 边界情况处理

- 未生成时显示空状态："🎙️ 还没有生成有声书，点击下方按钮开始"
- 生成中显示每段的 status tag：🟢已生成 / 🟡生成中 / 🔴失败
- 音频加载失败时静默跳过，不阻塞整体播放
- 用户手动切换段落时，如果该段音频未生成，自动触发生成并等待

---

## 参考代码风格

项目已有组件如 `CoReadingRoom.vue` 和 `PetWidget.vue` 的风格：毛玻璃面板 + Element Plus + `<script setup>` + scoped CSS。

请求封装 `request.js` 用法：
```js
import request from '../utils/request'
const res = await request.get('/api/audiobook/segments', { params: { isbn, chapter, pageNum, pageSize } })
if (res.code === 200) { /* res.data.list */ }
```

---

请生成完整的 `AudiobookPlayer.vue` 文件。
