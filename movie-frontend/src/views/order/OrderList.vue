<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminOrderList } from '@/api/order'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 10,
  orderNo: '',
})

// 状态字典
const statusMap = {
  0: { text: '待支付', type: 'info' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已取消', type: 'danger' },
  3: { text: '已退款', type: 'warning' },
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getAdminOrderList(queryParams)
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  getList()
}

// 格式化时间 (去掉T)
const formatTime = (row, column, cellValue) => {
  if (!cellValue) return ''
  return cellValue.replace('T', ' ')
}

onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 搜索栏 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="输入完整订单号"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="orderNo" label="订单编号" width="220" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="100" align="center" />
      <el-table-column prop="scheduleId" label="排片ID" width="100" align="center" />

      <el-table-column label="座位信息" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag
            v-for="seat in row.seatInfo.split(',')"
            :key="seat"
            size="small"
            style="margin-right: 5px"
          >
            {{ seat }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="总金额" width="120" align="center">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: bold">￥{{ row.totalPrice }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'" effect="dark">
            {{ statusMap[row.status]?.text || '未知' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="createTime"
        label="下单时间"
        width="170"
        align="center"
        :formatter="formatTime"
      />
      <el-table-column prop="payTime" label="支付时间" width="170" align="center">
        <template #default="{ row }">
          {{ row.payTime ? row.payTime.replace('T', ' ') : '-' }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 20px; text-align: right">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="getList"
      />
    </div>
  </div>
</template>
