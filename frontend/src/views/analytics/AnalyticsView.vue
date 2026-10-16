<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">任务总数</div>
          <div class="stat-value">{{ stats.totalTasks ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">完成率</div>
          <div class="stat-value success">{{ stats.completionRate ?? 0 }}%</div>
          <el-progress :percentage="stats.completionRate ?? 0" :stroke-width="8" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">逾期任务</div>
          <div class="stat-value" :class="{ danger: (stats.overdueCount ?? 0) > 0 }">
            {{ stats.overdueCount ?? 0 }}
          </div>
          <div class="stat-sub">3天内到期：{{ stats.dueSoonCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">项目健康度</div>
          <div class="stat-value" :class="healthClass">{{ stats.healthScore ?? 100 }}</div>
          <div class="stat-sub">{{ stats.healthLevel }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><strong>任务状态分布</strong></template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="待办">{{ stats.todo ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="进行中">{{ stats.doing ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ stats.finished ?? 0 }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:14px;display:flex;height:28px;border-radius:6px;overflow:hidden">
            <div class="seg todo" :style="{ width: segWidth(stats.todo) }">待办 {{ stats.todo ?? 0 }}</div>
            <div class="seg doing" :style="{ width: segWidth(stats.doing) }">进行中 {{ stats.doing ?? 0 }}</div>
            <div class="seg done" :style="{ width: segWidth(stats.finished) }">完成 {{ stats.finished ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><strong>近 7 天完成趋势</strong></template>
          <div class="chart-bars">
            <div v-for="d in (stats.dailyFinished || [])" :key="d.d" class="bar-item">
              <div class="bar" :style="{ height: barHeight(d.cnt) }"></div>
              <div class="bar-label">{{ String(d.d).slice(5) }}</div>
              <div class="bar-cnt">{{ d.cnt }}</div>
            </div>
            <el-empty v-if="!(stats.dailyFinished || []).length" description="近7天暂无完成记录"
              :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:16px">
      <template #header><strong>成员任务统计</strong></template>
      <el-table :data="memberRows" size="small">
        <el-table-column label="成员">
          <template #default="{ row }">{{ row.nickname || ('成员#' + row.userId) }}</template>
        </el-table-column>
        <el-table-column prop="total" label="任务总数" width="100" />
        <el-table-column prop="finished" label="已完成" width="100" />
        <el-table-column label="完成情况" min-width="240">
          <template #default="{ row }">
            <el-progress :percentage="row.total ? Math.round(100 * row.finished / row.total) : 0" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import analyticsApi from '@/api/analytics'

const route = useRoute()
const projectId = Number(route.params.id)
const stats = ref<any>({})
const members = ref<any>({ members: [] })
const loading = ref(false)

const memberRows = computed(() => members.value.members || [])
const healthClass = computed(() => {
  const s = stats.value.healthScore ?? 100
  return s >= 80 ? 'success' : s >= 60 ? 'warning' : 'danger'
})

function segWidth(n?: number) {
  const total = (stats.value.totalTasks ?? 0)
  return total ? `${(100 * (n || 0) / total)}%` : '0%'
}
function barHeight(n: number) {
  const max = Math.max(1, ...(stats.value.dailyFinished || []).map((d: any) => d.cnt))
  return `${Math.max(6, 120 * n / max)}px`
}

onMounted(async () => {
  loading.value = true
  try {
    const [dash, mem] = await Promise.all([
      analyticsApi.dashboard(projectId),
      analyticsApi.members(projectId)
    ])
    stats.value = dash
    members.value = mem
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 30px; font-weight: 700; margin: 6px 0; }
.stat-value.success { color: #67c23a; }
.stat-value.danger, .stat-value.warning { color: #f56c6c; }
.stat-sub { color: #c0c4cc; font-size: 12px; }
.seg { color: #fff; font-size: 12px; text-align: center; line-height: 28px; }
.seg.todo { background: #909399; }
.seg.doing { background: #409eff; }
.seg.done { background: #67c23a; }
.chart-bars { display: flex; gap: 14px; align-items: flex-end; height: 170px; padding: 8px; }
.bar-item { display: flex; flex-direction: column; align-items: center; }
.bar { width: 26px; background: linear-gradient(180deg, #79bbff, #409eff); border-radius: 4px 4px 0 0; }
.bar-label { font-size: 11px; color: #909399; margin-top: 4px; }
.bar-cnt { font-size: 12px; font-weight: 600; }
</style>
