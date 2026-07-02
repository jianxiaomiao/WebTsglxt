<script setup>
// ====================== 1. 依赖导入 ======================
import { ref, onMounted, inject, onUnmounted } from 'vue'
import { ElCard, ElRow, ElCol, ElButton, ElMessage, ElMessageBox, ElTag, ElDivider } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../utils/request.js'
import { useUserStore } from '../stores/userStore'

// ====================== 2. 实例化对象 ======================
const userStore = useUserStore()
const router = useRouter()

// ====================== 3. 全局依赖注入 ======================
// 用户相关注入
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')
// 笔记数据注入
// 笔记数据注入（分页版）
const userNotes = inject('userNotes')
const fetchUserNotes = inject('fetchUserNotes')
const loadMoreUserNotes = inject('loadMoreUserNotes')
const isLoadingNotes = inject('isLoadingNotes')
const hasMoreNotes = inject('hasMoreNotes')
const resetUserNotes = inject('resetUserNotes')
const noteTypeList = inject('noteTypeList', [])

// 分享功能注入
const generateShareContent = inject("generateShareContent")
const shareToForum = inject("shareToForum")

// ====================== 4. 基础响应式变量 ======================
// 移动端适配
const isMobile = inject('isMobile')

// ====================== 5. 右键菜单相关变量 ======================
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickNote = inject('currentRightClickBook')

// ====================== 6. 通用工具函数 ======================
// 笔记类型名称获取
const getNoteTypeName = inject('getNoteTypeName')

// 笔记时间格式化
const formatNoteTime = inject('formatDateTime')

// 提取章节号
const getChapterNumber = (chapterId) => {
  if (!chapterId) return ''
  const parts = chapterId.split('-')
  return parts.length > 1 ? parts[1] : chapterId
}

// 笔记标签类型映射
const getNoteTagType = inject('getNoteTagType')

// 批注样式映射
const getReaderCommentStyle = inject('getReaderCommentStyle')

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
  const bg = userStore.isDark.value ? noteBackgroundsDark[idx] : noteBackgroundsLight[idx]
  const border = userStore.isDark.value ? noteBordersDark[idx] : noteBordersLight[idx]
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
  const color = userStore.isDark.value ? tapeColorsDark[idx] : tapeColorsLight[idx]
  const rotate = (index % 2 === 0 ? -4 : 3) + (index % 3)
  return {
    backgroundColor: color,
    transform: `rotate(${rotate}deg)`
  }
}
// ====================== 7. 右键菜单核心方法 ======================
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

  currentRightClickNote.value = null

  const noteItem = target.closest('.note-item')
  if (noteItem) {
    const allNotes = document.querySelectorAll('.note-item')
    const index = Array.from(allNotes).indexOf(noteItem)
    if (index >= 0 && index < userNotes.value.length) {
      currentRightClickNote.value = userNotes.value[index]
    }
  }

  if (!currentRightClickNote.value) return

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
  currentRightClickNote.value = null
}

// 分享笔记到论坛
const shareNote = () => {
  if (!currentRightClickNote.value) return

  const note = currentRightClickNote.value
  const shareData = {
    bookName: note.book?.bookname || '未知书籍',
    chapter: `第${getChapterNumber(note.chapterId)}章`,
    noteType: getNoteTypeName(note.type),
    text: note.text,
    comment: note.readerComment || ''
  }

  const content = generateShareContent('note', shareData)
  shareToForum({
    type: 'note',
    id: note.id,
    ...content
  })

  closeContextMenu()
}

// ====================== 8. 笔记业务方法 ======================
// 删除笔记
const deleteNote = async (noteId) => {
  try {
    await ElMessageBox.confirm('确定删除这条笔记吗？', '删除提示', {
      type: 'warning', center: true
    })
    const res = await request.delete('/user/note', { params: { id: noteId } })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchUserNotes(currentUserId.value)
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ====================== 9. 生命周期钩子 ======================
onMounted(() => {
  // 🔥 重置分页 → 加载第一页
  resetUserNotes()
  fetchUserNotes(currentUserId.value)
  document.addEventListener('contextmenu', handleContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', handleContextMenu)
})
</script>

<template>
  <!-- 页面根容器 -->
  <div style="padding: 20px; width: 100%; min-height: 100%; margin: 0 auto; position: relative; height:calc(100vh - 190px)">

    <!-- 🔥 头部同行容器：标题 + 加载更多按钮（和书籍评论页样式统一） -->
    <div class="header-flex-wrap">
      <h2 style="margin: 0; font-size: 22px; font-weight: 600;">📖 我的全部笔记</h2>
      <!-- 加载更多按钮 -->
      <div class="load-more-btn-wrap">
        <el-button
            type="primary"
            size="default"
            :disabled="isLoadingNotes || !hasMoreNotes"
            @click="loadMoreUserNotes(currentUserId)"
        >
          {{ isLoadingNotes ? '加载中...' : '加载更多' }}
        </el-button>
      </div>
    </div>

    <!-- ====================== 模块2：无数据空状态 ====================== -->
    <div v-if="userNotes.length === 0" style="text-align: center; padding: 60px 0; color: #999;">
      暂无笔记，快去阅读书籍添加笔记吧～
    </div>

    <!-- ====================== 模块3：笔记列表（移除无限滚动 @end-reached） ====================== -->
    <el-scrollbar
        v-else
        height="calc(100vh - 240px)"
        style="padding-right: 5px;"
    >
      <div class="comment-masonry-container">
        <div
            v-for="(note, index) in userNotes"
            :key="note.id || index"
            class="masonry-item note-item"
            :style="getStickyNoteStyle(index)"
            @contextmenu.prevent="showContextMenu($event, note)"
        >
          <div class="washi-tape" :style="getTapeStyle(index)"></div>

          <el-card class="sticky-note-card">
            <div style="display: flex; align-items: center; gap: 20px;">
              <!-- 笔记内容区 -->
              <div style="flex: 1; min-width: 0;">
                <!-- 书名 + 标签 + 删除按钮 -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 16px; font-weight: 600;">{{ note.bookName || '未知书籍' }}</span>
                    <el-tag size="small" :type="getNoteTagType(note.type)" effect="plain">
                      {{ getNoteTypeName(note.type) }}
                    </el-tag>
                  </div>
                  <el-button type="danger" size="small" link @click="deleteNote(note.id)" class="auto-width-link-btn">删除</el-button>
                </div>

                <el-divider content-position="left" style="border-color: #eee; border-width: 2px"></el-divider>

                <!-- 笔记正文 -->
                <div style="margin-bottom: 8px; line-height: 1.6; white-space: pre-wrap; text-align: left">
                  {{ note.text || '暂无笔记内容' }}
                </div>

                <!-- 个人批注 -->
                <div
                    v-if="note.readerComment"
                    :style="{
                    whiteSpace: 'pre-wrap',
                    textAlign: 'left',
                    margin: '8px 0',
                    fontStyle: 'italic',
                    color: getReaderCommentStyle(note.type).color,
                    fontSize: 'inherit',
                    borderLeft: `2px solid ${getReaderCommentStyle(note.type).border}`,
                    paddingLeft: '8px'
                  }"
                >
                  我的批注：{{ note.readerComment }}
                </div>

                <!-- 章节 + 时间 -->
                <div style="font-size: 12px; color: #999; text-align: right;">
                  第{{ getChapterNumber(note.chapterId) }}章 · {{ formatNoteTime(note.createTime) }}
                </div>
              </div>

            </div>
          </el-card>
        </div>
      </div>

      <!-- 加载中 & 无更多提示 -->
      <div v-if="isLoadingNotes" class="loading-tip">加载中...</div>
      <div v-if="!hasMoreNotes && userNotes.length > 0" class="no-more-tip">没有更多笔记了</div>
    </el-scrollbar>

    <!-- ====================== 模块4：右键菜单 ====================== -->
    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <div class="context-menu-item" @click="shareNote">
        <span>📝</span>
        <span>分享这条笔记到论坛</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 🔥 新增：标题 + 加载按钮 同行 Flex 容器 */
.header-flex-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap; /* 小屏幕自动换行，防止挤压 */
  gap: 12px;
}

/* 按钮容器：右对齐，取消多余上下边距 */
.load-more-btn-wrap {
  text-align: right;
}

/* ====================== 1. 卡片基础样式 ====================== */
:deep(.el-card) {
  transition: transform 0.2s;
}
:deep(.el-card:hover) {
  transform: translateY(-2px);
}

/* ====================== 2. 通用按钮样式 ====================== */
.auto-width-link-btn {
  display: inline-block !important;
  flex-shrink: 0 !important;
  width: auto !important;
  min-width: auto !important;
  border: none !important;
  background: transparent !important;
  padding: 0 4px !important;
  font-size: 12px !important;
}

/* ====================== 3. 右键菜单样式 ====================== */
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

/* ====================== 4. 加载提示文字样式 ====================== */
.loading-tip, .no-more-tip {
  text-align: center;
  padding: 15px 0;
  color: #909399;
  font-size: 14px;
}

/* ====================== 5. 移动端通用适配 ====================== */
@media (max-width: 768px) {
  /* 隐藏书籍封面 */
  .note-book-cover {
    display: none !important;
  }
  /* 卡片内边距缩小 */
  .note-card :deep(.el-card__body) {
    padding: 12px !important;
  }
  /* 手机端：标题和按钮换行居中 */
  .header-flex-wrap {
    justify-content: center;
    flex-direction: column;
  }
  .load-more-btn-wrap {
    text-align: center;
  }
}

/* ======================================
   手账瀑布流核心样式【重点修复：恢复PC端3列】
======================================== */
.comment-masonry-container {
  column-count: 3;       /* 固定PC端3列 */
  column-gap: 16px;      /* 缩小列间距（原24px → 16px），释放宽度 */
  padding: 10px 5px;     /* 大幅缩小左右内边距，解决宽度挤占 */
}

.masonry-item {
  break-inside: avoid;   /* 防止卡片被列分割 */
  margin-bottom: 28px;   /* 卡片上下间距 */
  position: relative;
  width: 100%;
  display: block;       /* ✅ 关键修复：替换 inline-block，消除浏览器空白间隙 */
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

/* 便签卡片基础样式 */
.sticky-note-card {
  background-color: inherit !important; /* 继承外层随机纸张色 */
  border-color: inherit !important;     /* 继承外层随机边框色 */
  border-style: dashed !important;      /* 虚线边框手账风格 */
  border-width: 1px !important;
  box-shadow: 2px 5px 10px rgba(0, 0, 0, 0.04) !important;
}

/* 内部布局适配，防止挤压变形 */
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
   屏幕自适应断点
======================================== */
@media (max-width: 1200px) {
  .comment-masonry-container {
    column-count: 2; /* 中屏(平板) → 自动变为2列 */
  }
}

@media (max-width: 768px) {
  .comment-masonry-container {
    column-count: 1; /* 手机端 → 自动单列 */
    padding: 10px 5px;
  }
}
</style>