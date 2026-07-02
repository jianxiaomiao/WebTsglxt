<template>
  <div class="co-read-capsule glass-panel" @click="openRoomDrawer" v-if="!isAuthPage">

    <span v-if="unreadCount > 0" class="unread-badge">
      {{ unreadCount > 99 ? '99+' : unreadCount }}
    </span>

    <transition name="bubble-pop">
      <div v-if="showPreviewToast" class="msg-preview-toast glass-panel" @click.stop="openRoomDrawer">
        <div class="toast-sender">{{ previewData.user }}</div>
        <div class="toast-content">{{ previewData.text }}</div>
      </div>
    </transition>

    <span class="pulse-dot" :class="roomState === 'INSIDE' ? 'status-active' : 'status-idle'"></span>
    <span class="capsule-text">
      <template v-if="roomState === 'INSIDE'">
        📚 房间 #{{ currentRoom.id }}
        <span class="capsule-user-badge">👥 {{ onlineCount }}人</span>
      </template>
      <template v-else>☕ 书籍共读舱</template>
    </span>
  </div>

  <el-drawer
      v-model="showDrawer"
      :title="drawerTitle"
      :size="drawerWidth + 'px'"
      :append-to-body="true"
      class="co-read-drawer glass-panel"
      @close="handleDrawerClose"
  >
    <div
        class="drawer-resizer-handle"
        :class="{ 'is-resizing': isDraggingWidth }"
        @mousedown.prevent="startDragResize"
        title="按住拖拽调整宽度"
    ></div>
    <div v-if="roomState === 'IDLE'" class="idle-bento-box">
      <div class="bento-card create-card glass-panel" @click="roomState = 'CREATING'">
        <h3>✨ 创建共读房间</h3>
        <p>挑选藏书，做思想的领航者</p>
        <el-button type="primary" size="small" round>发起共读</el-button>
      </div>

      <div class="bento-card join-card glass-panel" @click="roomState = 'JOINING'">
        <h3>🔍 加入已有房间</h3>
        <p>输入密令或书名，寻找同频共振</p>
        <el-button type="success" size="small" round>查找房间</el-button>
      </div>
    </div>

    <div v-else-if="roomState === 'CREATING'" class="creating-box">
      <el-tabs v-model="activeBookTab">
        <el-tab-pane label="从书架挑选" name="bookshelf">
          <el-scrollbar height="200px">
            <div class="mini-book-grid">
              <div
                  v-for="book in myCollections"
                  :key="book.isbn"
                  class="mini-book-item glass-panel"
                  :class="{'is-selected': createForm.isbn === (book.isbn || book.iSBN)}"
                  @click="selectBookForCreate(book)"
              >
                <span class="book-title-txt">
                  {{ book.bookName || book.bookname || book.isbn || book.iSBN || '神秘藏书' }}
                </span>
              </div>
            </div>
          </el-scrollbar>
        </el-tab-pane>

        <el-tab-pane label="全网联想" name="search">
          <el-autocomplete
              v-model="searchKey"
              :fetch-suggestions="fetchSuggestions"
              placeholder="输入书名联想..."
              value-key="bookname"
              @select="(item) => selectBookForCreate(item)"
              style="width: 100%; margin-top: 10px;"
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="createForm.bookName" class="selected-preview glass-panel">
        已选：<strong>《{{ createForm.bookName }}》</strong>
      </div>

      <div class="form-switch-row">
        <span>房间性质</span>
        <el-switch v-model="createForm.isPublic" active-text="公开" inactive-text="私密" />
      </div>

      <div class="drawer-footer-btns">
        <el-button @click="roomState = 'IDLE'" round size="small">返回</el-button>
        <el-button type="primary" :disabled="!createForm.isbn" @click="handleDoCreateRoom" round size="small">
          立即创建并进入 🚀
        </el-button>
      </div>
    </div>

    <div v-else-if="roomState === 'JOINING'" class="joining-box">
      <div class="search-bar-group">
        <el-select v-model="joinSearchType" style="width: 105px;">
          <el-option label="房间 ID" value="roomId" />
          <el-option label="房主 ID" value="userId" />
          <el-option label="书名" value="bookName" />
        </el-select>

        <el-autocomplete
            v-if="joinSearchType === 'bookName'"
            v-model="joinSearchKeyword"
            :fetch-suggestions="fetchSuggestions"
            placeholder="输入书名联想..."
            value-key="bookname"
            @select="(item) => doSearchRoomsByBook(item.bookname)"
            style="flex: 1;"
        />
        <el-input v-else v-model="joinSearchKeyword" placeholder="输入搜索词..." @keyup.enter="doSearchRoomsGeneral" style="flex: 1;" />
      </div>
      <el-button type="success" @click="doSearchRoomsGeneral" style="width:100%;" round size="small">检索房间 🔍</el-button>

      <el-scrollbar height="300px">
        <div v-loading="searchingRooms" class="room-result-list">
          <div v-for="rm in searchResultRooms" :key="rm.id" class="room-item-card glass-panel">
            <div class="rm-info">
              <div class="rm-id">房间 #{{ rm.id }} <el-tag size="small" type="info">{{rm.isPublic ? '公开' : '私密'}}</el-tag></div>
              <div class="rm-book">《{{ rm.bookName || '神秘名著' }}》</div>
              <div class="rm-host">房主: {{ rm.userId }}</div>
            </div>
            <el-button type="primary" size="small" @click="joinAndEnterRoom(rm)" round>进入</el-button>
          </div>
          <el-empty v-if="searchResultRooms.length===0" description="未检索到房间" :image-size="50" />
        </div>
      </el-scrollbar>

      <el-button @click="roomState = 'IDLE'" round size="small" style="width:100%; margin-top:10px;">返回上一步</el-button>
    </div>

    <div v-else-if="roomState === 'INSIDE'" class="room-inside-stage">

      <div class="room-header-banner glass-panel">
        <div class="header-main-row">
          <div class="book-title-meta">正在读: <strong>《{{ currentRoom.bookName }}》</strong></div>
          <el-button type="danger" size="small" link @click="exitCurrentRoom">退出房间</el-button>
        </div>

        <div class="online-members-corridor">
          <span class="corridor-label">在线 ({{ onlineCount }})</span>
          <el-scrollbar>
            <div class="members-avatar-track">
              <div v-for="uid in onlineUsersList" :key="uid" class="member-avatar-box" :title="uid">
                <span class="host-crown" v-if="uid === currentRoom.userId">👑</span>
                <div class="avatar-circle glass-panel" :class="{'is-me-avatar': uid === currentUserId}"
                     @click="router.push('/user/profile?userId=' + uid)">
                  {{ uid.slice(0, 2).toUpperCase() }}
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <el-scrollbar ref="chatScrollbarRef" class="chat-msg-container">
        <div class="msg-list">
          <div v-for="(msg, idx) in chatList" :key="idx" class="msg-item-wrapper">

            <div v-if="!msg.type || msg.type === 'CHAT'" class="msg-bubble" :class="msg.userId === currentUserId ? 'is-me' : 'is-other'">
              <span class="sender-name"  @click="router.push('/user/profile?userId=' + msg.userId)">{{ msg.userId }}</span>
              <div class="bubble-body glass-panel">{{ msg.content }}</div>
            </div>

            <div v-else-if="msg.type === 'NOTE_CARD'" class="literary-note-card glass-panel" :class="msg.userId === currentUserId ? 'note-is-me' : 'note-is-other'">
              <div class="washi-tape"></div>

              <div class="note-card-header">
                <span class="note-author">🔖 {{ msg.userId }} 的读书书摘</span>
                <span class="note-time">{{ formatMsgTime(msg.timestamp) }}</span>
              </div>

              <div class="excerpt-box">
                <span class="quote-watermark">“</span>
                <p class="excerpt-text">{{ msg.excerpt }}</p>
              </div>

              <div class="note-divider"></div>

              <div class="user-comment-box">
                <p>{{ msg.content }}</p>
              </div>
            </div>

          </div>
        </div>
      </el-scrollbar>

      <div class="chat-input-area">
        <el-input v-model="chatInputText" placeholder="聊聊见解..." @keyup.enter="sendChatMessage">
          <template #prefix>
            <el-button link type="primary" @click="showExcerptModal = true" title="发送精致书摘卡片" style="font-weight:bold;">
              🔖 书摘
            </el-button>
          </template>
          <template #append>
            <el-button type="primary" @click="sendChatMessage">发送</el-button>
          </template>
        </el-input>
      </div>
    </div>
  </el-drawer>

  <el-dialog v-model="showExcerptModal" title="分享手账书摘卡片" width="340px" append-to-body class="excerpt-dialog glass-panel">
    <div class="excerpt-form">
      <label>原文摘录 (Excerpt)</label>
      <el-input v-model="excerptForm.excerpt" type="textarea" :rows="3" placeholder="复制书里让你触动的一段话..." />

      <label style="margin-top:12px; display:block;">我的感悟 (Thoughts)</label>
      <el-input v-model="excerptForm.content" type="textarea" :rows="2" placeholder="写下你的见解..." />
    </div>
    <template #footer>
      <el-button @click="showExcerptModal = false" size="small" round>取消</el-button>
      <el-button type="primary" :disabled="!excerptForm.excerpt.trim()" @click="sendNoteCardMessage" size="small" round>
        贴上墙 📌
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, inject, onMounted, nextTick, onBeforeUnmount } from 'vue'
import request from "../utils/request.js";
import {createRouter as $router, useRoute, useRouter} from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from "../stores/userStore.js";

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userId || '')
const isDark = inject('isDark')

const showDrawer = ref(false)
const roomState = ref('IDLE')
const currentRoom = ref(null)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

// ====================== 🔥 新增：抽屉无极缩放引擎 ======================
const drawerWidth = ref(userStore.coReadDrawerWidth || 380)
const isDraggingWidth = ref(false)

// 拖拽开始（按下鼠标）
const startDragResize = () => {
  isDraggingWidth.value = true
  document.body.style.cursor = 'ew-resize' // 锁死全局鼠标指针为左右箭头
  document.body.style.userSelect = 'none'  // 禁止拖拽时意外选中书本上的文字

  window.addEventListener('mousemove', handleDragMove)
  window.addEventListener('mouseup', stopDragResize)
}

// 拖拽中计算公式
const handleDragMove = (e) => {
  if (!isDraggingWidth.value) return

  // 📐 核心几何运算：因为抽屉贴在屏幕最右侧，
  // 鼠标当前的 X 坐标越小（越往左拉），抽屉的宽度反而应该越大！
  // 公式：新宽度 = 浏览器可视窗口宽度 - 鼠标当前实时的 X 坐标
  const calculatedWidth = window.innerWidth - e.clientX

  // 边界锁死：最窄不低于 280px（保证头像不挤爆），最宽不超过 640px（不挡住正文）
  const clampedWidth = Math.max(280, Math.min(640, calculatedWidth))

  drawerWidth.value = clampedWidth
}

// 拖拽结束（松开鼠标）
const stopDragResize = () => {
  if (!isDraggingWidth.value) return

  isDraggingWidth.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''

  window.removeEventListener('mousemove', handleDragMove)
  window.removeEventListener('mouseup', stopDragResize)

  // 🌟 落地到 Store 和 localStorage 永久保存！
  userStore.updateCoReadWidth(drawerWidth.value)
  console.log(`[共读舱无极轨] 抽屉新宽度 ${drawerWidth.value}px 已永久固化`)
}

// ====================== 🌟 新增：未读气泡引擎变量 ======================
const unreadCount = ref(0)
const showPreviewToast = ref(false)
const previewData = ref({ user: '', text: '' })
let previewTimer = null // 防抖计时器凭证

// 👥 在线人员名单追踪池
const onlineUsersSet = ref(new Set())
const onlineUsersList = computed(() => Array.from(onlineUsersSet.value))
const onlineCount = computed(() => Math.max(1, onlineUsersList.value.length))

const drawerTitle = computed(() => {
  if (roomState.value === 'CREATING') return '发起新共读'
  if (roomState.value === 'JOINING') return '加入共读房间'
  if (roomState.value === 'INSIDE') return `共读房间 #${currentRoom.value?.id}`
  return '思想共读广场'
})

// 升级打开抽屉方法
const openRoomDrawer = () => {
  showDrawer.value = true
  unreadCount.value = 0 // 🌟 归零未读数
  showPreviewToast.value = false // 🌟 强行收起头顶气泡
  if (previewTimer) clearTimeout(previewTimer)

  nextTick(() => { chatScrollbarRef.value?.setScrollTop(99999) })
}

const handleDrawerClose = () => {}
// ================== 1. 创建房间 ==================
const activeBookTab = ref('bookshelf')
const myCollections = ref([])
const searchKey = ref('')
const createForm = ref({ isbn: '', bookName: '', isPublic: true })

const selectBookForCreate = (book) => {
  createForm.value.isbn = book.isbn || book.iSBN
  createForm.value.bookName = book.bookname || book.bookName || '未知书名'
  ElMessage.success(`选择了《${createForm.value.bookName}》`)
}

const fetchSuggestions = async (queryString, callback) => {
  if (!queryString.trim()) return callback([])
  try {
    const res = await request.get('/book', { params: { suggest: true, keyword: queryString.trim(), searchType: 'bookName' } })
    callback(res.data || [])
  } catch (err) { callback([]) }
}

const handleDoCreateRoom = async () => {
  if (!currentUserId.value) return ElMessage.warning("请先登录账号！");
  if (!createForm.value.isbn) return ElMessage.warning("请选择书籍");

  try {
    const rmPayload = {
      userId: currentUserId.value,
      isbn: createForm.value.isbn,
      isPublic: createForm.value.isPublic ? 1 : 0,
      bookName: createForm.value.bookName
    };
    const res = await request.post('/book/room', rmPayload);
    if (res.code === 200) {
      ElMessage.success('创建成功！');
      const createdRm = res.data || { id: 999, ...rmPayload };
      joinAndEnterRoom(createdRm);
    }
  } catch (err) { ElMessage.error('创建失败'); }
};

// ================== 2. 搜索房间 ==================
const joinSearchType = ref('roomId')
const joinSearchKeyword = ref('')
const searchingRooms = ref(false)
const searchResultRooms = ref([])

const doSearchRoomsByBook = (bookName) => {
  joinSearchKeyword.value = bookName; doSearchRoomsGeneral()
}

const doSearchRoomsGeneral = async () => {
  if (!joinSearchKeyword.value.trim()) return ElMessage.warning('请输入搜索词')
  searchingRooms.value = true
  try {
    const params = { [joinSearchType.value]: joinSearchKeyword.value.trim() }
    const res = await request.get('/book/room', { params })
    if (Array.isArray(res.data)) searchResultRooms.value = res.data
    else if (res.data?.list) searchResultRooms.value = res.data.list
    else if (res.data?.id) searchResultRooms.value = [res.data]
    else searchResultRooms.value = []
  } catch (e) { searchResultRooms.value = [] }
  finally { searchingRooms.value = false }
}

// ================== 3. 聊天室内部引擎 ==================
const chatList = ref([])
const chatInputText = ref('')
let eventSourceInstance = null
const chatScrollbarRef = ref(null)

const showExcerptModal = ref(false)
const excerptForm = ref({ excerpt: '', content: '' })

const joinAndEnterRoom = (rmObj) => {
  currentRoom.value = rmObj
  roomState.value = 'INSIDE'
  chatList.value = []
  onlineUsersSet.value = new Set([currentUserId.value]) // 默认把自己加进去

  if (currentUserId.value) {
    sessionStorage.setItem(`co_room_token_${currentUserId.value}`, JSON.stringify(rmObj))
  }
  initRoomSse(rmObj.id)
}

const initRoomSse = (roomId) => {
  if (eventSourceInstance) eventSourceInstance.close()

  const sseUrl = `${import.meta.env.VITE_BASE_API || ''}/api/book/room/sse?roomId=${roomId}&userId=${currentUserId.value}`
  eventSourceInstance = new EventSource(sseUrl)

  eventSourceInstance.onmessage = (event) => {
    const incoming = JSON.parse(event.data)

    if (incoming.type === 'ROOM_META') {
      const newUsersSet = new Set(incoming.onlineUsers || [])

      // 1. 集合运算：用“新名单”过滤出不在“旧名单”里、且“不是我自己”的人
      const newComers = [...newUsersSet].filter(
          uid => !onlineUsersSet.value.has(uid) && uid !== currentUserId.value
      )

      // 2. 覆盖更新本地大名单
      onlineUsersSet.value = newUsersSet

      // 3. 触发推门迎客动作！
      if (newComers.length > 0) {
        const freshUser = newComers[0] // 拿到刚进来的书友ID

        if (!showDrawer.value) {
          // 【轨道 A：抽屉关着】 -> 顶出头顶的毛玻璃气泡
          previewData.value = {
            user: '🔔 门铃微响',
            text: `书友 ${freshUser} 轻轻推门坐下了`
          }
          showPreviewToast.value = true

          if (previewTimer) clearTimeout(previewTimer)
          previewTimer = setTimeout(() => { showPreviewToast.value = false }, 3500)

        } else {
          // 【轨道 B：抽屉开着】 -> 直接在聊天流里印一条静默系统提示！
          chatList.value.push({
            type: 'SYSTEM',
            content: `书友 ${freshUser} 悄悄推门进来了 ☕`,
            timestamp: Date.now()
          })
          nextTick(() => { chatScrollbarRef.value?.setScrollTop(99999) })
        }
      }
      return;
    }

    // 🌟 核心修改：如果是“我自己发出的实时回音”【且不是云端恢复的历史包】，才拦截！
    if (incoming.userId === currentUserId.value && !incoming.isHistory) {
      return;
    }

    chatList.value.push(incoming)
    // 🔥 极其核心的分判：如果【抽屉关着】且【来的不是历史漫游包】，触发未读气泡！
    if (!showDrawer.value && !incoming.isHistory) {
      unreadCount.value++ // 未读角标 +1

      // 1. 提取安全的预览摘要
      let snippet = ''
      if (incoming.type === 'NOTE_CARD') {
        snippet = `🔖 分享了一页手账书摘`
      } else {
        const raw = incoming.content || ''
        snippet = raw.length > 15 ? raw.slice(0, 15) + '...' : raw
      }

      previewData.value = { user: incoming.userId, text: snippet }
      showPreviewToast.value = true

      // 2. 气泡续命防抖：先掐死上一个倒计时，重新开启 3.5 秒自毁！
      if (previewTimer) clearTimeout(previewTimer)
      previewTimer = setTimeout(() => {
        showPreviewToast.value = false
      }, 3500)

    } else {
      // 如果抽屉开着，直接静默滚到底部，不弹未读
      nextTick(() => { chatScrollbarRef.value?.setScrollTop(99999) })
    }
  }
}

// 发送普通聊天
const sendChatMessage = async () => {
  const text = chatInputText.value.trim(); if (!text) return;
  const optMsg = { type: 'CHAT', userId: currentUserId.value, content: text, timestamp: Date.now() }
  chatList.value.push(optMsg)
  chatInputText.value = ''; nextTick(() => { chatScrollbarRef.value?.setScrollTop(99999) })

  await request.post('/book/room/chat', { roomId: currentRoom.value.id, userId: currentUserId.value, content: text })
}

// 🔥 发送特殊卡片：和纸手账书摘
const sendNoteCardMessage = async () => {
  const excerptTxt = excerptForm.value.excerpt.trim()
  const contentTxt = excerptForm.value.content.trim()
  if (!excerptTxt) return;

  const notePayload = {
    type: 'NOTE_CARD',
    userId: currentUserId.value,
    excerpt: excerptTxt,
    content: contentTxt || '分享了一段触动心弦的文字',
    timestamp: Date.now()
  }

  // 1. 本地上墙
  chatList.value.push(notePayload)
  showExcerptModal.value = false
  excerptForm.value = { excerpt: '', content: '' }
  nextTick(() => { chatScrollbarRef.value?.setScrollTop(99999) })

  // 2. 借用 chat 接口，把 payload 转成 string 广播出去
  try {
    await request.post('/book/room/chat', {
      roomId: currentRoom.value.id,
      userId: currentUserId.value,
      content: JSON.stringify(notePayload) // 包装成特殊载荷下发
    })
  } catch(e) {}
}

const exitCurrentRoom = () => {
  ElMessageBox.confirm('确认退出房间吗？', '提示').then(() => {
    if (eventSourceInstance) { eventSourceInstance.close(); eventSourceInstance = null }
    roomState.value = 'IDLE'; currentRoom.value = null
    if (currentUserId.value) sessionStorage.removeItem(`co_room_token_${currentUserId.value}`)
  }).catch(()=>{})
}

const formatMsgTime = (ts) => {
  if(!ts) return ''; const d = new Date(ts);
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

// 🌟 工业级防御：利用 keepalive 信标，在浏览器标签页被直接 X 掉时强行上报退房
const handleEmergencyTeardown = () => {
  if (roomState.value === 'INSIDE' && currentRoom.value && currentUserId.value) {
    // ⚠️ 注意：绝对不能用普通的 axios 或 request.post！必须用原生的 keepalive fetch！
    const emergencyExitUrl = `${import.meta.env.VITE_BASE_API || ''}/api/book/room/chat`

    // 我们伪造一个特殊的退出载荷发给发信接口，让它转交 SSE 广播并清理
    const teardownPayload = JSON.stringify({
      roomId: currentRoom.value.id,
      userId: currentUserId.value,
      content: '【系统提示：该书友合上书本离开了房间】'
    })

    fetch(emergencyExitUrl, {
      method: 'POST',
      body: teardownPayload,
      headers: { 'Content-Type': 'application/json' },
      keepalive: true // 🔥 灵魂所在：移交操作系统后台发射！
    })
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleEmergencyTeardown)
  if (currentUserId.value) {
    request.get('/user/collection', { params: { userId: currentUserId.value, page: 1, pageSize: 50 } })
        .then(res => { myCollections.value = (res.data?.list || []).filter(item => item?.isbn) })

    const activeRm = sessionStorage.getItem(`co_room_token_${currentUserId.value}`)
    if (activeRm) { try { joinAndEnterRoom(JSON.parse(activeRm)) } catch(e){} }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleEmergencyTeardown)
  if (eventSourceInstance) eventSourceInstance.close() })
</script>

<style scoped>
/* 悬浮小胶囊 */
.co-read-capsule {
  position: fixed; bottom: 24px; right: 24px; z-index: 1999; display: flex; align-items: center; gap: 10px; padding: 8px 18px; border-radius: 25px; cursor: pointer; box-shadow: 0 8px 24px rgba(0,0,0,0.15); transition: all 0.3s;
}
.co-read-capsule:hover { transform: translateY(-3px) scale(1.05); }
.pulse-dot { width: 10px; height: 10px; border-radius: 50%; }
.status-idle { background: #909399; }
.status-active { background: #67c23a; box-shadow: 0 0 8px #67c23a; animation: pulse 2s infinite; }
.capsule-user-badge { font-size:11px; background:rgba(100,181,246,0.25); color:#409eff; padding:2px 6px; border-radius:10px; margin-left:4px; font-weight:bold; }

/* Bento 风格选择卡片 */
.idle-bento-box { display: flex; flex-direction: column; gap: 16px; padding: 10px; }
.bento-card { padding: 24px; border-radius: 16px; text-align: left; cursor: pointer; transition: transform 0.2s; display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.bento-card:hover { transform: translateY(-3px); }
.create-card { border-left: 5px solid #409eff; }
.join-card { border-left: 5px solid #67c23a; }
.bento-card h3 { margin:0; font-size: 16px; }
.bento-card p { margin:0; font-size: 12px; color: #666; margin-bottom: 10px; }

/* 创建表单 */
.creating-box, .joining-box { padding: 0 10px; text-align: left; }
.mini-book-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding-right: 10px; }
.mini-book-item { padding: 10px; border-radius: 8px; cursor: pointer; text-align: center; border: 1px solid transparent; transition: all 0.2s; font-size: 12px;}
.mini-book-item.is-selected { border-color: #409eff; background: rgba(64,158,255,0.1) !important; font-weight: bold; }
.selected-preview { margin: 15px 0; padding: 10px; border-radius: 8px; background: rgba(0,0,0,0.03); font-size: 13px; }
.form-switch-row { display: flex; justify-content: space-between; align-items: center; margin: 15px 0; font-size: 14px; }
.drawer-footer-btns { display: flex; justify-content: space-between; gap: 10px; margin-top: 25px; }

/* 搜索房间 */
.search-bar-group { display: flex; gap: 8px; margin-bottom: 15px; }
.room-result-list { display: flex; flex-direction: column; gap: 10px; padding-right: 10px; }
.room-item-card { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-radius: 12px; }
.rm-id { font-size: 12px; font-weight: bold; }
.rm-book { font-size: 14px; color: #409eff; margin: 4px 0; }
.rm-host { font-size: 11px; color: #888; }

/* ================= 正式房间内部 ================= */
.room-inside-stage { display: flex; flex-direction: column; height: calc(100vh - 80px); }
.room-header-banner { display: flex; flex-direction: column; gap: 8px; padding: 12px 15px; border-radius: 12px; margin-bottom: 10px; }
.header-main-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }

/* 成员长廊 */
.online-members-corridor { display: flex; align-items: center; gap: 10px; border-top: 1px solid rgba(150,150,150,0.15); padding-top: 8px; }
.corridor-label { font-size: 11px; color: #888; white-space: nowrap; font-weight: bold; }
.members-avatar-track { display: flex; gap: 8px; padding-bottom: 4px; }
.member-avatar-box { position: relative; flex-shrink: 0; }
.host-crown { position: absolute; top: -10px; left: 50%; transform: translateX(-50%); font-size: 12px; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.3)); z-index: 10; }
.avatar-circle { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 11px; font-weight: bold; background: rgba(255,255,255,0.4);
  border: 1px solid rgba(255,255,255,0.6); color: #409eff; cursor: pointer; }
.is-me-avatar { border: 2px solid #67c23a !important; background: rgba(103,194,58,0.2) !important; color: #67c23a; }

/* 聊天流 */
.chat-msg-container { flex: 1; padding-right: 10px; }
.msg-list { display: flex; flex-direction: column; gap: 16px; padding-bottom: 20px; }
.msg-bubble { display: flex; flex-direction: column; max-width: 80%; }
.msg-bubble.is-me { align-self: flex-end; align-items: flex-end; }
.msg-bubble.is-other { align-self: flex-start; align-items: flex-start; }

.sender-name { font-size: 11px; color: #888; margin: 0 4px 4px 4px; }
.bubble-body { padding: 10px 14px; border-radius: 14px; font-size: 13px; line-height: 1.4; }
.is-me .bubble-body { background: rgba(64,158,255,0.2) !important; border-top-right-radius: 2px; }
.is-other .bubble-body { background: rgba(150,150,150,0.1) !important; border-top-left-radius: 2px; }

/* ================= 📜 核心灵魂：和纸手账书摘卡片 ================= */
.literary-note-card {
  position: relative;
  width: 90%;
  margin: 10px 0;
  border-radius: 12px !important;
  padding: 16px 18px !important;
  text-align: left;
  /* 极致温柔的奶油复古纸张底色 */
  background: linear-gradient(135deg, #FFFDF8 0%, #F6F0E2 100%) !important;
  border: 1px solid #E6DBC4 !important;
  box-shadow: 0 10px 25px -5px rgba(180, 165, 135, 0.3), 0 0 1px 1px rgba(255,255,255,0.8) !important;
}

.note-is-me { align-self: flex-end; border-right: 4px solid #409eff !important; }
.note-is-other { align-self: flex-start; border-left: 4px solid #8CAFA0 !important; }

/* 贴在头顶的半透明和纸胶带 */
.washi-tape {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 70px;
  height: 16px;
  background: rgba(235, 215, 160, 0.65);
  backdrop-filter: blur(4px);
  border-radius: 2px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 4px rgba(0,0,0,0.06);
  z-index: 5;
}

.note-card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #DDD2BD; padding-bottom: 6px; margin-bottom: 10px; font-size: 11px; color: #8C7B65; }
.note-author { font-weight: bold; color: #5C4E3A; }

.excerpt-box { position: relative; background: rgba(225, 215, 195, 0.2); padding: 12px 14px; border-radius: 6px; margin-bottom: 12px; }
.quote-watermark { position: absolute; top: -12px; left: 6px; font-size: 40px; color: rgba(180, 165, 140, 0.25); font-family: 'Georgia', serif; pointer-events: none; }
.excerpt-text { font-family: 'KaiTi', 'STKaiti', serif; font-size: 13px; color: #4A3E2C; line-height: 1.6; margin: 0; position: relative; z-index: 1; text-align: justify; }

.user-comment-box p { font-size: 12px; color: #665845; line-height: 1.5; margin: 0; font-weight: 500; }

/* 🌙 暗黑模式下的手账卡片适配：黑曜石特种纸 */
:deep(.dark-mode) .literary-note-card,
html.dark .literary-note-card {
  background: linear-gradient(135deg, #2A2622 0%, #1B1816 100%) !important;
  border-color: #3D3730 !important;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.8) !important;
}
html.dark .washi-tape { background: rgba(180, 155, 110, 0.35); border-color: rgba(255,255,255,0.1); }
html.dark .note-card-header { border-bottom-color: #3D3730; color: #A6998A; }
html.dark .note-author { color: #D4C8B4; }
html.dark .excerpt-box { background: rgba(0,0,0,0.25); }
html.dark .excerpt-text { color: #E0D7C6; }
html.dark .user-comment-box p { color: #BAB0A1; }

.chat-input-area { padding-top: 10px; border-top: 1px solid rgba(150,150,150,0.2); margin-top: auto; }
.excerpt-form label { font-size: 12px; color: #666; font-weight: bold; margin-bottom: 4px; display: block; }
@keyframes pulse { 0% { transform: scale(0.95); opacity: 0.8; } 50% { transform: scale(1.2); opacity: 1; } 100% { transform: scale(0.95); opacity: 0.8; } }
/* =========================================================
   💬 胶囊头顶未读气泡 & 红点角标
   ========================================================= */

/* 1. 右上角未读红点 */
.unread-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #f56c6c;
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  padding: 2px 6px;
  border-radius: 10px;
  border: 2px solid var(--glass-border);
  box-shadow: 0 4px 8px rgba(245, 108, 108, 0.4);
  z-index: 10;
  animation: badgeBounce 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.27);
}

/* 2. 头顶悬浮预览吐司框 */
.msg-preview-toast {
  position: absolute;
  bottom: calc(100% + 14px); /* 永远悬浮在胶囊正上方 14px 的空中 */
  right: 0;
  width: max-content;
  max-width: 240px;
  padding: 10px 14px;
  border-radius: 16px !important;
  background: var(--glass-bg) !important;
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.25), 0 0 15px rgba(100, 181, 246, 0.25);
  text-align: left;
  line-height: 1.3;
  cursor: pointer;
  pointer-events: auto; /* 允许用户直接点击这个气泡打开抽屉 */
}

/* 气泡底部指向胶囊的小尾巴（CSS 倒三角） */
.msg-preview-toast::after {
  content: '';
  position: absolute;
  bottom: -6px;
  right: 36px; /* 刚好对齐下方胶囊舱的中心点 */
  width: 12px;
  height: 12px;
  background: inherit;
  border-right: 1px solid rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid rgba(255, 255, 255, 0.6);
  transform: rotate(45deg);
}

.toast-sender { font-size: 11px; color: #409eff; font-weight: 800; margin-bottom: 2px; }
.toast-content { font-size: 12px; color: var(--el-text-color-regular); font-weight: 500; }

/* 气泡弹入弹出的果冻物理动画 */
.bubble-pop-enter-active, .bubble-pop-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bubble-pop-enter-from, .bubble-pop-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.85);
}

@keyframes badgeBounce {
  0% { transform: scale(0); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

/* =========================================================
   📐 抽屉左侧无极缩放感应轨
   ========================================================= */
.drawer-resizer-handle {
  position: absolute;
  top: 0;
  left: -4px; /* 故意往抽屉外面探出 4px，给用户极大的鼠标判定吸附区 */
  width: 10px;
  height: 100%;
  cursor: ew-resize;
  z-index: 9999;
  background: transparent;
  transition: background-color 0.2s ease;
}

/* 🖱️ 鼠标悬停或正在拖拽时：浮现出极其高雅的赛博流光蓝线 */
.drawer-resizer-handle:hover,
.drawer-resizer-handle.is-resizing {
  background: linear-gradient(
      to right,
      transparent 3px,
      rgba(64, 158, 255, 0.8) 4px,
      rgba(64, 158, 255, 0.8) 5px,
      transparent 6px
  );
}

/* 拖拽时让整个抽屉产生轻微的“物理紧绷加速感” */
.is-resizing ~ :deep(.el-drawer__body) {
  pointer-events: none; /* 极其重要：拖拽时屏蔽抽屉内部所有 iframe/图片的鼠标捕获，防止卡顿！ */
  transition: none !important;
}
</style>