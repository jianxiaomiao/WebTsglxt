<template>
  <div class="badge-gallery-wrapper">
    <div class="medal-dashboard">
      <div v-for="(count, rarity) in rarityStats" :key="rarity" :class="['stat-medal-card', `card-${rarity}`]">
        <div class="medal-shine"></div>
        <span class="medal-emoji">{{ getRarityEmoji(rarity) }}</span>
        <div class="medal-meta">
          <span class="medal-count">{{ count.unlocked }}<span class="total-slash">/{{ count.total }}</span></span>
          <span class="medal-label">{{ getRarityLabel(rarity) }}</span>
        </div>
      </div>
    </div>

    <div class="gallery-controls glass-panel">
      <div class="control-left">
        <el-radio-group v-model="filterStatus" size="default" class="custom-radio-group">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unlocked">已解锁</el-radio-button>
          <el-radio-button value="locked">未解锁</el-radio-button>
        </el-radio-group>

        <el-select v-model="filterCategory" placeholder="徽章分类" style="width: 130px" class="custom-select">
          <el-option label="所有分类" value="all" />
          <el-option label="📖 阅读里程" value="reading" />
          <el-option label="⏱️ 阅读时长" value="duration" />
          <el-option label="💬 社交互动" value="social" />
          <el-option label="📦 书架收藏" value="collection" />
          <el-option label="🦉 隐藏成就" value="hidden" />
        </el-select>
      </div>

      <div class="control-right">
        <span class="sort-label">排序方式</span>
        <el-select v-model="sortBy" style="width: 140px" class="custom-select">
          <el-option label="⚡ 稀有度从高到低" value="rarityDesc" />
          <el-option label="📅 最近解锁优先" value="dateDesc" />
          <el-option label="🎯 默认系统排序" value="default" />
        </el-select>
      </div>
    </div>

    <div class="badges-wall-container">
      <div v-if="filteredBadges.length === 0" class="empty-gallery glass-panel">
        🔮 虚位以待，快去阅读或探索社区解锁它们吧！
      </div>

      <TransitionGroup name="fluid-grid" tag="div" class="badges-grid">
        <BadgeItem
            v-for="badge in filteredBadges"
            :key="badge.id"
            :badge="badge"
            @click="handleBadgeClick(badge)"
        />
      </TransitionGroup>
    </div>

    <el-dialog
        v-model="detailVisible"
        :title="selectedBadge.isUnlocked ? '🏆 璀璨成就详情' : '🔒 未解锁成就'"
        width="360px"
        align-center
        class="immersive-detail-dialog"
    >
      <div class="immersive-detail-card" :class="[`detail-rarity-${selectedBadge.rarity}`, { 'is-detail-locked': !selectedBadge.isUnlocked }]">
        <div class="holo-glow"></div>
        <div class="detail-icon-halo">
          <span class="detail-emoji-large">{{ getRarityEmoji(selectedBadge.rarity) }}</span>
        </div>
        <h2 class="detail-title-glow">{{ selectedBadge.name }}</h2>

        <div class="detail-rarity-badge">
          <span class="rarity-dot"></span>
          {{ getRarityLabel(selectedBadge.rarity) }}
        </div>

        <p class="detail-description">“ {{ selectedBadge.description }} ”</p>

        <div class="detail-footer-meta">
          <div v-if="selectedBadge.isUnlocked" class="unlocked-time-stamp">
            📅 解锁于：{{ selectedBadge.unlockDate }}
          </div>
          <div v-else class="locked-hint-stamp">
            🧩 前往对应模块完成挑战即可点亮
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAchievementStore } from '../stores/achievementStore'
import { useUserStore } from '../stores/userStore'
import BadgeItem from '../components/BadgeItem.vue' // 上一步我们补齐的组件

const store = useAchievementStore()
const userStore = useUserStore()

// 筛选控制状态
const filterStatus = ref('all') // all / unlocked / locked
const filterCategory = ref('all') // all / reading / duration / social / collection / hidden
const sortBy = ref('rarityDesc') // rarityDesc / dateDesc / default

// 弹窗状态
const detailVisible = ref(false)
const selectedBadge = ref({})

// 稀有度权重配置（用于排序）
const rarityWeights = { legendary: 4, gold: 3, silver: 2, bronze: 1 }

// 1. 核心大招：极其丝滑的多维组合筛选与排序引擎
const filteredBadges = computed(() => {
  let result = [...store.badges]

  // 维度一：按解锁状态筛选
  if (filterStatus.value === 'unlocked') result = result.filter(b => b.isUnlocked)
  if (filterStatus.value === 'locked') result = result.filter(b => !b.isUnlocked)

  // 维度二：按徽章分类筛选
  if (filterCategory.value !== 'all') result = result.filter(b => b.category === filterCategory.value)

  // 维度三：动态物理排序
  if (sortBy.value === 'rarityDesc') {
    result.sort((a, b) => (rarityWeights[b.rarity] || 0) - (rarityWeights[a.rarity] || 0))
  } else if (sortBy.value === 'dateDesc') {
    result.sort((a, b) => {
      if (!a.isUnlocked) return 1
      if (!b.isUnlocked) return -1
      return new Date(b.unlockDate).getTime() - new Date(a.unlockDate).getTime()
    })
  }

  return result
})

// 2. 统计看板计算属性
const rarityStats = computed(() => {
  const stats = {
    legendary: { total: 0, unlocked: 0 },
    gold: { total: 0, unlocked: 0 },
    silver: { total: 0, unlocked: 0 },
    bronze: { total: 0, unlocked: 0 }
  }
  store.badges.forEach(b => {
    if (stats[b.rarity]) {
      stats[b.rarity].total++
      if (b.isUnlocked) stats[b.rarity].unlocked++
    }
  })
  return stats
})

// 基础辅助函数
const getRarityEmoji = (rarity) => {
  const map = { bronze: '🥉', silver: '🥈', gold: '🥇', legendary: '👑' }
  return map[rarity] || '🏵️'
}

const getRarityLabel = (rarity) => {
  const map = { bronze: '普通成就', silver: '稀有荣誉', gold: '史诗圣殿', legendary: '不朽传说' }
  return map[rarity] || '未知稀有度'
}

const handleBadgeClick = (badge) => {
  selectedBadge.value = badge
  detailVisible.value = true
}

onMounted(() => {
  if (userStore.userId) {
    store.fetchAchievements(userStore.userId)
  }
})
</script>

<style scoped>
.badge-gallery-wrapper {
  width: 100%;
  padding: 12px 4px;
}

/* ====================== 1. 荣誉奖座仪表盘 ====================== */
.medal-dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-medal-card {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.medal-emoji { font-size: 32px; }
.medal-meta { display: flex; flex-direction: column; }
.medal-count { font-size: 22px; font-weight: 700; font-family: 'Courier New', Courier, monospace; color: var(--el-text-color-primary); }
.total-slash { font-size: 13px; opacity: 0.5; font-weight: 400; }
.medal-label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }

/* 各稀有度专属微光色盘 */
.card-bronze { border-left: 4px solid #cd7f32; }
.card-silver { border-left: 4px solid #c0c0c0; }
.card-gold { border-left: 4px solid #ffd700; }
.card-legendary {
  border-left: 4px solid #e040fb;
  background: linear-gradient(135deg, rgba(224, 64, 251, 0.05) 0%, rgba(255, 255, 255, 0.05) 100%);
}

/* ====================== 2. 中控控制栏 ====================== */
.gallery-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
}

.control-left { display: flex; align-items: center; gap: 16px; }
.control-right { display: flex; align-items: center; gap: 8px; }
.sort-label { font-size: 13px; color: var(--el-text-color-secondary); }

/* ====================== 3. 网格与无缝流体动画 ====================== */
.badges-wall-container { min-height: 300px; position: relative; }
.badges-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 16px;
}
.empty-gallery {
  text-align: center; padding: 60px; color: var(--el-text-color-secondary); font-size: 14px;
}

/* 🔥 Vue FLIP 流体过渡过渡动画引擎：让网格过滤时像水流一样顺滑移动 */
.fluid-grid-enter-active,
.fluid-grid-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.fluid-grid-enter-from,
.fluid-grid-leave-to {
  opacity: 0;
  transform: scale(0.8) translateY(20px);
}
.fluid-grid-move {
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.fluid-grid-leave-active {
  position: absolute; /* 绝对核心：离开时脱离文档流，保证其他卡片能平滑移过来 */
  width: 130px; /* 保持与网格最小跨度一致防止形变 */
}

/* ====================== 4. 沉浸式巨幕详情卡片 ====================== */
.immersive-detail-card {
  position: relative;
  text-align: center;
  padding: 24px 16px;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.detail-icon-halo {
  width: 90px; height: 90px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 30px rgba(255,255,255,0.05);
}
.detail-emoji-large { font-size: 52px; }
.detail-title-glow { font-size: 20px; margin: 0 0 10px; font-weight: 700; color: var(--el-text-color-primary); }

.detail-rarity-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 20px; font-size: 12px;
  background: rgba(255,255,255,0.08); color: var(--el-text-color-regular);
  margin-bottom: 20px;
}
.rarity-dot { width: 6px; height: 6px; border-radius: 50%; background: #409eff; }

.detail-description {
  font-size: 14px; line-height: 1.6; color: var(--el-text-color-regular);
  margin: 0 0 24px; padding: 0 10px; font-style: italic;
}
.detail-footer-meta { font-size: 12px; color: var(--el-text-color-secondary); }

/* 巨幕弹窗微光氛围渗出效果 */
.detail-rarity-gold .detail-icon-halo { box-shadow: 0 0 40px rgba(255, 215, 0, 0.3); }
.detail-rarity-gold .rarity-dot { background: #ffd700; }
.detail-rarity-gold .detail-title-glow { text-shadow: 0 0 10px rgba(255, 215, 0, 0.4); }

.detail-rarity-legendary .detail-icon-halo { box-shadow: 0 0 40px rgba(224, 64, 251, 0.4); }
.detail-rarity-legendary .rarity-dot { background: #e040fb; }
.detail-rarity-legendary .detail-title-glow { animation: text-chromatic 3s infinite alternate; }

.is-detail-locked { filter: grayscale(100%); opacity: 0.7; }

@keyframes text-chromatic {
  0% { text-shadow: 0 0 8px #ff0000; }
  50% { text-shadow: 0 0 8px #00ff00; }
  100% { text-shadow: 0 0 8px #0000ff; }
}

/* ====================== 📱 移动端自适应折叠布局 ====================== */
@media (max-width: 768px) {
  .medal-dashboard {
    grid-template-columns: repeat(2, 1fr); /* 手机端排版为 2x2 便当盒 */
    gap: 10px;
  }
  .gallery-controls {
    flex-direction: column; /* 手机端中控台上下解耦 */
    align-items: stretch;
    gap: 12px;
    padding: 12px;
  }
  .control-left { justify-content: space-between; }
  .control-right { justify-content: flex-end; }
  .badges-grid {
    grid-template-columns: repeat(auto-fill, minmax(105px, 1fr)); /* 手机端紧凑网格 */
    gap: 10px;
  }
  .fluid-grid-leave-active { width: 105px; } /* 维持手机跨度 */
}
</style>