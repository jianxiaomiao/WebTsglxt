<template>
  <div class="chat-container">
    <ElRow :gutter="0" class="glass-panel chat-main-row">
      <!-- 左边：会话+好友列表（原代码完全不变） -->
      <ElCol
          :span="isMobile ? (showChatView ? 0 : 24) : 6"
          class="session-list-col"
          :style="{ display: isMobile && showChatView ? 'none' : 'block' }"
      >
        <ElTabs v-model="activeTab" type="border-card" style="height: 100%;">
          <ElTabPane label="最近聊天" name="session">
            <div class="session-list">
              <div
                  v-for="session in sessionList"
                  :key="session.targetUserId"
                  class="session-item"
                  :class="{ active: currentSession?.targetUserId === session.targetUserId }"
                  @click="selectSession(session)"
              >
                <!-- 🔥 AI会话头像固定显示🐱 -->
                <ElAvatar size="48" style="font-size: 18px; cursor: pointer;" @click.stop="goToUserProfile(session.targetUserId)">
                  {{ session.targetUserId === AI_DOUBAO_ID ? '🐱' : session.userName.slice(-2) }}
                </ElAvatar>
                <div class="session-info">
                  <div class="session-name">{{ session.userName }}</div>
                  <div class="session-last-message">{{ session.lastMessage || '暂无消息' }}</div>
                </div>
                <div class="session-right">
                  <div class="session-time">{{ formatTime(session.lastMessageTime) }}</div>
                  <ElBadge :value="session.unreadCount" :hidden="session.unreadCount === 0" class="session-badge" />
                </div>
              </div>
              <div v-if="sessionList.length === 0" class="empty-list">
                <p>暂无最近聊天</p>
              </div>
            </div>
          </ElTabPane>
          <ElTabPane label="我的好友" name="friend">
            <div class="session-list">
              <div
                  v-for="friend in friendList"
                  :key="friend.friendId"
                  class="session-item friend-item"
                  :class="{ active: currentSession?.targetUserId === friend.friendId }"
                  @click="selectFriend(friend)"
              >
                <ElAvatar size="48" style="font-size: 18px; cursor: pointer;" @click.stop="goToUserProfile(friend.friendId)">
                  {{ friend.userName.slice(-2) }}
                </ElAvatar>
                <div class="session-info">
                  <div class="session-name">{{ friend.userName }}</div>
                  <div class="session-last-message">{{ friend.friendRemark || '暂无备注' }}</div>
                </div>
              </div>
              <div v-if="friendList.length === 0" class="empty-list">
                <p>暂无好友，去论坛添加吧~</p>
              </div>
            </div>
          </ElTabPane>
          <ElTabPane label="查询好友" name="search">
            <!-- 电脑端：显示原来的提示 -->
            <div v-if="!isMobile" class="empty-list">
              <p>点击右侧搜索用户并添加好友</p>
            </div>

            <!-- 🔥 手机端：直接显示搜索栏和结果 -->
            <div v-else class="mobile-search-container">
              <div class="search-bar">
                <ElSelect v-model="queryUserType" placeholder="查询类型" style="width: 120px;">
                  <ElOption label="按ID查询" value="userId" />
                  <ElOption label="按姓名查询" value="name" />
                </ElSelect>
                <ElInput
                    v-model="searchUserKey"
                    placeholder="输入用户ID或姓名"
                    style="flex: 1; margin: 0 12px;"
                    suffix-icon="Search"
                    @keyup.enter="searchUsers"
                />
                <el-button type="primary" icon="Search" @click="searchUsers" round>搜索</el-button>
              </div>
              <div class="search-results">
                <div v-if="searchResult.length > 0" class="result-list">
                  <div
                      v-for="user in searchResult"
                      :key="user.userId"
                      class="user-result-item"
                  >
                    <ElAvatar size="48" style="font-size: 18px; cursor: pointer;" @click.stop="goToUserProfile(user.userId)">
                      {{ user.name?.slice(-2) || '未知' }}
                    </ElAvatar>
                    <div class="user-info" style="text-align:left; cursor: pointer;" @click="router.push('/user/profile?userId=' + user.userId)">
                      <div class="user-name">{{ user.name || '未知用户' }}</div>
                      <div class="user-id">({{ user.userId }})</div>
                    </div>

                    <ElButton
                        :type="getButtonType(user)"
                        size="small"
                        @click="handleAddFriend(user.userId, user.name)"
                        :disabled="isButtonDisabled(user)"
                    >
                      {{ getButtonText(user) }}
                    </ElButton>
                  </div>
                </div>
                <div v-else class="empty-search">
                  <el-icon size="64" style="color: #ccc;"><Search /></el-icon>
                  <p>选择查询方式，输入内容搜索用户吧~</p>
                </div>
              </div>
            </div>
          </ElTabPane>
        </ElTabs>
      </ElCol>

      <!-- 🔥 右侧：给外层div加上height:100%，确保占满ElCol高度 -->
      <ElCol :span="18" class="chat-window-col">
        <!-- 🔥 情况1：activeTab不是search → 新增style="height: 100%;" -->
        <div v-if="activeTab !== 'search'" style="height: 100%;">
          <div v-if="currentSession" class="chat-window">
            <div class="chat-header">
              <!-- 🔥 新增：手机端返回箭头（仅手机端显示） -->
              <el-icon
                  v-if="isMobile"
                  class="back-icon"
                  @click="backToSessionList"
                  style="margin-right: 12px; font-size: 20px; cursor: pointer;"
              >
                <ArrowLeft />
              </el-icon>
              <!-- 🔥 AI聊天顶部头像固定🐱 -->
              <ElAvatar size="36" style="font-size: 16px; cursor: pointer;" @click.stop="goToUserProfile(currentSession.targetUserId)">
                {{ currentSession.targetUserId === AI_DOUBAO_ID ? '🐱' : currentSession.userName.slice(-2) }}
              </ElAvatar>
              <span class="chat-name">{{ currentSession.userName }}</span>
            </div>
            <div class="chat-messages" ref="chatMessagesRef" @scroll="handleChatScroll">
              <!-- ====================== 🔥 聊天骨架屏（加载时显示） ====================== -->
              <template v-if="showChatSkeleton">
                <div v-for="i in skeletonCount" :key="i" class="message-item" :class="{ 'message-self': i % 2 === 0 }">
                  <ElAvatar size="32" style="font-size: 14px;">
                    <template v-if="i%2===0">我</template>
                    <template v-else>🐱</template>
                  </ElAvatar>
                  <div class="message-content">
                    <div class="message-text skeleton-bubble"></div>
                    <div class="message-time" :class="{ 'time-self': i % 2 === 0 }">
                      <div class="skeleton-text"></div>
                    </div>
                  </div>
                </div>
              </template>

              <!-- ====================== 原有消息列表（骨架屏隐藏后显示） ====================== -->
              <template v-else>
                <!-- ✅ 提示放在最顶部 -->
                <div v-if="noMoreData && messageList.length > 0" style="text-align:center; padding:10px; color:#999;">
                  已经到最早的消息啦~
                </div>

                <!-- 1. 普通好友聊天 -->
                <template v-if="currentSession.targetUserId !== AI_DOUBAO_ID">
                  <transition-group name="el-zoom-in" tag="div">
                    <div
                        v-for="(message, index) in messageList"
                        :key="message.id"
                        class="message-item"

                        v-memo="[
                          message.messageContent,
                          message.isRecalled,
                          message.isRead,
                          message.isNew,
                          message.cards,
                          currentPlayingVoiceId === message.id && isVoicePlaying,
                          imageGalleryState[message.id]?.expanded,
                          imageGalleryState[message.id]?.currentIndex
                        ]"

                        :class="{
                          'message-self': message.isRecalled
                            ? String(message.fromUserId) === String(currentUserId)
                            : String(message.fromUserId) === String(currentUserId)
                          }"
                        :data-new="message.isNew || false"
                    >
                      <ElAvatar size="32" style="font-size: 14px; cursor: pointer;" @click="goToUserProfile(message.fromUserId)">
                        {{ getMessageUserName(message.fromUserId).slice(-2) }}
                      </ElAvatar>
                      <div class="message-content">
                        <!-- 🔥 引用消息逻辑（已适配 文字/图片/文件） -->
                        <div v-if="message.replyToId" class="quote-message glass-effect">
                          <div class="quote-content">
                            <!-- 1. 文字引用 -->
                            <template v-if="getQuotedMessageType(message.replyToId) === 1">
                              {{ getQuotedMessageContent(message.replyToId) }}
                            </template>
                            <!-- 2. 图片引用（只显示图片） -->
                            <template v-else-if="getQuotedMessageType(message.replyToId) === 2">
                              <el-image
                                  class="quote-image"
                                  :src="IMAGE_BASE_URL + getQuotedMessageContent(message.replyToId)"
                                  :preview-src-list="[IMAGE_BASE_URL + getQuotedMessageContent(message.replyToId)]"
                                  preview
                                  preview-teleported
                                  fit="cover"
                                  alt="引用图片"
                                  style="width:80px;height:80px;border-radius:4px;"
                              />
                            </template>
                            <!-- 3. 文件引用（只显示文件） -->
                            <template v-else-if="getQuotedMessageType(message.replyToId) === 3">
                              <div class="quote-file">
                                <span class="file-icon">{{ getFileIcon(getQuotedMessageContent(message.replyToId)) }}</span>
                                <span class="file-name">{{ getQuotedMessageContent(message.replyToId) }}</span>
                              </div>
                            </template>
                            <!-- 3. 视频引用（只显示视频） -->
                            <template v-else-if="getQuotedMessageType(message.replyToId) === 4">
                              {{"[视频]"}}
                            </template>
                            <!-- 兜底 -->
                            <template v-else>
                              {{ getQuotedMessageContent(message.replyToId) }}
                            </template>
                          </div>
                        </div>

                        <!-- 🔥 统一消息渲染：支持所有类型（修复转发重复问题） -->
                        <template v-if="!message.isRecalled">
                          <!-- 1. 转发消息：先显示转发前缀（只显示一次） -->
                          <div v-if="isForwardMessage(message.messageContent)" class="forward-prefix  glass-effect">
                            {{ getForwardPrefix(message.messageContent) }}
                          </div>

                          <!-- 2. 图片消息（新增单张/多张画廊逻辑） -->
                          <template v-if="message.messageType === 2">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <!-- 单张图片 -->
                              <template v-if="message.parsed.images.length === 1">
                                <div class="message-image">
                                  <el-image
                                      class="talk-image"
                                      :src="IMAGE_BASE_URL + message.parsed.images[0]"
                                      :preview-src-list="[IMAGE_BASE_URL + message.parsed.images[0]]"
                                      preview
                                      preview-teleported
                                      fit="cover"
                                      alt="聊天图片"
                                      error-src="/dist/logo.png"
                                  >
                                    <template #placeholder>
                                      <div class="image-loading">
                                        <el-icon class="is-loading"><Loading /></el-icon>
                                      </div>
                                    </template>
                                  </el-image>
                                </div>
                              </template>

                              <!-- 多张图片：折叠画廊 -->
                              <template v-else-if="message.parsed.images.length > 1">
                                <div class="image-gallery-container" :class="{'is-self': String(message.fromUserId) === String(currentUserId)}">
                                  <div v-show="!initGalleryState(message.id).expanded" class="gallery-folded">
                                    <div class="gallery-track">
                                      <div
                                          v-for="(imgUrl, imgIndex) in message.parsed.images"
                                          :key="`fold-${imgIndex}-${message.id}`"
                                          class="gallery-item"
                                          :class="{
                                          'is-center': imgIndex === initGalleryState(message.id).currentIndex,
                                          'is-left': imgIndex === initGalleryState(message.id).currentIndex - 1,
                                          'is-right': imgIndex === initGalleryState(message.id).currentIndex + 1,
                                          'is-hidden': Math.abs(imgIndex - initGalleryState(message.id).currentIndex) > 1
                                        }"
                                          @click="imgIndex < initGalleryState(message.id).currentIndex ? prevGalleryImg(message.id) : (imgIndex > initGalleryState(message.id).currentIndex ? nextGalleryImg(message.id, message.parsed.images.length) : null)"
                                      >
                                        <el-image
                                            v-if="Math.abs(imgIndex - initGalleryState(message.id).currentIndex) <= 1"
                                            class="talk-image gallery-img"
                                            :src="IMAGE_BASE_URL + imgUrl"
                                            :preview-src-list="imgIndex === initGalleryState(message.id).currentIndex ? message.parsed.images.map(url => IMAGE_BASE_URL + url) : []"
                                            :initial-index="imgIndex"
                                            preview-teleported
                                            fit="cover"
                                            :style="{ pointerEvents: imgIndex === initGalleryState(message.id).currentIndex ? 'auto' : 'none' }"
                                        />
                                      </div>
                                    </div>
                                    <div class="gallery-expand-btn glass-effect" @click="toggleGalleryExpand(message.id)">
                                      <el-icon><ArrowDown /></el-icon>
                                    </div>
                                  </div>

                                  <!-- 展开状态 -->
                                  <div v-show="initGalleryState(message.id).expanded" class="gallery-expanded">
                                    <transition-group name="gallery-expand-list" tag="div" class="gallery-expanded-list">
                                      <div
                                          v-for="(imgUrl, imgIndex) in message.parsed.images"
                                          :key="`exp-${imgIndex}-${message.id}`"
                                          class="message-image"
                                      >
                                        <el-image
                                            class="talk-image"
                                            :src="IMAGE_BASE_URL + imgUrl"
                                            :preview-src-list="message.parsed.images.map(url => IMAGE_BASE_URL + url)"
                                            :initial-index="imgIndex"
                                            preview
                                            preview-teleported
                                            fit="cover"
                                        />
                                      </div>
                                    </transition-group>
                                    <div class="gallery-collapse-btn glass-effect" @click="toggleGalleryExpand(message.id)">
                                      <el-icon><ArrowUp /></el-icon>
                                    </div>
                                  </div>
                                </div>
                              </template>

                              <!-- 图片下方文字/卡片 -->
                              <div
                                  v-if="message.pureText || message.cards.length > 0"
                                  class="file-text-wrap"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                              >
                                <el-divider
                                    border-style="dashed"
                                    style="margin:6px 0; width:100%; border-color:#999;"
                                />
                                <!-- 卡片 -->
                                <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                                  <div v-if="message.cards.length === 1" class="book-share-card-wrapper">
                                    <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                                  </div>

                                  <div v-else class="multi-card-slider-wrapper">
                                    <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                      <el-icon><ArrowLeft /></el-icon>
                                    </div>

                                    <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                      <div
                                          v-for="card in message.cards"
                                          :key="card.id"
                                          class="slider-item"
                                          v-html="parseBookLinkToCard(card.link, true)"
                                      ></div>
                                    </div>

                                    <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                      <el-icon><ArrowRight /></el-icon>
                                    </div>
                                  </div>
                                </div>
                                <!-- 纯文字（新增磨砂玻璃） -->
                                <div v-if="message.pureText" class="message-text glass-effect">
                                  <span v-html="formatMentionText(message.pureText.replace(/\n/g, '<br>'))"></span>
                                </div>
                              </div>
                            </template>
                          </template>

                          <!-- 3. 文件消息（纯文件/文字+文件/批量文件） -->
                          <template v-else-if="message.messageType === 3">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <!-- 批量文件渲染 -->
                              <template v-for="(doc, docIndex) in message.parsed.documents" :key="`doc-${docIndex}-${message.id}`">
                                <el-card shadow="hover" class="chat-file-card glass-effect">
                                  <div class="file-card-content">
                                    <!-- 文件图标 -->
                                    <div class="file-icon">{{ getFileIcon(doc.url) }}</div>
                                    <!-- 文件信息 -->
                                    <div class="file-info" @click="downloadChatFile(doc.url)" style="cursor: pointer;">
                                      <div class="file-name">{{ truncateFileName(doc.meta.fileName || doc.url.split('/').pop() || '') }}</div>
                                      <div class="file-size">{{ doc.meta.fileSize || '未知大小' }}</div>
                                    </div>
                                  </div>
                                </el-card>
                              </template>

                              <!-- 文件下方文字/卡片 -->
                              <div
                                  v-if="message.pureText || message.cards.length > 0"
                                  class="file-text-wrap"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                              >
                                <el-divider
                                    border-style="dashed"
                                    style="margin: 6px 0; border-color: #999999; width: 100%;"
                                />
                                <!-- 先渲染卡片 -->
                                <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                                  <div v-if="message.cards.length === 1" class="book-share-card-wrapper">
                                    <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                                  </div>

                                  <div v-else class="multi-card-slider-wrapper">
                                    <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                      <el-icon><ArrowLeft /></el-icon>
                                    </div>

                                    <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                      <div
                                          v-for="card in message.cards"
                                          :key="card.id"
                                          class="slider-item"
                                          v-html="parseBookLinkToCard(card.link, true)"
                                      ></div>
                                    </div>

                                    <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                      <el-icon><ArrowRight /></el-icon>
                                    </div>
                                  </div>
                                </div>
                                <!-- 再渲染纯文字 -->
                                <div v-if="message.pureText" class="message-text">
                                  {{ message.pureText }}
                                </div>
                              </div>
                            </template>
                          </template>

                          <!-- 视频消息（纯视频/文字+视频/批量视频） -->
                          <template v-else-if="message.messageType === 4">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <!-- 批量视频渲染 -->
                              <div
                                  v-for="(videoUrl, videoIndex) in message.parsed.videos"
                                  :key="`video-${videoIndex}-${message.id}`"
                                  class="video-msg-wrap"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) || message.formType === 0 }"
                              >
                                <video
                                    class="talk-video"
                                    :src="FILE_BASE_URL + videoUrl"
                                    controls
                                    preload="metadata"
                                    style="max-width: 300px; max-height: 200px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.15);"
                                ></video>
                              </div>

                              <!-- 视频下方文字 -->
                              <div
                                  v-if="message.pureText"
                                  class="message-text glass-effect"
                                  style="margin-top: 6px;"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) || message.formType === 0 }"
                              >
                                {{ message.pureText }}
                              </div>
                            </template>
                          </template>

                          <!-- 🔥 新增：语音消息（纯语音/文字+语音/批量语音） -->
                          <template v-else-if="message.messageType === 5">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <template v-for="(voice, voiceIndex) in message.parsed.voices" :key="`voice-${voiceIndex}-${message.id}`">
                                <div
                                    class="voice-msg-wrap glass-effect"
                                    :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                                    @click="toggleVoicePlay(message.id, voice.url)"
                                >
                                  <!-- 播放按钮 -->
                                  <div class="voice-play-btn">
                                    <span v-if="!(currentPlayingVoiceId === message.id && isVoicePlaying)">▶</span>
                                    <!-- 播放动效：三条波动的竖线 -->
                                    <div v-else class="voice-wave">
                                      <span></span>
                                      <span></span>
                                      <span></span>
                                    </div>
                                  </div>
                                  <!-- 语音时长 -->
                                  <span class="voice-duration">{{ voice.duration }}''</span>
                                </div>
                              </template>

                              <!-- 语音下方文字 -->
                              <div
                                  v-if="message.pureText"
                                  class="message-text glass-effect"
                                  style="margin-top: 6px;"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                              >
                                {{ message.pureText }}
                              </div>
                            </template>
                          </template>
                          <!-- 3. 纯文字消息（✅ 卡片独立渲染） -->
                          <template v-else>
                            <!-- 🔥 第一步：先渲染所有卡片（独立于气泡） -->
                            <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                              <div v-if="message.cards.length === 1" class="book-share-card-wrapper">
                                <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                              </div>

                              <div v-else class="multi-card-slider-wrapper">
                                <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                  <el-icon><ArrowLeft /></el-icon>
                                </div>

                                <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                  <div
                                      v-for="card in message.cards"
                                      :key="card.id"
                                      class="slider-item"
                                      v-html="parseBookLinkToCard(card.link, true)"
                                  ></div>
                                </div>

                                <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                  <el-icon><ArrowRight /></el-icon>
                                </div>
                              </div>
                            </div>

                            <!-- 🔥 第二步：再渲染纯文字气泡（仅当有文字时显示） -->
                            <div v-if="message.pureText" class="message-text glass-effect">
                              <span v-html="formatMentionText(message.pureText.replace(/\n/g, '<br>'))"></span>
                            </div>
                          </template>
                        </template>

                        <!-- 4. 撤回消息（保持不变） -->
                        <template v-else>
                          <div class="message-text glass-effect">
                            {{ String(message.fromUserId) === String(currentUserId)
                              ? '你撤回了一条消息'
                              : '对方撤回了一条消息' }}
                            <a
                                v-if="String(message.fromUserId) === String(currentUserId)
              && message.messageType === 1
              && getMinutesDiffFromNow(message.recallTime) <= 2"
                                class="reedit-link"
                                @click="reeditMessageHandler(message)"
                            >
                              重新编辑
                            </a>
                          </div>
                        </template>

                        <!-- 消息时间 + 已读状态（保持不变） -->
                        <div class="message-time-row">
                            <span class="message-time" :class="{ 'time-self': String(message.fromUserId) === String(currentUserId) }">
                              {{ formatTime(message.createTime) }}
                            </span>
                          <el-badge
                              v-if="String(message.fromUserId) === String(currentUserId) && isLastSelfMessage(index)"
                              :value="message.isRead === 1 ? '已读' : '未读'"
                              type="info"
                              class="read-status"
                          >
                          </el-badge>
                        </div>
                      </div>
                    </div>
                  </transition-group>
                </template>

                <!-- 2. AI豆包聊天（✅ 完全适配formType+messageType规则，复用普通聊天渲染） -->
                <template v-else>
                  <transition-group name="el-zoom-in" tag="div">
                    <div
                        v-for="message in messageList"
                        :key="message.id"
                        class="message-item"

                        v-memo="[
                          message.messageContent,
                          message.isRecalled,
                          message.isNew,
                          message.cards,
                          currentPlayingVoiceId === message.id && isVoicePlaying,
                          imageGalleryState[message.id]?.expanded,
                          imageGalleryState[message.id]?.currentIndex
                        ]"

                        :class="{ 'message-self': message.formType === 0 }"
                        :data-new="message.isNew || false"
                    >
                      <ElAvatar
                          size="32"
                          style="font-size: 14px; cursor: pointer;"
                      >
                        {{ message.formType === 0 ? currentUserInfo.name.slice(-2) : '🐱' }}
                      </ElAvatar>
                      <div class="message-content">
                        <!-- 🔥 统一消息渲染：和普通聊天完全一致，适配新格式+批量 -->
                        <template v-if="!message.isRecalled">
                          <!-- 1. 转发消息前缀 -->
                          <div v-if="isForwardMessage(message.messageContent)" class="forward-prefix glass-effect">
                            {{ getForwardPrefix(message.messageContent) }}
                          </div>

                          <!-- 图片消息（适配画廊） -->
                          <template v-if="message.messageType === 2">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <!-- 单张图片 -->
                              <template v-if="message.parsed.images.length === 1">
                                <div class="message-image">
                                  <el-image
                                      class="talk-image"
                                      :src="IMAGE_BASE_URL + message.parsed.images[0]"
                                      :preview-src-list="[IMAGE_BASE_URL + message.parsed.images[0]]"
                                      preview
                                      preview-teleported
                                      fit="cover"
                                      alt="聊天图片"
                                      error-src="/dist/logo.png"
                                  >
                                    <template #placeholder>
                                      <div class="image-loading"><el-icon class="is-loading"><Loading /></el-icon></div>
                                    </template>
                                  </el-image>
                                </div>
                              </template>

                              <!-- 多张图片：折叠画廊 -->
                              <template v-else-if="message.parsed.images.length > 1">
                                <div class="image-gallery-container" :class="{'is-self': message.formType === 0}">
                                  <div v-show="!initGalleryState(message.id).expanded" class="gallery-folded">
                                    <div class="gallery-track">
                                      <div
                                          v-for="(imgUrl, imgIndex) in message.parsed.images"
                                          :key="`fold-${imgIndex}-${message.id}`"
                                          class="gallery-item"
                                          :class="{
                                          'is-center': imgIndex === initGalleryState(message.id).currentIndex,
                                          'is-left': imgIndex === initGalleryState(message.id).currentIndex - 1,
                                          'is-right': imgIndex === initGalleryState(message.id).currentIndex + 1,
                                          'is-hidden': Math.abs(imgIndex - initGalleryState(message.id).currentIndex) > 1
                                        }"
                                          @click="imgIndex < initGalleryState(message.id).currentIndex ? prevGalleryImg(message.id) : (imgIndex > initGalleryState(message.id).currentIndex ? nextGalleryImg(message.id, message.parsed.images.length) : null)"
                                      >
                                        <el-image
                                            v-if="Math.abs(imgIndex - initGalleryState(message.id).currentIndex) <= 1"
                                            class="talk-image gallery-img"
                                            :src="IMAGE_BASE_URL + imgUrl"
                                            :preview-src-list="imgIndex === initGalleryState(message.id).currentIndex ? message.parsed.images.map(url => IMAGE_BASE_URL + url) : []"
                                            :initial-index="imgIndex"
                                            preview-teleported
                                            fit="cover"
                                            :style="{ pointerEvents: imgIndex === initGalleryState(message.id).currentIndex ? 'auto' : 'none' }"
                                        />
                                      </div>
                                    </div>
                                    <div class="gallery-expand-btn glass-effect" @click="toggleGalleryExpand(message.id)">
                                      <el-icon><ArrowDown /></el-icon>
                                    </div>
                                  </div>

                                  <!-- 展开状态 -->
                                  <div v-show="initGalleryState(message.id).expanded" class="gallery-expanded">
                                    <transition-group name="gallery-expand-list" tag="div" class="gallery-expanded-list">
                                      <div
                                          v-for="(imgUrl, imgIndex) in message.parsed.images"
                                          :key="`exp-${imgIndex}-${message.id}`"
                                          class="message-image"
                                      >
                                        <el-image
                                            class="talk-image"
                                            :src="IMAGE_BASE_URL + imgUrl"
                                            :preview-src-list="message.parsed.images.map(url => IMAGE_BASE_URL + url)"
                                            :initial-index="imgIndex"
                                            preview
                                            preview-teleported
                                            fit="cover"
                                        />
                                      </div>
                                    </transition-group>
                                    <div class="gallery-collapse-btn glass-effect" @click="toggleGalleryExpand(message.id)">
                                      <el-icon><ArrowUp /></el-icon>
                                    </div>
                                  </div>
                                </div>
                              </template>

                              <!-- 图片下方文字/卡片 -->
                              <div
                                  v-if="message.pureText || message.cards.length > 0"
                                  class="file-text-wrap"
                                  :class="{ 'self-wrap': message.formType === 0 }"
                              >
                                <el-divider border-style="dashed" style="margin:6px 0; width:100%; border-color:#999;" />
                                <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                                  <div v-if="message.cards.length === 1" class="book-share-card-wrapper">
                                    <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                                  </div>

                                  <div v-else class="multi-card-slider-wrapper">
                                    <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                      <el-icon><ArrowLeft /></el-icon>
                                    </div>

                                    <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                      <div
                                          v-for="card in message.cards"
                                          :key="card.id"
                                          class="slider-item"
                                          v-html="parseBookLinkToCard(card.link, true)"
                                      ></div>
                                    </div>

                                    <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                      <el-icon><ArrowRight /></el-icon>
                                    </div>
                                  </div>
                                </div>
                                <div v-if="message.pureText" class="message-text glass-effect">
                                  <span v-html="formatMentionText(message.pureText.replace(/\n/g, '<br>'))"></span>
                                </div>
                              </div>
                            </template>
                          </template>

                          <!-- 3. 文件消息（批量/单个） -->
                          <template v-else-if="message.messageType === 3">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <template v-for="(doc, docIndex) in message.parsed.documents" :key="`doc-${docIndex}-${message.id}`">
                                <el-card shadow="hover" class="chat-file-card glass-effect">
                                  <div class="file-card-content">
                                    <div class="file-icon">{{ getFileIcon(doc.url) }}</div>
                                    <div class="file-info" @click="downloadChatFile(doc.url)" style="cursor: pointer;">
                                      <div class="file-name">{{ truncateFileName(doc.meta.fileName || doc.url.split('/').pop() || '') }}</div>
                                      <div class="file-size">{{ doc.meta.fileSize || '未知大小' }}</div>
                                    </div>
                                  </div>
                                </el-card>
                              </template>

                              <!-- 文件下方文字 -->
                              <div
                                  v-if="message.pureText || message.cards.length > 0"
                                  class="file-text-wrap"
                                  :class="{ 'self-wrap': message.formType === 0 }"
                              >
                                <el-divider
                                    border-style="dashed"
                                    style="margin: 6px 0; border-color: #999999; width: 100%;"
                                />
                                <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                                  <div v-if="message.cards.length === 1" class="book-share-card-wrapper">
                                    <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                                  </div>

                                  <div v-else class="multi-card-slider-wrapper">
                                    <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                      <el-icon><ArrowLeft /></el-icon>
                                    </div>

                                    <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                      <div
                                          v-for="card in message.cards"
                                          :key="card.id"
                                          class="slider-item"
                                          v-html="parseBookLinkToCard(card.link, true)"
                                      ></div>
                                    </div>

                                    <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                      <el-icon><ArrowRight /></el-icon>
                                    </div>
                                  </div>
                                </div>
                                <div v-if="message.pureText" class="message-text glass-effect">
                                  {{ message.pureText }}
                                </div>
                              </div>
                            </template>
                          </template>

                          <!-- 4. 视频消息（批量/单个）🔥 新增适配 -->
                          <template v-else-if="message.messageType === 4">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <div
                                  v-for="(videoUrl, videoIndex) in message.parsed.videos"
                                  :key="`video-${videoIndex}-${message.id}`"
                                  class="video-msg-wrap"
                                  :class="{ 'self-wrap': message.formType === 0 }"
                              >
                                <video
                                    class="talk-video"
                                    :src="FILE_BASE_URL + videoUrl"
                                    controls
                                    preload="metadata"
                                    style="max-width: 300px; max-height: 200px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.15);"
                                ></video>
                              </div>
                              <!-- 视频下方文字 -->
                              <div
                                  v-if="message.pureText"
                                  class="message-text glass-effect"
                                  style="margin-top: 6px;"
                                  :class="{ 'self-wrap': message.formType === 0 }"
                              >
                                {{ message.pureText }}
                              </div>
                            </template>
                          </template>

                          <!-- 🔥 新增：语音消息（纯语音/文字+语音/批量语音） -->
                          <template v-else-if="message.messageType === 5">
                            <template v-for="realContent in [getForwardContent(message.messageContent)]" :key="realContent">
                              <template v-for="(voice, voiceIndex) in message.parsed.voices" :key="`voice-${voiceIndex}-${message.id}`">
                                <div
                                    class="voice-msg-wrap glass-effect"
                                    :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                                    @click="toggleVoicePlay(message.id, voice.url)"
                                >
                                  <!-- 播放按钮 -->
                                  <div class="voice-play-btn">
                                    <span v-if="!(currentPlayingVoiceId === message.id && isVoicePlaying)">▶</span>
                                    <!-- 播放动效：三条波动的竖线 -->
                                    <div v-else class="voice-wave">
                                      <span></span>
                                      <span></span>
                                      <span></span>
                                    </div>
                                  </div>
                                  <!-- 语音时长 -->
                                  <span class="voice-duration">{{ voice.duration }}''</span>
                                </div>
                              </template>

                              <!-- 语音下方文字 -->
                              <div
                                  v-if="message.pureText"
                                  class="message-text glass-effect"
                                  style="margin-top: 6px;"
                                  :class="{ 'self-wrap': String(message.fromUserId) === String(currentUserId) }"
                              >
                                {{ message.pureText }}
                              </div>
                            </template>
                          </template>

                          <!-- 5. 纯文字+卡片消息 -->
                          <template v-else>
                            <div v-if="message.cards && message.cards.length > 0" class="message-cards-container">

                              <div v-if="message.cards.length === 1" class="book-share-card-wrapper" :class="{ 'self-wrapper': message.formType === 0 }">
                                <div v-html="parseBookLinkToCard(message.cards[0].link, false)"></div>
                              </div>

                              <div v-else class="multi-card-slider-wrapper">
                                <div class="slider-arrow left" @click="scrollCards(message.id, 'left')">
                                  <el-icon><ArrowLeft /></el-icon>
                                </div>

                                <div class="multi-card-scroll-view" :id="'scroll-view-' + message.id">
                                  <div
                                      v-for="card in message.cards"
                                      :key="card.id"
                                      class="slider-item"
                                      v-html="parseBookLinkToCard(card.link, true)"
                                  ></div>
                                </div>

                                <div class="slider-arrow right" @click="scrollCards(message.id, 'right')">
                                  <el-icon><ArrowRight /></el-icon>
                                </div>
                              </div>
                            </div>
                            <div v-if="message.pureText" class="message-text glass-effect">
                              <span v-html="formatMentionText(message.pureText.replace(/\n/g, '<br>'))"></span>
                            </div>
                          </template>
                        </template>

                        <!-- 撤回消息 -->
                        <template v-else>
                          <div class="message-text glass-effect">
                            {{ message.formType === 0 ? '你撤回了一条消息' : '对方撤回了一条消息' }}
                          </div>
                        </template>
                      </div>
                    </div>
                  </transition-group>

                  <!-- 思考+生成图片气泡（原有逻辑不变） -->
                  <transition name="el-zoom-in-left">
                    <div v-if="isAiThinking" class="message-item">
                      <ElAvatar size="32" style="font-size: 14px; cursor: pointer;">🐱</ElAvatar>
                      <div class="message-content">
                        <div class="message-text thinking-text glass-effect">
                          <span v-html="aiThinkingText"></span><span class="typing-dots">...</span>
                        </div>
                      </div>
                    </div>
                  </transition>
                  <transition name="el-zoom-in-left">
                    <div v-if="isGeneratingImage" class="message-item">
                      <ElAvatar size="32" style="font-size: 14px; cursor: pointer;">🐱</ElAvatar>
                      <div class="message-content">
                        <div class="message-text thinking-text glass-effect">
                          <span>{{ generatingImageText }}</span><span class="typing-dots">...</span>
                        </div>
                      </div>
                    </div>
                  </transition>
                </template>
              </template>
            </div>

            <div class="chat-input-area">
              <!-- 1. 引用预览（单独占一行，最顶部 → 已适配所有类型） -->
              <div v-if="quotedMessage" class="quote-preview glass-effect">
                <div class="quote-preview-content">
                  <span class="quote-preview-title">引用：{{ getMessageUserName(quotedMessage.fromUserId) }}</span>
                  <!-- 文字预览 -->
                  <span v-if="quotedMessage.messageType === 1" class="quote-preview-text">
                    {{ extractPureText(quotedMessage.messageContent).substring(0,30) + '...' }}
                  </span>
                  <!-- 图片预览 -->
                  <span v-else-if="quotedMessage.messageType === 2" class="quote-preview-text">
                    🖼️ 图片
                  </span>
                  <!-- 文件预览 -->
                  <span v-else-if="quotedMessage.messageType === 3" class="quote-preview-text">
                    {{ getFileIcon(quotedMessage.messageContent) }} {{ parseFileMessage(quotedMessage.messageContent).fileName }}
                  </span>
                  <!-- 🔥 视频预览（新增） -->
                  <span v-else-if="quotedMessage.messageType === 4" class="quote-preview-text">
                    🎥 视频
                  </span>
                  <!-- 🔥 新增：语音预览 -->
                  <span v-else-if="quotedMessage.messageType === 5" class="quote-preview-text">
                    🎤 语音 {{ extractVoices(quotedMessage.messageContent)[0]?.duration || 0 }}''
                  </span>
                </div>
                <el-icon class="quote-close" @click="cancelQuote"><Close /></el-icon>
              </div>
              <!-- ✅ 通用卡片实时预览（支持所有5种类型） -->
              <div v-if="previewBookCards.length > 0" style="margin-bottom: 15px;">
                <div
                    v-for="card in previewBookCards"
                    :key="card.id"
                    class="book-share-card"
                >
                  <!-- 1. 书籍卡片（不变） -->
                  <template v-if="card.type === 'book'">
                    <img
                        v-if="!card.loading"
                        :src="IMAGE_BASE_URL + (card.data?.pictureName || '/default-book.png')"
                        class="book-share-card-cover"
                        alt="书籍封面"
                    >
                    <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                    <div class="book-share-card-info">
                      <p class="book-title">{{ card.loading ? '加载中...' : card.data?.bookname || '未知书籍' }}</p>
                      <p class="book-author">{{ card.loading ? '' : `作者：${card.data?.author || '未知'}` }}</p>
                      <p class="book-rating">{{ card.loading ? '' : `⭐ 评分：${card.data?.star || 0}` }}</p>
                      <p class="book-desc">{{ card.loading ? '' : (card.data?.information?.slice(0, 50) + '...' || '暂无简介') }}</p>
                    </div>
                  </template>

                  <!-- 🔥 修复2：书籍评论卡片类型判断（从book-comment改成bookComment） -->
                  <template v-else-if="card.type === 'bookComment'">
                    <img
                        v-if="!card.loading"
                        :src="IMAGE_BASE_URL + (card.data?.book?.pictureName || '/default-book.png')"
                        class="book-share-card-cover"
                        alt="书籍封面"
                    >
                    <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                    <div class="book-share-card-info">
                      <p class="book-title">📚 书籍评论</p>
                      <p class="book-author">{{ card.loading ? '书籍：加载中' : `书籍：${card.data?.book?.bookname || '未知书籍'}` }}</p>
                      <p class="book-rating">{{ card.loading ? '⭐ 评分：--' : `⭐ 评分：${card.data?.star || 0}` }}</p>
                      <p class="book-desc">{{ card.loading ? '评论内容：加载中...' : `评论内容：${card.data?.comment?.slice(0, 40) + '...' || '暂无内容'}` }}</p>
                      <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">
                        {{ card.loading ? '' : `发布时间：${formatTime(card.data?.time, true)}` }}
                      </p>
                    </div>
                  </template>

                  <!-- 🔥 修复3：用户论坛评论卡片类型判断（从user-comment改成userComment） -->
                  <template v-else-if="card.type === 'userComment'">
                    <div class="book-share-card-cover" style="background:#f0f2f5; display:flex; align-items:center; justify-content:center; font-size:24px;">💬</div>
                    <div class="book-share-card-info">
                      <p class="book-title">🔄 论坛评论</p>
                      <p class="book-author">{{ card.loading ? '作者：加载中' : `作者：${card.data?.user?.userName || '未知用户'} (ID:${card.data?.userid || '--'})` }}</p>
                      <p class="book-rating">{{ card.loading ? '👍 点赞：-- | 💬 回复：--' : `👍 点赞：${card.data?.prefer || 0} | 💬 回复：${getSubTotal(card.data?.commentId) || 0}` }}</p>
                      <p class="book-desc">{{ card.loading ? '评论内容：加载中...' : `评论内容：${card.data?.userComment?.slice(0, 40) + '...' || '暂无内容'}` }}</p>
                      <p class="book-time" style="color:#999; font-size:12px; margin-top:4px;">
                        {{ card.loading ? '' : `发布时间：${formatTime(card.data?.commentTime, true)}` }}
                      </p>
                    </div>
                  </template>

                  <!-- 4. 用户信息卡片（不变，根据你后端返回的字符串字段调整） -->
                  <template v-else-if="card.type === 'user'">
                    <div class="book-share-card-cover" style="background:#e6f7ff; display:flex; align-items:center; justify-content:center; font-size:24px;">👤</div>
                    <div class="book-share-card-info">
                      <p class="book-title">👤 用户信息</p>
                      <p class="book-author">{{ card.loading ? '用户名：加载中' : `用户名：${card.data?.userName || '未知用户'} (ID:${card.id})` }}</p>
                      <!-- ✅ 修复：后端直接返回字符串，不需要数字映射 -->
                      <p class="book-rating">{{ card.loading ? '性别：-- | 类型：--' : `性别：${card.data?.sex || '保密'} | 类型：${card.data?.typeName || '普通用户'}` }}</p>
                      <p class="book-desc">{{ card.loading ? '阅读时长：--小时 | 个人介绍：加载中...' : `阅读时长：${Math.floor((card.data?.read_time_long || 0) / 3600)}小时 | 个人介绍：${card.data?.bio?.slice(0, 30) + '...' || '暂无'}` }}</p>
                    </div>
                  </template>

                  <!-- 5. 笔记卡片（不变） -->
                  <template v-else-if="card.type === 'note'">
                    <img
                        v-if="!card.loading"
                        :src="IMAGE_BASE_URL + (card.data?.book?.pictureName || '/default-book.png')"
                        class="book-share-card-cover"
                        alt="书籍封面"
                    >
                    <img v-else src="/default-book.png" class="book-share-card-cover" alt="加载中">
                    <div class="book-share-card-info">
                      <p class="book-title">📝 读书笔记</p>
                      <p class="book-author">{{ card.loading ? '出自：《加载中》第--章' : `出自：《${card.data?.book?.bookname || '未知书籍'}》第${card.data?.chapterId || '--'}章` }}</p>
                      <p class="book-rating">{{ card.loading ? '类型：--' : `类型：${card.data?.typeName || '默认'}` }}</p>
                      <p class="book-desc">{{ card.loading ? '原文：加载中...' : `原文：${card.data?.text?.slice(0, 30) + '...' || '暂无'}` }}</p>
                      <p class="book-desc" style="margin-top:4px; color:#666;">
                        {{ card.loading ? '批注：加载中...' : `批注：${card.data?.readerComment?.slice(0, 30) + '...' || '暂无'}` }}
                      </p>
                    </div>
                  </template>
                </div>
              </div>
              <div v-if="localVideos.length > 0" class="chat-video-preview" style="margin-top: 8px;">
                <div class="video-list">
                  <div v-for="(vid, index) in localVideos" :key="index" class="video-item" style="position: relative; width: 120px; height: 120px; border-radius: 4px; overflow: hidden; border: 1px solid #ddd; background: #000;">
                    <video :src="vid.previewUrl" style="width: 100%; height: 100%; object-fit: cover;" controls></video>
                    <div class="delete-btn" @click="deleteChatVideo(index)" style="position: absolute; top: 2px; right: 2px; background: rgba(0,0,0,0.6); color: #fff; width: 18px; height: 18px; line-height: 16px; text-align: center; border-radius: 50%; cursor: pointer; z-index: 10;">×</div>
                  </div>
                </div>
              </div>

              <!-- 🔥 新增：本地语音预览 -->
              <div v-if="localVoices.length > 0" class="chat-voice-preview" style="margin-top: 8px;">
                <div class="voice-list">
                  <div v-for="(voice, index) in localVoices" :key="index" class="voice-item">
                    <div class="voice-preview-card">
                      <div class="voice-preview-play" @click="new Audio(voice.previewUrl).play()">▶</div>
                      <span class="voice-preview-duration">{{ voice.duration }}''</span>
                      <div class="delete-btn" @click="deleteChatVoice(index)">×</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 🔥 新增：本地图片预览区域（在卡片预览下面，工具栏上面） -->
              <div v-if="localImages.length > 0" class="chat-image-preview">
                <div class="image-list">
                  <div v-for="(img, index) in localImages" :key="index" class="image-item">
                    <el-image
                        :src="img.previewUrl"
                        :preview-src-list="[img.previewUrl]"
                        preview
                        preview-teleported
                        fit="cover"
                        alt="预览图片"
                        style="width: 100%; height: 100%;"
                    />
                    <div class="delete-btn" @click="deleteChatImage(index)">×</div>
                  </div>
                </div>
                <!-- 🔥 文生图模式下自动显示参考图标识 -->
                <span v-if="isImageMode" style="font-size: 12px; color: #999; margin-left: 8px;">参考图</span>
              </div>
              <!-- 🔥 新增：本地文件预览区域（和图片预览一致） -->
              <div v-if="localFiles.length > 0" class="chat-file-preview" style="margin-top: 8px;">
                <div class="file-list">
                  <div v-for="(file, index) in localFiles" :key="index" class="file-item">
                    <el-card shadow="hover" class="file-preview-card">
                      <div class="file-card-content">
                        <!-- 左侧文件图标 -->
                        <div class="file-icon">{{ file.icon }}</div>
                        <!-- 右侧文件名 + 大小 -->
                        <div class="file-info">
                          <div class="file-name" :title="file.name">{{ truncateFileName(file.name) }}</div>
                          <div class="file-size">{{ file.size }}</div>
                        </div>
                        <!-- 删除按钮 -->
                        <div class="file-delete-btn" @click="deleteChatFile(index)">×</div>
                      </div>
                    </el-card>
                  </div>
                </div>
              </div>
              <!-- 2. 工具栏（单独占一行，图片按钮左对齐） -->
              <!-- ========== 改造：AI场景+工具栏同一行 / 普通用户原有工具栏 ========== -->
              <!-- AI聊天：场景按钮+工具栏合并单行 -->
              <!-- ========== 最终版：AI 场景+工具栏 纯一行 无多余按钮 ========== -->
              <div v-if="currentSession.targetUserId === AI_DOUBAO_ID"
                   class="ai-all-in-one-bar"
                   style="margin-bottom:10px;display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
                <!-- 左侧：所有场景按钮（包含文生图） -->
                <el-button
                    v-for="scene in aiSceneList"
                    :key="scene.value"
                    size="small"
                    :type="currentAiScene === scene.value ? 'primary' : 'default'"
                    @click="switchAiScene(scene.value)"
                    style="border-radius: 15px; padding: 0 12px;"
                    round
                >
                  {{ scene.label }}
                </el-button>

                <!-- 固定图标：图片上传 + 表情包 -->
                <el-tooltip content="上传图片" placement="top">
                  <el-icon class="tool-icon" @click="triggerImageUpload"><Picture /></el-icon>
                </el-tooltip>
                <!-- 🔥 新增：文件上传按钮 -->
                <el-tooltip content="上传文件" placement="top">
                  <el-icon class="tool-icon" @click="triggerFileUpload"><DocumentAdd /></el-icon>
                </el-tooltip>
                <el-tooltip content="表情包" placement="top">
                  <img src="/smile.png" class="tool-icon emoji-btn" @click="toggleEmojiPanel" alt="表情包"/>
                </el-tooltip>
                <el-tooltip content="上传视频" placement="top">
                  <el-icon class="tool-icon" @click="triggerVideoUpload"><VideoCamera /></el-icon>
                </el-tooltip>
                <el-tooltip :content="isRecording ? '点击结束语音识别' : '语音转文字'" placement="top">
                  <el-icon class="tool-icon" :class="{ 'recording-icon': isRecording }" @click="toggleSpeechRecognition"><Service /></el-icon>
                </el-tooltip>

                <!-- 文生图模式提示（自动显示） -->
                <!--                <span v-if="isImageMode" class="image-mode-tip">🎨 生成图片模式</span>-->
              </div>

              <!-- 普通用户会话：原有工具栏（不动） -->
              <div v-else class="input-tools-bar">
                <el-tooltip content="上传图片" placement="top">
                  <el-icon class="tool-icon" @click="triggerImageUpload"><Picture /></el-icon>
                </el-tooltip>
                <!-- 🔥 新增：文件上传按钮 -->
                <el-tooltip content="上传文件" placement="top">
                  <el-icon class="tool-icon" @click="triggerFileUpload"><DocumentAdd /></el-icon>
                </el-tooltip>
                <el-tooltip content="分享好友" placement="top">
                  <el-icon class="tool-icon" @click="openFriendSelectModal"><User /></el-icon>
                </el-tooltip>
                <el-tooltip content="表情包" placement="top">
                  <img src="/smile.png" class="tool-icon emoji-btn" @click="toggleEmojiPanel" alt="表情包"/>
                </el-tooltip>
                <el-tooltip content="上传视频" placement="top">
                  <el-icon class="tool-icon" @click="triggerVideoUpload"><VideoCamera /></el-icon>
                </el-tooltip>
                <!-- 🔥 新增：语音消息按钮 -->
                <el-tooltip :content="isRecordingVoice ? '点击停止录音' : '录制语音消息'" placement="top">
                  <el-icon
                      class="tool-icon"
                      :class="{ 'recording-icon': isRecordingVoice }"
                      @click="isRecordingVoice ? stopVoiceRecording() : startVoiceRecording()"
                  >
                    <Microphone />
                  </el-icon>
                </el-tooltip>
                <el-tooltip :content="isRecording ? '点击结束语音识别' : '语音转文字'" placement="top">
                  <el-icon class="tool-icon" :class="{ 'recording-icon': isRecording }" @click="toggleSpeechRecognition"><Service /></el-icon>
                </el-tooltip>
              </div>

              <!-- 修复后：添加 multiple 支持多选 -->
              <input type="file" ref="imageInput" accept="image/*" multiple style="display: none;" @change="handleImageUpload">
              <input type="file" ref="fileInput" multiple style="display: none;" @change="handleFileUpload">
              <input type="file" ref="videoInput" accept="video/*" multiple style="display: none;" @change="handleVideoUpload">

              <!-- 新增：表情包面板 -->
              <!-- 新增：优化后的表情包面板 -->
              <div v-if="showEmojiPanel" class="emoji-panel">
                <!-- 分类标签（文字形式） -->
                <el-tabs
                    v-model="activeEmojiTab"
                    type="border-card"
                    class="emoji-tabs"
                    @tab-click="handleEmojiTabClick"
                >
                  <el-tab-pane
                      v-for="group in emojiGroups"
                      :key="group.name"
                      :label="group.name"
                      :name="group.name"
                  >
                    <!-- 横向滚动的表情包区域 -->
                    <el-scrollbar class="emoji-scrollbar" wrap-class="emoji-scrollbar-wrap">
                      <div class="emoji-list">
                        <span
                            v-for="emoji in group.items"
                            :key="emoji"
                            class="emoji-item"
                            @click="insertEmoji(emoji)"
                        >
                          {{ emoji }}
                        </span>
                      </div>
                    </el-scrollbar>
                  </el-tab-pane>
                </el-tabs>
              </div>
              <!-- 3. 输入行（输入框+发送按钮同一行） -->
              <div class="input-row">
                <ElInput
                    ref="inputRef"
                    v-model="messageContent"
                    type="textarea"
                    :autosize="{ minRows: 2, maxRows: 4 }"
                    :placeholder="aiInputPlaceholder"
                    @input="handleInputWithDebounce"
                    @keydown="handleInputKey"
                    :disabled="currentSession?.targetUserId === AI_DOUBAO_ID && (isAiThinking || isGeneratingImage)"
                />
                <ElButton
                    type="primary"
                    @click="sendMessage"
                    :disabled="( !messageContent.trim() && localImages.length === 0 && localFiles.length === 0 && localVoices.length === 0 && localVideos.length === 0 && !isImageMode ) || (currentSession?.targetUserId === AI_DOUBAO_ID && (isAiThinking || isGeneratingImage))"
                    :loading="isGeneratingImage"
                >
                  {{ isImageMode ? '生成图片' : '发送' }}
                </ElButton>
              </div>
            </div>
          </div>

          <!-- 未选择会话时的提示 -->
          <div v-else class="empty-chat">
            <el-icon size="64" style="color: #ccc;"><ChatDotRound /></el-icon>
            <p>选择一个好友开始聊天吧</p>
          </div>
        </div>

        <!-- 🔥 情况2：activeTab是search → 已有height:100%，无需修改 -->
        <div v-else-if="!isMobile" class="user-search-container">
          <div class="search-bar">
            <ElSelect v-model="queryUserType" placeholder="查询类型" style="width: 120px;">
              <ElOption label="按ID查询" value="userId" />
              <ElOption label="按姓名查询" value="name" />
            </ElSelect>
            <ElInput
                v-model="searchUserKey"
                placeholder="输入用户ID或姓名"
                style="flex: 1; margin: 0 12px;"
                suffix-icon="Search"
                @keyup.enter="searchUsers"
            />
            <ElButton type="primary" icon="Search" @click="searchUsers">搜索</ElButton>
          </div>
          <div class="search-results">
            <div v-if="searchResult.length > 0" class="result-list">
              <div
                  v-for="user in searchResult"
                  :key="user.userId"
                  class="user-result-item"
              >
                <ElAvatar size="48" style="font-size: 18px; cursor: pointer;" @click="goToUserProfile(user.userId)">
                  {{ user.name?.slice(-2) || '未知' }}
                </ElAvatar>
                <div class="user-info" style="text-align:left; cursor: pointer;" @click="router.push('/user/profile?userId=' + user.userId)">
                  <div class="user-name">{{ user.name || '未知用户' }}</div>
                  <div class="user-id">({{ user.userId }})</div>
                </div>

                <!-- 🔥 动态控制按钮的文字、样式和禁用状态 -->
                <ElButton
                    :type="getButtonType(user)"
                    size="small"
                    @click="handleAddFriend(user.userId, user.name)"
                    :disabled="isButtonDisabled(user)"
                >
                  {{ getButtonText(user) }}
                </ElButton>
              </div>
            </div>
            <div v-else class="empty-search">
              <el-icon size="64" style="color: #ccc;"><Search /></el-icon>
              <p>选择查询方式，输入内容搜索用户吧~</p>
            </div>
          </div>
        </div>
      </ElCol>
    </ElRow>
    <!-- ✅ 智能右键菜单 -->
    <div
        v-if="showContextMenu"
        class="context-menu"
        :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
        @click.stop
    >
      <!-- 好友列表右键菜单 -->
      <template v-if="contextMenuType === 'friend'">
        <div class="context-menu-item" @click="openRemarkDialog">
          <span>✏️</span>
          <span>修改备注</span>
        </div>
      </template>

      <!-- 消息右键菜单 -->
      <template v-else-if="contextMenuType === 'message'">
        <!-- 有选中文本时显示复制选中文本 -->
        <div v-if="selectedText" class="context-menu-item" @click="copyMessage">
          <span>📋</span>
          <span>复制选中文本</span>
        </div>
        <!-- 无选中文本且是文本消息时显示复制整条 -->
        <div v-else-if="currentRightClickItem.messageType === 1" class="context-menu-item" @click="copyMessage">
          <span>📋</span>
          <span>复制</span>
        </div>

        <div  class="context-menu-item" @click="forwardMessageHandler">
          <span>🔄</span>
          <span>转发</span>
        </div>

        <!-- 🔥 新增：仅图片消息显示 下载图片 -->
        <div v-if="isImageMessage(currentRightClickItem)" class="context-menu-item" @click="downloadImage">
          <span>💾</span>
          <span>下载图片</span>
        </div>

        <!-- 非AI聊天显示引用 -->
        <div v-if="currentSession.targetUserId !== AI_DOUBAO_ID" class="context-menu-item" @click="quoteMessage">
          <span>💬</span>
          <span>引用</span>
        </div>

        <!-- 自己发的消息且非AI显示撤回 -->
        <div
            v-if="String(currentRightClickItem.fromUserId) === String(currentUserId)
            && currentSession.targetUserId !== AI_DOUBAO_ID
            && getMinutesDiffFromNow(currentRightClickItem.createTime) <= 2"
            class="context-menu-item"
            @click="recallMessage"
        >
          <span>↩️</span>
          <span>撤回</span>
        </div>

        <div class="context-menu-item danger" @click="deleteMessage">
          <span>🗑️</span>
          <span>删除</span>
        </div>
      </template>
    </div>

    <!-- ✅ 修改备注弹窗 -->
    <el-dialog v-model="showRemarkDialog" title="修改备注" width="300px" center>
      <el-input v-model="newRemark" placeholder="请输入备注" maxlength="50" show-word-limit />
      <template #footer>
        <el-button @click="showRemarkDialog = false" round>取消</el-button>
        <el-button type="primary" @click="saveRemark" round>确定</el-button>
      </template>
    </el-dialog>

    <!-- ✅ 转发弹窗 -->
    <el-dialog v-model="showForwardDialog" title="选择转发对象" width="400px" center>
      <div class="forward-list">
        <div
            v-for="friend in friendList"
            :key="friend.friendId"
            class="forward-item"
            @click="confirmForward(friend.friendId)"
        >
          <ElAvatar size="40">{{ friend.userName.slice(-2) }}</ElAvatar>
          <span class="forward-name">{{ friend.friendRemark || friend.userName }}</span>
        </div>
      </div>
    </el-dialog>
    <!-- ====================== 🔥 聊天界面：分享好友弹窗（ElDialog 版本） ====================== -->
    <ElDialog
        v-model="showFriendSelectModal"
        title="选择要分享的好友"
        width="420px"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
        @close="closeFriendSelectModal"
    >
      <!-- 好友搜索 + 列表 -->
      <div class="share-friend-select">
        <el-input
            v-model="friendSearchText"
            placeholder="搜索好友（支持姓名/ID）"
            prefix-icon="Search"
            @input="handleFriendSearch"
            clearable
            autofocus
        ></el-input>

        <!-- 好友列表（复用@艾特的过滤结果） -->
        <div
            class="mention-user-list share-mention-list"
            :style="{
          position: 'static',
          maxHeight: '350px',
          overflowY: 'auto',
          margin: '15px 0 0 0',
          boxShadow: 'none',
          background: isDark ? '#2d3748' : '#fff',
          border: isDark ? '1px solid #374151' : '1px solid #eee',
          borderRadius: '8px'
        }"
        >
          <div
              v-for="(user, index) in filteredMentionUsers"
              :key="user.userId"
              class="mention-item"
              :class="{ active: index === selectedMentionIndex }"
              :style="{ color: isDark ? '#e5e7eb' : '#333' }"
              @click="selectFriendToSend(user)"
          >
            <ElAvatar size="28">{{ user.userName?.slice(-2) || '?' }}</ElAvatar>
            <span class="mention-user-name">{{ user.userName }}</span>
            <span class="mention-user-id">({{ user.friendId }})</span>
          </div>
          <div v-if="filteredMentionUsers.length === 0" class="mention-no-result">
            未找到匹配的好友
          </div>
        </div>
      </div>
    </ElDialog>
  </div>
</template>


<script setup>
import {ref, onMounted, nextTick, onUnmounted, watch, inject, computed} from 'vue'
import { useUserStore } from '../stores/userStore'
import request from '../utils/request.js'
import { ElMessage, ElAvatar, ElBadge, ElInput, ElButton, ElRow, ElCol, ElTabs, ElTabPane, ElSelect, ElOption, ElMessageBox } from 'element-plus'
import { ChatDotRound, Picture, Search, Menu, ArrowLeft, PictureRounded, Loading,DocumentAdd, User, VideoCamera, Microphone, Service } from '@element-plus/icons-vue'
import {createRouter as $router, useRoute, useRouter} from 'vue-router'
import { usePetStore } from '../stores/petStore'
import { useAchievementStore } from '../stores/achievementStore'
const petStore = usePetStore()
const achievementStore = useAchievementStore()

const route = useRoute()
const router = useRouter()
const isDark = inject('isDark')
// 🔥 新增：标记是否来自个人主页跳转
const isFromProfile = ref(false)

const userStore = useUserStore()
const currentUserId = ref(userStore.userId)
const currentUserInfo = ref(userStore.userInfo)
const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL
// 🔥 新增：获取文件与视频的专属基础路径
const FILE_BASE_URL = import.meta.env.VITE_FILE_BASE_URL || ''

// 原代码变量（不变）
const activeTab = ref('session')
const sessionList = ref([])
const friendList = inject('friendList')
const currentSession = ref(null)
const messageList = ref([])
const messageContent = ref('')
const chatMessagesRef = ref(null)

// ====================== 🔥 新增：文生图专属状态 ======================
const isImageMode = computed(() => currentAiScene.value === 'image') // ✅ 新代码：自动跟随场景
const isGeneratingImage = ref(false) // 是否正在生成图片（防重复）

// ====================== 🔥 新增：反向无限滚动变量 ======================
const page = ref(1) // 当前页码
const pageSize = ref(30) // 每页条数
const isLoadingMore = ref(false) // 是否正在加载更多（防重复）
const noMoreData = ref(false) // 是否无更多历史消息

// ====================== 🔥 新增：聊天骨架屏变量 ======================
const showChatSkeleton = ref(false) // 显示骨架屏
const skeletonCount = ref(8) // 骨架屏气泡数量

// 🔥 新增：用户搜索相关变量
const queryUserType = ref('userId') // 默认按ID查询
const searchUserKey = ref('') // 搜索关键词
const searchResult = ref([]) // 搜索结果列表

// 🔥 跳转到对应页面：豆包跳转专属界面，普通用户跳转个人主页
const goToUserProfile = (userId) => {
  if (userId === AI_DOUBAO_ID) {
    return ElMessage.info("豆包没有个人界面哦~")
  } else {
    // 普通用户跳转到个人主页
    router.push(`/user/profile?userId=${userId}`)
  }
}

const formatMentionText = inject('formatMentionText')
// 注入全局书籍卡片方法（从BasicLayout统一提供）
const bookShortLinkReg = inject('bookShortLinkReg')
const parseBookLinkToCard = inject('parseBookLinkToCard')
const scrollCards = inject('scrollCards')
// ====================== 🔥 新增：消息内容分离工具（卡片+纯文本） ======================
/**
 * 从消息内容中提取所有卡片链接
 * @param {string} content 原始消息内容
 * @returns {Array} 卡片列表 [{type, id, link}]
 */
const extractBookCards = (content) => {
  if (!content) return []
  // 匹配所有卡片格式：[{book:001}]、[{note:123}]等
  const cardReg =/\[{(\w+):([a-zA-Z0-9_-]+)}\]/g
  const cards = []
  let match

  while ((match = cardReg.exec(content)) !== null) {
    cards.push({
      type: match[1],
      id: match[2],
      link: match[0] // 原始链接，直接传给parseBookLinkToCard
    })
  }

  return cards
}

// ====================== 🔥 新增：语音消息常量与状态 ======================
const MESSAGE_TYPE_VOICE = 5 // 语音消息类型
const localVoices = ref([]) // 本地语音预览数组：{ file: File, duration: number, previewUrl: string }
const isRecordingVoice = ref(false) // 是否正在录制语音消息
let mediaRecorder = null // 录音器实例
let audioChunks = [] // 录音数据块
let recordTimer = null // 录音计时定时器
const recordDuration = ref(0) // 录音时长（秒）

// ====================== 🔥 批量内容解析核心工具函数（修正格式版） ======================
// 解析批量内容（[image]url[image]url...[text]文本）
const parseBatchContent = (content) => {
  if (!content) return {
    images: [],
    videos: [],
    documents: [],
    voices: [],
    text: '',
    fileMeta: {}
  }

  const isNewFormat = /\[(image|video|document|voice|text|fileMeta)\]/.test(content)

  // 兼容原有|||格式先转换为新格式，保证旧数据正常显示
  if (content.includes('|||') && !isNewFormat) {
    // 旧格式：文字|||资源 → 转换为 [类型]资源[text]文字
    const parts = content.split('|||')
    let type = ''
    let resource = ''
    let text = parts[0] || ''

    // 判断资源类型
    if (parts.length > 1) {
      resource = parts[1]
      if (resource.includes('/forum_images/')) {
        type = 'image'
      } else if (resource.includes('/chat_files/') && (resource.endsWith('.mp4') || resource.endsWith('.avi') || resource.endsWith('.mov'))) {
        type = 'video'
      } else if (resource.includes('/chat_files/') && (resource.endsWith('.mp3') || resource.endsWith('.wav') || resource.endsWith('.ogg'))) {
        type = 'voice' // 新增语音类型识别
      } else if (resource.includes('/chat_files/')) {
        type = 'document'
      }
      // 转换为新格式
      content = `[${type}]${resource}[text]${text}`
      // 处理文件旧格式的额外信息（文件名+大小）
      if (type === 'document' && parts.length > 2) {
        content += `[fileMeta]${parts.slice(2).join('|||')}`
      }
    }
  }

  const result = {
    images: [], // 图片url列表
    videos: [], // 视频url列表
    documents: [], // 文件url列表，格式：{url: '', meta: {fileName: '', fileSize: ''}}
    voices: [],
    text: '', // 纯文本
    fileMeta: {} // 兼容旧文件格式的元信息
  }

  // 提取文件元信息（兼容旧格式）
  const fileMetaMatch = content.match(/\[fileMeta\](.*?)(?=\[text\]|$)/)
  if (fileMetaMatch) {
    const metaParts = fileMetaMatch[1].split('|||')
    result.fileMeta = {
      fileName: metaParts[0] || '',
      fileSize: metaParts[1] || ''
    }
    content = content.replace(/\[fileMeta\].*?(?=\[text\]|$)/, '')
  }

  // 提取图片
  const imageMatches = content.match(/\[image\](.*?)(?=\[image\]|\[video\]|\[document\]|\[text\]|$)/g)
  if (imageMatches) {
    result.images = imageMatches.map(item => item.replace('[image]', ''))
  }

  // 提取视频
  const videoMatches = content.match(/\[video\](.*?)(?=\[image\]|\[video\]|\[document\]|\[text\]|$)/g)
  if (videoMatches) {
    result.videos = videoMatches.map(item => item.replace('[video]', ''))
  }

  // 提取文件
  const docMatches = content.match(/\[document\](.*?)(?=\[image\]|\[video\]|\[document\]|\[text\]|$)/g)
  if (docMatches) {
    result.documents = docMatches.map((item, index) => {
      const url = item.replace('[document]', '')
      // 兼容旧文件元信息
      if (result.fileMeta.fileName && index === 0) {
        return {
          url,
          meta: result.fileMeta
        }
      }
      // 解析新文件元信息（url|||文件名|||大小）
      const docParts = url.split('|||')
      if (docParts.length > 1) {
        return {
          url: docParts[0],
          meta: {
            fileName: docParts[1] || '',
            fileSize: docParts[2] || ''
          }
        }
      }
      return { url, meta: { fileName: truncateFileName(url.split('/').pop() || ''), fileSize: '' } }
    })
  }

  // 🔥 新增：提取语音
  const voiceMatches = content.match(/\[voice\](.*?)(?=\[image\]|\[video\]|\[document\]|\[voice\]|\[text\]|$)/g)
  if (voiceMatches) {
    result.voices = voiceMatches.map(item => {
      const voiceStr = item.replace('[voice]', '')
      // 格式：[voice]url|||时长（秒）
      const voiceParts = voiceStr.split('|||')
      return {
        url: voiceParts[0],
        duration: parseInt(voiceParts[1] || '0')
      }
    })
  }

  // 提取文本（兼容纯文本[text]标签）
  const textMatch = content.match(/\[text\](.*)$/)
  result.text = textMatch ? textMatch[1] : content // 如果没有[text]标签，直接用原内容（兜底）

  return result
}

// 🔥 新增：提取语音列表
const extractVoices = (content) => {
  return parseBatchContent(content).voices
}

// 在接收到消息时预处理：
const processMessage = (msg) => {
  msg.parsed = parseBatchContent(msg.messageContent);
  msg.pureText = extractPureText(msg.messageContent);
  msg.cards = extractBookCards(msg.messageContent);
  return msg;
}

// 提取纯文字（适配新格式+完全兼容原有卡片过滤逻辑）
const extractPureText = (content) => {
  if (!content) return ''
  const batchContent = parseBatchContent(content)
  // 去除转发前缀后提取文本
  let pureText = batchContent.text || ''
  // ✅ 完全复用你原有卡片过滤逻辑（[{book:123}]格式）
  pureText = pureText.replace(/\[\{(\w+):(\d+)\}\]/g, '')
  // 清理多余的空行和首尾空格
  pureText = pureText.replace(/\n+/g, '\n').trim()
  return pureText
}

// 提取图片列表（批量）
const extractImages = (content) => {
  return parseBatchContent(content).images
}

// 提取视频列表（批量）
const extractVideos = (content) => {
  return parseBatchContent(content).videos
}

// 提取文件列表（批量）
const extractDocuments = (content) => {
  return parseBatchContent(content).documents
}

// ====================== 🔥 新增：语音播放状态管理 ======================
const currentPlayingVoiceId = ref(null) // 当前播放的语音消息ID
const isVoicePlaying = ref(false) // 是否正在播放
let audioPlayer = null // 全局音频播放器实例

// 播放/暂停语音
const toggleVoicePlay = (messageId, voiceUrl) => {
  // 如果点击的是当前正在播放的语音 → 暂停
  if (currentPlayingVoiceId.value === messageId && isVoicePlaying.value) {
    audioPlayer.pause()
    isVoicePlaying.value = false
    return
  }

  // 如果有其他语音正在播放 → 先停止
  if (audioPlayer && !audioPlayer.paused) {
    audioPlayer.pause()
    audioPlayer.currentTime = 0
  }

  // 创建新的音频播放器
  audioPlayer = new Audio(FILE_BASE_URL + voiceUrl)
  currentPlayingVoiceId.value = messageId
  isVoicePlaying.value = true

  // 播放结束回调
  audioPlayer.onended = () => {
    isVoicePlaying.value = false
    currentPlayingVoiceId.value = null
  }

  // 播放错误回调
  audioPlayer.onerror = () => {
    ElMessage.error('语音播放失败')
    isVoicePlaying.value = false
    currentPlayingVoiceId.value = null
  }

  // 开始播放
  audioPlayer.play().catch(() => {
    ElMessage.error('语音播放失败，请检查网络')
    isVoicePlaying.value = false
    currentPlayingVoiceId.value = null
  })
}

// ====================== 🔥 新增：语音录制核心方法 ======================
// 开始录制语音
const startVoiceRecording = async () => {
  try {
    // 请求麦克风权限
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []
    recordDuration.value = 0

    // 收集录音数据
    mediaRecorder.ondataavailable = (e) => {
      audioChunks.push(e.data)
    }

    // 录制完成回调
    mediaRecorder.onstop = () => {
      // 停止麦克风流
      stream.getTracks().forEach(track => track.stop())

      // 生成音频Blob和File对象
      const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' })
      const audioFile = new File([audioBlob], `voice_${Date.now()}.mp3`, { type: 'audio/mp3' })

      // 生成本地预览URL
      const previewUrl = URL.createObjectURL(audioBlob)

      // 添加到本地预览列表
      localVoices.value.push({
        file: audioFile,
        duration: recordDuration.value,
        previewUrl: previewUrl
      })

      // 清除定时器
      clearInterval(recordTimer)
    }

    // 开始录制
    mediaRecorder.start()
    isRecordingVoice.value = true
    ElMessage.info('开始录音，再次点击停止')

    // 启动计时
    recordTimer = setInterval(() => {
      recordDuration.value++
      // 最长录制60秒
      if (recordDuration.value >= 60) {
        stopVoiceRecording()
        ElMessage.info('录音已达最长时长60秒')
      }
    }, 1000)

  } catch (err) {
    console.error('麦克风权限获取失败:', err)
    ElMessage.error('请允许麦克风权限以录制语音')
  }
}

// 停止录制语音
const stopVoiceRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
    isRecordingVoice.value = false
  }
}

// 删除本地预览语音
const deleteChatVoice = (index) => {
  if (localVoices.value[index]) {
    URL.revokeObjectURL(localVoices.value[index].previewUrl)
    localVoices.value.splice(index, 1)
  }
}

const updateAllBookCards = inject('updateAllBookCards')

// ====================== 🔥 新增：输入框防抖优化 ======================
let inputDebounceTimer = null
const handleInputWithDebounce = (value) => {
  if (inputDebounceTimer) clearTimeout(inputDebounceTimer)
  // 用户停止打字 300 毫秒后，再执行卡片正则解析
  inputDebounceTimer = setTimeout(() => {
    updatePreviewBookCards(value)
  }, 300)
}

const updatePreviewBookCards = inject('updatePreviewBookCards')
const previewBookCards = inject('previewBookCards')
// ====================== 原工具函数（不变） ======================
const formatTime = inject('formatDateTime')

const getUserInfo = inject('getUserInfo')

// ====================== 🔥 性能优化版：反向无限滚动 - 监听滚动 ======================
const scrollDebounce = ref(null)

const handleChatScroll = () => {
  const el = chatMessagesRef.value
  if (!el || isLoadingMore.value  || showChatSkeleton.value) return

  // 仅保留加载更多的防抖逻辑，删除耗性能的 DOM 实时计算
  if (scrollDebounce.value) clearTimeout(scrollDebounce.value)
  scrollDebounce.value = setTimeout(() => {
    if (el.scrollTop <= 10) {
      loadMoreHistoryMessages()
    }
  }, 300)
}

const loadMoreHistoryMessages = async () => {
  if (noMoreData.value || isLoadingMore.value) return
  isLoadingMore.value = true
  page.value++

  const el = chatMessagesRef.value
  if (!el) {
    isLoadingMore.value = false
    return
  }

  // ✅ 关键1：加载前记录滚动条位置和容器高度
  const oldScrollTop = el.scrollTop
  const oldScrollHeight = el.scrollHeight

  try {
    if (currentSession.value.targetUserId === AI_DOUBAO_ID) {
      await getAiChatHistory(true)
    } else {
      await getChatHistory(true)
    }

    await nextTick(() => {
      const newScrollHeight = el.scrollHeight
      // ✅ 关键2：无痕滚动位置修正
      el.scrollTop = oldScrollTop + (newScrollHeight - oldScrollHeight)

      // ✅ 关键3：加载完成后，手动重置所有消息的动画状态
      const messageItems = el.querySelectorAll('.message-item')
      messageItems.forEach((item) => {
        item.style.opacity = 1
        item.style.transform = 'translateX(0)'
        item.style.pointerEvents = 'auto'
      })

      // ✅ 关键4：手动触发一次handleChatScroll，更新所有消息的滑出状态
      handleChatScroll()
    })
  } finally {
    isLoadingMore.value = false
  }
}

// ====================== 🔥 重置聊天状态（切换会话时调用） ======================
// 判断当前消息是不是自己发的最后一条消息
const isLastSelfMessage = (currentIndex) => {
  // 从当前索引往后找，有没有自己发的消息
  for (let i = currentIndex + 1; i < messageList.value.length; i++) {
    if (String(messageList.value[i].fromUserId) === String(currentUserId.value)) {
      // 后面还有自己发的消息，不是最后一条
      return false
    }
  }
  // 后面没有自己发的消息了，是最后一条
  return true
}

const resetChatState = () => {
  page.value = 1
  noMoreData.value = false
  isLoadingMore.value = false
  messageList.value = []
  showChatSkeleton.value = true
  previewBookCards.value = []
  quotedMessage.value = null
  localImages.value = []
  localFiles.value = []
  // ✅ 切换会话时重置所有消息的动画状态
  nextTick(() => {
    const el = chatMessagesRef.value
    if (el) {
      const messageItems = el.querySelectorAll('.message-item')
      messageItems.forEach((item) => {
        item.style.opacity = 1
        item.style.transform = 'translateX(0)'
        item.style.pointerEvents = 'auto'
      })
    }
  })
}

const selectedMentionIndex = inject('selectedMentionIndex')
const showFriendSelectModal = ref(false)
const friendSearchText = ref('')
const filteredMentionUsers = inject('filteredMentionUsers')
// 打开好友选择弹窗
const openFriendSelectModal = () => {
  if (!currentSession.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  showFriendSelectModal.value = true
  friendSearchText.value = ''
  // 初始显示所有好友
  filteredMentionUsers.value = friendList.value
}

// 关闭好友选择弹窗
const closeFriendSelectModal = () => {
  showFriendSelectModal.value = false
  friendSearchText.value = ''
}

const handleMentionInput = inject('handleMentionInput')
const  updateMentionSearch = inject('updateMentionSearch')
// ====================== 🔥 修复：独立实现好友搜索（不依赖$refs和@艾特方法） ======================
const handleFriendSearch = (val) => {
  if (!val.trim()) {
    // 搜索为空时显示所有好友
    filteredMentionUsers.value = friendList.value
    return
  }

  // 按姓名或ID模糊搜索（不区分大小写）
  const keyword = val.trim().toLowerCase()
  filteredMentionUsers.value = friendList.value.filter(user => {
    return (
        // 匹配用户名
        user.userName?.toLowerCase().includes(keyword)
        // 匹配用户ID
        || String(user.friendId).includes(keyword)
    )
  })
}
// 选择好友并发送
const selectFriendToSend = (user) => {
  // 构造标准好友卡片格式
  messageContent.value = `[{user:${user.friendId}}]`
  // 复用你已有的发送消息方法（自动处理AI/普通好友、后端请求、消息列表更新）
  sendMessage()
  // 关闭弹窗
  closeFriendSelectModal()
}
// ====================== 🔥 新增：通过用户ID直接打开聊天会话 ======================
const openChatByUserId = async (targetUserId) => {
  if (!targetUserId || !currentUserId.value) return

  // 1. 先查会话列表里是否已有该用户
  const existingSession = sessionList.value.find(s =>
      String(s.targetUserId) === String(targetUserId)
  )

  if (existingSession) {
    // 已有会话 → 直接选中
    await selectSession(existingSession)
    return
  }

  // 2. 无会话 → 获取用户信息，创建**临时会话**（不入库，仅前端展示）
  const userInfo = await getUserInfo(targetUserId)
  currentSession.value = {
    targetUserId: targetUserId,
    userName: userInfo.userName,
    lastMessage: '',
    lastMessageTime: null,
    unreadCount: 0
  }

  // 3. 加载聊天记录（无记录则为空）
  messageList.value = []
}

const getMessageUserName = (userId) => {
  // 🔥 新增：豆包AI名称
  if (userId === AI_DOUBAO_ID) {
    return '豆包小助手'
  }
  if (userId === currentUserId.value) {
    return currentUserInfo.value.name
  }
  return currentSession.value?.userName || '未知用户'
}

const previewImage = (imageUrl) => {
  window.open(imageUrl)
}

// 🔥 终极修复版：区分【初始加载】和【发新消息】，彻底解决滚不到底的问题
const scrollToBottom = async (isInitial = false) => {
  await nextTick()
  setTimeout(() => {
    const el = chatMessagesRef.value
    if (el) {
      // 1. 初始加载用瞬间跳转(auto)，发新消息用平滑滚动(smooth)
      el.scrollTo({
        top: el.scrollHeight,
        behavior: isInitial ? 'auto' : 'smooth'
      })

      // 2. 🔥 终极补刀：针对初次加载时由于图片或DOM撑开导致没到底的情况，等待150ms后再次强制锁死底部
      if (isInitial) {
        setTimeout(() => {
          if (el) el.scrollTop = el.scrollHeight
        }, 150)
      }

      // 3. 重置新消息状态
      messageList.value.forEach((msg) => {
        if (msg.isNew) msg.isNew = false
      })
    }
  }, 10)
}

// ====================== 🔥 新增：AI聊天状态控制变量 ======================
// ====================== 🔥 图生图/文生图通用生成中状态 ======================
const generatingImageText = ref('🎨 正在生成图片中，请稍候...')
// ====================== 🔥 新增：文生图核心方法 ======================
// 切换文生图模式
const toggleImageMode = () => {
  isImageMode.value = !isImageMode.value
  if (isImageMode.value) {
    ElMessage.info('已切换到文生图模式，输入提示词后点击生成图片即可~')
  } else {
    ElMessage.info('已切换回正常聊天模式')
  }
}

// 文生图专属发送逻辑
// ====================== 🔥 最终版generateImage：统一格式+纯文本加[text]标签 ======================
const generateImage = async () => {
  // 校验：提示词和参考图不能同时为空
  if (!messageContent.value.trim() && localImages.value.length === 0) {
    ElMessage.warning('请输入提示词或上传参考图')
    return
  }
  isGeneratingImage.value = true
  const userMsg = messageContent.value.trim()
  messageContent.value = ''

  // 2. 🔥 先上传参考图（如果有，支持多张参考图）
  let imagePaths = []
  if (localImages.value.length > 0) {
    generatingImageText.value = '🎨 正在上传参考图...'
    const total = localImages.value.length

    imagePaths = await Promise.all(
        localImages.value.map(async (item, index) => {
          const formData = new FormData()
          formData.append('action', 'upload')
          formData.append('commentId', '0')
          formData.append('image', item.file)

          const uploadRes = await request.post('/user/comment/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })

          if (uploadRes.code !== 200) {
            throw new Error(`第${index+1}张参考图上传失败`)
          }

          return uploadRes.data.imageUrl
        })
    )

    localImages.value = [] // 生成完成自动清空参考图
    generatingImageText.value = '🎨 正在生成图片中，请稍候...'
  }

  // 1. 🔥 构造用户消息（统一格式）
  let userMessageContent = ''
  let userMessageType = 1 // 默认纯文字（文生图）

  if (imagePaths.length > 0) {
    // 有参考图：图生图模式
    userMessageType = 2 // ✅ 图文混合=messageType=2
    // 拼接参考图
    imagePaths.forEach(url => {
      userMessageContent += `[image]${url}`
    })
    // 拼接提示词（统一加[text]标签）
    if (userMsg) {
      userMessageContent += `[text]🎨 图生图：${userMsg}`
    } else {
      userMessageContent += `[text]🎨 图生图`
    }
  } else {
    // 没有参考图：纯文生图模式（统一加[text]标签）
    userMessageContent = `[text]🎨 文生图提示词：${userMsg}`
  }

  const userMessage = {
    id: Date.now(),
    messageType: userMessageType,
    formType: 0,
    messageContent: userMessageContent,
    createTime: new Date().toISOString(),
    isNew: true
  }
  // ✅ 替换为：
  messageList.value.push(processMessage(userMessage))
  scrollToBottom()

  try {
    // 3. ✅ 修正1：构造文生图请求参数（将 key 改为 imagePath，并拼接成后端认识的 [image]url 格式）
    const params = {
      action: 'generateImage',
      fromUserId: currentUserId.value,
      toUserId: currentSession.value.targetUserId,
      prompt: userMsg,
      imagePath: imagePaths.map(url => `[image]${url}`).join(''), // 适配后端 parseBatchReferenceImages
      strength: 0.5
    }

    // 4. 调用后端文生图接口
    const res = await request.post('/user/chat/message', null, { params })
    if (res.code === 200) {
      ElMessage.success('✅ 图片生成成功！')

      // 5. ✅ 修正2：直接解构后端返回的完整、合法的 AI 消息对象（自带正确格式的 messageContent 和 formType）
      const aiImageMessage = {
        ...res.data, // 包含 id, formType: 1, messageType: 2, messageContent 等全部正确字段
        isNew: true
      }

      // ✅ 替换为：
      messageList.value.push(processMessage(aiImageMessage))
      scrollToBottom()

      // 6. 更新会话最后一条消息
      currentSession.value.lastMessage = '[图片]'
      currentSession.value.lastMessageTime = new Date().toISOString()
      // 置顶豆包会话
      let idx = sessionList.value.findIndex(s => s.targetUserId === AI_DOUBAO_ID)
      if(idx > -1){
        sessionList.value.splice(idx,1)
      }
      sessionList.value.unshift({...currentSession.value})
    } else {
      ElMessage.error('❌ 图片生成失败：' + (res.msg || '未知错误'))
    }
  } catch (err) {
    console.error('文生图失败', err)
    ElMessage.error('❌ 图片生成失败，请重试')
  } finally {
    isGeneratingImage.value = false
    generatingImageText.value = '🎨 正在生成图片中，请稍候...'
  }
}
const isAiThinking = ref(false)       // 是否正在思考（显示占位气泡）
const typingInterval = ref(null)      // 打字机定时器
const isSending = ref(false)          // 防止重复发送消息
// AI思考内容（实时更新）
const aiThinkingText = ref('🤔 正在思考中...')

// 👇👇👇【新增：AI场景控制】👇👇👇
const currentAiScene = ref('') // 默认：图书服务
const aiSceneList = ref([
  { label: '📚 图书服务', value: 'book' },
  { label: '💬 读书树洞', value: 'treehole' },
  { label: '🎮 书籍游戏', value: 'game' },
  { label: '✍️ 话术润色', value: 'write' },
  { label: '🎨 文生图模式', value: 'image' },
  { label: '📄 识别文件', value: 'file' }
])

// ====================== 🔥 计算属性：动态输入框占位提示 ======================
const aiInputPlaceholder = computed(() => {
  switch (currentAiScene.value) {
    case 'book':
      return '问我图书相关问题（查书/借书/笔记/好友功能）...'
    case 'treehole':
      return '说说你的心情/读书感悟，我来倾听和陪伴~'
    case 'game':
      return '开始猜书名/猜人物/答题小游戏吧！'
    case 'write':
      return '输入需要润色的文案，支持幽默/温柔/正式风格~'
    case 'image':
      return '输入文生图提示词，如：可爱的图书馆小猫、古风书籍插画...'
    case 'file':
      return '选择文件后，AI将自动识别/总结文件内容...(图片识别可以选择上传图片方式↗️)'
    default:
      return '输入消息...'
  }
})

// ====================== 🔥 统一场景切换方法（新增气泡提示） ======================
const switchAiScene = (sceneValue) => {
  // ✅ 核心判断：如果点击的是【当前已选中】的按钮 → 取消选中
  if (currentAiScene.value === sceneValue) {
    currentAiScene.value = ''        // 清空场景（取消选中）
    isImageMode.value = false        // 强制关闭文生图模式
    petStore.addMessage('✅ 已取消AI场景，恢复默认模式', petStore.MESSAGE_TYPES.CHAT)
    ElMessage.info('已取消场景选择')
    return
  }

  currentAiScene.value = sceneValue
  isImageMode.value = sceneValue === 'image'

  // ====================== ✅ 核心：切换场景 → 小宠物弹出对应提示气泡 ======================
  let sceneTip = ''
  switch (sceneValue) {
    case 'treehole':
      sceneTip = '【当前模式：读书树洞】可以谈心、共情、聊读书感悟'
      break
    case 'game':
      sceneTip = '【当前模式：书籍小游戏】立刻开启猜书名/猜人物/答题游戏'
      break
    case 'write':
      sceneTip = '【当前模式：话术润色】可以优化文案，支持幽默/温柔/正式风格'
      break
    case 'book':
      sceneTip = '【当前模式：图书服务】可以使用借书/查书/笔记/好友功能'
      break
    case 'image':
      sceneTip = '【当前模式：文生图】可以输入提示词生成相关图片'
      break
      // 🔥 新增：文件识别场景
    case 'file':
      sceneTip = '【当前模式：文件识别】选择文件上传，AI自动总结/分析内容'
      break
    default:
      sceneTip = '【当前模式：默认服务】'
  }

  petStore.addMessage(sceneTip, petStore.MESSAGE_TYPES.CHAT)
  ElMessage.info(`已切换 ${aiSceneList.value.find(s => s.value === sceneValue).label}`)
}

// ====================== 🔥 新增：AI回复打字机效果 ======================
const startTypingEffect = async (fullText) => {
  console.log('启动打字机，内容：', fullText)
  if (!fullText || typeof fullText !== 'string') {
    console.error('打字机内容无效：', fullText)
    return
  }
  const aiTempMessage = processMessage({
    id: Date.now() + 1,
    messageType: 1,
    messageContent: '',
    createTime: new Date().toISOString(),
    isNew: true
  })
  messageList.value.push(aiTempMessage)
  const lastIndex = messageList.value.length - 1
  const aiMessage = messageList.value[lastIndex]

  let index = 0
  // ✅ 配置：间隔10ms，一次打5个字（超快+流畅）
  const typingSpeed = 5 // 打字间隔
  const charCount = 5    // 每次打几个字
  typingInterval.value = setInterval(() => {
    if (index < fullText.length) {
      // 批量添加字符
      aiMessage.messageContent += fullText.slice(index, index + charCount)
      // 👇 新增这一行：实时更新预处理文本，触发 v-memo 重新渲染气泡！
      aiMessage.pureText = extractPureText(aiMessage.messageContent)
      index += charCount
      scrollToBottom()
    } else {
      clearInterval(typingInterval.value)
      typingInterval.value = null
      currentSession.value.lastMessage = fullText
      currentSession.value.lastMessageTime = new Date().toISOString()
      let idx = sessionList.value.findIndex(s => s.targetUserId === AI_DOUBAO_ID)
      if(idx > -1){
        sessionList.value.splice(idx,1)
      }
      sessionList.value.unshift({...currentSession.value})
    }
  }, typingSpeed)
};

// ========== 新增：图片画廊状态管理 ==========
const imageGalleryState = ref({})

// 初始化某条消息的画廊状态
const initGalleryState = (msgId) => {
  if (!imageGalleryState.value[msgId]) {
    imageGalleryState.value[msgId] = { expanded: false, currentIndex: 0 }
  }
  return imageGalleryState.value[msgId]
}

// 展开/收起画廊
const toggleGalleryExpand = (msgId) => {
  const state = initGalleryState(msgId)
  state.expanded = !state.expanded
}

// 下一张图片
const nextGalleryImg = (msgId, total) => {
  const state = initGalleryState(msgId)
  if (state.currentIndex < total - 1) state.currentIndex++
}

// 上一张图片
const prevGalleryImg = (msgId) => {
  const state = initGalleryState(msgId)
  if (state.currentIndex > 0) state.currentIndex--
}
// ====================== 🔥 新增：豆包AI配置 ======================
// 豆包虚拟用户ID（固定，不与真实用户冲突）
const AI_DOUBAO_ID = 'doubao_ai'
// 豆包名称、头像
const AI_INFO = {
  userName: '豆包小助手🐱', // 名字加🐱
  targetUserId: AI_DOUBAO_ID,
  lastMessage: '我是你的智能小助手~',
  lastMessageTime: new Date().toISOString(),
  unreadCount: 0
}

// ====================== 🔥 新增：添加豆包到会话列表 ======================
const addDoubaoToSessionList = () => {
  const hasAi = sessionList.value.some(item => item.targetUserId === AI_DOUBAO_ID)
  if (hasAi) {
    // 如果已经存在，把它移到最前面
    const index = sessionList.value.findIndex(item => item.targetUserId === AI_DOUBAO_ID)
    const aiSession = sessionList.value.splice(index, 1)[0]
    sessionList.value.unshift(aiSession)
  } else {
    sessionList.value.unshift(AI_INFO)
  }
}

// ====================== 🔥 最终版sendMessageToAI：支持文字/图片/文件/视频/语音 ======================
const sendMessageToAI = async () => {
  if (isSending.value) return // 防止重复发送

  // 👉 1. 校验：文字、图片、文件、视频、语音 不能同时为空
  if (!messageContent.value.trim()
      && localImages.value.length === 0
      && localFiles.value.length === 0
      && localVideos.value.length === 0 // 新增：视频校验
      && localVoices.value.length === 0) { // 新增：语音校验
    ElMessage.warning('消息内容不能为空')
    return
  }

  // 👉 2. 校验：图片/文件/视频/语音 只能选一种发送（文字可混合）
  const mediaTypes = [
    localImages.value.length > 0,
    localFiles.value.length > 0,
    localVideos.value.length > 0,
    localVoices.value.length > 0
  ].filter(Boolean).length
  if (mediaTypes > 1) {
    ElMessage.warning('图片、文件、视频、语音只能选择一种类型发送')
    return
  }

  const userMsg = messageContent.value.trim()
  isSending.value = true
  console.log('发送AI消息：', userMsg)
  previewBookCards.value = []
  let finalMessageContent = userMsg
  let finalMessageType = 1 // 默认纯文字

  // ====================== 🔥 1. 批量上传文件 ======================
  if (localFiles.value.length > 0) {
    const total = localFiles.value.length
    ElMessage.info(`正在上传第1/${total}个文件...`, 0)

    const documents = await Promise.all(
        localFiles.value.map(async (item, index) => {
          const formData = new FormData()
          formData.append('action', 'upload')
          formData.append('userId', currentUserId.value)
          formData.append('file', item.file)

          const uploadRes = await request.post('/chat/file', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })

          if (uploadRes.code !== 200) {
            throw new Error(`第${index+1}个文件上传失败`)
          }

          if (index < total - 1) {
            ElMessage.info(`正在上传第${index+2}/${total}个文件...`, 0)
          }

          return {
            url: uploadRes.data.documentUrl,
            name: item.name,
            size: item.size
          }
        })
    )

    // 拼接统一格式
    let content = ''
    documents.forEach(doc => {
      content += `[document]${doc.url}|||${doc.name}|||${doc.size}`
    })
    // ✅ 无论有没有文字，都加上[text]标签
    content += `[text]${finalMessageContent}`

    finalMessageContent = content
    finalMessageType = 3 // 文件类型=3
    localFiles.value = []
  }

  // ====================== 🔥 2. 批量上传图片 ======================
  else if (localImages.value.length > 0) {
    const total = localImages.value.length
    ElMessage.info(`正在上传第1/${total}张图片...`, 0)

    const imageUrls = await Promise.all(
        localImages.value.map(async (item, index) => {
          const formData = new FormData()
          formData.append('action', 'upload')
          formData.append('commentId', '0')
          formData.append('image', item.file)

          const uploadRes = await request.post('/user/comment/image', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })

          if (uploadRes.code !== 200) {
            throw new Error(`第${index+1}张图片上传失败`)
          }

          if (index < total - 1) {
            ElMessage.info(`正在上传第${index+2}/${total}张图片...`, 0)
          }

          return uploadRes.data.imageUrl
        })
    )

    // 拼接统一格式
    let content = ''
    imageUrls.forEach(url => {
      content += `[image]${url}`
    })
    // ✅ 无论有没有文字，都加上[text]标签
    content += `[text]${finalMessageContent}`

    finalMessageContent = content
    finalMessageType = 2 // 图片类型=2
    localImages.value = []
  }

  // ====================== 🔥 3. 新增：批量上传视频 ======================
  else if (localVideos.value.length > 0) {
    const total = localVideos.value.length
    ElMessage.info(`正在上传第1/${total}个视频...`, 0)

    const videoUrls = await Promise.all(
        localVideos.value.map(async (item, index) => {
          const formData = new FormData()
          formData.append('action', 'upload')
          formData.append('userId', currentUserId.value)
          formData.append('file', item.file) // 视频文件字段

          // 请根据实际后端接口调整：视频上传接口地址/参数
          const uploadRes = await request.post('/chat/file', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 60000 // 视频上传超时时间加长
          })

          if (uploadRes.code !== 200) {
            throw new Error(`第${index+1}个视频上传失败`)
          }

          if (index < total - 1) {
            ElMessage.info(`正在上传第${index+2}/${total}个视频...`, 0)
          }

          return uploadRes.data.documentUrl
        })
    )

    // 拼接统一格式
    let content = ''
    videoUrls.forEach(url => {
      content += `[video]${url}`
    })
    // ✅ 无论有没有文字，都加上[text]标签
    content += `[text]${finalMessageContent}`

    finalMessageContent = content
    finalMessageType = 4 // 视频类型=4
    localVideos.value = []
  }

  // ====================== 🔥 4. 新增：批量上传语音 ======================
  else if (localVoices.value.length > 0) {
    const total = localVoices.value.length
    ElMessage.info(`正在上传第1/${total}条语音...`, 0)

    const voiceList = await Promise.all(
        localVoices.value.map(async (item, index) => {
          const formData = new FormData()
          formData.append('action', 'upload')
          formData.append('userId', currentUserId.value)
          formData.append('file', item.file) // 语音文件字段

          // 请根据实际后端接口调整：语音上传接口地址/参数
          const uploadRes = await request.post('/chat/file', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })

          if (uploadRes.code !== 200) {
            throw new Error(`第${index+1}条语音上传失败`)
          }

          if (index < total - 1) {
            ElMessage.info(`正在上传第${index+2}/${total}条语音...`, 0)
          }

          // 返回语音URL + 时长（时长建议由后端返回，前端也可自行计算）
          return {
            url: uploadRes.data.documentUrl,
            duration: item.duration || 0 // 语音时长（秒）
          }
        })
    )

    // 拼接统一格式
    let content = ''
    voiceList.forEach(voice => {
      content += `[voice]${voice.url}|||${voice.duration}` // 格式：[voice]URL|||时长
    })
    // ✅ 无论有没有文字，都加上[text]标签
    content += `[text]${finalMessageContent}`

    finalMessageContent = content
    finalMessageType = 5 // 语音类型=5
    localVoices.value = []
  }

  // ✅ 🔥 纯文本消息：统一加上[text]标签
  else {
    finalMessageContent = `[text]${finalMessageContent}`
  }

  // 3. 前端渲染用户消息
  const userMessage = {
    id: Date.now(),
    messageType: finalMessageType,
    formType: 0, // 固定：用户发送=0
    messageContent: finalMessageContent,
    createTime: new Date().toISOString(),
    isNew: true
  }
  // ✅ 替换为：
  messageList.value.push(processMessage(userMessage))
  messageContent.value = ''
  scrollToBottom()

  // 4. 显示“正在思考”状态
  isAiThinking.value = true
  try {
    // 5. 请求AI接口，传递完整消息（包含视频/语音类型）
    const res = await request.post('/pet/ai/chat/history', {
      userId: currentUserId.value,
      content: finalMessageContent,
      scene: currentAiScene.value,
      messageType: finalMessageType // 传递视频/语音类型
    })
    console.log('AI接口返回：', res)
    // 6. 收到回复，隐藏思考状态
    isAiThinking.value = false
    // 7. 启动打字机效果
    await startTypingEffect(res.data)
  } catch (err) {
    ElMessage.error('AI回复失败，请重试')
    console.error('AI请求错误：', err)
    isAiThinking.value = false
  } finally {
    isSending.value = false
  }
}
// ====================== 🔥 重写：获取AI聊天历史（适配AI数据结构） ======================
const getAiChatHistory = async (isLoadMore = false) => {
  try {
    const res = await request.get('/pet/ai/chat/history', {
      params: {
        userId: currentUserId.value,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    console.log('后端返回的原始数据：', res.data) // ✅ 加这行
    console.log('返回的消息ID列表：', res.data.map(item => item.id)) // ✅ 加这行
    if (res.code === 200) {
      let data = res.data || []
      data = data.reverse()
      // ✅ 标记历史消息为非新消息
      // ✅ 替换为：
      data = data.map(m => processMessage({ ...m, isNew: false }))
      if (data.length < pageSize.value) {
        noMoreData.value = true
      }
      // 反向加载：追加到头部
      if (isLoadMore) {
        const existingIds = new Set(messageList.value.map(m => m.id))
        const newData = data.filter(m => !existingIds.has(m.id))
        messageList.value.unshift(...newData)
      } else {
        messageList.value = data
      }
    }
  } catch (err) {
    console.error('加载AI历史失败', err)
  } finally {
    showChatSkeleton.value = false // 加载完成隐藏骨架屏
    // ✅ 只有初始加载（不是加载更多）时，才滚动到底部
    if (!isLoadMore) {
      await scrollToBottom(true)
    }
  }
}
// AI记忆条数：保留最近2轮对话（控制Token）
const AI_MEMORY_LIMIT = 10
// ====================== 原接口调用（不变） ======================
const getSessionList = async () => {
  try {
    const res = await request.get('/user/chat/session', {
      params: { action: 'list', userId: currentUserId.value }
    })
    if (res.code === 200) {
      const sessions = res.data || []
      for (let session of sessions) {
        const user = await getUserInfo(session.targetUserId)
        session.userName = user.userName
      }
      // 🔥 核心修复：先赋值后端返回的会话列表
      sessionList.value = sessions

      // 🔥 再重新添加豆包AI会话（确保永远存在）
      addDoubaoToSessionList()
    }
  } catch (err) {
    console.error('获取会话列表失败', err)
    ElMessage.error('获取会话列表失败')
  }
}

const getFriendList = inject('getFriendList')

const selectSession = async (session) => {
  // 清除之前的打字机定时器
  if (typingInterval.value) {
    clearInterval(typingInterval.value)
    typingInterval.value = null
  }
  isAiThinking.value = false

  // ✅ 核心：手机端点击后切换到聊天视图
  if (isMobile.value) {
    showChatView.value = true
  }

  // ✅ 核心：重置状态+显示骨架屏
  resetChatState()
  currentSession.value = session

  // 加载对应会话消息
  if (session.targetUserId === AI_DOUBAO_ID) {
    await getAiChatHistory()
    scrollToBottom()
    return
  }
  await getChatHistory()
  await markAsRead()
  session.unreadCount = 0
  scrollToBottom()
}

const selectFriend = async (friend) => {
  const existingSession = sessionList.value.find(s => s.targetUserId === friend.friendId)
  if (existingSession) {
    await selectSession(existingSession)
    return
  }

  // ✅ 核心：手机端点击后切换到聊天视图
  if (isMobile.value) {
    showChatView.value = true
  }

  // ✅ 重置状态+显示骨架屏
  resetChatState()
  currentSession.value = {
    targetUserId: friend.friendId,
    userName: friend.userName,
    lastMessage: '',
    lastMessageTime: null,
    unreadCount: 0
  }
  await getChatHistory()
}

const getChatHistory = async (isLoadMore = false) => {
  try {
    const res = await request.get('/user/chat/message', {
      params: {
        action: 'history',
        userId: currentUserId.value,
        targetUserId: currentSession.value.targetUserId,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    if (res.code === 200) {
      let data = res.data || []
      // ✅ 关键：后端返回倒序，前端反转成正序（最早→最新）
      data = data.reverse()
      // ✅ 关键：标记历史消息为非新消息，禁用进入动画
      // ✅ 替换为：
      data = data.map(m => processMessage({ ...m, isNew: false }))

      // 无更多数据（后端返回不足pageSize，说明没有更早的了）
      if (data.length < pageSize.value) {
        noMoreData.value = true
      }

      // ✅ 反向加载逻辑：
      if (isLoadMore) {
        // 向上加载更多时，把更早的消息追加到列表头部
        messageList.value.unshift(...data)
      } else {
        // 初始加载，直接赋值（最新的20条，反转后最新在底部）
        messageList.value = data
      }
    }
  } catch (err) {
    console.error('获取聊天记录失败', err)
    ElMessage.error('获取聊天记录失败')
  } finally {
    showChatSkeleton.value = false
    // ✅ 只有初始加载（不是加载更多）时，才滚动到底部
    if (!isLoadMore) {
      await scrollToBottom(true)
    }
  }
}

const markAsRead = async () => {
  try {
    const res = await request.post('/user/chat/message', null, {
      params: {
        action: 'markRead',
        fromUserId: currentSession.value.targetUserId,
        toUserId: currentUserId.value
      }
    })
    if (res.code === 200) {
      const session = sessionList.value.find(s => s.targetUserId === currentSession.value.targetUserId);
      if (session) {
        session.unreadCount = 0;
      }
      window.refreshBasicUnread?.()
    }
  } catch (err) {
    console.error('标记已读失败', err)
  }
}
// ====================== 🔥 右键菜单相关变量 ======================
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const currentRightClickItem = ref(null) // 当前右键点击的对象
const contextMenuType = ref('') // 菜单类型：'friend' | 'message'
const selectedText = ref('') // 选中的文本

// ====================== 🔥 引用消息相关变量 ======================
const quotedMessage = ref(null) // 当前引用的消息

// ====================== 🔥 转发弹窗相关变量 ======================
const showForwardDialog = ref(false)
const forwardMessage = ref(null) // 要转发的消息

// ====================== 🔥 修改备注弹窗相关变量 ======================
const showRemarkDialog = ref(false)
const remarkFriend = ref(null)
const newRemark = ref('')

// 监听窗口尺寸
const isMobile = inject('isMobile')
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}
// 🔥 新增：手机端返回会话列表
const backToSessionList = () => {
  showChatView.value = false
  currentSession.value = null
}
// 🔥 新增：手机端视图控制（false=显示会话列表，true=显示聊天界面）
const showChatView = ref(false)
// ====================== 🔥 智能右键菜单处理 ======================
const handleContextMenu = (e) => {
  if (isMobile.value) return

  const target = e.target
  if (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.tagName === 'BUTTON' ||
      target.closest('.el-button') ||
      target.closest('.el-input') ||
      target.closest('.el-select') ||
      target.closest('.el-dialog')
  ) {
    return
  }

  e.preventDefault()
  e.stopPropagation()

  // 重置状态
  currentRightClickItem.value = null
  contextMenuType.value = ''
  selectedText.value = window.getSelection().toString().trim()

  // 1. 检测是否右键点击了好友列表项
  const friendItem = target.closest('.friend-item')
  if (friendItem && activeTab.value === 'friend') {
    const allFriends = document.querySelectorAll('.friend-item')
    const index = Array.from(allFriends).indexOf(friendItem)
    if (index >= 0 && index < friendList.value.length) {
      currentRightClickItem.value = friendList.value[index]
      contextMenuType.value = 'friend'
    }
  }

  // 2. 检测是否右键点击了聊天气泡
  const messageItem = target.closest('.message-item')
  if (messageItem && currentSession.value) {
    const allMessages = document.querySelectorAll('.message-item')
    const index = Array.from(allMessages).indexOf(messageItem)
    if (index >= 0 && index < messageList.value.length) {
      currentRightClickItem.value = messageList.value[index]
      contextMenuType.value = 'message'
    }
  }

  if (!currentRightClickItem.value) {
    return
  }

  // 计算菜单位置
  const menuWidth = 160
  let menuHeight = 0
  if (contextMenuType.value === 'friend') {
    menuHeight = 50
  } else {
    // 消息菜单高度根据内容动态计算
    const isSelf = String(currentRightClickItem.value.fromUserId) === String(currentUserId.value)
    const isText = currentRightClickItem.value.messageType === 1
    const isAi = currentSession.value.targetUserId === AI_DOUBAO_ID
    const canRecall = isSelf && !isAi && getMinutesDiffFromNow(currentRightClickItem.value.createTime) <= 2

    menuHeight = 50 // 基础高度
    if (selectedText.value) menuHeight += 50 // 有选中文本多一个复制
    if (!isAi) menuHeight += 50 // 非AI多一个引用
    if (canRecall) menuHeight += 50 // 只有能撤回时才加撤回按钮的高度
    menuHeight += 100 // 转发+删除
  }

  let x = e.clientX
  let y = e.clientY

  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

// 🔥 优化版：下载图片（兼容 普通好友聊天 + AI聊天）
const downloadImage = () => {
  const msg = currentRightClickItem.value;
  if (!msg || !isImageMessage(msg)) {
    ElMessage.warning('非图片消息');
    closeContextMenu();
    return;
  }

  let imagePath = '';
  const content = msg.messageContent;

  // 统一解析图片路径（兼容 文字+图片 / 纯图片）
  if (content.includes('|||')) {
    // 文字+图片 格式：文字|||图片路径
    imagePath = content.split('|||')[1];
  } else {
    // 纯图片 格式：直接是图片路径
    imagePath = content;
  }

  // 拼接完整URL
  const fullUrl = IMAGE_BASE_URL + imagePath;

  // 执行下载
  const a = document.createElement('a');
  a.href = fullUrl;
  a.download = imagePath.split('/').pop() || '聊天图片.png';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);

  ElMessage.success('图片下载成功');
  closeContextMenu();
};

// 🔥 核心：通用判断【是否为图片消息】，兼容 普通聊天 + AI聊天
const isImageMessage = (msg) => {
  return msg && msg.messageType === 2
}

// 关闭右键菜单
const closeContextMenu = () => {
  showContextMenu.value = false
  currentRightClickItem.value = null
  contextMenuType.value = ''
  selectedText.value = ''
}

// ====================== 🔥 右键菜单功能实现 ======================
// 获取被引用消息的显示内容（按messageType适配）
const getQuotedMessageContent = (replyToId) => {
  if (!replyToId) return ''
  const quotedMsg = messageList.value.find(msg => msg.id === replyToId)

  // 消息不存在/已撤回
  if (!quotedMsg || quotedMsg.isRecalled) {
    return '引用内容已撤回'
  }

  // 1. 文字消息：返回纯文字（截断）
  if (quotedMsg.messageType === 1) {
    const content = extractPureText(quotedMsg.messageContent) || ''
    return content.length > 50 ? content.substring(0, 50) + '...' : content
  }

  // 2. 图片消息：返回纯图片路径
  if (quotedMsg.messageType === 2) {
    return getForwardContent(quotedMsg.messageContent).split('|||').pop()
  }

  // 3. 文件消息：返回文件名
  if (quotedMsg.messageType === 3) {
    const { fileName } = parseFileMessage(quotedMsg.messageContent)
    return fileName || '文件'
  }

  return '[不支持的消息类型]'
}

// 获取被引用消息的类型（1文字 2图片 3文件）
const getQuotedMessageType = (replyToId) => {
  if (!replyToId) return 0
  const quotedMsg = messageList.value.find(msg => msg.id === replyToId)
  if (!quotedMsg || quotedMsg.isRecalled) return 1
  return quotedMsg.messageType
}

// 复制
const copyMessage = () => {
  const text = selectedText.value || currentRightClickItem.value.messageContent
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('复制成功')
  })
  closeContextMenu()
}

// 转发
const forwardMessageHandler = () => {
  forwardMessage.value = currentRightClickItem.value
  showForwardDialog.value = true
  closeContextMenu()
}

// 删除（隐藏）
const deleteMessage = async () => {
  try {
    await ElMessageBox.confirm('确定删除这条消息吗？', '删除提示', {
      type: 'warning', center: true
    })

    await request.post('/user/chat/message', null, {
      params: {
        action: 'hide',
        messageId: currentRightClickItem.value.id,
        userId: currentUserId.value
      }
    })

    // 从前端消息列表中移除
    const index = messageList.value.findIndex(m => m.id === currentRightClickItem.value.id)
    if (index !== -1) {
      messageList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
  closeContextMenu()
}

// 引用消息（适配所有类型）
const quoteMessage = () => {
  // 创建一个副本，防止修改原消息对象
  const quoted = { ...currentRightClickItem.value };
  quotedMessage.value = quoted;
  closeContextMenu();

  // 自动聚焦输入框
  nextTick(() => {
    document.querySelector('.chat-input-area .el-textarea__inner').focus();
  });
};

// 撤回
const recallMessage = async () => {
  try {
    await ElMessageBox.confirm('确定撤回这条消息吗？', '撤回提示', {
      type: 'warning', center: true
    })

    const res = await request.post('/user/chat/message', null, {
      params: {
        action: 'recall',
        messageId: currentRightClickItem.value.id,
        userId: currentUserId.value
      }
    })

    if (res.code === 200) {
      // 更新前端消息
      const index = messageList.value.findIndex(m => m.id === currentRightClickItem.value.id)
      if (index !== -1) {
        messageList.value[index] = res.data

        // 如果是文本消息，显示重新编辑链接
        /*if (res.data.messageType === 1) {
          showReeditLink.value = true
          reeditMessage.value = res.data
        }*/
      }

      ElMessage.success('撤回成功')
    } else {
      ElMessage.error(res.msg || '撤回失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('撤回失败')
    }
  }
  closeContextMenu()
}

// 重新编辑（🔥 重构：直接接收消息对象，不需要全局变量）
const reeditMessageHandler = (message) => {
  messageContent.value = message.originalContent
  // 自动聚焦输入框
  nextTick(() => {
    document.querySelector('.chat-input-area .el-textarea__inner').focus()
  })
}

// 修改备注
const openRemarkDialog = () => {
  remarkFriend.value = currentRightClickItem.value
  newRemark.value = remarkFriend.value.friendRemark || ''
  showRemarkDialog.value = true
  closeContextMenu()
}

const saveRemark = async () => {
  try {
    await request.post('/user/friend', null, {
      params: {
        action: 'updateRemark',
        userId: currentUserId.value,
        friendId: remarkFriend.value.friendId,
        remark: newRemark.value.trim()
      }
    })

    // 更新前端好友列表
    const index = friendList.value.findIndex(f => f.friendId === remarkFriend.value.friendId)
    if (index !== -1) {
      friendList.value[index].friendRemark = newRemark.value.trim()
    }

    ElMessage.success('备注修改成功')
    showRemarkDialog.value = false
  } catch (e) {
    ElMessage.error('修改备注失败')
  }
}

// 判断是否是转发消息
const isForwardMessage = (content) => {
  return content && content.startsWith('转发于')
}

// 提取转发前缀（第一行：转发于 XXX）
const getForwardPrefix = (content) => {
  if (!isForwardMessage(content)) return ''
  return content.split('\n')[0]
}

// 兼容原有getForwardContent函数（修正格式）
const getForwardContent = (content) => {
  if (!content) return ''
  // 移除转发前缀，保留核心内容
  const forwardPrefix = getForwardPrefix(content)
  return forwardPrefix ? content.replace(forwardPrefix, '') : content
}

// 确认转发 🔥 修复版：解决好友消失+实时刷新+置顶会话
const confirmForward = async (targetUserId) => {
  try {
    // 直接用你原来的 forwardMessage.value（永远有值，不会null）
    const msg = forwardMessage.value;

    // 🔥 仅新增：自动判断消息类型（文本/图片）
    const messageType = msg.messageType;

    if(!msg.fromUserId){
      msg.fromUserId = 'DouBao'
    }

    // 拼接转发内容（你原来的逻辑，完全不变）
    const forwardContent = `转发于 ${getMessageUserName(msg.fromUserId)}(${msg.fromUserId})\n${msg.messageContent}`;

    await request.post('/user/chat/message', null, {
      params: {
        action: 'send',
        fromUserId: currentUserId.value,
        toUserId: targetUserId,
        messageType: messageType, // 🔥 动态类型，不再固定1
        messageContent: forwardContent
      }
    })

    ElMessage.success('转发成功')
    showForwardDialog.value = false

    // ========== 新增代码：如果当前打开的就是目标会话，前端立刻追加消息 ==========
    if(currentSession.value && String(currentSession.value.targetUserId) === String(targetUserId)){
      const newForwardMsg = {
        id: Date.now(),
        fromUserId: currentUserId.value,
        toUserId: targetUserId,
        messageType: messageType,
        messageContent: forwardContent,
        createTime: new Date().toISOString(),
        isNew: true
      }
      messageList.value.push(newForwardMsg)
      scrollToBottom()
    }

// ====================== ✅ 修复：正确的会话置顶逻辑（和发消息完全一致） ======================
    // 1. 先找到原有会话
    const targetIndex = sessionList.value.findIndex(s => String(s.targetUserId) === String(targetUserId));
    let targetSession;
    // 2. 如果会话存在，先取出，再删除
    if (targetIndex > -1) {
      targetSession = sessionList.value.splice(targetIndex, 1)[0];
    } else {
      // 3. 不存在则创建新会话（无异步await，避免报错）
      targetSession = {
        targetUserId: targetUserId,
        userName: '好友',
        lastMessage: '[转发消息]',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0
      };
    }
    // 4. 更新会话最后一条消息
    targetSession.lastMessage = '[转发消息]';
    targetSession.lastMessageTime = new Date().toISOString();
    // 5. 插入到列表最顶部
    sessionList.value.unshift(targetSession);

  } catch (e) {
    console.error('转发失败', e)
    ElMessage.error('转发失败')
  }
}

// 取消引用
const cancelQuote = () => {
  quotedMessage.value = null
}

// 绑定输入框ref
const inputRef = ref(null)
const handleInputKey = (e) => {
  // 只处理Enter按键，其他按键直接放行原生行为
  if (e.key !== 'Enter') return

  // ✅ 正确获取ElInput内部原生textarea（官方规范取值）
  const innerTextarea = inputRef.value.$refs.textarea
  if (!innerTextarea) return

  // Ctrl+Enter：手动插入换行
  if (e.ctrlKey || e.metaKey) {
    e.preventDefault()
    const posStart = innerTextarea.selectionStart
    const posEnd = innerTextarea.selectionEnd
    // 拼接换行
    const oldText = messageContent.value
    messageContent.value = oldText.slice(0, posStart) + '\n' + oldText.slice(posEnd)
    // 光标跳到换行后面
    nextTick(() => {
      innerTextarea.selectionStart = posStart + 1
      innerTextarea.selectionEnd = posStart + 1
    })
  } else {
    // 普通Enter：阻止换行，发送消息
    e.preventDefault()
    sendMessage()
  }
}

// ====================== 🔥 最终版sendMessage：统一格式+纯文本加[text]标签 ======================
const sendMessage = async () => {
  // 🔥 新增：如果正在语音输入，点击发送按钮时，立刻结束语音识别
  if (isRecording && typeof stopSpeechRecognition === 'function') {
    stopSpeechRecognition()
  }

  // 👉 1. 校验：文字和所有资源不能同时为空
  if (!messageContent.value.trim()
      && localImages.value.length === 0
      && localFiles.value.length === 0
      && localVideos.value.length === 0
      && localVoices.value.length === 0) {
    ElMessage.warning('消息内容不能为空')
    return
  }

  if (!currentSession.value) return

  // 🔥 AI逻辑100%完全不变！
  if (currentSession.value.targetUserId === AI_DOUBAO_ID) {
    // 只要是文生图场景，强制走generateImage（图片+文字=图生图，纯文字=文生图）
    if (currentAiScene.value === 'image') {
      await generateImage()
    }else {
      // 正常聊天 → 调用改造后的AI聊天逻辑
      await sendMessageToAI()
    }
    return
  }

  previewBookCards.value = []
  const isStranger = !friendList.value.some(f => String(f.friendId) === String(currentSession.value.targetUserId))
  const targetUserId = currentSession.value?.targetUserId
  const hasTargetReply = messageList.value.some(msg => String(msg.fromUserId) === String(targetUserId))

  if (isStranger && messageList.value.length >= 1 && !hasTargetReply) {
    ElMessage.warning('等待对方回话后，即可继续聊天~')
    return
  }

  try {
    let finalMessageContent = messageContent.value.trim()
    let finalMessageType = 1 // 默认纯文字

    // 🔥 2. 批量上传图片（支持多张）
    if (localImages.value.length > 0) {
      if (finalMessageType !== 1) {
        ElMessage.warning('图片、文件和视频不能同时发送哦~')
        return
      }

      const total = localImages.value.length
      ElMessage.info(`正在上传第1/${total}张图片...`, 0)

      // 批量上传所有图片，等待全部完成
      const imageUrls = await Promise.all(
          localImages.value.map(async (item, index) => {
            const formData = new FormData()
            formData.append('action', 'upload')
            formData.append('commentId', '0') // ✅ 固定传0，表示非评论图片
            formData.append('image', item.file)

            const uploadRes = await request.post('/user/comment/image', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (uploadRes.code !== 200) {
              throw new Error(`第${index+1}张图片上传失败`)
            }

            // 更新上传进度提示
            if (index < total - 1) {
              ElMessage.info(`正在上传第${index+2}/${total}张图片...`, 0)
            }

            return uploadRes.data.imageUrl
          })
      )

      // ✅ 统一格式：[image]url1[image]url2...[text]文字
      let content = ''
      imageUrls.forEach(url => {
        content += `[image]${url}`
      })
      // ✅ 无论有没有文字，都加上[text]标签
      content += `[text]${finalMessageContent}`

      finalMessageContent = content
      finalMessageType = 2 // 图片类型=2
      localImages.value = [] // 清空本地预览
    }

    // 🔥 3. 批量上传文件（支持多张）
    else if (localFiles.value.length > 0) {
      if (finalMessageType !== 1) {
        ElMessage.warning('图片、文件和视频不能同时发送哦~')
        return
      }

      const total = localFiles.value.length
      ElMessage.info(`正在上传第1/${total}个文件...`, 0)

      // 批量上传所有文件，等待全部完成
      const documents = await Promise.all(
          localFiles.value.map(async (item, index) => {
            const formData = new FormData()
            formData.append('action', 'upload')
            formData.append('userId', currentUserId.value)
            formData.append('file', item.file)

            const uploadRes = await request.post('/chat/file', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (uploadRes.code !== 200) {
              throw new Error(`第${index+1}个文件上传失败`)
            }

            // 更新上传进度提示
            if (index < total - 1) {
              ElMessage.info(`正在上传第${index+2}/${total}个文件...`, 0)
            }

            return {
              url: uploadRes.data.documentUrl,
              name: item.name,
              size: item.size
            }
          })
      )

      // ✅ 统一格式：[document]url|||name|||size[document]url2|||name2|||size2...[text]文字
      let content = ''
      documents.forEach(doc => {
        content += `[document]${doc.url}|||${doc.name}|||${doc.size}`
      })
      // ✅ 无论有没有文字，都加上[text]标签
      content += `[text]${finalMessageContent}`

      finalMessageContent = content
      finalMessageType = 3 // 文件类型=3
      localFiles.value = [] // 清空本地预览
    }

    // 🔥 4. 批量上传视频（支持多张）
    else if (localVideos.value.length > 0) {
      if (finalMessageType !== 1) {
        ElMessage.warning('图片、文件和视频不能同时发送哦~')
        return
      }

      const total = localVideos.value.length
      ElMessage.info(`正在上传第1/${total}个视频...`, 0)

      // 批量上传所有视频，等待全部完成
      const videoUrls = await Promise.all(
          localVideos.value.map(async (item, index) => {
            const formData = new FormData()
            formData.append('action', 'upload')
            formData.append('userId', currentUserId.value)
            formData.append('file', item.file)

            const uploadRes = await request.post('/chat/file', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (uploadRes.code !== 200) {
              throw new Error(`第${index+1}个视频上传失败`)
            }

            // 更新上传进度提示
            if (index < total - 1) {
              ElMessage.info(`正在上传第${index+2}/${total}个视频...`, 0)
            }

            return uploadRes.data.documentUrl
          })
      )

      // ✅ 统一格式：[video]url1[video]url2...[text]文字
      let content = ''
      videoUrls.forEach(url => {
        content += `[video]${url}`
      })
      // ✅ 无论有没有文字，都加上[text]标签
      content += `[text]${finalMessageContent}`

      finalMessageContent = content
      finalMessageType = 4 // 视频类型=4
      localVideos.value = [] // 清空本地预览
    }

    // 🔥 5. 新增：上传语音消息
    else if (localVoices.value.length > 0) {
      if (finalMessageType !== 1) {
        ElMessage.warning('语音不能和其他资源同时发送哦~')
        return
      }

      const total = localVoices.value.length
      ElMessage.info(`正在上传第1/${total}条语音...`, 0)

      // 批量上传所有语音
      const voices = await Promise.all(
          localVoices.value.map(async (item, index) => {
            const formData = new FormData()
            formData.append('action', 'upload')
            formData.append('userId', currentUserId.value)
            formData.append('file', item.file)

            const uploadRes = await request.post('/chat/file', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })

            if (uploadRes.code !== 200) {
              throw new Error(`第${index+1}条语音上传失败`)
            }

            if (index < total - 1) {
              ElMessage.info(`正在上传第${index+2}/${total}条语音...`, 0)
            }

            return {
              url: uploadRes.data.documentUrl,
              duration: item.duration
            }
          })
      )

      // ✅ 拼接新格式：[voice]url|||时长[voice]url2|||时长2...[text]文字
      let content = ''
      voices.forEach(voice => {
        content += `[voice]${voice.url}|||${voice.duration}`
      })
      // 无论有没有文字，都加上[text]标签
      content += `[text]${finalMessageContent}`

      finalMessageContent = content
      finalMessageType = MESSAGE_TYPE_VOICE // 语音类型=5
      localVoices.value = [] // 清空本地预览
    }

    // ✅ 🔥 纯文本消息：统一加上[text]标签
    else {
      finalMessageContent = `[text]${finalMessageContent}`
    }

    // 3. 发送最终消息
    const params = {
      action: 'send',
      fromUserId: currentUserId.value,
      toUserId: currentSession.value.targetUserId,
      messageType: finalMessageType,
      messageContent: finalMessageContent
    }

    // 如果有引用消息，添加replyToId参数
    if (quotedMessage.value) {
      params.replyToId = quotedMessage.value.id
    }

    const res = await request.post('/user/chat/message', null, { params })

    if (res.code === 200) {
      const messageId = res.data
      // 4. 创建前端消息对象
      const newMessage = {
        id: messageId,
        fromUserId: currentUserId.value,
        toUserId: currentSession.value.targetUserId,
        messageType: finalMessageType,
        messageContent: finalMessageContent,
        createTime: new Date().toISOString(),
        replyToId: quotedMessage.value?.id || null,
        isNew: true // 标记为新消息，触发进入动画
      }

      // 5. 添加到消息列表，清空输入
      // ✅ 替换为：
      messageList.value.push(processMessage(newMessage))
      messageContent.value = ''
      quotedMessage.value = null // 清空引用
      scrollToBottom()

      // 更新会话最后一条消息
      const lastMsgMap = {
        1: finalMessageContent.replace('[text]', ''),
        2: '[图片]',
        3: '[文件]',
        4: '[视频]',
        5: '[语音]' // 新增语音
      }
      currentSession.value.lastMessage = lastMsgMap[finalMessageType] || finalMessageContent
      currentSession.value.lastMessageTime = new Date().toISOString()

      // ✅ 置顶会话：已有就删掉原位置，插到最顶部
      let idx = sessionList.value.findIndex(s => String(s.targetUserId) === String(currentSession.value.targetUserId))
      if (idx > -1) {
        sessionList.value.splice(idx, 1)
      }
      sessionList.value.unshift({ ...currentSession.value })

    } else {
      ElMessage.error(res.msg || '发送消息失败')
    }
  } catch (err) {
    console.error('发送消息失败', err)
    ElMessage.error(err.message || '发送消息失败')
  }
}
// ====================== 🔥 聊天图片本地预览（和论坛逻辑完全一致） ======================
// 本地预览图片数组
const localImages = ref([])
// 文件DOM引用（保留原来的）
const imageInput = ref(null)

const videoInput = ref(null)
const localVideos = ref([]) // 存储本地选择的视频：{ file: File, previewUrl: string }

// 语音输入状态
const isRecording = ref(false)
let recognition = null // 存储 Web Speech API 实例

// ====================== 🔥 新增：聊天文件本地预览 ======================
const localFiles = ref([])   // 本地预览文件数组
const fileInput = ref(null)  // 文件上传DOM引用
// 文件图标映射（根据后缀显示对应图标）
const fileIconMap = {
  pdf: '📄', doc: '📃', docx: '📃', xls: '📊', xlsx: '📊',
  ppt: '📑', pptx: '📑', txt: '📝', mp3: '🎵', zip: '🗂️', rar: '🗂️'
}

// 触发图片选择
const triggerImageUpload = () => {
  if (localImages.value.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }
  imageInput.value.click()
}

// 选择文件 → 仅本地预览，不上传服务器
const handleImageUpload = (e) => {
  const files = e.target.files
  if (!files || files.length === 0) return
  if (localImages.value.length + files.length > 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }

  // 遍历文件，生成本地预览地址
  for (let file of files) {
    const reader = new FileReader()
    reader.onload = (evt) => {
      localImages.value.push({
        file: file,                // 真实文件（发送时用）
        previewUrl: evt.target.result // 本地预览地址
      })
    }
    reader.readAsDataURL(file)
  }
  imageInput.value.value = '' // 清空input，支持重复选择同一张图
}

// 删除本地预览图片
const deleteChatImage = (index) => {
  localImages.value.splice(index, 1)
}

// ====================== 🔥 文件上传核心方法 ======================
// 触发文件选择
const triggerFileUpload = () => {
  if (localFiles.value.length >= 9) {
    ElMessage.warning('单次最多上传9个文件') // 修正文案，匹配上限9
    return
  }
  fileInput.value.click()
}

// 选择文件 → 仅本地预览，不上传
const handleFileUpload = (e) => {
  const files = e.target.files
  if (!files || files.length === 0) return

  for (const file of files) {
    // 校验总数上限
    if (localFiles.value.length >= 9) {
      ElMessage.warning('单次最多上传9个文件，部分文件未添加')
      break
    }
    // 获取后缀图标
    const suffix = file.name.split('.').pop().toLowerCase()
    const icon = fileIconMap[suffix] || '📎'
    // 格式化大小
    const size = file.size < 1024 * 1024
        ? (file.size / 1024).toFixed(1) + ' KB'
        : (file.size / (1024 * 1024)).toFixed(1) + ' MB'

    localFiles.value.push({
      file: file,
      name: file.name,
      icon: icon,
      size: size,
      suffix: suffix
    })
  }

  fileInput.value.value = ''
}

// 删除本地预览文件
const deleteChatFile = (index) => {
  localFiles.value.splice(index, 1)
}

// ====================== 🔥 文件消息工具方法 ======================
// 兼容原有parseFileMessage函数（修正格式+增强批量支持）
const parseFileMessage = (content) => {
  const batchContent = parseBatchContent(content)
  if (batchContent.documents.length > 0) {
    return {
      fileName: batchContent.documents[0].meta.fileName,
      fileSize: batchContent.documents[0].meta.fileSize,
      text: batchContent.text,
      fileUrl: batchContent.documents[0].url
    }
  }
  // 原有逻辑兜底
  const parts = content.split('|||')
  return {
    fileName: parts[1] || '',
    fileSize: parts[2] || '',
    text: parts[0] || '',
    fileUrl: parts[1] || ''
  }
}
// 🔥 新增：文件名截断工具（保留后缀，支持中文15字单行）
const truncateFileName = (fileName, maxLength = 36) => {
  if (!fileName) return '未知文件';

  // 1. 分离文件名和后缀（处理无后缀/多后缀情况）
  const lastDotIndex = fileName.lastIndexOf('.');
  let baseName = fileName;
  let extension = '';

  if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
    baseName = fileName.substring(0, lastDotIndex);
    extension = '.' + fileName.substring(lastDotIndex + 1);
  }

  // 2. 计算显示长度（中文算1.5字符，英文/数字算1字符）
  const getDisplayLength = (str) => {
    return str.split('').reduce((len, char) => {
      return len + (char.match(/[\u4e00-\u9fa5]/) ? 1.5 : 1);
    }, 0);
  };

  // 3. 截断逻辑（确保后缀显示完整）
  if (getDisplayLength(baseName) <= maxLength) {
    return fileName; // 长度足够，完整显示
  }

  // 4. 逐步截断直到长度合适
  let truncatedBase = baseName;
  while (getDisplayLength(truncatedBase) > maxLength && truncatedBase.length > 1) {
    truncatedBase = truncatedBase.slice(0, -1);
  }

  return `${truncatedBase}...${extension}`;
};

// 获取文件图标
const getFileIcon = (url) => {
  if (!url) return '📎'
  const suffix = url.split('.').pop().toLowerCase()
  return fileIconMap[suffix] || '📎'
}

// 获取文件名（从url截取）
const getFileName = (url) => {
  if (!url) return '未知文件'
  // 拆分路径|||原始名称
  if(url.includes('|||')){
    return url.split('|||')[1]
  }
  return url.split('/').pop()
}

// 格式化文件大小（聊天消息显示用，发送时会查询后端）
const getFileSizeText = (url) => {
  if (!url) return '点击预览'
  const splitArr = url.split('|||')
  // 新格式：路径|||名字|||大小 → 取第三个值
  if (splitArr.length >= 3) {
    return splitArr[2]
  }
  // 老历史数据没有名字+大小，兼容兜底
  return '点击预览'
}

// 下载聊天文件（适配 文字+文件 / 纯文件 格式）
const downloadChatFile = async (url) => {
  try {
    // 1. 解析消息，拿到纯文件路径
    const { fileUrl } = parseFileMessage(url)
    let realUrl = fileUrl

    // 2. 验证路径格式
    if (!realUrl.startsWith('/chat_files/')) {
      ElMessage.error('无效的文件路径')
      return
    }

    // 3. 调用接口查询文件
    const res = await request.get('/chat/file', {
      params: { action: 'getByUrl', url: realUrl }
    })

    if (res.code !== 200) {
      ElMessage.error('文件不存在或已删除')
      return
    }

    // 4. 下载
    const targetUrl = `/api/chat/file?action=preview&url=${encodeURIComponent(realUrl)}`
    const link = document.createElement('a')
    link.href = targetUrl
    link.target = '_blank'
    link.click()
    ElMessage.success('文件下载成功')

  } catch (e) {
    ElMessage.error('下载失败，请重试')
    console.error('文件下载错误:', e)
  }
}

// 触发视频选择
const triggerVideoUpload = () => {
  if (localVideos.value.length >= 9) {
    ElMessage.warning('单次只能上传9个视频')
    return
  }
  videoInput.value?.click()
}

const handleVideoUpload = (e) => {
  const files = e.target.files
  if (!files || files.length === 0) return

  // 修复：循环所有选中文件，不再只取第一个
  for (const file of files) {
    // 限制单视频100MB
    if (file.size > 100 * 1024 * 1024) {
      ElMessage.error(`视频 ${file.name} 超过100MB限制，已跳过`)
      continue
    }
    const previewUrl = URL.createObjectURL(file)
    localVideos.value.push({
      file: file,
      previewUrl: previewUrl
    })
  }

  e.target.value = ''
}

// 删除预览视频
const deleteChatVideo = (index) => {
  if (localVideos.value[index]) {
    URL.revokeObjectURL(localVideos.value[index].previewUrl)
    localVideos.value.splice(index, 1)
  }
}

// 初始化并切换语音输入状态
const toggleSpeechRecognition = () => {
  if (isRecording.value) {
    // 再次点击图标：正常手动结束
    stopSpeechRecognition()
    return
  }

  // 检查浏览器是否支持
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('好可惜，您的浏览器暂不支持语音转文字功能功能 😭')
    return
  }

  recognition = new SpeechRecognition()
  recognition.continuous = true // 连续识别
  recognition.interimResults = true // 实时产生临时结果
  recognition.lang = 'zh-CN' // 设为中文

  recognition.onstart = () => {
    isRecording.value = true
    ElMessage.success('语音输入已开启，请说话... 🎙️')
  }

  recognition.onerror = (event) => {
    console.error('语音识别错啦: ', event.error)
    stopSpeechRecognition()
  }

  recognition.onend = () => {
    isRecording.value = false
  }

  // 实时捕获语音并追加/替换到输入框中
  recognition.onresult = (event) => {
    let resultText = ''
    for (let i = event.resultIndex; i < event.results.length; ++i) {
      if (event.results[i].isFinal) {
        // 最终稳定的识别结果
        messageContent.value += event.results[i][0].transcript
      }
    }
  }

  recognition.start()
}

// 结束语音输入
const stopSpeechRecognition = () => {
  if (recognition) {
    recognition.stop()
    recognition = null
  }
  isRecording.value = false
}
// ====================== 🔥 表情包功能变量 ======================
// 表情包面板显示状态
const showEmojiPanel = ref(false)
// ====================== 🔥 表情包分组数据 ======================
const emojiGroups = ref([
  {
    name: '表情',
    items: [
      '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊',
      '😋', '😎', '😍', '🥰', '😘', '😗', '😙', '😚', '🙂', '🤗',
      '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥'
    ]
  },
  {
    name: '爱心',
    items: [
      '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '💔', '💖', '💗',
      '💓', '💞', '💕', '💝', '💘', '💟', '❣️', '💌', '💎', '💍'
    ]
  },
  {
    name: '手势',
    items: [
      '👍', '👎', '👏', '🙌', '✌️', '🤞', '🤝', '🙏', '👋', '🤚',
      '🖐️', '✋', '👌', '🤌', '🤏', '👈', '👉', '👆', '👇', '☝️'
    ]
  },
  {
    name: '符号',
    items: [
      '✨', '🌟', '💫', '⭐', '💥', '🔥', '💯', '✅', '❌', '⭕',
      '❓', '❗', '💢', '💤', '💦', '💨', '🌈', '☀️', '🌙', '💭'
    ]
  }
])
// 当前选中的表情包分类标签
const activeEmojiTab = ref('表情')

// 切换表情包面板显示
const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value
}

// 插入emoji到输入框（支持光标位置插入）
const insertEmoji = (emoji) => {
  const textarea = document.querySelector('.chat-input-area .el-textarea__inner')
  if (!textarea) return

  // 获取当前光标位置
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const oldContent = messageContent.value

  // 拼接新内容
  const newContent = oldContent.substring(0, start) + emoji + oldContent.substring(end)
  messageContent.value = newContent

  // 光标移到插入的emoji后面
  nextTick(() => {
    const newCursorPos = start + emoji.length
    textarea.selectionStart = newCursorPos
    textarea.selectionEnd = newCursorPos
    textarea.focus()
  })
}

// 点击其他地方自动关闭面板（优化体验）
document.addEventListener('click', (e) => {
  const emojiBtn = document.querySelector('.emoji-btn')
  const emojiPanel = document.querySelector('.emoji-panel')
  if (emojiBtn && emojiPanel && !emojiBtn.contains(e.target) && !emojiPanel.contains(e.target)) {
    showEmojiPanel.value = false
  }
})
// ====================== 🔥 新增：用户搜索&申请好友逻辑 ======================
// 搜索用户（适配你的后端接口）
const searchUsers = async () => {
  if (!searchUserKey.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }
  try {
    // 根据查询类型动态传参
    const params = {}
    if (queryUserType.value === 'userId') {
      params.userId = searchUserKey.value.trim()
    } else if (queryUserType.value === 'name') {
      params.name = searchUserKey.value.trim()
    }
    const res = await request.get('/user/info', { params })
    if (res.code === 200) {
      // 处理后端返回的单个对象/列表，统一转为数组
      searchResult.value = Array.isArray(res.data) ? res.data : (res.data ? [res.data] : [])
      if (searchResult.value.length === 0) {
        ElMessage.info('未找到匹配的用户')
      }
    } else {
      ElMessage.error(res.msg || '搜索失败')
      searchResult.value = []
    }
  } catch (err) {
    console.error('搜索用户失败', err)
    ElMessage.error('搜索用户失败，请稍后再试')
    searchResult.value = []
  }
}

// 1. 判断用户是否为好友（兼容数字/字符串类型）
const isFriend = (userId) => {
  return friendList.value.some(friend =>
      String(friend.friendId) === String(userId)
  )
}

// 2. 获取按钮文字
const getButtonText = (user) => {
  if (String(user.userId) === String(currentUserId.value)) {
    return '不能添加自己'
  }
  if (isFriend(user.userId)) {
    return '已成为好友'
  }
  return '申请好友'
}

// 3. 获取按钮类型（default 或 primary）
const getButtonType = (user) => {
  if (String(user.userId) === String(currentUserId.value) || isFriend(user.userId)) {
    return 'default'
  }
  return 'primary'
}

// 4. 判断按钮是否禁用
const isButtonDisabled = (user) => {
  return (
      String(user.userId) === String(currentUserId.value) ||
      isFriend(user.userId)
  )
}
// 申请好友（复用你给的代码）
const handleAddFriend = async (targetUserId, targetUserName) => {
  if (targetUserId === currentUserId.value) {
    ElMessage.warning('不能添加自己为好友')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
        `向 ${targetUserName} (${targetUserId}) 发送好友申请`,
        '添加好友',
        {
          confirmButtonText: '发送',
          cancelButtonText: '取消',
          type: 'info',
          inputPlaceholder: '请输入申请消息（选填）',
          inputValidator: (val) => {
            if (val && val.length > 50) return '申请消息不能超过50字'
            return true
          }
        }
    )
    const requestMsg = value?.trim() || `你好，我是${currentUserInfo.value?.name || currentUserId.value}，通过系统认识你的~`
    const res = await request.post('/user/friend/request', null, {
      params: {
        action: 'send',
        fromUserId: currentUserId.value,
        toUserId: targetUserId,
        requestMsg: requestMsg
      }
    })
    if (res.code === 200) {
      ElMessage.success('好友申请已发送，请等待对方同意')
      achievementStore.checkAchievements()
    } else {
      ElMessage.error(res.msg || '发送好友申请失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      console.error('发送好友申请失败', err)
      ElMessage.error('发送失败，请稍后再试')
    }
  }
}

// 计算指定时间到现在的分钟差（支持ISO字符串、时间戳等多种格式）
const getMinutesDiffFromNow = (timeStr) => {
  if (!timeStr) return 999; // 时间无效，默认超过2分钟

  const targetTime = new Date(timeStr);
  if (isNaN(targetTime.getTime())) return 999; // 时间解析失败，默认超过2分钟

  const now = new Date();
  const diffMs = now - targetTime;
  return Math.floor(diffMs / (1000 * 60)); // 转换为分钟，向下取整
};
// 定时器：每分钟检查一次撤回消息的可编辑状态
let recallCheckTimer = null;

// 启动撤回检查定时器
const startRecallCheckTimer = () => {
  // 先清除旧定时器
  if (recallCheckTimer) {
    clearInterval(recallCheckTimer);
  }

  // 每分钟检查一次
  recallCheckTimer = setInterval(() => {
    // 强制触发Vue重新渲染（因为时间是响应式的，会自动重新计算getMinutesDiff）
    messageList.value = [...messageList.value];
  }, 60000); // 60秒检查一次
};

// ====================== 🔥 新增：SSE 回调注册 ======================
// 从BasicLayout注入的回调管理器
const registerChatSSECallback = inject('registerChatSSECallback')

// 处理新消息
const handleNewMessage = (data) => {
  // ✅ 关键：兼容两种后端格式
  let messageData = null
  // 情况1：后端返回的是 { type: "NEW_MESSAGE", data: {...} }
  if (data && data.data) {
    messageData = data.data
  }
  // 情况2：后端直接返回消息对象 { fromUserId: "...", ... }
  else if (data && data.fromUserId) {
    messageData = data
  }

  if (!messageData) {
    console.error("无效的NEW_MESSAGE数据：", data)
    return
  }

  // 🔥 新增：自动修正语音消息类型（兼容后端bug）
  if (messageData.messageContent?.includes('[voice]') && messageData.messageType !== 5) {
    console.log('自动修正语音消息类型：', messageData)
    messageData.messageType = 5
  }

  // 👉 3. 最后看后端返回的消息内容
  console.log('【调试】收到的后端消息：', messageData)
  // 如果当前正在和发消息的人聊天，直接添加到消息列表
  if (currentSession.value && String(messageData.fromUserId) === String(currentSession.value.targetUserId)) {
    messageData.isNew = true // 标记为新消息，触发进入动画
    // ✅ 替换为：
    messageList.value.push(processMessage(messageData))
    scrollToBottom()
    markAsRead()
  } else {
    // 不在当前会话，刷新会话列表
    getSessionList()
  }
}

// 处理消息撤回
const handleRecallMessage = (messageId) => {
  const index = messageList.value.findIndex(m => m.id === messageId)
  if (index !== -1) {
    messageList.value[index].isRecalled = true
    messageList.value[index].messageContent = '对方撤回了一条消息'
  }
}

// 🔥 修复后的：处理AI思考过程（同时支持字符串和对象格式）
const handleAiThinking = (text) => {
  if (currentSession.value && currentSession.value.targetUserId === AI_DOUBAO_ID) {
    isAiThinking.value = true
    console.log('收到AI思考消息：', text, '类型：', typeof text) // 调试日志

    // 🔥 同时支持两种格式：
    // 1. 后端直接推送的JSON对象
    // 2. 未解析的JSON字符串
    let isAppend = false
    let appendContent = ''

    if (typeof text === 'object' && text.type === 'AI_THINKING_APPEND') {
      // 情况1：已经是解析好的对象（你的情况）
      isAppend = true
      appendContent = text.content
    } else if (typeof text === 'string') {
      // 情况2：还是字符串，尝试解析
      if (text.startsWith('{') && text.includes('"type":"AI_THINKING_APPEND"')) {
        try {
          const msg = JSON.parse(text)
          isAppend = true
          appendContent = msg.content
        } catch (e) {
          // 解析失败，当作普通文本
          isAppend = false
        }
      }
    }

    if (isAppend) {
      // 追加模式
      aiThinkingText.value += appendContent.replace(/\n/g, '<br>')
    } else {
      // 普通模式，覆盖内容
      aiThinkingText.value = String(text).replace(/\n/g, '<br>')
    }

    // 自动滚动到底部
    nextTick(() => {
      scrollToBottom()
    })
  }
}

// 🔥 处理AI思考完成
const handleAiThinkingDone = () => {
  isAiThinking.value = false
  aiThinkingText.value = '🤔 正在思考中'
}

// 🔥 新增：处理消息已读事件
const handleMessageRead = (data) => {
  // ✅ 兼容两种后端格式（和handleNewMessage保持一致）
  let readData = null
  // 情况1：后端返回 { type: "MESSAGE_READ", data: {...} }
  if (data && data.data) {
    readData = data.data
  }
  // 情况2：后端直接返回已读对象 { fromUserId: "...", toUserId: "..." }
  else if (data && data.fromUserId && data.toUserId) {
    readData = data
  }

  if (!readData) {
    console.error("无效的MESSAGE_READ数据：", data)
    return
  }

  console.log('【调试】收到已读通知，更新消息状态：', readData)

  // ✅ 核心：更新本地消息列表的isRead状态
  // 逻辑：自己发的消息，对方读了，就把isRead设为1
  messageList.value.forEach(msg => {
    if (
        String(msg.fromUserId) === String(readData.toUserId) && // 消息是我发的
        String(msg.toUserId) === String(readData.fromUserId)    // 消息是发给对方的
    ) {
      msg.isRead = 1
    }
  })

  // ✅ 刷新会话列表（同步更新会话的未读数）
  getSessionList()
}
// ====================== 原生命周期（不变） ======================
// 修改后（加 async + await）
onMounted(async () => {
  window.getSessionList = getSessionList

  // 🔥 注册SSE回调
  // ✅ 正确：调用注册方法，把所有回调传给BasicLayout
  registerChatSSECallback({
    onNewMessage: handleNewMessage,
    onRecallMessage: handleRecallMessage,
    onAiThinking: handleAiThinking,
    onAiThinkingDone: handleAiThinkingDone,
    onMessageRead: handleMessageRead // 🔥 新增：注册已读事件回调
  })

  await getSessionList() // 先等后端会话列表加载完成
  addDoubaoToSessionList() // 再添加豆包，不会被覆盖
  // 🔥 核心：监听路由是否携带 userId
  const targetUserId = route.query.userId
  if (targetUserId) {
    isFromProfile.value = true
    await openChatByUserId(targetUserId)
  }

  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu)
  document.addEventListener('contextmenu', handleContextMenu)

  startRecallCheckTimer(); // 启动撤回检查定时器
})

// 🔥 新增：监听路由变化（防止重复跳转）
watch(
    () => route.query.userId,
    async (newUserId) => {
      if (newUserId) {
        await openChatByUserId(newUserId)
      }
    },
    { immediate: false }
)
watch(messageList, async () => {
  await nextTick()
  updateAllBookCards()
}, { deep: true })

onUnmounted(() => {
  if (recallCheckTimer) clearInterval(recallCheckTimer); // 清除定时器

  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('scroll', closeContextMenu)
  document.removeEventListener('contextmenu', handleContextMenu)

  // ✅ 正确：清除所有回调
  registerChatSSECallback({
    onNewMessage: null,
    onRecallMessage: null,
    onAiThinking: null,
    onAiThinkingDone: null
  })
  // 🔥 卸载时清除全局方法
  window.getSessionList = null
});
</script>


<style scoped>
/* ========== 全局磨砂玻璃样式 ========== */
.glass-effect {
  background: rgba(255, 255, 255, 0.45) !important;
  backdrop-filter: blur(12px) !important;
  -webkit-backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
  border-radius: 8px;
}

/* 自己发送的消息磨砂玻璃样式 */
.message-self .glass-effect,
.message-self .message-text,
.message-self .voice-msg-wrap,
.message-self .forward-prefix {
  background: rgba(64, 158, 255, 0.65) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: white !important;
}

/* 自己发送的引用消息样式 */
.message-self .quote-message.glass-effect {
  background: rgba(230, 247, 255, 0.45) !important;
  border-left: 3px solid #409eff !important;
  color: #333 !important;
}

/* 深色模式适配 */
:deep(.dark-mode) .glass-effect {
  background: rgba(31, 41, 55, 0.5) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: #e5e7eb !important;
}

:deep(.dark-mode) .message-self .glass-effect,
:deep(.dark-mode) .message-self .message-text,
:deep(.dark-mode) .message-self .voice-msg-wrap,
:deep(.dark-mode) .message-self .forward-prefix {
  background: rgba(30, 58, 95, 0.75) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

:deep(.dark-mode) .message-self .quote-message.glass-effect {
  background: rgba(30, 58, 95, 0.45) !important;
  color: #e5e7eb !important;
}

/* ========== 图片折叠画廊样式 ========== */
.image-gallery-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}

/* 自己发送的图片画廊靠右 */
.image-gallery-container.is-self {
  flex-direction: row-reverse;
}

/* 折叠状态 */
.gallery-folded {
  display: flex;
  align-items: center;
  gap: 8px;
}
.image-gallery-container.is-self .gallery-folded {
  flex-direction: row-reverse;
}

.gallery-track {
  position: relative;
  width: 220px;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gallery-item {
  position: absolute;
  width: 140px;
  height: 140px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
  cursor: pointer;
}

.gallery-item .talk-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: filter 0.3s ease;
}

/* 画廊图片位置样式 */
.gallery-item.is-center {
  transform: translateX(0) scale(1);
  z-index: 3;
  opacity: 1;
}
.gallery-item.is-left {
  transform: translateX(-55px) scale(0.9);
  z-index: 2;
  opacity: 0.9;
  filter: brightness(0.85);
}
.gallery-item.is-right {
  transform: translateX(55px) scale(0.9);
  z-index: 2;
  opacity: 0.9;
  filter: brightness(0.85);
}
.gallery-item.is-hidden {
  transform: scale(0.5);
  z-index: 1;
  opacity: 0;
  pointer-events: none;
}

/* 展开/收起按钮 */
.gallery-expand-btn, .gallery-collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  color: #409eff;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.gallery-expand-btn:hover, .gallery-collapse-btn:hover {
  transform: scale(1.1);
  color: #60a5fa;
}

/* 展开状态 */
.gallery-expanded {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.image-gallery-container.is-self .gallery-expanded {
  flex-direction: row-reverse;
}

.gallery-expanded-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 展开列表动画 */
.gallery-expand-list-enter-active,
.gallery-expand-list-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.gallery-expand-list-enter-from,
.gallery-expand-list-leave-to {
  opacity: 0;
  transform: translateY(-20px) scaleY(0.8);
}

/* ======================================
   1. 全局基础样式 & 通用工具类
======================================== */
/* 滚动平滑 */
.chat-messages {
  overflow-y: auto;
}
/* 通用右对齐（所有自己发的消息统一） */
.self-wrap {
  margin-left: auto !important;
  text-align: right !important;
}

.self-wrapper .book-share-card,
.self-wrap .book-share-card-wrapper .book-share-card {
  margin-left: auto !important;
}

.self-wrap .message-text {
  align-self: flex-end !important;
  margin-left: auto !important;
}

.self-wrap .el-divider {
  margin-left: auto !important;
  margin-right: 0 !important;
}

/* 通用删除按钮 */
.delete-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  background: rgba(0,0,0,0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  font-size: 14px;
  z-index: 10;
}

/* 通用媒体列表 */
.media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 通用过渡动画 */
.transition-base {
  transition: all 0.2s ease;
}

/* ======================================
   2. 通用组件样式（全站复用）
======================================== */
/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 99999 !important;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 8px 0;
  min-width: 160px;
  font-size: 14px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: #f5f7fa;
}

.context-menu-item.danger {
  color: #f56c6c;
}

.context-menu-item.danger:hover {
  background-color: #fef0f0;
}

/* 转发列表 */
.forward-list {
  max-height: 400px;
  overflow-y: auto;
}

.forward-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.forward-item:hover {
  background-color: #f5f7fa;
}

.forward-name {
  font-size: 14px;
  font-weight: 500;
}

/* 骨架屏 */
.skeleton-bubble {
  width: 120px;
  height: 36px;
  background: #f1f1f1;
  border-radius: 18px;
  animation: skeleton-loading 1.2s infinite ease-in-out;
}

.skeleton-text {
  width: 40px;
  height: 12px;
  background: #f1f1f1;
  border-radius: 6px;
  animation: skeleton-loading 1.2s infinite ease-in-out;
}

@keyframes skeleton-loading {
  0% { opacity: 0.6; }
  50% { opacity: 0.3; }
  100% { opacity: 0.6; }
}

/* 录音呼吸灯动画 */
.recording-icon {
  color: #f56c6c !important;
  animation: speech-pulse 1.4s infinite ease-in-out;
}

@keyframes speech-pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.6; }
  100% { transform: scale(1); opacity: 1; }
}

/* ======================================
   3. 左侧会话列表样式
======================================== */
.chat-container {
  padding: 20px;
  height: 100%;
}

.session-list-col {
  border-right: 1px solid var(--glass-border);
  height: 100%;
}

.session-list-col .el-tabs {
  height: 100%;
}

.session-list-col .el-tabs__content {
  height: calc(100% - 60px);
  overflow-y: auto;
}

.session-list {
  height: 100%;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.session-item:hover,
.session-item.active {
  background-color: rgba(64, 158, 255, 0.15);
}

.session-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
  text-align: left;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-last-message {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-right {
  text-align: right;
}

.session-time {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.session-badge {
  margin-top: 4px;
}

.empty-list {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 14px;
}

/* ======================================
   4. 右侧聊天窗口核心样式
======================================== */
.chat-window-col {
  height: 100%;
}

.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 聊天头部 */
.chat-header {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.chat-name {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 500;
}

/* 消息列表通用 */
.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  height: calc(100% - 60px);
}

.message-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 12px;
  position: relative;
  z-index: 1;

  /* 🔥 性能黑魔法：不在屏幕内的聊天气泡，完全跳过渲染和排版！ */
  content-visibility: auto;
  contain-intrinsic-size: 80px; /* 预估单个气泡高度，防止滚动条抖动 */
  will-change: transform;
  transform: translateZ(0); /* 强制开启 GPU 独立图层加速 */
}

.message-item.message-self {
  flex-direction: row-reverse;
}
/* 彻底隔离self右对齐样式对横向滚动卡片的干扰 */
:deep(.self-wrapper .multi-card-slider-wrapper .simplified-card),
:deep(.self-wrap .multi-card-slider-wrapper .simplified-card) {
  margin: 0 !important;
  margin-left: 0 !important;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

/* 消息气泡通用 */
.message-text {
  padding: 10px 14px;
  border-radius: 8px;
  background-color: white;
  font-size: 14px;
  line-height: 1.5;
  text-align: left;
  width: fit-content;
  max-width: 500px;
  white-space: pre-line;
  word-break: break-word;
}

.message-self .message-text {
  background-color: #409eff;
  color: white;
  align-self: flex-end;
}

/* 引用消息 */
.quote-message {
  border-left: 3px solid #315961;
  padding: 4px 8px;
  margin-bottom: 8px;
  background-color: #D0F2D6;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  max-width: 60%;
  min-width: 100px;
  white-space: normal;
  word-break: break-all;
  align-self: flex-start;
}

.message-self .quote-message {
  background-color: #e6f7ff;
  border-left: 3px solid #409eff;
  align-self: flex-end;
}

.quote-image {
  max-width: 120px;
  max-height: 90px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 4px;
}

.quote-file {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
}

.quote-file .file-icon {
  font-size: 16px;
}

.quote-file .file-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 转发前缀 */
.forward-prefix {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 6px;
  line-height: 1.4;
  background: #D0F2D6;
  color: #666;
  max-width: 100%;
  white-space: normal;
  word-break: break-all;
  align-self: flex-start;
}

.message-self .forward-prefix {
  align-self: flex-end;
  background: #60C3FF;
  color: rgba(255, 255, 255, 0.95);
}

/* 图片消息 */
.message-image .talk-image {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
}

.message-image {
  display: inline-block;
  width: 200px;
}

.message-self .message-image {
  margin-left: auto;
}

/* 文件消息 */
.file-card-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 8px;
}

.file-icon {
  font-size: 24px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.file-name {
  text-align: left;
  font-size: 14px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: normal;
  line-height: 1.4;
}

.file-size {
  text-align: left;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.file-delete-btn {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ff4d4f;
  color: #fff;
  text-align: center;
  line-height: 20px;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.chat-file-card {
  margin: 0;
  cursor: default;
  max-width: 300px;
}

.message-self .chat-file-card {
  background: #e8f3ff;
  margin-left: auto;
}

.file-text-wrap {
  max-width: 500px;
}

.message-self .file-text-wrap {
  margin-left: auto;
}

/* 视频消息 */
.video-msg-wrap {
  display: flex;
  flex-direction: column;
  max-width: 300px;
}

.self-wrap.video-msg-wrap {
  align-items: flex-end;
}

.talk-video {
  outline: none;
  background-color: #000;
  max-width: 300px;
  max-height: 200px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

/* 语音消息 */
.voice-msg-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  max-width: 200px;
  min-width: 80px;
}

.message-self .voice-msg-wrap {
  background-color: #409eff;
  color: white;
  margin-left: auto;
}

.voice-play-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.voice-wave {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 16px;
}

.voice-wave span {
  width: 2px;
  background-color: currentColor;
  animation: voice-wave 0.5s infinite ease-in-out alternate;
}

.voice-wave span:nth-child(1) { height: 4px; animation-delay: 0s; }
.voice-wave span:nth-child(2) { height: 8px; animation-delay: 0.2s; }
.voice-wave span:nth-child(3) { height: 12px; animation-delay: 0.4s; }

@keyframes voice-wave {
  from { transform: scaleY(0.5); }
  to { transform: scaleY(1); }
}

.voice-duration {
  font-size: 14px;
  margin-left: auto;
}

/* 消息时间和状态 */
.message-time-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  width: 100%;
}

.message-self .message-time-row {
  justify-content: flex-end;
}

.message-item:not(.message-self) .message-time-row {
  justify-content: flex-start;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-time.time-self {
  text-align: right;
}

/* 重新编辑链接 */
.reedit-link {
  color: gainsboro;
  margin-left: 8px;
  cursor: pointer;
}

/* 空聊天界面 */
.empty-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-chat p {
  margin-top: 16px;
  font-size: 16px;
}

/* 批量消息间距 */
.message-image + .message-image,
.video-msg-wrap + .video-msg-wrap,
.chat-file-card + .chat-file-card {
  margin-top: 8px;
}

/* ======================================
   5. 聊天输入区样式
======================================== */
.chat-input-area {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  border-top: 1px solid var(--glass-border);
  gap: 8px;
}

/* 引用预览 */
.quote-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #409eff;
  width: 100%;
  box-sizing: border-box;
}

.quote-preview-content {
  flex: 1;
  overflow: hidden;
}

.quote-preview-title {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
  margin-bottom: 2px;
}

.quote-preview-text {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quote-close {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
  flex-shrink: 0;
}

.quote-close:hover {
  color: #666;
}

/* 媒体预览通用 */
.media-preview {
  margin-bottom: 8px;
}

/* 图片预览 */
.chat-image-preview .image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chat-image-preview .image-item {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  border: 1px solid #eee;
}

/* 文件预览 */
.chat-file-preview {
  padding: 0 10px;
}

.file-preview-card {
  margin: 0;
  cursor: default;
  max-width: 300px;
}

/* 视频预览 */
.chat-video-preview .video-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.chat-video-preview .video-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ddd;
  background: #000;
}

/* 语音预览 */
.chat-voice-preview .voice-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.voice-preview-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  min-width: 120px;
}

.voice-preview-play {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  background: #e6f7ff;
  border-radius: 50%;
}

.voice-preview-duration {
  font-size: 14px;
  color: #666;
}

/* 输入工具栏 */
.input-tools-bar,
.ai-all-in-one-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 4px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tool-icon {
  width: 24px;
  height: 24px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  transition: opacity 0.2s;
}

.tool-icon:hover {
  opacity: 0.8;
}

.tool-icon-active {
  color: #409eff !important;
  background: rgba(64, 158, 255, 0.1) !important;
  border-radius: 4px;
  padding: 2px;
}

.image-mode-tip {
  font-size: 12px;
  color: #409eff;
  margin-left: 8px;
  background: rgba(64, 158, 255, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
}

/* 输入行 */
.input-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.input-row .el-textarea {
  flex: 1;
}

.input-row .el-textarea :deep(.el-textarea__inner) {
  border-radius: 18px;
  padding: 10px 16px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  border: 1px solid #dcdfe6;
}

.input-row .el-button {
  flex-shrink: 0;
  margin-bottom: 0;
  border-radius: 18px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
}

.input-row .el-button:disabled {
  background-color: #c9cdd4;
  border-color: #c9cdd4;
  color: #fff;
  cursor: not-allowed;
}

/* 表情包面板 - 磨砂玻璃核心适配 */
.emoji-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: 8px;
  margin-top: 8px;
  width: 360px;
  overflow: hidden;
  /* 应用磨砂玻璃样式 */
  @extend .glass-effect;

  /* 适配深色模式下的面板基础样式 */
  :deep(.dark-mode) & {
    @extend :deep(.dark-mode) .glass-effect;
  }
}

/* 表情包标签栏 - 磨砂适配（移除原有背景，继承面板磨砂） */
.emoji-tabs {
  --el-tabs-header-height: 36px;
  border: none;

  .el-tabs__header {
    margin: 0;
    background: transparent !important; /* 清空原有灰色背景 */
  }

  .el-tabs__nav-wrap {
    padding: 0 16px !important;
    background: transparent !important; /* 清空原有灰色背景 */
  }

  .el-tabs__nav {
    height: 36px;
    line-height: 36px;
  }

  .el-tabs__item {
    font-size: 13px;
    height: 36px;
    line-height: 36px;
    /* 标签选中态适配磨砂 */
    &.is-active {
      color: var(--el-color-primary);
      /* 深色模式标签选中态 */
      :deep(.dark-mode) & {
        color: var(--el-color-primary-light-3);
      }
    }
  }
}

/* 表情包滚动容器 */
.emoji-scrollbar {
  height: 168px;
  width: 100%;
}

/* 表情包列表 */
.emoji-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  width: fit-content;
}

/* 表情包项 - 磨砂hover适配 */
.emoji-item {
  font-size: 22px;
  cursor: pointer;
  padding: 6px;
  border-radius: 4px;
  transition: background 0.2s;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 基础hover适配 */
  &:hover {
    background: rgba(255, 255, 255, 0.2) !important;
  }
  /* 深色模式下hover */
  :deep(.dark-mode) &:hover {
    background: rgba(255, 255, 255, 0.1) !important;
  }
}

/* ======================================
   6. 用户搜索界面样式
======================================== */
.user-search-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  border-radius: 8px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-result-item {
  display: flex;
  align-items: center;
  padding: 16px;
  /* border: 1px dashed #eee; */
  border: 1px dashed var(--glass-border);
  border-radius: 8px;
  transition: background-color 0.2s;
  /* background-color: #fafafa; */
  background-color: rgba(255, 255, 255, 0.4); /* 改为半透明白 */
}

.user-result-item:hover {
  background-color: rgba(255, 255, 255, 0.6);
}

.user-info {
  flex: 1;
  margin-left: 12px;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-id {
  font-size: 12px;
  color: #999;
}

.empty-search {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-search p {
  margin-top: 16px;
  font-size: 16px;
}

/* 好友分享弹窗 */
.share-friend-select .mention-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.share-friend-select .mention-item:hover,
.share-friend-select .mention-item.active {
  background-color: #f5f7fa;
}

.share-friend-select .mention-user-name {
  font-size: 14px;
  font-weight: 500;
}

.share-friend-select .mention-user-id {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.share-friend-select .mention-no-result {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

/* ======================================
   7. AI专属样式
======================================== */
/* AI思考动画 */
.thinking-text {
  display: flex;
  align-items: center;
}

.typing-dots {
  animation: blink 1.4s infinite both;
}

@keyframes blink {
  0%, 60%, 100% { opacity: 0; }
  20% { opacity: 1; }
}

/* ======================================
   8. 消息入场动画
======================================== */
.el-zoom-in-enter-active {
  transition: opacity 0.25s ease, transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.el-zoom-in-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.el-zoom-in-enter-from,
.el-zoom-in-leave-to {
  opacity: 0;
  transform: scale(0.85);
}

.message-item:not(.message-self).el-zoom-in-enter-from {
  transform: translateX(-30px) scale(0.85);
}

.message-item.message-self.el-zoom-in-enter-from {
  transform: translateX(30px) scale(0.85);
}

.message-item[data-new="false"].el-zoom-in-enter-active {
  transition: none !important;
}

.message-item[data-new="false"].el-zoom-in-enter-from {
  opacity: 1 !important;
  transform: translateX(0) scale(1) !important;
}

.el-zoom-in-left-enter-active {
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.el-zoom-in-left-enter-from {
  opacity: 0;
  transform: translateX(-30px) scale(0.85);
}

.el-zoom-in-left-leave-active {
  transition: all 0.2s ease;
}

.el-zoom-in-left-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.85);
}

/* ======================================
   9. 移动端响应式适配（max-width:768px）
======================================== */
@media (max-width: 768px) {
  /* 全局容器重置 */
  .el-row {
    margin: 0 !important;
    width: 100% !important;
  }

  .chat-window-col {
    width: 100% !important;
    flex: 0 0 100% !important;
    max-width: 100% !important;
    padding: 0 !important;
  }

  .chat-container {
    padding: 0 !important;
    height: calc(100vh - 60px) !important;
  }

  /* 🔥 新增：移动端高度铺满，消灭底部间隙 */
  .chat-main-row {
    height: 100% !important;
  }

  .chat-window {
    height: 100% !important;
  }

  /* 输入区增加底部安全距离（适配全面屏 iPhone 的底部横条） */
  .chat-input-area {
    padding: 8px 12px !important;
    gap: 6px !important;
    padding-bottom: calc(8px + env(safe-area-inset-bottom)) !important;
  }

  /* 会话列表 */
  .session-list-col {
    border-right: none !important;
  }

  .session-item {
    padding: 16px 12px !important;
  }

  .session-name {
    font-size: 15px !important;
  }

  .session-last-message {
    font-size: 12px !important;
  }

  .session-time {
    font-size: 11px !important;
  }

  .ElAvatar {
    --el-avatar-size: 44px !important;
  }

  /* 聊天窗口 */
  .chat-header {
    padding: 12px 16px !important;
  }

  .chat-name {
    font-size: 16px !important;
  }

  .chat-messages {
    padding: 12px 8px !important;
  }

  .message-item {
    margin-bottom: 12px !important;
    gap: 8px !important;
  }

  .message-item .ElAvatar {
    --el-avatar-size: 30px !important;
    font-size: 12px !important;
  }

  .message-text {
    padding: 8px 12px !important;
    font-size: 14px !important;
    max-width: 100% !important;
  }

  .message-time {
    font-size: 11px !important;
  }

  .message-image .talk-image {
    max-width: 160px !important;
  }

  /* 输入区 */
  .chat-input-area {
    padding: 8px 12px !important;
    gap: 6px !important;
  }

  .input-tools-bar {
    gap: 12px !important;
  }

  .tool-icon {
    width: 22px !important;
    height: 22px !important;
  }

  .input-row .el-textarea :deep(.el-textarea__inner) {
    padding: 8px 14px !important;
    font-size: 14px !important;
    border-radius: 16px !important;
  }

  .input-row .el-button {
    padding: 8px 16px !important;
    font-size: 13px !important;
    border-radius: 16px !important;
  }

  /* 表情包面板 */
  .emoji-panel {
    width: 100% !important;
    margin: 8px 0 !important;
  }

  .emoji-item {
    font-size: 20px !important;
    width: 32px !important;
    height: 32px !important;
  }

  /* 搜索界面 */
  .user-search-container {
    padding: 16px 12px !important;
  }

  .search-bar {
    margin-bottom: 16px !important;
    padding-bottom: 12px !important;
  }

  .user-result-item {
    padding: 12px !important;
  }

  /* 手机端搜索好友 */
  .mobile-search-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: 16px 12px !important;
  }

  .mobile-search-container .search-bar {
    flex-wrap: wrap;
    gap: 8px;
  }

  .mobile-search-container .search-bar .el-select,
  .mobile-search-container .search-bar .el-input,
  .mobile-search-container .search-bar .el-button {
    width: 100% !important;
    margin: 0 !important;
  }

  /* 空状态 */
  .empty-chat, .empty-search, .empty-list {
    font-size: 14px !important;
  }

  .empty-chat .el-icon, .empty-search .el-icon {
    font-size: 48px !important;
  }

  /* 隐藏右键菜单（手机端用长按） */
  .context-menu {
    display: none !important;
  }
}

/* ======================================
   10. 统一暗黑模式适配
======================================== */
:deep(.dark-mode) {
  /* 右键菜单 */
  .context-menu {
    background: #1f2937;
    color: #e5e7eb;
  }

  .context-menu-item:hover {
    background-color: #374151;
  }

  .context-menu-item.danger:hover {
    background-color: #450a0a;
  }

  /* 好友分享弹窗 */
  .share-friend-select .mention-item:hover,
  .share-friend-select .mention-item.active {
    background-color: #374151;
  }

  /* 聊天背景 */
  .chat-messages {
  }

  /* 消息气泡 */
  .message-text {
    background-color: #374151;
    color: #e5e7eb;
  }

  /* 引用消息 */
  .quote-message {
    background-color: #374151;
    border-left-color: #4ade80;
    color: #d1d5db;
  }

  .message-self .quote-message {
    background-color: #1e3a5f;
    border-left-color: #60a5fa;
  }

  /* 转发前缀 */
  .forward-prefix {
    background: #374151;
    color: #d1d5db;
  }

  .message-self .forward-prefix {
    background: #1e3a5f;
    color: rgba(255, 255, 255, 0.95);
  }

  /* 文件卡片 */
  .chat-file-card {
    background-color: #374151;
  }

  .message-self .chat-file-card {
    background-color: #1e3a5f;
  }

  .file-icon {
    background: #4b5563;
  }

  .file-name {
    color: #e5e7eb;
  }

  /* 语音消息 */
  .voice-msg-wrap {
    background-color: #374151;
    color: #e5e7eb;
  }

  .message-self .voice-msg-wrap {
    background-color: #1e3a5f;
  }

  /* 输入区 */
  .chat-input-area {
    border-top-color: rgba(255, 255, 255, 0.1);
  }

  .quote-preview {
    background-color: #374151;
  }

  /* 表情包面板 */
  .emoji-panel {
    background: #1f2937;
    border-color: #374151;
  }

  .emoji-tabs .el-tabs__header {
    background: #374151;
  }

  .emoji-item:hover {
    background: #4b5563;
  }

  /* 搜索界面 */
  .search-results {
  }

  .user-result-item {
    background-color: rgba(0, 0, 0, 0.2); /* 保持半透明黑 */
    border-color: rgba(255, 255, 255, 0.1);
  }

  .user-name {
    color: #e5e7eb;
  }

  /* 空状态 */
  .empty-chat, .empty-search, .empty-list {
    color: #9ca3af;
  }
}

/* ======================================
   11. 覆盖 ElTabs 实色背景，让毛玻璃渗透
======================================== */
:deep(.el-tabs--border-card) {
  background: transparent !important;
  border: none !important;
}
:deep(.el-tabs--border-card > .el-tabs__header) {
  background: transparent !important;
  border-bottom: 1px solid var(--glass-border) !important;
}
:deep(.el-tabs--border-card > .el-tabs__content) {
  background: transparent !important;
}

.marquee-ctrl-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 30;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.4);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
  opacity: 0;
  pointer-events: none;
}
.is-dark .marquee-ctrl-btn {
  background: rgba(255, 255, 255, 0.2);
}
.marquee-ctrl-btn:hover {
  background: #409eff;
  color: #fff;
  scale: 1.1;
}

/* ====================== 横向滚动容器 (番茄同款) ====================== */
.multi-card-slider-wrapper {
  display: flex !important;
  flex-direction: row !important; /* 强制横向排列 */
  align-items: center;
  position: relative;
  width: 100%;
  max-width: 460px;
  /* 新增：扩大hover感应区域，避免鼠标移出箭头瞬间闪烁 */
  padding: 0 16px;
  box-sizing: border-box;
}

.multi-card-scroll-view {
  display: flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  flex: 1;
  padding: 4px 0;
  width: 100% !important;
  min-height: 130px; /* 防止高度塌陷 */
  /* 隐藏滚动条 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.multi-card-scroll-view::-webkit-scrollbar {
  display: none;
}

/* 左右控制箭头 ========== 【主要修改区域】 ========== */
.slider-arrow {
  position: absolute;
  z-index: 10;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;

  /* ========== 新增：默认隐藏箭头 ========== */
  opacity: 0;
  pointer-events: none;
}

/* ========== 新增：悬浮外层容器时，显示箭头 + 允许点击 ========== */
.multi-card-slider-wrapper:hover .slider-arrow {
  opacity: 1;
  pointer-events: auto;
}

/* 箭头自身hover效果（原有逻辑保留） */
.slider-arrow:hover {
  background: #fff;
  color: #1890ff;
  transform: scale(1.1);
}

.slider-arrow.left { left: -14px; }
.slider-arrow.right { right: -14px; }

/* 必须限制子卡片不被拉伸缩水 */
.slider-item {
  display: block !important;
  width: max-content !important;
  flex-shrink: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 强制清除自己发消息时的样式污染 */
:deep(.self-wrapper .simplified-card),
:deep(.self-wrap .simplified-card) {
  margin: 0 !important;
}

/* ====================== 精简版书籍卡片 ====================== */
:deep(.simplified-card) {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 90px !important;
  max-width: 90px !important;
  flex-shrink: 0 !important;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  cursor: pointer;
}
:deep(.simplified-card:hover) {
  transform: translateY(-2px);
}

:deep(.simplified-cover-wrapper) {
  position: relative;
  width: 90px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

:deep(.simplified-cover-wrapper .book-share-card-cover) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

:deep(.simplified-cover-wrapper .badge) {
  position: absolute;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #ff9500;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 6px;
  border-top-right-radius: 6px;
}

:deep(.simplified-card .book-title) {
  width: 100%;
  font-size: 13px;
  color: #333;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 🔥 新增：PC端聊天容器高度控制 */
.chat-main-row {
  height: calc(100vh - 120px);
  overflow: hidden;
}

/* 移动端：屏幕宽度 ≤ 768px 时，箭头永久显示 */
@media (max-width: 768px) {
  .slider-arrow {
    opacity: 1 !important;
    pointer-events: auto !important;
  }
}

</style>