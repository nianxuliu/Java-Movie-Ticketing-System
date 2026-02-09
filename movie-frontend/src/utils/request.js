import axios from 'axios'
import { ElMessage } from 'element-plus' // 引入 Element Plus 的消息提示
import router from '@/router'

// 1. 创建 Axios 实例
const service = axios.create({
  // baseURL: 'http://localhost:8080', // 直接写死后端地址
  baseURL: '/api', // 使用代理，更灵活
  timeout: 60000, // 请求超时时间
})

// 2. 请求拦截器 (Request Interceptor)
//    作用：在每次发送请求前，都来这里检查一下，可以统一加上 Token
service.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      // 如果有 token，就在请求头里加上
      config.headers['authorization'] = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 3. 响应拦截器 (Response Interceptor)
//    作用：在每次接收到后端返回时，都来这里检查一下，可以统一处理错误
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 如果后端的 code 不是 200，就抛出错误
    if (res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000,
      })

      // 401: Token 无效;
      if (res.code === 401) {
        localStorage.removeItem('token')
        // 3. 跳转到登录页
        router.push('/login')
        console.log('登陆状态已过期，需要重新登录')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      // 如果 code 是 200，直接返回 data 部分
      return res.data
    }
  },
  (error) => {
    ElMessage({
      message: error.message,
      type: 'error',
      duration: 5 * 1000,
    })
    return Promise.reject(error)
  },
)

export default service
