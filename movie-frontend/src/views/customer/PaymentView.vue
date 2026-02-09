<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Lock, WarningFilled, ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo

const order = ref(null)
const loading = ref(true)
const payPassword = ref('')
const submittig = ref(false)

// 加载订单
const loadOrder = async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(orderNo)
    order.value = res.data || res
  } catch (e) {
    console.error('获取订单失败', e)
    ElMessage.error('订单加载失败，请检查网络或订单号')
  } finally {
    loading.value = false
  }
}

// 支付逻辑
const handlePay = async () => {
  if (!payPassword.value) return ElMessage.warning('请输入支付密码')

  submittig.value = true
  try {
    await payOrder({
      orderNo: orderNo,
      payPassword: payPassword.value,
    })

    // 【UI修改点】使用自定义样式的弹窗
    ElMessageBox.alert('支付成功！电影票已存入您的账户', '购票成功', {
      confirmButtonText: '查看我的订单',
      type: 'success',
      // 关键：添加自定义类名，用于覆盖默认白色样式
      customClass: 'retro-message-box',
      // 锁定滚动，防止背景滑动
      lockScroll: true,
      callback: () => {
        router.push('/customer/my-orders')
      },
    })
  } catch (e) {
    // 错误拦截
  } finally {
    submittig.value = false
  }
}

// 格式化时间
const formatTime = (t) => (t ? t.replace('T', ' ').substring(0, 16) : '')

onMounted(() => loadOrder())
</script>

<template>
  <div
    class="pay-page"
    v-loading="loading"
    element-loading-background="#0c0c0c"
    element-loading-text="正在加载订单..."
  >
    <div class="nav-back" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon> 返回
    </div>

    <div class="pay-container" v-if="order">
      <!-- 左侧：复古票根 -->
      <div class="ticket-stub">
        <div class="ticket-header">VINTAGE CINEMA TICKET</div>

        <div class="ticket-body">
          <h2 class="movie-name">{{ order.movieTitle || '加载中...' }}</h2>

          <div class="info-row">
            <div class="info-item">
              <span class="label">DATE / 日期</span>
              <span class="value">{{ formatTime(order.startTime).split(' ')[0] }}</span>
            </div>
            <div class="info-item">
              <span class="label">TIME / 时间</span>
              <span class="value">{{ formatTime(order.startTime).split(' ')[1] }}</span>
            </div>
          </div>

          <div class="info-row">
            <div class="info-item">
              <span class="label">HALL / 影厅</span>
              <span class="value">{{ order.hallName }}</span>
            </div>
          </div>

          <div class="seats-box">
            <span class="label">SEATS / 座位</span>
            <div class="seat-tags">
              <span
                v-for="s in order.seatInfo ? order.seatInfo.split(',') : []"
                :key="s"
                class="seat-tag"
              >
                {{ s.split('-')[0] }}排{{ s.split('-')[1] }}座
              </span>
            </div>
          </div>

          <div class="price-box">
            <span class="label">TOTAL AMOUNT</span>
            <span class="amount">￥{{ order.totalPrice }}</span>
          </div>
        </div>

        <div class="ticket-footer">
          <div class="barcode"></div>
          <p class="order-id">ORDER ID: {{ order.orderNo }}</p>
        </div>

        <div class="perforation top"></div>
        <div class="perforation bottom"></div>
      </div>

      <!-- 右侧：支付面板 -->
      <div class="pay-panel">
        <div class="panel-header">
          <el-icon><Wallet /></el-icon>
          <span>收银台</span>
        </div>

        <div class="pay-summary">
          <p class="pay-label">待支付金额</p>
          <h1 class="pay-amount">￥{{ order.totalPrice }}</h1>
        </div>

        <el-form label-position="top" class="pay-form">
          <el-form-item label="请输入 6 位支付密码">
            <el-input
              v-model="payPassword"
              type="password"
              placeholder="******"
              show-password
              maxlength="6"
              size="large"
              class="retro-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="warning" class="pay-btn" @click="handlePay" :loading="submittig">
            确认支付
          </el-button>
        </el-form>

        <div class="pay-tips">
          <p>
            <el-icon><WarningFilled /></el-icon> 提示：订单锁座时长为 15 分钟，请尽快完成支付。
          </p>
          <p class="pwd-link" @click="router.push('/customer/profile')">忘记支付密码？</p>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="empty-error">
      <el-empty description="订单信息加载失败或订单不存在" />
      <el-button @click="router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
/* 保持原有布局样式不变 */
.pay-page {
  min-height: 100vh;
  background-color: #0c0c0c;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  position: relative;
}

.nav-back {
  position: absolute;
  top: 40px;
  left: 40px;
  color: #888;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  z-index: 10;
}
.nav-back:hover {
  color: #d4af37;
}

.pay-container {
  display: flex;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
  max-width: 900px;
  width: 100%;
}

/* 票根样式 */
.ticket-stub {
  flex: 1;
  background: #e2d1b0;
  color: #2c1e11;
  padding: 40px;
  position: relative;
  border-right: 2px dashed rgba(0, 0, 0, 0.2);
  font-family: 'Courier New', Courier, monospace;
}

.ticket-header {
  text-align: center;
  border-bottom: 2px solid #2c1e11;
  padding-bottom: 15px;
  font-weight: bold;
  letter-spacing: 3px;
  margin-bottom: 30px;
}

.movie-name {
  font-size: 28px;
  margin: 0 0 30px 0;
  border-left: 8px solid #2c1e11;
  padding-left: 15px;
  line-height: 1.2;
}

.info-row {
  display: flex;
  gap: 40px;
  margin-bottom: 25px;
}
.info-item {
  display: flex;
  flex-direction: column;
}
.label {
  font-size: 10px;
  color: #7a634a;
  font-weight: bold;
  margin-bottom: 5px;
  text-transform: uppercase;
}
.value {
  font-size: 16px;
  font-weight: bold;
}

.seat-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.seat-tag {
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid #2c1e11;
  padding: 2px 8px;
  font-size: 14px;
  font-weight: bold;
}

.price-box {
  margin-top: 40px;
  text-align: right;
}
.amount {
  font-size: 32px;
  color: #d32f2f;
  font-weight: bold;
  display: block;
}

.ticket-footer {
  margin-top: 50px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 20px;
}
.barcode {
  height: 40px;
  background: linear-gradient(
    to right,
    #000 2px,
    transparent 2px,
    #000 4px,
    transparent 1px,
    #000 8px
  );
  background-size: 15px 100%;
}
.order-id {
  font-size: 10px;
  color: #888;
  margin-top: 10px;
}

.perforation {
  position: absolute;
  right: -15px;
  width: 30px;
  height: 30px;
  background: #0c0c0c;
  border-radius: 50%;
}
.perforation.top {
  top: 20%;
}
.perforation.bottom {
  bottom: 20%;
}

/* 支付面板样式 */
.pay-panel {
  width: 400px;
  background: #1a1a1a;
  padding: 40px;
  border: 1px solid #d4af37;
  color: #eee;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #d4af37;
  margin-bottom: 40px;
  font-size: 18px;
}

.pay-summary {
  text-align: center;
  margin-bottom: 40px;
}
.pay-label {
  color: #888;
  font-size: 14px;
  margin-bottom: 10px;
}
.pay-amount {
  font-size: 48px;
  color: #d4af37;
  margin: 0;
}

.pay-form {
  margin-bottom: 30px;
}
:deep(.el-form-item__label) {
  color: #aaa !important;
}

.retro-input :deep(.el-input__wrapper) {
  background-color: #222 !important;
  box-shadow: none !important;
  border: 1px solid #444;
}
.retro-input :deep(input) {
  color: #d4af37 !important;
  font-size: 24px;
  letter-spacing: 10px;
  text-align: center;
}

.pay-btn {
  width: 100%;
  height: 55px;
  font-size: 18px;
  font-weight: bold;
  background: #d4af37 !important;
  color: #000 !important;
  border: none !important;
  margin-top: 20px;
}
.pay-tips {
  font-size: 12px;
  color: #555;
  line-height: 1.6;
}
.pwd-link {
  color: #666;
  cursor: pointer;
  text-decoration: underline;
  margin-top: 10px;
}
.pwd-link:hover {
  color: #d4af37;
}

.empty-error {
  text-align: center;
}
</style>

<!-- 
  【重点修改】
  这里是全局样式，专门用于修改 Element Plus 挂载在 body 上的弹窗。
  retro-message-box 类名对应 JS 中的 customClass
-->
<style>
/* 1. 弹窗整体背景和边框 */
.retro-message-box {
  background-color: #1a1a1a !important;
  border: 1px solid #d4af37 !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.9) !important;
  border-radius: 4px !important;
  padding-bottom: 20px !important;
  width: 420px !important;
}

/* 2. 标题颜色 */
.retro-message-box .el-message-box__title {
  color: #d4af37 !important;
  font-weight: bold;
  font-size: 18px;
}

/* 3. 内容文本颜色 */
.retro-message-box .el-message-box__message {
  color: #ccc !important;
  font-size: 15px;
}

/* 4. 关闭按钮 (X) */
.retro-message-box .el-message-box__headerbtn .el-message-box__close {
  color: #666 !important;
}
.retro-message-box .el-message-box__headerbtn .el-message-box__close:hover {
  color: #d4af37 !important;
}

/* 5. 成功图标颜色 (原绿色改为金色) */
.retro-message-box .el-message-box__status.el-icon-success {
  color: #d4af37 !important;
}

/* 6. 确认按钮 (Primary Button) */
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

/* 7. 取消/稍后支付按钮 (Default Button) - 用于下单成功的弹窗 */
.retro-message-box .el-button--default {
  background-color: transparent !important;
  border: 1px solid #555 !important;
  color: #aaa !important;
}
.retro-message-box .el-button--default:hover {
  border-color: #d4af37 !important;
  color: #d4af37 !important;
}
</style>