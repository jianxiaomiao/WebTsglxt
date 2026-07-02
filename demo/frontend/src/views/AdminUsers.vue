<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)
const keyword = ref('')
const loading = ref(false)

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/users', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }
    })
    if (res.code === 200 && res.data) {
      list.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

// 🔥 核心修复：基于 canUse 判断，直接拼接 URL
const toggleStatus = async (row) => {
  const newCanUse = row.canUse === 0 ? 1 : 0
  try {
    const res = await request.put(`/admin/user/toggle-freeze?userId=${row.userId}&canUse=${newCanUse}`)
    if (res.code === 200) {
      ElMessage.success(newCanUse === 1 ? '已解冻' : '已封号')
      row.canUse = newCanUse
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (err) {
    ElMessage.error(err?.msg || err?.message || '网络异常，请稍后重试')
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="glass-panel" style="display:flex;flex-direction:column;gap:16px;padding: 20px;">
    <div style="display:flex;gap:12px; margin-bottom: 16px;">
      <ElInput v-model="keyword" placeholder="搜索 用户名 / 用户UID" style="width:280px" clearable @keyup.enter="fetchUsers" />
      <ElButton type="primary" icon="Search" @click="fetchUsers" style="--el-button-bg-color: var(--el-color-primary); --el-button-border-color: var(--el-color-primary); --el-button-hover-bg-color: var(--el-color-primary-light-3); --el-button-hover-border-color: var(--el-color-primary-light-3);">精准检索</ElButton>
    </div>

    <ElTable :data="list" v-loading="loading" border stripe class="custom-table" style="background: transparent;">
      <ElTableColumn prop="userId" label="用户UID" width="110" align="center" />
      <ElTableColumn prop="name" label="昵称" min-width="150" />
      <!-- 🔥 已删掉邮箱列 -->
      <ElTableColumn label="角色组" width="120" align="center">
        <template #default="{ row }">
          <ElTag v-if="row.type === 3 || row.userType === 3" type="danger" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-danger);">超管</ElTag>
          <ElTag v-else type="primary" style="--el-tag-bg-color: var(--glass-bg); --el-tag-border-color: var(--glass-border); --el-tag-text-color: var(--el-color-primary);">读者</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="管控" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 🔥 核心修复：使用 canUse 驱动文字与颜色 -->
          <ElButton size="small" :type="row.canUse === 0 ? 'success' : 'danger'" @click="toggleStatus(row)" style="min-width: 80px;">
            {{ row.canUse === 0 ? '解冻' : '封号' }}
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <div style="display:flex;justify-content:flex-end; margin-top: 16px;">
      <ElPagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchUsers" />
    </div>
  </div>
</template>

<style scoped>
:deep(.custom-table .el-table__cell) {
  border-color: var(--glass-border);
}

:deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}
html.dark :deep(.custom-table .el-table__row--striped .el-table__cell) {
  background-color: rgba(255, 255, 255, 0.02) !important;
}

:deep(.el-button--danger) {
  --el-button-bg-color: var(--el-color-danger);
  --el-button-border-color: var(--el-color-danger);
  --el-button-hover-bg-color: var(--el-color-danger-light-3);
  --el-button-hover-border-color: var(--el-color-danger-light-3);
}

:deep(.el-button--success) {
  --el-button-bg-color: var(--el-color-success);
  --el-button-border-color: var(--el-color-success);
  --el-button-hover-bg-color: var(--el-color-success-light-3);
  --el-button-hover-border-color: var(--el-color-success-light-3);
}
</style>