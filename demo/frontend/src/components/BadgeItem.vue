<template>
  <div
      class="badge-item glass-card"
      :class="{ 'is-locked': !badge.isUnlocked, [`rarity-${badge.rarity}`]: badge.isUnlocked }"
      @click="$emit('click', badge)"
  >
    <div class="badge-icon-wrapper">
      <span class="badge-icon">{{ getRarityEmoji(badge.rarity) }}</span>
      <div v-if="!badge.isUnlocked" class="lock-overlay">
        <el-icon><Lock /></el-icon>
      </div>
    </div>
    <div class="badge-info">
      <h4 class="badge-name">{{ badge.name }}</h4>
      <p class="badge-date" v-if="badge.isUnlocked">{{ formatDate(badge.unlockDate) }}</p>
      <p class="badge-date locked-text" v-else>未解锁</p>
    </div>
  </div>
</template>

<script setup>
import { Lock } from '@element-plus/icons-vue'

defineProps({
  badge: { type: Object, required: true }
})

defineEmits(['click'])

// 动态 Emoji 占位
const getRarityEmoji = (rarity) => {
  const map = { bronze: '🥉', silver: '🥈', gold: '🥇', legendary: '👑' }
  return map[rarity] || '🏵️'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.split(' ')[0] // 仅保留 YYYY-MM-DD
}
</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
  overflow: hidden;
}

/* 悬浮动效 */
.glass-card:not(.is-locked):hover {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

/* 锁定状态 */
.is-locked {
  filter: grayscale(100%);
  opacity: 0.6;
}
.lock-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(0, 0, 0, 0.4);
  font-size: 24px;
  color: #fff;
  border-radius: 50%;
}

.badge-icon-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 36px;
  position: relative;
  background: rgba(255, 255, 255, 0.1);
}

/* 稀有度边框与发光体系 */
.rarity-bronze { border-color: rgba(205, 127, 50, 0.5); }
.rarity-bronze .badge-icon-wrapper { box-shadow: 0 0 15px rgba(205, 127, 50, 0.3); }

.rarity-silver { border-color: rgba(192, 192, 192, 0.6); }
.rarity-silver .badge-icon-wrapper { box-shadow: 0 0 20px rgba(192, 192, 192, 0.4); }

.rarity-gold { border-color: rgba(255, 215, 0, 0.8); }
.rarity-gold .badge-icon-wrapper { box-shadow: 0 0 25px rgba(255, 215, 0, 0.6); }

/* 传说级炫彩呼吸灯 */
.rarity-legendary {
  animation: border-chromatic 3s infinite alternate;
}
.rarity-legendary .badge-icon-wrapper {
  animation: glow-chromatic 3s infinite alternate;
}

@keyframes border-chromatic {
  0% { border-color: rgba(255, 0, 0, 0.5); }
  33% { border-color: rgba(0, 255, 0, 0.5); }
  66% { border-color: rgba(0, 0, 255, 0.5); }
  100% { border-color: rgba(255, 0, 255, 0.5); }
}

.badge-name { font-size: 14px; margin: 0 0 4px; color: var(--el-text-color-primary); }
.badge-date { font-size: 12px; margin: 0; color: var(--el-text-color-secondary); }
</style>