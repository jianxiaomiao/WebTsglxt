<script setup>
import { ref, inject, watch, nextTick, computed} from 'vue'
import request from '../utils/request.js'
import {
  ElCard,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElButton,
  ElMessage,
  ElRow,
  ElCol,
  ElPagination,
  ElMessageBox, ElDivider
} from 'element-plus'

// 1. 注入BasicLayout传递的用户列表和搜索方法
const users = inject('users') // 所有用户列表（只读，避免直接修改）
const searchUsers = inject('searchUsers') // 搜索方法

// 2. 核心响应式变量（重点：editUserInfo 专门存储当前编辑的用户数据）
const editStatus = ref({}) // 每个用户的编辑状态：{ userId: true/false }
const originalUserInfos = ref({}) // 每个用户的原始数据备份：{ userId: 用户对象 }
const editUserInfo = ref({}) // 【关键】当前正在编辑的用户数据（与users解耦，避免修改users触发watch）

// 3. 下拉框选项
const typenameOptions = ref([
  { label: '学生', value: '学生' },
  { label: '老师', value: '老师' }
])
const deptOptions = ref([
  { label: '未知', value: '未知' },
  { label: '软件工程', value: '软件工程' }
])

// ========== 优化：姓名特殊字符过滤函数（和Profile完全一致） ==========
/**
 * 过滤姓名中的表情、特殊字符，仅保留汉字、英文、数字
 * @param {string} str - 待过滤的姓名字符串
 * @returns {string} 过滤后的合法姓名
 */
const filterNameSpecialChar = (str) => {
  const reg = /[^\u4e00-\u9fa5a-zA-Z0-9]/g; // 过滤表情+特殊字符
  return str ? str.replace(reg, '') : '';
};

// ========== 优化：姓名合法性校验函数（增强空值处理） ==========
const validateUserName = (userId) => {
  // 1. 空值兜底（避免editUserInfo为空导致报错）
  const name = editUserInfo.value.name || '';

  // 2. 空值校验
  if (!name.trim()) {
    ElMessage.error(`用户${userId}的姓名不能为空！`);
    return false;
  }

  // 3. 字符合法性校验（仅汉字/英文/数字）
  const charReg = /^[\u4e00-\u9fa5a-zA-Z0-9]+$/;
  if (!charReg.test(name.trim())) {
    ElMessage.error(`用户${userId}的姓名仅允许输入汉字、英文或数字！`);
    return false;
  }

  // 4. 长度校验（可选：限制1-50字符）
  if (name.length > 50) {
    ElMessage.error(`用户${userId}的姓名长度不能超过50个字符！`);
    return false;
  }

  return true;
};

// 4. 初始化：用户列表变化时，重置编辑状态和原始数据（仅数组本身变化时触发）
const initUserStates = () => {
  const newEditStatus = {}
  const newOriginalInfos = {}
  users.value.forEach(user => {
    newEditStatus[user.userId] = false // 默认未编辑
    newOriginalInfos[user.userId] = JSON.parse(JSON.stringify(user)) // 深拷贝备份
  })
  editStatus.value = newEditStatus
  originalUserInfos.value = newOriginalInfos
  editUserInfo.value = {} // 清空编辑数据（防止残留上一次的编辑内容）
}

// 5. 切换编辑状态（核心优化：进入编辑时主动过滤原有姓名）
const toggleEdit = (user) => {
  const userId = user.userId
  // 如果当前是编辑状态 → 取消编辑
  if (editStatus.value[userId]) {
    editStatus.value[userId] = false
    editUserInfo.value = {} // 清空编辑数据
  }
  // 如果当前是非编辑状态 → 进入编辑（主动过滤原有姓名）
  else {
    editStatus.value[userId] = true
    // 【关键优化】深拷贝用户数据 + 主动过滤姓名中的表情/特殊字符
    editUserInfo.value = JSON.parse(JSON.stringify(user))
    editUserInfo.value.name = filterNameSpecialChar(user.name || '') // 编辑初始化时过滤
  }
}

// 6. 保存编辑（核心优化：先校验姓名合法性，再提交）
const saveUserInfo = async (userId) => {
  try {
    // 【新增】第一步：校验姓名合法性，不通过则直接返回
    if (!validateUserName(userId)) {
      return;
    }

    // 校验：确保编辑数据存在
    if (!editUserInfo.value || !editUserInfo.value.userId) {
      ElMessage.warning('编辑数据异常，请重新编辑！')
      return
    }

    // 1. 合并原始数据和编辑后的数据（只更新允许编辑的字段）
    const updatedUser = JSON.parse(JSON.stringify(originalUserInfos.value[userId]))
    updatedUser.name = editUserInfo.value.name // 姓名（已过滤）
    updatedUser.typeName = editUserInfo.value.typeName // 用户类型
    updatedUser.dept = editUserInfo.value.dept // 系别

    // 2. 调用后端更新接口
    const res = await request.put('/user/info', updatedUser)
    // 修改点1：res.data.code → res.code
    if (res.code !== 200) {
      ElMessage.error(res.msg || `保存用户${userId}信息失败`)
      return
    }

    // 3. 同步更新users数组（让页面展示最新数据）
    await searchUsers(); // 这个方法是从父组件provide来的，负责重新请求后端数据

    // 4. 更新原始数据备份 + 退出编辑状态 + 清空编辑数据
    originalUserInfos.value[userId] = JSON.parse(JSON.stringify(updatedUser))
    editStatus.value[userId] = false
    editUserInfo.value = {} // 清空，防止影响下一次编辑

    ElMessage.success(`用户${userId}信息修改成功！`)
  } catch (err) {
    console.error(`保存用户${userId}信息失败`, err)
    ElMessage.error(`保存用户${userId}信息失败，请稍后再试`)
  }
}

// 7. 监听users数组变化（仅数组新增/删除时触发，移除deep: true避免编辑时触发）
watch(users, (newVal) => {
  if (newVal && newVal.length > 0) {
    nextTick(() => {
      initUserStates()
    })
  }
}, { immediate: true }) // 仅初始化和数组本身变化时执行，不监测元素属性

// 8. 新增：删除用户方法
const deleteUser = async (userId) => {
  try {
    // 1. 二次确认删除操作
    await ElMessageBox.confirm(
        '删除用户将自动归还其所有借阅书籍，确认删除吗？',
        '危险提示',
        { type: 'error', confirmButtonText: '确认', cancelButtonText: '取消' }
    )

    // 2. 获取该用户的所有借阅记录
    const borrowRes = await request.get('/book/borrow', { params: { userId: userId } })
    // 修改点2：res.data.data → res.data
    const borrowList = borrowRes.data || []

    if (borrowList.length > 0) {
      // 3. 遍历借阅记录，批量归还
      await Promise.all(
          borrowList.map(async (borrowItem) => {
            // 3.1 根据ISBN查询书籍信息
            const bookRes = await request.get('/book', { params: { isbn: borrowItem.iSBN } })
            // 修改点3：res.data.data[0] → res.data[0]
            const bookInfo = bookRes.data[0]
            if (!bookInfo) return

            // 3.2 更新书籍库存（还书逻辑：库存+1）
            const updatedBook = JSON.parse(JSON.stringify(bookInfo))
            updatedBook.now_book = updatedBook.now_book + 1
            const bookUpdateRes = await request.patch('/book', updatedBook)
            // 修改点4：判断书籍更新返回码
            if (bookUpdateRes.code !== 200) {
              ElMessage.warning(`书籍${borrowItem.iSBN}库存更新失败：${bookUpdateRes.msg}`)
            }

            // 3.3 删除该条借阅记录
            const delBorrowRes = await request.delete(`/book/borrow?borrowId=${borrowItem.borrowId}&userId=${userId}`)
            // 修改点5：判断借阅记录删除返回码
            if (delBorrowRes.code !== 200) {
              ElMessage.warning(`借阅记录${borrowItem.borrowId}删除失败：${delBorrowRes.msg}`)
            }
          })
      )
    }

    // 调用后端DELETE接口
    const delUserRes = await request.delete(`/user/info?userId=${userId}`)
    // 修改点6：判断用户删除返回码
    if (delUserRes.code !== 200) {
      ElMessage.error(delUserRes.msg || '用户删除失败')
      return
    }

    // 5. 操作成功提示
    ElMessage.success('用户删除成功，已自动归还其所有借阅书籍！')
    // 刷新用户列表（根据实际业务补充）
    // 前端移除该用户
    const targetIndex = users.value.findIndex(u => u.userId === userId)
    if (targetIndex > -1) {
      users.value.splice(targetIndex, 1)
    }

    // 清理编辑状态
    delete editStatus.value[userId]
    delete originalUserInfos.value[userId]
    editUserInfo.value = {}

  } catch (err) {
    if (err === 'cancel') return
    console.error('删除用户并归还书籍失败', err)
    ElMessage.error('删除失败，请稍后重试')
  }
}

const currentPage = ref(1);    // 当前页码
const pageSize = ref(4);      // 每页展示数量（保持4条）
const allUsers = ref([]);      // 分页原始数据源
// 监听注入的books变化，同步更新分页数据源并重置页码
watch(
    () => users.value,
    (newUsers) => {
      allUsers.value = newUsers || [];
      currentPage.value = 1; // 数据更新时重置到第一页
    },
    { immediate: true, deep: true } // 初始化立即执行 + 深度监听数组变化
);

// 计算属性：当前页要展示的书籍（分页截取）
const currentUsers = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  return allUsers.value.slice(startIndex, endIndex);
});

// 页码切换事件（修正参数名 + 滚动到顶部）
const handlePageChange = (val) => {
  console.log('页码切换：', val);
  currentPage.value = val;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
</script>

<template>
  <div class="book-card-container" style="height: calc(100vh - 200px); overflow-y: auto; padding: 0 10px; ">
    <!-- 有用户数据时：一行一个窗格展示 -->
    <el-row :gutter="20" style="width: 95%; margin: 20px auto;">
      <!-- 单个用户窗格 -->
      <el-col :span="24" style="margin-bottom: 20px;" v-for="user in currentUsers" :key="user.userId">
        <el-card style="
        border-radius: 8px;
        box-shadow: 0 2px 12px rgba(0,0,0,0.08);
        min-height: 50px; /* 卡片整体最小高度 */
      ">
          <!-- 用户信息表单（核心：根据编辑状态切换绑定的数据源） -->
          <el-form label-width="80px" >
            <el-row :gutter="20">
              <!-- 1. 用户ID（始终绑定原user，不可编辑） -->
              <el-col :span="6">
                <el-form-item label="用户ID">
                  <el-input v-model="user.userId" disabled style="width: 180px;" />
                </el-form-item>
              </el-col>

              <!-- 2. 姓名（编辑状态绑定editUserInfo，非编辑绑定原user） -->
              <el-col :span="6">
                <el-form-item label="姓名">
                  <!-- 编辑状态：绑定editUserInfo.name，可编辑 + 实时过滤 -->
                  <el-input
                      v-if="editStatus[user.userId]"
                      v-model="editUserInfo.name"
                      style="width: 180px;"
                      @input="editUserInfo.name = filterNameSpecialChar(editUserInfo.name)"
                      maxlength="50"
                      show-word-limit
                      placeholder="仅允许输入汉字、英文或数字"
                  />
                  <!-- 非编辑状态：绑定user.name，禁用 -->
                  <el-input
                      v-else
                      v-model="user.name"
                      disabled
                      style="width: 180px;"
                  />
                </el-form-item>
              </el-col>

              <!-- 3. 用户类型（同上） -->
              <el-col :span="6">
                <el-form-item label="用户类型">
                  <el-select
                      v-if="editStatus[user.userId]"
                      v-model="editUserInfo.typeName"
                      :options="typenameOptions"
                      style="width: 180px;"
                  />
                  <el-select
                      v-else
                      v-model="user.typeName"
                      :options="typenameOptions"
                      disabled
                      style="width: 180px;"
                  />
                </el-form-item>
              </el-col>

              <!-- 4. 系别（同上） -->
              <el-col :span="6">
                <el-form-item label="系别">
                  <el-select
                      v-if="editStatus[user.userId]"
                      v-model="editUserInfo.dept"
                      :options="deptOptions"
                      style="width: 180px;"
                  />
                  <el-select
                      v-else
                      v-model="user.dept"
                      :options="deptOptions"
                      disabled
                      style="width: 180px;"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <!-- 操作按钮区域（每个用户独立） -->
          <div style="text-align: right; margin-bottom: 20px;">
            <!-- 非编辑状态：编辑按钮 -->
            <el-button
                type="primary"
                @click="toggleEdit(user)"
                v-if="!editStatus[user.userId]"
                style="margin-right: 8px;"
            >
              编辑
            </el-button>
            <!-- 编辑状态：删除+确定+取消（统一间距） -->
            <div v-else>
              <el-button
                  type="danger"
                  @click="deleteUser(user.userId)"
                  style="margin-right: 8px;"
              >
                删除
              </el-button>
              <el-button
                  type="primary"
                  @click="saveUserInfo(user.userId)"
                  style="margin-right: 8px;"
              >
                确定
              </el-button>
              <el-button
                  type="default"
                  @click="toggleEdit(user)"
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
        :total="allUsers.length"
        layout="prev, pager, next, jumper, ->, total"
        style=" pointer-events: auto; cursor: pointer; background: transparent; border: none; "
        v-if="allUsers.length > 0"
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

/* 穿透修改el-card__body，让内容垂直居中 */
:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  justify-content: center; /* 内容垂直居中（中段） */
  padding: 30px 20px; /* 上下内边距加大，远离顶部 */
  min-height: 180px; /* 内容区域最小高度，支撑居中 */
}
</style>