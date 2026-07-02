<template>
  <div class="weather-wrapper" :class="weather">
    <transition name="fade" mode="out-in">
      <div v-if="weather === 'sunny'" key="sunny" class="celestial sun">☀️</div>
      <div v-else-if="weather === 'cloudy'&& !isDark" key="sun-cloudy" class="celestial sun">🌥️</div>
      <div v-else-if="weather === 'cloudy'&& isDark" key="moon-cloudy" class="celestial moon">🌚</div>

      <div v-else-if="weather === 'rain' && !isDark" key="sun-rain" class="celestial sun">🌤️</div>
      <div v-else-if="weather === 'rain' && isDark" key="moon-rain" class="celestial moon">🌕</div>

      <div v-else-if="weather === 'wind' && !isDark" key="sun-wind" class="celestial sun">🌞</div>
      <div v-else-if="weather === 'wind' && isDark" key="moon-wind" class="celestial moon">🌖</div>

      <div v-else-if="weather === 'snow' && !isDark" key="sun-snow" class="celestial sun">🌥️</div>
      <div v-else-if="weather === 'snow' && isDark" key="moon-snow" class="celestial moon">🌑</div>

      <div v-else-if="weather === 'night'" key="night" class="celestial moon">🌙</div>
    </transition>

    <transition name="fade" mode="out-in">
      <div v-if="['sunny', 'cloudy', 'night'].includes(weather)" key="normal-clouds" class="clouds">
        <div class="cloud cloud-1">☁️</div>
        <div class="cloud cloud-2">☁️</div>
        <div class="cloud cloud-3">☁️</div>
        <div class="cloud cloud-4">☁️</div>
      </div>

      <div v-else-if="weather === 'rain'" key="rain-clouds" class="clouds">
        <div class="cloud cloud-1">🌧️</div>
        <div class="cloud cloud-2">🌨️</div>
        <div class="cloud cloud-3">☁️</div>
        <div class="cloud cloud-4">🌧️</div>
      </div>

      <div v-else-if="weather === 'thunder'" key="thunder-clouds" class="clouds">
        <div class="cloud cloud-1">🌧️</div>
        <div class="cloud cloud-2">
          🌩️
          <div class="lightning-strike">⚡</div>
        </div>
        <div class="cloud cloud-3">🌧️</div>
        <div class="cloud cloud-4">
          🌩️
          <div class="lightning-strike delay">⚡</div>
        </div>
      </div>

      <div v-else-if="weather === 'wind'" key="wind-clouds" class="clouds">
        <div class="cloud cloud-1">🌀</div>
        <div class="cloud cloud-2">🌀</div>
        <div class="cloud cloud-3">🌀</div>
        <div class="cloud cloud-4">🌀</div>
      </div>

      <div v-else-if="weather === 'snow'" key="snow-clouds" class="clouds">
        <div class="cloud cloud-1">🌨️</div>
        <div class="cloud cloud-2">🌨️</div>
        <div class="cloud cloud-3">🌨️</div>
        <div class="cloud cloud-4">🌨️</div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="['rain', 'thunder'].includes(weather)" class="rain-container">
        <div v-for="i in 30" :key="i" class="raindrop" :style="getRainStyle(i)"></div>
      </div>
    </transition>
    <div v-if="weather === 'thunder'" class="lightning-flash"></div>

    <transition name="fade">
      <div v-if="['sunny', 'cloudy'].includes(weather) && !isDark" class="leaf-container">
        <div v-for="i in 15" :key="'leaf-'+i" class="leaf" :style="getLeafStyle(i)">🍃</div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="(weather === 'night' || (weather === 'cloudy' && isDark))" class="star-container">
        <div v-for="i in 30" :key="'star-'+i" class="star" :style="getStarStyle(i)">⭐</div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="weather === 'wind'" class="wind-container">
        <div v-for="i in 18" :key="'wind-leaf-'+i" class="wind-leaf" :style="getWindLeafStyle(i)">
          {{ isDark ? '🍂' : '🍃' }}
        </div>
        <div class="tornado-bottom">🌪️</div>
      </div>
    </transition>

    <transition name="fade">
      <div v-if="weather === 'snow'" class="snow-container">
        <template v-if="!isDark">
          <div v-for="i in 25" :key="'snow-'+i" class="snow-flake" :style="getSnowStyle(i)">❄️</div>
        </template>
        <template v-else>
          <div v-for="i in 15" :key="'snowman-'+i" class="star snowman-star" :style="getSnowmanStyle(i)">☃️</div>
        </template>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { defineProps, inject } from 'vue'

const props = defineProps({
  weather: {
    type: String,
    default: 'sunny'
  }
})

// 注入 App.vue 提供的黑夜模式状态
const isDark = inject('isDark')

// 生成雨滴随机样式
const getRainStyle = (index) => {
  const left = Math.random() * 100
  const delay = Math.random() * 2
  const duration = 0.5 + Math.random() * 0.3
  return {
    left: `${left}vw`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

// 🔥 新增：生成落叶的随机样式（分布在屏幕中右侧及上方）
const getLeafStyle = (index) => {
  const left = 30 + Math.random() * 90 // 从屏幕 30% 到右侧外 120%
  const top = -10 - Math.random() * 20 // 初始高度在屏幕外部上方
  const delay = Math.random() * 15 // 拉长延迟时间，避免同时大面积掉落
  const duration = 8 + Math.random() * 7 // 下落时间比较缓慢 (8-15秒)
  const scale = 0.5 + Math.random() * 0.6 // 大小错落

  return {
    left: `${left}vw`,
    top: `${top}vh`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    transform: `scale(${scale})`
  }
}

// 🔥 新增：生成繁星的随机样式（铺满整个夜空）
const getStarStyle = (index) => {
  const left = Math.random() * 100 // 水平随机
  const top = Math.random() * 70   // 垂直随机，集中在上半部分
  const delay = Math.random() * 6  // 随机闪烁起步时间
  const duration = 2.5 + Math.random() * 3 // 闪烁一次 2.5-5.5秒
  const scale = 0.3 + Math.random() * 0.5 // 星星要小一点才精致

  return {
    left: `${left}vw`,
    top: `${top}vh`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    transform: `scale(${scale})`
  }
}
// 🔥 大风落叶生成（起点设在屏幕极左侧）
const getWindLeafStyle = (i) => {
  return {
    left: `${-20 - Math.random() * 20}vw`, // 屏幕左边外面
    top: `${Math.random() * 80}vh`,
    animationDuration: `${3 + Math.random() * 3}s`, // 风比较大，飘得快
    animationDelay: `${Math.random() * 4}s`,
    fontSize: `${16 + Math.random() * 12}px`
  }
}

// 🔥 雪花飘落生成（起点在右上侧）
const getSnowStyle = (i) => {
  return {
    left: `${40 + Math.random() * 80}vw`, // 靠右
    top: `${-10 - Math.random() * 20}vh`,
    animationDuration: `${5 + Math.random() * 5}s`, // 雪花慢悠悠
    animationDelay: `${Math.random() * 5}s`,
    fontSize: `${12 + Math.random() * 14}px`
  }
}

// 🔥 雪人闪烁生成
const getSnowmanStyle = (i) => {
  return {
    left: `${Math.random() * 95}vw`,
    top: `${Math.random() * 95}vh`,
    animationDuration: `${1.5 + Math.random() * 2}s`,
    animationDelay: `${Math.random() * 2}s`,
    fontSize: `${16 + Math.random() * 10}px`
  }
}

</script>

<style scoped>
.weather-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: -2;
  overflow: hidden;
  transition: all 1s ease;
}

.weather-wrapper.thunder {
  background-color: rgba(15, 23, 42, 0.4);
}

/* ================== 天体与云朵动画 (保持不变) ================== */
.celestial {
  position: absolute;
  font-size: 120px;
  top: 5%;
  right: 10%;
  animation: float-celestial 6s ease-in-out infinite alternate;
}
.sun { filter: drop-shadow(0 0 60px rgba(253, 224, 71, 0.6)); }
.moon { filter: drop-shadow(0 0 50px rgba(226, 232, 240, 0.5)); }

@keyframes float-celestial {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(-15px) scale(1.05); }
}

.clouds {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0.8;
}
.cloud {
  position: absolute;
  font-size: 100px;
  filter: drop-shadow(0 10px 15px rgba(0, 0, 0, 0.1));
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cloud-1 { top: 10%; left: -20%; font-size: 140px; animation: drift 40s linear infinite; }
.cloud-2 { top: 25%; left: -20%; font-size: 90px; animation: drift 35s linear infinite 10s; opacity: 0.8; }
.cloud-3 { top: 5%; left: -20%; font-size: 110px; animation: drift 45s linear infinite 5s; opacity: 0.9; }
.cloud-4 { top: 18%; left: -20%; font-size: 160px; animation: drift 50s linear infinite 20s; opacity: 0.7; }

@keyframes drift {
  0% { transform: translateX(-15vw); }
  100% { transform: translateX(120vw); }
}

.thunder .cloud { filter: brightness(0.6) drop-shadow(0 10px 15px rgba(0,0,0,0.5)); }

.lightning-strike {
  position: absolute;
  top: 60%;
  font-size: 1.2em;
  opacity: 0;
  filter: drop-shadow(0 0 30px rgba(255, 255, 255, 1));
  animation: flash-strike 6s infinite;
}
.lightning-strike.delay { animation-delay: 3s; }

@keyframes flash-strike {
  0%, 95%, 100% { opacity: 0; transform: translateY(-20px) scale(0.8); }
  96% { opacity: 1; transform: translateY(0) scale(1.1); }
  98% { opacity: 1; transform: translateY(0) scale(1.2); }
}

/* ================== 雨滴背景 (保持不变) ================== */
.rain-container {
  position: absolute;
  width: 100%;
  height: 100%;
}
.raindrop {
  position: absolute;
  top: -10%;
  width: 2px;
  height: 80px;
  background: linear-gradient(to bottom, rgba(255,255,255,0), rgba(255,255,255,0.6));
  animation: fall linear infinite;
}
.thunder .raindrop { background: linear-gradient(to bottom, rgba(255,255,255,0), rgba(148, 163, 184, 0.8)); }

@keyframes fall {
  0% { transform: translateY(-10vh) rotate(10deg); opacity: 0; }
  10% { opacity: 1; }
  100% { transform: translateY(110vh) rotate(10deg); opacity: 0; }
}

.lightning-flash {
  position: absolute;
  width: 100%;
  height: 100%;
  background-color: white;
  opacity: 0;
  animation: flash-bg 6s infinite;
}
@keyframes flash-bg {
  0%, 95%, 100% { opacity: 0; }
  96% { opacity: 0.2; }
  97% { opacity: 0; }
  98% { opacity: 0.5; }
  99% { opacity: 0; }
}

/* ================== 🔥 新增：落叶飞舞效果 ================== */
.leaf-container {
  position: absolute;
  width: 100%;
  height: 100%;
}

.leaf {
  position: absolute;
  font-size: 24px;
  opacity: 0;
  /* 加上极其柔和的阴影，让它看起来更立体 */
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.15));
  animation: leaf-drift linear infinite;
  /* 开启 3D 加速让掉落更顺滑 */
  will-change: transform, opacity;
}

@keyframes leaf-drift {
  0% {
    opacity: 0;
    /* 初始状态：未旋转 */
    transform: translate(0, 0) rotate3d(1, 1, 1, 0deg);
  }
  15% {
    opacity: 0.8; /* 渐渐浮现 */
  }
  85% {
    opacity: 0.8;
  }
  100% {
    opacity: 0;
    /* 核心位移：往左侧飘动（-60vw），往下飘动（110vh），同时 X/Y/Z 轴都在 3D 打转！ */
    transform: translate(-60vw, 110vh) rotate3d(1, 2, 1, 1080deg);
  }
}

/* ================== 🔥 新增：繁星闪烁效果 ================== */
.star-container {
  position: absolute;
  width: 100%;
  height: 100%;
}

.star {
  position: absolute;
  font-size: 20px; /* 原尺寸大一点点，靠 JS scale() 错落缩放 */
  opacity: 0;
  /* 核心特效：用多重 drop-shadow 模拟边缘浅浅的黄色光晕 */
  filter: drop-shadow(0 0 6px rgba(253, 224, 71, 0.4)) drop-shadow(0 0 12px rgba(253, 224, 71, 0.2));
  animation: star-twinkle ease-in-out infinite;
  will-change: transform, opacity, filter;
}

@keyframes star-twinkle {
  0% {
    opacity: 0;
    transform: scale(0.6);
  }
  50% {
    opacity: 0.8; /* 闪烁至最亮 */
    transform: scale(1.2); /* 微微放大模拟光晕扩散 */
    /* 最高潮时光晕更亮更清晰 */
    filter: drop-shadow(0 0 8px rgba(253, 224, 71, 0.8)) drop-shadow(0 0 16px rgba(253, 224, 71, 0.4));
  }
  100% {
    opacity: 0;
    transform: scale(0.6);
  }
}

.fade-enter-active, .fade-leave-active { transition: opacity 1.5s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>