<script setup>
// ====================== 1. 依赖导入 ======================
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { ElCard, ElButton, ElMessage, ElMessageBox, ElDivider, ElInput, ElImage } from 'element-plus'
import request from '../utils/request.js'
import { useUserStore } from '../stores/userStore'
// 🌟 1. 顶部别忘了引入图标
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

// ====================== 2. 状态实例化 & 全局注入 ======================
const userStore = useUserStore()
// 暗黑模式状态
const isDark = inject('isDark')
// 全局分享方法
const generateShareContent = inject("generateShareContent")
const shareToForum = inject("shareToForum")
const expandedImages = ref({})
// 评论数据与方法
const userForumComments = inject('userForumComments') // ✅ 用注入的
const forumCommentPage = ref(1)
const forumCommentPageSize = ref(6)
const forumCommentTotal = ref(0)
const isLoadingForumComments = ref(false)
const hasMoreForumComments = ref(true)
const loadMoreUserForumComments =inject('loadMoreUserForumComments')
const resetUserForumComments =inject('resetUserForumComments')
const fetchUserForumComments = inject('fetchUserForumComments')

// ====================== 3. 静态常量 ======================
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;
const parseBookLinkToCard = inject('parseBookLinkToCard')
const formatMentionText = inject('formatMentionText')
// ====================== 4. 基础用户响应式变量 ======================
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')

// ====================== 5. 右键菜单相关变量 ======================
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickComment = inject('currentRightClickBook')
const isMobile = inject('isMobile')

// ====================== 6. 论坛评论编辑相关变量 ======================

// ====================== 7. 通用工具函数 ======================
// 时间格式化
const formatTime = inject('formatDateTime')

// ======================================
// 新增：手账便签风不规则样式与胶带生成器
// ======================================
// 浅色配色
const noteBackgroundsLight = ['#f3faf4', '#fffdeb', '#edf7f5', '#fffdf0']
const noteBordersLight = ['#cfead4', '#ebdca5', '#cceee2', '#e3d69c']
const tapeColorsLight = ['rgba(168, 218, 220, 0.5)', 'rgba(241, 166, 166, 0.5)', 'rgba(233, 196, 106, 0.5)', 'rgba(138, 201, 143, 0.5)']

// 暗黑配色（和你之前CSS深色色盘统一）
const noteBackgroundsDark = ['#1D2D46', '#2F3A37', '#262E30', '#2E3B3E']
const noteBordersDark = ['#2b3d5c', '#3e4c49', '#364144', '#405155']
const tapeColorsDark = ['rgba(235, 181, 92, 0.45)', 'rgba(160, 187, 177, 0.35)', 'rgba(220, 239, 233, 0.25)', 'rgba(173, 198, 176, 0.35)']

const getStickyNoteStyle = (index) => {
  const idx = index % 4
  const bg = isDark.value ? noteBackgroundsDark[idx] : noteBackgroundsLight[idx]
  const border = isDark.value ? noteBordersDark[idx] : noteBordersLight[idx]
  // 旋转角度不变，统一保留
  const rotate = (index % 2 === 0 ? 1 : -1) * (0.4 + (index % 3) * 0.4)
  return {
    backgroundColor: bg,
    borderColor: border,
    transform: `rotate(${rotate}deg)`
  }
}

const getTapeStyle = (index) => {
  const idx = index % 4
  const color = isDark.value ? tapeColorsDark[idx] : tapeColorsLight[idx]
  const rotate = (index % 2 === 0 ? -4 : 3) + (index % 3)
  return {
    backgroundColor: color,
    transform: `rotate(${rotate}deg)`
  }
}

// ====================== 新增：论坛卡片解析与图片缓存 ======================
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

const extractPureText = (content) => {
  if (!content) return ''
  return content.replace(/\[{(\w+):([a-zA-Z0-9_-]+)}\]/g, '').trim()
}

const parsedForumContentCache = new Map()
const getParsedForumContent = (commentId, text) => {
  if (!parsedForumContentCache.has(commentId)) {
    const pureText = extractPureText(text)
    const cards = extractBookCards(text)
    parsedForumContentCache.set(commentId, { text: pureText, cards })
  }
  return parsedForumContentCache.get(commentId)
}

const parsedPreviewCache = new Map()
const getPreviewList = (commentId, images) => {
  if (!parsedPreviewCache.has(commentId)) {
    parsedPreviewCache.set(commentId, images.map(item => IMAGE_BASE_URL + item.imageUrl))
  }
  return parsedPreviewCache.get(commentId)
}

const scrollCards = (commentId, direction) => {
  const container = document.getElementById(`scroll-view-forum-${commentId}`)
  if (!container) return
  const scrollAmount = 140
  if (direction === 'left') {
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
  } else {
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// ====================== 8. 右键菜单核心方法 ======================
// 移动端尺寸检测
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 右键菜单触发逻辑
const handleContextMenu = (e) => {
  if (isMobile.value) return

  const target = e.target
  if (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'BUTTON' ||
      target.closest('.el-button') ||
      target.closest('.el-input') ||
      target.closest('.el-select')
  ) {
    return
  }

  e.preventDefault()
  e.stopPropagation()

  currentRightClickComment.value = null

  const commentItem = target.closest('.forum-comment-item')
  if (commentItem) {
    const allComments = document.querySelectorAll('.forum-comment-item')
    const index = Array.from(allComments).indexOf(commentItem)
    if (index >= 0 && index < userForumComments.value.length) {
      currentRightClickComment.value = userForumComments.value[index]
    }
  }

  if (!currentRightClickComment.value) return

  const menuWidth = 180
  const menuHeight = 50
  let x = e.clientX
  let y = e.clientY

  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  showContextMenu.value = false
  currentRightClickComment.value = null
}

// 分享论坛评论
const shareForumComment = () => {
  if (!currentRightClickComment.value) return

  const comment = currentRightClickComment.value
  const shareData = {
    content: comment.userComment,
    time: formatTime(comment.commentTime)
  }

  const content = generateShareContent('forum-comment', shareData)
  shareToForum({
    type: 'forum-comment',
    id: comment.commentId,
    ...content
  })

  closeContextMenu()
}

// ====================== 9. 论坛评论业务方法 ======================
// 删除评论
const deleteForum = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条论坛评论吗？', '删除提示', {
      type: 'warning', center: true
    })
    await request.delete('/user/comment', { params: { commentId: id } })
    ElMessage.success({ message: '删除成功', center: true })
    await fetchUserForumComments(currentUserId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error({ message: '删除失败', center: true })
      console.error(e)
    }
  }
}

// 打开编辑状态


// ====================== 10. 生命周期钩子 ======================
onMounted(() => {
  resetUserForumComments()
  fetchUserForumComments(currentUserId.value)
  document.addEventListener('contextmenu', handleContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', handleContextMenu)
})
</script>

<template>
  <!-- 页面根容器 -->
  <div style="padding: 20px; width: 100%; margin: 0 auto; position: relative; z-index: 999;min-height: calc(100vh - 190px);">

    <!-- 🔥 头部同行容器：标题 + 加载更多按钮（和其他页面样式统一） -->
    <div class="header-flex-wrap">
      <h2 :style="{
        margin: '0',
        fontSize: '22px',
        fontWeight: '600',
        color: isDark ? '#e5e7eb' : '#333',
      }">💬 我的全部论坛评论</h2>

      <!-- 加载更多按钮 -->
      <div class="load-more-btn-wrap">
        <el-button
            type="primary"
            size="default"
            :disabled="isLoadingForumComments || !hasMoreForumComments"
            @click="loadMoreUserForumComments(currentUserId)"
        >
          {{ isLoadingForumComments ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>

    <!-- ====================== 模块2：无数据空状态 ====================== -->
    <div v-if="userForumComments.length === 0" :style="{
      textAlign: 'center',
      padding: '60px 0',
      color: isDark ? '#9ca3af' : '#999'
    }">
      暂无论坛评论～
    </div>

    <!-- ====================== 模块3：评论列表（移除无限滚动 @end-reached） ====================== -->
    <el-scrollbar
        v-else
        height="calc(100vh - 240px)"
        style="padding-right: 5px;"
    >
      <div  class="comment-masonry-container">
        <div
            v-for="(comment, index) in userForumComments"
            :key="comment.id || index"
            class="masonry-item"
            :style="getStickyNoteStyle(index)"
        >
          <div class="washi-tape" :style="getTapeStyle(index)"></div>

          <el-card class="sticky-note-card">
            <!-- 时间 + 操作按钮 -->
            <div style="display: flex; justify-content: space-between; margin-bottom: 12px; align-items: center;">
              <span :style="{
                fontSize: '12px',
                color: isDark ? '#9ca3af' : '#999'
              }">{{ formatTime(comment.commentTime) }}</span>
              <div>
                <el-button type="danger" size="small" link @click="deleteForum(comment.commentId)" class="auto-width-link-btn">删除</el-button>
              </div>
            </div>

            <!-- 分割线 -->
            <el-divider content-position="left" :style="{
              borderColor: isDark ? '#ffffff' : '#eee',
              borderWidth: '2px'
            }"></el-divider>
            <div>
              <div
                  :style="{
      whiteSpace: 'pre-wrap',
      lineHeight: '1.6',
      marginBottom: '10px',
      textAlign: 'left',
      color: isDark ? '#d1d5db' : '#333'
    }"
                  v-html="formatMentionText(getParsedForumContent(comment.commentId, comment.userComment).text)"
              >
              </div>

              <div v-if="getParsedForumContent(comment.commentId, comment.userComment).cards.length > 0" class="forum-cards-area">
                <div v-if="getParsedForumContent(comment.commentId, comment.userComment).cards.length === 1" class="book-share-card-wrapper" style="margin-bottom: 12px;">
                  <div v-html="parseBookLinkToCard(getParsedForumContent(comment.commentId, comment.userComment).cards[0].link, false)"></div>
                </div>

                <div v-else class="multi-card-slider-wrapper">
                  <div class="slider-arrow left" @click.stop="scrollCards(comment.commentId, 'left')">
                    <el-icon><ArrowLeft /></el-icon>
                  </div>
                  <div class="multi-card-scroll-view" :id="'scroll-view-forum-' + comment.commentId">
                    <div
                        v-for="card in getParsedForumContent(comment.commentId, comment.userComment).cards"
                        :key="card.id"
                        class="slider-item"
                        v-html="parseBookLinkToCard(card.link, true)"
                    ></div>
                  </div>
                  <div class="slider-arrow right" @click.stop="scrollCards(comment.commentId, 'right')">
                    <el-icon><ArrowRight /></el-icon>
                  </div>
                </div>
              </div>

              <div v-if="comment.images && comment.images.length > 0" class="comment-images">
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
            </div>
          </el-card>
        </div>
      </div>

      <!-- 加载中 & 无更多提示 -->
      <div v-if="isLoadingForumComments" class="loading-tip">加载中...</div>
      <div v-if="!hasMoreForumComments && userForumComments.length > 0" class="no-more-tip">没有更多评论了</div>
    </el-scrollbar>

    <!-- ====================== 模块4：右键菜单 ====================== -->
    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <div class="context-menu-item" @click="shareForumComment">
        <span>💬</span>
        <span>分享这条论坛评论</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 🔥 新增：标题 + 加载按钮 同行 Flex 容器（全局统一样式） */
.header-flex-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap; /* 小屏幕自动换行，防止挤压 */
  gap: 12px;
}

/* 按钮容器：右对齐，精简边距 */
.load-more-btn-wrap {
  text-align: right;
}

/* ====================== 1. 右键菜单样式 ====================== */
.context-menu {
  position: fixed;
  z-index: 99999 !important;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 180px;
  font-size: 14px;
}
.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.context-menu-item:hover {
  background-color: #f5f7fa;
}
/* 暗黑模式适配 */
:deep(.dark-mode .context-menu) {
  background: #1f2937;
  color: #e5e7eb;
}
:deep(.dark-mode .context-menu-item:hover) {
  background-color: #374151;
}

/* ====================== 2. 滚动条样式 ====================== */
.scroll-card {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.scroll-card::-webkit-scrollbar {
  display: none;
}

/* ====================== 3. 评论图片 & 轮播优化样式 ====================== */
.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

:deep(.comment-img) {
  width: 80px !important;
  height: 80px !important;
  border-radius: 6px !important;
  border: 1px solid rgba(0,0,0,0.05) !important;
  object-fit: cover !important;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

:deep(.comment-img:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1) !important;
}

/* 折叠遮罩 */
.image-overlay-wrapper {
  position: relative;
  cursor: pointer;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
}

.expand-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  border-radius: 6px;
  transition: background 0.2s ease;
}

.image-overlay-wrapper:hover .expand-mask {
  background: rgba(0, 0, 0, 0.3);
}

/* ===== 书籍分享轮播容器 ===== */
.multi-card-slider-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0;
}

.multi-card-scroll-view {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  gap: 10px;
  width: 100% !important;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.multi-card-scroll-view::-webkit-scrollbar {
  display: none;
}

.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
}

/* 轮播悬浮箭头 (半透明手账风) */
.slider-arrow {
  position: absolute;
  z-index: 10;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
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
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  transform: scale(1.1);
}

.slider-arrow.left  { left: -10px; }
.slider-arrow.right { right: -10px; }

/* 修复：覆盖之前强制 100px 的限制（保证图片遮罩布局不被打破） */
.sticky-note-card :deep(.comment-img) {
  max-width: 80px !important;
  height: 80px !important;
}
/* ====================== 4. 编辑状态图片样式 ====================== */
.image-upload-area .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.image-upload-area .image-item {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  border: 1px solid #eee;
}
.image-upload-area .image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-upload-area .delete-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: rgba(0,0,0,0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  font-size: 14px;
}

/* ====================== 5. 卡片基础样式 ====================== */
:deep(.el-card) {
  border-radius: 8px;
  transition: transform 0.2s;
}
:deep(.el-card:hover) {
  transform: translateY(-2px);
}

/* ====================== 6. 输入框暗黑模式适配 ====================== */
:deep(.dark-mode .el-textarea__inner) {
  background: #2d3748 !important;
  border-color: #4a5568 !important;
  color: #e5e7eb !important;
}

/* ====================== 7. 通用按钮样式 ====================== */
.auto-width-link-btn {
  display: inline-block !important;
  flex-shrink: 0 !important;
  width: auto !important;
  min-width: auto !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 0 4px !important;
  line-height: 1 !important;
  font-size: 12px !important;
}

/* 加载/无更多提示文字 */
.loading-tip, .no-more-tip {
  text-align: center;
  padding: 15px 0;
  color: #909399;
  font-size: 14px;
}

/* ======================================
   手账瀑布流核心样式【重点修复：恢复PC端3列】
======================================== */
.comment-masonry-container {
  column-count: 3;       /* 固定PC端3列 */
  column-gap: 16px;      /* 缩小列间距（原24px → 16px），释放横向宽度 */
  padding: 10px 5px;     /* 大幅缩小左右内边距，解决宽度挤占 */
}

.masonry-item {
  break-inside: avoid;   /* 防止单张卡片被跨列斩断 */
  margin-bottom: 28px;   /* 卡片上下间距 */
  position: relative;
  width: 100%;
  display: block;        /* ✅ 关键修复：替换 inline-block，消除浏览器空白间隙 */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 鼠标悬浮：扶正、放大、阴影加深 */
.masonry-item:hover {
  transform: translateY(-6px) scale(1.03) rotate(0deg) !important;
  z-index: 50;
}
.masonry-item:hover .sticky-note-card {
  box-shadow: 5px 12px 22px rgba(0, 0, 0, 0.12) !important;
}

/* 灵魂撕裂纸胶带 */
.washi-tape {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 75px;
  height: 18px;
  z-index: 99;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  clip-path: polygon(0% 15%, 4% 0%, 96% 0%, 100% 12%, 98% 50%, 100% 88%, 95% 100%, 5% 100%, 0% 85%, 2% 50%);
}

/* 便签卡片基础复写 */
.sticky-note-card {
  background-color: inherit !important; /* 继承外层随机纸张色 */
  border-color: inherit !important;     /* 继承外层随机边框色 */
  border-style: dashed !important;      /* 虚线边框更具手账感 */
  border-width: 1px !important;
  box-shadow: 2px 5px 10px rgba(0, 0, 0, 0.04) !important;
}

/* 内部布局防挤压 */
.sticky-note-card :deep(.el-row) {
  display: flex !important;
  flex-direction: column !important;
  gap: 10px;
}
.sticky-note-card :deep(.el-col) {
  width: 100% !important;
  max-width: 100% !important;
  flex: 0 0 100% !important;
}

/* 限制图片尺寸 */
.sticky-note-card :deep(.image-item),
.sticky-note-card :deep(.comment-img) {
  max-width: 100px !important;
  height: 100px !important;
}

/* ======================================
   屏幕自适应断点控制
======================================== */
@media (max-width: 1200px) {
  .comment-masonry-container {
    column-count: 2; /* 平板/小屏电脑 → 2列 */
  }
}

@media (max-width: 768px) {
  /* 手机端：标题+按钮换行居中 */
  .header-flex-wrap {
    justify-content: center;
    flex-direction: column;
  }
  .load-more-btn-wrap {
    text-align: center;
  }

  .comment-masonry-container {
    column-count: 1; /* 手机端 → 单列 */
    padding: 10px 5px;
  }
}
</style>