<script setup>
import { ref, onMounted, inject, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request.js'
import { ElMessage } from 'element-plus'
import { Collection, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const currentUserId = inject('currentUserId')
const parseBookLinkToCard = inject('parseBookLinkToCard')
const updateAllBookCards = inject('updateAllBookCards')
const updatePreviewBookCards = inject('updatePreviewBookCards')
const previewBookCards = inject('previewBookCards')
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL

const mainPost = ref(null)
const subPosts = ref([])
const page = ref(1)
const hasMore = ref(true)

// 回复弹窗相关
const showSubPublishDialog = ref(false)
const subContent = ref('')

// 插书弹窗相关
const showBookSelectDialog = ref(false)
const activeBookTab = ref('bookshelf')
const myBooks = ref([])
const searchKey = ref('')
const searchSuggests = ref([])
const isComposing = ref(false)

// ======================================
// 核心解析逻辑
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

const scrollCards = (id, direction) => {
  const container = document.getElementById(id)
  if (!container) return
  container.scrollBy({ left: direction === 'left' ? -180 : 180, behavior: 'smooth' })
}

// ========== 1. 加载数据 ==========
const loadData = async () => {
  try {
    // 假设后端有根据ID查询主帖详情的接口：action='getById'
    const resMain = await request.get('/book/square/post', {
      params: { action: 'getById', id: route.params.id }
    })
    if (resMain.code === 200) mainPost.value = resMain.data

    // 加载子帖分页数据
    const resSub = await request.get('/book/square/post', {
      params: { action: 'querySub', parentId: route.params.id, pageNum: page.value, pageSize: 20 }
    })
    if (resSub.code === 200) {
      subPosts.value.push(...resSub.data.list)
      if (subPosts.value.length >= resSub.data.total) hasMore.value = false
      page.value++
    }

    // 更新渲染页面中的所有书籍卡片信息
    await nextTick()
    updateAllBookCards()
  } catch (err) { }
}

// ========== 2. 回复与插书功能 ==========
const submitSubPost = async () => {
  if (!subContent.value.trim()) return ElMessage.warning('内容不能为空')
  try {
    const res = await request.post('/book/square/post', {
      parentId: route.params.id,
      userId: currentUserId.value,
      content: subContent.value,
      type: 0,
      status: 1
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      showSubPublishDialog.value = false
      subContent.value = ''
      previewBookCards.value = [] // 提交清空预览
      page.value = 1
      subPosts.value = []
      hasMore.value = true
      loadData()
    }
  } catch (err) { }
}

// 插书相关方法 (复用主页逻辑)
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
  subContent.value += ` [{book:${isbn}}] `
  showBookSelectDialog.value = false
  updatePreviewBookCards(subContent.value)
  ElMessage.success('书籍已插入内容区')
}

// ⚠️ 🔥 已移除 JS 控制便签颜色的逻辑，全部交由 CSS 处理！

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="glass-panel detail-wrapper" style="height: calc(100vh - 100px); display: flex; flex-direction: column;">

    <div class="main-post-section" style="padding: 20px; flex-shrink: 0;" v-if="mainPost">
      <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
        <span style="font-weight: bold; font-size: 18px; color: #409EFF;">楼主: @{{ mainPost.userName || mainPost.userId }}</span>
        <el-button round @click="$router.back()">返回广场</el-button>
      </div>

      <div class="main-content" style="font-size: 16px; white-space: pre-wrap; margin-bottom: 15px;">
        {{ getParsedForumContent(mainPost.content).text }}
      </div>

      <div v-if="getParsedForumContent(mainPost.content).cards.length > 0" class="message-cards-container">
        <div v-if="getParsedForumContent(mainPost.content).cards.length === 1" class="book-share-card-wrapper" style="max-width: 400px;">
          <div v-html="parseBookLinkToCard(getParsedForumContent(mainPost.content).cards[0].link, false)"></div>
        </div>
        <div v-else class="multi-card-slider-wrapper">
          <div class="slider-arrow left" @click.stop="scrollCards('scroll-detail-main', 'left')"><el-icon><ArrowLeft /></el-icon></div>
          <div class="multi-card-scroll-view" id="scroll-detail-main">
            <div
                v-for="card in getParsedForumContent(mainPost.content).cards"
                :key="card.id"
                class="slider-item"
                v-html="parseBookLinkToCard(card.link, true)"
            ></div>
          </div>
          <div class="slider-arrow right" @click.stop="scrollCards('scroll-detail-main', 'right')"><el-icon><ArrowRight /></el-icon></div>
        </div>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 15px;">
        <span style="font-size: 12px; color: #999;">发布于 {{ mainPost.createTime }}</span>
        <el-button type="primary" round @click="showSubPublishDialog = true; previewBookCards = []">我要回复</el-button>
      </div>
    </div>

    <el-divider style="margin: 0;"></el-divider>

    <div class="sub-posts-section" style="flex: 1; overflow-y: auto; padding: 20px;">

      <div class="notes-masonry-container">
        <div v-for="post in subPosts" :key="post.id" class="note-wrapper">

          <div class="washi-tape"></div>

          <el-card class="sticky-note-card">
            <div style="font-weight: bold; margin-bottom: 10px; color: #409EFF;">@{{ post.userName || post.userId }}</div>

            <div style="font-size: 14px; white-space: pre-wrap; margin-bottom: 12px;">
              {{ getParsedForumContent(post.content).text }}
            </div>

            <div v-if="getParsedForumContent(post.content).cards.length > 0" class="message-cards-container">
              <div v-if="getParsedForumContent(post.content).cards.length === 1" class="book-share-card-wrapper" style="max-width: 100%;">
                <div v-html="parseBookLinkToCard(getParsedForumContent(post.content).cards[0].link, false)"></div>
              </div>
              <div v-else class="multi-card-slider-wrapper">
                <div class="slider-arrow left" @click.stop="scrollCards('scroll-detail-sub-' + post.id, 'left')"><el-icon><ArrowLeft /></el-icon></div>
                <div class="multi-card-scroll-view" :id="'scroll-detail-sub-' + post.id">
                  <div
                      v-for="card in getParsedForumContent(post.content).cards"
                      :key="card.id"
                      class="slider-item"
                      v-html="parseBookLinkToCard(card.link, true)"
                  ></div>
                </div>
                <div class="slider-arrow right" @click.stop="scrollCards('scroll-detail-sub-' + post.id, 'right')"><el-icon><ArrowRight /></el-icon></div>
              </div>
            </div>

            <div style="font-size: 12px; color: #999; margin-top: 15px; text-align: right;">{{ post.createTime }}</div>
          </el-card>
        </div>
      </div>

      <div style="text-align: center; margin-top: 20px; padding-bottom: 30px;">
        <el-button v-if="hasMore" round @click="loadData">加载更多回复</el-button>
        <span v-else style="color: #999; font-size: 14px;">没有更多回复了</span>
      </div>
    </div>

    <el-dialog v-model="showSubPublishDialog" title="回复帖子" width="500px">
      <div v-if="previewBookCards.length > 0" style="margin-bottom: 15px;">
        <div v-if="previewBookCards.length === 1">
          <div class="book-share-card inline-card" v-for="card in previewBookCards" :key="card.id">
            <img :src="card.loading ? '/default-book.png' : IMAGE_BASE_URL + (card.data?.pictureName || '/default-book.png')" class="book-share-card-cover">
            <div class="book-share-card-info">
              <p class="book-title">{{ card.loading ? '加载中...' : card.data?.bookname || '未知书籍' }}</p>
              <p class="book-rating">{{ card.loading ? '' : `⭐ 评分：${card.data?.star || 0}` }}</p>
            </div>
          </div>
        </div>
        <div v-else class="multi-card-slider-wrapper">
          <div class="slider-arrow left" @click="scrollCards('preview-reply-scroll', 'left')"><el-icon><ArrowLeft /></el-icon></div>
          <div class="multi-card-scroll-view" id="preview-reply-scroll">
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
          <div class="slider-arrow right" @click="scrollCards('preview-reply-scroll', 'right')"><el-icon><ArrowRight /></el-icon></div>
        </div>
      </div>

      <el-input
          v-model="subContent"
          type="textarea"
          :rows="5"
          placeholder="写下你的神评论..."
          @input="(val) => updatePreviewBookCards(val)"
      />
      <div style="margin-top: 10px;">
        <el-button type="primary" plain size="small" icon="Collection" @click="showBookSelectDialog = true; loadMyBooks()">
          插入书籍
        </el-button>
      </div>
      <template #footer>
        <el-button @click="showSubPublishDialog = false" round>取消</el-button>
        <el-button type="primary" @click="submitSubPost" round>提交回复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBookSelectDialog" title="选择书籍" width="400px" append-to-body>
      <el-tabs v-model="activeBookTab">
        <el-tab-pane label="我的书架" name="bookshelf">
          <div class="book-list-mini">
            <div v-for="book in myBooks" :key="book.isbn" class="mini-book-item" @click="insertBook(book.isbn)">
              <span>{{ book.bookName }}</span>
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
/* =========== 上半部分 主帖区域 =========== */
.main-post-section { background: transparent; }

/* =========== 下半部分 便签回复区域 (🔥 核心修改：两列瀑布流) =========== */
.notes-masonry-container {
  column-count: 2; /* 默认两列一行 */
  column-gap: 24px;
  padding: 10px 0;
}

.note-wrapper {
  break-inside: avoid; /* 防截断 */
  position: relative;
  display: inline-block;
  width: 100%;
  margin-bottom: 28px;
}

/* ======================================
   🚀 性能优化版：纯 CSS 实现手账便签随机样式
======================================== */
/* 便签卡片背景与边框交替循环 */
.note-wrapper:nth-child(4n+1) .sticky-note-card { background-color: #f3faf4 !important; border-color: #cfead4 !important; transform: rotate(0.4deg); }
.note-wrapper:nth-child(4n+2) .sticky-note-card { background-color: #fffdeb !important; border-color: #ebdca5 !important; transform: rotate(-0.8deg); }
.note-wrapper:nth-child(4n+3) .sticky-note-card { background-color: #edf7f5 !important; border-color: #cceee2 !important; transform: rotate(1.2deg); }
.note-wrapper:nth-child(4n+4) .sticky-note-card { background-color: #fffdf0 !important; border-color: #e3d69c !important; transform: rotate(-1.2deg); }

/* 纸胶带颜色与角度交替循环 */
.note-wrapper:nth-child(4n+1) .washi-tape { background-color: rgba(168, 218, 220, 0.5); transform: translateX(-50%) rotate(-4deg); }
.note-wrapper:nth-child(4n+2) .washi-tape { background-color: rgba(241, 166, 166, 0.5); transform: translateX(-50%) rotate(4deg); }
.note-wrapper:nth-child(4n+3) .washi-tape { background-color: rgba(233, 196, 106, 0.5); transform: translateX(-50%) rotate(-2deg); }
.note-wrapper:nth-child(4n+4) .washi-tape { background-color: rgba(138, 201, 143, 0.5); transform: translateX(-50%) rotate(5deg); }


/* 基础卡片悬浮样式 */
.sticky-note-card {
  border-radius: 8px; border: 1px dashed var(--el-border-color); box-shadow: 2px 5px 15px rgba(0,0,0,0.05); transition: all 0.3s;
}
.sticky-note-card:hover {
  transform: scale(1.02) rotate(0deg) !important; box-shadow: 5px 10px 20px rgba(0,0,0,0.1) !important; z-index: 5;
}

.washi-tape {
  position: absolute; top: -10px; left: 50%; width: 90px; height: 25px; z-index: 10;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  clip-path: polygon(0% 15%, 4% 0%, 96% 0%, 100% 12%, 98% 50%, 100% 88%, 95% 100%, 5% 100%, 0% 85%, 2% 50%);
}

.book-list-mini { max-height: 300px; overflow-y: auto; }
.mini-book-item { padding: 10px; border-bottom: 1px solid #eee; cursor: pointer; }

/* ====================== 横向滚动容器核心 CSS ====================== */
.multi-card-slider-wrapper {
  display: flex !important; flex-direction: row !important; align-items: center; position: relative; width: 100%;
}
.multi-card-scroll-view {
  display: flex !important; flex-direction: row !important; flex-wrap: nowrap !important; gap: 12px;
  overflow-x: auto; scroll-behavior: smooth; flex: 1; padding: 4px 0; scrollbar-width: none;
  /* 👇 新增：手机端触摸滚动核心属性 */
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

/* ====================== 响应式适配 ====================== */
@media (max-width: 768px) {
  .notes-masonry-container {
    column-count: 1; /* 手机端恢复单列一排 */
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

  /* 4. 回复弹窗内的简化卡片同步缩小 */
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

/* ====================== 正确暗黑模式适配 html.dark + note-wrapper ====================== */
/* 1. 便签卡片深色背景覆盖 */
html.dark .note-wrapper:nth-child(4n+1) .sticky-note-card {
  background-color: #1D2D46 !important;
  border-color: #2b3d5c !important;
}
html.dark .note-wrapper:nth-child(4n+2) .sticky-note-card {
  background-color: #2F3A37 !important;
  border-color: #3e4c49 !important;
}
html.dark .note-wrapper:nth-child(4n+3) .sticky-note-card {
  background-color: #262E30 !important;
  border-color: #364144 !important;
}
html.dark .note-wrapper:nth-child(4n+4) .sticky-note-card {
  background-color: #2E3B3E !important;
  border-color: #405155 !important;
}

/* 2. 纸胶带深色半透明 */
html.dark .note-wrapper:nth-child(4n+1) .washi-tape {
  background-color: rgba(235, 181, 92, 0.45) !important;
}
html.dark .note-wrapper:nth-child(4n+2) .washi-tape {
  background-color: rgba(160, 187, 177, 0.35) !important;
}
html.dark .note-wrapper:nth-child(4n+3) .washi-tape {
  background-color: rgba(220, 239, 233, 0.25) !important;
}
html.dark .note-wrapper:nth-child(4n+4) .washi-tape {
  background-color: rgba(173, 198, 176, 0.35) !important;
}

/* 3. 便签内文字、用户名、时间深色适配（模板无自定义class，直接匹配内部元素） */
html.dark .sticky-note-card {
  color: #d1d5db !important;
}
/* 用户名蓝色文字提亮 */
html.dark .sticky-note-card div[style*="color: #409EFF"] {
  color: #60a5fa !important;
}
/* 底部时间灰色文字 */
html.dark .sticky-note-card div[style*="color: #999"] {
  color: #9ca3af !important;
}

/* 4. el-card 内部透明背景，避免叠加白色 */
html.dark :deep(.sticky-note-card .el-card__body) {
  background: transparent !important;
}
</style>