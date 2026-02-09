import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

// --- Element Plus 引入 ---
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// --- -------------------- ---

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus) // 全局注册

app.mount('#app')
