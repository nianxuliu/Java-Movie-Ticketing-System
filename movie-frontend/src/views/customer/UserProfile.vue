<!-- eslint-disable no-empty -->
<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { getWalletInfo, setPayPassword, changePayPassword, rechargeWallet } from '@/api/userWallet'
// 引入刚才确认的 API
import { login, updateUserInfo, getInfo, updateLoginPassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, Wallet, Edit, Plus, Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('info')

const currentUsername = ref('')
const wallet = ref({ balance: 0 })
const rechargeDialog = ref(false)
const rechargeAmount = ref(100)
const loading = ref(false)

// 密码弹窗控制
const payPwdDialog = ref(false)
const isSetPayPwdMode = ref(true)
const payPwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const loginPwdDialog = ref(false)
const loginPwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 个人资料
const userInfoForm = reactive({ nickname: '', phone: '', email: '', avatarUrl: '' })
const verifyDialog = ref(false)
const verifyPassword = ref('')
const isSavingInfo = ref(false)

// 上传相关
const uploadHeaders = { Authorization: localStorage.getItem('token') }
const uploadActionUrl = import.meta.env.VITE_API_URL
  ? `${import.meta.env.VITE_API_URL}/file/upload`
  : '/api/file/upload'

// 初始化数据
const initData = async () => {
  loading.value = true
  try {
    // 获取个人信息
    const uRes = await getInfo()
    const uData = uRes.data || uRes
    if (uData) {
      currentUsername.value = uData.username
      Object.assign(userInfoForm, {
        nickname: uData.nickname,
        phone: uData.phone,
        email: uData.email,
        avatarUrl: uData.avatarUrl,
      })
      // 更新store显示（如果有这个方法的话）
      if (userStore.setUserInfo) userStore.setUserInfo(uData)
    }

    // 获取钱包
    const wRes = await getWalletInfo()
    let val = 0
    if (wRes?.data?.data?.balance !== undefined) val = wRes.data.data.balance
    else if (wRes?.data?.balance !== undefined) val = wRes.data.balance
    else if (wRes?.balance !== undefined) val = wRes.balance
    wallet.value = { balance: Number(val) }
  } catch (e) {
    console.error('Data Load Error', e)
  } finally {
    loading.value = false
  }
}

// 头像上传成功
const handleAvatarSuccess = (response) => {
  const url = response.data || response
  userInfoForm.avatarUrl = url
  if (userStore.setUserInfo) userStore.setUserInfo({ ...userStore.$state, avatarUrl: url })
  ElMessage.success('头像上传成功，请点击“保存修改”以持久化')
}
const beforeAvatarUpload = (rawFile) => {
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 修改个人资料 (此处保持你原有逻辑)
const onSaveProfileClick = () => {
  verifyPassword.value = ''
  verifyDialog.value = true
}
const confirmSaveProfile = async () => {
  if (!verifyPassword.value) return ElMessage.warning('请输入登录密码')
  isSavingInfo.value = true
  try {
    await login({ username: currentUsername.value, password: verifyPassword.value })
    await updateUserInfo({ ...userInfoForm, id: userStore.userId })
    if (userStore.setUserInfo) {
      userStore.setUserInfo({
        ...userStore.$state,
        nickname: userInfoForm.nickname,
        avatarUrl: userInfoForm.avatarUrl,
      })
    }
    ElMessage.success('修改成功')
    verifyDialog.value = false
    await initData()
  } catch (e) {
    // 资料修改验证失败走这里
  } finally {
    isSavingInfo.value = false
  }
}

// =======================================================
// 【核心修改部分】修改登录密码逻辑
// =======================================================
const openLoginPwdDialog = () => {
  loginPwdForm.oldPassword = ''
  loginPwdForm.newPassword = ''
  loginPwdForm.confirmPassword = ''
  loginPwdDialog.value = true
}

const submitLoginPwd = async () => {
  // 1. 前端校验：两次新密码是否一致
  if (loginPwdForm.newPassword !== loginPwdForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  // 2. 简单的长度校验
  if (!loginPwdForm.oldPassword || !loginPwdForm.newPassword) {
    return ElMessage.warning('请输入原密码和新密码')
  }

  try {
    // 3. 直接调用后端接口
    // 后端 updatePassword 方法会接收 oldPassword 和 newPassword 并进行比对
    await updateLoginPassword({
      oldPassword: loginPwdForm.oldPassword,
      newPassword: loginPwdForm.newPassword,
    })

    // 4. 成功后处理
    ElMessage.success('密码修改成功，请重新登录')
    loginPwdDialog.value = false

    // 登出逻辑
    if (userStore.clearUserSession) {
      userStore.clearUserSession()
    } else {
      localStorage.clear()
    }

    // 延迟跳转
    setTimeout(() => {
      window.location.href = '/login'
    }, 1000)
  } catch (e) {
    // 5. 如果原密码错误，后端返回 Result.error("原密码错误")
    // request.js 拦截器通常会弹出这个错误消息
    console.error(e)
  }
}
// =======================================================

// 支付密码相关
const openPayPwdDialog = (mode) => {
  isSetPayPwdMode.value = mode === 'set'
  payPwdForm.oldPassword = ''
  payPwdForm.newPassword = ''
  payPwdForm.confirmPassword = ''
  payPwdDialog.value = true
}
const submitPayPwd = async () => {
  if (payPwdForm.newPassword.length !== 6 || isNaN(payPwdForm.newPassword))
    return ElMessage.warning('6位数字')
  try {
    if (isSetPayPwdMode.value) await setPayPassword({ payPassword: payPwdForm.newPassword })
    else await changePayPassword(payPwdForm)
    ElMessage.success('操作成功')
    payPwdDialog.value = false
  } catch (e) {}
}

const handleRecharge = async () => {
  try {
    await rechargeWallet({ amount: rechargeAmount.value })
    ElMessage.success('充值成功')
    rechargeDialog.value = false
    await initData()
  } catch (e) {}
}

onMounted(() => {
  initData()
})
</script>

<template>
  <div class="profile-page">
    <div class="main-card">
      <div class="card-header">
        <div class="avatar-wrapper">
          <el-avatar
            :size="80"
            :src="userInfoForm.avatarUrl || userStore.avatarUrl"
            class="user-avatar"
          >
            {{ userStore.nickname ? userStore.nickname.charAt(0).toUpperCase() : 'U' }}
          </el-avatar>
          <div class="camera-icon">
            <el-icon><Edit /></el-icon>
          </div>
        </div>
        <div class="header-info">
          <h2 class="nickname">{{ userInfoForm.nickname || '未设置昵称' }}</h2>
          <p class="username">@{{ currentUsername }}</p>
          <div class="tags">
            <span class="vip-tag">VINTAGE MEMBER</span>
          </div>
        </div>
      </div>

      <div class="divider-gold"></div>

      <el-tabs v-model="activeTab" class="retro-tabs center-tabs">
        <el-tab-pane name="info">
          <template #label
            ><span class="tab-label"
              ><el-icon><User /></el-icon> 个人资料</span
            ></template
          >

          <div class="form-container">
            <el-form label-position="top" class="vintage-form">
              <div class="upload-section">
                <el-upload
                  class="avatar-uploader"
                  :action="uploadActionUrl"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
                >
                  <div class="upload-box">
                    <el-icon class="upload-icon"><Plus /></el-icon><span>更换头像</span>
                  </div>
                </el-upload>
              </div>

              <div class="input-grid">
                <el-form-item label="昵称"
                  ><el-input v-model="userInfoForm.nickname"
                /></el-form-item>
                <el-form-item label="手机号"
                  ><el-input v-model="userInfoForm.phone"
                /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="userInfoForm.email" /></el-form-item>
              </div>

              <div class="actions-row">
                <el-button type="warning" class="retro-btn" @click="onSaveProfileClick"
                  >保存修改</el-button
                >
                <el-button type="danger" link class="link-btn" @click="openLoginPwdDialog"
                  ><el-icon><Lock /></el-icon> 修改登录密码</el-button
                >
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane name="wallet">
          <template #label
            ><span class="tab-label"
              ><el-icon><Wallet /></el-icon> 钱包 & 安全</span
            ></template
          >
          <div class="wallet-container">
            <div class="balance-display">
              <div class="label">ACCOUNT BALANCE</div>
              <div class="number">￥{{ Number(wallet.balance).toFixed(2) }}</div>
              <el-button class="recharge-btn" @click="rechargeDialog = true">立即充值</el-button>
            </div>
            <div class="security-list">
              <div class="sec-row">
                <div class="sec-left">
                  <div class="sec-title">支付密码</div>
                  <div class="sec-desc">用于购票支付时的安全验证 (6位数字)</div>
                </div>
                <div class="sec-right">
                  <el-button link class="gold-link" @click="openPayPwdDialog('set')"
                    >重置</el-button
                  >
                  <span class="sep">|</span>
                  <el-button link class="gold-link" @click="openPayPwdDialog('change')"
                    >修改</el-button
                  >
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 修改登录密码弹窗 -->
    <el-dialog
      v-model="loginPwdDialog"
      title="修改登录密码"
      width="400px"
      class="retro-dialog"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="loginPwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="loginPwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="loginPwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="loginPwdDialog = false">取消</el-button>
        <el-button type="warning" @click="submitLoginPwd">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 其他弹窗保持不变 -->
    <el-dialog
      v-model="verifyDialog"
      title="身份验证"
      width="350px"
      class="retro-dialog"
      append-to-body
    >
      <p style="color: #888; margin-bottom: 15px">请输入登录密码以确认修改：</p>
      <el-input v-model="verifyPassword" type="password" show-password />
      <template #footer>
        <el-button @click="verifyDialog = false">取消</el-button>
        <el-button type="warning" @click="confirmSaveProfile" :loading="isSavingInfo"
          >确认</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="payPwdDialog"
      :title="isSetPayPwdMode ? '设置支付密码' : '修改支付密码'"
      width="400px"
      class="retro-dialog"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="旧密码" v-if="!isSetPayPwdMode"
          ><el-input v-model="payPwdForm.oldPassword" type="password" maxlength="6" show-password
        /></el-form-item>
        <el-form-item label="新密码"
          ><el-input v-model="payPwdForm.newPassword" type="password" maxlength="6" show-password
        /></el-form-item>
        <el-form-item label="确认"
          ><el-input
            v-model="payPwdForm.confirmPassword"
            type="password"
            maxlength="6"
            show-password
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payPwdDialog = false">取消</el-button>
        <el-button type="warning" @click="submitPayPwd">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="rechargeDialog"
      title="余额充值"
      width="350px"
      class="retro-dialog"
      append-to-body
    >
      <div style="text-align: center; margin: 20px 0">
        <el-input-number v-model="rechargeAmount" :min="1" :step="100" size="large" />
      </div>
      <template #footer>
        <el-button @click="rechargeDialog = false">取消</el-button>
        <el-button type="warning" @click="handleRecharge">充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 样式与你之前保持一致 */
.profile-page {
  min-height: calc(100vh - 70px);
  background: #121212;
  padding: 40px 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}
.main-card {
  width: 100%;
  max-width: 800px;
  background: #1a1a1a;
  border: 1px solid #333;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  position: relative;
}
.card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 30px;
}
.avatar-wrapper {
  position: relative;
  margin-bottom: 15px;
}
.camera-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #d4af37;
  color: #000;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  border: 2px solid #1a1a1a;
}
.nickname {
  color: #d4af37;
  margin: 0 0 5px 0;
  font-size: 24px;
}
.username {
  color: #666;
  margin: 0 0 10px 0;
  font-size: 14px;
}
.vip-tag {
  background: #222;
  color: #d4af37;
  border: 1px solid #d4af37;
  padding: 2px 10px;
  font-size: 10px;
  border-radius: 10px;
  letter-spacing: 1px;
}
.divider-gold {
  height: 1px;
  background: linear-gradient(90deg, transparent, #333, transparent);
  margin-bottom: 30px;
}
:deep(.center-tabs .el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
}
:deep(.el-tabs__item) {
  color: #666;
  font-size: 16px;
}
:deep(.el-tabs__item.is-active) {
  color: #d4af37;
  font-weight: bold;
}
:deep(.el-tabs__active-bar) {
  background-color: #d4af37;
}
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #333;
}
.form-container {
  max-width: 500px;
  margin: 30px auto 0;
}
.upload-section {
  text-align: center;
  margin-bottom: 30px;
}
.upload-box {
  border: 1px dashed #444;
  padding: 10px 20px;
  border-radius: 4px;
  color: #666;
  font-size: 12px;
  transition: 0.3s;
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}
.upload-box:hover {
  border-color: #d4af37;
  color: #d4af37;
}
.input-grid {
  display: grid;
  gap: 20px;
}
.actions-row {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}
.retro-btn {
  width: 200px;
  height: 45px;
  font-weight: bold;
  background: #d4af37 !important;
  border-color: #d4af37 !important;
  color: #000 !important;
}
.link-btn {
  color: #666;
  font-size: 12px;
}
.link-btn:hover {
  color: #d4af37;
}
.wallet-container {
  max-width: 500px;
  margin: 40px auto 0;
  text-align: center;
}
.balance-display {
  background: #111;
  border: 1px solid #333;
  padding: 30px;
  border-radius: 8px;
  margin-bottom: 40px;
}
.balance-display .label {
  color: #666;
  font-size: 12px;
  letter-spacing: 1px;
  margin-bottom: 10px;
}
.balance-display .number {
  color: #d4af37;
  font-size: 48px;
  font-family: 'Georgia', serif;
  margin-bottom: 20px;
}
.recharge-btn {
  background: transparent;
  border: 1px solid #d4af37;
  color: #d4af37;
  padding: 8px 30px;
  transition: 0.3s;
  cursor: pointer;
}
.recharge-btn:hover {
  background: #d4af37;
  color: #000;
}
.sec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  border-top: 1px dashed #333;
  text-align: left;
}
.sec-title {
  color: #eee;
  margin-bottom: 5px;
}
.sec-desc {
  color: #555;
  font-size: 12px;
}
.gold-link {
  color: #d4af37;
}
.gold-link:hover {
  color: #fff;
}
.sep {
  color: #333;
  margin: 0 5px;
}
:deep(.el-input__wrapper) {
  background-color: #111;
  box-shadow: 0 0 0 1px #333 inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #d4af37 inset !important;
}
:deep(.el-input__inner) {
  color: #eee;
}
:deep(.el-form-item__label) {
  color: #888;
}
</style>

<style>
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
