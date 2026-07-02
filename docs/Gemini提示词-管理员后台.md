# 🛠️ 管理员后台系统 — Gemini 提示词

## 项目概述

为「沉浸式图书阅读与交流书屋」创建完整的管理员后台。作为一个 BasicLayout 子路由页面，仅管理员可见。

---

## 技术栈

- Vue 3 Composition API (`<script setup>`)
- Element Plus (ElTable, ElTag, ElButton, ElPagination, ElInput, ElDialog, ElTabs)
- Axios: `import request from '../utils/request'`
- 毛玻璃 `glass-panel` + 暗黑模式

---

## 文件清单

### 新建
| 文件 | 说明 |
|------|------|
| `src/views/AdminPanel.vue` | 管理员主面板（Tab切换各模块） |
| `src/views/AdminBooks.vue` | 书籍管理 |
| `src/views/AdminUsers.vue` | 用户管理 |
| `src/views/AdminComments.vue` | 评论审核（书籍评论/段落评论/论坛评论3个tab） |
| `src/views/AdminBottles.vue` | 漂流瓶审核 |

### 修改
| 文件 | 改什么 |
|------|--------|
| `src/router/index.js` | 加 `/admin` 路由（加 meta: { requiresAdmin: true }） |
| `src/components/BasicLayout.vue` | 侧边栏加 `⚙️ 管理后台`（仅管理员可见） |

---

## 后端 API

### 审核统计
```
GET /api/admin/pending-counts
→ { code:200, data: { bookComments: 3, paragraphComments: 0, userComments: 5, bottles: 2 } }
```

### 审核操作
```
PUT /api/admin/book-comment/audit?id=1&status=1        (1=通过 2=拒绝)
PUT /api/admin/paragraph-comment/audit?id=1&status=1
PUT /api/admin/user-comment/audit?id=1&status=1
PUT /api/admin/bottle/audit?id=1&status=1
```

### 分页列表
```
GET /api/admin/book-comments?pageNum=1&pageSize=20&status=0   (0=待审核 1=通过 2=拒绝)
GET /api/admin/paragraph-comments?pageNum=1&pageSize=20&status=0
GET /api/admin/user-comments?pageNum=1&pageSize=20&status=0
GET /api/admin/bottles?pageNum=1&pageSize=20&auditStatus=0
GET /api/admin/users?pageNum=1&pageSize=20&keyword=
GET /api/admin/books?pageNum=1&pageSize=20&keyword=
```

所有列表返回: `{ code:200, data: { total, pageNum, pageSize, list: [...] } }`

---

## AdminPanel.vue 布局

```
┌────────────────────────────────────────────┐
│  ⚙️ 管理后台                               │
├────────────────────────────────────────────┤
│  [待审核: 书评3 | 段评0 | 论坛5 | 漂流瓶2] │  ← 统计卡片
├────────────────────────────────────────────┤
│  [书籍评论] [段落评论] [论坛评论] [漂流瓶]  │  ← ElTabs
│  [用户管理] [书籍管理]                      │
├────────────────────────────────────────────┤
│  表格（每行有操作按钮）                      │
│  ┌──────────────────────────────────────┐  │
│  │ 内容预览...    [通过] [拒绝]         │  │
│  └──────────────────────────────────────┘  │
│  [分页]                                    │
└────────────────────────────────────────────┘
```

## 管理入口

BasicLayout 侧边栏加一个菜单项，通过 `userStore.userType` 判断是否管理员：

```html
<el-menu-item v-if="userStore.userType === '管理员'" index="/admin">
  ⚙️ 管理后台
</el-menu-item>
```

> 获取 userStore: `import { useUserStore } from '../stores/userStore'`
> `const userStore = useUserStore()`

---

## 每个审核模块的通用模式

```vue
<ElTable :data="list" border stripe>
  <ElTableColumn prop="id" label="ID" width="60"/>
  <ElTableColumn prop="content" label="内容" show-overflow-tooltip/>
  <ElTableColumn prop="userId" label="用户" width="100"/>
  <!-- 根据模块显示不同列 -->
  <ElTableColumn label="操作" width="160">
    <template #default="{ row }">
      <ElButton size="small" type="success" @click="audit(row.id, 1)">通过</ElButton>
      <ElButton size="small" type="danger" @click="audit(row.id, 2)">拒绝</ElButton>
    </template>
  </ElTableColumn>
</ElTable>
```

## 样式
- 暗黑模式适配
- 表格用 `glass-panel` 毛玻璃包裹
- 统计卡片用彩色小标签
- 页面整体采用浅灰背景

---

请生成所有需要的 Vue 文件，并提供路由和导航的修改指示。
