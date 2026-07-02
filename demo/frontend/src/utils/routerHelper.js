import { ref } from 'vue'
import router from '../router/index.js'

// 全局加载状态（可以在 BasicLayout 里显示加载动画）
export const isPageLoading = ref(false)

/**
 * 带数据预加载的页面跳转
 * @param {Object} to - 目标路由对象 { path, query }
 * @param {Function} dataLoader - 数据加载函数（返回 Promise）
 */
export const navigateWithPreload = async (to, dataLoader = null) => {
    // 1. 开始加载，显示加载状态
    isPageLoading.value = true

    try {
        // 2. 如果有数据加载函数，先预加载数据
        if (dataLoader) {
            await dataLoader()
        }

        // 3. 强制等待 0.5s（让用户看到加载状态，体验更好）
        await new Promise(resolve => setTimeout(resolve, 500))

        // 4. 数据加载完，执行跳转（BasicLayout 的动画会自动播放）
        router.push(to)
    } catch (error) {
        console.error('页面跳转失败：', error)
    } finally {
        // 5. 跳转完成，隐藏加载状态
        isPageLoading.value = false
    }
}