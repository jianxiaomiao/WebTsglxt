<template>
  <div class="app-wallpaper-engine" :style="{ backgroundImage: `url(${settingsStore.wallpaper.url})` }"></div>
  <!-- 模板部分无需修改 -->
  <div v-if="showMask" class="dark-mode-mask" :style="maskStyle" ref="maskRef"></div>
  <WeatherAtmosphere v-if="settingsStore.widgets.showWeather" :weather="currentWeather" />
  <ElConfigProvider :locale="zhCn" :size="globalSize">
    <div class="app-wrapper">
      <div id="app" style="width: 100%; height: 100vh;  position: relative; z-index: 100;">
        <router-view />
      </div>
    </div>
  </ElConfigProvider>
  <canvas ref="mouseTrailCanvas" class="mouse-trail-canvas"></canvas>
  <PetWidget v-if="settingsStore.widgets.showPet"/>
  <CoReadingRoom v-if="settingsStore.widgets.showCoRead"/>
  <button v-if="settingsStore.widgets.showGuideBtn" class="liquid-guide-btn glass-panel" @click="replayGuide" title="重新观看指南">
    💡
  </button>
  <div v-if="settingsStore.widgets.showWeatherPane" class="weather-test-panel glass-panel"
       :class="{ 'is-expanded': isExpanded, 'is-half-hidden': weatherRight < 0 }"
       :style="{ top: weatherTop + 'px', right: weatherRight + 'px' }"
       @mousedown.left="startWeatherDrag"
       @touchstart.stop="handleWeatherTouchStart"
       @touchmove.stop="handleWeatherTouchMove"
       @touchend.stop="handleWeatherTouchEnd"
       @touchcancel.stop="handleWeatherTouchEnd"
       @click="handleWeatherClick">

    <div class="trigger-icon" :class="{ 'hide': isExpanded }">
      {{ weatherIcons[currentWeather] }}
    </div>

    <div class="expanded-content" :class="{ 'show': isExpanded }">
      <div class="vibe-mode-selector" @click.stop="handlePresetToggle" :title="glassPreset === 'liquid' ? '当前：液态水晶 (点击切换磨砂)' : '当前：磨砂玻璃 (点击切换液态)'">
        <span class="weather-label" style="margin:0;">氛围</span>
        <span class="preset-indicator">{{ glassPreset === 'liquid' ? '🙀' : '😹' }}</span>
      </div>
      <el-button-group>
        <el-button round @click.stop="handleWeatherChange('sunny', $event)" :type="currentWeather === 'sunny' ? 'primary' : 'default'">☀️</el-button>
        <el-button @click.stop="handleWeatherChange('cloudy', $event)" :type="currentWeather === 'cloudy' ? 'primary' : 'default'">☁️</el-button>
        <el-button @click.stop="handleWeatherChange('rain', $event)" :type="currentWeather === 'rain' ? 'primary' : 'default'">🌧️</el-button>
        <el-button @click.stop="handleWeatherChange('thunder', $event)" :type="currentWeather === 'thunder' ? 'primary' : 'default'">⛈️</el-button>
        <el-button round @click.stop="handleWeatherChange('wind', $event)" :type="currentWeather === 'wind' ? 'primary' : 'default'">🌪️</el-button>
        <el-button round @click.stop="handleWeatherChange('snow', $event)" :type="currentWeather === 'snow' ? 'primary' : 'default'">❄️</el-button>
        <el-button round @click.stop="handleWeatherChange('night', $event)" :type="currentWeather === 'night' ? 'primary' : 'default'">🌙</el-button>
      </el-button-group>
      <div class="collapse-btn" @click.stop="handleWeatherClose" title="收起">✖</div>
    </div>
  </div>
  <AchievementUnlocker />
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import { startGuideForPath, getGuideConfigByPath } from './utils/appGuide'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { ref, provide, watch, nextTick, onMounted, computed } from 'vue'
import PetWidget from "./components/PetWidget.vue";
import WeatherAtmosphere from "./components/WeatherAtmosphere.vue"
// 1. 引入 Pinia 的 userStore
import { useUserStore} from "./stores/userStore.js";
import { useRoute } from 'vue-router'
import CoReadingRoom from './components/CoReadingRoom.vue'
import { useAppSettingsStore } from './stores/appSettingsStore'
import { useAchievementStore } from './stores/achievementStore'
import confetti from 'canvas-confetti'
import AchievementUnlocker from "./components/AchievementUnlocker.vue";
const globalSize = 'default'
// 2. 获取 userStore 实例
const userStore = useUserStore()
const route = useRoute()

// 1. 在原有的 computed 区域，接管 preset
const glassPreset = computed(() => userStore.glassPreset || 'frosted')

const syncWeatherTheme = () => {
  const html = document.documentElement

  // 1. 极其霸道地把旧天气和旧材质类名全洗掉
  html.className = html.className.replace(/\bweather-\S+/g, '').replace(/\bpreset-\S+/g, '')

  if (userStore.currentWeather) {
    html.classList.add(`weather-${userStore.currentWeather}`)
  }

  // 🔥 2. 注入材质类名（必须加 .value，因为它是 computed）
  if (glassPreset.value) {
    html.classList.add(`preset-${glassPreset.value}`)
  }

  if (userStore.isDark) {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

// 3. 供 template 调用的切换函数
const handlePresetToggle = () => {
  userStore.toggleGlassPreset()
}

// 3. 用 computed 接管状态（双向绑定 store）
const isDark = computed(() => userStore.isDark)
const isExpanded = computed({
  get: () => userStore.isExpanded,
  set: (val) => { userStore.isExpanded = val }
})
const currentWeather = computed({
  get: () => userStore.currentWeather,
  set: (val) => { userStore.currentWeather = val }
})

const weatherIcons = {
  sunny: '☀️',
  cloudy: '☁️',
  rain: '🌧️',
  thunder: '⛈️',
  wind: '🌪️',  // 🔥 新增：大风
  snow: '❄️',  // 🔥 新增：下雪
  night: '🌙'
}

// =====================================================================
// 4. 升级版天气切换方法：自带日夜交替“圆形扩散动画”
// =====================================================================
const handleWeatherChange = (weatherType, event) => {
  // 1. 预判这次天气切换，会不会导致黑夜/白天的“大翻转”？
  let targetIsDark = userStore.isDark;
  if (weatherType === 'night') {
    targetIsDark = true;
  } else if (weatherType === 'sunny') {
    targetIsDark = false;
  }

  // 2. 如果发生了日夜大翻转，并且浏览器支持酷炫动画，且拿到了点击坐标！
  if (targetIsDark !== userStore.isDark && document.startViewTransition && event) {
    // 获取鼠标点击天气的坐标（作为动画扩散的圆心）
    const { clientX: x, clientY: y } = event;
    const maxRadius = Math.hypot(
        Math.max(x, window.innerWidth - x),
        Math.max(y, window.innerHeight - y)
    );

    // 冻结当前画面，准备变形
    const transition = document.startViewTransition(() => {
      userStore.setWeather(weatherType);
      userStore.isDark = targetIsDark; // 强制翻转黑夜/白天
    });

    // 播放画皮动画
    transition.ready.then(() => {
      const clipPath = [
        `circle(0px at ${x}px ${y}px)`,
        `circle(${maxRadius}px at ${x}px ${y}px)`
      ];
      document.documentElement.animate(
          { clipPath: clipPath },
          {
            duration: 800,
            easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
            pseudoElement: '::view-transition-new(root)',
          }
      );
    });
  } else {
    // 3. 只是普通切换天气（比如晴天切下雨），直接无缝切换
    userStore.setWeather(weatherType);
    userStore.isDark = targetIsDark;
  }
}

// 提供给子组件的变量（不变，只是基于 computed）
provide('isDark', isDark)
provide('currentWeather', currentWeather)
provide('handleWeatherChange', handleWeatherChange)

// 5. 监听暗黑模式：仅同步 HTML 类名（持久化由 Pinia 自动处理）
watch(isDark, (newVal) => {
  console.log('🔥 watch(isDark) 触发：', newVal)
  if (newVal) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  console.log('   - html标签当前类：', document.documentElement.className)
}, { immediate: true })

// 6. 修改暗黑模式切换方法：调用 store 的 toggleDarkMode
const toggleDarkModeWithAnimation = (event) => {
  if (!document.startViewTransition || !event) {
    userStore.toggleDarkMode()
    return;
  }

  const { clientX: x, clientY: y } = event;
  const maxRadius = Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y)
  );

  const transition = document.startViewTransition(() => {
    userStore.toggleDarkMode() // 改用 store 方法
  });

  transition.ready.then(() => {
    const clipPath = [
      `circle(0px at ${x}px ${y}px)`,
      `circle(${maxRadius}px at ${x}px ${y}px)`
    ];
    document.documentElement.animate(
        { clipPath: clipPath },
        {
          duration: 800,
          easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
          pseudoElement: '::view-transition-new(root)',
        }
    );
  });
};

provide('toggleDarkMode', toggleDarkModeWithAnimation)

// 7. 简化初始化逻辑：从 store 读取状态，同步 HTML 类名
const initDarkMode = () => {
  console.log('🔥 初始化暗黑模式：')
  console.log('   - store 中 isDark 值：', userStore.isDark)
  if (userStore.isDark) {
    document.documentElement.classList.add('dark')
  }
  console.log('   - 最终 isDark 值：', userStore.isDark)
}
initDarkMode()

// 波纹动画相关状态（不变）
const showMask = ref(false)
const maskStyle = ref({})
const maskRef = ref(null)

// 监听状态变化，实时更新
watch(
    () => [userStore.currentWeather, userStore.isDark, userStore.glassPreset],
    () => {
      syncWeatherTheme()
    }
)

// ====================== 🌤️ 天气面板交互与拖拽核心逻辑 ======================
const weatherTop = ref(150)    // 默认距离顶部 150px
const weatherRight = ref(-27)  // 默认向右溢出隐藏一半 (54的一半是27)
let isDraggingWeather = false
let weatherHasMoved = false
let wStartX = 0, wStartY = 0, wStartRight = 0, wStartTop = 0

// 边界检测：防止面板跑出屏幕外
const applyWeatherBoundary = (targetRight, targetTop) => {
  // 🔥 移动端与PC端双端自适应预判（手机端宽度预判为290，高度为82）
  const isMob = window.innerWidth <= 768
  const wWidth = isExpanded.value ? (isMob ? 290 : 360) : 54
  const wHeight = isExpanded.value && isMob ? 82 : 54

  const maxRight = window.innerWidth - wWidth
  const maxTop = window.innerHeight - wHeight

  weatherRight.value = Math.max(-27, Math.min(targetRight, maxRight))
  weatherTop.value = Math.max(0, Math.min(targetTop, maxTop))
}

// ------ 💻 PC端拖拽 ------
const startWeatherDrag = (e) => {
  // 展开状态下，如果点在按钮组上，不触发拖拽，保留按钮本身的点击
  if (isExpanded.value && e.target.closest('.el-button-group, .collapse-btn')) return;

  isDraggingWeather = true
  weatherHasMoved = false
  wStartX = e.clientX
  wStartY = e.clientY
  wStartRight = weatherRight.value
  wStartTop = weatherTop.value
  document.addEventListener('mousemove', onWeatherDrag)
  document.addEventListener('mouseup', stopWeatherDrag)
}

const onWeatherDrag = (e) => {
  if (!isDraggingWeather) return
  const dx = e.clientX - wStartX
  const dy = e.clientY - wStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) weatherHasMoved = true
  applyWeatherBoundary(wStartRight - dx, wStartTop + dy)
}

const stopWeatherDrag = () => {
  isDraggingWeather = false
  document.removeEventListener('mousemove', onWeatherDrag)
  document.removeEventListener('mouseup', stopWeatherDrag)

  // 灵动细节：如果拖拽松手时靠近右侧边缘（<10px）且没展开，自动吸附藏进去
  if (!isExpanded.value && weatherRight.value < 10) {
    weatherRight.value = -27
  }
}

// ------ 📱 移动端触摸拖拽 ------
const handleWeatherTouchStart = (e) => {
  if (isExpanded.value && e.target.closest('.el-button-group, .collapse-btn')) return;
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  wStartX = touch.clientX
  wStartY = touch.clientY
  wStartRight = weatherRight.value
  wStartTop = weatherTop.value
  weatherHasMoved = false
  isDraggingWeather = false
}

const handleWeatherTouchMove = (e) => {
  // 1. 智能放行：如果面板展开，且手指滑动的是按钮区域，直接放开拦截！
  if (isExpanded.value && e.target.closest('.el-button-group, .collapse-btn')) {
    return;
  }

  // 2. 拖拽本体：如果是按住外壳在拖拽，才手动阻止浏览器页面的默认滚动
  if (e.cancelable) {
    e.preventDefault();
  }

  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  const dx = touch.clientX - wStartX
  const dy = touch.clientY - wStartY

  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
    weatherHasMoved = true
    isDraggingWeather = true
  }

  if (isDraggingWeather) {
    applyWeatherBoundary(wStartRight - dx, wStartTop + dy)
  }
}

const handleWeatherTouchEnd = () => {
  if (isDraggingWeather) {
    isDraggingWeather = false
    if (!isExpanded.value && weatherRight.value < 10) {
      weatherRight.value = -27
    }
  } else if (!weatherHasMoved) {
    handleWeatherClick() // 没滑动则模拟点击
  }
}

// ------ ✨ 点击交互状态机 ------
const handleWeatherClick = () => {
  // 防误触：拖拽完成瞬间拦截点击事件
  if (weatherHasMoved) {
    weatherHasMoved = false
    return
  }

  if (weatherRight.value < 0) {
    // 状态1：半隐藏 -> 弹出完整圆球
    weatherRight.value = 20
  } else if (!isExpanded.value) {
    // 状态2：完整圆球 -> 展开按钮面板
    isExpanded.value = true
    nextTick(() => applyWeatherBoundary(weatherRight.value, weatherTop.value))
  }
}

// 专属收起逻辑
const handleWeatherClose = () => {
  isExpanded.value = false
  // 收起动画播放完后（约0.4秒），让它自动“缩”回边缘
  setTimeout(() => {
    weatherRight.value = -27
  }, 400)
}

// 在 <script setup> 里面加一个时间判断函数
const initAutoTimeVibe = () => {
  const hour = new Date().getHours()
  let targetVibe = 'sunny'

  // 划分时间段
  if (hour >= 6 && hour < 16) {
    targetVibe = 'sunny'     // 早上 6点 到 下午 4点：明媚晴天 ☀️
  } else if (hour >= 16 && hour < 19) {
    targetVibe = 'cloudy'    // 下午 4点 到 晚上 7点：傍晚多云 🌥️
  } else {
    targetVibe = 'night'     // 晚上 7点 到 明早 6点：静谧黑夜 🌙
  }

  // 自动触发天气与黑夜模式联动
  handleWeatherChange(targetVibe)
}

// ====================== ✨ 全局鼠标光绘 & 手电筒特效 (高性能休眠版) ======================
const mouseTrailCanvas = ref(null)

// 1. 抽离一个检查并触发引导的方法
const checkAndRunGuide = (path) => {
  const config = getGuideConfigByPath(path)

  // 如果当前页面没配置引导，直接不管
  if (!config) return

  // 动态生成 LocalStorage Key，比如 'app_guided_home_v1.0'
  const storageKey = `app_guided_${config.key}_v1.0`
  const hasVisited = localStorage.getItem(storageKey)

  if (!hasVisited) {
    // 延迟一点点，等页面动画和数据请求加载完再弹
    setTimeout(() => {
      startGuideForPath(path)
      localStorage.setItem(storageKey, 'true')
    }, 1000)
  }
}

// 4. 左上角魔法球：点击重新播放【当前页面】的引导
const replayGuide = () => {
  startGuideForPath(route.path)
}

const initMouseTrail = () => {
  const canvas = mouseTrailCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  let width = window.innerWidth
  let height = window.innerHeight
  canvas.width = width
  canvas.height = height

  window.addEventListener('resize', () => {
    width = window.innerWidth
    height = window.innerHeight
    canvas.width = width
    canvas.height = height
  })

  const particles = []
  // ====================== 🎨 活系统：天气专属流体色盘 ======================
  const weatherColorMap = {
    sunny: ['#64b5f6', '#ffd54f', '#81c784', '#f06292', '#e0e0e0'], // ☀️ 晴天：樱花粉、晴空蓝、暖金、薄荷绿
    cloudy: ['#cfd8dc', '#b0bec5', '#eceff1', '#ffffff', '#90a4ae'], // ☁️ 多云：柔和灰白、云朵银、雾霾蓝
    rain: ['#80d8ff', '#40c4ff', '#18ffff', '#a7ffeb', '#e1f5fe'],   // 🌧️ 下雨：水滴蓝、霓虹青、深邃青蓝
    thunder: ['#b388ff', '#8c9eff', '#18ffff', '#e040fb', '#90a4ae'],// ⛈️ 打雷：电光紫、闪电蓝、极光青、幽暗灰
    wind: ['#ffe57f', '#ffd180', '#ffb74d', '#bcaaa4', '#d7ccc8'],   // 🌪️ 大风：枯叶黄、琥珀金、沙尘灰
    snow: ['#ffffff', '#e0f7fa', '#84ffff', '#69f0ae', '#b9f6ca']    // ❄️ 下雪：纯白、冰蓝、极光绿
  }

  let mouseX = -1000
  let mouseY = -1000
  let currentRadius = 0
  let targetRadius = 0
  let stopTimer = null

  // 🔥 核心底层优化：动画引擎的启动钥匙与锁
  let animationFrameId = null
  let isAnimating = false // 记录当前是否正在渲染

  // 逐帧渲染引擎
  const render = () => {
    // 拦截器：如果页面切到后台了，直接掐断死循环
    if (document.hidden) {
      isAnimating = false
      return
    }

    ctx.clearRect(0, 0, width, height)
    let canSleep = false // 智能探测器：检测当前帧画完后，能不能直接睡觉？

    if (!isDark.value) {
      // 🌞 白天：发光流体
      ctx.globalCompositeOperation = 'lighter'
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i]
        p.vx *= 0.92
        p.vy *= 0.92
        p.x += p.vx
        p.y += p.vy
        p.life -= 0.02

        if (p.life <= 0) {
          particles.splice(i, 1)
          i--
          continue
        }
        ctx.globalAlpha = p.life
        ctx.beginPath()
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        ctx.fillStyle = p.color
        ctx.fill()
      }
      ctx.globalCompositeOperation = 'source-over'

      // 💡 智能休眠判断：如果没有粒子存活了，就可以睡觉了
      if (particles.length === 0) canSleep = true

    } else {
      // 🌙 黑夜：手电筒
      currentRadius += (targetRadius - currentRadius) * 0.08
      if (currentRadius > 1) {
        const gradient = ctx.createRadialGradient(mouseX, mouseY, 0, mouseX, mouseY, currentRadius)
        gradient.addColorStop(0, 'rgba(255, 230, 150, 0.15)')
        gradient.addColorStop(0.5, 'rgba(255, 215, 80, 0.05)')
        gradient.addColorStop(1, 'rgba(255, 215, 80, 0)')

        ctx.globalAlpha = 1
        ctx.beginPath()
        ctx.arc(mouseX, mouseY, currentRadius, 0, Math.PI * 2)
        ctx.fillStyle = gradient
        ctx.fill()
      } else {
        // 防止无限逼近0的浮点运算消耗性能
        currentRadius = 0
      }
      if (particles.length > 0) particles.length = 0

      // 💡 智能休眠判断：如果光晕完全收缩消失了，就可以睡觉了
      if (currentRadius === 0 && targetRadius === 0) canSleep = true
    }

    ctx.globalAlpha = 1

    // 🔥 见证奇迹的时刻：如果符合休眠条件，彻底终结本次 requestAnimationFrame，不吃1KB性能！
    if (canSleep) {
      isAnimating = false
      return
    }

    animationFrameId = requestAnimationFrame(render)
  }

  // 引擎唤醒器：只有在睡觉时才需要叫醒它
  const wakeUpAnimation = () => {
    if (!isAnimating && !document.hidden) {
      isAnimating = true
      render()
    }
  }

  // 鼠标移动监听
  window.addEventListener('mousemove', (e) => {
    mouseX = e.clientX
    mouseY = e.clientY

    if (!isDark.value) {
      // 💡 核心逻辑：实时读取当前天气，分配对应的色盘（如果没匹配到，默认给晴天）
      const currentColors = weatherColorMap[currentWeather.value] || weatherColorMap.sunny

      for(let i=0; i<2; i++){
        particles.push({
          x: mouseX,
          y: mouseY,
          vx: (Math.random() - 0.5) * 3,
          vy: (Math.random() - 0.5) * 3,
          life: 1,
          // 从当前专属天气的色盘中随机抽取颜色
          color: currentColors[Math.floor(Math.random() * currentColors.length)],
          size: Math.random() * 2 + 1.5
        })
      }
    } else {
      targetRadius = 180
      if (stopTimer) clearTimeout(stopTimer)
      stopTimer = setTimeout(() => { targetRadius = 0 }, 150)
    }

    // 🔥 鼠标一旦移动，立刻唤醒引擎！
    wakeUpAnimation()
  })

  // ====================== 🚀 Page Visibility 息屏/后台休眠机制 ======================
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      // 用户切走了：掐断 Canvas 循环，并给 body 打上冻结标签
      isAnimating = false
      if (animationFrameId) cancelAnimationFrame(animationFrameId)
      document.body.classList.add('suspend-animations')
    } else {
      // 用户回来了：解冻 CSS 动画，但 Canvas 继续保持休眠，直到他再次滑动鼠标
      document.body.classList.remove('suspend-animations')
    }
  })
}

const settingsStore = useAppSettingsStore()

watch(() => userStore.userId, (newUserId) => {
  if (newUserId) {
    settingsStore.initForUser(newUserId)
    console.log(`[中央控制台] 已为书友 ${newUserId} 唤醒专属空间磁场 🌌`)
  }
}, { immediate: true })

onMounted(() => {

  syncWeatherTheme()
  initMouseTrail()
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      document.title = '👀 页面崩溃啦！(骗你的)';
    } else {
      document.title = '💡 欢迎回到书屋！';
      setTimeout(() => { document.title = 'xiaomiao特调'; }, 2000);
    }
  });
  console.log('🔥 页面加载完成，全局样式检查：')
  console.log('   - html标签类：', document.documentElement.className)
  console.log('   - html背景色：', getComputedStyle(document.documentElement).backgroundColor)
  console.log('   - body背景色：', getComputedStyle(document.body).backgroundColor)
})

</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  width: 100%;
  height: 100%;
  -ms-overflow-style: none;
  scrollbar-width: none;
  font-family: system-ui, Avenir, Helvetica, Arial, sans-serif;
  line-height: 1.5;
  font-weight: 400;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
::-webkit-scrollbar {
  display: none;
}

/* 官方标准：全局背景色 */
html {
  background-color: transparent;
  transition: background-color 0.3s ease;
}

.app-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  transition: all 0.3s ease;
}
/* 修复 Element Plus 布局的高度问题 */
.el-container {
  height: 100% !important;
}

/* 🔥 液态玻璃遮罩层 */
.dark-mode-mask {
  position: fixed;
  border-radius: 50%;
  z-index: 9999 !important; /* 强制极高层级 */
  pointer-events: none;
  isolation: isolate !important;

  /* 核心 1：强力的毛玻璃折射效果 */
  backdrop-filter: blur(24px) saturate(150%) contrast(1.1);
  -webkit-backdrop-filter: blur(24px) saturate(150%) contrast(1.1);

  /* 核心 2：液态玻璃的厚度感与边缘高光（内外阴影叠加） */
  box-shadow:
      inset 0 0 40px rgba(255, 255, 255, 0.3),  /* 内部柔和反光 */
      inset 0 0 10px rgba(255, 255, 255, 0.8),  /* 边缘锐利高光 */
      0 20px 50px rgba(0, 0, 0, 0.2);           /* 水滴落在屏幕上的投影 */
}

/* 如果是从黑夜切回白天，阴影质感反转一下更通透 */
html.dark .dark-mode-mask {
  box-shadow:
      inset 0 0 40px rgba(0, 0, 0, 0.3),
      inset 0 0 10px rgba(255, 255, 255, 0.2),
      0 20px 50px rgba(0, 0, 0, 0.4);
}

/* ======================
 🔥 终极版：缓慢漫游流体背景（慢节奏+丰富动态）
 ====================== */
/* 补入真实壁纸层的 CSS */
/* 1. 给壁纸引擎本体加上滤镜过渡 */
.app-wallpaper-engine {
  position: fixed;
  top: -15%; left: -15%;
  width: 130vw; height: 130vh;
  background-size: cover;
  background-position: center;
  filter: blur(18px);
  opacity: 0.8;
  z-index: -10;
  animation: fluid-flow 25s ease-in-out infinite alternate;
  /* 极其丝滑的：切换壁纸渐变 + 黑夜压暗渐变 双轨过渡 */
  transition: background-image 0.8s cubic-bezier(0.25, 0.8, 0.25, 1),
  filter 0.6s ease,
  opacity 0.6s ease !important;
}

/* 🌙 黑夜模式触发时：本体适度压暗、稍微降噪 */
html.dark .app-wallpaper-engine {
  filter: blur(18px) brightness(0.55) saturate(80%) !important;
  opacity: 0.7 !important;
}

/* 2. 🔥 灵魂绝杀：在壁纸引擎肚子里，强行焊上一层“深空黑曜石柔光盾” */
.app-wallpaper-engine::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  /* 极致高级的深空蓝灰黑夜渐变 */
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.82) 0%, rgba(2, 6, 23, 0.95) 100%);
  mix-blend-mode: multiply; /* 正片叠底：完美吃掉过曝的刺眼白光，保留壁纸的幽暗色彩 */
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* 当切换到黑夜模式时，柔光盾瞬间显形！ */
html.dark .app-wallpaper-engine::after {
  opacity: 1;
}

/* 底层：背景图（缓慢漫游 + 微缩放，无生硬摇晃） */

/* 中层：渐变流体（剔除绿色调，升级为纯白微光渐变，只增幅光影流动，绝不染脏壁纸） */
html::after {
  content: '';
  position: fixed;
  top: -20%; left: -20%;
  width: 140vw; height: 140vh;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4) 0%, rgba(255, 255, 255, 0.03) 50%, rgba(255, 255, 255, 0.25) 100%);
  background-size: 300% 300%;
  z-index: -5;
  filter: blur(16px);
  opacity: 0.35;
  mix-blend-mode: soft-light;
  animation: fluid-gradient 30s linear infinite;
  pointer-events: none; /* 绝对穿透鼠标点击 */
}
/* 顶层：顶部沉浸式呼吸光晕（维持高雅顶光） */
body::before {
  content: '';
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background: radial-gradient(ellipse at top center, rgba(255, 255, 255, 0.18) 0%, transparent 65%);
  z-index: -1;
  mix-blend-mode: soft-light;
  animation: fluid-breath 20s ease-in-out infinite alternate;
  opacity: 0.8;
  pointer-events: none;
}

/* ======================
 🔥 重写动画：慢节奏 + 丰富动态（核心修改）
 ====================== */
/* 1. 底层背景：缓慢漫游 + 轻微缩放（告别左右摇晃） */
@keyframes fluid-flow {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-2%, -2%) scale(1.02);
  }
  100% {
    transform: translate(2%, 2%) scale(1);
  }
}
/* 2. 中层渐变：全角度缓慢流动（动态更丰富） */
@keyframes fluid-gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
/* 3. 顶层光晕：呼吸 + 微位移（柔和不突兀） */
@keyframes fluid-breath {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0.7;
  }
  100% {
    transform: translateY(-1%) scale(1.01);
    opacity: 0.9;
  }
}

/* ======================
🔥 🔥 修复：暗黑模式背景（核心修改）
 ====================== */
html.dark {
  --el-text-color-primary: rgba(255,255,255,0.95);
  --el-text-color-secondary: rgba(255,255,255,0.8);
  --el-border-color: rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.9);
}
/* 修复暗黑背景图：提高亮度、降低模糊、保留色彩 */

/* 黑夜模式下的中层流体：转为深邃极光暗流 */
html.dark::after {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.6) 0%, rgba(30, 41, 59, 0.4) 50%, rgba(2, 6, 23, 0.7) 100%);
  mix-blend-mode: normal;
  opacity: 0.4;
}
/* 黑夜模式下的顶层呼吸光晕：转为深空微弱月光 */
html.dark body::before {
  background: radial-gradient(ellipse at top center, rgba(100, 181, 246, 0.15) 0%, transparent 70%);
  opacity: 0.85;
}
/* 浅色兜底 */
@media (prefers-color-scheme: light) {
  :root { color: #213547; }
}
/* 🔥 移动端全局适配（核心：隐藏背景 + APP铺满全屏） */
@media (max-width: 768px) {
  /* 1. 彻底隐藏手机端所有外部背景层（流体动画、渐变、光晕） */
  html::before,
  html::after,
  body::before {
    display: none !important;
  }

  /* 2. #app 铺满全屏，无任何留白/内边距 */
  #app {
    padding: 0 !important;
    margin: 0 !important;
    max-width: 100% !important;
    width: 100% !important;
    height: 100vh !important;
  }

  /* 3. 容器全屏适配 */
  .app-wrapper {
    padding: 0 !important;
    margin: 0 !important;
    width: 100% !important;
    height: 100vh !important;
  }

  /* 4. 弹窗/卡片合理适配，不破坏原有组件 */
  .el-dialog {
    width: 92vw !important;
    max-width: 400px !important;
  }
  .el-card {
    width: 100% !important;
  }

  /* 5. 修复高度，不挤压内容 */
  [style*="calc(100vh"] {
    min-height: 85vh !important;
    height: auto !important;
  }
}

::view-transition-old(root), ::view-transition-new(root) {
  animation: none;
  mix-blend-mode: normal;
}

/* ================== 🔥 悬浮氛围面板动画 ================== */
.weather-test-panel {
  position: fixed !important;
  /* 删去原来的 top:30px 和 right:30px，现在由JS动态内联接管 */
  z-index: 2000;
  max-width: 54px; /* ✅ 新增：收起时最大宽度锁定为 54px，维持圆形 */
  width: max-content; /* ✅ 新增：让宽度根据内部内容自适应撑开 */
  height: 54px;
  border-radius: 27px;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  white-space: nowrap;
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none; /* 拖拽时防止误选文字 */
}

/* 🔥 新增：边缘半隐藏状态下的微交互效果 */
.weather-test-panel.is-half-hidden {
  opacity: 0.7;
  box-shadow: none !important; /* 边缘收起时去除突兀的投影 */
}
/* 当鼠标移到半隐藏的面板上时，让它微微探出头暗示可点击 */
.weather-test-panel.is-half-hidden:hover {
  opacity: 1;
  transform: translateX(-5px);
}

.weather-test-panel.is-expanded {
  /* 展开状态：药丸形状（左右半圆） */
  max-width: 800px;
  height: 54px;
  border-radius: 27px; /* 依然是高度的一半，形成完美的左右半圆 */
  padding: 0 16px;
  cursor: default;
  background: var(--glass-bg); /* 展开时稍微加深一点背景 */
}

/* 折叠状态下的小图标（带轻微缩放动画） */
.trigger-icon {
  font-size: 24px;
  position: absolute;
  transition: all 0.4s ease;
}
.trigger-icon.hide {
  opacity: 0;
  transform: scale(0) rotate(180deg); /* 收起时旋转缩小消失 */
}

/* 展开状态下的内容容器 */
.expanded-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  opacity: 0;
  pointer-events: none;
  transform: translateX(20px);
  transition: all 0.4s ease 0.1s; /* 稍微延迟一点，等壳子拉开了再显示 */
  white-space: nowrap;
}
.expanded-content.show {
  opacity: 1;
  pointer-events: auto;
  transform: translateX(0);
}

/* 内部文字与收起按钮样式 */
.weather-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  margin-right: 12px;
}
.collapse-btn {
  width: 26px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(100, 100, 100, 0.1);
  color: var(--el-text-color-regular);
  font-size: 12px;
  cursor: pointer;
  margin-left: 12px;
  transition: all 0.2s;
}
.collapse-btn:hover {
  background: rgba(245, 108, 108, 0.2); /* 悬停时微微泛红 */
  color: #f56c6c;
  transform: rotate(90deg);
}

/* 适配暗黑模式的收起按钮 */
html.dark .collapse-btn {
  background: rgba(255, 255, 255, 0.1);
}
html.dark .collapse-btn:hover {
  background: rgba(245, 108, 108, 0.3);
}

/* 🔥 全局光绘层：悬浮在最高层，但完全穿透鼠标点击 */
.mouse-trail-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 99998; /* 放在弹窗之下，页面之上 */
  pointer-events: none; /* 绝对核心：穿透点击，绝不影响用户操作 */
}

/* ====================== 🚀 性能优化：全局动画息屏冻结 ====================== */
/* 当页面退到后台时，JS会赋予 body 这个类名。
   它会极其霸道地强制暂停全站所有的 CSS 动画和过渡效果，阻止后台 GPU 空转！ */
body.suspend-animations * {
  animation-play-state: paused !important;
  transition: none !important;
}

.vibe-mode-selector{
  cursor: pointer; display: flex; align-items: center;
}

/* =========================================================
   ✨ 左上角悬浮引导按钮 (液态魔法球)
   ========================================================= */
.liquid-guide-btn {
  position: fixed !important;
  top: 24px;
  left: 24px;
  z-index: 2000; /* 与你的天气面板层级保持一致 */
  width: 48px;
  height: 48px;
  border-radius: 50% !important; /* 强制覆盖成完美的圆形 */
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  cursor: pointer;
  padding: 0 !important;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  color: var(--el-text-color-primary);
  /* 背景和毛玻璃阴影已经被 glass-panel 类完美接管！ */
}

/* 🖱️ 悬停状态：魔法球微微上浮，内部发光 */
.liquid-guide-btn:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow:
      0 12px 24px rgba(0, 0, 0, 0.15),
      inset 0 2px 6px rgba(255, 255, 255, 0.9),
      0 0 20px rgba(100, 181, 246, 0.5) !important;
}

/* 👆 按压状态：按进水洼里的感觉 */
.liquid-guide-btn:active {
  transform: translateY(2px) scale(0.95);
}

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

.pop-up-icon {
  width: 120px;
  height: 120px;
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

/* 📱 手机端适配：避免挡住左滑返回手势 */
@media (max-width: 768px) {
  .liquid-guide-btn {
    top: 16px;
    left: 16px;
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}

/* =================================================================
   📱 悬浮天气面板 —— 手机端“双层折叠便当盒”终极适配
   ================================================================= */
@media (max-width: 768px) {
  /* 1. 外壳由“长棍”收缩为精致的“双层圆角卡片” */
  .weather-test-panel.is-expanded {
    max-width: 290px !important;  /* 严格锁死宽度，给手机左右留出优雅的悬浮空隙 */
    height: 82px !important;      /* 高度向下优雅撑开 */
    border-radius: 22px !important;
    padding: 8px 14px !important;
  }

  /* 🔥 2. 核心魔法：用 CSS Grid 把 1D 数组强行折叠成 2D 网格 */
  .weather-test-panel .expanded-content.show {
    display: grid !important;
    grid-template-columns: 1fr auto; /* 左边分给氛围，右边分给关闭键 */
    grid-template-rows: auto auto;   /* 上下共两行 */
    gap: 8px 0;                      /* 行间距 8px */
    width: 100%;
  }

  /* 第一行左侧：氛围切换器 */
  .weather-test-panel .vibe-mode-selector {
    grid-column: 1 / 2;
    grid-row: 1 / 2;
    align-self: center;
  }

  /* 第一行右侧：收起键 */
  .weather-test-panel .collapse-btn {
    grid-column: 2 / 3;
    grid-row: 1 / 2;
    margin-left: 0 !important;
    align-self: center;
  }

  /* 第二行霸道横跨：7个天气按钮组 */
  .weather-test-panel .el-button-group {
    grid-column: 1 / 3;
    grid-row: 2 / 3;
    display: flex;
    justify-content: space-between;
    width: 100%;
  }

  /* 极其克制地压缩 7 个小按钮的体积，完美适配胖手指 */
  .weather-test-panel .el-button-group .el-button {
    padding: 0 !important;
    width: 32px !important;
    height: 28px !important;
    font-size: 13px !important;
    border-radius: 6px !important; /* 微方一点的圆角在底层卡片里视觉更稳 */
  }
}
</style>