<script setup>
import { ref, onMounted, watch, inject } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const IMAGE_BASE_URL = import.meta.env.VITE_IMAGE_BASE_URL;

const props = defineProps({
  commentType: {
    type: String,
    required: true // 'book' | 'paragraph' | 'user'
  }
})

const refreshCounts = inject('refreshCounts', () => {})

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)
const loading = ref(false)
const statusFilter = ref(0) // 0=待审核 1=通过 2=拒绝

// 业务映射映射表
const API_MAP = {
  book: { list: '/admin/book-comments', audit: '/admin/book-comment/audit' },
  paragraph: { list: '/admin/paragraph-comments', audit: '/admin/paragraph-comment/audit' },
  user: { list: '/admin/user-comments', audit: '/admin/user-comment/audit' }
}

const fetchData = async () => {
  loading.value = true
  try {
    const endpoint = API_MAP[props.commentType].list
    const res = await request.get(endpoint, {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        status: statusFilter.value
      }
    })
    if (res.code === 200 && res.data) {
      list.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

// 🔥 核心修复：直接拼接 URL 解决 Axios 传 null body 请求报错
const handleAudit = async (id, targetStatus) => {
  try {
    const endpoint = API_MAP[props.commentType].audit
    const res = await request.put(`${endpoint}?id=${id}&status=${targetStatus}`)
    if (res.code === 200) {
      ElMessage.success(targetStatus === 1 ? '已通过' : '已拒绝')
      fetchData()
      refreshCounts()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '请求服务端出错')
  }
}

watch(statusFilter, () => {
  pageNum.value = 1
  fetchData()
})

onMounted(fetchData)
</script>

<template>
  <div class="sub-module glass-panel" style="padding: 20px;">
    <div class="toolbar" style="margin-bottom: 16px;">
      <ElRadioGroup v-model="statusFilter" size="default" style="--el-radio-button-checked-bg-color: var(--el-color-primary); --el-radio-button-checked-border-color: var(--el-color-primary);">
        <ElRadioButton :label="0">🟡 待审核</ElRadioButton>
        <ElRadioButton :label="1">🟢 已通过</ElRadioButton>
        <ElRadioButton :label="2">🔴 已拒绝</ElRadioButton>
      </ElRadioGroup>
    </div>

    <ElTable :data="list" v-loading="loading" border stripe class="custom-table" style="background: transparent;">
      <ElTableColumn prop="id" label="ID" width="80" align="center" />

      <ElTableColumn label="发布内容" min-width="260">
        <template #default="{ row }">
          <div class="content-text" style="color: var(--el-text-color-primary);">{{ row.content || row.comment || row.userComment }}</div>
        </template>
      </ElTableColumn>

      <!-- 🔥 论坛评论图片预览（仅 commentType='user' 时显示） -->
      <ElTableColumn v-if="commentType === 'user'" label="图片" width="120" align="center">
        <template #default="{ row }">
          <div v-if="row.images && row.images.length > 0" class="audit-image-preview">
            <el-image
                v-for="(img, idx) in row.images.slice(0, 3)"
                :key="idx"
                :src="IMAGE_BASE_URL + img.imageUrl"
                :preview-src-list="row.images.map(i => IMAGE_BASE_URL + i.imageUrl)"
                :initial-index="idx"
                fit="cover"
                class="audit-thumb"
            />
            <span v-if="row.images.length > 3" class="more-badge">+{{ row.images.length - 3 }}</span>
          </div>
          <span v-else class="text-muted">—</span>
        </template>
      </ElTableColumn>

      <ElTableColumn prop="userId" label="用户ID" width="120" align="center" />

      <ElTableColumn label="关联载体" width="160" align="center">
        <template #default="{ row }">
          <ElTag v-if="row.isbn" type="info" size="small" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-text-color-regular);">ISBN: {{ row.isbn }}</ElTag>
          <ElTag v-else-if="row.chapterId" type="warning" size="small" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-warning);">章节: {{ row.chapterId }}</ElTag>
          <span v-else style="color: var(--el-text-color-regular);">帖子论坛</span>
        </template>
      </ElTableColumn>

      <ElTableColumn label="状态" width="100" align="center">
        <template #default="{ row }">
          <ElTag v-if="row.status === 0" type="warning" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-warning);">待审</ElTag>
          <ElTag v-else-if="row.status === 1" type="success" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-success);">正常</ElTag>
          <ElTag v-else type="danger" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-danger);">已封禁</ElTag>
        </template>
      </ElTableColumn>

      <ElTableColumn label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-btns" v-if="row.status === 0">
            <ElButton size="small" type="success" @click="handleAudit(row.id, 1)" style="--el-button-bg-color: var(--el-color-success); --el-button-border-color: var(--el-color-success);">通过</ElButton>
            <ElButton size="small" type="danger" @click="handleAudit(row.id, 2)" style="--el-button-bg-color: var(--el-color-danger); --el-button-border-color: var(--el-color-danger);">拒绝</ElButton>
          </div>
          <span v-else class="text-muted" style="color: var(--el-text-color-secondary);">已处理</span>
        </template>
      </ElTableColumn>
    </ElTable>

    <div class="pagination-wrap" style="margin-top: 16px;">
      <ElPagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
      />
    </div>
  </div>
</template>

<style scoped>
.sub-module { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: flex-start; }
.content-text { line-height: 1.5; font-size: 14px; white-space: pre-wrap; }
.action-btns { display: flex; gap: 8px; justify-content: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 10px; }
.text-muted { font-size: 12px; color: #9ca3af; }

:deep(.custom-table .el-table__cell) {
  border-color: var(--glass-border);
}

:deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}
html.dark :deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(255, 255, 255, 0.02) !important;
}

/* 🔥 审核图片预览缩略图 */
.audit-image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
  align-items: center;
}
.audit-thumb {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid var(--glass-border);
}
.audit-thumb:hover {
  border-color: var(--el-color-primary);
  transform: scale(1.15);
  transition: all 0.2s ease;
}
.more-badge {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-left: 2px;
}
</style>