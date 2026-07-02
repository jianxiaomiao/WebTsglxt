<script setup>
// ==============================================
// 🔥 1. 依赖导入模块（所有import集中存放，原顺序不变）
// ==============================================
import { ref, onMounted, inject, computed, watch, onUnmounted, nextTick } from 'vue'
import request from '../utils/request.js'
import {
  ElCard, ElRow, ElCol, ElImage, ElButton,
  ElMessageBox, ElMessage, ElDivider, ElTag
} from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import {usePetStore} from "../stores/petStore.js";
import { useUserStore } from '../stores/userStore'
import { useAchievementStore } from '../stores/achievementStore'
// 新增：导入Element Plus加载图标
import { Loading } from '@element-plus/icons-vue'

// ==============================================
// 🔥 2. 全局实例化模块（Store/路由）
// ==============================================
const petStore = usePetStore()
const userStore = useUserStore()
const achievementStore = useAchievementStore()
const router = useRouter()
const route = useRoute()

// ==============================================
// 🔥 3. 依赖注入模块（所有inject集中存放）
// ==============================================
const fetchUserReadHistory = inject('fetchUserReadHistory')
const generateShareContent = inject("generateShareContent")
const generateBookShareLink = inject('generateBookShareLink')
const shareToForum = inject("shareToForum")
const shareBookToFriend = inject('shareBookToFriend')
const isDark = inject('isDark')
const userCollections = inject('userCollections')
const getUserInfo = inject('getUserInfo')
const loadUserCollections = inject('loadUserCollections')

const getBookDetail = inject('getBookDetail') // 👈 新增注入获取书籍详情的方法
// 3. 依赖注入 新增👇
const loadUserCollectionsPage = inject('loadUserCollectionsPage')

// 4. 响应式变量 新增👇
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const noMore = ref(false)
const collections = ref([])

const userReadHistory = inject('userReadHistory')
// ✅ 新增：注入Basic已经获取好的借阅列表（不用自己再请求了！）
const userBorrowList = inject('userBorrowList')
// ✅ 新增：注入Basic刷新借阅的方法（还书后刷新用')
const getuserBorrows = inject('getuserBorrows')

// ==============================================
// 🔥 4. 响应式变量模块（按功能分类声明）
// ==============================================
// 4.1 移动端适配变量
const isMobile = inject('isMobile')
// 4.2 右键菜单变量
const handleCaptureScreen = inject('handleCaptureScreen')
const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickBook = inject('currentRightClickBook')
const currentRightClickGroup = ref(null)
// 4.3 业务数据变量
// ✨ 新增：书籍阅读页面过渡动画变量
const showTransition = ref(false)
const isZoomActive = ref(false)
const isFadeActive = ref(false)
const transitionImg = ref('')
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')

// ========== 新增：书架分组弹窗相关变量 ==========
const showGroupDialog = ref(false)
const groupList = ref([])
const groupPage = ref(1)
const groupPageSize = ref(5)
const groupTotal = ref(0)
const groupLoading = ref(false)
const selectedGroupId = ref(null)
const showAddGroupInput = ref(false)
const newGroupName = ref('')
// 新增缓存：独立存储要移动的书籍，规避右键菜单清空currentRightClickBook
const cacheMoveBook = ref(null)

// ==============================================
// 🔥 5. 计算属性模块
// ==============================================
// 合并借阅和收藏列表
// 合并借阅和收藏列表（直接用Basic全局数据，无重复请求）
// 5. 计算属性 替换这段
const allShelfBooks = computed(() => {
  const borrowWithType = userBorrowList.value.map(item => ({ ...item, type: 'borrow' }))
  const collectWithType = collections.value.map(item => ({ ...item, type: 'collection' }))
  return [...borrowWithType, ...collectWithType]
})

// ==============================================
// 🔥 6. 工具函数模块（通用纯函数）
// ==============================================
// 单条书籍item数据，循环列表里每一条都单独处理
const getSafeStar = (starVal) => {
  // 转数字，空值兜底0
  let num = Number(starVal) || 0
  // 最小0，最大5，杜绝负数/超5
  return Math.max(0, Math.min(5, num))
}

// 时间格式化函数
const formatTime = inject('formatDateTime')

// 原生JS复制文本到剪贴板（零依赖，所有现代浏览器都支持）
const copyToClipboard = inject('copyToClipboard')

// ==============================================
// 🔥 7. 右键菜单功能模块（核心方法+衍生方法）
// ==============================================
// ✅ 新增：处理右键菜单的归还操作
const handleReturnBookFromMenu = () => {
  const book = currentRightClickBook.value
  if (book) {
    // 调用你原有的归还逻辑，传入 borrowId 和 bookInfo
    returnBook(book.borrowId, book)
  }
  // 操作完关闭右键菜单
  closeContextMenu()
}

// ✅ 新增：处理右键菜单的取消收藏操作
const handleCancelCollectionFromMenu = () => {
  const book = currentRightClickBook.value
  if (book) {
    // 调用你原有的取消收藏逻辑，传入 collectionId 和 bookInfo
    cancelCollection(book.collectionId, book)
  }
  // 操作完关闭右键菜单
  closeContextMenu()
}

// 监听窗口尺寸（移动端适配）
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 处理右键点击
const handleContextMenu = (e, item) => {
  // 移动端不显示右键菜单
  if (isMobile.value) return

  // 阻止浏览器默认右键菜单
  e.preventDefault()
  e.stopPropagation()

  // 核心判断：清空上一次的状态，精准赋值
  if (item.isGroup) {
    currentRightClickGroup.value = item     // 存分组
    currentRightClickBook.value = null      // 清空书
  } else {
    currentRightClickBook.value = item      // 存书
    currentRightClickGroup.value = null     // 清空分组
  }

  // 计算菜单位置
  const menuWidth = 180
  // 动态高度估算：如果是点分组，菜单项少，高度不需要 320，给 120 足够了
  const menuHeight = item.isGroup ? 120 : 320
  let x = e.clientX
  let y = e.clientY

  // 右侧超出屏幕则左移
  if (x + menuWidth > window.innerWidth) {
    x = window.innerWidth - menuWidth - 10
  }
  // 底部超出屏幕则上移
  if (y + menuHeight > window.innerHeight) {
    y = window.innerHeight - menuHeight - 10
  }

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

const handleDeleteGroupFromMenu = () => {
  const group = currentRightClickGroup.value
  if (group) {
    // 调用我们之前写好的终极版 handleDeleteGroup
    handleDeleteGroup(group)
  }
  closeContextMenu() // 关闭右键菜单
}

// 关闭右键菜单
const closeContextMenu = inject('closeContextMenu')

const sendAiMsg = inject('sendAiMsg')
const aiLoading = inject('aiLoading')
const sendMessageToAI = inject('sendMessageToAI')
const addMessage = inject('addMessage')
// ✅ 修复后的AI总结书籍功能
const handleAiSummarizeBook = async () => {
  const book = currentRightClickBook.value
  closeContextMenu()
  if (!book) {
    ElMessage.warning('未获取到书籍信息，请重试')
    return
  }

  // ========== 🔥 第一步：优先用缓存，不重复生成 ==========
  if (book.aiSummary && book.aiSummary.trim()) {
    addMessage(book.aiSummary)
    return
  }

  const bookName = book.bookname || '未知书籍'
  const author = book.author || '未知作者'
  const introduction = book.information || '暂无简介'

  const prompt = `请帮我详细总结这本经典名著的核心内容：\n书名：《${bookName}》\n作者：${author}\n书籍简介：${introduction}\n\n请你从以下3个方面进行结构化总结：\n1. 核心故事梗概（200字以内）\n2.1-3个主要人物和性格（200字以内）\n3. 1-3句经典名句推荐。`

  try {
    // 在 handleAiSummarizeBook 内部替换调用那一行：
    const reply = await sendAiMsg(prompt, 'book_summary', { isbn: book.isbn || book.iSBN })

    if (reply) {
      // ========== 🔥 第二步：生成成功后，自动存回数据库 ==========
      try {
        await request.put('book?action=updateAiSummary', null, {
          params: {
            isbn: book.isbn || book.iSBN,
            aiSummary: reply
          }
        })
        // 同步更新本地对象，下次直接走缓存
        book.aiSummary = reply
      } catch (saveErr) {
        console.warn('AI总结保存失败，不影响查看：', saveErr)
      }
    } else {
      addMessage('😥 生成失败，请稍后重试')
    }
  } catch (err) {
    console.error('AI总结书籍失败:', err)
    addMessage('😥 服务异常，生成失败，请重试')
  } finally {
  }
}

// 分享书籍到论坛
const shareBookToForum = inject('shareBookToForum')

// 复制分享链接到剪贴板
const copyBookShareLink = inject('copyBookShareLink')

// ==============================================
// 🔥 8. 业务核心功能模块（书架操作）
// ==============================================
// ✨ 优化：3D 翻开飞出过渡动画
const handleGoToReaderWithAnimation = (isbn, bookname,information, pictureName) => {
  transitionImg.value = pictureName || '/default-book.png'
  showTransition.value = true

  // 🔥 AI 管家节流：同一本书 5 分钟内不重复调用
  const THROTTLE_MS = 5 * 60 * 1000
  const storageKey = `ai_steward_last_${isbn}`
  const lastCall = localStorage.getItem(storageKey)
  const now = Date.now()

  if (!lastCall || (now - parseInt(lastCall)) > THROTTLE_MS) {
    const promptText = `用户点开书籍`
    // 🐛 修复2：传入第三个参数 { isbn: isbn }，这样后端AI才能判断用户是否读过此书
    // 🐛 修复3：加上 .catch()，即使 AI 请求偶尔出错，也绝不会卡死用户的正常阅读流程
    sendAiMsg(promptText, 'ai_steward', { isbn: isbn })
        .catch(err => console.warn('AI管家打招呼失败，不影响阅读', err))

    // 记录本次调用时间，清理旧条目防止 localStorage 膨胀（保留最近 20 条）
    localStorage.setItem(storageKey, now.toString())
    const keys = Object.keys(localStorage).filter(k => k.startsWith('ai_steward_last_'))
    if (keys.length > 20) {
      keys.sort((a, b) => parseInt(localStorage.getItem(a)) - parseInt(localStorage.getItem(b)))
      keys.slice(0, keys.length - 20).forEach(k => localStorage.removeItem(k))
    }
  }

  nextTick(() => {
    setTimeout(() => {
      // 同时触发：整体飞向屏幕 + 封面左翻开
      isZoomActive.value = true
    }, 20)
  })

  // 稍微延长到 550ms 路由跳转，给封面翻开预留足够的视觉时间
  setTimeout(() => {
    isFadeActive.value = true
    goToBookReader(isbn)
  }, 550)

  setTimeout(() => {
    showTransition.value = false
    isZoomActive.value = false
    isFadeActive.value = false
  }, 1000)
}

// 跳转章节阅读页
const goToBookReader = inject('goToBookReader')

// 还书操作
const returnBook = async (borrowId, bookInfo) => {
  try {
    // 1. 弹窗确认
    await ElMessageBox.confirm('确认归还此书吗？', '提示', { type: 'warning' })

    // 2. 安全获取 ISBN（考虑到大小写兼容性）
    const isbn = bookInfo.iSBN || bookInfo.isbn
    if (!isbn) {
      ElMessage.warning('未获取到书籍的 ISBN 信息')
      return
    }

    // 3. 🔥 发请求获取最新的书籍详情（拿到真实的 now_book 库存）
    const latestBook = await getBookDetail(isbn)
    if (!latestBook) {
      ElMessage.error('获取书籍最新库存失败，请重试')
      return
    }

    // 4. 获取用户信息
    const user = await getUserInfo(currentUserId.value)

    // 5. 执行删除借阅记录
    await request.delete(`/book/borrow?borrowId=${borrowId}&userId=${currentUserId.value}`)

    // 6. 🌟 使用刚刚查到的最新库存数量加 1
    await request.patch('/book', {
      iSBN: isbn,
      now_book: Number(latestBook.now_book || 0) + 1
    })

    // 7. 恢复用户的可借阅额度
    await request.patch('/user/info', {
      userId: currentUserId.value,
      can_use: user.can_use + 1
    })

    ElMessage.success('还书成功！')

    // 8. 调用Basic的方法，刷新全局借阅列表
    await getuserBorrows()

  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error('还书失败，请稍后再试')
    console.error('还书失败', err)
  }
}

// ========== 新增：书架分组展示相关变量 ==========
const activeGroupId = ref(null) // 当前选中的分组ID，null代表全部
const shelfGroupList = ref([])  // 书架分组列表

// 1. 获取全部分组列表（用于在特定分组界面做顶部横向切换）
const fetchShelfGroupList = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/user/bookshelf/group', {
      params: { userId: currentUserId.value, page: 1, pageSize: 100 } // 一次性拉取全部组名
    })
    if (res.code === 200) {
      shelfGroupList.value = res.data.list || []
    }
  } catch (err) {
    console.error('加载分组导航导航失败', err)
  }
}

// 2. 核心算法：实时计算和聚合混编书架节点
const processedDisplayItems = computed(() => {
  // 视角分支：如果已经在特定的分组里面，直接展示这个组的分页书籍（不展示层叠结构）
  if (activeGroupId.value !== null) {
    return allShelfBooks.value
  }

  // 视角分支：全部藏书界面（进行3D智能聚合）
  const result = []
  const groupMap = {}

  // A. 借阅的书籍不参与分组，直接单本倾斜展示
  userBorrowList.value.forEach(item => {
    result.push({ ...item, type: 'borrow' })
  })

  // B. 智能梳理收藏夹数据
  collections.value.forEach(item => {
    const gid = item.groupId
    if (gid && gid !== 0) {
      // 发现带有分组的书籍，进行合并聚合
      if (!groupMap[gid]) {
        groupMap[gid] = {
          isGroup: true,
          groupId: gid,
          groupName: item.groupName || '未命名分组',
          books: []
        }
        result.push(groupMap[gid])
      }
      groupMap[gid].books.push({ ...item, type: 'collection' })
    } else {
      // 属于“未分组”的独立书籍，单本游离展示
      result.push({ ...item, type: 'collection' })
    }
  })

  return result
})

// 3. 改造视角切换器
const handleSwitchGroup = async (groupId) => {
  activeGroupId.value = groupId
  page.value = 1
  noMore.value = false
  collections.value = [] // 擦除旧数据，引发清空流变
  isInitialLoading.value = true // 唤醒精美骨架屏

  // 触发带有动态 groupId 的数据清洗
  await getCollections(false)
}

// 4. 联动改造你的局部数据请求器，使其支持全视角与群视角的切换切换
const getCollections = async (isLoadMore = false) => {
  if (!currentUserId.value || loading.value || (noMore.value && isLoadMore)) return
  loading.value = true

  try {
    // 灵活组装后端网络请求，完美配合你的后端Servlet API
    const params = {
      userId: currentUserId.value,
      page: page.value,
      pageSize: pageSize.value
    }
    // 如果是进入了具体的分组界面，才给后端送入 groupId
    if (activeGroupId.value !== null) {
      params.groupId = activeGroupId.value
    }

    const res = await request.get('/user/collection', { params })

    if (res && res.code === 200 && res.data) {
      const { list, total } = res.data
      if (isLoadMore) {
        collections.value.push(...list)
      } else {
        collections.value = list
      }
      noMore.value = collections.value.length >= total
    }
  } catch (err) {
    console.error('获取同步藏书失败:', err)
  } finally {
    loading.value = false
  }
}

// 滚动加载更多
const loadMoreCollections = (direction) => {
  if (direction === 'bottom' && !noMore.value && !loading.value) {
    page.value++
    getCollections(true)
  }
}

// 取消收藏 改造末尾👇
const cancelCollection = async (collectionId, bookInfo) => {
  try {
    await request.delete('/user/collection', { params: { collectionId, iSBN: bookInfo.isbn } })
    // 重置分页
    page.value = 1
    noMore.value = false
    await getCollections(false)
    ElMessage.success('取消收藏成功！')
  } catch (err) { if (err === 'cancel') return }
}

// ==============================================
// 🔥 新增：书架分组核心功能
// ==============================================
// 打开分组选择弹窗（右键菜单调用）
const handleMoveToGroup = () => {
  const book = currentRightClickBook.value
  if (!book || book.type !== 'collection') {
    ElMessage.warning('仅收藏书籍可进行分组操作')
    return
  }
  // 核心修复：先把书籍缓存到独立变量，再关闭右键菜单
  cacheMoveBook.value = book
  console.log("【打开分组弹窗】缓存待移动书籍 collectionId:", book.collectionId)
  console.log("【当前操作书籍】collectionId:", book.collectionId, "书籍名:", book.bookname)

  closeContextMenu() // 此行会清空currentRightClickBook，缓存已提前保存

  // 重置弹窗状态
  selectedGroupId.value = null
  groupPage.value = 1
  showAddGroupInput.value = false
  newGroupName.value = ''
  showGroupDialog.value = true
  console.log("【打开分组弹窗】重置选中分组 selectedGroupId =", selectedGroupId.value)
  // 加载第一页分组
  loadGroupList()
}

// 分页加载当前用户所有分组（对接后端 /user/bookshelf/group）
const loadGroupList = async () => {
  if (!currentUserId.value) return
  groupLoading.value = true
  try {
    const res = await request.get('/user/bookshelf/group', {
      params: {
        userId: currentUserId.value,
        page: groupPage.value,
        pageSize: groupPageSize.value
      }
    })
    if (res.code === 200) {
      groupList.value = res.data.list || []
      groupTotal.value = res.data.total || 0
    }
  } catch (err) {
    console.error('加载分组列表失败', err)
    ElMessage.error('加载分组列表失败')
  } finally {
    groupLoading.value = false
  }
}

// 提交新增分组
const submitAddGroup = async () => {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('分组名称不能为空')
    return
  }
  try {
    const res = await request.post('/user/bookshelf/group', {
      userId: currentUserId.value,
      groupName: newGroupName.value.trim(),
      sort: 0
    })
    if (res.code === 200) {
      ElMessage.success('新增分组成功')
      achievementStore.checkAchievements()
      newGroupName.value = ''
      showAddGroupInput.value = false
      // 刷新列表回到第一页
      groupPage.value = 1
      loadGroupList()
    } else {
      ElMessage.error(res.msg || '新增分组失败')
    }
  } catch (err) {
    console.error('新增分组失败', err)
    ElMessage.error('新增分组失败，请稍后重试')
  }
}

// 删除分组（带二次确认）
const handleDeleteGroup = async (group) => {
  try {
    await ElMessageBox.confirm(
        '删除分组后，该分组下的书籍将变为未分组状态，确认删除吗？',
        '提示',
        { type: 'warning' }
    )
    const res = await request.delete('/user/bookshelf/group', {
      params: { groupId: group.groupId }
    })

    if (res.code === 200) {
      ElMessage.success('删除分组成功')

      // ==========================================
      // 🧹 1. 弹窗内部的状态清理
      // ==========================================
      if (selectedGroupId.value === group.groupId) {
        selectedGroupId.value = null
      }
      loadGroupList() // 刷新弹窗内的列表

      // ==========================================
      // 🌟 2. 外部书架大屏的状态同步清理（核心修复！）
      // ==========================================
      // A. 刷新顶部横向胶囊导航栏，让删掉的分组消失
      if (typeof fetchShelfGroupList === 'function') {
        fetchShelfGroupList()
      }

      // B. 极其关键的视角重置：
      // 如果用户此时正处在【全部藏书】视角，删掉分组后，需要重新拉取数据，让原本的 3D 书堆“散开”变成单本。
      // 如果用户此时正好处于【被删的这个分组】的视角，底盘都没了，赶紧把他踢回【全部藏书】视角！
      if (activeGroupId.value === group.groupId) {
        activeGroupId.value = null // 退回全部藏书
      }

      // C. 暴力清空当前画卷，重新渲染最新状态
      page.value = 1
      noMore.value = false
      collections.value = []
      isInitialLoading.value = true // 唤醒精美骨架屏过渡
      await getCollections(false)   // 重新向后端拉取最新书籍数据

    } else {
      ElMessage.error(res.msg || '删除分组失败')
    }
  } catch (err) {
    if (err === 'cancel') return
    console.error('删除分组失败', err)
    ElMessage.error('删除分组失败，请稍后重试')
  }
}

// 点击选中分组
const handleSelectGroup = (group) => {
  console.log("===== 点击分组触发 =====")
  console.log("当前点击分组ID：", group.groupId)
  console.log("当前点击分组名称：", group.groupName)
  selectedGroupId.value = group.groupId
  console.log("赋值完成，selectedGroupId最新值：", selectedGroupId.value)
}
const handleConfirmMove = async () => {
  console.log("【点击确定按钮】当前选中分组ID selectedGroupId =", selectedGroupId.value)
  if (!selectedGroupId.value) {
    ElMessage.warning('请先选择目标分组')
    console.log("【提交拦截】未选中任何分组，终止请求")
    return
  }
  // 修复：读取缓存的书籍，不再依赖被清空的currentRightClickBook
  const book = cacheMoveBook.value
  console.log("【待移动书籍】collectionId:", book?.collectionId, "要提交的groupId:", selectedGroupId.value)

  if (!book) {
    ElMessage.error('书籍数据丢失，请重新右键书籍操作')
    return
  }
  try {
    const reqBody = {
      collectionId: book.collectionId,
      groupId: selectedGroupId.value
    }
    console.log("【PUT请求体】", reqBody)
    const res = await request.put('/user/collection', reqBody)
    console.log("【后端返回结果】", res)
    if (res.code === 200) {
      ElMessage.success('移动分组成功')
      showGroupDialog.value = false
      cacheMoveBook.value = null // 操作完成清空缓存
      // 刷新书架列表，显示最新分组状态
      page.value = 1
      noMore.value = false
      await getCollections(false)
    } else {
      ElMessage.error(res.msg || '移动分组失败')
    }
  } catch (err) {
    console.error('移动分组失败，请求异常：', err)
    ElMessage.error('移动分组失败，请稍后重试')
  }
}

// ==============================================
// 🌟 新增：将书籍移出当前分组
// ==============================================
const handleRemoveFromGroup = async () => {
  const book = currentRightClickBook.value

  if (!book || !book.collectionId) {
    ElMessage.warning('未能获取到书籍信息')
    return
  }

  // 1. 关掉右键菜单（因为关掉会清空 currentRightClickBook，所以我们已经提前保存到 book 变量里了）
  closeContextMenu()

  try {
    // 2. 构造请求体，直接将 groupId 置空
    // 注意：如果你的数据库默认未分组是存 0，这里就传 0；如果允许为空，传 null 最佳
    const reqBody = {
      collectionId: book.collectionId,
      groupId: null
    }

    // 3. 复用更新收藏的 PUT 接口
    const res = await request.put('/user/collection', reqBody)

    if (res.code === 200) {
      ElMessage.success('已成功移出分组')

      // 4. 刷新数据，重新触发 3D 聚合计算
      page.value = 1
      noMore.value = false
      collections.value = []
      isInitialLoading.value = true // 唤醒骨架屏，让过渡更自然
      await getCollections(false)

    } else {
      ElMessage.error(res.msg || '移出分组失败')
    }
  } catch (err) {
    console.error('移出分组请求异常：', err)
    ElMessage.error('移出分组失败，请稍后重试')
  }
}

// ==============================================
// 🔥 9. 监听器模块
// ==============================================
// ✅ 新增：书架骨架屏 首次加载状态
const isInitialLoading = ref(true)
// ✅ 新增：监听收藏加载状态，关闭骨架屏
watch(loading, (newVal) => {
  // 加载完成后，关闭首次骨架屏
  if (!newVal && isInitialLoading.value) {
    isInitialLoading.value = false
  }
})
// 监听路由变化，刷新数据
watch(
    () => route.path,
    async (newPath) => {
      if (newPath === '/bookshelf') {
        if (typeof fetchUserReadHistory === 'function') {
          await fetchUserReadHistory(currentUserId.value)
        }
        // 🌟 核心修复：每次回到书架页，重置分页并强制拉取最新的收藏列表！
        page.value = 1
        noMore.value = false
        await getCollections(false)
      }
    },
    { immediate: true }
)

// ==============================================
// 🔥 11. “游鱼出水” 诗意打字机引擎
// ==============================================
// 内置名言库
const poeticQuotes = [
  { text: "吾生也有涯，而知也无涯。", author: "《庄子》" },
  { text: "阅读是一座随身携带的避难所。", author: "毛姆" },
  { text: "脚步不能到达的地方，眼光可以到达。", author: "汪国真" },
  { text: "书卷多情似故人，晨昏忧乐每相亲。", author: "于谦" },
  { text: "我扑在书籍上，就像饥饿的人扑在面包上。", author: "高尔基" },
  { text: "要么旅行，要么读书，身体和灵魂必须有一个在路上。", author: "罗马假日" },
  { text: "你的气质里，藏着你走过的路，读过的书和爱过的人。", author: "卡萨布兰卡" }
]

const currentQuoteIndex = ref(0)
const displayedText = ref('')
const currentAuthor = ref('')
const quotePosition = ref({ left: '50%', top: '50%' })
const quotePhase = ref('hidden') // hidden, entering, typing, breathing, leaving
const isReaderUnmounted = ref(false)

// 顺时针游走的 4 个大致象限 (左上 -> 右上 -> 右下 -> 左下)，配上随机扰动
const quadrantPositions = [
  { baseLeft: 30, baseTop: 30 }, // 左上
  { baseLeft: 70, baseTop: 30 }, // 右上
  { baseLeft: 70, baseTop: 70 }, // 右下
  { baseLeft: 30, baseTop: 70 }  // 左下
]
let quadrantIndex = 0

// 辅助睡眠函数
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

// 游鱼引擎主循环
const startFishTypewriter = async () => {
  while (!isReaderUnmounted.value) {
    // 只有当书籍数量较少时才执行动画，节省性能
    if (allShelfBooks.value.length > 4) {
      await sleep(2000)
      continue
    }

    const quote = poeticQuotes[currentQuoteIndex.value]
    currentAuthor.value = quote.author
    displayedText.value = ''

    if (isMobile.value) {
      // 📱 手机端适配：取消随机乱跑，固定在正中间显示
      quotePosition.value = {
        left: '50%',
        top: '50%',
        transform: 'translate(-50%, -50%)'
      }
    } else {
      // 1. 计算顺时针游走的随机位置 (+- 10% 的随机扰动)
      const quad = quadrantPositions[quadrantIndex]
      const rLeft = quad.baseLeft + (Math.random() * 20 - 10)
      const rTop = quad.baseTop + (Math.random() * 20 - 10)
      quotePosition.value = {
        left: `${rLeft}%`,
        top: `${rTop}%`,
        transform: 'translate(-50%, -50%)'
      }
    }

    // 2. 游鱼上浮 (Fade In & Blur remove)
    quotePhase.value = 'entering'
    await sleep(1000) // 等待 CSS 进入动画完成

    // 3. 打字机输出
    quotePhase.value = 'typing'
    for (let i = 0; i < quote.text.length; i++) {
      if (isReaderUnmounted.value) break
      displayedText.value += quote.text[i]
      // 打字速度带点随机停顿，更像真人在敲击
      await sleep(100 + Math.random() * 150)
    }

    // 4. 呼吸悬浮状态
    quotePhase.value = 'breathing'
    await sleep(5000) // 停留 5 秒供用户欣赏

    // 5. 游鱼下沉消散
    quotePhase.value = 'leaving'
    await sleep(1200) // 等待 CSS 消失动画完成

    quotePhase.value = 'hidden'
    await sleep(500) // 彻底消失后静置半秒

    // 准备下一条数据
    currentQuoteIndex.value = (currentQuoteIndex.value + 1) % poeticQuotes.length
    quadrantIndex = (quadrantIndex + 1) % 4 // 顺时针流转
  }
}

// ==============================================
// 🔥 10. 生命周期钩子模块（合并重复钩子，逻辑不变）
// ==============================================
onMounted(() => {
  fetchShelfGroupList()
  getCollections(false)
  startFishTypewriter()
})

onUnmounted(() => {
  isReaderUnmounted.value = true
})
</script>

<template>
    <div class="book-card-container glass-panel fluid-shelf">
      <el-scrollbar  style="min-height:calc(80vh); overflow: hidden;" @end-reached="loadMoreCollections" :distance="50" >
        <div v-if="activeGroupId !== null" class="shelf-group-nav-wrapper animate-fade-in">
          <el-button class="back-to-all-btn glass-btn" @click="handleSwitchGroup(null)">
            <span>🔙</span> 返回全部
          </el-button>
        </div>
      <!-- ✅ 骨架屏包裹：仅首次加载显示 -->
      <el-skeleton
          :loading="isInitialLoading"
          animated
          style="width: 100%; padding: 5px;"
      >
        <!-- 骨架模板：模拟书架书籍卡片 -->
        <template #template>
          <el-row :gutter="10">
            <!-- 模拟6个书籍骨架（桌面6列/手机3列） -->
            <el-col :span="6" :xs="8" v-for="i in 12" :key="i" style="margin-bottom: 10px;">
              <div
                  style="
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  padding: 10px;
                  border-radius: 8px;
                  border: 2px solid #f0ebe4;
                  height: 100%;
                "
              >
                <!-- 书籍标签骨架 -->
                <el-skeleton-item variant="rect" style="width: 50px; height: 20px; margin-bottom: 8px;" />
                <!-- 书籍封面骨架 -->
                <el-skeleton-item variant="rect" style="width: 100%; height: 180px; border-radius: 4px;" />
                <!-- 书籍名称骨架 -->
                <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px;" />
              </div>
            </el-col>
          </el-row>
        </template>

        <!-- 真实书籍内容（加载完成后显示） -->
        <!-- 真实书籍内容（加载完成后显示） -->
        <template #default>
          <el-row
              :gutter="15"
              style="padding: 15px 5px;"
              :justify="processedDisplayItems.length <= 4 ? 'center' : 'start'"
          >
            <el-col
                :span="6"
                :xs="8"
                v-for="item in processedDisplayItems"
                :key="item.isGroup ? 'group-' + item.groupId : (item.borrowId || item.collectionId)"
                style="margin-bottom: 25px;"
            >

              <div
                  v-if="item.isGroup"
                  class="shelf-group-stack-wrapper"
                  @click="handleSwitchGroup(item.groupId)"
                  @contextmenu="handleContextMenu($event, item)"
              >
                <div class="group-stack-3d-entity">
                  <div
                      v-for="(book, index) in item.books.slice(0, 3)"
                      :key="book.collectionId"
                      class="stacked-book-cover"
                      :class="['stack-layer-' + index]"
                  >
                    <img :src="book.pictureName" class="stacked-cover-img" fallback-src="/default-book.png" />
                    <div class="stacked-cover-shading"></div>
                  </div>
                  <div class="glass-folder-back"></div>
                </div>
                <div class="group-stack-tag">
                  <span class="folder-icon">📂</span>
                  <span class="folder-name">{{ item.groupName }}</span>
                  <el-tag size="small" effect="dark" round class="folder-count-badge">{{ item.books.length }}</el-tag>
                </div>
              </div>

              <div v-else id="guide-shelf-book" class="shelf-book-card-3d-wrapper" @contextmenu="handleContextMenu($event, item)">
                <div class="book-3d-entity">
                  <div class="book-spine-3d"></div>
                  <div class="book-pages-3d"></div>
                  <div class="book-cover-3d">
                    <ElTag v-if="item.type === 'borrow'" type="danger" size="small" @click.stop="returnBook(item.borrowId, item)" class="book-tag-3d">借阅中</ElTag>
                    <ElTag v-else type="success" size="small" @click.stop="cancelCollection(item.collectionId, item)" class="book-tag-3d">已收藏</ElTag>
                    <ElImage :src="item.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" loading="lazy" :alt="item.bookname" @click="handleGoToReaderWithAnimation(item.isbn || item.iSBN,item.bookname, item.information, item.pictureName)" />
                    <div class="book-cover-shading"></div>
                  </div>
                  <div class="book-inside-reveal" @click="handleGoToReaderWithAnimation(item.isbn || item.iSBN, item.bookname, item.information, item.pictureName)">
                    <div class="inside-content">
                      <h3 class="book-name-3d">{{ item.bookname }}</h3>
                      <div class="book-detail-body">
                        <template v-if="item.type === 'borrow'"><p class="book-return-time-3d">⏳ 归还截止时间：<br><strong>{{ formatTime(item.returnDate) }}</strong></p></template>
                        <template v-else><p class="book-intro-3d">{{ item.information || '精选文学佳作，点击开启深度阅读之旅。' }}</p></template>
                      </div>
                      <div class="book-read-btn-hint">点击在线阅读 📖</div>
                    </div>
                  </div>
                </div>
              </div>

            </el-col>
          </el-row>

          <div v-if="processedDisplayItems.length <= 4" class="poetic-fish-ocean">
          </div>
        </template>
      </el-skeleton>

      <!-- 你原有右键菜单 完全不变 -->
        <Teleport to="body">
          <div
          v-if="showContextMenu"
          class="context-menu"
          :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
          @click.stop
      >
        <template v-if="currentRightClickBook">
          <div v-if="currentRightClickBook.type === 'borrow'" class="context-menu-item" @click="handleReturnBookFromMenu">
            <span>🔙</span>
            <span>归还书籍</span>
          </div>

          <div v-if="currentRightClickBook.type === 'collection'" class="context-menu-item" @click="handleCancelCollectionFromMenu">
            <span>❌</span>
            <span>取消收藏</span>
          </div>
          <!-- ✅ 新增：移动到分组（仅收藏书籍显示） -->
          <div v-if="currentRightClickBook.type === 'collection'" class="context-menu-item" @click="handleMoveToGroup">
            <span>📂</span>
            <span>移动到分组</span>
          </div>
          <div
              v-if="currentRightClickBook.type === 'collection' && currentRightClickBook.groupId && currentRightClickBook.groupId !== 0"
              class="context-menu-item"
              @click="handleRemoveFromGroup"
          >
            <span>📤</span>
            <span>移出当前分组</span>
          </div>
          <!-- ✅ 新增：AI总结书籍选项 -->
          <div class="context-menu-item" @click="handleAiSummarizeBook">
            <span>🤖</span>
            <span>AI总结书籍</span>
          </div>
          <div class="context-menu-item" @click="shareBookToForum">
            <span>📤</span>
            <span>分享书籍到论坛</span>
          </div>
          <div class="context-menu-item" @click="copyBookShareLink">
            <span>🔗</span>
            <span>复制分享链接</span>
          </div>
          <div class="context-menu-item" @click="shareBookToFriend">
            <span>👥</span>
            <span>分享给好友</span>
          </div>
        </template>
            <template v-if="currentRightClickGroup">
              <div class="context-menu-item" @click="handleDeleteGroupFromMenu">
                <span style="color: #f56c6c;">🗑️</span>
                <span style="color: #f56c6c;">删除此分组</span>
              </div>
            </template>
        <div class="context-menu-item" @click="handleCaptureScreen">
          <span>👓</span>
          <span>识别屏幕内容</span>
        </div>
      </div>
        </Teleport>
      </el-scrollbar>
    </div>

  <!-- ✨ 升级版：3D 翻页开书过渡动画遮罩 -->
  <div v-if="showTransition" class="read-transition-overlay" :class="{ 'fade-active': isFadeActive }">
    <div class="transition-book-3d" :class="{ 'fly-active': isZoomActive }">

      <!-- 1. 纸质内页（翻开后露出的内容） -->
      <div class="transition-inside-page">
        <div class="inside-paper-effect">
          <!-- 这里可以放一些雅致的内页装饰线条，模拟真实书页 -->
          <div class="paper-line"></div>
          <div class="paper-line"></div>
          <div class="paper-line"></div>
        </div>
      </div>

      <!-- 2. 书壳封面（向左轴翻转 180 度） -->
      <div class="transition-book-cover" :class="{ 'open-active': isZoomActive }">
        <img :src="transitionImg" class="transition-cover-img" />
        <div class="transition-cover-shading"></div>
      </div>

    </div>
  </div>

  <!-- ✅ 新增：书架分组选择弹窗 -->
  <el-dialog
      v-model="showGroupDialog"
      title="移动到书架分组"
      width="450px"
      :close-on-click-modal="false"
      :loading="groupLoading"
  >
    <template #header>
      <div class="dialog-header">
        <span>移动到书架分组</span>
        <el-button type="primary" size="small" @click="showAddGroupInput = !showAddGroupInput">
          + 新增分组
        </el-button>
      </div>
    </template>

    <!-- 新增分组输入框 -->
    <div v-if="showAddGroupInput" class="add-group-input">
      <el-input
          v-model="newGroupName"
          placeholder="请输入分组名称"
          maxlength="20"
          show-word-limit
          @keyup.enter="submitAddGroup"
      />
      <el-button type="primary" style="margin-top: 10px; width: 100%;" @click="submitAddGroup">
        确认新增
      </el-button>
    </div>

    <!-- 分组列表：左分组名，右删除按钮 -->
    <div class="group-list-wrap">
      <div
          v-for="group in groupList"
          :key="group.groupId"
          class="group-item"
          :class="{ 'is-selected': selectedGroupId === group.groupId }"
          @click="handleSelectGroup(group)"
      >
        <span class="group-name">{{ group.groupName }}</span>
        <el-button type="danger" text size="small" @click.stop="handleDeleteGroup(group)">
          删除
        </el-button>
      </div>
      <el-empty v-if="groupList.length === 0 && !groupLoading" description="暂无分组，点击右上角新增" :image-size="80" />
    </div>

    <!-- 底部分页 -->
    <div class="group-pagination">
      <el-pagination
          v-model:current-page="groupPage"
          v-model:page-size="groupPageSize"
          :total="groupTotal"
          layout="prev, pager, next, total"
          small
          @current-change="loadGroupList"
      />
    </div>

    <!-- 底部按钮：左取消，右确定 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="showGroupDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedGroupId" @click="handleConfirmMove">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.fluid-shelf {
  /* 🔥 核心1：用 height 代替 min-height，锁死内部独立滚动！ */
  /* 🔥 核心2：高度扣除的值，全权听从 BasicLayout 发来的总指挥信号 */
  min-height: calc(80vh);

  overflow: hidden;
  padding: 20px 10px;
  margin: 15px;
  border-radius: 16px;
  box-sizing: border-box;

  /* 极其丝滑的弹性形变过渡，时间必须和顶栏缩回去的 0.5s 完美咬合 */
  transition: height 0.5s cubic-bezier(0.16, 1, 0.3, 1) !important;
}
/* ======================================
   1. 全局容器与背景氛围升级
======================================== */
.book-card-container {

  overflow-y: auto;
  /* 🎨 融入你提供的 #ECF3F2 和 rgba(170, 198, 175) 混合微调，营造极其温柔的淡绿玉石背景 */
  padding: 0 20px !important;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* ======================================
   2. 🔥 每行书籍下方的定制“浅绿色调雅致书架”
======================================== */
.book-card-container :deep(.el-row) {
  position: relative;
  padding-bottom: 40px !important;
}

/* ======================================
   🔥 1. 悬浮亚克力层板 (替换掉原来单纯的绿色渐变)
======================================== */
.book-card-container :deep(.el-row)::after {
  content: "";
  position: absolute;
  bottom: 25px;
  left: 10px;
  right: 10px;
  height: 18px; /* 稍微加厚，更有承重感 */
  border-radius: 8px;
  z-index: 1;

  /* 核心质感：半透明高光亚克力 */
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

  /* 物理投影：底部的重阴影 + 顶部的发光边缘 */
  box-shadow:
      0 12px 24px rgba(0, 0, 0, 0.15),
      inset 0 1px 2px rgba(255, 255, 255, 0.8);
}

/* ======================================
   🔥 2. 画廊聚光灯效果 (打在每本书的背后)
======================================== */
.shelf-book-card-3d-wrapper::before {
  content: "";
  position: absolute;
  top: -30%;
  left: 50%;
  transform: translateX(-50%);
  width: 180%;
  height: 160%;
  /* 从书籍正上方打下来的径向渐变柔光 */
  background: radial-gradient(ellipse at top center, rgba(255, 255, 255, 0.25) 0%, transparent 60%);
  z-index: -1; /* 垫在书籍下方 */
  pointer-events: none;
}

/* ======================================
   🔥 3. 诗意留白填充样式
======================================== */
.poetic-space-filler {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 80px; /* 和上方的展台拉开距离 */
  opacity: 0;
  animation: fadeInPoetry 2s ease-in-out forwards 0.5s; /* 延迟半秒优雅淡入 */
  pointer-events: none;
}

.filler-content {
  text-align: center;
  color: var(--el-text-color-secondary);
  position: relative;
}

/* 装饰性引号 */
.filler-content::before {
  content: '“';
  position: absolute;
  top: -30px;
  left: -20px;
  font-size: 60px;
  color: rgba(255, 255, 255, 0.2);
  font-family: Georgia, serif;
}

.quote-text {
  font-size: 18px;
  letter-spacing: 4px;
  font-weight: 300;
  margin-bottom: 12px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.quote-author {
  font-size: 13px;
  letter-spacing: 2px;
  opacity: 0.7;
}

@keyframes fadeInPoetry {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 0.8; transform: translateY(0); }
}
.book-card-container::-webkit-scrollbar {
  display: none;
}

.book-shelf-loading :deep(.el-loading-mask) {
  z-index: 9;
}

/* ======================================
   2. 🔥 核心 3D 书籍交互架构
======================================== */
/* --------------------------------------
   3. 3D 书籍微调：给书本脚底加上投射到书架上的阴影
----------------------------------------- */
.shelf-book-card-3d-wrapper {
  height: 280px;
  perspective: 1200px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 2; /* 抬高层级，让书本立在书架前方 */
}

.book-3d-entity {
  position: relative;
  width: 150px;
  height: 220px;
  transform-style: preserve-3d;
  /* 稍微往下沉一点点，让它贴紧下方的书架面 */
  transform: rotateX(10deg) rotateY(-16deg) translateZ(0px) translateY(5px);
  transition: transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 加上一层接地气的环境光阴影 */
.book-3d-entity::after {
  content: "";
  position: absolute;
  bottom: -10px;
  left: 5%;
  width: 90%;
  height: 12px;
  background: rgba(74, 90, 76, 0.18);
  filter: blur(5px);
  transform: rotateX(90deg);
  pointer-events: none;
  transition: all 0.5s;
}

/* 当鼠标悬停书本翻开时，地面的影子也随之自然变淡隐去 */
.shelf-book-card-3d-wrapper:hover .book-3d-entity::after {
  background: rgba(74, 90, 76, 0.08);
  bottom: -15px;
  filter: blur(7px);
}

/* ======================================
   5. 暗黑模式绿黑色系书架适配
======================================== */
:deep(.dark-mode) {
  .book-card-container :deep(.el-row)::after {
    /* 暗黑模式下，层板变成暗夜幽蓝玻璃 */
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.01) 100%);
    border-top: 1px solid rgba(255, 255, 255, 0.15);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.6), inset 0 1px 1px rgba(255, 255, 255, 0.1);
  }

  .shelf-book-card-3d-wrapper::before {
    /* 暗黑模式聚光灯变暗，营造氛围 */
    background: radial-gradient(ellipse at top center, rgba(255, 255, 255, 0.08) 0%, transparent 60%);
  }

  .quote-text { color: #d1d5db; }
  .filler-content::before { color: rgba(255, 255, 255, 0.05); }
}

/* --------------------------------------
   A. 物理外书脊 (Spine)
----------------------------------------- */
.book-spine-3d {
  position: absolute;
  width: 22px;
  height: 100%;
  left: 0;
  top: 0;
  /* 模拟皮革/精装书脊光泽 */
  background: linear-gradient(90deg, #2c3e50 0%, #34495e 50%, #1a252f 100%);
  border-radius: 3px 0 0 3px;
  transform-origin: left center;
  transform: rotateY(-90deg);
  z-index: 5;
  box-shadow: inset -2px 0 5px rgba(0,0,0,0.3);
}

/* --------------------------------------
   B. 侧边真实纸页厚度 (Side Pages)
----------------------------------------- */
.book-pages-3d {
  position: absolute;
  width: 18px;
  height: 96%;
  top: 2%;
  right: 1px;
  /* 仿纸质线条纹理 */
  background: linear-gradient(90deg, #e4e4e4 0%, #fffbf2 100%);
  background-size: 100% 3px;
  background-image: linear-gradient(to bottom, #e2ddcf 1px, transparent 1px);
  transform-origin: right center;
  transform: rotateY(90deg) translateZ(-2px);
  box-shadow: inset 3px 0 6px rgba(0,0,0,0.15);
  border-radius: 0 2px 2px 0;
}

/* --------------------------------------
   C. 硬壳书封面 (Front Cover)
----------------------------------------- */
.book-cover-3d {
  position: absolute;
  width: 100%;
  height: 100%;
  left: 0;
  top: 0;
  z-index: 6;
  transform-origin: left center;
  transform-style: preserve-3d;
  /* 分段动画的核心：让封面比主体略微滞后翻开，层次感极强 */
  transition: transform 0.6s cubic-bezier(0.35, 1, 0.35, 1) 0.05s;
  box-shadow: 4px 6px 15px rgba(0, 0, 0, 0.3);
  border-radius: 1px 4px 4px 1px;
  overflow: hidden;
}

/* [Hover 第二阶段]: 封面随转轴流畅向左甩开 140° */
.shelf-book-card-3d-wrapper:hover .book-cover-3d {
  transform: rotateY(-142deg);
  box-shadow: -5px 6px 15px rgba(0, 0, 0, 0.15);
}

.book-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: auto;
  cursor: pointer;
}

/* 封面立体阴影和凹槽线 */
.book-cover-shading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255,255,255,0.1) 0%, transparent 5%, transparent 94%, rgba(0,0,0,0.15) 100%);
  box-shadow: inset 4px 0 4px rgba(0,0,0,0.2); /* 靠近书脊的装订凹槽 */
  pointer-events: none;
}

/* 贴在封面上的标签 */
.book-tag-3d {
  position: absolute;
  top: 8px;
  left: 12px;
  z-index: 10;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

/* --------------------------------------
   D. 翻开暴露出内页详情 (Inside Page)
----------------------------------------- */
.book-inside-reveal {
  position: absolute;
  width: 97%;
  height: 96%;
  left: 1%;
  top: 2%;
  z-index: 2;
  /* 羊皮复古纸张底色 */
  background: #fbf8ef;
  border: 1px solid #eadecc;
  border-radius: 0 4px 4px 0;
  box-shadow: inset 1px 0 6px rgba(0,0,0,0.05), 3px 4px 10px rgba(0,0,0,0.15);
  padding: 14px 10px;
  box-sizing: border-box;
  cursor: pointer;
}

.inside-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* 内页书名 */
.book-name-3d {
  font-size: 13px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  padding-bottom: 5px;
  border-bottom: 1px solid #eddcc6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

/* 🌟 推荐指数样式 */
.book-rating-3d {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 6px 0;
  font-size: 10px;
  background: rgba(234, 218, 194, 0.3);
  padding: 3px 6px;
  border-radius: 3px;
}
.rating-label {
  color: #7f8c8d;
  font-weight: 500;
}
.rating-stars {
  letter-spacing: 0.5px;
  transform: scale(0.95);
}

/* 原有样式保留，新增下面两行即可 */
.star-on {
  color: #ffc107; /* 金色亮星，贴合常规星级样式 */
}
.star-off {
  color: #cccccc; /* 灰色空星 */
  opacity: 0.7;
}

/* 详情叙述区 */
.book-detail-body {
  flex: 1;
  overflow: hidden;
  margin-top: 2px;
}

.book-intro-3d {
  font-size: 11px;
  color: #555;
  line-height: 1.5;
  margin: 0;
  text-align: left;
  display: -webkit-box;
  -webkit-line-clamp: 9; /* 纸质内页空间更大，可展示更多行字 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-return-time-3d {
  font-size: 11px;
  color: #c0392b;
  margin: 5px 0 0;
  line-height: 1.4;
  text-align: left;
}

/* 提示底部引导语 */
.book-read-btn-hint {
  font-size: 10px;
  text-align: center;
  color: #95a5a6;
  border-top: 1px dashed #eddcc6;
  padding-top: 5px;
  margin-top: 2px;
}

/* ======================================
   3. 全局右键菜单样式
======================================== */
.context-menu {
  position: fixed;
  z-index: 9999;
  border-radius: 8px;
  padding: 8px 0;
  min-width: 180px;
  font-size: 14px;

  /* ✨ 注入毛玻璃核心魔法 */
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  color: var(--el-text-color-primary); /* 跟随主题文字颜色 */
}

/* 悬浮时使用淡淡的半透明高光，替代死板的灰色实心背景 */
.context-menu-item:hover {
  background-color: rgba(128, 128, 128, 0.1) !important;
}

/* ======================================
   5. 暗黑模式优雅配色适配
======================================== */
:deep(.dark-mode) {
  /* 书籍纸张内页调暗，保护视力 */
  .book-inside-reveal {
    /* 🌟 修复：放弃纯色，采用幽深的斜向渐变，模拟高质感暗色特种纸的灵动光影 */
    background: linear-gradient(135deg, #242b35 0%, #1e242c 50%, #161b22 100%);
    border-color: #34495e;
    box-shadow: inset 1px 0 6px rgba(0,0,0,0.4), 3px 4px 10px rgba(0,0,0,0.4);
  }
  .book-name-3d {
    color: #ecf0f1;
    border-bottom-color: #34495e;
  }
  .book-rating-3d {
    background: rgba(52, 73, 94, 0.6);
  }
  .book-intro-3d {
    color: #bdc3c7;
  }
  /* 暗黑模式右键菜单 (✨ 毛玻璃适配) */
  .context-menu {
    background: var(--glass-bg) !important;
    border: 1px solid var(--glass-border) !important;
  }
  .context-menu-item {
    color: #e5e7eb;
  }
  .context-menu-item:hover {
    background-color: rgba(255, 255, 255, 0.1) !important;
  }
}
/* ======================================
   ✨ 电影级：3D 翻书飞出过渡动画架构
======================================== */
.read-transition-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(236, 243, 242, 0.9); /* 温润浅绿 */
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(12px);
  opacity: 1;
  pointer-events: none;
  transition: opacity 0.5s ease-in-out;
  perspective: 2500px; /* 极强的透视感 */
}

.read-transition-overlay.fade-active {
  opacity: 0;
}

/* 3D 书体总容器 */
.transition-book-3d {
  position: relative;
  width: 150px;
  height: 220px;
  transform-style: preserve-3d;
  /* 初始姿态：完美对齐书架上原书的倾斜角度 */
  transform: rotateX(10deg) rotateY(-16deg) translateZ(0px);
  transition: transform 0.7s cubic-bezier(0.25, 1, 0.35, 1);
}

/* [阶段一：飞出] 整个书体向眼球拍过来，并回正角度 */
.transition-book-3d.fly-active {
  transform: rotateX(0deg) rotateY(0deg) translateZ(500px) scale(2.8);
}

/* --------------------------------------
   📖 核心 1：硬壳封面（负责向左翻开）
----------------------------------------- */
.transition-book-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
  transform-origin: left center; /* 以左侧书脊为轴翻转 */
  transform: rotateY(0deg);
  transform-style: preserve-3d;
  backface-visibility: hidden; /* 翻过去后背面隐藏 */
  transition: transform 0.65s cubic-bezier(0.35, 1, 0.35, 1);
  box-shadow: 5px 5px 20px rgba(0, 0, 0, 0.3);
  border-radius: 1px 4px 4px 1px;
  overflow: hidden;
}

/* [阶段二：翻开] 封面流畅地向左甩开 145 度，露出底下的内页 */
.transition-book-cover.open-active {
  transform: rotateY(-145deg);
  box-shadow: -5px 5px 20px rgba(0, 0, 0, 0.1);
}

.transition-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.transition-cover-shading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255,255,255,0.1) 0%, transparent 5%, transparent 95%, rgba(0,0,0,0.15) 100%);
  box-shadow: inset 4px 0 4px rgba(0,0,0,0.2);
}

/* --------------------------------------
   📜 核心 2：纸质内页（静静躺在封面下方）
----------------------------------------- */
.transition-inside-page {
  position: absolute;
  top: 2%;
  left: 1%;
  width: 97%;
  height: 96%;
  z-index: 1;
  background: #fbf8ef; /* 复古羊皮纸色 */
  border: 1px solid #eadecc;
  border-radius: 0 4px 4px 0;
  box-shadow: inset 1px 0 6px rgba(0,0,0,0.05), 3px 4px 10px rgba(0,0,0,0.15);
  padding: 15px;
  box-sizing: border-box;
}

/* 仿真实的纸张质感与排版线条 */
.inside-paper-effect {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.paper-line {
  height: 4px;
  background: rgba(218, 201, 178, 0.4);
  border-radius: 2px;
}
.paper-line:nth-child(1) { width: 60%; height: 6px; background: rgba(218, 201, 178, 0.7); } /* 模拟标题 */
.paper-line:nth-child(2) { width: 90%; }
.paper-line:nth-child(3) { width: 80%; }

/* 暗黑模式适配：让飞出过渡极其护眼 */
:deep(.dark-mode) .read-transition-overlay {
  background: rgba(13, 20, 18, 0.92);
}
:deep(.dark-mode) .transition-inside-page {
  background: #242b35;
  border-color: #34495e;
}
:deep(.dark-mode) .paper-line {
  background: rgba(52, 73, 94, 0.6);
}
:deep(.dark-mode) .paper-line:nth-child(1) {
  background: rgba(52, 73, 94, 0.9);
}

/* ======================================
   🌊 游鱼诗意打字机特效引擎
======================================== */
/* 海洋层：覆盖在下方的大面留白区域 */
.poetic-fish-ocean {
  position: absolute;
  top: 320px; /* 避开上面的书籍展台 */
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none; /* 绝对不能阻挡用户点击 */
  overflow: hidden;
}

/* 游鱼本体容器 */
.poetic-fish-quote {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  white-space: nowrap;
  /* 为不同阶段挂载不同的 CSS 动画 */
  will-change: transform, opacity, filter;
}

/* 阶段 1：游鱼上浮 (Entering) */
.phase-entering {
  animation: fish-swim-up 1s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
}

/* 阶段 2：打字状态 (Typing) - 保持正常悬浮即可 */
.phase-typing {
  opacity: 1;
  filter: blur(0px);
  transform: translate(-50%, -50%);
}

/* 阶段 3：深海呼吸 (Breathing) - 如水母般上下微弱悬浮 */
.phase-breathing {
  animation: fish-breathe 4s ease-in-out infinite alternate;
}

/* 阶段 4：下潜消散 (Leaving) */
.phase-leaving {
  animation: fish-swim-down 1.2s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
}

/* --- 文字与光标样式 --- */
.quote-text-wrap {
  position: relative;
  font-size: 18px;
  letter-spacing: 4px;
  font-weight: 400;
  color: var(--el-text-color-primary);
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.quote-mark {
  position: absolute;
  left: -35px;
  top: -20px;
  font-size: 45px;
  color: rgba(64, 158, 255, 0.25); /* 淡淡的水光蓝 */
  font-family: Georgia, serif;
}

/* 打字机闪烁光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: #409eff;
  margin-left: 6px;
  vertical-align: text-bottom;
  animation: blink-cursor 0.8s step-end infinite;
  box-shadow: 0 0 5px #409eff;
}

.quote-author {
  margin-top: 15px;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--el-text-color-secondary);
  opacity: 0;
  transform: translateY(10px);
  transition: all 1s ease;
}

/* 当进入呼吸状态时，作者署名优雅浮出 */
.show-author {
  opacity: 0.8;
  transform: translateY(0);
}

/* --- 核心动画关键帧 --- */
@keyframes fish-swim-up {
  0% {
    opacity: 0;
    filter: blur(15px);
    transform: translate(-50%, calc(-50% + 40px)) scale(0.9);
  }
  100% {
    opacity: 1;
    filter: blur(0px);
    transform: translate(-50%, -50%) scale(1);
  }
}

@keyframes fish-breathe {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, calc(-50% - 10px));
  }
}

@keyframes fish-swim-down {
  0% {
    opacity: 1;
    filter: blur(0px);
    transform: translate(-50%, -50%) scale(1);
  }
  100% {
    opacity: 0;
    filter: blur(20px);
    transform: translate(-50%, calc(-50% - 60px)) scale(1.05); /* 向上飘散，模拟气泡 */
  }
}

@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 暗黑模式自适应 */
:deep(.dark-mode) .quote-text-wrap {
  color: #e5e7eb;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
}
:deep(.dark-mode) .quote-author {
  color: #9ca3af;
}

/* ======================================
   🌊 极致通透：重写全局毛玻璃，打造高透水族箱
======================================== */
/* 提高权重，覆写全局的 glass-panel 默认高不透明度 */
.book-card-container.glass-panel {
  /* 1. 背景透明度骤降：从原来的 0.65 降到 0.15，彻底透出底层的天气与自然背景 */
  background: rgba(255, 255, 255, 0.12) !important;

  /* 2. 降低模糊半径：让背景的雨滴、云朵更具辨识度，同时让游鱼仿佛在纯净的水中游动 */
  backdrop-filter: blur(8px) saturate(120%) !important;
  -webkit-backdrop-filter: blur(8px) saturate(120%) !important;

  /* 3. 强化边缘高光：让玻璃看起来更像清透的鱼缸外壳 */
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05), inset 0 0 20px rgba(255, 255, 255, 0.1) !important;
}

/* 🌙 暗黑模式下的深海极度通透 */
:deep(.dark-mode) .book-card-container.glass-panel,
html.dark .book-card-container.glass-panel {
  background: rgba(10, 15, 20, 0.25) !important; /* 极深的深海蓝黑，透光率极高 */
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), inset 0 0 20px rgba(255, 255, 255, 0.02) !important;
}

/* --- 同步优化：让亚克力层板也变得更加清透 --- */
.book-card-container :deep(.el-row)::after {
  /* 降低层板的实心白，主要靠顶部的高光线来勾勒形状 */
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12) 0%, rgba(255, 255, 255, 0.01) 100%) !important;
  border-top: 1px solid rgba(255, 255, 255, 0.35) !important;
  backdrop-filter: blur(4px) !important; /* 层板自身只有极轻微的折射 */
}

/* --- 同步优化：让画廊聚光灯柔和化，防止掩盖底色 --- */
.shelf-book-card-3d-wrapper::before {
  /* 从 0.25 降到 0.15，光晕更加内敛高级 */
  background: radial-gradient(ellipse at top center, rgba(255, 255, 255, 0.15) 0%, transparent 60%) !important;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.add-group-input {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.group-list-wrap {
  max-height: 300px;
  overflow-y: auto;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.group-item:hover {
  background-color: var(--el-fill-color-light);
}

.group-item.is-selected {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.group-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.group-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
/* ======================================
   🔥 新增：顶部书架分组导航栏
======================================== */
.shelf-group-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px !important;
  margin-bottom: 20px;
  overflow-x: auto;
  border-radius: 16px;

  /* 隐藏滚动条 */
  -ms-overflow-style: none;
  scrollbar-width: none;

  /* 弱化背景，不抢风头 */
  background: rgba(255, 255, 255, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
}

.shelf-group-nav::-webkit-scrollbar {
  display: none;
}

/* 分组药丸按钮 */
.group-pill {
  flex-shrink: 0; /* 防止被挤压 */
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.group-pill:hover {
  background: rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
  color: var(--el-text-color-primary);
}

/* 选中状态 */
.group-pill.is-active {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.8) 0%, rgba(30, 136, 229, 0.8) 100%);
  color: #fff;
  border-color: rgba(64, 158, 255, 0.5);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3), inset 0 1px 1px rgba(255, 255, 255, 0.4);
}

/* 暗黑模式适配 */
:deep(.dark-mode) .group-pill {
  background: rgba(0, 0, 0, 0.2);
  border-color: rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
}
:deep(.dark-mode) .group-pill:hover {
  background: rgba(255, 255, 255, 0.1);
}
/* =========================================================
   📂 🌟 3D 多维层叠聚合书堆 (3D Cascading Group Stack)
   ========================================================= */

/* 总外壳容器，自带高保真 3D 景深 */
.shelf-group-stack-wrapper {
  height: 280px;
  perspective: 1500px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 2;
  cursor: pointer;
}

/* 3D 实体空间基座 */
.group-stack-3d-entity {
  position: relative;
  width: 140px;
  height: 200px;
  transform-style: preserve-3d;
  transform: rotateX(12deg) rotateY(-12deg) translateY(5px);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 悬停特效：书堆整体舒展抬升，并在水族箱内散开 */
.shelf-group-stack-wrapper:hover .group-stack-3d-entity {
  transform: rotateX(8deg) rotateY(-4deg) translateZ(25px) translateY(-5px);
}

/* 统一封面纸张硬壳基础外衣 */
.stacked-book-cover {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 2px 4px 4px 2px;
  overflow: hidden;
  box-shadow: 3px 5px 12px rgba(0, 0, 0, 0.25);
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.6s;
}

.stacked-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 书皮侧边装订线高光模拟 */
.stacked-cover-shading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255,255,255,0.15) 0%, transparent 6%, transparent 94%, rgba(0,0,0,0.2) 100%);
  box-shadow: inset 2px 0 3px rgba(0,0,0,0.15);
}

/* -------------------------------------------
   🔮 极致矩阵：前三本封面的 3D 空间黄金离散错落布局
---------------------------------------------- */

/* 第一层：最前面的主封面（端正傲立，投影重） */
.stack-layer-0 {
  z-index: 3;
  transform: translateZ(25px) rotateY(-2deg) rotateX(1deg);
}
.shelf-group-stack-wrapper:hover .stack-layer-0 {
  transform: translateZ(45px) rotateY(-8deg) translateX(-10px);
  box-shadow: 10px 15px 25px rgba(0, 0, 0, 0.35);
}

/* 第二层：中间的过渡封面（向右侧偏移，微微后仰） */
.stack-layer-1 {
  z-index: 2;
  opacity: 0.95;
  transform: translateZ(10px) translateX(14px) translateY(-4px) rotateZ(5deg) rotateY(4deg);
}
.shelf-group-stack-wrapper:hover .stack-layer-1 {
  transform: translateZ(25px) translateX(28px) translateY(-8px) rotateZ(12deg) rotateY(8deg);
  box-shadow: 5px 10px 18px rgba(0, 0, 0, 0.25);
}

/* 第三层：最底部的背景封面（大幅度向左侧舒展，营造广角饱满度） */
.stack-layer-2 {
  z-index: 1;
  opacity: 0.8;
  transform: translateZ(-5px) translateX(-12px) translateY(2px) rotateZ(-8deg) rotateY(-6deg);
}
.shelf-group-stack-wrapper:hover .stack-layer-2 {
  transform: translateZ(5px) translateX(-26px) translateY(6px) rotateZ(-18deg) rotateY(-12deg);
  opacity: 0.9;
}

/* 磨砂折射底衬壳 */
.glass-folder-back {
  position: absolute;
  width: 106%;
  height: 104%;
  left: -3%;
  top: -2%;
  z-index: 0;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  transform: translateZ(-15px);
  box-shadow: inset 0 0 10px rgba(255,255,255,0.2);
}

/* -------------------------------------------
   🏷️ 亚克力包边液态水晶分组标签
---------------------------------------------- */
.group-stack-tag {
  margin-top: 15px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.25) !important;
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04), inset 0 1px 1px rgba(255,255,255,0.6);
  transition: all 0.3s ease;
}

.shelf-group-stack-wrapper:hover .group-stack-tag {
  background: rgba(255, 255, 255, 0.45) !important;
  border-color: rgba(64, 158, 255, 0.4);
  box-shadow: 0 6px 15px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.folder-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  max-width: 80px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.folder-count-badge {
  background-color: rgba(64, 158, 255, 0.8) !important;
  border: none;
  font-family: monospace;
}

/* -------------------------------------------
   🎛️ 分组操控台与精致悬浮返回键样式
---------------------------------------------- */
.shelf-group-nav-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 25px;
  width: 100%;
}

.back-to-all-btn {
  flex-shrink: 0;
  border-radius: 14px !important;
  padding: 8px 16px !important;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.35) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.back-to-all-btn:hover {
  transform: translateX(-4px);
  background: rgba(255, 255, 255, 0.55) !important;
  box-shadow: 0 6px 16px rgba(0,0,0,0.06);
}

.shelf-group-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding: 2px 0;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.shelf-group-nav::-webkit-scrollbar { display: none; }

/* 优雅渐显过渡 */
.animate-fade-in {
  animation: shelfFadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
@keyframes shelfFadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 暗黑模式自适应补丁 */
:deep(.dark-mode) {
  .group-stack-tag {
    background: rgba(15, 23, 42, 0.45) !important;
    border-color: rgba(255, 255, 255, 0.08);
  }
  .glass-folder-back {
    background: rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.05);
  }
  .back-to-all-btn {
    background: rgba(30, 41, 59, 0.5) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
  }
}

/* ======================================
   4. 移动端轻量化适配（无 Hover 设备降级）
======================================== */
@media (max-width: 768px) {
  .book-card-container {
    padding: 0 !important;
  }
  .shelf-book-card-3d-wrapper {
    height: 190px;
  }
  .book-card-container :deep(.el-row) {
    padding-bottom: 25px !important;
  }
  .book-card-container :deep(.el-row)::after {
    bottom: 15px;
    height: 9px;
  }
  /* 移动端缩小尺寸防止溢出 */
  .book-3d-entity {
    width: 95px;
    height: 145px;
    transform: none !important; /* 取消手机初始倾斜 */
  }

  /* 手机端通过简单触碰即可体验轻量翻页 */
  .shelf-book-card-3d-wrapper:hover .book-3d-entity {
    transform: translateX(10px);
  }
  .shelf-book-card-3d-wrapper:hover .book-cover-3d {
    transform: rotateY(-135deg);
  }

  .book-spine-3d { width: 12px; }
  .book-pages-3d { width: 10px; }

  .book-tag-3d {
    top: 4px;
    left: 4px;
    transform: scale(0.85);
  }
  .book-name-3d {
    font-size: 11px;
  }
  .book-rating-3d {
    display: none; /* 移动端屏幕小，隐藏部分辅助元素 */
  }
  .book-intro-3d {
    -webkit-line-clamp: 4;
    font-size: 9px;
  }
  .book-read-btn-hint {
    display: none;
  }

  /* ======================================
      🌊 游鱼打字机 手机端专属适配 (自动换行版)
   ======================================== */
  /* 1. 调整海域高度，确保视觉绝对居中 */
  .poetic-fish-ocean {
    top: 220px !important;
  }

  /* 2. 🌟 核心修复：解除单行限制，限制最大宽度以强制换行 */
  .poetic-fish-quote {
    white-space: normal !important; /* 解除不换行限制 */
    width: 85vw !important; /* 限制宽度为屏幕宽度的 85%，左右留出呼吸感 */
    text-align: left !important; /* 多行文本左对齐，阅读体验更好 */
  }

  /* 3. 缩小字号与字间距，增加多行行高 */
  .quote-text-wrap {
    font-size: 13px !important;
    letter-spacing: 2px !important;
    line-height: 1.8 !important; /* 增加行高，避免换行后上下两行文字粘连 */
    display: inline-block;
  }

  /* 4. 微调左上角的装饰性水滴双引号，防止换行后位置突兀 */
  .quote-mark {
    font-size: 32px !important;
    left: -15px !important;
    top: -10px !important;
  }

  /* 5. 缩小作者署名，并让其在换行模式下靠右对齐 */
  .quote-author {
    font-size: 11px !important;
    margin-top: 15px !important;
    width: 100%;
    text-align: right !important; /* 多行诗句的署名靠右侧更好看 */
    padding-right: 10px; /* 留出一点右边距 */
  }

  /* 1. 顶部返回按钮与胶囊导航缩小 */
  .shelf-group-nav-wrapper {
    margin-bottom: 15px !important;
    gap: 8px !important;
  }
  .back-to-all-btn {
    padding: 6px 10px !important;
    font-size: 12px !important;
    border-radius: 10px !important;
  }
  .group-pill {
    padding: 4px 12px;
    font-size: 12px;
  }

  /* 2. 书堆大外壳缩小，匹配单本书的高度 */
  .shelf-group-stack-wrapper {
    height: 190px;
  }

  /* 3. 3D 基座缩小，减弱初始的倾斜，防止超出手机网格 */
  .group-stack-3d-entity {
    width: 85px;
    height: 125px;
    transform: rotateX(8deg) rotateY(-6deg) translateY(2px);
  }

  /* 4. 收拢前三本书的层叠距离，排得紧凑一点 */
  .stack-layer-0 {
    transform: translateZ(10px) rotateY(-2deg);
  }
  .stack-layer-1 {
    transform: translateZ(3px) translateX(6px) translateY(-2px) rotateZ(4deg) rotateY(2deg);
  }
  .stack-layer-2 {
    transform: translateZ(-3px) translateX(-5px) translateY(2px) rotateZ(-5deg) rotateY(-3deg);
  }

  /* 5. 🚫 覆盖并取消 PC 端的 Hover 散开动画，防止手机端点按破版 */
  .shelf-group-stack-wrapper:hover .group-stack-3d-entity {
    transform: rotateX(8deg) rotateY(-6deg) translateY(2px); /* 维持静止 */
  }
  .shelf-group-stack-wrapper:hover .stack-layer-0 {
    transform: translateZ(10px) rotateY(-2deg);
    box-shadow: 2px 3px 8px rgba(0, 0, 0, 0.25);
  }
  .shelf-group-stack-wrapper:hover .stack-layer-1 {
    transform: translateZ(3px) translateX(6px) translateY(-2px) rotateZ(4deg) rotateY(2deg);
  }
  .shelf-group-stack-wrapper:hover .stack-layer-2 {
    transform: translateZ(-3px) translateX(-5px) translateY(2px) rotateZ(-5deg) rotateY(-3deg);
  }

  /* 👆 注入手机端专属：指尖按压时的微缩反馈 */
  .shelf-group-stack-wrapper:active .group-stack-3d-entity {
    transform: scale(0.95) translateZ(-10px);
    transition: transform 0.2s ease;
  }

  /* 6. 底部液态铭牌与微缩角标适配 */
  .group-stack-tag {
    margin-top: 10px;
    padding: 3px 8px;
    gap: 4px;
  }
  .folder-icon {
    font-size: 12px;
  }
  .folder-name {
    font-size: 11px;
    max-width: 40px; /* 保护名字过长，防止撑破手机端网格 */
  }
  /* 强制缩小 Element Plus 的 Tag 标签 */
  .folder-count-badge {
    transform: scale(0.75);
    transform-origin: left center;
    margin-left: -2px;
  }
}
</style>