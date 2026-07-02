<script setup>
import { ref, onMounted, watch, inject } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const refreshCounts = inject('refreshCounts', () => {})

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)
const loading = ref(false)
const auditStatus = ref(0) // 0=待审

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/bottles', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value, auditStatus: auditStatus.value }
    })
    if (res.code === 200 && res.data) {
      list.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

// 🔥 核心修复：直接拼接 URL
const auditBottle = async (id, status) => {
  try {
    const res = await request.put(`/admin/bottle/audit?id=${id}&status=${status}`)
    if (res.code === 200) {
      ElMessage.success('审核完成')
      fetchData()
      refreshCounts()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '网络异常，请稍后重试')
  }
}

watch(auditStatus, () => { pageNum.value = 1; fetchData() })
onMounted(fetchData)
</script>

<template>
  <div class="sub-module glass-panel" style="padding: 20px;">
    <div class="toolbar" style="margin-bottom: 16px;">
      <ElRadioGroup v-model="auditStatus" size="default" style="--el-radio-button-checked-bg-color: var(--el-color-primary); --el-radio-button-checked-border-color: var(--el-color-primary);">
        <ElRadioButton :label="0">🟡 待审瓶子</ElRadioButton>
        <ElRadioButton :label="1">🟢 已放行</ElRadioButton>
        <ElRadioButton :label="2">🔴 已拦截</ElRadioButton>
      </ElRadioGroup>
    </div>

    <ElTable :data="list" v-loading="loading" border stripe class="custom-table" style="background: transparent;">
      <ElTableColumn prop="id" label="瓶子ID" width="90" align="center" />
      <ElTableColumn prop="content" label="瓶中文本" min-width="300" show-overflow-tooltip />
      <!-- 🔥 核心修复：字段对齐为 userId -->
      <ElTableColumn prop="userId" label="投掷者ID" width="130" align="center" />
      <ElTableColumn prop="receiverId" label="捡到者ID" width="130" align="center">
        <template #default="{ row }">{{ row.receiverId || '未被捡起' }}</template>
      </ElTableColumn>
      <ElTableColumn label="审核操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <div v-if="row.status === 0 || row.auditStatus === 0" style="display:flex;gap:6px;justify-content:center">
            <ElButton size="small" type="success" @click="auditBottle(row.id, 1)" style="--el-button-bg-color: var(--el-color-success); --el-button-border-color: var(--el-color-success);">放行</ElButton>
            <ElButton size="small" type="danger" @click="auditBottle(row.id, 2)" style="--el-button-bg-color: var(--el-color-danger); --el-button-border-color: var(--el-color-danger);">销毁</ElButton>
          </div>
          <span v-else class="text-muted" style="color: var(--el-text-color-secondary);">归档</span>
        </template>
      </ElTableColumn>
    </ElTable>

    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <ElPagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-start; /* 强行左对齐 */
}

:deep(.custom-table .el-table__cell) {
  border-color: var(--glass-border);
}

:deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}
html.dark :deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(255, 255, 255, 0.02) !important;
}
</style>