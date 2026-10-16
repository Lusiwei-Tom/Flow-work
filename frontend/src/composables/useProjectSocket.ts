import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ref, onUnmounted } from 'vue'
import type { Activity } from '@/api/project'

/**
 * STOMP WebSocket 组合式函数
 * 订阅 /topic/project.{projectId}，接收实时任务/动态事件
 */
export function useProjectSocket(projectId: number, onEvent: (event: Activity) => void) {
  const connected = ref(false)
  let client: Client | null = null

  function connect() {
    const token = localStorage.getItem('accessToken') || ''
    client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${encodeURIComponent(token)}`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    })

    client.onConnect = () => {
      connected.value = true
      client?.subscribe(`/topic/project.${projectId}`, (message) => {
        try {
          onEvent(JSON.parse(message.body))
        } catch (e) {
          console.warn('实时消息解析失败', e)
        }
      })
    }
    client.onDisconnect = () => {
      connected.value = false
    }
    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
  }

  onUnmounted(disconnect)

  return { connected, connect, disconnect }
}
