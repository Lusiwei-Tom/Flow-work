<template>
  <div class="invite-page">
    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><Key /></el-icon>
          <span>生成邀请码</span>
        </div>
      </template>
      <el-form :model="form" inline>
        <el-form-item label="角色">
          <el-select v-model="form.presetRole" style="width:140px">
            <el-option label="普通成员" value="member" />
            <el-option label="项目管理员" value="project_manager" />
            <el-option label="超级管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用次数">
          <el-input-number v-model="form.maxUses" :min="1" :max="100" style="width:120px" />
          <span style="color:#909399;font-size:12px;margin-left:6px">（1=一次性）</span>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="form.expiresAt" type="datetime" placeholder="不填=永久"
            value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="绑定项目">
          <el-select v-model="form.presetProjectId" placeholder="可选" clearable style="width:200px">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目角色" v-if="form.presetProjectId">
          <el-select v-model="form.presetProjectRole" style="width:130px">
            <el-option label="管理员" value="MANAGER" />
            <el-option label="成员" value="MEMBER" />
            <el-option label="观察者" value="OBSERVER" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="creating" @click="onCreate">生成</el-button>
      </el-form>
    </el-card>

    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><Tickets /></el-icon>
          <span>邀请码列表（{{ list.length }}）</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="邀请码" width="160">
          <template #default="{ row }">
            <el-typography copyable>{{ row.code }}</el-typography>
          </template>
        </el-table-column>
        <el-table-column label="预设角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTagType(row.presetRole)">{{ roleText(row.presetRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目" width="140">
          <template #default="{ row }">
            <span v-if="row.presetProjectId">{{ projectName(row.presetProjectId) }}</span>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="项目角色" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.presetProjectRole">{{ row.presetProjectRole }}</span>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="使用情况" width="100" align="center">
          <template #default="{ row }">
            {{ row.usedCount }} / {{ row.maxUses === 0 ? '∞' : row.maxUses }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">
            {{ row.expiresAt || '永久' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
            <el-tag v-else type="info" size="small">已停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'active'" size="small" @click="onDisable(row.id)">停用</el-button>
            <el-button size="small" type="danger" @click="onDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import inviteApi, { type InviteCodeVO } from '@/api/invite'
import projectApi, { type Project } from '@/api/project'

const loading = ref(true)
const creating = ref(false)
const list = ref<InviteCodeVO[]>([])
const projects = ref<Project[]>([])

const form = reactive({
  presetRole: 'member',
  maxUses: 1,
  expiresAt: '',
  presetProjectId: null as number | null,
  presetProjectRole: 'MEMBER'
})

async function load() {
  loading.value = true
  try {
    list.value = await inviteApi.list()
    projects.value = await projectApi.mine()
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  creating.value = true
  try {
    const res = await inviteApi.create(form)
    ElMessage.success(`已生成邀请码：${res.code}`)
    await load()
  } finally {
    creating.value = false
  }
}

async function onDisable(id: number) {
  await ElMessageBox.confirm('确定停用该邀请码？', '提示', { type: 'warning' })
  await inviteApi.disable(id)
  ElMessage.success('已停用')
  await load()
}

async function onDelete(id: number) {
  await ElMessageBox.confirm('确定删除该邀请码？', '提示', { type: 'warning' })
  await inviteApi.delete(id)
  ElMessage.success('已删除')
  await load()
}

function roleText(role: string) {
  return { admin: '超级管理员', project_manager: '项目管理员', member: '普通成员' }[role] || role
}
function roleTagType(role: string) {
  return role === 'admin' ? 'danger' : role === 'project_manager' ? 'warning' : ''
}
function projectName(id: number) {
  return projects.value.find(p => p.id === id)?.name || `#${id}`
}

onMounted(load)
</script>

<style scoped>
.invite-page { max-width: 1100px; margin: 0 auto; }
.card-section { margin-bottom: 18px; border-radius: 14px; border: 1px solid rgba(120,115,100,0.08); }
.card-header { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #5a5a52; }
</style>
