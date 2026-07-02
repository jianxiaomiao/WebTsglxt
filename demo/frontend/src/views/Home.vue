<template>
  <!-- 整体页面滚动：提升整体层级，避免被其他组件遮挡 -->
  <div class="home-page-wrapper" style="padding: 15px; overflow-y: auto; min-height: calc(100vh - 190px); position: relative; z-index: 10;">
    <!-- ====================== ✅ 骨架屏包裹：书籍展示区域 ====================== -->
    <el-skeleton
        :loading="isInitialLoading"
        animated
        style="width: 100%; padding: 10px;"
    >
      <template #template>
        <div>
          <el-skeleton-item variant="text" style="width: 120px; height: 24px; margin-bottom: 15px;" />
          <el-row :gutter="15" style="margin-bottom: 20px;">
            <el-col :span="4" :xs="8" v-for="i in 8" :key="i" style="margin-bottom: 20px;">
              <div style="padding: 10px; border-radius: 8px; height: 100%;">
                <el-skeleton-item variant="rect" style="width: 100%; height: 160px; border-radius: 4px;" />
                <el-skeleton-item variant="text" style="width: 80%; margin: 10px auto 0;" />
                <el-skeleton-item variant="text" style="width: 60%; margin: 5px auto 0;" />
              </div>
            </el-col>
          </el-row>
        </div>
      </template>

      <template #default>
        <!-- ✨✨ 全新便当盒(Bento)主容器 ✨✨ -->
        <div class="bento-dashboard">

          <!-- ====================== 🔍 模块1：搜索结果区域 ====================== -->
          <div v-if="isSearching" class="search-result-section glass-panel bento-item search-bento">
            <h2 :style="{ fontSize: '18px', fontWeight: 'bold', marginBottom: '10px', color: isDark ? '#e5e7eb' : '#333' }">🔍 搜索结果</h2>
            <el-divider content-position="left" :style="{ borderColor: isDark ? '#374151' : '#eee', borderWidth: '2px', margin: '0 0 15px 0' }"></el-divider>
            <ElRow :gutter="15">
              <ElCol :span="12" :xs="24" v-for="book in books" :key="book.ISBN" style="margin-bottom: 15px;">
                <ElCard
                    shadow="hover"
                    @contextmenu.prevent.stop="handleBookContextMenu($event, book)"
                    :body-style="{
                  width: '100%',
                  height: '220px', /* 🔻 高度大幅压缩 */
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  gap: '15px',
                  padding: '15px',
                  boxSizing: 'border-box',
                  overflow: 'hidden',
                  borderRadius: '12px',
                }"
                >
                  <div style="display: flex; flex-direction: row; width: 100%; height: 100%; gap: 15px;">
                    <div style="display: flex; flex-direction: column; align-items: center; gap: 8px; width: 140px; flex-shrink: 0;">
                      <ElImage
                          :src="book.pictureName"
                          fit="cover"
                          style="width: 110px; height: 150px; cursor: pointer; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);"
                          fallback-src="/default-book.png"
                          @click="gotoBookDetail(book.iSBN)"
                          lazy
                      />
                      <h3 :style="{ fontSize: '14px', fontWeight: 600, margin: 0, textAlign: 'center', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', width: '100%', color: isDark ? '#e5e7eb' : '#333' }" v-html="highlightBookName(book.bookname)"></h3>
                      <div class="book-info-item" style="display: flex; align-items: center; width: 100%; gap: 4px;">
                        <span style="width: 45px; text-align: left; font-size: 12px; color: #666; flex-shrink: 0;">作者：</span>
                        <span style="font-size: 12px; color: #888; white-space: nowrap; overflow: hidden; textOverflow: ellipsis;">{{ book.author || '未知' }}</span>
                      </div>
                    </div>
                    <div style="flex: 1; height: 100%; display: flex; align-items: flex-start; padding-top: 5px;">
                      <div :style="{ width: '100%', maxHeight: '180px', overflowY: 'auto', paddingRight: '8px', fontSize: '13px', color: isDark ? '#d1d5db' : '#555', lineHeight: 1.6, textAlign: 'justify' }">
                        {{ book.information || '暂无书籍简介' }}
                      </div>
                    </div>
                  </div>
                </ElCard>
              </ElCol>
              <div v-if="books.length === 0" :style="{ width: '100%', textAlign: 'center', padding: '30px 0', color: isDark ? '#9ca3af' : '#999' }">未找到匹配的书籍，换个关键词试试吧~</div>
            </ElRow>
          </div>

          <!-- ====================== 🔥 模块2：热门书籍 (长格) ====================== -->
          <div id="guide-hot-books" class="hot-section glass-panel bento-item hot-bento">
            <h2 :style="{ fontSize: '16px', fontWeight: 'bold', marginBottom: '10px', color: isDark ? '#e5e7eb' : '#333', display: 'flex', alignItems: 'center'}">🔥 热门书籍</h2>
            <el-divider content-position="left" :style="{ borderColor: isDark ? '#374151' : '#eee', borderWidth: '2px', marginTop: '0', marginBottom: '10px' }"></el-divider>

            <div class="hot-marquee-container hot-container-wrapper" style="flex: 1; display: flex; flex-direction: column; justify-content: center; padding: 5px 0;">
              <button class="marquee-ctrl-btn btn-left" @click="changeDirection('left')"><el-icon><DArrowLeft /></el-icon></button>
              <button class="marquee-ctrl-btn btn-right" @click="changeDirection('right')"><el-icon><DArrowRight /></el-icon></button>

              <div class="hot-marquee-track row-1" :class="{ 'paused': isMarqueePaused1, 'reverse-direction': isReverse }" @mouseenter="isMarqueePaused1 = true" @mouseleave="isMarqueePaused1 = false" style="margin-bottom: 12px;">
                <div class="hot-marquee-group">
                  <div v-for="(book, index) in hotBooksRow1" :key="'r1-g1-' + book.ISBN + '-' + index" class="hot-book-item" @contextmenu.prevent.stop="handleBookContextMenu($event, book)">
                    <div class="shelf-book-card-3d-wrapper-home">
                      <div class="book-3d-entity-home">
                        <div class="book-spine-3d"></div>
                        <div class="book-pages-3d"></div>
                        <div class="book-cover-3d" @click="gotoBookDetail(book.iSBN)">
                          <ElImage :src="book.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" lazy />
                          <div class="book-cover-shading"></div>
                        </div>
                        <div class="book-inside-reveal-home" @click="gotoBookDetail(book.iSBN)">
                          <div class="inside-content">
                            <h3 class="book-name-3d">{{ book.bookname }}</h3>
                            <div class="book-detail-body"><p class="book-intro-3d">{{ book.information || '探索文学之美...' }}</p></div>
                            <div class="book-read-btn-hint">点击查看 📖</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="hot-marquee-group">
                  <div v-for="(book, index) in hotBooksRow1" :key="'r1-g2-' + book.ISBN + '-' + index" class="hot-book-item" @contextmenu.prevent.stop="handleBookContextMenu($event, book)">
                    <div class="shelf-book-card-3d-wrapper-home">
                      <div class="book-3d-entity-home">
                        <div class="book-spine-3d"></div>
                        <div class="book-pages-3d"></div>
                        <div class="book-cover-3d" @click="gotoBookDetail(book.iSBN)">
                          <ElImage :src="book.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" lazy />
                          <div class="book-cover-shading"></div>
                        </div>
                        <div class="book-inside-reveal-home" @click="gotoBookDetail(book.iSBN)">
                          <div class="inside-content">
                            <h3 class="book-name-3d">{{ book.bookname }}</h3>
                            <div class="book-detail-body"><p class="book-intro-3d">{{ book.information || '探索文学之美...' }}</p></div>
                            <div class="book-read-btn-hint">点击查看 📖</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="hot-marquee-track row-2" :class="{ 'paused': isMarqueePaused2, 'reverse-direction': isReverse }" @mouseenter="isMarqueePaused2 = true" @mouseleave="isMarqueePaused2 = false">
                <div class="hot-marquee-group">
                  <div v-for="(book, index) in hotBooksRow2" :key="'r2-g1-' + book.ISBN + '-' + index" class="hot-book-item" @contextmenu.prevent.stop="handleBookContextMenu($event, book)">
                    <div class="shelf-book-card-3d-wrapper-home">
                      <div class="book-3d-entity-home">
                        <div class="book-spine-3d"></div>
                        <div class="book-pages-3d"></div>
                        <div class="book-cover-3d" @click="gotoBookDetail(book.iSBN)">
                          <ElImage :src="book.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" lazy />
                          <div class="book-cover-shading"></div>
                        </div>
                        <div class="book-inside-reveal-home" @click="gotoBookDetail(book.iSBN)">
                          <div class="inside-content">
                            <h3 class="book-name-3d">{{ book.bookname }}</h3>
                            <div class="book-detail-body"><p class="book-intro-3d">{{ book.information || '探索文学之美...' }}</p></div>
                            <div class="book-read-btn-hint">点击查看 📖</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="hot-marquee-group">
                  <div v-for="(book, index) in hotBooksRow2" :key="'r2-g2-' + book.ISBN + '-' + index" class="hot-book-item" @contextmenu.prevent.stop="handleBookContextMenu($event, book)">
                    <div class="shelf-book-card-3d-wrapper-home">
                      <div class="book-3d-entity-home">
                        <div class="book-spine-3d"></div>
                        <div class="book-pages-3d"></div>
                        <div class="book-cover-3d" @click="gotoBookDetail(book.iSBN)">
                          <ElImage :src="book.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" lazy />
                          <div class="book-cover-shading"></div>
                        </div>
                        <div class="book-inside-reveal-home" @click="gotoBookDetail(book.iSBN)">
                          <div class="inside-content">
                            <h3 class="book-name-3d">{{ book.bookname }}</h3>
                            <div class="book-detail-body"><p class="book-intro-3d">{{ book.information || '探索文学之美...' }}</p></div>
                            <div class="book-read-btn-hint">点击查看 📖</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- ====================== 🎨 模块3：灵宝日记 (原生16:9适配) ====================== -->
          <div id="guide-daily-pic" class="ai-pic-section glass-panel bento-item ai-bento" v-loading="loadingAiPic">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <h2 :style="{ fontSize: '16px', fontWeight: 'bold', margin: 0, color: isDark ? '#e5e7eb' : '#333', display: 'flex', alignItems: 'center', gap: '8px'}">
                <span>🎨 每日一图</span>
              </h2>
              <div class="ai-pic-nav" :class="{ 'dark-nav': isDark }">
                <el-button circle size="small" :icon="ArrowLeft" @click="getPrevPic" :disabled="!hasPrevPic || loadingAiPic" class="nav-arrow-btn" />
                <span class="nav-date-text" style="font-size: 11px;">{{ displayAiDate }}</span>
                <el-button circle size="small" :icon="ArrowRight" @click="getNextPic" :disabled="isToday || loadingAiPic" class="nav-arrow-btn" />
              </div>
            </div>
            <el-divider content-position="left" :style="{ borderColor: isDark ? '#374151' : '#eee', borderWidth: '2px', marginTop: 0, marginBottom: '15px' }"></el-divider>

            <!-- 🔻 核心修改：利用 aspect-ratio 死锁 16:9 比例，不乱撑高度 -->
            <div class="ai-pic-content-wrapper">
              <ElImage
                  v-if="aiPicData && aiPicData.imgUrl"
                  :src="IMAGE_BASE_URL + aiPicData.imgUrl"
                  fit="contain"
                  class="ai-pic-img"
                  :preview-src-list="[IMAGE_BASE_URL + aiPicData.imgUrl]"
                  lazy
              />
              <div v-else class="ai-pic-empty" :class="{ 'dark-empty': isDark }">
                <div class="empty-icon-wrap">
                  <span class="empty-emoji">📝</span>
                </div>
                <p class="empty-text">ai正在休息哦~</p>
                <p class="empty-subtext">明天来看看吧</p>
              </div>
            </div>
          </div>

          <!-- ====================== 🔮 模块4：塔罗牌推荐 (强锁双列排布) ====================== -->
          <div id="guide-tarot" class="tarot-recommend-section glass-panel bento-item tarot-bento">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; flex-wrap: wrap; gap: 8px;">
              <h3 style="font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 6px; margin: 0;">
                <span :style="{ color: isDark ? '#d4af37' : '#7A9B8C', filter: isDark ? 'drop-shadow(0 0 4px rgba(212,175,55,0.6))' : 'drop-shadow(0 2px 4px rgba(173,198,176,0.6))' }">
                  {{ isDark ? '🔮' : '🌿' }}
                </span>
                <span :style="{ fontFamily: 'Georgia, serif', letterSpacing: '1px', color: isDark ? '#e5e7eb' : '#5A7A68' }">命运之书</span>
              </h3>

              <el-button class="tarot-refresh-btn" :class="isDark ? 'btn-dark' : 'btn-light'" type="primary" plain :loading="loadingRecommend" @click="handleChangeBatch" size="small" style="padding: 4px 8px; height: 26px;">
                <template #icon><el-icon><Refresh /></el-icon></template>新启示
              </el-button>
            </div>

            <!-- 🔻 核心修改：双列容器，高度压缩 -->
            <div class="tarot-grid bento-tarot-scroll">
              <div
                  v-for="(book, index) in displayRecommendBooks"
                  :key="book.id || index"
                  class="tarot-card-wrapper"
                  :class="isDark ? 'is-dark-hover' : 'is-light-hover'"
                  @click="handleTarotClick(book)"
                  @contextmenu.prevent.stop="handleBookContextMenu($event, book)"
              >
                <div class="tarot-card-inner" :class="{ 'is-flipped': book.isFlipped }">
                  <!-- 🃏 牌背 -->
                  <div class="tarot-card-back" :class="isDark ? 'tarot-back-dark' : 'tarot-back-light'">
                    <div class="tarot-border-line"></div>
                    <div class="tarot-center-pattern">
                      <div class="mystic-circle" :class="isDark ? 'dark-circle' : 'light-circle'">
                        <template v-if="!isDark">
                          <span class="spin-leaf leaf-1">🍃</span>
                          <span class="spin-leaf leaf-2">🍃</span>
                          <span class="spin-leaf leaf-3">🍃</span>
                        </template>
                      </div>
                      <div class="mystic-icon light-tree" v-if="!isDark">🌳</div>
                      <el-icon class="mystic-icon dark-star" v-else><StarFilled /></el-icon>
                    </div>
                    <div class="tarot-text">DESTINY</div>
                    <div class="tarot-hint">点击抽取</div>
                  </div>

                  <!-- 🃏 牌面 -->
                  <div class="tarot-card-front" :class="isDark ? 'tarot-front-dark' : 'tarot-front-light'">
                    <div class="book-card-content">
                      <div class="tarot-book-cover-wrap">
                        <img :src="book.pictureName || 'https://via.placeholder.com/150x210?text=No+Cover'" class="tarot-book-cover" alt="cover" />
                      </div>
                      <div class="tarot-book-info">
                        <h4 class="tarot-book-title" :title="book.bookname">{{ book.bookname || '神秘文献' }}</h4>
                        <p class="tarot-book-author">{{ book.author || '佚名' }}</p>
                        <div class="tarot-tags-container">
                          <el-tag size="small" effect="dark" class="tarot-book-tag" :color="isDark ? '#d4af37' : '#8CAFA0'" :style="{ borderColor: isDark ? '#d4af37' : '#8CAFA0' }">{{ book.bookTypeName || '命运' }}</el-tag>
                          <el-tag v-for="(tag, tagIdx) in book.tagNames" :key="tagIdx" size="small" effect="plain" type="info" class="tarot-sub-tag">{{ tag }}</el-tag>
                        </div>
                        <p class="tarot-book-intro">{{ book.information || '等待你的翻阅...' }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="!loadingRecommend && recommendBooks.length === 0" description="暂无推荐书籍" :image-size="60" />
          </div>

          <!-- ====================== 🌌 模块5：星系探索模式 (压缩高度) ====================== -->
          <div id="guide-universe" class="type-section glass-panel bento-item universe-bento" :class="{ 'dark-theme': isDark }" style="overflow: hidden;">
            <div class="section-header" style="display: flex; flex-direction: column; margin-bottom: 10px; gap: 10px;">
              <div style="display: flex; justify-content: space-between; align-items: center; width: 100%; flex-wrap: wrap; gap: 8px;">
                <h2 :style="{ fontSize: '16px', fontWeight: 'bold', margin: 0, color: isDark ? '#e5e7eb' : '#5A7A68' }">
                  {{ isDark ? '🌌' : '☁️' }} {{ currentView === 'universe'
                    ? (isDark ? '星图坐标' : '思绪云境')
                    : `云域 · ${selectedTypeName}` }}
                </h2>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <el-button v-if="currentView === 'books'" :type="isDark ? 'primary' : 'success'" plain @click="loadTypeBooks" :disabled="!hasMoreType" size="small" style="padding: 4px 8px; height: 26px;">
                    {{ isDark ? '星域跃迁' : '云境漫游' }}
                  </el-button>
                  <el-button v-if="currentView === 'books'" :type="isDark ? 'primary' : 'success'" plain :icon="Back" circle @click="backToUniverse" class="back-universe-btn" size="small"/>
                </div>
              </div>

              <div class="control-console" :class="isDark ? 'dark-console' : 'light-console'" style="width: 100%; padding: 8px 12px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                  <span :style="{ fontWeight: 'bold', color: isDark ? '#d1d5db' : '#6B8E7D', fontSize: '12px' }">
                    {{ isDark ? '🛰️ 坐标控制台' : '☁️ 导航控制台' }}
                  </span>
                  <el-button :type="isDark ? 'primary' : 'success'" link @click="isCategoryExpanded = !isCategoryExpanded" style="font-size: 12px;">
                    {{ isCategoryExpanded ? '收起 ▲' : '展开 ▼' }}
                  </el-button>
                </div>

                <transition name="el-zoom-in-top">
                  <div v-show="isCategoryExpanded" style="padding-top: 8px;">
                    <ElRadioGroup v-model="selectedTypeId" @change="handleSelectType" class="four-grid-group">
                      <ElRadio v-for="type in bookTypeList" :key="type.id" :label="type.id" class="grid-radio-item" :class="{ dark: isDark }">
                        {{ type.typeName }}
                      </ElRadio>
                    </ElRadioGroup>
                  </div>
                </transition>
              </div>
            </div>

            <el-divider content-position="left" :style="{ borderColor: isDark ? '#374151' : '#eee', borderWidth: '2px', margin: '0 0 10px 0' }"></el-divider>

            <div v-show="currentView === 'universe' || isTransitioning" class="universe-stage" :class="{ 'space-explosion': isExploding, 'dark-theme': isDark, 'light-theme': !isDark }" style="flex: 1; border-radius: 8px;">
              <div v-if="!isDark" class="day-sky-scenery">
                <div class="sky-sun"></div>
                <div class="sky-clouds-bg"></div>
              </div>

              <div class="floating-stars-layer">
                <div v-for="star in floatingStars" :key="'star-' + star.id"
                     :class="isDark ? 'float-star-item' : 'float-leaf-item'"
                     :style="{
                       left: `${star.x}%`,
                       top: `${star.y}%`,
                       fontSize: isDark ? `${star.size}px` : `${star.size * 1.2}px`,
                       animationDuration: `${star.duration}s`,
                       animationDelay: `${star.delay}s`
                     }">
                  {{ isDark ? '⭐' : '🍃' }}
                </div>
              </div>

              <div class="galaxy-grid">
                <div v-for="planet in planetPhysics" :key="planet.id" class="planet-system"
                     :style="{
             translate: `${planet.x}px ${planet.y}px`,
             width: `${planet.width}px`,
             height: `${planet.height}px`,
             zIndex: planet.zIndex,
             transform: `scale(${planet.scale})`,
             '--spin-start': `${planet.spinStart}deg`
           }"
                     @mouseenter="planet.isHovered = true"
                     @mouseleave="planet.isHovered = false"
                     @click="explorePlanet(planet.type)">

                  <div class="nebula-ring" v-if="planet.nebulaConfig.show" :style="{transform: `rotateX(${planet.nebulaConfig.rotateX}deg) rotateY(${planet.nebulaConfig.rotateY}deg)`}"></div>

                  <div class="planet-body" :class="`planet-style-${planet.styleIndex}`">
                    <div class="planet-glow"></div>
                    <span class="planet-name">{{ planet.type.typeName }}</span>
                  </div>

                  <div v-for="(book, idx) in planet.previewBooks.slice(0, 3)" :key="`${planet.id}-sat-${idx}`" class="satellite-orbit" :class="`orbit-${idx + 1}`">
                    <div class="satellite-item">
                      <span class="satellite-text">📖 {{ book.bookname.slice(0, 6) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 📚 书籍列表视图 -->
            <div v-show="currentView === 'books'" class="books-stage" style="flex: 1;">
              <el-scrollbar style="width: 100%; height: 100%; min-height: 350px;">
                <div :style="{
                   width: '100%',
                  display: 'grid',
                  gridTemplateColumns: isMobile ? 'repeat(3, 1fr)' : 'repeat(auto-fill, minmax(160px, 1fr))',
                  gap: isMobile ? '12px' : '15px',
                  padding: '10px',
                  boxSizing: 'border-box',
                  overflow: 'hidden'
                }">
                  <div
                      v-for="(book, idx) in typeBooks"
                      :key="book.ISBN"
                      class="stagger-card-item"
                      @contextmenu.prevent.stop="handleBookContextMenu($event, book)"
                  >
                    <div class="shelf-book-card-3d-wrapper-home" :style="{
                      transform: isMobile ? 'scale(0.7)' : 'scale(1)',
                      margin: '5px auto',
                      width: '100%',
                      boxSizing: 'border-box'
                    }">
                      <div class="book-3d-entity-home">
                        <div class="book-spine-3d"></div>
                        <div class="book-pages-3d"></div>
                        <div class="book-cover-3d" @click="gotoBookDetail(book.iSBN)">
                          <ElImage :src="book.pictureName" fit="cover" class="book-cover-img" fallback-src="/default-book.png" lazy />
                          <div class="book-cover-shading"></div>
                        </div>
                        <div class="book-inside-reveal-home" @click="gotoBookDetail(book.iSBN)">
                          <div class="inside-content">
                            <h3 class="book-name-3d">{{ book.bookname }}</h3>
                            <div class="book-detail-body"><p class="book-intro-3d">{{ book.information || '暂无书籍简介...' }}</p></div>
                            <div class="book-read-btn-hint">点击查看 📖</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="loadingType && hasMoreType" class="load-more-container"><span>穿梭星域加载中...</span></div>
              </el-scrollbar>
            </div>
          </div>
        </div>
      </template>
    </el-skeleton>

    <!-- ====================== 鼠标右键菜单面板 ====================== -->
    <div id="guide-context-menu" v-if="showContextMenu" class="context-menu" :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }" @click.stop>
      <template v-if="currentRightClickBook">
        <div class="context-menu-item" @click="shareBookToFriend"><span>👥</span><span>分享给好友</span></div>
        <div class="context-menu-item" @click="shareBookToForum"><span>📤</span><span>分享书籍到论坛</span></div>
        <div class="context-menu-item" @click="copyBookShareLink"><span>🔗</span><span>复制分享链接</span></div>
        <div class="context-menu-item" @click="handleAiSummarizeBook"><span>🤖</span><span>AI总结书籍</span></div>
        <div class="context-menu-divider"></div>
      </template>

      <div class="context-menu-item" @click="handleCaptureScreen"><span>👓</span><span>识别屏幕内容</span></div>
      <div class="context-menu-item" @click="goToMyNotes"><span>📝</span><span>我的笔记</span></div>
      <div class="context-menu-item" @click="goToMyForumComments"><span>💬</span><span>我的论坛评论</span></div>
      <div class="context-menu-item" @click="goToMyBookComments"><span>⭐</span><span>我的书籍评论</span></div>
      <div class="context-menu-item" @click="goToReadHistory"><span>📚</span><span>阅读历史</span></div>
    </div>
  </div>
</template>

<script setup>
import { inject, computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElMessageBox, ElMessage, ElRow, ElCol, ElCard, ElImage, ElDivider, ElScrollbar, ElIcon, ElRadioGroup, ElRadio } from "element-plus";
import request from '../utils/request.js'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import { Loading, Back, Refresh, StarFilled, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const petStore = usePetStore()

const generateShareContent = inject("generateShareContent")
const shareToForum = inject("shareToForum")
const isDark = inject('isDark')
const hotBooks = inject('hotBooks')
const recommendBooks = inject('recommendBooks')

const displayRecommendBooks = computed(() => {
  return recommendBooks.value || []
})
const books = inject('books')
const searchKey = inject('searchKey')

const loadingRecommend = inject('loadingRecommend')
const recommendPageNum = inject('recommendPageNum')
const recommendPageSize = inject('recommendPageSize')
const hasMoreRecommend = inject('hasMoreRecommend')
const isRecommendFirstLoad = ref(false)
const loadRecommendBooks = inject('loadRecommendBooks')

const showContextMenu = inject('showContextMenu')
const contextMenuPosition = inject('contextMenuPosition')
const currentRightClickBook = inject('currentRightClickBook')
const shareBookToFriend = inject('shareBookToFriend')
const isMobile = inject('isMobile')
const currentUserId = inject('currentUserId')
const currentUserType = inject('currentUserType')

const currentView = ref('universe')
const isExploding = ref(false)
const isTransitioning = ref(false)
const selectedTypeName = ref('')

const isCategoryExpanded = ref(false)

const activeMaxPlanets = computed(() => isMobile.value ? 5 : 15)
const AUTO_SPAWN_INTERVAL = 4000
const PLANET_MIN_SPEED = 0.25
const PLANET_MAX_SPEED = 0.8
const BOUNCE_PROBABILITY = 0.6

const INIT_PLANET_COUNT = 8
let universeContainer = null
let universeWidth = 0
let universeHeight = 0
let universeAnimationId = null
const planetPhysics = ref([])

const floatingStars = ref([])
const initFloatingStars = () => {
  const stars = []
  for (let i = 0; i <20; i++) {
    stars.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: 10 + Math.random() * 12,
      duration: 15 + Math.random() * 25,
      delay: -Math.random() * 40,
    })
  }
  floatingStars.value = stars
}
const allTypeData = ref([])
const typeBookCache = ref({})

const handleBookContextMenu = (e, book) => {
  if (isMobile.value) return
  e.preventDefault()
  e.stopPropagation()

  currentRightClickItem.value = null
  currentRightClickBook.value = book
  itemType.value = 'book'

  const menuWidth = 180
  const menuHeight = 220

  let x = e.clientX
  let y = e.clientY
  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

const createPlanet = () => {
  if (!universeContainer || allTypeData.value.length === 0) return null
  const randomType = allTypeData.value[Math.floor(Math.random() * allTypeData.value.length)]
  const typeId = randomType.id
  const previewBooks = typeBookCache.value[typeId] || []
  const planetSize = 80 + Math.random() * 30 // 微缩星球大小
  const spinStart = Math.random() * 360

  const cx = universeWidth / 2
  const cy = universeHeight / 2

  const maxRadiusX = Math.max(120, cx - 60)
  const minRadiusX = 80
  const orbitRadiusX = minRadiusX + Math.random() * (maxRadiusX - minRadiusX)
  const orbitRadiusY = orbitRadiusX * (0.3 + Math.random() * 0.4)
  const orbitAngle = Math.random() * Math.PI * 2
  const orbitSpeed = (Math.random() * 0.003 + 0.001) * (Math.random() > 0.5 ? 1 : -1)

  const cloudX = Math.random() * universeWidth;
  const cloudY = Math.random() * (universeHeight * 0.5) + 20;
  const cloudSpeed = 0.15 + Math.random() * 0.3;
  const cloudScale = 0.6 + Math.random() * 0.5;

  const nebulaConfig = {
    show: true,
    rotateX: 60 + Math.random() * 30,
    rotateY: -30 + Math.random() * 60
  }

  return {
    id: Date.now() + Math.random(),
    type: randomType,
    width: planetSize, height: planetSize,
    orbitRadiusX, orbitRadiusY, orbitAngle, orbitSpeed,
    cloudX, cloudY, cloudSpeed, cloudScale,
    x: 0, y: 0, scale: 1, zIndex: 10, spinStart, nebulaConfig, previewBooks,
    styleIndex: Math.floor(Math.random() * 10),
    isHovered: false
  }
}

const universeAnimate = () => {
  if (!universeContainer || currentView.value !== 'universe') return

  const cx = universeWidth / 2
  const cy = universeHeight / 2

  for (let i = 0; i < planetPhysics.value.length; i++) {
    const planet = planetPhysics.value[i]

    if (isDark.value) {
      planet.orbitAngle += planet.isHovered ? (planet.orbitSpeed * 0.05) : planet.orbitSpeed
      planet.x = cx + planet.orbitRadiusX * Math.cos(planet.orbitAngle) - planet.width / 2
      planet.y = cy + planet.orbitRadiusY * Math.sin(planet.orbitAngle) - planet.height / 2

      const depth = Math.sin(planet.orbitAngle)
      planet.scale = 0.85 + depth * 0.35
      planet.zIndex = Math.floor(20 + depth * 10)

    } else {
      const currentSpeed = planet.isHovered ? 0 : planet.cloudSpeed;
      planet.cloudX -= currentSpeed;

      if (planet.cloudX < -planet.width * 1.5) {
        planet.cloudX = universeWidth + planet.width;
        planet.cloudY = Math.random() * (universeHeight * 0.5) + 20;
      }

      planet.x = planet.cloudX;
      planet.y = planet.cloudY;
      planet.scale = planet.cloudScale + (planet.isHovered ? 0.15 : 0);
      planet.zIndex = Math.floor(20 + planet.cloudScale * 10);
    }
  }

  universeAnimationId = requestAnimationFrame(universeAnimate)
}

const loadTypePreviewBooks = async () => {
  if (!bookTypeList.value || bookTypeList.value.length === 0) return
  allTypeData.value = [...bookTypeList.value]

  const requestPromises = bookTypeList.value.map(async (type) => {
    if (typeBookCache.value[type.id]) return
    try {
      const res = await request.get('/book', { params: { type: 'category', typeId: type.id, pageNum: 1, pageSize: 3 } })
      typeBookCache.value[type.id] = res.data?.list || []
    } catch (err) {
      typeBookCache.value[type.id] = []
    }
  })

  await Promise.all(requestPromises)
  await nextTick()
  initUniverseAnimation()
}

const initUniverseAnimation = async () => {
  universeContainer = document.querySelector('.galaxy-grid')
  if (!universeContainer || allTypeData.value.length === 0) return
  universeWidth = universeContainer.offsetWidth
  universeHeight = universeContainer.offsetHeight
  stopAllPlanetTask()

  planetPhysics.value = []
  for (let i = 0; i < INIT_PLANET_COUNT; i++) {
    const planet = createPlanet()
    if (planet) planetPhysics.value.push(planet)
  }
  universeAnimationId = requestAnimationFrame(universeAnimate)
}

const stopAllPlanetTask = () => {
  if (universeAnimationId) { cancelAnimationFrame(universeAnimationId); universeAnimationId = null }
}

const handleWindowResize = () => { if (currentView.value === 'universe') initUniverseAnimation() }
window.addEventListener('resize', handleWindowResize)

const handleSelectType = async (typeId) => {
  if (!typeId) return
  const targetType = bookTypeList.value.find(item => item.id === typeId)
  if (!targetType) return
  selectedTypeName.value = targetType.bookType
  selectedTypeId.value = typeId
  typeBooks.value = []
  typePageNum.value = 1
  hasMoreType.value = true
  loadingType.value = false
  currentView.value = 'books'
  isExploding.value = false
  isTransitioning.value = false
  stopAllPlanetTask()
  await loadTypeBooks()
}

const explorePlanet = (type) => {
  isExploding.value = true
  isTransitioning.value = true
  setTimeout(() => { handleSelectType(type.id) }, 750)
}

const backToUniverse = () => {
  currentView.value = 'universe'
  selectedTypeId.value = null
  selectedTypeName.value = ''
  typeBooks.value = []
  typePageNum.value = 1
  hasMoreType.value = true
  loadingType.value = false
  nextTick(() => { initUniverseAnimation() })
}

const handleTarotClick = (book) => {
  if (!book.isFlipped) { book.isFlipped = true }
  else { if (typeof gotoBookDetail === 'function') { gotoBookDetail(book.iSBN) } }
}

const handleChangeBatch = async () => {
  if (loadingRecommend.value) return
  if (!hasMoreRecommend.value) { recommendPageNum.value = 1; hasMoreRecommend.value = true }
  recommendBooks.value.forEach(book => { book.isFlipped = false })
  setTimeout(async () => { recommendBooks.value = []; await loadRecommendBooks() }, 250)
}

const typeBooks = ref([])
const loadingType = ref(false)
const typePageNum = ref(1)
const typePageSize = ref(8)
const hasMoreType = ref(true)
const bookTypeList = inject('bookTypeList')
const selectedTypeId = ref(null)

const loadTypeBooks = async () => {
  if (!hasMoreType.value || loadingType.value || !selectedTypeId.value) return
  loadingType.value = true
  try {
    const res = await request.get('/book', { params: { type: 'category', typeId: selectedTypeId.value, pageNum: typePageNum.value, pageSize: typePageSize.value } })
    const newBooks = res.data?.list || []
    if (newBooks.length > 0) typeBooks.value.push(...newBooks)
    hasMoreType.value = newBooks.length >= typePageSize.value
    typePageNum.value++
  } catch (err) {
    console.error('加载分类书籍失败', err)
    ElMessage.error('分类书籍加载失败')
  } finally {
    loadingType.value = false
  }
}

const isSearching = computed(() => searchKey.value?.trim() !== '')
const isMarqueePaused1 = ref(false)
const isMarqueePaused2 = ref(false)
const isReverse = ref(false)

const changeDirection = (dir) => { isReverse.value = dir === 'right' }

const hotBooksRow1 = computed(() => {
  if (!hotBooks.value) return []
  const half = Math.ceil(hotBooks.value.length / 2)
  return hotBooks.value.slice(0, half)
})

const hotBooksRow2 = computed(() => {
  if (!hotBooks.value) return []
  const half = Math.ceil(hotBooks.value.length / 2)
  return hotBooks.value.slice(half)
})

const copyToClipboard = inject('copyToClipboard')
const generateBookShareLink = inject('generateBookShareLink')

const highlightBookName = (bookName) => {
  const keyword = searchKey.value?.trim()
  if (!keyword || !bookName) return bookName
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(escapedKeyword, 'gi')
  return bookName.replace(regex, (match) => `<span style="font-weight: bold; text-decoration: underline; text-underline-offset: 3px; color: ${isDark.value ? '#60a5fa' : '#1890ff'};">${match}</span>`)
}

const gotoBookDetail = inject('gotoBookDetail')
const goToMyNotes = () => { router.push('/profile/notes'); closeContextMenu() }
const goToMyForumComments = () => { router.push('/profile/forum-comments'); closeContextMenu() }
const goToMyBookComments = () => { router.push('/profile/book-comments'); closeContextMenu() }
const goToReadHistory = () => { router.push('/profile'); closeContextMenu() }

const shareBookToForum = inject('shareBookToForum')
const copyBookShareLink = inject('copyBookShareLink')

const aiLoading = inject('aiLoading')
const addMessage = inject('addMessage')
const sendAiMsg = inject('sendAiMsg')
const sendMessageToAI = inject('sendMessageToAI')
const handleCaptureScreen = inject('handleCaptureScreen')

const currentRightClickItem = inject('currentRightClickItem')
const itemType = ref('')

const handleContextMenu = (e) => {
  if (isMobile.value) return
  const target = e.target
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'BUTTON' || target.closest('.el-button') || target.closest('.el-input') || target.closest('.el-select') || target.closest('.el-date-picker')) return

  e.preventDefault()

  currentRightClickItem.value = null
  let targetElement = target.closest('.note-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.note-item')
    const index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userNotes.value.length) { currentRightClickItem.value = userNotes.value[index]; itemType.value = 'note' }
  }

  targetElement = target.closest('.forum-comment-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.forum-comment-item')
    const index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userForumComments.value.length) { currentRightClickItem.value = userForumComments.value[index]; itemType.value = 'forum-comment' }
  }

  targetElement = target.closest('.book-comment-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.book-comment-item')
    const index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userBookComments.value.length) { currentRightClickItem.value = userBookComments.value[index]; itemType.value = 'book-comment' }
  }

  targetElement = target.closest('.read-history-item')
  if (targetElement) {
    const allItems = document.querySelectorAll('.read-history-item')
    const index = Array.from(allItems).indexOf(targetElement)
    if (index >= 0 && index < userReadHistory.value.length) { currentRightClickItem.value = userReadHistory.value[index]; itemType.value = 'book'; currentRightClickBook.value = currentRightClickItem.value.book }
  }

  if (!currentRightClickItem.value && !currentRightClickBook.value) return

  const menuWidth = 180
  let menuHeight = 50
  if (itemType.value === 'book') menuHeight = 220

  let x = e.clientX
  let y = e.clientY
  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 10
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 10

  contextMenuPosition.value = { x, y }
  showContextMenu.value = true
}

const closeContextMenu = inject('closeContextMenu')

const handleAiSummarizeBook = async () => {
  const book = currentRightClickBook.value
  closeContextMenu()
  if (!book) {
    addMessage('未获取到书籍信息，请重试')
    return
  }

  if (book.aiSummary && book.aiSummary.trim()) {
    addMessage(book.aiSummary)
    return
  }

  const bookName = book.bookname || '未知书籍'
  const author = book.author || '未知作者'
  const introduction = book.information || '暂无简介'

  const prompt = `请帮我详细总结这本经典名著的核心内容：\n书名：《${bookName}》\n作者：${author}\n书籍简介：${introduction}\n\n请你从以下3个方面进行结构化总结：\n1. 核心故事梗概（200字以内）\n2.1-3个主要人物和性格（200字以内）\n3. 1-3句经典名句推荐。`
  try {
    const reply = await sendAiMsg(prompt, 'book_summary', { isbn: book.isbn || book.iSBN })

    if (reply) {
      try {
        await request.put('/book?action=updateAiSummary', null, {
          params: {
            isbn: book.iSBN,
            aiSummary: reply
          }
        })
        book.aiSummary = reply
        ElMessage.success('书籍总结已缓存，下次查看无需重新生成')
      } catch (saveErr) {
        console.warn('AI总结保存失败，不影响查看：', saveErr)
      }
    } else {
      addMessage('😥 生成失败，请稍后重试')
    }
  } catch (err) {
    console.error('AI总结书籍失败:', err)
    addMessage('😥 服务异常，生成失败，请重试')
  } finally {
  }
}

const isInitialLoading = ref(true)

const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL || '';
const aiPicDateObj = ref(new Date());
const aiPicData = ref(null);
const loadingAiPic = ref(false);
const hasPrevPic = ref(true);

// 🔥 新增：画报数据本地缓存池与定时器
const aiPicCache = new Map();
let aiPicCacheTimer = null;

const displayAiDate = computed(() => {
  const y = aiPicDateObj.value.getFullYear();
  const m = String(aiPicDateObj.value.getMonth() + 1).padStart(2, '0');
  const d = String(aiPicDateObj.value.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
});

const isToday = computed(() => {
  const today = new Date();
  return aiPicDateObj.value.toDateString() === today.toDateString();
});

const fetchDailyAiPic = async (targetDate) => {
  if (!currentUserId.value) return;

  const y = targetDate.getFullYear();
  const m = String(targetDate.getMonth() + 1).padStart(2, '0');
  const d = String(targetDate.getDate()).padStart(2, '0');
  const dateStr = `${y}-${m}-${d}`;

  // 🔥 1. 检查缓存：如果缓存池里有这天的数据，直接秒读，终止后续请求
  if (aiPicCache.has(dateStr)) {
    const cachedData = aiPicCache.get(dateStr);
    if (cachedData) {
      aiPicData.value = cachedData;
      hasPrevPic.value = true;
    } else {
      // 缓存的值为 null，说明已经确认过这天没画报
      aiPicData.value = null;
      if (dateStr !== formatDate(new Date())) {
        hasPrevPic.value = false;
        ElMessage.info('没有更早的画报记录啦~');
      }
    }
    return; // 命中缓存，直接结束
  }

  // 2. 没命中缓存，开始发起请求
  loadingAiPic.value = true;
  try {
    const res = await request.get('/user/dailyAiPic', {
      params: { userId: currentUserId.value, genDate: dateStr }
    });

    if (res.code === 200 && res.data) {
      aiPicData.value = res.data;
      hasPrevPic.value = true;
      // 🔥 请求成功，存入缓存池
      aiPicCache.set(dateStr, res.data);
    } else {
      aiPicData.value = null;
      // 🔥 请求成功但没数据，也存入 null 缓存，防止用户反复切这天导致“缓存击穿”
      aiPicCache.set(dateStr, null);

      if (dateStr !== formatDate(new Date())) {
        hasPrevPic.value = false;
        ElMessage.info('没有更早的画报记录啦~');
      }
    }
  } catch (error) {
    aiPicData.value = null;
  } finally {
    loadingAiPic.value = false;
  }
};

const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const getPrevPic = () => {
  if (!hasPrevPic.value || loadingAiPic.value) return;
  const prevDate = new Date(aiPicDateObj.value);
  prevDate.setDate(prevDate.getDate() - 1);
  aiPicDateObj.value = prevDate;
  fetchDailyAiPic(prevDate);
};

const getNextPic = () => {
  if (isToday.value || loadingAiPic.value) return;
  const nextDate = new Date(aiPicDateObj.value);
  nextDate.setDate(nextDate.getDate() + 1);
  aiPicDateObj.value = nextDate;
  hasPrevPic.value = true;
  fetchDailyAiPic(nextDate);
};

onMounted(() => {
  initFloatingStars()
  if (!isRecommendFirstLoad.value) { loadRecommendBooks(); isRecommendFirstLoad.value = true }
  if (selectedTypeId.value) loadTypeBooks()
  loadTypePreviewBooks()
  document.addEventListener('contextmenu', handleContextMenu)

  if(currentUserId.value) {
    fetchDailyAiPic(aiPicDateObj.value);
  }

  // 🔥 新增：开启缓存定时清理任务 (例如：每 10 分钟清空一次缓存，释放内存)
  aiPicCacheTimer = setInterval(() => {
    if (aiPicCache.size > 0) {
      aiPicCache.clear();
      console.log('【灵宝日记】图片缓存已定时清理');
    }
  }, 10 * 60 * 1000);

})

onUnmounted(() => {
  stopAllPlanetTask()
  document.removeEventListener('contextmenu', handleContextMenu)
  window.removeEventListener('resize', handleWindowResize)

  // 🔥 新增：组件销毁时，切记清除定时器
  if (aiPicCacheTimer) {
    clearInterval(aiPicCacheTimer);
    aiPicCacheTimer = null;
  }
})

watch(
    [hotBooks, recommendBooks, books],
    () => { if (hotBooks.value.length || recommendBooks.value.length || books.value.length) isInitialLoading.value = false },
    { deep: true, immediate: true }
)

watch(
    bookTypeList,
    async (newVal) => { if (newVal && newVal.length > 0) await loadTypePreviewBooks() },
    { deep: true, immediate: true }
)
</script>

<style scoped>
/* ==================== 🍱 全新全局紧凑便当盒布局 ==================== */
.bento-dashboard {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 16px; /* 🔻 间距缩小紧凑 */
  width: 100%;
  position: relative;
  z-index: 20;
  padding-bottom: 20px;
}

.bento-item {
  border-radius: 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 15px; /* 🔻 整体内边距收缩 */
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s ease;
}

.bento-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(0,0,0,0.08);
}

.search-bento { grid-column: span 12; }
.hot-bento { grid-column: span 8; }
.ai-bento { grid-column: span 4; }

.tarot-bento { grid-column: span 4; display: flex; flex-direction: column;}
.universe-bento { grid-column: span 8; min-height: 480px; /* 🔻 压缩宇宙高度 */}

.bento-tarot-scroll {
  flex: 1;
  max-height: 450px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  align-content: start;
}
.bento-tarot-scroll::-webkit-scrollbar { width: 4px; }
.bento-tarot-scroll::-webkit-scrollbar-thumb { background: var(--color-scrollbar, #ccc); border-radius: 10px; }
.bento-tarot-scroll::-webkit-scrollbar-track { background: transparent; }

@media screen and (max-width: 1280px) {
  .tarot-bento { grid-column: span 5; }
  .universe-bento { grid-column: span 7; }
}

@media screen and (max-width: 1024px) {
  .hot-bento, .ai-bento, .tarot-bento, .universe-bento {
    grid-column: span 12;
    height: auto;
  }
  .bento-tarot-scroll {
    max-height: none;
    overflow-y: visible;
    overflow-x: auto;
    display: flex !important;
    flex-wrap: nowrap !important;
  }
}

:root {
  --color-text-primary: #333;
  --color-text-secondary: #666;
  --color-text-tertiary: #888;
  --color-text-quaternary: #999;
  --color-border-light: #eee;
  --color-border-medium: #ccc;
  --color-bg-primary: #fff;
  --color-bg-secondary: #f5f7fa;
  --color-scrollbar: #ccc;
  --color-primary: #409eff;
  --color-warning: #ffc107;
  --color-danger: #ff4d4f;
  --radius-sm: 2px;
  --radius-md: 4px;
  --radius-lg: 8px;
  --transition-base: 0.2s ease;
}
.text-ellipsis { white-space: nowrap !important; overflow: hidden !important; text-overflow: ellipsis !important; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.transition-base { transition: all var(--transition-base); }
:deep(.el-card:hover) { transform: translateY(-2px); transition: transform var(--transition-base); position: relative; z-index: 30; }
.book-info-item { display: flex; align-items: center; width: 100%; gap: 8px; }
.book-info-item span:first-child { width: 55px; text-align: left; font-size: 13px; color: #666; flex-shrink: 0; }
.book-info-item span:last-child { font-size: 13px; color: #888; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
:deep(.book-info-item .el-rate__text) { font-size: 12px !important; margin-left: 4px !important; }
:deep(.recommend-section .el-scrollbar__wrap), :deep(.type-section .el-scrollbar__wrap) { overflow-x: hidden !important; overflow-y: auto !important; margin-bottom: 0 !important; }
:deep(.recommend-section .el-scrollbar__view), :deep(.type-section .el-scrollbar__view) { overflow-x: hidden !important; }
:deep(.el-card div[style*="overflow-y: auto"])::-webkit-scrollbar { width: 4px; }
:deep(.el-card div[style*="overflow-y: auto"])::-webkit-scrollbar-thumb { background: var(--color-scrollbar); border-radius: var(--radius-sm); }
.hot-scroll-wrapper::-webkit-scrollbar { height: 4px; }
.hot-scroll-wrapper::-webkit-scrollbar-thumb { background: var(--color-scrollbar); border-radius: var(--radius-sm); }
.load-more-container { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 15px 0; width: 100%; font-size: 14px; color: var(--color-text-secondary); }
.hot-marquee-container { width: 100%; overflow: hidden; padding: 10px 0; position: relative; }
.hot-marquee-track { display: flex; width: max-content; animation: marquee linear infinite; animation-duration: 30s; }
.hot-marquee-track.paused { animation-play-state: paused; }
.hot-marquee-track.reverse-direction { animation-direction: reverse; }
.hot-marquee-group { display: flex; align-items: center; gap: 15px; padding-right: 15px; }
.hot-book-item { flex-shrink: 0; }
@keyframes marquee { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }
.marquee-ctrl-btn { position: absolute; top: 50%; transform: translateY(-50%); z-index: 30; width: 40px; height: 40px; border-radius: 50%; border: none;
  background: rgba(0, 0, 0, 0.4); color: #ffffff; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 18px;
  transition: all 0.3s ease; opacity: 0; pointer-events: none; }
.is-dark .marquee-ctrl-btn { background: rgba(255, 255, 255, 0.2); }
.marquee-ctrl-btn:hover { background: #409eff; color: #fff; scale: 1.1; }
.btn-left { left: 10px; }
.btn-right { right: 10px; }
.hot-container-wrapper:hover .marquee-ctrl-btn { opacity: 1; pointer-events: auto; }
.context-menu {
  position: fixed;
  z-index: 99999 !important;
  display: block !important;
  visibility: visible !important;
  border-radius: 8px;
  padding: 8px 0;
  min-width: 180px;
  font-size: 14px;
  background: var(--glass-bg) !important;
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 1px solid var(--glass-border) !important;
  box-shadow: var(--glass-shadow) !important;
}
.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  color: var(--el-text-color-primary);
}
.context-menu-item:hover { background-color: rgba(128, 128, 128, 0.1) !important; }
.context-menu-divider { height: 1px; background-color: var(--glass-border) !important; margin: 4px 0; }
.universe-stage { perspective: 1200px; min-height: 300px; padding: 20px 10px; display: block; position: relative; overflow: hidden; border-radius: 12px; transition: all 0.6s cubic-bezier(0.25, 1, 0.5, 1); }
.universe-stage::before, .universe-stage::after { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }
.universe-stage::before { background: radial-gradient(ellipse at center, #0a0e27 0%, #000000 100%); }
.universe-stage::after { background-size: 650px 250px; animation: stars-twinkle 8s ease-in-out infinite alternate; background-image: radial-gradient(2px 2px at 20px 30px, #fff, rgba(0,0,0,0)), radial-gradient(2px 2px at 60px 70px, #fff, rgba(0,0,0,0)); }
.universe-stage.space-explosion { transform: scale(2.5); opacity: 0; filter: blur(15px); }
.galaxy-grid { position: relative; width: 100%; height: 100%; min-height: 350px; max-width: 1400px; margin: 0 auto; z-index: 1; }
.planet-system {
  position: absolute;
  left: 0;
  top: 0;
  transform-style: preserve-3d;
  cursor: pointer;
  z-index: 20;
  will-change: translate;
}

.planet-system:hover {
  z-index: 100 !important;
  transform: scale(1.05);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.planet-body { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border-radius: 50%; display: flex; align-items: center; justify-content: center; animation: planet-spin 25s linear infinite; box-shadow: inset -20px -20px 40px rgba(0,0,0,0.7), inset 10px 10px 20px rgba(255,255,255,0.2), 0 0 30px rgba(64,158,255,0.3); }
.planet-style-0 { background: radial-gradient(circle at 30% 30%, #EBB55C, #2E3B3E); }
.planet-style-1 { background: radial-gradient(circle at 30% 30%, #5E7175, #1D2D46); }
.planet-style-2 { background: radial-gradient(circle at 30% 30%, #859B96, #2F3A37); }
.planet-style-3 { background: radial-gradient(circle at 30% 30%, #907C64, #262E30); }
.planet-style-4 { background: radial-gradient(circle at 30% 30%, #EBB55C, #1D2D46); }
.planet-style-5 { background: radial-gradient(circle at 30% 30%, #556763, #262E30); }
.planet-style-6 { background: radial-gradient(circle at 30% 30%, #8191A6, #1D2D46); }
.planet-style-7 { background: radial-gradient(circle at 30% 30%, #CFA56B, #2F3A37); }
.planet-style-8 { background: radial-gradient(circle at 30% 30%, #687980, #2E3B3E); }
.planet-style-9 { background: radial-gradient(circle at 30% 30%, #A2AFBD, #262E30); }
.planet-name { color: #fff; font-weight: bold; font-size: 14px; text-shadow: 0 2px 6px rgba(0,0,0,0.7); z-index: 5; text-align: center; padding: 0 10px; animation: text-anti-spin 25s linear infinite; }
.planet-system:hover .planet-body { width: 120%; height: 120%; top: -10%; left: -10%; animation-duration: 10s; box-shadow: inset -15px -15px 30px rgba(0,0,0,0.5), 0 0 50px rgba(64,158,255,0.7); }
.nebula-ring { position: absolute; top: -15%; left: -15%; width: 130%; height: 130%; border: 3px solid rgba(255, 255, 255, 0.2); border-radius: 50%; pointer-events: none; animation: ring-spin 30s linear infinite; }
.satellite-orbit { position: absolute; top: -20%; left: -20%; width: 160%; height: 160%; border-radius: 50%; transform-style: preserve-3d; pointer-events: none; z-index: 2; }
.orbit-1 { transform: rotateX(70deg) rotateY(10deg); animation: orbit-rotation-1 15s linear infinite; }
.orbit-2 { transform: rotateX(60deg) rotateY(-20deg); animation: orbit-rotation-2 20s linear infinite reverse; }
.orbit-3 { transform: rotateX(50deg) rotateY(30deg); animation: orbit-rotation-3 25s linear infinite; }
.planet-system:hover .satellite-orbit { animation-duration: 40s; }
.planet-system:hover .planet-name { animation-duration: 10s; }
.satellite-item { position: absolute; top: 0; left: 50%; width: max-content; transform: translateX(-50%); pointer-events: auto; }
.satellite-text { display: block; padding: 2px 8px; border-radius: 20px; font-size: 10px; white-space: nowrap; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.orbit-1 .satellite-text { background: rgba(255, 255, 255, 0.9); color: #333; animation: anti-rotation-1 15s linear infinite; }
.orbit-2 .satellite-text { background: rgba(64, 158, 255, 0.95); color: #fff; animation: anti-rotation-2 20s linear infinite reverse; }
.orbit-3 .satellite-text { background: rgba(255, 193, 7, 0.95); color: #333; animation: anti-rotation-3 25s linear infinite; }
.stagger-card-item { transition: all 0.7s cubic-bezier(0.34, 1.56, 0.64, 1); opacity: 1; }
@keyframes planet-spin { from { transform: rotateZ(var(--spin-start, 0deg)); } to { transform: rotateZ(calc(var(--spin-start, 0deg) + 360deg)); } }
@keyframes text-anti-spin { from { transform: rotateZ(calc(var(--spin-start, 0deg) + 360deg)); } to { transform: rotateZ(var(--spin-start, 0deg)); } }
@keyframes ring-spin { from { transform: rotateX(75deg) rotateY(-15deg) rotateZ(0deg); } to { transform: rotateX(75deg) rotateY(-15deg) rotateZ(360deg); } }
@keyframes orbit-rotation-1 { from { transform: rotateX(70deg) rotateY(10deg) rotateZ(0deg); } to { transform: rotateX(70deg) rotateY(10deg) rotateZ(360deg); } }
@keyframes orbit-rotation-2 { from { transform: rotateX(60deg) rotateY(-20deg) rotateZ(0deg); } to { transform: rotateX(60deg) rotateY(-20deg) rotateZ(360deg); } }
@keyframes orbit-rotation-3 { from { transform: rotateX(50deg) rotateY(30deg) rotateZ(0deg); } to { transform: rotateX(50deg) rotateY(30deg) rotateZ(360deg); } }
@keyframes anti-rotation-1 { from { transform: rotateZ(360deg) rotateX(-70deg); } to { transform: rotateZ(0deg) rotateX(-70deg); } }
@keyframes anti-rotation-2 { from { transform: rotateZ(0deg) rotateX(-60deg); } to { transform: rotateZ(360deg) rotateX(-60deg); } }
@keyframes anti-rotation-3 { from { transform: rotateZ(360deg) rotateX(-50deg); } to { transform: rotateZ(0deg) rotateX(-50deg); } }
@keyframes stars-twinkle { 0% { opacity: 0.3; } 50% { opacity: 0.8; } 100% { opacity: 0.5; } }
.dark-theme .universe-stage::before { background: radial-gradient(ellipse at center, #0f172a 0%, #000000 100%); }
.dark-theme .universe-stage::after { opacity: 0.7; }
.dark-theme .satellite-text { border: 1px solid #4b5563; }
.dark-theme .orbit-1 .satellite-text { background: rgba(31, 41, 55, 0.95); color: #e5e7eb; }
.dark-theme .nebula-ring { border-color: rgba(255,255,255,0.1); box-shadow: inset 0 0 15px rgba(255,255,255,0.05); }
.back-universe-btn { transition: all 0.3s ease; }
.back-universe-btn:hover { transform: scale(1.1) rotate(-45deg); }
:deep(.el-select .el-input__inner) { height: 36px; line-height: 36px; border-radius: 6px; }
.dark-select :deep(.el-input__inner) { background-color: #2d3748 !important; border-color: #4b5563 !important; color: #e5e7eb !important; }

/* ==================== 🔮 命运之书·塔罗牌 (压缩高度双列) ==================== */
.tarot-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding: 5px; perspective: 1200px; }
.tarot-card-wrapper { height: 210px; /* 🔻 极限压缩 */ cursor: pointer; perspective: 1000px; }
.tarot-card-inner { position: relative; width: 100%; height: 100%; text-align: center; transform-style: preserve-3d; transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1); border-radius: 12px; }
.tarot-card-inner.is-flipped { transform: rotateY(180deg); }

.tarot-card-wrapper:hover .tarot-card-inner:not(.is-flipped) { transform: translateY(-4px); }
.tarot-card-wrapper.is-dark-hover:hover .tarot-card-inner:not(.is-flipped) { box-shadow: 0 8px 16px rgba(212,175,55,0.2); }
.tarot-card-wrapper.is-light-hover:hover .tarot-card-inner:not(.is-flipped) { box-shadow: 0 8px 16px rgba(173,198,176,0.4); }

.tarot-card-back, .tarot-card-front {
  position: absolute; width: 100%; height: 100%; backface-visibility: hidden;
  border-radius: 12px; overflow: hidden; box-sizing: border-box; box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.tarot-card-back { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 8px; }
.tarot-card-front { transform: rotateY(180deg); display: flex; flex-direction: column; padding: 8px; }

.tarot-refresh-btn { border-radius: 20px; background: transparent !important; transition: all 0.3s; }
.tarot-refresh-btn.btn-light { border-color: #ADC6B0 !important; color: #7A9B8C !important; }
.tarot-refresh-btn.btn-light:hover { background: rgba(173, 198, 176, 0.2) !important; color: #5A7A68 !important; }
.tarot-refresh-btn.btn-dark { border-color: #d4af37 !important; color: #d4af37 !important; }
.tarot-refresh-btn.btn-dark:hover { background: rgba(212,175,55, 0.1) !important; }

.tarot-back-light {
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 3px solid #ADC6B0;
  color: #7A9B8C;
}
.tarot-back-light .tarot-border-line { border: 1px solid rgba(173, 198, 176, 0.7); }
.tarot-back-light .tarot-text { color: #6B8E7D; text-shadow: 0 1px 3px rgba(255,255,255,0.8); }
.tarot-back-light .tarot-hint { color: #A0BBB1; border-top: 1px solid rgba(173, 198, 176, 0.4); }

.tarot-front-light {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(16px) saturate(120%);
  -webkit-backdrop-filter: blur(16px) saturate(120%);
  border: 2px solid #DCEFE9;
  color: #333;
}

.tarot-back-dark {
  background: linear-gradient(135deg, #0f0c1b 0%, #24243e 50%, #0f0c1b 100%);
  border: 3px solid #d4af37;
  color: #d4af37;
}
.tarot-back-dark .tarot-border-line { border: 1px solid rgba(212,175,55,0.45); }
.tarot-back-dark .tarot-text { color: #d4af37; }
.tarot-back-dark .tarot-hint { color: rgba(212,175,55,0.55); border-top: 1px solid rgba(212,175,55,0.2); }

.tarot-front-dark { background: #1f2937; border: 2px solid #374151; color: #e5e7eb; }

.tarot-border-line { position: absolute; top: 4px; left: 4px; right: 4px; bottom: 4px; border-radius: 8px; }
.tarot-center-pattern { position: relative; width: 45px; height: 45px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }

.mystic-circle { position: absolute; width: 100%; height: 100%; border-radius: 50%; animation: tarotSpin 25s linear infinite; }
.dark-circle { border: 2px dashed rgba(212,175,55,0.4); }
.light-circle { border: none; }

.light-tree { font-size: 26px; filter: drop-shadow(0 4px 6px rgba(173,198,176,0.6)); z-index: 2; }
.dark-star { font-size: 22px; color: #d4af37; filter: drop-shadow(0 0 6px rgba(212,175,55,0.7)); }

.spin-leaf { position: absolute; font-size: 12px; filter: drop-shadow(0 2px 4px rgba(173, 198, 176, 0.6)); }
.leaf-1 { top: -8px; left: 50%; transform: translateX(-50%) rotate(15deg); }
.leaf-2 { bottom: 0px; left: -4px; transform: rotate(-105deg); }
.leaf-3 { bottom: 0px; right: -4px; transform: rotate(115deg); }

.tarot-text { font-family: 'Georgia', serif; font-size: 11px; letter-spacing: 2px; font-weight: bold; margin-left: 2px; }
.tarot-hint { position: absolute; bottom: 10px; font-size: 9px; letter-spacing: 1px; padding-top: 4px; width: 80%; }

.book-card-content { display: flex; flex-direction: column; height: 100%; justify-content: flex-start; }
.tarot-book-cover-wrap { width: 100%; height: 95px; overflow: hidden; border-radius: 6px; background-color: #f3f4f6; display: flex; align-items: center; justify-content: center; }
.tarot-book-cover { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.tarot-card-wrapper:hover .tarot-book-cover { transform: scale(1.06); }
.tarot-book-info { flex: 1; display: flex; flex-direction: column; align-items: flex-start; text-align: left; margin-top: 6px; min-width: 0; }
.tarot-book-title { font-size: 12px; font-weight: 600; margin: 0 0 2px 0; width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tarot-book-author { font-size: 10px; color: #6b7280; margin: 0 0 4px 0; width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.tarot-tags-container { display: flex; flex-wrap: wrap; gap: 2px; margin-bottom: 4px; width: 100%; max-height: 40px; overflow: hidden; }
.tarot-book-tag { border: none; font-weight: bold; border-radius: 4px; color: #fff !important; padding: 0 4px; height: 16px; line-height: 16px; font-size: 9px;}
.tarot-sub-tag { background-color: rgba(0,0,0,0.02) !important; border-color: rgba(0,0,0,0.1) !important; color: #555 !important; border-radius: 4px; padding: 0 4px; height: 16px; line-height: 16px; font-size: 9px; }
:deep(.dark-mode) .tarot-sub-tag { background-color: rgba(255,255,255,0.05) !important; border-color: rgba(255,255,255,0.15) !important; color: #ccc !important; }

.tarot-book-intro { font-size: 10px; color: #4b5563; line-height: 1.3; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

@keyframes tarotSpin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* ==================== 📚 全局3D卡片体积微缩 ==================== */
.shelf-book-card-3d-wrapper-home {
  height: 170px; /* 🔻 整体调扁 */
  width: 100% !important;
  perspective: 1000px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  box-sizing: border-box;
}

.book-3d-entity-home { position: relative; width: 100px; height: 140px; transform-style: preserve-3d; transition: transform 0.5s; } /* 🔻 大小减小 */
.shelf-book-card-3d-wrapper-home:hover .book-3d-entity-home { transform: rotateY(-20deg); }
.shelf-book-card-3d-wrapper-home:hover .book-cover-3d { transform: rotateY(-140deg); }
.book-inside-reveal-home { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: #fbf8ef; z-index: 1; padding: 8px; border-radius: 0 4px 4px 0; box-shadow: 3px 4px 10px rgba(0,0,0,0.1); display: flex; flex-direction: column; color: #333;}
.book-spine-3d { position: absolute; width: 15px; height: 100%; left: 0; top: 0; background: linear-gradient(90deg, #2c3e50 0%, #34495e 50%, #1a252f 100%); border-radius: 3px 0 0 3px; transform-origin: left center; transform: rotateY(-90deg); z-index: 5; }
.book-pages-3d { position: absolute; width: 13px; height: 96%; right: 0; top: 2%; background: #eae6df; transform: rotateY(90deg); transform-origin: right center; z-index: 4; }
.book-cover-3d { position: absolute; top: 0; left: 0; width: 100%; height: 100%; transform-origin: left center; transition: transform 0.5s; z-index: 6; transform-style: preserve-3d; }
.book-cover-img { width: 100%; height: 100%; border-radius: 0 4px 4px 0; object-fit: cover; }
.book-name-3d { font-size: 11px; font-weight: bold; margin: 0 0 4px 0; text-align: left; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-height: 1.2; color: #2c3e50; }
.book-intro-3d { font-size: 9px; color: #7f8c8d; text-align: justify; line-height: 1.3; margin: 0; display: -webkit-box; -webkit-line-clamp: 5; -webkit-box-orient: vertical; overflow: hidden; }
.book-read-btn-hint { margin-top: auto; font-size: 9px; color: #3498db; font-weight: bold; text-align: center; border-top: 1px dashed #bdc3c7; padding-top: 2px; }

:deep(.dark-mode) .context-menu { background: var(--glass-bg) !important; border: 1px solid var(--glass-border) !important; }
:deep(.dark-mode) .context-menu-item { color: #e5e7eb; }
:deep(.dark-mode) .context-menu-item:hover { background-color: rgba(255, 255, 255, 0.1) !important; }
:deep(.dark-mode) .context-menu-divider { background-color: var(--glass-border) !important; }
.floating-stars-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; pointer-events: none; z-index: 0; }
.float-star-item { position: absolute; color: #ffd700; text-shadow: 0 0 8px rgba(255, 215, 0, 0.8), 0 0 15px rgba(255, 255, 255, 0.4); opacity: 0; animation: star-drifting linear infinite; will-change: transform, opacity; }

@keyframes star-drifting {
  0% { transform: translateY(80px) rotate(0deg) scale(0.6); opacity: 0; }
  20% { opacity: 0.9; }
  50% { transform: translateY(-120px) rotate(180deg) scale(1.2); opacity: 1; }
  80% { opacity: 0.8; }
  100% { transform: translateY(-300px) rotate(360deg) scale(0.5); opacity: 0; }
}

@media screen and (max-width: 768px) {
  .books-stage .shelf-book-card-3d-wrapper-home { height: 150px !important; }
  .book-grid-container { gap: 12px !important; padding: 8px !important; }
  .tarot-card-wrapper { flex-shrink: 0 !important; width: 160px; height: 210px; }
  .tarot-book-title { font-size: 12px !important; }
  .tarot-book-author { font-size: 10px !important; }
  .tarot-book-intro { font-size: 9px !important; }
}

:deep(.books-stage .el-scrollbar__view) { width: 100% !important; box-sizing: border-box; }
:deep(.books-stage .el-scrollbar__wrap) { overflow-x: hidden !important; }

:deep(.four-grid-group) { display: grid !important; grid-template-columns: repeat(4, 1fr); gap: 6px; width: 100%; }
@media screen and (max-width: 768px) { :deep(.four-grid-group) { grid-template-columns: repeat(2, 1fr); } }

:deep(.grid-radio-item) { width: 70% !important; margin: 0 !important; border: 1px solid rgba(64, 158, 255, 0.3); border-radius: 6px; padding: 6px 4px; text-align: center; background: rgba(255, 255, 255, 0.4); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); font-size: 12px;}
:deep(.grid-radio-item.dark) { background: rgba(0, 0, 0, 0.2); border-color: rgba(255, 255, 255, 0.1); color: #d1d5db; }
:deep(.grid-radio-item:hover) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2); border-color: #409eff; }
:deep(.grid-radio-item .el-radio__input) { display: none; }
:deep(.grid-radio-item.is-checked) { background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(64, 158, 255, 0.3) 100%); border-color: #409eff; font-weight: bold; }

.planet-system { transform-style: preserve-3d; perspective: 1200px; transition: filter 0.3s ease; }
.satellite-orbit { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; border-radius: 50%; border: 1px dashed rgba(255, 255, 255, 0.2); transform-style: preserve-3d; pointer-events: none; z-index: 2; transition: all 0.5s ease; }

.orbit-1 { animation: orbit-spin-1 15s linear infinite; }
.orbit-2 { animation: orbit-spin-2 20s linear infinite reverse; }
.orbit-3 { animation: orbit-spin-3 25s linear infinite; }

@keyframes orbit-spin-1 { 0% { transform: rotateX(75deg) rotateY(10deg) rotateZ(0deg); } 100% { transform: rotateX(75deg) rotateY(10deg) rotateZ(360deg); } }
@keyframes orbit-spin-2 { 0% { transform: rotateX(65deg) rotateY(-20deg) rotateZ(0deg); } 100% { transform: rotateX(65deg) rotateY(-20deg) rotateZ(360deg); } }
@keyframes orbit-spin-3 { 0% { transform: rotateX(80deg) rotateY(30deg) rotateZ(0deg); } 100% { transform: rotateX(80deg) rotateY(30deg) rotateZ(360deg); } }

.satellite-item { position: absolute; top: 0; left: 50%; margin-left: -50px; width: 100px; transform-style: preserve-3d; pointer-events: auto; text-align: center; }
.orbit-1 .satellite-item { animation: anti-spin-1 15s linear infinite reverse; }
.orbit-2 .satellite-item { animation: anti-spin-2 20s linear infinite; }
.orbit-3 .satellite-item { animation: anti-spin-3 25s linear infinite reverse; }

@keyframes anti-spin-1 { 0% { transform: rotateZ(0deg) rotateY(-10deg) rotateX(-75deg); } 100% { transform: rotateZ(-360deg) rotateY(-10deg) rotateX(-75deg); } }
@keyframes anti-spin-2 { 0% { transform: rotateZ(0deg) rotateY(20deg) rotateX(-65deg); } 100% { transform: rotateZ(-360deg) rotateY(20deg) rotateX(-65deg); } }
@keyframes anti-spin-3 { 0% { transform: rotateZ(0deg) rotateY(-30deg) rotateX(-80deg); } 100% { transform: rotateZ(-360deg) rotateY(-30deg) rotateX(-80deg); } }

.planet-system:hover .satellite-orbit { border: 2px solid rgba(64, 158, 255, 0.6); box-shadow: inset 0 0 20px rgba(64, 158, 255, 0.3), 0 0 20px rgba(64, 158, 255, 0.3); animation-duration: 3s; }
.planet-system:hover .satellite-item { animation-duration: 3s; }
.planet-system:hover .nebula-ring { width: 80%; height: 80%; top: 10%; left: 10%; border-width: 6px; border-color: rgba(255, 255, 255, 0.9); box-shadow: 0 0 30px rgba(255, 255, 255, 0.5); animation-duration: 1.5s; }
.planet-system:hover .satellite-text { background: rgba(255, 255, 255, 1); color: #1890ff; transform: scale(1.2); box-shadow: 0 4px 15px rgba(24, 144, 255, 0.5); }

.universe-stage.space-explosion { transform: scale(4) translateZ(800px); opacity: 0; filter: blur(20px) brightness(200%); transition: all 0.8s cubic-bezier(0.55, 0.085, 0.68, 0.53); }
.universe-stage.space-explosion::after { content: ''; position: absolute; top: 50%; left: 50%; width: 200vw; height: 200vh; transform: translate(-50%, -50%); background: radial-gradient(circle, rgba(255,255,255,1) 0%, transparent 60%); opacity: 0; animation: supernova-flash 0.8s forwards; z-index: 1000; pointer-events: none; }

@keyframes supernova-flash { 0% { opacity: 0; transform: translate(-50%, -50%) scale(0.1); } 50% { opacity: 1; transform: translate(-50%, -50%) scale(1); } 100% { opacity: 0; transform: translate(-50%, -50%) scale(2); } }

.light-theme.universe-stage { background: linear-gradient(180deg, #DCEFE9 0%, #ADC6B0 60%, #A0BBB1 100%); overflow: visible; }
.light-theme.universe-stage::before, .light-theme.universe-stage::after { display: none; }
.day-sky-scenery { position: absolute; inset: 0; pointer-events: none; overflow: hidden; z-index: 0; }
.float-light-mote { position: absolute; background: rgba(255, 255, 255, 0.6); border-radius: 50%; box-shadow: 0 0 10px rgba(255, 255, 255, 0.9); animation: light-mote-float linear infinite; will-change: transform, opacity; z-index: 1; }

@keyframes light-mote-float { 0% { transform: translateY(100px) scale(0.5); opacity: 0; } 20% { opacity: 1; } 80% { opacity: 0.8; } 100% { transform: translateY(-300px) scale(1.5); opacity: 0; } }

.light-theme .planet-body { width: 140% !important; height: 60% !important; left: -20% !important; top: 30% !important; border-radius: 50px !important; background: linear-gradient(135deg, #ffffff 20%, #DCEFE9 100%) !important; border: 1px solid rgba(255, 255, 255, 0.9) !important; box-shadow: 0 12px 20px rgba(160, 187, 177, 0.4), inset -5px -10px 15px rgba(173, 198, 176, 0.3) !important; animation: cloud-floating 4s ease-in-out infinite alternate !important; overflow: visible !important; }
.light-theme .planet-body::before { content: ''; position: absolute; width: 55%; height: 130%; bottom: 30%; left: 10%; background: linear-gradient(135deg, #ffffff 0%, #E8F5F0 100%); border-radius: 50%; z-index: -1; }
.light-theme .planet-body::after { content: ''; position: absolute; width: 70%; height: 160%; bottom: 25%; right: 8%; background: linear-gradient(135deg, #ffffff 10%, #DCEFE9 100%); border-radius: 50%; z-index: -1; }

@keyframes cloud-floating { 0% { transform: translateY(0); } 100% { transform: translateY(-12px); } }

.light-theme .planet-system:hover .planet-body { box-shadow: 0 18px 30px rgba(160, 187, 177, 0.7) !important; background: #ffffff !important; }
.light-theme .planet-name { position: relative; z-index: 10; color: #5A7A68; text-shadow: 0 2px 4px rgba(255,255,255,1); font-weight: 800; font-size: 13px; transform: translateY(-6px); animation: none !important; }
.light-theme .nebula-ring { border: 2px dashed rgba(160, 187, 177, 0.6); box-shadow: 0 0 15px rgba(173, 198, 176, 0.3); }
.light-theme .satellite-text { background: rgba(255, 255, 255, 0.95); color: #5A7A68; border: 1px solid #ADC6B0; box-shadow: 0 4px 12px rgba(160, 187, 177, 0.3); font-weight: bold; }

.float-leaf-item { position: absolute; opacity: 0; animation: leaf-drifting linear infinite; will-change: transform, opacity; z-index: 10; filter: drop-shadow(0 2px 4px rgba(160, 187, 177, 0.8)); pointer-events: none; }

@keyframes leaf-drifting { 0% { transform: translate(20vw, -30px) rotate(0deg); opacity: 0; } 10% { opacity: 0.9; } 80% { opacity: 0.8; } 100% { transform: translate(-60vw, 200px) rotate(360deg); opacity: 0; } }
.light-theme .satellite-orbit { animation: none !important; transform: none !important; }
.light-theme .satellite-item { animation: none !important; transform: translateX(-50%) !important; }

.light-theme .orbit-1 .satellite-item { top: 25%; left: 15%; }
.light-theme .orbit-2 .satellite-item { top: 75%; left: 80%; }
.light-theme .orbit-3 .satellite-item { top: 35%; left: 95%; }

.light-theme .satellite-text { animation: cloud-label-float 3s ease-in-out infinite alternate !important; }
.light-theme .orbit-2 .satellite-text { animation-duration: 3.5s !important; animation-direction: alternate-reverse !important; }
.light-theme .orbit-3 .satellite-text { animation-duration: 4s !important; }

@keyframes cloud-label-float { 0% { transform: translateY(0px); } 100% { transform: translateY(-8px); } }

.control-console { width: 60%; padding: 10px 15px; border-radius: 8px; box-sizing: border-box; transition: all 0.4s ease; }
.dark-console { background: rgba(128, 128, 128, 0.05); border: 1px solid transparent; }
.light-console { background: rgba(173, 198, 176, 0.15); border: 1px solid rgba(173, 198, 176, 0.4); }
@media screen and (max-width: 768px) { .control-console { width: 100%; } }

/* 🔻 灵宝日记：原生支持16:9比例缩放 */
.ai-pic-content-wrapper { flex: none; width: 100%; aspect-ratio: 16 / 9; border-radius: 8px; overflow: hidden; background: rgba(0, 0, 0, 0.03); border: 1px solid rgba(0, 0, 0, 0.05); display: flex; align-items: center; justify-content: center; position: relative;}
.dark-theme .ai-pic-content-wrapper { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.05); }

.ai-pic-img { width: 100%; height: 100%; object-fit: contain; transition: transform 0.4s ease; cursor: pointer; }
.ai-pic-img:hover { transform: scale(1.03); }

.ai-pic-nav { display: flex; align-items: center; gap: 8px; background: rgba(255, 255, 255, 0.4); padding: 2px 8px; border-radius: 20px; backdrop-filter: blur(8px); border: 1px solid rgba(255, 255, 255, 0.6); }
.ai-pic-nav.dark-nav { background: rgba(0, 0, 0, 0.2); border: 1px solid rgba(255, 255, 255, 0.1); }
.nav-arrow-btn { border: none !important; background: transparent !important; color: #666; transition: all 0.2s; }
.dark-nav .nav-arrow-btn { color: #ccc; }
.nav-arrow-btn:hover:not(:disabled) { background: rgba(64, 158, 255, 0.2) !important; color: #409eff !important; transform: scale(1.1); }
.nav-arrow-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.nav-date-text { font-size: 11px; font-weight: bold; color: #555; font-family: 'Georgia', serif; letter-spacing: 1px; }
.dark-nav .nav-date-text { color: #ddd; }

.ai-pic-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 20px; }
.empty-icon-wrap { width: 50px; height: 50px; background: rgba(255, 255, 255, 0.6); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 10px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.dark-empty .empty-icon-wrap { background: rgba(255, 255, 255, 0.1); }
.empty-emoji { font-size: 24px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }
.empty-text { font-size: 13px; color: #6b7280; font-weight: bold; margin: 0 0 6px 0; }
.empty-subtext { font-size: 11px; color: #9ca3af; margin: 0; }
</style>