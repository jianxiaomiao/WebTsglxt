<template>
  <!-- 无UI中转页：自动跳转书籍详情 -->
  <div style="display: flex;justify-content: center;align-items: center;height: 100vh">
    正在跳转到书籍详情页...
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 从短链接 /s/001 中获取 ISBN
const isbn = route.params.isbn

// 🔥 动态修改OG标签（微信/QQ生成书籍卡片）
const updateOgMeta = (book) => {
  const baseUrl = import.meta.env.DEV
      ? 'https://prologue-freebee-scholar.ngrok-free.dev'
      : window.location.origin

  // 替换页面标题 + OG卡片信息
  document.title = `${book.bookname || '书籍分享'} - 好书推荐`
  document.querySelector('meta[property="og:title"]').content = book.bookname || '书籍分享'
  document.querySelector('meta[property="og:description"]').content = `${book.author || '未知作者'} | ${book.information || '暂无简介'}`
  document.querySelector('meta[property="og:image"]').content = baseUrl + (book.pictureName || '/default-book.png')
  document.querySelector('meta[property="og:url"]').content = window.location.href
}

onMounted(async () => {
  // 校验ISBN
  if (!isbn) {
    ElMessage.error('书籍链接无效')
    router.push('/')
    return
  }

  // ========= 测试用：临时书籍数据（上线后替换为接口请求） =========
  const bookData = ref({
    bookname: '测试书籍',
    author: '作者名称',
    information: '这是一本精彩的书籍',
    pictureName: '/default-book.png'
  })

  // 更新卡片信息
  updateOgMeta(bookData.value)

  // 自动跳转到书籍详情页
  await router.push({
    path: '/book/detail',
    query: {isbn: isbn}
  })
})
</script>