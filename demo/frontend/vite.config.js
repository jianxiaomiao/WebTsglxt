import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 改成带 mode 参数的函数形式，用于读取环境变量
export default defineConfig(({ mode }) => {
  // 加载当前环境的.env文件（开发环境读.env.development，生产环境读.env.production）
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [
      vue(),
      // 保留Element Plus自动导入的原有配置
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    base: '/', // 保留相对路径配置（解决静态文件404）
    // 保留开发环境的代理配置（开发时/api仍转发到localhost）
    server: {
      // ✅【新增1】允许所有网络访问（解决ngrok外部无法访问）
      host: '0.0.0.0',
      // ✅【新增2】放行你的ngrok域名（解决403 Forbidden）
      allowedHosts: ['prologue-freebee-scholar.ngrok-free.dev'],
      proxy: {
        '/api': {
          target: 'http://localhost:8082/demo_war_exploded',
          changeOrigin: true,
          // ✅ 核心修复：重写 Cookie 路径，解决上下文不匹配导致的 Cookie 丢失
          cookiePathRewrite: {
            '/demo_war_exploded': '/'
          },
          // 可选：统一 Cookie 域名，本地都是 localhost 可省略，加上更稳妥
          cookieDomainRewrite: 'localhost'
        },
        '/forum_images': {
          target: 'http://localhost:8082/',
          changeOrigin: true
        },
      // 🔥【新增】视频和文件请求路径的代理规则，保持和图片一致的 target
      '/chat_files': {
        target: 'http://localhost:8082/',
        changeOrigin: true
      },
        // ✅ 新增：WebSocket 代理（核心！）
        '/chat/ws': {
          target: 'ws://localhost:8082/demo_war_exploded', // 后端WebSocket地址
          ws: true,        // 开启WebSocket代理（必须加！）
          changeOrigin: true
        }
      }
    },
    // 新增：把环境变量注入到代码中，供前端使用
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL),
      'import.meta.env.VITE_IMAGE_BASE_URL': JSON.stringify(env.VITE_IMAGE_BASE_URL),
      // 🔥【新增】把文件环境变量注入到代码中，供前端项目使用
      'import.meta.env.VITE_FILE_BASE_URL': JSON.stringify(env.VITE_FILE_BASE_URL)
    }
  }
})
/*
// 改成带mode参数的函数形式，用于读取环境变量，开发环境
export default defineConfig(({ mode }) => {
  // 加载当前环境的.env文件（开发环境读.env.development，生产环境读.env.production）
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [
      vue(),
      // 保留Element Plus自动导入的原有配置
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    base: './', // 保留相对路径配置（解决静态文件404）
    // 保留开发环境的代理配置（开发时/api仍转发到localhost）
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8081/tsglxt',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '/forum_images': {
        target: 'http://localhost:8081/',
        changeOrigin: true
      }
      },
    },
    // 新增：把环境变量注入到代码中，供前端使用
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL),
      'import.meta.env.VITE_IMAGE_BASE_URL': JSON.stringify(env.VITE_IMAGE_BASE_URL)
    }
  }
})
*/
