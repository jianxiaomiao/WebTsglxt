<template>
  <div class="auth-page ">
    <!-- 🔥 新增动态类，绑定黑夜模式 -->
    <el-form
        id="register-form"
        :model="registerForm"
        label-width="80px"
        class="auth-form glass-panel"
        :class="{ 'dark-form': isDark }"
        style="--el-form-item-margin-bottom: 15px;"
        :rules="registerRules"
        ref="registerFormRef"
    >
      <el-form-item label="登录邮箱" prop="userid">
        <el-input v-model="registerForm.userid" placeholder="请输入真实邮箱作为账号"
                  @keyup.enter="handleRegister"
        />
      </el-form-item>
      <el-form-item label="验证码" prop="emailCode">
        <div style="display: flex; gap: 10px; width: 100%;">
          <el-input
              v-model="registerForm.emailCode"
              placeholder="请输入邮箱验证码"
              style="flex: 1;"
              @keyup.enter="handleRegister"
          />
          <el-button
              type="primary"
              :disabled="countdown > 0"
              @click="sendEmailCode"
              style="width: 110px;"
          >
            {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <!-- 用户名 -->
      <el-form-item label="用户名" prop="username">
        <el-input v-model="registerForm.username" placeholder="请输入用户名"
                  @keyup.enter="handleRegister"
        />
      </el-form-item>

      <!-- 密码 -->
      <el-form-item label="密码" prop="password">
        <el-input
            v-model="registerForm.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="请输入密码"
            @keyup.enter="handleRegister"
        >
          <template #suffix>
            <el-icon @click="showPwd = !showPwd" style="cursor: pointer;">
              <View v-if="showPwd" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 确认密码 -->
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
            v-model="registerForm.confirmPassword"
            :type="showConfirmPwd ? 'text' : 'password'"
            placeholder="请再次输入密码"
            @keyup.enter="handleRegister"
        >
          <template #suffix>
            <el-icon @click="showConfirmPwd = !showConfirmPwd" style="cursor: pointer;">
              <View v-if="showConfirmPwd" />
              <Hide v-else />
            </el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 性别 -->
      <el-form-item label="性别" prop="sex">
        <el-select v-model="registerForm.sex" placeholder="请选择性别">
          <el-option label="未知" value="未知" />
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </el-form-item>

      <!-- 生日 -->
      <el-form-item label="生日" prop="birthday">
        <el-date-picker
            v-model="registerForm.birthday"
            type="date"
            placeholder="选择生日"
            style="width: 100%;"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <!-- 🔥 新增：图形验证码 -->
      <el-form-item id="register-form-captcha" label="验证码" prop="captcha">
        <div style="display: flex; gap: 10px; align-items: center;">
          <el-input v-model="registerForm.captcha" placeholder="请输入验证码" style="flex: 1;" />
          <canvas
              ref="captchaCanvas"
              width="120"
              height="40"
              style="border-radius: 4px; cursor: pointer; border: 1px solid #dcdfe6;"
              title="点击刷新验证码"
              @keyup.enter="handleRegister"
          ></canvas>
        </div>
      </el-form-item>

      <!-- 按钮区域 -->
      <el-form-item style="margin-top: 20px;">
        <div style="display: flex; gap: 10px; width: 100%;">
          <el-button
              type="default"
              style="flex: 1;"
              @click="$router.push('/login')"

              round
          >
            登录
          </el-button>
          <el-button
              type="primary"
              style="flex: 1;"
              @click="handleRegister"
              round
          >
            注册
          </el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton, ElIcon, ElDatePicker, ElMessage } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import request from '../utils/request.js'

const router = useRouter()
// 🔥 注入黑夜模式状态
const isDark = inject('isDark')

// 注册表单数据
const registerForm = reactive({
  userid: '',
  username: '',
  password: '',
  confirmPassword: '',
  sex: '未知',
  birthday: '',
  captcha: '',
  emailCode: '', // 新增
})

// 倒计时变量
const countdown = ref(0)
let timer = null

// 发送验证码方法
const sendEmailCode = async () => {
  // 先单独校验邮箱格式
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!registerForm.userid || !emailRegex.test(registerForm.userid)) {
    ElMessage.warning('请先输入正确的邮箱地址')
    return
  }

  try {
    // 开启倒计时，防止疯狂点击
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)

    // 调用后端发邮件接口
    const res = await request.post('/user/sendEmail', { email: registerForm.userid })

    if (res.code === 200) {
      ElMessage.success('验证码已发送，请查收邮件')
    } else {
      // 发送失败恢复按钮
      clearInterval(timer)
      countdown.value = 0
      ElMessage.error(res.msg || '发送失败')
    }
  } catch (error) {
    clearInterval(timer)
    countdown.value = 0
    ElMessage.error('网络异常，发送失败')
  }
}

// 密码显隐状态
const showPwd = ref(false)
const showConfirmPwd = ref(false)



// 表单校验规则
const registerRules = reactive({
  userid: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱收到的验证码', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z0-9]+$/, message: '用户名仅允许输入汉字、英文或数字', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
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
const registerFormRef = ref(null)

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

// 页面加载
onMounted(() => {
  // 初始化canvas上下文
  ctx = captchaCanvas.value.getContext('2d')
  // 首次加载验证码
  refreshCode()
  // 点击canvas刷新验证码
  captchaCanvas.value.onclick = () => {
    refreshCode()
    // 清空用户输入
    registerForm.captcha = ''
  }
})



// 注册逻辑
const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
  } catch (error) {
    ElMessage.warning('请完善表单信息')
    return
  }

  try {
    // 🔥 核心修改：追加 ?action=register，配合后端 doPost 里的分支路由
    const res = await request({
      url: '/user/auth?action=register',
      method: 'post',       // POST 请求
      data: registerForm,   // 数据包裹在 body 中（非明文 URL 传参，非常安全）
      timeout: 5000
    })

    if (res.code === 200) {
      ElMessage.success(res.msg || '注册成功，跳转登录页')
      setTimeout(() => router.push('/login'), 1500)
    } else {
      ElMessage.error(res.msg || '注册失败')
      // 注册失败时刷新验证码，防止暴力破解
      refreshCode()
      registerForm.captcha = ''
    }
  } catch (error) {
    console.error('注册失败详情：', error)
    ElMessage.error('网络异常，注册失败')
    // 异常时刷新验证码
    refreshCode()
    registerForm.captcha = ''
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

/* 🔥 黑夜模式表单背景（和登录页完全统一） */
.auth-form.dark-form {
  background: rgba(30, 30, 32, 0.8);
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.3) !important;
  box-shadow: none !important;
}

:deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.3) !important;
  box-shadow: none !important;
}
</style>