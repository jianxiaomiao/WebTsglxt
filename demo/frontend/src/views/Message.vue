<template>
  <div class="message-container">
    <ElRow :gutter="20" class="dashboard-row">

      <ElCol :xs="24" :md="14" :lg="15" class="dashboard-col">
        <div class="dashboard-card system-notifications-box glass-panel">
          <div class="card-panel-header">
            <div class="header-left">
              <el-icon class="panel-icon"><Notification /></el-icon>
              <span class="panel-title">互动与系统通知</span>
              <ElBadge :value="notificationUnread" :hidden="notificationUnread === 0" class="panel-badge" />
            </div>
            <ElButton
                v-if="notificationUnread > 0"
                type="primary"
                plain
                size="small"
                style="flex-shrink: 0; margin-left: auto;"
                @click="() => handleMarkAllRead(false)"
            >
              全部标记已读
            </ElButton>
          </div>

          <div class="card-panel-body scrollbar-custom">
            <div v-if="notifications.length > 0" class="notification-list">
              <div
                  v-for="item in notifications"
                  :key="item.id"
                  class="notification-item"
              >
                <ElRow v-if="!isMobile" align="top" :gutter="16">
                  <ElCol :span="2">
                    <ElAvatar size="48" style="font-size: 18px; cursor: pointer;" @click="router.push('/user/profile?userId=' + item.fromUserId)">
                      {{ item.fromUserName?.slice(-2) || '未知' }}
                    </ElAvatar>
                  </ElCol>
                  <ElCol :span="22">
                    <div class="notification-header">
                      <div class="notification-name-wrapper">
                        <span class="notification-name">{{ item.fromUserName || '未知用户' }}</span>
                        <span class="notification-type-badge" :class="item.type">
                          {{ item.type === 'comment' ? '回复了你的评论' : '点赞了你的评论' }}
                        </span>
                        <span class="notification-time">{{ formatTime(item.createTime) }}</span>
                      </div>
                    </div>
                    <el-divider v-if="item.type === 'comment' && item.replyCommentId" border-style="dashed" :style="{ borderColor: isDark ? '#374151' : '#eaedf1', borderWidth: '1px', margin: '8px 0' }" />

                    <div class="notification-content"
                         v-if="item.type === 'comment' && item.replyCommentId"
                         :style="{
                           marginTop: '8px',
                           fontSize: '14px',
                           color: isDark ? '#e5e7eb' : '#333',
                           textAlign: 'left',
                           whiteSpace: 'pre-wrap',
                           wordWrap: 'break-word',
                           marginBottom: '12px'
                         }"
                    >
                      {{ item.content || '' }}
                    </div>

                    <div class="notification-images" v-if="item.images.length > 0">
                      <template v-if="expandedImages[item.id] || item.images.length <= 2">
                        <el-image
                            v-for="img in item.images"
                            :key="img.id"
                            :src="IMAGE_BASE_URL + img.imageUrl"
                            :preview-src-list="item.images.map(img => IMAGE_BASE_URL + img.imageUrl)"
                            :preview="true"
                            fit="cover"
                            class="notification-img"
                        />
                      </template>
                      <template v-else>
                        <el-image
                            v-for="img in item.images.slice(0, 2)"
                            :key="img.id"
                            :src="IMAGE_BASE_URL + img.imageUrl"
                            :preview-src-list="item.images.map(img => IMAGE_BASE_URL + img.imageUrl)"
                            :preview="true"
                            fit="cover"
                            class="notification-img"
                        />
                        <div class="expand-btn" @click="expandedImages[item.id] = true">
                          +{{ item.images.length - 2 }}张
                        </div>
                      </template>
                    </div>
                  </ElCol>
                </ElRow>

                <div v-else class="mobile-notification-item">
                  <div class="mobile-notification-header">
                    <ElAvatar size="40" style="font-size: 16px; cursor: pointer;" @click="router.push('/user/profile?userId=' + item.fromUserId)">
                      {{ item.fromUserName?.slice(-2) || '未知' }}
                    </ElAvatar>
                    <div class="mobile-notification-info">
                      <div class="mobile-notification-name-type">
                        <span class="notification-name">{{ item.fromUserName || '未知用户' }}</span>
                        <span class="notification-type-badge" :class="item.type">
                          {{ item.type === 'comment' ? '回复了你的评论' : '点赞了你的评论' }}
                        </span>
                      </div>
                      <span class="notification-time">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                  <div class="notification-content"
                       v-if="item.type === 'comment' && item.replyCommentId"
                       style="margin: 8px 0 12px 0; font-size: 14px; color: #333; text-align: left; white-space: pre-wrap; word-wrap: break-word;"
                  >
                    {{ item.content || '' }}
                  </div>
                  <div class="mobile-notification-images" v-if="item.images.length > 0">
                    <template v-if="expandedImages[item.id] || item.images.length <= 2">
                      <el-image
                          v-for="img in item.images"
                          :key="img.id"
                          :src="IMAGE_BASE_URL + img.imageUrl"
                          :preview-src-list="item.images.map(img => IMAGE_BASE_URL + img.imageUrl)"
                          :preview="true"
                          fit="cover"
                          class="mobile-notification-img"
                      />
                    </template>
                    <template v-else>
                      <el-image
                          v-for="img in item.images.slice(0, 2)"
                          :key="img.id"
                          :src="IMAGE_BASE_URL + img.imageUrl"
                          :preview-src-list="item.images.map(img => IMAGE_BASE_URL + img.imageUrl)"
                          :preview="true"
                          fit="cover"
                          class="mobile-notification-img"
                      />
                      <div class="expand-btn" @click="expandedImages[item.id] = true">
                        +{{ item.images.length - 2 }}张
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <el-icon size="48" style="color: #ccc;"><Notification /></el-icon>
              <p>暂无互动消息</p>
            </div>
          </div>
        </div>
      </ElCol>

      <ElCol :xs="24" :md="10" :lg="9" class="dashboard-col side-column">

        <div class="dashboard-card friend-requests-box glass-panel">
          <div class="card-panel-header">
            <div class="header-left">
              <el-icon class="panel-icon"><UserFilled /></el-icon>
              <span class="panel-title">收到的申请</span>
              <ElBadge :value="receivedUnread" :hidden="receivedUnread === 0" class="panel-badge" />
            </div>
          </div>

          <div class="card-panel-body scrollbar-custom">
            <div v-if="friendRequests.length > 0" class="request-list">
              <div v-for="request in friendRequests" :key="request.id" class="request-item">
                <ElRow v-if="!isMobile" align="top" :gutter="12">
                  <ElCol :span="3">
                    <ElAvatar size="40" style="font-size: 14px; cursor:pointer;" @click="router.push('/user/profile?userId=' + request.fromUserId)">
                      {{ request.userName.slice(-2) }}
                    </ElAvatar>
                  </ElCol>
                  <ElCol :span="14" style="flex: 1;">
                    <div class="request-header">
                      <span class="request-name">{{ request.userName }}</span>
                      <span class="request-time">{{ formatTime(request.createTime) }}</span>
                    </div>
                    <div class="request-msg">
                      {{ request.requestMsg || '申请添加你为好友' }}
                    </div>
                  </ElCol>
                  <ElCol :span="7" class="request-actions">
                    <ElButton v-if="request.status === 0" type="primary" size="small" @click="acceptRequest(request)">同意</ElButton>
                    <ElButton v-if="request.status === 0" size="small" @click="rejectRequest(request)">拒绝</ElButton>
                    <span v-else-if="request.status === 1" class="status-text success">已同意</span>
                    <span v-else-if="request.status === 2" class="status-text danger">已拒绝</span>
                  </ElCol>
                </ElRow>
                <div v-else class="mobile-request-item">
                  <div class="mobile-request-header">
                    <ElAvatar size="40" style="font-size: 16px; cursor:pointer;" @click="router.push('/user/profile?userId=' + request.fromUserId)">
                      {{ request.userName.slice(-2) }}
                    </ElAvatar>
                    <div class="mobile-request-info">
                      <div class="mobile-request-name-id">
                        <span class="request-name">{{ request.userName }}</span>
                      </div>
                      <span class="request-time">{{ formatTime(request.createTime) }}</span>
                    </div>
                  </div>
                  <div class="request-msg" style="margin: 8px 0 12px 0;">
                    {{ request.requestMsg || '申请添加你为好友' }}
                  </div>
                  <div class="mobile-request-actions">
                    <ElButton v-if="request.status === 0" type="primary" size="small" @click="acceptRequest(request)" style="margin-right: 8px;">同意</ElButton>
                    <ElButton v-if="request.status === 0" size="small" @click="rejectRequest(request)">拒绝</ElButton>
                    <span v-else-if="request.status === 1" class="status-text success">已同意</span>
                    <span v-else-if="request.status === 2" class="status-text danger">已拒绝</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <el-icon size="36" style="color: #ccc;"><UserFilled /></el-icon>
              <p style="font-size: 13px;">暂无收到的好友申请</p>
            </div>
          </div>
        </div>

        <div class="dashboard-card friend-requests-box glass-panel">
          <div class="card-panel-header">
            <div class="header-left">
              <el-icon class="panel-icon"><UserFilled /></el-icon>
              <span class="panel-title">发出的申请</span>
              <ElBadge :value="sentUnread" :hidden="sentUnread === 0" class="panel-badge" />
            </div>
          </div>

          <div class="card-panel-body scrollbar-custom">
            <div v-if="sentRequests.length > 0" class="request-list">
              <div v-for="request in sentRequests" :key="request.id" class="request-item">
                <ElRow v-if="!isMobile" align="top" :gutter="12">
                  <ElCol :span="3">
                    <ElAvatar size="40" style="font-size: 14px; cursor:pointer;" @click="router.push('/user/profile?userId=' + request.toUserId)">
                      {{ request.userName.slice(-2) }}
                    </ElAvatar>
                  </ElCol>
                  <ElCol :span="15">
                    <div class="request-header">
                      <span class="request-name">{{ request.userName }}</span>
                      <span class="request-time">{{ formatTime(request.createTime) }}</span>
                    </div>
                    <div class="request-msg">
                      发送申请：{{ request.requestMsg || '未填验证消息' }}
                    </div>
                  </ElCol>
                  <ElCol :span="6" class="request-actions">
                    <span v-if="request.status === 0" class="status-text warning">待处理</span>
                    <span v-else-if="request.status === 1" class="status-text success">对方已同意</span>
                    <span v-else-if="request.status === 2" class="status-text danger">对方已拒绝</span>
                  </ElCol>
                </ElRow>
                <div v-else class="mobile-request-item">
                  <div class="mobile-request-header">
                    <ElAvatar size="40" style="font-size: 16px; cursor:pointer;" @click="router.push('/user/profile?userId=' + request.toUserId)">
                      {{ request.userName.slice(-2) }}
                    </ElAvatar>
                    <div class="mobile-request-info">
                      <div class="mobile-request-name-id">
                        <span class="request-name">{{ request.userName }}</span>
                      </div>
                      <span class="request-time">{{ formatTime(request.createTime) }}</span>
                    </div>
                  </div>
                  <div class="request-msg" style="margin: 8px 0 12px 0;">
                    你发送了好友申请：{{ request.requestMsg || '' }}
                  </div>
                  <div class="mobile-request-actions">
                    <span v-if="request.status === 0" class="status-text warning">待处理</span>
                    <span v-else-if="request.status === 1" class="status-text success">对方已同意</span>
                    <span v-else-if="request.status === 2" class="status-text danger">对方已拒绝</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <el-icon size="36" style="color: #ccc;"><UserFilled /></el-icon>
              <p style="font-size: 13px;">暂无发出的好友申请</p>
            </div>
          </div>
        </div>

      </ElCol>
    </ElRow>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, inject } from 'vue'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'
import { UserFilled, Notification } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isDark = inject('isDark')

// 注入的全局数据和状态
const currentUserId = inject('currentUserId')
const friendRequests = inject('friendRequests')
const sentRequests = inject('sentRequests')
const notifications = inject('notifications')
const expandedImages = ref({})
const isMobile = inject('isMobile')

const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL

// 未读统计
const receivedUnread = computed(() => friendRequests.value.filter(r => r.status === 0).length)
const sentUnread = computed(() => sentRequests.value.filter(r => r.status === 0).length)
const notificationUnread = computed(() => notifications.value.filter(n => n.isRead === 0).length)

// 注入的方法
const formatTime = inject('formatDateTime')
const batchMarkAllRead = inject('batchMarkAllRead')
const acceptRequest = inject('acceptRequest')
const rejectRequest = inject('rejectRequest')

// 1. 改造原有的已读函数，增加 isSilent（是否静默）参数
const handleMarkAllRead = async (isSilent = false) => {
  try {
    // 调用后端的全部已读API
    const res = await request.put('/message/read-all')
    if (res.code === 200) {
      notificationUnread.value = 0 // 前端本地未读数归零

      // 🔥 核心体验：用户手动点按钮，给个爽快的绿提示；进页面自动消的，绝不弹窗打扰！
      if (!isSilent) {
        ElMessage.success('全部通知已阅 ~')
      }
    }
  } catch (e) {
    console.error('标记已读失败', e)
  }
}


// 2. 在组件挂载时，分两步走：
onMounted(() => {
  // 第二步：大厂 UX 精髓 —— 留出 2.5 秒的“目光扫描缓冲期”，再静默把红点抹掉
  setTimeout(() => {
    if (notificationUnread.value > 0) {
      handleMarkAllRead(true) // 传入 true，开启静默阅毕！
    }
  }, 2500)
})
</script>

<style scoped>
/* ======================================
   基础容器及高宽精准控制 (1259.2 * 928)
======================================== */
.message-container {
  padding: 16px;
  max-width: 1300px;
  margin: 0 auto;
  box-sizing: border-box;
}

.dashboard-row {
  display: flex;
  flex-wrap: wrap;
}

.dashboard-col {
  display: flex;
  flex-direction: column;
}

.side-column {
  gap: 20px; /* 右侧两块面板之间的间距 */
}

/* 区域高度精准强控 */
.system-notifications-box {
  min-height: 840px !important;
  max-height: 840px !important;
}

.friend-requests-box {
  min-height: 410px !important;
  max-height: 410px !important; /* 410 + 410 + 20(gap) = 840px 与左侧像素级对齐 */
}

/* ======================================
   防透光卡片设计 (实体不透明底色)
======================================== */
.dashboard-card {
  /* background-color: #ffffff !important; 🗑️ 删掉 */
  /* border: 1px solid #eaedf1; */
  /* border-radius: 12px; */
  /* box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04); */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* (这里不需要加额外的边框和圆角了，因为 glass-panel 已经帮你做好了) */
}

.card-panel-header {
  padding: 14px 20px;
  /* border-bottom: 1px solid #eaedf1; */
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  /* background-color: #ffffff !important; 🗑️ 删掉 */
  background-color: transparent !important; /* 强制透明 */
  z-index: 2;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-icon {
  font-size: 18px;
  color: #409eff;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #161823;
}

/* 面板内部容器铺上一层舒适的微灰，用来烘托纯白通知单页 */
.card-panel-body {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
  /* background-color: #f8f9fa !important; 🗑️ 删掉 */
  background-color: transparent !important; /* 强制透明 */
}

/* ======================================
   通知与申请的小卡片配置 (#FFFFFF)
======================================== */
.notification-item, .request-item {
  /* background-color: #ffffff !important; 🗑️ 删掉纯白 */
  background-color: rgba(255, 255, 255, 0.4) !important; /* 浅色半透明，透出一点底层质感 */
  /* border: 1px solid #eaedf1; */
  border: 1px dashed var(--glass-border); /* 改为虚线边框更柔和 */
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.015);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s;
}

.notification-item:hover, .request-item:hover {
  transform: translateY(-2px); /* 悬浮幅度微调 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  background-color: rgba(255, 255, 255, 0.6) !important; /* 悬浮时变得更实一点 */
}

.notification-list, .request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ======================================
   用户名字及附属信息【严格左对齐】
======================================== */
.notification-header {
  margin-bottom: 6px;
  text-align: left;
}

.notification-name-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-start !important; /* 绝对左对齐 */
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
  text-align: left !important;
}

.notification-name {
  font-size: 14px;
  font-weight: 600;
  color: #161823;
}

.notification-type-badge {
  font-size: 12px;
  padding: 2px 6px;
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
  border-radius: 4px;
}
.notification-type-badge.like {
  background: rgba(255, 125, 0, 0.08);
  color: #ff7d00;
}

.notification-time {
  font-size: 12px;
  color: #86909c;
}

/* 右侧申请内容排流 */
.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.request-name {
  font-size: 14px;
  font-weight: 600;
  color: #161823;
}

.request-time {
  font-size: 12px;
  color: #86909c;
}

.request-msg {
  font-size: 13px;
  color: #666;
  text-align: left;
}

.request-actions {
  display: flex;
  gap: 4px;
  justify-content: flex-end;
  align-items: center;
}

/* 状态字色 */
.status-text { font-size: 13px; }
.status-text.success { color: #52c41a; }
.status-text.danger { color: #ff4d4f; }
.status-text.warning { color: #faad14; }

/* 自定义滚动条 */
.scrollbar-custom::-webkit-scrollbar { width: 5px; }
.scrollbar-custom::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.06); border-radius: 4px; }

/* 媒体图片展示网格 */
.notification-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-top: 10px;
  max-width: 360px;
}
.notification-img { width: 76px; height: 76px; border-radius: 4px; cursor: pointer; }
.expand-btn {
  background: #f5f5f5; border-radius: 4px; cursor: pointer; font-size: 12px;
  display: flex; align-items: center; justify-content: center; color: #666;
}

.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 0; color: #999;
}

/* ======================================
   🌑 暗黑模式完美映射隔离 (修复选择器与文字颜色)
======================================== */
:global(html.dark) {
  /* 修复：时间文字 */
  .notification-time,
  .request-time {
    color: #9ca3af;
  }

  .dashboard-card {
    border-color: #374151;
  }

  .card-panel-header {
    background-color: transparent !important;
    border-color: #374151;
  }

  .card-panel-body {
    background-color: transparent !important;
  }

  .notification-item,
  .request-item {
    background-color: rgba(0, 0, 0, 0.2) !important; /* 黑色半透明底 */
    border-color: rgba(255, 255, 255, 0.05);
  }

  .notification-item:hover,
  .request-item:hover {
    background-color: rgba(0, 0, 0, 0.4) !important;
  }

  /* 修复：用户名 */
  .notification-name,
  .request-name {
    color: #e5e7eb;
  }

  .expand-btn {
    background: #374151;
    color: #9ca3af;
  }

  /* 👇 核心修复：补充原本写死的灰色字体 */
  .request-msg {
    color: #d1d5db; /* 申请内容变浅银白色 */
  }

  .empty-state {
    color: #9ca3af; /* 暂无数据的提示文字变浅 */
  }

  .empty-state .el-icon {
    color: #4b5563 !important; /* 暂无数据的图标调暗，不刺眼 */
  }
}
/* 移动端覆盖 */
/* ======================================
   📱 移动端专属适配覆盖
======================================== */
@media (max-width: 768px) {
  /* 1. 保证 PC 端左右两列在手机端上下堆叠时，有呼吸间距 */
  .dashboard-row {
    row-gap: 16px;
  }

  /* 2. 🔥 修复：设置合理的最大高度，避免区域过长，保持内部独立滚动 */
  .system-notifications-box {
    min-height: auto !important;
    max-height: 60vh !important; /* 限制在60%视口高度 */
  }
  .friend-requests-box {
    min-height: auto !important;
    max-height: 40vh !important; /* 限制在40%视口高度 */
  }

  /* 补充限制最大宽度，防止内容被强行拉扯超出屏幕 */
  .system-notifications-box, .friend-requests-box {
    width: 100% !important;
    max-width: 100vw !important;
  }
  .message-container { padding: 12px; }

  /* 3. 保证右侧两块（收到的申请、发出的申请）在手机端的间距 */
  .side-column { gap: 16px; }

  /* ==================== 移动端内部卡片排版 ==================== */
  .mobile-notification-header, .mobile-request-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
  }

  .mobile-notification-info, .mobile-request-info {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    text-align: left;
  }

  .mobile-notification-name-type {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  /* 图片九宫格在手机端自适应等比缩放 */
  .mobile-notification-images {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
    margin-top: 8px;
  }

  .mobile-notification-img {
    width: 100%;
    aspect-ratio: 1 / 1;
    border-radius: 6px;
  }

  /* 移动端的好友操作按钮（同意/拒绝）置于右下角 */
  .mobile-request-actions {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    margin-top: 8px;
    padding-top: 10px;
    border-top: 1px dashed var(--glass-border); /* 加一道浅浅的分割线提升层级感 */
  }
}
</style>