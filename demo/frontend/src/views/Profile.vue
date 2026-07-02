<script setup>
// ====================== 1. 依赖导入 ======================
// 🌟 新增导入 nextTick，用于确保 DOM 渲染完成后进行平滑滚动
import { ref, onMounted, shallowRef, inject, computed, watch, onUnmounted, nextTick } from 'vue'
import request from '../utils/request.js'
import {
  ElAvatar, ElForm, ElFormItem, ElInput, ElSelect, ElOption,
  ElDatePicker, ElButton, ElMessage, ElMessageBox, ElCard, ElRow, ElCol, ElDivider, ElInputNumber, ElRate, ElImage
} from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import { useAchievementStore } from '../stores/achievementStore'
// 🌟 1. 顶部别忘了引入图标
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

// ====================== 2. 核心实例化 ======================
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
// 2. 在实例化区域 (const userStore = useUserStore() 附近) 加上：
const achievementStore = useAchievementStore()
// ====================== 3. 全局依赖注入 ======================
const generateShareContent = inject('generateShareContent')
const shareToForum = inject('shareToForum')
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;
const expandedImages = ref({})

const refreshUserInfo = inject('refreshUserInfo')
const deptTypeList = inject('deptTypeList', [])
const userNotes = inject('userNotes')
const fetchUserNotes = inject('fetchUserNotes')
const resetUserNotes = inject('resetUserNotes')

const userForumComments = inject('userForumComments')
const fetchUserForumComments = inject('fetchUserForumComments')
const resetUserForumComments = inject('resetUserForumComments')

const userBookComments = inject('userBookComments')
const fetchUserBookComments = inject('fetchUserBookComments')
const resetBookCommentPage = inject('resetBookCommentPage')

const userReadHistory = inject('userReadHistory')
const fetchUserReadHistory = inject('fetchUserReadHistory')

const formatMentionText = inject('formatMentionText')
const parseBookLinkToCard = inject('parseBookLinkToCard')

// ====================== 4. 基础响应式变量 ======================
const currentUserId = inject('currentUserId')
const currentUserType = ref(userStore.userType || '')
const userInfo = computed(() => userStore.userInfo)
const originalUserInfo = shallowRef({})
const editUserInfo = ref({})
const isEdit = ref(false)

const CARD_TYPES = inject('CARD_TYPES')
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickItem = inject('currentRightClickItem')
const contextMenuType = inject('contextMenuType')

const currentRightClickBook = inject('currentRightClickBook')
const currentRightClickBookComment = inject('currentRightClickBookComment')
const currentRightClickForumComment = inject('currentRightClickForumComment')
const currentRightClickNote = inject('currentRightClickNote')
const currentRightClickUser = inject('currentRightClickUser')

const isMobile = ref(false)
const readRecords = ref([])
const levelColors = [
  '#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'
]

// 🏷️ 新增：热力图滚动容器的 DOM 引用
const heatmapScrollRef = ref(null)

// ====================== 5. 通用工具函数 ======================
const formatTime = inject('formatDateTime')
const getChapterNumber = (chapterId) => {
  if (!chapterId) return ''
  const parts = chapterId.split('-')
  return parts.length > 1 ? parts[1] : chapterId
}
const formatChapter = (pageNum) => {
  if (!pageNum) return ''
  if (pageNum.includes('-')) {
    return pageNum.split('-')[1]
  }
  return pageNum
}
const filterNameSpecialChar = inject('filterSpecialCharNormal')
const validateName = (name) => {
  const validName = filterNameSpecialChar(name)
  return validName.length >= 1 && validName.length <= 20
}
const deptOptions = computed(() => {
  return deptTypeList.value.map(item => ({
    label: item.deptType || '未知系别',
    value: item.id
  }))
})
const getNoteTypeName = inject('getNoteTypeName')
const getNoteTagType = inject('getNoteTagType')
const getReaderCommentStyle = inject('getReaderCommentStyle')

// ====================== 6. 右键菜单核心方法 ======================
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}
const handleContextMenu = (e) => {
  if (isMobile.value) return
  const target = e.target
  if (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'BUTTON' ||
      target.closest('.el-button') ||
      target.closest('.el-input') ||
      target.closest('.el-select') ||
      target.closest('.el-date-picker')
  ) {
    return
  }
  e.preventDefault()
  e.stopPropagation()

  clearContextMenu()

  let targetElement = null
  let index = -1

  targetElement = target.closest('.note-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.note-item')
    index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userNotes.value.length) {
      currentRightClickItem.value = userNotes.value[index]
      currentRightClickNote.value = currentRightClickItem.value
      contextMenuType.value = CARD_TYPES.NOTE
    }
  }
  targetElement = target.closest('.forum-comment-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.forum-comment-item')
    index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userForumComments.value.length) {
      currentRightClickItem.value = userForumComments.value[index]
      currentRightClickForumComment.value = currentRightClickItem.value
      contextMenuType.value = CARD_TYPES.USER_COMMENT
    }
  }
  targetElement = target.closest('.book-comment-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.book-comment-item')
    index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userBookComments.value.length) {
      currentRightClickItem.value = userBookComments.value[index]
      currentRightClickBookComment.value = currentRightClickItem.value
      contextMenuType.value = CARD_TYPES.BOOK_COMMENT
    }
  }
  targetElement = target.closest('.read-history-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.read-history-item')
    index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userReadHistory.value.length) {
      currentRightClickItem.value = userReadHistory.value[index]
      contextMenuType.value = CARD_TYPES.BOOK
      currentRightClickBook.value = currentRightClickItem.value.book
    }
  }

  if (!currentRightClickItem.value) return
  const menuWidth = 180
  const menuHeight = 50
  let x = e.clientX
  let y = e.clientY
  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10
  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}
const closeContextMenu = inject('closeContextMenu')
const clearContextMenu = inject('clearContextMenu')
const shareNoteToForum = inject('shareNoteToForum')
const shareForumComment = inject('shareForumCommentToForum')
const shareBookComment = inject('shareBookCommentToForum')
const shareBookToForum = inject('shareBookToForum')
const shareBookToFriend = inject('shareBookToFriend')
const copyBookShareLink = inject('copyBookShareLink')

// ====================== 7. 用户信息操作方法 ======================
const toggleEdit = () => {
  if (isEdit.value) {
    editUserInfo.value = JSON.parse(JSON.stringify(originalUserInfo.value))
    isEdit.value = false
  } else {
    originalUserInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    editUserInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    editUserInfo.value.name = filterNameSpecialChar(editUserInfo.value.name)
    isEdit.value = true
  }
}
const saveUserInfo = async () => {
  try {
    if (!validateName(editUserInfo.value.name)) {
      ElMessage.error('姓名只能是1-10位汉字/英文/数字！')
      return
    }
    const res = await request.put('/user/info', editUserInfo.value)
    if (res.code === 200) {
      ElMessage.success('用户信息修改成功！')
      const res = await request.get('/user/info', { params: { userId: currentUserId.value } })
      if (res.code === 200 && res.data.length > 0) {
        userStore.updateUserInfo(res.data[0])
        isEdit.value = false
        if (typeof refreshUserInfo === 'function') {
          await refreshUserInfo(currentUserId.value)
        }
        originalUserInfo.value = JSON.parse(JSON.stringify(userInfo.value))
        editUserInfo.value = JSON.parse(JSON.stringify(userInfo.value))
      } else {
        ElMessage.warning('用户信息加载失败')
      }
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (err) {
    console.error('保存用户信息失败：', err)
    ElMessage.error('修改用户信息出错，请稍后再试')
  }
}
const form = computed(() => isEdit.value ? editUserInfo.value : userInfo.value)

// ====================== 8. 笔记操作方法 ======================
const deleteNote = async (noteId) => {
  try {
    await ElMessageBox.confirm('确定删除这条笔记吗？', '删除提示', {
      type: 'warning', center: true, confirmButtonText: '确定', cancelButtonText: '取消'
    })
    const res = await request.delete('/user/note', { params: { id: noteId } })
    if (res.code === 200) {
      ElMessage.success({ message: '删除成功', center: true })
      fetchUserNotes(currentUserId.value)
    }
  } catch (e) { if (e !== 'cancel') console.error(e) }
}
const goToBookReader = inject('goToBookReader')
const goToGallery = () => {
  router.push('/read-gallery') // 确保你在 router/index.js 里注册了这个路由！
}
// ====================== 9. 论坛评论操作方法 ======================
const deleteForum = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条论坛评论吗？', '删除提示', {
      type: 'warning', center: true, confirmButtonText: '确定', cancelButtonText: '取消', reverseButtons: true
    })
    await request.delete('/user/comment', { params: { commentId: id } })
    ElMessage.success({ message: '删除成功', center: true })
    fetchUserForumComments(currentUserId.value)
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error({ message: '删除失败', center: true })
  }
}

// ====================== 10. 书籍评论操作方法 ======================
const deleteBookComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条书籍评论吗？', '删除提示', {
      type: 'warning', center: true, confirmButtonText: '确定', cancelButtonText: '取消', reverseButtons: true
    })
    await request.delete('/book/comment', { params: { commentId } })
    ElMessage.success({ message: '书籍评论删除成功！', center: true })
    await fetchUserBookComments(currentUserId.value)
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error({ message: '删除失败', center: true })
  }
}

// ====================== 11. 阅读热力图方法 ======================
const getDurationLevel = (seconds) => {
  if (seconds === 0) return 0
  const minutes = seconds / 60
  if (minutes < 30) return 1
  if (minutes < 60) return 2
  if (minutes < 120) return 3
  return 4
}
const fetchReadRecords = async () => {
  try {
    const today = new Date()
    const startDate = new Date(today)
    startDate.setFullYear(today.getFullYear() - 1)
    const formatDate = (date) => date.toISOString().split('T')[0]
    const res = await request.get('/user/read/records', {
      params: {
        userId: currentUserId.value,
        startDate: formatDate(startDate),
        endDate: formatDate(today)
      }
    })
    if (res.code === 200) {
      readRecords.value = res.data.map(r => ({
        ...r, readDate: r.readDate.split(' ')[0]
      }))
    }
  } catch (e) {
    console.error('获取阅读记录失败', e)
  }
}
const gotoWeeklyReport = () => {
  router.push({
    path: '/weeklyReport',
    query: { userid: currentUserId.value, type: 'week' }
  })
}

// 自动向右对齐滚动条的封装函数
const scrollToLatestHeatmap = () => {
  nextTick(() => {
    if (heatmapScrollRef.value) {
      heatmapScrollRef.value.scrollLeft = heatmapScrollRef.value.scrollWidth
    }
  })
}

const heatmapData = computed(() => {
  const dataMap = new Map()
  readRecords.value.forEach(r => {
    dataMap.set(r.readDate, r.readDuration || 0)
  })
  const today = new Date()
  const startDate = new Date(today)
  const dayOfWeek = startDate.getDay()
  const daysToSubtract = dayOfWeek === 0 ? 6 : dayOfWeek - 1
  startDate.setDate(today.getDate() - daysToSubtract - (52 * 7))
  const weeks = []
  let currentDate = new Date(startDate)
  for (let weekIndex = 0; weekIndex < 53; weekIndex++) {
    const week = []
    for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
      const dateStr = currentDate.toISOString().split('T')[0]
      const duration = dataMap.get(dateStr) || 0
      week.push({ date: dateStr, duration, level: getDurationLevel(duration) })
      currentDate.setDate(currentDate.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
})

const monthLabelList = computed(() => {
  const labels = []
  if (heatmapData.value.length === 0) return labels

  const startDate = new Date(heatmapData.value[0][0].date)
  const today = new Date()
  let current = new Date(startDate)

  // 用来记录已经添加过的月份，避免同一个月在交界处重复渲染
  const seenMonths = new Set()

  while (current <= today) {
    const year = current.getFullYear()
    const month = current.getMonth()
    const monthKey = `${year}-${month}`

    if (!seenMonths.has(monthKey)) {
      seenMonths.add(monthKey)

      const firstDayOfMonth = new Date(year, month, 1)
      const daysSinceStart = Math.floor((firstDayOfMonth - startDate) / (1000 * 60 * 60 * 24))
      let weekIndex = Math.floor(daysSinceStart / 7)

      // 🌟 核心修复：限制最大列索引为 52（第53列），防止最后一月被挤出网格
      if (weekIndex < 0) weekIndex = 0
      if (weekIndex > 52) weekIndex = 52

      labels.push({ text: `${month + 1}月`, offsetWeek: weekIndex })
    }

    // 推进到下个月
    current.setMonth(current.getMonth() + 1)
  }
  return labels
})

// ====================== 13. 论坛卡片解析与图片缓存 ======================
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
  const scrollAmount = 140 // 配合便当盒尺寸稍微改小滚动步长
  if (direction === 'left') {
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
  } else {
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// ====================== 12. 监听 & 生命周期 ======================
watch(() => route.path, async (p) => {
  if (p === '/profile') {
    await fetchReadRecords()
    scrollToLatestHeatmap() // 路由切回时重新对齐最新日期
  }
}, { immediate: true })

onMounted(async () => {
  achievementStore.fetchAchievements(currentUserId.value) // 🔥 加上这行

  document.addEventListener('contextmenu', handleContextMenu)
  resetUserNotes()
  fetchUserNotes(currentUserId.value)

  resetUserForumComments()
  fetchUserForumComments(currentUserId.value)

  resetBookCommentPage()
  fetchUserBookComments(currentUserId.value)

  await fetchReadRecords()
  fetchUserReadHistory(currentUserId.value)
  scrollToLatestHeatmap() // 首次挂载自动滑到最右侧
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', handleContextMenu)
})
</script>

<template>
  <div class="profile-page-container">
    <el-row :gutter="20" class="bento-main-flow-grid">

      <el-col :xs="24" :md="8" class="bento-vertical-stack-col">

        <div class="main-card bento-profile-capsule">
          <div id="guide-avatar" class="avatar-meta-wrapper">
            <el-avatar size="large" class="bento-avatar"  @click="router.push('/user/profile?userId=' + userInfo.userId)"
            style="cursor:pointer;">
              {{(userInfo.name || '未知用户').slice(-2)}}
            </el-avatar>
            <div class="meta-text">
              <div class="name-badge-row">
                <h2 class="user-display-name">{{ userInfo.name || '未知用户' }}</h2>
                <div class="profile-medal-entry" @click="router.push('/achievements')" title="前往荣誉陈列馆">
                  🏆 Lv.{{ achievementStore.userLevel || 1 }}
                </div>
              </div>
              <span class="user-time-badge">
                ⏱️ {{ Math.floor((userInfo.readTimeLong||0)/3600) }}时{{ Math.floor(((userInfo.readTimeLong||0)%3600)/60) }}分
              </span>
            </div>
          </div>

          <div class="bento-mini-bio">
            {{ userInfo.bio || '这个人很懒，什么都没写~' }}
          </div>

          <el-divider class="bento-divider" />

          <div class="form-scroll-area">
            <el-form :model="form" label-width="65px" label-align="left" class="profile-form compact-bento-form">
              <el-form-item label="姓名">
                <el-input
                    v-model="form.name"
                    :disabled="!isEdit"
                    @input="form.name=filterNameSpecialChar(form.name)"
                />
              </el-form-item>
              <el-form-item label="性别">
                <el-select v-model="form.sex" :disabled="!isEdit" class="profile-select">
                  <el-option label="男" value="男"/>
                  <el-option label="女" value="女"/>
                  <el-option label="未知" value="未知"/>
                </el-select>
              </el-form-item>
              <el-form-item label="生日">
                <el-date-picker v-model="form.birthday" type="date" :disabled="!isEdit" style="width:100%" />
              </el-form-item>
              <el-form-item label="类型">
                <el-input v-model="form.typeName" disabled />
              </el-form-item>
              <el-form-item label="介绍">
                <el-input
                    v-model="form.bio"
                    :disabled="!isEdit"
                    type="textarea"
                    :rows="2"
                    maxlength="100"
                    show-word-limit
                    placeholder="介绍一下自己吧"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="form-footer-actions">
            <el-button type="primary" size="small" @click="toggleEdit" v-if="!isEdit" round>编辑资料</el-button>
            <div v-else class="action-btn-group">
              <el-button type="success" size="small" @click="saveUserInfo" round>确认</el-button>
              <el-button size="small" @click="toggleEdit" round>取消</el-button>
            </div>
          </div>
        </div>

        <div class="main-card bento-book-comments-box custom-scroll-y">
          <h3 id="guide-book-comment" class="bento-box-title" @click="router.push('/profile/book-comments')">⭐ 书籍评论</h3>
          <div v-if="userBookComments.length===0" class="bento-empty-state">暂无书籍评论</div>
          <div v-else class="bento-list-wrapper">
            <div v-for="item in userBookComments" :key="item.commentId" class="item-card book-comment-item bento-inner-item-mini">
              <div class="book-comment-header-bento">
                <span class="bento-book-title-tag">{{ item.bookName || '未知书籍' }}</span>
                <el-rate v-model="item.star" disabled size="small" active-color="#ffc107"/>
                <el-button size="small" type="danger" link @click="deleteBookComment(item.commentId)" class="delete-mini-btn">删除</el-button>
              </div>
              <div class="item-body-content-mini">{{ item.comment || '暂无评论内容' }}</div>
              <div class="item-timestamp-footer">{{ formatTime(item.time) }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="16" class="bento-vertical-stack-col">

        <div class="main-card bento-heatmap-box">
          <h3 id="guide-heatmap-box" class="bento-box-title" @click="gotoWeeklyReport">📊 阅读贡献度</h3>
          <div ref="heatmapScrollRef" class="heatmap-scroll-container">
            <div class="heatmap-month-line">
              <template v-for="item in monthLabelList" :key="item.offsetWeek">
                <div :style="{ gridColumnStart: item.offsetWeek + 1 }" class="month-label">
                  {{ item.text }}
                </div>
              </template>
            </div>
            <div class="heatmap-grid">
              <template v-for="dayIndex in [0,1,2,3,4,5,6]" :key="dayIndex">
                <template v-for="(week, weekIndex) in heatmapData" :key="weekIndex">
                  <el-tooltip
                      :content="`${week[dayIndex].duration === 0 ? '0分钟' : Math.floor(week[dayIndex].duration/60) + '分钟'} · ${week[dayIndex].date}`"
                      placement="top"
                      open-delay="1000"
                      :show-arrow="false"
                      effect="light"
                  >
                    <div class="heatmap-cell" :style="{ backgroundColor: levelColors[week[dayIndex].level] }"></div>
                  </el-tooltip>
                </template>
              </template>
            </div>
            <div class="heatmap-legend">
              <span>少</span>
              <div v-for="color in levelColors" :key="color" :style="{backgroundColor: color}" class="legend-dot"></div>
              <span>多</span>
            </div>
          </div>
        </div>

        <el-row :gutter="20" class="bento-mid-twin-row">
          <el-col :xs="24" :sm="12">
            <div class="main-card bento-scroll-box custom-scroll-y">
              <h3 id="guide-note" class="bento-box-title" @click="router.push('/profile/notes')">📝 我的笔记</h3>
              <div v-if="userNotes.length===0" class="bento-empty-state">暂无笔记</div>
              <div v-else class="bento-list-wrapper">
                <div v-for="note in userNotes" :key="note.id" class="item-card note-item bento-inner-item">
                  <div class="item-header-row">
                    <div class="badge-title-group">
                      <el-tag size="small" :type="getNoteTagType(note.type)" effect="plain">{{ getNoteTypeName(note.type) }}</el-tag>
                      <span class="book-name-text">{{ note.bookName || '未知书籍' }}</span>
                    </div>
                    <el-button size="small" type="danger" link @click="deleteNote(note.id)" class="auto-width-link-btn">删除</el-button>
                  </div>
                  <div class="item-body-content">{{ note.text }}</div>
                  <div v-if="note.readerComment" :style="{ color: getReaderCommentStyle(note.type).color, borderLeftColor: getReaderCommentStyle(note.type).border }" class="bento-annotation">
                    批注：{{ note.readerComment }}
                  </div>
                  <div class="item-timestamp-footer">第{{ getChapterNumber(note.chapterId) }}章 · {{ formatTime(note.createTime) }}</div>
                </div>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :sm="12">
            <div class="main-card bento-scroll-box custom-scroll-y">
              <h3 id="guide-forum" class="bento-box-title" @click="router.push('/profile/forum-comments')">💬 论坛评论</h3>
              <div v-if="userForumComments.length===0" class="bento-empty-state">暂无评论</div>
              <div v-else class="bento-list-wrapper">
                <div v-for="item in userForumComments" :key="item.commentId" class="item-card forum-comment-item bento-inner-item">
                  <div class="item-header-row">
                    <span class="item-time-text">{{ formatTime(item.commentTime) }}</span>
                    <el-button size="small" type="danger" link @click="deleteForum(item.commentId)" class="auto-width-link-btn">删除</el-button>
                  </div>
                  <div class="item-body-content" v-html="formatMentionText(getParsedForumContent(item.commentId, item.userComment).text)"></div>

                  <div v-if="getParsedForumContent(item.commentId, item.userComment).cards.length > 0" class="forum-cards-area">
                    <div v-if="getParsedForumContent(item.commentId, item.userComment).cards.length === 1" class="book-share-card-wrapper" style="margin-top: 10px;">
                      <div v-html="parseBookLinkToCard(getParsedForumContent(item.commentId, item.userComment).cards[0].link, false)"></div>
                    </div>

                    <div v-else class="multi-card-slider-wrapper">
                      <div class="slider-arrow left" @click.stop="scrollCards(item.commentId, 'left')">
                        <el-icon><ArrowLeft /></el-icon>
                      </div>
                      <div class="multi-card-scroll-view custom-scroll-x" :id="'scroll-view-forum-' + item.commentId">
                        <div
                            v-for="card in getParsedForumContent(item.commentId, item.userComment).cards"
                            :key="card.id"
                            class="slider-item"
                            v-html="parseBookLinkToCard(card.link, true)"
                        ></div>
                      </div>
                      <div class="slider-arrow right" @click.stop="scrollCards(item.commentId, 'right')">
                        <el-icon><ArrowRight /></el-icon>
                      </div>
                    </div>
                  </div>

                  <div v-if="item.images && item.images.length > 0" class="comment-images">
                    <template v-if="expandedImages[item.commentId] || item.images.length <= 2">
                      <el-image
                          lazy
                          v-for="(img, index) in item.images"
                          :key="img.id"
                          :src="IMAGE_BASE_URL + img.imageUrl"
                          :preview-src-list="getPreviewList(item.commentId, item.images)"
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
                          :src="IMAGE_BASE_URL + item.images[0].imageUrl"
                          :preview-src-list="getPreviewList(item.commentId, item.images)"
                          :initial-index="0"
                          preview-teleported
                          fit="cover"
                          class="comment-img"
                      />
                      <div class="image-overlay-wrapper" @click="expandedImages[item.commentId] = true">
                        <el-image
                            lazy
                            :src="IMAGE_BASE_URL + item.images[1].imageUrl"
                            class="comment-img"
                        />
                        <div class="expand-mask">+{{ item.images.length - 2 }}张</div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <div class="main-card bento-history-shelf">
          <h3 id="guide-history" class="bento-box-title" @click="goToGallery">📚 阅读历史画廊</h3>
          <div class="shelf-horizontal-scroll custom-scroll-x">
            <div v-if="userReadHistory.length===0" class="bento-empty-state-horizontal">快去开启你的第一场阅读之旅吧~</div>
            <div v-for="h in userReadHistory" :key="h.id" class="item-card read-history-item bento-book-capsule">
              <div class="book-cover-wrapper" @click="goToBookReader(h.isbn)">
                <img :src="h.pictureName || '/default-book.png'" class="bento-book-img" alt="封面">
                <div class="book-cover-overlay">
                  <span>继续阅读</span>
                </div>
              </div>
              <div class="bento-book-info">
                <div class="bento-book-name">{{ h.bookName }}</div>
                <div class="bento-book-chapter">⏱️ 第 {{formatChapter(h.pageNum) }} 章</div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

    </el-row>

    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <div v-if="contextMenuType  === CARD_TYPES.NOTE" class="context-menu-item" @click="shareNoteToForum">
        <span>📝</span><span>分享这条笔记</span>
      </div>
      <div v-if="contextMenuType  === CARD_TYPES.USER_COMMENT" class="context-menu-item" @click="shareForumComment">
        <span>💬</span><span>分享这条论坛评论</span>
      </div>
      <div v-if="contextMenuType  === CARD_TYPES.BOOK_COMMENT" class="context-menu-item" @click="shareBookComment">
        <span>⭐</span><span>分享这条书籍评论</span>
      </div>
      <div v-if="contextMenuType  === CARD_TYPES.BOOK" class="context-menu-item" @click="shareBookToForum">
        <span>📤</span><span>分享这本书籍到论坛</span>
      </div>
      <div v-if="contextMenuType  === CARD_TYPES.BOOK" class="context-menu-item" @click="shareBookToFriend">
        <span>👥</span><span>分享给好友</span>
      </div>
      <div v-if="contextMenuType  === CARD_TYPES.BOOK" class="context-menu-item" @click="copyBookShareLink">
        <span>🔗</span><span>复制分享链接</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ======================================
   1. 便当盒核心变量与全新纵向流容器适配
======================================== */
.profile-page-container {
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
  position: relative;
  z-index: 10;
  min-height: calc(100vh - 190px);
}

/* 纵向列容器：允许卡片在垂直方向自然叠加，没有硬高度拉伸 */
.bento-vertical-stack-col {
  display: flex;
  flex-direction: column;
}

/* ======================================
   2. 精英级盒模型通用样式 (Bento Boxes)
======================================== */
.main-card {
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;

  /* ✅ 新增：给便当盒强行注入毛玻璃变量 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

.main-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
  border-color: var(--el-border-color);
}

.bento-box-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.bento-box-title:hover {
  color: var(--el-color-primary);
}

/* ======================================
   修改 1：让笔记 & 论坛评论变成你喜欢的“无边框”清爽流
======================================== */
.bento-inner-item {
  /* ✨ 核心：去掉背景和四周的边框，让它完全透明，透出大盒子的毛玻璃 ✨ */
  background: transparent !important;
  border: none !important;
  /* 只保留一条淡淡的底边框做分割，和书籍评论保持一致 */
  border-bottom: 1px solid var(--glass-border) !important;
  border-radius: 0 !important;
  padding: 12px 0;
  box-shadow: none !important;
  margin-bottom: 0;
  transition: background-color 0.2s ease;
}

/* 悬浮时给一点点极其微弱的背景高光，替代原来的阴影浮动 */
.bento-inner-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  transform: none; /* 取消悬浮上移，保持文字流的稳重感 */
}

/* 最后一个元素不需要底边框 */
.bento-inner-item:last-child {
  border-bottom: none !important;
}


/* ======================================
   修改 2：让阅读历史画廊的图片也融入毛玻璃质感
======================================== */
.bento-book-capsule {
  /* 确保外壳本身有毛玻璃 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

.bento-book-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease, opacity 0.3s ease;

  /* ✨ 重点魔法：给图片 85% 的透明度！ */
  /* 这样图片就不会那么“死”，能微微透出背后环境的色彩，充满呼吸感 */
  opacity: 0.85;
}

/* 鼠标悬浮时，图片恢复完全清晰并放大 */
.book-cover-wrapper:hover .bento-book-img {
  transform: scale(1.08);
  opacity: 1;
}

/* ======================================
   3. 【盒 1】胶囊主卡片 & 【盒 5】书籍评论（流式上下间距）
======================================== */
.bento-profile-capsule {
  height: 520px;
  display: flex;
  flex-direction: column;
  margin-bottom: 20px; /* 👈 新增：完美推开下方的书籍评论，告别大块空白 */
}

.bento-book-comments-box {
  height: 350px;
  margin-bottom: 20px; /* 👈 即使左边内容增多，也有安全边距 */
}

.avatar-meta-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.bento-avatar {
  font-size: 22px;
  font-weight: bold;
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border: 2px solid var(--el-color-primary-light-5);
  flex-shrink: 0;
}

.user-display-name {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
}

/* 🌟 核心修改：让名字和时间徽章都完美靠左对齐 */
.meta-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* 👈 关键：强制子元素全部靠左对齐 */
  gap: 6px;                /* 名字和时间徽章之间的上下间距 */
}

/* 顺便微调一下徽章本身，确保它在左边时也很好看 */
.user-time-badge {
  font-size: 11px;
  background: var(--el-fill-color);
  padding: 3px 8px;
  border-radius: 20px;
  color: var(--el-text-color-secondary);
  display: inline-flex;    /* 确保里面的闹钟图标和文字水平居中 */
  align-items: center;
}

.bento-mini-bio {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: left;
  line-height: 1.4;
  margin-bottom: 4px;
}

.bento-divider {
  margin: 12px 0;
}

.form-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}
.form-scroll-area::-webkit-scrollbar { width: 4px; }
.form-scroll-area::-webkit-scrollbar-thumb { background: var(--el-border-color); border-radius: 2px; }

.compact-bento-form :deep(.el-form-item) {
  margin-bottom: 10px;
}
.compact-bento-form :deep(.el-input__inner) {
  font-size: 13px;
}

.form-footer-actions {
  padding-top: 12px;
  text-align: center;
}

/* ======================================
   4. 🛠️【盒 2】贡献热力图卡片分辨率与对齐优化
======================================== */
.bento-heatmap-box {
  /* 这里保持宝宝你之前在界面上调大后的高度即可，无需写死，让它自由撑开 */
  margin-bottom: 20px;
}

.heatmap-scroll-container {
  width: 100%;
  margin: 0 auto;
  overflow-x: auto;
  padding-bottom: 6px;
  scroll-behavior: smooth; /* 让自动对齐最新的滚动更平滑 */
}

.heatmap-scroll-container::-webkit-scrollbar { height: 5px; }
.heatmap-scroll-container::-webkit-scrollbar-thumb { background: var(--el-border-color-light); border-radius: 3px; }

/* 🌟 核心修复：月份宽度音轨和格子音轨完全等比例对齐（53列 * 11px，间距 3px） */
.heatmap-month-line {
  display: grid;
  grid-template-columns: repeat(53, 11px);
  gap: 3px;
  margin-bottom: 6px;
  grid-auto-flow: column;
}

.month-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  text-align: left;
  white-space: nowrap;
  position: relative;
}

/* 🌟 核心修复：缩小单格宽度以完美契合 2/3 大小画幅 */
.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(53, 11px);
  grid-template-rows: repeat(7, 11px);
  gap: 3px;
}

.heatmap-cell {
  width: 11px;
  height: 11px;
  border-radius: 2px;
  cursor: pointer;
  transition: transform 0.1s ease;
}
.heatmap-cell:hover {
  transform: scale(1.3);
  z-index: 5;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

/* ======================================
   5. 【盒 3、4】中间并排双子卡片 & 底部画廊
======================================== */
.bento-mid-twin-row {
  margin-bottom: 20px; /* 推开底部的阅读历史画廊 */
}

.bento-scroll-box {
  height: 320px;
}

.bento-history-shelf {
  height: 300px;
  margin-bottom: 20px;
}

.bento-list-wrapper {
  padding-right: 2px;
}

.bento-empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.item-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.badge-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.book-name-text {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-body-content {
  font-size: 13px;
  line-height: 1.5;
  text-align: left;
  color: var(--el-text-color-regular);
  word-break: break-all;
}

.bento-annotation {
  white-space: pre-wrap;
  text-align: left;
  margin: 6px 0;
  font-style: italic;
  font-size: 12px;
  border-left: 3px solid;
  padding-left: 8px;
}

.item-timestamp-footer {
  text-align: right;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  margin-top: 6px;
}

/* 书籍评论卡片细节 */
.bento-inner-item-mini {
  border-bottom: 1px solid var(--el-border-color-extra-light);
  padding: 10px 0;
  text-align: left;
}

.book-comment-header-bento {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.bento-book-title-tag {
  font-size: 12px;
  font-weight: bold;
  background: var(--el-color-info-light-9);
  padding: 1px 6px;
  border-radius: 4px;
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-body-content-mini {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
}

.delete-mini-btn {
  margin-left: auto;
  font-size: 11px !important;
}

/* ======================================
   6. 【盒 6】横向虚拟书架画廊布局
======================================== */
.shelf-horizontal-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 4px 0 12px 0;
}

.bento-empty-state-horizontal {
  width: 100%;
  text-align: center;
  padding: 80px 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.bento-book-capsule {
  /* 移除原本的实心背景和边框 */
  /* background: var(--el-fill-color-blank); */
  /* border: 1px solid var(--el-border-color-light); */

  /* ✨ 注入毛玻璃魔法 ✨ */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;

  width: 130px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s ease;
}

/* 同样的悬浮微调 */
.bento-book-capsule:hover {
  border-color: var(--el-color-primary) !important;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1) !important;
}

.book-cover-wrapper {
  position: relative;
  width: 100%;
  height: 160px;
  cursor: pointer;
  overflow: hidden;
}

.bento-book-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease, opacity 0.3s ease;

  /* ✨ 重点魔法：给图片 85% 的透明度！ */
  /* 这样图片就不会那么“死”，能微微透出背后环境的色彩，充满呼吸感 */
  opacity: 0.85;
}
/* 鼠标悬浮时，图片恢复完全清晰并放大 */
.book-cover-wrapper:hover .bento-book-img {
  transform: scale(1.08);
  opacity: 1;
}

.book-cover-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.book-cover-wrapper:hover .bento-book-img {
  transform: scale(1.08);
}

.book-cover-wrapper:hover .book-cover-overlay {
  opacity: 1;
}

.bento-book-info {
  padding: 8px;
  text-align: left;
}

.bento-book-name {
  font-size: 12px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
  color: var(--el-text-color-primary);
}

.bento-book-chapter {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

/* ======================================
   7. 全局辅助类
======================================== */
/* ======================================
   🚀 新增：原生滚动条美化与渲染性能黑魔法
======================================== */
/* 垂直和水平滚动容器 */
.custom-scroll-y {
  overflow-y: auto;
  overflow-x: hidden;
}
.custom-scroll-x {
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 14px !important; /* 给横向滚动条多留一点呼吸空间，防止被卡片遮挡 */
  /* 🔥 针对 Firefox 的原生支持 */
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-dark) transparent;
}

/* 滚动条整体大小 */
.custom-scroll-y::-webkit-scrollbar {
  width: 6px;
}
.custom-scroll-x::-webkit-scrollbar {
  height: 8px; /* 横向稍微加粗一点点，不仅更容易点击，也更显眼 */
}

/* 🌟 核心修复：必须要有 Track（轨道）背景，否则部分浏览器会直接隐藏滚动条！ */
.custom-scroll-y::-webkit-scrollbar-track,
.custom-scroll-x::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02); /* 给轨道一个极其微弱的底色，强制浏览器渲染 */
  border-radius: 4px;
}

/* 滚动条滑块美化 */
.custom-scroll-y::-webkit-scrollbar-thumb,
.custom-scroll-x::-webkit-scrollbar-thumb {
  background: var(--el-border-color-dark);
  border-radius: 4px;
}

/* 鼠标悬浮滑块时加深颜色 */
.custom-scroll-y::-webkit-scrollbar-thumb:hover,
.custom-scroll-x::-webkit-scrollbar-thumb:hover {
  background: var(--el-text-color-secondary);
}

/* 屏蔽主线程卡顿：只渲染视野内的评论与笔记！ */
.bento-inner-item,
.bento-inner-item-mini {
  content-visibility: auto;
  contain-intrinsic-size: 100px; /* 预估单个卡片高度，防止滚动条抖动 */
  will-change: transform;
}
.auto-width-link-btn {
  font-size: 12px;
  padding: 0 2px;
}

/* ======================================
   🌟 替换原有图片与轮播图样式（适配便当盒）
======================================== */
.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

/* 单张图片适配便当盒的精致尺寸 */
:deep(.comment-img) {
  width: 80px !important;
  height: 80px !important;
  border-radius: 6px !important;
  border: 1px solid var(--glass-border) !important;
  object-fit: cover !important;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

:deep(.comment-img:hover) {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1) !important;
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
  margin-top: 10px;
}

.multi-card-scroll-view {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  gap: 10px;
  width: 100% !important;
}

.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
}

/* 轮播悬浮箭头 (使用毛玻璃材质) */
.slider-arrow {
  position: absolute;
  z-index: 10;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--glass-bg);
  backdrop-filter: blur(8px);
  border: 1px solid var(--glass-border);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  color: var(--el-text-color-regular);
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

.expand-btn {
  width: 50px;
  height: 50px;
  background: rgba(0,0,0,0.04);
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
}

/* ======================================
   🌟 右键菜单毛玻璃化适配 (Profile.vue)
======================================== */
.context-menu {
  position: fixed;
  z-index: 99999 !important;
  border-radius: 8px;
  padding: 6px 0;
  min-width: 160px;
  font-size: 13px;

  /* ✨ 核心毛玻璃魔法，替代原本的实体背景色和边框 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--el-text-color-primary); /* 适配全局文字颜色 */
}

/* 悬浮时使用通透的半透明高光，并保留你原本的主题色文字高亮 */
.context-menu-item:hover {
  background-color: rgba(128, 128, 128, 0.1) !important;
  color: var(--el-color-primary);
}

/* ======================================
   🔥 名字与成就徽章的内联布局
======================================== */
.name-badge-row {
  display: flex;
  align-items: center;
  gap: 10px; /* 名字和徽章的间距 */
}

/* 尊贵的流金毛玻璃成就入口 */
.profile-medal-entry {
  font-size: 13px;
  font-weight: 700;
  color: #e6a23c;
  padding: 2px 10px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;

  /* 鎏金液态玻璃效果 */
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
  backdrop-filter: blur(8px);
  box-shadow: inset 0 0 8px rgba(255, 215, 0, 0.1);

  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 悬停时的呼吸发光放大感 */
.profile-medal-entry:hover {
  transform: translateY(-2px) scale(1.05);
  background: rgba(230, 162, 60, 0.2);
  border-color: rgba(230, 162, 60, 0.6);
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3), inset 0 0 10px rgba(255, 215, 0, 0.3);
  text-shadow: 0 0 8px rgba(230, 162, 60, 0.5);
}

/* 适配暗黑模式 */
html.dark .profile-medal-entry {
  background: rgba(255, 215, 0, 0.08);
  color: #ffd700;
}

/* ======================================
   8. 移动端响应式断点平滑回退
======================================== */
@media (max-width: 768px) {
  .bento-main-flow-grid {
    display: flex;
    flex-direction: column !important;
  }
  .bento-profile-capsule, .bento-scroll-box, .bento-book-comments-box, .bento-history-shelf {
    height: auto !important;
    max-height: 450px;
    margin-bottom: 16px;
  }
}
</style>

