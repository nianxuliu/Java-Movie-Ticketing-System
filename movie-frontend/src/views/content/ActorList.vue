<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getActorList, addActor, updateActor, deleteActor } from '@/api/actor'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, User, Male, Female } from '@element-plus/icons-vue'

// --- 基础数据 ---
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const queryParams = reactive({ page: 1, size: 10, name: '' })

// 上传配置
const uploadUrl = '/api/file/upload'
const headers = computed(() => ({ authorization: localStorage.getItem('token') }))

// 弹窗数据
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({
  id: null,
  name: '',
  enName: '',
  gender: 1, // 默认为1(男)
  nationality: '',
  birthDate: '',
  avatarUrl: '',
})

// --- 方法 ---
const getList = async () => {
  loading.value = true
  try {
    const res = await getActorList(queryParams)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增演员'
  // 重置表单，注意 gender 默认为 1
  Object.assign(form, {
    id: null,
    name: '',
    enName: '',
    gender: 1,
    nationality: '',
    birthDate: '',
    avatarUrl: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑演员'
  // 回显数据
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除演员 "${row.name}" 吗?`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteActor(row.id)
      ElMessage.success('删除成功')
      getList()
    },
  )
}

const submitForm = async () => {
  if (!form.name) return ElMessage.warning('姓名必填')
  try {
    if (form.id) await updateActor(form)
    else await addActor(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    getList()
  } catch (e) {
    console.error(e)
  }
}

const handleAvatarSuccess = (res) => {
  if (res.code === 200) form.avatarUrl = res.data
  else ElMessage.error(res.message)
}

onMounted(() => getList())
</script>

<template>
  <div class="app-container" style="padding: 20px">
    <!-- 搜索 -->
    <el-card shadow="never" class="mb-20">
      <el-form :inline="true">
        <el-form-item label="姓名">
          <el-input
            v-model="queryParams.name"
            placeholder="搜索演员..."
            clearable
            @clear="getList"
            @keyup.enter="getList"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="getList">查询</el-button>
          <el-button type="success" :icon="Plus" @click="handleAdd">新增演员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column label="头像" width="90" align="center">
        <template #default="{ row }">
          <el-avatar :size="50" :src="row.avatarUrl" :icon="User" shape="square" />
        </template>
      </el-table-column>

      <el-table-column prop="name" label="姓名" min-width="100" />
      <el-table-column prop="enName" label="英文名" min-width="120" show-overflow-tooltip />

      <el-table-column label="性别" width="80" align="center">
        <template #default="{ row }">
          <!-- 兼容字符串和数字类型的比较 -->
          <el-tag v-if="row.gender == 1" type="primary">男</el-tag>
          <el-tag v-else-if="row.gender == 0" type="danger">女</el-tag>
          <el-tag v-else type="info">未知</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="nationality" label="国籍" width="100" />
      <el-table-column prop="birthDate" label="出生日期" width="120" align="center" />

      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="80px">
        <el-row :gutter="20">
          <!-- 左侧上传头像 -->
          <el-col :span="8" style="text-align: center">
            <el-upload
              class="avatar-uploader"
              :action="uploadUrl"
              :headers="headers"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              name="file"
            >
              <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar" />
              <div v-else class="avatar-uploader-icon"><Plus /></div>
              <div style="margin-top: 10px; font-size: 12px; color: #999">点击上传头像</div>
            </el-upload>
          </el-col>

          <!-- 右侧表单项 -->
          <el-col :span="16">
            <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
            <el-form-item label="英文名"><el-input v-model="form.enName" /></el-form-item>

            <el-form-item label="性别">
              <!-- 使用 :label="1" 确保绑定的是数字类型 -->
              <el-radio-group v-model="form.gender">
                <el-radio :label="1"
                  ><el-icon><Male /></el-icon> 男</el-radio
                >
                <el-radio :label="0"
                  ><el-icon><Female /></el-icon> 女</el-radio
                >
              </el-radio-group>
            </el-form-item>

            <el-form-item label="国籍"><el-input v-model="form.nationality" /></el-form-item>

            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}
.avatar-uploader .avatar {
  width: 140px;
  height: 180px;
  display: block;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #eee;
}
.avatar-uploader .el-upload {
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 140px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  background: #fafafa;
}
.avatar-uploader-icon:hover {
  border-color: #409eff;
}
</style>
