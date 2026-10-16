import http from './http'

export interface Notification {
  id: number
  userId: number
  type: string
  title: string
  content?: string
  projectId?: number
  taskId?: number
  isRead: number
  createdAt: string
}

export interface MemberInfo {
  userId: number
  username: string
  nickname: string
  role: string
  joinedAt: string
}

/** 站内通知 */
export const notifyApi = {
  list: (onlyUnread = false) => http.get<Notification[]>('/api/notifications', { params: { onlyUnread } }),
  unreadCount: () => http.get<{ count: number }>('/api/notifications/unread-count'),
  markRead: (id: number) => http.put(`/api/notifications/${id}/read`),
  markAllRead: () => http.put('/api/notifications/read-all')
}

/** 用户搜索（邀请成员） */
export const userApi = {
  search: (keyword: string) =>
    http.get<{ id: number; username: string; nickname: string }[]>('/api/users/search', {
      params: { keyword }
    })
}
