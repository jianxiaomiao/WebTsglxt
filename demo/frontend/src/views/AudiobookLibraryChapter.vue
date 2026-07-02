<template>
  <div class="audiobook-chapters-wrap">
    <ElSkeleton v-if="loading" :rows="6" animated />

    <ElEmpty v-else-if="chapterList.length === 0" description="本书暂无AI已通读章节" />

    <div v-else class="chapter-list-scroll">
      <div
          v-for="item in chapterList"
          :key="item.chapterNumber"
          class="chapter-row-item"
          @click="handleOpenPlayer(item)"
      >
        <div class="row-left">
          <span class="num-badge">第{{ item.chapterNumber }}章</span>
          <span class="name-text">{{ item.chapterName || '未命名章节' }}</span>
        </div>

        <div class="row-right">
          <span class="seg-count-badge">🎙️ {{ item.segmentCount }}段对话</span>

          <ElButton size="small" type="primary" plain round class="listen-btn">
            ▶️ 去听书
          </ElButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const props = defineProps({
  isbn: { type: String, required: true },
  bookName: { type: String, default: '' }
})

const router = useRouter()
const chapterList = ref([])
const loading = ref(false)

const formatDuration = (sec) => {
  if (!sec || isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = Math.floor(sec % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const emit = defineEmits(['play'])

const fetchChapters = async () => {
  loading.value = true
  try {
    const res = await request.get('/audiobook/chapters', { params: { isbn: props.isbn } })
    if (res.code === 200) chapterList.value = res.data || []
  } finally { loading.value = false }
}

// 🔥 点击章节 → 通知父组件打开 AudiobookPlayer
const handleOpenPlayer = (item) => {
  emit('play', {
    isbn: props.isbn,
    chapterNumber: item.chapterNumber,
    bookName: props.bookName
  })
}

onMounted(() => fetchChapters())
</script>

<style scoped>
.audiobook-chapters-wrap { max-height: 480px; overflow-y: auto; padding: 4px 8px; }
.chapter-list-scroll { display: flex; flex-direction: column; gap: 8px; }

.chapter-row-item {
  padding: 12px 16px; border-radius: 10px; background: rgba(0,0,0,0.03); border: 1px solid transparent;
  display: flex; justify-content: space-between; align-items: center; cursor: pointer; transition: all 0.2s;
}
:deep(.dark-mode) .chapter-row-item { background: rgba(255,255,255,0.05); }

.chapter-row-item:hover { background: rgba(230, 162, 60, 0.1); border-color: rgba(230, 162, 60, 0.3); transform: translateX(4px); }

.row-left { display: flex; align-items: center; gap: 10px; overflow: hidden; }
.num-badge { font-weight: bold; font-size: 13px; color: #e6a23c; flex-shrink: 0; }
.name-text { font-size: 14px; color: var(--el-text-color-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.row-right { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.status-badge { font-size: 12px; }
.status-badge.ready { color: #67c23a; font-weight: 500; }
.status-badge.gen { color: #e6a23c; animation: pulse 1.5s infinite; }
.status-badge.none { color: #909399; }
</style>