<script setup>
import { ref, onMounted, inject, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request.js'
import { ElMessage } from 'element-plus'
import { Edit, Collection, Search, CollectionTag, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const currentUserId = inject('currentUserId')
const isDark = inject('isDark')

// 🔥 注入全局的卡片解析和预览函数
const parseBookLinkToCard = inject('parseBookLinkToCard')
const updateAllBookCards = inject('updateAllBookCards')
const previewBookCards = inject('previewBookCards')
const updatePreviewBookCards = inject('updatePreviewBookCards')
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL

// 列表数据
const postList = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const sortType = ref('time')

// 发帖弹窗控制
const showPublishDialog = ref(false)
const postForm = ref({ type: 1, content: '' })

// 插书弹窗控制
const showBookSelectDialog = ref(false)
const activeBookTab = ref('bookshelf')
const myBooks = ref([])
const searchKey = ref('')
const searchSuggests = ref([])
const isComposing = ref(false)

// ======================================
// 核心：纯文本与卡片分离解析
// ======================================
const cardReg = /\[{(\w+):([a-zA-Z0-9_-]+)}\]/g
const extractBookCards = (content) => {
  if (!content) return []
  const cards = []
  let match
  while ((match = cardReg.exec(content)) !== null) {
    cards.push({ type: match[1], id: match[2], link: match[0] })
  }
  return cards
}
const extractPureText = (content) => content ? content.replace(cardReg, '').trim() : ''

const getParsedForumContent = (text) => {
  return {
    text: extractPureText(text),
    cards: extractBookCards(text)
  }
}

// 左右滚动控制
const scrollCards = (id, direction) => {
  const container = document.getElementById(id)
  if (!container) return
  container.scrollBy({ left: direction === 'left' ? -180 : 180, behavior: 'smooth' })
}

// ========== 1. 帖子加载逻辑 ==========
const loadPosts = async (reset = false) => {
  if (reset) {
    page.value = 1
    postList.value = []
    hasMore.value = true
  }
  if (!hasMore.value || loading.value) return

  loading.value = true
  try {
    const res = await request.get('/book/square/post', {
      params: { action: 'queryMain', pageNum: page.value, pageSize: 12, sort: sortType.value }
    })
    if (res.code === 200 && res.data.list) {
      postList.value.push(...res.data.list)
      if (postList.value.length >= res.data.total) hasMore.value = false
      page.value++
      // 🔥 数据加载完毕后，通知全局渲染书籍卡片信息
      await nextTick()
      updateAllBookCards()
    }
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// ========== 2. 发帖逻辑 ==========
const submitPost = async () => {
  if (!postForm.value.content.trim()) return ElMessage.warning('内容不能为空')
  try {
    const res = await request.post('/book/square/post', {
      parentId: 0,
      userId: currentUserId.value,
      content: postForm.value.content,
      type: postForm.value.type,
      status: 1
    })
    if (res.code === 200) {
      ElMessage.success('发布成功')
      showPublishDialog.value = false
      postForm.value.content = ''
      previewBookCards.value = [] // 清空预览
      loadPosts(true)
    }
  } catch (err) {
    ElMessage.error('发布失败')
  }
}

// ========== 3. 插书功能逻辑 ==========
const loadMyBooks = async () => {
  try {
    const res = await request.get('/user/collection', { params: { userId: currentUserId.value, page: 1, pageSize: 50 } })
    myBooks.value = (res.data?.list || []).filter(item => item?.isbn)
  } catch (err) { }
}

const fetchSuggestions = async (queryString, callback) => {
  if (isComposing.value || !queryString.trim()) return callback([])
  try {
    const res = await request.get('/book', { params: { suggest: true, keyword: queryString.trim(), searchType: 'bookName' } })
    searchSuggests.value = res.data || []
    callback(searchSuggests.value)
  } catch (err) { callback([]) }
}

const insertBook = (isbn) => {
  postForm.value.content += ` [{book:${isbn}}] `
  showBookSelectDialog.value = false
  updatePreviewBookCards(postForm.value.content) // 插入书籍后立即更新预览
  ElMessage.success('书籍已插入内容区')
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="book-square-container">
    <div class="square-header">
      <div class="btn-group">
        <el-button round :type="sortType === 'time' ? 'primary' : 'default'" @click="sortType='time'; loadPosts(true)">最新</el-button>
        <el-button round :type="sortType === 'hot' ? 'primary' : 'default'" @click="sortType='hot'; loadPosts(true)">最热</el-button>
      </div>
      <el-button type="success" icon="Edit" round @click="showPublishDialog = true; previewBookCards = []">发布帖子</el-button>
    </div>

    <div class="staggered-grid">
      <div
          v-for="post in postList"
          :key="post.id"
          class="grid-item"
          @click="router.push(`/bookSquare/detail/${post.id}`)"
      >
        <el-card class="post-card" shadow="hover">
          <div class="post-tag" :class="post.type === 1 ? 'push' : 'ask'">
            {{ post.type === 1 ? '推书' : '求书' }}
          </div>
          <div class="post-user">@{{ post.userName || post.userId }}</div>
          <div class="post-content">{{ getParsedForumContent(post.content).text }}</div>

          <div v-if="getParsedForumContent(post.content).cards.length > 0" class="message-cards-container" style="margin-top: 12px;">
            <div v-if="getParsedForumContent(post.content).cards.length === 1" class="book-share-card-wrapper">
              <div v-html="parseBookLinkToCard(getParsedForumContent(post.content).cards[0].link, false)"></div>
            </div>
            <div v-else class="multi-card-slider-wrapper" @click.stop>
              <div class="slider-arrow left" @click.stop="scrollCards(`scroll-main-${post.id}`, 'left')">
                <el-icon><ArrowLeft /></el-icon>
              </div>
              <div class="multi-card-scroll-view" :id="`scroll-main-${post.id}`">
                <div
                    v-for="card in getParsedForumContent(post.content).cards"
                    :key="card.id"
                    class="slider-item"
                    v-html="parseBookLinkToCard(card.link, true)"
                ></div>
              </div>
              <div class="slider-arrow right" @click.stop="scrollCards(`scroll-main-${post.id}`, 'right')">
                <el-icon><ArrowRight /></el-icon>
              </div>
            </div>
          </div>

        </el-card>
      </div>
    </div>

    <div class="load-more">
      <el-button v-if="hasMore" :loading="loading" @click="loadPosts(false)" round>加载更多</el-button>
      <span v-else class="no-more">没有更多了~</span>
    </div>

    <el-dialog v-model="showPublishDialog" title="发布帖子" width="500px">
      <div v-if="previewBookCards.length > 0" style="margin-bottom: 15px;">
        <div v-if="previewBookCards.length === 1">
          <div class="book-share-card inline-card" v-for="card in previewBookCards" :key="card.id">
            <img :src="card.loading ? '/default-book.png' : IMAGE_BASE_URL + (card.data?.pictureName || '/default-book.png')" class="book-share-card-cover">
            <div class="book-share-card-info">
              <p class="book-title">{{ card.loading ? '加载中...' : card.data?.bookname || '未知书籍' }}</p>
              <p class="book-rating">{{ card.loading ? '' : `⭐ 评分：${card.data?.star || 0}` }}</p>
              <p class="book-desc">{{ card.loading ? '' : (card.data?.information?.slice(0, 50) + '...' || '暂无简介') }}</p>
            </div>
          </div>
        </div>
        <div v-else class="multi-card-slider-wrapper">
          <div class="slider-arrow left" @click="scrollCards('preview-scroll', 'left')"><el-icon><ArrowLeft /></el-icon></div>
          <div class="multi-card-scroll-view" id="preview-scroll">
            <div v-for="card in previewBookCards" :key="card.id" class="slider-item">
              <div class="book-share-card simplified-card">
                <div class="simplified-cover-wrapper">
                  <img :src="card.loading ? '/default-book.png' : IMAGE_BASE_URL + (card.data?.pictureName || '/default-book.png')" class="book-share-card-cover">
                  <div class="badge">{{ card.loading ? '--' : (card.data?.star || 0) }}分</div>
                </div>
                <p class="book-title">{{ card.loading ? '加载中...' : card.data?.bookname || '未知书籍' }}</p>
              </div>
            </div>
          </div>
          <div class="slider-arrow right" @click="scrollCards('preview-scroll', 'right')"><el-icon><ArrowRight /></el-icon></div>
        </div>
      </div>

      <el-select v-model="postForm.type" style="width: 100%; margin-bottom: 15px;">
        <el-option label="📢 推书 (我要推荐好书)" :value="1" />
        <el-option label="🆘 求书 (我书荒了求推荐)" :value="2" />
      </el-select>

      <el-input
          v-model="postForm.content"
          type="textarea"
          :rows="6"
          placeholder="写点什么吧，支持插入书籍哦..."
          @input="(val) => updatePreviewBookCards(val)"
      />

      <div style="margin-top: 10px;">
        <el-button type="primary" plain size="small" icon="Collection" @click="showBookSelectDialog = true; loadMyBooks()">
          插入书籍
        </el-button>
      </div>

      <template #footer>
        <el-button @click="showPublishDialog = false" round>取消</el-button>
        <el-button type="primary" @click="submitPost" round>发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBookSelectDialog" title="选择书籍" width="400px" append-to-body>
      <el-tabs v-model="activeBookTab">
        <el-tab-pane label="我的书架" name="bookshelf">
          <div class="book-list-mini">
            <div v-for="book in myBooks" :key="book.isbn" class="mini-book-item" @click="insertBook(book.isbn)">
              <span>{{ book.bookName || book.isbn }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="全网搜索" name="search">
          <el-autocomplete
              v-model="searchKey"
              :fetch-suggestions="fetchSuggestions"
              value-key="bookname"
              placeholder="输入书名联想..."
              @select="(item) => insertBook(item.iSBN)"
              @compositionstart="isComposing = true"
              @compositionend="isComposing = false"
              style="width: 100%;"
          >
            <template #default="{ item }"><span>{{ item.bookname }}</span></template>
          </el-autocomplete>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 包含刚才所有的 grid 和基础样式 */
.book-square-container {  overflow-y: auto; padding: 20px; }
.square-header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.staggered-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; grid-auto-flow: dense; }
.grid-item:nth-child(4n + 1) { grid-column: span 2; }
.grid-item:nth-child(4n + 2) { grid-column: span 1; }
.grid-item:nth-child(4n + 3) { grid-column: span 1; }
.grid-item:nth-child(4n + 4) { grid-column: span 2; }
.post-card { height: 100%; min-height: 150px; position: relative; cursor: pointer; border-radius: 12px; }
.post-tag { position: absolute; top: 10px; right: 10px; font-size: 12px; padding: 2px 8px; border-radius: 10px; color: #fff; z-index: 10;}
.post-tag.push { background: #67C23A; }
.post-tag.ask { background: #E6A23C; }
.post-user { font-weight: bold; color: #409EFF; margin-bottom: 10px; }
.post-content { font-size: 14px; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; white-space: pre-wrap;}
.load-more { text-align: center; margin-top: 20px; padding-bottom: 20px;}
.book-list-mini { max-height: 300px; overflow-y: auto; }
.mini-book-item { padding: 10px; border-bottom: 1px solid #eee; cursor: pointer; }

/* ====================== 横向滚动容器核心 CSS ====================== */
.multi-card-slider-wrapper {
  display: flex !important; flex-direction: row !important; align-items: center; position: relative; width: 100%;
}
.multi-card-scroll-view {
  display: flex !important; flex-direction: row !important; flex-wrap: nowrap !important; gap: 12px;
  overflow-x: auto; scroll-behavior: smooth; flex: 1; padding: 4px 0; scrollbar-width: none;
  -webkit-overflow-scrolling: touch; /* iOS 开启惯性滚动，松手后继续滑行 */
  touch-action: pan-x; /* 明确只处理横向滑动，避免干扰页面垂直滚动 */
}
.multi-card-scroll-view::-webkit-scrollbar { display: none; }
.slider-arrow {
  position: absolute; z-index: 10; width: 28px; height: 28px; border-radius: 50%; background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 6px rgba(0,0,0,0.15); display: flex; justify-content: center; align-items: center; cursor: pointer; opacity: 0; transition: all 0.2s;
}
.multi-card-slider-wrapper:hover .slider-arrow { opacity: 1; }
.slider-arrow:hover { color: #1890ff; transform: scale(1.1); }
.slider-arrow.left { left: -14px; }
.slider-arrow.right { right: -14px; }
.slider-item { display: block !important; width: max-content !important; flex-shrink: 0 !important; margin: 0 !important; }

/* 简易卡片样式 */
:deep(.simplified-card) { display: flex; flex-direction: column; align-items: center; width: 90px !important; flex-shrink: 0 !important; background: transparent; border: none; box-shadow: none; cursor: pointer; }
:deep(.simplified-card:hover) { transform: translateY(-2px); }
:deep(.simplified-cover-wrapper) { position: relative; width: 90px; height: 120px; border-radius: 6px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
:deep(.simplified-cover-wrapper .book-share-card-cover) { width: 100%; height: 100%; object-fit: cover; }
:deep(.simplified-cover-wrapper .badge) { position: absolute; bottom: 0; left: 0; background: rgba(0, 0, 0, 0.6); color: #ff9500; font-size: 11px; font-weight: bold; padding: 2px 6px; border-top-right-radius: 6px; }
:deep(.simplified-card .book-title) { width: 100%; font-size: 13px; color: #333; margin-top: 6px; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ==================== 📱 手机端专属适配 ==================== */
@media screen and (max-width: 768px) {
  /* 1. 将网格改为单列显示 */
  .staggered-grid {
    grid-template-columns: 1fr;
    gap: 12px; /* 手机端可以适当缩小一点间隙 */
  }

  /* 2. 强制覆盖大屏下的不对称跨列设置，让所有卡片都只占 1 列 */
  .grid-item:nth-child(n) {
    grid-column: span 1 !important;
  }
  /* 👇 新增：书籍横向滚动区手机适配 */
  /* 1. 隐藏左右箭头，手机端纯触摸滑动 */
  .slider-arrow {
    display: none !important;
  }

  /* 2. 调整滚动容器间距与内边距，首末卡片不贴边 */
  .multi-card-scroll-view {
    gap: 10px !important;
    padding: 4px 6px !important;
  }
  /* 4. 发帖弹窗内的简化卡片同步缩小 */
  :deep(.simplified-card) {
    width: 70px !important;
  }
  :deep(.simplified-cover-wrapper) {
    width: 70px !important;
    height: 95px !important;
  }
  :deep(.simplified-card .book-title) {
    font-size: 11px !important;
  }

  /* 5. 卡片容器左右微扩，抵消内边距让卡片对齐内容 */
  .message-cards-container {
    margin: 12px -6px 0 -6px !important;
  }
}
</style>