<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminReviewList, deleteReview, getReplyList, deleteReply } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotSquare, Delete, Search } from '@element-plus/icons-vue'

// --- 影评列表数据 ---
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({ page: 1, size: 10, keyword: '' })

// --- 回复弹窗数据 ---
const replyDialog = reactive({ visible: false, title: '', reviewId: null })
const replyList = ref([])
const replyLoading = ref(false)
const replyParams = reactive({ page: 1, size: 10 })
const replyTotal = ref(0)

// 获取影评列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getAdminReviewList(queryParams)
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

// 删除影评
const handleDeleteReview = (row) => {
  ElMessageBox.confirm('删除该影评将同步删除其下所有回复，确定吗？', '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
  }).then(async () => {
    await deleteReview(row.id)
    ElMessage.success('已删除')
    getList()
  })
}

// --- 回复管理逻辑 ---

// 打开回复弹窗
const openReplyDialog = (row) => {
  replyDialog.visible = true
  replyDialog.title = `查看回复 - ${row.nickname}`
  replyDialog.reviewId = row.id
  replyParams.page = 1
  getReplies()
}

// 获取回复列表 (分页)
const getReplies = async () => {
  replyLoading.value = true
  try {
    const res = await getReplyList(replyDialog.reviewId, replyParams)
    replyList.value = res.records
    replyTotal.value = res.total
  } finally {
    replyLoading.value = false
  }
}

// 删除单条回复
const handleDeleteReply = (row) => {
  ElMessageBox.confirm('确定删除这条回复吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteReply(row.id)
    ElMessage.success('删除成功')
    getReplies() // 刷新回复列表
  })
}

onMounted(() => getList())
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-20">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜内容/用户/电影名..."
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 影评列表 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="movieTitle" label="电影" width="150" show-overflow-tooltip />
      <el-table-column prop="nickname" label="用户" width="120" show-overflow-tooltip />
      <el-table-column prop="score" label="评分" width="80" align="center">
        <template #default="{ row }">
          <span style="color: #ff9900; font-weight: bold">{{ row.score }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="170" align="center">
        <template #default="{ row }">{{ row.createTime?.replace('T', ' ') }}</template>
      </el-table-column>

      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="ChatDotSquare" @click="openReplyDialog(row)"
            >管理回复</el-button
          >
          <el-button link type="danger" :icon="Delete" @click="handleDeleteReview(row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 主列表分页 -->
    <div style="margin-top: 15px; text-align: right">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="getList"
      />
    </div>

    <!-- 回复列表弹窗 -->
    <el-dialog v-model="replyDialog.visible" :title="replyDialog.title" width="700px">
      <el-table :data="replyList" border v-loading="replyLoading" height="400px">
        <el-table-column prop="nickname" label="回复人" width="120" />
        <el-table-column prop="targetNickname" label="回复对象" width="120">
          <template #default="{ row }">
            <span v-if="row.targetNickname" style="color: #909399">@{{ row.targetNickname }}</span>
            <span v-else>楼主</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="danger" :icon="Delete" @click="handleDeleteReply(row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <!-- 回复列表分页 -->
      <div style="margin-top: 10px; text-align: right">
        <el-pagination
          v-model:current-page="replyParams.page"
          :page-size="replyParams.size"
          :total="replyTotal"
          layout="prev, pager, next"
          small
          @current-change="getReplies"
        />
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
</style>
