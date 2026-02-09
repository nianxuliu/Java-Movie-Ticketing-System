<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import {
  getCinemaList,
  addCinema,
  updateCinema,
  deleteCinema,
  getHallList,
  addHall,
  deleteHall,
  updateHall,
} from '@/api/cinema'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Grid, CircleCloseFilled } from '@element-plus/icons-vue'

// ================= 影院管理部分 =================
const cinemaList = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({ page: 1, size: 10, name: '', city: '' })
const cinemaDialog = reactive({ visible: false, title: '' })
const cinemaForm = reactive({ id: null, name: '', city: '', address: '' })

// 获取影院列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getCinemaList(queryParams)
    cinemaList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 提交影院 (新增/修改)
const submitCinema = async () => {
  if (!cinemaForm.name || !cinemaForm.city) return ElMessage.warning('名称和城市必填')
  try {
    if (cinemaForm.id) await updateCinema(cinemaForm)
    else await addCinema(cinemaForm)
    ElMessage.success('操作成功')
    cinemaDialog.visible = false
    getList()
  } catch (e) {
    console.error(e)
  }
}

// 删除影院
const handleDeleteCinema = (row) => {
  ElMessageBox.confirm(`删除影院"${row.name}"?`, '警告', { type: 'warning' }).then(async () => {
    await deleteCinema(row.id)
    ElMessage.success('已删除')
    getList()
  })
}

const openCinemaDialog = (row) => {
  cinemaDialog.visible = true
  if (row) {
    cinemaDialog.title = '修改影院'
    Object.assign(cinemaForm, row)
  } else {
    cinemaDialog.title = '新增影院'
    Object.assign(cinemaForm, { id: null, name: '', city: '', address: '' })
  }
}

// ================= 影厅管理 + 座位可视化 (核心部分) =================
const hallDialogVisible = ref(false)
const currentCinema = ref({})
const hallList = ref([])
const hallLoading = ref(false)

const hallFormDialog = reactive({ visible: false, title: '' })
// 影厅表单数据
const hallForm = reactive({
  id: null,
  name: '',
  rows: 8,
  cols: 10,
  brokenSeats: [], // 存储坏座坐标 ["1-1", "2-5"]
})

// 打开影厅列表弹窗
const handleManageHall = async (row) => {
  currentCinema.value = row
  hallDialogVisible.value = true
  loadHalls()
}

// 加载该影院下的所有影厅
const loadHalls = async () => {
  hallLoading.value = true
  try {
    const res = await getHallList(currentCinema.value.id)
    hallList.value = res.data || res
  } finally {
    hallLoading.value = false
  }
}

// 打开 新增/编辑 影厅表单
const openHallForm = (row) => {
  hallFormDialog.visible = true
  hallForm.brokenSeats = [] // 重置坏座

  if (row) {
    hallFormDialog.title = '修改影厅'
    hallForm.id = row.id
    hallForm.name = row.name
    try {
      // 解析后端存的 JSON 配置
      const config = JSON.parse(row.seatConfig)
      hallForm.rows = config.rows
      hallForm.cols = config.cols
      // 回显坏座
      if (config.broken_seats) {
        hallForm.brokenSeats = config.broken_seats
      }
    } catch (e) {
      // 容错处理
      hallForm.rows = 8
      hallForm.cols = 10
    }
  } else {
    hallFormDialog.title = '新增影厅'
    Object.assign(hallForm, { id: null, name: '', rows: 8, cols: 10, brokenSeats: [] })
  }
}

// 切换座位状态 (好座 <-> 坏座)
const toggleBroken = (r, c) => {
  const seatKey = `${r}-${c}`
  const index = hallForm.brokenSeats.indexOf(seatKey)
  if (index > -1) {
    // 已经是坏座，移除 -> 变好
    hallForm.brokenSeats.splice(index, 1)
  } else {
    // 增加 -> 变坏
    hallForm.brokenSeats.push(seatKey)
  }
}

// 判断是否坏座
const isBroken = (r, c) => {
  return hallForm.brokenSeats.includes(`${r}-${c}`)
}

// 提交影厅配置
const submitHall = async () => {
  if (!hallForm.name) return ElMessage.warning('影厅名称必填')

  // 构造标准 JSON 对象，包含行列和坏座信息
  const seatConfigObj = {
    rows: hallForm.rows,
    cols: hallForm.cols,
    broken_seats: hallForm.brokenSeats,
  }

  const submitData = {
    id: hallForm.id,
    cinemaId: currentCinema.value.id,
    name: hallForm.name,
    seatConfig: JSON.stringify(seatConfigObj), // 统一传 JSON 字符串给后端
  }

  try {
    if (hallForm.id) {
      await updateHall(submitData)
      ElMessage.success('修改成功')
    } else {
      await addHall(submitData)
      ElMessage.success('添加成功')
    }
    hallFormDialog.visible = false
    loadHalls()
  } catch (e) {
    console.error(e)
  }
}

const handleDeleteHall = (row) => {
  ElMessageBox.confirm(`确认删除 "${row.name}" 吗?`, '提示').then(async () => {
    await deleteHall(row.id)
    ElMessage.success('删除成功')
    loadHalls()
  })
}

// 动态计算 Grid 样式，实现座位的网格布局
const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${hallForm.cols}, 1fr)`, // 动态列数
  gap: '5px',
  marginTop: '20px',
  justifyContent: 'center',
  maxWidth: '100%',
}))

onMounted(() => getList())
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 影院搜索栏 -->
    <el-card shadow="never" class="mb-20">
      <el-form :inline="true">
        <el-form-item label="城市">
          <el-input v-model="queryParams.city" placeholder="如：北京" clearable @clear="getList" />
        </el-form-item>
        <el-form-item label="影院名称">
          <el-input
            v-model="queryParams.name"
            placeholder="搜索影院..."
            clearable
            @clear="getList"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">查询</el-button>
          <el-button type="success" :icon="Plus" @click="openCinemaDialog()">新增影院</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 影院列表 -->
    <el-table :data="cinemaList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="影院名称" min-width="150" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="address" label="详细地址" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" plain size="small" :icon="Grid" @click="handleManageHall(row)"
            >影厅管理</el-button
          >
          <el-button type="primary" link :icon="Edit" @click="openCinemaDialog(row)"
            >编辑</el-button
          >
          <el-button type="danger" link :icon="Delete" @click="handleDeleteCinema(row)"
            >删除</el-button
          >
        </template>
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

    <!-- 弹窗1: 影院 编辑/新增 -->
    <el-dialog v-model="cinemaDialog.visible" :title="cinemaDialog.title" width="500px">
      <el-form :model="cinemaForm" label-width="80px">
        <el-form-item label="影院名称" required>
          <el-input v-model="cinemaForm.name" />
        </el-form-item>
        <el-form-item label="所在城市" required>
          <el-input v-model="cinemaForm.city" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="cinemaForm.address" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cinemaDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitCinema">保存</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗2: 影厅列表 -->
    <el-dialog
      v-model="hallDialogVisible"
      :title="`管理影厅 - ${currentCinema.name}`"
      width="700px"
    >
      <div style="margin-bottom: 10px">
        <el-button type="primary" size="small" :icon="Plus" @click="openHallForm()"
          >新增影厅</el-button
        >
      </div>
      <el-table :data="hallList" border v-loading="hallLoading" height="300px">
        <el-table-column prop="name" label="影厅名称" />
        <el-table-column label="规格" align="center">
          <template #default="{ row }">
            <el-tag type="info">
              {{ JSON.parse(row.seatConfig).rows }}行 x {{ JSON.parse(row.seatConfig).cols }}列
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总座位 / 坏座" align="center">
          <template #default="{ row }">
            <span style="color: #409eff">{{
              JSON.parse(row.seatConfig).rows * JSON.parse(row.seatConfig).cols
            }}</span>
            <span style="margin: 0 5px">/</span>
            <span style="color: #f56c6c">{{
              JSON.parse(row.seatConfig).broken_seats?.length || 0
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHallForm(row)">修改</el-button>
            <el-button link type="danger" @click="handleDeleteHall(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 弹窗3: 影厅 编辑/新增 (带可视化选座) -->
    <el-dialog
      v-model="hallFormDialog.visible"
      :title="hallFormDialog.title"
      width="600px"
      append-to-body
    >
      <el-form :model="hallForm" label-width="80px">
        <el-form-item label="影厅名称" required>
          <el-input v-model="hallForm.name" placeholder="如: 1号IMAX厅" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="行数" required>
              <el-input-number v-model="hallForm.rows" :min="4" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="列数" required>
              <el-input-number v-model="hallForm.cols" :min="4" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 可视化座位图区域 -->
        <div class="seat-map-container">
          <p class="tips">
            点击座位可将其标记为 <span style="color: #f56c6c">坏座 (红色)</span>，再次点击恢复。
          </p>
          <div class="screen">屏幕方向</div>

          <div :style="gridStyle" class="seat-grid">
            <template v-for="r in hallForm.rows" :key="r">
              <template v-for="c in hallForm.cols" :key="c">
                <div
                  class="seat-item"
                  :class="{ 'is-broken': isBroken(r, c) }"
                  @click="toggleBroken(r, c)"
                  :title="`${r}排${c}座`"
                >
                  <el-icon v-if="isBroken(r, c)"><CircleCloseFilled /></el-icon>
                  <span v-else class="seat-no">{{ c }}</span>
                </div>
              </template>
            </template>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="hallFormDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitHall">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}

/* 座位图容器 */
.seat-map-container {
  margin-top: 10px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-x: auto; /* 允许横向滚动以防列数太多 */
}

.tips {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.screen {
  width: 80%;
  height: 20px;
  background: #e0e0e0;
  border-radius: 0 0 20px 20px;
  text-align: center;
  font-size: 12px;
  color: #999;
  line-height: 20px;
  margin-bottom: 20px;
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.1);
}

.seat-grid {
  /* 这里的 grid-template-columns 会通过 js 动态控制 */
}

.seat-item {
  width: 30px;
  height: 30px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  color: #606266;
  transition: all 0.2s;
}

.seat-item:hover {
  border-color: #409eff;
  color: #409eff;
  transform: scale(1.1);
}

/* 坏座样式 */
.seat-item.is-broken {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}
</style>
