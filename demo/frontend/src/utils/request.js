import axios from 'axios'
import { ElMessage } from 'element-plus' // 替换为Element Plus的消息提示
// 创建axios实例
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 60000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    },
    withCredentials: true
})

// 🐢 请求拦截器：记录开始时间用于慢请求统计
request.interceptors.request.use(config => {
    config._startTime = Date.now()
    return config
})

// 响应拦截器：统一处理返回结果
request.interceptors.response.use(
    (response) => {
        // 后端返回的ResultDTO数据
        const res = response.data
        // 成功：直接返回数据
        if (res.code === 200 || res.code === "200"||res.error_code === 0) {
            const duration = Date.now() - (response.config._startTime || 0)
            if (duration > 1000) console.warn(`🐢 慢请求: ${response.config.url} (${duration}ms)`)
            return res
        } else {
            const errorMsg = res.msg || res.reason || '请求失败'
            // 失败：提示错误（ElMessage用法和Arco的Message一致）
            return Promise.reject(res)
        }
    },
    (error) => {
        // 网络错误提示（区分404/500等）
        const errorMsg = error.response
            ? `请求错误[${error.response.status}]：${error.response.data?.msg || '后端无响应'}`
            : '网络异常（请检查后端是否启动）'
        ElMessage.error(errorMsg)
        return Promise.reject(error)
    }
)

export default request

// 🔥 批量聚合请求：一次拉多个模块数据
// 用法：const { user, unread, pending } = await batch(['user', 'unread', 'pending'])
export const batch = async (modules) => {
    const res = await request.get('/aggregate/init', {
        params: { modules: modules.join(',') }
    })
    if (res.code === 200) return res.data
    throw new Error('聚合请求失败')
}