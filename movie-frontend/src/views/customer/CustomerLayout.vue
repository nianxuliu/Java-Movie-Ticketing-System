<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getWalletInfo, rechargeWallet } from '@/api/userWallet'
import { getInfo } from '@/api/user'
import { searchMovies } from '@/api/movie'
import { ElMessage, ElNotification } from 'element-plus'
import {
  Wallet,
  Ticket,
  User,
  SwitchButton,
  Search,
  Money,
  UserFilled,
  House,
  StarFilled,
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const balance = ref(0)
const rechargeDialog = ref(false)
const rechargeAmount = ref(100)
const loading = ref(false)

// --- 搜索相关逻辑 ---
const searchKeyword = ref('')
const searchResults = ref([])
const showSearchResults = ref(false)
let searchTimer = null

// --- WebSocket 逻辑 ---
let socket = null

const initWebSocket = () => {
  if (!userStore.userId) return
  if (socket) return

  // 假设后端端口是 8080，如果不同请修改
  const wsUrl = `ws://localhost:8080/ws/${userStore.userId}`
  socket = new WebSocket(wsUrl)

  socket.onmessage = (event) => {
    const message = event.data
    // 【修改点 1】添加 customClass 配置
    ElNotification({
      title: '新消息提醒',
      message: message,
      type: 'success',
      position: 'top-right',
      duration: 5000,
      customClass: 'retro-notification', // 指定自定义黑金样式类
    })
  }

  socket.onopen = () => {
    console.log(`用户 ${userStore.userId} WebSocket 连接成功`)
  }

  socket.onerror = (e) => {
    console.error('WebSocket 连接错误', e)
  }
  
  socket.onclose = () => {
    console.log('WebSocket 连接断开')
    socket = null
  }
}
// ---------------------------

// 跳转电影详情
const goToMovie = (id) => {
  showSearchResults.value = false
  searchKeyword.value = ''
  router.push(`/customer/movie/${id}`)
}

// 执行搜索
const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchMovies({ keyword: searchKeyword.value, page: 1, size: 5 })
      searchResults.value = res.content || res.records || res || []
      showSearchResults.value = true
    } catch (e) {
      console.error(e)
    }
  }, 300)
}

const closeSearch = () => {
  setTimeout(() => {
    showSearchResults.value = false
  }, 200)
}

// 加载钱包
const loadWallet = async () => {
  const token = localStorage.getItem('token') || userStore.token
  if (!token) return
  try {
    const res = await getWalletInfo()
    let val = 0
    if (res?.data?.data?.balance !== undefined) val = res.data.data.balance
    else if (res?.data?.balance !== undefined) val = res.data.balance
    else if (res?.balance !== undefined) val = res.balance
    balance.value = Number(val)
  } catch (e) {
    console.error('Nav Wallet Load Error', e)
  }
}

// 加载用户
const loadUser = async () => {
  const token = localStorage.getItem('token') || userStore.token
  if (!token) return
  try {
    const res = await getInfo()
    const userData = res.data || res
    
    if (userStore.setUserInfo && typeof userStore.setUserInfo === 'function') {
      userStore.setUserInfo(userData)
    } else {
      if (userData.id) userStore.userId = userData.id
      else if (userData.userId) userStore.userId = userData.userId
      
      if (userData.nickname) userStore.nickname = userData.nickname
      if (userData.avatarUrl) userStore.avatarUrl = userData.avatarUrl
      userStore.token = token
    }

    if (userStore.userId) {
      initWebSocket()
    }
  } catch (e) {
    console.error('Nav User Load Error', e)
  }
}

const handleRecharge = async () => {
  loading.value = true
  try {
    await rechargeWallet({ amount: rechargeAmount.value })
    ElMessage.success(`成功充值 ${rechargeAmount.value} 元`)
    rechargeDialog.value = false
    await loadWallet()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  if (socket) socket.close()
  
  if (userStore.clearUserSession) {
    userStore.clearUserSession()
  } else {
    userStore.token = ''
    userStore.userId = ''
    userStore.nickname = ''
    userStore.avatarUrl = ''
    localStorage.clear()
  }
  ElMessage.success('已安全退出')
  router.push('/login')
}

onMounted(async () => {
  await loadUser()
  await loadWallet()
})

onUnmounted(() => {
  if (socket) socket.close()
})
</script>

<template>
  <div class="customer-layout">
    <nav class="nav-bar">
      <div class="nav-content">
        <div class="nav-left" @click="router.push('/home')">
          <img src="https://img.icons8.com/color/48/000000/movie-projector.png" class="nav-logo" />
          <span class="brand-text">VINTAGE CINEMA</span>
        </div>

        <div class="nav-center">
          <div class="search-wrapper">
            <div class="search-box">
              <input
                type="text"
                v-model="searchKeyword"
                @input="handleSearchInput"
                @focus="handleSearchInput"
                @blur="closeSearch"
                placeholder="搜索经典电影..."
              />
              <el-icon class="search-icon"><Search /></el-icon>
            </div>
            <!-- 搜索结果 -->
            <div class="search-results-panel" v-if="showSearchResults && searchResults.length > 0">
              <div
                v-for="movie in searchResults"
                :key="movie.id"
                class="search-item"
                @mousedown.prevent
                @click="goToMovie(movie.id)"
              >
                <img :src="movie.posterUrl || movie.poster_url" class="mini-poster" referrerpolicy="no-referrer" />
                <div class="item-info">
                  <div class="item-header">
                    <span class="item-title" :title="movie.title">{{ movie.title }}</span>
                    <div class="item-rating" v-if="movie.rating > 0">
                      <el-icon color="#d4af37"><StarFilled /></el-icon>
                      <span>{{ movie.rating }}</span>
                    </div>
                  </div>
                  <div class="item-sub">
                    {{ movie.releaseDate ? movie.releaseDate.substring(0, 4) : '' }} · {{ movie.genre }}
                  </div>
                </div>
              </div>
            </div>
            <!-- 无结果 -->
            <div class="search-results-panel" v-if="showSearchResults && searchKeyword && searchResults.length === 0">
              <div class="no-result">未找到相关电影</div>
            </div>
          </div>
        </div>

        <div class="nav-right">
          <div class="info-item" title="当前余额">
            <el-icon><Wallet /></el-icon>
            <span class="gold-text">￥{{ Number(balance).toFixed(2) }}</span>
          </div>
          <div class="info-item clickable" @click="rechargeDialog = true">
            <el-icon><Money /></el-icon>
            <span>充值</span>
          </div>
          <div class="info-item clickable" @click="router.push('/home')">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </div>
          <div class="info-item clickable" @click="router.push('/customer/my-orders')">
            <el-icon><Ticket /></el-icon>
            <span>订单</span>
          </div>
          <div class="info-item clickable" @click="router.push('/customer/profile')">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </div>
          <div class="info-item clickable logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </div>
          <div class="user-display">
            <el-avatar :size="36" :src="userStore.avatarUrl" class="nav-avatar" :icon="UserFilled">
              {{ userStore.nickname ? userStore.nickname.charAt(0).toUpperCase() : 'U' }}
            </el-avatar>
          </div>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <RouterView />
    </main>

    <!-- 充值弹窗 -->
    <el-dialog
      v-model="rechargeDialog"
      title="余额充值"
      width="350px"
      class="retro-dialog"
      append-to-body
    >
      <div style="text-align: center; margin: 20px 0">
        <p style="color: #aaa; margin-bottom: 10px; font-size: 14px">请输入充值金额 (CNY)</p>
        <el-input-number
          v-model="rechargeAmount"
          :min="1"
          :step="100"
          size="large"
          style="width: 200px"
        />
      </div>
      <template #footer>
        <el-button @click="rechargeDialog = false">取消</el-button>
        <el-button
          type="warning"
          @click="handleRecharge"
          :loading="loading"
          style="background: #d4af37; border: none; color: #000"
        >
          立即充值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 保持 scoped 样式不变 */
.customer-layout {
  min-height: 100vh;
  background-color: #121212;
}
.nav-bar {
  height: 70px;
  background: linear-gradient(to bottom, #1a1a1a, #0a0a0a);
  border-bottom: 1px solid #d4af37;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
}
.nav-content {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.nav-left {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 10px;
  flex-shrink: 0;
}
.brand-text {
  font-family: 'Copperplate', serif;
  font-size: 20px;
  color: #d4af37;
  letter-spacing: 2px;
  font-weight: bold;
}
.nav-center {
  flex: 1;
  display: flex;
  justify-content: center;
  margin: 0 20px;
}
.search-wrapper {
  position: relative;
  width: 100%;
  max-width: 400px;
}
.search-box {
  position: relative;
  width: 88%;
}
.search-box input {
  width: 100%;
  background: #222;
  border: 1px solid #444;
  border-radius: 20px;
  padding: 8px 15px 8px 35px;
  color: #eee;
  outline: none;
  transition: 0.3s;
}
.search-box input:focus {
  border-color: #d4af37;
  box-shadow: 0 0 8px rgba(212, 175, 55, 0.4);
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #888;
}
.search-results-panel {
  position: absolute;
  top: 110%;
  left: 0;
  width: 100%;
  background: #1a1a1a;
  border: 1px solid #d4af37;
  border-radius: 8px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.8);
  z-index: 2000;
  overflow: hidden;
}
.search-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #333;
  transition: background 0.2s;
}
.search-item:last-child {
  border-bottom: none;
}
.search-item:hover {
  background: #2a2a2a;
}
.mini-poster {
  width: 40px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 12px;
  border: 1px solid #555;
}
.item-info {
  flex: 1;
  overflow: hidden;
}
.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.item-title {
  color: #eee;
  font-weight: bold;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-rating {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #d4af37;
  font-size: 12px;
  font-weight: bold;
}
.item-sub {
  color: #888;
  font-size: 12px;
}
.no-result {
  padding: 15px;
  text-align: center;
  color: #666;
  font-size: 14px;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #ccc;
  font-size: 14px;
  flex-shrink: 0;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  user-select: none;
  white-space: nowrap;
}
.gold-text {
  color: #d4af37;
  font-weight: bold;
  font-family: 'Georgia', serif;
  font-size: 16px;
}
.clickable {
  cursor: pointer;
  transition: 0.3s;
  padding: 6px 8px;
  border-radius: 4px;
}
.clickable:hover {
  color: #d4af37;
  background: rgba(212, 175, 55, 0.1);
}
.logout-btn:hover {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}
.user-display {
  margin-left: 5px;
  border: 2px solid #333;
  border-radius: 50%;
  padding: 2px;
}
.nav-avatar {
  display: flex;
  justify-content: center;
  align-items: center;
  background: #333;
  color: #d4af37;
  font-weight: bold;
}
.main-content {
  min-height: calc(100vh - 70px);
}
</style>

<style>
/* 
  【修改点 2】
  在全局 CSS 中添加 .retro-notification 样式
  专门针对右上角的 ElNotification
*/
.retro-notification {
  background-color: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.9) !important;
}

/* 标题金色 */
.retro-notification .el-notification__title {
  color: #d4af37 !important;
  font-weight: bold;
}

/* 内容灰白 */
.retro-notification .el-notification__content {
  color: #ccc !important;
}

/* 关闭按钮 X */
.retro-notification .el-notification__closeBtn {
  color: #666 !important;
}
.retro-notification .el-notification__closeBtn:hover {
  color: #d4af37 !important;
}

/* 成功图标（原绿色改为金色） */
.retro-notification .el-notification__icon.el-icon-success {
  color: #d4af37 !important;
}

/* --------------- 以下是之前写好的其他黑金样式 ---------------- */

/* 全局弹窗黑金样式 (MessageBox) */
.retro-message-box {
  background-color: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.9) !important;
  border-radius: 4px !important;
  padding-bottom: 20px !important;
  width: 420px !important;
}
.retro-message-box .el-message-box__title {
  color: #d4af37 !important;
  font-weight: bold;
  font-size: 18px;
}
.retro-message-box .el-message-box__message {
  color: #ccc !important;
  font-size: 15px;
}
.retro-message-box .el-message-box__headerbtn .el-message-box__close {
  color: #666 !important;
}
.retro-message-box .el-message-box__headerbtn .el-message-box__close:hover {
  color: #d4af37 !important;
}
.retro-message-box .el-message-box__status.el-icon-success {
  color: #d4af37 !important;
}
.retro-message-box .el-button--primary {
  background-color: #d4af37 !important;
  border-color: #d4af37 !important;
  color: #000 !important;
  font-weight: bold;
  padding: 8px 25px;
}
.retro-message-box .el-button--primary:hover {
  background-color: #f0c040 !important;
  border-color: #f0c040 !important;
}
.retro-message-box .el-button--default {
  background-color: transparent !important;
  border: 1px solid #555 !important;
  color: #aaa !important;
}
.retro-message-box .el-button--default:hover {
  border-color: #d4af37 !important;
  color: #d4af37 !important;
  background-color: rgba(212, 175, 55, 0.1) !important;
}

/* 充值弹窗 Dialog */
.retro-dialog {
  background: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.8) !important;
}
.retro-dialog .el-dialog__header .el-dialog__title {
  color: #d4af37 !important;
}
.retro-dialog .el-dialog__body {
  color: #ccc !important;
}
.retro-dialog .el-input__wrapper {
  background-color: #222 !important;
  box-shadow: 0 0 0 1px #444 inset !important;
}
.retro-dialog .el-input__inner {
  color: #eee !important;
}
.retro-dialog .el-button--default {
  background-color: transparent !important;
  border-color: #555 !important;
  color: #888 !important;
}
.retro-dialog .el-button--default:hover {
  border-color: #d4af37 !important;
  color: #d4af37 !important;
}
</style>