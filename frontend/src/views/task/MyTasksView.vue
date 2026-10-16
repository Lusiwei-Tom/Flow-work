<template>
  <div>
    <div class="page-head">
      <h2>我的任务</h2>
      <span class="sub">跨项目聚合 · 共 {{ tasks.length }} 个</span>
    </div>

    <div v-if="groups.length === 0" class="empty">
      <el-empty description="暂时没有分配给你的任务" />
    </div>

    <div v-for="g in groups" :key="g.title" class="group">
      <div class="group-title">
        {{ g.title }}
        <el-tag size="small" type="info">{{ g.items.length }}</el-tag>
      </div>
      <div v-for="t in g.items" :key="t.id" class="task-row" @click="goBoard(t)">
        <el-tag size="small" :type="prioTag(t.priority)" class="prio">{{ prioText(t.priority) }}</el-tag>
        <span class="task-title" :class="{ done: t.status === 3 }">{{ t.title }}</span>
        <span class="task-project">
          <el-icon><Folder /></el-icon>{{ t.project_name }}
        </span>
        <span class="task-due" :class="{ overdue: isOverdue(t) }">
          <el-icon><Calendar /></el-icon>{{ t.dueDate ? t.dueDate : '无截止日期' }}
        </span>
        <el-select
          :model-value="t.status"
          size="small"
          class="status-select"
          @click.stop
          @mousedown.stop
          @change="(v: number) => changeStatus(t, v)"
        >
          <el-option :value="1" label="待办" />
          <el-option :value="2" label="进行中" />
          <el-option :value="3" label="已完成" />
        </el-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myTaskApi, taskApi } from '@/api/project'
import type { Task } from '@/api/project'

interface MyTask extends Task {
  project_name: string
}

const router = useRouter()
const tasks = ref<MyTask[]>([])

onMounted(reload)

async function reload() {
  try {
    tasks.value = await myTaskApi.list()
  } catch { /* http 层已提示 */ }
}

/** 行内直接切换状态（区别于行点击跳看板），成功后刷新分组 */
async function changeStatus(t: MyTask, status: number) {
  try {
    await taskApi.setStatus(t.id, status)
    await reload()
  } catch { /* http 层已提示 */ }
}

const groups = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const mk = (title: string, items: MyTask[]) => (items.length ? { title, items } : null)
  return [
    mk('已逾期', tasks.value.filter((t) => t.status !== 3 && t.dueDate && t.dueDate < today)),
    mk('进行中', tasks.value.filter((t) => t.status === 2 && !(t.dueDate && t.dueDate < today))),
    mk('待办', tasks.value.filter((t) => t.status === 1)),
    mk('已完成（近 7 天）', tasks.value.filter((t) => t.status === 3))
  ].filter(Boolean) as { title: string; items: MyTask[] }[]
})

function isOverdue(t: MyTask) {
  const today = new Date().toISOString().slice(0, 10)
  return t.status !== 3 && !!t.dueDate && t.dueDate < today
}

function goBoard(t: MyTask) {
  router.push(`/projects/${t.projectId}/board`)
}

function prioText(p: number) {
  const map: Record<number, string> = { 1: '低', 2: '中', 3: '高', 4: '紧急' }
  return map[p] || '中'
}
function prioTag(p: number) {
  const map: Record<number, string> = { 1: 'info', 2: 'primary', 3: 'warning', 4: 'danger' }
  return (map[p] || 'primary') as 'info' | 'primary' | 'warning' | 'danger'
}
</script>

<style scoped>
.page-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 16px; }
.page-head h2 { margin: 0; }
.sub { color: #999; font-size: 13px; }
.empty { background: #fff; border-radius: 8px; }
.group { margin-bottom: 18px; }
.group-title { font-weight: 600; margin-bottom: 8px; display: flex; align-items: center; gap: 6px; }
.task-row {
  display: flex; align-items: center; gap: 10px;
  background: #fff; border-radius: 6px; padding: 10px 14px; margin-bottom: 6px;
  cursor: pointer; transition: box-shadow .15s;
}
.task-row:hover { box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.task-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-title.done { color: #999; text-decoration: line-through; }
.task-project, .task-due { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #666; flex-shrink: 0; }
.status-select { width: 92px; flex-shrink: 0; }
.task-due.overdue { color: #f56c6c; font-weight: 600; }
.prio { flex-shrink: 0; }
</style>
