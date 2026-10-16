<template>
  <div>
    <div class="toolbar">
      <h3>我的项目</h3>
      <el-button type="primary" :icon="Plus" v-permission="'project:project:create'"
        @click="dialogVisible = true">新建项目</el-button>
    </div>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6" v-for="p in projects" :key="p.id" style="margin-bottom:16px">
        <el-card shadow="hover" class="project-card" @click="openBoard(p.id)">
          <div class="card-title">
            <el-icon color="#409eff"><Folder /></el-icon>
            <span>{{ p.name }}</span>
          </div>
          <p class="desc">{{ p.description || '暂无描述' }}</p>
          <div class="meta">
            <el-tag size="small" :type="statusType(p.status)">{{ statusText(p.status) }}</el-tag>
            <span class="date" v-if="p.endDate">截止 {{ p.endDate }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24" v-if="!loading && projects.length === 0">
        <el-empty description="还没有项目，点击右上角创建第一个项目吧" />
      </el-col>
    </el-row>

    <div v-if="total > pageSize" style="display:flex;justify-content:center;margin-top:8px">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
        :total="total" :page-sizes="[12, 24, 48]" layout="total, sizes, prev, pager, next"
        @size-change="load" @current-change="load" background />
    </div>

    <el-dialog v-model="dialogVisible" title="新建项目" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="周期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import projectApi, { type Project } from '@/api/project'

const router = useRouter()
const projects = ref<Project[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const creating = ref(false)
const form = reactive({ name: '', description: '' })
const dateRange = ref<string[]>()
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res: any = await projectApi.mine({ page: currentPage.value, size: pageSize.value })
    projects.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!form.name) {
    ElMessage.warning('请输入项目名称')
    return
  }
  creating.value = true
  try {
    await projectApi.create({
      name: form.name,
      description: form.description,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    })
    ElMessage.success('项目创建成功，已自动生成默认看板列')
    dialogVisible.value = false
    form.name = ''
    form.description = ''
    currentPage.value = 1
    load()
  } finally {
    creating.value = false
  }
}

function openBoard(id: number) {
  router.push(`/projects/${id}/board`)
}

function statusText(s: number) {
  return { 1: '进行中', 2: '已归档', 3: '已删除' }[s] || '未知'
}
function statusType(s: number) {
  return ({ 1: 'success', 2: 'info', 3: 'danger' } as const)[s as 1 | 2 | 3]
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.project-card {
  cursor: pointer;
  border-radius: 14px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(80,76,60,0.1);
}
.card-title { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 15px; color: #5a5a52; }
.desc { color: #909399; font-size: 13px; min-height: 40px; margin: 10px 0; line-height: 1.6; }
.meta { display: flex; justify-content: space-between; align-items: center; }
.date { color: #c0c4cc; font-size: 12px; }
</style>
