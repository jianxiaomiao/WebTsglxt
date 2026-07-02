<script setup>
// ====================== 1. 依赖导入 ======================
// 🌟 导入 nextTick，保障热力图加载完成后自动滚动的时序正确
import { computed, inject, onMounted, ref, watch, nextTick } from 'vue'
import request from '../utils/request.js'
import { ElAvatar, ElButton, ElMessage, ElRate, ElTag, ElMessageBox, ElDivider, ElCard, ElImage, ElTooltip, ElSkeletonItem, ElRow, ElCol, ElSkeleton } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
// 🌟 1. 顶部别忘了引入图标
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

defineOptions({ name: 'UserProfile' })
// ====================== 2. 全局实例初始化 ======================
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isDark = inject("isDark")
const userNotes = ref([])
const targetUserReadHistory = ref([])
const noteTypeList = inject('noteTypeList', [])
const deptTypeList = inject('deptTypeList', [])
const targetUserCollections = ref([])
const isLoadingNotes = ref(false)
const loadUserCollections = inject('loadUserCollections')
const fetchUserNotes = inject('fetchUserNotes')

// ====================== 新增：目标用户 专属评论变量（组件私有，不会串数据） ======================
// TA的书籍评论
const targetUserBookComments = ref([])
// TA的论坛评论
const targetUserForumComments = ref([])
// 评论加载状态（可选，统一加载体验）
const isLoadingComment = ref(false)

// 分页获取用户笔记
const fetchTargetUserNotes = async (userId, isLoadMore = false) => {
  if (!userId || isLoadingNotes.value) return
  isLoadingNotes.value = true
  try {
    const res = await request.get('/user/note', { params: { userId } })
    if (res.code === 200) {
      const noteList = res.data?.list || res.data || []
      const notesWithBook = await Promise.all(
          noteList.map(async (note) => {
            const bookRes = await request.get('/book', { params: { isbn: note.isbn } })
            const book = bookRes.data[0] || {}
            return { ...note, book }
          })
      )
      userNotes.value = notesWithBook
    } else {
      userNotes.value = []
    }
  } catch (err) {
    console.error('笔记接口失败', err)
    userNotes.value = []
  } finally {
    isLoadingNotes.value = false
  }
}
// ====================== 新增：获取【目标用户】书籍评论 ======================
const fetchTargetUserBookComments = async (userId) => {
  if (!userId || isLoadingComment.value) return
  isLoadingComment.value = true
  try {
    const res = await request.get('/book/comment', {
      params: { userId } // 强制使用被查看的用户ID
    })
    if (res.code === 200) {
      targetUserBookComments.value = res.data?.list || res.data || []
    } else {
      targetUserBookComments.value = []
    }
  } catch (err) {
    console.error('获取目标用户书籍评论失败', err)
    targetUserBookComments.value = []
  } finally {
    isLoadingComment.value = false
  }
}

// ====================== 新增：获取【目标用户】论坛评论 ======================
const fetchTargetUserForumComments = async (userId) => {
  if (!userId || isLoadingComment.value) return
  isLoadingComment.value = true
  try {
    const res = await request.get('/user/comment', {
      params: { userId } // 强制使用被查看的用户ID
    })
    if (res.code === 200) {
      targetUserForumComments.value = res.data?.list || res.data || []
    } else {
      targetUserForumComments.value = []
    }
  } catch (err) {
    console.error('获取目标用户论坛评论失败', err)
    targetUserForumComments.value = []
  } finally {
    isLoadingComment.value = false
  }
}

// 获取指定userId的阅读历史
const fetchTargetUserReadHistory = async (userId) => {
  if (!userId) return
  try {
    const res = await request.get('/user/progress', { params: { userId } })
    const historyList = res.data || []

    const historyWithBook = historyList.map(progress => {
      const lastReadTime = progress.createTime
          ? new Date(progress.createTime).toLocaleString('zh-CN', {
            year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
          })
          : '暂无记录'
      return { ...progress, lastReadTime }
    })

    // 赋值给组件私有变量，不再污染全局
    targetUserReadHistory.value = historyWithBook
  } catch (err) {
    console.error('阅读历史接口404/失败', err)
    targetUserReadHistory.value = []
  }
}

// 加载指定用户收藏（调用你现成的loadUserCollectionsPage分页方法）
const fetchTargetUserCollections = async (userId) => {
  if (!userId) return
  // 第一页，每页10条
  const { list } = await loadUserCollectionsPage(userId, 1, 10)
  targetUserCollections.value = list
}

// 你原本的分页收藏工具函数保留不动
const loadUserCollectionsPage = async (userId, page = 1, pageSize = 10) => {
  if (!userId) return { list: [], total: 0 }
  try {
    console.log('【Basic分页收藏】请求参数：', { userId, page, pageSize })
    const res = await request.get('/user/collection', {
      params: { userId, page, pageSize }
    })

    if (res.code !== 200) return { list: [], total: 0 }
    const { list, total } = res.data
    console.log('【Basic分页收藏】返回数据：', list.length, '条，总条数：', total)

    const resultList = list
        .filter(item => item?.isbn)
        .map(item => ({ ...item, type: 'collection' }))

    console.log('【Basic分页收藏】组装完成：', resultList.length, '条')
    return { list: resultList, total }
  } catch (err) {
    console.error('【Basic分页收藏】加载失败', err)
    return { list: [], total: 0 }
  }
}

const fetchUserReadHistory = inject('fetchUserReadHistory')
const fetchUserForumComments = inject('fetchUserForumComments')
const fetchUserBookComments = inject('fetchUserBookComments')
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;
const formatMentionText = inject('formatMentionText')
const parseBookLinkToCard = inject('parseBookLinkToCard')
const expandedImages = ref({})

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
  // 防御性兜底，防止渲染瞬间报错
  if (!commentId || !text) return { text: text || '', cards: [] }
  if (!parsedForumContentCache.has(commentId)) {
    const pureText = extractPureText(text)
    const cards = extractBookCards(text)
    parsedForumContentCache.set(commentId, { text: pureText, cards })
  }
  return parsedForumContentCache.get(commentId) || { text: '', cards: [] }
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

// ====================== 3. 响应式变量定义 ======================
const loading = ref(true)
const targetUserId = ref(route.query.userId || '')
const currentUserId = inject("currentUserId")
const activeName = ref('first')
const friendList = inject('friendList')
const getUserInfo = inject('getUserInfo')
const targetUserInfo = ref({})
const targetUserBorrowList = ref([])
const readRecords = ref([])
const levelColors = ['#ebedf0','#9be9a8','#40c463','#30a14e','#216e39']

// 🏷️ 新增：热力图滚动容器的 DOM 引用
const heatmapScrollRef = ref(null)

// ====================== 4. 计算属性 ======================
const heatmapData = computed(() => {
  const dataMap = new Map()
  readRecords.value.forEach(r => { dataMap.set(r.readDate, r.readDuration || 0) })
  const today = new Date()
  const startDate = new Date(today)
  const dayOfWeek = startDate.getDay()
  const daysToSubtract = dayOfWeek === 0 ? 6 : dayOfWeek - 1
  startDate.setDate(today.getDate() - daysToSubtract - 52 * 7)

  const weeks = []
  let currentDate = new Date(startDate)
  for (let w = 0; w < 53; w++) {
    const week = []
    for (let d = 0; d < 7; d++) {
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
const currentUserInfo = computed(() => userStore.userInfo)

// ====================== 5. 通用工具函数 ======================
const getDurationLevel = (seconds) => {
  if (seconds === 0) return 0
  const minutes = seconds / 60
  if (minutes < 30) return 1
  if (minutes < 60) return 2
  if (minutes < 120) return 3
  return 4
}
const getNoteTypeName = inject('getNoteTypeName')
const getNoteTagType = inject('getNoteTagType')
const getReaderCommentStyle = inject('getReaderCommentStyle')
const formatTime = inject('formatDateTime')
const getChapterNumber = (id) => id?.includes('-') ? id.split('-')[1] : id || ''
const formatChapter = (num) => num?.includes('-') ? num.split('-')[1] : num || ''
const goToBookReader = inject('goToBookReader')

// 自动向右对齐滚动条的封装函数
const scrollToLatestHeatmap = () => {
  nextTick(() => {
    if (heatmapScrollRef.value) {
      heatmapScrollRef.value.scrollLeft = heatmapScrollRef.value.scrollWidth
    }
  })
}

// 跳转到阅读周报的方法
const gotoWeeklyReport = () => {
  router.push({
    path: '/weeklyReport',
    query: { userid: targetUserId.value, type: 'week' }
  })
}

// ====================== 6. 数据请求函数 ======================
const fetchTargetUserInfo = async () => {
  if (!targetUserId.value) return
  try {
    const res = await request.get('/user/info', { params: { userId: targetUserId.value } })
    targetUserInfo.value = res.data?.[0] || {}
  } catch (e) {
    console.error('获取用户信息失败', e)
    targetUserInfo.value = {}
  }
}
const fetchReadRecords = async () => {
  if (!targetUserId.value) return
  try {
    const today = new Date()
    const start = new Date(today.setFullYear(today.getFullYear() - 1))
    const format = d => d.toISOString().split('T')[0]
    const res = await request.get('/user/read/records', {
      params: { userId: targetUserId.value, startDate: format(start), endDate: format(new Date()) }
    })
    if (res.code === 200) {
      readRecords.value = res.data.map(r => ({ ...r, readDate: r.readDate.split(' ')[0] }))
    }
  } catch (e) { console.error('获取阅读记录失败', e) }
}
const getTargetUserBorrows = async () => {
  if (!targetUserId.value) return
  try {
    const borrowRes = await request.get('/book/borrow', { params: { userId: targetUserId.value } })
    const borrowList = borrowRes.data || []
    targetUserBorrowList.value = await Promise.all(
        borrowList.map(async (borrowItem) => {
          const bookRes = await request.get('/book', { params: { isbn: borrowItem.iSBN } })
          const bookInfo = bookRes.data[0] || {}
          return { ...borrowItem, book: bookInfo }
        })
    )
  } catch (err) {
    console.error('获取借阅信息失败', err)
    targetUserBorrowList.value = []
  }
}
const getFriendList = inject('getFriendList')

// ====================== 7. 关注/好友功能 ======================
const isFriend = (userId) => {
  return friendList.value.some(friend => String(friend.friendId) === String(userId))
}
const getButtonText = () => {
  const targetId = targetUserId.value
  const currentId = currentUserId.value
  if (String(targetId) === String(currentId)) { return '自己' }
  if (isFriend(targetId)) { return '已关注' }
  return '关注'
}
const getButtonType = () => {
  const targetId = targetUserId.value
  const currentId = currentUserId.value
  if (String(targetId) === String(currentId) || isFriend(targetId)) { return 'default' }
  return 'primary'
}
const isButtonDisabled = () => {
  const targetId = targetUserId.value
  const currentId = currentUserId.value
  return (String(targetId) === String(currentId) || isFriend(targetId))
}
const handleAddFriend = async (targetUserId, targetUserName) => {
  if (targetUserId === currentUserId.value) {
    ElMessage.warning('不能关注自己哦~')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
        `向 ${targetUserName} 发送好友申请`,
        '添加关注',
        {
          confirmButtonText: '发送申请',
          cancelButtonText: '取消',
          type: 'info',
          inputPlaceholder: '输入申请消息（选填）',
          inputValidator: (val) => {
            if (val && val.length > 50) return '消息不能超过50字'
            return true
          }
        }
    )
    const requestMsg = value?.trim() || `你好，我是${currentUserInfo.value?.name || '书友'}，想和你交个朋友~`
    const res = await request.post('/user/friend/request', null, {
      params: { action: 'send', fromUserId: currentUserId.value, toUserId: targetUserId, requestMsg }
    })
    if (res.code === 200) {
      ElMessage.success('关注申请已发送，等待对方同意')
    } else {
      ElMessage.error(res.msg || '发送失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('申请失败', err)
      ElMessage.error('发送失败，请稍后重试')
    }
  }
}

// ====================== 8. 私信功能 ======================
const handlePrivateChat = () => {
  if (String(targetUserId.value) === String(currentUserId.value)) {
    ElMessage.warning('不能给自己发私信哦~')
    return
  }
  router.push({ path: '/chat', query: { userId: targetUserId.value } })
}

// ====================== 9. 数据加载整合 ======================
const loadAllData = async () => {
  if (!targetUserId.value) return
  loading.value = true
  await fetchTargetUserInfo()
  await fetchReadRecords()
  await getTargetUserBorrows()

  // 替换收藏加载
  // await loadUserCollections?.(targetUserId.value) // 删除旧全局
  await fetchTargetUserCollections(targetUserId.value)

  await fetchTargetUserNotes(targetUserId.value)

  // 替换阅读历史加载
  // await fetchUserReadHistory?.(targetUserId.value) // 删除旧全局
  await fetchTargetUserReadHistory(targetUserId.value)

  // 之前改好的评论加载
  await fetchTargetUserForumComments(targetUserId.value)
  await fetchTargetUserBookComments(targetUserId.value)

  loading.value = false
  scrollToLatestHeatmap()
}

// ====================== 10. 监听 & 生命周期 ======================
watch(() => route.query.userId, async () => {
  targetUserId.value = route.query.userId || ''
  // 切换用户时全部清空旧数据
  targetUserBookComments.value = []
  targetUserForumComments.value = []
  targetUserReadHistory.value = []
  targetUserCollections.value = []
  await loadAllData()
}, { immediate: true })

onMounted(() => {
  loadAllData()
})
</script>

<template>
  <div style="padding: 20px; max-width: 1240px; margin: 0 auto; position: relative; z-index: 10; pointer-events: auto; min-height: calc(100vh - 190px);">

    <!-- 1. 顶部个人名片 -->
    <div class="main-card profile-header-card" style="background: var(--el-bg-color); border-radius: 12px; padding: 24px; border: 1px solid var(--el-border-color); margin-bottom: 20px;">
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div style="display: flex; align-items: center; gap:20px;">
            <el-skeleton-item variant="circle" size="64" />
            <div>
              <el-skeleton-item variant="h3" width="150px" />
              <el-skeleton-item variant="text" width="200px" style="margin-top: 8px;" />
              <el-skeleton-item variant="text" width="250px" style="margin-top: 8px;" />
            </div>
          </div>
        </template>

        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 15px;">
          <div style="display: flex; align-items: center; gap: 20px;">
            <el-avatar size="large" class="glass-avatar">
              {{ (targetUserInfo.name || '用户').slice(-2) }}
            </el-avatar>
            <div style="display: flex; flex-direction: column; gap: 6px; text-align: left !important;">
              <h2 style="margin: 0; font-size: 24px; font-weight: 600;">{{ targetUserInfo.name || '未知用户' }}</h2>
              <p style="margin: 0; font-size: 13px; color: var(--el-text-color-secondary); text-align: left !important;">
                ⏱️ 阅读时长：{{ Math.floor((targetUserInfo.read_time_long || 0) / 3600) }}时{{ Math.floor(((targetUserInfo.read_time_long || 0) % 3600) / 60) }}分
              </p>
              <p style="margin: 0; font-size: 13px; color: var(--el-text-color-regular); text-align: left !important; font-style: italic;">
                “ {{ targetUserInfo.bio || '这个人很懒，什么都没写~' }} ”
              </p>
            </div>
          </div>
          <div v-if="targetUserId !== currentUserId" style="display: flex; gap: 10px;">
            <el-button
                :type="getButtonType()"
                size="default"
                @click="handleAddFriend(targetUserId, targetUserInfo.name)"
                :disabled="isButtonDisabled()"
            >
              {{ getButtonText() }}
            </el-button>
            <el-button type="primary" size="default" @click="handlePrivateChat">
              私信
            </el-button>
          </div>
        </div>
      </el-skeleton>
    </div>

    <!-- 2. 便当盒网格核心布局区域 -->
    <el-row :gutter="20" style="display: flex; align-items: stretch; flex-wrap: wrap;">

      <!-- ================= 左侧纵向流 (占比 9/24) ================= -->
      <el-col :xs="24" :lg="9" style="display: flex; flex-direction: column; gap: 20px; margin-bottom: 20px;">

        <!-- 模块 A：TA的最近笔记 -->
        <div style="height: 380px; display: flex; flex-direction: column;">
          <el-skeleton :loading="loading" animated :rows="5" style="height: 100%;">
            <div class="main-card scroll-card" style="height: 100%; background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color); overflow-y:auto;">
              <h3 style="margin:0 0 15px; display: flex; align-items: center; gap: 6px;">📝 TA最近的笔记</h3>
              <div v-if="userNotes.length===0" style="text-align:center; padding:40px; color: var(--el-text-color-secondary);">暂无笔记</div>
              <div v-for="note in userNotes" :key="note.id" class="bento-inner-item" style="border-radius:8px; padding:15px; margin-bottom:10px; border: 2px solid var(--el-fill-color-dark); background: var(--el-fill-color-blank);">
                <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px;">
                  <el-tag size="small" :type="getNoteTagType(note.type)" effect="plain">
                    {{ getNoteTypeName(note.type) }}
                  </el-tag>
                  <span style="font-weight: 600; font-size: 13px;">《{{ note.bookName || '未知书籍' }}》</span>
                </div>
                <div style="white-space:pre-wrap; text-align:left; margin-bottom:8px; font-size: 13px; line-height: 1.5;">{{ note.text }}</div>
                <div
                    v-if="note.readerComment"
                    :style="{
                      whiteSpace: 'pre-wrap',
                      textAlign: 'left',
                      margin: '8px 0',
                      fontStyle: 'italic',
                      color: getReaderCommentStyle(note.type).color,
                      fontSize: '13px',
                      borderLeft: `3px solid ${getReaderCommentStyle(note.type).border}`,
                      paddingLeft: '8px'
                    }"
                >
                  批注：{{ note.readerComment }}
                </div>
                <div style="text-align:right; color:var(--el-text-color-secondary); font-size:12px">
                  第{{ getChapterNumber(note.chapterId) }}章 · {{ formatTime(note.createTime) }}
                </div>
              </div>
            </div>
          </el-skeleton>
        </div>

        <!-- 模块 B：TA的书籍评论 -->
        <div style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
          <el-skeleton :loading="loading" animated :rows="6" style="height: 100%;">
            <div class="main-card scroll-card" style="height: 100%; width:100%; background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color); overflow-y:auto;">
              <h3 style="margin:0 0 15px; display: flex; align-items: center; gap: 6px;">⭐ TA最近的书籍评论</h3>
              <div v-if="targetUserBookComments.length===0" style="text-align:center; padding:40px; color: var(--el-text-color-secondary);">暂无评论</div>
              <el-card
                  shadow="none"
                  :style="{
                    marginBottom: '12px',
                    background: isDark ? '#1D1E1F' : '#f9f9f9',
                    border: isDark ? '1px dashed #374151' : '1px dashed #dcdfe6',
                    borderRadius: '8px'
                  }"
                  v-for="item in targetUserBookComments"
                  :key="item.commentId"
              >
                <div style="display: flex; gap: 12px; align-items: flex-start;">
                  <div class="book-comment-cover" style="width: 70px; height: 100px; flex-shrink: 0; border-radius: 6px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.12);">
                    <img
                        :src="item.pictureName || '/default-book.png'"
                        alt="书籍封面"
                        style="width: 100%; height: 100%; object-fit: cover; cursor: pointer;"
                        @click="goToBookReader(item.isbn)"
                    />
                  </div>
                  <div style="flex: 1; min-width: 0;">
                    <div class="book-comment-header" style="display: flex; flex-direction: column; gap: 4px; margin-bottom: 6px;">
                      <span :style="{
                        fontSize: '14px',
                        fontWeight: '600',
                        color: isDark ? '#93c5fd' : '#409eff',
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'
                      }">{{ item.bookName || '未知书籍' }}</span>
                      <div style="display: flex; align-items: center; justify-content: space-between;">
                        <el-rate v-model="item.star" disabled size="small" active-color="#ffc107"/>
                        <span style="font-size: 11px; color: var(--el-text-color-secondary);">{{ formatTime(item.time) }}</span>
                      </div>
                    </div>
                    <el-divider style="margin: 8px 0; border-style: dashed;"></el-divider>
                    <div :style="{ whiteSpace: 'pre-wrap', lineHeight: '1.5', textAlign: 'left', color: isDark ? '#d1d5db' : '#444', fontSize: '13px' }">
                      {{ item.comment || '暂无评论内容' }}
                    </div>
                  </div>
                </div>
              </el-card>
            </div>
          </el-skeleton>
        </div>

      </el-col>

      <!-- ================= 右侧纵向流 (占比 15/24) ================= -->
      <el-col :xs="24" :lg="15" style="display: flex; flex-direction: column; gap: 20px; margin-bottom: 20px;">

        <!-- 模块 C：阅读贡献热力图 -->
        <div style="display: flex; flex-direction: column; gap: 20px;">
          <el-skeleton :loading="loading" animated>
            <template #template>
              <div class="main-card" style="padding:20px; border-radius: 12px;">
                <el-skeleton-item variant="rect" height="180px" />
              </div>
            </template>

            <div class="main-card" style="background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color);">
              <h3 style="margin:0 0 15px; display: flex; align-items: center; gap: 6px; cursor: pointer;" @click="gotoWeeklyReport">📊 阅读贡献度</h3>

              <!-- 🌟 结构优化：引入 heatmap-scroll-container 与精确对齐层 -->
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
                          placement="top" open-delay="1000" :show-arrow="false" effect="light"
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
          </el-skeleton>
        </div>

        <!-- 模块 D：多维书架展台 -->
        <div style="display: flex; flex-direction: column; gap: 20px;">

          <!-- D1: TA的借阅 -->
          <el-skeleton :loading="loading" animated>
            <template #template>
              <div class="main-card" style="padding:20px; border-radius: 12px;"><el-skeleton-item variant="rect" height="150px" /></div>
            </template>
            <div class="main-card" style="background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color);">
              <h3 style="margin:0 0 15px">📚 TA的借阅</h3>
              <div class="scroll-collection" style="display:flex; gap:15px; overflow-x:auto; padding-bottom:10px">
                <div v-if="targetUserBorrowList.length===0" style="text-align:center; padding:20px; width:100%; color: var(--el-text-color-secondary); font-size: 13px;">暂无借阅记录</div>
                <div v-for="item in targetUserBorrowList" :key="item.borrowId" class="bento-book-gallery-item" style="width:120px; flex-shrink:0; border-radius:8px; overflow:hidden; border: 1px solid var(--el-border-color); background: var(--el-fill-color-blank);">
                  <img :src="item.book?.pictureName || '/default-book.png'" style="width:100%; height:150px; object-fit:cover; cursor:pointer;" @click="goToBookReader(item.isbn)" />
                  <div style="padding:8px">
                    <div style="font-size:12px; font-weight:bold; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; text-align: left;">{{ item.bookname }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-skeleton>

          <!-- D2: TA的收藏 -->
          <el-skeleton :loading="loading" animated>
            <template #template>
              <div class="main-card" style="padding:20px; border-radius: 12px;"><el-skeleton-item variant="rect" height="150px" /></div>
            </template>
            <div class="main-card" style="background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color);">
              <h3 style="margin:0 0 15px">⭐ TA的收藏</h3>
              <div class="scroll-collection" style="display:flex; gap:15px; overflow-x:auto; padding-bottom:10px; max-width:100%;">
                <div v-if="targetUserCollections.length===0" style="text-align:center; padding:20px; width:100%; color: var(--el-text-color-secondary); font-size: 13px;">暂无收藏书籍</div>
                <div v-for="item in targetUserCollections" :key="item.collectionId" class="bento-book-gallery-item" style="width:120px; flex-shrink:0; border-radius:8px; overflow:hidden; border: 1px solid var(--el-border-color); background: var(--el-fill-color-blank);">
                  <img :src="item.pictureName || '/default-book.png'" style="width:100%; height:150px; object-fit:cover; cursor:pointer;" @click="goToBookReader(item.isbn)" />
                  <div style="padding:8px">
                    <div style="font-size:12px; font-weight:bold; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; text-align: left;">{{ item.bookname }}</div>
                    <div :style="{
                      fontSize: '10px',
                      color: isDark ? '#9ca3af' : '#999',
                      margin: '4px 0 0',
                      lineHeight: '1.3',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: '-webkit-box',
                      WebkitLineClamp: '2',
                      WebkitBoxOrient: 'vertical',
                      textAlign: 'left'
                    }">{{ item.information || '暂无简介' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </el-skeleton>

          <!-- D3: TA的阅读历史 -->
          <el-skeleton :loading="loading" animated>
            <template #template>
              <div class="main-card" style="padding:20px; border-radius: 12px;"><el-skeleton-item variant="rect" height="150px" /></div>
            </template>
            <div class="main-card" style="background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color);">
              <h3 style="margin:0 0 15px">📖 阅读历史</h3>
              <div class="scroll-collection" style="display:flex; gap:15px; overflow-x:auto; padding-bottom:10px">
                <div v-if="targetUserReadHistory.length===0" style="text-align:center; padding:20px; width:100%; color: var(--el-text-color-secondary); font-size: 13px;">暂无阅读历史</div>
                <div v-for="h in targetUserReadHistory" :key="h.id" class="bento-book-gallery-item" style="width:120px; flex-shrink:0; border-radius:8px; overflow:hidden; border: 1px solid var(--el-border-color); background: var(--el-fill-color-blank);">
                  <img :src="h.pictureName || '/default-book.png'" style="width:100%; height:150px; object-fit:cover; cursor:pointer;" @click="goToBookReader(h.isbn)" />
                  <div style="padding:8px">
                    <div style="font-size:12px; font-weight:bold; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; text-align: left;">{{ h.bookname }}</div>
                    <div style="font-size:10px; color:var(--el-text-color-secondary); text-align: left; margin-top: 4px;">读至：第{{ formatChapter(h.pageNum) }}章</div>
                  </div>
                </div>
              </div>
            </div>
          </el-skeleton>

        </div>

      </el-col>
    </el-row>
    <!-- 模块 E：TA的论坛评论动态 -->
    <div>
      <el-skeleton :loading="loading" animated :rows="6">
        <div class="main-card scroll-card" style="width:100%; background:var(--el-bg-color); border-radius:12px; padding:20px; border:1px solid var(--el-border-color); max-height:480px; overflow-y:auto;">
          <h3 style="margin:0 0 15px; display: flex; align-items: center; gap: 6px;">💬 TA最近的论坛评论</h3>
          <div v-if="targetUserForumComments.length===0" style="text-align:center; padding:40px; color: var(--el-text-color-secondary);">暂无评论</div>
          <el-card
              shadow="none"
              :style="{
                    marginBottom: '12px',
                    background: isDark ? '#1D1E1F' : '#ffffff',
                    border: isDark ? '1px dashed #374151' : '1px dashed #000000',
                    borderRadius: '10px'
                  }"
              v-for="item in targetUserForumComments"
              :key="item.commentId"
          >
            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; align-items: center;">
              <span :style="{ fontSize: '11px', color: isDark ? '#9ca3af' : '#999' }">发布于：{{ formatTime(item.commentTime) }}</span>
            </div>
            <el-divider style="margin: 8px 0; border-width: 1px; border-style: dotted;"></el-divider>
            <div>
              <div
                  :style="{
                        whiteSpace: 'pre-wrap',
                        lineHeight: '1.6',
                        marginBottom: '10px',
                        textAlign: 'left',
                        color: isDark ? '#d1d5db' : '#333',
                        fontSize: '13px'
                      }"
                  v-html="formatMentionText(getParsedForumContent(item?.commentId, item?.userComment).text)"
              />

              <div v-if="getParsedForumContent(item?.commentId, item?.userComment).cards.length > 0" class="forum-cards-area">
                <div v-if="getParsedForumContent(item?.commentId, item?.userComment).cards.length === 1" class="book-share-card-wrapper" style="margin-bottom: 12px;">
                  <div v-html="parseBookLinkToCard(getParsedForumContent(item?.commentId, item?.userComment).cards[0].link, false)"></div>
                </div>

                <div v-else class="multi-card-slider-wrapper">
                  <div class="slider-arrow left" @click.stop="scrollCards(item?.commentId, 'left')">
                    <el-icon><ArrowLeft /></el-icon>
                  </div>
                  <div class="multi-card-scroll-view custom-scroll-x" :id="'scroll-view-forum-' + item?.commentId">
                    <div
                        v-for="card in getParsedForumContent(item?.commentId, item?.userComment).cards"
                        :key="card.id"
                        class="slider-item"
                        v-html="parseBookLinkToCard(card.link, true)"
                    ></div>
                  </div>
                  <div class="slider-arrow right" @click.stop="scrollCards(item?.commentId, 'right')">
                    <el-icon><ArrowRight /></el-icon>
                  </div>
                </div>
              </div>

              <div v-if="item?.images && item.images.length > 0" class="comment-images">
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
          </el-card>
        </div>
      </el-skeleton>
    </div>
  </div>
</template>

<style scoped>
:root {
  --color-border: var(--el-border-color);
  --color-bg-page: var(--el-bg-color-page);
  --color-text-placeholder: var(--el-text-color-placeholder);
  --radius-sm: 3px;
  --radius-md: 8px;
  --transition-base: 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.main-card {
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  background: var(--el-bg-color);
}

.main-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08) !important;
}

.bento-book-gallery-item {
  transition: transform 0.2s ease;
}
.bento-book-gallery-item:hover {
  transform: scale(1.03);
}

/* 🌟 核心优化：完美隐藏卡片垂直滚动条且保留自然滚动能力 */
.scroll-card {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}
.scroll-card::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

/* 📊 贡献热力图精细化渲染与对齐 */
.heatmap-scroll-container {
  width: 100%;
  margin: 0 auto;
  overflow-x: auto;
  padding-bottom: 6px;
  scroll-behavior: smooth;
}
.heatmap-scroll-container::-webkit-scrollbar {
  height: 5px;
}
.heatmap-scroll-container::-webkit-scrollbar-thumb {
  background: var(--el-border-color-light);
  border-radius: 3px;
}

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

.scroll-collection {
  -ms-overflow-style: auto !important;
  scrollbar-width: auto !important;
}

.scroll-collection::-webkit-scrollbar {
  height: 5px !important;
  display: block !important;
}

.scroll-collection::-webkit-scrollbar-track {
  background: var(--el-fill-color-light);
  border-radius: 3px;
}

.scroll-collection::-webkit-scrollbar-thumb {
  background: var(--el-border-color-hover);
  border-radius: 3px;
}

.scroll-collection::-webkit-scrollbar-thumb:hover {
  background: var(--el-text-color-placeholder);
}

/* ======================================
   🌟 替换原有的评论图片与轮播图样式
======================================== */
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

/* 轮播悬浮箭头 (毛玻璃材质) */
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

@media (max-width: 768px) {
  .book-comment-cover {
    display: none !important;
  }
  .book-comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px !important;
  }
  .main-card:hover {
    transform: none;
  }
}

:deep(.dark-mode) {
  .scroll-collection::-webkit-scrollbar-track {
    background: #1f2937;
  }
  .scroll-collection::-webkit-scrollbar-thumb {
    background: #4a5568;
  }
}

/* ======================================
   🌟 1. 毛玻璃魔法：主便当盒卡片
======================================== */
.main-card {
  /* 使用 !important 强行覆盖你在 HTML 里的 style="background: ..." */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.main-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12) !important;
}

/* ======================================
   🖼️ 2. 毛玻璃魔法：画廊胶囊（借阅、收藏、历史）
======================================== */
.bento-book-gallery-item {
  /* 加上毛玻璃底色 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.bento-book-gallery-item:hover {
  transform: scale(1.05) !important;
  border-color: var(--el-color-primary) !important;
}

/* 重点！让画廊图片半透明，透出背后的环境光 */
.bento-book-gallery-item img {
  opacity: 0.85;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

/* 鼠标悬浮时，图片恢复高清并放大 */
.bento-book-gallery-item:hover img {
  transform: scale(1.08);
  opacity: 1;
}

/* ======================================
   💬 3. 笔记 & 评论：清爽无边框流
======================================== */
/* 针对 TA的书籍/论坛评论 (el-card) 以及刚刚加上类名的笔记 (bento-inner-item) */
.scroll-card .el-card,
.bento-inner-item {
  /* 强行抹除内联的 isDark 颜色和实体边框，透出大盒子的毛玻璃 */
  background: transparent !important;
  border: none !important;
  border-bottom: 1px solid var(--glass-border) !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  margin-bottom: 0 !important;
  padding-top: 12px;
  padding-bottom: 12px;
  transition: background-color 0.2s ease;
}

/* 悬浮时给一点淡淡的高光 */
.scroll-card .el-card:hover,
.bento-inner-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  transform: none !important;
}

/* 最后一个元素不需要底部的分割线 */
.scroll-card .el-card:last-child,
.bento-inner-item:last-child {
  border-bottom: none !important;
}

/* 顺便优化一下评论里的 el-divider 的虚线颜色，让它融入毛玻璃 */
.scroll-card .el-divider {
  border-top-color: var(--glass-border) !important;
}

</style>