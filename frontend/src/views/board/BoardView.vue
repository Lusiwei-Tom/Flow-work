<template>
  <div v-loading="loading">
    <div class="toolbar">
      <el-space wrap>
        <el-button type="primary" :icon="Plus" @click="createDialog = true"
          v-permission="'project:task:create'">新建任务</el-button>
        <el-button type="warning" plain :icon="MagicStick"
          v-permission="'ai:task:generate'" @click="$router.push(`/projects/${projectId}/ai-tools`)">
          AI 任务拆解
        </el-button>
        <el-button :icon="ChatDotRound" @click="$router.push(`/projects/${projectId}/ai`)"
          v-permission="'ai:agent:execute'">问 AI 管家</el-button>
        <el-input v-model="filter.keyword" placeholder="搜索任务标题/描述" clearable
          style="width:200px" :prefix-icon="Search" @keyup.enter="reloadTasks" @clear="reloadTasks" />
        <el-select v-model="filter.assigneeId" placeholder="负责人" clearable style="width:130px"
          @change="reloadTasks">
          <el-option v-for="m in members" :key="m.userId" :value="m.userId"
            :label="memberNames[m.userId] || ('成员#' + m.userId)" />
        </el-select>
        <el-select v-model="filter.priority" placeholder="优先级" clearable style="width:110px"
          @change="reloadTasks">
          <el-option :value="1" label="低" />
          <el-option :value="2" label="中" />
          <el-option :value="3" label="高" />
          <el-option :value="4" label="紧急" />
        </el-select>
        <el-select v-model="filter.status" placeholder="状态" clearable style="width:110px"
          @change="reloadTasks">
          <el-option :value="1" label="待办" />
          <el-option :value="2" label="进行中" />
          <el-option :value="3" label="已完成" />
        </el-select>
      </el-space>
      <el-space>
        <el-tag v-if="socketConnected" type="success" effect="plain">
          <el-icon><Connection /></el-icon> 实时已连接
        </el-tag>
        <el-tag v-else type="info" effect="plain">实时连接中...</el-tag>
        <el-button :icon="User" @click="openMembers">成员（{{ members.length }}）</el-button>
        <el-button text @click="loadAll">刷新</el-button>
      </el-space>
    </div>

    <MemberDrawer ref="memberDrawerRef" @changed="loadAll" />

    <div style="display:flex; gap:12px">
      <div class="board-columns" style="flex:1">
        <div v-for="col in columns" :key="col.id" class="board-column">
          <div class="board-column-header">
            <span>{{ col.name }}（{{ tasksByColumn[col.id]?.length || 0 }}）</span>
          </div>
          <draggable
            class="board-column-body"
            :list="tasksByColumn[col.id] || []"
            group="tasks"
            item-key="id"
            :animation="180"
            @change="(e: any) => onChange(e, col.id)">
            <template #item="{ element }">
              <TaskCard :task="element" :member-names="memberNames" @open="openTask" />
            </template>
          </draggable>
        </div>
      </div>

      <el-aside width="320px" style="background:#fff;border-radius:8px;height:calc(100vh - 150px);overflow-y:auto">
        <ActivityFeed :activities="activities" :connected="socketConnected" />
      </el-aside>
    </div>

    <!-- 新建任务 -->
    <el-dialog v-model="createDialog" title="新建任务" width="520px">
      <el-form :model="newTask" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="newTask.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newTask.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="所属列">
          <el-select v-model="newTask.columnId">
            <el-option v-for="c in columns" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="newTask.priority">
            <el-option :value="1" label="低" />
            <el-option :value="2" label="中" />
            <el-option :value="3" label="高" />
            <el-option :value="4" label="紧急" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="newTask.assigneeId" clearable placeholder="未分配">
            <el-option v-for="m in members" :key="m.userId" :value="m.userId"
              :label="memberNames[m.userId] || ('成员#' + m.userId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="newTask.dueDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>

    <TaskDetailDrawer v-model="drawerVisible" :task-id="activeTaskId" :members="members"
      :member-names="memberNames" @saved="loadAll" @deleted="loadAll" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { ElMessage } from 'element-plus'
import { Plus, MagicStick, ChatDotRound, User, Search } from '@element-plus/icons-vue'
import projectApi, { boardApi, taskApi, memberApi, realtimeApi, type Task, type BoardColumn, type Activity } from '@/api/project'
import { useProjectSocket } from '@/composables/useProjectSocket'
import { useUserStore } from '@/stores/user'
import TaskCard from '@/components/TaskCard.vue'
import TaskDetailDrawer from '@/components/TaskDetailDrawer.vue'
import MemberDrawer from '@/components/MemberDrawer.vue'
import ActivityFeed from '@/components/ActivityFeed.vue'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)
const userStore = useUserStore()

const loading = ref(false)
const columns = ref<BoardColumn[]>([])
const tasks = ref<Task[]>([])
const activities = ref<Activity[]>([])
const members = ref<any[]>([])
const memberNames = ref<Record<number, string>>({})
const myRole = ref('MEMBER')
const memberDrawerRef = ref<InstanceType<typeof MemberDrawer> | null>(null)

const filter = ref<{ keyword: string; assigneeId: number | null; priority: number | null; status: number | null }>({
  keyword: '',
  assigneeId: null,
  priority: null,
  status: null
})

/** 仅按当前筛选条件重载任务列表（不重新拉列/成员/活动） */
async function reloadTasks() {
  try {
    tasks.value = await taskApi.list(projectId, filter.value)
  } catch {
    // 错误由拦截器提示
  }
}

function openMembers() {
  memberDrawerRef.value?.open(projectId, myRole.value)
}

const createDialog = ref(false)
const creating = ref(false)
const drawerVisible = ref(false)
const activeTaskId = ref<number | null>(null)
const newTask = ref<any>({ title: '', priority: 2, columnId: null })

const tasksByColumn = computed(() => {
  const map: Record<number, Task[]> = {}
  for (const col of columns.value) map[col.id] = []
  for (const t of tasks.value) {
    if (map[t.columnId]) map[t.columnId].push(t)
  }
  return map
})

async function loadAll(redirectOnError = true) {
  loading.value = true
  try {
    const [cols, ts, acts, mems, project] = await Promise.all([
      boardApi.columns(projectId),
      taskApi.list(projectId, filter.value),
      realtimeApi.activities(projectId),
      memberApi.list(projectId),
      projectApi.detail(projectId)
    ])
    columns.value = cols
    tasks.value = ts
    activities.value = acts
    members.value = mems
    const nameMap: Record<number, string> = {}
    for (const m of mems) {
      nameMap[m.userId] = m.nickname || m.username || ('成员' + m.userId)
    }
    memberNames.value = nameMap
    // 当前用户在项目内的角色（owner 视为 MANAGER）
    const meId = userStore.userInfo?.id
    const mine = mems.find((m: any) => m.userId === meId)
    myRole.value = project.ownerId === meId ? 'MANAGER' : (mine?.role || 'OBSERVER')
    if (!newTask.value.columnId && cols.length) newTask.value.columnId = cols[0].id
  } catch {
    // 具体错误（如“不是项目成员”）已由 http 拦截器统一提示；
    // 看板主数据加载失败时回到项目列表，避免页面空白卡死
    if (redirectOnError) router.replace('/projects')
  } finally {
    loading.value = false
  }
}

// 拖拽结束（vuedraggable moved/added 才上报，避免 added+removed 重复）
async function onChange(evt: any, targetColumnId: number) {
  if (evt.added) {
    const moved: Task = evt.added.element
    const sortOrder = evt.added.newIndex
    const optimistic = moved
    try {
      await taskApi.move(projectId, {
        taskId: moved.id,
        targetColumnId,
        sortOrder
      })
    } catch {
      ElMessage.error('移动失败，正在同步最新数据')
      await loadAll(false)
    }
  }
}

async function onCreate() {
  if (!newTask.value.title) {
    ElMessage.warning('请输入任务标题')
    return
  }
  creating.value = true
  try {
    await taskApi.create(projectId, newTask.value)
    ElMessage.success('任务已创建')
    createDialog.value = false
    newTask.value = { title: '', priority: 2, columnId: columns.value[0]?.id }
    await loadAll()
  } finally {
    creating.value = false
  }
}

function openTask(id: number) {
  activeTaskId.value = id
  drawerVisible.value = true
}

// ---- 实时事件 ----
let refreshTimer: any = null
function onSocketEvent(event: any) {
  if (event.type !== 'KANBAN_DRAG') {
    realtimeApi.activities(projectId).then((a) => (activities.value = a))
  }
  // 防抖刷新看板
  clearTimeout(refreshTimer)
  refreshTimer = setTimeout(loadAll, 600)
  realtimeApi.heartbeat(projectId).catch(() => {})
}

const { connected: socketConnected, connect, disconnect } = useProjectSocket(projectId, onSocketEvent)
let heartbeatTimer: any = null

onMounted(async () => {
  await loadAll()
  connect()
  heartbeatTimer = setInterval(() => realtimeApi.heartbeat(projectId).catch(() => {}), 25000)
})
onUnmounted(() => {
  clearInterval(heartbeatTimer)
  realtimeApi.leave(projectId).catch(() => {})
  disconnect()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
</style>
