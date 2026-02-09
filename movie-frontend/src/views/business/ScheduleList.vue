<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getScheduleList, addSchedule, deleteSchedule } from '@/api/schedule'
import { getMovieList } from '@/api/movie'
import { getCinemaList, getHallList, getAllHalls } from '@/api/cinema'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Timer } from '@element-plus/icons-vue'

// --- 1. 状态定义 ---
const currentMovieId = ref(null)
const movieList = ref([])
const loadingMovies = ref(false)

const scheduleList = ref([]) // 存储当前电影的所有排片
const tableLoading = ref(false)

// 字典 Map (ID -> Name)，用于在表格中把数字 ID 显示为中文名
const cinemaMap = ref({})
const hallMap = ref({})

// 表单相关
const dialogVisible = ref(false)
const form = reactive({ movieId: null, cinemaId: null, hallId: null, startTime: '', price: 0 })
const cinemaOptions = ref([])
const hallOptions = ref([])
const loadingHalls = ref(false)

// --- 2. 初始化字典 (核心性能优化：一次性加载影院和影厅，防止表格渲染时反复请求) ---
const initMaps = async () => {
  try {
    // 获取所有影院字典
    const cRes = await getCinemaList({ page: 1, size: 1000 })
    if (cRes.records) {
      cRes.records.forEach((item) => {
        cinemaMap.value[item.id] = item.name
      })
    }
    // 获取所有影厅字典
    const hRes = await getAllHalls()
    const halls = hRes.data || hRes
    if (halls) {
      halls.forEach((item) => {
        hallMap.value[item.id] = item.name
      })
    }
  } catch (e) {
    console.error('字典加载失败', e)
  }
}

// --- 3. 电影搜索逻辑 ---
const remoteSearchMovie = async (query) => {
  loadingMovies.value = true
  try {
    const res = await getMovieList({ name: query, page: 1, size: 20 })
    movieList.value = res.records || []
  } finally {
    loadingMovies.value = false
  }
}

// --- 4. 获取排片列表 (修复 400 错误：直接传递 ID 字符串，而不是对象) ---
const loadSchedules = async (movieId) => {
  if (!movieId) {
    scheduleList.value = []
    return
  }
  tableLoading.value = true
  try {
    // 关键修正：这里只传 movieId，对应后端的 /list/{movieId}
    const res = await getScheduleList(movieId)
    // 后端返回的是 Result<List<Schedule>>，所以取 res.data 或 res
    scheduleList.value = res.data || res || []
  } catch (e) {
    console.error('加载排片失败:', e)
  } finally {
    tableLoading.value = false
  }
}

// 监听当前选中的电影切换
watch(currentMovieId, (newVal) => {
  loadSchedules(newVal)
})

// --- 5. 状态判断与时间格式化 ---
const getStatus = (startTime) => {
  const now = new Date()
  const start = new Date(startTime)
  return now < start
    ? { text: '售票中', type: 'success', effect: 'dark' }
    : { text: '停止售票', type: 'info', effect: 'plain' }
}

const formatTime = (isoStr) => (isoStr ? isoStr.replace('T', ' ').substring(0, 16) : '')

// --- 6. 新增排片逻辑 ---
const handleAdd = async () => {
  if (!currentMovieId.value) return ElMessage.warning('请先在顶部选择一部电影！')
  form.movieId = currentMovieId.value
  form.cinemaId = null
  form.hallId = null
  form.startTime = ''
  form.price = 0
  dialogVisible.value = true

  const res = await getCinemaList({ page: 1, size: 100 })
  cinemaOptions.value = res.records
}

const handleCinemaChange = async (cinemaId) => {
  form.hallId = null
  loadingHalls.value = true
  try {
    const res = await getHallList(cinemaId)
    hallOptions.value = res.data || res
  } finally {
    loadingHalls.value = false
  }
}

const submitForm = async () => {
  if (!form.cinemaId || !form.hallId || !form.startTime || !form.price)
    return ElMessage.warning('请完整填写')
  try {
    await addSchedule(form)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    loadSchedules(currentMovieId.value) // 重新加载
  } catch (e) {
    console.error(e)
  }
}

// --- 7. 删除逻辑 ---
const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该场次吗？', '警告', { type: 'warning' }).then(async () => {
    await deleteSchedule(row.id)
    ElMessage.success('删除成功')
    loadSchedules(currentMovieId.value) // 重新加载
  })
}

onMounted(() => {
  initMaps()
  remoteSearchMovie('')
})
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 顶部选择 -->
    <el-card shadow="never" class="mb-20 selector-card">
      <div class="selector-wrapper">
        <span class="label">当前操作电影：</span>
        <el-select
          v-model="currentMovieId"
          filterable
          remote
          reserve-keyword
          placeholder="请输入电影名搜索..."
          :remote-method="remoteSearchMovie"
          :loading="loadingMovies"
          style="width: 300px"
          size="large"
          clearable
        >
          <el-option v-for="item in movieList" :key="item.id" :label="item.title" :value="item.id">
            <span style="float: left">{{ item.title }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">{{
              item.releaseDate
            }}</span>
          </el-option>
        </el-select>
        <el-button
          type="primary"
          size="large"
          :icon="Plus"
          style="margin-left: 20px"
          :disabled="!currentMovieId"
          @click="handleAdd"
          >新增排片</el-button
        >
      </div>
    </el-card>

    <!-- 排片列表：固定高度保证在大规模渲染时不会假死 -->
    <el-table
      v-if="currentMovieId"
      :data="scheduleList"
      border
      stripe
      v-loading="tableLoading"
      height="calc(100vh - 220px)"
    >
      <el-table-column label="场次时间" width="220">
        <template #default="{ row }">
          <div class="time-block">
            <span class="start">{{ formatTime(row.startTime).split(' ')[1] }}</span>
            <span class="date">{{ formatTime(row.startTime).split(' ')[0] }}</span>
          </div>
          <div class="end-time">预计 {{ formatTime(row.endTime).split(' ')[1] }} 散场</div>
        </template>
      </el-table-column>

      <el-table-column label="放映影院" min-width="150">
        <template #default="{ row }">
          <div style="font-weight: bold; font-size: 15px">
            {{ cinemaMap[row.cinemaId] || row.cinemaId }}
          </div>
        </template>
      </el-table-column>

      <el-table-column label="影厅" min-width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">
            {{ hallMap[row.hallId] || row.hallId + '号厅' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="票价" width="120" align="center">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: bold; font-size: 16px">￥{{ row.price }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatus(row.startTime).type" :effect="getStatus(row.startTime).effect">
            {{ getStatus(row.startTime).text }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="请先在顶部搜索并选择一部电影" />

    <!-- 新增排片弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增排片场次" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属影院" required>
          <el-select
            v-model="form.cinemaId"
            placeholder="选择影院"
            filterable
            style="width: 100%"
            @change="handleCinemaChange"
          >
            <el-option v-for="c in cinemaOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="放映影厅" required>
          <el-select
            v-model="form.hallId"
            placeholder="请先选影院"
            :disabled="!form.cinemaId"
            :loading="loadingHalls"
            style="width: 100%"
          >
            <el-option v-for="h in hallOptions" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开场时间" required>
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="票价" required>
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="5"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
.selector-card {
  background: #fdfdfd;
}
.selector-wrapper {
  display: flex;
  align-items: center;
}
.label {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-right: 10px;
}
.time-block {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.time-block .start {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.time-block .date {
  font-size: 12px;
  color: #909399;
}
.end-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}
</style>
