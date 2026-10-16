<template>
  <el-tabs v-model="activeTab" type="border-card">
    <!-- AI 任务拆解 -->
    <el-tab-pane label="AI 任务拆解" name="breakdown">
      <el-card v-permission="'ai:task:generate'">
        <el-input v-model="requirement" type="textarea" :rows="5"
          placeholder="描述你的项目需求，例如：我要做一个电商小程序，包含用户登录、商品浏览、购物车、订单支付、后台管理..." />
        <div style="margin-top:12px;text-align:right">
          <el-button type="primary" :loading="loading.breakdown" :icon="MagicStick"
            @click="onBreakdown">一键拆解为任务并入库</el-button>
        </div>
        <el-alert v-if="resultTasks.length" type="success" :closable="false" style="margin-top:12px"
          :title="`已生成 ${resultTasks.length} 个任务，可在看板中查看（AI 标签）`" />
        <el-table v-if="resultTasks.length" :data="resultTasks" size="small" style="margin-top:8px">
          <el-table-column prop="title" label="任务" min-width="240" />
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">{{ pText(row.priority) }}</template>
          </el-table-column>
          <el-table-column prop="estimatedHours" label="估时(h)" width="90" />
        </el-table>
      </el-card>
    </el-tab-pane>

    <!-- 会议纪要 -->
    <el-tab-pane label="会议纪要转任务" name="meeting">
      <el-card v-permission="'ai:meeting:parse'">
        <el-input v-model="meetingText" type="textarea" :rows="6"
          placeholder="粘贴会议记录/纪要文本，AI 将提取行动项生成任务（含负责人、截止日期）" />
        <div style="margin-top:12px;text-align:right">
          <el-button type="primary" :loading="loading.meeting" :icon="Document"
            @click="onMeeting">解析并生成任务</el-button>
        </div>
        <el-table v-if="meetingTasks.length" :data="meetingTasks" size="small" style="margin-top:8px">
          <el-table-column prop="title" label="行动项" min-width="220" />
          <el-table-column prop="dueDate" label="截止" width="120" />
        </el-table>
      </el-card>
    </el-tab-pane>

    <!-- 风险分析 -->
    <el-tab-pane label="项目风险分析" name="risk">
      <el-card v-permission="'ai:risk:analyze'">
        <el-space style="margin-bottom:12px">
          <el-button type="danger" plain :loading="loading.risk" :icon="Warning"
            @click="onRisk">运行规则引擎 + AI 风险分析</el-button>
          <el-tag v-if="risk" :type="riskTag" size="large">风险等级：{{ risk.level }} · 评分 {{ risk.score }}</el-tag>
        </el-space>
        <el-table v-if="risk" :data="risk.items" size="small">
          <el-table-column prop="rule" label="命中规则" width="200" />
          <el-table-column label="级别" width="90">
            <template #default="{ row }">
              <el-tag :type="row.level === 'HIGH' ? 'danger' : row.level === 'MEDIUM' ? 'warning' : 'info'"
                size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" />
        </el-table>
        <el-alert v-if="risk" type="warning" :closable="false" style="margin-top:12px">
          <template #title>AI 应对建议</template>
          <div style="white-space:pre-wrap">{{ risk.aiSuggestion }}</div>
        </el-alert>
        <el-empty v-if="risk && risk.items.length === 0" description="规则引擎未发现风险项" />
      </el-card>
    </el-tab-pane>

    <!-- 周报 -->
    <el-tab-pane label="自动周报" name="report">
      <el-card v-permission="'ai:report:generate'">
        <el-button type="primary" :loading="loading.report" :icon="Tickets"
          @click="onReport">生成本周项目周报</el-button>
        <div v-if="report" style="margin-top:16px">
          <h3>{{ report.title }}</h3>
          <div class="report-content" v-html="renderMd(report.content)"></div>
        </div>
      </el-card>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Document, Warning, Tickets } from '@element-plus/icons-vue'
import aiApi from '@/api/ai'
import type { Task } from '@/api/project'

const route = useRoute()
const projectId = Number(route.params.id)
const activeTab = ref('breakdown')

const requirement = ref('')
const meetingText = ref('')
const resultTasks = ref<Task[]>([])
const meetingTasks = ref<Task[]>([])
const risk = ref<any>(null)
const report = ref<any>(null)
const loading = ref<Record<string, boolean>>({} as any)

const riskTag = computed(() => {
  if (!risk.value) return 'info'
  const map: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }
  return map[risk.value.level] || 'info'
})

function pText(p: number) {
  return { 1: '低', 2: '中', 3: '高', 4: '紧急' }[p]
}

async function onBreakdown() {
  if (!requirement.value.trim()) return ElMessage.warning('请先描述项目需求')
  loading.value.breakdown = true
  try {
    resultTasks.value = await aiApi.breakdown(projectId, requirement.value)
    ElMessage.success(`已生成 ${resultTasks.value.length} 个任务`)
  } finally {
    loading.value.breakdown = false
  }
}

async function onMeeting() {
  if (!meetingText.value.trim()) return ElMessage.warning('请粘贴会议记录')
  loading.value.meeting = true
  try {
    meetingTasks.value = await aiApi.parseMeeting(projectId, meetingText.value)
    ElMessage.success(`已生成 ${meetingTasks.value.length} 个行动项任务`)
  } finally {
    loading.value.meeting = false
  }
}

async function onRisk() {
  loading.value.risk = true
  try {
    risk.value = await aiApi.analyzeRisk(projectId)
  } finally {
    loading.value.risk = false
  }
}

async function onReport() {
  loading.value.report = true
  try {
    report.value = await aiApi.weeklyReport(projectId)
  } finally {
    loading.value.report = false
  }
}

/** 极简 Markdown 渲染（标题/列表/加粗） */
function renderMd(md: string) {
  if (!md) return ''
  return md
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^\s*[-*] (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g, (m) => `<ul>${m}</ul>`)
    .replace(/\n/g, '<br/>')
    .replace(/<br\/><(h[234]|ul)/g, '<$1')
    .replace(/(h[234]|\/ul)><br\/>/g, '$1>')
}
</script>

<style scoped>
.report-content { line-height: 1.8; }
.report-content :deep(h2) { font-size: 18px; margin: 14px 0 8px; }
.report-content :deep(h3) { font-size: 16px; margin: 12px 0 6px; }
.report-content :deep(ul) { padding-left: 22px; }
</style>
