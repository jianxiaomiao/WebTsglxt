<script setup>
// ==============================================
// 🔥 模块1：依赖导入（按类型分类排序）
// ==============================================
// Vue 组合式API
import { ref, watch, computed, provide, inject, onMounted, onUnmounted, nextTick } from 'vue'
// Vue Router
import { createRouter as $router, useRoute, useRouter } from 'vue-router'
// Element Plus 图标
import {
  HomeFilled, CollectionTag, Comment, User,
  Sunny, Moon, Search, ChatDotRound, Message
} from '@element-plus/icons-vue'
// Element Plus 组件
import {
  ElContainer, ElHeader, ElMain, ElMenu, ElMenuItem,
  ElIcon, ElAvatar, ElSelect, ElOption, ElInput, ElButton,
  ElDatePicker, ElInputNumber, ElDialog, ElMessageBox, ElMessage,
  ElAutocomplete
} from 'element-plus'
// 工具/状态管理/自定义组件
import request, { batch } from '../utils/request.js'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import { useReaderStore } from '../stores/readerStore'
import { useAchievementStore } from '../stores/achievementStore'
import GlobalContextMenu from '../components/GlobalContextMenu.vue'
import html2canvas from 'html2canvas'
// ==============================================
// 🔥 模块2：核心实例初始化 + 全局注入接收
// ==============================================
// Pinia 状态
const petStore = usePetStore()
const achievementStore = useAchievementStore()
const userStore = useUserStore()
// 使用方式：
console.log(userStore.userId)        // 用户ID
console.log(userStore.userType)      // 用户类型
console.log(userStore.userInfo)      // 完整信息
console.log(userStore.isLogin)       // 是否登录

// 路由实例
const route = useRoute()
const router = useRouter()

// 暗黑模式注入
const isDark = inject('isDark')
const toggleDarkMode = inject('toggleDarkMode')
// 调试日志
console.log('✅ BasicLayout 注入成功：', { isDark, toggleDarkMode })

// 右键菜单实例
const contextMenuRef = ref(null)

// 👇 新增：全局通用右键菜单状态（支持所有类型：书籍/笔记/评论/用户）
const currentRightClickItem = ref(null)    // 通用右键对象
const contextMenuType = ref('')            // 右键类型：book / note / userComment / bookComment / user
const currentRightClickBook = ref(null)
const currentRightClickBookComment = ref(null)
const currentRightClickForumComment = ref(null)
const currentRightClickNote = ref(null)
const currentRightClickUser = ref(null)

// 👇 新增：阅读页右键专用变量
const rightClickSelectedText = ref('') // 右键选中的文本
const rightClickNoteId = ref(null)     // 右键命中的笔记ID

provide('currentRightClickBook',currentRightClickBook)
provide('currentRightClickBookComment',currentRightClickBookComment)
provide('currentRightClickForumComment',currentRightClickForumComment)
provide('currentRightClickNote',currentRightClickNote)
provide('currentRightClickUser',currentRightClickUser)
provide('rightClickSelectedText',rightClickSelectedText)
provide('rightClickNoteId',rightClickNoteId)

// 👇 新增：注入通用右键状态（所有子页面使用）
provide('currentRightClickItem', currentRightClickItem)
provide('contextMenuType', contextMenuType)
// ==============================================
// 🔥 模块3：全局工具函数（分享/用户信息）
// ==============================================
// 分享到论坛
const shareToForum = (shareData) => {
  const { type, id, content, bookInfo, image } = shareData
  router.push({
    path: '/forum',
    query: {
      shareType: type,
      // 只保留核心：分享的文本内容（书籍短链接）
      content: encodeURIComponent(content)
    }
  })
}

const shareBookToForum = () => {
  if (!currentRightClickBook.value) return
  const book = currentRightClickBook.value

  const shareLink = generateBookShareLink(book)
  shareToForum({
    type: CARD_TYPES.BOOK,
    id: book.iSBN ,
    content: shareLink,
  })

  closeContextMenu()
}
provide('shareBookToForum',shareBookToForum)

// ===================== 替换原有 inject 代码，直接实现三个方法 =====================
// 2. 笔记分享
const shareNoteToForum = () => {
  // 判断右键选中的笔记是否存在
  if (!currentRightClickNote.value) return
  const note = currentRightClickNote.value

  // 生成笔记分享链接
  const shareLink = generateNoteShareLink(note)
  // 调用分享方法，严格遵循 {type: id} 格式
  shareToForum({
    type: CARD_TYPES.NOTE,
    id: note.collectionId, // 笔记的唯一ID，按你项目实际字段修改
    content: shareLink,
  })

  // 关闭右键菜单
  closeContextMenu()
}
provide('shareNoteToForum',shareNoteToForum)
// 3. 论坛评论分享
const shareForumCommentToForum = () => {
  // 判断右键选中的论坛评论是否存在
  if (!currentRightClickForumComment.value) return
  const forumComment = currentRightClickForumComment.value

  // 生成论坛评论分享链接
  const shareLink = generateUserCommentShareLink(forumComment)
  // 调用分享方法
  shareToForum({
    type: CARD_TYPES.USER_COMMENT,
    id: forumComment.commentId, // 论坛评论的唯一ID，按你项目实际字段修改
    content: shareLink,
  })

  closeContextMenu()
}
provide('shareForumCommentToForum',shareForumCommentToForum)
// 4. 书籍评论分享
const shareBookCommentToForum = () => {
  // 判断右键选中的书籍评论是否存在
  if (!currentRightClickBookComment.value) return
  const bookComment = currentRightClickBookComment.value

  // 生成书籍评论分享链接
  const shareLink = generateBookCommentShareLink(bookComment)
  // 调用分享方法
  shareToForum({
    type: CARD_TYPES.BOOK_COMMENT,
    id: bookComment.commentId, // 书籍评论的唯一ID，按你项目实际字段修改
    content: shareLink,
  })
  closeContextMenu()
}
provide('shareBookCommentToForum',shareBookCommentToForum)

const shareUserToForum = () => {
  // 判断右键选中的书籍评论是否存在
  if (!currentRightClickUser.value) return
  const user = currentRightClickUser.value

  // 生成书籍评论分享链接
  const shareLink = generateUserShareLink(user)
  // 调用分享方法
  shareToForum({
    type: CARD_TYPES.USER,
    id: user.userId, // 书籍评论的唯一ID，按你项目实际字段修改
    content: shareLink,
  })
  closeContextMenu()
}
provide('shareUserToForum',shareUserToForum)

const contextMenuPosition = ref({ x: 0, y: 0 })
provide('contextMenuPosition',contextMenuPosition)

const closeContextMenu = () => {
  showContextMenu.value = false
  // 清空所有右键状态
  currentRightClickItem.value = null
  currentRightClickBook.value = null
  currentRightClickBookComment.value = null
  currentRightClickUser.value = null
  currentRightClickForumComment.value = null
  currentRightClickNote.value = null
  contextMenuType.value = ''
}
provide('closeContextMenu',closeContextMenu)

const clearContextMenu = () =>{
  // 清空所有右键状态
  currentRightClickItem.value = null
  currentRightClickBook.value = null
  currentRightClickBookComment.value = null
  currentRightClickUser.value = null
  currentRightClickForumComment.value = null
  currentRightClickNote.value = null

  rightClickSelectedText.value = null
  contextMenuType.value = ''
}
provide('clearContextMenu',clearContextMenu)

const showContextMenu = ref(false)
provide('showContextMenu',showContextMenu)
// 生成分享内容
const generateShareContent = (shareType, shareData) => {
  const templates = {
    book: (data) => ({
      content: `📚 推荐书籍：《${data.bookName}》\n作者：${data.author || '未知'}\n\n${data.intro || '暂无简介'}\n\n推荐大家读一读~`,
      image: data.cover || '/default-book.png'
    }),
    text: (data) => ({
      content: `📖 书籍摘抄\n出自《${data.bookName}》第${data.chapterNumber}章\n——————\n摘抄内容：\n${data.text}`,
      image: ''
    }),
    note: (data) => ({
      content: `📝 我的读书笔记\n类型：${data.noteType || '默认'}\n原文：${data.text}\n我的批注：${data.comment || '暂无批注'}\n\n—— 《${data.bookName}》${data.chapter}`,
      image: ''
    }),
    image: (data) => ({
      content: data.content || '',
      image: data.imageUrl
    }),
    post: (data) => ({
      content: `🔄 转发帖子\n原作者：@${data.author}\n原帖内容：\n${data.content}\n\n——————\n我的评论：`,
      image: data.image || ''
    }),
    'forum-comment': (data) => ({
      content: `💬 我的论坛评论\n发表时间：${data.time}\n——————\n${data.content}`,
      image: ''
    }),
    'book-comment': (data) => ({
      content: `⭐ 我的书籍评论\n《${data.bookName}》 ${'⭐'.repeat(data.star || 3)}\n发表时间：${data.time}\n——————\n${data.content}`,
      image: ''
    })
  }
  return templates[shareType]?.(shareData) || { content: '', image: '' }
}

// ====================== 🔥 新增：4种卡片分享链接生成函数 ======================
// 🔥 重构：生成通用格式的书籍分享链接
const generateBookShareLink = (book) => {
  return `[{${CARD_TYPES.BOOK}:${book.iSBN}}]`
}
provide('generateBookShareLink', generateBookShareLink)

// 1. 生成书籍评论分享链接
const generateBookCommentShareLink = (comment) => {
  return `[{${CARD_TYPES.BOOK_COMMENT}:${comment.commentId}}]`
}
const generateBookCommentShareLinkById = (commentId) => {
  return `[{${CARD_TYPES.BOOK_COMMENT}:${commentId}}]`
}
provide('generateBookCommentShareLinkById',generateBookCommentShareLinkById)
provide('generateBookCommentShareLink', generateBookCommentShareLink)

// 2. 生成用户论坛评论分享链接
const generateUserCommentShareLink = (comment) => {
  return `[{${CARD_TYPES.USER_COMMENT}:${comment.commentId}}]`
}
provide('generateUserCommentShareLink', generateUserCommentShareLink)
const generateUserCommentShareLinkById = (commentId) => {
  return `[{${CARD_TYPES.USER_COMMENT}:${commentId}}]`
}
provide('generateUserCommentShareLinkById', generateUserCommentShareLinkById)

// 3. 生成用户信息分享链接
const generateUserShareLink = (user) => {
  return `[{${CARD_TYPES.USER}:${user.userId}}]`
}
provide('generateUserShareLink', generateUserShareLink)

const generateUserShareLinkById = (userId) => {
  return `[{${CARD_TYPES.USER}:${userId}}]`
}
provide('generateUserShareLinkById', generateUserShareLinkById)

// 4. 生成笔记分享链接
const generateNoteShareLink = (note) => {
  return `[{${CARD_TYPES.NOTE}:${note.id}}]`
}
provide('generateNoteShareLink', generateNoteShareLink)

// 获取用户信息
const getUserInfo = async (userId) => {
  try {
    const res = await request.get('/user/info', { params: { userId } })
    const userList = res.data || []
    const userData = userList[0] || {}
    return {
      userName: userData.name || '未知用户',
      ...userData
    }
  } catch (err) {
    console.error(`获取用户${userId}信息失败`, err)
    return { userName: '未知用户' }
  }
}

// 刷新用户信息
const refreshUserInfo = async (userId) => {
  try {
    const userInfo = await getUserInfo(userId)
    userName.value = userInfo.userName
  } catch (err) {
    console.error('刷新用户信息失败', err)
  }
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (err) {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    return true
  }
}
provide('copyToClipboard',copyToClipboard)

const copyBookShareLink = async () => {
  if (!currentRightClickBook.value) return

  const link = generateBookShareLink(currentRightClickBook.value)
  const success = await copyToClipboard(link)

  if (success) {
    ElMessage.success('✅ 复制成功！发送到微信/QQ自动生成精美卡片')
  } else {
    ElMessage.error('复制失败，请手动复制：' + link)
  }

  closeContextMenu()
}
provide('copyBookShareLink',copyBookShareLink)
// ==============================================
// 🔥 模块4：所有响应式变量（按功能分组）
// ==============================================
// 全局用户信息
const currentUserId = ref(userStore.userId || '')
const currentUserType = ref(userStore.userType || '')
provide('currentUserId', currentUserId)
provide('currentUserType', currentUserType)
const userName = ref(' 未知用户 ')

// 移动端适配
const isMobile = ref(false)
provide('isMobile',isMobile)
const drawerVisible = ref(false)

// 实时时间
const currentTime = ref('')
let timer = null

// 书籍搜索
const queryType = ref('isbn')
const searchKey = ref('')
const books = ref([])
const suggestList = ref([])
const isComposing = ref(false)
let suggestDebounceTimer = null
let searchDebounceTimer = null
let suggestAbortController = null
let searchAbortController = null

// 书籍榜单
const hotBooks = ref([])
const recommendBooks = ref([])

// 书籍类型
const bookTypeList = ref([])

// 添加书籍弹窗
const isAddBookShow = ref(false)
// 🔥 新增：标签列表
const allTagList = ref([])
provide('allTagList', allTagList)
// 🔥 扩展添加书籍表单，新增标签ID数组
const addBookForm = ref({
  Bookname: '',
  Author: '',
  ISBN: '',
  Publisher: '',
  PublishDate: new Date(),
  Type: '',
  all_book: 1,
  PictureName: '',
  Information: '',
  tagIds: [] // 多选标签ID
})

// 🔥 加载所有标签（下拉选项）
const loadAllTags = async () => {
  try {
    const res = await request.get('/book/tag')
    if (res.code === 200) {
      allTagList.value = res.data
    }
  } catch (e) {
    console.error('加载标签失败', e)
  }
}
const imgUrl = ref('/default-book.png')

// 论坛评论
const forumSearchUserId = ref('')
const forumSortType = ref('time')
// 🔥 新增：主评论分页状态
const mainCommentPage = ref(1) // 当前主评论页码
const mainCommentPageSize = ref(8) // 每页主评论条数
const mainCommentTotal = ref(0) // 主评论总条数
const isLoadingMainComments = ref(false) // 主评论加载中状态
const hasMoreMainComments = ref(true) // 是否还有更多主评论

// 🔥 主评论列表（结构：[{ comment: UserComment, subTotal: 子评论总数 }]）
const forumMainComments = ref([])

// 🔥 子评论分页状态（key: parentId, value: { list: [], pageNum: 1, total: 0, loading: false }）
const forumSubCommentState = ref({})


const forumComments = ref([])
let forumSearchDebounceTimer = null
// ✅ 新增：全局点赞记录（论坛复用）
const likedCommentIds = ref([])
provide('likedCommentIds', likedCommentIds)

// 用户搜索
const queryUserType = ref('userId')
const searchUserKey = ref('')
const users = ref([])

// 借阅信息
const borrowQueryType = ref('userId')
const borrowSearchKey = ref('')
const borrows = ref([])
const userBorrowList = ref([])

// 收藏/书架
const userCollections = ref([])

// 用户笔记/历史/评论
// ====================== 🔥 我的阅读笔记 - 分页状态（和论坛/书籍评论统一） ======================
const userNotes = ref([])
const notePage = ref(1)
const notePageSize = ref(10)
const noteTotal = ref(0)
const isLoadingNotes = ref(false)
const hasMoreNotes = ref(true)

const userReadHistory = ref([])

const userBookComments = ref([])
const noteTypeList = ref([])

// 全局类型
const userTypeList = ref([])
const deptTypeList = ref([])

// 消息未读
const chatUnreadCount = ref(0)
const messageUnreadCount = ref(0)

// ✅ 聚合接口未读状态
const unreadCount = ref(0)
const unreadNotifyCount = ref(0)
const unreadRequestCount = ref(0)

// ✅ 新增：全局消息/好友申请状态
const friendRequests = ref([])
const sentRequests = ref([])
const notifications = ref([])

// ✅ 新增：全局@艾特功能状态
const showMentionList = ref(false)
const filteredMentionUsers = ref([])
const selectedMentionIndex = ref(0)
const mentionDropdownPosition = ref({ x: 0, y: 0 })
const currentMentionInput = ref(null)
provide('currentMentionInput',currentMentionInput)
const currentMentionContent = ref('')
const mentionUpdateCallback = ref(null) // ✅ 新增：输入框更新回调函数

// ====================== 🔥 通用卡片系统：全局配置 ======================
// 支持的卡片类型（未来新增类型只需要在这里添加）
const CARD_TYPES = {
  BOOK: 'book',                // 书籍卡片
  BOOK_COMMENT: 'bookComment',// 书籍评论卡片
  USER_COMMENT: 'userComment',// 用户论坛评论卡片
  USER: 'user',                // 用户信息卡片
  NOTE: 'note'                 // 笔记卡片
}

// 通用卡片正则：匹配 [{类型}:ID] 格式
// 支持字母、数字、下划线、连字符作为ID
const cardReg = /\[{(\w+):([a-zA-Z0-9_-]+)}\]/g

// 注入全局，所有子组件共享
provide('CARD_TYPES', CARD_TYPES)
provide('cardReg', cardReg)
// ✅ 新增：全局书籍分享卡片状态
const isUpdatingBookCards = ref(false)
const previewBookCards = ref([]) // 输入框实时预览卡片
const bookShortLinkReg = /https?:\/\/[^\s/]+\/s\/([0-9a-zA-Z]+)/g
provide('bookShortLinkReg',bookShortLinkReg)
// 路由激活状态
const activeRoute = ref(route.name)

// ==============================================
// 🔥 模块5：计算属性
// ==============================================
const isAdmin = computed(() => {
  return currentUserType.value === 3
})

// ==============================================
// 🔥 模块6：业务函数（按功能分组）
// ==============================================
// ------------------------------
// 函数6.1：布局/时间相关
// ------------------------------
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}


/**
 * 万能时间格式化（兼容所有场景）
 * @param {String|Number} time - 时间戳(数字/字符串) / 日期字符串(2024-05-20 / 2024-05-20 12:00)
 * @param {Boolean} showTime - 是否显示时分，默认true
 * @returns {String} 格式化后的时间
 */
const formatDateTime = (time, showTime = true) => {
  // 1. 空值直接返回默认
  if (!time) return '暂无创建时间'

  let date
  // 2. 兼容：数字/字符串时间戳 → 转数字
  if (!isNaN(Number(time))) {
    date = new Date(Number(time))
  }
  // 3. 兼容：日期字符串（纯年月日 / 带时分秒）
  else {
    date = new Date(time)
  }

  // 4. 无效日期判断
  if (isNaN(date.getTime())) return '暂无创建时间'

  // 5. 统一格式化年月日
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dateStr = `${year}-${month}-${day}`

  // 6. 不需要时分 → 直接返回年月日
  if (!showTime) return dateStr

  // 7. 需要时分 → 拼接返回
  const hour = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${dateStr} ${hour}时${min}分`
}
provide('formatDateTime',formatDateTime)

// 1. 新增：控制导航胶囊展开/折叠的状态
const isNavExpanded = ref(false)

// 2. 找到你的 updateTime 函数，直接替换为这个（只显示 HH:mm）
const updateTime = () => {
  const now = new Date()
  const hour = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  currentTime.value = `${hour}:${min}`
}

// ------------------------------
// 函数6.2：书籍相关
// ------------------------------
// ====================== 🔥 新增：推荐书籍无限滚动分页变量 ======================
const loadingRecommend = ref(false)        // 加载状态
const recommendPageNum = ref(1)           // 当前页码
const recommendPageSize = ref(4)         // 每页条数
const hasMoreRecommend = ref(true)        // 是否有更多数据
provide('loadingRecommend', loadingRecommend)
provide('recommendPageNum', recommendPageNum)
provide('recommendPageSize', recommendPageSize)
provide('hasMoreRecommend', hasMoreRecommend)
// 加载热门书籍
const loadHotBooks = async () => {
  try {
    const res = await request.get('/book', { params: { number: '24' } })
    hotBooks.value = res.data || []
  } catch (err) {
    console.error('加载热门书籍失败', err)
    hotBooks.value = []
  }
}

// 🔥 改造：推荐书籍分页加载（无限滚动核心）
const loadRecommendBooks = async () => {
  if (!hasMoreRecommend.value || loadingRecommend.value) return

  loadingRecommend.value = true
  try {
    const res = await request.get('/book', {
      params: {
        type: 'recommend',
        pageNum: recommendPageNum.value,
        pageSize: recommendPageSize.value
      }
    })

    const newBooks = res.data?.list || []

    // ✨ 核心注入：为每一本新加载进来的书籍，初始化塔罗牌“未翻开”状态
    const processedBooks = newBooks.map(book => ({
      ...book,
      isFlipped: false // 初始全部背面朝上
    }))

    // 数据追加
    if (processedBooks.length > 0) {
      recommendBooks.value.push(...processedBooks)
    }

    // 判断是否还有更多数据
    if (newBooks.length < recommendPageSize.value) {
      hasMoreRecommend.value = false
    }

    // 页码+1
    recommendPageNum.value++

  } catch (err) {
    console.error('加载推荐书籍失败', err)
    ElMessage.error('推荐书籍加载失败')
  } finally {
    loadingRecommend.value = false
  }
}
provide('loadRecommendBooks', loadRecommendBooks)

// ====================== 公共方法：获取书籍详情（接收 ISBN 参数） ======================
const getBookDetail = async (bookIsbn) => {
  // 1. 接收传入的 ISBN，不再用局部变量
  if (!bookIsbn) {
    ElMessage.warning('书籍ISBN不能为空')
    router.push('/home')
    return null
  }

  try {
    const res = await request.get('/book', { params: { isbn: bookIsbn } })
    if (res.code === 200 && res.data.length > 0) {
      // 公共函数只返回数据，不赋值给局部变量，由调用页面自己处理
      return res.data[0]
    } else {
      ElMessage.warning('未找到该书籍信息')
      router.push('/home')
      return null
    }
  } catch (err) {
    console.error('获取书籍详情失败：', err)
    ElMessage.error('加载书籍信息失败')
    return null
  }
}
provide('getBookDetail', getBookDetail)

// 获取书籍类型
const fetchBookTypes = async () => {
  try {
    const res = await request.get('/book/type')
    if (res.code === 200) {
      bookTypeList.value = res.data
    }
  } catch (error) {
    ElMessage.error('书籍类型加载失败')
    console.error('获取书籍类型失败：', error)
  }
}

// 搜索书籍
const searchBooks = async () => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(async () => {
    const keyword = searchKey.value.trim()
    if (!keyword) {
      books.value = []
      return
    }
    try {
      if (searchAbortController) searchAbortController.abort()
      searchAbortController = new AbortController()
      const params = {}
      if (queryType.value === 'isbn') params.isbn = keyword
      else if (queryType.value === 'bookName') params.name = keyword
      const res = await request.get('/book', { params, signal: searchAbortController.signal })
      books.value = res.data || []
      // ====================== 🔥 核心：搜索无结果 → 弹出小宠物确认框 ======================
      if (books.value.length === 0) {
        petStore.addConfirmMessage(
            "是否需要全网搜索" + keyword + "?",
            async () => {
              // ✅ 点击【是】：调用爬虫接口
              try {
                const res = await request.post('/book', {}, {
                  params: {
                    action: 'crawl',
                    bookName: keyword
                  }
                })
                // 显示思考气泡
                const thinkingId = petStore.addMessage('正在全网搜索书籍中', petStore.MESSAGE_TYPES.THINKING)
                if (res.code === 200) {
                  petStore.addMessage("😺书籍已入库，刷新获取最新书籍")
                } else {
                  petStore.addMessage("😿全网未找到书籍......")
                }
                petStore.removeMessage(thinkingId)
              } catch (e) {
                petStore.addMessage("😿全网未找到书籍......")
              }
            },
            () => {
              // ❌ 点击【否】：不做操作
              petStore.addMessage("好的~已取消搜索")
            }
        )
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('正式搜索失败', err)
        books.value = []
      }
    }
  }, 300)
}

// 联想搜索
const fetchSuggestions = (queryString, callback) => {
  if (isComposing.value) {
    callback([])
    return
  }
  const keyword = queryString.trim()
  if (!keyword) {
    suggestList.value = []
    callback([])
    return
  }
  if (suggestDebounceTimer) clearTimeout(suggestDebounceTimer)
  suggestDebounceTimer = setTimeout(async () => {
    try {
      if (suggestAbortController) suggestAbortController.abort()
      suggestAbortController = new AbortController()
      const res = await request.get('/book', {
        params: { suggest: true, keyword: keyword, searchType: queryType.value },
        signal: suggestAbortController.signal
      })
      suggestList.value = res.data || []
      callback(suggestList.value)
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('联想搜索失败', err)
        callback([])
      }
    }
  }, 100)
}

// 选中联想项
const handleSelectSuggest = (item) => {
  if (queryType.value === 'bookName') searchKey.value = item.bookname || ''
  else searchKey.value = item.iSBN || item.isbn || ''
  searchBooks()
}

// 中文输入法事件
const handleCompositionStart = () => { isComposing.value = true }
const handleCompositionEnd = () => {
  isComposing.value = false
  if (searchKey.value.trim()) {
    fetchSuggestions(searchKey.value, (list) => { suggestList.value = list })
  }
}

// 添加书籍表单校验
const checkAddBookForm = () => {
  if (!addBookForm.value.Bookname.trim()) { ElMessage.error('书籍名称不能为空！'); return false }
  if (!addBookForm.value.ISBN.trim()) { ElMessage.error('ISBN不能为空！'); return false }
  if (!addBookForm.value.Author.trim()) { ElMessage.error('作者不能为空！'); return false }
  if (!addBookForm.value.Publisher.trim()) { ElMessage.error('出版社不能为空！'); return false }
  if (addBookForm.value.all_book <= 0) { ElMessage.error('总量必须大于0！'); return false }
  if (addBookForm.value.Information.trim().length > 1000) { ElMessage.error('书籍信息不能超过1000字！'); return false }

  const charReg = /^[\u4e00-\u9fa5a-zA-Z0-9]+$/
  if (!charReg.test(addBookForm.value.Bookname.trim())) { ElMessage.error('书籍名称仅允许输入汉字、英文或数字！'); return false }
  if (!charReg.test(addBookForm.value.Author.trim())) { ElMessage.error('作者仅允许输入汉字、英文或数字！'); return false }
  if (!charReg.test(addBookForm.value.Publisher.trim())) { ElMessage.error('出版社仅允许输入汉字、英文或数字！'); return false }

  const publishDate = new Date(addBookForm.value.PublishDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  publishDate.setHours(0, 0, 0, 0)
  if (publishDate > today) { ElMessage.error(`出版日期不能超过当前日期（${today.toLocaleDateString()}）！`); return false }
  return true
}

// 过滤特殊字符
const filterSpecialChar = (field) => {
  const reg = /[^\u4e00-\u9fa5a-zA-Z0-9]/g
  addBookForm.value[field] = addBookForm.value[field].replace(reg, '')
}
// 过滤特殊字符
const filterSpecialCharNormal = (str) => {
  if (!str) return ''
  const emojiReg = /[\uD83C-\uDBFF\uDC00-\uDFFF]+/g;
  const specialCharReg = /[^\u4e00-\u9fa5a-zA-Z0-9]/g;
  let result = str.replace(emojiReg, '');
  result = result.replace(specialCharReg, '');
  return result;
};
provide('filterSpecialCharNormal',filterSpecialCharNormal)

// 有效内容长度计算
const getValidContentLength = (content) => {
  const validContent = content.replace(/[\n\r\s]/g, '')
  return validContent.length
}
provide('getValidContentLength',getValidContentLength)

// 跳转章节阅读页
const goToBookReader = (iSBN) => {
  router.push({
    path: '/book/reader',
    query: {
      isbn: iSBN,
      number: 1,
      userid: currentUserId.value
    }
  })
}
provide('goToBookReader',goToBookReader)
// 图片预览更新
const handlePictureChange = () => {
  imgUrl.value = addBookForm.value.PictureName || '/default-book.png'
}

// 打开添加书籍弹窗
const openAddBookDialog = () => {
  if (isAdmin.value) {
    addBookForm.value = {
      Bookname: '', Author: '', ISBN: '', Publisher: '', PublishDate: new Date(),
      Type: '', all_book: 1, PictureName: '', Information: '',
      tagIds: [] // 🔥 清空标签
    }
    imgUrl.value = '/default-book.png'
    isAddBookShow.value = true
  } else {
  }
}

// 提交添加书籍
const submitAddBook = async () => {
  if (!checkAddBookForm()) return
  const bookData = { ...addBookForm.value, now_book: addBookForm.value.all_book }

  try {
    // 1. 先添加书籍
    const res = await request.post('/book', bookData)
    if (res.code === 200) {
      // 🔥 2. 批量绑定标签（适配后端单条新增接口）
      const tagIds = addBookForm.value.tagIds
      const isbn = bookData.ISBN
      if (tagIds.length > 0) {
        for (const tagId of tagIds) {
          await request.post('/book/tag/relation', {
            isbn: isbn,
            tagId: tagId
          })
        }
      }

      ElMessage.success('书籍+标签添加成功！')
      isAddBookShow.value = false
      // 清空标签
      addBookForm.value.tagIds = []

      if (route.path === '/home') {
        await searchBooks()
        await loadHotBooks()
        await loadRecommendBooks()
      }
    } else {
      ElMessage.error(res.msg || '书籍添加失败')
    }
  } catch (err) {
    ElMessage.error('添加书籍出错啦~')
    console.error('添加书籍失败：', err)
  }
}

// ------------------------------
// 函数6.3：论坛评论相关
// ------------------------------
// ======================================
// 🔥 新增：主评论分页加载函数
// ======================================
const getSubTotal = (commentId) => {
  const item = forumMainComments.value.find(i => i.comment.commentId === commentId)
  return item ? item.subTotal : 0
}
const loadMainComments = async (isLoadMore = false) => {
  if (isLoadingMainComments.value || !hasMoreMainComments.value) return
  isLoadingMainComments.value = true

  try {
    const params = {
      action: 'mainPage',
      pageNum: mainCommentPage.value,
      pageSize: mainCommentPageSize.value,
      sort: forumSortType.value
    }

    const res = await request.get('/user/comment', { params })
    if (res.code === 200) {
      const { total, list, totalPages } = res.data
      mainCommentTotal.value = total
      hasMoreMainComments.value = mainCommentPage.value < totalPages

      // 🔥 直接使用数据，无需请求用户信息！
      const processedList = list

      if (isLoadMore) {
        // 加载更多：追加数据
        forumMainComments.value.push(...processedList)
        await nextTick()
        const container = document.querySelector('.el-scrollbar__wrap')
        if (container) {
          container.scrollTop += 300
        }
      } else {
        // 重新加载：替换数据
        forumMainComments.value = processedList
      }
    } else {
      ElMessage.error(res.msg || '加载评论失败')
    }
  } catch (err) {
    console.error('加载主评论失败', err)
    ElMessage.error('加载评论失败，请稍后再试')
  } finally {
    isLoadingMainComments.value = false
  }
}

// 🔥 加载更多主评论（无限滚动用）
const loadMoreMainComments = async () => {
  if (!hasMoreMainComments.value || isLoadingMainComments.value) return
  mainCommentPage.value += 1
  await loadMainComments(true)
}

// ======================================
// 🔥 新增：子评论分页加载函数
// ======================================
const loadSubComments = async (parentId, isLoadMore = false) => {
  // 初始化子评论状态
  if (!forumSubCommentState.value[parentId]) {
    forumSubCommentState.value[parentId] = {
      list: [],
      pageNum: 1,
      total: 0,
      loading: false
    }
  }

  const state = forumSubCommentState.value[parentId]
  if (state.loading) return
  state.loading = true

  try {
    const params = {
      action: 'subPage',
      parentId: parentId,
      pageNum: state.pageNum,
      pageSize: 3 // 子评论默认每页3条
    }

    const res = await request.get('/user/comment', { params })
    if (res.code === 200) {
      const { total, list } = res.data
      state.total = total

      // 🔥 直接使用数据，无需请求用户信息！
      const processedList = list

      if (isLoadMore) {
        state.list.push(...processedList)
      } else {
        state.list = processedList
      }
    } else {
      ElMessage.error(res.msg || '加载回复失败')
    }
  } catch (err) {
    console.error('加载子评论失败', err)
    ElMessage.error('加载回复失败，请稍后再试')
  } finally {
    state.loading = false
  }
}

// 🔥 加载更多子评论
const loadMoreSubComments = async (parentId) => {
  const state = forumSubCommentState.value[parentId]
  if (!state || state.loading) return
  state.pageNum += 1
  await loadSubComments(parentId, true)
}

// 🔥 折叠子评论
const collapseSubComments = (parentId) => {
  if (forumSubCommentState.value[parentId]) {
    forumSubCommentState.value[parentId].list = []
    forumSubCommentState.value[parentId].pageNum = 1
  }
}

// ======================================
// 修改原有排序切换函数
// ======================================
const toggleForumSort = (type) => {
  if (['time', 'prefer'].includes(type)) {
    forumSortType.value = type
    // 重置分页状态
    mainCommentPage.value = 1
    hasMoreMainComments.value = true
    forumMainComments.value = []
    forumSubCommentState.value = {} // 清空所有子评论
    forumSearchComments()
  }
}

// ======================================
// 重写 forumSearchComments 函数
// ======================================
const forumSearchComments = async () => {
  if (forumSearchDebounceTimer) clearTimeout(forumSearchDebounceTimer)
  forumSearchDebounceTimer = setTimeout(async () => {
    try {
      const params = {}
      if (forumSearchUserId.value) params.userId = forumSearchUserId.value
      if (forumSortType.value) params.sort = forumSortType.value

      if (params.userId) {
        // 保留原有用户评论查询逻辑不变
        const normalRes = await request.get('/user/comment', { params })
        // 直接赋值，无需额外请求用户信息
        forumComments.value = normalRes.data || []
      } else {
        // 🔥 替换原有tree接口，使用新的分页接口
        // 重置分页状态
        mainCommentPage.value = 1
        hasMoreMainComments.value = true
        forumMainComments.value = []
        forumSubCommentState.value = {}
        await loadMainComments(false)
      }
    } catch (err) {
      console.error('搜索评论失败', err)
      forumComments.value = []
      forumMainComments.value = []
      forumSubCommentState.value = {}
    }
  }, 300)
}

const getUserLikedComments = async () => {
  if (!currentUserId.value) return;
  try {
    const res = await request.get('/user/comment/like', {
      params: { userId: currentUserId.value }
    });
    if (res.code === 200) {
      likedCommentIds.value = res.data || [];
      console.log('✅ 全局点赞记录更新完成', likedCommentIds.value)
    }
  } catch (err) {
    console.error('获取点赞记录失败', err);
  }
}
provide('getUserLikedComments',getUserLikedComments)
// ------------------------------
// 函数6.4：用户搜索相关
// ------------------------------
const searchUsers = async () => {
  try {
    const params = {}
    if (queryUserType.value === 'userId') params.userId = searchUserKey.value
    else if (queryUserType.value === 'name') params.name = searchUserKey.value
    const res = await request.get('/user/info', { params })
    users.value = res.data || []
  } catch (err) {
    console.error('搜索用户失败', err)
  }
}

// ------------------------------
// 函数6.5：借阅信息相关
// ------------------------------
const searchBorrows = async () => {
  try {
    const params = {}
    if (borrowQueryType.value === 'userId' && borrowSearchKey.value) params.userId = borrowSearchKey.value
    else if (borrowQueryType.value === 'isbn' && borrowSearchKey.value) params.isbn = borrowSearchKey.value
    const res = await request.get('/book/borrow', { params })
    borrows.value = res.data || []
  } catch (err) {
    console.error('搜索借阅记录失败', err)
  }
}

const getuserBorrows = async () => {
  try {
    const borrowRes = await request.get('/book/borrow', { params: { userId: currentUserId.value } })
    // 🔥 直接赋值，无需请求书籍信息
    userBorrowList.value = borrowRes.data || []
  } catch (err) {
    console.error('获取借阅信息失败', err)
    ElMessage.error('加载借阅信息失败')
  }
}

// ------------------------------
// 函数6.6：收藏/书架相关
// ------------------------------
// ✅ 给 BasicLayout 里的旧函数加兼容判断
const loadUserCollections = async (userId) => {
  if (!userId) return
  try {
    const res = await request.get('/user/collection', { params: { userId } })
    // 🔥 最小化修改：兼容分页对象和纯数组两种返回格式
    const collectionList = res.data?.list || res.data || []
    // 🔥 优化：直接赋值，删除循环请求书籍逻辑，仅添加type字段
    const fullCollections = collectionList
        .filter(item => item?.isbn) // 过滤无效数据
        .map(item => ({ ...item, type: 'collection' }))

    userCollections.value = fullCollections
    console.log('✅ 全局收藏列表更新完成', userCollections.value)
  } catch (err) {
    console.error('加载收藏列表失败', err)
    userCollections.value = []
  }
}

// --------------- 🔥 新增：书架专用 - 分页加载收藏方法 ---------------
const loadUserCollectionsPage = async (userId, page = 1, pageSize = 10) => {
  if (!userId) return { list: [], total: 0 }
  try {
    console.log('【Basic分页收藏】请求参数：', { userId, page, pageSize })
    // 调用后端分页接口
    const res = await request.get('/user/collection', {
      params: { userId, page, pageSize }
    })

    if (res.code !== 200) return { list: [], total: 0 }
    const { list, total } = res.data
    console.log('【Basic分页收藏】返回数据：', list.length, '条，总条数：', total)

    // 🔥 优化：直接组装，删除所有书籍请求逻辑
    const resultList = list
        .filter(item => item?.isbn) // 过滤无效数据
        .map(item => ({ ...item, type: 'collection' }))

    console.log('【Basic分页收藏】组装完成：', resultList.length, '条')
    return { list: resultList, total }
  } catch (err) {
    console.error('【Basic分页收藏】加载失败', err)
    return { list: [], total: 0 }
  }
}

const handleCollect = async (book) => {
  try {
    await request.post('/user/collection', { userId: currentUserId.value, isbn: book.iSBN })
    await loadUserCollections(currentUserId.value)
    ElMessage.success('收藏成功！')
    achievementStore.checkAchievements()
  } catch (err) {
    console.error('收藏失败', err)
    ElMessage.error('收藏失败，请稍后再试')
  }
}

const gotoBookDetail = (isbn) => {
  router.push({
    path: '/book/detail',
    query: { isbn: isbn, userid: currentUserId.value }
  })
}
provide('gotoBookDetail',gotoBookDetail)


// ✅ 新增：从书籍评论跳转到书籍详情
window.gotoBookDetailFromComment = (commentId) => {
  request.get('/book/comment', { params: { commentId } }).then(res => {
    if (res.code === 200 && res.data.length) {
      router.push({ path: '/book/detail', query: { isbn: res.data[0].iSBN } })
    }
  })
}

// ✅ 原有：用户主页跳转（不变）
window.gotoUserProfile = (userId) => {
  router.push('/user/profile?userId=' + userId)
}

const getChapterNumber = (chapterId) => {
  if (!chapterId) return ''
  const parts = chapterId.split('-')
  return parts.length > 1 ? parts[1] : chapterId
}
// ✅ 新增：跳转到笔记阅读页
window.gotoNoteReader = (noteId) => {
  request.get('/user/note', { params: { id: noteId } }).then(res => {
    if (res.code === 200 && res.data.length) {
      const note = res.data[0]
      router.push({
        path: '/book/reader',
        query: {
          isbn: note.isbn,
          number: getChapterNumber(note.chapterId),
          userid: currentUserId.value
        }
      })
    }
  })
}
// ------------------------------
// 函数6.7：笔记/阅读历史相关
// ------------------------------
const fetchNoteTypes = async () => {
  try {
    const res = await request.get('/user/note/type')
    if (res.code === 200) noteTypeList.value = res.data || []
  } catch (err) { console.error('获取笔记类型失败', err) }
}

// 🔥 分页获取用户笔记（对接后端分页接口）
const fetchUserNotes = async (userId, isLoadMore = false) => {
  // 🔥 加这行，控制台看是否是目标用户ID（240950200）
  console.log("获取笔记的用户ID：", userId)
  if (!userId || isLoadingNotes.value || !hasMoreNotes.value) return
  isLoadingNotes.value = true

  try {
    const res = await request.get('/user/note', {
      params: {
        userId,
        pageNum: notePage.value,
        pageSize: notePageSize.value
      }
    })

    if (res.code === 200) {
      const { total, list, totalPages } = res.data
      noteTotal.value = total
      hasMoreNotes.value = notePage.value < totalPages

      // 加载更多：追加；刷新：替换
      if (isLoadMore) {
        userNotes.value.push(...list)
      } else {
        userNotes.value = list

      }
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

// 🔥 无限滚动加载更多笔记
const loadMoreUserNotes = async (userId) => {
  if (!hasMoreNotes.value || isLoadingNotes.value) return
  notePage.value += 1
  await fetchUserNotes(userId, true)
}

// 🔥 重置笔记分页状态
const resetUserNotes = () => {
  notePage.value = 1
  hasMoreNotes.value = true
  userNotes.value = []
}

const fetchUserReadHistory = async (userId) => {
  if (!userId) return
  try {
    const res = await request.get('/user/progress', { params: { userId } })
    const historyList = res.data || []

    // 🔥 直接处理时间格式化，无需请求书籍
    const historyWithBook = historyList.map(progress => {
      const lastReadTime = progress.createTime
          ? new Date(progress.createTime).toLocaleString('zh-CN', {
            year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
          })
          : '暂无记录'
      // 后端已返回书名和封面，直接使用
      return { ...progress, lastReadTime }
    })

    userReadHistory.value = historyWithBook
  } catch (err) {
    console.error('阅读历史接口404/失败', err)
  }
}

const getNoteTypeName = (typeId) => {
  if (!typeId || !noteTypeList.value?.length) return '默认'
  const type = noteTypeList.value.find(t => t.id === typeId)
  return type?.userTextColType || '默认'
}
provide('getNoteTypeName',getNoteTypeName)

const getNoteTagType = (typeId) => {
  const tagTypeMap = {
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'warning',
    5: 'danger'
  };
  return tagTypeMap[typeId] || 'info';
};
provide('getNoteTagType',getNoteTagType)

const getReaderCommentStyle = (typeId) => {
  const styleMap = {
    1: { color: '#9ca3af', border: '#d1d5db' },
    2: { color: '#3b82f6', border: '#60a5fa' },
    3: { color: '#10b981', border: '#34d399' },
    4: { color: '#f59e0b', border: '#fbbf24' },
    5: { color: '#ef4444', border: '#f87171' }
  };
  return styleMap[typeId] || styleMap[1];
};
provide('getReaderCommentStyle',getReaderCommentStyle)

// ====================== 🔥 我的论坛评论 - 分页状态 ======================
const userForumComments = ref([])
const forumCommentPage = ref(1)
const forumCommentPageSize = ref(6)
const forumCommentTotal = ref(0)
const isLoadingForumComments = ref(false)
const hasMoreForumComments = ref(true)

// 🔥 分页获取我的论坛评论
const fetchUserForumComments = async (userId, isLoadMore = false) => {
  if (isLoadingForumComments.value || !hasMoreForumComments.value) return
  isLoadingForumComments.value = true

  try {
    const res = await request.get('/user/comment', {
      params: {
        userId,
        pageNum: forumCommentPage.value,
        pageSize: forumCommentPageSize.value
      }
    })

    if (res.code === 200) {
      const { total, list, totalPages } = res.data
      forumCommentTotal.value = total
      hasMoreForumComments.value = forumCommentPage.value < totalPages

      if (isLoadMore) {
        userForumComments.value.push(...list)
      } else {
        userForumComments.value = list
      }
    } else {
      userForumComments.value = []
    }
  } catch (e) {
    console.error('获取论坛评论失败', e)
    userForumComments.value = []
  } finally {
    isLoadingForumComments.value = false
  }
}

// 🔥 加载更多（无限滚动用）
const loadMoreUserForumComments = async (userId) => {
  if (!hasMoreForumComments.value || isLoadingForumComments.value) return
  forumCommentPage.value += 1
  await fetchUserForumComments(userId, true)
}

// 🔥 重置分页
const resetUserForumComments = () => {
  forumCommentPage.value = 1
  hasMoreForumComments.value = true
  userForumComments.value = []
}

// ====================== 🔥 新增：书籍评论分页状态 ======================
const bookCommentPage = ref(1)
const bookCommentPageSize = ref(6)
const bookCommentTotal = ref(0)
const isLoadingBookComments = ref(false)
const hasMoreBookComments = ref(true)


// ====================== 🔥 重写：分页获取用户书籍评论 ======================
const fetchUserBookComments = async (userId, isLoadMore = false) => {
  if (isLoadingBookComments.value || !hasMoreBookComments.value) return
  isLoadingBookComments.value = true

  try {
    const res = await request.get('/book/comment', {
      params: {
        userId,
        pageNum: bookCommentPage.value,
        pageSize: bookCommentPageSize.value
      }
    })

    if (res.code === 200) {
      const { total, list, totalPages } = res.data
      bookCommentTotal.value = total
      hasMoreBookComments.value = bookCommentPage.value < totalPages

      // 🔥 直接使用后端返回的书籍信息，无需额外请求
      const commentsWithBookInfo = list.map(comment => ({
        ...comment,
        bookName: comment.bookName || '未知书籍',
        star: comment.star || 0,
        pictureName: comment.pictureName
      }))

      if (isLoadMore) {
        userBookComments.value.push(...commentsWithBookInfo)
      } else {
        userBookComments.value = commentsWithBookInfo
      }
    } else {
      ElMessage.error(res.msg || '加载书籍评论失败')
      userBookComments.value = []
    }
  } catch (e) {
    console.error('获取用户书籍评论失败', e)
    userBookComments.value = []
  } finally {
    isLoadingBookComments.value = false
  }
}

// ====================== 🔥 新增：加载更多书籍评论（无限滚动用） ======================
const loadMoreBookComments = async (userId) => {
  if (!hasMoreBookComments.value || isLoadingBookComments.value) return
  bookCommentPage.value += 1
  await fetchUserBookComments(userId, true)
}

// ====================== 🔥 新增：重置书籍评论分页状态 ======================
const resetBookCommentPage = () => {
  bookCommentPage.value = 1
  hasMoreBookComments.value = true
  userBookComments.value = []
}
// ------------------------------
// 函数6.8：全局类型获取
// ------------------------------
const fetchUserTypes = async () => {
  try {
    const res = await request.get('/user/type')
    if (res.code === 200) userTypeList.value = res.data || []
  } catch (error) { console.error('用户类型加载失败', error) }
}

const fetchDeptTypes = async () => {
  try {
    const res = await request.get('/user/deptType')
    if (res.code === 200) deptTypeList.value = res.data || []
  } catch (error) { console.error('系别类型加载失败', error) }
}

// ------------------------------
// 函数6.9：消息未读/登出/权限点击
// ------------------------------
const getUnreadCount = async () => {
  if (!userStore.isLogin) return
  try {
    const chatRes = await request.get('/user/chat/message', { params: { action: 'unreadCount', userId: userStore.userId } })
    chatUnreadCount.value = chatRes.code === 200 ? chatRes.data : 0

    const friendRes = await request.get('/user/friend/request', { params: { action: 'received', userId: userStore.userId } })
    const friendUnread = friendRes.code === 200 ? friendRes.data.filter(req => req.status === 0).length : 0

    const notifRes = await request.get('/user/notifications', { params: { userId: userStore.userId, action: 'list' } })
    const notificationUnread = notifRes.code === 200 ? notifRes.data.filter(item => item.isRead === 0).length : 0

    messageUnreadCount.value = friendUnread + notificationUnread
  } catch (err) { console.error('获取未读消息数失败', err) }
}

// ==========================================
// 🏝️ 灵动岛 Logo 交互：单击展开，双击回主页
// ==========================================
let logoClickTimer = null;

const handleLogoBallClick = () => {
  if (logoClickTimer) {
    // 💡 250ms内点了第二次：判定为【双击】
    clearTimeout(logoClickTimer);
    logoClickTimer = null;

    isNavExpanded.value = false; // 顺手把菜单收起来（显得干脆利落）
    router.push('/home');
  } else {
    // 💡 第一次点击：开启250ms等待期
    logoClickTimer = setTimeout(() => {
      logoClickTimer = null;

      // 执行双击逻辑：展开 / 收起胶囊导航菜单
      isNavExpanded.value = !isNavExpanded.value;
      // 250ms后没有触发第二次点击：判定为【单击】
      // 执行单击逻辑：收起菜单并跳转到主页
    }, 250);

  }
};
// ==========================================
// 🔥 头像交互：单击跳转主页，双击退出登录
// ==========================================
let avatarClickTimer = null;
const handleAvatarClick = () => {
  if (avatarClickTimer) {
    // 💡 250ms内点击了第二次：判定为【双击】
    clearTimeout(avatarClickTimer);
    avatarClickTimer = null;
    handleLogout(); // 触发退出登录
  } else {
    // 💡 第一次点击：开启250ms等待期
    avatarClickTimer = setTimeout(() => {
      avatarClickTimer = null;
      // 250ms后没有第二次点击：判定为【单击】
      router.push('/profile'); // 跳转我的主页
    }, 250);
  }
};

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出提示', { type: 'info' }).then(() => {
    userStore.logout()
    ElMessage.success('退出登录成功！')
    router.push('/login')
  }).catch((err) => {
    if (err === 'cancel') ElMessage.info('取消退出登录')
    else { ElMessage.error('退出登录失败，请重试'); console.error('退出登录错误：', err) }
  })
}

const handleLogoClick = () => {
  isAdmin.value ? router.push('/userInfo') : ElMessage.warning('无权限！')
}

const handleTimeClick = () => {
  isAdmin.value ? router.push('/borrowInfo') : ElMessage.warning('无权限！')
}
// ------------------------------
// 函数6.10：好友信息
// ------------------------------
const friendList = ref([])
provide('friendList',friendList)

const getFriendList = async () => {
  try {
    const res = await request.get('/user/friend', {
      params: { action: 'list', userId: currentUserId.value }
    })
    if (res.code === 200) {
      const friends = res.data || []
      // 🔥 直接使用后端返回的用户名，无需额外请求
      friendList.value = friends
    }
  } catch (err) {
    console.error('获取好友列表失败', err)
    ElMessage.error('获取好友列表失败')
  }
}
provide('getFriendList',getFriendList)
// ------------------------------
// 函数6.12：消息/好友申请相关
// ------------------------------
// 获取收到的好友申请
const getFriendRequests = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/user/friend/request', {
      params: { action: 'received', userId: currentUserId.value }
    })
    if (res.code === 200) {
      const requests = res.data || []
      // 🔥 直接使用后端返回的用户名，无需额外请求
      friendRequests.value = requests
      // 自动更新未读数量
      updateMessageUnreadCount()
    }
  } catch (err) {
    console.error('获取收到的申请失败', err)
  }
}

// 获取发出的好友申请
const getSentRequests = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/user/friend/request', {
      params: { action: 'sent', userId: currentUserId.value }
    })
    if (res.code === 200) {
      const requests = res.data || []
      // 🔥 直接使用后端返回的用户名，无需额外请求
      sentRequests.value = requests
    }
  } catch (err) {
    console.error('获取发出的申请失败', err)
  }
}

// 获取系统互动通知
const getNotifications = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/user/notifications', {
      params: { userId: currentUserId.value, action: 'list' }
    })
    if (res.code === 200) {
      // 🔥 后端已返回：用户名、评论内容、评论图片，直接赋值
      const notificationsData = res.data || []
      // 格式化类型（保持原有逻辑）
      notifications.value = notificationsData.map(item => ({
        id: item.id,
        type: item.type === 1 ? 'comment' : 'like',
        fromUserId: item.fromUserId,
        fromUserName: item.fromUserName || '未知用户', // 直接用后端数据
        content: item.content || '', // 直接用后端数据
        targetId: item.commentId,
        replyCommentId: item.replyCommentId,
        images: item.images || [], // 直接用后端图片
        createTime: item.createTime,
        isRead: item.isRead
      }))
      // 自动更新未读数量
      updateMessageUnreadCount()
    }
  } catch (err) {
    console.error('获取互动消息失败', err)
  }
}

// 批量标记系统通知为已读
const batchMarkAllRead = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/user/notifications', {
      params: {
        userId: currentUserId.value,
        action: 'readAll'
      }
    })
    if (res.code === 200) {
      notifications.value.forEach(item => {
        item.isRead = 1
      })
      await getNotifications()
      updateMessageUnreadCount()
    }
  } catch (err) {
    console.error('批量标记已读失败', err)
  }
}

// 同意好友申请
const acceptRequest = async (friendRequest) => {
  try {
    const res = await request.post('/user/friend/request', null, {
      params: {
        action: 'accept',
        requestId: friendRequest.id,
        userId: currentUserId.value
      }
    })
    if (res.code === 200) {
      ElMessage.success('已同意好友申请')
      friendRequest.status = 1
      await getSentRequests()
      // 同意后刷新好友列表
      await getFriendList()
    } else {
      ElMessage.error(res.msg || '同意失败')
    }
  } catch (err) {
    console.error('同意好友申请失败', err)
    ElMessage.error('同意失败')
  }
}

// 拒绝好友申请
const rejectRequest = async (friendRequest) => {
  try {
    const res = await request.post('/user/friend/request', null, {
      params: {
        action: 'reject',
        requestId: friendRequest.id
      }
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝好友申请')
      friendRequest.status = 2
      await getSentRequests()
    } else {
      ElMessage.error(res.msg || '拒绝失败')
    }
  } catch (err) {
    console.error('拒绝好友申请失败', err)
    ElMessage.error('拒绝失败')
  }
}

// 更新消息未读数量（整合到全局）
const updateMessageUnreadCount = () => {
  const receivedUnread = friendRequests.value.filter(r => r.status === 0).length
  const notificationUnread = notifications.value.filter(n => n.isRead === 0).length
  messageUnreadCount.value = receivedUnread + notificationUnread
}

// ------------------------------
// 函数6.13：全局@艾特功能
// ------------------------------
// 处理输入框@触发
// ------------------------------
// 函数6.13：全局@艾特功能
// ------------------------------
// 处理输入框@触发（新增：接收输入框更新回调）
const handleMentionInput = (val, inputRef) => {
  currentMentionInput.value = inputRef
  currentMentionContent.value = val

  // 检测@符号（支持@后输入部分用户名过滤）
  const lastAtIndex = val.lastIndexOf('@')
  if (lastAtIndex === -1) {
    showMentionList.value = false
    return
  }

  // 提取@后面的搜索关键词
  const searchText = val.slice(lastAtIndex + 1).toLowerCase()

  // 过滤好友列表
  filteredMentionUsers.value = friendList.value.filter(user =>
      user.userName.toLowerCase().includes(searchText) ||
      String(user.friendId).includes(searchText)
  )

  // 显示好友列表
  showMentionList.value = filteredMentionUsers.value.length > 0

  // 计算下拉列表位置（在光标下方）
  if (inputRef?.textarea) {
    const rect = inputRef.textarea.getBoundingClientRect()
    mentionDropdownPosition.value = {
      x: rect.left,
      y: rect.bottom + 5
    }
  }
}

// 键盘导航选择用户
const handleMentionKeydown = (e) => {
  if (!showMentionList.value) return

  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedMentionIndex.value = (selectedMentionIndex.value + 1) % filteredMentionUsers.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedMentionIndex.value = (selectedMentionIndex.value - 1 + filteredMentionUsers.value.length) % filteredMentionUsers.value.length
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (filteredMentionUsers.value[selectedMentionIndex.value]) {
      selectMentionUser(filteredMentionUsers.value[selectedMentionIndex.value])
    }
  } else if (e.key === 'Escape') {
    showMentionList.value = false
  }
}

// 选择艾特用户（通过回调更新输入框，不再直接修改modelValue）
const selectMentionUser = (user) => {
  if (!mentionUpdateCallback.value) return

  const atText = `@${user.userName}(${user.friendId}) `
  const lastAtIndex = currentMentionContent.value.lastIndexOf('@')

  // ✅ 正确替换：从最后一个@到当前末尾的内容
  const newContent = currentMentionContent.value.slice(0, lastAtIndex) + atText

  // ✅ 通过回调函数更新Vue响应式变量
  mentionUpdateCallback.value(newContent)

  // 重置状态
  showMentionList.value = false
  selectedMentionIndex.value = 0
  mentionUpdateCallback.value = null
}

// 格式化艾特文本为可点击链接
const formatMentionText = (content) => {
  if (!content) return ''
  return content.replace(/@([^()]+)\((\d+)\)/g, (match, userName, userId) => {
    return `<span
      class="mention-text"
      data-user-id="${userId}"
      style="color:#409eff; cursor:pointer; font-weight:500;"
      onclick="window.gotoUserProfile('${userId}')"
    >@${userName}</span>`
  })
}

// 点击艾特文本跳转用户主页
window.gotoUserProfile = (userId) => {
  router.push('/user/profile?userId=' + userId)
}

// 点击外部关闭艾特列表
const closeMentionListOnClickOutside = (e) => {
  const list = document.querySelector('.mention-user-list')
  const input = currentMentionInput.value?.textarea
  if (list && !list.contains(e.target) && !input?.contains(e.target)) {
    showMentionList.value = false
  }
}
provide('closeMentionListOnClickOutside',closeMentionListOnClickOutside)
// ------------------------------
// 函数6.14：全局书籍分享卡片功能
// ------------------------------
// 解析文本中的书籍链接为卡片HTML
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;
// 🔥 重构：通用消息解析函数（将 [{类型}:ID] 转换为HTML卡片）
// 🔥 重构：通用消息解析函数（支持所有5种卡片）
const parseBookLinkToCard = (content, isSimplified = false) => {
  if (!content) return ''

  return content.replace(cardReg, (match, type, id) => {
    switch (type) {
        // ------------------------------
        // 原有：书籍卡片（完全不变）
        // ------------------------------
      case CARD_TYPES.BOOK:
        // 👇 新增：多卡片时的精简版 UI（仿番茄小说）
        // 原换行模板全部替换成压缩单行
        if (isSimplified) {
          // 书籍精简卡片压缩版
          return `<div class="book-share-card simplified-card" data-type="book" data-id="${id}" onclick="window.gotoBookDetail('${id}')"><div class="simplified-cover-wrapper"><img src="${IMAGE_BASE_URL}/default-book.png" class="book-share-card-cover" alt="书籍封面" data-isbn="${id}"><div class="book-rating badge">--分</div></div><p class="book-title">加载中...</p></div>`
        }
        // 👇 原有：单卡片时的全量 UI（保持完全不变）
        return `
          <div class="book-share-card inline-card" data-type="book" data-id="${id}" onclick="window.gotoBookDetail('${id}')">
            <img src="${IMAGE_BASE_URL}/default-book.png" class="book-share-card-cover" alt="书籍封面" data-isbn="${id}">
            <div class="book-share-card-info">
              <p class="book-title">加载中...</p>
              <p class="book-author">作者：加载中</p>
              <p class="book-rating">⭐ 评分：--</p>
              <p class="book-desc">暂无简介</p>
            </div>
          </div>
        `

        // ------------------------------
        // 新增1：书籍评论卡片
        // ------------------------------
      case CARD_TYPES.BOOK_COMMENT:
        return `
          <div class="book-share-card inline-card" data-type="bookComment" data-id="${id}" onclick="window.gotoBookDetailFromComment('${id}')">
            <img src="${IMAGE_BASE_URL}/default-book.png" class="book-share-card-cover" alt="书籍封面">
            <div class="book-share-card-info">
              <p class="book-title">📚 书籍评论</p>
              <p class="book-author">书籍：加载中</p>
              <p class="book-rating">⭐ 评分：--</p>
              <p class="book-desc">评论内容：加载中...</p>
              <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">发布时间：--</p>
            </div>
          </div>
        `

        // ------------------------------
        // 新增2：用户论坛评论卡片
        // ------------------------------
      case CARD_TYPES.USER_COMMENT:
        return `
          <div class="book-share-card inline-card" data-type="userComment" data-id="${id}">
            <div class="book-share-card-cover" style="background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:24px;">💬</div>
            <div class="book-share-card-info">
              <p class="book-title">🔄 论坛评论</p>
              <p class="book-author">作者：加载中 (ID:--)</p>
              <p class="book-rating">👍 点赞：-- | 💬 回复：--</p>
              <p class="book-desc">评论内容：加载中...</p>
              <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">发布时间：--</p>
            </div>
          </div>
        `

        // ------------------------------
        // 新增3：用户信息卡片
        // ------------------------------
      case CARD_TYPES.USER:
        return `
          <div class="book-share-card inline-card" data-type="user" data-id="${id}" onclick="window.gotoUserProfile('${id}')">
            <div class="book-share-card-cover" style="background:#e6f7ff; display:flex; align-items:center; justify-content:center; font-size:24px;">👤</div>
            <div class="book-share-card-info">
              <p class="book-title">👤 用户信息</p>
              <p class="book-author">用户名：加载中 (ID:${id})</p>
              <p class="book-rating">性别：-- | 类型：--</p>
              <p class="book-desc">阅读时长：--小时 | 个人介绍：加载中...</p>
            </div>
          </div>
        `

        // ------------------------------
        // 新增4：笔记卡片
        // ------------------------------
      case CARD_TYPES.NOTE:
        return `
          <div class="book-share-card inline-card" data-type="note" data-id="${id}" onclick="window.gotoNoteReader('${id}')">
            <img src="${IMAGE_BASE_URL}/default-book.png" class="book-share-card-cover" alt="书籍封面">
            <div class="book-share-card-info">
              <p class="book-title">📝 读书笔记</p>
              <p class="book-author">出自：《加载中》第--章</p>
              <p class="book-rating">类型：--</p>
              <p class="book-desc">原文：加载中...</p>
              <p class="book-desc" style="margin-top:4px; color:#666;">批注：加载中...</p>
            </div>
          </div>
        `

        // 未知类型，原样返回
      default:
        return match
    }
  })
}

// ✅ 修复：挂载全局跳转函数（解决点击卡片不跳转问题）
window.gotoBookDetail = (isbn) => {
  router.push({
    path: '/book/detail',
    query: { isbn: isbn }
  })
}

// BasicLayout.vue 函数6.14
// 🔥 重构：通用卡片信息更新（支持所有类型）
// 1. 新增箭头触发的滚动逻辑
const scrollCards = (messageId, direction) => {
  const container = document.getElementById(`scroll-view-${messageId}`)
  if (!container) return

  // 每次滚动的像素距离，可根据卡片宽度自行微调 (大概是 1 个半卡片的距离)
  const scrollAmount = 180

  if (direction === 'left') {
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
  } else {
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}
provide('scrollCards', scrollCards)
// 🔥 重构：通用卡片信息更新（支持所有5种类型）
const updateAllBookCards = () => {
  // ------------------------------
  // 1. 更新书籍卡片（完全不变）
  // ------------------------------
  const bookCards = document.querySelectorAll('.book-share-card[data-type="book"]')
  bookCards.forEach(async (card) => {
    const isbn = card.dataset.id
    if (!isbn || card.classList.contains('loaded')) return

    try {
      const res = await request.get('/book', { params: { isbn } })
      if (res.code === 200 && res.data.length > 0) {
        const book = res.data[0]
        const img = card.querySelector('.book-share-card-cover')
        const title = card.querySelector('.book-title')
        const author = card.querySelector('.book-author')
        const rating = card.querySelector('.book-rating')
        const desc = card.querySelector('.book-desc')

        const isSimplified = card.classList.contains('simplified-card')

        if (img) img.src = IMAGE_BASE_URL + (book.pictureName || '/default-book.png')
        if (title) title.textContent = book.bookname || '未知书籍'

        // 👇 这里做个判断：如果是精简版，评分显示 "7.9分"，否则显示 "⭐ 评分：7.9"
        if (rating) {
          rating.textContent = isSimplified
              ? `${book.star || 0}分`
              : `⭐ 评分：${book.star || 0}`
        }

        // 精简版没有 author 和 desc，这里会安全跳过
        if (author) author.textContent = `作者：${book.author || '未知'}`
        if (desc) desc.textContent = (book.information?.slice(0, 50) + '...' || '暂无简介')

        card.classList.add('loaded')
      }
    } catch (err) {
      console.error('更新书籍卡片失败', err)
      card.classList.add('loaded')
    }
  })

  // ------------------------------
  // 2. 新增：更新书籍评论卡片
  // ------------------------------
  const bookCommentCards = document.querySelectorAll('.book-share-card.inline-card[data-type="bookComment"]')
  bookCommentCards.forEach(async (card) => {
    const commentId = card.dataset.id
    if (!commentId || card.classList.contains('loaded')) return

    try {
      // 第一步：获取评论信息
      const commentRes = await request.get('/book/comment', { params: { commentId } })
      if (commentRes.code !== 200 || !commentRes.data.length) return
      const comment = commentRes.data[0]

      // 第二步：获取对应书籍信息
      const bookRes = await request.get('/book', { params: { isbn: comment.iSBN } })
      const book = bookRes.data?.[0] || { bookname: '未知书籍' }

      // 更新卡片内容
      const img = card.querySelector('.book-share-card-cover')
      const bookName = card.querySelector('.book-author')
      const star = card.querySelector('.book-rating')
      const content = card.querySelector('.book-desc')
      const time = card.querySelector('.book-time')

      if (img) img.src = IMAGE_BASE_URL + (book.pictureName || '/default-book.png')
      if (bookName) bookName.textContent = `书籍：${book.bookname}`
      if (star) star.textContent = `⭐ 评分：${comment.star || 0}`
      if (content) content.textContent = `评论内容：${comment.comment?.slice(0, 40) + '...' || '暂无内容'}`
      if (time) time.textContent = `发布时间：${formatDateTime(comment.time, true)}`

      card.classList.add('loaded')
    } catch (err) {
      console.error('更新书籍评论卡片失败', err)
      card.classList.add('loaded')
    }
  })

  // ------------------------------
  // 3. 新增：更新用户论坛评论卡片
  // ------------------------------
  const userCommentCards = document.querySelectorAll('.book-share-card.inline-card[data-type="userComment"]')
  userCommentCards.forEach(async (card) => {
    const commentId = card.dataset.id
    if (!commentId || card.classList.contains('loaded')) return

    try {
      // 第一步：获取评论信息
      const commentRes = await request.get('/user/comment', { params: { commentId } })
      if (commentRes.code !== 200 || !commentRes.data.length) return
      const comment = commentRes.data[0]

      // 第二步：获取用户信息
      const userRes = await getUserInfo(comment.UserId)

      // 第三步：计算点赞和回复数量
      const likeCount = comment.prefer || 0
      const replyCount = await  getSubTotal(commentId)

      // 更新卡片内容
      const author = card.querySelector('.book-author')
      const stats = card.querySelector('.book-rating')
      const content = card.querySelector('.book-desc')
      const time = card.querySelector('.book-time')

      if (author) author.textContent = `作者：${userRes.userName} (ID:${comment.userid})`
      if (stats) stats.textContent = `👍 点赞：${likeCount} | 💬 回复：${replyCount}`
      if (content) content.textContent = `评论内容：${comment.userComment?.slice(0, 40) + '...' || '暂无内容'}`
      if (time) time.textContent = `发布时间：${formatDateTime(comment.commentTime, true)}`

      card.classList.add('loaded')
    } catch (err) {
      console.error('更新用户评论卡片失败', err)
      card.classList.add('loaded')
    }
  })

  // ------------------------------
  // 4. 新增：更新用户信息卡片
  // ------------------------------
  const userCards = document.querySelectorAll('.book-share-card.inline-card[data-type="user"]')
  userCards.forEach(async (card) => {
    const userId = card.dataset.id
    if (!userId || card.classList.contains('loaded')) return

    try {
      const user = await getUserInfo(userId)

      // 更新卡片内容
      const name = card.querySelector('.book-author')
      const info = card.querySelector('.book-rating')
      const desc = card.querySelector('.book-desc')

      if (name) name.textContent = `用户名：${user.userName} (ID:${userId})`
      if (info) info.textContent = `性别：${user.sex || '保密'} | 类型：${user.typeName || '普通用户'}`
      if (desc) desc.textContent = `阅读时长：${Math.floor(user.read_time_long / 3600)}小时 | 个人介绍：${user.bio?.slice(0, 30) + '...' || '暂无'}`

      card.classList.add('loaded')
    } catch (err) {
      console.error('更新用户信息卡片失败', err)
      card.classList.add('loaded')
    }
  })

  // ------------------------------
  // 5. 新增：更新笔记卡片
  // ------------------------------
  const noteCards = document.querySelectorAll('.book-share-card.inline-card[data-type="note"]')
  noteCards.forEach(async (card) => {
    const noteId = card.dataset.id
    if (!noteId || card.classList.contains('loaded')) return

    try {
      // 第一步：获取笔记信息
      const noteRes = await request.get('/user/note', { params: { id: noteId } })
      if (noteRes.code !== 200 || !noteRes.data.length) return
      const note = noteRes.data[0]

      // 第二步：获取对应书籍信息
      const bookRes = await request.get('/book', { params: { isbn: note.isbn } })
      const book = bookRes.data?.[0] || { bookname: '未知书籍' }

      // 第三步：获取笔记类型名称
      const noteType = getNoteTypeName(note.Type)

      // 更新卡片内容
      const img = card.querySelector('.book-share-card-cover')
      const bookInfo = card.querySelector('.book-author')
      const type = card.querySelector('.book-rating')
      const text = card.querySelectorAll('.book-desc')[0]
      const comment = card.querySelectorAll('.book-desc')[1]

      if (img) img.src = IMAGE_BASE_URL + (book.pictureName || '/default-book.png')
      if (bookInfo) bookInfo.textContent = `出自：《${book.bookname}》第${note.chapterId}章`
      if (type) type.textContent = `类型：${noteType}`
      if (text) text.textContent = `原文：${note.text?.slice(0, 30) + '...' || '暂无'}`
      if (comment) comment.textContent = `批注：${note.readerComment?.slice(0, 30) + '...' || '暂无'}`

      card.classList.add('loaded')
    } catch (err) {
      console.error('更新笔记卡片失败', err)
      card.classList.add('loaded')
    }
  })
}

// ✅ 新增：输入框实时预览书籍卡片
// 🔥 修复：通用卡片实时预览（解决书籍评论不显示+笔记空ISBN报错）
const updatePreviewBookCards = (content) => {
  if (!content) {
    previewBookCards.value = []
    return
  }

  // 提取所有通用卡片
  const matches = [...content.matchAll(cardReg)]
  previewBookCards.value = matches.map(match => ({
    type: match[1],
    id: match[2],
    data: null,
    loading: true
  }))

  // 异步加载所有类型的卡片信息
  previewBookCards.value.forEach(async (card, index) => {
    try {
      switch (card.type) {
          // ------------------------------
          // 1. 书籍卡片（原有逻辑不变）
          // ------------------------------
        case CARD_TYPES.BOOK:
          const bookRes = await request.get('/book', { params: { isbn: card.id } })
          if (bookRes.code === 200 && bookRes.data.length > 0) {
            previewBookCards.value[index].data = bookRes.data[0]
          }
          break

          // ------------------------------
          // 🔥 修复2：新增书籍评论卡片预览逻辑
          // ------------------------------
        case CARD_TYPES.BOOK_COMMENT:
          // 第一步：获取评论信息（兼容commentId和id两种参数）
          const commentRes = await request.get('/book/comment', {
            params: { commentId: card.id, id: card.id }
          })
          if (commentRes.code === 200 && commentRes.data.length > 0) {
            const comment = commentRes.data[0]
            // 兼容大小写：ISBN/isbn
            const isbn = comment.iSBN || ''

            // 第二步：获取书籍信息（空值保护）
            let book = { bookname: '未知书籍' }
            if (isbn) {
              const bookRes = await request.get('/book', { params: { isbn } })
              if (bookRes.code === 200 && bookRes.data.length > 0) {
                book = bookRes.data[0]
              }
            }

            previewBookCards.value[index].data = {
              ...comment,
              book
            }
          }
          break

          // ------------------------------
          // 3. 用户论坛评论卡片（原有逻辑不变）
          // ------------------------------
        case CARD_TYPES.USER_COMMENT:
          const userCommentRes = await request.get('/user/comment', {
            params: { commentId: card.id, id: card.id }
          })
          if (userCommentRes.code === 200 && userCommentRes.data.length > 0) {
            const comment = userCommentRes.data[0]
            const user = await getUserInfo(comment.UserId || comment.userId)
            previewBookCards.value[index].data = {
              ...comment,
              user
            }
          }
          break

          // ------------------------------
          // 4. 用户信息卡片（原有逻辑不变）
          // ------------------------------
        case CARD_TYPES.USER:
          const user = await getUserInfo(card.id)
          previewBookCards.value[index].data = user
          break

          // ------------------------------
          // 🔥 修复1：笔记卡片空ISBN报错
          // ------------------------------
        case CARD_TYPES.NOTE:
          // 第一步：获取笔记信息
          const noteRes = await request.get('/user/note', { params: { id: card.id } })
          if (noteRes.code === 200 && noteRes.data.length > 0) {
            const note = noteRes.data[0]
            // 兼容大小写：Isbn/isbn
            const isbn = note.Isbn || note.isbn || ''

            // 第二步：获取书籍信息（空值保护，防止传null给后端）
            let book = { bookname: '未知书籍' }
            if (isbn) { // ✅ 只有ISBN不为空时才调用接口
              const bookRes = await request.get('/book', { params: { isbn } })
              if (bookRes.code === 200 && bookRes.data.length > 0) {
                book = bookRes.data[0]
              }
            }

            previewBookCards.value[index].data = {
              ...note,
              book,
              typeName: getNoteTypeName(note.Type || note.type)
            }
          }
          break
      }
    } catch (err) {
      console.error(`加载${card.type}预览失败`, err)
      // ✅ 即使加载失败，也标记为加载完成，防止一直显示加载中
      previewBookCards.value[index].data = { error: true }
    } finally {
      previewBookCards.value[index].loading = false
    }
  })
}
provide('updatePreviewBookCards', updatePreviewBookCards)

// 注入全局
provide('previewBookCards', previewBookCards)
// ==============================================
// 🔥 模块7：所有监听（watch）
// ==============================================
// 路由监听-书籍榜单
watch([() => route.path], () => {
  if (route.path === '/home') {
    loadHotBooks()
  }
}, { immediate: true })

// 搜索关键词监听
watch(searchKey, (newVal) => {
  if (route.path === '/home' && !isComposing.value) searchBooks()
})

// 用户ID监听-用户名更新
watch(currentUserId, async (newUserId) => {
  if (newUserId) {
    const userInfo = await getUserInfo(newUserId)
    userName.value = userInfo.userName
  } else userName.value = '未知用户'
}, { immediate: true })

// 路由监听-用户列表
watch([() => route.path], () => {
  if (route.path === '/userInfo') {
    request.get('/user/info').then(res => users.value = res.data).catch(err => console.error('初始化加载用户信息失败', err))
  }
}, { immediate: true })

// 路由监听-借阅记录


// 路由监听-导航激活态
watch(route, (newRoute) => {
  activeRoute.value = newRoute.name
})

// 用户ID监听-全量数据更新
watch(currentUserId, async (uid) => {
  if (uid) {
    await getuserBorrows()
    await fetchUserNotes(uid)
    await fetchUserReadHistory(uid)
    await fetchUserForumComments(uid)
    await fetchUserBookComments(uid)
    // ✅ 新增：用户登录后自动加载点赞记录
    await getUserLikedComments()
  }
}, { immediate: true })

// ==============================================
// 🔥 模块8：生命周期钩子（整合所有）
// ==============================================
// 全局SSE实时通信
let eventSource = null
// 存储 Chat 注册的回调函数（用于转发聊天消息）
const chatMessageCallbacks = ref({
  onNewMessage: null,    // 新消息回调
  onRecallMessage: null, // 消息撤回回调
  onAiThinking: null,    // AI思考回调
  onAiThinkingDone: null, // AI思考完成回调
  onMessageRead: null    // 🔥 新增：消息已读回调
})

// 提供方法给 Chat 注册回调（关键修正：合并所有回调）
provide('registerChatSSECallback', (callbacks) => {
  chatMessageCallbacks.value = {
    ...chatMessageCallbacks.value,
    ...callbacks
  }
})

const connectSSE = () => {
  const userId = currentUserId.value
  if (!userId) return

  // 关闭旧连接，防止重复
  if (eventSource) eventSource.close()

  // 🔥 从环境变量获取基础路径，无环境变量时使用当前页面的 origin（最安全）
  const baseURL = import.meta.env.VITE_API_BASE_URL || window.location.origin + '/api'

  // 建立唯一 SSE 连接
  eventSource = new EventSource(`${baseURL}/message/sse?userId=${userId}`)

  // 接收所有消息
  eventSource.onmessage = (e) => {
    try {
      // 🔥 第一步：先尝试解析成JSON
      if (e.data.startsWith('{')) {
        const data = JSON.parse(e.data)
        console.log("SSE 全局消息：", data)

        // 🔥 优先处理AI相关的JSON消息
        if (data.type === "AI_THINKING_APPEND" || data.type === "AI_THINKING") {
          console.log("SSE AI思考（JSON格式）：", data)
          if (chatMessageCallbacks.value.onAiThinking) {
            // 把整个JSON对象传给回调，Chat.vue会处理
            chatMessageCallbacks.value.onAiThinking(data)
          }
          return
        }

        // 2. AI思考完成通知
        if (data.type === "AI_THINKING_DONE") {
          if (chatMessageCallbacks.value.onAiThinkingDone) {
            chatMessageCallbacks.value.onAiThinkingDone()
          }
          return
        }

        // 3. 好友申请通知（原逻辑不变）
        if (data.type === "FRIEND_REQUEST") {
          petStore.addMessage(`👋 你收到了一条新的好友申请！`, petStore.MESSAGE_TYPES.CHAT)
          getFriendRequests()
          getSentRequests()
          window.refreshBasicUnread?.()
        }

        // 4. 互动通知（原逻辑不变）
        if (data.type === "INTERACTION") {
          // ✅ 修复1：变量名拼写错误 date -> data
          // ✅ 修复2：正确解析SSE消息结构：外层data.type是"INTERACTION"，具体类型在data.data.type
          const interactionType = data.type;
          let notificationText = '';

          // ✅ 根据后端定义显示不同文案：1=被回复，2=被点赞
          if (interactionType === 1) {
            notificationText = '💬 有人回复了你的评论！';
          } else if (interactionType === 2) {
            notificationText = '👍 有人点赞了你的评论！';
          } else {
            notificationText = '🔔 你收到了一条新的互动通知！';
          }

          // ✅ 修复3：模板字符串必须用反引号 ` 包裹，不能用单引号
          petStore.addMessage(notificationText, petStore.MESSAGE_TYPES.CHAT);
          getNotifications()
          window.refreshBasicUnread?.()
        }

        // 5. 新聊天消息（原逻辑不变）
        if (data.type === "NEW_MESSAGE") {
          petStore.addMessage(`📩 ${data.fromUserId}给你发了一条新消息`, petStore.MESSAGE_TYPES.CHAT)
          window.refreshBasicUnread?.()
          if (chatMessageCallbacks.value.onNewMessage) {
            chatMessageCallbacks.value.onNewMessage(data)
          } else {
            window.getSessionList?.()
          }
        }

        // 🔥 新增：消息已读事件（对方已读了你的消息）
        if (data.type === "MESSAGE_READ") {
          // ✅ 完全对齐NEW_MESSAGE逻辑：优先转发给Chat组件回调
          if (chatMessageCallbacks.value.onMessageRead) {
            chatMessageCallbacks.value.onMessageRead(data)
          } else {
            // 没有注册回调时，只刷新会话列表
            window.getSessionList?.()
          }
        }

        // 6. 消息撤回（原逻辑不变）
        if (data.type === "MESSAGE_RECALLED") {
          petStore.addMessage(`⚠️ 对方撤回了一条消息`, petStore.MESSAGE_TYPES.CHAT)
          if (chatMessageCallbacks.value.onRecallMessage) {
            chatMessageCallbacks.value.onRecallMessage(data.id)
          }
          window.getSessionList?.()
        }

        // ① 收到开播信号，建好空盒
        if (data.type === "PET_AI_START") {
          // 🔥 核心修改：把后端传来的 bubbleType 掏出来，传给 Store！
          // 如果后端没传（或者老版本数据），默认兜底为 'chat'
          const targetBubble = data.bubbleType || 'chat'
          petStore.createStreamingMessage(data.msgId, targetBubble)
          return
        }

        // ② 收到一小段文字，往盒子里贴
        if (data.type === "PET_AI_CHUNK") {
          petStore.appendStreamingChunk(data.msgId, data.content)
          return
        }

        // ③ 收到全文完结信号，开始5秒倒计时
        if (data.type === "PET_AI_DONE") {
          petStore.finishStreamingMessage(data.msgId)
          achievementStore.checkAchievements()
          return
        }

        // ④ 极速报错兜底
        if (data.type === "PET_AI_ERROR") {
          petStore.addMessage('📶 信号飘走了，小喵没听清喵~')
          return
        }

      } else {
        // 🔥 第二步：纯文本 = 普通AI思考内容（兼容旧格式）
        console.log("SSE AI思考（纯文本）：", e.data)
        if (chatMessageCallbacks.value.onAiThinking) {
          chatMessageCallbacks.value.onAiThinking(e.data)
        }
      }
    } catch (err) {
      console.error("SSE 消息解析失败：", err)
    }
  }

  // 错误重连（原逻辑不变）
  eventSource.onerror = (err) => {
    console.error('SSE 连接错误，3秒后重连', err)
    eventSource.close()
    setTimeout(connectSSE, 3000)
  }
}

// =================================================================
// 🧲 顶栏智能磁吸显隐逻辑 (支持 PC 悬停 & 手机滑动)
// =================================================================
const isHeaderVisible = ref(true)
let headerHideTimer = null

// 1. 开始倒计时（PC端专属：3.5秒后缩回天花板）
const startHideCountdown = () => {
  // 📱 核心拦截：移动端绝对不走定时隐藏逻辑！
  if (isMobile.value) return

  if (headerHideTimer) clearTimeout(headerHideTimer)
  headerHideTimer = setTimeout(() => {
    isHeaderVisible.value = false
  }, 3500)
}

// 2. 保持显示（PC端专属：鼠标碰到顶栏立刻现形，并强行续命）
const keepHeaderAlive = () => {
  if (isMobile.value) return
  if (headerHideTimer) clearTimeout(headerHideTimer)
  isHeaderVisible.value = true
}

// =================================================================
// 📱 移动端专属：手指滑动感知引擎 (Swipe Detection)
// =================================================================
let touchStartY = 0
let touchCurrentY = 0

const handleTouchStart = (e) => {
  if (!isMobile.value) return
  touchStartY = e.touches[0].clientY
}

const handleTouchMove = (e) => {
  if (!isMobile.value) return
  touchCurrentY = e.touches[0].clientY
  const deltaY = touchCurrentY - touchStartY

  // 设置 30px 的滑动阈值，防止用户手指微颤导致的疯狂闪烁
  if (deltaY > 40) {
    // 手指向下滑动 (页面往上滚) -> 用户想看之前的内容/导航 -> 召唤顶栏！
    if (!isHeaderVisible.value) isHeaderVisible.value = true
  } else if (deltaY < -40) {
    // 手指向上滑动 (页面往下滚) -> 用户进入沉浸式浏览 -> 收起顶栏！
    if (isHeaderVisible.value) isHeaderVisible.value = false
  }
}

// 🌟 监听设备切换：如果用户在浏览器里把窗口从PC缩放到手机尺寸
watch(isMobile, (newVal) => {
  if (newVal) {
    // 切到手机：立刻清除定时器，并强制显示一次顶栏
    if (headerHideTimer) clearTimeout(headerHideTimer)
    isHeaderVisible.value = true
  } else {
    // 切回PC：重新激活 3.5 秒隐藏机制
    startHideCountdown()
  }
})

const dynamicShelfGap = computed(() => isHeaderVisible.value ? '190px' : '90px')

onMounted(() => {
  startHideCountdown()
  // ✅ 新增：注册移动端全局触摸事件，使用 passive: true 提升滚动性能不卡顿
  document.addEventListener('touchstart', handleTouchStart, { passive: true })
  document.addEventListener('touchmove', handleTouchMove, { passive: true })
  // 在BasicLayout的onMounted里添加
  // ====================== 路由守卫：离开阅读页自动上报 + 清定时器 ======================
  router.beforeEach(async (to, from, next) => {
    if (from.name === 'BookReader') {
      const readerStore = useReaderStore()

      const userId = userStore.userId
      const isbn = userStore.currentReadingIsbn
      const lastReadTime = userStore.lastReadTime

      // 1. 上报最后一段阅读时长
      if (userId && isbn && lastReadTime > 0) {
        try {
          const now = Date.now()
          const elapsedSeconds = Math.floor((now - lastReadTime) / 1000)

          if (elapsedSeconds >= 1) {
            console.log('📤 离开阅读页，自动上报时长：', elapsedSeconds, '秒')

            // 上报总阅读时长
            const { data } = await request.get('/user/info', { params: { userId } })
            const read_time_long = (data[0]?.read_time_long || 0) + elapsedSeconds
            await request.patch('/user/info', { userId, read_time_long })

            // 上报当日阅读记录
            const today = new Date().toISOString().split('T')[0]
            await request.post('/user/read', {
              userId,
              readDate: today,
              readDuration: elapsedSeconds
            })

            // 上报单本书籍阅读记录
            await request.post('/user/record', {
              userId,
              isbn,
              readDuration: elapsedSeconds
            })
          }
        } catch (e) {
          console.error('离开页阅读时长上报失败', e)
        }
      }

      // 2. 停止 AI 自动出题
      if (petStore.isAutoQuizRunning && petStore.autoQuizMode === 'ai') {
        petStore.stopAutoQuiz()
        petStore.addMessage('📚 已离开阅读页面，自动出题已停止')
      }

      // 3. 调用全局统一清理（朗读、缓存、漂流瓶、定时器 一键清空）
      readerStore.cleanupAllResources()

      // 4. 清空用户阅读状态
      userStore.stopReading()

      console.log('✅ 阅读页所有资源已清理完毕')
    }

    next()
  })
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu)
  // 移动端适配
  checkMobile()
  window.addEventListener('resize', checkMobile)
  // 实时时间
  updateTime()
  timer = setInterval(updateTime, 1000)

  // 🔥 批量初始化：一次请求替代 10 次独立请求
  batch(['bookTypes', 'noteTypes', 'tags', 'userTypes', 'deptTypes', 'friends', 'friendRequests', 'notifications', 'unread'])
    .then(data => {
      if (data.bookTypes) bookTypeList.value = data.bookTypes
      if (data.noteTypes) noteTypeList.value = data.noteTypes
      if (data.tags) allTagList.value = data.tags
      if (data.userTypes) userTypeList.value = data.userTypes
      if (data.deptTypes) deptTypeList.value = data.deptTypes
      if (data.friends) friendList.value = data.friends
      if (data.friendRequests) friendRequests.value = data.friendRequests
      if (data.notifications) notifications.value = data.notifications
      if (data.unread) {
        unreadCount.value = data.unread.total
        unreadNotifyCount.value = data.unread.notifications
        unreadRequestCount.value = data.unread.friendRequests
      }
    })

  // 消息未读定时刷新（每30秒）
  getUnreadCount()
  window.refreshBasicUnread = getUnreadCount
  setInterval(getUnreadCount, 30000)

  getSentRequests()
  // ✅ 新增：全局建立SSE连接
  if (currentUserId.value) connectSSE()
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('scroll', closeContextMenu)
  // 移动端适配
  window.removeEventListener('resize', checkMobile)
  // ✅ 新增：卸载移动端触摸事件
  document.removeEventListener('touchstart', handleTouchStart)
  document.removeEventListener('touchmove', handleTouchMove)
  // 时间定时器
  clearInterval(timer)
  // 搜索定时器/请求
  if (suggestDebounceTimer) clearTimeout(suggestDebounceTimer)
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  if (suggestAbortController) suggestAbortController.abort()
  if (searchAbortController) searchAbortController.abort()

  // ✅ 新增：页面销毁时关闭SSE连接
  if (eventSource) eventSource.close()
})

// ==============================================
// 🔥 模块9：全局注入（provide 统一整理）
// ==============================================
// 基础全局
provide('globalContextMenu', contextMenuRef)
provide('currentUserId', currentUserId)
provide('currentUserType', currentUserType)

// 分享相关
provide('shareToForum', shareToForum)
provide('generateShareContent', generateShareContent)

// 书籍相关
provide('hotBooks', hotBooks)
provide('recommendBooks', recommendBooks)
provide('books', books)
provide('searchBooks', searchBooks)
provide('searchKey', searchKey)
provide('bookTypeList', bookTypeList)
provide('handlePictureChange', handlePictureChange)
provide('imgUrl', imgUrl)

// 用户相关
provide('refreshUserInfo', refreshUserInfo)
provide('getUserInfo', getUserInfo)
provide('users', users)
provide('searchUsers', searchUsers)

// 论坛相关
provide('forumComments', forumComments)
provide('forumSearchUserId', forumSearchUserId)
provide('forumSearchComments', forumSearchComments)
provide('forumSortType', forumSortType)
provide('toggleForumSort', toggleForumSort)
provide('forumMainComments', forumMainComments)

provide('forumSubCommentState', forumSubCommentState)
provide('loadSubComments', loadSubComments)
provide('loadMoreSubComments', loadMoreSubComments)
provide('collapseSubComments', collapseSubComments)
provide('loadMoreMainComments', loadMoreMainComments)
provide('isLoadingMainComments', isLoadingMainComments)
provide('hasMoreMainComments', hasMoreMainComments)
// 借阅相关
provide('borrows', borrows)
provide('searchBorrows', searchBorrows)
provide('userBorrowList', userBorrowList)
provide('getuserBorrows', getuserBorrows)

// 收藏/书架
provide('userCollections', userCollections)
provide('loadUserCollections', loadUserCollections)
provide('handleCollect', handleCollect)
provide('loadUserCollectionsPage', loadUserCollectionsPage) // 🔥 分页加载（书架用）

// 笔记/阅读/评论
provide('noteTypeList', noteTypeList)

provide('userReadHistory', userReadHistory)
// ====================== 注入所有分页状态/方法 ======================
provide('userNotes', userNotes)
provide('fetchUserNotes', fetchUserNotes)
provide('loadMoreUserNotes', loadMoreUserNotes)
provide('isLoadingNotes', isLoadingNotes)
provide('hasMoreNotes', hasMoreNotes)
provide('resetUserNotes', resetUserNotes)

provide('fetchUserReadHistory', fetchUserReadHistory)
provide('userForumComments', userForumComments)
provide('userBookComments', userBookComments)
// ====================== 新增：provide 分页相关函数和状态 ======================
provide('fetchUserBookComments', fetchUserBookComments)
provide('loadMoreBookComments', loadMoreBookComments)
provide('isLoadingBookComments', isLoadingBookComments)
provide('hasMoreBookComments', hasMoreBookComments)
provide('resetBookCommentPage', resetBookCommentPage)
// ====================== 注入 ======================
provide('fetchUserForumComments', fetchUserForumComments)
provide('loadMoreUserForumComments', loadMoreUserForumComments)
provide('isLoadingForumComments', isLoadingForumComments)
provide('hasMoreForumComments', hasMoreForumComments)
provide('resetUserForumComments', resetUserForumComments)

// 消息/好友申请相关
provide('friendRequests', friendRequests)
provide('sentRequests', sentRequests)
provide('notifications', notifications)
provide('getFriendRequests', getFriendRequests)
provide('getSentRequests', getSentRequests)
provide('getNotifications', getNotifications)
provide('batchMarkAllRead', batchMarkAllRead)
provide('acceptRequest', acceptRequest)
provide('rejectRequest', rejectRequest)

// ✅ 新增：全局@艾特功能注入
provide('showMentionList', showMentionList)
provide('filteredMentionUsers', filteredMentionUsers)
provide('selectedMentionIndex', selectedMentionIndex)
provide('mentionDropdownPosition', mentionDropdownPosition)
provide('handleMentionInput', handleMentionInput)
provide('handleMentionKeydown', handleMentionKeydown)
provide('selectMentionUser', selectMentionUser)
provide('formatMentionText', formatMentionText)

// ✅ 新增：全局书籍分享卡片注入
provide('parseBookLinkToCard', parseBookLinkToCard)
provide('updateAllBookCards', updateAllBookCards)
provide('updatePreviewBookCards', updatePreviewBookCards)
provide('previewBookCards', previewBookCards)

// 全局类型
provide('userTypeList', userTypeList)
provide('deptTypeList', deptTypeList)
// ==============================================
// 🔥 模块10：全局分享书籍给好友功能（新增，全页面复用）
// ==============================================
// 响应式变量
const showShareFriendModal = ref(false) // 分享弹窗
const selectedShareFriend = ref(null)  // 选中的好友
const shareBook = ref(null)            // 待分享书籍
const shareMessage = ref('')           // 留言
const mentionSearchText = ref('')      // 好友搜索关键词

// 1. 触发分享：右键菜单调用（核心入口）
const shareBookToFriend = () => {
  if (!currentRightClickBook.value) {
    ElMessage.warning('未选中书籍')
    return
  }
  // 关闭右键菜单
  showContextMenu.value = false
  // 记录书籍
  shareBook.value = currentRightClickBook.value
  // 生成书籍短链接 + 加载预览卡片
  const shareLink = generateBookShareLink(shareBook.value)
  updatePreviewBookCards(shareLink)
  // 打开弹窗
  showShareFriendModal.value = true
  selectedShareFriend.value = null
  shareMessage.value = ''
  mentionSearchText.value = ''
}

// 2. 选择分享好友
const selectShareFriend = (user) => {
  selectedShareFriend.value = {
    userId: user.friendId,
    friendId: user.friendId,
    userName: user.userName
  }
  showMentionList.value = false
}

// 3. 发送分享消息（完全对齐你的聊天接口，修复发送失败问题）
const sendShareMessage = async () => {
  if (!selectedShareFriend.value || !shareBook.value) return
  try {
    // 拼接消息：书籍短链接 + 留言（100%复用你生成的分享链接）
    const shareLink = generateBookShareLink(shareBook.value)
    const sendContent = `📚 书籍分享\n${shareLink}\n💬 留言：${shareMessage.value || '无'}`

    // ✅ 【核心修复】接口地址/参数 100% 对齐你的 sendMessage 函数
    const params = {
      action: 'send',
      fromUserId: currentUserId.value,
      toUserId: selectedShareFriend.value.friendId, // 你的好友ID字段是 friendId
      messageType: 1, // 文本消息
      messageContent: sendContent
    }

    // ✅ 【核心修复】用你真实的聊天接口地址
    const res = await request.post('/user/chat/message', null, { params })

    if (res.code === 200) {
      ElMessage.success(`已分享给 ${selectedShareFriend.value.userName}`)
      closeShareModal()
    } else {
      ElMessage.error(res.msg || '分享失败')
    }
  } catch (err) {
    console.error('分享发送失败：', err)
    ElMessage.error('发送失败，请检查网络或接口')
  }
}

// 4. 关闭弹窗
const closeShareModal = () => {
  showShareFriendModal.value = false
  selectedShareFriend.value = null
  shareBook.value = null
  shareMessage.value = ''
  showMentionList.value = false
}

// 5. 搜索框回调更新
const updateMentionSearch = (val) => {
  mentionSearchText.value = val
}

// ==============================================
// 🔥 模块11：全局注入（新增分享相关，子组件调用）
// ==============================================
provide('showShareFriendModal', showShareFriendModal)
provide('shareBookToFriend', shareBookToFriend)
provide('closeShareModal', closeShareModal)

//ai部分
const aiLoading = ref(false)
// 🔥 关键：获取宠物的消息方法
const addMessage = petStore.addMessage
// 🤖 AI 角色独立寻址表
const AI_ROLE_ENDPOINTS = {
  ai_steward: '/pet/ai/ai_steward',               // 小管家
  ai_debater: '/pet/ai/ai_debater',               // 辩论者
  chapter_summary: '/pet/ai/chapter_summary',     // 章节总结
  character_analyze: '/pet/ai/character_analyze', // 角色分析
  text_rewrite: '/pet/ai/text_rewrite',           // 文本润色
  generate_quiz: '/pet/ai/generate_quiz',         // 自动出题
  emotion_analyze: '/pet/ai/emotion_analyze',     // TTS情绪识别
  role_analyze: '/pet/ai/role_analyze',           // TTS角色台词识别
  answer: '/pet/ai/chat'                          // 默认闲聊兜底
}
// 🔥 修复：原有sendAiMsg方法（兼容全章节/选中文本两种场景）
const sendAiMsg = async (prompt, actionType = 'answer', extraData = {}) => {
  if(!rightClickSelectedText.value && !prompt) {
    ElMessage.warning('未获取到文本内容'); closeContextMenu(); return
  }
  closeContextMenu()
  if(aiLoading.value){ addMessage('请等待上一个问题解答完成哦~'); return }

  const question = prompt
  if (!question) { ElMessage.warning('未能获取到问题哦~'); return }

  aiLoading.value = true

  // 🔥 核心识别：哪些角色是走流式打字机的？(必须和 Java 后端严格对齐！)
  const isStreamingTask = ['answer', 'ai_steward', 'ai_debater'].includes(actionType)

  let thinkingId = null

  // ========== 思考气泡分层处理 ==========
  if (isStreamingTask) {
    // ✅ 流式任务：加过渡思考气泡，由 SSE 的 PET_AI_START 事件自动清除
    addMessage('🙀 正在思考...', petStore.MESSAGE_TYPES.THINKING)
  } else if (actionType !== 'generateQuiz') {
    // ✅ 非流式任务（非出题）：保留原逻辑，用ID精准移除
    thinkingId = addMessage('🙀 正在思考...', petStore.MESSAGE_TYPES.THINKING)
  }
  // generateQuiz 分支不额外加，因为 showQuizFromContent 内部会自己管理思考气泡

  try {
    if (actionType === 'generateQuiz') {
      await petStore.showQuizFromContent(rightClickSelectedText.value)
    } else {
      const targetEndpoint = AI_ROLE_ENDPOINTS[actionType] || '/pet/ai/chat'
      const payload = {
        content: prompt,
        userId: currentUserId.value,
        actionType: actionType,
        isbn: extraData.isbn || userStore.currentReadingIsbn,
        chapterNumber: extraData.chapterNumber || userStore.currentReadingChapter
      };

      if (isStreamingTask) {
        // 🏎️ 跑车通道：请求发出后立刻放行，文字全交给 SSE 推送
        await request.post(targetEndpoint, payload);
      } else {
        // 🚛 卡车通道：同步等待完整结果
        const res = await request.post(targetEndpoint, payload);

        if (actionType !== 'character_analyze' && actionType !== 'chapter_summary') {
          addMessage(res.data, 'static' || '我处理完啦！')
        }
        return res.data
      }
    }
  } catch (err) {
    // ✅ 异常兜底：不管流式/非流式，出错都清掉所有思考气泡
    petStore.clearThinkingMessages()
    addMessage('😥 信号有点差，你可以重试一下哦~')
    ElMessage.error('网络请求超时')
  } finally {
    aiLoading.value = false
    // 仅非流式任务在这里精准移除思考气泡
    if (thinkingId !== null) petStore.removeMessage(thinkingId)
    rightClickSelectedText.value = ''
  }
}
provide('addMessage', addMessage)
provide('sendAiMsg', sendAiMsg)
provide('aiLoading', aiLoading)
// 屏幕识别
const showCaptureDialog = ref(false)
const captureImageUrl = ref('')
const captureFile = ref(null)
const capturePrompt = ref('')
const captureReply = ref('')
const isSendingCapture = ref(false)

// ====================== 🔥 屏幕截图功能 ======================
const captureScreen = async () => {
  try {
    ElMessage.info('正在截取屏幕...')

    // 截取整个页面
    const canvas = await html2canvas(document.body, {
      useCORS: true,
      scale: 2,
      backgroundColor: '#ffffff'
    })

    // 转换为File对象
    canvas.toBlob((blob) => {
      captureFile.value = new File([blob], `capture_${Date.now()}.png`, { type: 'image/png' })
      captureImageUrl.value = URL.createObjectURL(blob)
      showCaptureDialog.value = true
      ElMessage.closeAll()
    }, 'image/png', 1.0)

  } catch (err) {
    console.error('截图失败', err)
    ElMessage.error('截图失败，请重试')
  }
}
provide('captureScreen', captureScreen)

const handleCaptureScreen = () => {
  // 关闭右键菜单
  showContextMenu.value = false
  captureScreen()
}
provide('handleCaptureScreen',handleCaptureScreen)
// ====================== 🔥 通用AI消息发送方法（全局复用） ======================
// 所有界面都可以调用这个方法发送消息给AI
const sendMessageToAI = async (userMsg, imageFile = null) => {
  // 🔥 并发控制放在方法内部，自己管理
  if (isSendingCapture.value) {
    ElMessage.info('正在处理中，请稍候...')
    return null
  }

  isSendingCapture.value = true // ✅ 在这里设为true

  // 构造本地图片对象
  const localImages = []
  if (imageFile) {
    localImages.push({
      file: imageFile,
      previewUrl: URL.createObjectURL(imageFile)
    })
  }

  // 校验
  if (!userMsg.trim() && localImages.length === 0) {
    ElMessage.warning('消息内容不能为空')
    return
  }

  isSendingCapture.value = true
  let finalMessageContent = userMsg.trim()

  // 🔥 有图片先上传（和聊天界面逻辑完全一致）
  if (localImages.length > 0) {
    ElMessage.info('正在上传图片...', 0)

    const formData = new FormData()
    formData.append('action', 'upload')
    formData.append('commentId', '0')
    formData.append('image', localImages[0].file)

    const uploadRes = await request.post('/user/comment/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (uploadRes.code === 200) {
      const imageUrl = uploadRes.data.imageUrl
      if (finalMessageContent) {
        finalMessageContent = `${finalMessageContent}|||${imageUrl}`
      } else {
        finalMessageContent = imageUrl
      }
    } else {
      ElMessage.error('图片上传失败')
      isSendingCapture.value = false
      return
    }
  }

  try {
    // 5. 请求AI接口
    const res = await request.post('/pet/ai/chat/history', {
      userId: currentUserId.value,
      content: finalMessageContent
    })

    console.log('AI接口返回：', res)
    console.log('AI回复内容：', res.data)

    return res.data
  } catch (err) {
    ElMessage.error('AI回复失败，请重试')
    console.error('AI请求错误', err)
    return null
  } finally {
    isSendingCapture.value = false
  }
}
provide('sendMessageToAI', sendMessageToAI)
// ====================== 🔥 识屏对话框发送消息 ======================
const sendCaptureMessage = async () => {
  if (!captureFile.value) {
    ElMessage.warning('没有截图内容')
    return
  }

  try {
    captureReply.value = '正在努力识别中...'
    // 调用通用AI发送方法
    const reply = await sendMessageToAI(capturePrompt.value, captureFile.value)

    if (reply) {
      captureReply.value = reply
      ElMessage.success('识别完成！')
    }

  } catch (err) {
    ElMessage.error('识别失败，请重试')
    captureReply.value = ''
  } finally {
    isSendingCapture.value = false
  }
}
const handleColseCapture = () => {
  showCaptureDialog.value = false
  resetCaptureState()
}
// ====================== 🔥 重置识屏状态 ======================
const resetCaptureState = () => {
  captureImageUrl.value = ''
  captureFile.value = null
  capturePrompt.value = ''
  captureReply.value = ''
  isSendingCapture.value = false
}
</script>

<template>
  <!-- 根容器必须撑满父元素（#app 的 100vh） -->
  <div class="basic-layout-wrapper">
    <ElContainer class="app-layout-container" :style="{ '--shelf-top-gap': dynamicShelfGap }">
      <div
          class="header-hover-trigger-zone"
          @mouseenter="keepHeaderAlive"
          @mouseleave="startHideCountdown"
      ></div>
      <ElHeader
          class="layout-header"
          :class="{ 'is-auto-hidden': !isHeaderVisible }"
          @mouseenter="keepHeaderAlive"
          @mouseleave="startHideCountdown"
          :style="{
            padding: '0 20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            position: 'relative'
          }"
      >
        <div class="dynamic-nav-island" :class="{ 'is-expanded': isNavExpanded }" v-show="!isMobile">

          <div class="island-ball" @click="handleLogoBallClick">
            <img src="/logo.png" alt="logo" class="logo-img" />
          </div>

          <div class="island-content" @click="isNavExpanded = false">
            <span class="system-title" @click="openAddBookDialog">图书系统</span>
            <div class="island-divider"></div>

            <ElMenu
                mode="horizontal"
                :ellipsis="false"
                :default-active="activeRoute"
                class="island-menu"
            >
              <ElMenuItem index="Home" @click="() => router.push('/home')"><ElIcon><HomeFilled /></ElIcon>主页</ElMenuItem>
              <ElMenuItem index="Bookshelf" @click="() => router.push('/bookshelf')"><ElIcon><CollectionTag /></ElIcon>书架</ElMenuItem>
              <ElMenuItem index="/audiobook" @click="router.push('/audiobook')">
                <el-icon><Headset /></el-icon>
                <span>有声书</span>
              </ElMenuItem>
              <ElMenuItem index="Forum" @click="() => router.push('/forum')"><ElIcon><Comment /></ElIcon>论坛</ElMenuItem>
              <ElMenuItem index="BookSquare" @click="() => router.push('/bookSquare')"><ElIcon><Search /></ElIcon>广场</ElMenuItem>

              <ElMenuItem index="Chat" @click="() => router.push('/chat')">
                <ElIcon><ChatDotRound /></ElIcon>
                <ElBadge :value="chatUnreadCount" :hidden="chatUnreadCount === 0" :offset="[10, 15]">好友</ElBadge>
              </ElMenuItem>

              <ElMenuItem index="Message" @click="() => router.push('/message')">
                <ElIcon><Message /></ElIcon>
                <ElBadge :value="messageUnreadCount" :hidden="messageUnreadCount === 0" :offset="[10, 15]">消息</ElBadge>
              </ElMenuItem>
              <ElMenuItem index="/settings" @click="() => router.push('/settings')">
                <el-icon><Setting /></el-icon>
                <span>设置</span>
              </ElMenuItem>
              <ElMenuItem v-if="userStore.userType === 3" index="AdminPanel" @click="() => router.push('/admin')">
                <el-icon><MagicStick /></el-icon>
                <span>后台</span>
              </ElMenuItem>
              <ElMenuItem index="Profile" @click="() => router.push('/profile')"><ElIcon><User /></ElIcon>我的</ElMenuItem>
            </ElMenu>
          </div>
        </div>

        <div v-show="isMobile" class="hamburger-btn" @click="drawerVisible = true">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 6h18v2H3V6zm0 5h18v2H3v-2zm0 5h18v2H3v-2z"/>
          </svg>
        </div>

        <div class="header-middle" style="flex: auto; margin: 0 40px; display: flex; align-items: center; justify-content: center; gap: 10px;">

          <template v-if="route.path === '/home'">
            <ElSelect v-model="queryType" placeholder="查询" style="width: 120px;">
              <ElOption label="ISBN" value="isbn" />
              <ElOption label="书名查询" value="bookName" />
            </ElSelect>
            <div class="search-container" style="flex: 1; max-width: 400px; margin: 0;">
              <el-autocomplete v-model="searchKey" :fetch-suggestions="fetchSuggestions" :value-key="queryType === 'bookName' ? 'bookname' : 'iSBN'" :filter-method="() => true" placeholder="搜索书籍..." @select="handleSelectSuggest" @compositionstart="handleCompositionStart" @compositionend="handleCompositionEnd" clearable style="width: 100%;">
                <template #default="{ item }">
                  <div class="suggest-item">
                    <span v-if="queryType === 'bookName'">{{ item.bookname }}</span>
                    <span v-if="queryType === 'isbn'">{{ item.iSBN }}</span>
                  </div>
                </template>
              </el-autocomplete>
            </div>
          </template>

          <template v-if="route.path === '/userInfo'">
            <ElSelect v-model="queryUserType" placeholder="类型" style="width: 100px;">
              <ElOption label="userId" value="userId" /><ElOption label="姓名" value="name" />
            </ElSelect>
            <ElInput v-model="searchUserKey" placeholder="输入内容" style="width: 200px;" suffix-icon="Search" />
            <ElButton type="primary" icon="Search" @click="searchUsers" round>搜索</ElButton>
          </template>

          <template v-if="route.path === '/borrowInfo'">
            <ElSelect v-model="borrowQueryType" placeholder="类型" style="width: 100px;">
              <ElOption label="用户ID" value="userId" /><ElOption label="ISBN" value="isbn" />
            </ElSelect>
            <ElInput v-model="borrowSearchKey" placeholder="输入内容" style="width: 250px;" suffix-icon="Search" />
          </template>

          <template v-if="route.path === '/forum'">
            <ElInput v-model="forumSearchUserId" placeholder="输入用户ID搜索" style="width: 300px;" suffix-icon="Search" @input="forumSearchComments" @keyup.enter="forumSearchComments" />
          </template>

          <template v-if="['/bookshelf', '/profile' ,'/bookSquare', '/read-gallery', '/weeklyReport'].includes(route.path) || route.path.includes('/book/detail') || route.path.includes('/book/reader')
          || route.path.includes('/bookSquare/detail') || route.path.includes('/profile/notes') || route.path.includes('/profile/forum-comments')
          || route.path.includes('profile/book-comments') || route.path.includes('user/profile')">
            <span class="header-time-display" @click="handleTimeClick" v-show="!isMobile">{{ currentTime }}</span>
          </template>
        </div>

        <div style="display: flex; align-items: center; gap: 15px; flex-shrink: 0; margin-left: auto;">
          <ElAvatar size="default" @click="handleAvatarClick" class="glass-avatar" style="cursor: pointer;">
            {{ userName.slice(-2) }}
          </ElAvatar>
        </div>
      </ElHeader>
      <!-- 🔥 移动端汉堡菜单抽屉 -->
      <ElDrawer
          v-model="drawerVisible"
          title="导航"
          placement="left"
          :width="'80%'"
          :append-to-body="true"
          class="glass-panel"
      >
        <ElMenu
            mode="vertical"
            class="drawer-menu"
            :active-text-color="isDark ? '#ffd04b' : '#409eff'"
            :text-color="isDark ? '#d1d5db' : '#666'"
            @select="(index) => { router.push(index); drawerVisible = false }"
        >
          <ElMenuItem index="/home" >
            <ElIcon><HomeFilled /></ElIcon> 主页
          </ElMenuItem>
          <ElMenuItem index="/bookshelf">
            <el-icon><Reading /></el-icon> 书架
          </ElMenuItem>
          <ElMenuItem index="/audiobook">
            <el-icon><Headset /></el-icon> 有声书
          </ElMenuItem>
          <ElMenuItem index="/forum">
            <ElIcon><Comment /></ElIcon> 论坛
          </ElMenuItem>
          <ElMenuItem index="/bookSquare">
            <ElIcon><Search /></ElIcon> 广场
          </ElMenuItem>
          <!-- 🔥 新增：好友聊天入口（带未读徽章） -->
          <ElMenuItem index="/chat">
            <ElIcon><ChatDotRound /></ElIcon>
            <ElBadge
                :value="chatUnreadCount"
                :hidden="chatUnreadCount === 0"
                :offset="[8, -2]"
                style="margin-left: 4px;"
            >
              好友
            </ElBadge>
          </ElMenuItem>

          <!-- 🔥 新增：消息通知入口（带未读徽章） -->
          <ElMenuItem index="/message">
            <ElIcon><Message /></ElIcon>
            <ElBadge
                :value="messageUnreadCount"
                :hidden="messageUnreadCount === 0"
                :offset="[8, -2]"
                style="margin-left: 4px;"
            >
              消息
            </ElBadge>
          </ElMenuItem>
          <ElMenuItem index="/settings">
            <el-icon><Setting /></el-icon> 设置
          </ElMenuItem>
          <ElMenuItem v-if="userStore.userType === 3" index="/admin">
            <el-icon><MagicStick /></el-icon> 后台
          </ElMenuItem>
          <ElMenuItem index="/profile">
            <ElIcon><User /></ElIcon> 我的
          </ElMenuItem>
        </ElMenu>
      </ElDrawer>
      <!-- 内容区域 -->
      <!-- 内容区：自动减去 header 高度，撑满剩余空间 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <Transition name="liquid-blur"> <KeepAlive :max="10" :exclude="['BookDetail', 'BookReader', 'UserProfile']">
            <component
                :is="Component"
                :key="route.name"  class="page-container"
            />
          </KeepAlive>
          </Transition>
        </router-view>
      </el-main>
      <ElDialog v-model="isAddBookShow" title="添加新书籍" width="1050px" align-center destroy-on-close>

        <div class="add-book-three-col-layout" style="display: flex; gap: 20px; align-items: stretch; height: 520px;">

          <div class="col-basic-info custom-scroll-y" style="flex: 0 0 48%; display: flex; flex-direction: column; gap: 15px; padding-right: 15px;">

            <div style="display: flex; gap: 15px; align-items: flex-start;">
              <div :style="{
          width: '120px',
          height: '160px',
          overflow: 'hidden',
          flexShrink: 0,
          border: isDark ? '1px solid #374151' : '1px solid #eee',
          borderRadius: '6px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
        }">
                <img :src="imgUrl" style="width: 100%; height: 100%; object-fit: cover;" alt="书籍封面预览" onerror="this.src='/default-book.png'">
              </div>

              <div style="flex: 1; display: flex; flex-direction: column; gap: 12px;">
                <ElInput
                    v-model="addBookForm.Bookname"
                    placeholder="书籍名称 (仅汉字/英文/数字)"
                    @input="filterSpecialChar('Bookname')"
                    maxlength="50"
                    show-word-limit
                >
                  <template #prepend>书名</template>
                </ElInput>

                <ElInput
                    v-model="addBookForm.Author"
                    placeholder="作者 (仅汉字/英文/数字)"
                    @input="filterSpecialChar('Author')"
                    maxlength="50"
                    show-word-limit
                >
                  <template #prepend>作者</template>
                </ElInput>

                <ElInput v-model="addBookForm.ISBN" placeholder="请输入ISBN">
                  <template #prepend>ISBN</template>
                </ElInput>
              </div>
            </div>

            <ElInput
                v-model="addBookForm.Publisher"
                placeholder="出版社 (仅汉字/英文/数字)"
                @input="filterSpecialChar('Publisher')"
                maxlength="50"
                show-word-limit
            >
              <template #prepend>出版社</template>
            </ElInput>

            <div style="display: flex; gap: 15px;">
              <ElDatePicker v-model="addBookForm.PublishDate" type="date" placeholder="出版日期" style="flex: 1;" />
              <ElInputNumber v-model="addBookForm.all_book" :min="1" placeholder="总量" style="width: 140px;" />
            </div>

            <ElInput v-model="addBookForm.PictureName" placeholder="输入图片URL/本地路径" @input="handlePictureChange">
              <template #prepend>图片路径</template>
            </ElInput>

            <div style="display: flex; flex-direction: column; gap: 8px;">
              <span :style="{ color: isDark ? '#d1d5db' : '#606266', fontSize: '13px' }">书籍简介：</span>
              <ElInput
                  v-model="addBookForm.Information"
                  type="textarea"
                  :rows="4"
                  maxlength="1000"
                  show-word-limit
                  placeholder="请输入书籍信息（最多1000字）"
              />
            </div>
          </div>

          <el-divider direction="vertical" style="height: 100%; margin: 0;" />

          <div class="col-book-types" style="flex: 0 0 22%; display: flex; flex-direction: column;">
            <h4 :style="{ color: isDark ? '#e5e7eb' : '#333', margin: '0 0 12px 0', display: 'flex', alignItems: 'center', gap: '6px' }">
              📑 书籍类型
            </h4>
            <div class="custom-scroll-y" style="flex: 1; padding-right: 10px;">
              <ElRadioGroup v-model="addBookForm.Type" class="two-col-grid-group">
                <ElRadio
                    v-for="type in bookTypeList"
                    :key="type.id"
                    :label="type.id"
                    class="grid-selector-item"
                    :class="{ dark: isDark }"
                    border
                >
                  {{ type.typeName }}
                </ElRadio>
              </ElRadioGroup>
            </div>
          </div>

          <el-divider direction="vertical" style="height: 100%; margin: 0;" />

          <div class="col-book-tags" style="flex: 1; display: flex; flex-direction: column;">
            <h4 :style="{ color: isDark ? '#e5e7eb' : '#333', margin: '0 0 12px 0', display: 'flex', alignItems: 'center', gap: '6px' }">
              🏷️ 书籍标签
            </h4>
            <div class="custom-scroll-y" style="flex: 1; padding-right: 10px;">
              <ElCheckboxGroup v-model="addBookForm.tagIds" class="two-col-grid-group">
                <ElCheckbox
                    v-for="tag in allTagList"
                    :key="tag.id"
                    :label="tag.id"
                    class="grid-selector-item"
                    :class="{ dark: isDark }"
                    border
                >
                  {{ tag.name }}
                </ElCheckbox>
              </ElCheckboxGroup>
            </div>
          </div>

        </div>

        <template #footer>
          <div style="padding-top: 10px;">
            <ElButton @click="isAddBookShow = false" round>取消</ElButton>
            <ElButton type="primary" @click="submitAddBook" round>确认保存</ElButton>
          </div>
        </template>
      </ElDialog>
    </ElContainer>
    <div class="basic-layout"> <!-- 加这一行！ -->
      <GlobalContextMenu ref="contextMenuRef" />
    </div>
    <!-- ====================== 🔥 全局：分享书籍给好友弹窗（所有页面复用） ====================== -->
    <div
        v-if="showShareFriendModal"
        class="share-modal-overlay"
        :style="{ background: isDark ? 'rgba(0,0,0,0.7)' : 'rgba(0,0,0,0.5)' }"
        @click="closeShareModal"
    >
      <div
          class="share-modal"
          :style="{
      background: isDark ? '#1D1E1F' : '#fff',
      border: isDark ? '1px solid #374151' : '1px solid #eee'
    }"
          @click.stop
      >
        <!-- 标题栏 -->
        <div class="share-modal-header" :style="{ color: isDark ? '#e5e7eb' : '#333' }">
          <span>{{ selectedShareFriend ? '分享书籍' : '选择好友' }}</span>
          <i class="el-icon-close" @click="closeShareModal"></i>
        </div>

        <!-- 第一步：选择好友（复用全局@艾特） -->
        <div v-if="!selectedShareFriend" class="share-friend-select">
          <el-input
              v-model="mentionSearchText"
              placeholder="搜索好友（支持@关键词）"
              prefix-icon="el-icon-search"
              @input="handleMentionInput(mentionSearchText, $refs, updateMentionSearch)"
              @keydown="handleMentionKeydown"
          ></el-input>

          <!-- 好友下拉列表 -->
          <div
              v-if="showMentionList"
              class="mention-user-list share-mention-list"
              :style="{
          background: isDark ? '#2d3748' : '#fff',
          border: isDark ? '1px solid #374151' : '1px solid #eee'
        }"
          >
            <div
                v-for="(user, index) in filteredMentionUsers"
                :key="user.userId"
                class="mention-item"
                :class="{ active: index === selectedMentionIndex }"
                :style="{ color: isDark ? '#e5e7eb' : '#333' }"
                @click="selectShareFriend(user)"
            >
              {{ user.userName }} (ID:{{ user.friendId }})
            </div>
          </div>
        </div>

        <!-- 第二步：发送分享（书籍预览+留言） -->
        <div v-else class="share-send-box">
          <!-- ✅ 通用卡片实时预览（支持所有5种类型） -->
          <div v-if="previewBookCards.length > 0" style="margin-bottom: 15px;">
            <div
                v-for="card in previewBookCards"
                :key="card.id"
                class="book-share-card"
                style="max-width: 100%;"
            >
              <!-- 1. 书籍卡片（不变） -->
              <template v-if="card.type === 'book'">
                <img
                    v-if="!card.loading"
                    :src="IMAGE_BASE_URL + (card.data?.pictureName || '/default-book.png')"
                    class="book-share-card-cover"
                    alt="书籍封面"
                >
                <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                <div class="book-share-card-info">
                  <p class="book-title">{{ card.loading ? '加载中...' : card.data?.bookname || '未知书籍' }}</p>
                  <p class="book-author">{{ card.loading ? '' : `作者：${card.data?.author || '未知'}` }}</p>
                  <p class="book-rating">{{ card.loading ? '' : `⭐ 评分：${card.data?.star || 0}` }}</p>
                  <p class="book-desc">{{ card.loading ? '' : (card.data?.information?.slice(0, 50) + '...' || '暂无简介') }}</p>
                </div>
              </template>

              <!-- 🔥 修复2：书籍评论卡片类型判断（从book-comment改成bookComment） -->
              <template v-else-if="card.type === 'bookComment'">
                <img
                    v-if="!card.loading"
                    :src="IMAGE_BASE_URL + (card.data?.book?.pictureName || '/default-book.png')"
                    class="book-share-card-cover"
                    alt="书籍封面"
                >
                <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                <div class="book-share-card-info">
                  <p class="book-title">📚 书籍评论</p>
                  <p class="book-author">{{ card.loading ? '书籍：加载中' : `书籍：${card.data?.book?.bookname || '未知书籍'}` }}</p>
                  <p class="book-rating">{{ card.loading ? '⭐ 评分：--' : `⭐ 评分：${card.data?.star || 0}` }}</p>
                  <p class="book-desc">{{ card.loading ? '评论内容：加载中...' : `评论内容：${card.data?.comment?.slice(0, 40) + '...' || '暂无内容'}` }}</p>
                  <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">
                    {{ card.loading ? '' : `发布时间：${formatTime(card.data?.time, true)}` }}
                  </p>
                </div>
              </template>

              <!-- 🔥 修复3：用户论坛评论卡片类型判断（从user-comment改成userComment） -->
              <template v-else-if="card.type === 'userComment'">
                <div class="book-share-card-cover" style="background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:24px;">💬</div>
                <div class="book-share-card-info">
                  <p class="book-title">🔄 论坛评论</p>
                  <p class="book-author">{{ card.loading ? '作者：加载中' : `作者：${card.data?.user?.userName || '未知用户'} (ID:${card.data?.userid || '--'})` }}</p>
                  <p class="book-rating">{{ card.loading ? '👍 点赞：-- | 💬 回复：--' : `👍 点赞：${card.data?.prefer || 0} | 💬 回复：${getSubTotal(card.data?.commentId) || 0}` }}</p>
                  <p class="book-desc">{{ card.loading ? '评论内容：加载中...' : `评论内容：${card.data?.userComment?.slice(0, 40) + '...' || '暂无内容'}` }}</p>
                  <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">
                    {{ card.loading ? '' : `发布时间：${formatTime(card.data?.commentTime, true)}` }}
                  </p>
                </div>
              </template>

              <!-- 4. 用户信息卡片（不变，根据你后端返回的字符串字段调整） -->
              <template v-else-if="card.type === 'user'">
                <div class="book-share-card-cover" style="background:#e6f7ff; display:flex; align-items:center; justify-content:center; font-size:24px;">👤</div>
                <div class="book-share-card-info">
                  <p class="book-title">👤 用户信息</p>
                  <p class="book-author">{{ card.loading ? '用户名：加载中' : `用户名：${card.data?.userName || '未知用户'} (ID:${card.id})` }}</p>
                  <!-- ✅ 修复：后端直接返回字符串，不需要数字映射 -->
                  <p class="book-rating">{{ card.loading ? '性别：-- | 类型：--' : `性别：${card.data?.sex || '保密'} | 类型：${card.data?.typeName || '普通用户'}` }}</p>
                  <p class="book-desc">{{ card.loading ? '阅读时长：--小时 | 个人介绍：加载中...' : `阅读时长：${Math.floor((card.data?.read_time_long || 0) / 3600)}小时 | 个人介绍：${card.data?.bio?.slice(0, 30) + '...' || '暂无'}` }}</p>
                </div>
              </template>

              <!-- 5. 笔记卡片（不变） -->
              <template v-else-if="card.type === 'note'">
                <img
                    v-if="!card.loading"
                    :src="IMAGE_BASE_URL + (card.data?.book?.pictureName || '/default-book.png')"
                    class="book-share-card-cover"
                    alt="书籍封面"
                >
                <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                <div class="book-share-card-info">
                  <p class="book-title">📝 读书笔记</p>
                  <p class="book-author">{{ card.loading ? '出自：《加载中》第--章' : `出自：《${card.data?.book?.bookname || '未知书籍'}》第${card.data?.chapterId || '--'}章` }}</p>
                  <p class="book-rating">{{ card.loading ? '类型：--' : `类型：${card.data?.typeName || '默认'}` }}</p>
                  <p class="book-desc">{{ card.loading ? '原文：加载中...' : `原文：${card.data?.text?.slice(0, 30) + '...' || '暂无'}` }}</p>
                  <p class="book-desc" style="margin-top:4px; color:#666;">
                    {{ card.loading ? '批注：加载中...' : `批注：${card.data?.readerComment?.slice(0, 30) + '...' || '暂无'}` }}
                  </p>
                </div>
              </template>
            </div>
          </div>

          <!-- 留言输入框 -->
          <el-input
              v-model="shareMessage"
              type="textarea"
              :rows="3"
              placeholder="说点什么吧~"
              :style="{ margin: '10px 0' }"
          ></el-input>

          <!-- 发送按钮 -->
          <div class="share-btn-group">
            <el-button @click="closeShareModal" round>取消</el-button>
            <el-button type="primary" @click="sendShareMessage" round>发送</el-button>
          </div>
        </div>
      </div>
    </div>
    <!-- 🔥 屏幕识别对话框 -->
    <el-dialog
        v-model="showCaptureDialog"
        title="屏幕内容识别"
        width="600px"
        :close-on-click-modal="false"
        @closed="resetCaptureState"
    >
      <!-- 截图预览 -->
      <div class="capture-preview">
        <el-image
            :src="captureImageUrl"
            fit="contain"
            alt="截图预览"
            style="width: 100%; max-height: 300px; cursor: pointer;"
            preview
            preview-teleported
            :preview-src-list="[captureImageUrl]"
        />
      </div>

      <!-- AI回复区域 -->
      <div v-if="captureReply" class="capture-reply">
        <div class="reply-title">🤖 AI识别结果：</div>
        <div class="reply-content">
          <span v-html="formatMentionText(parseBookLinkToCard(captureReply))"></span>
        </div>
      </div>

      <!-- 用户输入框 -->
      <el-input
          v-model="capturePrompt"
          type="textarea"
          :rows="3"
          placeholder="请输入你想让AI做什么（例如：帮我总结这段文字、解释这个代码、提取表格数据）"
          style="margin-top: 16px;"
      />
      <ElButton @click="handleColseCapture " round>取消</ElButton>
      <ElButton
          type="primary"
          :loading="isSendingCapture"
          @click="sendCaptureMessage"
          round
      >
        {{ isSendingCapture ? '识别中...' : '立即识别' }}
      </ElButton>

    </el-dialog>
  </div>
</template>

<style scoped>
/* 关键：让布局撑满父容器 */
.basic-layout-wrapper {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 顶部导航栏：强制去掉默认 padding，避免挤压 */
.layout-header {
  /* 🔥 核心1：四周留出呼吸空隙，让它“悬浮”起来 */
  margin: 10px 20px 0 20px !important;
  width: calc(100% - 40px) !important; /* 减去左右边距，防止溢出屏幕 */

  /* 🔥 核心2：极其优雅的全面大圆角 */
  border-radius: 16px !important;

  /* 🔥 核心3：升级为全包裹边框（不再只是 border-bottom） */
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;

  /* 材质底色与毛玻璃滤镜 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);

  /* 增加平滑材质过渡动画 */
  transition: all 1.5s ease-in-out, width 0.3s ease, margin 0.3s ease !important;
}

/* 内容区：强制撑满，去掉 Element Plus 默认 padding */
.layout-main {
  padding: 0 !important;
  height: calc(100% - 60px); /* 减去 header 高度 */
  position: relative;
}


.el-header {
  height: 60px !important;
}
.el-menu--horizontal {
  border-bottom: none;
}
/* 隐藏横向滚动条，保留竖向滚动条 */
:deep(.el-main) {
  width: 100%;
  height: 100%;
  overflow-x: hidden; /* 彻底禁止横向滚动 */
  overflow-y: auto;   /* 允许竖向滚动 */
  padding: 0;         /* 移除默认内边距防止挤压 */
}

/* 如果是 ElScrollbar 产生的横向滚动条，也一并隐藏 */
:deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.suggest-item {
  padding: 6px 12px;
  cursor: pointer;
}
.suggest-item:hover {
  background-color: #f5f5f5;
}
.search-container {
  display: flex;
  align-items: center;
  margin: 20px;
}


/* ======================
🔥 完美兼容所有页面的滑动动画（核心修复）
======================= */
/* 所有子页面统一容器类 */
.page-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: auto;
}

/* 进入和离开动画：时间完全一致，同时进行 */
.slide-seamless-enter-active,
.slide-seamless-leave-active {
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 进入前：新页面在右侧 */
.slide-seamless-enter-from {
  transform: translateX(100%);
}

/* 离开后：旧页面滑到左侧 */
.slide-seamless-leave-to {
  transform: translateX(-100%);
}

/* 进入后/离开前：在屏幕中间 */
.slide-seamless-enter-to,
.slide-seamless-leave-from {
  transform: translateX(0);
}

/* 隐藏子页面的滚动条（和全局保持一致） */
.page-container::-webkit-scrollbar {
  display: none;
}
.page-container {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 汉堡按钮 */
.hamburger-btn {
  font-size: 24px;
  cursor: pointer;
  color: var(--el-text-color-primary);
  display: none;
}

/* ======================================
   弹窗内部网格与选项卡样式优化
======================================== */
.two-col-grid-group {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  width: 100%;
}

/* 统一复选框和单选框的外观结构 */
.grid-selector-item {
  margin-right: 0 !important;
  width: 100%;
  border-radius: 6px !important;
  transition: all 0.2s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 悬浮微动效 */
.grid-selector-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* 暗黑模式下边框适配 */
.grid-selector-item.dark {
  border-color: #4b5563 !important;
  background: rgba(255, 255, 255, 0.02);
}

/* ======================================
   弹窗内部滚动条美化
======================================== */
.custom-scroll-y {
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-dark) transparent;
}

.custom-scroll-y::-webkit-scrollbar {
  width: 6px;
}
.custom-scroll-y::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}
.custom-scroll-y::-webkit-scrollbar-thumb {
  background: var(--el-border-color-dark);
  border-radius: 4px;
}

/* ======================================
   输入框前缀 (prepend) 样式微调，使其紧凑
======================================== */
.col-basic-info :deep(.el-input-group__prepend) {
  width: 60px;
  text-align: center;
  padding: 0 10px;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

@media (max-width: 768px) {
  .hamburger-btn {
    display: block;
  }
  /* 1. 去掉中间区域左右大margin */
  .header-middle {
    margin: 0 10px !important; /* 原来的40px → 10px */
    gap: 5px !important;       /* 选择器和输入框间距缩小 */
  }

  /* 2. 去掉搜索容器的额外margin */
  .search-container {
    margin: 0 !important;
    flex: 1 !important; /* 输入框占满剩余空间 */
  }

  /* 3. 缩小ElSelect宽度，避免挤压 */
  :deep(.el-select) {
    width: 80px !important;
  }
  /* 头部间距适配 */
  .layout-header {
    padding: 0 10px !important;
    height: 50px !important;
  }
  /* 中间区域间距重置 */
  .layout-header > div:nth-child(2) {
    margin: 0 10px !important;
  }
  .el-menu--horizontal {
    display: none !important; /* 简单粗暴：隐藏桌面菜单，后期可加汉堡菜单 */
  }
  .el-header {
    height: 50px !important;
  }
  .el-autocomplete, .el-input {
    width: 100% !important;
  }
  .header-middle {
    margin: 0 10px !important;
    gap: 6px !important;
  }
  .search-type-select :deep(.el-select__wrapper) {
    width: 70px !important; /* 手机端下拉框缩小到70px */
  }
  /* ======================
  🔥 修复移动端抽屉菜单高度/内边距问题
  ======================= */
  .drawer-menu {
    height: 100% !important;       /* 占满Drawer的可用高度，不被压缩 */
    border-right: none !important; /* 去掉默认的分割线，更清爽 */
    padding: 10px 0 !important;   /* 上下加内边距，避免内容被截断 */
  }
  .drawer-menu :deep(.el-menu-item) {
    padding-left: 20px !important; /* 调整菜单项左边距，和标题对齐 */
  }

  /* ======================
  🔥 抽屉菜单百分比适配优化
  ======================= */
  /* 限制抽屉最大宽度，避免平板/大屏上太宽 */
  :deep(.el-drawer__wrapper) {
    max-width: 300px !important; /* 最多不超过300px，保证体验 */
  }

  /* 菜单内边距对齐，和标题保持一致 */
  .drawer-menu {
    height: 100% !important;
    border-right: none !important;
    padding: 10px 0 !important;
  }

  /* ======================
  🔥 导航栏搜索框居中修复
  ======================= */
  /* 1. 确保导航栏整体是flex布局，三个部分分布在左/中/右 */
  :deep(.el-header) {
    display: flex !important;
    align-items: center !important;
    justify-content: space-between !important;
    padding: 0 16px !important;
  }

  /* ======================================
     🔥 修复移动端抽屉菜单毛玻璃透光问题 (终极权重版)
  ======================================== */
  /* 1. 彻底清除抽屉内容区的默认 padding 和背景，让菜单贴边 */
  :deep(.el-drawer__body) {
    padding: 0 !important;
    background-color: transparent !important;
  }

  /* 2. 组合选择器提高权重 (ul.drawer-menu.el-menu)，强行碾压全局 style.css */
  ul.drawer-menu.el-menu {
    background: transparent !important;
    background-color: transparent !important;
    backdrop-filter: none !important; /* 拔除多余的第二层毛玻璃滤镜 */
    -webkit-backdrop-filter: none !important;
    border-right: none !important;
    padding-top: 15px !important; /* 顶部留出一点呼吸空间 */
  }

  /* 3. 彻底扒掉所有菜单项的底色 */
  ul.drawer-menu.el-menu :deep(.el-menu-item) {
    background: transparent !important;
    background-color: transparent !important;
  }

  /* 4. 保留淡淡的悬浮与选中微光，质感拉满 */
  ul.drawer-menu.el-menu :deep(.el-menu-item:hover),
  ul.drawer-menu.el-menu :deep(.el-menu-item.is-active) {
    background-color: rgba(128, 128, 128, 0.1) !important;
  }
}

/* 把你之前的@艾特和书籍卡片样式全部复制到这里 */
/* ====================== 🔥 @艾特功能样式 ====================== */
.mention-user-list {
  position: fixed;
  z-index: 99999 !important; /* 确保在弹窗之上 */
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 240px;
  overflow-y: auto;
  min-width: 200px;
}

.mention-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.mention-user-item:hover,
.mention-user-item.active {
  background-color: #f5f7fa;
}

.mention-user-name {
  font-size: 14px;
  font-weight: 500;
}

.mention-user-id {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.mention-no-result {
  padding: 12px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.mention-link {
  color: #409eff;
  text-decoration: none;
}

.mention-link:hover {
  text-decoration: underline;
}

/* 暗黑模式适配 */
:deep(.dark-mode .mention-user-list) {
  background: #1f2937;
  color: #e5e7eb;
}

:deep(.dark-mode .mention-user-item:hover),
:deep(.dark-mode .mention-user-item.active) {
  background-color: #374151;
}
/* 让@好友文本高亮，模拟链接效果 */
div[style*="pre-wrap"] {
  user-select: text;
}
div[style*="pre-wrap"]::selection {
  background: #409eff;
  color: #fff;
}
.mention-text:hover {
  text-decoration: underline;
}
:deep(.dark-mode .mention-text) {
  color: #60a5fa;
}
/* ====================== 🔥 优化版：书籍分享卡片样式（分行信息） ====================== */
:deep(.book-share-card) {
  display: flex;
  align-items: flex-start; /* 顶部对齐，适配多行文字 */
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid #f0ebe4;
  margin: 4px 0;
  cursor: pointer;
  transition: all 0.2s ease;
  max-width: 420px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
:deep(.book-share-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border-color: #1890ff;
}
:deep(.book-share-card-cover) {
  width: 60px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}
:deep(.book-share-card-info) {
  text-align: left !important;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px; /* 每行信息间距 */
  overflow: hidden;
}

/* 书名：单独一行，加粗突出 */
:deep(.book-title) {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 作者：单独一行 */
:deep(.book-author) {
  font-size: 12px;
  color: #666;
  margin: 0;
}
/* 评分：单独一行 */
:deep(.book-rating) {
  font-size: 12px;
  color: #ff9500;
  margin: 0;
}
/* 简介：单独一行，自动换行+截断 */
:deep(.book-desc) {
  font-size: 12px;
  color: #999;
  margin: 0;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 🔥 暗黑模式适配 */
:deep(.dark-mode .book-share-card) {
  background: #2d3748 !important;
  border-color: #374151 !important;
}
:deep(.dark-mode .book-share-card:hover) {
  border-color: #60a5fa !important;
}
:deep(.dark-mode .book-title) {
  color: #e5e7eb !important;
}
:deep(.dark-mode .book-author) {
  color: #d1d5db !important;
}
:deep(.dark-mode .book-desc) {
  color: #9ca3af !important;
}

/* ====================== 横向滚动容器 ====================== */
.multi-card-slider-wrapper {
  display: flex !important;
  flex-direction: row !important; /* 强制横向排列，防止被聊天气泡的 column 污染 */
  align-items: center;
  position: relative;
  width: 100%;
  max-width: 460px;
}

/* 隐藏原生滚动条，更像 App 的体验 */
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
  min-height: 130px; /* 防止高度塌陷挤压卡片 */
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.multi-card-scroll-view::-webkit-scrollbar {
  display: none;
}

/* 左右控制箭头 */
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
}
.slider-arrow:hover {
  background: #fff;
  color: #1890ff;
  transform: scale(1.1);
}
.slider-arrow.left {
  left: -14px; /* 悬浮在内容左边缘 */
}
.slider-arrow.right {
  right: -14px; /* 悬浮在内容右边缘 */
}

.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 4. 彻底阻断 self-wrapper 对内部简易卡片的 margin 污染 */
:deep(.self-wrapper .simplified-card),
:deep(.self-wrap .simplified-card) {
  margin: 0 !important; /* 强制清除发送方样式自带的 margin */
}

/* ====================== 精简版书籍卡片 (番茄同款) ====================== */
:deep(.simplified-card) {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 90px !important;
  max-width: 90px !important; /* 覆盖父级最大宽度 */
  flex-shrink: 0 !important;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  cursor: pointer;
}
:deep(.simplified-card:hover) {
  transform: translateY(-2px);
}

/* 封面与评分悬浮窗的父级 */
:deep(.simplified-cover-wrapper) {
  position: relative;
  width: 90px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

:deep(.simplified-cover-wrapper .book-share-card-cover) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

/* 评分小角标悬浮在左下角 (跟番茄一模一样) */
:deep(.simplified-cover-wrapper .badge) {
  position: absolute;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #ff9500;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 6px;
  border-top-right-radius: 6px;
}

/* 底部书名一行省略 */
:deep(.simplified-card .book-title) {
  width: 100%;
  font-size: 13px;
  color: #333;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 暗黑模式兼容 */
:deep(.dark-mode .slider-arrow) {
  background: rgba(50, 50, 50, 0.9);
  color: #ccc;
}
:deep(.dark-mode .simplified-card .book-title) {
  color: #e5e7eb;
}

/* 全局分享书籍弹窗样式 */
.share-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.share-modal {
  width: 450px;
  max-width: 90vw;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  overflow: hidden;
  animation: modalFadeIn 0.3s ease;
}
.share-modal-header {
  padding: 12px 16px;
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
}
.share-modal-header i {
  cursor: pointer;
  font-size: 16px;
}
.share-friend-select {
  padding: 15px;
}
.share-mention-list {
  position: static !important;
  margin-top: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.mention-item {
  padding: 8px 12px;
  cursor: pointer;
}
.mention-item.active {
  background: #409eff;
  color: #fff !important;
}
.share-send-box {
  padding: 15px;
}
.preview-card-wrapper {
  margin-bottom: 10px;
}
.book-share-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
.book-share-card-cover {
  width: 60px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.book-share-card-info p {
  margin: 2px 0;
  font-size: 12px;
  line-height: 1.2;
}
.book-title {
  font-weight: bold;
  font-size: 14px !important;
}
.share-btn-group {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}
/* 🔥 主评论展示图片（固定大小，强制裁剪） */
:deep(.comment-images) {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
  max-width: 600px;
  position: relative;
}
:deep(.comment-images img) {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}
:deep(.comment-images img:hover) {
  transform: scale(1.02);
}

/* 🔥 子评论展示图片（固定大小，强制裁剪） */
:deep(.sub-comment-images) {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-top: 8px;
  max-width: 400px;
  position: relative;
}
:deep(.sub-comment-images img) {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
}

/* 🔥 图片折叠/展开按钮 */
:deep(.expand-btn) {
  position: absolute;
  left: calc(100% / 3);
  width: calc(100% / 3);
  bottom: 0;
  height: 100px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 0 0 4px 0;
  box-sizing: border-box;
}
:deep(.sub-expand-btn) {
  height: 80px;
  font-size: 12px;
}

/* 识屏对话框 */
.capture-preview {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.capture-reply {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.reply-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.reply-content {
  line-height: 1.6;
  color: #606266;
}

/* 核心：给单选/复选组开启4列网格布局 */
:deep(.four-grid-group) {
  display: grid !important;
  grid-template-columns: repeat(4, 1fr); /* 一行4个 */
  gap: 10px; /* 格子间距 */
}

/* 重置单个单选框默认样式，消除独占整行 */
:deep(.grid-radio-item) {
  width: 100% !important;
  margin: 0 !important;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 5px 4px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 暗黑模式单选框 */
:deep(.grid-radio-item.dark) {
  border-color: #4b5563;
  color: #d1d5db;
}

/* 重置单个复选框默认样式 */
:deep(.grid-check-item) {
  width: 100% !important;
  margin: 0 !important;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 5px 4px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 暗黑模式复选框 */
:deep(.grid-check-item.dark) {
  border-color: #4b5563;
  color: #d1d5db;
}

/* 隐藏原生单选圆点/复选框（可选，纯格子点击区域更大） */
/* :deep(.grid-radio-item .el-radio__input),
:deep(.grid-check-item .el-checkbox__input) {
  display: none;
} */

:deep(.four-grid-group) {
  max-height: 120px;
  overflow-y: auto;
}

/* ======================================
   💎 全局骨架：头部导航 & 移动端抽屉毛玻璃化
======================================== */
/* 1. 顶部导航栏 */
.layout-header {
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border-bottom: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

/* 2. 导航栏内部的菜单背景设为透明 */
:deep(.el-menu--horizontal),
:deep(.el-menu) {
  background-color: transparent !important;
  border-bottom: none !important;
  border-right: none !important;
}
:deep(.el-menu-item) {
  background-color: transparent !important;
}
:deep(.el-menu-item:hover) {
  background-color: rgba(128, 128, 128, 0.1) !important;
}

/* 3. 移动端侧边抽屉 */
:deep(.el-drawer) {
  background: var(--glass-bg) !important;
  backdrop-filter: blur(20px) saturate(120%);
  -webkit-backdrop-filter: blur(20px) saturate(120%);
  border-left: 1px solid var(--glass-border) !important;
}
:deep(.el-drawer__header) {
  border-bottom: 1px solid var(--glass-border) !important;
  margin-bottom: 0;
  padding-bottom: 16px;
}

/* ======================================
   🪟 控件融化：输入框 / 下拉框 / 选择器
======================================== */
/* 统一消除默认白底/黑底，引入半透明质感 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper) {
  background-color: rgba(255, 255, 255, 0.2) !important; /* 浅浅的半透明基底 */
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid var(--glass-border) !important;
  box-shadow: none !important;
}

/* 暗黑模式下的输入框底色微调，保证文字清晰 */
:deep(.dark-mode .el-input__wrapper),
:deep(.dark-mode .el-textarea__inner),
:deep(.dark-mode .el-select__wrapper) {
  background-color: rgba(0, 0, 0, 0.2) !important;
}

/* 获取焦点时的微光特效 */
:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
  background-color: rgba(255, 255, 255, 0.3) !important;
}

/* ======================================
   🔮 弹窗 & 浮层：分享好友卡片、艾特列表
======================================== */
/* 1. 分享书籍弹窗主体 */
.share-modal {
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}
.share-modal-header {
  border-bottom: 1px solid var(--glass-border) !important;
}

/* 2. 艾特好友下拉列表 (@功能) */
.mention-user-list {
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

.mention-user-item:hover,
.mention-item.active {
  background-color: rgba(64, 158, 255, 0.2) !important; /* 让高亮也变得通透 */
}

/* 3. 拦截内联强行覆盖的黑色遮罩，让遮罩变柔和 */
.share-modal-overlay {
  background: rgba(0, 0, 0, 0.3) !important;
  backdrop-filter: blur(4px);
}

/* ======================================
   🏝️ 动态悬浮岛导航 (Sphere to Capsule)
======================================== */
.dynamic-nav-island {
  display: flex;
  align-items: center;
  background: var(--glass-bg);
  backdrop-filter: blur(20px) saturate(120%);
  -webkit-backdrop-filter: blur(20px) saturate(120%);
  border: 1px solid var(--glass-border);
  border-radius: 25px; /* 高度50px的一半，完美圆角胶囊 */
  height: 48px;
  overflow: hidden;
  max-width: 48px; /* 折叠状态：强制收缩成一颗球 */
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  /* 弹簧物理动画，展开时带有极强的 Q弹 感 */
  transition: max-width 0.6s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
  position: absolute; /* 左侧浮动脱离文档流，不挤压中间搜索框 */
  left: 20px;
  z-index: 100;
}

.dynamic-nav-island.is-expanded {
  max-width: 1200px; /* 展开后的最大宽度，足以容纳整个菜单 */
  padding-right: 15px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

/* 左侧触发球（Logo） */
.island-ball {
  width: 48px;
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 50%;
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.3s;
}
.island-ball:hover {
  background: rgba(128, 128, 128, 0.1);
}
.dynamic-nav-island.is-expanded .island-ball {
  /* 点击展开时，Logo炫酷地旋转一圈 */
  transform: rotate(360deg);
}
.logo-img {
  width: 26px;
  height: 26px;
  object-fit: cover;
  border-radius: 50%;
}

/* 展开后的容器内容 */
.island-content {
  display: flex;
  align-items: center;
  opacity: 0;
  white-space: nowrap;
  pointer-events: none;
  transition: opacity 0.3s ease;
  transition-delay: 0s;
}
.dynamic-nav-island.is-expanded .island-content {
  opacity: 1;
  pointer-events: auto;
  transition-delay: 0.2s; /* 延迟0.2秒出现文字，等胶囊变长后再显示，防止穿模 */
}

/* 标题和分割线 */
.system-title {
  font-size: 15px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  cursor: pointer;
  margin: 0 10px 0 5px;
  transition: color 0.2s;
}
.system-title:hover { color: #409eff; }
.island-divider {
  width: 1px;
  height: 20px;
  background: var(--glass-border);
  margin: 0 5px;
}

/* 胶囊内的菜单深度定制：移除所有白底和边框，彻底融入毛玻璃 */
.island-menu.el-menu {
  border-bottom: none !important;
  background: transparent !important;
  height: 48px;
}
.island-menu :deep(.el-menu-item) {
  height: 36px !important;
  line-height: 36px !important;
  margin: 6px 2px !important;
  padding: 0 14px !important;
  border-radius: 18px !important;
  font-size: 14px;
  border-bottom: none !important;
  color: var(--el-text-color-regular) !important;
  transition: all 0.2s ease;
}
.island-menu :deep(.el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.15) !important;
  color: #409eff !important;
  font-weight: bold;
}
.island-menu :deep(.el-menu-item:hover:not(.is-active)) {
  background: rgba(128, 128, 128, 0.1) !important;
}

/* ======================================
   ⏱️ 极简纯时间 (专属主题色发光)
======================================== */
.header-time-display {
  font-size: 32px;
  font-weight: 900;
  letter-spacing: 2px;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  cursor: pointer;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);

  /* 🌞 白天模式：森林薄荷 (#ADC6B0) 碰撞 鎏金 (#EBB55C) */
  background: linear-gradient(135deg, #ADC6B0 0%, #7A9B8C 50%, #EBB55C 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 6px rgba(173, 198, 176, 0.5));
  transition: transform 0.3s, filter 0.3s;
}

.header-time-display:hover {
  transform: translateX(-50%) scale(1.05);
  /* 悬浮时散发微弱的金光 */
  filter: drop-shadow(0 4px 12px rgba(235, 181, 92, 0.6));
}

/* 🌙 黑夜模式：冰封幽蓝 (#8191A6) 碰撞 暗银星核 (#e5e7eb)
   注：因为你的黑夜背景色(如#1D2D46,#2E3B3E)很深，这里时间文字采用亮色反差，并透出淡淡的金芒 */
html.dark .header-time-display,
.dark-mode .header-time-display {
  background: linear-gradient(135deg, #8191A6 0%, #e5e7eb 60%, #CFA56B 100%);
  -webkit-background-clip: text;
  filter: drop-shadow(0 2px 8px rgba(207, 165, 107, 0.3));
}

html.dark .header-time-display:hover,
.dark-mode .header-time-display:hover {
  filter: drop-shadow(0 4px 15px rgba(207, 165, 107, 0.7));
}

/* =================================================================
   🚀 方案 A：苹果 iPadOS 级空间纵深切换 (Scale Depth)
   ================================================================= */
.scale-depth-enter-active,
.scale-depth-leave-active {
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.45s ease;
}

/* 新页面：从正前方 104% 的大小、透明状态，往下“盖”到 100% */
.scale-depth-enter-from {
  opacity: 0;
  transform: scale(1.04);
}

/* 旧页面：往后方 96% 的大小沉入背景黑暗中 */
.scale-depth-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.scale-depth-enter-to,
.scale-depth-leave-from {
  opacity: 1;
  transform: scale(1);
}

/* =================================================================
   💧 方案 B：液态水晶专享 —— 光学失焦凝结切换 (Liquid Blur)
   ================================================================= */
.liquid-blur-enter-active,
.liquid-blur-leave-active {
  transition: all 1.0s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 离开时/进入前：画面瞬间失去焦点（模糊16px），并伴随轻微的水滴下坠感（12px） */
.liquid-blur-enter-from,
.liquid-blur-leave-to {
  opacity: 0;
  filter: blur(16px);
  transform: translateY(12px);
}

.liquid-blur-enter-to,
.liquid-blur-leave-from {
  opacity: 1;
  filter: blur(0px);
  transform: translateY(0);
}

/* 🧲 隐形磁吸触发带：贴在天花板上，用户鼠标往上一顶就会碰到 */
.header-hover-trigger-zone {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 30px; /* 🔥 1. 加大热区，从 14px 提升到 30px，闭着眼都能顶到 */
  z-index: 999999;
  background: transparent !important; /* 🔥 2. 极其关键：强制透明背景，防止浏览器事件穿透忽略 */
  pointer-events: auto; /* 🔥 3. 强制捕获鼠标动作 */
}

/* 顶栏本体的丝滑过渡 */
.layout-header {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.4s ease !important;
  transform: translateY(0);
}

/* 隐藏状态下的终极形态 */
.layout-header.is-auto-hidden {
  /* 往上平移自身高度的 100% 刚好藏进天花板 */
  transform: translateY(-100%);
  opacity: 0;
  /* 🔥 极其关键的一行：隐藏后彻底释放下层 DOM 的鼠标点击事件！否则页面顶部会有一条透明的死区点不到 */
  pointer-events: none;
  margin-bottom: -70px !important;
}
</style>