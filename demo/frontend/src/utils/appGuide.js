import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';

// 1. 将原来的纯文本升级为 Driver 步骤数组
const guideStepsMap = {
    login: [
        { element: '#login-form', popover: { title: '🔑 登录', description: '填写必填项，可点击眼睛图标切换显隐。', side: 'right' } },
        { element: '#login-form-captcha', popover: { title: '🙀 验证码', description: '点击刷新验证码~', side: 'right' } }
    ],
    register: [
        { element: '#register-form', popover: { title: '📝 注册', description: '除生日，其余均为必填项，邮箱收件较慢，请耐心等待。', side: 'right' } },
        { element: '#register-form-captcha', popover: { title: '🙀 验证码', description: '点击刷新验证码~', side: 'right' } }
    ],
    home: [
        {
            element: '#guide-hot-books',
            popover: { title: '🔥 热门书籍舱', description: '这里推流全网最热的文学作品，3D书本可以随着你的鼠标翻转哦。', side: 'bottom', align: 'start' }
        },
        {
            element: '#guide-daily-pic',
            popover: { title: '🎨 灵宝日记', description: 'AI 每天为你生成的专属视觉画报，点击箭头可以回溯往期历史。', side: 'left', align: 'start' }
        },
        {
            element: '#guide-tarot',
            popover: { title: '🔮 命运之书', description: '神秘的塔罗牌推荐算法，点击翻牌抽取你的专属命运之书。', side: 'right', align: 'start' }
        },
        {
            element: '#guide-universe',
            popover: { title: '🌌 思绪云境 / 星图', description: '你的所有灵感会像宇宙星辰一样悬浮在这里，点击星球即可探索对应领域的书籍。', side: 'top', align: 'center' }
        },
        {
            element: '#guide-context-menu',
            popover: { title: '😼右键菜单', description: '点击启用丰富功能', side: 'top', align: 'center' }
        },
    ],
    bookDetail: [
        { element: '#guide-book-title', popover: { title: '📖 开始阅读', description: '点击书名进入沉浸阅读。', side: 'bottom' } },
        { element: '#guide-column', popover: { title: '💬 评论区', description: '在这里可以发布或删除你的评价。', side: 'top' } }
    ],
    bookshelf: [
        { element: '#guide-shelf-book', popover: { title: '🗂️ 书架引导', description: '点击进入阅读界面~右键归还书籍~。', side: 'bottom' } }
    ],
    profile: [
        { element: '#guide-avatar', popover: { title: '😼 用户界面', description: '点击进入用户对外显示界面~。', side: 'bottom' } },
        { element: '#guide-book-comment', popover: { title: '📖 书籍评论', description: '点击进入书籍评论界面~。', side: 'bottom' } },
        { element: '#guide-heatmap-box', popover: { title: '🌤️ 阅读贡献', description: '点击进入周报界面~。', side: 'bottom' } },
        { element: '#guide-note', popover: { title: '📒 我的笔记', description: '点击进入笔记界面~。', side: 'bottom' } },
        { element: '#guide-forum', popover: { title: '😾 论坛评论', description: '点击进入论坛评论界面~。', side: 'bottom' } },
        { element: '#guide-history', popover: { title: '📚 阅读历史', description: '点击进入阅读历史界面~。', side: 'bottom' } },
    ],
    // ... 其他页面可以照着这个格式继续加
}

// 2. 导出匹配逻辑（复用你原来的判断）
export const getGuideConfigByPath = (path) => {
    if (path === '/login') return { key: 'login', steps: guideStepsMap.login };
    if (path === '/register') return { key: 'register', steps: guideStepsMap.register };
    if (path === '/home' || path === '/') return { key: 'home', steps: guideStepsMap.home };
    if (path.includes('/book/detail')) return { key: 'bookDetail', steps: guideStepsMap.bookDetail };
    if (path.includes('/bookshelf')) return { key: 'bookshelf', steps: guideStepsMap.bookshelf };
    if (path === '/profile') return { key: 'profile', steps: guideStepsMap.profile };
    return null;
}

// 3. 核心调用函数
export const startGuideForPath = (path) => {
    const config = getGuideConfigByPath(path);

    // 如果当前页面没有配置引导，直接静默退出
    if (!config || !config.steps || config.steps.length === 0) {
        return;
    }

    const driverObj = driver({
        showProgress: true,
        animate: true,
        allowClose: false,
        showButtons: ['next', 'previous', 'close'], // 🌟 新增：显式呼出关闭按钮
        doneBtnText: '我知道啦 🚀',
        nextBtnText: '下一步 ✨',
        prevBtnText: '上一步',
        progressText: '探索进度 {{current}} / {{total}}',
        steps: config.steps
    });

    driverObj.drive();
}