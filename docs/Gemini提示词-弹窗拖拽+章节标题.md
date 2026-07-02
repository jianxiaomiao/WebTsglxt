# 🖐️ 两个前端改动 — Gemini 提示词

> 将此提示词发给 Gemini，让它同时完成两个改动

---

## 改动一：有声书弹窗增加拖拽功能

### 文件
`src/components/AudiobookPlayer.vue`

### 要做什么
给有声书播放器弹窗加鼠标拖拽移动功能。

### 实现方案
在 `<script setup>` 中加入以下拖拽逻辑（参考项目里已有的拖拽模式）：

```js
// 拖拽状态
const isDragging = ref(false)
const hasMoved = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const panelX = ref(window.innerWidth - 440) // 默认右侧
const panelY = ref(60)                        // 默认距顶60px

const onDragStart = (e) => {
  // 如果点在按钮/输入框上，不触发拖拽
  if (e.target.closest('.el-button, .el-slider, input, textarea, .no-drag')) return
  isDragging.value = true
  hasMoved.value = false
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  e.preventDefault()
}

const onDragMove = (e) => {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX.value
  const dy = e.clientY - dragStartY.value
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved.value = true
  panelX.value = Math.max(0, Math.min(window.innerWidth - 200, panelX.value + dx))
  panelY.value = Math.max(0, Math.min(window.innerHeight - 60, panelY.value + dy))
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
}

const onDragEnd = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}
```

在**面板最外层 div** 上加：
- `@mousedown="onDragStart"`
- `:style="{ left: panelX + 'px', top: panelY + 'px', right: 'auto' }"`

把面板从 `right: 20px` 固定定位改为通过 `panelX`/`panelY` 动态定位。

### ⚠️ 注意
- 保留暗黑模式适配
- 保留移动端 `@media (max-width: 768px)` 下的底部抽屉样式（拖拽在移动端自动禁用）
- 保留关闭按钮、播放按钮的正常点击（`no-drag` class 或 `e.target.closest` 过滤）

---

## 改动二：阅读区域章节标题显示

### 文件
`src/views/BookReader.vue`

### 要做什么
在每章第一页顶部显示章节标题，格式为`第N章 - 章节名`。

### 当前代码（已部分实现）
在 `.reader-page-content` 区域内，段落循环之前，有这段代码：

```html
<!-- 🔥 章节标题（每章第一页显示） -->
<div v-if="currentPage === 1 && currentChapter?.name" class="chapter-header"
  :style="{ fontSize: (parseInt(fontSizeValue) + 4) + 'px', color: textColor }">
  {{ currentChapter.name }}
</div>
```

### 需要修改
- 显示格式改为 `第{{ currentNumber }}章 - {{ currentChapter.name }}`
- CSS 保持不变（左对齐、字号=正文+4、颜色跟正文）

### 当前CSS（不需要改）

```css
.chapter-header {
  text-align: left;
  font-weight: 700;
  padding: 8px 0 20px 0;
  margin-bottom: 4px;
  letter-spacing: 1px;
  opacity: 0.85;
}
```

### ⚠️ 变量说明
- `currentPage` — 当前页码（ref，number）
- `currentChapter` — 当前章节对象（ref，有 `.name` 属性）
- `currentNumber` — 当前章节号（ref，number）
- `fontSizeValue` — 正文字号（ref，字符串如 "14px"）
- `textColor` — 正文颜色（ref）
- `fontSizeStyle` — 字体样式computed
