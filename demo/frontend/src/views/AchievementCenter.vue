<template>
  <div class="achievement-container glass-panel">
    <header class="overview-header">
      <div class="level-info">
        <h2>Lv.{{ store.userLevel }} 阅读大师</h2>
        <el-progress
            :percentage="store.expProgress"
            :format="() => `${store.currentExp} / ${store.nextLevelExp} EXP`"
            :color="customColors"
        />
      </div>
      <div class="stats-info">
        <div class="stat-box">
          <span class="label">已解锁</span>
          <span class="value">{{ store.unlockedCount }} / {{ store.totalBadges }}</span>
        </div>
      </div>
    </header>

    <el-divider border-style="dashed" />

    <div class="badge-grid">
      <BadgeItem
          v-for="badge in store.badges"
          :key="badge.id"
          :badge="badge"
          @click="showBadgeDetail(badge)"
      />
    </div>

    <el-dialog v-model="detailVisible" :title="currentBadge.name" width="30%" class="glass-dialog">
      <div class="badge-detail-content">
        <img :src="currentBadge.icon" :class="['detail-icon', `rarity-${currentBadge.rarity}`]" />
        <p class="desc">{{ currentBadge.description }}</p>
        <p class="status" :class="{ 'is-locked': !currentBadge.isUnlocked }">
          {{ currentBadge.isUnlocked ? `解锁于: ${currentBadge.unlockDate}` : '未解锁' }}
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from 'vue'
import { useAchievementStore } from '../stores/achievementStore'
import { useUserStore } from '../stores/userStore'
import BadgeItem from '../components/BadgeItem.vue'

const store = useAchievementStore()
const userStore = useUserStore()
const detailVisible = ref(false)
const currentBadge = ref({})

// 响应式进度条颜色：暗黑模式用统一主题色，亮色用阶段渐变色
const customColors = computed(() => {
  return userStore.isDark
      ? [{ color: '#409EFF', percentage: 100 }] // Element 默认蓝
      : [
        { color: '#f56c6c', percentage: 20 },
        { color: '#e6a23c', percentage: 40 },
        { color: '#5cb87a', percentage: 60 },
        { color: '#6f7ad3', percentage: 100 }
      ]
})

const showBadgeDetail = (badge) => {
  currentBadge.value = badge
  detailVisible.value = true
}

onMounted(() => {
  store.fetchAchievements(userStore.userId)
})
</script>

<style scoped>
.glass-panel {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 24px;
}
.badge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 20px;
}
/* 其他基础样式省略，契合主框架 */
</style>