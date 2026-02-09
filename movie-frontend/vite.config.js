import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // --- 新增：Vite 代理服务器配置 ---
  server: {
    proxy: {
      // 字符串简写写法：/foo -> http://localhost:4567/foo
      // '/foo': 'http://localhost:4567',
      // 带选项写法：/api -> http://localhost:8080/api
      '/api': {
        target: 'http://localhost:8080', // 你的后端地址
        changeOrigin: true, // 必须设置为 true
        rewrite: (path) => path.replace(/^\/api/, ''), // 去掉请求里的 /api
      },
    },
  },
})
