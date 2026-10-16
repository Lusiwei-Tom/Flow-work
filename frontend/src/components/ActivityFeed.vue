<template>
  <div class="activity-feed">
    <div class="feed-title">
      <el-icon><Bell /></el-icon> 项目动态
      <el-tag size="small" v-if="connected" type="success" effect="plain">实时</el-tag>
      <el-tag size="small" v-else type="info" effect="plain">连接中</el-tag>
    </div>
    <el-timeline>
      <el-timeline-item v-for="a in activities" :key="a.id || (a.createdAt + a.type)"
        :timestamp="a.createdAt" placement="top"
        :type="a.aiGenerated === 1 ? 'warning' : 'primary'">
        <strong>{{ a.actorName || (a.actorId === 0 ? '系统' : '成员') }}</strong>
        {{ a.content || typeText(a.type) }}
        <el-tag v-if="a.aiGenerated === 1" size="small" type="warning" effect="plain"
          style="margin-left:6px">AI</el-tag>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="activities.length === 0" description="暂无动态" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import type { Activity } from '@/api/project'

defineProps<{
  activities: Activity[]
  connected: boolean
}>()

function typeText(type: string) {
  const map: Record<string, string> = {
    PROJECT_CREATED: '创建了项目',
    TASK_CREATED: '创建了任务',
    TASK_UPDATED: '更新了任务',
    TASK_STATUS_CHANGED: '变更了任务状态',
    TASK_PRIORITY_CHANGED: '调整了优先级',
    TASK_ASSIGNED: '分配了任务',
    TASK_COMMENTED: '评论了任务',
    KANBAN_DRAG: '拖拽了任务',
    AI_TASK_BREAKDOWN: 'AI 拆解了任务',
    AI_MEETING_PARSE: 'AI 解析会议生成任务',
    AI_RISK_ANALYSIS: 'AI 分析了项目风险',
    AI_WEEKLY_REPORT: 'AI 生成了周报',
    MEMBER_ADDED: '添加了成员'
  }
  return map[type] || type
}
</script>

<style scoped>
.activity-feed { padding: 8px; }
.feed-title { display: flex; align-items: center; gap: 6px; font-weight: 600; margin-bottom: 12px; }
</style>
