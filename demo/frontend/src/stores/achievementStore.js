import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from "../utils/request.js";

export const useAchievementStore = defineStore('achievement', () => {
    // 状态
    const badges = ref([])
    const userLevel = ref(1)
    const currentExp = ref(0)
    const nextLevelExp = ref(100)
    const totalBooks = ref(0)
    const totalDuration = ref(0) // 分钟
    const consecutiveDays = ref(0)
    // 动效弹窗状态
    const showUnlockModal = ref(false)
    const unlockedBadgesQueue = ref([])

    // 计算属性
    const totalBadges = computed(() => badges.value.length)
    const unlockedCount = computed(() => badges.value.filter(b => b.isUnlocked).length)
    const expProgress = computed(() => (currentExp.value / nextLevelExp.value) * 100)

    // Actions
    const fetchAchievements = async (userId) => {
        const res = await request.get(`/achievements?userId=${userId}`)
        if (res.code === 200) {
            badges.value = res.data.badges
            userLevel.value = res.data.level
            currentExp.value = res.data.exp
            nextLevelExp.value = res.data.nextExp
            // 读取阅读统计
            if (res.data.stats) {
                totalBooks.value = res.data.stats.totalBooks || 0
                totalDuration.value = res.data.stats.totalDuration || 0
                consecutiveDays.value = res.data.stats.consecutiveDays || 0
            }
        }
    }

    const checkAchievements = async () => {
        const res = await request.post('/achievements/check')
        if (res.code === 200 && res.data.newlyUnlocked.length > 0) {
            // 加入队列并触发全局动效
            unlockedBadgesQueue.value.push(...res.data.newlyUnlocked)
            if (!showUnlockModal.value) {
                showUnlockModal.value = true
            }
            // 刷新本地列表
            await fetchAchievements()
        }
    }

    const closeUnlockModal = () => {
        unlockedBadgesQueue.value.shift()
        if (unlockedBadgesQueue.value.length === 0) {
            showUnlockModal.value = false
        }
    }

    return {
        badges, userLevel, currentExp, nextLevelExp, showUnlockModal, unlockedBadgesQueue,
        totalBadges, unlockedCount, expProgress,
        fetchAchievements, checkAchievements, closeUnlockModal
    }
}, {
    persist: true
})