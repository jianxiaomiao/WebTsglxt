# 🎧 有声书书架页面 — Gemini 提示词

## 任务

为「沉浸式图书阅读与交流书屋」项目创建一个**有声书书架页面**，作为 BasicLayout 的子路由页面。

---

## 文件清单（需要创建/修改的）

### 新建
| 文件 | 说明 |
|------|------|
| `src/views/AudiobookLibrary.vue` | 有声书书架主页面 |
| `src/views/AudiobookLibraryChapter.vue` | 点击书籍后显示的有声书章节列表组件（可作为弹窗或内嵌） |

### 修改
| 文件 | 位置 | 改什么 |
|------|------|--------|
| `src/router/index.js` | routes 数组 | 加 `/audiobook` 路由 | 不需要按需加载
| `src/components/BasicLayout.vue` | 侧边导航 | 加 `🎧 有声书` 菜单项 |

---

## 技术栈

- Vue 3 Composition API (`<script setup>`)
- Element Plus (ElInput, ElImage, ElPagination, ElTag, ElSkeleton, ElEmpty)
- Axios: `import request from '../utils/request'`
- 毛玻璃 `glass-panel` class + 暗黑模式 `.dark-mode`

---

## 后端 API

### 1. 有声书书籍列表（分页+搜索）
```
GET /api/audiobook/books?pageNum=1&pageSize=12&keyword=活着

返回:
{
  code: 200,
  data: {
    total: 5, pageNum: 1, pageSize: 12, totalPages: 1,
    list: [
      { isbn: "xxx", bookName: "活着", author: "余华",
        pictureName: "xxx.jpg", information: "简介...",
        chapterCount: 3 }
    ]
  }
}
```

### 2. 某书的有声书章节列表
```
GET /api/audiobook/chapters?isbn=9787530210543

返回:
{
  code: 200,
  data: [
    { chapterNumber: 2, segmentCount: 53 },
    { chapterNumber: 3, segmentCount: 47 }
  ]
}
```

### 3. 有声书播放器组件（已有）
```html
<AudiobookPlayer
  v-model:visible="showPlayer"
  :isbn="selectedIsbn"
  :chapter-number="selectedChapter"
  :book-name="selectedBookName"
  :is-dark="isDark"
/>
```

> 通过 inject 获取 `isDark`：`const isDark = inject('isDark')`

---

## AudiobookLibrary.vue 设计

### 页面布局
```
┌─────────────────────────────────────────────┐
│  📚 有声书书架            [🔍 搜索书籍...]  │
├─────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │ 📖 活着   │ │ 📖 围城   │ │ 📖 红楼梦  │    │
│  │ 余华      │ │ 钱钟书    │ │ 曹雪芹    │    │
│  │ 🎧 3章节 │ │ 🎧 2章节  │ │ 🎧 5章节  │    │
│  └──────────┘ └──────────┘ └──────────┘    │
│  ┌──────────┐ ┌──────────┐                 │
│  │ ...       │ │ ...       │                │
│  └──────────┘ └──────────┘                 │
│           [< 1 2 3 ... >]                   │
└─────────────────────────────────────────────┘
```

### 点击书籍 → 弹窗显示章节
```
┌────────────────────────────────────┐
│  📖 《活着》有声书章节              │
│                                    │
│  🎧 第1章 — 53段对话  [▶ 播放]    │
│  🎧 第2章 — 47段对话  [▶ 播放]    │
│  🎧 第3章 — 38段对话  [▶ 播放]    │
│                                    │
│                    [✕ 关闭]        │
└────────────────────────────────────┘
```

点击"播放" → 打开 `AudiobookPlayer` 组件（v-model:visible, isbn, chapter-number）

---

## 🎨 视觉风格要求（卡通手工感）

### 核心元素
- **绳子**：书籍卡片上方用 CSS 画一条棕色麻绳纹理（`border-top: 3px dashed #8B7355` + 伪元素画绳结）
- **卡片**：米白色背景，圆角12px，微阴影，hover 时微微上浮 `transform: translateY(-4px)`
- **吊牌感**：每个卡片像一个挂在绳子上的手工吊牌
- **字体**：标题用衬线字体（Georgia / 思源宋体）

### 颜色
- 卡片背景: `#FFF8F0`（暖白/米色）
- 绳子颜色: `#8B7355`（棕色）
- 暗黑模式: 卡片 `#2A241B`，绳子 `#A0845C`
- 章节数标签: `ElTag type="warning"` 暖橙色

### 卡片样式参考
```css
.audiobook-card {
  background: #FFF8F0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(139, 115, 85, 0.15);
  transition: all 0.3s ease;
  overflow: hidden;
  cursor: pointer;
  /* 顶部绳子装饰 */
  border-top: 3px dashed #8B7355;
  position: relative;
}
.audiobook-card::before {
  content: '🪢';
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16px;
}
.audiobook-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(139, 115, 85, 0.25);
}
.dark-mode .audiobook-card {
  background: #2A241B;
  border-top-color: #A0845C;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
}
```

### 页面背景
复用之前的磨砂玻璃和液态玻璃设计
---

## 功能要点

1. **搜索**：页面顶部搜索框，输入时实时筛选（防抖300ms），调 `/api/audiobook/books?keyword=xxx`
2. **分页**：底部 `ElPagination`，每页12本书
3. **空状态**：无结果时显示 `ElEmpty`："📭 还没有有声书，去阅读器里生成吧~"
4. **加载态**：首次加载用 `ElSkeleton` 卡片骨架屏
5. **暗黑模式**：通过 `inject('isDark')` 获取
6. **响应式**：`@media (max-width: 768px)` 下卡片2列布局，搜索框全宽

---

## 路由配置（需要加到 `router/index.js`）

```js
// 在 BasicLayout 的 children 数组中加：
{
  path: 'audiobook',
  name: 'AudiobookLibrary',
  component: () => import('../views/AudiobookLibrary.vue')
}
```

## 侧边导航（需要加到 `BasicLayout.vue`）

在侧边栏菜单中加一项：
```html
<el-menu-item index="/audiobook">
  <el-icon><Headset /></el-icon>
  <span>有声书</span>
</el-menu-item>
```

---

请生成完整的 `AudiobookLibrary.vue` 和 `AudiobookLibraryChapter.vue`（如果拆分为两个文件），以及标注需要在 router 和 BasicLayout 中加的内容。
