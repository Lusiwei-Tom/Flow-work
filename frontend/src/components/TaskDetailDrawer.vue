<template>
  <el-drawer v-model="visible" size="420px" :title="`任务 #${task?.id || ''}`" @closed="visible = false">
    <template v-if="task">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option :value="1" label="低" />
            <el-option :value="2" label="中" />
            <el-option :value="3" label="高" />
            <el-option :value="4" label="紧急" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" clearable placeholder="未分配" style="width:100%">
            <el-option v-for="m in members" :key="m.userId" :value="m.userId"
              :label="memberNames[m.userId] || ('成员#' + m.userId)" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD"
            style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">待办</el-radio-button>
            <el-radio-button :value="2">进行中</el-radio-button>
            <el-radio-button :value="3">已完成</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-tag v-if="task.aiGenerated === 1" type="warning">AI 生成 · {{ task.aiSource }}</el-tag>
      </el-form>

      <el-divider>评论</el-divider>
      <div v-for="c in comments" :key="c.id" class="comment">
        <el-avatar :size="26">{{ memberNames[c.userId]?.[0] || 'U' }}</el-avatar>
        <div class="comment-body">
          <div class="comment-meta">{{ memberNames[c.userId] || '成员#' + c.userId }} · {{ c.createdAt }}</div>
          <div>{{ c.content }}</div>
        </div>
      </div>
      <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="50" />
      <div style="display:flex;gap:8px;margin-top:12px">
        <el-input v-model="newComment" placeholder="写评论..." @keyup.enter="addComment" />
        <el-button type="primary" @click="addComment">发送</el-button>
      </div>
    </template>
    <template #footer>
      <el-button v-permission="'project:task:delete'" type="danger" plain @click="onDelete">删除</el-button>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" v-permission="'project:task:update'" :loading="saving" @click="onSave">
        保存
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, type Task } from '@/api/project'

const props = defineProps<{
  modelValue: boolean
  taskId: number | null
  members: any[]
  memberNames: Record<number, string>
}>()
const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  saved: []
  deleted: []
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => (visible.value = v))
watch(visible, (v) => emit('update:modelValue', v))

const task = ref<Task | null>(null)
const comments = ref<any[]>([])
const newComment = ref('')
const saving = ref(false)
const form = reactive<any>({})

watch(
  () => props.taskId,
  async (id) => {
    if (!id) return
    const t = await taskApi.detail(id)
    task.value = t
    comments.value = await taskApi.comments(id)
    Object.assign(form, {
      title: t.title,
      description: t.description,
      priority: t.priority,
      assigneeId: t.assigneeId,
      dueDate: t.dueDate,
      status: t.status
    })
  }
)

async function onSave() {
  if (!task.value) return
  saving.value = true
  try {
    await taskApi.update(task.value.id, { ...form })
    ElMessage.success('已保存')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

async function addComment() {
  if (!newComment.value || !task.value) return
  await taskApi.addComment(task.value.id, newComment.value)
  newComment.value = ''
  comments.value = await taskApi.comments(task.value.id)
}

async function onDelete() {
  await ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' })
  await taskApi.remove(task.value!.id)
  ElMessage.success('已删除')
  visible.value = false
  emit('deleted')
}
</script>

<style scoped>
.comment { display: flex; gap: 8px; margin-bottom: 12px; }
.comment-body { flex: 1; }
.comment-meta { font-size: 12px; color: #909399; margin-bottom: 2px; }
</style>
