<template>
  <div class="gallery-page-container">
    <div class="gallery-header glass-panel">
      <el-button link @click="router.back()" :icon="ArrowLeft" class="back-btn">
        返回我的
      </el-button>
      <h2 class="gallery-title">
        <span class="title-icon">{{ isDark ? '🌌' : '🖼️' }}</span>
        阅读时光画廊
      </h2>
      <div class="header-stats">
        <span>已点亮 <strong>{{ readHistory.length }}</strong> 本记忆</span>
      </div>
    </div>

    <el-row :gutter="20" class="gallery-main-row">
      <el-col :span="24">
        <div class="gallery-swiper-section glass-panel">
          <div class="section-title">✨ 记忆回廊</div>
          <Swiper
              v-if="readHistory.length > 0"
              :effect="'coverflow'"
              :grabCursor="true"
              :centeredSlides="true"
              :slidesPerView="'auto'"
              :coverflowEffect="{
                rotate: 40,
                stretch: 0,
                depth: 150,
                modifier: 1,
                slideShadows: true,
              }"
              :autoplay="{ delay: 2500, disableOnInteraction: false }"
              :pagination="{ clickable: true }"
              :modules="swiperModules"
              class="my-swiper"
          >
            <SwiperSlide v-for="book in readHistory" :key="book.id" class="swiper-slide-custom">
              <div class="gallery-book-card" @click="goToReader(book.isbn)">
                <img :src="book.pictureName || '/default-book.png'" alt="book cover" class="gallery-book-img" />
                <div class="gallery-book-overlay">
                  <h3 class="overlay-title">{{ book.bookName }}</h3>
                  <p class="overlay-author">{{ book.author || '未知作者' }}</p>
                  <el-button type="primary" size="small" round class="read-btn">再次翻阅</el-button>
                </div>
              </div>
            </SwiperSlide>
          </Swiper>
          <el-empty v-else description="画廊空空如也，快去阅读吧~" />
        </div>
      </el-col>

      <el-col :xs="24" :lg="16">
        <div class="gallery-echarts-section glass-panel">
          <div class="section-title">🌌 知识认知星图</div>
          <div class="echarts-desc">每一次阅读，都在你的认知宇宙中点亮一颗星星，建立新的联结。</div>
          <div ref="chartRef" class="echarts-container"></div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="8">
        <div class="gallery-timeline-section glass-panel">
          <div class="section-title" style="flex-shrink: 0; margin-bottom: 10px;">📜 岁月史书</div>

          <div class="timeline-scroll-wrap custom-scroll-y">
            <el-timeline style="margin-top: 10px; padding-left: 2px;">
              <el-timeline-item
                  v-for="(activity, index) in timelineData"
                  :key="index"
                  :type="activity.type"
                  :color="activity.color"
                  :size="activity.size"
                  placement="top"
              >
                <el-card class="timeline-card" :class="{'dark-card': isDark}" @click="goToReader(activity.isbn)">
                  <div class="timeline-content">
                    <img :src="activity.cover || '/default-book.png'" class="timeline-cover" />
                    <div class="timeline-info">
                      <h4 class="timeline-book-title">{{ activity.title }}</h4>
                      <p class="timeline-desc">阅读至 第 {{ activity.chapter }} 章</p>
                    </div>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>

        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, inject, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

// 引入 Swiper
import { Swiper, SwiperSlide } from 'swiper/vue'
import { EffectCoverflow, Pagination, Autoplay } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/effect-coverflow'
import 'swiper/css/pagination'

// 引入 ECharts
import * as echarts from 'echarts'

const router = useRouter()
const currentUserId = inject('currentUserId')
const isDark = inject('isDark')
const goToBookReader = inject('goToBookReader')

// 全局注入阅读历史状态与拉取方法（替换原有手动请求接口）
const userReadHistory = inject('userReadHistory')
const fetchUserReadHistory = inject('fetchUserReadHistory')

const swiperModules = [EffectCoverflow, Pagination, Autoplay]
const chartRef = ref(null)
let myChart = null

// 绑定全局阅读历史数据
const readHistory = computed(() => userReadHistory.value || [])

// 跳转书籍阅读页
const goToReader = (isbn) => {
  if (isbn) goToBookReader(isbn)
}

// 解析pageNum字段：格式 978xxxx-6 提取章节数字
const getChapterNum = (pageStr) => {
  if (!pageStr || !pageStr.includes('-')) return '1'
  const splitArr = pageStr.split('-')
  return splitArr[1] || '1'
}

// 组装时间轴数据
const timelineData = computed(() => {
  return readHistory.value.map((book, index) => ({
    title: book.bookName,
    date: book.lastReadTime || '最近阅读',
    chapter: getChapterNum(book.pageNum),
    cover: book.pictureName,
    isbn: book.isbn,
    type: index === 0 ? 'primary' : 'info',
    color: index === 0 ? '#40c463' : '',
    size: index === 0 ? 'large' : 'normal'
  }))
})

// ====================== 🌌 ECharts 知识星图核心引擎 ======================
const initKnowledgeGraph = () => {
  if (!chartRef.value || readHistory.value.length === 0) return
  if (myChart) myChart.dispose()

  myChart = echarts.init(chartRef.value)
  // 存储resize监听函数，卸载时销毁
  const resizeHandler = () => myChart && myChart.resize()
  myChart._resizeHandler = resizeHandler
  window.addEventListener('resize', resizeHandler)

  // 1. 数据转换：节点 + 关联连线
  const nodes = []
  const links = []
  const categorySet = new Set()
  const authorSet = new Set()

  readHistory.value.forEach(book => {
    // 后端暂未返回书籍分类，统一兜底为未分类
    const cat = book.typeName || '未分类'
    const auth = book.author || '未知作者'
    categorySet.add(cat)
    authorSet.add(auth)

    // 书籍节点
    nodes.push({
      id: book.bookName,
      name: book.bookName,
      category: 2,
      symbolSize: 30,
      itemStyle: { color: isDark.value ? '#60a5fa' : '#409eff' }
    })
    // 书籍-作者连线
    links.push({ source: book.bookName, target: auth })
    // 书籍-分类连线
    links.push({ source: book.bookName, target: cat })
  })

  // 分类星系中心节点
  categorySet.forEach(cat => {
    nodes.push({
      id: cat,
      name: cat,
      category: 0,
      symbolSize: 60,
      itemStyle: {
        color: isDark.value ? '#d4af37' : '#e6a23c',
        shadowBlur: 20,
        shadowColor: 'rgba(230,162,60,0.5)'
      }
    })
  })

  // 作者星球节点
  authorSet.forEach(auth => {
    nodes.push({
      id: auth,
      name: auth,
      category: 1,
      symbolSize: 45,
      itemStyle: { color: isDark.value ? '#34d399' : '#67c23a' }
    })
  })

  // 图表配置
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}' },
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        force: {
          repulsion: 400,
          edgeLength: [80, 150],
          gravity: 0.1
        },
        roam: true,
        label: { show: true, position: 'right', color: isDark.value ? '#e5e7eb' : '#333' },
        lineStyle: {
          color: isDark.value ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)',
          curveness: 0.2,
          width: 2
        },
        data: nodes,
        links: links,
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 4 }
        }
      }
    ]
  }

  myChart.setOption(option)
}

// 监听暗黑模式切换，重绘星图配色
watch(isDark, () => {
  if (myChart) initKnowledgeGraph()
})

onMounted(async () => {
  // 拉取当前用户阅读历史
  await fetchUserReadHistory(currentUserId.value)
  // 等待DOM渲染完成再初始化图表
  await nextTick()
  initKnowledgeGraph()
})

onUnmounted(() => {
  // 销毁图表 + 移除resize监听，防止内存泄漏
  if (myChart) {
    const resizeFn = myChart._resizeHandler
    if (resizeFn) window.removeEventListener('resize', resizeFn)
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
/* ======================================
   1. 基础布局与画廊质感
======================================== */
.gallery-page-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 100px);
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  margin-bottom: 24px;
  border-radius: 12px;
}

.back-btn { font-size: 15px; font-weight: bold; }
.gallery-title { margin: 0; font-size: 22px; display: flex; align-items: center; gap: 8px; color: var(--el-text-color-primary); }
.header-stats { color: var(--el-text-color-secondary); font-size: 14px; }
.header-stats strong { color: var(--el-color-primary); font-size: 18px; margin: 0 4px; }

.gallery-main-row {
  display: flex;
  flex-wrap: wrap;
}

/* 通用毛玻璃面板 */
.glass-panel {
  background: var(--glass-bg) !important;
  backdrop-filter: blur(20px) saturate(120%);
  -webkit-backdrop-filter: blur(20px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
  border-radius: 16px;
  margin-bottom: 20px;
  padding: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
}

/* ======================================
   2. Swiper 3D 封面流样式
======================================== */
.gallery-swiper-section {
  height: 400px;
  overflow: hidden;
}

.my-swiper {
  width: 100%;
  padding-top: 20px;
  padding-bottom: 40px;
}

.swiper-slide-custom {
  background-position: center;
  background-size: cover;
  width: 200px; /* 调整封面宽度 */
  height: 280px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 15px 35px rgba(0,0,0,0.2);
}

.gallery-book-card {
  position: relative;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.gallery-book-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 鼠标悬停时的黑色半透明遮罩与按钮 */
.gallery-book-overlay {
  position: absolute;
  bottom: -100%;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.4) 50%, transparent 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 20px;
  transition: bottom 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-sizing: border-box;
}

.gallery-book-card:hover .gallery-book-overlay {
  bottom: 0;
}

.overlay-title { color: #fff; margin: 0 0 6px 0; font-size: 16px; font-weight: bold; }
.overlay-author { color: #ccc; margin: 0 0 16px 0; font-size: 12px; }
.read-btn { width: 100%; }

/* ======================================
   3. ECharts 知识星图样式
======================================== */
.gallery-echarts-section {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.echarts-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 10px;
}

.echarts-container {
  flex: 1;
  width: 100%;
  border-radius: 8px;
  background: radial-gradient(circle at center, rgba(255,255,255,0.1) 0%, transparent 70%);
}
/* 暗黑模式下给星图加一个深邃的星空底色 */
html.dark .echarts-container {
  background: radial-gradient(circle at center, rgba(64,158,255,0.05) 0%, transparent 80%);
}

/* ======================================
   4. 岁月史书 (时间轴) 样式
======================================== */
.gallery-timeline-section {
  height: 500px;
  /* 新增：垂直flex布局，分配标题与滚动区域高度 */
  display: flex;
  flex-direction: column;
}

.custom-scroll-y {
  overflow-y: auto;
  overflow-x: hidden;
  /* 关键1：自动占满父容器除去标题后的剩余高度 */
  flex: 1;
  /* 关键2：修复flex子元素被内容撑开、无法收缩的bug */
  min-height: 0;
}
.custom-scroll-y::-webkit-scrollbar { width: 6px; }
.custom-scroll-y::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}
.custom-scroll-y::-webkit-scrollbar-thumb {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 4px;
}
.custom-scroll-y::-webkit-scrollbar-thumb:hover {
  background: rgba(144, 147, 153, 0.5);
}

.timeline-card {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}
.timeline-card:hover {
  transform: translateX(5px);
  border-color: var(--el-color-primary);
  background: rgba(255, 255, 255, 0.8);
}
.dark-card { background: rgba(0, 0, 0, 0.2); border-color: rgba(255, 255, 255, 0.1); }
.dark-card:hover { background: rgba(0, 0, 0, 0.4); }

.timeline-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.timeline-cover {
  width: 45px;
  height: 60px;
  border-radius: 4px;
  object-fit: cover;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.timeline-book-title { margin: 0 0 6px 0; font-size: 14px; color: var(--el-text-color-primary); }
.timeline-desc { margin: 0; font-size: 12px; color: var(--el-text-color-secondary); }

/* ======================================
   5. 📱 移动端专属响应式适配
======================================== */
@media (max-width: 768px) {
  /* 1. 整体容器边距极限收缩，留出更多屏幕空间 */
  .gallery-page-container {
    padding: 10px;
    min-height: calc(100vh - 60px);
  }

  /* 2. 头部信息在小屏幕改为上下堆叠排版 */
  .gallery-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 12px 16px;
    margin-bottom: 15px;
    gap: 8px;
  }
  .gallery-title {
    font-size: 18px;
  }
  .header-stats {
    align-self: flex-end; /* 统计信息靠右下对齐，增加层次感 */
    font-size: 12px;
  }
  .header-stats strong {
    font-size: 16px;
  }

  /* 3. 全局毛玻璃卡片内边距收缩 */
  .glass-panel {
    padding: 15px;
    margin-bottom: 15px;
    border-radius: 12px;
  }
  .section-title {
    font-size: 16px;
    margin-bottom: 12px;
  }

  /* 4. 画廊 3D 轮播图等比例缩小，防止超出屏幕高度 */
  .gallery-swiper-section {
    height: 320px; /* 从 400px 压缩 */
  }
  .my-swiper {
    padding-top: 10px;
    padding-bottom: 25px;
  }
  .swiper-slide-custom {
    width: 130px;  /* 封面宽度变窄 */
    height: 185px; /* 封面高度按比例压缩 */
    border-radius: 8px;
  }
  /* 悬停遮罩内文字同步缩小 */
  .gallery-book-overlay {
    padding: 12px;
  }
  .overlay-title { font-size: 13px; margin-bottom: 4px; }
  .overlay-author { font-size: 11px; margin-bottom: 10px; }
  .read-btn { transform: scale(0.9); transform-origin: left bottom; }

  /* 5. ECharts 知识星图高度压缩，节省首屏纵向空间 */
  .gallery-echarts-section {
    height: 350px; /* 从 500px 压缩 */
  }
  .echarts-desc {
    font-size: 12px;
    line-height: 1.4;
  }

  /* 6. 岁月史书时间轴高度适配 */
  .gallery-timeline-section {
    height: 380px; /* 从 500px 压缩 */
  }
  .timeline-cover {
    width: 36px;
    height: 48px;
  }
  .timeline-book-title {
    font-size: 13px;
    margin-bottom: 4px;
  }
  .timeline-desc {
    font-size: 11px;
  }
}
</style>