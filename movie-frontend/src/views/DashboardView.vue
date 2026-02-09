<script setup>
import { ref, onMounted } from 'vue'
import { getReportData } from '@/api/report'
import request from '@/utils/request' // 直接用 request 调一下 top10，省的去 api 文件加
import * as echarts from 'echarts'
import { DataLine, User, Ticket, Money } from '@element-plus/icons-vue'

// 1. 统计数据
const stats = ref({
  todayBoxOffice: 0,
  totalBoxOffice: 0,
  totalUsers: 0,
  totalOrders: 0,
})

// 2. 加载统计数据
const loadData = async () => {
  try {
    const res = await getReportData()
    if (res) stats.value = res
  } catch (e) {
    console.error(e)
  }
}

// 3. 加载 Top10 并渲染真实图表
const initChart = async () => {
  const chartDom = document.getElementById('main-chart')
  if (!chartDom) return
  const myChart = echarts.init(chartDom)

  // 显示 Loading
  myChart.showLoading()

  try {
    // 调用真实的 Top10 接口
    const res = await request.get('/movie/top20')
    const movies = res.data || [] // 假设返回的是 List<Info>

    // 提取数据：X轴是名字，Y轴是评分
    const xData = movies.map((m) => m.title)
    const yData = movies.map((m) => m.rating)

    const option = {
      title: { text: '热门电影评分 Top 20 (实时)', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { interval: 0, rotate: 30 }, // 名字太长斜着显示
      },
      yAxis: { type: 'value', max: 10 }, // 评分满分10
      series: [
        {
          name: '评分',
          type: 'bar',
          data: yData,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' },
            ]),
          },
          barWidth: '40%',
        },
      ],
    }
    myChart.hideLoading()
    myChart.setOption(option)

    window.addEventListener('resize', () => myChart.resize())
  } catch (error) {
    console.error('图表数据加载失败', error)
    myChart.hideLoading()
  }
}

onMounted(async () => {
  await loadData()
  initChart()
})
</script>

<template>
  <div class="dashboard-container">
    <!-- 顶部卡片 (保持不变) -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <div class="card-header">
            <span>今日票房</span>
            <el-icon class="icon" style="background: #e6f7ff; color: #1890ff"><DataLine /></el-icon>
          </div>
          <div class="card-num">￥{{ stats.todayBoxOffice }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <div class="card-header">
            <span>总票房</span>
            <el-icon class="icon" style="background: #fff7e6; color: #fa8c16"><Money /></el-icon>
          </div>
          <div class="card-num">￥{{ stats.totalBoxOffice }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <div class="card-header">
            <span>总订单数</span>
            <el-icon class="icon" style="background: #f6ffed; color: #52c41a"><Ticket /></el-icon>
          </div>
          <div class="card-num">{{ stats.totalOrders }} 单</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card">
          <div class="card-header">
            <span>注册用户</span>
            <el-icon class="icon" style="background: #fff0f6; color: #eb2f96"><User /></el-icon>
          </div>
          <div class="card-num">{{ stats.totalUsers }} 人</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.data-card {
  height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 14px;
}
.card-header .icon {
  padding: 10px;
  border-radius: 8px;
  font-size: 20px;
}
.card-num {
  font-size: 28px;
  font-weight: bold;
  margin-top: 15px;
  color: #303133;
}
</style>
