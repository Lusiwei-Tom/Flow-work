<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
          <el-icon color="#e6a23c"><MagicStick /></el-icon>
          <strong>AI 项目管家</strong>
          <el-tag size="small" type="warning" effect="plain">ReActAgent · 可调用真实业务工具</el-tag>
          <el-button v-if="!streaming" size="small" text type="danger" style="margin-left:auto"
            @click="onClearHistory">清空对话</el-button>
        </div>
      </template>

      <div class="chat-container">
        <div ref="msgBox" class="chat-messages">
          <div v-for="(m, i) in messages" :key="i" class="chat-bubble" :class="m.role">
            <div v-if="m.role === 'ai'">
              <el-icon color="#e6a23c"><MagicStick /></el-icon>
              <span style="margin-left:4px">{{ m.content || 'AI 正在思考（可能先调用工具查询，约 10~60 秒）...' }}</span>
            </div>
            <div v-else>{{ m.content }}</div>
          </div>
        </div>
        <div style="display:flex;gap:8px;padding:10px;border-top:1px solid #ebeef5">
          <el-input v-model="input" type="textarea" :rows="2" resize="none"
            :disabled="streaming"
            placeholder='试试："帮我查一下有哪些逾期任务"、"把 #3 任务优先级改成紧急"（Enter 发送，Shift+Enter 换行）'
            @keydown.enter.exact.prevent="send"
            @keydown.enter.ctrl.exact.prevent="send" />
          <el-button v-if="!streaming" type="primary" @click="send" style="height:auto">
            发送
          </el-button>
          <el-button v-else type="danger" @click="stop" style="height:auto">
            停止
          </el-button>
        </div>
      </div>
      <div style="margin-top:8px;color:#909399;font-size:12px">
        提示：AI 可查询任务/逾期、搜索知识库、修改任务优先级/负责人/状态，写操作会触发实时动态推送
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import aiApi from '@/api/ai'

const route = useRoute()
const projectId = Number(route.params.id)

const WELCOME = '你好，我是 FlowWork AI 项目管家，可以帮你查询任务、分析风险、检索知识库，也可以直接修改任务。'

interface Msg { role: 'user' | 'ai'; content: string }
const messages = ref<Msg[]>([{ role: 'ai', content: WELCOME }])
const input = ref('')
const streaming = ref(false)
const msgBox = ref<HTMLElement>()
let abortController: AbortController | null = null

async function scrollBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

/** 解析 SSE 流：按 \n\n 切事件，事件内多个 data: 行用 \n 连接；data: 后首空格为分隔符 */
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
              // data: 后第一个空格是分隔符，token 自身前导空格需保留
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

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const aiMsg: Msg = { role: 'ai', content: '' }
  messages.value.push(aiMsg)
  streaming.value = true
  await scrollBottom()

  const token = localStorage.getItem('accessToken')
  abortController = new AbortController()
  // DeepSeek 流式工具调用偶发挂起：120 秒未完成则强制中止，把已收到内容作为最终回答
  const timeoutId = window.setTimeout(() => abortController?.abort(), 120000)
  try {
    const resp = await fetch(`/api/projects/${projectId}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream'
      },
      body: JSON.stringify({ message: text }),
      signal: abortController.signal
    })
    if (!resp.ok || !resp.body) {
      const errText = await resp.text().catch(() => `HTTP ${resp.status}`)
      throw new Error(errText || `HTTP ${resp.status}`)
    }
    const reader = resp.body.getReader()
    await consumeSse(reader, (chunk) => {
      aiMsg.content += chunk
      scrollBottom()
    })
    if (!aiMsg.content) aiMsg.content = '（AI 未返回内容）'
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      if (!aiMsg.content) {
        aiMsg.content = '（AI 响应超时，已停止生成）'
        ElMessage.warning('AI 响应超时，请重试')
      } else {
        aiMsg.content += '\n\n_（已停止生成）_'
      }
    } else {
      if (!aiMsg.content) aiMsg.content = `请求失败：${e.message}`
      ElMessage.error(e.message)
    }
  } finally {
    clearTimeout(timeoutId)
    abortController = null
    streaming.value = false
    await scrollBottom()
  }
}

function stop() {
  abortController?.abort()
}

/** 进入页面加载最近 50 条历史；有历史则覆盖欢迎语 */
async function loadHistory() {
  try {
    const list: any[] = await aiApi.history(projectId, 50)
    if (list && list.length) {
      messages.value = list.map(m => ({ role: m.role as 'user' | 'ai', content: m.content }))
      await scrollBottom()
    }
  } catch {
    // 历史加载失败不影响使用
  }
}

async function onClearHistory() {
  try {
    await ElMessageBox.confirm('确定清空当前项目的 AI 对话历史？（不可恢复）', '清空对话', { type: 'warning' })
    await aiApi.clearHistory(projectId)
    messages.value = [{ role: 'ai', content: WELCOME }]
    ElMessage.success('已清空')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '清空失败')
  }
}

onMounted(loadHistory)
</script>
