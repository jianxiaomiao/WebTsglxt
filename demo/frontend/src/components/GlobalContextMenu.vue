<script setup>
import { ref, nextTick, onMounted, onUnmounted, inject, computed, reactive, provide } from 'vue'
import { ElDropdown, ElDropdownItem } from 'element-plus'

// 注入全局状态
const isDark = inject('isDark')
const isMobile = inject('isMobile')

// ====================== 核心状态管理（100%兼容你原来的变量名） ======================
const state = reactive({
  // 完全兼容你原来的provide变量
  showContextMenu: false,
  contextMenuPosition: { x: 0, y: 0 },
  contextMenuType: '',

  currentRightClickItem: null,
  currentRightClickBook: null,
  currentRightClickBookComment: null,
  currentRightClickForumComment: null,
  currentRightClickNote: null,
  currentRightClickUser: null,

  rightClickSelectedText: '',
  rightClickNoteId: null,
  data: {}
})

// 自动把所有状态provide出去，完全兼容原来的代码
Object.keys(state).forEach(key => {
  provide(key, computed(() => state[key]))
})

// ====================== 菜单管理 ======================
const dropdownRef = ref(null)
const virtualTriggerRef = ref(null)
const baseMenu = ref([]) // 全局基础菜单
const customMenu = ref([]) // 当前页面自定义菜单

// 自动合并菜单：自定义菜单在前，基础菜单在后，自动加分隔线
const mergedMenu = computed(() => {
  const result = [...customMenu.value]
  if (result.length > 0 && baseMenu.value.length > 0) {
    result.push({ type: 'divider' })
  }
  result.push(...baseMenu.value)
  return result
})

// ====================== 内部方法 ======================
const initVirtualTrigger = () => {
  virtualTriggerRef.value = {
    getBoundingClientRect() {
      return {
        x: state.contextMenuPosition.x,
        y: state.contextMenuPosition.y,
        width: 0, height: 0,
        top: state.contextMenuPosition.y, left: state.contextMenuPosition.x
      }
    }
  }
}

// 完全兼容你原来的closeContextMenu方法
const closeContextMenu = () => {
  dropdownRef.value?.handleClose()
  // 清空所有状态，和你原来的逻辑完全一致
  Object.assign(state, {
    showContextMenu: false,
    contextMenuType: '',
    currentRightClickItem: null,
    currentRightClickBook: null,
    currentRightClickBookComment: null,
    currentRightClickForumComment: null,
    currentRightClickNote: null,
    currentRightClickUser: null,
    rightClickSelectedText: '',
    rightClickNoteId: null,
    data: {}
  })
}

// 完全兼容你原来的clearContextMenu方法
const clearContextMenu = () => {
  Object.assign(state, {
    currentRightClickItem: null,
    currentRightClickBook: null,
    currentRightClickBookComment: null,
    currentRightClickUser: null,
    currentRightClickForumComment: null,
    currentRightClickNote: null,
    rightClickSelectedText: null,
    contextMenuType: ''
  })
}

// 全局点击关闭菜单
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('contextmenu', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('contextmenu', closeContextMenu)
})

// 对外暴露兼容方法
provide('closeContextMenu', closeContextMenu)
provide('clearContextMenu', clearContextMenu)

// ====================== 对外API ======================
/**
 * 设置全局基础菜单（BasicLayout调用一次即可）
 */
const setBaseMenu = (menu) => {
  baseMenu.value = menu
}

/**
 * 打开右键菜单（指令内部调用）
 */
const openMenu = async (e, options) => {
  // 手机端禁用
  if (isMobile.value) return

  e.preventDefault()
  e.stopPropagation()
  closeContextMenu()

  // 1. 设置状态
  state.contextMenuPosition = { x: e.clientX + 2, y: e.clientY + 2 }
  state.contextMenuType = options.type || ''
  state.rightClickSelectedText = window.getSelection().toString()
  state.data = options.data || {}

  // 2. 设置当前右键对象（自动映射）
  if (options.type && options.item) {
    state[`currentRightClick${options.type.charAt(0).toUpperCase() + options.type.slice(1)}`] = options.item
    state.currentRightClickItem = options.item
  }

  // 3. 生成自定义菜单
  customMenu.value = typeof options.menu === 'function'
      ? options.menu(e, options.item, state)
      : options.menu

  // 4. 打开菜单
  initVirtualTrigger()
  await nextTick()
  dropdownRef.value?.handleOpen()
  state.showContextMenu = true
}

// ====================== 自定义指令 v-context-menu ======================
const vContextMenu = {
  mounted(el, binding) {
    el._contextMenuHandler = (e) => openMenu(e, binding.value)
    el.addEventListener('contextmenu', el._contextMenuHandler)
  },
  unmounted(el) {
    el.removeEventListener('contextmenu', el._contextMenuHandler)
    delete el._contextMenuHandler
  }
}

// ====================== 菜单点击处理 ======================
const handleMenuClick = (key) => {
  // 优先执行自定义菜单的点击事件
  const customItem = customMenu.value.find(item => item.key === key)
  if (customItem && customItem.onClick) {
    customItem.onClick(state)
    closeContextMenu()
    return
  }

  // 再执行基础菜单的点击事件
  const baseItem = baseMenu.value.find(item => item.key === key)
  if (baseItem && baseItem.onClick) {
    baseItem.onClick(state)
    closeContextMenu()
    return
  }

  closeContextMenu()
}

// 对外暴露
defineExpose({
  setBaseMenu,
  closeContextMenu,
  clearContextMenu,
  vContextMenu,
  ...state
})
</script>

<template>
  <ElDropdown
      ref="dropdownRef"
      virtual-triggering
      :virtual-ref="virtualTriggerRef"
      placement="bottom-start"
      hide-on-click
  >
    <div style="width:0;height:0;pointer-events:none"></div>
    <template #dropdown>
      <template v-for="item in mergedMenu" :key="item.key || item.type">
        <ElDropdownItem v-if="item.type === 'divider'" divided />
        <ElDropdownItem
            v-else
            :key="item.key"
            @click="handleMenuClick(item.key)"
            :disabled="item.disabled"
            :class="{ danger: item.danger }"
        >
          {{ item.icon }} {{ item.label }}
        </ElDropdownItem>
      </template>
    </template>
  </ElDropdown>
</template>

<style scoped>
:deep(.el-dropdown-menu) {
  padding: 4px 0;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  background: v-bind('isDark ? "#1f2937" : "#fff"');
  border: v-bind('isDark ? "1px solid #374151" : "1px solid #eee"');
}

:deep(.el-dropdown-menu__item) {
  padding: 8px 16px;
  font-size: 14px;
  color: v-bind('isDark ? "#e5e7eb" : "#333"');
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: v-bind('isDark ? "#374151" : "#f5f7fa"');
}

:deep(.el-dropdown-menu__item--divided) {
  border-top: v-bind('isDark ? "1px solid #374151" : "1px solid #eee"');
}

:deep(.el-dropdown-menu__item.danger) {
  color: #f56c6c;
}

:deep(.el-dropdown-menu__item.danger:hover) {
  background-color: #fef0f0;
}
</style>