<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone, MagicStick } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

// 表单数据
const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

// 校验规则：两次密码必须一致
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

// 校验手机号
const validatePhone = (rule, value, callback) => {
  const reg = /^1[3-9]\d{9}$/
  if (!reg.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
}

// 提交注册
const handleRegister = () => {
  if (!formRef.value) return

  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 构造发送给后端的数据 (后端不需要 confirmPassword)
        const submitData = {
          username: form.username,
          nickname: form.nickname,
          phone: form.phone,
          password: form.password,
        }

        await register(submitData)
        ElMessage.success('注册成功，请登录')

        // 注册成功跳转到登录页
        router.push('/login')
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}

// 返回登录页
const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="register-container">
    <!-- 装饰背景层 -->
    <div class="vintage-overlay"></div>

    <div class="register-box">
      <!-- 头部 Logo 区域 -->
      <div class="logo-area">
        <img src="https://img.icons8.com/color/96/000000/movie-projector.png" class="logo-icon" />
        <h1 class="title">JOIN THE CLUB</h1>
        <p class="subtitle">Create Your Vintage Account</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <!-- 账号 -->
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="Username / 账号"
            :prefix-icon="User"
            class="vintage-input"
          />
        </el-form-item>

        <!-- 昵称 -->
        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="Nickname / 昵称"
            :prefix-icon="MagicStick"
            class="vintage-input"
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="Phone Number / 手机号"
            :prefix-icon="Iphone"
            maxlength="11"
            class="vintage-input"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Password / 设置密码"
            :prefix-icon="Lock"
            show-password
            class="vintage-input"
          />
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Confirm Password / 确认密码"
            :prefix-icon="Lock"
            show-password
            class="vintage-input"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <!-- 按钮区域 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleRegister"
            class="register-btn"
          >
            CREATE ACCOUNT / 立即注册
          </el-button>
        </el-form-item>

        <div class="login-link">
          <span>Already have an account? </span>
          <span class="link-text" @click="goLogin">Go Login / 去登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* 全局容器：黑金复古背景 */
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #0d0d0d;
  background-image: linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.7)),
    url('@/assets/login-bg.jpg'); /* 保持背景图片一致 */
  background-size: cover;
  background-position: center;
  font-family: 'Georgia', serif;
  position: relative;
}

/* 注册框主体 */
.register-box {
  width: 450px;
  padding: 40px;
  background: #1a1a1a;
  border: 1px solid #d4af37; /* 金色边框 */
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.8), 0 0 10px rgba(212, 175, 55, 0.2);
  position: relative;
  z-index: 2;
  margin: 20px; /* 防止屏幕过小时贴边 */
}

/* 标题区域 */
.logo-area {
  text-align: center;
  margin-bottom: 30px;
}
.logo-icon {
  width: 50px;
  filter: sepia(100%) hue-rotate(5deg) brightness(0.9);
  margin-bottom: 10px;
}
.title {
  color: #d4af37;
  font-size: 24px;
  margin: 5px 0;
  letter-spacing: 2px;
  font-weight: bold;
  font-family: 'Copperplate', 'Times New Roman', serif;
}
.subtitle {
  color: #666;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 2px;
  margin-top: 5px;
}

/* --- Element Plus 样式覆写 (黑金风格) --- */

/* 输入框样式 */
:deep(.vintage-input .el-input__wrapper) {
  background-color: #000 !important;
  box-shadow: 0 0 0 1px #333 inset !important;
  border-radius: 4px;
  padding-left: 15px;
  transition: 0.3s;
}
:deep(.vintage-input .el-input__wrapper:hover),
:deep(.vintage-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #d4af37 inset !important;
}
:deep(.vintage-input .el-input__inner) {
  color: #d4af37 !important;
  font-family: 'Georgia', serif;
  height: 45px;
}
:deep(.vintage-input .el-input__prefix-inner) {
  color: #666;
}

/* 校验错误提示文字颜色 */
:deep(.el-form-item__error) {
  color: #b91c1c; /* 暗红色，在深色背景下更易读 */
  padding-top: 4px;
}

/* 注册按钮 */
.register-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(to right, #d4af37, #bfa15f) !important;
  border: none !important;
  color: #000 !important;
  font-weight: bold;
  font-size: 16px;
  letter-spacing: 1px;
  margin-top: 10px;
  transition: all 0.3s;
}
.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(212, 175, 55, 0.3);
  background: linear-gradient(to right, #eec747, #d4af37) !important;
}

/* 底部链接 */
.login-link {
  text-align: center;
  font-size: 13px;
  color: #666;
  margin-top: 10px;
}
.link-text {
  color: #d4af37;
  cursor: pointer;
  text-decoration: none;
  font-weight: bold;
  margin-left: 5px;
  transition: 0.3s;
}
.link-text:hover {
  text-decoration: underline;
  color: #eec747;
}

/* 自动填充样式覆盖 */
:deep(input:-webkit-autofill) {
  -webkit-box-shadow: 0 0 0px 1000px #000 inset !important;
  -webkit-text-fill-color: #d4af37 !important;
  caret-color: #d4af37;
}
</style>