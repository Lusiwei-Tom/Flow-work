<template>
  <el-drawer v-model="visible" title="项目成员" size="420px">
    <!-- 邀请成员（仅项目 MANAGER） -->
    <div v-if="myRole === 'MANAGER'" class="invite">
      <el-input
        v-model="keyword"
        placeholder="输入用户名搜索并邀请"
        clearable
        @input="onSearch"
      >
        <template #append>
          <el-button @click="invite">邀请</el-button>
        </template>
      </el-input>
      <div v-if="candidates.length > 0" class="candidates">
        <div
          v-for="c in candidates"
          :key="c.id"
          class="candidate"
          @click="pick(c)"
        >
          <el-icon><User /></el-icon>
          <span>{{ c.nickname }}（{{ c.username }}）</span>
        </div>
      </div>
      <el-radio-group v-model="inviteRole" size="small" style="margin-top: 8px">
        <el-radio-button value="MEMBER">成员</el-radio-button>
        <el-radio-button value="MANAGER">管理员</el-radio-button>
        <el-radio-button value="OBSERVER">观察者</el-radio-button>
      </el-radio-group>
      <el-divider />
    </div>

    <!-- 成员列表 -->
    <div v-if="members.length === 0" class="empty">暂无成员</div>
    <div v-for="m in members" :key="m.userId" class="member">
      <el-avatar :size="32">{{ (m.nickname || m.username)?.[0]?.toUpperCase() }}</el-avatar>
      <div class="member-info">
        <div class="member-name">{{ m.nickname || m.username }}</div>
        <div class="member-username">@{{ m.username }}</div>
      </div>
      <template v-if="myRole === 'MANAGER'">
        <el-select
          :model-value="m.role"
          size="small"
          style="width: 96px"
          @change="(r: string) => changeRole(m, r)"
        >
          <el-option label="管理员" value="MANAGER" />
          <el-option label="成员" value="MEMBER" />
          <el-option label="观察者" value="OBSERVER" />
        </el-select>
        <el-button link type="danger" size="small" @click="remove(m)">移除</el-button>
      </template>
      <el-tag v-else size="small" :type="roleTag(m.role)">{{ roleText(m.role) }}</el-tag>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { memberApi } from '@/api/project'
import { userApi, type MemberInfo } from '@/api/notify'

const visible = ref(false)
const projectId = ref(0)
const members = ref<MemberInfo[]>([])
const myRole = ref('MEMBER')
const keyword = ref('')
const inviteRole = ref('MEMBER')
const candidates = ref<{ id: number; username: string; nickname: string }[]>([])
const picked = ref<{ id: number; username: string; nickname: string } | null>(null)
let searchTimer: number | undefined

function open(pid: number, role: string) {
  projectId.value = pid
  myRole.value = role
  visible.value = true
  keyword.value = ''
  candidates.value = []
  picked.value = null
  load()
}

async function load() {
  try {
    members.value = await memberApi.list(projectId.value)
  } catch { /* ignore */ }
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  picked.value = null
  const kw = keyword.value.trim()
  if (!kw) { candidates.value = []; return }
  searchTimer = window.setTimeout(async () => {
    try {
      candidates.value = (await userApi.search(kw)).filter(
        (u) => !members.value.some((m) => m.userId === u.id)
      )
    } catch { candidates.value = [] }
  }, 300)
}

function pick(c: { id: number; username: string; nickname: string }) {
  picked.value = c
  keyword.value = c.username
  candidates.value = []
}

async function invite() {
  if (!picked.value) {
    ElMessage.warning('请先从搜索结果中选择用户')
    return
  }
  try {
    await memberApi.add(projectId.value, { username: picked.value.username, projectRole: inviteRole.value })
    ElMessage.success('已邀请')
    keyword.value = ''
    picked.value = null
    load()
  } catch { /* http 层已提示 */ }
}

async function changeRole(m: MemberInfo, role: string) {
  try {
    await memberApi.updateRole(projectId.value, m.userId, role)
    ElMessage.success('角色已更新')
    load()
  } catch { /* ignore */ }
}

async function remove(m: MemberInfo) {
  try {
    await ElMessageBox.confirm(`确定移除成员「${m.nickname || m.username}」？`, '提示', { type: 'warning' })
    await memberApi.remove(projectId.value, m.userId)
    ElMessage.success('已移除')
    load()
  } catch { /* 取消或失败 */ }
}

function roleText(r: string) {
  const map: Record<string, string> = { MANAGER: '管理员', MEMBER: '成员', OBSERVER: '观察者' }
  return map[r] || r
}
function roleTag(r: string) {
  if (r === 'MANAGER') return 'warning'
  if (r === 'OBSERVER') return 'info'
  return 'success'
}

defineExpose({ open })
</script>

<style scoped>
.invite { margin-bottom: 8px; }
.candidates { border: 1px solid #e4e7ed; border-radius: 4px; margin-top: 4px; max-height: 140px; overflow-y: auto; }
.candidate { padding: 6px 10px; font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 6px; }
.candidate:hover { background: #f5f7fa; }
.empty { text-align: center; color: #999; padding: 32px 0; font-size: 13px; }
.member { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.member-info { flex: 1; min-width: 0; }
.member-name { font-size: 14px; color: #303133; }
.member-username { font-size: 12px; color: #999; }
</style>
