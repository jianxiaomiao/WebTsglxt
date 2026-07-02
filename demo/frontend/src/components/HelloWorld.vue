<script setup>
import { ref } from 'vue'
// 引入Element Plus的消息提示组件（按需）
import { ElMessage } from 'element-plus'

defineProps({
  msg: String,
})

const count = ref(0)
const inputValue = ref('Hello Element Plus!')
</script>

<template>
  <h1>{{ msg }}</h1>

  <!-- 替换Arco的a-card为Element Plus的el-card -->
  <el-card title="Element Plus组件示例" style="width: 100%; max-width: 800px; margin: 0 auto;">
    <!-- 替换Arco的a-button为el-button，type属性兼容primary等 -->
    <el-button type="primary" @click="count++" style="margin-bottom: 16px;">
      count is {{ count }}
    </el-button>

    <!-- 替换Arco的a-input为el-input，v-model用法一致 -->
    <el-input
        v-model="inputValue"
        placeholder="请输入内容"
        style="margin-bottom: 16px;"
    />

    <!-- 展示输入框内容 -->
    <p>输入框内容：{{ inputValue }}</p>

    <p>
      Edit
      <code>components/HelloWorld.vue</code> to test HMR
    </p>
  </el-card>

  <p>
    Check out
    <a href="https://vuejs.org/guide/quick-start.html#local" target="_blank">create-vue</a>,
    the official Vue + Vite starter
  </p>
  <p>
    Learn more about IDE Support for Vue in the
    <a
        href="https://vuejs.org/guide/scaling-up/tooling.html#ide-support"
        target="_blank"
    >Vue Docs Scaling up Guide</a>.
  </p>
  <p class="read-the-docs">Click on the Vite and Vue logos to learn more</p>
</template>

<style scoped>
.read-the-docs {
  color: #888;
  margin-top: 20px;
}
</style>