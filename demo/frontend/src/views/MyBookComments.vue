<script setup>
// ====================== 1. 依赖导入 ======================
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { ElCard, ElButton, ElMessage, ElMessageBox, ElRate, ElDivider, ElInput } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../utils/request.js'
import { useUserStore } from '../stores/userStore'

// ====================== 2. 实例化 & 全局注入 ======================
const router = useRouter()
const userStore = useUserStore()

// 全局状态注入
const isDark = inject('isDark')
// 分享功能注入
const generateShareContent = inject("generateShareContent")
const shareToForum = inject("shareToForum")
// 评论数据注入（分页相关保留）
const userBookComments = inject('userBookComments')
const fetchUserBookComments = inject('fetchUserBookComments')
const loadMoreBookComments = inject('loadMoreBookComments')
const isLoadingBookComments = inject('isLoadingBookComments')
const hasMoreBookComments = inject('hasMoreBookComments')
const resetBookCommentPage = inject('resetBookCommentPage')

// ====================== 3. 基础响应式变量 ======================
const currentUserId = inject("currentUserId")
const currentUserType =inject("currentUserType")

// ====================== 4. 右键菜单相关变量 ======================
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickComment = inject('currentRightClickBook')
const isMobile = inject('isMobile')

// ====================== 5. 评论编辑/删除相关变量 ======================

// ====================== 6. 通用工具函数 ======================
// 时间格式化
const formatTime = inject('formatDateTime')

// 评论长度校验
const getValidContentLength = inject('getValidContentLength')

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

// ====================== 7. 右键菜单核心方法 ======================
// 移动端检测
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 右键菜单处理
const handleContextMenu = (e) => {
  if (isMobile.value) return

  const target = e.target
  if (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'BUTTON' ||
      target.closest('.el-button') ||
      target.closest('.el-input') ||
      target.closest('.el-select')) {
    return
  }

  e.preventDefault()
  e.stopPropagation()

  currentRightClickComment.value = null

  const commentItem = target.closest('.book-comment-item')
  if (commentItem) {
    const allComments = document.querySelectorAll('.book-comment-item')
    const index = Array.from(allComments).indexOf(commentItem)
    if (index >= 0 && index < userBookComments.value.length) {
      currentRightClickComment.value = userBookComments.value[index]
    }
  }

  if (!currentRightClickComment.value) return

  const menuWidth = 180
  const menuHeight = 50
  let x = e.clientX
  let y = e.clientY

  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - 10

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

// 关闭右键菜单
const closeContextMenu = () => {
  showContextMenu.value = false
  currentRightClickComment.value = null
}

// 分享书籍评论
const shareBookComment = () => {
  if (!currentRightClickComment.value) return

  const comment = currentRightClickComment.value
  const shareData = {
    bookName: comment.bookName || '未知书籍',
    star: comment.star || 3,
    content: comment.comment,
    time: formatTime(comment.time)
  }

  const content = generateShareContent('book-comment', shareData)
  shareToForum({
    type: 'book-comment',
    id: comment.commentId,
    ...content
  })

  closeContextMenu()
}

// ====================== 8. 评论编辑/删除业务方法 ======================
// 删除评论
const deleteBookComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条书籍评论吗？', '删除提示', {
      type: 'warning', center: true
    })
    await request.delete('/book/comment', { params: { commentId } })
    ElMessage.success({ message: '删除成功', center: true })
    fetchUserBookComments(currentUserId.value)
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ====================== 9. 页面跳转方法 ======================
const gotoBookDetail = inject('gotoBookDetail')

// ====================== 10. 生命周期钩子 ======================
onMounted(() => {
  fetchUserBookComments(currentUserId.value)
  document.addEventListener('contextmenu', handleContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', handleContextMenu)
})
</script>

<template>
  <!-- 页面根容器 -->
  <div style="padding: 20px; width: 100%; margin: 0 auto; position: relative; z-index: 999;min-height: calc(100vh - 190px);">

    <!-- ====================== 模块1：页面标题 ====================== -->
    <!-- 🔥 改造：标题 + 加载按钮 同行 Flex 容器 -->
    <div class="header-flex-wrap">
      <h2 :style="{
        margin: '0',
        fontSize: '22px',
        fontWeight: '600',
        color: isDark ? '#e5e7eb' : '#333'
      }">📚 我的全部书籍评论</h2>

      <!-- 加载更多按钮 -->
      <div class="load-more-btn-wrap">
        <el-button
            type="primary"
            size="default"
            :disabled="isLoadingBookComments || !hasMoreBookComments"
            @click="loadMoreBookComments(currentUserId)"
        >
          {{ isLoadingBookComments ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>

    <!-- ====================== 模块2：无数据状态 ====================== -->
    <div v-if="userBookComments.length === 0" :style="{
      textAlign: 'center',
      padding: '60px 0',
      color: isDark ? '#9ca3af' : '#999'
    }">
      暂无书籍评论～
    </div>

    <!-- ====================== 模块3：评论列表（移除无限滚动 + 右侧内边距） ====================== -->
    <el-scrollbar
        v-else
        height="calc(100vh - 190px)"
    >
      <div class="comment-masonry-container">
        <div
            v-for="(comment, index) in userBookComments"
            :key="comment.id || index"
            class="masonry-item book-comment-item"
            :style="getStickyNoteStyle(index)"
        >
          <div class="washi-tape" :style="getTapeStyle(index)"></div>

          <el-card class="sticky-note-card">
            <div style="display: flex; gap: 20px; align-items: flex-start;">
              <!-- 评论内容区 -->
              <div style="flex: 1; min-width: 0;">
                <!-- 评论头部：书名+星级+时间+操作按钮 -->
                <div class="book-comment-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                  <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                <span :style="{
                  fontSize: '16px',
                  fontWeight: '600',
                  color: isDark ? '#93c5fd' : '#60a5fa'
                }">{{ comment.bookName || '未知书籍' }}</span>
                    <el-rate v-model="comment.star" disabled size="small" active-color="#ffc107"/>
                    <span :style="{
                  fontSize: '12px',
                  color: isDark ? '#9ca3af' : '#999'
                }">{{ formatTime(comment.time) }}</span>
                  </div>
                  <div class="book-comment-actions">
                    <el-button type="danger" size="small" link @click="deleteBookComment(comment.commentId)" class="auto-width-link-btn">删除</el-button>
                  </div>
                </div>

                <!-- 分割线 -->
                <el-divider content-position="left" :style="{
              borderColor: isDark ? '#ffffff' : '#eee',
              borderWidth: '2px'
            }"></el-divider>

                <!-- 查看状态 -->
                <div
                    :style="{
                  whiteSpace: 'pre-wrap',
                  lineHeight: '1.6',
                  textAlign: 'left',
                  color: isDark ? '#d1d5db' : '#333'
                }"
                >
                  {{ comment.comment || '暂无评论内容' }}
                </div>

              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 加载中提示 -->
      <div v-if="isLoadingBookComments" class="loading-tip">
        加载中...
      </div>

      <!-- 没有更多了提示 -->
      <div v-if="!hasMoreBookComments && userBookComments.length > 0" class="no-more-tip">
        没有更多评论了
      </div>

    </el-scrollbar>

    <!-- ====================== 模块4：右键菜单 ====================== -->
    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <div class="context-menu-item" @click="shareBookComment">
        <span>⭐</span>
        <span>分享这条书籍评论</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 🔥 新增：标题+按钮 同行容器 */
.header-flex-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap; /* 小屏幕自动换行，防止挤压 */
  gap: 12px;
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

/* ====================== 3. 卡片基础样式 ====================== */
:deep(.el-card) {
  border-radius: 8px;
  transition: transform 0.2s;
}
:deep(.el-card:hover) {
  transform: translateY(-2px);
}

/* ====================== 4. 输入框暗黑模式适配 ====================== */
:deep(.dark-mode .el-textarea__inner) {
  background: #2d3748 !important;
  border-color: #4a5568 !important;
  color: #e5e7eb !important;
}

/* ====================== 5. 通用按钮样式 ====================== */
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
.auto-width-link-btn--primary { color: #1890ff !important; }
.auto-width-link-btn--danger { color: var(--el-color-danger) !important; }

/* ====================== 6. 加载提示文字样式 ====================== */
.loading-tip, .no-more-tip {
  text-align: center;
  padding: 16px 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* ✅ 新增：加载更多按钮容器（居中） */
.load-more-btn-wrap {
  text-align: center;
  padding: 20px 0 30px;
}

/* ====================== 7. 移动端适配 ====================== */
@media (max-width: 768px) {
  .book-comment-cover {
    display: none !important;
  }

  .book-comment-card :deep(.el-card__body) {
    padding: 12px !important;
  }

  .book-comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px !important;
  }

  .book-comment-actions {
    align-self: flex-start;
  }

  .book-comment-inner {
    gap: 10px !important;
  }
}

/* ======================================
   手账瀑布流核心样式【重点修复3列布局】
======================================== */
.comment-masonry-container {
  column-count: 3;       /* 固定PC端3列 */
  column-gap: 16px;      /* 缩小列间距，释放宽度（原24px → 16px） */
  padding: 10px 5px;     /* 大幅减小左右内边距，解决宽度挤占 */
}

.masonry-item {
  break-inside: avoid;   /* 防止卡片被列分割 */
  margin-bottom: 28px;   /* 微调上下间距 */
  position: relative;
  width: 100%;
  display: block;       /* ✅ 关键修复：替换 inline-block，消除空白间隙 */
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

/* 压缩内部布局，防止变形 */
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

/* ====================== 暗黑模式 - 手账便签完整适配 ====================== */
/* 1. 四组深色便签底色+边框，覆盖行内浅色style */
html.dark :deep(.masonry-item:nth-child(4n+1) .sticky-note-card) {
  background-color: #1D2D46 !important;
  border-color: #2b3d5c !important;
}
html.dark :deep(.masonry-item:nth-child(4n+2) .sticky-note-card) {
  background-color: #2F3A37 !important;
  border-color: #3e4c49 !important;
}
html.dark :deep(.masonry-item:nth-child(4n+3) .sticky-note-card) {
  background-color: #262E30 !important;
  border-color: #364144 !important;
}
html.dark :deep(.masonry-item:nth-child(4n+4) .sticky-note-card) {
  background-color: #2E3B3E !important;
  border-color: #405155 !important;
}

/* 2. 深色半透明纸胶带，覆盖行内浅色胶带 */
html.dark :deep(.masonry-item:nth-child(4n+1) .washi-tape) {
  background-color: rgba(235, 181, 92, 0.45) !important;
}
html.dark :deep(.masonry-item:nth-child(4n+2) .washi-tape) {
  background-color: rgba(160, 187, 177, 0.35) !important;
}
html.dark :deep(.masonry-item:nth-child(4n+3) .washi-tape) {
  background-color: rgba(220, 239, 233, 0.25) !important;
}
html.dark :deep(.masonry-item:nth-child(4n+4) .washi-tape) {
  background-color: rgba(173, 198, 176, 0.35) !important;
}

/* 3. 暗黑模式卡片悬浮阴影适配（深色柔和阴影） */
html.dark :deep(.masonry-item:hover .sticky-note-card) {
  box-shadow: 5px 12px 22px rgba(0, 0, 0, 0.35) !important;
}

/* 4. el-card 内部主体背景透明，消除自带白色底色叠加 */
html.dark :deep(.sticky-note-card .el-card__body) {
  background: transparent !important;
}

/* 5. 分割线深色适配 */
html.dark :deep(.sticky-note-card .el-divider__line) {
  border-color: rgba(255,255,255,0.15) !important;
}

/* 6. 右键菜单强化暗黑样式（补充原有残缺样式） */
html.dark :deep(.context-menu) {
  background: #1f2937 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4) !important;
}
html.dark :deep(.context-menu-item) {
  color: #e5e7eb !important;
}
html.dark :deep(.context-menu-item:hover) {
  background-color: #374151 !important;
}

/* 7. 卡片删除文字按钮暗黑配色 */
html.dark :deep(.auto-width-link-btn--danger) {
  color: #f87272 !important;
}

/* 8. 滚动加载提示文字深色 */
html.dark .loading-tip,
html.dark .no-more-tip {
  color: #9ca3af !important;
}
/* ======================================
   屏幕自适应断点（保留原有逻辑）
======================================== */
@media (max-width: 1200px) {
  .comment-masonry-container {
    column-count: 2; /* 中屏 → 2列 */
  }
}

@media (max-width: 768px) {
  .comment-masonry-container {
    column-count: 1; /* 手机 → 单列 */
    padding: 10px 5px;
  }
}
</style>