<template>
  <el-popover placement="bottom-end" :width="360" trigger="click" @show="loadList">
    <template #reference>
      <el-badge :value="unread" :hidden="unread === 0" :max="99" class="bell">
        <el-icon :size="20" color="#606266"><Bell /></el-icon>
      </el-badge>
    </template>

    <div class="head">
      <span class="head-title">通知中心</span>
      <el-button link type="primary" size="small" :disabled="unread === 0" @click="readAll">
        全部已读
      </el-button>
    </div>

    <el-scrollbar max-height="360px">
      <div v-if="list.length === 0" class="empty">暂无通知</div>
      <div
        v-for="n in list"
        :key="n.id"
        class="item"
        :class="{ unread: n.isRead === 0 }"
        @click="onClick(n)"
      >
        <div class="item-row">
          <el-tag size="small" :type="tagType(n.type)" class="item-tag">{{ tagText(n.type) }}</el-tag>
          <span class="item-title">{{ n.title }}</span>
          <span class="item-time">{{ shortTime(n.createdAt) }}</span>
        </div>
        <div class="item-content">{{ n.content }}</div>
      </div>
    </el-scrollbar>
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notifyApi, type Notification } from '@/api/notify'
import { useNotificationSocket } from '@/composables/useNotificationSocket'

const router = useRouter()
const unread = ref(0)
const list = ref<Notification[]>([])

async function loadCount() {
  try {
    unread.value = (await notifyApi.unreadCount()).count
  } catch { /* ignore */ }
}

async function loadList() {
  try {
    list.value = await notifyApi.list()
    loadCount()
  } catch { /* ignore */ }
}

async function onClick(n: Notification) {
  if (n.isRead === 0) {
    try { await notifyApi.markRead(n.id) } catch { /* ignore */ }
    loadCount()
  }
  if (n.projectId) {
    router.push(`/projects/${n.projectId}/board`)
  }
}

async function readAll() {
  await notifyApi.markAllRead()
  ElMessage.success('已全部标记为已读')
  loadList()
}

function tagType(t: string) {
  if (t === 'TASK_OVERDUE') return 'danger'
  if (t === 'TASK_DUE_SOON') return 'warning'
  if (t === 'TASK_ASSIGNED') return 'primary'
  return 'info'
}

function tagText(t: string) {
  const map: Record<string, string> = {
    TASK_ASSIGNED: '分配',
    TASK_COMMENTED: '评论',
    TASK_DUE_SOON: '临期',
    TASK_OVERDUE: '逾期',
    MEMBER_JOINED: '成员'
  }
  return map[t] || '通知'
}

function shortTime(s: string) {
  if (!s) return ''
  const d = new Date(s.replace(' ', 'T'))
  const diff = Date.now() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  return `${d.getMonth() + 1}-${d.getDate()}`
}

// 实时推送：刷新未读数并弹窗提醒
const { connect } = useNotificationSocket((n) => {
  unread.value++
  ElMessage({
    message: `[${n.title}] ${n.content || ''}`,
    type: n.type === 'TASK_OVERDUE' ? 'error' : 'info',
    duration: 4000
  })
})

onMounted(() => {
  loadCount()
  connect()
})
</script>

<style scoped>
.bell { cursor: pointer; display: flex; align-items: center; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.head-title { font-weight: 600; font-size: 14px; }
.empty { text-align: center; color: #999; padding: 24px 0; font-size: 13px; }
.item { padding: 8px 4px; border-bottom: 1px solid #f0f0f0; cursor: pointer; border-radius: 4px; }
.item:hover { background: #f5f7fa; }
.item.unread .item-title { font-weight: 600; }
.item-row { display: flex; align-items: center; gap: 6px; }
.item-tag { flex-shrink: 0; }
.item-title { font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-time { margin-left: auto; font-size: 11px; color: #999; flex-shrink: 0; }
.item-content { font-size: 12px; color: #666; margin-top: 2px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
