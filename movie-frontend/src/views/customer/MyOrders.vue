<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, cancelOrder, refundOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, CircleClose, RefreshRight, Ticket, Check, VideoPlay } from '@element-plus/icons-vue'

const router = useRouter()
const activeStatus = ref('all') // all, 0(待支付), 1(已支付)
const orderList = ref([])
const loading = ref(false)

// 状态字典
const statusMap = {
  0: { label: '待支付', type: 'danger', icon: Timer },
  1: { label: '已支付', type: 'success', icon: Ticket },
  2: { label: '已取消', type: 'info', icon: CircleClose },
  3: { label: '已退款', type: 'warning', icon: RefreshRight },
  4: { label: '已完成', type: 'primary', icon: Check },
}

// 座位格式化函数： "1-1,1-2" -> "1排1座 1排2座"
const formatSeats = (seatStr) => {
  if (!seatStr) return ''
  return seatStr
    .split(',')
    .map((s) => {
      const [r, c] = s.split('-')
      return `${r}排${c}座`
    })
    .join('  ') // 用两个空格隔开
}

// 格式化时间
const formatTime = (t) => (t ? t.replace('T', ' ').substring(0, 16) : '')

const loadOrders = async () => {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (activeStatus.value !== 'all') {
      params.status = activeStatus.value
    }
    const res = await getMyOrders(params)

    let realData = res.data || res
    if (realData && realData.data) realData = realData.data
    orderList.value = realData && realData.records ? realData.records : []
  } catch (e) {
    console.error('加载订单失败:', e)
  } finally {
    loading.value = false
  }
}

// 跳转去支付
const goToPay = (orderNo) => {
  router.push(`/customer/pay/${orderNo}`)
}

// --- 复古弹窗配置 ---
const vintageConfirm = (message, title, type = 'warning') => {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: type,
    // 【关键】挂载自定义类名，用于覆盖样式
    customClass: 'retro-message-box',
    showClose: true,
    center: true,
  })
}

// 取消订单
const handleCancel = (orderNo) => {
  vintageConfirm('确定要取消该订单吗？取消后无法恢复。', '取消订单')
    .then(async () => {
      await cancelOrder(orderNo)
      ElMessage.success('订单已取消')
      loadOrders()
    })
    .catch(() => {})
}

// 申请退款
const handleRefund = (orderNo) => {
  vintageConfirm('确定要申请退款吗？退款将原路返回到余额。', '退款申请')
    .then(async () => {
      await refundOrder(orderNo)
      ElMessage.success('退款成功，金额已返回钱包')
      loadOrders()
    })
    .catch(() => {})
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">MY TICKET HOLDER / 我的票夹</h1>
        <div class="decorative-line"></div>
      </div>

      <!-- 筛选标签 -->
      <el-tabs v-model="activeStatus" @tab-change="loadOrders" class="retro-tabs">
        <el-tab-pane label="全部" name="all"></el-tab-pane>
        <el-tab-pane label="待支付" name="0"></el-tab-pane>
        <el-tab-pane label="已支付" name="1"></el-tab-pane>
      </el-tabs>

      <!-- 订单列表 -->
      <div class="order-list" v-loading="loading" element-loading-background="rgba(0,0,0,0.5)">
        <div v-if="orderList.length === 0 && !loading" class="empty">
          <el-empty description="暂无相关订单" />
        </div>

        <div v-for="order in orderList" :key="order.orderNo" class="order-card">
          <!-- 左侧：图标装饰区 -->
          <div class="card-left">
            <div class="icon-circle">
              <el-icon :size="24" color="#000"><VideoPlay /></el-icon>
            </div>
            <div class="vertical-text">ADMIT ONE</div>
          </div>

          <!-- 中间：核心信息 -->
          <div class="card-center">
            <div class="header-row">
              <span class="movie-title">{{ order.movieTitle || 'VINTAGE MOVIE' }}</span>
              <span class="order-no">NO. {{ order.orderNo }}</span>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span class="label">TIME</span>
                <span class="val">{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="info-item">
                <span class="label">SEATS</span>
                <!-- 使用格式化函数 -->
                <span class="val seat-highlight">{{ formatSeats(order.seatInfo) }}</span>
              </div>
            </div>

            <div class="footer-row">
              <div class="price-display">
                TOTAL: <span class="amount">￥{{ order.totalPrice }}</span>
              </div>
            </div>
          </div>

          <!-- 右侧：状态与操作 (优化布局) -->
          <div class="card-right">
            <!-- 状态勋章 -->
            <div class="status-badge" :class="'status-' + order.status">
              <el-icon><component :is="statusMap[order.status]?.icon" /></el-icon>
              <span>{{ statusMap[order.status]?.label }}</span>
            </div>

            <div class="divider-dash"></div>

            <!-- 操作按钮组 -->
            <div class="action-group">
              <template v-if="order.status === 0">
                <button class="retro-btn primary" @click="goToPay(order.orderNo)">去支付</button>
                <button class="retro-btn text" @click="handleCancel(order.orderNo)">取消</button>
              </template>

              <template v-if="order.status === 1">
                <button class="retro-btn outline" @click="handleRefund(order.orderNo)">
                  申请退款
                </button>
              </template>

              <template v-if="order.status === 2">
                <span class="disable-text">已失效</span>
              </template>

              <template v-if="order.status === 3">
                <span class="disable-text">退款完成</span>
              </template>
            </div>
          </div>

          <!-- 装饰：票据缺口 -->
          <div class="notch top"></div>
          <div class="notch bottom"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  min-height: 100vh;
  position: relative; /* 为灯光定位 */
  background-color: #26100a;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.6)),
    url('https://www.transparenttextures.com/patterns/brick-wall.png'); /* 砖块纹理 */

  color: #42350b;
  font-family: 'Georgia', serif;
  padding: 40px 20px;
  overflow-x: hidden;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  color: #d4af37;
  font-family: 'Copperplate', serif;
  margin: 0;
  font-size: 24px;
  letter-spacing: 2px;
}
.decorative-line {
  height: 2px;
  background: linear-gradient(90deg, #d4af37, transparent);
  margin: 15px 0 30px;
}

/* --- 订单卡片核心布局 --- */
.order-card {
  background: #1a1a1a;
  border: 1px solid #333;
  margin-bottom: 25px;
  display: flex;
  height: 160px; /* 稍微增高以便布局 */
  position: relative;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}
.order-card:hover {
  border-color: #555;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.5);
}

/* 左侧装饰 */
.card-left {
  width: 70px;
  background: #d4af37;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-right: 2px dashed #1a1a1a;
  position: relative;
}
.icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #000;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 10px;
}
.vertical-text {
  writing-mode: vertical-rl;
  font-size: 10px;
  font-weight: bold;
  color: #1a1a1a;
  letter-spacing: 3px;
}

/* 中间信息 */
.card-center {
  flex: 1;
  padding: 20px 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-bottom: 1px solid #333;
  padding-bottom: 10px;
}
.movie-title {
  font-size: 20px;
  color: #d4af37;
  font-weight: bold;
}
.order-no {
  font-size: 12px;
  color: #666;
  font-family: monospace;
}
.info-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 13px;
}
.info-item .label {
  color: #666;
  width: 50px;
  font-size: 10px;
}
.seat-highlight {
  color: #fff;
  font-weight: bold;
  background: #333;
  padding: 2px 8px;
  border-radius: 4px;
}
.footer-row {
  display: flex;
  justify-content: flex-end;
}
.price-display {
  font-size: 12px;
  color: #888;
}
.amount {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
  margin-left: 5px;
}

/* 右侧操作区 (优化版) */
.card-right {
  width: 180px;
  background: #111;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-left: 2px dashed #333;
  padding: 0 15px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 15px;
}
/* 状态颜色 */
.status-0 {
  color: #f56c6c;
} /* 待支付 */
.status-1 {
  color: #67c23a;
} /* 已支付 */
.status-2 {
  color: #909399;
} /* 已取消 */
.status-3 {
  color: #e6a23c;
} /* 已退款 */
.status-4 {
  color: #409eff;
} /* 已完成 */

.divider-dash {
  width: 80%;
  height: 1px;
  background: #333;
  margin-bottom: 15px;
}

.action-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  align-items: center;
}

/* 复古按钮样式 */
.retro-btn {
  border: none;
  padding: 6px 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  width: 100%;
  font-family: inherit;
  letter-spacing: 1px;
}
.retro-btn.primary {
  background: #d4af37;
  color: #000;
  font-weight: bold;
}
.retro-btn.primary:hover {
  background: #f0c753;
}
.retro-btn.outline {
  background: transparent;
  border: 1px solid #d4af37;
  color: #d4af37;
}
.retro-btn.outline:hover {
  background: rgba(212, 175, 55, 0.1);
}
.retro-btn.text {
  background: transparent;
  color: #666;
  text-decoration: underline;
  padding: 4px;
}
.retro-btn.text:hover {
  color: #aaa;
}
.disable-text {
  font-size: 12px;
  color: #444;
  font-style: italic;
}

/* 票据缺口装饰 */
.notch {
  position: absolute;
  width: 16px;
  height: 16px;
  background: #121212;
  border-radius: 50%;
  right: 172px; /* 根据 card-right 宽度调整 */
  z-index: 2;
  border: 1px solid #333;
}
.notch.top {
  top: -9px;
  border-bottom-color: transparent;
}
.notch.bottom {
  bottom: -9px;
  border-top-color: transparent;
}

/* Tabs 样式覆盖 */
:deep(.el-tabs__item) {
  color: #666;
}
:deep(.el-tabs__item.is-active) {
  color: #d4af37;
  font-weight: bold;
}
:deep(.el-tabs__active-bar) {
  background-color: #d4af37;
}
:deep(.el-tabs__nav-wrap::after) {
  background-color: #333;
}
</style>

<!-- ============================================== -->
<!-- 全局样式：专门用于修改 ElMessageBox 的样式 -->
<!-- 注意：这里没有 scoped，否则无法影响到 body 下的弹窗 -->
<!-- ============================================== -->
<style>
.retro-message-box {
  background-color: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  padding-bottom: 20px !important;
  border-radius: 0 !important; /* 方角更复古 */
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.8) !important;
}

/* 标题 */
.retro-message-box .el-message-box__title {
  color: #d4af37 !important;
  font-family: 'Georgia', serif;
  font-weight: bold;
}

/* 内容文字 */
.retro-message-box .el-message-box__message p {
  color: #ccc !important;
  font-size: 14px;
}

/* 关闭按钮 */
.retro-message-box .el-message-box__headerbtn .el-message-box__close {
  color: #666 !important;
}
.retro-message-box .el-message-box__headerbtn .el-message-box__close:hover {
  color: #d4af37 !important;
}

/* 按钮容器 */
.retro-message-box .el-message-box__btns {
  display: flex;
  justify-content: center;
  gap: 15px;
}

/* 确认按钮 */
.retro-message-box .el-button--primary {
  background-color: #d4af37 !important;
  border-color: #d4af37 !important;
  color: #000 !important;
  font-weight: bold;
  border-radius: 2px !important;
}
.retro-message-box .el-button--primary:hover {
  background-color: #f0c753 !important;
}

/* 取消按钮 */
.retro-message-box .el-button--default {
  background-color: transparent !important;
  border: 1px solid #555 !important;
  color: #aaa !important;
  border-radius: 2px !important;
}
.retro-message-box .el-button--default:hover {
  border-color: #d4af37 !important;
  color: #d4af37 !important;
}
</style>
