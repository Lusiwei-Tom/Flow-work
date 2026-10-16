<template>
  <div class="task-card" @click="$emit('open', task.id)">
    <div>
      <span class="priority-bar" :style="{ background: priorityColor }"></span>
      <span class="title">{{ task.title }}</span>
      <el-tag v-if="task.aiGenerated === 1" size="small" type="warning" effect="plain"
        style="margin-left:4px">AI</el-tag>
    </div>
    <div class="footer">
      <el-tag size="small" :type="priorityTag" effect="plain">{{ priorityText }}</el-tag>
      <span class="due" :class="{ overdue: isOverdue }" v-if="task.dueDate">
        <el-icon><Calendar /></el-icon>{{ task.dueDate }}
      </span>
      <el-avatar :size="22" v-if="task.assigneeId" style="margin-left:auto">
        {{ assigneeText }}
      </el-avatar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Task } from '@/api/project'

const props = defineProps<{
  task: Task
  memberNames?: Record<number, string>
}>()

defineEmits<{ open: [id: number] }>()

const priorityColor = computed(() => ({
  1: '#909399', 2: '#409eff', 3: '#e6a23c', 4: '#f56c6c'
}[props.task.priority] || '#909399'))

const priorityText = computed(() => ({ 1: '低', 2: '中', 3: '高', 4: '紧急' }[props.task.priority]))
const priorityTag = computed(() => ({
  1: 'info', 2: 'primary', 3: 'warning', 4: 'danger'
}[props.task.priority] as 'info' | 'primary' | 'warning' | 'danger'))

const isOverdue = computed(() => {
  if (!props.task.dueDate || props.task.status === 3) return false
  return props.task.dueDate < new Date().toISOString().slice(0, 10)
})

const assigneeText = computed(() => {
  const name = props.memberNames?.[props.task.assigneeId!]
  return name ? name[0] : props.task.assigneeId
})
</script>

<style scoped>
.title { font-size: 14px; vertical-align: middle; }
.footer { display: flex; align-items: center; gap: 6px; margin-top: 8px; }
.due { display: inline-flex; align-items: center; gap: 3px; font-size: 12px; color: #909399; }
.due.overdue { color: #f56c6c; font-weight: 600; }
</style>
