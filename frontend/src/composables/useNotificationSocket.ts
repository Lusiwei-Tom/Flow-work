import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { onUnmounted, ref } from 'vue'
import type { Notification } from '@/api/notify'

/**
 * 个人通知 WebSocket：订阅 /user/queue/notifications
 * 收到新通知时回调（用于刷新未读数 + 弹出提示）
 */
export function useNotificationSocket(onNew: (n: Notification) => void) {
  const connected = ref(false)
  let client: Client | null = null
  let reconnectTimer: number | undefined

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
      client?.subscribe('/user/queue/notifications', (message) => {
        try {
          onNew(JSON.parse(message.body))
        } catch (e) {
          console.warn('通知解析失败', e)
        }
      })
    }
    client.onDisconnect = () => {
      connected.value = false
    }
    client.onStompError = () => {
      // token 过期等情况由 axios 拦截器统一处理，这里安排重连
      reconnectTimer = window.setTimeout(connect, 8000)
    }
    client.activate()
  }

  function disconnect() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    client?.deactivate()
    client = null
  }

  onUnmounted(disconnect)

  return { connected, connect, disconnect }
}
