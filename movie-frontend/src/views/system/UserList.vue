<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Search, User, Lock, Unlock } from '@element-plus/icons-vue'

const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({ page: 1, size: 10, username: '' })

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
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

// 切换状态 (封号/解封)
const handleStatusChange = async (row) => {
  // 注意：v-model 绑定的 row.status 在触发 change 事件时已经是变化后的值了
  // true = 正常(1), false = 禁用(0)
  const actionText = row.status ? '解封' : '封禁'
  const newStatusInt = row.status ? 1 : 0

  try {
    // 乐观更新：先发请求
    await updateUserStatus(row.id, newStatusInt)
    ElMessage.success(`用户 ${row.username} 已${actionText}`)
  } catch (e) {
    // 失败回滚：如果请求失败，把开关状态切回去
    row.status = !row.status
    console.error(e)
  }
}

// 格式化金额
const formatMoney = (val) => {
  return val ? '￥' + Number(val).toFixed(2) : '￥0.00'
}

onMounted(() => getList())
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 搜索 -->
    <el-card shadow="never" class="mb-20">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="queryParams.username"
            placeholder="输入用户名搜索"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column label="头像" width="80" align="center">
        <template #default="{ row }">
          <el-avatar :size="40" :src="row.avatarUrl" :icon="User" />
        </template>
      </el-table-column>

      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="phone" label="手机号" width="120" />

      <!-- 如果后端没有返回余额字段，这一列可以根据实际情况保留或删除 -->
      <!-- 
      <el-table-column prop="balance" label="余额" width="100">
         <template #default="{ row }">{{ formatMoney(row.balance) }}</template>
      </el-table-column> 
      -->

      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <!-- 核心：状态开关 -->
          <el-switch
            v-model="row.status"
            inline-prompt
            active-text="正常"
            inactive-text="封禁"
            :active-icon="Unlock"
            :inactive-icon="Lock"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="注册时间" width="170" align="center">
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
