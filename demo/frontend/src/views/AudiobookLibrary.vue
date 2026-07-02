<template>
  <div class="audiobook-library-container" :class="{ 'dark-mode': isDark }">
    <div class="library-header glass-panel">
      <div class="title-area">
        <span class="main-title">🎧 AI 智能有声书屋</span>
        <span class="sub-title">听见文字的心跳与剧播光影</span>
      </div>
      <div class="search-area">
        <ElInput
            v-model="keyword"
            placeholder="搜索有声书名称、作者..."
            clearable
            class="custom-glass-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
        >
          <template #append>
            <ElButton icon="Search" @click="handleSearch" />
          </template>
        </ElInput>
      </div>
    </div>

    <div class="book-shelf-main glass-panel">
      <ElSkeleton v-if="loading" :rows="4" animated :count="8" class="shelf-skeleton-grid" />

      <ElEmpty v-else-if="bookList.length === 0" description="暂无相关有声书资源~" />

      <div v-else class="book-grid">
        <div
            v-for="book in bookList"
            :key="book.id"
            class="audiobook-card"
            @click="openChapterModal(book)"
        >
          <div class="tag-paper-host">
            <div class="tag-hole"></div>

            <div class="card-cover-wrap">
              <ElImage
                  :src="book.pictureName || '/default-book.png'"
                  fit="cover"
                  class="book-cover"
                  lazy
              />
              <div class="cover-hover-mask">
                <span class="play-trigger">▶</span>
              </div>
            </div>

            <div class="card-info">
              <div class="book-name" :title="book.bookName">{{ book.bookName }}</div>
              <div class="book-author">{{ book.author || '佚名' }}</div>

              <div class="card-footer">
                <ElTag size="small" type="warning" effect="dark" round class="chapter-tag">
                  🎙️ {{ book.generatedCount || 0 }}/{{ book.chapterCount || '?' }}章
                </ElTag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-bar glass-panel" v-if="total > 0">
      <ElPagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          background
          layout="total, prev, pager, next"
          @current-change="fetchBooks"
      />
    </div>

    <ElDialog
        v-model="showChapterDialog"
        :title="`📖 《${activeBook?.bookName || ''}》· 剧播选集`"
        width="620px"
        append-to-body
        destroy-on-close
        class="custom-glass-dialog"
    >
      <AudiobookLibraryChapter
          v-if="activeBook"
          :isbn="activeBook.isbn"
          :book-name="activeBook.bookName"
          @play="handlePlayChapter"
      />
    </ElDialog>

    <AudiobookPlayer
        v-model:visible="showPlayer"
        :isbn="playerIsbn"
        :chapter-number="playerChapter"
        :book-name="playerBookName"
        :is-dark="isDark"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import request from '../utils/request'
import AudiobookLibraryChapter from './AudiobookLibraryChapter.vue'
import AudiobookPlayer from '../components/AudiobookPlayer.vue'

defineOptions({ name: 'AudiobookLibrary' })

const isDark = inject('isDark', ref(false))

const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const bookList = ref([])
const loading = ref(false)

const showChapterDialog = ref(false)
const activeBook = ref(null)

const showPlayer = ref(false)
const playerIsbn = ref('')
const playerChapter = ref(1)
const playerBookName = ref('')

const fetchBooks = async () => {
  loading.value = true
  try {
    const res = await request.get('/audiobook/books', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword.value.trim()
      }
    })
    if (res.code === 200 && res.data) {
      bookList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('拉取有声书架报错', err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchBooks()
}

const openChapterModal = (book) => {
  activeBook.value = book
  showChapterDialog.value = true
}

const handlePlayChapter = ({ isbn, chapterNumber, bookName }) => {
  showChapterDialog.value = false
  playerIsbn.value = isbn
  playerChapter.value = chapterNumber
  playerBookName.value = bookName
  showPlayer.value = true
}

onMounted(() => {
  fetchBooks()
})
</script>

<style scoped>
.audiobook-library-container {
  display: flex; flex-direction: column; gap: 20px; height: 100%; padding-bottom: 20px;
}

/* ==================== 顶部搜索栏 ==================== */
.library-header {
  padding: 20px 28px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;
}
.title-area { display: flex; align-items: baseline; gap: 12px; }
.main-title { font-size: 22px; font-weight: bold; color: var(--el-text-color-primary); }
.sub-title { font-size: 13px; color: #888; }
.search-area { width: clamp(260px, 40vw, 360px); }

/* ==================== 书架主体 (完全交由 glass-panel 接管背景) ==================== */
.book-shelf-main {
  flex: 1; overflow-y: auto; padding: 32px 20px;
  position: relative; border-radius: 18px;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(185px, 1fr)); /* 更窄的比率，更像书签 */
  gap: 46px 32px;
  align-items: start;
  padding: 10px 6px;
}

/* ==================== 🔥 图3同款：垂挂式独立书签卡片 ==================== */
.audiobook-card {
  position: relative;
  background: transparent !important; /* 外壳必须透明 */
  border: none !important;
  box-shadow: none !important;
  overflow: visible !important;
  cursor: pointer;
  /* 神级技巧：在父容器投影，能精准顺着子容器的 clip-path 多边形外轮廓产生真实阴影！ */
  filter: drop-shadow(0 10px 16px rgba(110, 85, 55, 0.16));
  transition: transform 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex; flex-direction: column;
}

.audiobook-card:hover {
  transform: translateY(-8px) rotate(-1.5deg);
  filter: drop-shadow(0 16px 26px rgba(110, 85, 55, 0.28));
}

/* 纸张基底容器 */
.tag-paper-host {
  background: linear-gradient(180deg, #FFF9F2 0%, #FFF0DC 100%);
  /* 削掉左上和右上角，诞生复古标签轮廓 */
  clip-path: polygon(18% 0%, 82% 0%, 100% 9%, 100% 100%, 0% 100%, 0% 9%);
  display: flex; flex-direction: column;
  position: relative;
  padding-top: 20px; /* 给头部圆孔留出空间 */
  border-bottom: 3px dashed #B89770; /* 底部虚线缝口 */
  border-radius: 0 0 6px 6px;
}

/* 顶端开凿的打孔圆眼 */
.tag-hole {
  position: absolute;
  top: 7px; left: 50%; transform: translateX(-50%);
  width: 12px; height: 12px; border-radius: 50%;
  background: rgba(0, 0, 0, 0.12);
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.4), 0 1px 0 rgba(255,255,255,0.9);
  z-index: 10;
}

/* 穿过孔眼往上提的垂直细索 */
.audiobook-card::before {
  content: '';
  position: absolute;
  top: -22px; left: calc(50% - 1px);
  width: 2px; height: 30px;
  background: linear-gradient(to bottom, #A68A68, #7d6548);
  z-index: 5;
}

/* 卡片之间交错串联的下坠麻绳 */
.audiobook-card::after {
  content: '';
  position: absolute;
  top: -14px; right: -28px;
  width: 38px; height: 18px;
  border-bottom: 2px dashed #B89770;
  border-radius: 50%; /* 形成自然下坠的弧度 */
  transform: rotate(-12deg);
  z-index: 1;
  pointer-events: none;
}

/* ==================== 暗黑模式卡片材质适配 ==================== */
.dark-mode .audiobook-card { filter: drop-shadow(0 10px 18px rgba(0,0,0,0.55)); }
.dark-mode .audiobook-card:hover { filter: drop-shadow(0 16px 28px rgba(0,0,0,0.85)); }

.dark-mode .tag-paper-host {
  background: linear-gradient(180deg, #2A241B 0%, #1E1A14 100%);
  border-bottom-color: #8C7355;
}
.dark-mode .tag-hole {
  background: rgba(0, 0, 0, 0.55);
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.8), 0 1px 0 rgba(255,255,255,0.08);
}
.dark-mode .audiobook-card::before { background: linear-gradient(to bottom, #8C7355, #594834); }
.dark-mode .audiobook-card::after  { border-bottom-color: #8C7355; }

/* ==================== 书籍立体包裹层 ==================== */
.card-cover-wrap {
  width: calc(100% - 20px);
  margin: 6px 10px 0;
  aspect-ratio: 3 / 4;
  position: relative; overflow: hidden;
  border-radius: 6px;
  box-shadow: 2px 4px 10px rgba(0,0,0,0.15);
  background: #eee;
}

/* 顶部微光遮罩 */
.card-cover-wrap::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.18) 0%, transparent 30%);
  z-index: 2; pointer-events: none;
}

/* 右侧立体书脊线 */
.card-cover-wrap::after {
  content: ''; position: absolute; top: 0; right: 0; bottom: 0; width: 4px;
  background: linear-gradient(90deg, rgba(0,0,0,0.02) 0%, rgba(0,0,0,0.3) 100%);
  z-index: 3; pointer-events: none;
}

.book-cover { width: 100%; height: 100%; }
:deep(.book-cover img) { transition: transform 0.4s ease !important; }
.audiobook-card:hover :deep(.book-cover img) { transform: scale(1.06); }

.cover-hover-mask {
  position: absolute; inset: 0; background: rgba(0,0,0,0.38); display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.25s ease; backdrop-filter: blur(2px); z-index: 5;
}
.audiobook-card:hover .cover-hover-mask { opacity: 1; }

.play-trigger {
  width: 44px; height: 44px; border-radius: 50%; background: rgba(255,255,255,0.92); color: #e6a23c;
  display: flex; align-items: center; justify-content: center; font-size: 18px; padding-left: 3px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
}

/* ==================== 文字居中排版 ==================== */
.card-info {
  padding: 12px 10px 16px; display: flex; flex-direction: column; gap: 5px; flex: 1;
}

.book-name {
  font-family: Georgia, 'Times New Roman', serif;
  font-weight: bold; font-size: 15px; color: #2C221E;
  text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.dark-mode .book-name { color: #E5E7EB; }

.book-author { font-size: 12px; color: #7A6B63; text-align: center; }
.dark-mode .book-author { color: #9CA3AF; }

.card-footer { margin-top: 6px; display: flex; justify-content: center; }
.chapter-tag { font-weight: bold; border: none; }

/* ==================== 底部分页 ==================== */
.pagination-bar { padding: 12px; display: flex; justify-content: center; }

.shelf-skeleton-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(185px, 1fr)); gap: 32px;
}

@media (max-width: 768px) {
  .book-grid { grid-template-columns: repeat(2, 1fr); gap: 36px 16px; }
}
</style>