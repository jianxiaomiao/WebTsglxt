<template>
  <Teleport to="body">
    <Transition name="audiobook-fade">
      <div
          v-if="visible"
          class="audiobook-float-panel glass-panel"
          :class="{ 'dark-mode': isDark, 'is-dragging': isDragging }"
          :style="dynamicPanelStyle"
          @mousedown="onDragStart"
          @click.stop
      >
        <div class="panel-header drag-handle">
          <div class="header-title">
            <span class="icon">🎧</span>
            <span class="title-text">《{{ bookName || '未知书籍' }}》第{{ chapterNumber }}章</span>
            <ElTag size="small" effect="plain" type="info" round class="status-tag">智能有声书</ElTag>
          </div>
          <div class="header-actions no-drag">
            <ElButton circle size="small" icon="Close" @click="closePlayer" />
          </div>
        </div>

        <div v-if="!isGenerated && !isGenerating" class="empty-guide-wrap no-drag">
          <div class="guide-content">
            <div class="guide-emoji">🎙️</div>
            <div class="guide-title">本章尚未生成 AI 有声书</div>
            <div class="guide-desc">大模型将自动通读本章，智能标注文本角色与情绪，并合成多音色剧播音频</div>
            <ElButton
                type="primary"
                round
                size="large"
                class="generate-btn glass-button-primary"
                :loading="isGenerating"
                @click="handleStartGenerate"
            >
              ✨ 立即生成有声书
            </ElButton>
            <ElButton
                type="warning"
                round
                size="large"
                class="generate-btn"
                :loading="isBatchGenerating"
                @click="handleBatchGenerateAll"
                style="margin-top: 10px;"
            >
              📚 批量生成全书
            </ElButton>
          </div>
        </div>

        <div v-else class="player-body no-drag">
          <div v-if="isGenerating || progressPercent < 100" class="progress-notice-bar">
            <div class="progress-info">
              <span>正在智能标注与合成剧播音频...</span>
              <span class="percent">{{ progressPercent }}% ({{ generatedCount }}/{{ totalCount }})</span>
            </div>
            <ElProgress :percentage="progressPercent" :show-text="false" :stroke-width="6" status="success" />
          </div>

          <div class="main-player-console glass-sub-panel">
            <div class="slider-container">
              <ElSlider
                  v-model="currentAudioTime"
                  :max="currentDuration || 100"
                  :format-tooltip="formatSeconds"
                  :disabled="!currentSegment || isAudioLoading"
                  @change="handleSliderSeek"
              />
              <div class="time-display">
                <span>{{ formatSeconds(currentAudioTime) }}</span>
                <span>{{ formatSeconds(currentDuration) }}</span>
              </div>
            </div>

            <div class="controls-row">
<!--              <div class="left-tools">
                <ElDropdown trigger="click" @command="handleVoiceChange">
                  <ElButton size="small" plain round class="tool-btn">
                    🗣️ {{ currentVoiceLabel }}
                  </ElButton>
                  <template #dropdown>
                    <ElDropdownMenu>
                      <ElDropdownItem v-for="v in voiceOptions" :key="v.val" :command="v.val">
                        {{ v.label }}
                      </ElDropdownItem>
                    </ElDropdownMenu>
                  </template>
                </ElDropdown>
              </div>-->

              <div class="center-play-group">
                <ElButton circle size="default" @click="playPrevSegment" :disabled="currentIndex <= 0">⏮</ElButton>
                <ElButton
                    circle
                    type="primary"
                    size="large"
                    class="play-toggle-btn"
                    :loading="isAudioLoading"
                    @click="togglePlay"
                >
                  {{ isPlaying ? '⏸' : '▶' }}
                </ElButton>
                <ElButton circle size="default" @click="playNextSegment" :disabled="currentIndex >= segmentList.length - 1">⏭</ElButton>
              </div>

              <div class="right-tools">
                <ElDropdown trigger="click" @command="handleSpeedChange">
                  <ElButton size="small" plain round class="tool-btn">🐢 {{ playbackRate }}x</ElButton>
                  <template #dropdown>
                    <ElDropdownMenu>
                      <ElDropdownItem :command="0.75">0.75x 慢速</ElDropdownItem>
                      <ElDropdownItem :command="1.0">1.0x 正常</ElDropdownItem>
                      <ElDropdownItem :command="1.25">1.25x 加速</ElDropdownItem>
                      <ElDropdownItem :command="1.5">1.5x 加速</ElDropdownItem>
                      <ElDropdownItem :command="2.0">2.0x 极速</ElDropdownItem>
                    </ElDropdownMenu>
                  </template>
                </ElDropdown>
              </div>
            </div>
          </div>

          <div class="segments-list-container" ref="listContainerRef">
            <ElSkeleton v-if="isLoadingList" :rows="5" animated />

            <div
                v-else
                v-for="(seg, idx) in segmentList"
                :key="seg.id || idx"
                :id="`seg-item-${idx}`"
                class="segment-item"
                :class="{ 'is-active': currentIndex === idx, 'is-error': seg.status === 2 }"
                @click="jumpToSegment(idx)"
            >
              <div class="seg-header">
                <div class="role-info">
                  <span class="role-emoji">{{ getRoleEmoji(seg.roleType) }}</span>
                  <span class="role-name">{{ seg.roleType || '旁白' }}</span>
                  <span class="emotion-badge">{{ getEmotionEmoji(seg.emotion) }} {{ seg.emotion || '平静' }}</span>
                </div>
                <div class="seg-status">
                  <span v-if="currentIndex === idx && isPlaying" class="playing-indicator">🎵 播放中</span>
                  <span v-else class="duration">{{ formatSeconds(seg.audioDuration) }}</span>
                </div>
              </div>
              <div class="seg-content">{{ seg.paragraphContent || seg.textContent || '(文本内容加载中...)' }}</div>
            </div>
          </div>
        </div>

        <audio
            ref="nativeAudioRef"
            @timeupdate="onAudioTimeUpdate"
            @ended="onAudioEnded"
            @error="onAudioError"
            @canplay="onAudioCanPlay"
        ></audio>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useUserStore } from '../stores/userStore'

const props = defineProps({
  visible: { type: Boolean, default: false },
  isbn: { type: String, default: '' },
  chapterNumber: { type: Number, default: 1 },
  bookName: { type: String, default: '' },
  isDark: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible'])
const userStore = useUserStore()

// ====================== 🔥 核心改动：自由拖拽引擎 ======================
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const panelX = ref(window.innerWidth > 480 ? window.innerWidth - 440 : 20)
const panelY = ref(80)

const dynamicPanelStyle = computed(() => {
  // 手机端让出定位控制权，由 @media (max-width: 768px) 统一接管底部抽屉
  if (window.innerWidth <= 768) return {}
  return {
    left: `${panelX.value}px`,
    top: `${panelY.value}px`,
    right: 'auto'
  }
})

const onDragStart = (e) => {
  // 过滤交互控件与非拖拽区域
  if (e.target.closest('.el-button, .el-slider, input, textarea, .no-drag, .segments-list-container')) return
  isDragging.value = true
  dragStartX.value = e.clientX - panelX.value
  dragStartY.value = e.clientY - panelY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  e.preventDefault()
}

const onDragMove = (e) => {
  if (!isDragging.value) return
  // 严密边界限制，防止把面板拖出屏幕外找不回来
  const maxLeft = window.innerWidth - 300
  const maxTop = window.innerHeight - 80
  panelX.value = Math.max(0, Math.min(maxLeft, e.clientX - dragStartX.value))
  panelY.value = Math.max(0, Math.min(maxTop, e.clientY - dragStartY.value))
}

const onDragEnd = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// ====================== 原有业务逻辑保持完整 ======================
const nativeAudioRef = ref(null)
const listContainerRef = ref(null)
const isGenerated = ref(false)
const isGenerating = ref(false)
const isBatchGenerating = ref(false)
const isLoadingList = ref(false)
const isAudioLoading = ref(false)
const isPlaying = ref(false)

const totalCount = ref(0)
const generatedCount = ref(0)
const progressPercent = ref(0)
const segmentList = ref([])
const currentIndex = ref(-1)

const currentAudioTime = ref(0)
const currentDuration = ref(0)
const playbackRate = ref(1.0)
const selectedVoice = ref('default')

let progressTimer = null
let sequentialGenerateFlag = false

const voiceOptions = [
  { label: '默认多角色混播', val: 'default' },
  { label: '青年男声播场', val: 'zh-CN-YunjianNeural' },
  { label: '青年女声播场', val: 'zh-CN-XiaoxiaoNeural' },
  { label: '温润磁性旁白', val: 'zh-CN-YunzeNeural' }
]

const currentVoiceLabel = computed(() => {
  return voiceOptions.find(v => v.val === selectedVoice.value)?.label || '多角色混播'
})

const currentSegment = computed(() => segmentList.value[currentIndex.value] || null)

const closePlayer = () => {
  pauseAudio()
  emit('update:visible', false)
}

const formatSeconds = (sec) => {
  if (!sec || isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = Math.floor(sec % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const getRoleEmoji = (role) => {
  if (!role || role.includes('旁白')) return '🧑'
  if (role.includes('女') || role.includes('妈') || role.includes('姐')) return '👩'
  if (role.includes('男') || role.includes('爸') || role.includes('哥')) return '👨'
  if (role.includes('孩') || role.includes('童')) return '👧'
  if (role.includes('老') || role.includes('爷') || role.includes('奶')) return '👴'
  return '🧑'
}

const getEmotionEmoji = (emo) => {
  if (!emo) return '😌'
  if (emo.includes('欢') || emo.includes('喜') || emo.includes('笑')) return '😊'
  if (emo.includes('悲') || emo.includes('哭') || emo.includes('伤') || emo.includes('痛')) return '😢'
  if (emo.includes('怒') || emo.includes('气') || emo.includes('恨')) return '😡'
  if (emo.includes('柔') || emo.includes('爱') || emo.includes('情')) return '💕'
  return '😌'
}

const checkChapterProgress = async () => {
  if (!props.isbn) return
  try {
    const res = await request.get('/audiobook/progress', {
      params: { isbn: props.isbn, chapter: props.chapterNumber }
    })
    if (res.code === 200 && res.data) {
      totalCount.value = res.data.total || 0
      generatedCount.value = res.data.generated || 0
      progressPercent.value = res.data.percent || 0

      if (totalCount.value > 0) {
        isGenerated.value = true
        loadSegments()
        if (progressPercent.value < 100) {
          isGenerating.value = true
          startProgressPoll()
        }
      } else {
        isGenerated.value = false
      }
    }
  } catch (e) { console.error(e) }
}

const handleStartGenerate = async () => {
  isGenerating.value = true
  isGenerated.value = true
  try {
    const res = await request.post('/audiobook/generate', {
      isbn: props.isbn,
      chapter: props.chapterNumber,
      userId: userStore.userInfo?.id || ''
    })
    if (res.code === 200) {
      ElMessage.success('已启动 AI 智能标注任务，剧本稍后呈现')
      startProgressPoll()
      loadSegments()
    } else {
      isGenerating.value = false
      ElMessage.warning(res.msg || '启动任务生成失败')
    }
  } catch { isGenerating.value = false }
}

const handleBatchGenerateAll = async () => {
  isBatchGenerating.value = true
  try {
    const res = await request.post('/audiobook/generateAll', {
      isbn: props.isbn,
      userId: userStore.userInfo?.id || ''
    })
    if (res.code === 200) {
      const data = res.data
      ElMessage.success(data.message || '全书批量生成已启动')
      // 刷新当前章节的生成状态
      await checkChapterProgress()
      loadSegments()
    } else {
      ElMessage.warning(res.msg || '批量生成失败')
    }
  } catch {
    ElMessage.error('网络异常')
  } finally {
    isBatchGenerating.value = false
  }
}

const startProgressPoll = () => {
  if (progressTimer) clearInterval(progressTimer)
  progressTimer = setInterval(async () => {
    await checkChapterProgress()
    if (progressPercent.value >= 100) {
      clearInterval(progressTimer)
      progressTimer = null
      isGenerating.value = false
    }
  }, 3000)
}

const loadSegments = async () => {
  isLoadingList.value = true
  try {
    const res = await request.get('/audiobook/segments', {
      params: { isbn: props.isbn, chapter: props.chapterNumber, pageNum: 1, pageSize: 500 }
    })
    if (res.code === 200) {
      segmentList.value = res.data?.list || res.data || []
      if (segmentList.value.length > 0 && currentIndex.value === -1) {
        jumpToSegment(0, false)
      }
      triggerSequentialAudioGen()
    }
  } finally { isLoadingList.value = false }
}

const triggerSequentialAudioGen = () => {
  if (sequentialGenerateFlag) return
  sequentialGenerateFlag = true
  const pendingSegs = segmentList.value.filter(s => s.status !== 1 && !s.audioUrl)
  if (pendingSegs.length === 0) {
    sequentialGenerateFlag = false
    return
  }

  const CONCURRENCY = 3
  let idx = 0

  const genOne = async () => {
    while (idx < pendingSegs.length) {
      const seg = pendingSegs[idx++]
      try {
        const res = await request.post('/audiobook/generateAudio', { segmentId: seg.id })
        if (res.code === 200 && res.data) {
          seg.audioUrl = res.data.audioUrl
          seg.audioDuration = res.data.audioDuration
          seg.status = 1
          if (currentIndex.value === segmentList.value.indexOf(seg) && isPlaying.value) {
            playSegmentAudio(seg)
          }
        }
      } catch { seg.status = 2 }
    }
  }

  Promise.all(Array.from({ length: CONCURRENCY }, () => genOne()))
      .finally(() => { sequentialGenerateFlag = false })
}

const playSegmentAudio = (seg) => {
  if (!nativeAudioRef.value || !seg) return
  if (!seg.audioUrl) {
    isAudioLoading.value = true
    request.post('/audiobook/generateAudio', { segmentId: seg.id }).then(res => {
      if (res.code === 200 && res.data) {
        seg.audioUrl = res.data.audioUrl
        seg.audioDuration = res.data.audioDuration
        seg.status = 1
        playSegmentAudio(seg)
      } else {
        isAudioLoading.value = false
        playNextSegment()
      }
    })
    return
  }

  isAudioLoading.value = true
  nativeAudioRef.value.src = seg.audioUrl
  nativeAudioRef.value.playbackRate = playbackRate.value
  nativeAudioRef.value.play().then(() => {
    isPlaying.value = true
  }).catch(() => { isPlaying.value = false })
}

const togglePlay = () => {
  if (!nativeAudioRef.value) return
  if (isPlaying.value) {
    pauseAudio()
  } else {
    if (currentIndex.value === -1) currentIndex.value = 0
    playSegmentAudio(currentSegment.value)
  }
}

const pauseAudio = () => {
  if (nativeAudioRef.value) nativeAudioRef.value.pause()
  isPlaying.value = false
}

const playPrevSegment = () => {
  if (currentIndex.value > 0) jumpToSegment(currentIndex.value - 1, true)
}

const playNextSegment = () => {
  if (currentIndex.value < segmentList.value.length - 1) {
    jumpToSegment(currentIndex.value + 1, true)
  } else {
    isPlaying.value = false
    ElMessage.success('本章剧播完毕')
  }
}

const jumpToSegment = (idx, autoPlay = true) => {
  currentIndex.value = idx
  currentAudioTime.value = 0
  currentDuration.value = segmentList.value[idx]?.audioDuration || 0
  scrollToActiveItem(idx)
  if (autoPlay) playSegmentAudio(segmentList.value[idx])
}

const scrollToActiveItem = (idx) => {
  nextTick(() => {
    const el = document.getElementById(`seg-item-${idx}`)
    if (el && listContainerRef.value) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
}

const onAudioCanPlay = () => {
  isAudioLoading.value = false
  currentDuration.value = nativeAudioRef.value?.duration || currentSegment.value?.audioDuration || 0
}

const onAudioTimeUpdate = () => {
  if (nativeAudioRef.value) currentAudioTime.value = nativeAudioRef.value.currentTime
}

const onAudioEnded = () => { playNextSegment() }

const onAudioError = () => {
  isAudioLoading.value = false
  if (isPlaying.value) playNextSegment()
}

const handleSliderSeek = (val) => {
  if (nativeAudioRef.value) nativeAudioRef.value.currentTime = val
}

const handleSpeedChange = (rate) => {
  playbackRate.value = rate
  if (nativeAudioRef.value) nativeAudioRef.value.playbackRate = rate
}

const handleVoiceChange = (val) => {
  selectedVoice.value = val
  ElMessage.info('已切换音色模式，将在下一段合成生效')
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    checkChapterProgress()
  } else {
    pauseAudio()
    if (progressTimer) clearInterval(progressTimer)
  }
})

watch(() => props.chapterNumber, () => {
  if (props.visible) {
    pauseAudio()
    currentIndex.value = -1
    segmentList.value = []
    checkChapterProgress()
  }
})

onBeforeUnmount(() => {
  pauseAudio()
  if (progressTimer) clearInterval(progressTimer)
  onDragEnd()
})
</script>

<style scoped>
.audiobook-float-panel {
  position: fixed;
  width: clamp(340px, 90vw, 420px);
  height: calc(100vh - 120px);
  max-height: 720px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.3s ease; /* 移除 left/top 过渡，防止拖拽卡顿 */
}

/* 拖拽中状态：提升层级与指针响应 */
.audiobook-float-panel.is-dragging {
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
  cursor: move;
  user-select: none;
}

.panel-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}
.drag-handle { cursor: move; }
.dark-mode .panel-header { border-bottom-color: rgba(255, 255, 255, 0.08); }

.header-title { display: flex; align-items: center; gap: 8px; font-weight: bold; font-size: 15px; overflow: hidden; }
.title-text { white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }

.empty-guide-wrap { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px 24px; text-align: center; }
.guide-emoji { font-size: 56px; margin-bottom: 16px; animation: float-bounce 3s ease-in-out infinite; }
.guide-title { font-size: 18px; font-weight: bold; margin-bottom: 8px; }
.guide-desc { font-size: 13px; color: #888; line-height: 1.6; margin-bottom: 24px; }
.dark-mode .guide-desc { color: #aaa; }

.player-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.progress-notice-bar { padding: 10px 20px; background: rgba(64, 158, 255, 0.08); flex-shrink: 0; font-size: 12px; color: #409eff; }
.progress-info { display: flex; justify-content: space-between; margin-bottom: 6px; font-weight: 500; }

.main-player-console {
  padding: 16px 20px; margin: 10px 16px; border-radius: 16px; background: rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.4); flex-shrink: 0; box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}
.dark-mode .main-player-console { background: rgba(0, 0, 0, 0.25); border-color: rgba(255, 255, 255, 0.08); }

.slider-container { margin-bottom: 12px; }
.time-display { display: flex; justify-content: space-between; font-size: 11px; color: #888; margin-top: -4px; }

.controls-row { display: flex; align-items: center; justify-content: space-between; }
.center-play-group { display: flex; align-items: center; gap: 14px; }

.play-toggle-btn { width: 48px !important; height: 48px !important; font-size: 20px !important; box-shadow: 0 6px 16px rgba(64, 158, 255, 0.35); }
.tool-btn { border: none !important; background: rgba(0,0,0,0.05) !important; font-size: 12px; }
.dark-mode .tool-btn { background: rgba(255,255,255,0.1) !important; color: #ddd; }

.segments-list-container { flex: 1; overflow-y: auto; padding: 8px 16px 24px; display: flex; flex-direction: column; gap: 10px; }

.segment-item { padding: 12px 16px; border-radius: 14px; background: rgba(255, 255, 255, 0.25); border: 1px solid transparent; cursor: pointer; transition: all 0.25s ease; }
.dark-mode .segment-item { background: rgba(255, 255, 255, 0.04); }

.segment-item:hover { background: rgba(255, 255, 255, 0.45); transform: translateX(2px); }
.dark-mode .segment-item:hover { background: rgba(255, 255, 255, 0.08); }

.segment-item.is-active { background: rgba(64, 158, 255, 0.12) !important; border-color: rgba(64, 158, 255, 0.4); box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1); }
.dark-mode .segment-item.is-active { background: rgba(64, 158, 255, 0.2) !important; }

.seg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; font-size: 12px; }
.role-info { display: flex; align-items: center; gap: 6px; font-weight: bold; color: #444; }
.dark-mode .role-info { color: #ccc; }

.emotion-badge { font-weight: normal; font-size: 11px; padding: 1px 6px; border-radius: 6px; background: rgba(0,0,0,0.05); }
.dark-mode .emotion-badge { background: rgba(255,255,255,0.1); }

.duration { color: #999; font-size: 11px; }
.playing-indicator { color: #409eff; font-weight: bold; animation: pulse 1.5s infinite; }

.seg-content { font-size: 13.5px; line-height: 1.6; color: #333; text-align: justify; }
.dark-mode .seg-content { color: #e2e8f0; }

@keyframes float-bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

/* 移动端强行降级接管 */
@media (max-width: 768px) {
  .audiobook-float-panel {
    left: 0 !important;
    top: auto !important;
    bottom: 0 !important;
    right: 0 !important;
    width: 100vw !important;
    height: 82vh !important;
    border-radius: 20px 20px 0 0 !important;
  }
}

.audiobook-fade-enter-active, .audiobook-fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.audiobook-fade-enter-from, .audiobook-fade-leave-to { opacity: 0; transform: translateY(15px) scale(0.98); }
</style>