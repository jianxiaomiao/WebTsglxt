// stores/petStore.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request.js'
import {useUserStore} from "./userStore.js";

export const usePetStore = defineStore('pet', () => {
    // 全局共享消息列表
    const messages = ref([])
    // 全局消息ID计数器（保证唯一）
    let messageId = 0

    // ====================== 消息类型定义 ======================
    // 1. chat: 普通聊天消息（5秒自动消失）
    // 2. thinking: AI思考气泡（常驻，直到手动移除）
    // 3. interaction: 互动答题气泡（常驻，带关闭按钮）
    const MESSAGE_TYPES = {
        CHAT: 'chat',
        THINKING: 'thinking',
        INTERACTION: 'interaction',
        CONFIRM: 'confirm',
        STATIC: 'static', // 🔥 新增：静态常驻长文
        TALK: 'talk'      // 🔥 新增：可连续回击对话
    }

    // ====================== 全局发消息方法 ======================
    const addMessage = (text, type = MESSAGE_TYPES.CHAT, extraData = {}) => {
        const id = messageId++
        const message = {
            id,
            text,
            type,
            autoCloseTimer: null,
            ...extraData // 存储互动题目的额外数据（选项、解析等）
        }

        messages.value.push(message)

        // 只有普通聊天消息自动消失
        if (type === MESSAGE_TYPES.CHAT) {
            setTimeout(() => {
                removeMessage(id)
            }, 5000)
        }

        return id
    }

    // ====================== 🔥 流式打字机气泡专区 ======================
    // 1. 创建一个“待定稿”的空气泡，动态接收后端下发的气泡执照
    const createStreamingMessage = (msgId, bubbleType = MESSAGE_TYPES.CHAT) => {
        clearThinkingMessages()

        const message = {
            id: msgId,
            text: '',
            type: bubbleType, // 👈 动态绑定为 chat / static / talk
            autoCloseTimer: null,
            userReply: ''     // 👈 提前为 talk 气泡的输入框双向绑定占位
        }
        messages.value.push(message)
    }

// 2. 电台DJ发来了一个字，精准塞进对应的气泡里
    const appendStreamingChunk = (msgId, chunkText) => {
        const targetMsg = messages.value.find(m => m.id === msgId)
        console.log("追加chunk，找到消息:", targetMsg)
        if (targetMsg) {
            targetMsg.text += chunkText // ✨ 魔法发生处：Pinia变了，小宠物气泡里的字会自动往后“顶”！
        } else {
            // 万一网络抖动，Start包丢了，这里安全兜底创建
            createStreamingMessage(msgId)
            messages.value.find(m => m.id === msgId).text = chunkText
        }
    }

    // 3. 电台DJ说“播音结束”
    const finishStreamingMessage = (msgId) => {
        const targetMsg = messages.value.find(m => m.id === msgId)
        if (targetMsg) {
            // 🔥 灵魂判决：只有普通的 CHAT 气泡才执行 5 秒自毁！static 和 talk 永不自毁！
            if (targetMsg.type === MESSAGE_TYPES.CHAT) {
                targetMsg.autoCloseTimer = setTimeout(() => {
                    removeMessage(targetMsg.id)
                }, 5000)
            }
        }
    }

    // 🔥 新增：创建确认气泡（是否全网搜书）
    const addConfirmMessage = (text, onConfirm, onCancel) => {
        // 先清空旧的确认气泡，避免重复
        messages.value = messages.value.filter(m => m.type !== MESSAGE_TYPES.CONFIRM)
        return addMessage(text, MESSAGE_TYPES.CONFIRM, {
            onConfirm,  // 确认回调
            onCancel    // 取消回调
        })
    }

    // 修改removeMessage方法，清除定时器防止内存泄漏
    // stores/petStore.js
    const removeMessage = (id) => {
        const message = messages.value.find(item => item.id === id)
        if (message) {
            // 清除自动关闭定时器
            if (message.autoCloseTimer) {
                clearTimeout(message.autoCloseTimer)
            }
            // 🔥 新增：清除倒计时定时器
            if (message.countdownTimer) {
                clearInterval(message.countdownTimer)
            }
        }
        messages.value = messages.value.filter(item => item.id !== id)
    }

    // 移除所有思考气泡
    const clearThinkingMessages = () => {
        messages.value = messages.value.filter(item => item.type !== MESSAGE_TYPES.THINKING)
    }

    // ====================== 每日阅读日签（保留原有功能） ======================
    const dailyGreeting = ref('')
    const greetingList = [
        '书香浸润心灵，阅读点亮生活。每日翻开书页，让文字治愈疲惫，让思想奔赴远方。在墨香中沉淀自我，在故事里收获力量，坚持阅读，你会遇见更温柔、更强大的自己。',
        '一字一句皆是风景，一书一页皆有成长。今日宜静心阅读，把浮躁放下，把热爱拾起。文字不会辜负每一个用心品读的人，愿你在书香里安享时光，收获内心的丰盈与安宁。',
        '阅读是一场无声的远行，不必远行，亦能遍历山河。今天，给自己一段纯粹的阅读时光，让灵魂在文字中漫步，让知识沉淀为底气，让阅读成为陪伴你一生的好习惯。',
        '以书为友，一生受益；以书为伴，不负年华。每日阅读一点点，积累学识，滋养心性，治愈焦虑。愿你在文字的世界里，找到属于自己的宁静与力量，向阳而行。',
        '生活的美好，藏在字里行间。静下心读一本书，感受文字的温度，体会思想的光芒。阅读不是任务，而是享受，是给自己最好的礼物，愿你日日与书香相伴。'
    ]

    const generateDailyGreeting = () => {
        const randomIndex = Math.floor(Math.random() * greetingList.length)
        dailyGreeting.value = greetingList[randomIndex]
        return dailyGreeting.value
    }

    // ====================== 前端模拟互动题目库 ======================
    const questionBank = [
        {
            type: 'judge', // 判断题
            question: '《红楼梦》的作者是清代作家曹雪芹。',
            answer: true,
            analysis: '《红楼梦》是中国古典四大名著之首，作者曹雪芹，清代小说家。'
        },
        {
            type: 'choice', // 选择题
            question: '以下哪部作品不是鲁迅的代表作？',
            options: ['A. 《呐喊》', 'B. 《彷徨》', 'C. 《骆驼祥子》'],
            answer: 'C',
            analysis: '《骆驼祥子》是老舍的代表作，鲁迅的代表作有《呐喊》《彷徨》《朝花夕拾》等。'
        },
        {
            type: 'judge',
            question: '“但愿人长久，千里共婵娟”中的“婵娟”指的是月亮。',
            answer: true,
            analysis: '这句词出自苏轼的《水调歌头·明月几时有》，“婵娟”在这里代指明月。'
        },
        {
            type: 'choice',
            question: '我国第一部纪传体通史是？',
            options: ['A. 《资治通鉴》', 'B. 《史记》', 'C. 《汉书》'],
            answer: 'B',
            analysis: '《史记》是西汉史学家司马迁撰写的中国第一部纪传体通史，记载了上至上古传说中的黄帝时代，下至汉武帝太初四年间共3000多年的历史。'
        }
    ]

    // 随机生成一道互动题目
    const generateRandomQuestion = () => {
        const randomIndex = Math.floor(Math.random() * questionBank.length)
        return questionBank[randomIndex]
    }


    // ====================== 新增：AI定时自动出题功能 ======================
    // 自动出题定时器
    let autoQuizTimer = null
    // 自动出题状态
    const isAutoQuizRunning = ref(false)
    // 默认出题间隔（毫秒）- 2分钟
    const DEFAULT_QUIZ_INTERVAL = 2 * 60 * 1000
    // 当前出题间隔
    const currentQuizInterval = ref(DEFAULT_QUIZ_INTERVAL)
    // 自动出题模式：local(本地题库) / ai(AI生成)
    const autoQuizMode = ref('local')

    /**
     * 生成并显示一道互动题目
     * @param {Object} customQuestion - 可选，自定义题目对象（本地或AI生成）
     * @returns {number} 消息ID
     */
    const generateAndShowQuiz = (customQuestion = null) => {
            const question = customQuestion || generateRandomQuestion()

            // 🔥 统一答案类型：判断题答案必须是布尔值
            if (question.type === 'judge') {
                if (typeof question.answer === 'string') {
                    question.answer = question.answer.toLowerCase() === 'true'
                }
            }

            return addMessage('', MESSAGE_TYPES.INTERACTION, {
                question: question.question,
                questionType: question.type,
                options: question.options || [],
                correctAnswer: question.answer,
                analysis: question.analysis,
                userAnswer: null,
                isCorrect: null
            })
        }

    /**
     * 🔥 核心：调用AI根据指定内容生成题目
     * @param {string} content - 用于生成题目的文本内容
     * @param {string} type - 题目类型：'choice'(选择题) / 'judge'(判断题) / 'random'(随机)
     * @returns {Promise<Object>} 符合本地题库格式的题目对象
     */
        // 替换petStore.js中的generateQuestionFromContent方法
    const generateQuestionFromContent = async (content, type = 'random') => {
            if (!content || content.trim().length < 20) {
                throw new Error('内容太短，无法生成题目')
            }

            // 2. 动态实例化 userStore
            const userStore = useUserStore()

            // 🔥 优化Prompt：专门强调判断题必须有question字段
            const typePrompt = type === 'random'
                ? '随机生成一道选择题或判断题'
                : `生成一道${type === 'choice' ? '选择题' : '判断题'}`

            const prompt = `
请根据以下文本内容，${typePrompt}，严格按照JSON格式输出，不要添加任何额外文字。
⚠️ 重要要求：
1. 题目必须基于文本内容，不能超出文本范围
2. 选择题提供3个选项，格式为["A. xxx", "B. xxx", "C. xxx"]
3. 判断题answer必须是布尔值true或false，**绝对不能是字符串"true"或"false"**
4. 所有题目**必须包含question字段**，不能为空
5. 解析要简洁明了，不超过50字
6. JSON格式必须正确，字段不能缺失

输出格式示例（选择题）：
{"type":"choice","question":"问题内容","options":["A. 选项1","B. 选项2","C. 选项3"],"answer":"A","analysis":"解析内容"}

输出格式示例（判断题）：
{"type":"judge","question":"问题内容","answer":true,"analysis":"解析内容"}

文本内容：
${content}
    `

            // 🔥 新增：带上 generate_quiz 标签，后端会自动用出题专属 SystemPrompt 压制 AI 的废话
            // 改为直调专属独立接口：
            const res = await request.post('/pet/ai/generate_quiz', {
                content: prompt,
                // actionType 甚至都不用传了，Java会从URL里自己剥出来！
                isbn: userStore.currentReadingIsbn,
                chapterNumber: userStore.currentReadingChapter
            })
            console.log('AI返回原始数据:', res.data)

            try {
                // 处理AI可能返回的多余文字，只提取JSON部分
                const jsonMatch = res.data.match(/\{[\s\S]*\}/)
                if (!jsonMatch) {
                    throw new Error('AI返回格式错误')
                }
                let question = JSON.parse(jsonMatch[0])
                console.log('解析后的题目:', question)

                // 🔥 新增：自动修复AI生成的常见问题
                // 修复1：判断题answer是字符串的情况
                if (question.type === 'judge' && typeof question.answer === 'string') {
                    question.answer = question.answer.toLowerCase() === 'true'
                }

                // 🔥 修复2：AI漏掉question字段的情况（最关键）
                if (!question.question) {
                    // 从解析中提取问题，或者生成一个默认问题
                    if (question.analysis) {
                        question.question = question.analysis.replace('原文提到', '根据原文，').replace('。', '，对吗？')
                    } else {
                        question.question = '根据原文内容，以下说法正确吗？'
                    }
                    console.log('自动修复了缺失的question字段:', question.question)
                }

                // 验证返回格式是否正确
                if (!question.type || !question.question || question.answer === undefined || !question.analysis) {
                    throw new Error('题目字段不完整')
                }
                if (question.type === 'choice' && (!question.options || question.options.length < 2)) {
                    throw new Error('选择题缺少选项')
                }

                return question
            } catch (err) {
                console.error('AI题目解析失败:', err)
                // 🔥 终极降级方案：如果解析失败，返回本地题库的随机题目
                console.log('降级到本地题库')
                return generateRandomQuestion()
            }
        }

    /**
     * 根据内容生成并显示题目（带思考状态）
     * @param {string} content - 用于生成题目的文本内容
     * @param {string} type - 题目类型
     */
    const showQuizFromContent = async (content, type = 'random') => {
        addMessage('😼正在根据内容生成题目...', MESSAGE_TYPES.THINKING)
        try {
            const question = await generateQuestionFromContent(content, type)
            generateAndShowQuiz(question)
            addMessage('✅ 题目生成成功！快来答题吧~')
        } catch (err) {
            addMessage(`😥 ${err.message || '题目生成失败，请稍后再试'}`)
        } finally {
            // ✨ 核心修复4：不再依赖ID，直接清除思考气泡
            clearThinkingMessages()
        }
    }

    /**
     * 启动AI自动出题
     * @param {number} interval - 可选，出题间隔（毫秒）
     * @param {string} mode - 可选，出题模式：'local'(本地题库) / 'ai'(AI生成)
     * @param {string} aiContent - 可选，AI模式下用于生成题目的基础内容
     */
    const startAutoQuiz = (interval = DEFAULT_QUIZ_INTERVAL, mode = 'local', aiContent = '') => {
        if (autoQuizTimer) {
            stopAutoQuiz()
        }

        currentQuizInterval.value = interval
        autoQuizMode.value = mode
        isAutoQuizRunning.value = true

        // 立即出一道题
        if (mode === 'ai' && aiContent) {
            showQuizFromContent(aiContent)
        } else {
            generateAndShowQuiz()
        }

        // 设置定时器
        autoQuizTimer = setInterval(() => {
            if (mode === 'ai' && aiContent) {
                showQuizFromContent(aiContent)
            } else {
                generateAndShowQuiz()
            }
        }, interval)

        addMessage(`🤖 AI自动出题模式已开启，每隔${(interval / 60000).toFixed(1)}分钟会出一道${mode === 'ai' ? 'AI智能' : '随机'}题~`)
    }

    /**
     * 停止AI自动出题
     */
    const stopAutoQuiz = () => {
        if (autoQuizTimer) {
            clearInterval(autoQuizTimer)
            autoQuizTimer = null
        }
        isAutoQuizRunning.value = false
        autoQuizMode.value = 'local'
        addMessage('🤖 AI自动出题模式已关闭')
    }

    /**
     * 切换自动出题状态
     * @param {number} interval - 可选，出题间隔（毫秒）
     * @param {string} mode - 可选，出题模式
     * @param {string} aiContent - 可选，AI模式基础内容
     */
    const toggleAutoQuiz = (interval = DEFAULT_QUIZ_INTERVAL, mode = 'local', aiContent = '') => {
        if (isAutoQuizRunning.value) {
            stopAutoQuiz()
        } else {
            startAutoQuiz(interval, mode, aiContent)
        }
    }

    return {
        messages,
        addMessage,
        removeMessage,
        clearThinkingMessages,
        MESSAGE_TYPES,
        addConfirmMessage,
        createStreamingMessage,
        appendStreamingChunk,
        finishStreamingMessage,
        dailyGreeting,
        generateDailyGreeting,
        generateRandomQuestion,
        generateAndShowQuiz,
        startAutoQuiz,
        stopAutoQuiz,
        toggleAutoQuiz,
        generateQuestionFromContent,
        showQuizFromContent,
        isAutoQuizRunning,
        currentQuizInterval,
        autoQuizMode
    }
})