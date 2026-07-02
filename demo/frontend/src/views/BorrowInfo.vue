<!-- views/BorrowInfo.vue -->
<script setup>
import { ref, inject, watch, nextTick ,computed} from 'vue'
import request from '../utils/request.js'
import {
  ElCard, ElForm, ElFormItem, ElInput, ElDatePicker, ElButton, ElMessage, ElRow, ElCol, ElMessageBox,
  ElPagination, ElDivider
} from 'element-plus'

// 1. 注入BasicLayout传递的借阅列表和搜索方法
const borrows = inject('borrows')
const searchBorrows = inject('searchBorrows')

// 2. 核心响应式变量
const editStatus = ref({}) // 每个借阅记录的编辑状态：{ borrowId: true/false }
const originalBorrowInfos = ref({}) // 原始数据备份
const editBorrowInfo = ref({}) // 当前编辑的借阅数据（解耦）

// 3. 初始化：借阅列表变化时重置状态
const initBorrowStates = () => {
  const newEditStatus = {}
  const newOriginalInfos = {}
  borrows.value.forEach(borrow => {
    newEditStatus[borrow.borrowId] = false
    newOriginalInfos[borrow.borrowId] = JSON.parse(JSON.stringify(borrow))
  })
  editStatus.value = newEditStatus
  originalBorrowInfos.value = newOriginalInfos
  editBorrowInfo.value = {}
}

// 4. 切换编辑状态
const toggleEdit = (borrow) => {
  const borrowId = borrow.borrowId
  if (editStatus.value[borrowId]) {
    editStatus.value[borrowId] = false
    editBorrowInfo.value = {}
  } else {
    editStatus.value[borrowId] = true
    // 深拷贝当前借阅数据（包含关联的书籍信息）
    editBorrowInfo.value = JSON.parse(JSON.stringify(borrow))
    // 转换日期格式（后端LocalDate→前端Date）
    editBorrowInfo.value.borrowDate = new Date(editBorrowInfo.value.borrowDate)
    editBorrowInfo.value.returnDate = new Date(editBorrowInfo.value.returnDate)
  }
}

// 5. 保存编辑（更新归还期限，限制不能早于借阅时间）
const saveBorrowInfo = async (borrowId) => {
  try {
    // 1. 日期校验
    if (editBorrowInfo.value.returnDate <= editBorrowInfo.value.borrowDate) {
      ElMessage.warning('归还期限不能早于借阅时间！')
      return
    }

    // 2. 构造更新数据（仅更新归还期限）
    const updatedBorrow = JSON.parse(JSON.stringify(originalBorrowInfos.value[borrowId]))
    // 转换为后端需要的LocalDate格式（YYYY-MM-DD）
    const formatDate = (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始，补前导零
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };
    updatedBorrow.returnDate = formatDate(editBorrowInfo.value.returnDate)

    // 3. 调用后端更新接口
    await request.put('/book/borrow', updatedBorrow)

    // 4. 同步前端列表
    const targetIndex = borrows.value.findIndex(b => b.borrowId === borrowId)
    if (targetIndex > -1) {
      borrows.value[targetIndex] = {
        ...borrows.value[targetIndex],
        returnDate: updatedBorrow.returnDate
      }
    }

    // 5. 收尾
    originalBorrowInfos.value[borrowId] = JSON.parse(JSON.stringify(updatedBorrow))
    editStatus.value[borrowId] = false
    editBorrowInfo.value = {}
    ElMessage.success(`借阅记录${borrowId}修改成功！`)
  } catch (err) {
    console.error(`保存借阅记录${borrowId}失败`, err)
    ElMessage.error(`保存失败，请稍后再试`)
  }
}

// 新增：更新用户可借阅数量（核心辅助函数）
const updateUserBorrowCount = async (userid) => {
  try {
    // 1. 先查询用户当前信息（获取可借阅数量）
    const userRes = await request.get('/user/info', { params: {userId: userid} })
    if (userRes.code !== 200 || !userRes.data) {
      ElMessage.warning(`用户[${userid}]信息查询失败，跳过数量更新`)
      return false
    }
    const userInfo = userRes.data[0]
    // 2. 构造更新参数（可借阅数量+1，因为归还/删除借阅记录）
    // 2. 深度复制用户对象（避免修改原数据）
    const updateUser = JSON.parse(JSON.stringify(userInfo))

    // 3. 更新“可借阅数量”字段（can_use）
    updateUser.can_use = userInfo.can_use + 1
    // 3. 调用用户更新接口（PUT）
    const updateRes = await request.patch('/user/info', updateUser)
    if (updateRes.code === 200) {
      ElMessage.info(`用户[${userid}]可借阅数量已更新`)
      return true
    } else {
      ElMessage.error(`用户[${userid}]可借阅数量更新失败：${updateRes.msg}`)
      return false
    }
  } catch (err) {
    console.error(`更新用户[${userid}]可借阅数量异常：`, err)
    ElMessage.error(`用户[${userid}]数量更新出错`)
    return false
  }
}

// 6. 删除借阅记录（同时更新书籍库存）
const deleteBorrow = async (borrow) => {
  try {
    await ElMessageBox.confirm(
        '确认删除该借阅记录吗？删除后书籍将自动归还',
        '提示',
        { type: 'error' }
    )

    await updateUserBorrowCount(borrow.userid)
    // 1. 删除借阅记录
    await request.delete(`/book/borrow?borrowId=${borrow.borrowId}&userId=${borrow.userid}`)

    // 2. 更新书籍库存（还书：now_book+1）
    const bookRes = await request.get('/book', { params: { isbn: borrow.iSBN } })
    const book = bookRes.data[0]
    if (book) {
      const updatedBook = JSON.parse(JSON.stringify(book))
      updatedBook.now_book = updatedBook.now_book + 1
      await request.patch('/book', updatedBook)
    }

    // 3. 前端移除该记录
    const targetIndex = borrows.value.findIndex(b => b.borrowId === borrow.borrowId)
    if (targetIndex > -1) {
      borrows.value.splice(targetIndex, 1)
    }

    ElMessage.success('借阅记录删除成功，书籍已归还！')
  } catch (err) {
    if (err === 'cancel') return
    console.error('删除借阅记录失败', err)
    ElMessage.error('删除失败，请稍后再试')
  }
}

// 7. 监听借阅列表变化
watch(borrows, (newVal) => {
  if (newVal && newVal.length > 0) {
    nextTick(() => {
      initBorrowStates()
    })
  }
}, { immediate: true })

const currentPage = ref(1);    // 当前页码
const pageSize = ref(4);      // 每页展示数量（保持4条）
const allBorrows = ref([]);      // 分页原始数据源
// 监听注入的books变化，同步更新分页数据源并重置页码
watch(
    () => borrows.value,
    (newBorrows) => {
      allBorrows.value = newBorrows || [];
      currentPage.value = 1; // 数据更新时重置到第一页
    },
    { immediate: true, deep: true } // 初始化立即执行 + 深度监听数组变化
);

// 计算属性：当前页要展示的书籍（分页截取）
const currentBooks = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  return allBorrows.value.slice(startIndex, endIndex);
});

// 页码切换事件（修正参数名 + 滚动到顶部）
const handlePageChange = (val) => {
  console.log('页码切换：', val);
  currentPage.value = val;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
</script>

<template>
  <div class="book-card-container" style="height: calc(100vh - 190px); overflow-y: auto; padding: 0 10px; ">
  <el-row :gutter="20" style="width: 95%; margin: 20px auto;">
    <el-col :span="24" style="margin-bottom: 20px;" v-for="borrow in currentBooks" :key="borrow.borrowId">
      <el-card style="
        border-radius: 8px;
        box-shadow: 0 2px 12px rgba(0,0,0,0.08);
        min-height: 50px;
      ">
        <el-form label-width="80px">
          <el-row :gutter="20">
            <!-- 用户ID（不可编辑） -->
            <el-col :span="6">
              <el-form-item label="用户ID">
                <el-input v-model="borrow.userid" disabled style="width: 180px;" />
              </el-form-item>
            </el-col>

            <!-- 书名+ISBN（不可编辑） -->
            <el-col :span="6">
              <el-form-item label="书籍信息">
                <el-input
                    :value="`${borrow.book.bookname}（${borrow.iSBN}）`"
                    disabled
                    style="width: 180px;"
                />
              </el-form-item>
            </el-col>

            <!-- 借阅时间（不可编辑） -->
            <el-col :span="6">
              <el-form-item label="借阅时间">
                <el-input
                    :value="new Date(borrow.borrowDate).toLocaleDateString()"
                    disabled
                    style="width: 180px;"
                />
              </el-form-item>
            </el-col>

            <!-- 归还期限（可编辑） -->
            <el-col :span="6">
              <el-form-item label="归还期限">
                <el-date-picker
                    v-if="editStatus[borrow.borrowId]"
                    v-model="editBorrowInfo.returnDate"
                    type="date"
                    :disabled-date="(date) => date <= editBorrowInfo.borrowDate"
                    style="width: 180px;"
                />
                <el-input
                    v-else
                    :value="new Date(borrow.returnDate).toLocaleDateString()"
                    disabled
                    style="width: 180px;"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 操作按钮 -->
        <div style="text-align: right; margin-bottom: 20px;">
          <el-button
              type="primary"
              @click="toggleEdit(borrow)"
              v-if="!editStatus[borrow.borrowId]"
              style="margin-right: 8px;"
          >
            编辑
          </el-button>
          <div v-else>
            <el-button
                type="danger"
                @click="deleteBorrow(borrow)"
                style="margin-right: 8px;"
            >
              删除
            </el-button>
            <el-button
                type="primary"
                @click="saveBorrowInfo(borrow.borrowId)"
                style="margin-right: 8px;"
            >
              确定
            </el-button>
            <el-button
                type="default"
                @click="toggleEdit(borrow)"
            >
              取消
            </el-button>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
  </div>
  <el-divider content-position="left" style="border-color: black; border-width: 1px">
  </el-divider>
  <!-- 分页控件（数据大于2条才显示） -->
  <div
      style="display: flex;
           justify-content: flex-end;
           align-items: center;
           pointer-events: auto;  /* 强制开启点击事件 */
           z-index: 9999;         /* 提升层级，避免被遮挡 */
           position: relative;
           padding-right: 50px;"
  >
    <el-pagination
        @current-change="handlePageChange"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="allBorrows.length"
        layout="prev, pager, next, jumper, ->, total"
        style=" pointer-events: auto; cursor: pointer; background: transparent; border: none; "
        v-if="allBorrows.length > 0"
    />
  </div>
</template>

<style scoped>
/* 透明风格分页控件样式（适配浅色调界面） */
:deep(.el-pagination) {
  background: transparent !important;
  border: none !important;
}

/* 页码按钮：透明背景 + 浅色文字 */
:deep(.el-pager li) {
  background: transparent !important;
  color: #666 !important; /* 适配界面的浅灰文字 */
  border: none !important;
  margin: 0 4px;
}

/* 选中页码：保持原有蓝色 + 透明背景 */
:deep(.el-pager li.is-active) {
  background: rgba(100, 149, 237, 0.2) !important; /* 淡蓝透明背景 */
  color: #1989fa !important; /* 选中态蓝色（和你界面的按钮色一致） */
  font-weight: 500;
}

/* 页码hover效果：浅灰透明背景 */
:deep(.el-pager li:hover) {
  background: rgba(200, 200, 200, 0.15) !important;
}

:deep(.el-pagination__prev),
:deep(.el-pagination__next),
:deep(.el-pagination__button) { /* 覆盖Element Plus按钮的默认背景 */
  background: transparent !important;
  border: none !important;
}
:deep(.el-pagination__prev:hover, .el-pagination__next:hover) {
  color: #1989fa !important;
}

/* 上一页/下一页 禁用状态：更淡的灰色（避免突兀） */
:deep(.el-pagination__prev.is-disabled),
:deep(.el-pagination__next.is-disabled) {
  color: #ccc !important;
  cursor: not-allowed;
}

/* 跳转输入框：透明边框 + 浅色调 */
:deep(.el-pagination__jump .el-input__wrapper) {
  border: 1px solid rgba(200, 200, 200, 0.5) !important;
}
:deep(.el-pagination__jump .el-input__inner) {
  background: transparent !important;
  color: #666 !important;
}
:deep(.el-pagination__total) {
  display: none !important; /* 强制隐藏“共17条” */
}

:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 30px 20px;
  min-height: 180px;
}
</style>