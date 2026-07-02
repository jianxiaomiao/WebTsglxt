<template>
  <Transition name="bounce">
    <div v-if="store.showUnlockModal" class="unlock-overlay" @click="handleClose">
      <div class="unlock-card glass-panel" @click.stop>
        <h2 class="glow-text">🎉 成就解锁！</h2>
        <div class="badge-showcase">
          <span class="pop-up-emoji" :class="`rarity-${currentBadge.rarity}`">{{ getRarityEmoji(currentBadge.rarity) }}</span>
        </div>
        <h3 class="badge-name">{{ currentBadge.name }}</h3>
        <p class="badge-desc">{{ currentBadge.description }}</p>
        <el-button type="primary" round @click="handleClose">收下徽章</el-button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useAchievementStore } from '../stores/achievementStore'
import confetti from 'canvas-confetti'

const store = useAchievementStore()

const getRarityEmoji = (rarity) => {
  const map = { bronze: '🥉', silver: '🥈', gold: '🥇', legendary: '👑' }
  return map[rarity] || '🏵️'
}

const currentBadge = computed(() => {
  return store.unlockedBadgesQueue[0] || {}
})

// 监听弹窗显示，触发烟花
watch(() => store.showUnlockModal, (newVal) => {
  if (newVal) {
    fireConfetti()
  }
})

const handleClose = () => {
  store.closeUnlockModal()
  if (store.showUnlockModal) {
    // 如果队列里还有下一个徽章，再次触发烟花
    setTimeout(fireConfetti, 300)
  }
}

const fireConfetti = () => {
  const isLegendary = currentBadge.value.rarity === 'legendary'
  confetti({
    particleCount: isLegendary ? 200 : 100,
    spread: isLegendary ? 100 : 70,
    origin: { y: 0.6 },
    colors: isLegendary ? ['#FFD700', '#FF69B4', '#00FFFF'] : undefined
  })
}
</script>

<style scoped>
.unlock-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
}

.unlock-card {
  text-align: center;
  padding: 40px;
  transform-origin: center;
}

/* 从中心放大弹出 + 旋转 */
.bounce-enter-active {
  animation: bounce-in 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.bounce-leave-active {
  animation: bounce-in 0.3s reverse;
}

@keyframes bounce-in {
  0% { transform: scale(0) rotate(-15deg); opacity: 0; }
  100% { transform: scale(1) rotate(0deg); opacity: 1; }
}

.pop-up-emoji {
  font-size: 100px;
  line-height: 120px;
  display: block;
  width: 120px;
  height: 120px;
  margin: 0 auto;
  filter: drop-shadow(0 0 20px rgba(255, 215, 0, 0.6));
}

.rarity-legendary {
  animation: chromatic-glow 2s linear infinite;
}

@keyframes chromatic-glow {
  0% { filter: drop-shadow(0 0 20px #ff0000); }
  33% { filter: drop-shadow(0 0 20px #00ff00); }
  66% { filter: drop-shadow(0 0 20px #0000ff); }
  100% { filter: drop-shadow(0 0 20px #ff0000); }
}
</style>