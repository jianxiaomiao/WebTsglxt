// src/stores/reader.js
import { defineStore } from 'pinia'

export const useReaderStore = defineStore('reader', {
    state: () => ({
        // ========== 1. 章节缓存系统 ==========
        chapterCache: new Map(),
        cachedIsbn: '',
        cachedTotalChapter: 0,
        cacheClearTimer: null,

        // ========== 2. 文本朗读(TTS)资源 ==========
        audioPlayer: null,
        ttsAbortController: null,
        audioCache: new Map(),
        currentAudioUrl: '',
        isReading: false,
        isPaused: false,
        isAudioLoading: false,
        showReadingDialog: false,
        readingRate: 1.0,
        isAiMode: false,
        textChunksForAi: [],
        currentChunkIndexForAi: 0,
        currentSubtitle: '',
        fullNoteText: '',
        fullReadingText: '',
        readStartIndex: 0,
        currentReadIndex: -1,

        // ========== 3. 漂流瓶系统 ==========
        bubbleTimer: null,
        floatingBubbles: [],
        hasNewBottle: false,
        bottleQueue: [],

        // ========== 4. 阅读时长定时器 ==========
        readTimer: null,

        // ========== 5. 清理信号（通知组件解绑事件） ==========
        emitCleanupSignal: false
    }),

    actions: {
        // 初始化音频实例（全局唯一，避免重复创建）
        initAudio() {
            if (!this.audioPlayer) {
                this.audioPlayer = new Audio()
            }
        },

        // ========== 核心：一键清理所有阅读资源 ==========
        cleanupAllResources() {
            this.clearAllTimers()
            this.stopReadingCompletely()
            this.clearChapterCache()
            this.clearBottleSystem()

            // 发送清理信号，通知组件解绑自身事件监听
            this.emitCleanupSignal = true
            setTimeout(() => {
                this.emitCleanupSignal = false
            }, 0)

            console.log('🧹 阅读页全局资源已全部释放')
        },

        // ========== 1. 清理所有定时器 ==========
        clearAllTimers() {
            // 清理阅读时长上报定时器
            if (this.readTimer) {
                clearInterval(this.readTimer)
                this.readTimer = null
            }
            // 清理缓存定时清理器
            if (this.cacheClearTimer) {
                clearInterval(this.cacheClearTimer)
                this.cacheClearTimer = null
            }
            // 清理漂流瓶轮询定时器
            if (this.bubbleTimer) {
                clearInterval(this.bubbleTimer)
                this.bubbleTimer = null
            }
            // 暴力兜底：清除所有可能泄漏的定时器
            for (let i = 1; i < 1000; i++) {
                clearInterval(i)
                clearTimeout(i)
            }
        },

        // ========== 2. 彻底销毁朗读资源 ==========
        stopReadingCompletely() {
            // 1. 瞬间掐断所有待处理的 TTS 网络请求
            if (this.ttsAbortController) {
                this.ttsAbortController.abort()
                this.ttsAbortController = null
            }

            // 2. 停止播放器并解绑所有事件
            if (this.audioPlayer) {
                this.audioPlayer.pause()
                this.audioPlayer.src = ''
                this.audioPlayer.onended = null
                this.audioPlayer.onplay = null
                this.audioPlayer.onerror = null
            }

            // 3. 释放所有 Blob URL，彻底解决内存泄漏
            if (this.currentAudioUrl) {
                URL.revokeObjectURL(this.currentAudioUrl)
                this.currentAudioUrl = ''
            }
            this.audioCache.forEach(promise => {
                promise.then(url => { if (url) URL.revokeObjectURL(url) }).catch(() => {})
            })
            this.audioCache.clear()

            // 4. 重置所有朗读状态
            this.isReading = false
            this.isPaused = false
            this.isAudioLoading = false
            this.showReadingDialog = false
            this.isAiMode = false
            this.currentChunkIndexForAi = 0
            this.currentSubtitle = ''
            this.textChunksForAi = []
            this.fullNoteText = ''
            this.fullReadingText = ''
            this.readStartIndex = 0
            this.currentReadIndex = -1
        },

        // ========== 3. 清空章节缓存 ==========
        clearChapterCache() {
            this.chapterCache.clear()
            this.cachedIsbn = ''
            this.cachedTotalChapter = 0
        },

        // ========== 4. 清理漂流瓶系统 ==========
        clearBottleSystem() {
            this.floatingBubbles = []
            this.hasNewBottle = false
            // 清理页面上残留的粒子DOM
            document.querySelectorAll('.shatter-particle').forEach(el => el.remove())
        },

        // ========== 缓存读写便捷方法 ==========
        setCacheChapter(num, data) {
            this.chapterCache.set(num, data)
        },
        getCacheChapter(num) {
            return this.chapterCache.get(num)
        },
        hasCacheChapter(num) {
            return this.chapterCache.has(num)
        }
    }
})