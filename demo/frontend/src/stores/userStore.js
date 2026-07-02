// src/stores/user.js
import { defineStore } from 'pinia'
import request from '../utils/request'
// 定义用户仓库
export const useUserStore = defineStore('user', {
    // 状态：存储所有用户信息
    state: () => ({
        userId: '',           // 用户ID（核心！）
        userType: '',         // 用户类型（学生/管理员）
        userInfo: {},         // 用户完整信息（姓名、性别、系别等）
        isLogin: false,        // 登录状态
        token: '',

        // ====================== 🔥 新增阅读状态 ======================
        currentReadingIsbn: '',   // 当前正在阅读的书籍 ISBN
        currentReadingChapter:'',
        lastReadTime: 0,          // 最后一次阅读计时点（时间戳）
        readTimerId: null,         // 阅读定时器 ID（用于精准清除）

        // ====================== 🔥 知识图谱状态 ======================
        graphPanelWidth: 400, // 默认图谱面板宽度
        isDark: false,          // 暗黑模式（默认关闭）
        currentWeather: 'sunny',// 当前天气（默认晴天）
        isExpanded: false,      // 面板展开状态（默认收起）

        glassPreset: 'frosted', // 预设材质：'frosted'(磨砂) | 'liquid'(液态)

        // 🔥 新增：共读舱抽屉的持久化宽度（默认 380px）
        coReadDrawerWidth: Number(localStorage.getItem('co_read_width')) || 380,
    }),

    // 方法：修改状态
    actions: {
        // 1. 登录成功 → 保存用户所有信息
        login(data) {
            // 注意：现在后端返回的可能是包含 { user: {...}, token: 'xxx' } 的对象
            this.userId = data.user.userId
            this.userType = data.user.type
            this.userInfo = data.user
            this.isLogin = true
            if (data.token) {
                this.token = data.token
                localStorage.setItem('autoLoginToken', data.token)
            }
        },

        // 2. 退出登录 → 清空所有信息
        async logout() {
            // 🔥 通知后端清除云端 Token（安全退出）
            if (this.userId && localStorage.getItem('autoLoginToken')) {
                try {
                    await request.patch('/user/info?type=clearToken', { userId: this.userId })
                } catch (e) {
                    console.error('清除云端Token失败', e)
                }
            }

            // 🌟 核心修复：退出登录前，缓存界面的环境偏好
            const preserveDark = this.isDark
            const preserveWeather = this.currentWeather
            const preserveExpanded = this.isExpanded

            // 重置仓库数据（清空 userId, token 等用户信息）
            this.$reset()

            // 🌟 核心修复：重置完毕后，立刻恢复界面的环境偏好，防止画面闪烁跳变
            this.isDark = preserveDark
            this.currentWeather = preserveWeather
            this.isExpanded = preserveExpanded

            // 🔥 清除本地的持久化 Token
            localStorage.removeItem('autoLoginToken')
        },

        // 3. 更新用户信息（个人中心修改资料用）
        updateUserInfo(info) {
            this.userInfo = { ...this.userInfo, ...info }
        },

        // ====================== 🔥 新增阅读相关方法 ======================
        // 开始阅读
        startReading(isbn,chapter) {
            this.currentReadingIsbn = isbn
            this.currentReadingChapter = chapter
            this.lastReadTime = Date.now()
        },
        // 结束阅读（清空）
        stopReading() {
            this.currentReadingIsbn = ''
            this.currentReadingChapter = ''
            this.lastReadTime = 0
        },
        // 更新最后阅读时间
        updateLastReadTime() {
            this.lastReadTime = Date.now()
        },

        // 更新图谱面板宽度
        updateGraphPanelWidth(width) {
            this.graphPanelWidth = width
        },
        // ====================== ✅ 新增全局状态修改方法 ======================
        // 切换暗黑模式
        // 切换暗黑模式
        toggleDarkMode() {
            this.isDark = !this.isDark
            // 🔥 新增：把 wind 和 snow 加入“全天候天气”保护名单
            if (!['rain', 'thunder', 'wind', 'snow'].includes(this.currentWeather)) {
                this.currentWeather = this.isDark ? 'night' : 'sunny'
            }
        },
        // 设置天气类型
        setWeather(weatherType) {
            this.currentWeather = weatherType
        },
        // 切换面板展开/收起
        toggleExpanded() {
            this.isExpanded = !this.isExpanded
        },

        // 补一个切换材质的方法
        toggleGlassPreset() {
            this.glassPreset = this.glassPreset === 'frosted' ? 'liquid' : 'frosted';
        },

        // 🔥 新增：更新并持久化共读舱宽度
        updateCoReadWidth(newWidth) {
            this.coReadDrawerWidth = newWidth
            localStorage.setItem('co_read_width', newWidth)
        }
    },

    // 🔥 开启本地持久化（刷新不丢失！）
    persist: {
        storage: sessionStorage
    }
})