import http from './http'

export interface Project {
  id: number
  name: string
  description: string
  ownerId: number
  status: number
  startDate?: string
  endDate?: string
}

export interface Task {
  id: number
  projectId: number
  columnId: number
  title: string
  description?: string
  assigneeId?: number
  priority: number
  estimatedHours?: number
  actualHours?: number
  dueDate?: string
  status: number
  parentId?: number
  sortOrder: number
  aiGenerated: number
  aiSource?: string
}

export interface BoardColumn {
  id: number
  projectId: number
  name: string
  sortOrder: number
  status: number
}

export interface Activity {
  id: number
  projectId: number
  taskId?: number
  type: string
  actorId: number
  actorName: string
  content?: string
  aiGenerated: number
  createdAt: string
}

const projectApi = {
  mine: (params?: { page?: number; size?: number }) => http.get('/api/projects', { params }),
  create: (data: Partial<Project>) => http.post('/api/projects', data),
  detail: (id: number) => http.get(`/api/projects/${id}`),
  update: (id: number, data: Partial<Project>) => http.put(`/api/projects/${id}`, data),
  archive: (id: number) => http.post(`/api/projects/${id}/archive`),
  remove: (id: number) => http.delete(`/api/projects/${id}`)
}

/** 项目成员管理 */
export const memberApi = {
  list: (projectId: number) => http.get(`/api/projects/${projectId}/members`),
  add: (projectId: number, data: { username: string; projectRole: string }) =>
    http.post(`/api/projects/${projectId}/members`, data),
  updateRole: (projectId: number, userId: number, projectRole: string) =>
    http.put(`/api/projects/${projectId}/members/${userId}`, { projectRole }),
  remove: (projectId: number, userId: number) =>
    http.delete(`/api/projects/${projectId}/members/${userId}`)
}

/** 我的任务（跨项目聚合） */
export const myTaskApi = {
  list: () => http.get('/api/my/tasks')
}

export const boardApi = {
  columns: (projectId: number) => http.get(`/api/projects/${projectId}/columns`),
  createColumn: (projectId: number, data: { name: string }) =>
    http.post(`/api/projects/${projectId}/columns`, data),
  renameColumn: (projectId: number, columnId: number, name: string) =>
    http.put(`/api/projects/${projectId}/columns/${columnId}`, { name }),
  deleteColumn: (projectId: number, columnId: number) =>
    http.delete(`/api/projects/${projectId}/columns/${columnId}`)
}

export const taskApi = {
  list: (projectId: number, params?: { keyword?: string; assigneeId?: number; priority?: number; status?: number }) =>
    http.get(`/api/projects/${projectId}/tasks`, { params }),
  create: (projectId: number, data: any) =>
    http.post(`/api/projects/${projectId}/tasks`, data),
  move: (projectId: number, data: { taskId: number; targetColumnId: number; sortOrder: number }) =>
    http.post(`/api/projects/${projectId}/tasks/move`, data),
  detail: (taskId: number) => http.get(`/api/tasks/${taskId}`),
  update: (taskId: number, data: any) => http.put(`/api/tasks/${taskId}`, data),
  setPriority: (taskId: number, priority: number) =>
    http.patch(`/api/tasks/${taskId}/priority`, { priority }),
  setAssignee: (taskId: number, assigneeId: number) =>
    http.patch(`/api/tasks/${taskId}/assignee`, { assigneeId }),
  setStatus: (taskId: number, status: number) =>
    http.patch(`/api/tasks/${taskId}/status`, { status }),
  remove: (taskId: number) => http.delete(`/api/tasks/${taskId}`),
  comments: (taskId: number) => http.get(`/api/tasks/${taskId}/comments`),
  addComment: (taskId: number, content: string) =>
    http.post(`/api/tasks/${taskId}/comments`, { content })
}

export const realtimeApi = {
  activities: (projectId: number) => http.get(`/api/projects/${projectId}/activities`),
  online: (projectId: number) => http.get(`/api/projects/${projectId}/online`),
  heartbeat: (projectId: number) => http.post(`/api/projects/${projectId}/heartbeat`),
  leave: (projectId: number) => http.post(`/api/projects/${projectId}/leave`)
}

export default projectApi
