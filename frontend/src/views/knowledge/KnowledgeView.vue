<template>
  <el-row :gutter="16">
    <el-col :span="13">
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <strong><el-icon><Reading /></el-icon> 项目文档</strong>
            <el-upload :show-file-list="false" :before-upload="onUpload" accept=".txt,.md,.pdf"
              v-permission="'knowledge:doc:upload'">
              <el-button type="primary" :loading="uploading" :icon="Upload">上传文档</el-button>
            </el-upload>
          </div>
        </template>
        <el-table :data="docs" v-loading="loading" size="small">
          <el-table-column prop="fileName" label="文件名" min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="分块" width="70" />
          <el-table-column label="大小" width="90">
            <template #default="{ row }">{{ (row.sizeBytes / 1024).toFixed(1) }}KB</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button text type="danger" @click="onDelete(row)"
                v-permission="'knowledge:doc:upload'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && docs.length === 0" description="支持 txt / md / pdf，上传后自动分块向量化" />
      </el-card>
    </el-col>

    <el-col :span="11">
      <el-card>
        <template #header>
          <strong><el-icon><ChatLineSquare /></el-icon> 知识库 RAG 问答</strong>
        </template>
        <el-input v-model="question" placeholder="基于已上传文档提问，如：项目的技术选型是什么？"
          @keyup.enter="onAsk">
          <template #append>
            <el-button v-if="!asking" @click="onAsk">提问</el-button>
            <el-button v-else type="danger" @click="stopAsk">停止</el-button>
          </template>
        </el-input>
        <div v-if="asking && !answer" style="margin-top:16px">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-if="answer" class="answer">{{ answer }}<span v-if="asking" class="cursor" /></div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import knowledgeApi, { type KnowledgeDoc } from '@/api/knowledge'
import aiApi from '@/api/ai'

const route = useRoute()
const projectId = Number(route.params.id)

const docs = ref<KnowledgeDoc[]>([])
const loading = ref(false)
const uploading = ref(false)
const question = ref('')
const answer = ref('')
const asking = ref(false)
let abortController: AbortController | null = null

/** 解析 SSE 流：按 \n\n 切事件，事件内多个 data: 行用 \n 连接 */
function consumeSse(reader: ReadableStreamDefaultReader<Uint8Array>, onChunk: (t: string) => void) {
  const decoder = new TextDecoder()
  let buffer = ''
  return new Promise<void>(async (resolve, reject) => {
    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const event = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 2)
          let text = ''
          for (const line of event.split('\n')) {
            if (line.startsWith('data:')) {
              const content = line.slice(5)
              text += (content.startsWith(' ') ? content.slice(1) : content) + '\n'
            }
          }
          if (text.endsWith('\n')) text = text.slice(0, -1)
          if (text) onChunk(text)
        }
      }
      resolve()
    } catch (e) {
      reject(e)
    }
  })
}

function statusType(s: string) {
  return ({ READY: 'success', PROCESSING: 'warning', FAILED: 'danger' } as Record<string, any>)[s] || 'info'
}

function statusText(s: string) {
  return ({ READY: '已索引', PROCESSING: '解析中', FAILED: '失败' } as Record<string, string>)[s] || s
}

async function load() {
  loading.value = true
  try {
    docs.value = await knowledgeApi.list(projectId)
  } finally {
    loading.value = false
  }
}

async function onUpload(file: File) {
  uploading.value = true
  try {
    await knowledgeApi.upload(projectId, file)
    ElMessage.success('文档上传并向量化成功')
    load()
  } finally {
    uploading.value = false
  }
  return false
}

async function onDelete(row: KnowledgeDoc) {
  await ElMessageBox.confirm(`删除文档「${row.fileName}」及其向量索引？`, '确认', { type: 'warning' })
  await knowledgeApi.remove(projectId, row.id)
  ElMessage.success('已删除')
  load()
}

async function onAsk() {
  if (!question.value.trim() || asking.value) return
  asking.value = true
  answer.value = ''
  abortController = new AbortController()
  const timeoutId = window.setTimeout(() => abortController?.abort(), 90000)
  try {
    const token = localStorage.getItem('accessToken')
    const resp = await fetch(aiApi.knowledgeQaStreamUrl(projectId), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream'
      },
      body: JSON.stringify({ question: question.value }),
      signal: abortController.signal
    })
    if (!resp.ok || !resp.body) {
      const errText = await resp.text().catch(() => `HTTP ${resp.status}`)
      throw new Error(errText || `HTTP ${resp.status}`)
    }
    const reader = resp.body.getReader()
    await consumeSse(reader, (chunk) => {
      answer.value += chunk
    })
    if (!answer.value) answer.value = '（AI 未返回内容）'
  } catch (e: any) {
    if (e?.name !== 'AbortError' || !answer.value) {
      ElMessage.error(e?.name === 'AbortError' ? '已停止生成' : e.message)
      if (!answer.value) answer.value = `请求失败：${e.message}`
    }
  } finally {
    clearTimeout(timeoutId)
    abortController = null
    asking.value = false
  }
}

function stopAsk() {
  abortController?.abort()
}

onUnmounted(() => {
  abortController?.abort()
})

onMounted(load)
</script>

<style scoped>
.answer {
  margin-top: 16px;
  padding: 16px;
  background: #fffdf9;
  border-radius: 12px;
  line-height: 1.8;
  white-space: pre-wrap;
  border-left: 3px solid #6c9a8b;
  border: 1px solid rgba(120,115,100,0.08);
}
.cursor {
  display: inline-block;
  width: 8px;
  height: 16px;
  background: #409eff;
  vertical-align: text-bottom;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}
</style>
