<template>
  <div class="appl-page">
    <el-card class="card-section" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon size="18"><List /></el-icon>
          <span>注册申请审批</span>
          <el-radio-group v-model="filter" style="margin-left:auto" size="small" @change="load">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待审批</el-radio-button>
            <el-radio-button label="approved">已通过</el-radio-button>
            <el-radio-button label="rejected">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="申请人" width="120" prop="applicantName" />
        <el-table-column label="手机号" width="130" prop="phone">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" width="170" prop="email">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="申请理由" min-width="220" prop="reason" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="邀请码" width="170">
          <template #default="{ row }">
            <el-typography v-if="row.inviteCode" copyable>{{ row.inviteCode }}</el-typography>
            <span v-else style="color:#c0c4cc">—</span>
          </template>
        </el-table-column>
        <el-table-column label="审批备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewNote || '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="160" prop="createdAt" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="success" @click="onApprove(row.id)">通过</el-button>
              <el-button size="small" type="danger" @click="onReject(row.id)">拒绝</el-button>
            </template>
            <span v-else style="color:#c0c4cc">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import applicationApi, { type RegApplicationVO } from '@/api/application'

const loading = ref(true)
const list = ref<RegApplicationVO[]>([])
const filter = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await applicationApi.list(filter.value)
  } finally {
    loading.value = false
  }
}

async function onApprove(id: number) {
  const { value } = await ElMessageBox.prompt('审批备注（可选）', '通过申请', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    inputPlaceholder: '请输入备注',
    inputValue: ''
  })
  const res = await applicationApi.approve(id, value)
  ElMessage.success(`已通过，邀请码：${res.inviteCode}`)
  await load()
}

async function onReject(id: number) {
  const { value } = await ElMessageBox.prompt('拒绝原因', '拒绝申请', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    inputPlaceholder: '请输入拒绝原因',
    inputValidator: (v) => (v && v.trim() ? true : '请填写拒绝原因'),
    inputValue: ''
  })
  await applicationApi.reject(id, value)
  ElMessage.success('已拒绝')
  await load()
}

function statusText(s: string) {
  return { pending: '待审批', approved: '已通过', rejected: '已拒绝' }[s] || s
}
function statusType(s: string) {
  return s === 'approved' ? 'success' : s === 'rejected' ? 'danger' : 'warning'
}

onMounted(load)
</script>

<style scoped>
.appl-page { max-width: 1200px; margin: 0 auto; }
.card-section { margin-bottom: 18px; border-radius: 14px; border: 1px solid rgba(120,115,100,0.08); }
.card-header { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #5a5a52; }
</style>
