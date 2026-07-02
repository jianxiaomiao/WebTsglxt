<template>
  <div
      v-if="!isHidden"
      class="pet-widget"
      :style="{ bottom: bottomPos + 'px', left: leftPos + 'px' }"
      :class="{ 'is-dragging': isDragging, 'is-half-hidden': isHalfHidden }"
      @mousedown.left="startDrag"
      @click="handleClick"
      @contextmenu="handleContextmenu"
      @touchstart.prevent.stop="handleTouchStart"
      @touchmove.prevent.stop="handleTouchMove"
      @touchend.prevent.stop="handleTouchEnd"
      @touchcancel.prevent.stop="handleTouchEnd"
  >
    <img
        :src="currentGif"
        alt="桌面小宠物"
        class="pet-image"
        :class="{ 'pet-paused': isPaused, 'pet-flipped': needsFlip }"
        draggable="false"
    >

    <el-dropdown
        ref="dropdownRef"
        :virtual-ref="triggerRef"
        :show-arrow="false"
        :popper-options="{ modifiers: [{ name: 'offset', options: { offset: [0, 0] } }] }"
        virtual-triggering
        trigger="contextmenu"
        placement="top-start"
        @command="handleMenuCommand"
    >
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="hide" icon="Hide">隐藏宠物</el-dropdown-item>
          <el-dropdown-item command="switch" icon="Refresh">切换宠物/模式</el-dropdown-item>
          <el-dropdown-item command="question" icon="Search">向小宠物提问</el-dropdown-item>
          <el-dropdown-item command="quiz" icon="Edit">出一道题</el-dropdown-item>
          <el-dropdown-item
              :command="petStore.isAutoQuizRunning ? 'stopAutoQuiz' : 'startAutoQuiz'"
              :icon="petStore.isAutoQuizRunning ? 'Close' : 'Clock'"
          >
            {{ petStore.isAutoQuizRunning ? '关闭自动出题' : '开启自动出题' }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <Teleport to="body">
      <div
          v-if="!isHidden"
          class="bubble-track-layer"
          :class="{ 'is-dragging': isDragging }"
          :style="{ bottom: bottomPos + 'px', left: leftPos + 'px' }"
      >
        <div
            v-for="(item, index) in messages"
            :key="item.id"
            class="pet-bubble"
            :ref="el => bubbleRefs[index] = el"
            :style="{ bottom: computedBottoms[index] + 'px' }"
            :class="[
              `bubble-${item.type}`,
              { 'closing': item.isClosing, 'bubble-pos-left': !isPetOnLeft }
            ]"
            @click.stop
            @contextmenu.stop
            @mousedown.stop
            @touchstart.stop
        >
          <template v-if="item.type === petStore.MESSAGE_TYPES.CHAT">
            {{ item.text }}
          </template>

          <template v-else-if="item.type === petStore.MESSAGE_TYPES.THINKING">
            <div class="thinking-content">
              {{ item.text }}
              <span class="thinking-dot"></span>
              <span class="thinking-dot"></span>
              <span class="thinking-dot"></span>
            </div>
          </template>

          <template v-else-if="item.type === petStore.MESSAGE_TYPES.INTERACTION">
            <div class="interaction-bubble">
              <span class="close-btn" @click.stop="petStore.removeMessage(item.id)">×</span>
              <div class="question-title">{{ item.question }}</div>

              <div class="answer-area" v-if="item.userAnswer === null">
                <template v-if="item.questionType === 'judge'">
                  <el-button size="small" @click.stop="submitAnswer(item, true)">对</el-button>
                  <el-button size="small" @click.stop="submitAnswer(item, false)">错</el-button>
                </template>
                <template v-else-if="item.questionType === 'choice'">
                  <el-button v-for="(option, idx) in item.options" :key="idx" size="small" @click.stop="submitAnswer(item, option.charAt(0))">
                    {{ option }}
                  </el-button>
                </template>
              </div>

              <div class="result-area" v-else>
                <div class="answer-area">
                  <template v-if="item.questionType === 'judge'">
                    <el-button size="small" :class="{ correct: item.correctAnswer === true, wrong: item.userAnswer === true && item.correctAnswer === false }">对</el-button>
                    <el-button size="small" :class="{ correct: item.correctAnswer === false, wrong: item.userAnswer === false && item.correctAnswer === true }">错</el-button>
                  </template>
                  <template v-else-if="item.questionType === 'choice'">
                    <el-button v-for="(option, idx) in item.options" :key="idx" size="small" :class="{ correct: idx === item.correctIndex, wrong: option.charAt(0) === item.userAnswer && !item.isCorrect }">
                      {{ option }}
                    </el-button>
                  </template>
                </div>
                <div class="result-text" :class="item.isCorrect ? 'correct' : 'wrong'">
                  {{ item.isCorrect ? '✅ 回答正确！' : '❌ 回答错误' }}
                </div>
                <div class="analysis-text">
                  解析：{{ item.analysis }}
                  <span class="auto-close-tip" v-if="item.remainingTime">（{{ item.remainingTime }}秒后自动关闭）</span>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="item.type === petStore.MESSAGE_TYPES.CONFIRM">
            <div class="confirm-bubble">
              <div class="confirm-text">{{ item.text }}</div>
              <div class="confirm-buttons">
                <el-button size="small" type="primary" @click.stop="handleConfirm(item)">是</el-button>
                <el-button size="small" @click.stop="handleCancel(item)">否</el-button>
              </div>
            </div>
          </template>
          <!-- 🌟 新增形态一：Static 静态常驻长文气泡（点 X 才关闭） -->
          <template v-else-if="item.type === petStore.MESSAGE_TYPES.STATIC">
            <div class="static-bubble-box">
              <span class="close-btn" @click.stop="petStore.removeMessage(item.id)">×</span>
              <div class="long-text-content">{{ item.text }}</div>
            </div>
          </template>

          <!-- 🌟 新增形态二：Talk 连续辩论气泡（带底部回击输入框） -->
          <template v-else-if="item.type === petStore.MESSAGE_TYPES.TALK">
            <div class="talk-bubble-box">
              <span class="close-btn" @click.stop="petStore.removeMessage(item.id)">×</span>
              <div class="long-text-content">{{ item.text }}</div>

              <div class="talk-reply-bar" @click.stop>
                <el-input
                    v-model="item.userReply"
                    placeholder="回敬辩论者一句..."
                    size="small"
                    @keyup.enter="sendTalkRebuttal(item)"
                >
                  <template #append>
                    <el-button type="primary" @click="sendTalkRebuttal(item)">回击</el-button>
                  </template>
                </el-input>
              </div>
            </div>
          </template>
        </div>
      </div>
    </Teleport>
  </div>

  <el-dialog
      v-model="showAiInput"
      title="💡 小宠物智能引导"
      width="480px"
      :close-on-click-modal="false"
      class="mobile-dialog"
      @close="closeAiInput"
  >
    <el-input
        v-model="aiQuestion"
        type="textarea"
        :rows="3"
        placeholder="请问你想了解什么功能？\n例：怎么登录？怎么借阅书籍？怎么发表评论？"
        class="mb-3"
    />
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeAiInput">取消</el-button>
        <el-button type="primary" :loading="aiLoading" :disabled="aiLoading" @click="sendAiMsg">发送</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import request from '../utils/request.js'

const petStore = usePetStore()
const userStore = useUserStore()
const route = useRoute()

// 提取状态，在未登录时也有安全的默认值
const { currentWeather, isDark } = storeToRefs(userStore)

const userInfo = computed(() => userStore.userInfo || {})
const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

const isHidden = ref(false)
const isPaused = ref(false)

// ====================== 🐾 宠物外观与天气联动核心 ======================

// 所有可用的宠物列表（用于手动切换轮播）
const petGifs = [
  '/dist/dog_sun.gif',
  '/dist/dog_rain.gif',
  '/dist/dog_cloudy.gif',
  '/dist/dog_nightRain.gif',
  '/dist/dog_night.gif'
]

// 手动覆盖模式的指定 GIF。若为 null，则代表“自动天气联动模式”
const manualOverrideGif = ref(null)
// 记录手动切换的索引 (-1代表自动模式)
let currentSwitchIndex = -1

// 🔥 核心计算属性：自动匹配天气和暗黑模式
const currentGif = computed(() => {
  // 1. 如果用户手动指定了外观，优先使用
  if (manualOverrideGif.value) {
    return manualOverrideGif.value
  }

  // 2. 自动匹配模式 (由于 userStore 默认提供 isDark=false 和 currentWeather='sunny'，登录页也能安全运转)
  const isNightMode = isDark.value || false
  const weather = currentWeather.value || 'sunny'

  // 3. 开始映射这 5 种形态
  if (isNightMode || weather === 'night') {
    // 【夜晚组】
    if (weather === 'rain' || weather === 'thunder') {
      return '/dist/dog_nightRain.gif' // 夜晚下雨/打雷
    }
    return '/dist/dog_night.gif'       // 夜晚晴天/多云
  } else {
    // 【白天组】
    if (weather === 'rain' || weather === 'thunder') {
      return '/dist/dog_rain.gif'      // 白天下雨/打雷
    } else if (weather === 'cloudy') {
      return '/dist/dog_cloudy.gif'    // 白天多云
    }
    return '/dist/dog_sun.gif'         // 默认：白天晴天
  }
})

// 切换宠物逻辑：在所有形态中循环，最后回到自动模式
const switchPet = () => {
  currentSwitchIndex++
  // 循环一圈后，回归 -1 (自动联动模式)
  if (currentSwitchIndex >= petGifs.length) {
    currentSwitchIndex = -1
  }

  if (currentSwitchIndex === -1) {
    manualOverrideGif.value = null
    petStore.addMessage('🐾 已恢复【天气自动联动】模式~')
  } else {
    manualOverrideGif.value = petGifs[currentSwitchIndex]
    petStore.addMessage('🐾 已手动切换小宠物外貌\n(当前为固定显示，暂不随天气变化)')
  }
}

// ====================== 🦋 坐标系统彻底重构为 Left ======================
const leftPos = ref(-75) // 默认 PC 端左侧隐藏一半 (-150/2)
const bottomPos = ref(20)

// 响应式追踪，用于计算隐藏深度和气泡方向
const screenWidth = ref(window.innerWidth)
const handleResize = () => { screenWidth.value = window.innerWidth }
const isPetOnLeft = computed(() => leftPos.value < screenWidth.value / 2)
// 🔥 核心计算属性：判断当前宠物是否需要镜像反转
const needsFlip = computed(() => {
  // 通过名称简单判断当前是否为夜晚组的狗（原生朝左）
  const isNightDog = currentGif.value.includes('night')

  if (isNightDog) {
    // 夜晚组（原生朝左）：在左半屏时需要反转朝右（面向屏幕内），在右半屏不需要
    return isPetOnLeft.value
  } else {
    // 白天组（原生朝右）：在左半屏不需要反转，在右半屏时需要反转朝左（面向屏幕内）
    return !isPetOnLeft.value
  }
})
// 🔥 判断是否处于半隐藏状态 (左侧贴边且为负数)
const isHalfHidden = computed(() => {
  const threshold = window.innerWidth <= 768 ? -10 : -30;
  return leftPos.value < threshold;
})

// 拖拽核心状态 (改为 ref 以便响应式触发 CSS 动画解绑)
const isDragging = ref(false)
let startX = 0, startY = 0, startLeft = 0, startBottom = 0
let hasMoved = false

// 🔥 边缘防溢出围栏 (针对 left 做了改造)
const applyBoundary = (targetLeft, targetBottom) => {
  const isMobile = window.innerWidth <= 768
  const petW = isMobile ? 60 : 150
  const petH = isMobile ? 60 : 150

  const maxLeft = window.innerWidth - petW
  const maxBottom = window.innerHeight - petH
  // 允许向左突破负数，形成隐藏效果
  const minLeft = isMobile ? -30 : -75

  leftPos.value = Math.max(minLeft, Math.min(targetLeft, maxLeft))
  bottomPos.value = Math.max(0, Math.min(targetBottom, maxBottom))
}

// 自动贴边吸附逻辑
const snapPetToEdge = () => {
  const isMobile = window.innerWidth <= 768
  const hiddenLeft = isMobile ? -30 : -75
  // 如果距离左边缘小于 20px，直接吸附进去藏一半
  if (leftPos.value < 20) {
    leftPos.value = hiddenLeft
  }
}

// ====================== 📱 移动端触摸拖拽 (彻底修复版) ======================
let touchTimer = null
let isLongPress = false

const handleTouchStart = (e) => {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  startX = touch.clientX
  startY = touch.clientY
  startLeft = leftPos.value
  startBottom = bottomPos.value
  hasMoved = false
  isDragging.value = false
  isLongPress = false

  touchTimer = setTimeout(() => {
    isLongPress = true
    handleContextmenu({ preventDefault: () => {}, stopPropagation: () => {}, clientX: touch.clientX, clientY: touch.clientY })
  }, 500)
}

const handleTouchMove = (e) => {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  const dx = touch.clientX - startX
  const dy = touch.clientY - startY

  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
    if (touchTimer) { clearTimeout(touchTimer); touchTimer = null }
    hasMoved = true
    if (!isLongPress) isDragging.value = true
  }

  if (isDragging.value && !isLongPress) {
    applyBoundary(startLeft + dx, startBottom - dy)
  }
}

const handleTouchEnd = () => {
  if (touchTimer) { clearTimeout(touchTimer); touchTimer = null }
  if (isDragging.value) {
    isDragging.value = false
    snapPetToEdge() // 拖拽结束检查是否需要吸附
    savePosition()
  } else if (!isLongPress && !hasMoved) {
    handleClick()
  }
}

// ====================== 💻 PC端拖拽 ======================
const startDrag = (e) => {
  isDragging.value = true
  hasMoved = false
  startX = e.clientX
  startY = e.clientY
  startLeft = leftPos.value
  startBottom = bottomPos.value
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!isDragging.value) return
  const dx = e.clientX - startX
  const dy = e.clientY - startY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  applyBoundary(startLeft + dx, startBottom - dy)
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  snapPetToEdge() // 拖拽结束检查是否需要吸附
  savePosition()
}

// ====================== ✨ 点击弹射与常规交互 ======================
const handleClick = () => {
  if (isDragging.value || hasMoved) {
    isDragging.value = false; hasMoved = false; return
  }

  // 🔥 核心：如果是隐藏状态，点第一下只会弹出，不执行其他动作
  if (isHalfHidden.value) {
    leftPos.value = 20
    nextTick(() => updateBubblePositions())
    savePosition()
    return
  }

  // 正常弹出 AI 或气泡
  const autoGuide = getCurrentPageGuide()
  petStore.addMessage('💡 当前页面引导：\n' + autoGuide)
  if (isAuthPage.value) return
  showAiInput.value = true
}

const savePosition = () => {
  localStorage.setItem('petPosition', JSON.stringify({ left: leftPos.value, bottom: bottomPos.value }))
  showAiInput.value = false
  nextTick(() => updateBubblePositions())
}

// ====================== 下方均为原有逻辑保持不变 ======================
const dropdownRef = ref()
const position = ref({ x: 0, y: 0, width: 0, height: 0 })
const triggerRef = ref({ getBoundingClientRect: () => DOMRect.fromRect(position.value) })

const { messages } = storeToRefs(petStore)

const bubbleRefs = ref([])
const computedBottoms = ref([])
const BUBBLE_GAP = 10

const showAiInput = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
provide('aiLoading', aiLoading)

const guideMap = {
  login: `【登录功能引导】\n1. 填写必填项\n2. 可点击眼睛图标切换显隐\n3. 点击提交验证`,
  register: `【注册功能引导】\n1. 填写所有必填项\n2. 两次密码一致\n3. 注册成功跳转登录页`,
  home: `【主页引导】\n1. 顶部搜索图书\n2. 点击卡片进详情\n3. 点击头像退出`,
  bookDetail: `【书籍详情引导】\n1. 点击书名阅读\n2. 评论区可编辑/删除评价`,
  bookshelf: `【书架功能引导】\n1. 查看已借阅/收藏\n2. 点击左上角归还`,
  reader: `【阅读引导】\n1. 右上角切字体/章节\n2. 切换背景\n3. 选字解锁更多`,
  forum: `【论坛引导】\n1. 搜索用户ID查看评论\n2. 支持发帖、回复、图片`,
  mine: `【个人中心引导】\n1. 下滑编辑信息\n2. 点击笔记/论坛进大屏`,
  common: '我是你的专属图书系统小宠物～'
}
const getCurrentPageGuide = () => {
  const path = route.path
  if (path === '/login') return guideMap.login
  if (path === '/register') return guideMap.register
  if (path === '/home' || path === '/') return guideMap.home
  if (path.includes('/book/detail')) return guideMap.bookDetail
  if (path.includes('/bookshelf')) return guideMap.bookshelf
  if (path.includes('/book/reader')) return guideMap.reader
  if (path.includes('/forum')) return guideMap.forum
  if (path.includes('/mine')) return guideMap.mine
  return guideMap.common
}
const matchUserQuestion = (q) => {
  q = q.toLowerCase()
  if (q.includes('登录')) return guideMap.login
  if (q.includes('注册')) return guideMap.register
  if (q.includes('主页')||q.includes('借书')) return guideMap.home
  if (q.includes('详情')||q.includes('评论')) return guideMap.bookDetail
  if (q.includes('书架')||q.includes('还书')) return guideMap.bookshelf
  if (q.includes('阅读')) return guideMap.reader
  if (q.includes('论坛')) return guideMap.forum
  if (q.includes('我的')||q.includes('个人中心')) return guideMap.mine
  return null
}

const hidePet = () => {
  isHidden.value = true
  ElMessage.success('小宠物已隐藏~ 刷新页面可重新显示')
}

const handleContextmenu = (event) => {
  event.preventDefault(); event.stopPropagation()
  position.value = { x: event.clientX, y: event.clientY, width: 0, height: 0 }
  dropdownRef.value?.handleOpen()
}
const handleMenuCommand = (command) => {
  switch (command) {
    case 'hide': hidePet(); break
    case 'switch': switchPet(); break
    case 'question': showAiInput.value = true; break
    case 'quiz': petStore.generateAndShowQuiz(); break
    case 'startAutoQuiz': petStore.startAutoQuiz(); break
    case 'stopAutoQuiz': petStore.stopAutoQuiz(); break
  }
}


const handleConfirm = (item) => { if (item.onConfirm) item.onConfirm(); petStore.removeMessage(item.id) }
const handleCancel = (item) => { if (item.onCancel) item.onCancel(); petStore.removeMessage(item.id) }

const submitAnswer = (message, userAnswer) => {
  let correctAnswer = message.correctAnswer
  if (message.questionType === 'judge') {
    correctAnswer = typeof correctAnswer === 'string' ? correctAnswer.toLowerCase() === 'true' : Boolean(correctAnswer)
  }
  message.userAnswer = userAnswer
  message.isCorrect = userAnswer === correctAnswer
  message.remainingTime = 8
  if (message.questionType === 'choice') {
    message.correctIndex = message.options.findIndex(option => option.charAt(0) === message.correctAnswer)
  }
  const countdownTimer = setInterval(() => {
    message.remainingTime--
    if (message.remainingTime <= 0) {
      clearInterval(countdownTimer)
      message.isClosing = true
      setTimeout(() => { petStore.removeMessage(message.id) }, 1500)
    }
  }, 1000)
  message.countdownTimer = countdownTimer
}

const updateBubblePositions = () => {
  if (!bubbleRefs.value.length) return
  const bottoms = []
  const isMobile = window.innerWidth <= 768
  let currentBottom = isMobile ? 10 : 60 // 起始高度
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const el = bubbleRefs.value[i]
    if (!el) continue
    const bubbleHeight = el.offsetHeight + BUBBLE_GAP
    bottoms[i] = currentBottom
    currentBottom += bubbleHeight
  }
  computedBottoms.value = bottoms
}
provide('addMessage', petStore.addMessage)

const getBubbleTexts = () => {
  const path = route.path
  const name = userInfo.value.name || '书友'
  const weather = currentWeather.value || '晴'
  const isNightMode = isDark.value
  let weatherTip = '闲暇时刻，不要忘记阅读哦'
  if (weather.includes('雨')) weatherTip = '外面下雨啦，窝在室内看书最舒服～'
  else if (weather.includes('雪')) weatherTip = '落雪天适合静心品读好书✨'
  else if (weather.includes('风')) weatherTip = '大风天不宜外出，读本书放松一下吧'
  else if (weather.includes('晴')) weatherTip = '阳光正好，劳逸结合读会儿书吧'

  const darkTip = isNightMode ? '夜间阅读记得适当放松眼睛哦🌙' : '光线充足，放心阅读啦'

  if (path === '/login') return ['点击我获取登录引导哦~', '登录后即可解锁海量图书资源']
  if (path.includes('/book/reader')) return [weatherTip, darkTip, '遇到好句可以随手记录笔记哦']
  if (path === '/home' || path === '/') return [`欢迎回来${name}！${weatherTip}`, '首页可以检索全网馆藏图书']
  return [weatherTip, darkTip, '读书是最低成本的自我提升✨', '碎片化时间也可以抽空阅读']
}

const startAutoBubble = () => {
  setInterval(() => {
    const texts = getBubbleTexts()
    petStore.addMessage(texts[Math.floor(Math.random() * texts.length)])
  }, 120000)
}

const closeAiInput = () => { showAiInput.value = false; aiQuestion.value = ''; aiLoading.value = false }

// ====================== 🔥 对话式气泡：向AI发起第二轮回击 ======================
const sendTalkRebuttal = async (item) => {
  const userRebuttalText = item.userReply?.trim()
  if (!userRebuttalText) return ElMessage.warning('回击内容不能为空哦！')

  const targetMsgId = item.id
  // 1. 极其丝滑：立刻把当前这个旧气泡从屏幕抹去，保持视野清爽
  petStore.removeMessage(targetMsgId)

  // 2. 挂上小猫思考中状态
  petStore.addMessage('😼正在接招并重新组织反驳...', petStore.MESSAGE_TYPES.THINKING)

  try {
    // 3. 再次向 debater 发起 POST 请求！
    await request.post('/pet/ai/ai_debater', {
      content: userRebuttalText,
      userId: userStore.userId,
      isbn: userStore.currentReadingIsbn,
      chapterNumber: userStore.currentReadingChapter
    })
  } catch (err) {
    petStore.clearThinkingMessages()
    petStore.addMessage('😥 辩论者暂时去喝水了，请稍后再试')
  }
}

const sendAiMsg = async () => {
  if (isAuthPage.value) return
  const question = aiQuestion.value.trim()
  if (!question) { ElMessage.warning('请输入你想了解的功能~'); return }

  showAiInput.value = false
  aiLoading.value = true // 防止重复点击

  // ✅ 发送前立刻显示思考气泡，给用户即时反馈
  petStore.addMessage('😾正在思考中...', petStore.MESSAGE_TYPES.THINKING)

  try {
    const localGuide = matchUserQuestion(question)
    if (localGuide) petStore.addMessage('✅ 功能引导：\n' + localGuide)

    await request.post('/pet/ai/chat', {
      content: question,
      userId: userStore.userId
    })
    // 这里不做任何气泡操作，文字完全交给 SSE 流式推送

  } catch (err) {
    // ❌ 请求失败：手动清掉思考气泡，再显示错误
    petStore.clearThinkingMessages()
    petStore.addMessage('😥 信号飘走了，你可以直接问我功能关键词哦~')
  } finally {
    aiLoading.value = false
    closeAiInput()
    // ⚠️ 注意：绝对不能在这里调用 clearThinkingMessages()
    // 否则请求刚发出去，思考气泡就被立刻清掉，等于白加
  }
}
provide('sendAiMsg', sendAiMsg)
watch(messages, () => nextTick(() => updateBubblePositions()), { deep: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  const savedPos = localStorage.getItem('petPosition')
  if (savedPos) {
    const parsed = JSON.parse(savedPos)
    // 兼容老版本的 right 缓存，如果没有 left 则使用默认隐藏
    const initLeft = parsed.left !== undefined ? parsed.left : (window.innerWidth <= 768 ? -30 : -75)
    applyBoundary(initLeft, parsed.bottom || 20)
    snapPetToEdge() // 初始化检查贴边
  } else {
    // 默认初始靠左隐藏
    leftPos.value = window.innerWidth <= 768 ? -30 : -75
    bottomPos.value = 20
  }
  startAutoBubble()
  setTimeout(() => petStore.addMessage('📅 今日阅读日签：\n' + petStore.generateDailyGreeting()), 3000)
  document.addEventListener('click', () => dropdownRef.value?.handleClose())
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', () => dropdownRef.value?.handleClose())
})
</script>

<style scoped>
/* ====================== 宠物本体样式 ====================== */
.pet-widget {
  position: fixed;
  z-index: 150;
  cursor: grab;
  /* 加入位置与不透明度的过渡效果 */
  transition: left 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), bottom 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s;
  user-select: none;
  touch-action: none; /* 防止移动端原生滚动干扰拖拽 */
}

/* 核心修复：拖拽时关闭所有物理补间动画，彻底解决拖拽卡顿失灵 */
.pet-widget.is-dragging, .bubble-track-layer.is-dragging {
  transition: none !important;
  cursor: grabbing;
}

.pet-widget:not(.is-half-hidden):hover { transform: scale(1.03); }

/* 半隐藏状态样式 */
.pet-widget.is-half-hidden {
  opacity: 0.6;
}
.pet-widget.is-half-hidden:hover {
  opacity: 1;
  transform: translateX(5px); /* 探头暗示 */
}

/* 🔥 修改后 */
.pet-image {
  width: 150px !important;
  height: auto;
  border-radius: 12px;
  filter: drop-shadow(0 3px 10px rgba(0, 0, 0, 0.2));
  /* 加上 transform 的过渡，当小宠物跨越屏幕中线时，会有一个丝滑的“转身”动画 */
  transition: filter 0.3s ease, transform 0.4s ease;
}

/* 🆕 新增：纯 CSS 水平镜像反转 */
.pet-flipped {
  transform: scaleX(-1);
}

.pet-paused {
  animation-play-state: paused !important;
  pointer-events: none;
}

:root.dark .pet-image {
  /* 黑夜模式下让小狗图片稍微融入环境 */
  filter: drop-shadow(0 3px 10px rgba(0, 0, 0, 0.4)) brightness(0.85);
}

/* ====================== 气泡追踪层 ====================== */
.bubble-track-layer {
  position: fixed;
  z-index: 99999;
  pointer-events: none;
  transition: left 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), bottom 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* ====================== 气泡基础样式与方向适配 ====================== */
.pet-bubble {
  position: absolute;
  /* 默认：宠物在左，气泡显示在宠物的右侧 (150宽 + 20间距) */
  left: 170px;
  right: auto;
  bottom: 0;

  width: max-content;
  max-width: 320px;
  pointer-events: auto;
  padding: 10px 14px;
  background: var(--glass-bg, rgba(255, 255, 255, 0.65));
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.5));
  box-shadow: var(--glass-shadow, 0 8px 32px 0 rgba(31, 38, 135, 0.05));
  color: var(--el-text-color-primary);
  border-radius: 16px;
  font-size: 13px;
  text-align: left;
  white-space: pre-line;
  word-break: break-word;
  transition: left 0.4s ease, right 0.4s ease, bottom 0.3s ease;
}

/* 当宠物被拖到右侧屏幕时，气泡切换到宠物左侧显示 */
.pet-bubble.bubble-pos-left {
  left: auto;
  /* 右边缘距离追踪层原点10px，形成间隙 */
  right: 10px;
}

/* ====================== 气泡动效 ====================== */
.bubble-chat { animation: bubbleFloatFade 5s ease; }

@keyframes bubbleFloatFade {
  0% { opacity: 0; transform: translateY(12px); }
  20% { opacity: 1; transform: translateY(0); }
  80% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-30px); }
}

@keyframes bubbleExitUp {
  0% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-30px); }
}

.pet-bubble.closing {
  animation: bubbleExitUp 1.5s ease forwards !important;
}

/* 思考点动效 */
.thinking-content { display: flex; align-items: center; gap: 8px; }
.thinking-dot {
  width: 3px; height: 3px; background: black; border-radius: 50%;
  animation: thinkingBounce 1.4s infinite ease-in-out both;
}
.thinking-dot:nth-child(1) { animation-delay: -0.32s; }
.thinking-dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes thinkingBounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
html.dark .thinking-dot { background: rgba(255, 255, 255, 0.85); }

/* 互动答题气泡内部样式 */
.bubble-interaction {
  max-width: 420px !important;
  min-width: 280px;
  padding: 16px !important;
}
.interaction-bubble .close-btn {
  position: absolute; top: 8px; right: 12px; font-size: 18px;
  cursor: pointer; color: var(--el-text-color-secondary); transition: color 0.2s; line-height: 1;
}
.interaction-bubble .close-btn:hover { color: var(--el-color-danger); }
.question-title { font-weight: 600; margin-bottom: 16px; padding-right: 24px; font-size: 14px; }
.answer-area { margin-bottom: 8px; display: flex; flex-wrap: wrap; gap: 8px; }
.answer-area .el-button { background: var(--el-bg-color-page); border: 1px solid var(--el-border-color); color: var(--el-text-color-primary); }
.answer-area .el-button:hover { background: var(--el-color-primary-light-9); border-color: var(--el-color-primary); color: var(--el-color-primary); }
.result-area .el-button { pointer-events: none; opacity: 0.7; }
.result-area .el-button.correct { background: var(--el-color-success-light-9); border-color: var(--el-color-success); color: var(--el-color-success); }
.result-area .el-button.wrong { background: var(--el-color-danger-light-9); border-color: var(--el-color-danger); color: var(--el-color-danger); }
.result-text { font-weight: 600; margin-bottom: 8px; font-size: 14px; }
.result-text.correct { color: var(--el-color-success); }
.result-text.wrong { color: var(--el-color-danger); }
.analysis-text { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.6; padding: 8px; background: rgba(0, 0, 0, 0.04); border-radius: 8px; margin-top: 8px; }
html.dark .analysis-text { background: rgba(255, 255, 255, 0.06); }
.auto-close-tip { display: block; text-align: right; margin-top: 4px; color: var(--el-text-color-placeholder); font-size: 11px; }

/* 确认气泡 */
.confirm-bubble { padding: 8px 12px; text-align: center; }
.confirm-text { margin-bottom: 6px; font-weight: 500; }
.confirm-buttons { display: flex; gap: 10px; justify-content: center; }

/* Static 与 Talk 长文气泡容器规范 */
.static-bubble-box, .talk-bubble-box {
  position: relative;
  min-width: 260px;
  max-width: 380px;
  padding: 14px 16px 12px;
  line-height: 1.6;
}

.static-bubble-box .close-btn, .talk-bubble-box .close-btn {
  position: absolute;
  top: 6px; right: 10px;
  font-size: 18px; color: #999; cursor: pointer;
}
.static-bubble-box .close-btn:hover, .talk-bubble-box .close-btn:hover {
  color: #ff4d4f;
}

.long-text-content {
  text-align: justify;
  margin-bottom: 8px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 6px;
  white-space: pre-wrap;
}

.talk-reply-bar {
  margin-top: 12px;
  border-top: 1px dashed rgba(0, 0, 0, 0.1);
  padding-top: 10px;
}
html.dark .talk-reply-bar { border-top-color: rgba(255, 255, 255, 0.1); }

/* ====================== 📱 移动端极限响应式适配 ====================== */
@media (max-width: 768px) {
  .pet-image {
    /* 移动端宠物显著缩小，避免占屏 */
    width: 60px !important;
  }

  .pet-bubble {
    /* 移动端气泡字号、内间距全量缩小 */
    font-size: 12px !important;
    padding: 8px 12px !important;
    max-width: 220px !important;

    /* 移动端气泡位置向左挤压，紧贴宠物 */
    left: 70px !important;
  }

  /* 宠物在右半屏时，气泡的适配 */
  .pet-bubble.bubble-pos-left {
    left: auto !important;
    right: 10px !important;
  }

  /* 缩小互动答题卡片 */
  .bubble-interaction {
    max-width: 260px !important;
    min-width: 200px !important;
    padding: 12px !important;
  }
  .question-title { font-size: 13px !important; margin-bottom: 10px !important; }
  .answer-area .el-button { padding: 5px 10px !important; font-size: 12px !important; }

  /* 调整弹窗占屏比 */
  :deep(.mobile-dialog) {
    width: 90vw !important;
    max-width: 360px !important;
  }
}
</style>