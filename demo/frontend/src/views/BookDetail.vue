<script setup>
// ==============================================
// 1. 依赖导入 & 全局注入
// ==============================================
import { ref, onMounted, inject, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request.js'
import {
  ElCard, ElRow, ElCol, ElDivider, ElMessage, ElDialog, ElInput,
  ElButton, ElMessageBox, ElSelect, ElOption, ElDatePicker, ElInputNumber, ElImage, ElAvatar
} from 'element-plus'
import { useUserStore } from '../stores/userStore'
import { useAchievementStore } from '../stores/achievementStore'
defineOptions({ name: 'BookDetail' })
// ==============================================
// 2. 全局状态 & 注入变量初始化
// ==============================================
const userStore = useUserStore()
const achievementStore = useAchievementStore()
const isDark = inject('isDark')

// 用户信息注入
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')
const getUserInfo = inject('getUserInfo')

// 核心注入：全局借阅/收藏列表（从BasicLayout获取）
const userBorrowList = inject('userBorrowList')
const userCollections = inject('userCollections')
const loadUserCollections = inject('loadUserCollections')
const getuserBorrows = inject('getuserBorrows')
// ✅ 注入 Basic 全局的收藏方法
const handleCollect = inject('handleCollect')

// 公共注入变量
const allTagList = inject('allTagList')
const bookTypeList = inject('bookTypeList')
const imgUrl = inject('imgUrl')

// ==============================================
// 3. 路由 & 基础参数
// ==============================================
const route = useRoute()
const router = useRouter()
const bookIsbn = ref(route.query.isbn || '')

// ==============================================
// 4. 响应式数据定义（按功能分组）
// ==============================================
// 书籍基础信息
const bookDetail = ref({})
const bookComments = ref([])
const isShowBookInfo = ref(false)

// 🔥 已改造：改为按钮手动控制分页参数（去掉无限滚动）
const page = ref(1)        // 当前页码
const pageSize = ref(10)   // 每页10条
const loading = ref(false) // 加载中状态
const noMore = ref(false)  // 是否无更多数据


// 评论相关
const isPublishDialogShow = ref(false)
const commentContent = ref('')
const commentStar = ref(3)
const lengthError = ref(false)
const lengthErrorMsg = ref('')
const editingCommentId = ref(null)
const editingContent = ref('')
const editingStar = ref(3)
const editLengthError = ref(false)
const editLengthErrorMsg = ref('')

// 书籍编辑相关
const isEditBookShow = ref(false)

// 🔥 根据ISBN获取当前书籍的标签ID
const loadBookTags = async (isbn) => {
  try {
    const res = await request.get('/book/tag/relation', { params: { isbn } })
    if (res.code === 200) {
      const ids = res.data.map(item => item.tagId)
      editBookForm.value.tagIds = ids
      originTagIds.value = [...ids] // 保存原始数据
    }
  } catch (e) { console.error(e) }
}
// 🔥 标签列表 + 原始标签（取消时恢复）
const originTagIds = ref([]) // 原始标签ID
const editBookForm = ref({
  iSBN: '',
  bookname: '',
  author: '',
  publisher: '',
  publishDate: '',
  Type: '',
  all_book: 0,
  now_book: 0,
  pictureName: '',
  information: '',
  tagIds: []
})
const editBookRules = ref({})
const originStock = ref(0)
const originAllBook = ref(0)

// 评论排序
const sortOrder = ref('desc')

// ==============================================
// 5. 计算属性
// ==============================================
// 书籍评分
const displayStar = computed(() => {
  return bookDetail.value.star || 0
})
// 评分人数
const ratingCount = computed(() => {
  return bookComments.value.length
})
// 是否已借阅
const isBorrowed = computed(() => {
  if (!bookIsbn.value || !userBorrowList.value?.length) return false
  return userBorrowList.value.some(item => item.iSBN === bookIsbn.value)
})
// 是否已收藏
const isCollected = computed(() => {
  if (!bookIsbn.value || !userCollections.value?.length) return false
  return userCollections.value.some(item => item.isbn === bookIsbn.value)
})
// 评论排序
const sortedComments = computed(() => {
  const list = [...bookComments.value]
  if (sortOrder.value === 'desc') {
    return list.sort((a, b) => (b.star || 3) - (a.star || 3))
  } else {
    return list.sort((a, b) => (a.star || 3) - (a.star || 3))
  }
})

// 🔥 新增核心：将排好序的评论按照单双数分流到左右两栏
const leftComments = computed(() => {
  return sortedComments.value.filter((_, index) => index % 2 === 1) // 奇数索引去左边（书籍详情下方）
})
const rightComments = computed(() => {
  return sortedComments.value.filter((_, index) => index % 2 === 0) // 偶数索引去右边
})

// ==============================================
// 6. 工具函数
// ==============================================
const formatDateTime = inject('formatDateTime')
const filterSpecialCharNormal = inject('filterSpecialCharNormal')
const getValidContentLength = inject('getValidContentLength')

// ==============================================
// 7. 核心业务方法 - 书籍基础操作
// ==============================================
const getBookDetail = inject('getBookDetail')

// 获取书籍评论
const getBookComments = async (isLoadMore = false) => {
  try {
    if (!bookIsbn.value) return;
    if (loading.value || (noMore.value && isLoadMore)) return;

    loading.value = true;

    const res = await request.get('/book/comment', {
      params: {
        isbn: bookIsbn.value,
        page: page.value,
        pageSize: pageSize.value
      }
    });

    if (res.code === 200) {
      let comments = res.data || [];
      if (comments.length < pageSize.value) {
        noMore.value = true;
      } else {
        noMore.value = false;
      }

      // 🔥 后端已 JOIN user_information 返回 userName，无需 N+1 查询
      const commentsWithUser = await Promise.all(
          comments.map(async (comment) => {
            if (comment.userName) {
              return { ...comment, user: { name: comment.userName } }
            }
            const user = await getUserInfo(comment.userid);
            return { ...comment, user };
          })
      );

      if (isLoadMore) {
        bookComments.value.push(...commentsWithUser);
      } else {
        bookComments.value = commentsWithUser;
      }
    } else {
      if (!isLoadMore) bookComments.value = [];
    }
  } catch (err) {
    console.error('获取评论失败：', err);
    if (!isLoadMore) bookComments.value = [];
  } finally {
    loading.value = false;
  }
}

// 🔥 手动点击按钮加载更多评论
const handleLoadMoreComments = async () => {
  if (loading.value || noMore.value) return;
  page.value += 1;
  await getBookComments(true);
}

// 跳转章节阅读页
const goToBookReader = inject('goToBookReader')

// ==============================================
// 8. 核心业务方法 - 评论管理
// ==============================================
const checkCommentLength = () => {
  const validLen = getValidContentLength(commentContent.value)
  const rawLen = commentContent.value.length
  if (validLen < 1) {
    lengthError.value = true
    lengthErrorMsg.value = '评论内容不能为空！'
  } else if (validLen > 2000) {
    lengthError.value = true
    lengthErrorMsg.value = '评论内容不能超过2000字！评论现在长度'+ validLen
  } else {
    lengthError.value = false
    lengthErrorMsg.value = ''
    if (rawLen > 2000) {
      commentContent.value = commentContent.value.slice(0, 2000)
    }
  }
}

const submitComment = async () => {
  checkCommentLength()
  if (lengthError.value) return
  const commentData = {
    iSBN: bookIsbn.value,
    userid: currentUserId.value,
    comment: commentContent.value.trim(),
    star: commentStar.value,
    time: Date.now()
  }
  try {
    const res = await request.post('/book/comment', commentData)
    if (res.code === 200) {
      ElMessage.success('评论发表成功！')
      achievementStore.checkAchievements()
      isPublishDialogShow.value = false
      commentContent.value = ''
      commentStar.value = 3
      page.value = 1
      noMore.value = false
      await getBookComments(false)
      const data = await getBookDetail(bookIsbn.value)
      if (data) {
        bookDetail.value = data
      }
    } else {
      ElMessage.error(res.msg || '评论发表失败')
    }
  } catch (err) {
    ElMessage.error('请求出错啦，请稍后再试~')
  }
}

const handleDelete = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？','删除提示',{ type: 'warning', center: true })
    const res = await request.delete('/book/comment', { params: { commentId: commentId } })
    if (res.code === 200) {
      ElMessage.success('评论删除成功！')
      page.value = 1
      noMore.value = false
      await getBookComments(false)
      const data = await getBookDetail(bookIsbn.value)
      if (data) {
        bookDetail.value = data
      }
      editingCommentId.value = null
    } else {
      ElMessage.error(res.msg || '评论删除失败')
    }
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error('删除出错啦~')
  }
}


// ==============================================
// 9. 核心业务方法 - 借阅 & 收藏
// ==============================================
const borrowBook = async (isbn, bookInfo) => {
  try {
    if (bookInfo.now_book === 0) {
      ElMessage.warning('此书库存不足，无法借阅')
      return
    }
    await ElMessageBox.confirm('确认借阅此书吗？','提示',{ type: 'warning' })
    const userRes = await getUserInfo(currentUserId.value)
    const user = userRes || {}
    if (user.can_use <= 0) {
      ElMessage.warning('您的可借阅数量已用尽，无法继续借阅')
      return
    }
    const borrowData = { iSBN: isbn, userid: currentUserId.value }
    const borrowRes = await request.post('/book/borrow', borrowData)
    if (borrowRes.code !== 200) {
      ElMessage.error(borrowRes.msg || '借阅记录创建失败')
      return
    }
    const updatedBook = JSON.parse(JSON.stringify(bookInfo))
    updatedBook.now_book -= 1
    updatedBook.BorrowCount += 1
    const bookUpdateRes = await request.patch('/book', updatedBook)
    if (bookUpdateRes.code !== 200) {
      ElMessage.error(bookUpdateRes.msg || '书籍库存更新失败')
      return
    }
    const updatedUser = JSON.parse(JSON.stringify(user))
    updatedUser.can_use = updatedUser.can_use -1
    const userUpdateRes = await request.patch('/user/info', updatedUser)
    if (userUpdateRes.code !== 200) {
      ElMessage.error(userUpdateRes.msg || '用户可借数量更新失败')
      return
    }
    await getuserBorrows()
    const data = await getBookDetail(bookIsbn.value)
    if (data) {
      bookDetail.value = data
    }
    ElMessage.success('借阅成功！')
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error('借阅失败，请稍后再试')
  }
}

const cancelBorrowBook = async (bookInfo) => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录！')
    return
  }
  try {
    await ElMessageBox.confirm('确定归还此书吗？', '提示', { type: 'warning' })
    const borrowtItem = userBorrowList.value.find(item => item.iSBN === bookIsbn.value)
    if (!borrowtItem) {
      ElMessage.error('未找到借阅记录')
      return
    }
    const updatedBook = JSON.parse(JSON.stringify(bookInfo))
    updatedBook.now_book += 1
    const bookUpdateRes = await request.patch('/book', updatedBook)
    if (bookUpdateRes.code !== 200) {
      ElMessage.error(bookUpdateRes.msg || '书籍库存更新失败')
      return
    }
    const userRes = await getUserInfo(currentUserId.value)
    const user = userRes || {}
    const updatedUser = JSON.parse(JSON.stringify(user))
    updatedUser.can_use = updatedUser.can_use +1
    const userUpdateRes = await request.patch('/user/info', updatedUser)
    if (userUpdateRes.code !== 200) {
      ElMessage.error(userUpdateRes.msg || '用户可借数量更新失败')
      return
    }
    const res = await request.delete('/book/borrow', {
      params: { borrowId: borrowtItem.borrowId, userId: currentUserId.value }
    })
    if (res.code === 200) {
      ElMessage.success('归还成功！')
      await getuserBorrows()
      const data = await getBookDetail(bookIsbn.value)
      if (data) {
        bookDetail.value = data
      }
    }
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('归还失败')
  }
}

const cancelCollectBook = async () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录！')
    return
  }
  try {
    await ElMessageBox.confirm('确定取消收藏此书吗？', '提示', { type: 'warning' })
    const collectItem = userCollections.value.find(item => item.isbn === bookIsbn.value)
    if (!collectItem) {
      ElMessage.error('未找到收藏记录')
      return
    }
    const res = await request.delete('/user/collection', {
      params: { collectionId: collectItem.collectionId, iSBN: bookIsbn.value }
    })
    if (res.code === 200) {
      ElMessage.success('取消收藏成功！')
      await loadUserCollections(currentUserId.value)
    }
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('取消收藏失败')
  }
}

// ==============================================
// 10. 核心业务方法 - 书籍编辑 & 删除
// ==============================================
const handleEditPictureChange = () => {
  imgUrl.value = editBookForm.value.pictureName || '/default-book.png'
}

const openEditBookDialog = async () => {
  editBookForm.value = JSON.parse(JSON.stringify(bookDetail.value))
  originAllBook.value = bookDetail.value.all_book || 0
  originStock.value = bookDetail.value.now_book || 0

  editBookForm.value.bookname = filterSpecialCharNormal(editBookForm.value.bookname);
  editBookForm.value.author = filterSpecialCharNormal(editBookForm.value.author);
  editBookForm.value.publisher = filterSpecialCharNormal(editBookForm.value.publisher);

  if (editBookForm.value.publishDate) {
    const date = new Date(editBookForm.value.publishDate);
    editBookForm.value.publishDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  editBookForm.value.Type = bookDetail.value.type || null;
  imgUrl.value = editBookForm.value.pictureName || '/default-book.png';

  await loadBookTags(bookIsbn.value)
  isEditBookShow.value = true
}

const handleCancelEdit = () => {
  isEditBookShow.value = false
  editBookForm.value.tagIds = [...originTagIds.value]
}

const checkEditBookForm = () => {
  const allBook = editBookForm.value.all_book || 0
  const nowBook = originStock.value
  if (allBook < nowBook) {
    ElMessage.error(`总量不能小于当前存量（${nowBook}）！`)
    return false
  }
  if (!editBookForm.value.Type) {
    ElMessage.error('请选择书籍类型！')
    return false
  }
  const infoLen = (editBookForm.value.information || '').trim().length
  if (infoLen > 2000) {
    ElMessage.error('书籍信息不能超过2000字！')
    return false
  }
  const charReg = /^[\u4e00-\u9fa5a-zA-Z0-9]*$/;
  const bookName = (editBookForm.value.bookname || '').trim();
  if (bookName && !charReg.test(bookName)) {
    ElMessage.error('书籍名称仅允许输入汉字、英文或数字！');
    return false;
  }
  const author = (editBookForm.value.author || '').trim();
  if (author && !charReg.test(author)) {
    ElMessage.error('作者仅允许输入汉字、英文或数字！');
    return false;
  }
  const publisher = (editBookForm.value.publisher || '').trim();
  if (publisher && !charReg.test(publisher)) {
    ElMessage.error('出版社仅允许输入汉字、英文或数字！');
    return false;
  }
  if (!editBookForm.value.publishDate) {
    ElMessage.error('请选择出版日期！');
    return false;
  }
  const publishDate = new Date(editBookForm.value.publishDate);
  publishDate.setHours(0, 0, 0, 0);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (publishDate > today) {
    ElMessage.error(`出版日期不能超过当前日期（${today.toLocaleDateString()}）！`);
    return false;
  }
  return true
}

const submitEditBook = async () => {
  if (!checkEditBookForm()) return
  const newAllBook = editBookForm.value.all_book || 0
  const diff = newAllBook - originAllBook.value
  if (diff > 0) {
    editBookForm.value.now_book = originStock.value + diff
  }

  try {
    const res = await request.put('/book', editBookForm.value)
    if (res.code === 200) {
      const isbn = bookIsbn.value
      const oldRelations = await request.get('/book/tag/relation', { params: { isbn } })
      for (const item of oldRelations.data) {
        await request.delete('/book/tag/relation', { params: { id: item.id } })
      }
      const newTagIds = editBookForm.value.tagIds
      for (const tagId of newTagIds) {
        await request.post('/book/tag/relation', { isbn, tagId })
      }

      ElMessage.success('书籍+标签修改成功！')
      isEditBookShow.value = false
      const data = await getBookDetail(bookIsbn.value)
      if (data) bookDetail.value = data
    } else {
      ElMessage.error(res.msg || '书籍信息修改失败')
    }
  } catch (err) {
    ElMessage.error('修改书籍信息出错啦~')
  }
}

const updateUserBorrowCount = async (userid) => {
  try {
    const userRes = await request.get('/user/info', {params: { userId:currentUserId }})
    if (userRes.code !== 200 || !userRes.data) {
      ElMessage.warning(`用户[${currentUserId}]信息查询失败，跳过数量更新`)
      return false
    }
    const userInfo = userRes.data[0]
    const updateUser = JSON.parse(JSON.stringify(userInfo))
    updateUser.can_use = (userInfo.can_use || 0) + 1
    const updateRes = await request.patch('/user/info', updateUser)
    if (updateRes.code === 200) {
      ElMessage.info(`用户[${currentUserId}]可借阅数量已更新`)
      return true
    } else {
      ElMessage.error(`用户[${currentUserId}]可借阅数量更新失败：${updateRes.msg}`)
      return false
    }
  } catch (err) {
    ElMessage.error(`用户[${currentUserId}]数量更新出错`)
    return false
  }
}

const deleteBook = async () => {
  try {
    await ElMessageBox.confirm(
        '确定删除该书籍吗？删除后会同步删除相关借阅记录并恢复读者可借阅数量！',
        '删除提示',
        { type: 'warning', center: true }
    )
    const borrowRes = await request.get('/book/borrow', { params: { isbn: bookIsbn.value } })
    const borrowList = borrowRes.data || []
    if (borrowList.length > 0) {
      ElMessage.info(`发现${borrowList.length}条借阅记录，开始清理...`)
      for (const borrow of borrowList) {
        const { borrowId, currentUserId } = borrow
        await updateUserBorrowCount(currentUserId)
        await request.delete('/book/borrow', { params: { borrowId } })
      }
    }
    const delBookRes = await request.delete('/book', { params: { isbn: bookIsbn.value } })
    if (delBookRes.code === 200) {
      ElMessage.success('书籍及关联借阅记录删除成功！')
      router.push('/home')
    } else {
      ElMessage.error(`书籍删除失败：${delBookRes.msg || '未知错误'}`)
    }
  } catch (err) {
    if (err === 'cancel') return
    ElMessage.error('删除书籍出错啦~ 请检查借阅记录或联系管理员')
  }
}

// ==============================================
// 11. 生命周期钩子
// ==============================================
onMounted(async () => {
  if (bookIsbn.value) {
    const data = await getBookDetail(bookIsbn.value)
    if (data) {
      bookDetail.value = data
    }
    await getBookComments(false)
  } else {
    ElMessage.warning('缺少书籍ISBN参数')
    router.push('/home')
  }
})
</script>

<template>
  <el-scrollbar style="height: 85vh; overflow: auto;">
    <!-- ✅ 全新重构：标准双栏弹性布局包装器 -->
    <div class="dual-column-layout-container">

      <!-- ==============================================
           左侧栏：包含书籍详情卡片 + 与之等宽的左侧评论列表
           ============================================== -->
      <div class="left-layout-column">
        <!-- 书籍详情卡片 -->
        <el-card class="book-detail-card">
          <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;">
            <div id="guide-book-title" class="book-title" :style="{ margin: '0', color: isDark ? '#e5e7eb' : '#333' }" @click="goToBookReader(bookDetail.iSBN)">{{ bookDetail.bookname }}</div>

            <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
<!--              <el-button
                  :type="isBorrowed ? 'default' : 'danger'"
                  size="small"
                  :disabled="bookDetail.now_book === 0"
                  @click.stop="isBorrowed ? cancelBorrowBook(bookDetail) : borrowBook(bookDetail.iSBN, bookDetail)"
                  round
              >
                {{ isBorrowed ? '已借阅' : '借阅' }}
              </el-button>-->
              <el-button
                  :type="isCollected ? 'default' : 'primary'"
                  size="small"
                  @click.stop="isCollected ? cancelCollectBook() : handleCollect(bookDetail)"
                  round
              >
                {{ isCollected ? '已收藏' : '收藏' }}
              </el-button>
              <ElButton v-if="currentUserType === 3" @click="openEditBookDialog" type="text" style="color: #1890ff; padding: 0 4px;">编辑</ElButton>
              <ElButton v-if="currentUserType === 3" @click="deleteBook" type="text" style="color: #ff4d4f; padding: 0 4px;">删除</ElButton>
            </div>
          </div>

          <el-divider :style="{ margin: '15px 0', borderColor: isDark ? '#374151' : '#eee', borderWidth: '2px' }"></el-divider>

          <el-row :gutter="20" style="align-items: flex-start;" class="book-detail-row">
            <el-col :span="8" class="book-cover-col">
              <div style="width: 100%; max-width: 140px; height: 200px; overflow: hidden; margin: 0 auto; border-radius: 6px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <img
                    :src="bookDetail.pictureName || '/default-book.png'"
                    style="width: 100%; height: 100%; object-fit: cover;"
                    alt="书籍封面"
                >
              </div>
            </el-col>

            <el-col :span="16" class="book-info-col">
              <div style="display: flex; flex-direction: column; gap: 6px; font-size: 14px; width: 100%;">
                <div class="book-info-item-mini"><span>作者：</span><span>{{ bookDetail.author || '未知' }}</span></div>
                <div class="book-info-item-mini"><span>ISBN：</span><span>{{ bookDetail.iSBN }}</span></div>
                <div class="book-info-item-mini"><span>出版社：</span><span>{{ bookDetail.publisher || '未知' }}</span></div>
                <div class="book-info-item-mini"><span>出版日期：</span><span>{{ formatDateTime(bookDetail.publishDate,false) || '未知' }}</span></div>
                <div class="book-info-item-mini"><span>书籍类型：</span><span>{{ bookDetail.bookTypeName || '未知' }}</span></div>

                <div class="book-info-item-mini" style="align-items: center;">
                  <span>标签：</span>
                  <div style="display: flex; gap: 4px; flex-wrap: wrap;">
                    <el-tag v-for="tag in bookDetail.tagNames" :key="tag" type="success" size="small" effect="light">{{ tag }}</el-tag>
                    <span v-if="!bookDetail.tagNames || bookDetail.tagNames.length === 0" style=" font-size: 12px; text-align:left;">暂无</span>
                  </div>
                </div>

                <div class="book-info-item-mini" style="align-items: center;">
                  <span>评分：</span>
                  <template v-if="displayStar > 0">
                    <el-rate v-model="displayStar" disabled show-score text-color="#1890ff" active-color="#ffc107" size="small"/>
                  </template>
                  <template v-else><span style="color: #999; font-size: 12px;">暂无评分</span></template>
                </div>
              </div>
            </el-col>
          </el-row>

          <div :style="{ marginTop: '20px', fontSize: '14px', borderTop: isDark ? '1px dashed #374151' : '1px dashed #eee', paddingTop: '15px', textAlign: 'left' }">
            <span style="font-weight: bold; display: block; margin-bottom: 8px;" :style="{ color: isDark ? '#d1d5db' : '#666' }">书籍信息：</span>
            <div class="book-info-container">
              <span class="book-info-text" :class="{ 'show-all': isShowBookInfo }" :style="{  fontSize: '13px', lineHeight: '1.6' }">
                {{ bookDetail.information || '无' }}
              </span>
              <ElButton class="book-info-toggle-btn" type="text" size="small" @click="isShowBookInfo = !isShowBookInfo">
                {{ isShowBookInfo ? '收起 ↑' : '展开全文 ↓' }}
              </ElButton>
            </div>
          </div>
        </el-card>

        <!-- 左侧的分流评论列表（自动完美继承左边栏宽度，与书籍详情卡片等宽） -->
        <div class="column-comments-wrapper">
          <el-card class="card comment-item-card" v-for="comment in leftComments" :key="comment.commentId">
            <div style="display: flex; align-items: center; justify-content: space-between;" class="comment-header">
              <div class="comment-header-left">
                <el-avatar size="mideum" style="font-size: 20px; cursor: pointer;" @click="router.push('/user/profile?userId=' + comment.userid)">
                  {{ comment.user?.name.slice(-2) }}
                </el-avatar>
                <span style="font-weight: 600;">{{ comment.user?.name || '未知用户' }}</span>
                <span style="font-size: 12px; color: #999;">({{ comment.userid }})</span>
              </div>
              <div class="comment-header-right" style="display: flex; align-items: center; gap: 8px;">
                <el-rate v-model="comment.star" disabled :show-score="false" size="small"/>
                <span style="font-size: 11px; color: #999;">{{ formatDateTime(comment.time) }}</span>
                <ElButton v-if="comment.userid === currentUserId || currentUserType === 3" @click="handleDelete(comment.commentId)" type="text" style="color: #ff4d4f; padding:0;" class="auto-width-link-btn">删除</ElButton>
              </div>
            </div>
            <el-divider style="margin: 8px 0; border-color: #eee; border-width: 1px"></el-divider>
            <div class="comment-body-text" :style="{ color: isDark ? '#d1d5db' : '#333' }">{{ comment.comment }}</div>
          </el-card>
        </div>
      </div>

      <!-- ==============================================
           右侧栏：包含独立的“书籍评论”页眉组件 + 右侧独立排列的评论列表
           ============================================== -->
      <div class="right-layout-column">
        <div id="guide-column" class="comments-header-wrapper" style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 24px; font-weight: bold;">书籍评论 <span style="font-size: 13px; color: #999; font-weight: normal;">(共 {{ ratingCount }} 条)</span></span>
          <div style="display: flex; gap: 6px;">
            <el-button :type="sortOrder === 'desc' ? 'primary' : 'default'" size="small" @click="sortOrder = 'desc'" round>高到低</el-button>
            <el-button :type="sortOrder === 'asc' ? 'primary' : 'default'" size="small" @click="sortOrder = 'asc'" round>低到高</el-button>
            <el-button type="primary" size="small" @click="isPublishDialogShow = true" round>发表评论</el-button>
          </div>
        </div>
        <el-divider style="margin: 10px 0; border-color: #333; border-width: 2px;"></el-divider>

        <!-- 右侧的分流评论列表 -->
        <div class="column-comments-wrapper">
          <el-card class="card comment-item-card" v-for="comment in rightComments" :key="comment.commentId">
            <div style="display: flex; align-items: center; justify-content: space-between;" class="comment-header">
              <div class="comment-header-left">
                <el-avatar size="medium" style="font-size: 20px; cursor: pointer;" @click="router.push('/user/profile?userId=' + comment.userid)">
                  {{ comment.user?.name.slice(-2) }}
                </el-avatar>
                <span style="font-weight: 600;">{{ comment.user?.name || '未知用户' }}</span>
                <span style="font-size: 12px; color: #999;">({{ comment.userid }})</span>
              </div>
              <div class="comment-header-right" style="display: flex; align-items: center; gap: 8px;">
                <el-rate v-model="comment.star" disabled :show-score="false" size="small"/>
                <span style="font-size: 11px; color: #999;">{{ formatDateTime(comment.time) }}</span>
                <ElButton v-if="comment.userid === currentUserId || currentUserType === 3" @click="handleDelete(comment.commentId)" type="text" style="color: #ff4d4f; padding:0;" class="auto-width-link-btn">删除</ElButton>
              </div>
            </div>
            <el-divider style="margin: 8px 0; border-color: #eee; border-width: 1px"></el-divider>
            <div class="comment-body-text" :style="{ color: isDark ? '#d1d5db' : '#333' }">{{ comment.comment }}</div>
          </el-card>
        </div>
      </div>

    </div>

    <!-- ==============================================
         分页状态条：置于双栏底部居中
         ============================================== -->
    <div style="text-align: center; margin: 25px 0 40px; clear: both;">
      <el-button
          v-show="!noMore"
          :loading="loading"
          type="primary"
          plain
          :disabled="noMore"
          @click="handleLoadMoreComments"
          round
      >
        加载更多评论 ↓
      </el-button>
      <div v-if="loading && bookComments.length === 0" style="color: #999; padding: 15px;">加载中...</div>
      <div v-if="noMore && bookComments.length > 0" style="color: #999; padding: 15px;">没有更多评论啦~</div>
      <div v-if="bookComments.length === 0 && !loading" style="color: #999; padding: 30px;">暂无评论，快来发表第一条评论吧~</div>
    </div>

    <!-- 内部隐藏弹窗保持原件 -->
    <ElDialog v-model="isPublishDialogShow" title="发表书籍评论" width="500px">
      <div style="margin-bottom: 15px; display: flex; align-items: center;">
        <span style="margin-right: 10px; color: #666;">评分：</span>
        <el-rate v-model="commentStar" show-text text-color="#1890ff" active-color="#ffc107" style="font-size: 20px;"/>
      </div>
      <ElInput v-model="commentContent" type="textarea" placeholder="请输入你的评论内容..." rows="4" @input="checkCommentLength" @blur="checkCommentLength"/>
      <p v-if="lengthError" style="font-size: 12px; color: #ff4d4f; margin: 4px 0 0 0;">{{ lengthErrorMsg }}</p>
      <template #footer>
        <ElButton @click="isPublishDialogShow = false">取消</ElButton>
        <ElButton type="primary" @click="submitComment">确定</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="isEditBookShow" title="编辑书籍信息" width="1050px" align-center destroy-on-close>

      <div class="edit-book-three-col-layout" style="display: flex; gap: 20px; align-items: stretch; height: 520px;">

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
              <ElInput v-model="editBookForm.iSBN" disabled>
                <template #prepend>ISBN</template>
              </ElInput>

              <ElInput
                  v-model="editBookForm.bookname"
                  placeholder="请输入书籍名称"
                  @input="editBookForm.bookname = filterSpecialCharNormal(editBookForm.bookname)"
                  maxlength="50"
                  show-word-limit
              >
                <template #prepend>书名</template>
              </ElInput>

              <ElInput
                  v-model="editBookForm.author"
                  placeholder="请输入作者"
                  @input="editBookForm.author = filterSpecialCharNormal(editBookForm.author)"
                  maxlength="50"
                  show-word-limit
              >
                <template #prepend>作者</template>
              </ElInput>
            </div>
          </div>

          <ElInput
              v-model="editBookForm.publisher"
              placeholder="请输入出版社"
              @input="editBookForm.publisher = filterSpecialCharNormal(editBookForm.publisher)"
              maxlength="50"
              show-word-limit
          >
            <template #prepend>出版社</template>
          </ElInput>

          <div style="display: flex; gap: 15px; align-items: center;">
            <ElDatePicker v-model="editBookForm.publishDate" type="date" placeholder="出版日期" style="flex: 1;" />
            <ElInput v-model="editBookForm.pictureName" placeholder="输入图片URL" @input="handleEditPictureChange" style="flex: 1;">
              <template #prepend>图片路径</template>
            </ElInput>
          </div>

          <div style="display: flex; gap: 15px; align-items: center; background: var(--el-fill-color-light); padding: 8px 12px; border-radius: 6px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span :style="{ color: isDark ? '#d1d5db' : '#606266', fontSize: '13px' }">修改总量:</span>
              <ElInputNumber v-model="editBookForm.all_book" :min="originStock" placeholder="总量" style="width: 130px;" />
            </div>
            <div style="font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.4;">
              当前存量：<span style="color: var(--el-color-primary); font-weight: bold;">{{ editBookForm.now_book }}</span> 本<br>
              (总量不能低于原始存量 {{ originStock }})
            </div>
          </div>

          <div style="display: flex; flex-direction: column; gap: 8px;">
            <span :style="{ color: isDark ? '#d1d5db' : '#606266', fontSize: '13px' }">书籍信息：</span>
            <ElInput
                v-model="editBookForm.information"
                type="textarea"
                :rows="5"
                maxlength="2000"
                show-word-limit
                placeholder="请输入书籍信息"
            />
          </div>
        </div>

        <el-divider direction="vertical" style="height: 100%; margin: 0;" />

        <div class="col-book-types" style="flex: 0 0 22%; display: flex; flex-direction: column;">
          <h4 :style="{ color: isDark ? '#e5e7eb' : '#333', margin: '0 0 12px 0', display: 'flex', alignItems: 'center', gap: '6px' }">
            📑 书籍类型
          </h4>
          <div class="custom-scroll-y" style="flex: 1; padding-right: 10px;">
            <ElRadioGroup v-model="editBookForm.Type" class="two-col-grid-group">
              <ElRadio
                  v-for="type in bookTypeList"
                  :key="type.id"
                  :label="type.id"
                  class="grid-selector-item"
                  :class="{ dark: isDark }"
                  border
              >
                {{ type.bookType }}
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
            <ElCheckboxGroup v-model="editBookForm.tagIds" class="two-col-grid-group">
              <ElCheckbox
                  v-for="tag in allTagList"
                  :key="tag.id"
                  :label="tag.id"
                  class="grid-selector-item"
                  :class="{ dark: isDark }"
                  border
              >
                {{ tag.tagName }}
              </ElCheckbox>
            </ElCheckboxGroup>
          </div>
        </div>

      </div>

      <template #footer>
        <div style="padding-top: 10px;">
          <ElButton @click="handleCancelEdit" round>取消修改</ElButton>
          <ElButton type="primary" @click="submitEditBook" round>保存更改</ElButton>
        </div>
      </template>
    </ElDialog>
  </el-scrollbar>
</template>

<style scoped>
/* ======================================
   1. 新增：两栏严格网格对齐 Flex 样式
======================================== */
.dual-column-layout-container {
  display: flex;
  gap: 24px;
  padding: 20px;
  align-items: flex-start; /* 顶部对齐 */
}

/* 左侧主干栏：固定最大宽度 */
.left-layout-column {
  width: 45%;
  max-width: 500px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

/* 右侧主干栏：自适应拉满 */
.right-layout-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 书籍详情基础卡片 */
.book-detail-card {
  border-radius: 8px;
  border: 2px dashed #000000;
  width: 100%;
}

/* 独立列内的评论纵向排布容器 */
.column-comments-wrapper {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
}

/* 统一评论卡片基本外观（左右通用） */
.comment-item-card {
  border-radius: 8px;
  border: 1px dashed #000000;
  transition: all 0.2s ease;
  width: 100%;
}
.comment-item-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.comment-body-text {
  font-size: 13px;
  text-align: left;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin-top: 6px;
}

/* 细化紧凑信息项 */
.book-info-item-mini {
  display: flex;
  align-items: flex-start;
  line-height: 1.4;
  margin-bottom: 2px;
}
.book-info-item-mini span:first-child {
  color: #666;
  width: 75px;
  flex-shrink: 0;
}

/* ======================================
   2. 继承并优化原系统基础样式
======================================== */
.el-divider {
  margin: 15px 0;
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

.book-title {
  font-size: 22px;
  font-weight: bold;
  text-align: left;
  cursor: pointer;
  position: relative;
  display: inline-block;
  transition: color 0.2s;
}
.book-title:hover {
  color: #409eff;
}
.book-title::after {
  content: '点击书名，进行阅读';
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  background-color: #333;
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: normal;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s ease, visibility 0.2s ease;
  z-index: 1000;
}
.book-title:hover::after {
  opacity: 1;
  visibility: visible;
}

.book-info-container {
  position: relative;
}
.book-info-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: all 0.3s ease;
}
.book-info-text.show-all {
  -webkit-line-clamp: unset;
  overflow: visible;
}
.book-info-toggle-btn {
  color: #1890ff;
  margin-top: 4px;
  padding: 0;
  font-size: 12px;
}

/* ======================================
   3. 移动端平板等窄屏媒体查询自动降级
======================================== */
@media (max-width: 992px) {
  .dual-column-layout-container {
    flex-direction: column !important;
    gap: 16px;
  }
  .left-layout-column {
    width: 100% !important;
    max-width: none !important;
  }
  .book-title {
    font-size: 20px;
  }
}


:deep(.four-grid-group) {
  display: grid !important;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  max-height: 120px;
  overflow-y: auto;
}
:deep(.grid-radio-item), :deep(.grid-check-item) {
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
:deep(.grid-radio-item.dark), :deep(.grid-check-item.dark) {
  border-color: #4b5563;
  color: #d1d5db;
}

/* 两列/单列自适应网格，用于优化右侧两列的选项卡显示 */
.two-col-grid-group {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  width: 100%;
}

.grid-selector-item {
  margin-right: 0 !important;
  width: 100%;
  border-radius: 6px !important;
  transition: all 0.2s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grid-selector-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.grid-selector-item.dark {
  border-color: #4b5563 !important;
  background: rgba(255, 255, 255, 0.02);
}

/* 局部滚动条美化 */
.custom-scroll-y {
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-dark) transparent;
}
.custom-scroll-y::-webkit-scrollbar { width: 6px; }
.custom-scroll-y::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.02); border-radius: 4px; }
.custom-scroll-y::-webkit-scrollbar-thumb { background: var(--el-border-color-dark); border-radius: 4px; }

/* 紧凑型前缀插槽 */
.col-basic-info :deep(.el-input-group__prepend) {
  width: 60px;
  text-align: center;
  padding: 0 10px;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}

</style>