<template>
  <div class="profile-page">
    <!-- 系统角色卡片 -->
    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><User /></el-icon>
          <span>系统角色</span>
        </div>
      </template>
      <div v-loading="loading">
        <div class="role-tags">
          <el-tag
            v-for="role in profile.roles"
            :key="role"
            :type="roleTagType(role)"
            size="large"
            effect="dark"
            class="role-tag"
          >
            {{ roleText(role) }}
          </el-tag>
        </div>
        <p class="role-desc">{{ roleDescription }}</p>
      </div>
    </el-card>

    <!-- 权限矩阵 -->
    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><Key /></el-icon>
          <span>功能权限（{{ profile.permissions.length }} 项）</span>
        </div>
      </template>
      <div v-loading="loading" class="perm-grid">
        <div v-for="(perms, module) in profile.groupedPermissions" :key="module" class="perm-module">
          <div class="perm-module-title">{{ moduleLabel(module) }}</div>
          <div class="perm-tags">
            <el-tag
              v-for="p in perms"
              :key="p"
              type="success"
              effect="plain"
              size="small"
            >
              {{ permLabel(p) }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 各项目角色 -->
    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><Folder /></el-icon>
          <span>项目角色（{{ profile.projectCount }} 个项目）</span>
        </div>
      </template>
      <el-table v-loading="loading" :data="profile.projects" stripe>
        <el-table-column label="项目" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="goBoard(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="身份" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isOwner" type="warning" effect="dark" size="small">创建者</el-tag>
            <span v-else>成员</span>
          </template>
        </el-table-column>
        <el-table-column label="项目角色" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="projectRoleTagType(row.projectRole)" effect="plain" size="small">
              {{ projectRoleText(row.projectRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可执行操作" min-width="280">
          <template #default="{ row }">
            <span class="perm-desc">{{ projectRolePerms(row.projectRole) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && profile.projects.length === 0" description="还没有参与任何项目" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import meApi from '@/api/me'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)

const profile = reactive({
  userId: 0,
  roles: [] as string[],
  permissions: [] as string[],
  groupedPermissions: {} as Record<string, string[]>,
  projects: [] as any[],
  projectCount: 0
})

const roleDescription = computed(() => {
  if (profile.roles.includes('admin'))
    return '超级管理员：拥有系统全部权限，可管理用户、角色、项目及所有功能模块'
  if (profile.roles.includes('project_manager'))
    return '项目管理员：可管理项目和 AI 功能，但不能管理用户和角色'
  return '普通成员：可创建项目、读写任务、检索知识库，无法使用 AI 功能和数据看板'
})

function roleText(role: string) {
  const map: Record<string, string> = {
    admin: '超级管理员',
    project_manager: '项目管理员',
    member: '普通成员'
  }
  return map[role] || role
}

function roleTagType(role: string) {
  if (role === 'admin') return 'danger'
  if (role === 'project_manager') return 'warning'
  return 'info'
}

function moduleLabel(module: string) {
  const map: Record<string, string> = {
    auth: '认证与用户',
    project: '项目管理',
    ai: 'AI 功能',
    knowledge: '知识库',
    analytics: '数据看板',
    other: '其他'
  }
  return map[module] || module
}

function permLabel(p: string) {
  const map: Record<string, string> = {
    'auth:user:read': '查看用户',
    'auth:user:manage': '管理用户',
    'auth:role:manage': '管理角色',
    'project:project:create': '创建项目',
    'project:project:read': '查看项目',
    'project:project:update': '编辑项目',
    'project:project:delete': '删除项目',
    'project:task:create': '创建任务',
    'project:task:read': '查看任务',
    'project:task:update': '编辑任务',
    'project:task:delete': '删除任务',
    'project:kanban:manage': '管理看板',
    'ai:agent:execute': 'AI 管家',
    'ai:task:generate': '任务拆解',
    'ai:meeting:parse': '会议解析',
    'ai:report:generate': '生成周报',
    'ai:risk:analyze': '风险分析',
    'knowledge:doc:upload': '上传文档',
    'knowledge:doc:read': '查看文档',
    'knowledge:rag:query': 'RAG 检索',
    'analytics:dashboard:read': '查看看板',
    'analytics:member:read': '成员统计'
  }
  return map[p] || p
}

function projectRoleText(role: string) {
  const map: Record<string, string> = {
    MANAGER: '管理员',
    MEMBER: '成员',
    OBSERVER: '观察者'
  }
  return map[role] || role
}

function projectRoleTagType(role: string) {
  if (role === 'MANAGER') return 'success'
  if (role === 'MEMBER') return ''
  return 'info'
}

function projectRolePerms(role: string) {
  if (role === 'MANAGER')
    return '项目管理、成员管理、看板配置、任务增删改、AI 功能、知识库'
  if (role === 'MEMBER')
    return '任务增删改、评论、拖拽看板'
  return '只读：查看任务、动态、成员，不可修改'
}

function goBoard(id: number) {
  router.push(`/projects/${id}/board`)
}

onMounted(async () => {
  try {
    const res = await meApi.profile()
    Object.assign(profile, res.data)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 860px;
  margin: 0 auto;
}
.card-section {
  margin-bottom: 18px;
  border-radius: 14px;
  border: 1px solid rgba(120,115,100,0.08);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #5a5a52;
}
.role-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.role-tag {
  font-size: 14px;
}
.role-desc {
  margin: 12px 0 0;
  color: #909399;
  font-size: 14px;
}
.perm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.perm-module {
  background: #fffdf9;
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(120,115,100,0.06);
}
.perm-module-title {
  font-weight: 600;
  font-size: 13px;
  color: #6c9a8b;
  margin-bottom: 8px;
}
.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.perm-desc {
  color: #909399;
  font-size: 13px;
}
</style>
