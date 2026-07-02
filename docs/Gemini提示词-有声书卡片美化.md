# 📚 有声书书架卡片 — 书籍包裹封面设计提示词

## 任务

修改 `src/views/AudiobookLibrary.vue` 的卡片样式，将现有的简单卡片升级为**不规则手工卡片**，并用麻绳连线串联。

## 目标效果（手作风+不规则形状）

卡片不再是规整的矩形，而是像手工撕纸/牛皮纸标签一样，边缘微微不规则。多个卡片之间用弯曲的棕色虚线（麻绳）连接，像晾衣绳上挂着的书签。

## 不规则卡片实现方案（纯CSS，不需图片）

### 方案A：非对称圆角（最简单）
```css
.audiobook-card {
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
}
```
每个卡片用不同的圆角比例，看起来像手工剪裁的标签。

### 方案B：clip-path 多边形（更不规则）
```css
.audiobook-card:nth-child(odd) {
  clip-path: polygon(5% 0, 100% 3%, 95% 100%, 0% 97%);
}
.audiobook-card:nth-child(even) {
  clip-path: polygon(0% 5%, 95% 0%, 100% 95%, 3% 100%);
}
```

### 方案C：SVG 滤镜毛边（最自然的手撕感）
```css
.audiobook-card {
  filter: url(#rough-edge);
}
```
配合一个隐藏的 SVG filter 定义在模板中：
```html
<svg width="0" height="0" style="position:absolute">
  <filter id="rough-edge">
    <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="3" result="noise"/>
    <feDisplacementMap in="SourceGraphic" in2="noise" scale="5" xChannelSelector="R" yChannelSelector="G"/>
  </filter>
</svg>
```

## 麻绳连线效果

卡片之间用 CSS 伪元素画弯曲的虚线连线：
```css
/* 卡片右上角到相邻卡片左上角的连线 */
.audiobook-card::after {
  content: '';
  position: absolute;
  /* 用 border-bottom dashed + transform rotate 画斜线 */
  border-bottom: 2px dashed #C4A67D;
  width: 40px;
  transform: rotate(-15deg);
  top: -10px;
  right: -30px;
}
```

格子布局中相邻卡片之间交错连线，模拟"用麻绳把书签串在晾衣绳上"的感觉。

## 页面背景

```
┌──────────────────────────────────────────────┐
│  木纹/布纹背景（CSS渐变模拟）                  │
│                                              │
│  ════════════════════════════════════════     │ ← 晾衣绳（顶部横线）│
│    🏷️        🏷️        🏷️        🏷️       │ ← 不规则卡片
│     \        /  \      /                     │ ← 麻绳连线
│                                              │
└──────────────────────────────────────────────┘
```

## 颜色

- 卡片: 米白到淡黄渐变 (`#FFF8F0` → `#FFF3E0`)，模拟牛皮纸
- 绳子: `#C4A67D` / `#8B7355`，棕色虚线
- 暗黑: 卡片 `#2A241B`，绳子 `#A0845C`
- 背景: 淡木纹色 `#F5EDE3`（暗黑 `#1E1915`）

## 保持原有功能
- 搜索、分页、章节弹窗、播放器触发不做改动
- 只改 CSS 样式


---

## 当前卡片代码（需要修改的 CSS 区域）

```css
.audiobook-card {
  background: #FFF8F0;
  border-radius: 14px;
  box-shadow: 0 6px 20px rgba(139, 115, 85, 0.12);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: visible !important;
  cursor: pointer;
  border-top: 3px dashed #8B7355;
}
```

## 目标效果

```
┌──────────────────────────────┐
│  🪢 ← 绳子结                  │
│  ════════════════════════    │ ← 虚线绳子（border-top dashed）│
│  ┌──────────────────────┐    │
│  │                      │    │
│  │    📖 书籍封面图片     │    │
│  │                      │    │
│  │    ─────────────     │    │
│  │    活着               │    │
│  │    余华               │    │
│  │    🎙️ 3章有声书       │    │
│  │                      │    │
│  └──────────────────────┘    │
│     ════════════════════     │ ← 底部虚线绳子
│           🪢                  │ ← 底部绳结
└──────────────────────────────┘
```

## 具体要求

### 1. 3D 书籍效果
用 CSS 伪元素 + box-shadow 模拟书籍的**厚度感和立体感**：
- 右侧加 `::after` 伪元素模拟书脊（3px 宽深色竖条）
- 底部加阴影模拟书本放在桌面上的投影
- hover 时卡片微微旋转 `transform: rotateY(-3deg)` + 上浮

### 2. 绳子装饰
- 顶部：`border-top: 3px dashed #8B7355` + `::before` 伪元素画绳结（emoji 🪢 或 CSS 圆形）
- 底部同样加虚线绳子收尾

### 3. 封面图片区域
- 图片用 `border-radius: 8px` 圆角
- 图片上方叠加一个微妙的渐变遮罩（顶部略暗 → 高光区域）

### 4. 文字排版
- 书名：大号加粗，居中，衬线字体（Georgia）
- 作者：小号灰色，居中
- 章节标签：暖橙色 tag

### 5. 暗黑模式
- 卡片背景: `#2A241B`
- 绳子颜色: `#A0845C`
- 文字: `#e5e7eb`

### 6. 响应式
- `@media (max-width: 768px)` 下卡片宽度自适应，grid 2 列

### 7. Hover 动画
```css
.audiobook-card:hover {
  transform: translateY(-6px) rotateY(-2deg);
  box-shadow: 0 12px 32px rgba(139, 115, 85, 0.3);
}
.audiobook-card:hover .card-cover-wrap img {
  transform: scale(1.05);
}
```

---

## 不需要改动的内容
- 搜索逻辑
- API 调用
- 分页
- 章节弹窗

## 只需要改 CSS + 可能微调 HTML 结构
- `.audiobook-card` 的 CSS
- `.card-cover-wrap` 的 CSS
- `.card-info` 的排版
- 暗黑模式适配
