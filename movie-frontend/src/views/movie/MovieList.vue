<!-- eslint-disable no-unused-vars -->
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getMovieList, addMovie, updateMovie, deleteMovie, getMovieDetail } from '@/api/movie'
import { getActorList } from '@/api/actor'
import { getDirectorList } from '@/api/director'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Picture as IconPicture,
  View as IconView,
  Edit as IconEdit,
  Delete as IconDelete,
} from '@element-plus/icons-vue'

// --- 基础配置 ---
const tableData = ref([])
const loading = ref(false)
const total = ref(0) // 总条数
const queryParams = reactive({
  page: 1,
  size: 10,
  name: '',
})

// 上传地址 (处理代理)
const uploadUrl = '/api/file/upload'
const headers = computed(() => ({ authorization: localStorage.getItem('token') }))

// 详情/表单 状态
const detailVisible = ref(false)
const movieDetail = ref({})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)

// 表单数据
const form = reactive({
  id: null,
  title: '',
  originalTitle: '',
  releaseDate: '',
  duration: 120,
  genre: '',
  language: '',
  country: '',
  synopsis: '',
  posterUrl: '',
  trailerUrl: '',
  actorIds: [],
  directorIds: [],
})

const actorOptions = ref([])
const directorOptions = ref([])
const searchLoading = ref(false)

// 校验规则
const rules = {
  title: [{ required: true, message: '请输入电影名称', trigger: 'blur' }],
  releaseDate: [{ required: true, message: '请选择上映日期', trigger: 'change' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }],
  posterUrl: [{ required: true, message: '请上传海报', trigger: 'change' }],
}

// --- 核心逻辑：分页查询 ---

const getList = async () => {
  loading.value = true
  try {
    // 每次查询直接覆盖旧数据，保持 DOM 节点数量恒定（只有10条），彻底解决卡顿
    const res = await getMovieList(queryParams)
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1 // 搜索时重置回第一页
  getList()
}

// 处理页码改变
const handlePageChange = (val) => {
  queryParams.page = val
  getList()
}

const handleDetail = async (row) => {
  try {
    const res = await getMovieDetail(row.id)
    movieDetail.value = res.data || res
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增电影'
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  resetForm()
  dialogTitle.value = '编辑电影'
  try {
    const res = await getMovieDetail(row.id)
    const data = res.data || res
    Object.assign(form, {
      ...data,
      releaseDate: data.releaseDate ? data.releaseDate.toString() : '',
      actorIds: data.actorList ? data.actorList.map((a) => a.id) : [],
      directorIds: data.directorList ? data.directorList.map((d) => d.id) : [],
    })
    // 回显下拉框选项
    if (data.actorList) actorOptions.value = data.actorList
    if (data.directorList) directorOptions.value = data.directorList
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('数据加载失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除电影《${row.title}》吗?`, '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  }).then(async () => {
    await deleteMovie(row.id)
    ElMessage.success('删除成功')
    handleSearch()
  })
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const submitData = { ...form }
        // 简单处理日期格式
        if (submitData.releaseDate && submitData.releaseDate.length > 10) {
          submitData.releaseDate = submitData.releaseDate.substring(0, 10)
        }
        if (form.id) {
          await updateMovie(submitData)
          ElMessage.success('修改成功')
        } else {
          await addMovie(submitData)
          ElMessage.success('发布成功')
        }
        dialogVisible.value = false
        getList()
      } catch (e) {
        console.error(e)
      }
    }
  })
}

// --- 辅助逻辑 ---
const handlePosterSuccess = (response) => {
  if (response.code === 200) {
    form.posterUrl = response.data
    ElMessage.success('海报上传成功')
  } else {
    ElMessage.error('上传失败: ' + response.message)
  }
}
const beforePosterUpload = (file) => {
  const isImg = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImg) ElMessage.error('格式错误!')
  if (!isLt5M) ElMessage.error('大小超过5MB!')
  return isImg && isLt5M
}

const remoteActors = async (query) => {
  if (!query) return
  searchLoading.value = true
  try {
    const res = await getActorList({ name: query, page: 1, size: 20 })
    actorOptions.value = res.records || []
  } finally {
    searchLoading.value = false
  }
}
const remoteDirectors = async (query) => {
  if (!query) return
  searchLoading.value = true
  try {
    const res = await getDirectorList({ name: query, page: 1, size: 20 })
    directorOptions.value = res.records || []
  } finally {
    searchLoading.value = false
  }
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.assign(form, {
    id: null,
    title: '',
    originalTitle: '',
    releaseDate: '',
    duration: 120,
    genre: '',
    language: '',
    country: '',
    synopsis: '',
    posterUrl: '',
    trailerUrl: '',
    actorIds: [],
    directorIds: [],
  })
  actorOptions.value = []
  directorOptions.value = []
}

onMounted(() => {
  getList()
})
</script>

<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="电影搜索">
          <el-input
            v-model="queryParams.name"
            placeholder="输入电影名称搜索..."
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button type="success" :icon="Plus" @click="handleAdd">发布电影</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 
      表格区域：
      1. 去掉滚动加载指令
      2. height 属性固定高度，开启 Element Plus 虚拟滚动列表优化
    -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      style="width: 100%; margin-top: 20px"
      height="calc(100vh - 280px)"
    >
      <el-table-column label="海报" width="100" align="center" fixed="left">
        <template #default="{ row }">
          <el-image
            style="width: 60px; height: 85px; border-radius: 4px"
            :src="row.posterUrl"
            :preview-src-list="[row.posterUrl]"
            fit="cover"
            preview-teleported
          >
            <template #error>
              <div class="image-error">
                <el-icon><IconPicture /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="电影名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="originalTitle" label="原名" min-width="130" show-overflow-tooltip />
      <el-table-column prop="genre" label="类型" width="120" show-overflow-tooltip />
      <el-table-column prop="duration" label="时长" width="90" align="center">
        <template #default="{ row }">{{ row.duration }} min</template>
      </el-table-column>
      <el-table-column prop="releaseDate" label="上映日期" width="120" align="center" sortable />

      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="info" :icon="IconView" @click="handleDetail(row)">详情</el-button>
          <el-button link type="primary" :icon="IconEdit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="IconDelete" @click="handleDelete(row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页区域：B端管理的精髓 -->
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, prev, pager, next, jumper"
        :total="total"
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 电影表单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="电影名称" prop="title">
              <el-input v-model="form.title" />
            </el-form-item>
            <el-form-item label="原名/英文" prop="originalTitle">
              <el-input v-model="form.originalTitle" />
            </el-form-item>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="上映日期" prop="releaseDate">
                  <el-date-picker
                    v-model="form.releaseDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="时长" prop="duration">
                  <el-input-number
                    v-model="form.duration"
                    :min="1"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="类型" prop="genre">
              <el-input v-model="form.genre" />
            </el-form-item>
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="国家" prop="country">
                  <el-input v-model="form.country" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="语言" prop="language">
                  <el-input v-model="form.language" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>

          <el-col :span="8" class="upload-col">
            <el-form-item label-width="0" prop="posterUrl">
              <div class="poster-uploader-container">
                <el-upload
                  class="poster-uploader"
                  :action="uploadUrl"
                  :headers="headers"
                  :show-file-list="false"
                  :on-success="handlePosterSuccess"
                  :before-upload="beforePosterUpload"
                  name="file"
                >
                  <img v-if="form.posterUrl" :src="form.posterUrl" class="poster" />
                  <div v-else class="poster-placeholder">
                    <el-icon class="poster-icon"><Plus /></el-icon>
                    <span>点击上传海报</span>
                  </div>
                </el-upload>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="预告片URL">
          <el-input v-model="form.trailerUrl" placeholder="请输入视频链接" />
        </el-form-item>

        <el-form-item label="主演">
          <el-select
            v-model="form.actorIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="输入名字搜索"
            :remote-method="remoteActors"
            :loading="searchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in actorOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            >
              <span style="float: left">{{ item.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ item.enName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="导演">
          <el-select
            v-model="form.directorIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="输入名字搜索"
            :remote-method="remoteDirectors"
            :loading="searchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in directorOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="剧情简介" prop="synopsis">
          <el-input
            v-model="form.synopsis"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="电影详情" width="700px">
      <div class="detail-container" v-if="movieDetail.id">
        <div class="detail-header">
          <el-image
            :src="movieDetail.posterUrl"
            style="width: 120px; height: 170px; border-radius: 6px"
            fit="cover"
          />
          <div class="detail-info">
            <h2>{{ movieDetail.title }}</h2>
            <p class="en-title">{{ movieDetail.originalTitle }}</p>
            <el-tag size="small" effect="dark">{{ movieDetail.genre }}</el-tag>
            <el-tag size="small" type="warning" style="margin-left: 10px"
              >{{ movieDetail.duration }}分钟</el-tag
            >
          </div>
        </div>
        <el-divider content-position="left">基础信息</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="上映日期">{{
            movieDetail.releaseDate
          }}</el-descriptions-item>
          <el-descriptions-item label="制片国家">{{ movieDetail.country }}</el-descriptions-item>
          <el-descriptions-item label="语言">{{ movieDetail.language }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <span style="color: #ff9900; font-weight: bold">{{
              movieDetail.rating || '暂无'
            }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">演职人员</el-divider>
        <div class="people-list">
          <p>
            <strong>导演：</strong>
            <span v-for="d in movieDetail.directorList" :key="d.id" class="mr-10">{{
              d.name
            }}</span>
          </p>
          <p>
            <strong>主演：</strong>
            <span v-for="a in movieDetail.actorList" :key="a.id" class="mr-10">{{ a.name }}</span>
          </p>
        </div>
        <el-divider content-position="left">剧情简介</el-divider>
        <p class="synopsis">{{ movieDetail.synopsis }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-container {
  padding: 20px;
  height: 100%;
}
.search-card {
  margin-bottom: 20px;
}
.image-error {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 上传框样式 */
.upload-col {
  display: flex;
  justify-content: center;
}
.poster-uploader-container {
  width: 160px;
  height: 230px;
}
.poster-uploader {
  width: 100%;
  height: 100%;
  display: block;
}
:deep(.el-upload) {
  width: 100%;
  height: 100%;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: 0.2s;
  display: flex;
  justify-content: center;
  align-items: center;
}
:deep(.el-upload:hover) {
  border-color: #409eff;
}

.poster {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.poster-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #8c939d;
  background: #fafafa;
}
.poster-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

/* 详情页样式 */
.detail-header {
  display: flex;
  gap: 20px;
}
.detail-info h2 {
  margin: 0 0 5px 0;
  font-size: 20px;
}
.en-title {
  color: #909399;
  margin: 0 0 10px 0;
  font-size: 14px;
}
.mr-10 {
  margin-right: 10px;
  display: inline-block;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.synopsis {
  line-height: 1.6;
  color: #606266;
  text-indent: 2em;
}
</style>
