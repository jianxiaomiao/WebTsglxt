# Forum.vue — 书籍卡片轮播 + 图片显示 可复用代码提取

> 提取自 `demo/frontend/src/views/Forum.vue`
> 用途：交给 Gemini 在其他界面（论坛、评论区、详情页等）复用这些模式

---

## 1. 书籍卡片解析 — `[{book:ISBN}]` 格式

### 1.1 正则提取（JS 逻辑）

```js
// 从评论文本中提取 [{book:ISBN}] 格式的卡片标签
// 支持类型：book / bookComment / userComment / user / note
const extractBookCards = (content) => {
  if (!content) return []
  const regex = /\[{(\w+):([a-zA-Z0-9_-]+)}\]/g
  const cards = []
  let match
  while ((match = regex.exec(content)) !== null) {
    cards.push({ type: match[1], id: match[2], link: match[0] })
  }
  return cards
}

// 剔除标签后的纯文本
const extractPureText = (content) => {
  if (!content) return ''
  return content.replace(/\[{(\w+):([a-zA-Z0-9_-]+)}\]/g, '').trim()
}

// 带缓存的解析器（避免重复计算）
const parsedForumContentCache = new Map()
const getParsedForumContent = (commentId, text) => {
  if (!parsedForumContentCache.has(commentId)) {
    const pureText = extractPureText(text)
    const cards = extractBookCards(text)
    parsedForumContentCache.set(commentId, { text: pureText, cards })
  }
  return parsedForumContentCache.get(commentId)
}
```

### 1.2 多卡片水平滚动轮播（模板）

```html
<!-- ===== 单卡片：直接展示 ===== -->
<div v-if="cards.length === 1" class="book-share-card-wrapper">
  <div v-html="parseBookLinkToCard(cards[0].link, false)"></div>
</div>

<!-- ===== 多卡片：左右箭头水平滚动 ===== -->
<div v-else class="multi-card-slider-wrapper">
  <!-- 左箭头 -->
  <div class="slider-arrow left" @click.stop="scrollCards(commentId, 'left')">
    <el-icon><ArrowLeft /></el-icon>
  </div>
  <!-- 滚动容器 -->
  <div class="multi-card-scroll-view" :id="'scroll-view-forum-' + commentId">
    <div
        v-for="card in cards"
        :key="card.id"
        class="slider-item"
        v-html="parseBookLinkToCard(card.link, true)"
    ></div>
  </div>
  <!-- 右箭头 -->
  <div class="slider-arrow right" @click.stop="scrollCards(commentId, 'right')">
    <el-icon><ArrowRight /></el-icon>
  </div>
</div>
```

### 1.3 左右滚动 JS

```js
const scrollCards = (commentId, direction) => {
  const container = document.getElementById(`scroll-view-forum-${commentId}`)
  if (!container) return
  const scrollAmount = 180
  if (direction === 'left') {
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
  } else {
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}
```

### 1.4 轮播 CSS

```css
/* ===== 外层容器：relative 定位，让箭头可绝对定位 ===== */
.multi-card-slider-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

/* ===== 滚动视口 ===== */
.multi-card-scroll-view {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  flex: 1;
  padding: 4px 0;
  width: 100% !important;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.multi-card-scroll-view::-webkit-scrollbar {
  display: none; /* 隐藏滚动条 */
}

/* ===== 左右箭头（默认隐藏，hover 时出现）===== */
.slider-arrow {
  position: absolute;
  z-index: 10;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
  opacity: 0;
  pointer-events: none;
}
.multi-card-slider-wrapper:hover .slider-arrow {
  opacity: 1;
  pointer-events: auto;
}
.slider-arrow:hover {
  background: #fff;
  color: #1890ff;
  transform: scale(1.1);
}
.slider-arrow.left  { left: -12px; }
.slider-arrow.right { right: -12px; }

/* ===== 每个卡片项不换行 ===== */
.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
}

/* ===== 暗黑模式箭头 ===== */
html.dark .slider-arrow {
  background: rgba(50, 50, 55, 0.9);
  color: #aaa;
}
```

### 1.5 卡片预览（5 种类型）

卡片渲染通过 `parseBookLinkToCard(card.link, isSimplified)` 注入函数生成 HTML。5 种卡片类型：

| type | 显示 | 封面来源 |
|------|------|---------|
| `book` | 书籍封面 + 书名 + 作者 + 评分 + 简介 | `IMAGE_BASE_URL + card.data.pictureName` |
| `bookComment` | 📚 书籍评论 + 评论内容 + 评分 | `IMAGE_BASE_URL + card.data.book.pictureName` |
| `userComment` | 💬 论坛评论 + 内容 + 点赞数 | 无封面（纯色背景 + 💬 图标） |
| `user` | 👤 用户信息 + 性别 + 类型 + 阅读时长 | 无封面（纯色背景 + 👤 图标） |
| `note` | 📝 读书笔记 + 原文 + 批注 | `IMAGE_BASE_URL + card.data.book.pictureName` |

**单卡片展示 HTML 结构**：
```html
<div class="book-share-card" style="max-width: 100%;">
  <img src="封面URL" class="book-share-card-cover" alt="书籍封面">
  <div class="book-share-card-info">
    <p class="book-title">书名</p>
    <p class="book-author">作者：xxx</p>
    <p class="book-rating">⭐ 评分：4.5</p>
    <p class="book-desc">简介...</p>
  </div>
</div>
```

**精简版卡片 HTML（轮播中使用的）**：
```html
<div class="simplified-card" data-type="book" data-id="ISBN">
  <div class="simplified-cover-wrapper">
    <img src="封面URL" class="book-share-card-cover">
    <div class="book-rating badge">4.5分</div>
  </div>
  <p class="book-title">书名</p>
</div>
```

---

## 2. 论坛图片显示

### 2.1 配置

```js
// 图片基础路径
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL

// 图片预览缓存
const parsedPreviewCache = new Map()
const getPreviewList = (commentId, images) => {
  if (!parsedPreviewCache.has(commentId)) {
    parsedPreviewCache.set(commentId, images.map(item => IMAGE_BASE_URL + item.imageUrl))
  }
  return parsedPreviewCache.get(commentId)
}
```

### 2.2 图片数据格式

```json
// comment.images — 后端返回的图片数组
[
  { "id": 1, "imageUrl": "/forum_images/xxx.png" },
  { "id": 2, "imageUrl": "/forum_images/yyy.jpg" }
]
```

### 2.3 图片显示模板（≤2 张全部显示，>2 张折叠）

```html
<div v-if="comment.images && comment.images.length > 0" class="comment-images">
  <!-- ≤ 2 张：全部显示 -->
  <template v-if="expandedImages[comment.commentId] || comment.images.length <= 2">
    <el-image
        lazy
        v-for="(img, index) in comment.images"
        :key="img.id"
        :src="IMAGE_BASE_URL + img.imageUrl"
        :preview-src-list="getPreviewList(comment.commentId, comment.images)"
        :initial-index="index"
        preview-teleported
        fit="cover"
        alt="评论图片"
        class="comment-img"
    />
  </template>

  <!-- > 2 张：显示第一张 + 遮罩展示剩余数量 -->
  <template v-else>
    <el-image
        lazy
        :src="IMAGE_BASE_URL + comment.images[0].imageUrl"
        :preview-src-list="getPreviewList(comment.commentId, comment.images)"
        :initial-index="0"
        preview-teleported
        fit="cover"
        class="comment-img"
    />
    <div class="image-overlay-wrapper" @click="expandedImages[comment.commentId] = true">
      <el-image
          lazy
          :src="IMAGE_BASE_URL + comment.images[1].imageUrl"
          class="comment-img"
      />
      <div class="expand-mask">+{{ comment.images.length - 2 }}张</div>
    </div>
  </template>
</div>
```

### 2.4 图片 CSS

```css
/* 主评论图片网格 */
.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

/* 单张图片 */
:deep(.comment-img) {
  width: 180px !important;
  height: 180px !important;
  border-radius: 6px !important;
  border: 1px solid #e2e8f0 !important;
  object-fit: cover !important;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
:deep(.comment-img:hover) {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08) !important;
}

/* 折叠遮罩 */
.image-overlay-wrapper {
  position: relative;
  cursor: pointer;
  width: 180px;
  height: 180px;
  border-radius: 6px;
  overflow: hidden;
}
.expand-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  border-radius: 6px;
}

/* 子评论小图 (80x80) */
.sub-comment-images {
  display: grid;
  grid-template-columns: repeat(3, 80px);
  gap: 8px;
  margin-top: 8px;
}
:deep(.sub-comment-img) {
  width: 80px !important;
  height: 80px !important;
  border-radius: 4px !important;
}
```

### 2.5 图片上传 UI

```html
<div class="image-upload-area" style="margin-top: 15px;">
  <div class="image-list">
    <div v-for="(img, index) in uploadedImages" :key="img.id" class="image-item">
      <img :src="IMAGE_BASE_URL + img.imageUrl" alt="上传图片">
      <div class="delete-btn" @click="deleteImage(index)">×</div>
    </div>
    <div v-if="uploadedImages.length < 9" class="upload-btn" @click="triggerUpload">
      <el-icon size="24" color="#ccc"><Picture /></el-icon>
    </div>
  </div>
  <input ref="fileInput" type="file" multiple accept="image/*" style="display: none;" @change="handleFileUpload">
</div>
```

```css
.image-list { display: flex; flex-wrap: wrap; gap: 8px; }
.image-item { position: relative; width: 80px; height: 80px; border-radius: 6px; overflow: hidden; }
.image-item img { width: 100%; height: 100%; object-fit: cover; }
.delete-btn {
  position: absolute; top: 2px; right: 2px;
  width: 20px; height: 20px; border-radius: 50%;
  background: rgba(0,0,0,0.5); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; cursor: pointer;
}
.upload-btn {
  width: 80px; height: 80px; border: 2px dashed #ddd;
  border-radius: 6px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
```

---

## 3. 复用清单

| 组件 | 可以复用到 |
|------|-----------|
| `extractBookCards()` + 正则 | 任何需要解析 `[{book:ISBN}]` 标签的地方 |
| 多卡片轮播（箭头 + 滚动） | 评论详情弹窗、书籍详情页、个人主页 |
| 图片显示（折叠/展开） | 审核页面（已做）、聊天消息、帖子详情 |
| 图片上传 UI | 任何需要图文混合输入的地方 |
| `IMAGE_BASE_URL` | 全局常量，任何引用静态图片的地方 |
