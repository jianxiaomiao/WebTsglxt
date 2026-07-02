import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useAppSettingsStore = defineStore('appSettings', () => {
    const currentUserId = ref('')

    // 1. 默认视觉开关矩阵
    const widgets = ref({
        showBottle: true,        // 小宠物
        showCoRead: true,   // 左上角引导球
        showGraph: true,     // 漂流瓶入口
        showGuideBtn: true,     // 右下角共读舱胶囊
        showPet: true,    // 天气面板
        showWeather: true,       // 图谱节点渲染
        showWeatherPane: true
    })

    // 2. 壁纸引擎配置
    const wallpaper = ref({
        type: 'preset', // 'preset' | 'custom'
        url: '/dist/bg.jpg'
    })

    // 官方精心调制的预设壁纸库
    const presetWallpapers = [
        { id: 'default', name: '清晨书香', url: '/dist/bg.jpg', preview: '🌄' },
        { id: 'deepblue', name: '深海静思', url: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQlET3HkVEwINfGcnnUWgkEi_kQtVwcw7HkoHGtmzGxFw&s=10', preview: '🌊' },
        { id: 'cyber', name: '悠闲一刻', url: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTuovtCgErgiYLNzSkglJWbCPsbFk27q7Jrdg&s', preview: '🌆' },
        { id: 'paper', name: '绚烂水彩', url: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTL-i0EbyvqC5aUDVZyGejNxa1Wfxn--yKAby98XiPYHg&s=10', preview: '📜' }
    ]

    // 初始化加载当前用户的专属配置
    const initForUser = (userId) => {
        if (!userId) return
        currentUserId.value = userId
        const saved = localStorage.getItem(`app_settings_${userId}`)
        if (saved) {
            try {
                const parsed = JSON.parse(saved)
                if (parsed.widgets) widgets.value = parsed.widgets
                if (parsed.wallpaper) wallpaper.value = parsed.wallpaper
            } catch(e){}
        }
    }

    // 监听数据，只要产生任何改动，静默自动保存！
    watch([widgets, wallpaper], () => {
        if (!currentUserId.value) return
        const payload = {
            widgets: widgets.value,
            wallpaper: wallpaper.value
        }
        localStorage.setItem(`app_settings_${currentUserId.value}`, JSON.stringify(payload))
    }, { deep: true })

    // 恢复默认设置
    const resetAll = () => {
        widgets.value = { showPet: true, showGuideBtn: true, showBottle: true, showCoRead: true, showWeather: true, showGraph: true }
        wallpaper.value = { type: 'preset', url: '/dist/bg.jpg' }
    }

    return {
        currentUserId,
        widgets,
        wallpaper,
        presetWallpapers,
        initForUser,
        resetAll
    }
})