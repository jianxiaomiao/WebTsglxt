import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import request from '../utils/request'
import { ElMessage } from 'element-plus' // 🔥 必须加上这一句！

// 布局组件（必须同步加载）
import BasicLayout from '../components/BasicLayout.vue'

// 🔥 首屏核心页面：同步加载（6 个高频页面）
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import Bookshelf from '../views/Bookshelf.vue'
import Forum from '../views/Forum.vue'
import Chat from '../views/Chat.vue'
import Profile from '../views/Profile.vue'

// 🔥 其余页面：按需懒加载（首屏不加载，路由跳转时才 fetch）
const Message = () => import('../views/Message.vue')
const BookDetail = () => import('../views/BookDetail.vue')
const UserInfo = () => import('../views/UserInfo.vue')
const BorrowInfo = () => import('../views/BorrowInfo.vue')
const BookReader = () => import('../views/BookReader.vue')
const MyNotes = () => import('../views/MyNotesPage.vue')
const MyForumComments = () => import('../views/MyForumComments.vue')
const MyBookComments = () => import('../views/MyBookComments.vue')
const UserProfile = () => import('../views/UserProfile.vue')
const WeeklyReport = () => import('../views/WeeklyReport.vue')
const ShareRedirect = () => import('../views/ShareRedirect.vue')
const BookSquare = () => import('../views/BookSquare.vue')
const BookSquareDetail = () => import('../views/BookSquareDetail.vue')
const ReadHistoryGallery = () => import('../views/ReadHistoryGallery.vue')
const Settings = () => import('../views/Settings.vue')
const AudiobookLibrary = () => import('../views/AudiobookLibrary.vue')
const AdminPanel = () => import('../views/AdminPanel.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 1. 登录/注册：独立路由（不使用BasicLayout）
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/register',
      name: 'Register',
      component: Register
    },
    // 2. 其他页面：共享BasicLayout布局
    {
      path: '/',
      component: BasicLayout,
      children: [
        { path: '', redirect: '/login' },
        { path: 'home', name: 'Home', component: Home },
        { path: 'bookshelf', name: 'Bookshelf', component: Bookshelf },
        {
          path: '/audiobook',
          name: 'AudiobookLibrary',
          component: AudiobookLibrary
        },
        { path: 'forum', name: 'Forum', component: Forum },
        { path: 'chat', name: 'Chat', component: Chat },
        { path: 'message', name: 'Message', component: Message },
        { path: 'profile', name: 'Profile', component: Profile },
        { path: 'book/detail', name: 'BookDetail', component: BookDetail },
        { path: 'userInfo', name: 'UserInfo', component: UserInfo },
        { path: 'borrowInfo', name: 'BorrowInfo', component: BorrowInfo },
        { path: 'book/reader', name: 'BookReader', component: BookReader },
        { path: 'profile/notes', name: 'MyNotes', component: MyNotes },
        // 修复：子路由path移除开头多余斜杠
        { path: 'profile/forum-comments', name: 'MyForumComments', component: MyForumComments },
        { path: 'profile/book-comments', name: 'MyBookComments', component: MyBookComments },
        { path: 'user/profile', name: 'UserProfile', component: UserProfile },
        { path: 'weeklyReport', name: 'weeklyReport', component: WeeklyReport },
        { path: 'read-gallery', name: 'ReadHistoryGallery', component: ReadHistoryGallery },
        {
          path: 'settings', // 访问路径为 /settings
          name: 'SpaceSettings',
          component: Settings
        },
        // 2. 在 path: '/' 的 children 数组中追加路由：
        {
          path: 'admin',
          name: 'AdminPanel',
          component: AdminPanel,
          meta: { requiresAdmin: true }
        },
        {
          path: '/achievements',
          name: 'AchievementCenter',
          component: () => import('../views/AchievementCenter.vue'),
          meta: { title: '成就中心', requiresAuth: true }
        },
        // 分享重定向为一级独立路由，保持原有路径不变，不放进子路由
      ]
    },
    // 分享短链独立一级路由
    { path: '/s/:isbn', name: 'BookShareRedirect', component: ShareRedirect },
    // 书广场页面
    {
      path: '/bookSquare',
      component: BasicLayout,
      children: [
        { path: '', name: 'BookSquare', component: BookSquare },
        { path: 'detail/:id', name: 'BookSquareDetail', component: BookSquareDetail }
      ]
    }
  ]
})

// 白名单：无需登录即可访问的页面
const publicPages = ['/login', '/register']
// 🔥 新增标志位：判断是否是用户刚刚打开/刷新浏览器
let isAppInit = true

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // 1. 优先读取本地持久化 Token（页面刷新后 Pinia 会清空，从本地恢复）
  const localToken = localStorage.getItem('autoLoginToken')

  // ===================== 核心拦截：静默自动登录 =====================
  // 只有当应用程序刚初始化（重开浏览器）且 Pinia 发现未登录，但本地有持久 Token 时才触发
  if (isAppInit) {
    isAppInit = false

    if (!userStore.isLogin && localToken) {
      try {
        const res = await request.get('/user/info', {
          params: { token: localToken }
        })

        // 后端返回成功，说明 Token 有效且没过期
        if (res.code === 200 && res.data) {
          // 静默恢复登录状态
          userStore.login({
            user: res.data,
            token: localToken
          })
        } else {
          userStore.logout() // Token失效，清理废弃数据
        }
      } catch (error) {
        userStore.logout()
      }
    }
  }

  // ===================== 场景2：未登录，但本地存在记住我的 Token（尝试自动登录） =====================
  if (localToken) {
    try {
      // 调用 Servlet 接口：GET /user/info?token=xxx
      const res = await request.get('/user/info', {
        params: { token: localToken }
      })

      // 🔥 直接判断后端返回的是否成功即可，无需再算时间
      if (res.code === 200 && res.data) {
        const userInfo = res.data

        // Token 有效且未过期：初始化 Pinia 登录状态
        userStore.login({
          user: userInfo,
          token: userInfo.login_token || localToken
        })

        // 自动登录成功：目标是登录页 → 跳主页；其他页面正常放行
        return to.path === '/login' ? next('/home') : next()

      } else {
        // 🔥 后端返回了 500 (无效或过期)，直接清理并拦截
        userStore.logout()
        return next('/login')
      }
    } catch (error) {
      // 接口异常（网络/后端报错）：清空凭证，跳登录页
      userStore.logout()
      return next('/login')
    }
  }

  // ===================== 常规页面鉴权逻辑 =====================
  if (userStore.isLogin) {
    if (to.path === '/login' || to.path === '/register') {
      return next('/home')
    }
    // 🔥 新增：管理员路由页面拦截
    if (to.meta.requiresAdmin && userStore.userType !== 3) {
      ElMessage.error('越权访问：该页面仅限管理员进入')
      return next('/home')
    }
    return next()
  }

  const isPublicPage = publicPages.includes(to.path)
  if (isPublicPage) {
    return next()
  }

  return next('/login')
})

export default router