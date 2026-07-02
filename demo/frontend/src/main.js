// src/main.js
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate' // 持久化插件
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 新增：导入Element Plus暗黑模式样式
import 'element-plus/theme-chalk/dark/css-vars.css'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 2. 创建应用实例
const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 3. 全局注册Element Plus图标（保留原有逻辑）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 4. 注册全局依赖
app.use(ElementPlus)
app.use(pinia)
app.use(router)

// 5. 挂载根组件
/*核心作用：把创建好的 Vue 应用实例 app，挂载到 index.html 中 <div id="app"></div> 这个 DOM 容器上；*/
app.mount('#app')