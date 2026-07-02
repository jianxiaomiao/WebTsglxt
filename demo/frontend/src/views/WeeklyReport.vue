<script setup>
// ====================== 1. 依赖导入 ======================
import { computed, inject, onMounted, ref, watch, nextTick,onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request.js'
import {
  ElTabs, ElTabPane, ElSkeleton, ElSkeletonItem, ElRow, ElCol,
  ElStatistic, ElCard, ElTooltip, ElImage, ElScrollbar, ElTag,
  ElDivider, ElEmpty, ElMessage, ElDialog, ElDatePicker, ElButton
} from 'element-plus'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import { storeToRefs } from 'pinia'
import { ChatDotRound, Document, UserFilled, ChatLineRound, Star, Collection, Reading, ArrowLeft } from '@element-plus/icons-vue'

// ====================== 2. 全局实例初始化 ======================
const userStore = useUserStore()
const currentUserId = inject("currentUserId")
const isDark = inject("isDark")
const route = useRoute()
const router = useRouter()

const gotoBookDetail = inject('gotoBookDetail')
const parseBookLinkToCard = inject('parseBookLinkToCard')
const generateBookCommentShareLinkById = inject('generateBookCommentShareLinkById')
const generateUserCommentShareLinkById = inject('generateUserCommentShareLinkById')
const generateUserShareLinkById = inject('generateUserShareLinkById')

const updateAllBookCards = inject('updateAllBookCards')

// 直接从全局store获取日签数据
const petStore = usePetStore()
const { dailyGreeting } = storeToRefs(petStore)
// ====================== 3. 响应式变量定义 ======================
// 统一大写：DAY/WEEK/MONTH，默认显示WEEK
const activeTab = ref('WEEK')
const loading = ref(true)
const reportData = ref(null)
const levelColors = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']

// ✅ 历史报告弹窗控制变量（完全重构）
const dialogVisible = ref(false)       // 弹窗显示/隐藏
const selectedDate = ref('')           // 最终传给后端的格式化日期（数据库格式）
const rawDateValue = ref(null)         // 组件返回的原生Date对象
const dialogLoading = ref(false)       // 弹窗确认按钮加载状态
const currentReportDate = ref('')      // 当前报告的date，作为弹窗默认值
const datePickerRef = ref(null)        // 日期选择器实例引用


// ====================== 气泡动画 配置参数 ======================
const MAX_BUBBLE_COUNT = 15        // 最大气泡数
const MIN_INIT_COUNT = 6           // 首次最少气泡
const MAX_INIT_COUNT = 8           // 首次最多气泡
const AUTO_GENERATE_INTERVAL = 4000 // 新气泡生成间隔
const BUBBLE_MIN_SPEED = 0.25      // 最小速度
const BUBBLE_MAX_SPEED = 0.8       // 最大速度（柔和慢速）
const BOUNCE_PROBABILITY = 0.6     // 反弹概率
const MAX_BUBBLE_WIDTH = '75%'     // 气泡最大宽度

// 气泡动画变量
const bubbleContainer = ref(null)
const bubbles = ref([])
let animationFrameId = null
let generateTimer = null
let containerWidth = 0
let containerHeight = 0

// ====================== 4. 计算属性 ======================
// ✅ 新增：历史查询按钮文案（根据当前标签动态变化）
const historyButtonText = computed(() => {
  const textMap = {
    'DAY': '查询历史日报',
    'WEEK': '查询历史周报',
    'MONTH': '查询历史月报'
  }
  return textMap[activeTab.value] || '查询历史报告'
})

// ✅ 新增：日期选择器类型（周报改用普通date选择器）
const datePickerType = computed(() => {
  // 日报/周报都用普通日期选择器，仅月报用month类型
  return activeTab.value === 'MONTH' ? 'month' : 'date'
})

// ✅ 新增：弹窗标题（给周报加使用提示）
const dialogTitle = computed(() => {
  const titleMap = {
    'DAY': '选择历史日报',
    'WEEK': '选择历史周报（选择任意一天查看当周）',
    'MONTH': '选择历史月报'
  }
  return titleMap[activeTab.value] || '选择历史报告'
})

// 格式化总阅读时长
const formatTotalDuration = computed(() => {
  if (!reportData.value) return { hours: 0, minutes: 0 }
  const totalSeconds = reportData.value.stats.totalReadSecond || 0
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  return { hours, minutes }
})

// 计算总互动数
const totalInteraction = computed(() => {
  if (!reportData.value) return 0
  const stats = reportData.value.stats
  return stats.sendMsgCount + stats.receiveMsgCount + stats.aiChatCount +
      stats.likeCount + stats.bookCommentCount + stats.userCommentCount +
      stats.friendCount + stats.sentRequestCount + stats.receivedRequestCount
})

// 热力图数据（彻底修复周报结构+日期匹配）
const heatmapData = computed(() => {
  if (!reportData.value || activeTab.value === 'DAY') return []

  const dataMap = new Map()
  reportData.value.stats.dailyReadList.forEach(r => {
    // 修复日期格式匹配：截取后端readDate的纯日期部分
    const dateKey = r.readDate.split(' ')[0]
    dataMap.set(dateKey, r.readDuration || 0)
  })

  const weeks = []
  if (activeTab.value === 'WEEK') {
    // 🔥 周报修正：1行7列，weeks数组只有1个元素，每个week数组长度为7
    const startDate = new Date(reportData.value.stats.startTime)
    const week = [] // 先创建一个week数组，存放7天数据
    for (let i = 0; i < 7; i++) {
      const currentDate = new Date(startDate)
      currentDate.setDate(startDate.getDate() + i)
      // 手动拼接yyyy-MM-dd格式，避免UTC时区问题
      const year = currentDate.getFullYear()
      const month = String(currentDate.getMonth() + 1).padStart(2, '0')
      const day = String(currentDate.getDate()).padStart(2, '0')
      const dateStr = `${year}-${month}-${day}`
      const duration = dataMap.get(dateStr) || 0
      week.push({
        date: dateStr,
        duration,
        level: getDurationLevel(duration),
        isCurrentMonth: true
      })
    }
    weeks.push(week) // 最后把长度为7的week数组push到weeks里
  } else if (activeTab.value === 'MONTH') {
    // 月报逻辑保持不变（5行7列，完全正确）
    const startDate = new Date(reportData.value.stats.startTime)
    const endDate = new Date(reportData.value.stats.endTime)
    const daysInMonth = endDate.getDate()

    const firstDayOfMonth = new Date(startDate.getFullYear(), startDate.getMonth(), 1)
    const firstDayWeekday = firstDayOfMonth.getDay()
    const adjustedFirstDayWeekday = firstDayWeekday === 0 ? 7 : firstDayWeekday
    const totalWeeks = Math.ceil((adjustedFirstDayWeekday - 1 + daysInMonth) / 7)

    let currentDate = new Date(firstDayOfMonth)
    currentDate.setDate(currentDate.getDate() - (adjustedFirstDayWeekday - 1))

    for (let weekIndex = 0; weekIndex < totalWeeks; weekIndex++) {
      const week = []
      for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
        const year = currentDate.getFullYear()
        const month = String(currentDate.getMonth() + 1).padStart(2, '0')
        const day = String(currentDate.getDate()).padStart(2, '0')
        const dateStr = `${year}-${month}-${day}`
        const isCurrentMonth = currentDate.getMonth() === startDate.getMonth()
        const duration = isCurrentMonth ? (dataMap.get(dateStr) || 0) : 0
        week.push({
          date: dateStr,
          duration,
          level: isCurrentMonth ? getDurationLevel(duration) : 0,
          isCurrentMonth
        })
        currentDate.setDate(currentDate.getDate() + 1)
      }
      weeks.push(week)
    }
  }

  return weeks
})

// 🔥 修正循环范围：周报/月报统一循环0-6，因为每个week数组都是7个元素
const dayIndexRange = computed(() => {
  return [0,1,2,3,4,5,6]
})

// 阅读书籍墙数据（使用后端返回的书籍统计列表）
const bookWallData = computed(() => {
  if (!reportData.value) return []
  return reportData.value.stats.bookReadStatsList.slice(0, 12)
})

// 🔥 所有阅读书籍排行数据（不限数量，支持全部展示）
const allRankedBooks = computed(() => {
  if (!reportData.value) return []
  return reportData.value.stats.bookReadStatsList || []
})

// ====================== 新增：消息互动列表（无轮播，平铺展示） ======================
const messageList = computed(() => {
  if (!reportData.value) return []
  const stats = reportData.value.stats
  const list = []

  // AI 聊天记录 → 来源：AI助手
  stats.aiChatList?.forEach(item => {
    list.push({ from: '豆包', content: item.messageContent })
  })
  // 收到的消息 → 来源：对方用户ID
  stats.receiveMsgList?.forEach(item => {
    if (!item.isRecalled) list.push({ from: item.fromUserId, content: item.messageContent })
  })
  // 发送的消息 → 来源：我
  stats.sendMsgList?.forEach(item => {
    if (!item.isRecalled) list.push({ from: '🐱', content: item.messageContent })
  })

  return list
})
// ====================== 5. 通用工具函数 ======================
const getDurationLevel = (seconds) => {
  if (seconds === 0) return 0
  const minutes = seconds / 60
  if (minutes < 30) return 1
  if (minutes < 60) return 2
  if (minutes < 120) return 3
  return 4
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN')
}
/**
 * ✅ 核心：根据报告类型，将Date对象格式化为数据库需要的字符串
 * @param {Date} date 原生Date对象
 * @param {string} type 报告类型 DAY/WEEK/MONTH
 * @returns {string} 格式化后的日期字符串
 */
const formatDateForDatabase = (date, type) => {
  if (!date) return ''

  // 先创建一个新的Date对象，避免修改原对象
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')

  switch (type) {
    case 'DAY':
      // 日报：直接返回选中的日期
      return `${year}-${month}-${day}`
    case 'WEEK':
      // ✅ 周报：计算选中日期所在周的周一（周一为一周第一天）
      const dayOfWeek = d.getDay() // 0=周日,1=周一,...,6=周六
      // 计算到周一的天数差
      const diffToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
      // 得到周一的日期
      const monday = new Date(d)
      monday.setDate(d.getDate() + diffToMonday)
      // 格式化为yyyy-MM-dd
      const mondayYear = monday.getFullYear()
      const mondayMonth = String(monday.getMonth() + 1).padStart(2, '0')
      const mondayDay = String(monday.getDate()).padStart(2, '0')
      const result = `${mondayYear}-${mondayMonth}-${mondayDay}`
      console.log(`周报日期转换：选中${year}-${month}-${day} → 周一${result}`)
      return result
    case 'MONTH':
      // 月报：返回yyyy-MM
      return `${year}-${month}`
    default:
      return ''
  }
}

/**
 * ✅ 将数据库格式的日期字符串解析为Date对象（用于设置默认值）
 */
const parseDatabaseDate = (dateStr, type) => {
  if (!dateStr) return null

  try {
    // 先截取纯日期部分，忽略时间（兼容后端返回的 2026-05-25 00:00:00 格式）
    const pureDateStr = dateStr.split(' ')[0]

    if (type === 'MONTH') {
      // 月报：yyyy-MM → 解析为当月1号
      const [year, month] = pureDateStr.split('-')
      return new Date(parseInt(year), parseInt(month) - 1, 1)
    } else {
      // 日报/周报：yyyy-MM-dd → 直接解析
      return new Date(pureDateStr)
    }
  } catch (e) {
    console.error('日期解析失败', e)
    return null
  }
}

// ✅ 日期选择器change事件：手动格式化日期
const handleDateChange = (date) => {
  rawDateValue.value = date
  selectedDate.value = formatDateForDatabase(date, activeTab.value)
  console.log('日期选择器值变化：', {
    原生Date: date,
    格式化后: selectedDate.value,
    类型: activeTab.value
  })
}

// ✅ 新增：根据报告类型和date格式化报告标题
const formatReportTitle = (type, date) => {
  if (!date) return ''
  const dateObj = new Date(date)

  switch (type) {
    case 'DAY':
      return `${dateObj.getFullYear()}年${String(dateObj.getMonth()+1).padStart(2,'0')}月${String(dateObj.getDate()).padStart(2,'0')}日报告（日报）`
    case 'WEEK':
      const weekRange = getWeekRange(date)
      const weekNum = getWeekNumber(date)
      return `${dateObj.getFullYear()}年第${weekNum}周报告（${weekRange}）`
    case 'MONTH':
      return `${dateObj.getFullYear()}年${String(dateObj.getMonth()+1).padStart(2,'0')}月报告（月报）`
    default:
      return ''
  }
}

// ✅ 修复：根据周一日期计算周区间（格式：2026/5/4-2026/5/10）
const getWeekRange = (mondayDate) => {
  const monday = new Date(mondayDate)
  // 直接+6天，得到周日日期，确保周一→周日
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)

  const formatDate = (d) => `${d.getFullYear()}/${d.getMonth()+1}/${d.getDate()}`
  return `${formatDate(monday)}-${formatDate(sunday)}`
}
// ✅ 修复：周报日期转换，确保返回当周周一的日期（完全匹配数据库）
const getWeekMonday = (dateInput) => {
  let d
  // 兼容两种输入：Date对象 或 字符串（2026-05-25 或 2026-05-25 00:00:00）
  if (typeof dateInput === 'string') {
    const pureDate = dateInput.split(' ')[0] // 先截取纯日期部分
    d = new Date(pureDate)
  } else {
    d = new Date(dateInput)
  }

  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1) // 周日→周一，其他正常计算
  const monday = new Date(d.setDate(diff))

  // 格式化为yyyy-MM-dd
  const year = monday.getFullYear()
  const month = String(monday.getMonth() + 1).padStart(2, '0')
  const dayStr = String(monday.getDate()).padStart(2, '0')
  return `${year}-${month}-${dayStr}`
}

// ✅ 修复：加载指定日期的历史报告
const fetchReportByDate = async () => {
  if (!currentUserId.value || !selectedDate.value) {
    ElMessage.warning('请先选择报告日期')
    return
  }

  dialogLoading.value = true
  try {
    const res = await request.get('/user/report', {
      params: {
        userId: currentUserId.value,
        type: activeTab.value,
        date: selectedDate.value // 已经是正确的数据库格式
      }
    })

    if (res.code === 200) {
      reportData.value = res.data
      currentReportDate.value = selectedDate.value // 更新当前报告日期
      dialogVisible.value = false // 关闭弹窗
      ElMessage.success('历史报告加载成功')

      // 重新初始化气泡动画
      clearAnimation()
      nextTick(() => {
        setTimeout(() => {
          updateAllBookCards()
          initBubbleAnimation()
        }, 600)
      })
    } else {
      ElMessage.error(res.msg || '加载历史报告失败')
    }
  } catch (e) {
    console.error('加载历史报告异常', e)
    ElMessage.error('加载历史报告异常，请稍后重试')
  } finally {
    dialogLoading.value = false
  }
}
// ====================== 🔥 新增：气泡文字截断工具函数 ======================
/**
 * 气泡内容截断：正文截断、书籍卡片标签完整保留
 * @param originStr 原始消息文本
 * @returns 处理后文本（超长自动...）
 */
const getCutBubbleText = (originStr) => {
  if (!originStr) return ''
  // 🔥 自定义最大显示字符数（建议50~80，数字越小气泡越短）
  const MAX_TEXT_COUNT = 100
  // 匹配书籍卡片标签 [{book:xxx}]，不截断卡片
  const cardReg = /\[\{\w+:\d+\}\]/g
  const cardList = originStr.match(cardReg) || []

  // 临时替换卡片，避免截断破坏标签
  let tempContent = originStr.replace(cardReg, '||CARD||')
  // 文字超长截断
  if (tempContent.length > MAX_TEXT_COUNT) {
    tempContent = tempContent.slice(0, MAX_TEXT_COUNT) + '...'
  }
  // 还原卡片标签
  cardList.forEach(card => {
    tempContent = tempContent.replace('||CARD||', card)
  })
  return tempContent
}

// ====================== 气泡动画 核心函数（已整合截断） ======================
// 创建气泡（使用你的真实消息 + 文字截断）
const createBubble = (isFromEdge = false) => {
  const container = bubbleContainer.value
  if (!container || messageList.value.length === 0) return null

  const msg = messageList.value[Math.floor(Math.random() * messageList.value.length)]
  // 🔥 核心修改1：获取原始文字后，立即截断！
  const rawText = `${msg.content}`
  const text = getCutBubbleText(rawText) // 截断后的文字

  // 动态计算气泡尺寸（兼容卡片 + 截断后文字）
  const tempDiv = document.createElement('div')
  tempDiv.style.cssText = `
  position: absolute;
  visibility: hidden;
  white-space: pre-wrap;
  max-width: ${MAX_BUBBLE_WIDTH};
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 13px;
  line-height: 1.6;
  box-sizing: border-box;
`
  // 🔥 用截断后的文字解析卡片 + 计算尺寸
  tempDiv.innerHTML = parseBookLinkToCard(text)
  container.appendChild(tempDiv)
  const w = tempDiv.offsetWidth
  const h = tempDiv.offsetHeight
  container.removeChild(tempDiv)

  let x, y, vx, vy
  if (isFromEdge) {
    // 从左右边界进入
    const left = Math.random() > 0.5
    x = left ? -w : containerWidth
    y = Math.random() * (containerHeight - h)
    vx = (left ? 1 : -1) * (BUBBLE_MIN_SPEED + Math.random() * (BUBBLE_MAX_SPEED - BUBBLE_MIN_SPEED))
    vy = (Math.random() - 0.5) * (BUBBLE_MIN_SPEED + Math.random() * (BUBBLE_MAX_SPEED - BUBBLE_MIN_SPEED))
  } else {
    // 初始随机位置
    x = Math.random() * (containerWidth - w)
    y = Math.random() * (containerHeight - h)
    vx = (Math.random() - 0.5) * 2 * (BUBBLE_MIN_SPEED + Math.random() * (BUBBLE_MAX_SPEED - BUBBLE_MIN_SPEED))
    vy = (Math.random() - 0.5) * 2 * (BUBBLE_MIN_SPEED + Math.random() * (BUBBLE_MAX_SPEED - BUBBLE_MIN_SPEED))
  }

  return { id: Date.now() + Math.random(), text, x, y, vx, vy, width: w, height: h, bounce: Math.random() < BOUNCE_PROBABILITY }
}

// 初始化气泡
const initBubbleAnimation = () => {
  if (!bubbleContainer.value || messageList.value.length === 0) return
  // 清空旧数据
  bubbles.value = []
  containerWidth = bubbleContainer.value.offsetWidth
  containerHeight = bubbleContainer.value.offsetHeight
  // 随机初始化气泡
  const count = MIN_INIT_COUNT + Math.floor(Math.random() * (MAX_INIT_COUNT - MIN_INIT_COUNT + 1))
  for (let i = 0; i < count; i++) {
    const b = createBubble(false)
    if (b) bubbles.value.push(b)
  }
  animate()
  startAutoGenerate()
}

// 动画循环（TS语法兼容版）
const animate = () => {
  if (!bubbleContainer.value) return

  // 过滤气泡，合法TS写法
  bubbles.value = bubbles.value.filter(b => {
    // 更新位置
    b.x += b.vx
    b.y += b.vy

    // 左右边界处理
    if (b.x <= 0 || b.x + b.width >= containerWidth) {
      if (b.bounce) {
        // 反弹：反转水平速度 + 修正位置，避免卡边界
        b.vx *= -1
        b.x = Math.max(0, Math.min(containerWidth - b.width, b.x))
      } else {
        // 流出：完全超出容器则移除气泡
        if (b.x + b.width < 0 || b.x > containerWidth) {
          return false
        }
      }
    }

    // 上下边界处理
    if (b.y <= 0 || b.y + b.height >= containerHeight) {
      if (b.bounce) {
        // 反弹：反转垂直速度 + 修正位置
        b.vy *= -1
        b.y = Math.max(0, Math.min(containerHeight - b.height, b.y))
      } else {
        // 流出：完全超出容器则移除气泡
        if (b.y + b.height < 0 || b.y > containerHeight) {
          return false
        }
      }
    }

    // 未被移除的气泡保留
    return true
  })

  // 渲染下一帧
  animationFrameId = requestAnimationFrame(animate)
}

// 自动生成新气泡
const startAutoGenerate = () => {
  generateTimer = setInterval(() => {
    if (bubbles.value.length >= MAX_BUBBLE_COUNT) bubbles.value.shift()
    const b = createBubble(true)
    if (b) bubbles.value.push(b)
  }, AUTO_GENERATE_INTERVAL)
}

// 清理动画
const clearAnimation = () => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (generateTimer) clearInterval(generateTimer)
  bubbles.value = []
}

// ====================== 📚 网易云风格照片墙数据处理 ======================
const extendedBookWallData = computed(() => {
  // 自动兼容 ref 或普通响应式数组
  const rawData = bookWallData.value?.length !== undefined ? bookWallData.value : bookWallData;
  if (!rawData || rawData.length === 0) return [];

  let list = [...rawData];
  // 💡 如果书籍较少，循环拼接直到至少有 10 本，确保能撑满任何大屏幕宽度
  while (list.length < 10) {
    list = [...list, ...rawData];
  }
  // 🌟 核心技巧：将整组数据复制一份拼接在后面，实现 0% 到 -50% 的完美无缝平滑衔接
  return [...list, ...list];
});

const extendedBookWallDataRow2 = computed(() => {
  const rawData = bookWallData.value?.length !== undefined ? bookWallData.value : bookWallData;
  if (!rawData || rawData.length === 0) return [];

  // 让第二行的数据首尾错开，避免上下两行完全对齐，显得更加灵动
  let baseList = [...rawData];
  if (baseList.length > 1) {
    const first = baseList.shift();
    baseList.push(first);
  }

  let list = [...baseList];
  while (list.length < 10) {
    list = [...list, ...baseList];
  }
  return [...list, ...list];
});
// ====================== 6. 数据请求函数 ======================
const fetchReportData = async () => {
  if (!currentUserId) return
  loading.value = true
  clearAnimation()
  try {
    const res = await request.get('/user/report', {
      params: {
        userId: currentUserId.value,
        type: activeTab.value
      }
    })

    if (res.code === 200) {
      reportData.value = res.data

      // ✅ 核心修复：从 stats.startTime 提取日期，而非读取不存在的 res.data.date
      const rawStartTime = res.data.stats.startTime // 格式：2026-05-25 00:00:00
      const pureDateStr = rawStartTime.split(' ')[0] // 截取纯日期：2026-05-25

      // 根据报告类型，转换为后端需要的格式
      switch (activeTab.value) {
        case 'DAY':
          // 日报：直接用纯日期 2026-05-25
          currentReportDate.value = pureDateStr
          break
        case 'WEEK':
          // 周报：计算当周周一的日期（匹配数据库存储格式）
          currentReportDate.value = getWeekMonday(pureDateStr)
          break
        case 'MONTH':
          // 月报：提取年月 2026-05
          currentReportDate.value = pureDateStr.substring(0, 7)
          break
        default:
          currentReportDate.value = pureDateStr
      }

      // 同步更新选中日期
      selectedDate.value = currentReportDate.value

      nextTick(() => {
        setTimeout(() => {
          updateAllBookCards()
          initBubbleAnimation()
        }, 300)
      })
    }
  } catch (e) {
    console.error('获取阅读报告失败', e)
  } finally {
    loading.value = false
  }
}

// ====================== 7. 监听 & 生命周期 ======================
watch(activeTab, () => {
  clearAnimation()
  // 切换标签时清空旧值，避免状态混乱
  rawDateValue.value = null
  selectedDate.value = ''
  fetchReportData()
}, { immediate: false })

// ✅ 监听弹窗打开：强制重置日期为当前报告日期
watch(dialogVisible, (isOpen) => {
  if (isOpen) {
    // 兜底：如果当前报告日期为空，用今天的日期填充
    if (!currentReportDate.value) {
      const today = new Date()
      const year = today.getFullYear()
      const month = String(today.getMonth() + 1).padStart(2, '0')
      const day = String(today.getDate()).padStart(2, '0')

      switch (activeTab.value) {
        case 'DAY':
          currentReportDate.value = `${year}-${month}-${day}`
          break
        case 'WEEK':
          currentReportDate.value = getWeekMonday(new Date())
          break
        case 'MONTH':
          currentReportDate.value = `${year}-${month}`
          break
      }
      selectedDate.value = currentReportDate.value
    }

    // 将数据库格式的日期解析为Date对象，赋值给组件
    rawDateValue.value = parseDatabaseDate(currentReportDate.value, activeTab.value)
    selectedDate.value = currentReportDate.value

    // 强制刷新组件，彻底解决状态残留问题
    nextTick(() => {
      if (datePickerRef.value) {
        datePickerRef.value.focus()
        datePickerRef.value.blur()
      }
    })
  }
})

// ✅ 监听日期变化：打印日志
watch(selectedDate, (newVal) => {
  console.log('最终传给后端的日期：', newVal)
})

onMounted(() => {
  ElMessage.primary("第一次加载时间稍长，请耐心等待~")
  // 路由参数统一转大写，默认WEEK
  const typeFromUrl = route.query.type
  if (typeFromUrl) {
    activeTab.value = typeFromUrl.toString().toUpperCase()
  } else {
    activeTab.value = 'WEEK'
  }
  fetchReportData()
})
onUnmounted(() => { clearAnimation() })
</script>

<template>
  <div style="padding: 20px; max-width: 1200px; margin: 0 auto;position: relative; z-index: 10; min-height: calc(100vh - 190px);">
    <!-- 🔥 顶部标签切换 -->
    <div class="tab-header" style="margin-bottom: 24px; text-align: center;">
      <el-tabs v-model="activeTab" type="card" class="demo-tabs" style="display: inline-block;">
        <el-tab-pane label="日报" name="DAY"></el-tab-pane>
        <el-tab-pane label="周报" name="WEEK"></el-tab-pane>
        <el-tab-pane label="月报" name="MONTH"></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 🔥 骨架屏加载状态 -->
    <el-skeleton v-if="loading" animated style="width: 100%;">
      <template #template>
        <el-skeleton-item variant="rect" style="width: 100%; height: 120px; margin-bottom: 20px;" />
        <div style="display: flex; gap: 20px; margin-bottom: 20px;">
          <el-skeleton-item variant="rect" style="width: 25%; height: 120px;" />
          <el-skeleton-item variant="rect" style="width: 25%; height: 120px;" />
          <el-skeleton-item variant="rect" style="width: 25%; height: 120px;" />
          <el-skeleton-item variant="rect" style="width: 25%; height: 120px;" />
        </div>
        <el-skeleton-item variant="rect" style="width: 100%; height: 200px; margin-bottom: 20px;" />
        <el-skeleton-item variant="rect" style="width: 100%; height: 300px; margin-bottom: 20px;" />
        <el-skeleton-item variant="rect" style="width: 100%; height: 250px;" />
      </template>
    </el-skeleton>

    <template v-else-if="reportData">
      <el-button
          type="primary"
          link
          style="position: absolute; left: 20px; top: 20px; color: black; font-size: 14px; z-index: 100;"
          @click="dialogVisible = true"
      >
        <el-icon><ArrowLeft /></el-icon>
        {{ historyButtonText }}
      </el-button>

      <div class="morus-card-container">
        <div class="morus-wrapper">

          <div class="morus-panel panel-1">
            <el-card class="report-header-card glass-card" style="margin-bottom: 24px; border: none;">
              <div style="text-align: center; padding: 20px 0;">
                <h1 style="font-size: 28px; font-weight: 700; margin-bottom: 16px; color: var(--el-text-color-primary);">{{ reportData.reportTitle }}</h1>
                <div class="ai-comment" style="font-size: 16px; line-height: 1.8; max-width: 800px; margin: 0 auto; background: rgba(255,255,255,0.2); padding: 20px; border-radius: 12px; text-align: left">
                  {{ reportData.aiComment }}
                </div>
              </div>
            </el-card>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="12" :md="6" class="mb-4">
                <el-card class="stat-card glass-card" style="padding: 16px; height: auto; text-align: left;">
                  <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                    <el-icon style="font-size: 16px;"><Reading /></el-icon>
                    <span style="font-size: 14px;">阅读总时长</span>
                  </div>
                  <div style="font-size: 18px; font-weight: 600;">{{ formatTotalDuration.hours }}小时{{ formatTotalDuration.minutes }}分钟</div>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6" class="mb-4">
                <el-card class="stat-card glass-card" style="padding: 16px; height: auto; text-align: left;">
                  <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                    <el-icon style="font-size: 16px;"><ChatLineRound /></el-icon>
                    <span style="font-size: 14px;">总互动数</span>
                  </div>
                  <div style="font-size: 18px; font-weight: 600;">{{ totalInteraction }}</div>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6" class="mb-4">
                <el-card class="stat-card glass-card" style="padding: 16px; height: auto; text-align: left;">
                  <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                    <el-icon style="font-size: 16px;"><Collection /></el-icon>
                    <span style="font-size: 14px;">阅读书籍数</span>
                  </div>
                  <div style="font-size: 18px; font-weight: 600;">{{ reportData.stats.readBookCount }}本</div>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="12" :md="6" class="mb-4">
                <el-card class="stat-card glass-card" style="padding: 16px; height: auto; text-align: left;">
                  <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                    <el-icon style="font-size: 16px;"><Document /></el-icon>
                    <span style="font-size: 14px;">新增笔记数</span>
                  </div>
                  <div style="font-size: 18px; font-weight: 600;">{{ reportData.stats.newNoteCount }}篇</div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <div class="morus-panel panel-2">
            <el-card class="main-card glass-card" style="margin-bottom: 24px;">
              <template #header>
                <h3 style="margin: 0; display: flex; align-items: center; gap: 8px;">
                  <el-icon><ChatDotRound /></el-icon><span>消息互动记录</span>
                </h3>
              </template>
              <div style="padding: 0; height: 500px;">
                <el-empty v-if="messageList.length === 0" description="暂无消息记录" />
                <div ref="bubbleContainer" v-else class="bubble-container" style="width: 100%; height: 100%; position: relative; overflow: hidden; border-radius: 8px;">
                  <div v-for="bubble in bubbles" :key="bubble.id" class="msg-bubble" :style="{ left: `${bubble.x}px`, top: `${bubble.y}px`, zIndex: 10 }">
                    <div v-html="parseBookLinkToCard(bubble.text)"></div>
                  </div>
                </div>
              </div>
            </el-card>

            <el-row :gutter="16" style="margin-bottom: 24px;">
              <el-col :xs="24" :md="14">
                <el-card class="main-card glass-card" style="height: 360px; display: flex; flex-direction: column;">
                  <template #header>
                    <h3 style="margin: 0; display: flex; align-items: center; gap: 8px;">
                      <span>📅 阅读日历</span>
                      <el-tag v-if="activeTab !== 'DAY'" size="small" type="info">{{ formatDate(reportData.stats.startTime) }} - {{ formatDate(reportData.stats.endTime) }}</el-tag>
                    </h3>
                  </template>
                  <div v-if="activeTab === 'DAY'" style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:20px;text-align:center;">
                    <p style="font-size:18px;font-weight:500;margin:0 0 12px 0;">☀️ 今日阅读寄语</p>
                    <p style="font-size:15px;line-height:1.8;margin:0;max-width:600px;">{{ dailyGreeting }}</p>
                  </div>
                  <template v-else>
                    <div style="min-height: 192px; display: flex; align-items: center; justify-content: center; overflow: auto; padding: 10px 0;">
                      <div class="heatmap-grid" :style="{ display: 'grid', gridTemplateColumns: 'repeat(7, 32px)', gridTemplateRows: `repeat(${heatmapData.length}, 32px)`, gap: '8px' }">
                        <template v-for="(week, weekIndex) in heatmapData" :key="weekIndex">
                          <template v-for="dayIndex in dayIndexRange" :key="dayIndex">
                            <el-tooltip :content="`${week[dayIndex].duration === 0 ? '0分钟' : Math.floor(week[dayIndex].duration/60) + '分钟'} · ${week[dayIndex].date}`" placement="top" :show-arrow="false">
                              <div class="heatmap-cell" :style="{ backgroundColor: week[dayIndex].isCurrentMonth === false ? 'rgba(245,245,245,0.3)' : levelColors[week[dayIndex].level], width: '32px', height: '32px', borderRadius: '4px', cursor: 'pointer', opacity: week[dayIndex].isCurrentMonth === false ? 0.3 : 1 }"></div>
                            </el-tooltip>
                          </template>
                        </template>
                      </div>
                    </div>
                  </template>
                </el-card>
              </el-col>
              <el-col :xs="24" :md="10">
                <el-card class="main-card glass-card" style="height: 360px; display: flex; flex-direction: column;">
                  <template #header>
                    <h3 style="margin: 0; display: flex; align-items: center; gap: 8px;"><span>🏆 阅读排行榜</span></h3>
                  </template>
                  <el-empty v-if="allRankedBooks.length === 0" description="暂无阅读记录" style="flex: 1; display: flex; align-items: center; justify-content: center;" />
                  <el-scrollbar v-else style="flex: 1; padding-right: 4px;">
                    <div style="display: flex; flex-direction: column; gap: 14px; padding: 10px 4px 10px 0;">
                      <div v-if="allRankedBooks[0]" class="rank-item-box top1-item" style="display: flex; align-items: center; gap: 20px; padding: 16px; border-radius: 12px; background: rgba(255, 251, 235, 0.6); position: relative; overflow: hidden;">
                        <div class="rank-badge badge-top1">1</div>
                        <el-image :src="allRankedBooks[0].pictureName || '/default-book.png'" style="width: 70px; height: 95px; object-fit: cover; border-radius: 6px; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3); flex-shrink: 0;" fit="cover" />
                        <div style="flex: 1; min-width: 0;">
                          <div style="font-size: 16px; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; color: #92400e;">{{ allRankedBooks[0].bookname || '未知书籍' }}</div>
                          <div style="font-size: 14px; color: #b45309; font-weight: 500;">🥇 读了：{{ Math.floor((allRankedBooks[0].totalDuration || 0)/60) }}分钟</div>
                        </div>
                      </div>
                      <div v-if="allRankedBooks.length > 1" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;">
                        <div v-for="(book, index) in allRankedBooks.slice(1)" :key="book.isbn || index" class="rank-item-box other-rank-item" style="display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: 8px; background: rgba(255,255,255,0.3); position: relative; overflow: hidden;">
                          <div class="rank-badge" :class="index + 2 === 2 ? 'badge-top2' : (index + 2 === 3 ? 'badge-top3' : 'badge-other')">{{ index + 2 }}</div>
                          <el-image :src="book.pictureName || '/default-book.png'" style="width: 42px; height: 56px; object-fit: cover; border-radius: 4px; flex-shrink: 0;" fit="cover" />
                          <div style="flex: 1; min-width: 0;">
                            <div style="font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px;">{{ book.bookname || '未知书籍' }}</div>
                            <div style="font-size: 11px; opacity: 0.8;">{{ Math.floor((book.totalDuration || 0)/60) }}分钟</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-scrollbar>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <div class="morus-panel panel-3">
            <el-card class="main-card glass-card" style="margin-bottom: 24px;">
              <template #header>
                <h3 style="margin: 0;">📚 阅读书籍墙</h3>
              </template>
              <div class="photo-wall-container">
                <el-empty v-if="!bookWallData || bookWallData.length === 0" description="暂无阅读记录" />
                <div v-else class="marquee-wrapper">
                  <div class="marquee-track track-left">
                    <div class="marquee-group">
                      <div v-for="(book, index) in extendedBookWallData" :key="'row1-' + index" class="book-card-item">
                        <el-image :src="book.pictureName || '/default-book.png'" fit="cover" class="book-card-image" :preview-src-list="[book.pictureName || '/default-book.png']" @click="gotoBookDetail(book.iSBN)" />
                        <div class="book-card-mask">
                          <div class="mask-title" :title="book.bookname">{{ book.bookname || '未知书籍' }}</div>
                          <div class="mask-duration">⏱️ {{ Math.floor((book.totalDuration || 0)/60) }} 分钟</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="marquee-track track-right">
                    <div class="marquee-group">
                      <div v-for="(book, index) in extendedBookWallDataRow2" :key="'row2-' + index" class="book-card-item">
                        <el-image :src="book.pictureName || '/default-book.png'" fit="cover" class="book-card-image" :preview-src-list="[book.pictureName || '/default-book.png']" />
                        <div class="book-card-mask">
                          <div class="mask-title" :title="book.bookname">{{ book.bookname || '未知书籍' }}</div>
                          <div class="mask-duration">⏱️ {{ Math.floor((book.totalDuration || 0)/60) }} 分钟</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>

            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-card class="main-card glass-card" style="height: 160px; display: flex; flex-direction: column;">
                  <template #header><h3 style="margin: 0; display: flex; align-items: center; gap: 8px;"><el-icon><ChatLineRound /></el-icon><span>评论互动</span></h3></template>
                  <div style="flex: 1; overflow: hidden;">
                    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px;">
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #409eff;">{{ reportData.stats.bookCommentCount }}</div><div style="font-size: 14px;">书籍评论</div></div>
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #67c23a;">{{ reportData.stats.userCommentCount }}</div><div style="font-size: 14px;">论坛评论</div></div>
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #e6a23c;">{{ reportData.stats.likeCount }}</div><div style="font-size: 14px;">点赞数</div></div>
                    </div>
                  </div>
                </el-card>
                <el-empty v-if="reportData.stats.bookCommentList.length === 0 && reportData.stats.userCommentList.length === 0" description="暂无评论记录" :image-size="80" style="margin-top:10px" />
                <el-carousel v-else height="320px" direction="vertical" type="card" :autoplay="true" :interval="3000" class="wide-card-carousel comment-carousel" style="margin-top: 10px;">
                  <el-carousel-item v-for="comment in reportData.stats.bookCommentList.slice(0, 5)" :key="comment.commentId">
                    <div style="height: 100%; display: flex; align-items: center; justify-content: center; padding: 12px;"><div v-html="parseBookLinkToCard(generateBookCommentShareLinkById(comment.commentId))"></div></div>
                  </el-carousel-item>
                  <el-carousel-item v-for="comment in reportData.stats.userCommentList.slice(0, 5)" :key="comment.commentId">
                    <div style="height: 100%; display: flex; align-items: center; justify-content: center; padding: 12px;"><div v-html="parseBookLinkToCard(generateUserCommentShareLinkById(comment.commentId))"></div></div>
                  </el-carousel-item>
                </el-carousel>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-card class="main-card glass-card" style="height: 160px; display: flex; flex-direction: column;">
                  <template #header><h3 style="margin: 0; display: flex; align-items: center; gap: 8px;"><el-icon><UserFilled /></el-icon><span>好友社交</span></h3></template>
                  <div style="flex: 1; overflow: hidden;">
                    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px;">
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #67c23a;">{{ reportData.stats.friendCount }}</div><div style="font-size: 14px;">新增好友</div></div>
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #409eff;">{{ reportData.stats.sentRequestCount }}</div><div style="font-size: 14px;">发送申请</div></div>
                      <div style="text-align: center;"><div style="font-size: 24px; font-weight: 600; color: #e6a23c;">{{ reportData.stats.receivedRequestCount }}</div><div style="font-size: 14px;">收到申请</div></div>
                    </div>
                  </div>
                </el-card>
                <el-empty v-if="reportData.stats.friendList.length === 0" description="暂无新增好友" :image-size="80" style="margin-top:10px" />
                <el-carousel v-else height="320px" direction="vertical" type="card" :autoplay="true" :interval="3000" class="wide-card-carousel friend-carousel" style="margin-top: 10px;">
                  <el-carousel-item v-for="friend in reportData.stats.friendList" :key="friend.id">
                    <div style="height: 100%; display: flex; align-items: center; justify-content: center; padding: 12px;"><div v-html="parseBookLinkToCard(generateUserShareLinkById(friend.friendId))"></div></div>
                  </el-carousel-item>
                </el-carousel>
              </el-col>
            </el-row>
          </div>

        </div>
      </div>

      <!-- ✅ 新增：历史报告选择弹窗（完全重构） -->
      <el-dialog
          v-model="dialogVisible"
          :title="dialogTitle"
          width="400px"
          :close-on-click-modal="false"
      >
        <div style="padding: 20px 0;">
          <!-- ✅ 日期选择器：100%解决格式错误和跳回问题 -->
          <el-date-picker
              ref="datePickerRef"
              v-model="rawDateValue"
              :type="datePickerType"
              :placeholder="dialogTitle"
          :key="activeTab"
          :picker-options="{
          firstDayOfWeek: 1,
          disabledDate: time => time.getTime() > Date.now()
          }"
          @change="handleDateChange"
          clearable
          style="width: 100%;"
          />
        </div>

        <!-- 弹窗底部按钮 -->
        <template #footer>
    <span class="dialog-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
          type="primary"
          :loading="dialogLoading"
          @click="fetchReportByDate"
      >
        确认查询
      </el-button>
    </span>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
/* 通用卡片样式 */
.main-card {
  transition: all 0.2s ease;
}
.main-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 统计卡片样式 */
.stat-card {
  height: 100%;
  transition: all 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.stat-card :deep(.el-statistic__content) {
  font-size: 28px;
  font-weight: 600;
}

/* 报告头部卡片 */
.report-header-card {
  transition: all 0.3s ease;
}
.report-header-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

/* 书籍墙样式 */
.book-item {
  transition: all 0.2s ease;
}
.book-item:hover {
  transform: translateY(-4px);
}
.book-item:hover img {
  box-shadow: 0 8px 16px rgba(0,0,0,0.15);
}

/* TOP书籍样式 */
.top-book-item {
  transition: all 0.2s ease;
}
.top-book-item:hover {
  background: var(--el-fill-color) !important;
}

/* 热力图单元格（适配大格子） */
.heatmap-cell {
  transition: all 0.2s ease;
}
.heatmap-cell:hover {
  transform: scale(1.1);
  z-index: 10;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  height: 6px;
  width: 6px;
}
::-webkit-scrollbar-track {
  background: var(--el-bg-color-page);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb {
  background: var(--el-border-color);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: var(--el-text-color-placeholder);
}

   /* 🔥 核心：加宽垂直堆叠卡片宽度 */
 .wide-card-carousel :deep(.el-carousel__item) {
   /* 卡片宽度占轮播容器的90%，告别窄卡片 */
   width: 90% !important;
   /* 水平居中 */
   left: 5% !important;
   /* 优化堆叠间距，宽卡片更自然 */
   margin: 0 !important;
 }

/* 🔥 优化垂直card轮播的堆叠效果 */
.wide-card-carousel :deep(.el-carousel--vertical .el-carousel__item) {
  /* 调整垂直方向的堆叠偏移，适配宽卡片 */
  transform: translateY(0) scale(0.85);
  transition: all 0.3s ease;
}

/* 🔥 激活态卡片样式 */
.wide-card-carousel :deep(.el-carousel--vertical .el-carousel__item.is-active) {
  transform: translateY(0) scale(1);
  z-index: 10;
}

/* 🔥 前后卡片的透明度和缩放，增强层次感 */
.wide-card-carousel :deep(.el-carousel--vertical .el-carousel__item:nth-child(2)) {
  transform: translateY(10px) scale(0.9);
  opacity: 0.6;
}

.wide-card-carousel :deep(.el-carousel--vertical .el-carousel__item:nth-child(3)) {
  transform: translateY(20px) scale(0.8);
  opacity: 0.3;
}

/* 消息气泡样式（磨砂质感 + 短文字不换行 + 长文字自动换行 + 卡片适配） */
.msg-bubble {
  position: absolute;
  padding: 12px 16px;
  border-radius: 18px;
  /* 磨砂气泡 */
  background: rgba(255, 255, 255, 0.3) !important;
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;

  font-size: 13px;
  line-height: 1.6;
  color: #333;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
  transition: none;
  display: block;
  text-align: left;

  /* 🔥 核心修复：完美解决换行问题 */
  width: fit-content; /* 宽度自适应内容 */
  max-width: 320px; /* 最大宽度，超过自动换行 */
  min-width: 50px; /* 最小宽度，防止气泡太窄 */
  white-space: pre-line; /* 短文字不换行 + 长文字自动换行 + 保留换行符 */
  overflow-wrap: break-word; /* 长单词/长URL自动换行 */
  word-break: break-word; /* 中文/英文都能正常换行 */
}

/* 🔥 内部卡片适配：保证不溢出气泡 */
.msg-bubble :deep(.book-share-card) {
  max-width: 100% !important;
  margin: 8px 0 0 0 !important;
  background: transparent !important;
  border: none !important;
}

/* 🔥 内部图片适配：保证不溢出气泡 */
.msg-bubble :deep(img) {
  max-width: 100% !important;
  height: auto !important;
  border-radius: 8px;
  margin: 8px 0;
}

/* 🔥 终极毛玻璃卡片 */
:deep(.glass-card) {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(100px) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08) !important;
  border-radius: 12px !important;
  overflow: hidden !important;
}

/* 卡片头部透明 */
:deep(.glass-card .el-card__header) {
  background: transparent !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15) !important;
  color: var(--el-text-color-primary) !important;
}

/* 卡片内容彻底透明 */
:deep(.glass-card .el-card__body) {
  background: transparent !important;
  padding: 10px !important;
}

/* 🔥 气泡容器：完全透明 + 磨砂（修复选择器+生效） */
:deep(.bubble-container) {
  background: transparent !important;
  backdrop-filter: blur(4px) !important;
  border-radius: 8px !important;
}

/* 空状态适配 */
:deep(.glass-card .el-empty__description) {
  color: var(--el-text-color-secondary) !important;
}

/* ====================== 🎬 网易云无限滚动照片墙 CSS ====================== */

/* 照片墙外层可视窗口：切断溢出的卡片 */
.photo-wall-container {
  width: 100%;
  padding: 10px 0;
  overflow: hidden;
  box-sizing: border-box;
  position: relative;
}

/* 垂直排列的双行轨道包装盒 */
.marquee-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px; /* 上下两行的间距 */
  width: 100%;
}

/* 滚动的单行水平跑马灯轨道 */
.marquee-track {
  display: flex;
  width: max-content;
  overflow: hidden;
  user-select: none;
}

/* 包含所有卡片的弹性群组 */
.marquee-group {
  display: flex;
  gap: 16px; /* 单行卡片之间的左右间距 */
  width: max-content;
}

/* 🏃 行动效：第一行向左匀速无缝滑动 (35秒一轮，可自行调节速度) */
.track-left .marquee-group {
  animation: scrollLeft 35s linear infinite;
}

/* 🏃 行动效：第二行向右匀速无缝滑动 */
.track-right .marquee-group {
  animation: scrollRight 35s linear infinite;
}

/* 🔥 惊艳交互：鼠标悬停在某一行上时，该行平滑暂停，方便用户点选或看清 */
.marquee-track:hover .marquee-group {
  animation-play-state: paused;
}

/* 🃏 单个高颜值书籍立体卡片 */
.book-card-item {
  position: relative;
  width: 135px;         /* 完美的黄金比例书卡宽度 */
  height: 185px;        /* 完美的黄金比例书卡高度 */
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s ease;
  cursor: pointer;
  flex-shrink: 0;       /* 核心：防止 Flex 机制挤压卡片 */
}

/* 卡片悬停：微升、变大、深阴影 */
.book-card-item:hover {
  transform: translateY(-6px) scale(1.06);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.26);
  z-index: 10;
}

/* 书籍图片基础样式与微放大 */
.book-card-image {
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
}
.book-card-item:hover .book-card-image {
  transform: scale(1.08);
}

/* 🎵 网易云云音乐黑胶唱片同款：底部半透明黑金渐变遮罩 */
.book-card-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 100%;
  /* 从透明过渡到深黑，保证在任何背景色下字体清晰可见 */
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.3) 60%, rgba(0, 0, 0, 0) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 12px;
  box-sizing: border-box;
  opacity: 0; /* 🌟 默认隐藏，悬停时优雅淡入。如果你希望文字一直显示，改成 0.95 或 1 即可 */
  transition: opacity 0.3s ease;
}

/* 悬停时遮罩全显 */
.book-card-item:hover .book-card-mask {
  opacity: 1;
}

/* 遮罩内的精细字体样式 */
.mask-title {
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
}

.mask-duration {
  color: #d1d5db;
  font-size: 11px;
}

/* 核心无缝核心帧：向左位移一半自动回弹，肉眼完全无法察觉跳动 */
@keyframes scrollLeft {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

/* 核心无缝核心帧：向右从负一半归零 */
@keyframes scrollRight {
  0% {
    transform: translateX(-50%);
  }
  100% {
    transform: translateX(0);
  }
}

/* ====================== 🏆 丰富版阅读排行榜样式 ====================== */

/* 榜单卡片悬停微动特效 */
.rank-item-box {
  transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.rank-item-box:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  background-color: var(--el-fill-color) !important; /* 悬停时稍微加深灰色背景 */
}

/* Top1 悬停稍微特殊处理，不破坏金黄色渐变 */
.top1-item:hover {
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.25);
  background: linear-gradient(135deg, #fffbeb 0%, #fde68a 100%) !important;
}

/* 左上角榜单名次角标通用样式 */
.rank-badge {
  position: absolute;
  top: 0;
  left: 0;
  width: 22px;
  height: 18px;
  font-size: 10px;
  font-weight: 800;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom-right-radius: 8px;
  z-index: 2;
}

/* 🥇 冠军角标：霸气纯金 */
.badge-top1 {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  width: 26px;
  height: 22px;
  font-size: 12px;
  border-bottom-right-radius: 10px;
}

/* 🥈 亚军角标：高级银灰 */
.badge-top2 {
  background: linear-gradient(135deg, #9ca3af, #4b5563);
}

/* 🥉 季军角标：质感古铜 */
.badge-top3 {
  background: linear-gradient(135deg, #b45309, #78350f);
}

/* 🎖️ 4名开外的角标：内敛轻灰 */
.badge-other {
  background: #cbd5e1;
  color: #64748b;
}

/* 1. 3D 舞台容器 */
.morus-card-container {
  width: 100%;
  padding: 10px 0 100px 0; /* 底部预留充足留白，防止展开时撑破视口 */
  display: flex;
  justify-content: center;
  perspective: 1600px; /* 3D视距，数值决定了折叠的透视畸变程度 */
}

/* 2. 控制 Hover 触发范围的包裹器 */
.morus-wrapper {
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform-style: preserve-3d;
  position: relative;
}

/* 3. 面板基础动画：带阻尼回弹感的贝塞尔曲线 */
.morus-panel {
  width: 100%;
  transform-origin: top center;
  transition: transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1),
  opacity 0.6s ease,
  margin 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
  will-change: transform, opacity, margin; /* 性能优化：提前告知浏览器启用GPU渲染 */
}

/* 4. 第一层（信封封面）：始终处于最上层，完全平铺 */
.panel-1 {
  z-index: 3;
  transform: rotateX(0deg);
  position: relative;
}

/* 5. 第二、三层：默认向内翻转折叠、透明度降低，且向上拉拽制造堆叠重合感 */
.panel-2, .panel-3 {
  opacity: 0.3; /* 微微透出下层光影增强层次感 */
  pointer-events: none; /* 折叠时防误触 */

  /* 核心：X轴向后翻转、Y轴向上拉升、Z轴向屏幕内退缩 */
  transform: rotateX(-70deg) translateY(-80px) translateZ(-60px);
  margin-top: -160px; /* 让下面的层深深地插进上面层的底部空间 */
}

.panel-2 {
  z-index: 2;
}

.panel-3 {
  z-index: 1;
  /* 第三层折叠得更深 */
  transform: rotateX(-80deg) translateY(-140px) translateZ(-120px);
  margin-top: -240px;
}

/* 🔥 6. 解锁魔法：Hover 时全部层层掉落展开 */
.morus-wrapper:hover .panel-2,
.morus-wrapper:hover .panel-3 {
  opacity: 1;
  pointer-events: auto;
  transform: rotateX(0deg) translateY(0) translateZ(0); /* 归位平铺 */
  margin-top: 24px; /* 恢复正常的卡片间距 */
}

/* 7. 接力延迟动画：制造视觉上的次序感 */
/* 展开时：第2层先下落，第3层紧随其后 */
.morus-wrapper:hover .panel-2 { transition-delay: 0.05s; }
.morus-wrapper:hover .panel-3 { transition-delay: 0.15s; }

/* 折叠时：鼠标移出，第3层先收起，第2层再收回（符合物理现实） */
.morus-wrapper:not(:hover) .panel-3 { transition-delay: 0s; }
.morus-wrapper:not(:hover) .panel-2 { transition-delay: 0.1s; }

/* ====================== 修复：深色模式适配 ====================== */
/* 避免原有的 tooltip 或背景在 3D 转换中丢失毛玻璃效果 */
:deep(.glass-card) {
  /* 确保 3D 转换下的卡片背后依然保持良好的玻璃反射质感 */
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08) !important;
}
</style>