<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  House,
  VideoCameraFilled,
  User,
  Film,
  OfficeBuilding,
  Ticket,
  School,
  Calendar,
  ChatDotSquare,
  Setting,
  Document,
} from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
let socket = null

// --- WebSocket 逻辑开始 ---
const initWebSocket = () => {
  // 1. 检查用户是否登录及是否有 ID
  if (!userStore.id) {
    console.warn('未获取到用户ID，取消建立 WebSocket 连接')
    return
  }

  // 2. 建立连接 (根据后端代码地址为 /ws/{userId})
  // 如果你的后端端口是 8080，地址如下：
  const wsUrl = `ws://localhost:8080/ws/${userStore.id}`
  socket = new WebSocket(wsUrl)

  // 3. 监听消息接收
  socket.onmessage = (event) => {
    const message = event.data

    // 使用 Notification 在右上角弹出系统通知
    ElNotification({
      title: '站内信提醒',
      message: message,
      type: 'success',
      position: 'top-right',
      duration: 6000, // 6秒后自动关闭
    })
  }

  socket.onopen = () => {
    console.log('WebSocket 连接成功，已监听后端消息广播')
  }

  socket.onerror = () => {
    console.error('WebSocket 连接发生错误')
  }

  socket.onclose = () => {
    console.log('WebSocket 连接已关闭')
  }
}
// --- WebSocket 逻辑结束 ---

function handleLogout() {
  // 退出时关闭 socket
  if (socket) socket.close()

  userStore.clearUserSession()
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  // 只有管理员且已登录才建立连接（或者根据你的需求让普通用户也建立）
  if (userStore.role === 'admin') {
    initWebSocket()
  }
})

onUnmounted(() => {
  // 组件销毁时彻底关闭连接
  if (socket) {
    socket.close()
  }
})
</script>

<template>
  <el-container style="height: 100vh">
    <!-- 左侧菜单 -->
    <el-aside
      :width="isCollapse ? '64px' : '200px'"
      style="background-color: #304156; transition: width 0.3s"
    >
      <el-menu
        :default-active="route.path"
        class="el-menu-vertical-demo"
        :collapse="isCollapse"
        active-text-color="#409eff"
        background-color="#304156"
        text-color="#bfcbd9"
        router
      >
        <el-menu-item index="/">
          <el-icon><house /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-sub-menu index="/content" v-if="userStore.role === 'admin'">
          <template #title>
            <el-icon><video-camera-filled /></el-icon>
            <span>内容管理</span>
          </template>
          <el-menu-item index="/movie/list">
            <el-icon><film /></el-icon>电影管理
          </el-menu-item>
          <el-menu-item index="/actor/list">
            <el-icon><user /></el-icon>演员管理
          </el-menu-item>
          <el-menu-item index="/director/list">
            <el-icon><User /></el-icon>导演管理
          </el-menu-item>
          <el-menu-item index="/review/list">
            <el-icon><ChatDotSquare /></el-icon>影评管理
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/business" v-if="userStore.role === 'admin'">
          <template #title>
            <el-icon><office-building /></el-icon>
            <span>业务管理</span>
          </template>
          <el-menu-item index="/order/list">
            <el-icon><Ticket /></el-icon>订单管理
          </el-menu-item>
          <el-menu-item index="/cinema/list">
            <el-icon><School /></el-icon>影院管理
          </el-menu-item>
          <el-menu-item index="/schedule/list">
            <el-icon><Calendar /></el-icon>排片管理
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/system" v-if="userStore.role === 'admin'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统维护</span>
          </template>
          <el-menu-item index="/system/user">
            <el-icon><User /></el-icon>用户管理
          </el-menu-item>
          <el-menu-item index="/system/log">
            <el-icon><Document /></el-icon>操作日志
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部 Header -->
      <el-header
        style="
          display: flex;
          justify-content: space-between;
          align-items: center;
          border-bottom: 1px solid #eee;
          background-color: #fff;
        "
      >
        <el-button @click="isCollapse = !isCollapse">
          <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
        </el-button>

        <el-dropdown>
          <span style="cursor: pointer">
            欢迎你, <el-tag size="small">{{ userStore.nickname || userStore.role }}</el-tag>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容区 -->
      <el-main style="background-color: #f0f2f5; padding: 15px">
        <el-card shadow="never" style="min-height: 100%">
          <RouterView />
        </el-card>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
/* 侧边栏菜单样式微调 */
.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
}
.el-menu {
  border-right: none;
}
</style>
