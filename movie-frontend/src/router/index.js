import { createRouter, createWebHistory } from 'vue-router'

// 1. 使用新的文件名导入
import AdminLayout from '@/views/AdminLayout.vue'
import DashboardView from '@/views/DashboardView.vue'
import UserLogin from '@/views/UserLogin.vue'
import UserRegister from '@/views/UserRegister.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: UserLogin, // 使用新组件
    },
    // --- 新增注册路由 ---
    {
      path: '/register',
      name: 'register',
      component: UserRegister,
    },
    {
      path: '/customer',
      component: () => import('@/views/customer/CustomerLayout.vue'),
      children: [
        {
          path: '/home', // 访问 /home 时显示书架
          name: 'CustomerHome',
          component: () => import('@/views/customer/HomeView.vue'),
        },
        {
          path: 'movie/:id', // 上一步的详情页
          name: 'MovieDetail',
          component: () => import('@/views/customer/MovieDetail.vue'),
          meta: { title: '电影详情' },
        },
        {
          path: 'seats/:scheduleId', // 本步的选座页
          name: 'ScheduleSeats',
          component: () => import('@/views/customer/ScheduleAndSeats.vue'),
          meta: { title: '选座购票', requiresAuth: true },
        },
        {
          path: 'pay/:orderNo', // 路径参数带上订单号
          name: 'OrderPay',
          component: () => import('@/views/customer/PaymentView.vue'),
          meta: { title: '订单支付', requiresAuth: true },
        },
        // 【新增】个人中心 (包含设置密码)
        {
          path: 'profile',
          name: 'UserProfile',
          component: () => import('@/views/customer/UserProfile.vue'),
          meta: { title: '个人中心', requiresAuth: true },
        },
        // 【新增】我的订单
        {
          path: 'my-orders',
          name: 'MyOrders',
          component: () => import('@/views/customer/MyOrders.vue'),
          meta: { title: '我的订单', requiresAuth: true },
        },
      ],
    },
    {
      path: '/',
      component: AdminLayout, // 使用新组件
      // 2. 给父路由加上 meta，标记需要登录
      meta: { requiresAuth: true },
      children: [
        {
          path: '', // 默认子路由
          name: 'home',
          component: DashboardView, // 使用新组件
          // 3. 子路由也可以加 meta
          meta: { title: '首页' },
        },
        {
          path: 'movie/list',
          name: 'movie-list',
          component: () => import('@/views/movie/MovieList.vue'),
          meta: { title: '电影管理' },
        },
        // --- 新增：订单管理 ---
        {
          path: 'order/list',
          name: 'order-list',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'cinema/list', // 对应 sidebar 的 index
          name: 'cinema-list',
          component: () => import('@/views/business/CinemaList.vue'),
          meta: { title: '影院管理' },
        },
        {
          path: 'actor/list',
          name: 'actor-list',
          component: () => import('@/views/content/ActorList.vue'),
          meta: { title: '演员管理' },
        },
        {
          path: 'director/list',
          name: 'director-list',
          component: () => import('@/views/content/DirectorList.vue'),
          meta: { title: '导演管理' },
        },
        {
          path: 'schedule/list',
          name: 'schedule-list',
          component: () => import('@/views/business/ScheduleList.vue'),
          meta: { title: '排片管理' },
        },
        {
          path: 'review/list',
          name: 'review-list',
          component: () => import('@/views/content/ReviewList.vue'),
          meta: { title: '影评管理' },
        },
        {
          path: 'system/log',
          name: 'system-log',
          component: () => import('@/views/system/SystemLog.vue'),
          meta: { title: '操作日志' },
        },
        {
          path: 'system/user',
          name: 'user-list',
          component: () => import('@/views/system/UserList.vue'),
          meta: { title: '用户管理' },
        },
      ],
    },
  ],
})

// 路由守卫 (保持不变)
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.matched.some((record) => record.meta.requiresAuth) && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
