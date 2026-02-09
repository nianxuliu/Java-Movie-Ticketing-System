<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogList } from '@/api/log'
import { Search } from '@element-plus/icons-vue'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({ page: 1, size: 10, keyword: '' })

const getList = async () => {
  loading.value = true
  try {
    const res = await getLogList(queryParams)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  getList()
}

// 耗时颜色高亮：执行时间越长，颜色越醒目
const getTimeColor = (time) => {
  if (time < 200) return 'success' // 绿色 (快)
  if (time < 1000) return 'warning' // 黄色 (中)
  return 'danger' // 红色 (慢)
}

onMounted(() => getList())
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-20">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="搜索日志">
          <el-input
            v-model="queryParams.keyword"
            placeholder="操作人/操作内容..."
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="action" label="操作内容" min-width="200" show-overflow-tooltip />

      <el-table-column prop="time" label="耗时(ms)" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTimeColor(row.time)" size="small">{{ row.time }}ms</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="操作时间" width="180" align="center">
        <template #default="{ row }">{{ row.createTime?.replace('T', ' ') }}</template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 15px; text-align: right">
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

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
</style>
