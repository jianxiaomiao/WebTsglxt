<script setup>
// ======================================
// 1. 依赖导入区（原顺序完整保留，无修改）
// ======================================
import {ref, onMounted, inject, watch, computed, nextTick, onUnmounted, reactive } from 'vue'
import {createRouter as $router, useRoute, useRouter} from 'vue-router'
import request from '../utils/request.js'
import {
  ElButton, ElMessage, ElDialog, ElInput,
  ElCard, ElRow, ElCol, ElAvatar, ElDivider, ElMessageBox, ElImage
} from 'element-plus';
import { StarFilled, Comment, Picture, CaretRight, CaretTop, CaretBottom, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/userStore'
import { useAchievementStore } from '../stores/achievementStore'

// ======================================
// 2. 全局注入 & 状态初始化区
// ======================================
// Pinia 状态
const userStore = useUserStore()
const achievementStore = useAchievementStore()
// 路由实例
const route = useRoute()
const router = useRouter()
// 全局注入
const isDark = inject('isDark')
const forumSearchUserId = inject('forumSearchUserId')
const forumSearchComments = inject('forumSearchComments')
const toggleForumSort = inject('toggleForumSort')
const forumSortType = inject('forumSortType')
const forumMainComments = inject('forumMainComments')
const forumSubCommentState = inject('forumSubCommentState')
const loadSubComments = inject('loadSubComments')
const loadMoreSubComments = inject('loadMoreSubComments')
const collapseSubComments = inject('collapseSubComments')
const loadMoreMainComments = inject('loadMoreMainComments')
const isLoadingMainComments = inject('isLoadingMainComments')
const hasMoreMainComments = inject('hasMoreMainComments')
const forumComments = inject('forumComments')

// ✅ 新增：注入Basic全局@状态和方法
const showMentionList = inject('showMentionList')
const filteredMentionUsers = inject('filteredMentionUsers')
const selectedMentionIndex = inject('selectedMentionIndex')
const mentionDropdownPosition = inject('mentionDropdownPosition')
const selectMentionUser = inject('selectMentionUser')

// 环境变量
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;
// 当前用户信息
const currentUserId = inject('currentUserId')
const currentUserType=inject('currentUserType')

// ======================================
// 3. 响应式变量区（按功能模块分类）
// ======================================
// 3.1 基础评论变量
const commentContent = ref('')
const isPublishDialogShow = ref(false)

const lengthError = ref(false)
const lengthErrorMsg = ref('')



// 3.3 好友列表变量
const friendList = inject('friendList')

// 3.4 图片上传/预览变量
const uploadedImages = ref([])
const fileInput = ref(null)
const replyUploadedImages = ref([])
const replyFileInput = ref(null)
const expandedImages = reactive({})


const deletedImageIds = ref([])

// 3.5 回复评论变量
const isReplyDialogShow = ref(false)
const replyingParentId = ref(null)
const replyContent = ref('')
const replyLengthError = ref(false)
const replyLengthErrorMsg = ref('')
const replyingToCommentId = ref(null)
const replyingToUserId = ref(null)
const replyingToUserName = ref(null)

// 3.6 点赞功能变量
const likedCommentIds = inject('likedCommentIds')

// 3.7 书籍分享卡片变量
const isUpdatingBookCards = ref(false)

// 3.8 分享跳转变量
const isFromShare = ref(false)
const shareParams = ref({})
const hasAutoOpened = ref(false)

// 3.9 DOM 引用变量
const commentInputRef = ref(null)
const replyInputRef = ref(null)

// ======================================
// 🔥 优化版：便签正文折叠/展开 (0 性能开销)
// ======================================
// 仅存储展开状态，无需再收集 DOM ref！
const expandTextStatus = reactive({})

// 极速判断文本是否超长：提取纯文本长度 > 120字 就认为需要折叠
const isTextLong = (text) => {
  if (!text) return false
  return extractPureText(text).length > 120
}

const toggleTextExpand = (commentId) => {
  expandTextStatus[commentId] = !expandTextStatus[commentId]
}


// ======================================
// 4. 计算属性区
// ======================================
// 评论数据源切换
// 评论数据源切换
const displayComments = computed(() => {
  if (forumSearchUserId.value) {
    return forumComments.value || []
  }
  // 🔥 主评论现在是 { comment: ..., subTotal: ... } 结构
  return forumMainComments.value.map(item => item.comment) || []
})

// 🔥 新增：获取主评论的子评论总数
const getSubTotal = (commentId) => {
  const item = forumMainComments.value.find(i => i.comment.commentId === commentId)
  return item ? item.subTotal : 0
}

// 楼中楼模式判断
const isTreeMode = computed(() => {
  return !forumSearchUserId.value
})

// 当前用户信息
const currentUserInfo = computed(() => userStore.userInfo)

// ======================================
// 5. 通用工具函数区
// ======================================
// 计算有效文本长度
const getValidContentLength = inject('getValidContentLength')
// 时间格式化
const formatTime = inject('formatDateTime')


// ======================================
// 9. 新增：手账便签风独特不规则样式生成器
// ======================================
const noteBackgrounds = computed(() => isDark.value ?
    ['#1a221e', '#242115', '#172120', '#242217'] : // 暗黑模式：深灰绿、深暗黄、深青黑、深灰褐
    ['#f3faf4', '#fffdeb', '#edf7f5', '#fffdf0']
)

const noteBorders = computed(() => isDark.value ?
    ['#2b3b30', '#3b3420', '#243632', '#363222'] :
    ['#cfead4', '#ebdca5', '#cceee2', '#e3d69c']
)

const getStickyNoteStyle = (index) => {
  const bg = noteBackgrounds.value[index % noteBackgrounds.value.length]
  const border = noteBorders.value[index % noteBorders.value.length]
  // 错落有致的微小旋转（-1.5度 到 +1.5度之间），营造纯手工粘贴的凌乱美
  const rotate = (index % 2 === 0 ? 1 : -1) * (0.4 + (index % 3) * 0.4)
  return {
    backgroundColor: bg,
    borderColor: border,
    transform: `rotate(${rotate}deg)`
  }
}

// 纸胶带颜色池（支持暗夜模式切换）
const tapeColors = computed(() => isDark.value ?
    [
      'rgba(68, 108, 110, 0.4)', // 暗·盐系蓝
      'rgba(135, 76, 76, 0.4)',  // 暗·柔粉
      'rgba(120, 94, 38, 0.4)',  // 暗·芥末黄
      'rgba(62, 97, 66, 0.4)'    // 暗·薄荷绿
    ] :
    [
      'rgba(168, 218, 220, 0.5)', // 盐系蓝
      'rgba(241, 166, 166, 0.5)', // 柔粉
      'rgba(233, 196, 106, 0.5)', // 芥末黄
      'rgba(138, 201, 143, 0.5)'  // 薄荷绿
    ]
)

const getTapeStyle = (index) => {
  const color = tapeColors.value[index % tapeColors.value.length]
  // 胶带也来一点随机倾斜
  const rotate = (index % 2 === 0 ? -4 : 3) + (index % 3)
  return {
    backgroundColor: color,
    transform: `rotate(${rotate}deg)`
  }
}
// ======================================
// 6. 业务功能函数区（按模块分类）
// ======================================
// 🔥 优化：给解析文本和图片地址加上缓存，避免 Vue 疯狂重新计算
const parsedTextCache = new Map()
const parsedPreviewCache = new Map()

// 缓存文本解析结果
const getParsedContent = (commentId, text) => {
  if (!parsedTextCache.has(commentId)) {
    parsedTextCache.set(commentId, formatMentionText(parseBookLinkToCard(text)))
  }
  return parsedTextCache.get(commentId)
}

// ======================================
// 🔥 新增：纯文本与卡片分离 & 缓存解析
// ======================================
// 正则表达式：匹配类似 [{book:xxx}] 的标签
const cardReg = /\[{(\w+):([a-zA-Z0-9_-]+)}\]/g

// 提取卡片数组
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

// 提取剔除了标签后的纯文本
const extractPureText = (content) => {
  if (!content) return ''
  return content.replace(/\[{(\w+):([a-zA-Z0-9_-]+)}\]/g, '').trim()
}

const parsedForumContentCache = new Map()

// 核心：缓存提取结果（包含纯文本 text 和卡片数组 cards）
const getParsedForumContent = (commentId, text) => {
  if (!parsedForumContentCache.has(commentId)) {
    const pureText = extractPureText(text)
    const cards = extractBookCards(text)
    // 文本部分需要处理 @ 艾特高亮
    const formattedText = formatMentionText(pureText)
    parsedForumContentCache.set(commentId, { text: formattedText, cards })
  }
  return parsedForumContentCache.get(commentId)
}

// 左右滚动函数（给箭头绑定的方法）
const scrollCards = (commentId, direction) => {
  const container = document.getElementById(`scroll-view-forum-${commentId}`)
  if (!container) return
  const scrollAmount = 180
  if (direction === 'left') {
    container.scrollBy({ left: -scrollAmount, behavior: 'smooth' })
  } else {
    container.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// 缓存图片预览数组
const getPreviewList = (commentId, images) => {
  if (!parsedPreviewCache.has(commentId)) {
    parsedPreviewCache.set(commentId, images.map(item => IMAGE_BASE_URL + item.imageUrl))
  }
  return parsedPreviewCache.get(commentId)
}
// 6.1 好友列表相关函数
const getFriendList = inject('getFriendList')
const getUserInfo = inject('getUserInfo')
// 6.2 @艾特功能核心函数
const handleMentionInput = inject('handleMentionInput')
const handleMentionKeydown = inject('handleMentionKeydown')
const formatMentionText = inject('formatMentionText')
const handleClickOutside = inject('closeMentionListOnClickOutside')
const currentMentionInput = inject('currentMentionInput')
const handleInput = (val, inputRef) => {
  currentMentionInput.value = inputRef

  if (val.includes('@')) {
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
        String(user.fiendId).includes(searchText)
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
}
// 🔥 复用你原来的选择用户逻辑（永不报错！）
const selectUser = (user) => {
  const atText = `@${user.userName}(${user.friendId}) `
  if (isReplyDialogShow.value) {
    replyContent.value = replyContent.value.replace(/@$/, atText)
  } else {
    commentContent.value = commentContent.value.replace(/@$/, atText)
  }
  showMentionList.value = false // 选中立即关闭列表
}
// 6.3 书籍分享卡片函数
const bookShortLinkReg = inject('bookShortLinkReg')
const parseBookLinkToCard = inject('parseBookLinkToCard')
const gotoBookDetail = inject('gotoBookDetail')
const updateBookShareCards = inject('updateAllBookCards')

// 6.4 图片上传/删除函数
const triggerUpload = () => {
  if (uploadedImages.value.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }
  fileInput.value.click()
}
const handleFileUpload = async (e) => {
  const files = e.target.files
  console.log('【发表评论-上传图片】选中的文件列表：', files)
  if (!files || files.length === 0) return

  if (uploadedImages.value.length + files.length > 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }

  for (let file of files) {
    const formData = new FormData()
    formData.append('action', 'upload')
    formData.append('commentId', '0')
    formData.append('image', file)

    console.log('【发表评论-上传图片】发送的FormData：action=upload, commentId=0, 图片文件', file)
    try {
      const res = await request.post('/user/comment/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      console.log('【发表评论-上传图片】后端返回结果：', res)
      if (res.code === 200) {
        console.log('【发表评论-上传图片】上传成功的图片对象：', res.data)
        uploadedImages.value.push(res.data)
      } else {
        ElMessage.error(`上传失败：${res.msg}`)
      }
    } catch (err) {
      console.error('【发表评论-上传图片】请求报错：', err)
      ElMessage.error('上传图片失败')
    }
  }
  fileInput.value.value = ''
}
const deleteImage = async (index) => {
  const img = uploadedImages.value[index]
  console.log('【发表评论-删除图片】要删除的图片对象：', img)
  try {
    const formData = new FormData()
    formData.append('action', 'delete')
    formData.append('imageId', img.id)

    console.log('【发表评论-删除图片】发送的FormData：action=delete, imageId=', img.id)
    const res = await request.post('/user/comment/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    console.log('【发表评论-删除图片】后端返回结果：', res)
    uploadedImages.value.splice(index, 1)
  } catch (err) {
    console.error('【发表评论-删除图片】请求报错：', err)
    ElMessage.error('删除图片失败')
  }
}
const triggerReplyUpload = () => {
  if (replyUploadedImages.value.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }
  replyFileInput.value.click()
}
const handleReplyFileUpload = async (e) => {
  const files = e.target.files
  console.log('【回复评论-上传图片】选中的文件列表：', files)
  if (!files || files.length === 0) return

  if (replyUploadedImages.value.length + files.length > 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }

  for (let file of files) {
    const formData = new FormData()
    formData.append('action', 'upload')
    formData.append('commentId', '0')
    formData.append('image', file)
    console.log('【回复评论-上传图片】发送的FormData：action=upload, commentId=0, 图片文件', file)

    try {
      const res = await request.post('/user/comment/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      console.log('【回复评论-上传图片】后端返回结果：', res)
      if (res.code === 200) {
        console.log('【回复评论-上传图片】上传成功的图片对象：', res.data)
        replyUploadedImages.value.push(res.data)
      } else {
        ElMessage.error(`上传失败：${res.msg}`)
      }
    } catch (err) {
      console.error('【回复评论-上传图片】请求报错：', err)
      ElMessage.error('上传图片失败')
    }
  }
  replyFileInput.value.value = ''
}
const deleteReplyImage = async (index) => {
  const img = replyUploadedImages.value[index]
  console.log('【回复评论-删除图片】要删除的图片对象：', img)
  try {
    const formData = new FormData()
    formData.append('action', 'delete')
    formData.append('imageId', img.id)

    console.log('【回复评论-删除图片】发送的FormData：action=delete, imageId=', img.id)
    const res = await request.post('/user/comment/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    console.log('【回复评论-删除图片】后端返回结果：', res)
    replyUploadedImages.value.splice(index, 1)
  } catch (err) {
    console.error('【回复评论-删除图片】请求报错：', err)
    ElMessage.error('删除图片失败')
  }
}
const updatePreviewBookCards = inject('updatePreviewBookCards')
const previewBookCards = inject('previewBookCards')

// 6.5 核心评论操作函数
const checkCommentLength = () => {
  const validLen = getValidContentLength(commentContent.value)
  if (validLen < 1 && uploadedImages.value.length === 0) {
    lengthError.value = true
    lengthErrorMsg.value = '评论内容或图片不能为空！'
  } else if (validLen > 2000) {
    lengthError.value = true
    lengthErrorMsg.value = '评论内容不能超过2000字！当前长度：' + validLen
  } else {
    lengthError.value = false
    lengthErrorMsg.value = ''
    if (commentContent.value.length > 2000) {
      commentContent.value = commentContent.value.slice(0, 2000)
    }
  }
}
const openPublishDialog = () => {
  isPublishDialogShow.value = true
  commentContent.value = ''
  uploadedImages.value = []
  lengthError.value = false
}
const closePublishDialog = () => {
  isPublishDialogShow.value = false
  commentContent.value = ''
  uploadedImages.value = []
  lengthError.value = false
  showMentionList.value = false
  previewBookCards.value = [] // ✅ 关闭时清空预览
}
const submitComment = async () => {
  checkCommentLength()
  if (lengthError.value) return
  try {
    const commentData = {
      UserComment: commentContent.value.trim(),
      Userid: currentUserId.value,
      CommentTime: Date.now()
    }
    console.log('【提交评论】发送的评论数据：', commentData)
    const res = await request.post('/user/comment', commentData)
    console.log('【提交评论】后端返回结果：', res)

    if (res.code === 200) {
      const commentId = res.data
      console.log('【提交评论】成功获取评论ID：', commentId)
      console.log('【提交评论】待绑定的图片列表：', uploadedImages.value)

      if (uploadedImages.value.length > 0) {
        for (let img of uploadedImages.value) {
          if (Number.isInteger(img.id) && img.id < 1000000) {
            const formData = new FormData()
            formData.append('action', 'updateCommentId')
            formData.append('imageId', img.id)
            formData.append('newCommentId', commentId)

            console.log('【绑定图片】发送的FormData：action=updateCommentId, imageId=', img.id, ', newCommentId=', commentId)
            const bindRes = await request.post('/user/comment/image', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            console.log('【绑定图片】后端返回结果：', bindRes)
          } else {
            const formData = new FormData()
            formData.append('action', 'uploadByUrl')
            formData.append('commentId', commentId)
            formData.append('imageUrl', img.imageUrl)

            console.log('【上传网络图片】发送的FormData：action=uploadByUrl, commentId=', commentId, ', imageUrl=', img.imageUrl)
            const uploadRes = await request.post('/user/comment/image', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            console.log('【上传网络图片】后端返回结果：', uploadRes)
          }
        }

      }else {
        console.log('【提交评论】无图片，跳过绑定')
      }
      ElMessage.success('评论发表成功，系统正在审核中~')
      achievementStore.checkAchievements()
      isPublishDialogShow.value = false
      uploadedImages.value = []
      previewBookCards.value = [] // ✅ 清空预览卡片
      await forumSearchComments()
      await nextTick()
      updateBookShareCards() // ✅ 更新页面中所有书籍卡片
    } else {
      ElMessage.error(res.msg || '评论发表失败')
    }
  } catch (err) {
    console.error('【提交评论】请求报错：', err)
    ElMessage.error('请求出错啦，请稍后再试~')
  }
}

const handleDelete = async (commentId) => {
  try {
    await ElMessageBox.confirm(
        '确定要删除这条评论吗？',
        '删除提示',
        {
          type: 'warning',
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          center: true,
          reverseButtons: true
        }
    )
    const res = await request.delete('/user/comment', { params: { commentId: commentId } })
    if (res.code === 200) {
      ElMessage.success({
        message: '评论删除成功！',
        center: true
      })
      await forumSearchComments()
      editingCommentId.value = null
    } else {
      ElMessage.error({
        message: res.msg || '评论删除失败',
        center: true
      })
    }
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error({
      message: '删除出错啦，请稍后再试~',
      center: true
    })
    console.error('删除评论失败：', err)
  }
}

const handleDialogOpen = () => {
  if (isFromShare.value && shareParams.value.content) {
    commentContent.value = shareParams.value.content

    if (shareParams.value.image) {
      const fakeImageObj = {
        id: Date.now(),
        imageUrl: shareParams.value.image,
        commentId: 0,
        uploadTime: Date.now()
      }
      uploadedImages.value.push(fakeImageObj)
    }

    shareParams.value = {}
    isFromShare.value = false
    // ✅ 【核心修复】赋值后手动调用一次书籍预览更新
    nextTick(() => {
      updatePreviewBookCards(commentContent.value)
    })
  }
}

// 6.6 回复评论函数
const checkReplyLength = () => {
  const validLen = getValidContentLength(replyContent.value)
  if (validLen < 1 && replyUploadedImages.value.length === 0) {
    replyLengthError.value = true
    replyLengthErrorMsg.value = '回复内容或图片不能为空！'
  } else if (validLen > 2000) {
    replyLengthError.value = true
    replyLengthErrorMsg.value = '回复内容不能超过2000字！'
  } else {
    replyLengthError.value = false
    replyLengthErrorMsg.value = ''
  }
}
const openReplyDialog = (comment) => {
  replyingParentId.value = comment.parentId === 0 ? comment.commentId : comment.parentId
  replyingToCommentId.value = comment.commentId
  replyingToUserId.value = comment.userid
  replyingToUserName.value = comment.userName
  replyContent.value = ''
  replyUploadedImages.value = []
  replyLengthError.value = false
  isReplyDialogShow.value = true
}
const closeReplyDialog = () => {
  isReplyDialogShow.value = false
  replyContent.value = ''
  replyUploadedImages.value = []
  replyLengthError.value = false
  replyingParentId.value = null
  replyingToCommentId.value = null
  replyingToUserId.value = null
  replyingToUserName.value = null
  showMentionList.value = false // ✅ 改为全局变量
  previewBookCards.value = [] // ✅ 新增：关闭回复弹窗清空预览
}
const submitReply = async () => {
  checkReplyLength()
  if (replyLengthError.value) return
  try {
    const replyData = {
      UserComment:  replyContent.value.trim(),
      Userid: currentUserId.value,
      CommentTime: Date.now(),
      parentId: replyingParentId.value,
      replyToCommentId: replyingToCommentId.value,
      replyToUserId: replyingToUserId.value
    }
    console.log('【提交回复】发送的回复数据：', replyData)
    const res = await request.post('/user/comment', replyData)
    console.log('【提交回复】后端返回结果：', res)

    if (res.code === 200) {
      const commentId = res.data
      console.log('【提交回复】成功获取回复ID：', commentId)
      console.log('【提交回复】待绑定的图片列表：', replyUploadedImages.value)

      if (replyUploadedImages.value.length > 0) {
        for (let img of replyUploadedImages.value) {
          const formData = new FormData()
          formData.append('action', 'updateCommentId')
          formData.append('imageId', img.id)
          formData.append('newCommentId', commentId)

          console.log('【回复-绑定图片】发送的FormData：action=updateCommentId, imageId=', img.id, ', newCommentId=', commentId)
          const bindRes = await request.post('/user/comment/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          console.log('【回复-绑定图片】后端返回结果：', bindRes)
        }
      }else{
        console.log('【提交回复】无图片，跳过绑定')
      }
      ElMessage.success('回复成功，系统正在审核中~')
      isReplyDialogShow.value = false
      replyUploadedImages.value = []
      previewBookCards.value = [] // ✅ 新增：提交回复后清空预览
      await forumSearchComments()
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (err) {
    console.error('【提交回复】请求报错：', err)
    ElMessage.error('请求出错啦，请稍后再试~')
  }
}

// 6.7 点赞功能函数
const getUserLikedComments = inject('getUserLikedComments');
const likeComment = async (commentId) => {
  try {
    const res = await request.post('/user/comment/like', {}, {
      params: {
        userId: currentUserId.value,
        commentId: commentId
      }
    });
    if (res.code === 200) {
      ElMessage.success("点赞\\取消点赞成功");
      achievementStore.checkAchievements()
      await forumSearchComments();
      await getUserLikedComments();
    } else {
      ElMessage.error(res.msg);
    }
  } catch (err) {
    ElMessage.error('操作失败');
  }
};

// 6.8 添加好友函数
const handleAddFriend = async (targetUserId, targetUserName) => {
  if (targetUserId === currentUserId.value) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt(
        `向 ${targetUserName} (${targetUserId}) 发送好友申请`,
        '添加好友',
        {
          confirmButtonText: '发送',
          cancelButtonText: '取消',
          type: 'info',
          inputPlaceholder: '请输入申请消息（选填）',
          inputValidator: (val) => {
            if (val && val.length > 50) return '申请消息不能超过50字'
            return true
          }
        }
    )

    const requestMsg = value?.trim() || `你好，我是${currentUserInfo.value?.name || currentUserId.value}，通过论坛认识你的~`

    const res = await request.post('/user/friend/request', null, {
      params: {
        action: 'send',
        fromUserId: currentUserId.value,
        toUserId: targetUserId,
        requestMsg: requestMsg
      }
    })

    if (res.code === 200) {
      ElMessage.success('好友申请已发送，请等待对方同意')
    } else {
      ElMessage.error(res.msg || '发送好友申请失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('发送好友申请失败', err)
      ElMessage.error('发送失败，请稍后再试')
    }
  }
}

// 6.9 搜索用户函数
const searchUsers = inject('searchUsers')

// ======================================
// 7. 生命周期钩子区
// ======================================
// ✅ 新增：骨架屏初始加载状态（仅第一次加载显示骨架）
const isInitialLoading = ref(true)
// ✅ 新增：论坛首次加载标记（核心！控制只加载一次）
const hasLoadedForum = ref(false)
// ✅ 新增：第一次加载完成后关闭骨架屏
watch(isLoadingMainComments, (newVal) => {
  // 当加载状态从 true 变为 false，且是第一次加载时，关闭骨架屏
  if (!newVal && isInitialLoading.value) {
    isInitialLoading.value = false
  }
})
// 监听评论列表变化（新增/加载更多子评论/图片加载完成），重新检测高度
watch(
    [displayComments, forumSubCommentState],
    async () => {
      await nextTick()
      // 重置DOM引用 + 状态（可选，防止数据更新后判断失效）
      Object.keys(noteContentRefs).forEach(key => delete noteContentRefs[key])
    },
    { deep: true }
)
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  const { query } = route
  if (query.content) {
    shareParams.value = {
      content: decodeURIComponent(query.content),
      image: query.image || ''
    }
    isFromShare.value = true
    history.replaceState(null, '', '/forum')
  }

  // ✅ 核心：仅第一次进入页面，加载第一页评论
  if (!hasLoadedForum.value) {
    // 加载评论
    forumSearchComments()
    // 标记为已加载，后续永远不再自动刷新
    hasLoadedForum.value = true
  }
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// ======================================
// 8. 监听器区
// ======================================
watch(
    [forumMainComments, forumComments],
    async (newValues) => {
      await nextTick()
      updateBookShareCards()

      if (isFromShare.value && !hasAutoOpened.value && (newValues[0].length > 0 || newValues[1].length > 0)) {
        await nextTick(() => {
          isPublishDialogShow.value = true
          hasAutoOpened.value = true
        })
      }
    },
    { deep: true, immediate: true }
)
</script>

<template>

  <div class="publish-btn-wrap">
    <div class="forum-header">
      <el-affix :offset="40">
      <div class="forum-sort-group">
        <!-- 点击加载更多按钮 -->
        <ElButton
            v-if="hasMoreMainComments && forumMainComments.length > 0"
            size="small"
            :loading="isLoadingMainComments"
            @click="loadMoreMainComments"
            type="success"
            plain
            round
        >
          加载更多
        </ElButton>
        <ElButton size="small" :type="forumSortType === 'time' ? 'primary' : 'default'" @click="toggleForumSort('time')" round>按时间</ElButton>
        <ElButton size="small" :type="forumSortType === 'prefer' ? 'primary' : 'default'" @click="toggleForumSort('prefer')" round>按点赞</ElButton>
        <ElButton size="small" type="primary" @click="openPublishDialog" round>发表评论</ElButton>
      </div>
      </el-affix>
    </div>
  </div>

<!--  <div class="book-card-container" style="min-height: calc(100vh - 190px); overflow-y: auto; padding: 0 10px; ">-->
  <div class="book-card-container" style=" overflow-y: auto; padding: 0 10px; overflow-x: hidden;">

      <!-- ✅ 骨架屏 + 评论列表 区域 -->
      <el-skeleton
          :loading="isInitialLoading"
          animated
          :throttle="300"
          style="width: 100%"
      >
        <!-- 骨架屏模板：模拟真实评论的结构 -->
        <template #template>
          <div class="skeleton-comment-list">
            <!-- 生成3个骨架项，和初始加载的评论数量匹配 -->
            <div v-for="i in 3" :key="i" class="skeleton-comment-item">
              <el-row align="top" :gutter="16">
                <!-- 头像骨架 -->
                <el-col :span="2">
                  <el-skeleton-item variant="circle" style="width: 40px; height: 40px;" />
                </el-col>
                <!-- 评论内容骨架 -->
                <el-col :span="22">
                  <!-- 用户名 + 时间骨架 -->
                  <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
                    <el-skeleton-item variant="text" style="width: 100px;" />
                    <el-skeleton-item variant="text" style="width: 60px;" />
                  </div>
                  <!-- 分割线骨架 -->
                  <el-skeleton-item variant="rect" style="height: 2px; margin-bottom: 12px;" />
                  <!-- 评论正文骨架（多行） -->
                  <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 8px;" />
                  <el-skeleton-item variant="text" style="width: 80%; margin-bottom: 8px;" />
                  <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 16px;" />
                  <!-- 点赞 + 回复按钮骨架 -->
                  <div style="display: flex; gap: 16px;">
                    <el-skeleton-item variant="text" style="width: 40px;" />
                    <el-skeleton-item variant="text" style="width: 40px;" />
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </template>

        <!-- 真实内容：加载完成后显示 -->
        <template #default>
          <!-- 空状态提示：无评论时显示 -->
          <div v-if="forumMainComments.length === 0" class="empty-tip">
            暂无评论，快来抢沙发吧~
          </div>
          <!-- 🔥 核心改造：替换原本的 ElRow / ElCol 布局 -->
          <div v-else class="comment-masonry-container">
            <div
                v-for="(comment, index) in displayComments"
                :key="comment.commentId"
                class="masonry-item"
            >
              <!-- 🎀 顶部手账纸胶带装饰，极具灵魂 -->
              <div class="washi-tape" :style="getTapeStyle(index)"></div>

              <!-- 原有的 ElCard 完全不动，只加了个 sticky-note-card 类名 -->
              <ElCard
                  class="card rotate-card sticky-note-card"
                  :data-comment-id="comment.commentId"
                  style="border-radius:8px; border: 1px dashed #555;"
              >
          <!-- 原有卡片内部所有内容完全不动，全部保留 -->
          <el-row align="top" :gutter="16">
            <!-- 主评论头像 -->

            <!-- 主评论内容 -->
            <el-col :span="22">

              <!-- 原有所有内容：评论头部、正文、图片、点赞回复等 全部保留 -->
              <div class="comment-header">
                <div class="comment-header-left">
          <span style="font-weight: 600; cursor: pointer; color: #409eff;"
                @click="router.push('/user/profile?userId=' + comment.userid)">
            {{ comment.userName }}
          </span>
                  <span style="font-size: 12px; color: #999;">({{ comment.userid }})</span>
                </div>
                <div class="comment-header-right">
                  <span style="font-size: 12px; color: #999;">{{ formatTime(comment.commentTime) }}</span>
                  <ElButton
                      v-if="comment.userid === currentUserId || currentUserType === 3"
                      @click="handleDelete(comment.commentId)"
                      type="link"
                      class="edit-link-btn"
                      round
                  >
                    删除
                  </ElButton>
                </div>
              </div>

              <el-divider content-position="left" style="border-color: #eee; border-width: 2px"></el-divider>

              <!-- 主评论内容 -->
                <div class="text-content-wrapper">
                  <div
                      class="comment-text-body"
                      :class="{ 'text-clamped': !expandTextStatus[comment.commentId] }"
                      :style="{ marginTop: '8px', fontSize: '14px', color: isDark ? '#e5e7eb' : '#333', textAlign: 'left', whiteSpace: 'pre-wrap', wordWrap: 'break-word' }"
                      v-html="getParsedForumContent(comment.commentId, comment.userComment).text"
                  ></div>
                  <div
                      v-if="isTextLong(comment.userComment)"
                      class="expand-text-btn"
                      @click="toggleTextExpand(comment.commentId)"
                  >
                    {{ expandTextStatus[comment.commentId] ? '收起全文' : '展开全文 ↓' }}
                  </div>
                </div>

                <div v-if="getParsedForumContent(comment.commentId, comment.userComment).cards.length > 0" class="message-cards-container" style="margin-top: 8px;">
                  <div v-if="getParsedForumContent(comment.commentId, comment.userComment).cards.length === 1" class="book-share-card-wrapper">
                    <div v-html="parseBookLinkToCard(getParsedForumContent(comment.commentId, comment.userComment).cards[0].link, false)"></div>
                  </div>
                  <div v-else class="multi-card-slider-wrapper">
                    <div class="slider-arrow left" @click.stop="scrollCards(comment.commentId, 'left')">
                      <el-icon><ArrowLeft /></el-icon>
                    </div>
                    <div class="multi-card-scroll-view" :id="'scroll-view-forum-' + comment.commentId">
                      <div
                          v-for="card in getParsedForumContent(comment.commentId, comment.userComment).cards"
                          :key="card.id"
                          class="slider-item"
                          v-html="parseBookLinkToCard(card.link, true)"
                      ></div>
                    </div>
                    <div class="slider-arrow right" @click.stop="scrollCards(comment.commentId, 'right')">
                      <el-icon><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>
              <div v-if="comment.images && comment.images.length > 0" class="comment-images">
                <template v-if="expandedImages[comment.commentId] || comment.images.length <= 2">
                  <el-image
                      lazy
                      v-for="(img, index) in comment.images"
                      :key="img.id"
                      :src="IMAGE_BASE_URL + img.imageUrl"
                      :preview-src-list="getPreviewList(comment.commentId, comment.images)"
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
                        :src="IMAGE_BASE_URL + comment.images[0].imageUrl"
                        :preview-src-list="getPreviewList(comment.commentId, comment.images)"
                        preview-teleported
                        fit="cover"
                        alt="评论图片"
                        class="comment-img"
                    />
                  <div class="image-overlay-wrapper" @click="expandedImages[comment.commentId] = true">
                    <el-image
                        lazy
                        :src="IMAGE_BASE_URL + comment.images[1].imageUrl"
                        fit="cover"
                        alt="评论图片"
                        class="comment-img"
                    />
                    <div class="expand-mask">
                      +{{ comment.images.length - 2 }}张
                    </div>
                  </div>
                </template>
              </div>
              <!-- 点赞+回复 -->
              <div v-if="isTreeMode" style="margin-top: 12px; padding-top: 8px; border-top: 2px solid #e5e7eb; display: flex; align-items: center; gap: 16px; color: #999; font-size: 14px;">
                <div style="display: flex; align-items: center; gap: 4px; cursor: pointer;" @click="likeComment(comment.commentId)">
                  <el-icon style="width: 18px; height: 18px;" :color="likedCommentIds.includes(comment.commentId) ? '#ff4d4f' : '#999'">
                    <StarFilled />
                  </el-icon>
                  <span v-if="comment.prefer > 0">{{ comment.prefer }}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 4px; cursor: pointer;" @click="openReplyDialog(comment)">
                  <el-icon style="width: 18px; height: 18px; color: #999;"><Comment /></el-icon>
                  <span v-if="getSubTotal(comment.commentId) > 0">{{ getSubTotal(comment.commentId) }}</span>
                </div>
              </div>

              <!-- 子评论区域 -->
              <div v-if="isTreeMode" class="sub-comments">
                <!-- 原有子评论内容 全部保留 -->
                <div
                    v-if="!forumSubCommentState[comment.commentId]?.list.length && getSubTotal(comment.commentId) > 0"
                    class="expand-sub-btn"
                    @click="loadSubComments(comment.commentId)"
                >
                  展开 {{ getSubTotal(comment.commentId) }} 条回复
                </div>

                <template v-else-if="forumSubCommentState[comment.commentId]?.list.length > 0">
                  <div
                      v-for="subComment in forumSubCommentState[comment.commentId].list"
                      :key="subComment.commentId"
                      class="sub-comment-item"
                  >
                    <!-- 子评论原有内容 全部保留 -->
                    <div class="sub-comment-header">
                      <div class="sub-comment-header-left" style="display: flex; align-items: center; gap: 4px; flex-wrap: wrap;">
                        <span style="font-weight: 600; font-size: 13px; cursor: pointer; color: #409eff;"
                              @click="router.push('/user/profile?userId=' + subComment.userid)">
                          {{ subComment.userName }}
                        </span>
                        <span style="font-size: 11px; color: #999;">({{ subComment.userid }})</span>

                        <template v-if="subComment.replyToUserId && subComment.replyToCommentId !== subComment.parentId">
                          <el-icon style="width: 12px; height: 12px; color: #999; margin: 0 2px;"><CaretRight /></el-icon>
                          <span style="font-size: 13px; color: #409eff; cursor: pointer;" @click="router.push('/user/profile?userId=' + subComment.replyToUserId)">回复@{{ subComment.replyToUserName || '用户' }}</span>
                          <span style="font-size: 11px; color: #999; cursor: pointer;" @click="router.push('/user/profile?userId=' + subComment.replyToUserId)">({{ subComment.replyToUserId }})</span>
                        </template>
                      </div>
                      <div class="sub-comment-header-right">
                        <span style="font-size: 11px; color: #999;">{{ formatTime(subComment.commentTime) }}</span>
                        <ElButton
                            v-if="subComment.userid === currentUserId || currentUserType === 3"
                            @click="handleDelete(subComment.commentId)"
                            type="link"
                            class="edit-link-btn"
                            round
                        >
                          删除
                        </ElButton>
                      </div>
                    </div>

                    <!-- 子评论内容 -->
                    <div
                        :style="{ fontSize: '13px', color: isDark ? '#d1d5db' : '#555', textAlign: 'left', whiteSpace: 'pre-wrap', wordWrap: 'break-word', marginBottom: '8px' }"
                        v-html="getParsedForumContent(subComment.commentId, subComment.userComment).text"
                    ></div>

                    <div v-if="getParsedForumContent(subComment.commentId, subComment.userComment).cards.length > 0" class="message-cards-container" style="margin-bottom: 8px;">
                      <div v-if="getParsedForumContent(subComment.commentId, subComment.userComment).cards.length === 1" class="book-share-card-wrapper">
                        <div v-html="parseBookLinkToCard(getParsedForumContent(subComment.commentId, subComment.userComment).cards[0].link, false)"></div>
                      </div>
                      <div v-else class="multi-card-slider-wrapper">
                        <div class="slider-arrow left" @click.stop="scrollCards(subComment.commentId, 'left')">
                          <el-icon><ArrowLeft /></el-icon>
                        </div>
                        <div class="multi-card-scroll-view" :id="'scroll-view-forum-' + subComment.commentId">
                          <div
                              v-for="card in getParsedForumContent(subComment.commentId, subComment.userComment).cards"
                              :key="card.id"
                              class="slider-item"
                              v-html="parseBookLinkToCard(card.link, true)"
                          ></div>
                        </div>
                        <div class="slider-arrow right" @click.stop="scrollCards(subComment.commentId, 'right')">
                          <el-icon><ArrowRight /></el-icon>
                        </div>
                      </div>
                    </div>
                    <!-- 子评论图片（折叠功能） -->
                    <div v-if="subComment.images && subComment.images.length > 0" class="sub-comment-images">
                      <template v-if="expandedImages[subComment.commentId] || subComment.images.length <= 2">
                        <el-image
                            lazy
                            v-for="(img, index) in subComment.images"
                            :key="img.id"
                            :src="IMAGE_BASE_URL + img.imageUrl"
                            :preview-src-list="getPreviewList(subComment.commentId, subComment.images)"
                            :initial-index="index"
                            preview-teleported
                            fit="cover"
                            alt="回复图片"
                            class="sub-comment-img"
                        />
                      </template>
                      <template v-else>
                        <el-image
                            lazy
                            :src="IMAGE_BASE_URL + subComment.images[0].imageUrl"
                            :preview-src-list="getPreviewList(subComment.commentId, subComment.images)"
                            preview-teleported
                            fit="cover"
                            alt="回复图片"
                            class="sub-comment-img"
                        />
                        <div class="sub-image-overlay-wrapper" @click="expandedImages[subComment.commentId] = true">
                          <el-image
                              lazy
                              :src="IMAGE_BASE_URL + subComment.images[1].imageUrl"
                              fit="cover"
                              alt="回复图片"
                              class="sub-comment-img"
                          />
                          <div class="expand-mask sub-expand-mask">
                            +{{ subComment.images.length - 2 }}张
                          </div>
                        </div>
                      </template>
                    </div>

                    <!-- 子评论点赞+回复 -->
                    <div style="display: flex; align-items: center; gap: 16px; color: #999; font-size: 12px;">
                      <div style="display: flex; align-items: center; gap: 4px; cursor: pointer;" @click="likeComment(subComment.commentId)">
                        <el-icon style="width: 16px; height: 16px;" :color="likedCommentIds.includes(subComment.commentId) ? '#ff4d4f' : '#999'">
                          <StarFilled />
                        </el-icon>
                        <span v-if="subComment.prefer > 0">{{ subComment.prefer }}</span>
                      </div>
                      <div style="display: flex; align-items: center; gap: 4px; cursor: pointer;" @click="openReplyDialog(subComment)">
                        <el-icon style="width: 16px; height: 16px; color: #999;"><Comment /></el-icon>
                      </div>
                    </div>
                  </div>
                  <!-- 展开更多 & 收起按钮 -->
                  <div class="sub-comment-actions">
            <span
                v-if="forumSubCommentState[comment.commentId].list.length < forumSubCommentState[comment.commentId].total"
                class="action-btn"
                style="align-items : flex-start"
                :class="{ loading: forumSubCommentState[comment.commentId].loading }"
                @click="!forumSubCommentState[comment.commentId].loading && loadMoreSubComments(comment.commentId)"
            >
              <el-icon><CaretBottom /></el-icon>
              {{ forumSubCommentState[comment.commentId].loading ? '加载中...' : '展开更多回复' }}
            </span>
                    <span class="action-btn" style="align-items : flex-start" @click="collapseSubComments(comment.commentId)">
              <el-icon><CaretTop /></el-icon>
              收起回复
            </span>
                  </div>
                </template>
              </div>

            </el-col>
          </el-row>
        </ElCard>
            </div>
          </div>
        </template>
      </el-skeleton>
      <!-- 没有更多了提示 -->
      <div v-if="!hasMoreMainComments && forumMainComments.length > 0" class="no-more-tip">
        没有更多评论了
      </div>
    <!-- 发表评论弹窗 -->
    <!-- Forum.vue - 发表评论弹窗 -->
    <ElDialog
        v-model="isPublishDialogShow"
        title="发表我的评论"
        width="500px"
        :before-close="closePublishDialog"
        @open="handleDialogOpen"
    >
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
      <!-- 发表评论弹窗的输入框 -->
      <ElInput
          v-model="commentContent"
          type="textarea"
          placeholder="请输入你的评论内容，@用户可以艾特他..."
          :autosize="{ minRows: 4, maxRows: 15 }"
          ref="commentInputRef"
      @input="(value) => {
      checkCommentLength();
      handleInput(value,commentInputRef);
      updatePreviewBookCards(value);
      }"
      @keydown="handleMentionKeydown"
      @blur="checkCommentLength"
      />
      <p v-if="lengthError" style="font-size: 12px; color: #ff4d4f; margin: 4px 0 0 0;">{{ lengthErrorMsg }}</p>
      <div class="image-upload-area" style="margin-top: 15px;">
        <div class="image-list">
          <div v-for="(img, index) in uploadedImages" :key="img.id" class="image-item">
            <img v-if="img.id" :src="IMAGE_BASE_URL + img.imageUrl" alt="上传图片">
            <div class="delete-btn" @click="deleteImage(index)">×</div>
          </div>
          <div v-if="uploadedImages.length < 9" class="upload-btn" @click="triggerUpload">
            <el-icon size="24" color="#ccc"><Picture /></el-icon>
          </div>
        </div>
        <input ref="fileInput" type="file" multiple accept="image/*,.jpg,.png,.jpeg,.gif" style="display: none;" @change="handleFileUpload">
        <div style="font-size: 12px; color: #999; margin-top: 5px;">最多上传9张图片</div>
      </div>
      <template #footer>
        <ElButton type="primary" @click="submitComment" round>确定</ElButton>
        <ElButton @click="closePublishDialog" round>取消</ElButton>
      </template>
    </ElDialog>

    <!-- 回复评论弹窗 -->
    <ElDialog v-model="isReplyDialogShow" title="回复评论" width="500px" :before-close="closeReplyDialog">
      <!-- ✅ 新增：输入框上方实时预览书籍卡片 -->
      <div v-if="previewBookCards.length > 0" style="margin-bottom: 15px;">
        <div
            v-for="card in previewBookCards"
            :key="card.isbn"
            class="book-share-card"
            style="max-width: 100%;"
        >
          <img
              v-if="!card.loading"
              :src="IMAGE_BASE_URL + (card.book?.pictureName || '/default-book.png')"
              class="book-share-card-cover"
              alt="书籍封面"
          >
          <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
          <div class="book-share-card-info">
            <p class="book-title">{{ card.loading ? '加载中...' : card.book?.bookname || '未知书籍' }}</p>
            <p class="book-author">{{ card.loading ? '' : `作者：${card.book?.author || '未知'}` }}</p>
            <p class="book-rating">{{ card.loading ? '' : `⭐ 评分：${card.book?.star || 0}` }}</p>
            <p class="book-desc">{{ card.loading ? '' : (card.book?.information?.slice(0, 50) + '...' || '暂无简介') }}</p>
          </div>
        </div>
      </div>
      <ElInput
          v-model="replyContent"
          type="textarea"
          placeholder="请输入你的回复内容，@用户可以艾特他..."
          :autosize="{ minRows: 4, maxRows: 15 }"
          ref="replyInputRef"
      @input="(value) => {
      checkReplyLength();
      handleInput(value,replyInputRef);
      updatePreviewBookCards(value);
      }"
      @keydown="handleMentionKeydown"
      @blur="checkReplyLength"
      />
      <p v-if="replyLengthError" style="font-size: 12px; color: #ff4d4f; margin: 4px 0 0 0;">{{ replyLengthErrorMsg }}</p>
      <div class="image-upload-area" style="margin-top: 15px;">
        <div class="image-list">
          <div v-for="(img, index) in replyUploadedImages" :key="img.id" class="image-item">
            <img v-if="img.id" :src="IMAGE_BASE_URL + img.imageUrl" alt="上传图片">
            <div class="delete-btn" @click="deleteReplyImage(index)">×</div>
          </div>
          <div v-if="replyUploadedImages.length < 9" class="upload-btn" @click="triggerReplyUpload">
            <el-icon size="24" color="#ccc"><Picture /></el-icon>
          </div>
        </div>
        <input ref="replyFileInput" type="file" multiple accept="image/*,.jpg,.png,.jpeg,.gif" style="display: none;" @change="handleReplyFileUpload">
        <div style="font-size: 12px; color: #999; margin-top: 5px;">最多上传9张图片</div>
      </div>
      <template #footer>
        <ElButton type="primary" @click="submitReply" round>确定回复</ElButton>
        <ElButton @click="closeReplyDialog" round>取消</ElButton>
      </template>
    </ElDialog>
    <!-- 🔥 全局@艾特用户下拉列表（替换原来的局部版本） -->
    <div
        v-if="showMentionList"
        class="mention-user-list"
        :style="{ left: mentionDropdownPosition.x + 'px', top: mentionDropdownPosition.y + 'px' }"
        @click.stop
    >
      <div
          v-for="(user, index) in filteredMentionUsers"
          :key="user.userId"
          class="mention-user-item"
          :class="{ active: selectedMentionIndex === index }"
          @click="selectUser(user)"
      >
        <span class="mention-user-name">{{ user.userName }}</span>
        <span class="mention-user-id">({{ user.friendId }})</span>
      </div>
      <div v-if="filteredMentionUsers.length === 0" class="mention-no-result">
        未找到匹配的用户
      </div>
    </div>
  </div>

</template>

<style scoped>
/* ======================================
   1. 全局基础样式 & 通用工具类
======================================== */
/* 通用过渡动画 */
.transition-base {
  transition: all 0.2s ease;
}

/* 通用删除按钮 */
.delete-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: rgba(0,0,0,0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  font-size: 14px;
}

/* 通用空状态 */
.empty-tip {
  text-align: center;
  padding: 60px 0;
  color: var(--el-text-color-secondary);
}

/* 通用加载/无更多提示 */
.loading-tip, .no-more-tip {
  text-align: center;
  padding: 20px 0;
  color: #909399;
  font-size: 14px;
}

/* 通用卡片hover效果 */
.card {
  position: relative;
  transition: all 0.2s ease;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 30;
}

/* 通用操作按钮 */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  color: #409eff;
}

.action-btn .el-icon {
  width: 14px;
  height: 14px;
}

.action-btn.loading {
  cursor: not-allowed;
  opacity: 0.6;
}

/* ======================================
   2. 基础容器布局
======================================== */
.publish-btn-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
  top: 10px;
  padding-right: 20px;
  gap: 8px;
  z-index: 100;
}

.forum-sort-group {
  display: flex;
  gap: 10px;
}

/* 增加按钮样式优化 */
.forum-sort-group .el-button--success.is-plain {
  background-color: #ECF3F2;
  border-color: #AEC0BA;
  color: #5c8374;
}

.forum-sort-group .el-button--success.is-plain:hover {
  background-color: #93A895;
  color: white;
  border-color: #93A895;
}

.forum-loading :deep(.el-loading-mask) {
  z-index: 40;
}

/* 骨架屏 */
.skeleton-comment-item {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 8px;
  border: 1px dashed var(--el-border-color);
}

/* ======================================
   3. 评论结构样式（主评论 + 子评论）
======================================== */
/* 子评论整体容器 */
.sub-comments {
  margin-top: 16px;
  padding-left: 20px;
  border-left: 2px solid #e5e7eb;
}

.sub-comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.sub-comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 评论头部布局（主/子评论通用） */
.comment-header,
.sub-comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.comment-header-left,
.comment-header-right,
.sub-comment-header-left,
.sub-comment-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sub-comment-header {
  gap: 6px;
  margin-bottom: 8px;
}

/* 编辑按钮样式 */
.edit-link-btn {
  border: none !important;
  background: transparent !important;
  padding: 0 4px !important;
  font-size: 12px !important;
  color: #ef4444 !important;
  display: inline-block !important;
  width: auto !important;
  text-align: left !important;
}

/* 子评论底部操作栏 */
.sub-comment-actions {
  display: flex;
  gap: 32px;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

/* 展开子评论按钮 */
.expand-sub-btn {
  margin-top: 12px;
  padding: 8px 0;
  text-align: center;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  background-color: #f5f7fa;
  transition: background-color 0.2s ease;
}

.expand-sub-btn:hover {
  background-color: #ecf5ff;
}

/* ======================================
   4. 图片模块（上传 + 展示 + 折叠）
======================================== */
/* 图片上传区域（发表/编辑/回复通用） */
.image-upload-area .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-upload-area .image-item {
  width: 80px;
  height: 80px;
  border-radius: 8px; /* ✨ 稍微圆润一点 */
  overflow: hidden;
  position: relative;
  border: 1px solid var(--el-border-color-dark); /* ✨ 加深已上传图片的边框 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); /* 加一点悬浮阴影 */
}

.image-upload-area .image-item.small {
  width: 60px;
  height: 60px;
}

.image-upload-area .image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ✨ 修复：上传按钮毛玻璃适配 */
.image-upload-area .upload-btn {
  width: 80px;
  height: 80px;
  /* 使用动态文字次要色加深边框，深色/浅色模式自适应 */
  border: 1.5px dashed var(--el-text-color-secondary);
  border-radius: 8px;
  /* ✨ 核心：增加一点半透明底色，让它从透明弹窗里凸显出来 */
  background: rgba(128, 128, 128, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}
/* 强行覆盖你在 HTML 里写死的 color="#ccc" */
.image-upload-area .upload-btn .el-icon {
  color: var(--el-text-color-secondary) !important;
  transition: color 0.3s ease;
}
.image-upload-area .upload-btn:hover .el-icon {
  color: var(--el-color-primary) !important;
}

/* 悬浮交互优化 */
.image-upload-area .upload-btn:hover {
  border-color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.15); /* 悬浮时透出柔和的主题蓝 */
  transform: translateY(-2px);
}

/* 评论图片展示通用 */
.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.sub-comment-images {
  display: grid;
  grid-template-columns: repeat(3, 80px);
  gap: 8px;
  margin-top: 8px;
}

/* 图片遮罩层容器（主/子评论通用） */
.image-overlay-wrapper,
.sub-image-overlay-wrapper {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.image-overlay-wrapper {
  width: 180px;
  height: 180px;
  border-radius: 6px;
}

.sub-image-overlay-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 4px;
}

/* 评论图片基础样式（主/子评论通用） */
:deep(.comment-img),
:deep(.sub-comment-img) {
  border: 1px solid #e2e8f0 !important;
  object-fit: cover !important;
  cursor: pointer;
  display: block;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

/* 主评论大图 */
:deep(.comment-img) {
  width: 180px !important;
  height: 180px !important;
  border-radius: 6px !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03) !important;
}

:deep(.comment-img:hover) {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08) !important;
}

/* 子评论小图 */
:deep(.sub-comment-img) {
  width: 80px !important;
  height: 80px !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04) !important;
}

/* 展开遮罩层（主/子评论通用） */
.expand-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.45);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  z-index: 5;
  transition: background 0.2s ease;
}

.expand-mask {
  font-size: 16px;
  border-radius: 6px;
}

.sub-expand-mask {
  font-size: 12px;
  border-radius: 4px;
}

.image-overlay-wrapper:hover .expand-mask,
.sub-image-overlay-wrapper:hover .expand-mask {
  background: rgba(0, 0, 0, 0.55);
}

/* ======================================
   5. @艾特功能样式
======================================== */
.mention-user-list {
  position: fixed;
  z-index: 99999 !important;
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

.mention-link,
.mention-text {
  color: #409eff;
  text-decoration: none;
}

.mention-link:hover,
.mention-text:hover {
  text-decoration: underline;
}

/* 文本选择样式 */
div[style*="pre-wrap"] {
  user-select: text;
}

div[style*="pre-wrap"]::selection {
  background: #409eff;
  color: #fff;
}

/* ======================================
   6. 输入框修复
======================================== */
:deep(.el-textarea__inner) {
  resize: vertical !important;
  min-height: 120px;
  position: relative;
  z-index: 1;
}

/* ======================================
   7. 响应式适配（手机端 max-width:768px）
======================================== */
@media (max-width: 768px) {
  /* 评论头部垂直布局 */
  .comment-header,
  .sub-comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .comment-header-right,
  .sub-comment-header-right {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
    width: auto !important;
  }

  /* 按钮适配 */
  .publish-btn-wrap {
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 8px;
    padding-right: 10px !important;
  }

  .forum-sort-group {
    flex: 1;
  }

  .publish-btn-wrap .el-button {
    flex: 1;
    min-width: 0;
    font-size: 12px !important;
    padding: 4px 8px !important;
  }

  /* 隐藏头像，评论全屏 */
  .avatar-column {
    display: none !important;
  }

  .el-col[span="22"] {
    width: 100% !important;
    flex: 0 0 100% !important;
  }
}


/* ======================================
   9. 新增：不规则手账瀑布流核心样式
======================================== */
/* 瀑布流主容器 */
.comment-masonry-container {
  column-count: 2;    /* 默认 PC 端展示 3 列 */
  column-gap: 24px;   /* 便签之间的左右间距 */
  padding: 20px 10px;
}

/* 瀑布流各显神通的单项外壳 */
.masonry-item {
  break-inside: avoid; /* 核心：防止单张便签被截断到下一列 */
  margin-bottom: 32px; /* 便签之间的上下间距 */
  position: relative;  /* 为纸胶带做定位锚点 */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: inline-block;
  width: 100%;

  /* 👇 新增：三大渲染性能法宝 */
  /* 1. 魔法属性：告诉浏览器，没滑进屏幕的便签【完全跳过渲染和排版计算】！ */
  content-visibility: auto;
  /* 2. 配合上面的属性，给未渲染的元素一个估算高度，防止滚动条乱跳 */
  contain-intrinsic-size: 250px;
}


/* 灵魂画笔：手账纸胶带效果 */
.washi-tape {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 75px;
  height: 18px;
  z-index: 99;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  /* 用 clip-path 裁切出两边不规则撕裂的胶带质感 */
  clip-path: polygon(0% 15%, 4% 0%, 96% 0%, 100% 12%, 98% 50%, 100% 88%, 95% 100%, 5% 100%, 0% 85%, 2% 50%);
}

/* 便签卡片皮肤定制 */
.sticky-note-card {
  background-color: inherit !important;
  border-color: inherit !important;
  box-shadow: 2px 5px 10px rgba(0, 0, 0, 0.04) !important;
  /* 💥 新增：只对阴影做轻量级过渡，不改变物理尺寸 */
  transition: box-shadow 0.3s ease;
}

/* 💥 空间深度优化：把原先宽屏的“左头像、右文本”在窄便签里矫正为“上下结构” */
.sticky-note-card :deep(.el-row) {
  display: flex !important;
  flex-direction: column !important;
  gap: 12px;
}

/* 头像改到顶部靠左排列 */
.sticky-note-card :deep(.avatar-column) {
  width: 100% !important;
  max-width: 100% !important;
  flex: 0 0 100% !important;
  display: flex;
  justify-content: flex-start;
}

/* 内容撑满卡片宽度 */
.sticky-note-card :deep(.el-col-22) {
  width: 100% !important;
  max-width: 100% !important;
  flex: 0 0 100% !important;
}

/* 💥 修复：重塑便签内主评论图片尺寸，完美适配 3x3 九宫格布局 */
.sticky-note-card .comment-images {
  display: grid !important;
  grid-template-columns: repeat(3, 80px) !important; /* 强制采用3列九宫格 */
  gap: 8px !important;
  margin-top: 10px !important;
}

.sticky-note-card :deep(.comment-img) {
  width: 80px !important;
  max-width: 80px !important;
  height: 80px !important;
  border-radius: 4px !important;
}

.sticky-note-card .image-overlay-wrapper {
  width: 80px !important;
  max-width: 80px !important;
  height: 80px !important;
  border-radius: 4px !important;
}

.sticky-note-card .expand-mask {
  font-size: 13px !important; /* 让遮罩层里的 "+X张" 文字适配小方块 */
  border-radius: 4px !important;
}

/* ======================================
   🚀 性能优化版：纯 CSS 实现手账便签随机样式
======================================== */
/* 便签卡片背景与边框交替循环 */
.masonry-item:nth-child(4n+1) .sticky-note-card { background-color: #f3faf4 !important; border-color: #cfead4 !important; transform: rotate(0.4deg); }
.masonry-item:nth-child(4n+2) .sticky-note-card { background-color: #fffdeb !important; border-color: #ebdca5 !important; transform: rotate(-0.8deg); }
.masonry-item:nth-child(4n+3) .sticky-note-card { background-color: #edf7f5 !important; border-color: #cceee2 !important; transform: rotate(1.2deg); }
.masonry-item:nth-child(4n+4) .sticky-note-card { background-color: #fffdf0 !important; border-color: #e3d69c !important; transform: rotate(-1.2deg); }

/* 纸胶带颜色与角度交替循环 */
.masonry-item:nth-child(4n+1) .washi-tape { background-color: rgba(168, 218, 220, 0.5); transform: translateX(-50%) rotate(-4deg); }
.masonry-item:nth-child(4n+2) .washi-tape { background-color: rgba(241, 166, 166, 0.5); transform: translateX(-50%) rotate(4deg); }
.masonry-item:nth-child(4n+3) .washi-tape { background-color: rgba(233, 196, 106, 0.5); transform: translateX(-50%) rotate(-2deg); }
.masonry-item:nth-child(4n+4) .washi-tape { background-color: rgba(138, 201, 143, 0.5); transform: translateX(-50%) rotate(5deg); }


/* ======================================
   🔥 优化版：纯 CSS 文本截断与展开按钮
======================================== */
.comment-text-body {
  transition: all 0.3s ease;
}

/* 折叠状态下，使用 Webkit 引擎原生的多行文本截断神技，性能极高 */
.comment-text-body.text-clamped {
  display: -webkit-box;
  -webkit-line-clamp: 5; /* 默认只显示 5 行文本 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.expand-text-btn {
  color: #409eff;
  font-size: 13px;
  margin-top: 6px;
  cursor: pointer;
  display: inline-block;
  user-select: none;
  font-weight: 500;
  transition: color 0.2s;
}

.expand-text-btn:hover {
  color: #66b1ff;
  text-decoration: underline;
}

:deep(.dark-mode) .expand-text-btn {
  color: #60a5fa;
}
/* ====================== 卡片横向滚动容器 (番茄同款) ====================== */
.multi-card-slider-wrapper {
  display: flex !important;
  flex-direction: row !important;
  align-items: center;
  position: relative;
  width: 100%;
}

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
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.multi-card-scroll-view::-webkit-scrollbar {
  display: none;
}

/* 左右控制箭头 ========== 【主要修改区域】 ========== */
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

  /* ========== 新增：默认隐藏箭头 ========== */
  opacity: 0;
  pointer-events: none;
}

/* ========== 新增：悬浮外层容器时，显示箭头 + 允许点击 ========== */
.multi-card-slider-wrapper:hover .slider-arrow {
  opacity: 1;
  pointer-events: auto;
}

/* 箭头自身hover效果（原有逻辑保留） */
.slider-arrow:hover {
  background: #fff;
  color: #1890ff;
  transform: scale(1.1);
}
.slider-arrow.left { left: -12px; }
.slider-arrow.right { right: -12px; }

.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* ====================== 精简版卡片样式 ====================== */
:deep(.simplified-card) {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 90px !important;
  max-width: 90px !important;
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

/* 修复深色模式 */
:deep(.dark-mode) .slider-arrow {
  background: #374151;
  color: #e5e7eb;
}

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

/* 3. 子评论边框深色 */
html.dark .sub-comments { border-left-color: #4a5568; }
html.dark .sub-comment-item { border-bottom-color: #374151; }
html.dark .sub-comment-actions { border-top-color: #374151; }

/* 4. 评论图片边框+遮罩深色 */
html.dark :deep(.comment-img),
html.dark :deep(.sub-comment-img) {
  border-color: #3f3f46 !important;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3) !important;
}
html.dark .expand-mask { background: rgba(0, 0, 0, 0.6); }

/* 5. @艾特下拉弹窗深色 */
html.dark .mention-user-list {
  background: #1f2937;
  color: #e5e7eb;
}
html.dark .mention-user-item:hover,
html.dark .mention-user-item.active {
  background-color: #374151;
}
html.dark .mention-text { color: #60a5fa; }

/* 6. 展开子评论按钮深色 */
html.dark .expand-sub-btn { background-color: #374151; }
html.dark .expand-sub-btn:hover { background-color: #4b5563; }

/* 7. 便签底部渐变遮罩深色（修复原:deep(.dark-mode)失效） */
html.dark .note-fade-mask {
  background: linear-gradient(
      to bottom,
      transparent,
      rgba(30, 34, 39, 0.95)
  );
}

/* 8. 横向滑动卡片箭头深色 */
html.dark .slider-arrow {
  background: #374151;
  color: #e5e7eb;
}
/* ======================================
   🌑 终极覆盖：强制重置所有便签卡片在黑夜模式下的样式
======================================== */
html.dark .comment-item-card,
html.dark .sub-comment-item,
html.dark .forum-comment-card,
html.dark .note-card,
html.dark :deep(.el-card) {
  /* 强制把背景色和边框重置，!important 是必要的，以覆盖 el-card 的默认样式 */
  background-color: #1f2937 !important;
  border-color: #374151 !important;
  color: #e5e7eb !important;
}

/* 强制重置卡片内容区域的背景 */
html.dark :deep(.el-card__body) {
  background-color: transparent !important;
}

/* 强制重置评论文字颜色 */
html.dark .comment-body-text,
html.dark .content-text,
html.dark .comment-text {
  color: #d1d5db !important;
}

/* 强制重置用户名/作者名颜色 */
html.dark .comment-header-left span,
html.dark .author-name {
  color: #f3f4f6 !important;
}

/* 强制重置时间颜色 */
html.dark .comment-header-right span {
  color: #9ca3af !important;
}

/* ======================================
   屏幕自适应响应式断点
======================================== */
@media (max-width: 1200px) {
  .comment-masonry-container {
    column-count: 2; /* 平板端降为 2 列 */
  }
}

@media (max-width: 768px) {
  .comment-masonry-container {
    column-count: 1; /* 手机端回归 1 列，但保留便签不规则色调 */
    padding: 10px 5px;
  }
  .sticky-note-card :deep(.avatar-column) {
    display: flex !important; /* 手机端恢复显示头像，因为在顶部不占地方 */
  }
}

</style>