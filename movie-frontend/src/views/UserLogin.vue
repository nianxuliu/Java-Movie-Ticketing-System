<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { User, Lock, Key } from '@element-plus/icons-vue'

const username = ref('')
const password = ref('')
const verifyCode = ref('') // 用户输入的验证码
const loading = ref(false)
const router = useRouter()
const userStore = useUserStore()

// --- 验证码逻辑 Start ---
const verifyCanvas = ref(null)
const generatedCode = ref('')

// 生成随机数
const randomNum = (min, max) => Math.floor(Math.random() * (max - min) + min)
// 生成随机颜色 (偏亮色以便在深色背景显示)
const randomColor = (min, max) => {
  const r = randomNum(min, max)
  const g = randomNum(min, max)
  const b = randomNum(min, max)
  return `rgb(${r},${g},${b})`
}

// 绘制验证码
const drawCaptcha = () => {
  const canvas = verifyCanvas.value
  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height

  // 清空画布
  ctx.clearRect(0, 0, width, height)
  // 背景色 (深色系)
  ctx.fillStyle = '#222'
  ctx.fillRect(0, 0, width, height)

  // 生成文字
  let code = ''
  const chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678'
  for (let i = 0; i < 4; i++) {
    const char = chars[randomNum(0, chars.length)]
    code += char
    ctx.font = randomNum(25, 35) + 'px Georgia'
    ctx.fillStyle = '#d4af37' // 金色文字
    ctx.textBaseline = 'middle'
    // 随机旋转
    const x = (i + 1) * (width / 5)
    const y = height / 2
    const deg = randomNum(-30, 30)
    
    ctx.translate(x, y)
    ctx.rotate((deg * Math.PI) / 180)
    ctx.fillText(char, 0, 0)
    ctx.rotate((-deg * Math.PI) / 180)
    ctx.translate(-x, -y)
  }
  generatedCode.value = code

  // 绘制干扰线
  for (let i = 0; i < 3; i++) {
    ctx.strokeStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.moveTo(randomNum(0, width), randomNum(0, height))
    ctx.lineTo(randomNum(0, width), randomNum(0, height))
    ctx.stroke()
  }
  // 绘制干扰点
  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = randomColor(150, 255)
    ctx.beginPath()
    ctx.arc(randomNum(0, width), randomNum(0, height), 1, 0, 2 * Math.PI)
    ctx.fill()
  }
}
// --- 验证码逻辑 End ---

async function handleLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  // 1. 验证码校验
  if (!verifyCode.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (verifyCode.value.toLowerCase() !== generatedCode.value.toLowerCase()) {
    ElMessage.error('验证码错误，请重新输入')
    drawCaptcha() // 刷新验证码
    verifyCode.value = ''
    return
  }

  loading.value = true
  try {
    const data = await login({ username: username.value, password: password.value })
    localStorage.setItem('token', data.token)
    userStore.setRole(data.role)
    ElMessage.success('登录成功！')

    if (data.role === 'admin') {
      router.push('/')
    } else {
      router.push('/home')
    }
  } catch (error) {
    console.error('登录失败:', error)
    // 登录失败也刷新验证码
    drawCaptcha()
    verifyCode.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  drawCaptcha()
})
</script>

<template>
  <div class="login-container">
    <!-- 装饰背景层 -->
    <div class="vintage-overlay"></div>

    <div class="login-box">
      <div class="logo-area">
        <img src="https://img.icons8.com/color/96/000000/movie-projector.png" class="logo-icon" />
        <h1 class="title">VINTAGE CINEMA</h1>
        <p class="subtitle">Classic Movie Ticket System</p>
      </div>
      
      <el-form @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input 
            v-model="username" 
            placeholder="Username / 用户名" 
            size="large" 
            class="vintage-input"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="Password / 密码"
            size="large"
            show-password
            class="vintage-input"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 验证码区域 -->
        <el-form-item>
          <div class="captcha-row">
            <el-input 
              v-model="verifyCode" 
              placeholder="Code / 验证码" 
              size="large" 
              class="vintage-input captcha-input"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
            <canvas 
              ref="verifyCanvas" 
              width="120" 
              height="40" 
              class="captcha-canvas"
              @click="drawCaptcha"
              title="看不清？点击刷新"
            ></canvas>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            size="large"
            class="login-btn"
          >
            ENTER THEATRE / 登 录
          </el-button>
        </el-form-item>
        
        <div class="extra-links">
          <router-link to="/register">Create Account / 注册账号</router-link>
          <a>Forgot Password?</a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* 全局容器：黑金复古背景 */
.login-container {
  width: 100vw;
  height: 100vh;
  background-color: #0d0d0d;
  background-image: 
    linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.7)),
    url('@/assets/login-bg.jpg'); /* 如果没有图片，会显示黑色背景 */
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  font-family: 'Georgia', serif;
}

/* 登录框主体 */
.login-box {
  width: 420px;
  padding: 50px 40px;
  background: #1a1a1a;
  border: 1px solid #d4af37; /* 金色边框 */
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.8), 0 0 10px rgba(212, 175, 55, 0.2);
  position: relative;
  z-index: 2;
}

/* 标题区域 */
.logo-area {
  text-align: center;
  margin-bottom: 40px;
}
.logo-icon {
  width: 60px;
  filter: sepia(100%) hue-rotate(5deg) brightness(0.9); /* 调整图标色调为复古金 */
  margin-bottom: 10px;
}
.title {
  color: #d4af37;
  font-size: 28px;
  margin: 5px 0;
  letter-spacing: 2px;
  font-weight: bold;
  font-family: 'Copperplate', 'Times New Roman', serif;
}
.subtitle {
  color: #666;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 3px;
  margin-top: 5px;
}

/* 验证码布局 */
.captcha-row {
  display: flex;
  gap: 15px;
  width: 100%;
}
.captcha-input {
  flex: 1;
}
.captcha-canvas {
  border: 1px solid #444;
  cursor: pointer;
  border-radius: 4px;
  transition: border-color 0.3s;
}
.captcha-canvas:hover {
  border-color: #d4af37;
}

/* 链接区域 */
.extra-links {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  padding: 0 5px;
}
.extra-links a {
  color: #666;
  font-size: 12px;
  text-decoration: none;
  transition: 0.3s;
}
.extra-links a:hover {
  color: #d4af37;
  text-decoration: underline;
}

/* --- 深度定制 Element Plus 样式 (黑金风格) --- */

/* 输入框 */
:deep(.vintage-input .el-input__wrapper) {
  background-color: #000 !important;
  box-shadow: 0 0 0 1px #333 inset !important;
  border-radius: 4px;
  padding-left: 15px;
  transition: 0.3s;
}
:deep(.vintage-input .el-input__wrapper:hover),
:deep(.vintage-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #d4af37 inset !important; /* 聚焦变金 */
}
:deep(.vintage-input .el-input__inner) {
  color: #d4af37 !important;
  font-family: 'Georgia', serif;
  height: 45px;
}
:deep(.vintage-input .el-input__prefix-inner) {
  color: #666;
}

/* 登录按钮 */
.login-btn {
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
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(212, 175, 55, 0.3);
  background: linear-gradient(to right, #eec747, #d4af37) !important;
}
.login-btn:active {
  transform: translateY(0);
}

/* 去除默认的 input 自动填充背景色 */
:deep(input:-webkit-autofill) {
  -webkit-box-shadow: 0 0 0px 1000px #000 inset !important;
  -webkit-text-fill-color: #d4af37 !important;
  caret-color: #d4af37;
}
</style>