<template>
  <div class="settings-view-container">
    <div class="settings-header glass-panel">
      <h2>⚙️ 沉浸式空间 · 控制中心</h2>
      <p>自由调配你的书屋磁场，所有偏好已为您云静默固化</p>
    </div>

    <div class="bento-grid-wrapper">

      <div class="bento-box glass-panel widgets-bento">
        <div class="bento-title">
          <span class="icon">🛸</span>
          <h3>桌面悬浮矩阵 (Widgets)</h3>
        </div>
        <p class="bento-desc">关闭暂时不需要的灵感接收器，获取极简阅读视野</p>

        <div class="switch-list">
          <div class="switch-item glass-panel">
            <span>🐱 专属读书小宠物</span>
            <el-switch v-model="settings.widgets.showPet" />
          </div>
          <div class="switch-item glass-panel">
            <span>☕ 书籍共读胶囊舱</span>
            <el-switch v-model="settings.widgets.showCoRead" />
          </div>
          <div class="switch-item glass-panel">
            <span>💡 沉浸式功能引导球</span>
            <el-switch v-model="settings.widgets.showGuideBtn" />
          </div>
          <div class="switch-item glass-panel">
            <span>🌤️ 实时天气</span>
            <el-switch v-model="settings.widgets.showWeather" />
          </div>
          <div class="switch-item glass-panel">
            <span>🌞 天气面板</span>
            <el-switch v-model="settings.widgets.showWeatherPane" />
          </div>
          <div class="switch-item glass-panel">
            <span>🍾 灵感漂流瓶系统</span>
            <el-switch v-model="settings.widgets.showBottle" />
          </div>
          <div class="switch-item glass-panel">
            <span>📖 知识图谱面板</span>
            <el-switch v-model="settings.widgets.showGraph" />
          </div>
        </div>
      </div>

      <div class="bento-box glass-panel wallpaper-bento">
        <div class="bento-title">
          <span class="icon">🌌</span>
          <h3>全息壁纸引擎 (Wallpaper Engine)</h3>
        </div>
        <p class="bento-desc">选择官方意境预设，或上传你心底的白月光风景</p>

        <el-divider content-position="left">官方灵感池</el-divider>
        <div class="preset-wallpaper-grid">
          <div
              v-for="item in settings.presetWallpapers"
              :key="item.id"
              class="wallpaper-card glass-panel"
              :class="{ 'is-active': settings.wallpaper.url === item.url }"
              @click="applyPresetBg(item)"
          >
            <div class="bg-thumb" :style="{ backgroundImage: `url(${item.url})` }"></div>
            <div class="card-meta">
              <span>{{ item.preview }} {{ item.name }}</span>
              <el-icon v-if="settings.wallpaper.url === item.url"><Check /></el-icon>
            </div>
          </div>
        </div>

        <el-divider content-position="left" style="margin-top: 24px;">自定义磁场</el-divider>
        <div class="custom-bg-uploader glass-panel">
          <el-input
              v-model="customInputUrl"
              placeholder="粘贴任意高清网络图片 URL..."
              clearable
          >
            <template #append>
              <el-button type="primary" @click="applyCustomUrlBg">应用链接</el-button>
            </template>
          </el-input>

          <div class="upload-or-divider"><span>或</span></div>

          <label class="local-file-trigger glass-panel">
            <input type="file" accept="image/*" @change="handleLocalImageUpload" hidden />
            <span>📁 从本地设备上传高清壁纸...</span>
          </label>
        </div>
      </div>

      <div class="bento-box glass-panel danger-bento">
        <div class="bento-title">
          <span class="icon">⚡</span>
          <h3>系统磁场重置</h3>
        </div>
        <p class="bento-desc" style="margin-bottom: 16px;">如果你把界面调得太乱，可以一键回归初始清晨书香状态</p>
        <el-button type="danger" round @click="handleResetAll" style="width: 100%;">
          恢复默认出厂磁场 💥
        </el-button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {useAppSettingsStore} from "../stores/appSettingsStore.js";
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const settings = useAppSettingsStore()
const customInputUrl = ref('')

const applyPresetBg = (preset) => {
  settings.wallpaper.type = 'preset'
  settings.wallpaper.url = preset.url
  ElMessage.success(`已切换空间意境：${preset.name}`)
}

const applyCustomUrlBg = () => {
  if (!customInputUrl.value.trim()) return ElMessage.warning('链接不能为空哦')
  settings.wallpaper.type = 'custom'
  settings.wallpaper.url = customInputUrl.value.trim()
  ElMessage.success('自定义网络磁场连接成功！')
}

// 🔥 将用户本地图片转成 Base64 字符串直接塞进 localStorage
const handleLocalImageUpload = (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 2.5 * 1024 * 1024) {
    return ElMessage.warning('为了本地缓存顺畅，请上传小于 2.5MB 的图片哦')
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    const base64Img = event.target.result
    settings.wallpaper.type = 'custom'
    settings.wallpaper.url = base64Img
    ElMessage.success('本地壁纸已注入空间灵魂！')
  }
  reader.readAsDataURL(file)
}

const handleResetAll = () => {
  ElMessageBox.confirm('确定要抹除当前设备的所有自定义布局偏好吗？', '提示', { type: 'warning' })
      .then(() => {
        settings.resetAll()
        ElMessage.success('已恢复初始出厂设置')
      }).catch(()=>{})
}
</script>

<style scoped>
.settings-view-container { text-align: left; padding-bottom: 40px; }
.settings-header { padding: 24px 30px; border-radius: 20px; margin-bottom: 24px; }
.settings-header h2 { margin: 0 0 8px 0; color: #409eff; }
.settings-header p { margin: 0; color: var(--el-text-color-regular); font-size: 14px; }

.bento-grid-wrapper {
  display: grid;
  grid-template-columns: 1.1fr 1.3fr;
  gap: 24px;
}

.bento-box { padding: 24px; border-radius: 24px; display: flex; flex-direction: column; }
.bento-title { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.bento-title h3 { margin: 0; font-size: 18px; }
.bento-desc { margin: 0 0 20px 0; font-size: 12px; color: #888; }

/* 开关列表 */
.widgets-bento { grid-row: span 2; }
.switch-list { display: flex; flex-direction: column; gap: 12px; }
.switch-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-radius: 14px; font-size: 14px; font-weight: 600; background: rgba(255,255,255,0.08) !important;}

/* 壁纸卡片 */
.preset-wallpaper-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.wallpaper-card { overflow: hidden; border-radius: 16px; cursor: pointer; border: 2px solid transparent; transition: all 0.25s; }
.wallpaper-card:hover { transform: translateY(-3px); border-color: rgba(64,158,255,0.5); }
.wallpaper-card.is-active { border-color: #409eff; box-shadow: 0 0 15px rgba(64,158,255,0.4); }
.bg-thumb { height: 100px; background-size: cover; background-position: center; transition: transform 0.3s; }
.wallpaper-card:hover .bg-thumb { transform: scale(1.05); }
.card-meta { padding: 8px 12px; font-size: 12px; font-weight: bold; display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.4); color: #fff; }

.custom-bg-uploader { padding: 16px; border-radius: 16px; background: rgba(255,255,255,0.05) !important; text-align: center;}
.upload-or-divider { margin: 12px 0; font-size: 12px; color: #888; }
.local-file-trigger { display: block; padding: 12px; border-radius: 12px; cursor: pointer; font-size: 13px; font-weight: bold; color: #409eff; border: 1px dashed rgba(64,158,255,0.5); transition: all 0.2s; }
.local-file-trigger:hover { background: rgba(64,158,255,0.1) !important; border-style: solid; }

.danger-bento { border-left: 4px solid #f56c6c; }

@media (max-width: 768px) {
  .bento-grid-wrapper { grid-template-columns: 1fr; }
}
</style>