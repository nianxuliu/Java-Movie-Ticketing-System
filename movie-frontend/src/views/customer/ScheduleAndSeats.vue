<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSeatMap, createOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Monitor } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const scheduleId = route.params.scheduleId

// --- 状态定义 ---
const loading = ref(true)
const seatData = ref({
  rows: 0,
  cols: 0,
  soldSeats: [],
  brokenSeats: [],
})

// 定义侧边栏所需的电影信息结构
const scheduleInfo = ref({
  movieTitle: '',
  startTime: '',
  hallName: '',
  price: 0,
  cinemaName: '',
})
const selectedSeats = ref([])

// --- 加载数据 ---
const loadSeats = async () => {
  loading.value = true
  try {
    const res = await getSeatMap(scheduleId)
    // 兼容处理：有些后端封装可能多一层 data (Result.data)
    const data = res.data || res

    if (!data) {
      throw new Error('未获取到场次数据')
    }

    scheduleInfo.value = {
      movieTitle: data.movieTitle || '未知电影',
      cinemaName: data.cinemaName || '未知影院',
      hallName: data.hallName || '未知影厅',
      price: data.price || 0,
      startTime: data.startTime || '',
    }

    // 2. 解析影厅座位配置
    let config = data.hallConfig
    if (typeof config === 'string') {
      try {
        config = JSON.parse(config)
      } catch (e) {
        console.error('JSON解析失败', e)
        config = {}
      }
    }

    // 3. 赋值座位数据
    seatData.value = {
      rows: Number(config?.rows) || 0,
      cols: Number(config?.cols) || 0,
      soldSeats: data.soldSeats || [],
      brokenSeats: data.brokenSeats || config?.broken_seats || [],
    }
  } catch (e) {
    console.error('加载座位失败:', e)
    ElMessage.error('无法获取排片信息，请稍后重试')
  } finally {
    loading.value = false
  }
}

// --- 选座逻辑 ---
const handleSeatClick = (r, c) => {
  const seatKey = `${r}-${c}`

  if (seatData.value.brokenSeats.includes(seatKey)) {
    return ElMessage.warning('该座位正在维修中')
  }
  if (seatData.value.soldSeats.includes(seatKey)) {
    return ElMessage.warning('该座位已售出')
  }

  const index = selectedSeats.value.indexOf(seatKey)
  if (index > -1) {
    selectedSeats.value.splice(index, 1)
  } else {
    if (selectedSeats.value.length >= 6) {
      return ElMessage.warning('单笔订单限购 6 张票')
    }
    selectedSeats.value.push(seatKey)
  }
}

// 获取座位样式
const getSeatClass = (r, c) => {
  const key = `${r}-${c}`
  if (seatData.value.brokenSeats.includes(key)) return 'is-broken'
  if (seatData.value.soldSeats.includes(key)) return 'is-sold'
  if (selectedSeats.value.includes(key)) return 'is-selected'
  return 'is-available'
}

// 总价计算
const totalPrice = computed(() => {
  const price = Number(scheduleInfo.value.price) || 0
  return (selectedSeats.value.length * price).toFixed(2)
})

// 提交订单
const submitOrder = async () => {
  if (selectedSeats.value.length === 0) return ElMessage.warning('请先选座')

  try {
    const res = await createOrder({
      scheduleId: scheduleId,
      seats: selectedSeats.value,
    })
    const orderNo = res.data || res

    // 【修改点 1】添加 customClass 配置
    ElMessageBox.confirm('座位锁定成功！请在 15 分钟内完成支付。', '下单成功', {
      confirmButtonText: '去支付',
      cancelButtonText: '稍后支付',
      type: 'success',
      customClass: 'retro-message-box', // 指定自定义类名
    })
      .then(() => router.push(`/customer/pay/${orderNo}`))
      .catch(() => router.push('/customer/my-orders'))
  } catch (e) {
    console.error(e)
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '时间待定'
  return timeStr.replace('T', ' ').substring(0, 16)
}

onMounted(() => loadSeats())
</script>

<template>
  <div
    class="seat-page"
    v-loading="loading"
    element-loading-background="#121212"
    element-loading-text="正在获取影厅与排片信息..."
  >
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="header-left" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回详情</span>
      </div>
      <div class="header-center">
        <!-- 动态显示影院和影厅 -->
        <span class="cinema-name">{{ scheduleInfo.cinemaName }}</span>
        <span class="divider">/</span>
        <span class="hall-name">{{ scheduleInfo.hallName }}</span>
      </div>
      <div class="header-right"></div>
    </div>

    <div class="main-content" v-if="seatData.rows > 0">
      <!-- 左侧：选座区域 -->
      <div class="theater-section">
        <div class="screen-container">
          <div class="screen-curve">
            <el-icon class="screen-icon"><Monitor /></el-icon>
          </div>
          <p class="screen-text">银幕中央</p>
        </div>

        <div class="seats-wrapper">
          <div
            class="seats-grid"
            :style="{
              '--rows': seatData.rows,
              '--cols': seatData.cols,
            }"
          >
            <template v-for="r in seatData.rows" :key="'row-' + r">
              <div
                v-for="c in seatData.cols"
                :key="'col-' + c"
                class="seat-cell"
                :class="getSeatClass(r, c)"
                @click="handleSeatClick(r, c)"
              >
                <div class="seat-shape"></div>
                <el-tooltip
                  effect="dark"
                  :content="`${r}排${c}座`"
                  placement="top"
                  :show-after="500"
                >
                  <div class="tooltip-trigger"></div>
                </el-tooltip>
              </div>
            </template>
          </div>
        </div>

        <div class="legend-bar">
          <div class="legend-item type-available">
            <div class="dot"></div>
            <span>可选</span>
          </div>
          <div class="legend-item type-selected">
            <div class="dot"></div>
            <span>已选</span>
          </div>
          <div class="legend-item type-sold">
            <div class="dot"></div>
            <span>已售</span>
          </div>
          <div class="legend-item type-broken">
            <div class="dot"></div>
            <span>维修</span>
          </div>
        </div>
      </div>

      <!-- 右侧：详情侧边栏 (已绑定动态数据) -->
      <aside class="sidebar-info">
        <div class="info-card">
          <!-- 动态电影名 -->
          <h2 class="m-title">{{ scheduleInfo.movieTitle }}</h2>

          <div class="m-meta">
            <div class="meta-row">
              <span class="label">时间：</span>
              <!-- 动态时间 -->
              <span class="val">{{ formatTime(scheduleInfo.startTime) }}</span>
            </div>
            <div class="meta-row">
              <span class="label">影厅：</span>
              <!-- 动态影厅 -->
              <span class="val">{{ scheduleInfo.hallName }} (国语 2D)</span>
            </div>
            <div class="meta-row">
              <span class="label">票价：</span>
              <!-- 动态价格 -->
              <span class="val price">￥{{ scheduleInfo.price }}/张</span>
            </div>
          </div>

          <el-divider border-style="dashed" />

          <div class="selected-list">
            <p class="section-label">已选座位 ({{ selectedSeats.length }}/6)</p>
            <div class="tags-box">
              <transition-group name="list">
                <div v-for="seat in selectedSeats" :key="seat" class="seat-tag">
                  <span class="tag-txt">{{ seat.split('-')[0] }}排{{ seat.split('-')[1] }}座</span>
                  <span
                    class="tag-close"
                    @click="handleSeatClick(seat.split('-')[0], seat.split('-')[1])"
                    >×</span
                  >
                </div>
              </transition-group>
              <div v-if="selectedSeats.length === 0" class="no-seat-tip">
                请在左侧点击座位进行选择
              </div>
            </div>
          </div>

          <div class="total-bar">
            <div class="text">
              <span>总计</span>
              <span class="calc">￥{{ totalPrice }}</span>
            </div>
            <button class="buy-btn" :disabled="selectedSeats.length === 0" @click="submitOrder">
              确认选座
            </button>
          </div>
        </div>
      </aside>
    </div>

    <div v-if="!loading && seatData.rows === 0" class="empty-state">
      <el-empty description="该场次座位图配置异常" />
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<style scoped>
/* 保持原有 scoped 样式不变 */
.seat-page {
  background-color: #121212;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #eee;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', sans-serif;
}

/* 顶部 Header */
.page-header {
  height: 60px;
  background: #1a1a1a;
  border-bottom: 1px solid #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  z-index: 10;
}
.header-left {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  color: #999;
  transition: color 0.3s;
}
.header-left:hover {
  color: #d4af37;
}
.header-center {
  font-size: 16px;
  font-weight: bold;
  color: #ddd;
}
.divider {
  margin: 0 10px;
  color: #555;
}
.header-right {
  width: 80px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  height: calc(100vh - 60px);
  overflow: hidden;
}

/* 左侧 影厅大区 */
.theater-section {
  flex: 1;
  background: #000;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  user-select: none;
}

/* 屏幕 */
.screen-container {
  margin-top: 40px;
  margin-bottom: 60px;
  text-align: center;
  width: 60%;
}
.screen-curve {
  height: 20px;
  background: linear-gradient(to bottom, #d4af37, transparent);
  border-radius: 50% 50% 0 0 / 100% 100% 0 0;
  box-shadow: 0 -5px 20px rgba(212, 175, 55, 0.4);
  opacity: 0.6;
  position: relative;
  margin-bottom: 10px;
}
.screen-text {
  font-size: 12px;
  color: #666;
  letter-spacing: 4px;
}

/* 座位滚动容器 */
.seats-wrapper {
  flex: 1;
  width: 100%;
  overflow: auto;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  scrollbar-width: thin;
  scrollbar-color: #333 #111;
}

/* Grid 布局 */
.seats-grid {
  display: grid;
  grid-template-columns: repeat(var(--cols), 44px);
  grid-template-rows: repeat(var(--rows), 44px);
  gap: 10px;
  padding: 40px;
  margin: 0 auto;
}

/* 座位单元格 */
.seat-cell {
  width: 44px;
  height: 44px;
  position: relative;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: transform 0.2s;
}
.seat-cell:hover {
  transform: scale(1.1);
}

.seat-shape {
  width: 34px;
  height: 30px;
  background-color: #3d3d3d;
  border-radius: 4px 4px 10px 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  border: 1px solid #555;
  transition: all 0.2s;
}

.tooltip-trigger {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 颜色状态 */
.is-available .seat-shape {
  background-color: #3d3d3d;
  border-color: #666;
}
.is-available:hover .seat-shape {
  background-color: #555;
  border-color: #aaa;
}

.is-selected .seat-shape {
  background-color: #d4af37 !important;
  border-color: #fff !important;
  box-shadow: 0 0 10px rgba(212, 175, 55, 0.6);
}

.is-sold {
  cursor: not-allowed !important;
  pointer-events: none;
}
.is-sold .seat-shape {
  background-color: #14a453 !important;
  border-color: #17986b !important;
  opacity: 0.8;
}

.is-broken {
  cursor: not-allowed !important;
  pointer-events: none;
}
.is-broken .seat-shape {
  background: transparent !important;
  border: 1px dashed #444 !important;
  box-shadow: none;
}
.is-broken::after {
  content: '×';
  position: absolute;
  color: #444;
  font-size: 20px;
}

/* 图例 */
.legend-bar {
  height: 60px;
  width: 100%;
  background: #151515;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 30px;
  border-top: 1px solid #222;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #888;
}
.dot {
  width: 16px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid transparent;
}
.type-available .dot {
  background: #3d3d3d;
  border-color: #555;
}
.type-selected .dot {
  background: #d4af37;
}
.type-sold .dot {
  background: #16a25c;
}
.type-broken .dot {
  border: 1px dashed #444;
}

/* 右侧侧边栏 */
.sidebar-info {
  width: 340px;
  background: #1a1a1a;
  border-left: 1px solid #333;
  display: flex;
  flex-direction: column;
  padding: 30px;
  position: relative;
  z-index: 5;
}

.m-title {
  font-size: 22px;
  color: #d4af37;
  margin-top: 0;
  margin-bottom: 20px;
  line-height: 1.4;
}

.m-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 14px;
  color: #aaa;
}
.meta-row {
  display: flex;
  justify-content: space-between;
}
.val.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.selected-list {
  flex: 1;
  margin-top: 20px;
}
.section-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 15px;
}
.tags-box {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
}
.no-seat-tip {
  color: #444;
  font-size: 13px;
  width: 100%;
  text-align: center;
  padding: 20px 0;
  border: 1px dashed #333;
  border-radius: 4px;
}

.seat-tag {
  background: #2b2b2b;
  border: 1px solid #d4af37;
  color: #d4af37;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  animation: fadeIn 0.3s;
}
.tag-close {
  cursor: pointer;
  font-size: 16px;
  opacity: 0.7;
}
.tag-close:hover {
  opacity: 1;
}

.total-bar {
  margin-top: auto;
}
.total-bar .text {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 15px;
  color: #eee;
}
.calc {
  font-size: 28px;
  color: #d4af37;
  font-weight: bold;
  font-family: 'Georgia', serif;
}

.buy-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(to right, #d4af37, #fbbd08);
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
  color: #000;
  cursor: pointer;
  transition: all 0.3s;
}
.buy-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(212, 175, 55, 0.4);
}
.buy-btn:disabled {
  background: #333;
  color: #666;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding-top: 100px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

<!-- 【修改点 2】新增不带 scoped 的全局样式，专门用于覆盖弹窗 -->
<style>
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

/* 确认按钮 */
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

/* 取消按钮 */
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
</style>