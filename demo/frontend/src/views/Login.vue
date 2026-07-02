<template>
  <div class="auth-page">
    <!-- 🔥 给表单加动态类，适配黑夜模式 -->
    <el-form
        id="login-form"
        :model="loginForm"
        label-width="80px"
        class="auth-form glass-panel"
        :class="{ 'dark-form glass-panel': isDark }"
        :rules="loginRules"
        ref="loginFormRef"
    >
      <!-- 用户ID -->
      <el-form-item label="用户ID" prop="userid">
        <el-input v-model="loginForm.userid" placeholder="请输入用户ID"
                  @keyup.enter="handleLogin"
        />
      </el-form-item>

      <!-- 密码（显示/隐藏功能） -->
      <el-form-item label="密码" prop="password">
        <el-input
            v-model="loginForm.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
        >
          <template #suffix>
            <el-icon @click="showPwd = !showPwd" style="cursor: pointer;">
              <View v-if="showPwd" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 用户类型 -->
      <el-form-item label="用户类型" prop="userType">
        <el-select v-model="loginForm.userType" placeholder="请选择用户类型">
          <el-option
              v-for="type in userTypeList"
              :key="type.id"
              :label="type.userType"
              :value="type.id"
          />
        </el-select>
      </el-form-item>

      <!-- 🔥 新增：图形验证码 -->
      <el-form-item  id="login-form-captcha" label="验证码" prop="captcha">
        <div style="display: flex; gap: 10px; align-items: center;">
          <el-input
              v-model="loginForm.captcha"
              placeholder="请输入验证码"
              style="flex: 1;"
              @keyup.enter="handleLogin"
          />
          <canvas
              ref="captchaCanvas"
              width="120"
              height="40"
              style="border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6;"
              title="点击刷新验证码"
          ></canvas>
        </div>
      </el-form-item>

      <!-- 在验证码表单项下方、按钮区域上方新增 -->
      <el-form-item>
        <el-checkbox v-model="rememberMe" label="记住我" style="margin-left: 8px;">
          自动登录（7天）
        </el-checkbox>
      </el-form-item>

      <!-- 按钮区域 -->
      <el-form-item style="margin-top: 20px;">
        <div style="display: flex; gap: 10px; width: 100%;">
          <!-- 🔥 登录按钮：用Element Plus内置暗黑模式自动适配，无需额外修改 -->
          <el-button
              type="primary"
              style="flex: 1;"
              @click="handleLogin"
              round
          >
            登录
          </el-button>
          <el-button
              type="default"
              style="flex: 1;"
              @click="$router.push('/register')"
              round
          >
            注册
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
  <Teleport to="body">
    <div v-if="isRainingCats" class="cat-rain-container">
      <div
          v-for="cat in fallingCats"
          :key="cat.id"
          class="falling-cat"
          :style="{
          left: cat.left,
          fontSize: cat.fontSize,
          animationDuration: cat.duration,
          animationDelay: cat.delay,
          '--start-rot': cat.startRotate,
          '--end-rot': cat.endRotate
        }"
      >
        {{ cat.emoji }}
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import {ref, reactive, onMounted, inject} from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton, ElIcon, ElMessage } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import request from '../utils/request.js'
// 🔥 1. 新增：引入 Pinia 用户仓库
import { useUserStore } from '../stores/userStore'
const router = useRouter()
const isDark = inject('isDark')
// 🔥 2. 新增：创建 store 实例
const userStore = useUserStore()

const rememberMe = ref(false) // 记住我状态
// 登录表单数据
const loginForm = reactive({
  userid: '',
  password: '',
  userType: '',
  captcha: ''
})
// 密码显隐状态
const showPwd = ref(false)
// 存储后端返回的用户类型列表
const userTypeList = ref([])

// 表单校验规则
const loginRules = reactive({
  userid: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value.toLowerCase() !== captchaCode.value.toLowerCase()) {
          callback(new Error('验证码错误'))
        } else {
          callback()
        }
      }, trigger: 'blur' }
  ]
})
const loginFormRef = ref(null)

// 🔥 验证码核心变量
const captchaCanvas = ref(null)
let ctx = null
const captchaCode = ref('') // 存储生成的验证码

// 🔥 增强版：生成随机验证码（4位，大小写+数字）
const generateCode = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    const randomIndex = Math.floor(Math.random() * chars.length)
    code += chars[randomIndex]
  }
  return code
}

// 🔥 增强版：绘制背景噪点
const drawNoise = () => {
  for (let i = 0; i < 30; i++) {
    ctx.beginPath()
    ctx.arc(
        Math.random() * 120,
        Math.random() * 40,
        Math.random() * 2,
        0,
        2 * Math.PI
    )
    ctx.fillStyle = `rgba(${Math.random()*255},${Math.random()*255},${Math.random()*255},0.5)`
    ctx.fill()
  }
}

// 🔥 增强版：绘制干扰线
const drawLines = () => {
  for (let i = 0; i < 3; i++) {
    ctx.beginPath()
    ctx.moveTo(Math.random() * 120, Math.random() * 40)
    ctx.lineTo(Math.random() * 120, Math.random() * 40)
    ctx.strokeStyle = `rgba(${Math.random()*255},${Math.random()*255},${Math.random()*255},0.6)`
    ctx.lineWidth = 1
    ctx.stroke()
  }
}

// 🔥 增强版：刷新验证码（带扭曲/旋转/干扰）
const refreshCode = () => {
  // 清空画布
  ctx.clearRect(0, 0, 120, 40)
  // 1. 绘制背景
  ctx.fillStyle = '#f5f7fa'
  ctx.fillRect(0, 0, 120, 40)
  // 2. 绘制噪点和干扰线
  drawNoise()
  drawLines()
  // 3. 生成并存储验证码
  captchaCode.value = generateCode()
  // 4. 逐个绘制字符（随机位置/旋转/颜色/字体）
  const fonts = ['Arial', 'Verdana', 'Times New Roman', 'Courier New']
  for (let i = 0; i < captchaCode.value.length; i++) {
    ctx.beginPath()
    // 随机字体
    ctx.font = `${Math.floor(Math.random() * 10) + 22}px ${fonts[Math.floor(Math.random() * fonts.length)]}`
    // 随机颜色
    ctx.fillStyle = `rgb(${Math.floor(Math.random()*100)},${Math.floor(Math.random()*100)},${Math.floor(Math.random()*100)})`
    // 随机旋转角度（±15度）
    const angle = (Math.random() - 0.5) * 0.3
    ctx.save()
    ctx.translate(25 + i * 22, 25)
    ctx.rotate(angle)
    // 随机扭曲位置
    ctx.fillText(captchaCode.value[i], (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4)
    ctx.restore()
  }
}

// 页面加载：动态获取用户类型
onMounted(() => {
  fetchUserTypes()
  // 初始化canvas上下文
  ctx = captchaCanvas.value.getContext('2d')
  // 首次加载验证码
  refreshCode()
  // 点击canvas刷新验证码
  captchaCanvas.value.onclick = () => {
    refreshCode()
    // 清空用户输入
    loginForm.captcha = ''
  }
})

// 获取用户类型接口
const fetchUserTypes = async () => {
  try {
    const res = await request.get('user/type')
    if (res.code === 200) {
      userTypeList.value = res.data
    }
  } catch (error) {
    ElMessage.error('用户类型加载失败')
    console.error('获取用户类型失败：', error)
  }
}

// 🔥 1. 增加猫咪雨的相关状态
const isRainingCats = ref(false)
const fallingCats = ref([])
const catEmojis = ['😺', '😸', '😹', '😻', '😼', '😽', '🙀', '😿', '😾']

// 生成猫咪雨的数据
const startCatRain = () => {
  isRainingCats.value = true
  const cats = []
  // 一次性生成 60 只猫咪
  for (let i = 0; i < 60; i++) {
    cats.push({
      id: i,
      emoji: catEmojis[Math.floor(Math.random() * catEmojis.length)],
      left: Math.random() * 100 + 'vw', // 随机水平位置
      fontSize: (Math.random() * 40 + 50) + 'px', // 让猫咪大小在 50px 到 90px 之间随机错落！
      duration: (Math.random() * 1.5 + 1) + 's', // 坠落时间 1s - 2.5s，错落有致
      delay: (Math.random() * 0.5) + 's', // 随机延迟一点点下落
      startRotate: (Math.random() * 360) + 'deg', // 初始随机角度
      endRotate: (Math.random() * 360 + 360) + 'deg' // 掉落时还会翻滚
    })
  }
  fallingCats.value = cats
}

// 登录逻辑
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
  } catch (error) {
    ElMessage.warning('请完善登录信息')
    return
  }

  try {
    // 合并表单 + 记住我 字段，作为 POST 请求体
    const loginData = {
      ...loginForm,
      rememberMe: rememberMe.value
    }
    // 标准写法：request.post(接口地址, 请求体data)
    const res = await request.post('user/auth?action=login', loginData)

    if (res.code === 200) {
      // 🔥 3. 核心修改：保存用户到 Pinia + 去掉路由传参
      userStore.login(res.data)
      ElMessage.success({ message: '登录成功，正在进入书屋...', duration: 2000 })

      // 🔥 核心修改：先播放猫咪雨动画
      startCatRain()

      // 延迟 1.5 秒，让满屏猫咪下得正欢的时候，突然跳转，无缝衔接主页初始化！
      setTimeout(() => {
        router.push('/home')
      }, 1500)
    }
    else {
      refreshCode()
      loginForm.captcha = ''
    }
  } catch (error) {
    console.error('登录失败详情：', error)
    refreshCode()
    loginForm.captcha = ''
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
  background: transparent;
}
.auth-form {
  width: 100%;
  max-width: 400px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.3) !important;
  box-shadow: none !important;
}

:deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.3) !important;
  box-shadow: none !important;
}

/* 🔥 黑夜模式表单背景：和你的页面背景完全适配 */
.auth-form.dark-form {
  background: rgba(30, 30, 32, 0.8);
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
}

/* 适配暗黑模式的canvas边框 */
:root.dark .auth-form canvas {
  border-color: #404040 !important;
  background: #2d3748 !important;
}

/* ====================== 🔥 猫咪雨动画特效 ====================== */
.cat-rain-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none; /* 绝对不能阻挡点击 */
  z-index: 999999; /* 铺在整个系统最顶层 */
  overflow: hidden;
  background: rgba(255, 255, 255, 0.1); /* 给一点点微弱的白雾感 */
  backdrop-filter: blur(2px); /* 瞬间让背后的登录面板微微模糊，凸显加载状态 */
}

.falling-cat {
  position: absolute;
  top: -10vh; /* 初始位置藏在屏幕上方 */
  will-change: transform; /* 开启 GPU 加速，60只猫咪掉落也绝对不卡 */
  animation-name: cat-fall;
  animation-timing-function: cubic-bezier(0.25, 0.46, 0.45, 0.94); /* 模仿重力加速度 */
  animation-fill-mode: forwards;
}

@keyframes cat-fall {
  0% {
    transform: translateY(0) rotate(var(--start-rot));
    opacity: 0;
  }
  10% {
    opacity: 1; /* 刚掉下来时显现实体 */
  }
  90% {
    opacity: 1;
  }
  100% {
    /* 掉到屏幕外，并且翻滚 */
    transform: translateY(120vh) rotate(var(--end-rot));
    opacity: 0;
  }
}
</style>