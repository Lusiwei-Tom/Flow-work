<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="22" color="#fff"><Connection /></el-icon>
        <span>FlowWork</span>
      </div>
      <el-menu :default-active="activeMenu" background-color="#1f2d3d"
        text-color="#bfcbd9" active-text-color="#409eff" router>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon><span>项目</span>
        </el-menu-item>
        <el-menu-item index="/my-tasks">
          <el-icon><List /></el-icon><span>我的任务</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon><span>我的角色</span>
        </el-menu-item>
        <template v-if="userStore.roles.includes('admin')">
          <el-menu-item index="/admin/invite-codes">
            <el-icon><Key /></el-icon><span>邀请码管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/applications">
            <el-icon><Tickets /></el-icon><span>注册审批</span>
          </el-menu-item>
        </template>
        <template v-if="currentProject">
          <el-menu-item :index="boardPath">
            <el-icon><Grid /></el-icon><span>任务看板</span>
          </el-menu-item>
          <el-menu-item :index="`/projects/${projectId}/ai`" v-permission="'ai:agent:execute'">
            <el-icon><ChatDotRound /></el-icon><span>AI 管家</span>
          </el-menu-item>
          <el-menu-item :index="`/projects/${projectId}/ai-tools`"
            v-permission="['ai:task:generate','ai:risk:analyze']">
            <el-icon><MagicStick /></el-icon><span>AI 工具</span>
          </el-menu-item>
          <el-menu-item :index="`/projects/${projectId}/knowledge`"
            v-permission="'knowledge:doc:read'">
            <el-icon><Reading /></el-icon><span>知识库</span>
          </el-menu-item>
          <el-menu-item :index="`/projects/${projectId}/analytics`"
            v-permission="'analytics:dashboard:read'">
            <el-icon><DataAnalysis /></el-icon><span>数据看板</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">
          {{ currentProject ? currentProject.name : 'AI 驱动团队项目协作平台' }}
        </div>
        <div class="header-right">
          <NotificationBell />
          <el-dropdown @command="onCommand">
            <span class="user-area">
              <el-avatar :size="30">{{ userStore.userInfo?.nickname?.[0] || 'U' }}</el-avatar>
              <span style="margin-left:8px">{{ userStore.userInfo?.nickname }}</span>
              <el-tag size="small" type="info" style="margin-left:8px"
                v-if="userStore.roles[0]">{{ roleText }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import authApi from '@/api/auth'
import projectApi, { type Project } from '@/api/project'
import NotificationBell from '@/components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentProject = ref<Project | null>(null)

const projectId = computed(() => Number(route.params.id))
const boardPath = computed(() => `/projects/${projectId.value}/board`)
const activeMenu = computed(() => route.path)
const roleText = computed(() => {
  const map: Record<string, string> = {
    admin: '管理员', project_manager: '项目管理员', member: '成员'
  }
  return map[userStore.roles[0]] || userStore.roles[0]
})

watch(projectId, async (id) => {
  currentProject.value = null
  if (id) {
    try {
      currentProject.value = await projectApi.detail(id)
    } catch { /* ignore */ }
  }
}, { immediate: true })

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    try {
      await authApi.logout(localStorage.getItem('refreshToken') || '')
    } finally {
      userStore.clearAuth()
      router.replace('/login')
    }
  }
}
</script>

<style scoped>
.aside {
  background: linear-gradient(180deg, #3a4a3c, #344335);
  overflow-y: auto;
  border-right: 1px solid rgba(255,255,255,0.04);
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 20px;
  color: #e8e6e0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
:deep(.el-menu) {
  border-right: none;
  background: transparent !important;
}
:deep(.el-menu-item) {
  border-radius: 8px;
  margin: 2px 8px;
  transition: all 0.25s ease;
}
:deep(.el-menu-item:hover) {
  background: rgba(255,255,255,0.06) !important;
}
:deep(.el-menu-item.is-active) {
  background: rgba(108,154,139,0.15) !important;
}
.header {
  background: #fffdf9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(80,76,60,0.05);
  border-bottom: 1px solid rgba(120,115,100,0.06);
}
.header-title { font-weight: 600; font-size: 16px; color: #5a5a52; }
.header-right { display: flex; align-items: center; gap: 18px; }
.user-area { display: flex; align-items: center; cursor: pointer; transition: opacity 0.2s; }
.user-area:hover { opacity: 0.8; }
.main {
  background: #f5f3ef;
  padding: 18px;
  transition: background 0.4s ease;
}
</style>
