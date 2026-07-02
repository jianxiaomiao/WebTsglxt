<!-- AdminBooks.vue -->
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const keyword = ref('')
const loading = ref(false)

const fetchBooks = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/books', {
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

const deleteBook = (isbn) => {
  ElMessageBox.confirm('下架后全站无法搜到该书，确认删除?', '高危操作', { type: 'warning' }).then(async () => {
    await request.delete('/admin/book', { params: { isbn } })
    ElMessage.success('书籍已抹除')
    fetchBooks()
  }).catch(() => {})
}

onMounted(fetchBooks)
</script>

<template>
  <div class="glass-panel" style="display:flex;flex-direction:column;gap:16px;padding: 20px;">
    <div style="display:flex;gap:12px; margin-bottom: 16px;">
      <ElInput v-model="keyword" placeholder="输入 书名 / ISBN / 作者 查询" style="width:300px" clearable @keyup.enter="fetchBooks" />
      <ElButton type="primary" icon="Search" @click="fetchBooks" style="--el-button-bg-color: var(--el-color-primary); --el-button-border-color: var(--el-color-primary); --el-button-hover-bg-color: var(--el-color-primary-light-3); --el-button-hover-border-color: var(--el-color-primary-light-3);">检索图库</ElButton>
    </div>

    <ElTable :data="list" v-loading="loading" border stripe class="custom-table" style="background: transparent;">
      <ElTableColumn prop="isbn" label="标准书号(ISBN)" width="150" align="center" />
      <ElTableColumn prop="bookName" label="书名" min-width="180" show-overflow-tooltip />
      <ElTableColumn prop="author" label="作者" width="140" />
      <ElTableColumn prop="publisher" label="出版社" width="160" />
      <ElTableColumn prop="all_book" label="库存馆藏" width="100" align="center" />
      <ElTableColumn label="后台指令" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <ElButton size="small" type="danger" icon="Delete" @click="deleteBook(row.isbn || row.iSBN)" style="--el-button-bg-color: var(--el-color-danger); --el-button-border-color: var(--el-color-danger); --el-button-hover-bg-color: var(--el-color-danger-light-3); --el-button-hover-border-color: var(--el-color-danger-light-3);">强行下架</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <div style="display:flex;justify-content:flex-end; margin-top: 16px;">
      <ElPagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchBooks" />
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

</style>