<script setup>
/*
# ============================================================
#  沉浸式图书阅读与交流书屋 — BookReader.vue
#  模块：沉浸式阅读器（核心引擎）
#  职责：章节加载/翻页/分页计算 / 阅读设置 / 笔记批注 /
#        段评面板 / 分享卡片 / 词典查询 / 章节编辑 /
#        TTS朗读 / 知识图谱(X6) / 漂流瓶系统
#  状态管理：readerStore（朗读+缓存+漂流瓶）+ 本地 ref
#  实时通信：段落评论接口、阅读进度/时长同步
#  性能关键路径：paginateChapterContent() 分页计算引擎
#               TTS 粒子特效生成器
#  清理合约：onBeforeUnmount + onBeforeRouteLeave 双向拦截
#            readerStore.cleanupAllResources() 兜底清理
# ============================================================
*/

// ====================== 1. 模块导入 ======================
import { ref, onMounted, inject, nextTick, computed, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { storeToRefs } from 'pinia' // 🔥 新增：导入 storeToRefs
import { ElCard, ElButton, ElMessage, ElRow, ElCol, ElDivider, ElInput,ElDropdown, ElDropdownItem, ElProgress  } from 'element-plus'
import request from '../utils/request.js'
import html2canvas from 'html2canvas'
import WeatherAtmosphere from "../components/WeatherAtmosphere.vue";
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import { useAppSettingsStore} from "../stores/appSettingsStore.js";
// 1. 新增导入 readerStore
import { useReaderStore } from '../stores/readerStore'
import { useAchievementStore } from '../stores/achievementStore'

defineOptions({ name: 'BookReader' })

const settingsStore = useAppSettingsStore()
const readerStore = useReaderStore()
// 🔥 核心修复：使用 storeToRefs 一次性提取并保持响应式！
const {
  chapterCache, cacheClearTimer, cachedIsbn, cachedTotalChapter,
  fullNoteText, fullReadingText, isReading, isPaused, showReadingDialog, readingRate,
  audioPlayer, isAudioLoading, currentAudioUrl, ttsAbortController,
  audioCache, isAiMode, textChunksForAi, currentChunkIndexForAi,
  currentSubtitle, currentReadIndex, readStartIndex,
  bubbleTimer, floatingBubbles, hasNewBottle, readTimer, bottleQueue
} = storeToRefs(readerStore)
// ====================== 2. 全局实例 & 依赖注入 ======================
// 路由和导航实例
const route = useRoute()
const router = useRouter()

// 🔥 新增：判断当前是否还在阅读页面（用于控制挂载到 body 的全局组件）
const isCurrentPage = computed(() => route.name === 'BookReader')
// Pinia 状态
const userStore = useUserStore()
const petStore = usePetStore()
const achievementStore = useAchievementStore()
// 从BasicLayout注入暗黑模式状态
const isDark = inject('isDark')
// 注入全局右键菜单
const globalContextMenu = inject('globalContextMenu', null)
// 注入全局刷新笔记的方法
const fetchUserNotes = inject('fetchUserNotes')
// 注入全局分享工具
const generateShareContent = inject('generateShareContent')
const shareToForum = inject('shareToForum')
// 注入笔记类型列表
const noteTypeList = inject('noteTypeList', [])

const currentWeather = inject('currentWeather')           // 注入当前天气
const handleWeatherChange = inject('handleWeatherChange') // 注入切换天气的方法
const isGeneratingAtmosphere = ref(false)                 // AI 氛围感应的 loading 状态
// ====================== 3. 基础设备判断 ======================
// 判断是否为手机端
const isMobile = inject('isMobile')
// 监听窗口大小变化
// ====================== 4. 用户信息变量 ======================
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')

// ====================== 5. 阅读核心变量 ======================
const showToolbar = ref(true)
const contentContainerRef = ref(null)

const bookReaderRef = ref(null) // 阅读区域DOM引用
const currentIsbn = ref('') // 当前书籍ISBN
const currentNumber = ref(1) // 当前章节号（默认1）
const currentChapter = ref(null) // 当前章节数据
const totalChapter = ref(0) // 总章节数
const bookName = ref('未知书籍') // 书籍名称
const loading = ref(false) // 加载状态


const currentPage = ref(1) // 当前页码
const totalPages = ref(1) // 总页数
const paginatedContent = ref([]) // 分页后的内容数组
const pageHeight = ref(0) // 每页可显示高度
const isTwoColumnMode = ref(false)
// 👇 新增：暂存跨章节跳转时的目标书签进度
const targetProgressAfterLoad = ref(null)
// 👇 新增：记录当前翻页动作的方向，用于触发不同的 3D 动画
const flipDirection = ref('forward') // 'forward' 向下翻页 | 'backward' 向上翻页
// 👇 新增：标记是否是从下一章退回来的，用于定位到上一章的最后一页
const isNavigatingBack = ref(false)

// 1. 新增：进度条相关变量
const scrollContainerRef = ref(null)
const progressPercentage = ref(0) // 阅读进度 0-100

// 新增：控制进度条显示/隐藏的变量
const showProgressBar = ref(true)

// 3. 滚动监听函数（直接计算，无隐藏逻辑）
const handleScroll = () => {
  // ✅ 修改：让第1页严格等于0%，最后一页等于100%
  if (totalPages.value <= 1) {
    progressPercentage.value = 100
  } else {
    progressPercentage.value = Math.round(((currentPage.value - 1) / (totalPages.value - 1)) * 100)
  }
}

// 👇 新增：将后端的 paragraphs 数组拼合成纯文本，供 AI 和 TTS 朗读使用
const currentChapterRawText = computed(() => {
  if (!currentChapter.value || !currentChapter.value.paragraphs) return ''
  // 拼接所有的段落文本
  return currentChapter.value.paragraphs.map(p => p.content).join('\n')
})

// 👇 新增：点击气泡打开段评的方法（预留给你自己写抽屉业务）
const openParagraphComment = (para) => {
  console.log('准备拉取段评, 段落ID:', para.id)
  ElMessage.success(`拉取段评接口：段落 ${para.number}`)
  // TODO: 这里写你打开右侧段评抽屉、请求 /api/comment/paragraph 的代码
}
// ====================== 6. 章节编辑/新增变量 ======================
// 编辑相关
const isEditing = ref(false) // 是否处于编辑状态
const editingChapterName = ref('') // 编辑中的章节名称
const editingChapterContent = ref('') // 编辑中的章节内容
const chapterBackup = ref(null) // 编辑前的章节备份
// 新增章节相关
const isAdding = ref(false) // 是否处于新增章节状态
const addingChapterName = ref('') // 新增章节的名称
const addingChapterContent = ref('') // 新增章节的内容
const newChapterNumber = ref(0) // 新增章节的编号

// ====================== 7. 阅读设置变量（字体/背景/护眼） ======================
// 字体大小
const fontSizeLevel = ref('small') // 默认字体等级
const fontSizeMap = { // 字体大小映射表
  small: '14px',
  medium: '18px',
  large: '22px'
}
// 🔥 修改：添加过渡属性到字体样式
const fontSizeStyle = computed(() => ({
  fontSize: fontSizeMap[fontSizeLevel.value],
}))
const fontSizeValue = ref(fontSizeMap[fontSizeLevel.value]) // 当前字体大小值
// 护眼模式/阅读背景
const bgColor = ref('beige') // 默认背景色
const bgPresets = [
  { value: 'white', label: '纯净白', bg: '#ffffff', text: '#333333', darkBg: '#1a1a1a', darkText: '#e5e7eb' },
  { value: 'beige', label: '牛皮纸', bg: '#f5f0e6', text: '#4a4a4a', darkBg: '#2a241b', darkText: '#d1c5b4' },
  { value: 'green', label: '护眼绿', bg: '#e6f0e6', text: '#3d5c3d', darkBg: '#1b2a1d', darkText: '#b4d1b8' },
  { value: 'pink',  label: '樱花粉', bg: '#faeaea', text: '#5c3d4a', darkBg: '#2a1a20', darkText: '#d1b4c1' },
  { value: 'gray',  label: '极客灰', bg: '#f0f3f6', text: '#334050', darkBg: '#1a1d24', darkText: '#b4c1d1' },
  { value: 'sepia', label: '复古黄', bg: '#eaddc5', text: '#5c4a3d', darkBg: '#2a221a', darkText: '#d1c1b4' },
  { value: 'blue',  label: '海盐蓝', bg: '#e8eff5', text: '#3d4a5c', darkBg: '#1a202a', darkText: '#b4c5d1' }
]
// ====================== 8. 笔记相关变量 ======================
const chapterNotes = ref([]) // 章节笔记列表
const currentNoteId = ref(null) // 当前操作的笔记ID
const selectedNoteType = ref(null) // 选中的笔记类型
const readerCommentInput = ref('') // 笔记批注输入
const showAddNoteDialog = ref(false) // 添加笔记弹窗

// 朗读弹窗拖拽控制 (保持你原有代码不变)
const isMinimized = ref(false)
let isDragging = false
let hasMoved = false
let startX = 0, startY = 0, startRight = 0, startBottom = 0
const rightPos = ref(30)
const bottomPos = ref(30)

const readLength = ref(0)
const readingTextRef = ref(null)
const isFirstLoad = ref(true)

// ====================== 📚 新增：段落评论面板变量与逻辑 ======================
const showCommentPanel = ref(false)         // 控制毛玻璃面板显隐
const currentParagraphId = ref(null)        // 当前点击的段落ID
const paragraphComments = ref([])           // 评论列表数据
const commentPage = ref(1)                  // 分页：当前页
const commentPageSize = ref(10)             // 分页：每页数量
const hasMoreComments = ref(true)           // 是否还有更多数据
const loadingComments = ref(false)          // 加载状态

// 发表评论弹窗变量
const showAddCommentDialog = ref(false)
const newCommentContent = ref('')

// 1. 打开段评面板（你需要将这个方法绑定到原本的段评气泡 @click 事件上）
const openCommentPanel = async (paragraphId) => {
  if (!paragraphId) return
  currentParagraphId.value = paragraphId
  paragraphComments.value = []
  commentPage.value = 1
  showCommentPanel.value = true
  await fetchComments()
}

// 2. 拉取段落评论数据 (GET)
const fetchComments = async () => {
  if (loadingComments.value || !currentParagraphId.value) return
  loadingComments.value = true
  try {
    const res = await request.get('/book/paragraph/comment', {
      params: {
        paragraphId: currentParagraphId.value,
        pageNum: commentPage.value,
        pageSize: commentPageSize.value
      }
    })

    if (res.code === 200) {
      // 🐛 修复：res.data 就是后端的 PageResultDTO，数组字段通常是 data 或 list
      const pageData = res.data || {}
      const list = pageData.data || pageData.list || []

      paragraphComments.value.push(...list)

      // 判断是否还有下一页
      const total = pageData.total || 0
      hasMoreComments.value = paragraphComments.value.length < total
    } else {
      petStore.addMessage(res.msg || '获取评论失败')
    }
  } catch (err) {
    console.error('拉取段评失败:', err)
  } finally {
    loadingComments.value = false
  }
}

// 3. 点击加载更多
const loadMoreComments = () => {
  if (hasMoreComments.value) {
    commentPage.value++
    fetchComments()
  }
}

// 4. 发表新评论 (POST)
const submitComment = async () => {
  if (!newCommentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空哦~')
    return
  }

  try {
    const res = await request.post('/book/paragraph/comment', {
      paragraphId: currentParagraphId.value,
      userId: currentUserId.value,
      content: newCommentContent.value
    })

    // 🐛 修复：直接判断 res.code，不需要 res.data.code
    if (res.code === 200) {
      ElMessage.success('发表成功！')
      achievementStore.checkAchievements()
      showAddCommentDialog.value = false
      newCommentContent.value = ''

      // 发表成功后，清空列表并重新拉取第一页
      paragraphComments.value = []
      commentPage.value = 1
      await fetchComments()
    } else {
      petStore.addMessage(res.msg || '发表失败')
    }
  } catch (err) {
    console.error('发表评论异常:', err)
    petStore.addMessage('服务器异常，请稍后重试')
  }
}

// ====================== 10. 分享功能变量 ======================
const shareCardRef = ref(null)
const shareText = ref('')
const shareBookName = ref('')
const shareBookCover = ref('')
const isSharePreviewShow = ref(false)
const shareFontSize = ref(14)
// 🔥 新增：字体颜色变量，默认根据暗黑模式给个初始值
const shareTextColor = ref('#333333')
// 🆕 新增控制变量
const showBookCover = ref(true)       // 是否显示书籍封面
const bgType = ref('gradient')         // 背景类型：'gradient' (渐变) 或 'blur' (封面模糊)
const blurIntensity = ref(20)          // 高斯模糊强度 (px)
// 自定义双颜色自由组合（绑定颜色选择器）
const color1 = ref('#f5f7fa')
const color2 = ref('#e4efe9')
// 渐变背景
// 🔥 核心修复：转为计算属性，且统一使用 background-image 格式，解除暗黑模式固定限制
const shareBgGradient = computed(() => {
  return `linear-gradient(135deg, ${color1.value} 0%, ${color2.value} 100%)`
})
// 渐变预设
// 渐变预设（改用 c1 和 c2 显式存储，方便颜色选择器同步）
const gradientPresets = [
  { label: '清新薄荷', c1: '#e6f7ef', c2: '#c8f0e1' },
  { label: '暖杏奶白', c1: '#fff9f0', c2: '#ffe8d6' },
  { label: '天空浅蓝', c1: '#e0f7ff', c2: '#b3e5fc' },
  { label: '樱花淡粉', c1: '#fff0f6', c2: '#ffd6e0' },
  { label: '薰衣草紫', c1: '#f5f0ff', c2: '#e8d5ff' },
  { label: '海盐冰蓝', c1: '#e8f4ff', c2: '#cce4ff' },
  { label: '蜜桃渐变', c1: '#fff5f0', c2: '#ffd8cc' },
]
// 选择预设
const handleSelectPreset = (preset) => {
  color1.value = preset.c1
  color2.value = preset.c2
}
// 🔥 动态卡片基础样式计算（处理暗黑模式下的半透明蒙层叠加，保证文字可读性）
const cardStyle = computed(() => {
  const base = {
    boxShadow: isDark.value ? '0 8px 32px rgba(0, 0, 0, 0.3), inset 0 0 0 1px rgba(255, 255, 255, 0.08)' : '0 8px 32px rgba(31, 38, 135, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.18)',
    borderColor: isDark.value ? 'rgba(255, 255, 255, 0.08)' : 'rgba(255, 255, 255, 0.18)'
  }

  if (bgType.value === 'gradient') {
    // 🔥 html2canvas 核心修复：必须赋给 backgroundImage 属性
    return { ...base, backgroundImage: shareBgGradient.value }
  } else {
    // 模糊模式下，大背景给个基础底色即可
    return { ...base, backgroundColor: isDark.value ? '#1a202c' : '#ffffff' }
  }
})
// ====================== 11. 词典查询变量 ======================
const isDictShow = ref(false) // 查询弹窗显示状态
const dictWord = ref('') // 查询的词语
const dictPinyin = ref('') // 拼音
const dictMeanings = ref([]) // 释义列表
const dictPosition = ref({ x: 0, y: 0 }) // 弹窗位置
const localDict = ref({}) // 本地词典

// ====================== 12. 右键菜单/选中文本变量 ======================
// ✅ 修复：注入通用右键状态（Basic新增）
const currentRightClickItem = inject('currentRightClickItem')
const contextMenuType = inject('contextMenuType')
// 👇 新增：注入右键菜单依赖（和个人界面完全一致）
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const closeContextMenu = inject('closeContextMenu')
const clearContextMenu = inject('clearContextMenu')
const CARD_TYPES = inject('CARD_TYPES')

const selectedText = ref('') // 选中的文本
const menuPosition = inject('contextMenuPosition')

// 👇 新增：阅读页右键专用变量
const currentRightClickNote = inject('currentRightClickNote')
const rightClickSelectedText = inject('rightClickSelectedText')// 右键选中的文本
const rightClickNoteId = inject('rightClickNoteId')    // 右键命中的笔记ID

// ====================== 13. 阅读时长/历史变量 ======================
const readStartTime = ref(Date.now()) // 阅读开始时间
const currentProgressId = ref(null) // 阅读进度ID
const isChapterListShow = ref(false) // 章节列表弹窗
const allChapters = ref([]) // 全部章节
const chapterPage = ref(1) // 章节列表分页
const chapterPageSize = ref(10)

// ====================== 14. 计算属性 ======================
const pageBg = computed(() => {
  const theme = bgPresets.find(item => item.value === bgColor.value) || bgPresets[0]
  return isDark.value ? theme.darkBg : theme.bg
})

// 👇 替换：文字颜色（动态去配置表中找对应颜色，确保对比度阅读舒适）
const textColor = computed(() => {
  const theme = bgPresets.find(item => item.value === bgColor.value) || bgPresets[0]
  return isDark.value ? theme.darkText : theme.text
})
// 章节列表分页
const currentPageChapters = computed(() => {
  const start = (chapterPage.value - 1) * chapterPageSize.value
  const end = start + chapterPageSize.value
  return allChapters.value.slice(start, end)
})
// 朗读高亮文本
const highlightedReadingText = computed(() => {
  const text = fullReadingText.value
  const currentIndex = currentReadIndex.value
  if (!text || currentIndex < 0) return text
  const highlightEnd = findNextPunctuation(text, currentIndex)
  return text.slice(0, currentIndex)
      + `<span class="reading-highlight">${text.slice(currentIndex, highlightEnd)}</span>`
      + text.slice(highlightEnd)
})

// ====================== 15. 工具/格式化函数 ======================
// 格式化时间
const formatTime = inject('formatDateTime')

/**
 * 防抖：停止操作后延迟 delay 毫秒执行回调
 * @param {Function} fn 回调函数
 * @param {number} delay 延迟毫秒
 * @returns {Function}
 */
function debounce(fn, delay = 200) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}


// ====================== 替换：真实分页计算引擎 ======================
const paginateChapterContent = async () => {
  // ✅ 核心改动：基于 paragraphs 数组而不是 content 字符串
  if (!currentChapter.value?.paragraphs || !contentContainerRef.value) return
  await nextTick()

  const container = contentContainerRef.value
  const style = window.getComputedStyle(container)
  const paddingX = parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
  const paddingY = parseFloat(style.paddingTop) + parseFloat(style.paddingBottom)
  const exactWidth = container.clientWidth - paddingX
  pageHeight.value = container.clientHeight - paddingY

  if (pageHeight.value <= 100) pageHeight.value = 500
  isTwoColumnMode.value = exactWidth > 800
  const columnGap = 60

  const singlePageWidth = isTwoColumnMode.value ? (exactWidth - columnGap) / 2 : exactWidth
  const singlePageHeight = pageHeight.value - 5

  const tempDiv = document.createElement('div')
  // 补上 text-indent: 2em，保证测量和实际渲染首行缩进高度完全一致
  tempDiv.style.cssText = `
    position: absolute; visibility: hidden; top: -9999px; left: 0;
    width: ${singlePageWidth}px;
    font-size: ${fontSizeMap[fontSizeLevel.value]};
    line-height: 1.8; white-space: pre-wrap; text-align: justify; word-break: break-word;
    text-indent: 2em;
  `
  document.body.appendChild(tempDiv)

  const pages = []
  let currentPageParagraphs = [] // 👉 存放段落对象
  let currentHeight = 0

  for (const para of currentChapter.value.paragraphs) {
    const processedHtml = processContentWithUnderline(para.content)

    // 模拟真实渲染结构计算高度（包含可能存在的气泡）
    let bubbleHtml = para.commentCount > 0
        ? `<span style="display:inline-block; margin-left:8px; font-size:12px;">💬 ${para.commentCount}</span>`
        : ''

    tempDiv.innerHTML = processedHtml + bubbleHtml
    const paraHeight = tempDiv.offsetHeight

    if (currentHeight + paraHeight <= singlePageHeight) {
      currentPageParagraphs.push({ ...para, displayHtml: processedHtml })
      currentHeight += paraHeight
    } else {
      if (currentPageParagraphs.length > 0) pages.push(currentPageParagraphs)
      currentPageParagraphs = [{ ...para, displayHtml: processedHtml }]
      currentHeight = paraHeight
    }
  }
  if (currentPageParagraphs.length > 0) pages.push(currentPageParagraphs)
  document.body.removeChild(tempDiv)

  // 👉 paginatedContent 变成了二维数组：Array<Array<Paragraph>>
  paginatedContent.value = pages
  totalPages.value = pages.length || 1

  // (后面的翻页跳页逻辑保持你原样即可...)
  if (isNavigatingBack.value) {
    let targetPage = totalPages.value
    if (isTwoColumnMode.value && targetPage % 2 === 0) targetPage -= 1
    currentPage.value = targetPage
    isNavigatingBack.value = false
  } else if (targetProgressAfterLoad.value !== null) {
    let targetPage = 1
    if (totalPages.value > 1) {
      targetPage = Math.round((targetProgressAfterLoad.value / 100) * (totalPages.value - 1)) + 1
    }
    if (isTwoColumnMode.value && targetPage % 2 === 0) targetPage -= 1
    currentPage.value = targetPage
    targetProgressAfterLoad.value = null
  } else {
    currentPage.value = 1
  }
  handleScroll()
}

// 🔥 新增：窗口大小变化时的统一处理函数
const handleWindowResize = () => {
  paginateChapterContent() // 重新计算章节分页
  // 🛑 第二道防线：利用 nextTick 确保 DOM 宽高已经更新完毕再算边界
  nextTick(() => {
    applyBottleBoundary(bottleRight.value, bottleBottom.value)
  })
}

// 朗读：查找标点符号
const findNextPunctuation = (text, startIndex) => {
  const punctuationSet = new Set(['。', '！', '？', , '……'])
  const maxHighlightLength = 50
  for (let i = startIndex; i < Math.min(startIndex + maxHighlightLength, text.length); i++) {
    if (punctuationSet.has(text[i])) {
      return i + 1
    }
  }
  return Math.min(startIndex + 10, text.length)
}
// 重置分享配置
const resetShareConfig = () => {
  bgType.value = 'gradient'
  color1.value = '#f5f7fa'
  color2.value = '#e4efe9'
  shareFontSize.value = 14
  showBookCover.value = true
  blurIntensity.value = 20
  shareTextColor.value = isDark.value ? '#e5e7eb' : '#333333' // 🔥 新增
}

// ====================== 15. 书签功能变量 ======================
const getChapterNumber = (chapterId) => {
  if (!chapterId) return ''
  const parts = chapterId.split('-')
  return parts.length > 1 ? parts[1] : chapterId
}

const isBookmarkListShow = ref(false) // 书签列表弹窗显示
const bookmarkList = ref([]) // 我的书签列表
// ====================== 🔥 新增：添加书签方法 ======================
const addBookmark = async () => {
  // 1. 校验必填参数
  if (!currentUserId.value) {
    ElMessage.warning('请先登录！')
    return
  }
  if (!currentIsbn.value || !currentNumber.value) {
    petStore.addMessage('书籍信息异常，无法添加书签')
    return
  }

  try {
    // 2. 构造书签参数（完全匹配后端实体类）
    const bookmarkData = {
      userId: currentUserId.value,          // 用户ID
      isbn: currentIsbn.value,              // 书籍ISBN
      chapterNumber:`${currentIsbn.value}-${currentNumber.value}` , // 章节号（转字符串）
      readProgress: progressPercentage.value // 当前阅读进度
      // createTime 后端Service会自动填充，无需传
    }

    // 3. 调用后端新增书签接口（Servlet地址：/api/bookmark）
    const res = await request.post('/bookmark', bookmarkData)

    // 4. 结果提示
    if (res.code === 200) {
      ElMessage.success('书签添加成功！📌')
      graphRef.value?.createAutoGraphNode(graphRef.value?.GRAPH_NODE_TYPE.BOOKMARK, `书签: 第${currentNumber.value}章`, `进度 ${progressPercentage.value}%`)
    } else {
      petStore.addMessage(res.msg || '书签添加失败')
    }
  } catch (err) {
    console.error('添加书签失败：', err)
    petStore.addMessage('添加书签出错，请重试')
  }
}
// 查询当前书籍的所有书签
const fetchMyBookmarks = async () => {
  if (!currentUserId.value || !currentIsbn.value) {
    ElMessage.warning('请先登录或检查书籍信息')
    return []
  }
  try {
    // 调用后端接口：查询当前用户+当前书籍的所有书签
    const res = await request.get('/bookmark', {
      params: {
        userId: currentUserId.value,
        isbn: currentIsbn.value
      }
    })
    bookmarkList.value = res.data || []
    return bookmarkList.value
  } catch (err) {
    console.error('加载我的书签失败', err)
    petStore.addMessage('加载书签失败')
    return []
  }
}

// 打开我的书签列表弹窗
const openMyBookmarks = async () => {
  await fetchMyBookmarks()
  isBookmarkListShow.value = true
}
// 🔥 修复：查看书签（等待异步加载完成，再关闭右键菜单）
const handleOpenBookmarks = async () => {
  try {
    // 1. 先加载书签 + 打开弹窗
    await openMyBookmarks()
    console.log("✅ 书签弹窗已打开")
  } catch (err) {
    console.error("❌ 打开书签失败：", err)
    petStore.addMessage("加载书签失败，请重试")
  } finally {
    // 2. 弹窗打开后，再关闭右键菜单
    closeContextMenu()
  }
}
// ====================== 🔥 统一修复：右键异步弹窗处理 ======================
// 1. 处理：查询词语（异步加载完成 → 关菜单）
const handleQueryWord = async () => {
  try {
    await queryWord()
  } catch (err) {
    console.error('❌ 查询词语失败：', err)
    petStore.addMessage('查询失败，请重试')
  } finally {
    // 无论成功/失败，最终关闭右键菜单
    closeContextMenu()
  }
}

// 2. 处理：打开分享图片预览（异步加载完成 → 关菜单）
const handleOpenSharePreview = async () => {
  try {
    await openShareImagePreview()
  } catch (err) {
    console.error('❌ 生成分享图片失败：', err)
    petStore.addMessage('生成分享图片失败')
  } finally {
    closeContextMenu()
  }
}
// 处理：普通开始朗读
const handleStartReading = () => {
  // 🔥 修复：从选中位置读到章节末尾，而不是只读选中的文本
  if (currentChapterRawText.value && rightClickSelectedText.value) {
    const selectedIndex = currentChapterRawText.value.indexOf(rightClickSelectedText.value)
    if (selectedIndex !== -1) {
      fullNoteText.value = currentChapterRawText.value.slice(selectedIndex)
    } else {
      fullNoteText.value = rightClickSelectedText.value
    }
  } else {
    fullNoteText.value = rightClickSelectedText.value
  }
  closeContextMenu()
  startReadingProcess(false)
}

// 📖 全章朗读
const handleFullChapterRead = () => {
  fullNoteText.value = currentChapterRawText.value
  closeContextMenu()
  startReadingProcess(false)
}

// 处理：深度情感朗读（优先有声书，无则AI生成+回存）
const handleAiEmotionRead = () => {
  fullNoteText.value = rightClickSelectedText.value
  closeContextMenu()
  // 🔥 优先检查有声书是否已有内容
  tryLoadAudiobookThenFallback()
}

// 🔥 尝试加载有声书，无则走AI深度朗读
const tryLoadAudiobookThenFallback = async () => {
  if (!currentIsbn.value || !currentNumber.value) {
    startReadingProcess(true)
    return
  }
  try {
    // 检查有声书生进度
    const progressRes = await request.get('/api/audiobook/progress', {
      params: { isbn: currentIsbn.value, chapter: currentNumber.value }
    })
    if (progressRes.code === 200 && progressRes.data?.total > 0 && progressRes.data?.generated > 0) {
      // 有声书已生成，直接用
      showAudiobookPlayer.value = true
      return
    }
  } catch { /* 接口不通就走AI生成 */ }
  // 无声书：走AI深度朗读（原有逻辑）
  startReadingProcess(true)
}

// 处理：复制文本
const handleCopySelectedText = async () => {
  try {
    if (!selectedText.value) {
      ElMessage.warning('暂无文本可复制')
      return
    }
    await navigator.clipboard.writeText(selectedText.value)
    ElMessage.success('复制成功！')
  } catch (err) {
    console.error('❌ 复制文本失败：', err)
    petStore.addMessage('复制失败，请手动复制')
  } finally {
    closeContextMenu()
  }
}

// 处理：删除笔记
const handleDeleteNote = async () => {
  try {
    await deleteNote()
  } catch (err) {
    console.error('❌ 删除笔记失败：', err)
    petStore.addMessage('删除笔记失败')
  } finally {
    closeContextMenu()
  }
}

// 在变量定义区域添加
// ====================== 新增：AI章节自动出题功能 ======================
// 自动出题间隔选项（毫秒）
const QUIZ_INTERVAL_OPTIONS = [
  { label: '5分钟', value: 5 * 60 * 1000 },
  { label: '10分钟', value: 10 * 60 * 1000 },
  { label: '15分钟', value: 15 * 60 * 1000 },
  { label: '30分钟', value: 30 * 60 * 1000 }
]
// 当前选中的出题间隔
const selectedQuizInterval = ref(2 * 60 * 1000)
// 自动出题下拉框显示状态
const showQuizIntervalDropdown = ref(false)

// ====================== 🔥 新增：AI生成题目相关方法 ======================
/**
 * 基于选中的文本生成题目
 */
const generateQuizFromSelection = () => {
  if (!rightClickSelectedText.value || rightClickSelectedText.value.trim().length < 20) {
    ElMessage.warning('选中内容太短，无法生成题目')
    closeContextMenu()
    return
  }

  petStore.showQuizFromContent(rightClickSelectedText.value)
  closeContextMenu()
}

/**
 * 基于当前整个章节生成题目
 */
// ====================== 1. AI 出题方法修复 ======================
const generateQuizFromChapter = () => {
  // ✅ 修正：改用 currentChapterRawText.value
  if (!currentChapterRawText.value || currentChapterRawText.value.trim().length < 50) {
    ElMessage.warning('章节内容太短，无法生成题目')
    closeContextMenu()
    return
  }
  petStore.showQuizFromContent(currentChapterRawText.value)
  closeContextMenu()
}

/**
 * 切换基于当前章节的AI自动出题（和工具栏按钮功能完全一致）
 */
const toggleChapterAutoQuiz = () => {
  if (petStore.isAutoQuizRunning && petStore.autoQuizMode === 'ai') {
    petStore.stopAutoQuiz()
  } else {
    // ✅ 修正：改用 currentChapterRawText.value
    if (!currentChapterRawText.value || currentChapterRawText.value.trim().length < 50) {
      ElMessage.warning('章节内容太短，无法开启自动出题')
      closeContextMenu()
      return
    }
    petStore.startAutoQuiz(2 * 60 * 1000, 'ai', currentChapterRawText.value)
  }
  closeContextMenu()
}

const sendAiMsg = inject('sendAiMsg')
// ====================== 🔥 新增：AI高级功能弹窗变量 ======================
const showAiDialog = ref(false) // AI功能弹窗显示
const aiDialogTitle = ref('') // 弹窗标题
const aiTargetContent = ref('') // 要处理的文本（章节/选中内容）
const aiUserPrompt = ref('') // 用户自定义需求

// ====================== 🔥 新增：三大AI功能入口方法 ======================
const generateAiAtmosphere = async (event) => {
  // ✅ 修正：改用 currentChapterRawText.value
  if (!currentChapterRawText.value) {
    ElMessage.warning('章节内容太少，无法感应氛围~')
    return
  }

  isGeneratingAtmosphere.value = true
  petStore.addMessage('🔮 AI 正在通读章节，感应环境氛围...')

  try {
    // 1. 截取前 1000 个字给 AI 分析，防止 Token 超出
    const targetText = currentChapterRawText.value.substring(0, 1000)

    // 2. 构造强大的 Prompt，逼迫 AI 只能输出一个天气单词
    const promptText = `你是一个情感共鸣分析师。请阅读以下小说片段，分析当前场景的氛围。
    请你严格根据情感基调，从以下5个英文单词中选择唯一1个单词输出（不要输出任何多余的标点、解释或多余的字！）：
    - sunny (阳光明媚、温馨、轻松、愉悦、日常)
    - cloudy (平淡、迷茫、压抑但未爆发、阴天)
    - rain (悲伤、忧郁、流泪、离别、失落)
    - thunder (战斗、愤怒、冲突、震惊、雷暴)
    - night (安静、夜晚、神秘、沉思、睡眠)

    小说内容：${targetText}`

    // 3. 复用你已有的聊天接口发给豆包 AI
    const res = await request.post('/pet/ai/chat', { content: promptText })

    if (res.code === 200) {
      let aiResult = res.data.toLowerCase().trim()

      // 4. 正则提取 AI 结果（防止 AI 废话，比如回复"我觉得是 sunny"）
      const weatherMatch = aiResult.match(/(sunny|cloudy|rain|thunder|night)/)

      if (weatherMatch) {
        const targetWeather = weatherMatch[1]

        // 🌟 核心：调用 App.vue 提供的方法切换全局天气！
        handleWeatherChange(targetWeather, event)

        // 🌟 奖励：联动改变阅读纸张的颜色，沉浸感拉满！
        const vibeMap = {
          'sunny': 'beige',   // 晴天配暖黄色纸张
          'cloudy': 'gray',   // 阴天配极客灰纸张
          'rain': 'blue',     // 雨天配海盐蓝纸张
          'thunder': 'gray',  // 雷暴配深色
          'night': 'white'    // 黑夜模式由全局控制，纸张选白纸反转最好看
        }
        if (vibeMap[targetWeather]) {
          bgColor.value = vibeMap[targetWeather]
        }

        petStore.addMessage(`✨ 感应成功！已为您切换至【${targetWeather}】沉浸氛围`)
      } else {
        petStore.addMessage('AI 好像迷失在剧情里了，请稍后再试~')
      }
    }
  } catch (err) {
    console.error('氛围感应失败', err)
    petStore.addMessage('🔮 氛围魔法阵失效了，请重试')
  } finally {
    isGeneratingAtmosphere.value = false
  }
}

// 1. AI章节智能总结
// ====================== 3. AI 总结与人物分析修复 ======================
const openAiChapterSummary = () => {
  aiDialogTitle.value = 'AI 章节智能总结'
  // ✅ 修正：提取原始长文本
  aiTargetContent.value = currentChapterRawText.value || '暂无章节内容'
  aiUserPrompt.value = '请精简剧情、提炼核心要点，使用短句速读风格'
  showAiDialog.value = true
  closeContextMenu()
}
const openAiCharacterAnalyze = () => {
  aiDialogTitle.value = 'AI 人物分析'
  // ✅ 修正：提取原始长文本
  aiTargetContent.value = currentChapterRawText.value || '暂无章节内容'
  aiUserPrompt.value = '请分析书中角色的性格特点、人物关系与生平'
  showAiDialog.value = true
  closeContextMenu()
}

// 3. AI文字仿写&润色
const openAiTextRewrite = () => {
  if (!rightClickSelectedText.value) {
    ElMessage.warning('请先选中文本')
    closeContextMenu()
    return
  }
  aiDialogTitle.value = 'AI 文字仿写&润色'
  aiTargetContent.value = rightClickSelectedText.value // 用选中的文本
  aiUserPrompt.value = '请仿写句式、美化文笔，保持原文风格'
  showAiDialog.value = true
  closeContextMenu()
}

// ====================== 🔥 修复：弹窗发送按钮逻辑 ======================
// 1. 把函数改成 async 异步函数
const handleAiDialogSend = async () => {
  if (!aiTargetContent.value.trim()) {
    ElMessage.warning('暂无内容可处理')
    return
  }

  // 拼接用户的提示和选中的文本
  const finalPrompt = `${aiUserPrompt.value}，内容如下：${aiTargetContent.value}`
  rightClickSelectedText.value = aiTargetContent.value

  try {
    let reply = ''
    let nodeType = graphRef.value?.GRAPH_NODE_TYPE.QA

    // 🔥 根据标题精准分配 actionType
    if (aiDialogTitle.value.includes('总结')) {
      reply = await sendAiMsg(finalPrompt, 'chapter_summary', { isbn: currentIsbn.value, chapterNumber: currentNumber.value })
      nodeType = graphRef.value?.GRAPH_NODE_TYPE.AI_SUMMARY
    }
    else if (aiDialogTitle.value.includes('人物')) {
      reply = await sendAiMsg(finalPrompt, 'character_analyze', { isbn: currentIsbn.value, chapterNumber: currentNumber.value })
      nodeType = graphRef.value?.GRAPH_NODE_TYPE.AI_ROLE
    }
    else if (aiDialogTitle.value.includes('润色')) {
      reply = await sendAiMsg(finalPrompt, 'text_rewrite', { isbn: currentIsbn.value, chapterNumber: currentNumber.value })
      nodeType = graphRef.value?.GRAPH_NODE_TYPE.QA
    }

    // 拿到回复后创建图谱节点
    if (reply) {
      graphRef.value?.createAutoGraphNode(nodeType, aiDialogTitle.value, reply)
    }
  } catch (error) {
    petStore.addMessage('AI 回复获取失败，请重试')
    console.error('获取AI回复失败：', error)
  }

  showAiDialog.value = false
  aiUserPrompt.value = ''
}

const captureScreen = inject('captureScreen')
const handleCaptureScreen = inject('handleCaptureScreen')
// 1. 弹窗控制变量
const showImageGenDialog = ref(false)
// 2. 文生图自定义提示词
const imageGenPrompt = ref('')
// 3. 生成的图片链接
const generatedImageUrl = ref('')
// 4. 加载状态（复用你聊天的 isGeneratingImage 也可以）
const isGeneratingImage = ref(false)

// 🔥 新增：AI 运行状态计算属性
const isAiActive = computed(() => {
  return isGeneratingAtmosphere.value ||
      isGeneratingImage.value ||
      petStore.isAutoQuizRunning
})

// 🔥 打开文生图弹窗（右键菜单点击触发）
const openImageGenDialog = () => {
  // 关闭右键菜单
  showContextMenu.value = false
  // 清空上一次的生成数据
  generatedImageUrl.value = ''
  imageGenPrompt.value = ''
  // 打开弹窗
  showImageGenDialog.value = true
  closeContextMenu()
}

// 🔥 关闭弹窗清空数据
const handleCloseImageDialog = (done) => {
  generatedImageUrl.value = ''
  imageGenPrompt.value = ''
  done()
}

/**
 * 剔除 [image] 和 [text] 标记，提取纯图片路径
 * @param {string} originStr 原始带标记的字符串
 * @returns {string} 纯图片路径
 */
function extractImagePath(originStr) {
  // 非字符串/空值直接返回空
  if (!originStr || typeof originStr !== 'string') return ''

  // 正则解析：
  // ^\[image\]  → 匹配字符串开头的 [image]
  // (.*?)       → 非贪婪捕获中间所有内容（核心：图片路径）
  // \[text\]$   → 匹配字符串结尾的 [text]
  const reg = /^\[image\](.*?)\[text\]$/
  const result = originStr.match(reg)

  // 匹配成功返回中间内容并去除首尾空格；匹配失败返回原字符串
  return result ? result[1].trim() : originStr
}
// 🔥 核心：阅读界面调用文生图（直接复用聊天接口逻辑）
const generateImageFromReading = async () => {
  // 校验：必须选中阅读文本
  if (!rightClickSelectedText.value) {
    ElMessage.warning('请先选中文本')
    return
  }

  isGeneratingImage.value = true
  try {
    // 拼接提示词：原文 + 自定义描述
    const finalPrompt = `${rightClickSelectedText.value} ${imageGenPrompt.value || ''}`

    // 🔥 直接复用你聊天的文生图接口！完全不改动后端
    const res = await request.post('/user/chat/message', null, {
      params: {
        action: 'generateImage',
        fromUserId: currentUserId.value,
        toUserId: 'doubao_ai', // 沿用当前会话ID
        prompt: finalPrompt
      }
    })

    if (res.code === 200) {
      ElMessage.success('图片生成成功！')
      const rawContent = res.data.messageContent
      const pureImgUrl = extractImagePath(rawContent)
      generatedImageUrl.value = pureImgUrl

      // ========== 新增：串行创建双节点 + 自动连线 ==========
      // 1. 定义第一个节点的基准坐标
      const baseX = 120 + Math.random() * 30
      const baseY = 120 + Math.random() * 30

      // 2. 创建【AI生图原始文字】节点，拿到ID
      const textNodeId = await graphRef.value?.createAutoGraphNode(
          graphRef.value?.GRAPH_NODE_TYPE.MANUAL,
          'AI生图原始文字内容',
          finalPrompt,
          baseX,    // 基准X
          baseY     // 基准Y
      )

      // 3. 创建【AI生图灵感】图片节点（X轴向右偏移220px，防止重叠）
      const imgNodeId = await graphRef.value?.createAutoGraphNode(
          graphRef.value?.GRAPH_NODE_TYPE.AI_IMAGE,
          'AI生图灵感',
          pureImgUrl,
          baseX + 220, // 水平偏移
          baseY
      )

      // 4. 两个节点ID都存在，自动创建连线
      if (textNodeId && imgNodeId) {
        const edgeSuccess = await graphRef.value?.createAutoEdge(textNodeId, imgNodeId)
        if (edgeSuccess) {
          ElMessage.success('已自动关联文字与图片 🧩')
        }
      }

      // 5. 最后统一刷新图谱（只刷新一次，渲染所有新内容）
      if (graphRef.value) {
        await graphRef.value?.loadGraphData()
        petStore.addMessage('🧠 图谱已自动更新节点与关联！')
      }
    } else {
      petStore.addMessage('生成失败：' + (res.msg || '未知错误'))
    }
  } catch (err) {
    console.error('阅读文生图失败', err)
    petStore.addMessage('图片生成失败，请重试')
  } finally {
    isGeneratingImage.value = false
  }
}

// 处理：打开添加笔记弹窗
const handleOpenAddNote = async () => {
  try {
    showAddNoteDialog.value = true
  } catch (err) {
    console.error('❌ 打开添加笔记弹窗失败：', err)
    petStore.addMessage('操作失败')
  } finally {
    closeContextMenu()
  }
}

// 处理：分享选段到论坛
const handleShareToForum = async () => {
  try {
    shareSelectedTextToForum()
  } catch (err) {
    console.error('❌ 分享到论坛失败：', err)
    petStore.addMessage('分享失败')
  } finally {
    closeContextMenu()
  }
}

// 处理：添加书签（统一异步封装）
const handleAddBookmark = async () => {
  try {
    await addBookmark()
  } catch (err) {
    console.error('❌ 添加书签失败：', err)
    petStore.addMessage('添加书签失败，请重试')
  } finally {
    closeContextMenu()
  }
}

// 🔥 完美适配分页版的书签跳转方法
const jumpToBookmark = async (bookmark) => {
  if (!bookmark) return

  const targetChapterNum = getChapterNumber(bookmark.chapterNumber)
  isBookmarkListShow.value = false // 立即关闭书签弹窗

  // 情况 1：如果是【当前章节】内跳转
  if (targetChapterNum === currentNumber.value) {
    if (totalPages.value <= 1) {
      currentPage.value = 1
    } else {
      let targetPage = Math.round((bookmark.readProgress / 100) * (totalPages.value - 1)) + 1
      if (isTwoColumnMode.value && targetPage % 2 === 0) targetPage -= 1 // ✨ 强制奇数对齐
      currentPage.value = targetPage
    }
    handleScroll()
    if (scrollContainerRef.value) scrollContainerRef.value.scrollTop = 0
    ElMessage.success('已跳转到书签位置')
    return
  }

  // 情况 2：如果是【跨章节】跳转
  // 先把进度记录到暂存变量中，等 loadChapter 内部的异步分页完成后自动读取
  targetProgressAfterLoad.value = bookmark.readProgress
  currentNumber.value = targetChapterNum

  await loadChapter()
  await updateReadProgress()

  if (scrollContainerRef.value) scrollContainerRef.value.scrollTop = 0
  ElMessage.success('已跳转到书签位置')
}

// 根据进度百分比滚动到对应位置
const scrollToBookmarkProgress = async (progress) => {
  const container = scrollContainerRef.value
  if (!container) return

  await nextTick()
  const scrollHeight = container.scrollHeight
  const clientHeight = container.clientHeight
  const scrollableHeight = scrollHeight - clientHeight

  if (scrollableHeight <= 0) return

  // 计算目标滚动距离
  const targetScrollTop = (progress / 100) * scrollableHeight
  // 平滑滚动
  container.scrollTo({
    top: targetScrollTop,
    behavior: 'smooth'
  })
}
// ====================== 16. 核心业务方法 - 书籍/章节加载 ======================
// 获取书籍名称
const getBookName = async () => {
  try {
    const res = await request.get('/book', { params: { isbn: currentIsbn.value } })
    const book = res.data[0] || {}
    bookName.value = book.bookname || '未知书籍'
  } catch (err) {
    console.error('获取书籍名称失败', err)
    bookName.value = '未知书籍'
  }
}
// 🔥 增强版：异步预加载前后章节（修复数据格式）
const preloadChapters = async (centerNum) => {
  if (!currentIsbn.value) return
  const toLoad = []

  if (centerNum > 1 && !chapterCache.value.has(centerNum - 1)) {
    toLoad.push(centerNum - 1)
  }
  if (totalChapter.value && centerNum < totalChapter.value && !chapterCache.value.has(centerNum + 1)) {
    toLoad.push(centerNum + 1)
  }

  for (const num of toLoad) {
    try {
      const chapterId = `${currentIsbn.value}-${num}`
      request.get('/book/chapter', { params: { chapter_id: chapterId }, timeout: 10000 })
          .then(res => {
            // ✅ 修复：现在后端返回的是单一对象 res.data，而不是数组！
            if (res.data && res.data.chapter_id) {
              chapterCache.value.set(num, res.data)
              console.log(`[缓存系统] 异步预加载成功: 第${num}章`)
            }
          })
    } catch (e) {
      console.warn(`[缓存系统] 预加载失败: 第${num}章`, e)
    }
  }
}

// 🔥 增强版：带缓存优先策略的章节加载 (修复 JSON 解析奔溃)
const loadChapter = async () => {
  if (loading.value) return
  loading.value = true

  try {
    if (!currentIsbn.value) {
      petStore.addMessage('缺少书籍ISBN参数！')
      return
    }

    const targetNum = currentNumber.value
    const chapterId = `${currentIsbn.value}-${targetNum}`

    // ✨ 第一步：判断命中缓存？
    if (chapterCache.value.has(targetNum)) {
      console.log(`⚡ [缓存系统] 极速命中: 第${targetNum}章`)
      currentChapter.value = JSON.parse(JSON.stringify(chapterCache.value.get(targetNum)))
      loading.value = false // 命中缓存直接取消骨架屏，实现无缝翻章！
    } else {
      // 没命中缓存，走正常的网络请求
      const chapterRes = await request.get('/book/chapter', {
        params: { chapter_id: chapterId }, timeout: 10000
      })

      // ✅ 修复核心：这里拿到的 data 直接是对象，不是数组！
      const chapterData = chapterRes.data

      if (!chapterData || !chapterData.chapter_id) {
        ElMessage.warning(`未找到第${targetNum}章内容`)
        currentChapter.value = null
        loading.value = false
        return
      }

      currentChapter.value = JSON.parse(JSON.stringify(chapterData))
      // 加入缓存以备下次使用
      chapterCache.value.set(targetNum, chapterData)
    }

    // 维护总章节数逻辑...
    const shouldRefreshCache = currentIsbn.value !== cachedIsbn.value || cachedTotalChapter.value === 0 || targetNum % 10 === 0
    if (shouldRefreshCache) {
      request.get('/book/chapter', { params: { isbn: currentIsbn.value }, timeout: 10000 }).then(res => {
        cachedTotalChapter.value = (res.data || []).length
        cachedIsbn.value = currentIsbn.value
        totalChapter.value = cachedTotalChapter.value
      })
    } else {
      totalChapter.value = cachedTotalChapter.value
    }

    await updateReadProgress()

    // 重新计算分页
    setTimeout(async () => {
      await paginateChapterContent()
      loading.value = false // 确保界面彻底渲染完后关闭加载
      handleScroll()
    }, 50)

    // ✨ 第二步：当前页面排版完成后，偷偷预加载下一章和上一章！
    setTimeout(() => {
      preloadChapters(targetNum)
    }, 800) // 延迟 0.8 秒执行，绝不抢占当前翻页的性能

  } catch (err) {
    console.error('❌ 加载章节内容失败：', err)
    petStore.addMessage('加载章节内容失败，请稍后再试')
    currentChapter.value = null
    loading.value = false
  }
}

// ====================== 17. 核心业务方法 - 章节操作（上一章/下一章/目录） ======================
// 上一页
const prevPage = () => {
  flipDirection.value = 'backward'
  const step = isTwoColumnMode.value ? 2 : 1 // ✨ 双栏一次翻 2 页

  if (currentPage.value > 1) {
    currentPage.value -= step
    if (currentPage.value < 1) currentPage.value = 1 // 越界保护
    handleScroll()
  } else {
    isNavigatingBack.value = true
    prevChapter()
  }
}

// 下一页
const nextPage = () => {
  flipDirection.value = 'forward'
  const step = isTwoColumnMode.value ? 2 : 1 // ✨ 双栏一次翻 2 页

  // ✨ 判断当前是否已经是本章尽头（双栏模式下要检查右页是否包含最后一页）
  const isAtEnd = isTwoColumnMode.value
      ? (currentPage.value + 1 >= totalPages.value)
      : (currentPage.value >= totalPages.value)

  if (!isAtEnd) {
    currentPage.value += step
    handleScroll()
  } else {
    nextChapter()
  }
}

// ✨ 修复增强版：阅读区域点击事件
const handleReadAreaClick = (e) => {
  // 0. 🔥 拦截：如果当前正在【加载中】、【编辑】或【新增】章节，绝对禁止触发翻页！
  if (loading.value || isEditing.value || isAdding.value) return

  // 1. 🔥 拦截：如果用户划选了文字准备右键做笔记，阻止翻页
  // (使用更加严谨的 isCollapsed 判断，防止偶发选词判定失效)
  const selection = window.getSelection()
  if (selection && selection.toString().trim().length > 0) return

  // 2. 获取阅读容器的宽度和点击的 X 坐标
  const container = contentContainerRef.value
  if (!container) return

  const containerRect = container.getBoundingClientRect()
  const clickX = e.clientX - containerRect.left
  const containerWidth = containerRect.width

  // 3. 判断点击区域：稍微放宽判定面积，让用户点得更爽
  if (clickX < containerWidth * 0.3) {
    prevPage()
  } else if (clickX > containerWidth * 0.7) {
    nextPage()
  } else {
    // 🔥 点击中心区域，切换工具栏显示/隐藏
    showToolbar.value = !showToolbar.value
  }
}

// 上一章
const prevChapter = async () => {
  if (currentNumber.value <= 1) {
    ElMessage.info('已经是第一章了！');
    return;
  }
  if (loading.value) {
    ElMessage.warning('正在加载中，请稍候...');
    return;
  }
  flipDirection.value = 'backward';
  await updateReadTimeLong()
  currentNumber.value -= 1;
  await loadChapter();
  await updateReadProgress()
  window.scrollTo(0, 0);
  handleScroll()
};
// 下一章
const nextChapter = async () => {
  if (totalChapter.value === 0) {
    ElMessage.warning('未获取到总章节数，无法跳转下一章！');
    return;
  }
  if (currentNumber.value >= totalChapter.value) {
    ElMessage.info('已经是最后一章了！');
    return;
  }
  flipDirection.value = 'forward'; // 章节向前翻沿用该方向
  await updateReadTimeLong()
  currentNumber.value += 1;
  await loadChapter();
  await updateReadProgress()
  window.scrollTo(0, 0);
  handleScroll()
};
// 打开章节列表
const openChapterList = async () => {
  try {
    const allChapterRes = await request.get('/book/chapter', {
      params: { isbn: currentIsbn.value }
    })
    allChapters.value = allChapterRes.data || []
    cachedTotalChapter.value = allChapters.value.length
    cachedIsbn.value = currentIsbn.value
    chapterPage.value = Math.ceil(currentNumber.value / chapterPageSize.value)
    isChapterListShow.value = true
  } catch (err) {
    console.error('加载章节列表失败', err)
    petStore.addMessage('加载章节列表失败')
  }
}
// 跳转指定章节
const jumpToChapter = async (chapterNumber) => {
  if (chapterNumber === currentNumber.value) {
    isChapterListShow.value = false
    return
  }
  currentNumber.value = chapterNumber
  await loadChapter()
  await updateReadProgress()
  window.scrollTo(0, 0)
  isChapterListShow.value = false
}

// ====================== 18. 核心业务方法 - 章节编辑/新增 ======================
// 进入编辑模式
const editChapter = () => {
  if (!currentChapter.value) return
  chapterBackup.value = JSON.parse(JSON.stringify(currentChapter.value))
  editingChapterName.value = currentChapter.value.name || ''
  editingChapterContent.value = currentChapterRawText.value || ''
  isEditing.value = true
  ElMessage.info('进入编辑模式')
}
// 确认编辑
const handleConfirmEditChapter = async () => {
  if (!editingChapterName.value.trim()) {
    petStore.addMessage('章节名称不能为空！');
    return;
  }
  if (!editingChapterContent.value.trim()) {
    petStore.addMessage('章节内容不能为空！');
    return;
  }

  const updatedChapter = JSON.parse(JSON.stringify(currentChapter.value));
  updatedChapter.name = editingChapterName.value.trim();
  updatedChapter.fullText = editingChapterContent.value.trim();
  updatedChapter.create_time = new Date().getTime();

  try {
    const res = await request.put('/book/chapter', updatedChapter)
    if (res.code === 200) {
      ElMessage.success('章节修改成功！');
      await loadChapter();
      isEditing.value = false;
    } else {
      petStore.addMessage(res.msg || '章节修改失败');
    }
  } catch (err) {
    petStore.addMessage('修改出错啦，请稍后再试~');
    console.error('修改章节失败：', err);
  }
};
// 取消编辑
const handleCancelEditChapter = () => {
  if (chapterBackup.value) {
    currentChapter.value = JSON.parse(JSON.stringify(chapterBackup.value));
  }
  editingChapterName.value = '';
  editingChapterContent.value = '';
  isEditing.value = false;
  ElMessage.info('已取消编辑，恢复原内容');
};
// 打开新增章节
const handleAddChapter = () => {
  newChapterNumber.value = totalChapter.value + 1;
  addingChapterName.value = '';
  addingChapterContent.value = '';
  isAdding.value = true;
  nextTick(() => {
    const addNameInput = document.querySelector('.add-chapter-name-input');
    if (addNameInput) addNameInput.focus();
  });
};
// 确认新增章节
const handleConfirmAddChapter = async () => {
  if (!addingChapterName.value.trim()) {
    petStore.addMessage('章节名称不能为空！');
    return;
  }
  if (!addingChapterContent.value.trim()) {
    petStore.addMessage('章节内容不能为空！');
    return;
  }

  const newChapter = {
    chapter_id: `${currentIsbn.value}-${newChapterNumber.value}`,
    isbn: currentIsbn.value,
    number: newChapterNumber.value,
    name : addingChapterName.value.trim(),
    fullText : addingChapterContent.value.trim(),
    create_time : new Date().getTime()
  };

  try {
    const res = await request.post('/book/chapter', newChapter)
    if (res.code === 200) {
      ElMessage.success('章节新增成功！');
      cachedTotalChapter.value = newChapterNumber.value;
      totalChapter.value = newChapterNumber.value;
      isAdding.value = false;
      currentNumber.value = newChapterNumber.value;
      await loadChapter();
      await updateReadProgress();
    } else {
      petStore.addMessage(res.msg || '章节新增失败');
    }
  } catch (err) {
    petStore.addMessage('新增出错啦，请稍后再试~');
    console.error('新增章节失败：', err);
  }
};
// 取消新增章节
const handleCancelAddChapter = () => {
  addingChapterName.value = '';
  addingChapterContent.value = '';
  isAdding.value = false;
  ElMessage.info('已取消新增章节');
};

// ====================== 19. 核心业务方法 - 阅读设置（字体/背景） ======================
// 设置字体大小
const setFontSize = (level) => {
  fontSizeLevel.value = level
  fontSizeValue.value = fontSizeMap[level]
  const sizeText = level === 'small' ? '小' : level === 'medium' ? '中' : '大'
  ElMessage.success(`字体已调整为${sizeText}号`)
}

// ====================== 20. 核心业务方法 - 笔记功能 ======================
// 加载章节笔记
const loadChapterNotes = async () => {
  if (!currentChapter.value?.chapter_id) return
  try {
    const res = await request.get('user/note', {
      params: { chapterId: currentChapter.value.chapter_id }
    })
    chapterNotes.value = res.data || []
  } catch (err) {
    console.error('加载章节笔记失败', err)
  }
}
// 处理内容添加笔记下划线
const processContentWithUnderline = (content) => {
  if (!content || chapterNotes.value.length === 0) return content
  let processedContent = content

  chapterNotes.value.forEach(note => {
    const noteText = note.text?.trim()
    const noteId = note.id
    if (!noteText || !noteId) return

    const escapedText = noteText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const regex = new RegExp(`(${escapedText})`, 'g')

    const underlineColorMap = {
      1: { light: '#000000', dark: '#e5e7eb' },
      2: { light: '#3b82f6', dark: '#60a5fa' },
      3: { light: '#10b981', dark: '#34d399' },
      4: { light: '#f59e0b', dark: '#fbbf24' },
      5: { light: '#ef4444', dark: '#f87171' }
    };

    const colorConfig = underlineColorMap[note.type] || underlineColorMap[1];
    const underlineColor = isDark.value ? colorConfig.dark : colorConfig.light;

    processedContent = processedContent.replace(regex, (match) => {
      return `<span
        style="text-decoration: underline;
               text-decoration-color: ${underlineColor};
               text-underline-offset: 3px;
               text-decoration-thickness: 1px;
               cursor: pointer;"
        data-note-id="${noteId}"
      >${match}</span>`
    })
  })
  return processedContent
}
// 判断文本是否已有笔记
const isTextHasNote = (text) => {
  if (!text) return false
  return chapterNotes.value.some(note => {
    const noteText = note.text?.trim()
    if (!noteText) return false
    return text === noteText || text.includes(noteText) || noteText.includes(text)
  })
}


// ====================== 🔥 新增：静默发射手账书摘卡片到共读房间 ======================
const silentPublishNoteCardToRoom = async (excerptText, userThoughts) => {
  if (!currentUserId.value) return

  // 1. 嗅探用户当前是否正处于某一个共读房间中
  const activeRoomStr = sessionStorage.getItem(`co_room_token_${currentUserId.value}`)
  if (!activeRoomStr) return // 用户没进共读房间，静默取消，不打扰

  try {
    const currentRoomObj = JSON.parse(activeRoomStr)
    if (!currentRoomObj || !currentRoomObj.id) return

    // 2. 严格对齐 CoReadingRoom 里的和纸手账数据结构
    const noteCardPayload = {
      type: 'NOTE_CARD',
      userId: currentUserId.value,
      excerpt: excerptText.trim(),
      content: userThoughts ? userThoughts.trim() : '分享了一段触动心弦的文字',
      timestamp: Date.now()
    }

    // 3. 异步调用发信 Servlet（BookRoomChatServlet），利用载荷嗅探器自动广播！
    await request.post('/book/room/chat', {
      roomId: currentRoomObj.id,
      userId: currentUserId.value,
      content: JSON.stringify(noteCardPayload) // 转成纯字符串，穿透后端套娃
    })

    console.log(`[共读舱静默联动] 已将笔记自动发布为书摘卡片至房间 #${currentRoomObj.id}`)
  } catch (err) {
    console.warn('[共读舱静默联动失败] 不影响阅读', err)
  }
}

// 添加笔记
const addNoteFromSelection = async () => {
  if (!selectedText.value || !currentUserId.value || !currentIsbn.value) {
    ElMessage.warning('缺少必要信息，无法添加笔记')
    return
  }
  if (isTextHasNote(selectedText.value)) {
    ElMessage.warning('选中的文本中已存在笔记，无法重复添加')
    return
  }

  try {
    // 🔥 修复1：在清空输入框前，提前提取并保存安全的文本和批注变量
    const safeText = selectedText.value || '未知'
    const safeComment = readerCommentInput.value.trim() || '无'

    // 🔥 提前将原文和批注暂存到常量中，供稍后静默发射使用！
    const excerptToPublish = selectedText.value
    const commentToPublish = readerCommentInput.value.trim()

    const noteData = {
      userId: currentUserId.value,
      isbn: currentIsbn.value,
      chapterId: `${currentIsbn.value}-${currentNumber.value}`,
      text: selectedText.value,
      type: selectedNoteType.value,
      createTime : Date.now(),
      readerComment: readerCommentInput.value.trim() || null
    }

    const res = await request.post('/user/note', noteData)
    if (res.code === 200) {
      ElMessage.success('笔记添加成功！')
      silentPublishNoteCardToRoom(excerptToPublish, commentToPublish)
      graphRef.value?.createAutoGraphNode(graphRef.value?.GRAPH_NODE_TYPE.NOTE, '我的笔记', selectedText.value)
      if (typeof fetchUserNotes === 'function') {
        await fetchUserNotes(currentUserId.value)
      }
      await loadChapterNotes()

      // 清空弹窗和输入框
      showAddNoteDialog.value = false
      readerCommentInput.value = ''

      // 🔥 修复2：使用模板字符串安全拼接，避免 || null 的优先级错误
      const promptText = `用户添加笔记，选中文本：${safeText}，用户批注：${safeComment}`

      // 🔥 修复3：补齐 extraData (包含 isbn 和 chapterNumber)，并加上 .catch 容错
      sendAiMsg(promptText, 'ai_debater', {
        isbn: currentIsbn.value,
        chapterNumber: currentNumber.value
      }).catch(err => console.warn('AI辩论者触发失败，不影响笔记流程', err))

    } else {
      petStore.addMessage(res.msg || '添加笔记失败')
    }
  } catch (err) {
    console.error('添加笔记失败', err)
    petStore.addMessage('添加笔记出错啦~')
  }
}
// 删除笔记
const deleteNote = async () => {
  if (!currentNoteId.value) return

  try {
    const res = await request.delete('/user/note', { params: { id: currentNoteId.value } })
    if (res.code === 200) {
      ElMessage.success({ message: '删除成功', center: true })
      await fetchUserNotes(currentUserId.value)
    }
    await loadChapterNotes()
  } catch (err) {
    console.error('删除笔记失败', err)
    petStore.addMessage('删除失败，请重试')
  }
}
// 取消添加笔记
const handleCancel = () => {
  showAddNoteDialog.value = false
  selectedNoteType.value = null
  readerCommentInput.value = ''
}
// 笔记弹窗关闭前处理
const handleBeforeClose = (done) => {
  selectedNoteType.value = null
  readerCommentInput.value = ''
  done()
}

// ====================== 21. 核心业务方法 - 文本朗读 ======================
// ====================== 🔥 右键菜单方法（和个人界面完全同款） ======================
const handleContextMenu = (e) => {
  const target = e.target
  // 禁止输入框/按钮等元素右键
  if (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'BUTTON' ||
      target.closest('.el-button') ||
      target.closest('.el-input') ||
      target.closest('.el-select')
  ) return

  e.preventDefault()
  e.stopPropagation()
  // 清空所有状态
  clearContextMenu()
  rightClickNoteId.value = null

  // 获取选中文本
  const selection = window.getSelection()
  const text = selection.toString().trim()
  rightClickSelectedText.value = text
  selectedText.value = text // 同步原有变量

  // 检测是否右键在笔记上
  const noteEl = target.closest('[data-note-id]')
  rightClickNoteId.value = noteEl?.dataset.noteId || null
  currentRightClickNote.value = noteEl?.dataset || null
  currentNoteId.value = rightClickNoteId.value // 同步原有变量


  // 计算菜单位置（和个人页完全一致）
  const menuWidth = 180
  const menuHeight = 50
  let x = e.clientX
  let y = e.clientY
  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10
  contextMenuPosition.value = { x, y }

  showContextMenu.value = true
}

// 点击空白关闭菜单
const handleClickClose = inject('closeContextMenu')
// 🔥 开始朗读 (AI API 版)
const startReading = () => {
  stopReading() // 先清理上一次的播放

  if (!fullNoteText.value?.trim()) {
    petStore.addMessage('章节内容为空，无法朗读')
    return
  }

  let textToRead = ''
  if (readBreakIndex.value > 0) {
    textToRead = fullNoteText.value.slice(readBreakIndex.value)
    readStartIndex.value = readBreakIndex.value
    readBreakIndex.value = 0
    ElMessage.success('从断点继续朗读')
  } else if (selectedText.value?.trim()) {
    const selectedIndex = fullNoteText.value.indexOf(selectedText.value)
    if (selectedIndex !== -1) {
      textToRead = fullNoteText.value.slice(selectedIndex)
      readStartIndex.value = selectedIndex
      ElMessage.success('将从选中位置开始朗读')
    } else {
      textToRead = selectedText.value
      readStartIndex.value = 0
      ElMessage.info('将朗读选中的文本')
    }
  } else {
    textToRead = fullNoteText.value
    readStartIndex.value = 0
    ElMessage.info('将朗读全文')
  }

  if (!textToRead.trim()) {
    petStore.addMessage('没有可朗读的文本')
    return
  }

  readLength.value = textToRead.length
  fullReadingText.value = textToRead

  // 🔥 改为整段文本发送（不用句子拆分），用缓存模式避免卡顿
  const paragraphs = textToRead.split(/\n{2,}/).filter(p => p.trim().length > 0)
  textChunksForAi.value = paragraphs.length > 0 ? paragraphs : [textToRead]
  currentChunkIndexForAi.value = 0

  showReadingDialog.value = true
  playNextAiChunk()
}

// 🔥 建立 AI 情绪/角色 到 Edge-TTS 音色的映射字典
const emotionVoiceMap = {
  '欢快': 'zh-CN-YunjianNeural',
  '悲伤': 'zh-CN-YunzeNeural',
  '愤怒': 'zh-CN-YunfengNeural',
  '温柔': 'zh-CN-XiaoyiNeural',
  '平静': 'zh-CN-YunxiNeural'
}
const roleVoiceMap = {
  '青年男': 'zh-CN-YunjianNeural',
  '青年女': 'zh-CN-XiaoxiaoNeural',
  '中年男': 'zh-CN-YunzeNeural',
  '小女孩': 'zh-CN-XiaoyouNeural',
  '老爷爷': 'zh-CN-YunjianNeural' // 可替换其他老年音色
}

// 🔥 核心逻辑：播放下一个音频片段（完美 Fetch 版）
const playNextAiChunk = async () => {
  if (currentChunkIndexForAi.value >= textChunksForAi.value.length) {
    stopReading()
    ElMessage.success('沉浸式朗读完成')
    return
  }

  const chunkText = textChunksForAi.value[currentChunkIndexForAi.value]
  isAudioLoading.value = true // UI 显示波浪纹/加载中动画

  try {
    let currentVoice = 'zh-CN-YunxiNeural' // 兜底音色
    try {
      const emotionResult = await sendAiMsg(chunkText, 'emotion_analyze')
      const cleanEmotion = emotionResult?.trim().replace(/['"【】。]/g, '') || '平静'
      currentVoice = emotionVoiceMap[cleanEmotion] || 'zh-CN-YunxiNeural'
    } catch (e) {
      console.warn('情绪分析超时，使用默认平静音色继续朗读')
    }

    const response = await fetch('/api/tts/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text: chunkText,
        voice: currentVoice,
        cache: true  // 🔥 缓存模式：整段一次合成，返回URL不再逐句等
      })
    })

    if (!response.ok) throw new Error('TTS接口请求失败')

    // 🔥 缓存模式返回的是 JSON { url, size }
    const result = await response.json()
    const audioUrl = result.url
    if (!audioUrl) throw new Error('TTS未返回音频URL')

    // 直接播放缓存的音频URL（不需要blob了）
    if (currentAudioUrl.value) URL.revokeObjectURL(currentAudioUrl.value)
    currentAudioUrl.value = audioUrl

    // 🔥 核心修复2-1：如果 audioPlayer 为空，先实例化 Audio 对象
    if (!audioPlayer.value) {
      audioPlayer.value = new Audio()
    }

    audioPlayer.value.src = currentAudioUrl.value
    audioPlayer.value.playbackRate = readingRate.value

    audioPlayer.value.onended = () => {
      currentChunkIndexForAi.value++
      playNextAiChunk() // 递归读下一段
    }

    audioPlayer.value.onplay = () => {
      isAudioLoading.value = false
      isReading.value = true
      isPaused.value = false

      let preLength = 0
      for(let i = 0; i < currentChunkIndexForAi.value; i++) {
        preLength += textChunksForAi.value[i].length
      }
      currentReadIndex.value = preLength + readStartIndex.value
      nextTick(() => {
        const highlightEl = readingTextRef.value?.querySelector('.reading-highlight')
        highlightEl?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    }

    await audioPlayer.value.play()

  } catch (err) {
    console.error('AI音频链路出错:', err)
    petStore.addMessage('网络波动，尝试跳过该段...')
    isAudioLoading.value = false
    currentChunkIndexForAi.value++
    playNextAiChunk()
  }
}

// 暂停/继续与倍速逻辑
const togglePauseReading = () => {
  if (!audioPlayer.value || !audioPlayer.value.src) return
  isPaused.value ? audioPlayer.value.play() : audioPlayer.value.pause()
  isPaused.value = !isPaused.value
}

// 🛑 核心：彻底掐断播放与网络请求
const stopReading = () => {
  // 🔥 核心修复：添加 .value
  if (ttsAbortController.value) {
    ttsAbortController.value.abort()
    ttsAbortController.value = null
  }

  if (audioPlayer.value){
    audioPlayer.value?.pause();
    audioPlayer.value.src = ''
    audioPlayer.value.onended = null
    audioPlayer.value.onplay = null
  }

  if (currentAudioUrl.value) {
    URL.revokeObjectURL(currentAudioUrl.value)
    currentAudioUrl.value = ''
  }
  audioCache.value.forEach(promise => {
    promise.then(url => { if (url) URL.revokeObjectURL(url) }).catch(() => {})
  })
  audioCache.value.clear()

  isReading.value = false
  isPaused.value = false
  isAudioLoading.value = false
  showReadingDialog.value = false
  currentChunkIndexForAi.value = 0
  currentSubtitle.value = ''
  // 新增：清除段落高亮
  currentHighlightParaId.value = null
}

// 🚀 核心：预加载机制
const fetchAudioForChunk = async (index) => {
  if (index >= textChunksForAi.value.length) return null

  if (audioCache.value.has(index)) {
    return audioCache.value.get(index)
  }

  const fetchPromise = (async () => {
    const chunkText = textChunksForAi.value[index]
    let currentVoice = 'zh-CN-YunxiNeural'

    if (isAiMode.value) {
      try {
        const isDialogue = /[“"「『]/.test(chunkText)
        if (isDialogue) {
          const roleResult = await sendAiMsg(chunkText, 'role_analyze')
          const cleanRole = roleResult?.trim().replace(/['"【】。]/g, '') || '青年女'
          currentVoice = roleVoiceMap[cleanRole] || 'zh-CN-XiaoxiaoNeural'
        } else {
          const emotionResult = await sendAiMsg(chunkText, 'emotion_analyze')
          const cleanEmotion = emotionResult?.trim().replace(/['"【】。]/g, '') || '平静'
          currentVoice = emotionVoiceMap[cleanEmotion] || 'zh-CN-YunxiNeural'
        }
      } catch (e) {
        console.warn('AI情感分析超时，使用默认音色')
      }
    }

    const response = await fetch('/api/tts/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: chunkText, voice: currentVoice, cache: true }),
      signal: ttsAbortController.value.signal
    })

    if (!response.ok) throw new Error('TTS请求失败')

    const result = await response.json()
    return result.url || ''
  })()

  audioCache.value.set(index, fetchPromise)
  return fetchPromise
}

// 🎵 核心：播放控制器
const playAiChunk = async (index) => {
  if (!isReading.value && !showReadingDialog.value) return

  if (index >= textChunksForAi.value.length) {
    stopReading()
    ElMessage.success('阅读完毕')
    return
  }

  currentChunkIndexForAi.value = index
  currentSubtitle.value = textChunksForAi.value[index]
  isAudioLoading.value = true

  try {
    const url = await fetchAudioForChunk(index)

    fetchAudioForChunk(index + 1).catch(e => {
      if (e.name !== 'AbortError') console.warn('预加载下一段失败:', e)
    })

    if (currentAudioUrl.value) URL.revokeObjectURL(currentAudioUrl.value)
    currentAudioUrl.value = url

    // 🔥 核心修复2-2：如果 audioPlayer 为空，先实例化 Audio 对象
    if (!audioPlayer.value) {
      audioPlayer.value = new Audio()
    }

    audioPlayer.value.src = currentAudioUrl.value
    audioPlayer.value.playbackRate = readingRate.value

    audioPlayer.value.onended = () => {
      playAiChunk(index + 1)
    }

    audioPlayer.value.onplay = () => {
      isAudioLoading.value = false
    }

    await audioPlayer.value.play()

  } catch (err) {
    if (err.name === 'AbortError') {
      console.log('🛑 音频请求已被用户手动掐断')
      return
    }
    console.error('播放出错:', err)
    petStore.addMessage('当前段落生成失败，自动跳过...')
    isAudioLoading.value = false
    playAiChunk(index + 1)
  }
}

const startReadingProcess = (modeIsAi) => {
  stopReading()

  if (!fullNoteText.value?.trim()) {
    petStore.addMessage('没有可朗读的文本')
    return
  }

  isAiMode.value = modeIsAi
  ttsAbortController.value = new AbortController()
  audioCache.value.clear()
  batchAudioUrls.value = []
  batchAudioIndex.value = 0

  const rawText = fullNoteText.value

  // ========== 新增：分句 + 段落映射 ==========
  if (rawText === currentChapterRawText.value && currentChapter.value?.paragraphs) {
    // 全章朗读：逐段拆分句子，记录每个句子对应的段落ID
    const chunks = []
    const paraMap = []
    currentChapter.value.paragraphs.forEach(para => {
      // 和原规则完全一致的分句正则
      const sentences = para.content.match(/[^。！？.!?\n]+[。！？.!?\n]*/g)
          ?.map(t => t.trim()).filter(t => t.length > 0) || []
      sentences.forEach(sentence => {
        chunks.push(sentence)
        paraMap.push(para.id)
      })
    })
    textChunksForAi.value = chunks.length > 0 ? chunks : [rawText]
    chunkParagraphMap.value = paraMap
  } else {
    // 选中文本朗读等非全章场景：沿用原逻辑，不触发段落高亮
    textChunksForAi.value = rawText.match(/[^。！？.!?\n]+[。！？.!?\n]*/g)
        ?.map(t => t.trim()).filter(t => t.length > 0) || [rawText]
    chunkParagraphMap.value = []
    currentHighlightParaId.value = null
  }
  // ==========================================

  showReadingDialog.value = true
  isReading.value = true

  // 批量请求：一次性发送所有句子到后端并行生成
  startBatchPlayback('zh-CN-YunxiNeural')
}

// 🚀 批量音频 URL 缓存
const batchAudioUrls = ref([])
const batchAudioIndex = ref(0)

// 朗读句子 → 所属段落ID 的映射表
const chunkParagraphMap = ref([])
// 当前高亮的段落ID
const currentHighlightParaId = ref(null)

const startBatchPlayback = async (defaultVoice) => {
  const chunks = textChunksForAi.value
  if (!chunks.length) return

  // 初始化：创建空结果数组
  batchAudioUrls.value = new Array(chunks.length).fill(null)
  batchAudioIndex.value = -1
  let startedPlaying = false

  const segments = chunks.map(text => ({ text, voice: defaultVoice }))
  try {
    const res = await fetch('/api/tts/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(segments)
    })
    if (!res.ok) {
      petStore.addMessage('TTS批量生成失败')
      stopReading()
      return
    }

    // SSE 流式读取：每完成一段立即收到
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 解析 SSE 事件
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留未完成的行
      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event: ')) eventType = line.slice(7).trim()
        else if (line.startsWith('data: ') && eventType === 'segment') {
          try {
            const seg = JSON.parse(line.slice(6))
            if (seg.url) {
              batchAudioUrls.value[seg.index] = seg.url
              // 第一个 URL 到达时立即开始播放
              if (!startedPlaying) {
                startedPlaying = true
                // 找到第一个有效 URL 的 index
                const firstIdx = batchAudioUrls.value.findIndex(u => u !== null)
                if (firstIdx >= 0) {
                  batchAudioIndex.value = firstIdx
                  playBatchChunk()
                }
              }
            }
          } catch (e) { /* skip malformed */ }
          eventType = ''
        } else if (line.startsWith('data: ') && eventType === 'done') {
          logger.info('TTS SSE done')
          eventType = ''
        }
      }
    }
  } catch (e) {
    console.error('批量TTS SSE失败', e)
    if (!startedPlaying) stopReading()
  }
}

const playBatchChunk = () => {
  if (!isReading.value) return

  // 跳过已播放的，向前找下一个有效 URL
  while (batchAudioIndex.value < batchAudioUrls.value.length) {
    if (batchAudioUrls.value[batchAudioIndex.value] !== null &&
        batchAudioUrls.value[batchAudioIndex.value] !== undefined) {
      break
    }
    batchAudioIndex.value++
  }

  // 检查是否全部完成
  if (batchAudioIndex.value >= batchAudioUrls.value.length) {
    // 还有未生成的段？等待 500ms 再试（SSE 可能还在推送）
    const hasPending = batchAudioUrls.value.some(u => u === null)
    if (hasPending) {
      setTimeout(playBatchChunk, 500)
      return
    }
    stopReading()
    ElMessage.success('阅读完毕')
    return
  }

  const url = batchAudioUrls.value[batchAudioIndex.value]
  if (!url) {
    // 还没生成，等一会儿再试
    setTimeout(() => playBatchChunk(), 300)
    return
  }

  const chunkText = textChunksForAi.value[batchAudioIndex.value] || ''
  currentSubtitle.value = chunkText
  currentChunkIndexForAi.value = batchAudioIndex.value

  if (audioPlayer.value) {
    audioPlayer.value.pause()
    audioPlayer.value.src = ''
  }

  const audio = new Audio(url)
  audio.playbackRate = readingRate.value
  audioPlayer.value = audio
  isAudioLoading.value = false

  audio.onended = () => {
    batchAudioIndex.value++
    playBatchChunk()
  }
  audio.onerror = () => {
    batchAudioIndex.value++
    playBatchChunk()
  }
  audio.play().catch(() => {
    batchAudioIndex.value++
    playBatchChunk()
  })
}

// 调整朗读倍速
const changeReadingRate = () => {
  ElMessage.success(`倍速已调整为 ${readingRate.value}x`)
  // 🔥 核心修复4：只有播放器存在时，才去设置倍速
  if (audioPlayer.value) {
    audioPlayer.value.playbackRate = readingRate.value
  }
}
// 朗读弹窗拖拽
const handleDragStart_read = (e) => {
  if (e.target.closest('.reading-header-buttons')) return
  isDragging = true
  hasMoved = false
  startX = e.clientX
  startY = e.clientY
  startRight = rightPos.value
  startBottom = bottomPos.value

  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', handleDragEnd)
  e.preventDefault()
}
const handleDragMove = (e) => {
  if (!isDragging) return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  rightPos.value = startRight - dx
  bottomPos.value = startBottom - dy
}
const handleDragEnd = () => {
  isDragging = false
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
}

// ====================== 22. 核心业务方法 - 分享功能 ======================
// 复制选中文本
const copySelectedText = () => {
  if (!selectedText.value) return
  navigator.clipboard.writeText(selectedText.value).then(() => {
    ElMessage.success('复制成功！')
  }).catch(() => {
    petStore.addMessage('复制失败，请手动复制')
  })
}
// 分享选段到论坛
const shareSelectedTextToForum = () => {
  if (!selectedText.value || !currentIsbn.value) {
    ElMessage.warning('请先选中要分享的文本')
    return
  }

  const shareData = {
    bookName: bookName.value,
    chapterNumber: currentNumber.value,
    text: selectedText.value
  }
  const content = generateShareContent('text', shareData)

  shareToForum({
    type: 'text',
    id: `${currentIsbn.value}-${currentNumber.value}`,
    ...content,
    bookInfo: {
      isbn: currentIsbn.value,
      name: bookName.value
    }
  })
}
// 打开分享图片预览
const openShareImagePreview = async () => {
  if (!selectedText.value || !currentIsbn.value) {
    ElMessage.warning('请先选中要分享的文本')
    return
  }
  try {
    shareText.value = '　　' + selectedText.value
    shareBookName.value = bookName.value
    shareTextColor.value = isDark.value ? '#e5e7eb' : '#333333'
    const bookRes = await request.get('/book', { params: { isbn: currentIsbn.value } })
    shareBookCover.value = bookRes.data[0]?.pictureName || '/default-book.png'
    isSharePreviewShow.value = true
  } catch (err) {
    console.error('打开分享预览失败', err)
    petStore.addMessage('打开分享预览失败，请稍后再试')
  }
}

const downloadShareImage = async () => {
  try {
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 100))

    const images = shareCardRef.value.querySelectorAll('img')
    await Promise.all(
        Array.from(images).map(img =>
            img.complete ? Promise.resolve() : new Promise(resolve => {
              img.onload = resolve
              img.onerror = resolve
            })
        )
    )

    // 🔥 核心2：修改这里的配置
    const canvas = await html2canvas(shareCardRef.value, {
      useCORS: true,
      allowTaint: true,
      scale: 2,
      logging: false,
      // 💡 赋予实色背景：如果是暗黑模式用深灰，白天用浅灰，彻底消除边角透明网格！
      backgroundColor: isDark.value ? '#1a1a1a' : '#f5f7fa',
      width: shareCardRef.value.offsetWidth,   // 使用元素真实的 offsetWidth
      height: shareCardRef.value.offsetHeight  // 使用元素真实的 offsetHeight
    })

    const fileName = `${shareBookName.value}_${new Date().getTime()}.png`
    const link = document.createElement('a')
    link.download = fileName
    link.href = canvas.toDataURL('image/png')
    link.click()

    ElMessage.success('分享图片下载成功！')
  } catch (err) {
    console.error('生成分享图片失败', err)
    petStore.addMessage('生成分享图片失败，请稍后再试')
  }
}

// 取消分享
const cancelShare = () => {
  isSharePreviewShow.value = false
  resetShareConfig()
}
// 增大分享字体
const increaseFontSize = () => {
  if (shareFontSize.value < 24) {
    shareFontSize.value += 2
  }
}
// 减小分享字体
const decreaseFontSize = () => {
  if (shareFontSize.value > 8) {
    shareFontSize.value -= 2
  }
}

// ====================== 23. 核心业务方法 - 词典查询 ======================
// 查询词语
const queryWord = async () => {
  if (!selectedText.value) {
    ElMessage.warning('请先选中要查询的汉字');
    return;
  }

  try {
    dictWord.value = selectedText.value;

    if (isMobile.value) {
      dictPosition.value = { x: 0, y: 0 };
    } else {
      const popupWidth = 320;
      const popupHeight = 200;
      let x = menuPosition.value.x;
      let y = menuPosition.value.y - 10;

      if (x + popupWidth / 2 > window.innerWidth) {
        x = window.innerWidth - popupWidth / 2 - 20;
      }
      if (x - popupWidth / 2 < 0) {
        x = popupWidth / 2 + 20;
      }
      if (y - popupHeight < 0) {
        y = menuPosition.value.y + 30;
      }

      dictPosition.value = { x, y };
    }

    const res = await request.get('dictionary', {
      params: { word: dictWord.value }
    });

    dictPinyin.value = '';
    dictMeanings.value = [];

    if (res.error_code === 0) {
      const data = res.result;
      dictPinyin.value = data.pinyin || '';
      let simpleMeanings = [];
      if (data.jijie) {
        simpleMeanings = data.jijie.filter(item => {
          return item && item.trim() !== '' && item.length > 4;
        });
      }
      dictMeanings.value = simpleMeanings.slice(0, 3);
    } else {
      dictMeanings.value = ['查询失败'];
    }
    isDictShow.value = true;
  } catch (err) {
    console.error('查询字典失败', err);
    dictMeanings.value = ['解析失败，请重试'];
    isDictShow.value = true;
  }
};
// 关闭词典弹窗
const closeDict = () => {
  isDictShow.value = false
  dictWord.value = ''
  dictPinyin.value = ''
  dictMeanings.value = []
}

// ====================== 24. 核心业务方法 - 阅读时长/阅读历史 ======================
// 🔥 简化后的单本书籍阅读记录更新方法（原子性操作，无需前端判断）
const updateBookReadRecord = async (elapsedSeconds) => {
  // 🔥 新增：组件已卸载则直接返回，不执行任何请求
  if (isUnmounted.value) return
  if (!currentUserId.value || !currentIsbn.value) return

  try {
    // ✅ 只需要传这3个参数，后端自动补全readDate和createTime
    // ✅ 自动实现：同一天同一本书累加时长，不同天新增记录
    await request.post('/user/record', {
      userId: currentUserId.value,
      isbn: currentIsbn.value,
      readDuration: elapsedSeconds
    })

    console.log('✅ 书籍阅读记录原子性更新成功 | ISBN:', currentIsbn.value, '本次时长:', elapsedSeconds, '秒')
  } catch (err) {
    console.error('❌ 更新书籍阅读记录失败', err)
  }
}

// 更新阅读时长
const updateReadTimeLong = async () => {
  // 🔥 新增：组件已卸载则直接返回，不执行任何请求
  if (isUnmounted.value) return
  if (!currentUserId.value) return

  try {
    const now = Date.now()
    const elapsedSeconds = Math.floor((now - readStartTime.value) / 1000)

    if (elapsedSeconds < 1) {
      readStartTime.value = now
      return
    }

    console.log('📚 更新阅读时长：+', elapsedSeconds, '秒')

    const userRes = await request.get('/user/info', {
      params: { userId: currentUserId.value }
    })
    const currentUser = userRes.data[0] || {}
    const currentReadTime = currentUser.read_time_long || 0

    const newReadTime = currentReadTime + elapsedSeconds
    await request.patch('/user/info', {
      userId: currentUserId.value,
      read_time_long: newReadTime
    })

    // 🔥 直接使用你已经注入的formatDateTime，不需要重复定义
    const formatDate = (date) => {
      return formatTime(date, false) // showTime=false 只返回yyyy-MM-dd格式
    }
    const today = formatDate(new Date())

    await request.post('/user/read', {
      userId: currentUserId.value,
      readDate: today,
      readDuration: elapsedSeconds
    })
    console.log('✅ 当日阅读时长上报成功，日期：', today, '时长：', elapsedSeconds, '秒')

    // 🔥 核心：同步更新当前书籍的阅读记录
    await updateBookReadRecord(elapsedSeconds)

    readStartTime.value = now
    console.log('✅ 阅读时长更新成功，当前总时长：', newReadTime, '秒')

    // ====================== 🔥 刷新最后阅读时间 ======================
    userStore.updateLastReadTime()
    readStartTime.value = now
  } catch (err) {
    console.error('❌ 更新阅读时长失败', err)
  }
}
// 更新阅读进度
const updateReadProgress = async () => {
  // 🔥 新增：组件已卸载则直接返回，不执行任何请求
  if (isUnmounted.value) return
  if (!currentUserId.value || !currentIsbn.value || !currentChapter.value) return

  try {
    const res = await request.get('/user/progress', {
      params: { userId: currentUserId.value, isbn: currentIsbn.value }
    })
    const progressList = res.data || []
    const currentProgress = progressList[0]

    const correctPageNum = `${currentIsbn.value}-${currentNumber.value}`
    const progressData = {
      userId: currentUserId.value,
      isbn: currentIsbn.value,
      pageNum: correctPageNum,
      lastReadTime: Date.now()
    }

    if (currentProgress) {
      progressData.id = currentProgress.id
      await request.patch('/user/progress', progressData)
      currentProgressId.value = currentProgress.id
    } else {
      await request.post('/user/progress', progressData)
    }
  } catch (err) {
    console.error('❌ 更新阅读历史失败', err)
  }
}


// ====================== 导入知识图谱组件 ======================
import KnowledgeGraph from '../components/KnowledgeGraph.vue'
// ====================== 导入有声书播放器 ======================
import AudiobookPlayer from '../components/AudiobookPlayer.vue'

// ====================== 2. 面板宽度 ======================
const graphPanelWidth = ref(userStore.graphPanelWidth || 400)
const graphRef = ref(null) // 知识图谱组件引用 // 读取全局宽度
// ====================== 有声书播放器 ======================
const showAudiobookPlayer = ref(false)
// ====================== 🌊 漂流瓶状态 ======================
const showThrowDialog = ref(false)
const newBottleContent = ref('')
const allowReply = ref(true)
const isThrowing = ref(false)

// 🔥 新增这三个缺失的变量！
const currentBottlePage = ref(1)
const bottlePageSize = 10        // 常量即可，不需要 ref
const totalAvailable = ref(0)

const catEmojis = ['🐱', '😺', '😸', '😹', '😻', '😼', '😽', '🙀', '😿', '😾']

// ====================== 🌊 拖拽与物理参数 (彻底重构为 Right/Bottom 绝对定位) ======================
const bottleRight = ref(-30) // 默认隐藏一半在右侧
const bottleBottom = ref(150)
const bottleTilt = ref(0)
const rippleCanvas = ref(null)

const isDragging_bottle = ref(false)
let hasDragMove = false
let bStartX = 0, bStartY = 0, bStartRight = 0, bStartBottom = 0
let lastMouseX = 0

// 🎯 边界防溢出计算
const applyBottleBoundary = (targetRight, targetBottom) => {
  const bottleW = 60
  const maxRight = window.innerWidth - bottleW
  const maxBottom = window.innerHeight - bottleW

  // 🛑 第一道防线：强行把 NaN、undefined 解毒归零
  const safeRight = isNaN(targetRight) || targetRight === null ? 0 : Number(targetRight)
  const safeBottom = isNaN(targetBottom) || targetBottom === null ? 0 : Number(targetBottom)

  // 允许向右突破负数实现隐藏，上下严格锁死在屏幕内
  bottleRight.value = Math.max(-30, Math.min(safeRight, maxRight))
  bottleBottom.value = Math.max(0, Math.min(safeBottom, maxBottom))
}

// 🧲 自动贴边吸附逻辑
const snapBottleToEdge = () => {
  if (bottleRight.value < 20) {
    bottleRight.value = -30 // 吸附回边缘
  }
}

// ✨ 两段式点击交互机
const handleBottleClick = (e) => {
  if (isDragging_bottle.value || hasDragMove) return
  triggerWaterRipple(e)

  if (bottleRight.value < 0) {
    // 状态1：半隐藏 -> 点击探出完整瓶身
    bottleRight.value = 20
  } else {
    // 状态2：完整状态 -> 打开投掷弹窗
    showThrowDialog.value = true
  }
}

const triggerWaterRipple = (e) => {
  const canvas = rippleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const rect = canvas.getBoundingClientRect()

  // 兼容移动端 Touch 事件的坐标获取
  let clientX = e.clientX
  let clientY = e.clientY
  if (e.touches && e.touches.length > 0) {
    clientX = e.touches[0].clientX
    clientY = e.touches[0].clientY
  } else if (e.changedTouches && e.changedTouches.length > 0) {
    clientX = e.changedTouches[0].clientX
    clientY = e.changedTouches[0].clientY
  }

  const x = clientX - rect.left
  const y = clientY - rect.top

  canvas.width = rect.width
  canvas.height = rect.height

  let radius = 0
  let alpha = 1

  const animateRipple = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(100, 200, 255, ${alpha})`
    ctx.lineWidth = 2
    ctx.stroke()

    radius += 2
    alpha -= 0.03

    if (alpha > 0) requestAnimationFrame(animateRipple)
    else ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
  animateRipple()
}

// ====================== 💻 PC端拖拽 ======================
const startDragBottle = (e) => {
  isDragging_bottle.value = true
  hasDragMove = false
  bStartX = e.clientX
  bStartY = e.clientY
  bStartRight = bottleRight.value
  bStartBottom = bottleBottom.value
  lastMouseX = e.clientX

  document.addEventListener('mousemove', onBottleDrag)
  document.addEventListener('mouseup', stopBottleDrag)
}

const onBottleDrag = (moveEvent) => {
  if (!isDragging_bottle.value) return
  hasDragMove = true
  const dx = moveEvent.clientX - bStartX
  const dy = moveEvent.clientY - bStartY

  applyBottleBoundary(bStartRight - dx, bStartBottom - dy)

  const vx = moveEvent.clientX - lastMouseX
  bottleTilt.value = Math.max(-30, Math.min(30, vx * 0.5))
  lastMouseX = moveEvent.clientX
}

const stopBottleDrag = () => {
  bottleTilt.value = 0
  isDragging_bottle.value = false
  setTimeout(() => { hasDragMove = false }, 100)
  snapBottleToEdge()
  document.removeEventListener('mousemove', onBottleDrag)
  document.removeEventListener('mouseup', stopBottleDrag)
}

// ====================== 📱 移动端拖拽 (彻底修复卡顿与点按冲突) ======================
const handleBottleTouchStart = (e) => {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  bStartX = touch.clientX
  bStartY = touch.clientY
  bStartRight = bottleRight.value
  bStartBottom = bottleBottom.value
  hasDragMove = false
  isDragging_bottle.value = false
  lastMouseX = touch.clientX
}

const handleBottleTouchMove = (e) => {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  const dx = touch.clientX - bStartX
  const dy = touch.clientY - bStartY

  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
    hasDragMove = true
    isDragging_bottle.value = true
  }

  if (isDragging_bottle.value) {
    // 只有确定在拖拽时，才阻止浏览器默认的滑动，保护原生点击事件不被吞
    if (e.cancelable) e.preventDefault()

    applyBottleBoundary(bStartRight - dx, bStartBottom - dy)

    const vx = touch.clientX - lastMouseX
    bottleTilt.value = Math.max(-30, Math.min(30, vx * 0.5))
    lastMouseX = touch.clientX
  }
}

const handleBottleTouchEnd = (e) => {
  bottleTilt.value = 0
  if (isDragging_bottle.value) {
    isDragging_bottle.value = false
    setTimeout(() => { hasDragMove = false }, 100)
    snapBottleToEdge()
  }
  // 注意：如果是单纯的点按，不在这里处理，放行给 @click 原生触发 handleBottleClick
}
// ====================== 2. 碎星入海特效 (Throw POST) ======================
const throwBottleIntoSea = async () => {
  if (!newBottleContent.value.trim()) return ElMessage.warning('寄语不能为空哦~')
  isThrowing.value = true

  try {
    // 发送 POST 请求，字段名严格匹配你的 Java 实体类
    await request.post('/book/bottle', {
      userid: currentUserId.value,
      isbn: currentIsbn.value || userStore.currentReadingIsbn,
      chapter: currentChapter.value.chapter || userStore.currentReadingChapter,
      content: newBottleContent.value,
      allowReply: allowReply.value ? 1 : 0
    })

    // DOM 转图片与碎星粒子特效
    const targetEl = document.getElementById('bottle-capture-target')
    const canvas = await html2canvas(targetEl, { backgroundColor: null })

    targetEl.style.visibility = 'hidden'
    triggerShatterEffect(canvas, targetEl.getBoundingClientRect())

    setTimeout(() => {
      showThrowDialog.value = false
      newBottleContent.value = ''
      targetEl.style.visibility = 'visible'
      isThrowing.value = false
      ElMessage.success('漂流瓶已汇入星海~')
    }, 1200)

  } catch (err) {
    console.error('扔瓶子失败', err)
    isThrowing.value = false
  }
}

const triggerShatterEffect = (sourceCanvas, rect) => {
  const ctx = sourceCanvas.getContext('2d')
  // 采样 50 个点位作为粒子
  for (let i = 0; i < 50; i++) {
    const x = Math.random() * sourceCanvas.width
    const y = Math.random() * sourceCanvas.height
    // 获取图片该点的颜色
    const pixel = ctx.getImageData(x, y, 1, 1).data
    const color = `rgba(${pixel[0]}, ${pixel[1]}, ${pixel[2]}, ${pixel[3] / 255})`

    createParticle(rect.left + x, rect.top + y, color)
  }
}

const createParticle = (startX, startY, color) => {
  const particle = document.createElement('div')
  particle.className = 'shatter-particle'
  particle.style.left = `${startX}px`
  particle.style.top = `${startY}px`
  particle.style.background = color

  // 随机抛物线轨迹参数
  const tx = (Math.random() - 0.5) * 300
  const ty = Math.random() * 400 + 200
  particle.style.setProperty('--tx', `${tx}px`)
  particle.style.setProperty('--ty', `${ty}px`)

  document.body.appendChild(particle)
  setTimeout(() => particle.remove(), 1000) // 动画结束后清理 DOM
}

// ====================== 3. 捞取与猫咪气泡生命周期 ======================

// ====================== 1. 捞取漂流瓶 (GET) ======================
/**
 * 分页拉取可捞取的漂流瓶，加入缓冲队列
 */
const fetchBottlePage = async () => {
  try {
    const res = await request.get('/book/bottle', {
      params: {
        random: '1',
        isbn: currentIsbn.value,
        loginUserId: currentUserId.value,
        pageNum: currentBottlePage.value,
        pageSize: bottlePageSize
      }
    })

    if (res && res.code === 200 && res.data) {
      const { list, total } = res.data
      totalAvailable.value = total
      // 新数据追加到队列末尾
      bottleQueue.value.push(...list)
      currentBottlePage.value++
      return list.length > 0
    }
    return false
  } catch (err) {
    console.warn('拉取漂流瓶列表失败', err)
    return false
  }
}

/**
 * 从队列弹出一个瓶子，生成气泡
 */
const popOneBubble = async () => {
  // 队列空了，尝试加载下一页
  if (bottleQueue.value.length === 0) {
    const hasMore = await fetchBottlePage()
    if (!hasMore || bottleQueue.value.length === 0) {
      // 没有更多瓶子了，停止定时器
      clearInterval(bubbleTimer.value)
      bubbleTimer.value = null
      return
    }
  }

  // 取出队首的瓶子
  const bottle = bottleQueue.value.shift()

  // 触发瓶子亮光特效
  hasNewBottle.value = true
  setTimeout(() => {
    hasNewBottle.value = false
  }, 3000)

  // 生成猫咪气泡
  spawnCatBubble(bottle)
}


const spawnCatBubble = (bottleData) => {
  const newBubble = {
    ...bottleData,
    _feId: Date.now() + Math.random(), // 确保前端渲染唯一性
    emoji: catEmojis[Math.floor(Math.random() * catEmojis.length)],
    isOpen: false,
    isLeaving: false,
    replyText: ''
  }
  floatingBubbles.value.push(newBubble)

  // 气泡飘到左侧后，2分钟(120000ms)自动消失
  setTimeout(() => {
    closeBubble(newBubble, true) // 强制销毁
  }, 120000)
}

const openBubble = (bubble) => {
  if (bubble.isOpen || bubble.isLeaving) return
  bubble.isOpen = true
}

/**
 * 初始化漂流瓶定时弹出机制
 * 页面加载时先拉第一页并立刻弹出第一个，之后每2分钟弹出一个
 */
const initBottleTimer = async () => {
  // 先清空旧定时器
  if (bubbleTimer.value) {
    clearInterval(bubbleTimer)
    bubbleTimer.value = null
  }

  // 初始化拉取第一页，立刻弹出第一个
  await fetchBottlePage()
  if (bottleQueue.value?.length > 0) {
    popOneBubble()
  }

  // 每2分钟弹出一个
  bubbleTimer.value = setInterval(() => {
    popOneBubble()
  }, 120000)
}

/**
 * 关闭气泡：先提交捞取记录（含回复内容），再播放消失动画
 */
const closeBubble = async (bubble, forceDestroy = false, isopen = false) => {
  // 已经在离开中不重复处理
  if (bubble.isLeaving) return

  if(isopen){
    const replyText = bubble.replyText?.trim()

    try {
      // 提交捞取记录（无论有没有回复都要提交，标记为已捞取）
      await request.post('/book/bottle', null, {
        params: {
          action: 'pick',
          loginUserId: currentUserId.value,
          bottleId: bubble.id,
          replycontent: replyText || ''
        }
      })

      if (replyText) {
        ElMessage.success('回复成功，猫咪已经带着你的信离开了~')
      }
    } catch (err) {
      console.error('捞取漂流瓶失败', err)
      if (!forceDestroy) {
        petStore.addMessage('捞取失败，请稍后重试')
        return // 失败不关闭气泡
      }
    }
  }


  // 播放离开动画
  bubble.isOpen = false
  bubble.isLeaving = true

  setTimeout(() => {
    const index = floatingBubbles.value.findIndex(b => b._feId === bubble._feId)
    if (index !== -1) floatingBubbles.value.splice(index, 1)
  }, 1000)
}


// 原回复按钮直接触发关闭（关闭时自动提交回复）
const replyToBubble = (bubble) => {
  const text = bubble.replyText?.trim()
  if (!text) return ElMessage.warning('回复内容不能为空~')
  closeBubble(bubble, false, true)
}

// 🎵 音乐特效状态
const musicParticles = ref([])
let particleIdCounter = 0
const particleTimer = ref(null)
const musicSymbols = ['🎵', '🎼', '🎶', '✨', '🌟', '⭐']

// 🌟 修改为：每次随机生成 1 到 2 个音符，并严格控制产生时间
const spawnMusicParticleBatch = () => {
  if (!isReading.value) return

  // 🔥 核心修改：一次随机生成 1 到 2 个音符
  const batchSize = Math.floor(Math.random() * 2) + 1;

  for(let i = 0; i < batchSize; i++) {
    const id = particleIdCounter++
    const symbol = musicSymbols[Math.floor(Math.random() * musicSymbols.length)]

    const randomYOffset = Math.random() * 10 - 5
    const duration = 18 + Math.random() * 6
    const delay = Math.random() * 3

    musicParticles.value.push({ id, symbol, randomYOffset, duration, delay })

    setTimeout(() => {
      musicParticles.value = musicParticles.value.filter(p => p.id !== id)
    }, (duration + delay) * 1000)
  }
}

// ====================== 25. 监听器 (Watch) ======================
// 监听朗读状态，自动开启/关闭音符生成
watch(isReading, (newVal) => {
  if (newVal) {
    // 🔥 核心修改：去掉了随机概率，确保严格每 6 秒触发一次
    particleTimer.value = setInterval(() => {
      spawnMusicParticleBatch()
    }, 6000) // 设置为 6000 毫秒 (6秒)

    // 开启朗读时立刻先生成一次，不用干等第一个 6 秒
    spawnMusicParticleBatch()
  } else {
    clearInterval(particleTimer.value)
    musicParticles.value = []
  }
})
// ====================== 25. 监听器 (Watch) ======================
// ====================== 替换：Watch 监听器 ======================
// 跟随朗读进度更新高亮段落
watch(currentChunkIndexForAi, (newIndex) => {
  if (chunkParagraphMap.value.length === 0) {
    currentHighlightParaId.value = null
    return
  }
  if (newIndex >= 0 && newIndex < chunkParagraphMap.value.length) {
    currentHighlightParaId.value = chunkParagraphMap.value[newIndex]
  } else {
    currentHighlightParaId.value = null
  }
})

watch(
    () => currentChapter.value?.paragraphs, // 👉 监听段落数组
    (newParagraphs) => {
      if (newParagraphs && newParagraphs.length > 0) {
        fullNoteText.value = currentChapterRawText.value // 让朗读功能获取完整文本
        if (isReading.value) {
          stopReading()
          showReadingDialog.value = false
        }
        isFirstLoad.value = false

        if (petStore.isAutoQuizRunning && petStore.autoQuizMode === 'ai') {
          petStore.stopAutoQuiz()
          if (currentChapterRawText.value.length >= 50) {
            petStore.startAutoQuiz(selectedQuizInterval.value, 'ai', currentChapterRawText.value)
          }
        }
      }
    },
    { immediate: true, deep: true }
)

// 🔥 新增：监听字体大小变化，重新计算分页
watch(
    fontSizeLevel,
    async () => {
      // 记住当前的阅读进度百分比
      targetProgressAfterLoad.value = progressPercentage.value

      await nextTick()

      // ✅ 延迟 100ms：确保顶部标题因大字号撑开、DOM彻底重排完成后，再去测量剩余的精准高度
      setTimeout(async () => {
        await paginateChapterContent()
      }, 300)
    }
)

// 监听章节ID变化，加载笔记
watch(
    () => currentChapter.value?.chapter_id,
    (newChapterId) => { if (newChapterId) loadChapterNotes() },
    { immediate: true }
)

// 🔥 新增：监听图谱面板关闭，彻底销毁旧画布实例，保证下次打开能重新成功挂载
// ====================== 新增：组件卸载标志位（最关键的保险） ======================
const isUnmounted = ref(false)


// ====================== 26. 生命周期钩子 ======================
onMounted(async () => {

  showToolbar.value = false

  // 重置卸载标志位
  isUnmounted.value = false

  // 绑定滚动事件
  if (scrollContainerRef.value) {
    scrollContainerRef.value.addEventListener('scroll', handleScroll)
    handleScroll() // 初始加载时计算一次进度
  }

  // 加载书籍&章节
  const isbn = route.query.isbn
  const number = route.query.number ? Number(route.query.number) : 1
  if (!isbn) {
    petStore.addMessage('缺少书籍ISBN参数！')
    await router.push('/home')
    return
  }
  currentIsbn.value = isbn

  try {
    const progressRes = await request.get('/user/progress', {
      params: { userId: currentUserId.value, isbn: isbn }
    })
    const progressList = progressRes.data || []
    const lastProgress = progressList[0]
    if (lastProgress) {
      const newchapterNumber = parseInt(lastProgress.pageNum.split('-')[1])
      currentNumber.value = newchapterNumber
      currentProgressId.value = lastProgress.id
    } else {
      currentNumber.value = number
    }
  } catch (err) {
    console.error('查询阅读历史失败', err)
    currentNumber.value = number
  }

  await Promise.all([getBookName(), loadChapter()])
  await updateReadProgress()

  // ====================== 🔥 开始阅读，记录到全局 store ======================
  userStore.startReading(currentIsbn.value,currentChapter.value?.chapter_id)

  // 🔥 修复1：先清除可能存在的旧定时器，再创建新的
  if (readTimer.value) {
    clearInterval(readTimer.value)
    readTimer.value = null
  }

  // 创建新定时器
  readTimer.value = setInterval(() => {
    // 🔥 修复2：组件已卸载则直接返回，不执行任何逻辑
    if (isUnmounted.value) return
    updateReadTimeLong()
  }, 120000)

  //toggleChapterAutoQuiz()
  // 绑定右键事件
  document.addEventListener('contextmenu', handleContextMenu)
  // ✅ 换成新的：
  window.addEventListener('resize', handleWindowResize)

  // 👇 新增：开启缓存定时清理，每 3 分钟执行一次
  cacheClearTimer.value = setInterval(() => {
    if (isUnmounted.value) return
    const current = currentNumber.value
    // 遍历缓存，如果距离当前章节超过 5 章（前后5章），则清理掉释放内存
    for (const [key, value] of chapterCache.value.entries()) {
      if (Math.abs(key - current) >= 5) {
        chapterCache.value.delete(key)
        console.log(`[缓存系统] 定时清理过期内存: 第${key}章`)
      }
    }
  }, 3 * 60 * 1000) // 3分钟

  initBottleTimer()
})



// 3. 监听全局清理信号，执行组件内部的清理（事件监听、弹窗关闭）
watch(
    () => readerStore.emitCleanupSignal,
    (shouldClean) => {
      if (shouldClean) {
        // 移除所有全局事件监听
        document.removeEventListener('contextmenu', handleContextMenu)
        // ✅ 换成新的：
        window.removeEventListener('resize', handleWindowResize)
        document.removeEventListener('mousemove', handleDragMove)
        document.removeEventListener('mouseup', handleDragEnd)
        if (scrollContainerRef.value) {
          scrollContainerRef.value.removeEventListener('scroll', handleScroll)
        }

        // 关闭所有弹窗/面板，避免残留
        showAddNoteDialog.value = false
        isSharePreviewShow.value = false
        isDictShow.value = false
        showAiDialog.value = false
        showImageGenDialog.value = false
        isChapterListShow.value = false
        isBookmarkListShow.value = false
        showCommentPanel.value = false
        graphRef.value?.close()
        showAudiobookPlayer.value = false
        showThrowDialog.value = false
        showReadingDialog.value = false

        // 关闭右键菜单
        if (typeof closeContextMenu === 'function') {
          closeContextMenu()
        }
      }
    }
)
onBeforeUnmount(() => {
  achievementStore.checkAchievements()
  // ===== 清理合约：组件卸载时自动释放所有资源 =====
  isUnmounted.value = true

  // 1. 清理阅读时长定时器
  if (readTimer.value) {
    clearInterval(readTimer.value)
    readTimer.value = null
  }

  // 2. 清理粒子特效定时器
  if (particleTimer.value) {
    clearInterval(particleTimer.value)
    particleTimer.value = null
  }

  // 3. 清理缓存清理定时器
  if (cacheClearTimer.value) {
    clearInterval(cacheClearTimer.value)
    cacheClearTimer.value = null
  }

  // 4. 移除全局事件监听
  document.removeEventListener('contextmenu', handleContextMenu)
  window.removeEventListener('resize', handleWindowResize)
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  if (scrollContainerRef.value) {
    scrollContainerRef.value.removeEventListener('scroll', handleScroll)
  }

  // 5. 上报最后一段阅读时长（异步，不阻塞卸载）
  updateReadTimeLong()
})

// ====================== 新增：路由离开守卫 ======================
onBeforeRouteLeave((to, from, next) => {
  // 离开阅读页时触发同步清理
  isUnmounted.value = true

  // 清理 readTimer
  if (readTimer.value) {
    clearInterval(readTimer.value)
    readTimer.value = null
  }

  // 清理粒子定时器
  if (particleTimer.value) {
    clearInterval(particleTimer.value)
    particleTimer.value = null
  }

  // 移除全局事件监听
  document.removeEventListener('contextmenu', handleContextMenu)
  window.removeEventListener('resize', handleWindowResize)
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  if (scrollContainerRef.value) {
    scrollContainerRef.value.removeEventListener('scroll', handleScroll)
  }

  // 上报阅读时长（异步非阻塞）
  updateReadTimeLong()
  // 上报阅读进度
  updateReadProgress()

  next()
})




</script>

<template>
  <!-- ✅ 进度条：移到容器外面，直接挂在视口上，不被裁剪 -->

  <div ref="scrollContainerRef" class="book-card-container" :style="{ height: 'calc(100vh - 100px)', overflow: 'hidden', padding: '10px 10px', background: 'transparent' }">
    <ElCard class="book-detail-card"
            :class="{ 'ai-loading-border': isAiActive }"
            :style="{ height: '100%', display: 'flex', flexDirection: 'column', position: 'relative' }" shadow="hover">
      <!-- 修复后：阅读进度条（官方标准用法） -->

      <Transition name="toolbar-slide">
        <div v-show="showToolbar" class="immersive-toolbar" :style="{ backgroundColor: isDark ? 'var(--el-bg-color)' : 'var(--el-bg-color)' }">
      <!-- 👇 结尾加上 flex-shrink: 0; -->
      <div class="chapter-header-wrap" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; flex-shrink: 0;">
        <div class="chapter-head-left" style="display: flex; align-items: baseline; gap: 6px; flex: 1;">
          <template v-if="isEditing">
            <span :style="{ color: isDark ? '#d1d5db' : '#333', fontSize: '18px', fontWeight: 'bold' }">第{{ currentNumber }}章-</span>
            <ElInput v-model="editingChapterName" class="chapter-name-input" :style="{ fontSize: '18px', fontWeight: 'bold', width: 'auto', minWidth: '200px', color: isDark ? '#d1d5db' : '#333' }" placeholder="请输入章节名称" />
          </template>
          <template v-else-if="isAdding">
            <span :style="{ color: isDark ? '#d1d5db' : '#333', fontSize: '18px', fontWeight: 'bold' }">第{{ newChapterNumber }}章-</span>
            <ElInput v-model="addingChapterName" class="add-chapter-name-input" :style="{ fontSize: '18px', fontWeight: 'bold', width: 'auto', minWidth: '200px', color: isDark ? '#d1d5db' : '#333' }" placeholder="请输入新章节名称" />
          </template>
          <template v-else>
            <span :style="{ color: isDark ? '#d1d5db' : '#333', fontSize: '18px', fontWeight: 'bold' }">第{{ currentNumber }}章-</span>
            <span :style="{ color: isDark ? '#d1d5db' : '#333', fontSize: '18px', fontWeight: 'bold' }">{{ currentChapter?.name || '暂无章节名称' }}</span>
            <span class="book-name-hide" style="font-size: 12px; color: #999; margin-left: 8px;">({{ bookName }})</span>
          </template>
        </div>

        <div class="chapter-head-right" v-if="!isEditing" style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 12px; color: #999;">{{ formatTime(currentChapter?.create_time) }}</span>
          <ElButton class="auto-width-link-btn" @click="openChapterList" type="text" style="font-size: 12px; color: #1890ff; padding: 0 2px; margin-left: 4px; min-width: auto;">目录</ElButton>

          <div style="display: flex; gap: 6px; border-left: 1px solid #eee; padding-left: 8px; align-items: center;">
            <span style="font-size: 12px; color: #999; margin-right: 4px;">背景：</span>

            <div
                v-for="theme in bgPresets"
                :key="theme.value"
                @click="bgColor = theme.value"
                :title="theme.label"
                :style="{
                  width: '20px',
                  height: '20px',
                  borderRadius: '50%', /* 改成了圆形，看起来更像调色板，不喜欢可以改回 4px */
                  background: isDark ? theme.darkBg : theme.bg,
                  border: bgColor === theme.value ? '2px solid #1890ff' : '1px solid #d9d9d9',
                  cursor: 'pointer',
                  boxSizing: 'border-box'
                }"
            ></div>
          </div>

          <div style="display: flex; gap: 4px; border-left: 1px solid #eee; padding-left: 8px; align-items: center;">
            <span style="font-size: 12px; color: #999; margin-right: 4px;">字体：</span>
            <ElButton @click="setFontSize('small')" type="text" :style="{ fontSize: '12px', color: fontSizeLevel === 'small' ? '#1890ff' : '#999', padding: '0 4px', fontWeight: fontSizeLevel === 'small' ? 'bold' : 'normal', minWidth: '20px' }" class="auto-width-link-btn">小</ElButton>
            <ElButton @click="setFontSize('medium')" type="text" :style="{ fontSize: '12px', color: fontSizeLevel === 'medium' ? '#1890ff' : '#999', padding: '0 4px', fontWeight: fontSizeLevel === 'medium' ? 'bold' : 'normal', minWidth: '20px' }" class="auto-width-link-btn">中</ElButton>
            <ElButton @click="setFontSize('large')" type="text" :style="{ fontSize: '12px', color: fontSizeLevel === 'large' ? '#1890ff' : '#999', padding: '0 4px', fontWeight: fontSizeLevel === 'large' ? 'bold' : 'normal', minWidth: '20px' }" class="auto-width-link-btn">大</ElButton>
          </div>

          <el-dropdown trigger="click" style="margin: 0 10px;">
            <el-button class="glass-button" icon="Menu" circle /> <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="generateAiAtmosphere" :disabled="isGeneratingAtmosphere">
                ✨{{ isGeneratingAtmosphere ? '氛围感应中...' : 'AI 智能沉浸氛围' }}
              </el-dropdown-item>
              <el-dropdown-item @click="openAiChapterSummary">📝AI章节智能总结</el-dropdown-item>
              <el-dropdown-item @click="openAiCharacterAnalyze">👥AI人物关系分析</el-dropdown-item>
              <el-dropdown-item @click="generateQuizFromChapter">📖生成章节题目</el-dropdown-item>
              <el-dropdown-item @click="toggleChapterAutoQuiz">{{ petStore.isAutoQuizRunning && petStore.autoQuizMode === 'ai' ? '⏰停止自动出题' : '⏰开启自动出题' }}</el-dropdown-item>
              <el-dropdown-item @click="handleCaptureScreen">👓识别屏幕内容</el-dropdown-item>
              <el-dropdown-item @click="handleAddBookmark">📌添加书签</el-dropdown-item>
              <el-dropdown-item @click="handleOpenBookmarks">📑查看我的书签</el-dropdown-item>
              <el-dropdown-item v-if="currentUserType === 3" @click="showAudiobookPlayer = true">🎧 打开智能有声书</el-dropdown-item>
            </el-dropdown-menu>
          </template>
          </el-dropdown>

          <ElButton v-if="currentUserType === 3" @click="editChapter" type="text" style="font-size: 12px; color: #1890ff; padding: 0 4px; margin-left: 8px;">编辑</ElButton>
          <ElButton v-if="currentUserType === 3" @click="handleAddChapter" type="text" style="font-size: 12px; color: #1890ff; padding: 0 4px; margin-left: 4px;">添加</ElButton>
        </div>
      </div>
      <el-divider content-position="left" style="margin: 8px 0; border-color: #eee; border-width: 2px"></el-divider>
        </div>
      </Transition>
      <!-- 👇 把 height 换成 flex: 1 -->
      <div ref="contentContainerRef"
           :class="`vibe-${currentWeather}`"
           :style="{
       flex: 1,
       overflow: 'hidden',
       padding: '10px 15px', /* 💡 这里顺便用上了调紧凑后的内边距 */
       height: 0,
       position: 'relative',
       boxSizing: 'border-box',
       background: pageBg,
       borderRadius: '12px',
       boxShadow: isDark ? 'inset 0 4px 10px rgba(0,0,0,0.3)' : 'inset 0 4px 10px rgba(0,0,0,0.06)'
     }"
           @click="handleReadAreaClick"
      >
        <div v-if="isEditing" style="height: 100%; padding: 10px 0; box-sizing: border-box;">
          <ElInput
              v-model="editingChapterContent"
              type="textarea"
              resize="none"
              style="height: 100%; width: 100%;"
              :input-style="{ height: '100%', resize: 'none', fontSize: fontSizeValue, lineHeight: '1.8', color: isDark ? '#e5e7eb' : '#333', background: 'transparent', border: 'none', outline: 'none' }"
              placeholder="请输入章节内容"
          />
        </div>

        <div v-else-if="isAdding" style="height: 100%; padding: 10px 0; box-sizing: border-box;">
          <ElInput
              v-model="addingChapterContent"
              type="textarea"
              resize="none"
              style="height: 100%; width: 100%;"
              :input-style="{ height: '100%', resize: 'none', fontSize: fontSizeValue, lineHeight: '1.8', color: isDark ? '#e5e7eb' : '#333', background: 'transparent', border: 'none', outline: 'none' }"
              placeholder="请输入新章节内容"
          />
        </div>
        <div v-else-if="isAdding" style="padding: 10px 0;">
          <ElInput v-model="addingChapterContent" type="textarea" :autosize="{ minRows: 10, maxRows: Infinity }" :style="{ fontSize: fontSizeValue, lineHeight: '1.8', width: '100%', color: isDark ? '#e5e7eb' : '#333', background: 'transparent', border: '1px solid #e5e7eb' }" placeholder="请输入新章节内容" />
        </div>

        <div v-else-if="currentChapter"
             :style="{ padding: '10px 0', lineHeight: '1.8', fontSize: fontSizeValue, height: '100%', boxSizing: 'border-box' }"
             ref="bookReaderRef" class="book-reader-content"
        >
          <div class="reader-transition-wrapper">
              <div class="book-spread" :key="`${currentIsbn}-${currentNumber}-${currentPage}`" :class="{ 'is-dual': isTwoColumnMode }">

                <div
                    class="reader-page-content left-page"
                    style="position: relative;"
                    :style="[fontSizeStyle, { height: pageHeight + 'px', color: textColor, width: isTwoColumnMode ? 'calc(50% - 30px)' : '100%', backgroundColor: 'transparent' }]"
                >
                  <!-- 🔥 章节标题（每章第一页显示） -->
                  <div v-if="currentPage === 1 && currentChapter?.name" class="chapter-header" :style="{ fontSize: (parseInt(fontSizeValue) + 4) + 'px', color: textColor }">
                    第{{ currentNumber }}章 - {{ currentChapter.name }}
                  </div>
                  <p v-for="para in paginatedContent[currentPage - 1]" :key="para.id" class="paragraph-box"
                     :class="{ 'paragraph-highlight': para.id === currentHighlightParaId }">
                    <span v-html="para.displayHtml"></span>
                    <span class="glass-comment-bubble"  @click.stop="openCommentPanel(para.id)">
                       💬 {{ para.commentCount }}
                    </span>
                  </p>
                </div>

                <div class="book-spine" v-if="isTwoColumnMode"></div>

                <div
                    v-if="isTwoColumnMode"
                    class="reader-page-content right-page"
                    style="position: relative;"
                    :style="[fontSizeStyle, { height: pageHeight + 'px', color: textColor, width: 'calc(50% - 30px)', backgroundColor: 'transparent' }]"
                >
                  <p v-for="para in paginatedContent[currentPage]" :key="para.id" class="paragraph-box"
                     :class="{ 'paragraph-highlight': para.id === currentHighlightParaId }">
                    <span v-html="para.displayHtml"></span>
                    <span class="glass-comment-bubble"   @click.stop="openCommentPanel(para.id)">
                       💬 {{ para.commentCount }}
                    </span>
                  </p>
                </div>

              </div>
          </div>

          <div class="page-indicator-minimal" :style="{ color: isDark ? '#6b7280' : '#9ca3af'}">
            第 {{ isTwoColumnMode && currentPage < totalPages ? `${currentPage}-${currentPage + 1}` : currentPage }} / {{ totalPages }} 页
          </div>

          <!-- 添加笔记弹窗 -->
          <ElDialog v-model="showAddNoteDialog" title="添加笔记" width="500px" :modal="true" :before-close="handleBeforeClose" append-to-body class="note-dialog">
            <div style="padding: 20px; display: flex; flex-direction: column; gap: 20px;">
              <div style="padding: 16px; background: #f7f8fa; border-radius: 16px; min-height: 80px; border: 1px solid #e5e7eb;"><p style="margin: 0; color: #333; line-height: 1.6;">{{ selectedText || '未选中文本' }}</p></div>
              <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px;">
                <span style="color: #666; margin-right: 8px; align-self: center;">选择类型：</span>
                <ElTag v-for="type in noteTypeList" :key="type?.id" :type="selectedNoteType === type.id ? 'success' : 'default'" effect="plain" style="border-radius: 16px; padding: 6px 14px; cursor: pointer; font-size: 14px;" @click="selectedNoteType = type.id">{{ type?.typeName || '未知类型' }}</ElTag>
              </div>
              <ElInput v-model="readerCommentInput" type="textarea" :rows="3" placeholder="写下你的个人批注（可选，最多200字）" style="width: 100%;" maxlength="200" show-word-limit />
              <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <ElButton @click="handleCancel">取消</ElButton>
                <ElButton type="primary" @click="addNoteFromSelection" :disabled="!selectedText || !selectedNoteType">确认添加</ElButton>
              </div>
            </div>
          </ElDialog>
          <!-- 🔥 阅读界面 - 文生图弹窗（复用你的样式规范） -->
          <ElDialog
              v-model="showImageGenDialog"
              title="🎨 AI 文生图"
              width="550px"
              :modal="true"
              :before-close="handleCloseImageDialog"
              append-to-body
              class="image-gen-dialog"
          >
            <div style="padding: 20px; display: flex; flex-direction: column; gap: 16px;">
              <!-- 1. 展示选中的阅读文本（固定展示，不可编辑） -->
              <div style="padding: 16px; background: #f7f8fa; border-radius: 16px; border: 1px solid #e5e7eb;">
                <p style="margin: 0; color: #333; line-height: 1.6; font-size: 14px;">
                  原文内容：{{ rightClickSelectedText || '未选中文本' }}
                </p>
              </div>

              <!-- 2. 自定义提示词输入框（可选，补充描述） -->
              <ElInput
                  v-model="imageGenPrompt"
                  type="textarea"
                  :rows="3"
                  placeholder="可选：输入额外描述（例如：风格：二次元/写实/水墨，画质：高清）"
                  style="width: 100%;"
                  maxlength="200"
                  show-word-limit
              />

              <!-- 3. 生成的图片预览区域 -->
              <div v-if="generatedImageUrl" style="text-align: center; margin-top: 10px;">
                <p style="color: #666; margin-bottom: 8px;">✅ 生成成功</p>
                <el-image
                    :src="generatedImageUrl"
                    :preview-src-list="[generatedImageUrl]"
                    preview
                    preview-teleported
                    fit="contain"
                    alt="AI生成图片"
                    style="max-height: 300px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);"
                />
              </div>

              <!-- 4. 操作按钮 -->
              <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 10px;">
                <ElButton @click="showImageGenDialog = false">取消</ElButton>
                <ElButton
                    type="primary"
                    :loading="isGeneratingImage"
                    @click="generateImageFromReading"
                    :disabled="!rightClickSelectedText"
                >
                  {{ isGeneratingImage ? '生成中...' : '立即生成图片' }}
                </ElButton>
              </div>
            </div>
          </ElDialog>
          <!-- 🔥 AI高级功能弹窗 -->
          <ElDialog
              v-model="showAiDialog"
              :title="aiDialogTitle"
              width="650px"
              append-to-body
              :close-on-click-modal="false"
          >
            <div style="display: flex; flex-direction: column; gap: 16px;">
              <!-- 展示要处理的文本 -->
              <div>
                <label style="font-weight: 600;">处理内容：</label>
                <div
                    style="
          padding: 12px;
          background: #f7f8fa;
          border-radius: 8px;
          max-height: 200px;
          overflow-y: auto;
          margin-top: 8px;
          white-space: pre-wrap;
        "
                >
                  {{ aiTargetContent || '暂无内容' }}
                </div>
              </div>

              <!-- 用户自定义需求输入框 -->
              <ElInput
                  v-model="aiUserPrompt"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入你的自定义需求（可直接使用默认提示）"
                  maxlength="300"
                  show-word-limit
              />
            </div>

            <template #footer>
              <div style="display: flex; justify-content: flex-end; gap: 10px;">
                <ElButton @click="showAiDialog = false">取消</ElButton>
                <ElButton type="primary" @click="handleAiDialogSend">发送给AI</ElButton>
              </div>
            </template>
          </ElDialog>
        </div>
        <div v-else style="text-align: center; padding: 50px; color: #999;">暂无章节内容</div>
      </div>
      <Transition name="panel-slide">
        <div v-if="showCommentPanel" class="glass-comment-panel">
          <div class="panel-header">
            <span class="panel-title">段落想法 ({{ paragraphComments.length }})</span>
            <el-button circle icon="Close" size="small" @click="showCommentPanel = false"></el-button>
          </div>

          <div class="panel-action">
            <el-button type="primary" class="publish-btn" round @click="showAddCommentDialog = true">
              ✍️ 写下你的想法
            </el-button>
          </div>

          <div class="comment-grid">
            <div
                v-for="(comment, index) in paragraphComments"
                :key="comment.id || index"
                class="masonry-item"
            >
              <div class="sticky-note-card">
                <div class="washi-tape"></div>
                <div class="note-header">
                  <span class="note-user">{{ comment.userName || '热心书友' }}</span>
                </div>
                <div class="note-content" >{{ comment.content }}</div>
                <div class="note-footer">{{ formatTime(comment.createTime) }}</div>
              </div>
            </div>
          </div>
          <!-- 空提示移到网格外部，整行居中 -->
          <el-empty
              v-if="paragraphComments.length === 0 && !loadingComments"
              description="还没有人留下想法，快来抢沙发吧~"
              :image-size="80"
              class="empty-full-width"
          />
          <div class="panel-footer" v-if="paragraphComments.length > 0">
            <el-button v-if="hasMoreComments" :loading="loadingComments" text @click="loadMoreComments">
              加载更多...
            </el-button>
            <span v-else class="no-more-text">到底啦，没有更多想法了~</span>
          </div>
        </div>
      </Transition>

      <el-dialog
          v-model="showAddCommentDialog"
          title="✨ 发表你的段落想法"
          width="400px"
          append-to-body
          :show-close="false"
          class="custom-comment-dialog"
      >
        <el-input
            v-model="newCommentContent"
            type="textarea"
            :rows="5"
            placeholder="此刻的你，有什么想说的..."
            maxlength="300"
            show-word-limit
        ></el-input>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="showAddCommentDialog = false" round>取消</el-button>
            <el-button type="primary" @click="submitComment" round>发表</el-button>
          </div>
        </template>
      </el-dialog>
      <div v-if="isEditing" style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; padding: 10px 0;">
        <ElButton @click="handleConfirmEditChapter" type="primary" :style="{ background: isDark ? '#374151' : '#409eff', border: 'none' }">确定</ElButton>
        <ElButton @click="handleCancelEditChapter" type="default" :style="{ border: isDark ? '1px solid #374151' : '1px solid #e5e7eb' }">取消</ElButton>
      </div>
      <div v-if="isAdding" style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; padding: 10px 0;">
        <ElButton @click="handleConfirmAddChapter" type="primary" :style="{ background: isDark ? '#374151' : '#409eff', border: 'none' }">新增</ElButton>
        <ElButton @click="handleCancelAddChapter" type="default" :style="{ border: isDark ? '1px solid #374151' : '1px solid #e5e7eb' }">取消</ElButton>
      </div>
    </ElCard>

    <!-- 朗读悬浮窗 -->
    <div v-if="showReadingDialog" class="reading-float-panel" :class="{ 'dark-mode': isDark, 'minimized': isMinimized }" :style="{ right: rightPos + 'px', bottom: bottomPos + 'px' }">
      <div class="music-vfx-container" v-if="isReading">
        <svg class="music-staff-svg" preserveAspectRatio="none" viewBox="0 0 100 100" overflow="visible">
          <g fill="none" stroke-width="0.3">
            <path d="M 0 100 C -35 70, -25 40, 25 45 C 50 50, 70 15, 90 15 C 115 15, 120 45, 100 45" transform="translate(0, -4)" />
            <path d="M 0 100 C -35 70, -25 40, 25 45 C 50 50, 70 15, 90 15 C 115 15, 120 45, 100 45" transform="translate(0, -2)" />
            <path d="M 0 100 C -35 70, -25 40, 25 45 C 50 50, 70 15, 90 15 C 115 15, 120 45, 100 45" transform="translate(0, 0)" />
            <path d="M 0 100 C -35 70, -25 40, 25 45 C 50 50, 70 15, 90 15 C 115 15, 120 45, 100 45" transform="translate(0, 2)" />
            <path d="M 0 100 C -35 70, -25 40, 25 45 C 50 50, 70 15, 90 15 C 115 15, 120 45, 100 45" transform="translate(0, 4)" />
          </g>
        </svg>

        <div
            v-for="p in musicParticles"
            :key="p.id"
            class="music-particle"
            :style="{
      animationDuration: p.duration + 's',
      animationDelay: p.delay + 's',       /* 🔥 增加延迟，让同批次错开排队 */
      marginTop: p.randomYOffset + '%'
    }"
        >
          {{ p.symbol }}
        </div>
      </div>
      <div class="reading-header" @mousedown="handleDragStart_read">
        <span class="reading-title">正在朗读</span>
        <div class="reading-header-buttons">
          <div class="reading-minimize" @click="isMinimized = !isMinimized">{{ isMinimized ? '🌤️' : '🌥️' }}</div>
          <div class="reading-close" @click="stopReading">☁️</div>
        </div>
      </div>
      <div v-if="!isMinimized" ref="readingTextRef" class="reading-text">
        <Transition name="subtitle-fade" mode="out-in">
          <!-- 绑定 key 强制 Vue 在字幕切换时执行过渡动画 -->
          <span :key="currentChunkIndexForAi" class="dynamic-subtitle">{{ currentSubtitle }}</span>
        </Transition>
      </div>
      <div class="reading-controls">
        <div class="reading-play-btn" @click="togglePauseReading">{{ isPaused ? '▶️' : '⏸️' }}</div>
        <div class="reading-rate-wrapper">
          <select class="reading-rate-select" :value="readingRate" @change="(e) => { readingRate = Number(e.target.value); changeReadingRate(); }">
            <option :value="0.75">0.75x</option><option :value="1.0">1.0x</option><option :value="1.25">1.25x</option><option :value="1.5">1.5x</option><option :value="2.0">2.0x</option><option :value="3.0">3.0x</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 章节列表弹窗 -->
    <ElDialog v-model="isChapterListShow" title="书籍目录" width="500px" :close-on-click-modal="false">
      <div style="max-height: 500px; overflow-y: auto; padding: 0 10px;">
        <div v-for="chapter in currentPageChapters" :key="chapter.chapter_id" :style="{ padding: '12px 16px', borderBottom: '1px solid var(--el-border-color)', cursor: 'pointer', borderRadius: '4px', backgroundColor: chapter.number === currentNumber ? 'rgba(64, 158, 255, 0.1)' : 'transparent' }" @click="jumpToChapter(chapter.number)">
          <span :style="{ fontWeight: chapter.number === currentNumber ? 'bold' : 'normal', color: chapter.number === currentNumber ? '#409eff' : 'var(--el-text-color-primary)' }">第{{ chapter.number }}章 — {{ chapter.name }}</span>
        </div>
      </div>
      <template #footer>
        <div style="display: flex; justify-content: flex-end;">
          <ElPagination @current-change="(val) => chapterPage = val" :current-page="chapterPage" :page-size="chapterPageSize" :total="allChapters.length" layout="prev, pager, next, jumper" style="margin: 0;" />
        </div>
      </template>
    </ElDialog>

    <!-- 🔥 我的书签列表弹窗（已修复为同一行左右对齐） -->
    <ElDialog v-model="isBookmarkListShow" title="我的书签" width="500px" :close-on-click-modal="false" append-to-body>
      <div style="max-height: 500px; overflow-y: auto; padding: 0 10px;">
        <!-- 无书签 -->
        <div v-if="bookmarkList.length === 0" style="text-align: center; padding: 40px; color: #999;">
          暂无书签，右键添加书签吧～
        </div>

        <!-- 书签列表（flex 实现同一行左右对齐） -->
        <div
            v-for="bookmark in bookmarkList"
            :key="bookmark.id"
            :style="{
          padding: '12px 16px',
          borderBottom: '1px solid var(--el-border-color)',
          cursor: 'pointer',
          borderRadius: '4px'
        }"
            @click="jumpToBookmark(bookmark)"
        >
          <!-- ✅ 核心修改：用 flex 实现左右对齐 -->
          <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
            <!-- 左侧：章节+进度 -->
            <span style="font-weight: 600;">
          第{{ getChapterNumber(bookmark.chapterNumber) }}章 · 进度 {{ bookmark.readProgress }}%
        </span>
            <!-- 右侧：添加时间 -->
            <span style="font-size: 12px; color: #999; white-space: nowrap;">
          {{ formatTime(bookmark.createTime) }}
        </span>
          </div>
        </div>
      </div>
    </ElDialog>
    <ElDialog v-model="isSharePreviewShow"
              title="分享文本"
              width="920px"
              :close-on-click-modal="false"
              @close="cancelShare"
              class="share-dialog"
              append-to-body="false"
    >
      <div class="share-card" :style="cardStyle">
        <div v-if="bgType === 'blur'" class="share-blur-bg" :style="{ backgroundImage: `url(${shareBookCover})`, filter: `blur(${blurIntensity}px)` }"></div>
        <div v-if="isDark" class="share-dark-mix-layer"></div>

        <div class="share-cover" v-if="showBookCover && !isMobile">
          <img :src="shareBookCover" alt="书籍封面" style="width: 100%; height: 100%; object-fit: cover;" />
        </div>
        <div class="share-text-wrap">
          <div class="share-text" :style="{ fontSize: `${shareFontSize}px`, color: shareTextColor }">{{ shareText }}</div>
          <div class="share-footer" :style="{ fontSize: `${shareFontSize}px`, color: shareTextColor }">——《{{ shareBookName }}》</div>
        </div>
        <div class="share-glass-highlight"></div>
      </div>

      <template #footer>
        <div class="share-controls">
          <div class="share-control-row">
            <span>背景类型：</span>
            <el-radio-group v-model="bgType" size="small">
              <el-radio-button label="gradient">渐变颜色</el-radio-button>
              <el-radio-button label="blur">封面模糊</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="bgType === 'gradient'" class="share-control-row share-bg-row">
            <span>预设渐变：</span>
            <div class="share-bg-list">
              <div v-for="(g, idx) in gradientPresets" :key="idx" class="share-bg-item" :style="{ backgroundImage: `linear-gradient(135deg, ${g.c1}, ${g.c2})`, border: (color1 === g.c1 && color2 === g.c2) ? '2px solid #409eff' : '1px solid #ddd' }" @click="handleSelectPreset(g)"></div>
            </div>
            <span style="margin-left: 15px;">自定义双色：</span>
            <el-color-picker v-model="color1" size="small" />
            <el-color-picker v-model="color2" size="small" />
          </div>

          <div v-if="bgType === 'blur'" class="share-control-row">
            <span>模糊强度：</span>
            <el-slider v-model="blurIntensity" :min="0" :max="100" style="width: 200px; margin-left: 10px;" />
            <span style="margin-left: 10px; font-size: 13px; color: #666;">{{ blurIntensity }}px</span>
          </div>

          <div class="share-control-row share-bottom-row">
            <div class="share-switch-cover" style="margin-right: 20px;">
              <span>显示封面：</span>
              <el-switch v-model="showBookCover" />
            </div>

            <div class="share-font-size">
              <span>字体大小：</span>
              <ElButton size="small" @click="decreaseFontSize" :disabled="shareFontSize <= 8">-</ElButton>
              <span class="share-font-value">{{ shareFontSize }}</span>
              <ElButton size="small" @click="increaseFontSize" :disabled="shareFontSize >= 24">+</ElButton>
              <span style="margin-left: 20px;">文字颜色：</span>
              <el-color-picker v-model="shareTextColor" size="small" />
            </div>
            <div class="share-buttons" style="margin-left: auto;">
              <ElButton @click="cancelShare">取消</ElButton>
              <ElButton type="primary" @click="downloadShareImage">下载图片</ElButton>
            </div>
          </div>
        </div>
      </template>
    </ElDialog>

    <div
        ref="shareCardRef"
        class="share-card"
        v-show="isSharePreviewShow"
        :style="{
        position: 'fixed',
        top: '-9999px',
        left: '-9999px', /* 移得更远一点 */
        width: '800px',  /* 🔥 核心1：强制固定宽度，排版绝不再受窗口大小影响！ */
        maxWidth: 'none',/* 突破原本的 max-width 限制 */
        zIndex: '-1',
        ...cardStyle
      }"
    >
      <div v-if="bgType === 'blur'" class="share-blur-bg" :style="{ backgroundImage: `url(${shareBookCover})`, filter: `blur(${blurIntensity}px)` }"></div>
      <div v-if="isDark" class="share-dark-mix-layer"></div>

      <div class="share-cover" v-if="showBookCover && !isMobile">
        <img :src="shareBookCover" alt="书籍封面" style="width: 100%; height: 100%; object-fit: cover;" />
      </div>
      <div class="share-text-wrap">
        <div class="share-text" :style="{ fontSize: `${shareFontSize}px`, color: shareTextColor }">{{ shareText }}</div>
        <div class="share-footer" :style="{ fontSize: `${shareFontSize}px`, color: shareTextColor}">——《{{ shareBookName }}》</div>
      </div>
      <div class="share-glass-highlight"></div>
    </div>

    <!-- 词典弹窗 -->
    <div v-if="isDictShow" class="dict-popup" :style="isMobile ? { left: '50%', top: '50%', transform: 'translate(-50%, -50%)' } : { left: `${dictPosition.x}px`, top: `${dictPosition.y}px`, transform: 'translate(-50%, -100%)' }" @click.stop>
      <div class="dict-arrow" v-if="!isMobile"></div>
      <div class="dict-content">
        <div class="dict-header"><span class="dict-word">{{ dictWord }}</span><span v-if="dictPinyin" class="dict-pinyin">{{ dictPinyin }}</span></div>
        <div class="dict-meanings"><div v-for="(meaning, index) in dictMeanings" :key="index" class="dict-meaning-item">{{ index + 1 }}. {{ meaning }}</div></div>
      </div>
    </div>
    <div v-if="isDictShow" class="dict-mask" @click="closeDict"></div>

    <!-- 🔥 右键菜单（和个人界面样式/交互完全一致） -->
    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <!-- 1. 删除该笔记（仅右键命中笔记时显示） -->
      <div v-if="rightClickNoteId" class="context-menu-item" @click="handleDeleteNote">
        <span>🗑️</span>
        <span>删除该笔记</span>
      </div>

      <div v-if="rightClickSelectedText" class="context-menu-item" @click="sendAiMsg('这是来自书籍' + bookName +'的第' + currentNumber + '章节的文字内容:' + rightClickSelectedText)">
        <span>😽</span>
        <span>向AI提问</span>
      </div>
      <!-- 🔥 新增：AI高级功能菜单 -->
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="openAiTextRewrite">
        <span>✍️</span>
        <span>AI文字仿写&润色</span>
      </div>
      <!-- 🔥 新增：AI文生图（选中文本时显示） -->
      <div
          v-if="rightClickSelectedText" class="context-menu-item" @click="openImageGenDialog"
      >
        <span>🎨</span>
        <span>AI文生图</span>
      </div>

      <!-- 🔥 🔥 新增：AI生成题目功能组（和其他AI功能保持一致风格） -->
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="generateQuizFromSelection">
        <span>📚</span>
        <span>生成选段题目</span>
      </div>
      <!-- 2. 添加笔记（仅选中文本且无笔记时显示） -->
      <div v-if="rightClickSelectedText && !rightClickNoteId && !isTextHasNote(rightClickSelectedText)" class="context-menu-item" @click="handleOpenAddNote">
        <span>📝</span>
        <span>添加笔记</span>
      </div>

      <!-- 3. 选中文本通用功能 -->
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="handleStartReading">
        <span>🔊</span>
        <span>从选中位置朗读</span>
      </div>
      <div class="context-menu-item" @click="handleFullChapterRead">
        <span>📖</span>
        <span>全章朗读</span>
      </div>
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="handleCopySelectedText">
        <span>📄</span>
        <span>复制文本</span>
      </div>
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="handleShareToForum">
        <span>📤</span>
        <span>分享选段到论坛</span>
      </div>
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="handleOpenSharePreview">
        <span>🖼️</span>
        <span>生成分享图片</span>
      </div>
      <div v-if="rightClickSelectedText" class="context-menu-item" @click="handleQueryWord">
        <span>🔍</span>
        <span>查询词语</span>
      </div>
    </div>
  </div>

  <!-- ========== 知识图谱面板（独立组件） ========== -->
  <KnowledgeGraph
    ref="graphRef"
    :isbn="currentIsbn"
    :user-id="currentUserId"
    :book-name="bookName"
    v-model:panel-width="graphPanelWidth"
    :is-dark="isDark"
  />
  <!-- ========== 有声书播放器 ========== -->
  <AudiobookPlayer
    v-model:visible="showAudiobookPlayer"
    :isbn="currentIsbn"
    :chapter-number="currentNumber"
    :book-name="bookName"
    :is-dark="isDark"
  />

  <Teleport to="body">
    <div v-if="settingsStore.widgets.showBottle && isCurrentPage" class="ocean-bottle-system" :class="{ 'dark-mode': isDark }">

      <div
          class="main-bottle-wrapper glass-panel"
          :class="{
            'glow-yellow': hasNewBottle,
            'is-half-hidden': bottleRight < 0,
            'is-dragging': isDragging_bottle
          }"
          :style="{ bottom: bottleBottom + 'px', right: bottleRight + 'px', transform: `rotate(${bottleTilt}deg)` }"
          @mousedown.left.stop="startDragBottle"
          @touchstart.stop="handleBottleTouchStart"
          @touchmove.stop="handleBottleTouchMove"
          @touchend.stop="handleBottleTouchEnd"
          @touchcancel.stop="handleBottleTouchEnd"
          @click.stop="handleBottleClick"
          title="长按拖拽"
      >
        <canvas ref="rippleCanvas" class="ripple-canvas"></canvas>
        <span class="bottle-icon">🍾</span>
        <div class="bottle-hint"></div>
      </div>

      <transition-group name="bubble-fade" tag="div">
        <div
            v-for="bubble in floatingBubbles"
            :key="bubble._feId"
            class="cat-bubble glass-panel"
            :class="{
          'is-drifting': !bubble.isOpen && !bubble.isLeaving,
          'is-opened': bubble.isOpen,
          'is-leaving': bubble.isLeaving
        }"
            @click.stop="openBubble(bubble)"
        >
          <div class="bubble-inner">
          <div v-if="!bubble.isOpen" class="bubble-emoji">{{ bubble.emoji }}</div>

          <div v-else class="bottle-note-ui" @click.stop>
            <div class="note-header">
              <span class="note-user">来自: {{ bubble.username}}({{bubble.userid}})</span>
              <span class="note-close" @click.stop="closeBubble(bubble)">✖</span>
            </div>
            <div class="note-content">{{ bubble.content }}</div>
            <div class="note-footer" v-if="bubble.allowReply === 1">
              <el-input
                  v-model="bubble.replyText"
                  placeholder="写下你的回复..."
                  size="small"
                  class="reply-input"
                  @keyup.enter="replyToBubble(bubble)"
              />
              <el-button type="primary" size="small" @click.stop="replyToBubble(bubble)" plain>回传</el-button>
            </div>
            <div class="note-footer" v-else>
              <span class="read-only-hint">主人设置了不接收回复哦~</span>
            </div>
          </div>
          </div>
        </div>
      </transition-group>

      <el-dialog
          v-model="showThrowDialog"
          title="📜 写下你的漂流寄语"
          width="380px"
          custom-class="throw-bottle-dialog glass-panel"
          :show-close="false"
          destroy-on-close
          append-to-body
      >
        <div id="bottle-capture-target">
          <el-input
              type="textarea"
              v-model="newBottleContent"
              rows="5"
              placeholder="在这个时空，留下你想说的话..."
              resize="none"
          />
        </div>
        <template #footer>
          <!-- 外层flex容器，占满整行高度垂直居中 -->
          <div style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
            <!-- 左侧：复选框 -->
            <el-checkbox v-model="allowReply">允许捞取者回复我</el-checkbox>
            <!-- 右侧：两个按钮打包一组 -->
            <div>
              <el-button @click="showThrowDialog = false">取消</el-button>
              <el-button type="primary" @click="throwBottleIntoSea" :loading="isThrowing">发表</el-button>
            </div>
          </div>
        </template>
      </el-dialog>
    </div>
  </Teleport>
</template>

<style scoped>
/* 右键菜单样式 */
.context-menu {
  min-width: 140px;
}
:deep(.el-dropdown-menu__item) {
  padding: 8px 16px;
  font-size: 14px;
}

/* ====================== 🔥 iOS Safari 专属优化 ====================== */
* {
  -webkit-touch-callout: none;
  -webkit-tap-highlight-color: transparent;
}
.book-reader-content {
  -webkit-user-select: text !important;
  user-select: text !important;
}
input, textarea, [contenteditable="true"] {
  -webkit-touch-callout: default !important;
  -webkit-user-select: text !important;
  user-select: text !important;
}

.chapter-btn-wrap {
  position: relative;
  z-index: 999;
}
:deep(.el-button) {
  pointer-events: auto !important;
}
:deep(.el-card__body) {
  padding: 20px !important;
  height: 100%;
  /* 👇 新增下面三行，把卡片内部变成 Flex 布局 */
  display: flex !important;
  flex-direction: column !important;
  box-sizing: border-box;
}
:deep(.el-button.is-disabled) {
  background: #ccc !important;
  color: #fff !important;
  cursor: not-allowed;
}
:deep(.book-card-container::-webkit-scrollbar),
:deep(.el-card__body::-webkit-scrollbar) {
  width: 8px;
}
:deep(.book-card-container::-webkit-scrollbar-track),
:deep(.el-card__body::-webkit-scrollbar-track) {
  background: #e9ecef;
}
:deep(.book-card-container::-webkit-scrollbar-thumb),
:deep(.el-card__body::-webkit-scrollbar-thumb) {
  background: #ced4da;
  border-radius: 4px;
}
:deep(.book-card-container::-webkit-scrollbar-thumb:hover),
:deep(.el-card__body::-webkit-scrollbar-thumb:hover) {
  background: #adb5bd;
}

/* 🔥 恢复输入框的外观，使其符合直觉，并适配暗黑模式 */
:deep(.add-chapter-name-input .el-input__wrapper) {
  background: var(--el-fill-color-light) !important;
  box-shadow: 0 0 0 1px var(--el-border-color) inset !important;
  padding: 0 11px !important;
  border-radius: 4px !important;
  transition: all 0.2s;
}

/* 获取焦点时高亮（蓝色边框） */
:deep(.chapter-name-input .el-input__wrapper.is-focus),
:deep(.add-chapter-name-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
  background: transparent !important;
}

:deep(.el-textarea__wrapper) {
  background: transparent !important;
  border-radius: 4px !important;
  box-shadow: none !important;
}
:deep(.el-button--text) {
  border: none !important;
  outline: none !important;
}
:deep(.el-button--text:hover) {
  color: #1890ff !important;
  background: transparent !important;
}
:deep(.el-dropdown-menu) {
  padding: 4px 0;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}
:deep(.el-dropdown-menu__item:hover) {
  background-color: #f5f7fa;
}

/* 词典弹窗样式 */
/* 词典弹窗样式 (替换原有 .dict-popup) */
.dict-popup {
  position: fixed;
  /* 换成玻璃变量 */
  background: var(--glass-bg);
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: 12px;
  z-index: 9999;
  width: clamp(280px, 90vw, 300px);
  max-height: 60vh;
  overflow-y: auto;
}

/* 移除词典弹窗那个实心的小三角形，因为纯色三角形在玻璃面板上会显得突兀 */
.dict-arrow {
  display: none;
}
.dict-content {
  padding: 16px;
}
.dict-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}
.dict-word {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}
.dict-pinyin {
  font-size: 14px;
  color: #666;
}
.dict-meanings {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.dict-meaning-item {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  white-space: normal;
  word-break: break-all;
}
.dict-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9998;
}

/* 笔记弹窗 */
.note-dialog {
  --el-dialog-border-radius: 20px;
  --el-dialog-body-padding: 0;
}
.note-dialog .el-dialog__header {
  border-radius: 20px 20px 0 0;
  background: #f7f8fa;
  padding: 16px 20px;
}
.note-dialog .el-dialog__title {
  font-weight: 60;
  color: #333;
}
.note-dialog .el-dialog__body {
  padding: 0 !important;
}

/* 朗读悬浮窗 - 仅外层保留完整毛玻璃，子元素移除独立背景/模糊 */
.reading-float-panel {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 360px;
  /* 只在外层统一设置磨砂玻璃 */
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1);
  border-radius: 16px;
  overflow: visible;
  transition: all 0.3s ease;
  z-index: 9999;
  user-select: none;
}
.reading-float-panel.dark-mode {
  background: rgba(30, 30, 30, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.4);
  overflow: visible;
}

/* 头部：删除独立background、backdrop-filter、box-shadow，仅保留浅分割线 */
.reading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08); /* 透明浅分割线，不破坏玻璃 */
  cursor: move;
}
.reading-float-panel.dark-mode .reading-header {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}
.reading-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.reading-float-panel.dark-mode .reading-title {
  color: #e5e7eb;
}
.reading-header-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.reading-minimize, .reading-close {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}
.reading-minimize:hover, .reading-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}
.reading-float-panel.dark-mode .reading-minimize, .reading-float-panel.dark-mode .reading-close {
  color: #9ca3af;
}
.reading-float-panel.dark-mode .reading-minimize:hover, .reading-float-panel.dark-mode .reading-close:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e5e7eb;
}

.reading-text {
  padding: 16px;
  min-height: 80px;
  max-height: 120px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.6;
  color: #555;
}
.reading-float-panel.dark-mode .reading-text {
  color: #d1d5db;
}

/* 底部控制栏：删除独立background、backdrop-filter、box-shadow */
.reading-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}
.reading-float-panel.dark-mode .reading-controls {
  border-top-color: rgba(255, 255, 255, 0.1);
}

.reading-play-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.2s;
}
.reading-play-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
}
.reading-rate-select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  background: rgba(255,255,255,0.3);
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
}
.reading-float-panel.dark-mode .reading-rate-select {
  background: rgba(0,0,0,0.2);
  border-color: #4a5568;
  color: #e5e7eb;
}
.reading-float-panel.minimized {
  width: 220px;
  height: auto;
  overflow: visible;
}
.reading-float-panel.minimized .reading-text {
  display: none;
}

/* 分享弹窗 */
.share-dialog {
  --el-dialog-width: 95vw;
  --el-dialog-max-width: 920px;
}
/* 修改原有的 .share-card，确保内部元素具备相对定位层级 */
.share-card {
  width: 100%;
  max-width: 800px;
  min-height: 400px;
  margin: 0 auto;
  padding: 40px;
  box-sizing: border-box;
  border-radius: 24px;
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 30px;
  align-items: flex-start;
  background-size: cover;
  background-position: center;
}

/* 🆕 核心：高斯模糊专用背景层 */
.share-blur-bg {
  position: absolute;
  top: -10%; /* 稍微溢出并放大，防止高斯模糊边缘产生白边白影 */
  left: -10%;
  width: 120%;
  height: 120%;
  background-size: cover;
  background-position: center;
  z-index: 0;
  transform: scale(1.05);
  will-change: filter;
}

/* 🆕 暗黑模式下，自定义色彩调和层 */
.share-dark-mix-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(26, 32, 44, 0.45); /* 覆盖一层半透明深色，使亮色渐变在暗黑下契合度极高 */
  z-index: 1;
  pointer-events: none;
}

/* 确保所有的文字与封面容器都在背景层和暗黑层上方 */
.share-cover,
.share-text-wrap,
.share-glass-highlight {
  position: relative;
  z-index: 2;
}

.share-cover {
  width: 200px;
  height: 320px;
  flex-shrink: 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.share-text-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: auto;
}
.share-text {
  line-height: 1.8;
  text-align: justify;
  word-break: break-word;
}
.share-footer {
  text-align: right;
  font-style: italic;
  margin-top: 15px;
}
.share-glass-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0) 100%);
  pointer-events: none;
}
.share-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}
.share-control-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.share-bg-list {
  display: flex;
  gap: 6px;
}
.share-bg-item {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
}
.share-font-size {
  display: flex;
  align-items: center;
  gap: 8px;
}
.share-font-value {
  width: 30px;
  text-align: center;
}
.share-buttons {
  display: flex;
  gap: 10px;
}
.share-disabled-tip {
  font-size: 12px;
  color: #999;
}

/* 手机端适配 */
@media (max-width: 768px) {
  .share-card {
    flex-direction: column;
    padding: 20px;
    min-height: auto;
    gap: 20px;
  }
  .share-text-wrap {
    width: 100%;
  }
  .share-controls {
    gap: 12px;
    padding-top: 10px;
  }
  .share-bg-row {
    width: 100%;
    justify-content: center;
  }
  .share-bottom-row {
    width: 100%;
    justify-content: space-between;
  }
  .share-buttons {
    justify-content: flex-end;
  }
  .book-detail-card {
    margin: 0 !important;
    width: 100% !important;
    border-radius: 0 !important;
  }
  .book-detail-card :deep(.el-card__body) {
    padding: 12px !important;
  }
  .chapter-header-wrap {
    flex-direction: column !important;
    align-items: flex-start !important;
    gap: 8px !important;
  }
  .chapter-head-left {
    width: 100% !important;
    flex: none !important;
  }
  .book-name-hide {
    display: none !important;
  }
  .chapter-head-right {
    width: 100% !important;
    flex-wrap: wrap !important;
    align-items: center !important;
    gap: 4px !important;
    row-gap: 4px !important;
    justify-content: flex-start !important;
  }
}
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


/* 深度修改：进度条填充动画 */
:deep(.el-progress-bar__inner) {
  transition: width 0.2s ease !important;
}

/* ====================== 7. 右键菜单样式 ====================== */
/* ====================== 7. 右键菜单样式 (🔥 全新磨砂玻璃版) ====================== */
.context-menu {
  position: fixed;
  z-index: 99999 !important;
  /* 替换掉原来的实色 #fff，使用全局毛玻璃变量 */
  background: var(--glass-bg);
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: 8px;
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

/* 玻璃面板上的悬浮效果，用半透明遮罩代替实色 */
.context-menu-item:hover {
  background-color: rgba(0, 0, 0, 0.06);
}
/* 暗黑模式适配 */
:deep(.dark-mode .context-menu) {
  /* 暗黑模式下直接沿用全局暗色玻璃变量 */
  background: var(--glass-bg);
  border-color: var(--glass-border);
  color: #e5e7eb;
}
:deep(.dark-mode .context-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
}

/* 文生图弹窗样式 */
.image-gen-dialog .el-dialog__body {
  padding: 10px 20px;
}

/* 🔥 新增：暗黑模式全局平滑过渡 */
.book-detail-card {
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.book-reader-content {
  transition: color 0.3s ease;
}

/* ✨ 左下角极简页码指示器 */
.page-indicator-minimal {
  position: absolute;
  bottom: 12px;
  left: 20px;
  font-size: 12px;
  letter-spacing: 1px;
  pointer-events: none; /* 绝对不能阻挡用户的鼠标事件 */
  user-select: none;
  opacity: 0.8;
  transition: opacity 0.3s ease, color 0.3s ease;
}

/* 提示用户点击区域的鼠标指针 */
.book-reader-content {
  cursor: text; /* 默认是文本指针 */
}

/* ====================== 🔥 双栏排版（微信读书PC端同款） ====================== */

/* ====================== 🔥 真实的双页排版与装订线 ====================== */
.book-spread {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
  transform-style: preserve-3d;
  will-change: transform, opacity;
}

/* 真实的中间装订线阴影 (夹在两页中间，不阻挡交互) */
.book-spine {
  position: absolute;
  left: calc(50% - 30px);
  top: -30px;    /* ✨ 向上拉伸抵消 30px 内边距 */
  bottom: -30px; /* ✨ 向下拉伸抵消 30px 内边距 */
  width: 60px;
  background: linear-gradient(to right,
  rgba(0,0,0,0) 0%,
  rgba(0,0,0,0.02) 20%,
  rgba(0,0,0,0.08) 50%,
  rgba(0,0,0,0.02) 80%,
  rgba(0,0,0,0) 100%
  );
  pointer-events: none; /* 绝对不能挡住鼠标！ */
  z-index: 5;
}

/* 暗黑模式下的装订线 */
html.dark .book-spine, :root.dark .book-spine {
  background: linear-gradient(to right,
  rgba(0,0,0,0) 0%,
  rgba(0,0,0,0.2) 30%,
  rgba(0,0,0,0.4) 50%,
  rgba(0,0,0,0.2) 70%,
  rgba(0,0,0,0) 100%
  );
}

/* ====================== 🔥 终极 3D 仿真翻书动画 (番茄小说同款障眼法) ====================== */
.reader-transition-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  /* 1. 降低透视点 (原2500px -> 1200px)，增强 3D 纵深感，让纸张翻起时更贴近屏幕 */
  perspective: 1200px;
}

.book-spread:not(.is-dual).book-flip-backward-leave-to {
  transform-origin: right center;
  transform: rotateY(90deg);
  opacity: 0;
}

.glass-button {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  backdrop-filter: blur(5px);
  color: inherit;
}

/* ====================== 🔥 阅读区天气沉浸光影特效 ====================== */
.vibe-sunny::after,
.vibe-cloudy::after,
.vibe-rain::after,
.vibe-night::after,
.vibe-thunder::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none; /* 🔑 极其重要：确保光影层绝对不阻挡底层的文字选词、右键菜单和点击翻页 */
  z-index: 50;          /* 浮在所有阅读排版和页码工具的上方 */
  border-radius: inherit; /* 🔑 核心：自动继承容器的 12px 圆角，防止光影溢出卡片 */
  transition: all 1s ease-in-out; /* 切换天气时，光影如流水般缓慢过渡 */
}

/* ☀️ 晴天：四周泛起一层极淡的微醺金色暖光 */
.vibe-sunny::after {
  box-shadow: inset 0 0 80px rgba(255, 180, 50, 0.06);
}

/* ☁️ 阴天：轻微的冷灰色调压暗边缘，有种浓雾笼罩的胶片质感 */
.vibe-cloudy::after {
  box-shadow: inset 0 0 100px rgba(120, 130, 140, 0.12);
}

/* 🌧️ 雨天：湿冷的深邃蓝色晕染纸张四周，渲染一丝忧郁和孤独 */
.vibe-rain::after {
  box-shadow: inset 0 0 120px rgba(40, 100, 255, 0.1);
  background: radial-gradient(circle, transparent 65%, rgba(40, 80, 200, 0.04) 100%);
}

/* 🌙 夜晚：深度暗角滤镜，让用户的视觉焦距自然聚拢在屏幕中央的文字上 */
.vibe-night::after {
  box-shadow: inset 0 0 150px rgba(0, 0, 0, 0.45);
}

/* 🌩️ 雷暴：压抑的深色暗角 + 间歇性爆发的惨白闪电，动感十足 */
.vibe-thunder::after {
  box-shadow: inset 0 0 150px rgba(0, 0, 0, 0.35);
  animation: flash-lightning 7s infinite;
}

/* ⚡ 闪电帧动画 */
@keyframes flash-lightning {
  0%, 93%, 100% { background: transparent; }
  94% { background: rgba(255, 255, 255, 0.12); }
  95% { background: transparent; }
  97% { background: rgba(255, 255, 255, 0.22); }
}

/* ====================== 🔥 AI 运行时 - 圆点顺时针流动效果 ====================== */
.ai-loading-border {
  position: relative;
  border-radius: 12px;
  /* 必须开启，否则边缘的圆点会被卡片裁剪消失 */
  overflow: visible !important;
}

/* 流动的发光圆点 */
.ai-loading-border::before {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  /* 双层发光光晕，更有科技感 */
  box-shadow: 0 0 8px #409eff, 0 0 16px rgba(64, 158, 255, 0.7);
  z-index: 20;
  pointer-events: none;
  /* 4秒跑完一圈，匀速循环 */
  animation: dot-border-flow 4s linear infinite;
}

/* 顺时针路径：左上角 → 右上角 → 右下角 → 左下角 → 回到左上角 */
@keyframes dot-border-flow {
  0% {
    top: -4px;
    left: -4px;
  }
  25% {
    top: -4px;
    left: calc(100% - 4px);
  }
  50% {
    top: calc(100% - 4px);
    left: calc(100% - 4px);
  }
  75% {
    top: calc(100% - 4px);
    left: -4px;
  }
  100% {
    top: -4px;
    left: -4px;
  }
}

/* 暗黑模式适配：圆点提亮，光晕增强 */
html.dark .ai-loading-border::before,
:root.dark .ai-loading-border::before {
  background: #66b1ff;
  box-shadow: 0 0 10px #66b1ff, 0 0 20px rgba(102, 177, 255, 0.9);
}

/* ====================== 🔥 AI 运行时 - 跟随圆点的拖尾线条 ====================== */
/* 增加一个 ::after 来作为圆点拖出的线条（彗星拖尾） */
.ai-loading-border::after {
  content: '';
  position: absolute;
  /* 稍微外扩，让 2px 的线条刚好和 8px 的圆点居中对齐 */
  inset: -1px;
  border-radius: 12px;
  pointer-events: none;
  z-index: 10;

  /* 使用 4 个纯色线性渐变来分别充当上下左右四条边的线条 */
  background-image:
      linear-gradient(#409eff, #409eff), /* 上边 */
      linear-gradient(#409eff, #409eff), /* 右边 */
      linear-gradient(#409eff, #409eff), /* 下边 */
      linear-gradient(#409eff, #409eff); /* 左边 */
  background-repeat: no-repeat;

  /* 线条动画，4s 必须和你的圆点动画时间完全吻合！ */
  animation: tail-border-flow 4s linear infinite;
}

/* 核心魔法：四条边轮流伸展和缩回，形成完美的拖尾错觉 */
@keyframes tail-border-flow {
  /* --- 0% -> 25%: 圆点走上边。上边生长，左边缩回 --- */
  0% {
    background-position: 0 0, 100% 0, 100% 100%, 0 0;
    background-size: 0 2px, 0 0, 0 0, 2px 100%;
  }
  24.99% {
    background-position: 0 0, 100% 0, 100% 100%, 0 0;
    background-size: 100% 2px, 0 0, 0 0, 2px 0;
  }

  /* --- 25% -> 50%: 圆点走右边。右边生长，上边缩回 --- */
  25% {
    background-position: 100% 0, 100% 0, 100% 100%, 0 100%;
    background-size: 100% 2px, 0 0, 0 0, 0 0;
  }
  49.99% {
    background-position: 100% 0, 100% 0, 100% 100%, 0 100%;
    background-size: 0 2px, 2px 100%, 0 0, 0 0;
  }

  /* --- 50% -> 75%: 圆点走下边。下边生长，右边缩回 --- */
  50% {
    background-position: 0 0, 100% 100%, 100% 100%, 0 100%;
    background-size: 0 0, 2px 100%, 0 2px, 0 0;
  }
  74.99% {
    background-position: 0 0, 100% 100%, 100% 100%, 0 100%;
    background-size: 0 0, 2px 0, 100% 2px, 0 0;
  }

  /* --- 75% -> 100%: 圆点走左边。左边生长，下边缩回 --- */
  75% {
    background-position: 0 0, 100% 0, 0 100%, 0 100%;
    background-size: 0 0, 0 0, 100% 2px, 0 2px;
  }
  99.99% {
    background-position: 0 0, 100% 0, 0 100%, 0 100%;
    background-size: 0 0, 0 0, 0 2px, 2px 100%;
  }
  100% {
    background-position: 0 0, 100% 0, 100% 100%, 0 0;
    background-size: 0 2px, 0 0, 0 0, 2px 100%;
  }
}

/* 暗黑模式适配：线条提亮 */
html.dark .ai-loading-border::after,
:root.dark .ai-loading-border::after {
  background-image:
      linear-gradient(#66b1ff, #66b1ff),
      linear-gradient(#66b1ff, #66b1ff),
      linear-gradient(#66b1ff, #66b1ff),
      linear-gradient(#66b1ff, #66b1ff);
}

/* ====================== 🔥 沉浸模式工具栏样式 ====================== */
.immersive-toolbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  /* 填补卡片的 padding 空间，让它覆盖在最上方 */
  padding: 20px 20px 0 20px;
  /* 加一点轻微的阴影区分开阅读底色 */
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  border-radius: 4px 4px 0 0;
}

/* 手机端同步适配 padding */
@media (max-width: 768px) {
  .immersive-toolbar {
    padding: 12px 12px 0 12px;
  }
}

html.dark .immersive-toolbar, :root.dark .immersive-toolbar {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

/* 工具栏滑入滑出动画 */
.toolbar-slide-enter-active,
.toolbar-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), opacity 0.3s ease;
}
.toolbar-slide-enter-from,
.toolbar-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* ====================== 🔥 段落与段评气泡样式 ====================== */
/* 🔥 章节标题 */
.chapter-header {
  text-align: left;
  font-weight: 700;
  padding: 8px 0 20px 0;
  margin-bottom: 4px;
  letter-spacing: 1px;
  opacity: 0.85;
}
.paragraph-box {
  text-indent: 2em; /* 首行缩进直接由段落接管 */
  margin: 0;        /* 必须为 0，高度已在 JS 精确计算 */
  position: relative;
  text-align: justify;
}

.glass-comment-bubble {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-family: Arial, sans-serif;
  cursor: pointer;
  user-select: none;
  vertical-align: baseline; /* 保持跟文字基线对齐 */

  /* ✨ 核心：完美磨砂玻璃参数，与你的项目 UI 对齐 */
  background: var(--glass-bg, rgba(255, 255, 255, 0.4));
  backdrop-filter: blur(8px) saturate(120%);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
  border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.4));
  color: #409eff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  text-indent: 0; /* 强制取消从父元素继承的缩进 */
}

/* 气泡暗黑模式适配 */
html.dark .glass-comment-bubble,
:root.dark .glass-comment-bubble {
  background: rgba(60, 60, 60, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
  color: #66b1ff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.glass-comment-bubble:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: rgba(64, 158, 255, 0.15); /* Hover时透一点品牌蓝 */
  border-color: rgba(64, 158, 255, 0.3);
}

/* ====================== 🔥 动态字幕动画 ====================== */
.dynamic-subtitle {
  display: inline-block;
  font-size: 16px;
  line-height: 1.8;
  color: #1890ff; /* 品牌高亮蓝 */
  font-weight: 500;
  text-align: justify;
}
html.dark .dynamic-subtitle, :root.dark .dynamic-subtitle {
  color: #66b1ff;
}

/* 进出场的渐变效果 */
.subtitle-fade-enter-active,
.subtitle-fade-leave-active {
  transition: all 0.3s ease;
}
.subtitle-fade-enter-from {
  opacity: 0;
  transform: translateY(5px);
}
.subtitle-fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

/* ====================== 🔥 段评毛玻璃面板样式 ====================== */
/* 动画：从右侧平滑滑入 */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.4s cubic-bezier(0.25, 0.8, 0.25, 1), opacity 0.4s ease;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 侧边面板基础容器（绝对定位覆盖在右侧） */
.glass-comment-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 450px; /* 面板宽度 */
  z-index: 2000;
  display: flex;
  flex-direction: column;

  /* ✨ 核心毛玻璃效果 */
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-left: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.05);
}

/* 暗黑模式毛玻璃适配 */
html.dark .glass-comment-panel, :root.dark .glass-comment-panel {
  background: rgba(30, 30, 30, 0.7);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.3);
}

.panel-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-action {
  padding: 16px 20px 0;
  display: flex;
  justify-content: flex-end;
}

.publish-btn {
  width: 35%;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* ====================== 🔥 便签墙 Grid 布局 ====================== */
.comment-grid {
  flex: 1;
  padding: 24px 20px;
  overflow-y: auto;
  /* ✨ 一行两个，间距16px */
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 16px;
  align-content: start;
}

.panel-footer {
  padding: 12px;
  text-align: center;
  border-top: 1px solid rgba(0,0,0,0.05);
}

.no-more-text {
  font-size: 12px;
  color: #999;
}

/* ====================== 🔥 你提供的便签交替样式 + 补充完善 ====================== */
/* 修复：给每个便签加上相对定位和基础结构 */
.sticky-note-card {
  position: relative;
  padding: 24px 16px 16px;
  border: 1px solid;
  border-radius: 4px;
  box-shadow: 2px 4px 8px rgba(0,0,0,0.06);
  min-height: 120px;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.sticky-note-card:hover {
  box-shadow: 4px 8px 16px rgba(0,0,0,0.1);
  z-index: 10;
}

/* 纸胶带补充样式：绝对定位到便签上方中间 */
.washi-tape {
  position: absolute;
  top: -10px;
  left: 50%;
  width: 60px;
  height: 20px;
  opacity: 0.8;
  z-index: 2;
}

.note-header {
  font-size: 12px;
  font-weight: bold;
  color: #555;
  margin-bottom: 8px;
}

.note-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  flex: 1;
  word-break: break-all;
  text-align: justify;
}

.note-footer {
  font-size: 11px;
  color: #888;
  text-align: right;
  margin-top: 12px;
}

/* 引入你提供的交替循环逻辑 */
.masonry-item:nth-child(4n+1) .sticky-note-card { background-color: #f3faf4 !important; border-color: #cfead4 !important; transform: rotate(0.4deg); }
.masonry-item:nth-child(4n+2) .sticky-note-card { background-color: #fffdeb !important; border-color: #ebdca5 !important; transform: rotate(-0.8deg); }
.masonry-item:nth-child(4n+3) .sticky-note-card { background-color: #edf7f5 !important; border-color: #cceee2 !important; transform: rotate(1.2deg); }
.masonry-item:nth-child(4n+4) .sticky-note-card { background-color: #fffdf0 !important; border-color: #e3d69c !important; transform: rotate(-1.2deg); }

.masonry-item:nth-child(4n+1) .washi-tape { background-color: rgba(168, 218, 220, 0.5); transform: translateX(-50%) rotate(-4deg); }
.masonry-item:nth-child(4n+2) .washi-tape { background-color: rgba(241, 166, 166, 0.5); transform: translateX(-50%) rotate(4deg); }
.masonry-item:nth-child(4n+3) .washi-tape { background-color: rgba(233, 196, 106, 0.5); transform: translateX(-50%) rotate(-2deg); }
.masonry-item:nth-child(4n+4) .washi-tape { background-color: rgba(138, 201, 143, 0.5); transform: translateX(-50%) rotate(5deg); }

/* ====================== 🌊 海洋瓶系统容器 ====================== */
.ocean-bottle-system {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  pointer-events: none; /* 让鼠标穿透容器，不影响阅读 */
  z-index: 9999;
}

/* ====================== 🍾 主漂流瓶 (拖拽实体) ====================== */
.main-bottle-wrapper {
  position: absolute;
  width: 60px; height: 60px;
  border-radius: 50%;
  pointer-events: auto;
  cursor: grab;
  display: flex; flex-direction: column; align-items: center; justify-content: center;

  /* 🔥 加入 right/bottom 的丝滑阻尼过渡，和天气面板对齐 */
  transition: right 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
  bottom 0.5s cubic-bezier(0.34, 1.56, 0.64, 1),
  box-shadow 0.3s,
  transform 0.1s linear;

  animation: infinity-float 6s ease-in-out infinite;

  /* 极致毛玻璃 */
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

  /* 🔥 移动端极其重要：防止原生左右滑动手势切断拖拽 */
  touch-action: none;
}
.main-bottle-wrapper:active {
  cursor: grabbing;
}

/* 拖拽时：强制掐断补间动画防止卡顿，鼠标变为抓取 */
.main-bottle-wrapper.is-dragging {
  transition: transform 0.1s linear !important;
  cursor: grabbing;
}

/* 边缘半隐藏状态 */
.main-bottle-wrapper.is-half-hidden {
  opacity: 0.6;
}

/* 边缘悬停微交互：探头暗示 */
.main-bottle-wrapper.is-half-hidden:hover {
  opacity: 1;
  transform: translateX(-5px) rotate(0deg) !important;
}
.bottle-icon { font-size: 28px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2)); }
.bottle-hint { font-size: 10px; color: #fff; margin-top: -5px; opacity: 0.8; font-weight: bold;}

/* 发光提醒 */
.glow-yellow {
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.8), inset 0 0 15px rgba(255, 215, 0, 0.4) !important;
  border-color: rgba(255, 215, 0, 0.8) !important;
}

/* Canvas 水波纹画布置于最底层 */
.ripple-canvas {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  border-radius: 50%;
  pointer-events: none;
  z-index: -1;
}

/* ====================== 😺 猫咪气泡 (便签) ====================== */
.cat-bubble {
  position: absolute;
  bottom: 80px; left: 80px; /* 默认停留位置 */
  pointer-events: auto;
  cursor: pointer;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex; align-items: center; justify-content: center;
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); /* Q弹展开过渡 */
}

/* 状态一：外层负责从右向左的水平漂移出场 */
.is-drifting {
  width: 50px; height: 50px;

  /* ✨ 核心魔法：同时挂载横向移动和垂直水流两个动画 */
  animation:
      float-to-left 15s linear forwards,           /* 横向：匀速向左飘 15 秒 */
      waterFlowMacro 4s ease-in-out infinite;      /* 纵向：海浪上下起伏，4秒一个循环 */

  /* 开启 GPU 加速，把会变的属性都告诉浏览器 */
  will-change: left, transform, opacity, margin-bottom;
}

/* ✨ 新增：内层容器，全宽高占满 */
.bubble-inner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  /* ✨ 开启 GPU 加速，优化波纹摇摆性能 */
  will-change: transform;
}

/* 仅在漂流状态时触发内部晃动，气泡点开时自然静止，防止影响阅读 */
.is-drifting .bubble-inner {
  animation: waveSway 3.5s ease-in-out infinite;
  transform-origin: center bottom;
}

.bubble-emoji { font-size: 28px; }

/* 状态二：点击展开变成便签 */
.is-opened {
  width: 280px; height: auto; min-height: 150px;
  border-radius: 12px;
  bottom: 50%; left: 50%;
  transform: translate(-50%, 50%); /* 居中显示 */
  cursor: default;
  padding: 15px;
  flex-direction: column; align-items: flex-start; justify-content: space-between;
  background: rgba(255, 255, 255, 0.6);
}

/* 状态三：折叠并滚落屏幕外 */
.is-leaving {
  width: 50px; height: 50px;
  border-radius: 50%;
  animation: roll-away 1s ease-in forwards;
}

/* === 便签内部 UI === */
.bottle-note-ui { width: 100%; height: 100%; display: flex; flex-direction: column; gap: 10px; }
.note-header { display: flex; justify-content: space-between; align-items: center; width: 100%; border-bottom: 1px dashed rgba(0,0,0,0.1); padding-bottom: 5px; }
.note-user { font-size: 12px; color: #666; font-weight: bold; }
.note-close { cursor: pointer; color: #999; font-size: 14px; transition: color 0.3s; }
.note-close:hover { color: #ff4d4f; }
.note-content { flex: 1; font-size: 14px; color: #333; line-height: 1.6; text-align: justify; padding: 5px 0; }
.note-footer { display: flex; gap: 10px; justify-content: flex-end; width: 100%; align-items: center; }
.reply-input :deep(.el-input__inner) { background: rgba(255, 255, 255, 0.5) !important; border: none; }
.read-only-hint { font-size: 12px; color: #999; width: 100%; text-align: center; }

/* ====================== ☄️ 碎星入海粒子动画 (动态挂载至 Body) ====================== */
:global(.shatter-particle) {
  position: fixed;
  width: 6px; height: 6px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 10000;
  animation: shatter-fall 1s cubic-bezier(0.25, 1, 0.5, 1) forwards;
  box-shadow: 0 0 8px currentColor;
}

/* ====================== 🎬 关键帧动画库 ====================== */
@keyframes infinity-float {
  0%, 100% { margin-top: 0; }
  50% { margin-top: -15px; }
}

@keyframes float-to-left {
  0% { left: 90vw; opacity: 0; transform: scale(0.5); }
  5% { opacity: 1; transform: scale(1); }
  100% { left: 20px; opacity: 1; transform: scale(1); }
}

/* 动画 B：✨ 新增的大水流波动（利用 margin 完美避开 transform 冲突） */
@keyframes waterFlowMacro {
  0%, 100% { margin-bottom: 0; }
  50% { margin-bottom: 25px; } /* 浪头最高处，气泡被托起 25px */
}

@keyframes roll-away {
  0% { transform: translate(-50%, 50%) rotate(0); opacity: 1; }
  100% { transform: translate(-50vw, 150%) rotate(-360deg); opacity: 0; }
}

@keyframes shatter-fall {
  0% { transform: translate(0, 0) scale(1); opacity: 1; }
  100% { transform: translate(var(--tx), var(--ty)) scale(0.1); opacity: 0; }
}

/* ✨ 内层水波摇摆动画：上下 Q弹起伏 + 微小角度的旋转晃动 */
@keyframes waveSway {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-12px) rotate(-3deg); }
}

/* === 暗黑模式适配 === */
.dark-mode .main-bottle-wrapper, .dark-mode .cat-bubble {
  background: rgba(30, 30, 30, 0.4);
  border-color: rgba(255, 255, 255, 0.1);
}
.dark-mode .is-opened { background: rgba(40, 40, 40, 0.8); }
.dark-mode .note-user { color: #ccc; }
.dark-mode .note-content { color: #e5e7eb; }
.dark-mode .note-close:hover { color: #ff7875; }



/* ======================================
   🌑 手账便签：暗夜星核专属色盘适配
======================================== */
/* 1. 便签卡片底色（使用你的深色色盘） */
html.dark .masonry-item:nth-child(4n+1) .sticky-note-card {
  background-color: #1D2D46 !important; /* 迷雾海王（深蓝） */
  border-color: #2b3d5c !important; /* 边缘微亮，模拟纸张厚度 */
}
html.dark .masonry-item:nth-child(4n+2) .sticky-note-card {
  background-color: #2F3A37 !important; /* 苍绿寂灭（深灰绿） */
  border-color: #3e4c49 !important;
}
html.dark .masonry-item:nth-child(4n+3) .sticky-note-card {
  background-color: #262E30 !important; /* 灰烬余火（深灰） */
  border-color: #364144 !important;
}
html.dark .masonry-item:nth-child(4n+4) .sticky-note-card {
  background-color: #2E3B3E !important; /* 铁灰堡垒（灰蓝绿） */
  border-color: #405155 !important;
}

/* 2. 纸胶带深色适配（神来之笔：用白天的薄荷色和鎏金做半透明胶带） */
html.dark .masonry-item:nth-child(4n+1) .washi-tape {
  background-color: rgba(235, 181, 92, 0.45) !important; /* 鎏金 #EBB55C 透光版 */
}
html.dark .masonry-item:nth-child(4n+2) .washi-tape {
  background-color: rgba(160, 187, 177, 0.35) !important; /* 白天主题色 #A0BBB1 透光版 */
}
html.dark .masonry-item:nth-child(4n+3) .washi-tape {
  background-color: rgba(220, 239, 233, 0.25) !important; /* 白天主题色 #DCEFE9 透光版 */
}
html.dark .masonry-item:nth-child(4n+4) .washi-tape {
  background-color: rgba(173, 198, 176, 0.35) !important; /* 白天主题色 #ADC6B0 透光版 */
}
html.dark .mention-user-item:hover,
html.dark .mention-user-item.active {
  background-color: #374151;
}
html.dark .mention-text { color: #60a5fa; }

/* ======================================
   🌑 修复：手账便签内部文字暗黑模式颜色
======================================== */
/* 修复：用户名 */
html.dark .note-header .note-user {
  color: #e5e7eb !important; /* 亮白色 */
}

/* 修复：便签正文 */
html.dark .note-content {
  color: #d1d5db !important; /* 浅银白色 */
}

/* 修复：便签底部时间 */
html.dark .note-footer {
  color: #9ca3af !important; /* 偏暗的灰色，拉开层级 */
}

/* ======================================
   🎵 朗读弹窗五线谱景深特效 (跨越边界版)
======================================== */
.music-vfx-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  /* 🔥 核心魔法：允许超出弹窗边界显示 */
  overflow: visible !important;
  /* 新增：允许上下左右全部溢出 */
  clip-path: none !important;
}
/* SVG 五线谱铺满容器，加个微微呼吸感 */
.music-staff-svg {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: visible !important;
  animation: staffBreathe 6s infinite alternate ease-in-out;
}

/* 🔥 核心：白天模式下用柔和的淡墨色 */
.music-staff-svg g {
  stroke: rgba(0, 0, 0, 0.15);
}

/* 🔥 核心：暗黑模式下用发光的淡白色 */
:deep(.dark-mode) .music-staff-svg g {
  stroke: rgba(255, 255, 255, 0.2);
}

@keyframes staffBreathe {
  0% { opacity: 0.6; transform: translateY(0); }
  100% { opacity: 0.9; transform: translateY(-3px); }
}

/* 粒子初始设为透明，并在起点待命 */
.music-particle {
  position: absolute;
  font-size: 22px;
  opacity: 0;
  left: 0;
  top: 100%;
  text-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  animation-name: flyAlongStaff;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
  will-change: transform, opacity;
  transform: translateZ(1px);
}

:deep(.dark-mode) .music-particle {
  text-shadow: 0 0 15px rgba(255, 255, 255, 0.3); /* 夜晚光晕亮一点 */
}

/* 🔥 核心动画：完全吸附 SVG 的跨界轨道，并用大小模糊做 3D 景深 */
/* 🔥 核心动画：完全吸附 SVG 的跨界轨道，并用大小模糊做 3D 景深 */
@keyframes flyAlongStaff {
  0% {
    /* 起点：左下角 */
    left: 0%; top: 100%;
    transform: scale(0.2) rotate(-20deg);
    opacity: 0;
  }
  15% {
    /* 💥 飞出弹窗左侧边界！ */
    left: -20%; top: 60%;
    transform: scale(1.2) rotate(10deg);
    opacity: 0.8;
  }
  30% {
    /* 钻回弹窗内，到达左侧分割线 */
    left: 25%; top: 45%;
    transform: scale(2.2) rotate(25deg);
    opacity: 1;
    filter: blur(0px);
  }
  50% {
    /* 潜入弹窗背面悄悄游走 */
    left: 55%; top: 30%;
    transform: scale(0.6) rotate(-10deg);
    opacity: 0.15;
    filter: blur(4px);
  }
  70% {
    /* 🔥 新的完美圆弧顶点 */
    left: 90%; top: 15%;
    transform: scale(1.2) rotate(10deg);
    opacity: 0.8;
    filter: blur(0px);
  }
  85% {
    /* 💥 从圆弧顺滑飞出右侧弹窗边界！ */
    left: 115%; top: 25%;
    transform: scale(1.6) rotate(30deg);
    opacity: 0.9;
    filter: blur(0px);
  }
  100% {
    /* 🔥 收尾：刚好停在弹窗最右侧边缘 (100%)，不再进入内部 */
    left: 100%; top: 45%;
    transform: scale(0) rotate(60deg);
    opacity: 0;
    filter: blur(2px);
  }
}

/* ======================================
   🔥 终极解封：打破弹窗和毛玻璃的物理边界
======================================== */
/* 使用 :global 追踪被 append-to-body 传送到外层的弹窗，强行解除隐藏 */
:global(.reading-dialog),
:global(.reading-dialog.glass-panel),
:global(.reading-dialog .el-dialog__body) {
  overflow: visible !important;
}

/* 确保内部承载特效的直接父级也解除限制 */
.reading-dialog-body {
  overflow: visible !important;
}
/* 朗读段落高亮 */
.paragraph-highlight {
  background-color: rgba(64, 158, 255, 0.12);
  border-radius: 6px;
  padding: 4px 2px;
  margin: 0 -2px;
  transition: background-color 0.3s ease;
}

html.dark .paragraph-highlight,
:root.dark .paragraph-highlight {
  background-color: rgba(102, 177, 255, 0.18);
}
</style>
