<!-- AdminPanel.vue -->
<script setup>
import { ref, onMounted, provide, inject } from 'vue'
import request from '../utils/request'
import AdminComments from './AdminComments.vue'
import AdminBottles from './AdminBottles.vue'
import AdminUsers from './AdminUsers.vue'
import AdminBooks from './AdminBooks.vue'

const isDark = inject('isDark', ref(false))
const activeTab = ref('bookComments')

// 实时待审核数量统计
const pendingCounts = ref({
  bookComments: 0,
  paragraphComments: 0,
  userComments: 0,
  bottles: 0
})

const fetchPendingCounts = async () => {
  try {
    const res = await request.get('/admin/pending-counts')
    if (res.code === 200 && res.data) {
      pendingCounts.value = res.data
    }
  } catch (err) {
    console.error('获取待办统计失败:', err)
  }
}

// 注入给子业务组件：当某条评论/漂流瓶审核通过后，通知父容器自动扣减顶部 badge 数字
provide('refreshCounts', fetchPendingCounts)

onMounted(() => {
  fetchPendingCounts()
})
</script>

<template>
  <div class="admin-panel-container" style="background-color: transparent;">
    <div class="glass-panel stats-header" style="background: var(--glass-bg); backdrop-filter: var(--glass-blur); border: 1px solid var(--glass-border); box-shadow: var(--glass-shadow);">
      <div class="title-box">
        <span class="main-title" style="color: var(--el-text-color-primary);">⚙️ 系统管理后台</span>
        <ElButton size="small" icon="Refresh" circle @click="fetchPendingCounts" style="--el-button-bg-color: var(--glass-bg); --el-button-border-color: var(--glass-border); --el-button-text-color: var(--el-text-color-regular);"/>
      </div>

      <div class="counts-grid">
        <div class="stat-badge theme-blue" @click="activeTab = 'bookComments'">
          <div class="badge-glow"></div>
          <span class="label">📚 待审书评</span>
          <span class="num">{{ pendingCounts.bookComments }}</span>
        </div>

        <div class="stat-badge theme-emerald" @click="activeTab = 'paragraphComments'">
          <div class="badge-glow"></div>
          <span class="label">📝 待审段评</span>
          <span class="num">{{ pendingCounts.paragraphComments }}</span>
        </div>

        <div class="stat-badge theme-amber" @click="activeTab = 'userComments'">
          <div class="badge-glow"></div>
          <span class="label">💬 待审论坛</span>
          <span class="num">{{ pendingCounts.userComments }}</span>
        </div>

        <div class="stat-badge theme-purple" @click="activeTab = 'bottles'">
          <div class="badge-glow"></div>
          <span class="label">🍾 待审漂流瓶</span>
          <span class="num">{{ pendingCounts.bottles }}</span>
        </div>
      </div>
    </div>

    <div class="glass-panel main-workspace" style="background: var(--glass-bg); backdrop-filter: var(--glass-blur); border: 1px solid var(--glass-border); box-shadow: var(--glass-shadow);">
      <ElTabs v-model="activeTab" class="custom-tabs">
        <ElTabPane label="📚 书籍评论审核" name="bookComments">
          <AdminComments comment-type="book" />
        </ElTabPane>

        <ElTabPane label="📝 段落批注审核" name="paragraphComments">
          <AdminComments comment-type="paragraph" />
        </ElTabPane>

        <ElTabPane label="💬 论坛评论审核" name="userComments">
          <AdminComments comment-type="user" />
        </ElTabPane>

        <ElTabPane label="🍾 漂流瓶审核" name="bottles">
          <AdminBottles />
        </ElTabPane>

        <ElTabPane label="👥 用户管理" name="users">
          <AdminUsers />
        </ElTabPane>

        <ElTabPane label="📖 书籍管理" name="books">
          <AdminBooks />
        </ElTabPane>
      </ElTabs>
    </div>
  </div>
</template>

<style scoped>
.admin-panel-container {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
}

.glass-panel {
  border-radius: 16px;
  padding: 20px;
}

.stats-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.title-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.main-title {
  font-size: 20px;
  font-weight: 800;
}

.counts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

/* =========================================================
   ✨ 统计卡片群：光学棱镜 / UV树脂露珠终极形态
   ========================================================= */
.counts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-badge {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-radius: 16px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid var(--b-border);
  background: var(--b-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 4大语义色彩池 (磨砂预设：极淡极雅背板 + 28%通透细边框) */
.theme-blue    { --c-main: #3b82f6; --b-bg: rgba(59, 130, 246, 0.08);  --b-border: rgba(59, 130, 246, 0.28); }
.theme-emerald { --c-main: #10b981; --b-bg: rgba(16, 185, 129, 0.08);  --b-border: rgba(16, 185, 129, 0.28); }
.theme-amber   { --c-main: #f59e0b; --b-bg: rgba(245, 158, 11, 0.08);  --b-border: rgba(245, 158, 11, 0.28); }
.theme-purple  { --c-main: #8b5cf6; --b-bg: rgba(139, 92, 246, 0.08); --b-border: rgba(139, 92, 246, 0.28); }

/* 暗黑模式下，文字色调自动转为高反差的霓虹发光色 */
html.dark .theme-blue    { --c-main: #60a5fa; --b-bg: rgba(96, 165, 250, 0.06); }
html.dark .theme-emerald { --c-main: #34d399; --b-bg: rgba(52, 211, 153, 0.06); }
html.dark .theme-amber   { --c-main: #fbbf24; --b-bg: rgba(251, 191, 36, 0.06); }
html.dark .theme-purple  { --c-main: #a78bfa; --b-bg: rgba(167, 139, 250, 0.06); }

.stat-badge .label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  z-index: 2;
}

.stat-badge .num {
  font-size: 28px;
  font-weight: 900;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  color: var(--c-main);
  text-shadow: 0 0 16px var(--b-border);
  z-index: 2;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 悬停动效：瓷砖轻盈弹起，背后的彩色光雾瞬间晕开 */
.stat-badge:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: var(--c-main);
  box-shadow: 0 10px 25px -5px var(--b-bg);
}
.stat-badge:hover .num { transform: scale(1.15); }

/* 藏在卡片右下角的一团彩色呼吸柔光 */
.badge-glow {
  position: absolute;
  right: -15px;
  bottom: -15px;
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: var(--c-main);
  filter: blur(32px);
  opacity: 0.22;
  pointer-events: none;
  transition: all 0.4s ease;
}
.stat-badge:hover .badge-glow {
  opacity: 0.55;
  transform: scale(1.7);
}

/* =========================================================
   💧 液态水晶预设适配：秒变 4 滴挂在页面的立式彩色树脂标本！
   ========================================================= */
html.preset-liquid .stat-badge {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0.08) 100%);
  border: none;
  box-shadow:
      inset 0 1px 1px rgba(255, 255, 255, 0.85), /* 顶部高光折射水线 */
      inset 0 -2px 4px rgba(0, 0, 0, 0.05),
      0 8px 20px -6px var(--c-main); /* 投射到桌面的彩色环境光 */
}

html.dark.preset-liquid .stat-badge {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12) 0%, rgba(0, 0, 0, 0.2) 100%);
  box-shadow:
      inset 0 1px 1px rgba(255, 255, 255, 0.22),
      inset 0 -2px 4px rgba(0, 0, 0, 0.5),
      0 8px 25px -8px var(--c-main);
}

.main-workspace {
  flex: 1;
}

:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}
:deep(.el-tabs__active-bar) {
  background-color: var(--el-color-primary);
}
:deep(.el-tabs__nav-wrap::after) {
  background-color: var(--glass-border);
}

</style>